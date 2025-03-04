/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.siddhi.core.aggregation;

import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.event.stream.MetaStreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventPool;
import org.wso2.siddhi.core.query.StoreQueryRuntime;
import org.wso2.siddhi.core.table.Table;
import org.wso2.siddhi.core.util.IncrementalTimeConverterUtil;
import org.wso2.siddhi.core.util.parser.StoreQueryParser;
import org.wso2.siddhi.core.window.Window;
import org.wso2.siddhi.query.api.aggregation.TimePeriod;
import org.wso2.siddhi.query.api.execution.query.StoreQuery;
import org.wso2.siddhi.query.api.execution.query.input.store.InputStore;
import org.wso2.siddhi.query.api.execution.query.selection.OrderByAttribute;
import org.wso2.siddhi.query.api.execution.query.selection.Selector;
import org.wso2.siddhi.query.api.expression.Expression;
import org.wso2.siddhi.query.api.expression.condition.Compare;

import java.util.List;
import java.util.Map;

import static org.wso2.siddhi.core.util.SiddhiConstants.AGG_SHARD_ID_COL;
import static org.wso2.siddhi.core.util.SiddhiConstants.AGG_START_TIMESTAMP_COL;

/**
 * This class is used to recreate in-memory data from the tables (Such as RDBMS) in incremental aggregation.
 * This ensures that the aggregation calculations are done correctly in case of server restart
 */
public class IncrementalExecutorsInitialiser {
    private final List<TimePeriod.Duration> incrementalDurations;
    private final Map<TimePeriod.Duration, Table> aggregationTables;
    private final Map<TimePeriod.Duration, IncrementalExecutor> incrementalExecutorMap;

    private final boolean isDistributed;
    private final String shardId;

    private final SiddhiAppContext siddhiAppContext;
    private final StreamEventPool streamEventPool;
    private final Map<String, Table> tableMap;
    private final Map<String, Window> windowMap;
    private final Map<String, AggregationRuntime> aggregationMap;

    private boolean isInitialised;

    public IncrementalExecutorsInitialiser(List<TimePeriod.Duration> incrementalDurations,
                                           Map<TimePeriod.Duration, Table> aggregationTables,
                                           Map<TimePeriod.Duration, IncrementalExecutor> incrementalExecutorMap,
                                           boolean isDistributed, String shardId, SiddhiAppContext siddhiAppContext,
                                           MetaStreamEvent metaStreamEvent, Map<String, Table> tableMap,
                                           Map<String, Window> windowMap,
                                           Map<String, AggregationRuntime> aggregationMap) {
        this.incrementalDurations = incrementalDurations;
        this.aggregationTables = aggregationTables;
        this.incrementalExecutorMap = incrementalExecutorMap;

        this.isDistributed = isDistributed;
        this.shardId = shardId;

        this.siddhiAppContext = siddhiAppContext;
        this.streamEventPool = new StreamEventPool(metaStreamEvent, 10);
        this.tableMap = tableMap;
        this.windowMap = windowMap;
        this.aggregationMap = aggregationMap;

        this.isInitialised = false;
    }

