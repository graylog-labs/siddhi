/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.siddhi.core;

import com.lmax.disruptor.ExceptionHandler;
import org.apache.log4j.Logger;
import org.wso2.siddhi.core.aggregation.AggregationRuntime;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.debugger.SiddhiDebugger;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.exception.CannotClearSiddhiAppStateException;
import org.wso2.siddhi.core.exception.CannotRestoreSiddhiAppStateException;
import org.wso2.siddhi.core.exception.DefinitionNotExistException;
import org.wso2.siddhi.core.exception.QueryNotExistException;
import org.wso2.siddhi.core.exception.SiddhiAppRuntimeException;
import org.wso2.siddhi.core.exception.StoreQueryCreationException;
import org.wso2.siddhi.core.partition.PartitionRuntime;
import org.wso2.siddhi.core.query.QueryRuntime;
import org.wso2.siddhi.core.query.StoreQueryRuntime;
import org.wso2.siddhi.core.query.input.stream.StreamRuntime;
import org.wso2.siddhi.core.query.input.stream.single.SingleStreamRuntime;
import org.wso2.siddhi.core.query.output.callback.OutputCallback;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.StreamJunction;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.input.InputManager;
import org.wso2.siddhi.core.stream.input.source.Source;
import org.wso2.siddhi.core.stream.input.source.SourceHandlerManager;
import org.wso2.siddhi.core.stream.output.StreamCallback;
import org.wso2.siddhi.core.stream.output.sink.Sink;
import org.wso2.siddhi.core.stream.output.sink.SinkCallback;
import org.wso2.siddhi.core.stream.output.sink.SinkHandlerManager;
import org.wso2.siddhi.core.table.Table;
import org.wso2.siddhi.core.table.record.RecordTableHandler;
import org.wso2.siddhi.core.table.record.RecordTableHandlerManager;
import org.wso2.siddhi.core.util.ExceptionUtil;
import org.wso2.siddhi.core.util.Scheduler;
import org.wso2.siddhi.core.util.SiddhiConstants;
import org.wso2.siddhi.core.util.StringUtil;
import org.wso2.siddhi.core.util.extension.holder.EternalReferencedHolder;
import org.wso2.siddhi.core.util.parser.StoreQueryParser;
import org.wso2.siddhi.core.util.parser.helper.QueryParserHelper;
import org.wso2.siddhi.core.util.persistence.util.PersistenceHelper;
import org.wso2.siddhi.core.util.snapshot.PersistenceReference;
import org.wso2.siddhi.core.util.statistics.BufferedEventsTracker;
import org.wso2.siddhi.core.util.statistics.LatencyTracker;
import org.wso2.siddhi.core.util.statistics.MemoryUsageTracker;
import org.wso2.siddhi.core.util.statistics.metrics.Level;
import org.wso2.siddhi.core.window.Window;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.definition.AggregationDefinition;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.definition.StreamDefinition;
import org.wso2.siddhi.query.api.definition.TableDefinition;
import org.wso2.siddhi.query.api.definition.WindowDefinition;
import org.wso2.siddhi.query.api.exception.SiddhiAppContextException;
import org.wso2.siddhi.query.api.execution.query.StoreQuery;
import org.wso2.siddhi.query.compiler.SiddhiCompiler;

import java.beans.ExceptionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Keep streamDefinitions, partitionRuntimes, queryRuntimes of an SiddhiApp and streamJunctions and inputHandlers used.
 */
