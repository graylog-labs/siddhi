/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.siddhi.core.aggregation;

import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.stream.MetaStreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventPool;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.executor.VariableExpressionExecutor;
import org.wso2.siddhi.core.query.selector.GroupByKeyGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class implements logic to process aggregates(after retrieval from tables) that were aggregated
 * using external timestamp.
 */
public class OutOfOrderEventsDataAggregator {

    private final GroupByKeyGenerator groupByKeyGenerator;
    private final BaseIncrementalValueStore baseIncrementalValueStore;
    private final Map<String, BaseIncrementalValueStore> baseIncrementalValueGroupByStore;

    public OutOfOrderEventsDataAggregator(List<ExpressionExecutor> baseExecutors,
                                          ExpressionExecutor shouldUpdateTimestamp,
                                          GroupByKeyGenerator groupByKeyGenerator,
                                          MetaStreamEvent metaStreamEvent) {

        StreamEventPool streamEventPool = new StreamEventPool(metaStreamEvent, 10);
        List<ExpressionExecutor> expressionExecutorsWithoutTime = baseExecutors.subList(1, baseExecutors.size());

        this.baseIncrementalValueStore = new BaseIncrementalValueStore(null, -1,
                expressionExecutorsWithoutTime, shouldUpdateTimestamp, streamEventPool, null);
        this.baseIncrementalValueGroupByStore = new HashMap<>();
        this.groupByKeyGenerator = groupByKeyGenerator;
    }

    public ComplexEventChunk<StreamEvent> aggregateData(ComplexEventChunk<StreamEvent> retrievedData) {
        while (retrievedData.hasNext()) {
            StreamEvent streamEvent = retrievedData.next();
            String groupByKey = groupByKeyGenerator.constructEventKey(streamEvent);
            BaseIncrementalValueStore baseIncrementalValueStore = baseIncrementalValueGroupByStore
                    .computeIfAbsent(groupByKey, k -> this.baseIncrementalValueStore.cloneStore(-1));
            process(streamEvent, baseIncrementalValueStore);
        }
        return createEventChunkFromAggregatedData();
    }

    private void process(StreamEvent streamEvent, BaseIncrementalValueStore baseIncrementalValueStore) {

        List<ExpressionExecutor> expressionExecutors = baseIncrementalValueStore.getExpressionExecutors();

        boolean shouldUpdate = true;
        ExpressionExecutor shouldUpdateTimestamp = baseIncrementalValueStore.getShouldUpdateTimestamp();
        if (shouldUpdateTimestamp != null) {
            shouldUpdate = ((boolean) shouldUpdateTimestamp.execute(streamEvent));
        }

        for (int i = 0; i < expressionExecutors.size(); i++) { // keeping timestamp value location as null

            ExpressionExecutor expressionExecutor = expressionExecutors.get(i);

            if (shouldUpdate) {
                baseIncrementalValueStore.setValue(expressionExecutor.execute(streamEvent), i + 1);
            } else if (!(expressionExecutor instanceof VariableExpressionExecutor)) {
                baseIncrementalValueStore.setValue(expressionExecutor.execute(streamEvent), i + 1);
            }
        }

        baseIncrementalValueStore.setProcessed(true);
    }

    private ComplexEventChunk<StreamEvent> createEventChunkFromAggregatedData() {
        ComplexEventChunk<StreamEvent> processedInMemoryEventChunk = new ComplexEventChunk<>(true);
        for (Map.Entry<String, BaseIncrementalValueStore> entryAgainstTime :
                baseIncrementalValueGroupByStore.entrySet()) {
            processedInMemoryEventChunk.add(entryAgainstTime.getValue().createStreamEvent());
            entryAgainstTime.getValue().clean();
        }
        return processedInMemoryEventChunk;
    }
}
