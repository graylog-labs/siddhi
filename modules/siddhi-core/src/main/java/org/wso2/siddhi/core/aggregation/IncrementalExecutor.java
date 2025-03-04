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

import org.apache.log4j.Logger;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.event.ComplexEvent;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.stream.MetaStreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventPool;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.executor.VariableExpressionExecutor;
import org.wso2.siddhi.core.query.selector.GroupByKeyGenerator;
import org.wso2.siddhi.core.query.selector.attribute.processor.executor.GroupByAggregationAttributeExecutor;
import org.wso2.siddhi.core.table.Table;
import org.wso2.siddhi.core.util.IncrementalTimeConverterUtil;
import org.wso2.siddhi.core.util.Scheduler;
import org.wso2.siddhi.core.util.snapshot.Snapshotable;
import org.wso2.siddhi.query.api.aggregation.TimePeriod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Incremental executor class which is responsible for performing incremental aggregation.
 */
public class IncrementalExecutor implements Executor, Snapshotable {
    private static final Logger LOG = Logger.getLogger(IncrementalExecutor.class);

    private final StreamEvent resetEvent;
    private final ExpressionExecutor timestampExpressionExecutor;
    private final String aggregatorName;
    private TimePeriod.Duration duration;
    private Table table;
    private GroupByKeyGenerator groupByKeyGenerator;
    private StreamEventPool streamEventPool;
    private long nextEmitTime = -1;
    private long startTimeOfAggregates = -1;
    private boolean timerStarted = false;
    private boolean isGroupBy;
    private Executor next;
    private Scheduler scheduler;
    private boolean isRoot;
    private String elementId;
    private boolean isProcessingExecutor;
    private SiddhiAppContext siddhiAppContext;
    private ExecutorService executorService;

    private BaseIncrementalValueStore baseIncrementalValueStore;
    private Map<String, BaseIncrementalValueStore> baseIncrementalValueStoreGroupByMap = null;