public class SiddhiAppRuntime {
    private static final Logger log = Logger.getLogger(SiddhiAppRuntime.class);
    private final Map<String, Window> windowMap;
    private final Map<String, List<Source>> sourceMap;
    private final Map<String, List<Sink>> sinkMap;
    private ConcurrentMap<String, AggregationRuntime> aggregationMap;
    private Map<String, AbstractDefinition> streamDefinitionMap =
            new ConcurrentHashMap<String,
                    AbstractDefinition>(); // Contains stream definition.
    private Map<String, AbstractDefinition> tableDefinitionMap =
            new ConcurrentHashMap<String,
                    AbstractDefinition>(); // Contains table definition.
    private Map<String, AbstractDefinition> windowDefinitionMap =
            new ConcurrentHashMap<String,
                    AbstractDefinition>(); // Contains window definition.
    private Map<String, AbstractDefinition> aggregationDefinitionMap =
            new ConcurrentHashMap<String,
                    AbstractDefinition>(); // Contains aggregation definition.
    private InputManager inputManager;
    private Map<String, QueryRuntime> queryProcessorMap =
            Collections.synchronizedMap(new LinkedHashMap<String, QueryRuntime>());
    private Map<String, StreamJunction> streamJunctionMap =
            new ConcurrentHashMap<String, StreamJunction>(); // Contains stream junctions.
    private Map<String, Table> tableMap = new ConcurrentHashMap<String, Table>(); // Contains event tables.
    private Map<String, PartitionRuntime> partitionMap =
            new ConcurrentHashMap<String, PartitionRuntime>(); // Contains partitions.
    private LinkedHashMap<StoreQuery, StoreQueryRuntime> storeQueryRuntimeMap =
            new LinkedHashMap<>(); // Contains partitions.
    private SiddhiAppContext siddhiAppContext;
    private Map<String, SiddhiAppRuntime> siddhiAppRuntimeMap;
    private MemoryUsageTracker memoryUsageTracker;
    private BufferedEventsTracker bufferedEventsTracker;
    private LatencyTracker storeQueryLatencyTracker;
    private SiddhiDebugger siddhiDebugger;
    private boolean running = false;
    private boolean runningWithoutSources = false;
    private Future futureIncrementalPersistor;
    private boolean incrementalDataPurging = true;


    public SiddhiAppRuntime(Map<String, AbstractDefinition> streamDefinitionMap,
                            Map<String, AbstractDefinition> tableDefinitionMap,
                            Map<String, AbstractDefinition> windowDefinitionMap,
                            Map<String, AbstractDefinition> aggregationDefinitionMap,
                            InputManager inputManager,
                            Map<String, QueryRuntime> queryProcessorMap,
                            Map<String, StreamJunction> streamJunctionMap,
                            Map<String, Table> tableMap,
                            Map<String, Window> windowMap,
                            ConcurrentMap<String, AggregationRuntime> aggregationMap,
                            Map<String, List<Source>> sourceMap,
                            Map<String, List<Sink>> sinkMap,
                            Map<String, PartitionRuntime> partitionMap,
                            SiddhiAppContext siddhiAppContext,
                            Map<String, SiddhiAppRuntime> siddhiAppRuntimeMap) {
        this.streamDefinitionMap = streamDefinitionMap;
        this.tableDefinitionMap = tableDefinitionMap;
        this.windowDefinitionMap = windowDefinitionMap;
        this.aggregationDefinitionMap = aggregationDefinitionMap;
        this.inputManager = inputManager;
        this.queryProcessorMap = queryProcessorMap;
        this.streamJunctionMap = streamJunctionMap;
        this.tableMap = tableMap;
        this.windowMap = windowMap;
        this.aggregationMap = aggregationMap;
        this.sourceMap = sourceMap;
        this.sinkMap = sinkMap;
        this.partitionMap = partitionMap;
        this.siddhiAppContext = siddhiAppContext;
        this.siddhiAppRuntimeMap = siddhiAppRuntimeMap;
        if (siddhiAppContext.getStatisticsManager() != null) {
            monitorQueryMemoryUsage();
            monitorBufferedEvents();
            storeQueryLatencyTracker = QueryParserHelper.createLatencyTracker(siddhiAppContext, "query",
                    SiddhiConstants.METRIC_INFIX_STORE_QUERIES, null);
        }

        for (Map.Entry<String, List<Sink>> sinkEntries : sinkMap.entrySet()) {
            addCallback(sinkEntries.getKey(),
                    new SinkCallback(sinkEntries.getValue(), streamDefinitionMap.get(sinkEntries.getKey())));
        }
        for (Map.Entry<String, List<Source>> sourceEntries : sourceMap.entrySet()) {
            InputHandler inputHandler = getInputHandler(sourceEntries.getKey());
            for (Source source : sourceEntries.getValue()) {
                source.getMapper().setInputHandler(inputHandler);
            }
        }
    }

    public String getName() {
        return siddhiAppContext.getName();
    }

