/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.plugins.index.property;

import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.REINDEX_PROPERTY_NAME;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.PropertyType;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.memory.MemoryPropertyBuilder;
import org.apache.jackrabbit.oak.spi.query.PropertyValues;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.PropertyBuilder;

/**
 * Takes care of applying the updates to the index content.
 * <p>
 * The changes are temporarily added to an in-memory structure, and then applied
 * to the node.
 */
@Deprecated
class PropertyIndexUpdate {

    /**
     * The path of the index definition (where the index data is stored).
     */
    private final String path;

    /**
     * The node where the index definition is stored.
     */
    private final NodeBuilder node;

    /**
     * The set of added values / paths. The key of the map is the property value
     * (encoded as a string), the value of the map is a set of paths that where
     * added.
     */
    private final Map<String, Set<String>> insert;

    /**
     * The set of removed values / paths. The key of the map is the property
     * value (encoded as a string), the value of the map is a set of paths that
     * were removed.
     */
    private final Map<String, Set<String>> remove;

    public PropertyIndexUpdate(String path, NodeBuilder node) {
        this.path = path;
        this.node = node;
        this.insert = Maps.newHashMap();
        this.remove = Maps.newHashMap();
    }

    String getPath() {
        return path;
    }

    /**
     * A property value was added at the given path.
     * 
     * @param path the path
     * @param value the value
     */
    public void insert(String path, PropertyState value) {
        Preconditions.checkArgument(path.startsWith(this.path));
        putValues(insert, path.substring(this.path.length()), value);
    }

    /**
     * A property value was removed at the given path.
     * 
     * @param path the path
     * @param value the value
     */
    public void remove(String path, PropertyState value) {
        Preconditions.checkArgument(path.startsWith(this.path));
        putValues(remove, path.substring(this.path.length()), value);
    }

    private static void putValues(Map<String, Set<String>> map, String path,
            PropertyState value) {
        if (value.getType().tag() != PropertyType.BINARY) {
            List<String> keys = PropertyIndex.encode(PropertyValues.create(value));
            for (String key : keys) {
                Set<String> paths = map.get(key);
                if (paths == null) {
                    paths = Sets.newHashSet();
                    map.put(key, paths);
                }
                paths.add(path);
            }
        }
    }

    boolean getAndResetReindexFlag() {
        boolean reindex = node.getProperty(REINDEX_PROPERTY_NAME) != null
                && node.getProperty(REINDEX_PROPERTY_NAME).getValue(
                        Type.BOOLEAN);
        node.setProperty(REINDEX_PROPERTY_NAME, false);
        return reindex;
    }

    /**
     * Try to apply the changes to the index content (to the ":index" node.
     * 
     * @throws CommitFailedException if a unique index was violated
     */
    public void apply() throws CommitFailedException {
        boolean unique = node.getProperty("unique") != null
                && node.getProperty("unique").getValue(Type.BOOLEAN);
        NodeBuilder index = node.child(":index");

        for (Map.Entry<String, Set<String>> entry : remove.entrySet()) {
            String encoded = entry.getKey();
            Set<String> paths = entry.getValue();
            PropertyState property = index.getProperty(encoded);
            if (property != null) {
                PropertyBuilder<String> builder = MemoryPropertyBuilder.array(Type.STRING, encoded);
                for (String value : property.getValue(Type.STRINGS)) {
                    if (!paths.contains(value)) {
                        builder.addValue(value);
                    }
                }
                if (builder.isEmpty()) {
                    index.removeProperty(encoded);
                } else {
                    index.setProperty(builder.getPropertyState());
                }
            }
        }

        for (Map.Entry<String, Set<String>> entry : insert.entrySet()) {
            String encoded = entry.getKey();
            Set<String> paths = entry.getValue();
            PropertyState property = index.getProperty(encoded);
            PropertyBuilder<String> builder = MemoryPropertyBuilder.array(Type.STRING)
                    .setName(encoded)
                    .assignFrom(property);
            for (String path : paths) {
                if (!builder.hasValue(path)) {
                    builder.addValue(path);
                }
            }
            if (builder.isEmpty()) {
                index.removeProperty(encoded);
            } else if (unique && builder.count() > 1) {
                throw new CommitFailedException(
                        "Uniqueness constraint violated");
            } else {
                index.setProperty(builder.getPropertyState());
            }
        }
    }

}