    public IncrementalExecutor(String aggregatorName, TimePeriod.Duration duration,
                               List<ExpressionExecutor> processExpressionExecutors,
                               ExpressionExecutor shouldUpdateTimestamp,
                               GroupByKeyGenerator groupByKeyGenerator, MetaStreamEvent metaStreamEvent,
                               IncrementalExecutor child, boolean isRoot, Table table,
                               SiddhiAppContext siddhiAppContext) {
        this.duration = duration;
        this.next = child;
        this.isRoot = isRoot;
        this.table = table;
        this.siddhiAppContext = siddhiAppContext;
        this.aggregatorName = aggregatorName;
        this.streamEventPool = new StreamEventPool(metaStreamEvent, 10);
        this.timestampExpressionExecutor = processExpressionExecutors.remove(0);
        this.baseIncrementalValueStore = new BaseIncrementalValueStore(aggregatorName, -1, processExpressionExecutors,
                shouldUpdateTimestamp, streamEventPool, siddhiAppContext);
        this.isProcessingExecutor = false;

        if (groupByKeyGenerator != null) {
            this.isGroupBy = true;
            this.groupByKeyGenerator = groupByKeyGenerator;
            this.baseIncrementalValueStoreGroupByMap = new HashMap<>();
        } else {
            this.isGroupBy = false;
        }

        this.resetEvent = streamEventPool.borrowEvent();
        this.resetEvent.setType(ComplexEvent.Type.RESET);
        setNextExecutor(child);

        if (aggregatorName != null) {
            elementId = "IncrementalExecutor-" + siddhiAppContext.getElementIdGenerator().createNewId();
            siddhiAppContext.getSnapshotService().addSnapshotable(aggregatorName, this);
        }
        executorService = Executors.newSingleThreadExecutor();

    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public synchronized void execute(ComplexEventChunk streamEventChunk) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Event Chunk received by " + this.duration + " incremental executor: " +
                    streamEventChunk.toString());
        }
        streamEventChunk.reset();
        while (streamEventChunk.hasNext()) {
            StreamEvent streamEvent = (StreamEvent) streamEventChunk.next();
            streamEventChunk.remove();

            long timestamp = getTimestamp(streamEvent);

            startTimeOfAggregates = IncrementalTimeConverterUtil.getStartTimeOfAggregates(timestamp, duration);

            if (timestamp >= nextEmitTime) {
                nextEmitTime = IncrementalTimeConverterUtil.getNextEmitTime(timestamp, duration, null);
                dispatchAggregateEvents(startTimeOfAggregates);
                sendTimerEvent();
            }
            if (streamEvent.getType() == ComplexEvent.Type.CURRENT) {
                processAggregates(streamEvent);
            }
        }
    }

    private void sendTimerEvent() {
        if (getNextExecutor() != null) {
            StreamEvent timerEvent = streamEventPool.borrowEvent();
            timerEvent.setType(ComplexEvent.Type.TIMER);
            timerEvent.setTimestamp(startTimeOfAggregates);
            ComplexEventChunk<StreamEvent> timerStreamEventChunk = new ComplexEventChunk<>(true);
            timerStreamEventChunk.add(timerEvent);
            next.execute(timerStreamEventChunk);
        }
    }

    private long getTimestamp(StreamEvent streamEvent) {
        long timestamp;
        if (streamEvent.getType() == ComplexEvent.Type.CURRENT) {
            timestamp = (long) timestampExpressionExecutor.execute(streamEvent);
            if (isRoot && !timerStarted) {
                scheduler.notifyAt(IncrementalTimeConverterUtil.getNextEmitTime(timestamp, duration, null));
                timerStarted = true;
            }
        } else {
            timestamp = streamEvent.getTimestamp();
            if (isRoot) {
                // Scheduling is done by root incremental executor only
                scheduler.notifyAt(IncrementalTimeConverterUtil.getNextEmitTime(timestamp, duration, null));
            }
        }
        return timestamp;
    }

    @Override
    public Executor getNextExecutor() {
        return next;
    }

    @Override
    public void setNextExecutor(Executor nextExecutor) {
        next = nextExecutor;
    }

    private void processAggregates(StreamEvent streamEvent) {
        synchronized (this) {
            if (isGroupBy) {
                try {
                    String groupedByKey = groupByKeyGenerator.constructEventKey(streamEvent);
                    GroupByAggregationAttributeExecutor.getKeyThreadLocal().set(groupedByKey);
                    BaseIncrementalValueStore aBaseIncrementalValueStore = baseIncrementalValueStoreGroupByMap
                            .computeIfAbsent(groupedByKey,
                                    k -> baseIncrementalValueStore.cloneStore(startTimeOfAggregates));
                    process(streamEvent, aBaseIncrementalValueStore);
                } finally {
                    GroupByAggregationAttributeExecutor.getKeyThreadLocal().remove();
                }
            } else {
                process(streamEvent, baseIncrementalValueStore);
            }
        }
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

    private void dispatchAggregateEvents(long startTimeOfNewAggregates) {
        if (isGroupBy) {
            dispatchEvents(baseIncrementalValueStoreGroupByMap);
        } else {
            dispatchEvent(startTimeOfNewAggregates, baseIncrementalValueStore);
        }
    }

    private void dispatchEvent(long startTimeOfNewAggregates, BaseIncrementalValueStore aBaseIncrementalValueStore) {
        if (aBaseIncrementalValueStore.isProcessed()) {
            ComplexEventChunk<StreamEvent> eventChunk = new ComplexEventChunk<>(true);
            ComplexEventChunk<StreamEvent> tableEventChunk = new ComplexEventChunk<>(true);
            eventChunk.add(aBaseIncrementalValueStore.createStreamEvent());
            tableEventChunk.add(aBaseIncrementalValueStore.createStreamEvent());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Event dispatched by " + this.duration + " incremental executor: " + eventChunk.toString());
            }
            if (isProcessingExecutor) {
                executorService.execute(() -> {
                    try {
                        table.addEvents(tableEventChunk, 1);
                    } catch (Throwable t) {
                        LOG.error("Exception occurred at siddhi app '" + siddhiAppContext.getName() +
                                "' when performing table writes of aggregation '" + this.aggregatorName +
                                "' for duration '" + this.duration + "'. This should be investigated as this can " +
                                "cause accuracy loss.", t);
                    }
                });
            }
            if (getNextExecutor() != null) {
                next.execute(eventChunk);
            }
        }
        cleanBaseIncrementalValueStore(startTimeOfNewAggregates, aBaseIncrementalValueStore);
    }

    private void dispatchEvents(Map<String, BaseIncrementalValueStore> baseIncrementalValueGroupByStore) {
        int noOfEvents = baseIncrementalValueGroupByStore.size();
        if (noOfEvents > 0) {
            ComplexEventChunk<StreamEvent> eventChunk = new ComplexEventChunk<>(true);
            ComplexEventChunk<StreamEvent> tableEventChunk = new ComplexEventChunk<>(true);
            for (BaseIncrementalValueStore aBaseIncrementalValueStore : baseIncrementalValueGroupByStore.values()) {
                eventChunk.add(aBaseIncrementalValueStore.createStreamEvent());
                tableEventChunk.add(aBaseIncrementalValueStore.createStreamEvent());
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Event dispatched by " + this.duration + " incremental executor: " + eventChunk.toString());
            }
            if (isProcessingExecutor) {
                executorService.execute(() -> {
                    try {
                        table.addEvents(tableEventChunk, noOfEvents);
                    } catch (Throwable t) {
                        LOG.error("Exception occurred at siddhi app '" + siddhiAppContext.getName() +
                                "' when performing table writes of aggregation '" + this.aggregatorName +
                                "' for duration '" + this.duration + "'. This should be investigated as this can " +
                                "cause accuracy loss.", t);
                    }
                });
            }
            if (getNextExecutor() != null) {
                next.execute(eventChunk);
            }
        }
        for (BaseIncrementalValueStore baseIncrementalValueStore : baseIncrementalValueGroupByStore.values()) {
            baseIncrementalValueStore.clean();
        }
        baseIncrementalValueGroupByStore.clear();
    }

    private void cleanBaseIncrementalValueStore(long startTimeOfNewAggregates,
                                                BaseIncrementalValueStore baseIncrementalValueStore) {
        baseIncrementalValueStore.clearValues();
        baseIncrementalValueStore.setTimestamp(startTimeOfNewAggregates);
        baseIncrementalValueStore.setProcessed(false);
        for (ExpressionExecutor expressionExecutor : baseIncrementalValueStore.getExpressionExecutors()) {
            expressionExecutor.execute(resetEvent);
        }
    }


    Map<String, BaseIncrementalValueStore> getBaseIncrementalValueStoreGroupByMap() {
        return baseIncrementalValueStoreGroupByMap;
    }

    BaseIncrementalValueStore getBaseIncrementalValueStore() {
        return baseIncrementalValueStore;
    }

    public long getAggregationStartTimestamp() {
        return this.startTimeOfAggregates;
    }

    public void setEmitTimestamp(long emitTimeOfLatestEventInTable) {
        this.nextEmitTime = emitTimeOfLatestEventInTable;
    }

    public void setProcessingExecutor(boolean processingExecutor) {
        isProcessingExecutor = processingExecutor;
    }

    @Override
    public Map<String, Object> currentState() {
        Map<String, Object> state = new HashMap<>();

        state.put("NextEmitTime", nextEmitTime);
        state.put("StartTimeOfAggregates", startTimeOfAggregates);
        state.put("TimerStarted", timerStarted);
        return state;
    }

    @Override
    public void restoreState(Map<String, Object> state) {
        nextEmitTime = (long) state.get("NextEmitTime");
        startTimeOfAggregates = (long) state.get("StartTimeOfAggregates");
        timerStarted = (boolean) state.get("TimerStarted");
    }

    @Override
    public String getElementId() {
        return elementId;
    }

    @Override
    public void clean() {
        timestampExpressionExecutor.clean();
        baseIncrementalValueStore.clean();
        scheduler.clean();
        if (baseIncrementalValueStoreGroupByMap != null) {
            for (BaseIncrementalValueStore aBaseIncrementalValueStore : baseIncrementalValueStoreGroupByMap.values()) {
                aBaseIncrementalValueStore.clean();
            }
        }
        siddhiAppContext.getSnapshotService().removeSnapshotable(aggregatorName, this);
    }
}