    /**
     * Get the stream definition map.
     *
     * @return Map of {@link StreamDefinition}s.
     */
    public Map<String, StreamDefinition> getStreamDefinitionMap() {
        return streamDefinitionMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> (StreamDefinition) e.getValue()));
    }

    /**
     * Get the table definition map.
     *
     * @return Map of {@link TableDefinition}s.
     */
    public Map<String, TableDefinition> getTableDefinitionMap() {
        return tableDefinitionMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> (TableDefinition) e.getValue()));
    }

    /**
     * Get the window definition map.
     *
     * @return Map of {@link WindowDefinition}s.
     */
    public Map<String, WindowDefinition> getWindowDefinitionMap() {
        return windowDefinitionMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> (WindowDefinition) e.getValue()));
    }

    /**
     * Get the aggregation definition map.
     *
     * @return Map of {@link AggregationDefinition}s.
     */
    public Map<String, AggregationDefinition> getAggregationDefinitionMap() {
        return aggregationDefinitionMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> (AggregationDefinition) e.getValue()));
    }

    /**
     * Get the names of the available queries.
     *
     * @return string set of query names.
     */
    public Set<String> getQueryNames() {
        return queryProcessorMap.keySet();
    }

    public Map<String, Map<String, AbstractDefinition>> getPartitionedInnerStreamDefinitionMap() {
        Map<String, Map<String, AbstractDefinition>> innerStreams = new HashMap<>();
        for (PartitionRuntime partition : partitionMap.values()) {
            innerStreams.put(partition.getElementId(), partition.getLocalStreamDefinitionMap());
        }
        return innerStreams;
    }

    public void addCallback(String streamId, StreamCallback streamCallback) {
        streamCallback.setStreamId(streamId);
        StreamJunction streamJunction = streamJunctionMap.get(streamId);
        if (streamJunction == null) {
            throw new DefinitionNotExistException("No stream found with name: " + streamId);
        }
        streamCallback.setStreamDefinition(streamDefinitionMap.get(streamId));
        streamCallback.setContext(siddhiAppContext);
        streamJunction.subscribe(streamCallback);
    }

    public void addCallback(String queryName, QueryCallback callback) {
        callback.setContext(siddhiAppContext);
        QueryRuntime queryRuntime = queryProcessorMap.get(queryName);
        if (queryRuntime == null) {
            throw new QueryNotExistException("No query found with name: " + queryName);
        }
        callback.setQuery(queryRuntime.getQuery());
        queryRuntime.addCallback(callback);
    }

    public Event[] query(String storeQuery) {
        return query(SiddhiCompiler.parseStoreQuery(storeQuery), storeQuery);
    }

    public Event[] query(StoreQuery storeQuery) {
        return query(storeQuery, null);
    }

    private Event[] query(StoreQuery storeQuery, String storeQueryString) {
        try {
            if (Level.DETAIL.compareTo(siddhiAppContext.getRootMetricsLevel()) <= 0 &&
                    storeQueryLatencyTracker != null) {
                storeQueryLatencyTracker.markIn();
            }
            StoreQueryRuntime storeQueryRuntime;
            synchronized (this) {
                storeQueryRuntime = storeQueryRuntimeMap.remove(storeQuery);
                if (storeQueryRuntime == null) {
                    storeQueryRuntime = StoreQueryParser.parse(storeQuery, siddhiAppContext, tableMap, windowMap,
                            aggregationMap);
                } else {
                    storeQueryRuntime.reset();
                }
                storeQueryRuntimeMap.put(storeQuery, storeQueryRuntime);
                if (storeQueryRuntimeMap.size() > 50) {
                    Iterator i = storeQueryRuntimeMap.entrySet().iterator();
                    if (i.hasNext()) {
                        i.next();
                        i.remove();
                    }
                }
            }
            return storeQueryRuntime.execute();
        } catch (RuntimeException e) {
            if (e instanceof SiddhiAppContextException) {
                throw new StoreQueryCreationException(((SiddhiAppContextException) e).getMessageWithOutContext(), e,
                        ((SiddhiAppContextException) e).getQueryContextStartIndex(),
                        ((SiddhiAppContextException) e).getQueryContextEndIndex(), null, storeQueryString);
            }
            throw new StoreQueryCreationException(e.getMessage(), e);
        } finally {
            if (Level.DETAIL.compareTo(siddhiAppContext.getRootMetricsLevel()) <= 0 &&
                    storeQueryLatencyTracker != null) {
                storeQueryLatencyTracker.markOut();
            }
        }
    }

    public Attribute[] getStoreQueryOutputAttributes(String storeQuery) {
        return getStoreQueryOutputAttributes(SiddhiCompiler.parseStoreQuery(storeQuery), storeQuery);
    }

    public Attribute[] getStoreQueryOutputAttributes(StoreQuery storeQuery) {
        return getStoreQueryOutputAttributes(storeQuery, null);
    }


    /**
     * This method get the storeQuery and return the corresponding output and its types.
     *
     * @param storeQuery       this storeQuery is processed and get the output attributes.
     * @param storeQueryString this passed to report errors with context if there are any.
     * @return List of output attributes
     */
    private Attribute[] getStoreQueryOutputAttributes(StoreQuery storeQuery, String storeQueryString) {
        try {
            StoreQueryRuntime storeQueryRuntime = storeQueryRuntimeMap.get(storeQuery);
            if (storeQueryRuntime == null) {
                storeQueryRuntime = StoreQueryParser.parse(storeQuery, siddhiAppContext, tableMap, windowMap,
                        aggregationMap);
                storeQueryRuntimeMap.put(storeQuery, storeQueryRuntime);
            }
            return storeQueryRuntime.getStoreQueryOutputAttributes();
        } catch (RuntimeException e) {
            if (e instanceof SiddhiAppContextException) {
                throw new StoreQueryCreationException(((SiddhiAppContextException) e).getMessageWithOutContext(), e,
                        ((SiddhiAppContextException) e).getQueryContextStartIndex(),
                        ((SiddhiAppContextException) e).getQueryContextEndIndex(), null, siddhiAppContext
                        .getSiddhiAppString());
            }
            throw new StoreQueryCreationException(e.getMessage(), e);
        }
    }

    public InputHandler getInputHandler(String streamId) {
        return inputManager.getInputHandler(streamId);
    }

    public Collection<List<Source>> getSources() {
        return sourceMap.values();
    }

    public Collection<List<Sink>> getSinks() {
        return sinkMap.values();
    }

    public Collection<Table> getTables() {
        return tableMap.values();
    }

    public synchronized void start() {
        if (running) {
            log.warn("Error calling start() for Siddhi App '" + siddhiAppContext.getName() + "', " +
                    "SiddhiApp already started.");
            return;
        }
        if (!runningWithoutSources) {
            startWithoutSources();
        }
        if (runningWithoutSources) {
            startSources();
        }
    }

    public synchronized void startWithoutSources() {
        if (running || runningWithoutSources) {
            log.warn("Error calling startWithoutSources() for Siddhi App '" + siddhiAppContext.getName() + "', " +
                    "SiddhiApp already started.");
        } else {
            try {
                if (siddhiAppContext.getRootMetricsLevel().compareTo(Level.OFF) != 0 &&
                        siddhiAppContext.getStatisticsManager() != null) {
                    siddhiAppContext.getStatisticsManager().startReporting();
                }
                for (EternalReferencedHolder eternalReferencedHolder : siddhiAppContext.getEternalReferencedHolders()) {
                    eternalReferencedHolder.start();
                }
                for (List<Sink> sinks : sinkMap.values()) {
                    for (Sink sink : sinks) {
                        sink.connectWithRetry();
                    }
                }
                for (Table table : tableMap.values()) {
                    table.connectWithRetry();
                }
                for (StreamJunction streamJunction : streamJunctionMap.values()) {
                    streamJunction.startProcessing();
                }
                if (incrementalDataPurging) {
                    for (AggregationRuntime aggregationRuntime : aggregationMap.values()) {
                        aggregationRuntime.startPurging();
                    }
                }
                runningWithoutSources = true;
            } catch (Throwable t) {
                log.error("Error starting Siddhi App '" + siddhiAppContext.getName() + "', " +
                        "triggering shutdown process. " + t.getMessage());
                try {
                    shutdown();
                } catch (Throwable t1) {
                    log.error("Error shutting down partially started Siddhi App '" + siddhiAppContext.getName() + "', "
                            + t1.getMessage());
                }
            }
        }
    }

    public void setPurgingEnabled(boolean purgingEnabled) {
        this.incrementalDataPurging = purgingEnabled;
    }

    public synchronized void startSources() {
        if (running) {
            log.warn("Error calling startSources() for Siddhi App '" + siddhiAppContext.getName() + "', " +
                    "SiddhiApp already started with the sources.");
            return;
        }
        if (!runningWithoutSources) {
            throw new SiddhiAppRuntimeException("Cannot call startSources() without calling startWithoutSources() " +
                    "for Siddhi App '" + siddhiAppContext.getName() + "'");
        } else {
            try {
                for (List<Source> sources : sourceMap.values()) {
                    for (Source source : sources) {
                        source.connectWithRetry();
                    }
                }
                running = true;
                runningWithoutSources = false;
            } catch (Throwable t) {
                log.error("Error starting Siddhi App '" + siddhiAppContext.getName() + "', " +
                        "triggering shutdown process. " + t.getMessage());
                try {
                    shutdown();
                } catch (Throwable t1) {
                    log.error("Error shutting down partially started Siddhi App '" + siddhiAppContext.getName() + "', "
                            + t1.getMessage());
                }
            }
        }
    }

    public synchronized void shutdown() {
        SourceHandlerManager sourceHandlerManager = siddhiAppContext.getSiddhiContext().getSourceHandlerManager();
        for (List<Source> sources : sourceMap.values()) {
            for (Source source : sources) {
                try {
                    if (sourceHandlerManager != null) {
                        sourceHandlerManager.unregisterSourceHandler(source.getMapper().getHandler().
                                getElementId());
                    }
                    source.shutdown();
                } catch (Throwable t) {
                    log.error(StringUtil.removeCRLFCharacters(ExceptionUtil.getMessageWithContext
                            (t, siddhiAppContext)) + " Error in shutting down source '" + StringUtil.
                            removeCRLFCharacters(source.getType()) + "' at '" +
                            StringUtil.removeCRLFCharacters(source.getStreamDefinition().getId()) +
                            "' on Siddhi App '" + siddhiAppContext.getName() + "'.", t);
                }
            }
        }

        for (Table table : tableMap.values()) {
            try {
                table.shutdown();
            } catch (Throwable t) {
                log.error(StringUtil.removeCRLFCharacters(ExceptionUtil.getMessageWithContext(t, siddhiAppContext)) +
                        " Error in shutting down table '" +
                        StringUtil.removeCRLFCharacters(table.getTableDefinition().getId()) + "' on Siddhi App '" +
                        StringUtil.removeCRLFCharacters(siddhiAppContext.getName()) + "'.", t);
            }
        }

        SinkHandlerManager sinkHandlerManager = siddhiAppContext.getSiddhiContext().getSinkHandlerManager();
        for (List<Sink> sinks : sinkMap.values()) {
            for (Sink sink : sinks) {
                try {
                    if (sinkHandlerManager != null && sink.getHandler() != null) {
                        sinkHandlerManager.unregisterSinkHandler(sink.getHandler().getElementId());
                    }
                    sink.shutdown();
                } catch (Throwable t) {
                    log.error(StringUtil.removeCRLFCharacters(ExceptionUtil.getMessageWithContext
                            (t, siddhiAppContext)) + " Error in shutting down sink '" + StringUtil.
                            removeCRLFCharacters(sink.getType()) + "' at '" + StringUtil.removeCRLFCharacters(sink.
                            getStreamDefinition().getId()) + "' on Siddhi App '" +
                            StringUtil.removeCRLFCharacters(siddhiAppContext.getName()) + "'.", t);
                }
            }
        }

        for (Table table : tableMap.values()) {
            RecordTableHandlerManager recordTableHandlerManager = siddhiAppContext.getSiddhiContext().
                    getRecordTableHandlerManager();
            if (recordTableHandlerManager != null) {
                String elementId = null;
                RecordTableHandler recordTableHandler = table.getHandler();
                if (recordTableHandler != null) {
                    elementId = recordTableHandler.getElementId();
                }
                if (elementId != null) {
                    recordTableHandlerManager.unregisterRecordTableHandler(elementId);
                }
            }
            table.shutdown();
        }

        for (EternalReferencedHolder eternalReferencedHolder : siddhiAppContext.getEternalReferencedHolders()) {
            try {
                eternalReferencedHolder.stop();
            } catch (Throwable t) {
                log.error(StringUtil.removeCRLFCharacters(ExceptionUtil.getMessageWithContext(t, siddhiAppContext)) +
                        " Error while stopping EternalReferencedHolder '" +
                        StringUtil.removeCRLFCharacters(eternalReferencedHolder.toString()) + "' down Siddhi app '" +
                        StringUtil.removeCRLFCharacters(siddhiAppContext.getName()) + "'.", t);
            }
        }
        inputManager.disconnect();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
                for (StreamJunction streamJunction : streamJunctionMap.values()) {
                    streamJunction.stopProcessing();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
                siddhiAppContext.getScheduledExecutorService().shutdownNow();
                siddhiAppContext.getExecutorService().shutdownNow();

            }
        }, "Siddhi-SiddhiApp-" + siddhiAppContext.getName() + "-Shutdown-Cleaner");
        thread.start();

        if (siddhiAppRuntimeMap != null) {
            siddhiAppRuntimeMap.remove(siddhiAppContext.getName());
        }

        if (siddhiAppContext.getStatisticsManager() != null) {
            if (siddhiAppContext.getRootMetricsLevel().compareTo(Level.OFF) != 0) {
                siddhiAppContext.getStatisticsManager().stopReporting();
            }
            siddhiAppContext.getStatisticsManager().cleanup();
        }
        running = false;
        runningWithoutSources = false;
    }

    public synchronized SiddhiDebugger debug() {
        siddhiDebugger = new SiddhiDebugger(siddhiAppContext);
        List<StreamRuntime> streamRuntime = new ArrayList<>();
        List<OutputCallback> streamCallbacks = new ArrayList<>();
        for (QueryRuntime queryRuntime : queryProcessorMap.values()) {
            streamRuntime.add(queryRuntime.getStreamRuntime());
            streamCallbacks.add(queryRuntime.getOutputCallback());
        }
        for (StreamRuntime streamRuntime1 : streamRuntime) {
            for (SingleStreamRuntime singleStreamRuntime : streamRuntime1.getSingleStreamRuntimes()) {
                singleStreamRuntime.getProcessStreamReceiver().setSiddhiDebugger(siddhiDebugger);
            }
        }
        for (OutputCallback callback : streamCallbacks) {
            callback.setSiddhiDebugger(siddhiDebugger);
        }
        start();
        return siddhiDebugger;
    }

    public PersistenceReference persist() {
        try {
            // first, pause all the event sources
            sourceMap.values().forEach(list -> list.forEach(Source::pause));
            // take snapshots of execution units
            if (siddhiAppContext.getSiddhiContext().getPersistenceStore() != null) {
                return PersistenceHelper.persist(siddhiAppContext.getSnapshotService().fullSnapshot(),
                        siddhiAppContext);
            } else {
                return PersistenceHelper.persist(siddhiAppContext.getSnapshotService().incrementalSnapshot(),
                        siddhiAppContext);
            }
        } finally {
            // at the end, resume the event sources
            sourceMap.values().forEach(list -> list.forEach(Source::resume));
        }
    }

    public byte[] snapshot() {
        try {
            // first, pause all the event sources
            sourceMap.values().forEach(list -> list.forEach(Source::pause));
            // take snapshots of execution units
            return siddhiAppContext.getSnapshotService().fullSnapshot();
        } finally {
            // at the end, resume the event sources
            sourceMap.values().forEach(list -> list.forEach(Source::resume));
        }
    }

    public void restore(byte[] snapshot) throws CannotRestoreSiddhiAppStateException {
        try {
            // first, pause all the event sources
            sourceMap.values().forEach(list -> list.forEach(Source::pause));
            // start the restoring process
            siddhiAppContext.getSnapshotService().restore(snapshot);
        } finally {
            // at the end, resume the event sources
            sourceMap.values().forEach(list -> list.forEach(Source::resume));
        }
    }

    public void restoreRevision(String revision) throws CannotRestoreSiddhiAppStateException {
        try {
            // first, pause all the event sources
            sourceMap.values().forEach(list -> list.forEach(Source::pause));
            // start the restoring process
            siddhiAppContext.getSnapshotService().restoreRevision(revision);
        } finally {
            // at the end, resume the event sources
            sourceMap.values().forEach(list -> list.forEach(Source::resume));
        }
    }

    public String restoreLastRevision() throws CannotRestoreSiddhiAppStateException {
        String revision;
        try {
            // first, pause all the event sources
            sourceMap.values().forEach(list -> list.forEach(Source::pause));
            // start the restoring process
            revision = siddhiAppContext.getSnapshotService().restoreLastRevision();
        } finally {
            // at the end, resume the event sources
            sourceMap.values().forEach(list -> list.forEach(Source::resume));
        }
        return revision;
    }

    public void clearAllRevisions() throws CannotClearSiddhiAppStateException {
        try {
            // first, pause all the event sources
            sourceMap.values().forEach(list -> list.forEach(Source::pause));
            // start the restoring process
            siddhiAppContext.getSnapshotService().clearAllRevisions();
        } finally {
            // at the end, resume the event sources
            sourceMap.values().forEach(list -> list.forEach(Source::resume));
        }
    }

    private void monitorQueryMemoryUsage() {
        memoryUsageTracker = siddhiAppContext
                .getSiddhiContext()
                .getStatisticsConfiguration()
                .getFactory()
                .createMemoryUsageTracker(siddhiAppContext.getStatisticsManager());
        for (Map.Entry<String, QueryRuntime> entry : queryProcessorMap.entrySet()) {
            QueryParserHelper.registerMemoryUsageTracking(entry.getKey(), entry.getValue(),
                    SiddhiConstants.METRIC_INFIX_QUERIES, siddhiAppContext, memoryUsageTracker);
        }
        for (PartitionRuntime partitionRuntime : partitionMap.values()) {
            partitionRuntime.setMemoryUsageTracker(memoryUsageTracker);
        }
        for (Map.Entry<String, Table> entry : tableMap.entrySet()) {
            QueryParserHelper.registerMemoryUsageTracking(entry.getKey(), entry.getValue(),
                    SiddhiConstants.METRIC_INFIX_TABLES, siddhiAppContext, memoryUsageTracker);
        }
        for (Map.Entry<String, Window> entry : windowMap.entrySet()) {
            QueryParserHelper.registerMemoryUsageTracking(entry.getKey(), entry.getValue(),
                    SiddhiConstants.METRIC_INFIX_WINDOWS, siddhiAppContext, memoryUsageTracker);
        }
        for (Map.Entry<String, AggregationRuntime> entry : aggregationMap.entrySet()) {
            QueryParserHelper.registerMemoryUsageTracking(entry.getKey(), entry.getValue(),
                    SiddhiConstants.METRIC_INFIX_AGGREGATIONS, siddhiAppContext, memoryUsageTracker);
        }
    }

    private void monitorBufferedEvents() {
        bufferedEventsTracker = siddhiAppContext
                .getSiddhiContext()
                .getStatisticsConfiguration()
                .getFactory()
                .createBufferSizeTracker(siddhiAppContext.getStatisticsManager());
        for (Map.Entry<String, StreamJunction> entry : streamJunctionMap.entrySet()) {
            registerForBufferedEvents(entry);
        }
        for (Map.Entry entry : partitionMap.entrySet()) {
            ConcurrentMap<String, StreamJunction> streamJunctionMap = ((PartitionRuntime) entry.getValue())
                    .getLocalStreamJunctionMap();
            for (Map.Entry<String, StreamJunction> streamJunctionEntry : streamJunctionMap.entrySet()) {
                registerForBufferedEvents(streamJunctionEntry);
            }
        }
    }

    private void registerForBufferedEvents(Map.Entry<String, StreamJunction> entry) {
        if (entry.getValue().containsBufferedEvents()) {
            String metricName = siddhiAppContext.getSiddhiContext().getStatisticsConfiguration().getMetricPrefix() +
                    SiddhiConstants.METRIC_DELIMITER + SiddhiConstants.METRIC_INFIX_SIDDHI_APPS +
                    SiddhiConstants.METRIC_DELIMITER + getName() + SiddhiConstants.METRIC_DELIMITER +
                    SiddhiConstants.METRIC_INFIX_SIDDHI + SiddhiConstants.METRIC_DELIMITER +
                    SiddhiConstants.METRIC_INFIX_STREAMS + SiddhiConstants.METRIC_DELIMITER +
                    entry.getKey() + SiddhiConstants.METRIC_DELIMITER + "size";
            boolean matchExist = false;
            for (String regex : siddhiAppContext.getIncludedMetrics()) {
                if (metricName.matches(regex)) {
                    matchExist = true;
                    break;
                }
            }
            if (matchExist) {
                bufferedEventsTracker.registerEventBufferHolder(entry.getValue(), metricName);
            }
        }
    }

    public void handleExceptionWith(ExceptionHandler<Object> exceptionHandler) {
        siddhiAppContext.setDisruptorExceptionHandler(exceptionHandler);
    }

    public void handleRuntimeExceptionWith(ExceptionListener exceptionListener) {
        siddhiAppContext.setRuntimeExceptionListener(exceptionListener);
    }

    /**
     * Method to check the Siddhi App statistics level enabled.
     *
     * @return Level value of Siddhi App statistics state
     */
    public Level getRootMetricsLevel() {
        return siddhiAppContext.getRootMetricsLevel();
    }

    /**
     * To enable, disable and change Siddhi App statistics level on runtime.
     *
     * @param level whether statistics is enabled or not
     */
    public void enableStats(Level level) {
        if (running && siddhiAppContext.getStatisticsManager() != null) {

            if (siddhiAppContext.getRootMetricsLevel().compareTo(level) == 0) {
                if (level == Level.OFF) {
                    log.debug("Siddhi App '" + getName() + "' statistics reporting is already disabled!");
                } else if (level == Level.BASIC || level == Level.DETAIL) {
                    log.debug("Siddhi App '" + getName() + "' statistics reporting is already in " + level + " level!");
                }
                log.info("Siddhi App '" + getName() + "' statistics reporting didnt changed. ");
            } else {
                if (level == Level.OFF) {
                    siddhiAppContext.setRootMetricsLevel(Level.OFF);
                    siddhiAppContext.getStatisticsManager().stopReporting();
                    log.info("Siddhi App '" + getName() + "' statistics reporting stopped!");
                } else {
                    if (siddhiAppContext.getRootMetricsLevel().compareTo(Level.OFF) == 0) {
                        siddhiAppContext.getStatisticsManager().startReporting();
                        log.debug("Siddhi App '" + getName() + "' statistics reporting started!");
                    }
                    siddhiAppContext.setRootMetricsLevel(level);
                    log.info("Siddhi App '" + getName() + "' statistics reporting changed to: " + level.toString());
                }
            }
        } else {
            log.info("Siddhi App '" + getName() + "' statistics reporting not changed!");
        }
    }

    /**
     * To enable and disable Siddhi App playback mode on runtime along with optional parameters.
     *
     * @param playBackEnabled         whether playback is enabled or not
     * @param idleTime
     * @param incrementInMilliseconds
     */
    public void enablePlayBack(boolean playBackEnabled, Long idleTime, Long incrementInMilliseconds) {
        this.siddhiAppContext.setPlayback(playBackEnabled);
        if (!playBackEnabled) {
            for (Scheduler scheduler : siddhiAppContext.getSchedulerList()) {
                scheduler.switchToLiveMode();
            }
        } else {
            if (idleTime != null && incrementInMilliseconds != null) {
                //Only use if both values are present. Else defaults will be used which got assigned when creating
                // the siddhi app runtimes if app contained playback information
                this.siddhiAppContext.getTimestampGenerator().setIdleTime(idleTime);
                this.siddhiAppContext.getTimestampGenerator().setIncrementInMilliseconds(incrementInMilliseconds);
            }

            for (Scheduler scheduler : siddhiAppContext.getSchedulerList()) {
                scheduler.switchToPlayBackMode();
            }
        }
    }
}