    public synchronized void initialiseExecutors() {
        if (this.isInitialised) {
            return;
        }

        Event[] events;
        Long endOFLatestEventTimestamp = null;

        // Get max(AGG_TIMESTAMP) from table corresponding to max duration
        Table tableForMaxDuration = aggregationTables.get(incrementalDurations.get(incrementalDurations.size() - 1));
        StoreQuery storeQuery = getStoreQuery(tableForMaxDuration, true, endOFLatestEventTimestamp);
        storeQuery.setType(StoreQuery.StoreQueryType.FIND);
        StoreQueryRuntime storeQueryRuntime = StoreQueryParser.parse(storeQuery, siddhiAppContext, tableMap, windowMap,
                aggregationMap);

        // Get latest event timestamp in tableForMaxDuration and get the end time of the aggregation record
        events = storeQueryRuntime.execute();
        if (events != null) {
            Long lastData = (Long) events[events.length - 1].getData(0);
            endOFLatestEventTimestamp = IncrementalTimeConverterUtil
                    .getNextEmitTime(lastData, incrementalDurations.get(incrementalDurations.size() - 1), null);
        }

        for (int i = incrementalDurations.size() - 1; i > 0; i--) {
            TimePeriod.Duration recreateForDuration = incrementalDurations.get(i);
            IncrementalExecutor incrementalExecutor = incrementalExecutorMap.get(recreateForDuration);

            // Get the table previous to the duration for which we need to recreate (e.g. if we want to recreate
            // for minute duration, take the second table [provided that aggregation is done for seconds])
            // This lookup is filtered by endOFLatestEventTimestamp
            Table recreateFromTable = aggregationTables.get(incrementalDurations.get(i - 1));

            storeQuery = getStoreQuery(recreateFromTable, false, endOFLatestEventTimestamp);
            storeQuery.setType(StoreQuery.StoreQueryType.FIND);
            storeQueryRuntime = StoreQueryParser.parse(storeQuery, siddhiAppContext, tableMap, windowMap,
                    aggregationMap);
            events = storeQueryRuntime.execute();

            if (events != null) {
                long referenceToNextLatestEvent = (Long) events[events.length - 1].getData(0);
                endOFLatestEventTimestamp = IncrementalTimeConverterUtil
                        .getNextEmitTime(referenceToNextLatestEvent, incrementalDurations.get(i - 1), null);

                ComplexEventChunk<StreamEvent> complexEventChunk = new ComplexEventChunk<>(false);
                for (Event event : events) {
                    StreamEvent streamEvent = streamEventPool.borrowEvent();
                    streamEvent.setOutputData(event.getData());
                    complexEventChunk.add(streamEvent);
                }
                incrementalExecutor.execute(complexEventChunk);

                if (i == 1) {
                    TimePeriod.Duration rootDuration = incrementalDurations.get(0);
                    IncrementalExecutor rootIncrementalExecutor = incrementalExecutorMap.get(rootDuration);
                    long emitTimeOfLatestEventInTable = IncrementalTimeConverterUtil.getNextEmitTime(
                            referenceToNextLatestEvent, rootDuration, null);

                    rootIncrementalExecutor.setEmitTimestamp(emitTimeOfLatestEventInTable);

                }
            }
        }
        this.isInitialised = true;
    }

    private StoreQuery getStoreQuery(Table table, boolean isLargestGranularity, Long endOFLatestEventTimestamp) {
        Selector selector = Selector.selector();
        if (isLargestGranularity) {
            selector = selector
                    .orderBy(
                            Expression.variable(AGG_START_TIMESTAMP_COL), OrderByAttribute.Order.DESC)
                    .limit(Expression.value(1));
        } else {
            selector = selector.orderBy(Expression.variable(AGG_START_TIMESTAMP_COL));
        }

        InputStore inputStore;
        if (!this.isDistributed) {
            if (endOFLatestEventTimestamp == null) {
                inputStore = InputStore.store(table.getTableDefinition().getId());
            } else {
                inputStore = InputStore.store(table.getTableDefinition().getId())
                        .on(Expression.compare(
                                Expression.variable(AGG_START_TIMESTAMP_COL),
                                Compare.Operator.GREATER_THAN_EQUAL,
                                Expression.value(endOFLatestEventTimestamp)
                        ));
            }
        } else {
            if (endOFLatestEventTimestamp == null) {
                inputStore = InputStore.store(table.getTableDefinition().getId()).on(
                        Expression.compare(Expression.variable(AGG_SHARD_ID_COL), Compare.Operator.EQUAL,
                                Expression.value(shardId)));
            } else {
                inputStore = InputStore.store(table.getTableDefinition().getId()).on(
                        Expression.and(
                                Expression.compare(
                                        Expression.variable(AGG_SHARD_ID_COL),
                                        Compare.Operator.EQUAL,
                                        Expression.value(shardId)),
                                Expression.compare(
                                        Expression.variable(AGG_START_TIMESTAMP_COL),
                                        Compare.Operator.GREATER_THAN_EQUAL,
                                        Expression.value(endOFLatestEventTimestamp))));
            }
        }

        return StoreQuery.query().from(inputStore).select(selector);
    }
}
