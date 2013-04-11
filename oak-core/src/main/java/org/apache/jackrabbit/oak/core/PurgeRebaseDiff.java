/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.jackrabbit.oak.core;

import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.spi.state.AbstractRebaseDiff;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.apache.jackrabbit.oak.util.TODO;

/**
 * FIXME rename, document
 */
class PurgeRebaseDiff extends AbstractRebaseDiff {
    private PurgeRebaseDiff(NodeBuilder builder) {
        super(builder);
    }

    public static NodeState rebase(NodeState before, NodeState after, NodeBuilder builder) {
        after.compareAgainstBaseState(before, new PurgeRebaseDiff(builder));
        return builder.getNodeState();
    }

    @Override
    protected PurgeRebaseDiff createDiff(NodeBuilder builder, String name) {
        return new PurgeRebaseDiff(builder.child(name));
    }

    @Override
    protected void addExistingProperty(NodeBuilder builder, PropertyState after) {
        conflict();
    }

    @Override
    protected void changeDeletedProperty(NodeBuilder builder, PropertyState after) {
        conflict();
    }

    @Override
    protected void changeChangedProperty(NodeBuilder builder, PropertyState before, PropertyState after) {
        conflict();
    }

    @Override
    protected void deleteDeletedProperty(NodeBuilder builder, PropertyState before) {
        conflict();
    }

    @Override
    protected void deleteChangedProperty(NodeBuilder builder, PropertyState before) {
        conflict();
    }

    @Override
    protected void addExistingNode(NodeBuilder builder, String name, NodeState after) {
        conflict();
    }

    @Override
    protected void changeDeletedNode(NodeBuilder builder, String name, NodeState after) {
        conflict();
    }

    @Override
    protected void deleteDeletedNode(NodeBuilder builder, String name, NodeState before) {
        conflict();
    }

    @Override
    protected void deleteChangedNode(NodeBuilder builder, String name, NodeState before) {
        conflict();
    }

    private static void conflict() {
        // FIXME correctly handle conflict cases
        try {
            TODO.unimplemented().doNothing();
        }
        catch (UnsupportedRepositoryOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}