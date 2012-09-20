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
package org.apache.jackrabbit.mongomk.query;

import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.mongomk.MongoConnection;
import org.apache.jackrabbit.mongomk.model.CommitMongo;


/**
 * An query for fetching valid revisions.
 *
 * @author <a href="mailto:pmarx@adobe.com>Philipp Marx</a>
 */
public class FetchValidRevisionsQuery extends AbstractQuery<List<Long>> {

    private final String maxRevisionId;

    /**
     * Constructs a new {@code FetchValidRevisionsQuery}.
     *
     * @param mongoConnection
     *            The {@link MongoConnection}.
     * @param maxRevisionId
     *            The max revision id which should be fetched.
     */
    public FetchValidRevisionsQuery(MongoConnection mongoConnection, String maxRevisionId) {
        super(mongoConnection);
        this.maxRevisionId = maxRevisionId;
    }

    @Override
    public List<Long> execute() {
        List<CommitMongo> validCommits = fetchValidCommits();
        List<Long> validRevisions = new ArrayList<Long>(validCommits.size());
        for (CommitMongo commitMongo : validCommits) {
            validRevisions.add(commitMongo.getRevisionId());
        }

        return validRevisions;
    }

    private List<CommitMongo> fetchValidCommits() {
        return new FetchValidCommitsQuery(mongoConnection, maxRevisionId).execute();
    }
}