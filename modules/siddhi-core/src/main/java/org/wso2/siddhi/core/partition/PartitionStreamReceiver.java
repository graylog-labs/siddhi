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
package org.wso2.siddhi.core.partition;

import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.event.ComplexEvent;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.event.stream.MetaStreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventPool;
import org.wso2.siddhi.core.event.stream.converter.StreamEventConverter;
import org.wso2.siddhi.core.event.stream.converter.StreamEventConverterFactory;
import org.wso2.siddhi.core.partition.executor.PartitionExecutor;
import org.wso2.siddhi.core.query.QueryRuntime;
import org.wso2.siddhi.core.query.input.stream.StreamRuntime;
import org.wso2.siddhi.core.stream.StreamJunction;
import org.wso2.siddhi.query.api.definition.StreamDefinition;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Specific {@link StreamJunction.Receiver} implementation to pump events into partitions. This will send the event
 * to the matching partition.
 */
public class PartitionStreamReceiver implements StreamJunction.Receiver {

    private final StreamEventPool eventPool;
    private StreamEventConverter streamEventConverter;
    private String streamId;
    private MetaStreamEvent metaStreamEvent;
    private StreamDefinition streamDefinition;
    private SiddhiAppContext siddhiAppContext;
    private PartitionRuntime partitionRuntime;
    private List<PartitionExecutor> partitionExecutors;
    private Map<String, StreamJunction> cachedStreamJunctionMap = new ConcurrentHashMap<String, StreamJunction>();


    public PartitionStreamReceiver(SiddhiAppContext siddhiAppContext, MetaStreamEvent metaStreamEvent,
                                   StreamDefinition streamDefinition,
                                   List<PartitionExecutor> partitionExecutors,
                                   PartitionRuntime partitionRuntime) {
        this.metaStreamEvent = metaStreamEvent;
        this.streamDefinition = streamDefinition;
        this.partitionRuntime = partitionRuntime;
        this.partitionExecutors = partitionExecutors;
        this.siddhiAppContext = siddhiAppContext;
        this.streamId = streamDefinition.getId();
        this.eventPool = new StreamEventPool(metaStreamEvent, 5);

    }

    public void init() {
        streamEventConverter = StreamEventConverterFactory.constructEventConverter(metaStreamEvent);
    }

    @Override
    public String getStreamId() {
        return streamId;
    }


    @Override
    public void receive(ComplexEvent complexEvent) {

        if (partitionExecutors.size() == 0) {
            ComplexEventChunk<ComplexEvent> outputEventChunk = new ComplexEventChunk<ComplexEvent>(false);
            ComplexEvent aComplexEvent = complexEvent;
            while (aComplexEvent != null) {
                StreamEvent borrowedEvent = borrowEvent();
                streamEventConverter.convertComplexEvent(aComplexEvent, borrowedEvent);
                outputEventChunk.add(borrowedEvent);
                aComplexEvent = aComplexEvent.getNext();
            }
            send(outputEventChunk.getFirst());
        } else {
            if (complexEvent.getNext() == null) {
                for (PartitionExecutor partitionExecutor : partitionExecutors) {
                    StreamEvent borrowedEvent = borrowEvent();
                    streamEventConverter.convertComplexEvent(complexEvent, borrowedEvent);
                    String key = partitionExecutor.execute(borrowedEvent);
                    send(key, borrowedEvent);
                }
            } else {
                ComplexEventChunk<ComplexEvent> complexEventChunk = new ComplexEventChunk<ComplexEvent>(false);
                complexEventChunk.add(complexEvent);
                ComplexEventChunk<ComplexEvent> outputEventChunk = new ComplexEventChunk<ComplexEvent>(false);
                String currentKey = null;
                while (complexEventChunk.hasNext()) {
                    ComplexEvent aEvent = complexEventChunk.next();
                    complexEventChunk.remove();
                    StreamEvent borrowedEvent = borrowEvent();
                    streamEventConverter.convertComplexEvent(aEvent, borrowedEvent);
                    boolean currentEventMatchedPrevPartitionExecutor = false;
                    for (PartitionExecutor partitionExecutor : partitionExecutors) {
                        String key = partitionExecutor.execute(borrowedEvent);
                        if (key != null) {
                            if (currentKey == null) {
                                currentKey = key;
                            } else if (!currentKey.equals(key)) {
                                if (!currentEventMatchedPrevPartitionExecutor) {
                                    ComplexEvent firstEvent = outputEventChunk.getFirst();
                                    send(currentKey, firstEvent);
                                    currentKey = key;
                                    outputEventChunk.clear();
                                } else {
                                    ComplexEvent firstEvent = outputEventChunk.getFirst();
                                    send(currentKey, firstEvent);
                                    currentKey = key;
                                    outputEventChunk.clear();
                                    StreamEvent cloneEvent = borrowEvent();
                                    streamEventConverter.convertComplexEvent(aEvent, cloneEvent);
                                    outputEventChunk.add(cloneEvent);
                                }
                            }
                            if (!currentEventMatchedPrevPartitionExecutor) {
                                outputEventChunk.add(borrowedEvent);
                            }
                            currentEventMatchedPrevPartitionExecutor = true;
                        }
                    }
                }
                send(currentKey, outputEventChunk.getFirst());
                outputEventChunk.clear();
            }
        }

    }

    @Override
    public void receive(Event event) {
        StreamEvent borrowedEvent = borrowEvent();
        streamEventConverter.convertEvent(event, borrowedEvent);
        for (PartitionExecutor partitionExecutor : partitionExecutors) {
            String key = partitionExecutor.execute(borrowedEvent);
            send(key, borrowedEvent);
        }
        if (partitionExecutors.size() == 0) {
            send(borrowedEvent);
        }
        returnEvents(borrowedEvent);
    }

    @Override
    public void receive(long timestamp, Object[] data) {
        StreamEvent borrowedEvent = borrowEvent();
        streamEventConverter.convertData(timestamp, data, borrowedEvent);
        if (partitionExecutors.size() == 0) {
            send(borrowedEvent);
        } else {
            for (PartitionExecutor partitionExecutor : partitionExecutors) {
                String key = partitionExecutor.execute(borrowedEvent);
                send(key, borrowedEvent);
            }
        }
        returnEvents(borrowedEvent);
    }

    @Override
    public void receive(Event[] events) {
        if (partitionExecutors.size() == 0) {
            StreamEvent currentEvent;
            StreamEvent firstEvent = borrowEvent();
            streamEventConverter.convertEvent(events[0], firstEvent);
            currentEvent = firstEvent;
            for (int i = 1; i < events.length; i++) {
                StreamEvent nextEvent = borrowEvent();
                streamEventConverter.convertEvent(events[i], nextEvent);
                currentEvent.setNext(nextEvent);
                currentEvent = nextEvent;
            }
            send(firstEvent);
            returnEvents(firstEvent);

        } else {
            String key = null;
            StreamEvent firstEvent = null;
            StreamEvent currentEvent = null;
            for (Event event : events) {
                StreamEvent nextEvent = borrowEvent();
                streamEventConverter.convertEvent(event, nextEvent);
                for (PartitionExecutor partitionExecutor : partitionExecutors) {
                    String currentKey = partitionExecutor.execute(nextEvent);
                    if (currentKey != null) {
                        if (key == null) {
                            key = currentKey;
                            firstEvent = nextEvent;
                        } else if (!currentKey.equals(key)) {
                            send(key, firstEvent);
                            returnEvents(firstEvent);
                            key = currentKey;
                            firstEvent = nextEvent;
                        } else {
                            currentEvent.setNext(nextEvent);
                        }
                        currentEvent = nextEvent;
                    }
                }
            }
            send(key, firstEvent);
            returnEvents(firstEvent);
        }

    }

    @Override
    public void receive(List<Event> events) {
        if (partitionExecutors.size() == 0) {
            StreamEvent firstEvent = null;
            StreamEvent currentEvent = null;
            for (Event event : events) {
                StreamEvent nextEvent = borrowEvent();
                streamEventConverter.convertEvent(event, nextEvent);
                if (firstEvent == null) {
                    firstEvent = nextEvent;
                } else {
                    currentEvent.setNext(nextEvent);
                }
                currentEvent = nextEvent;
            }
            send(firstEvent);
            returnEvents(firstEvent);
        } else {
            String key = null;
            StreamEvent firstEvent = null;
            StreamEvent currentEvent = null;
            for (Event event : events) {
                StreamEvent nextEvent = borrowEvent();
                streamEventConverter.convertEvent(event, nextEvent);
                for (PartitionExecutor partitionExecutor : partitionExecutors) {
                    String currentKey = partitionExecutor.execute(nextEvent);
                    if (currentKey != null) {
                        if (key == null) {
                            key = currentKey;
                            firstEvent = nextEvent;
                        } else if (!currentKey.equals(key)) {
                            send(key, firstEvent);
                            returnEvents(firstEvent);
                            key = currentKey;
                            firstEvent = nextEvent;
                        } else {
                            currentEvent.setNext(nextEvent);
                        }
                        currentEvent = nextEvent;
                    }
                }
            }
            send(key, firstEvent);
            returnEvents(firstEvent);
        }
    }

    private void send(String key, ComplexEvent event) {
        if (key != null) {
            partitionRuntime.cloneIfNotExist(key);
            cachedStreamJunctionMap.get(streamId + key).sendEvent(event);
        }
    }

    private void send(ComplexEvent event) {
        for (StreamJunction streamJunction : cachedStreamJunctionMap.values()) {
            streamJunction.sendEvent(event);
        }
    }

    /**
     * create local streamJunctions through which events received by partitionStreamReceiver, are sent to
     * queryStreamReceivers
     *
     * @param key              partitioning key
     * @param queryRuntimeList queryRuntime list of the partition
     */
    public void addStreamJunction(String key, List<QueryRuntime> queryRuntimeList) {
        StreamJunction streamJunction = cachedStreamJunctionMap.get(streamId + key);
        if (streamJunction == null) {
            streamJunction = partitionRuntime.getLocalStreamJunctionMap().get(streamId + key);
            if (streamJunction == null) {
                streamJunction = createStreamJunction();
                partitionRuntime.addStreamJunction(streamId + key, streamJunction);
            }
            cachedStreamJunctionMap.put(streamId + key, streamJunction);
        }
        for (QueryRuntime queryRuntime : queryRuntimeList) {
            StreamRuntime streamRuntime = queryRuntime.getStreamRuntime();
            for (int i = 0; i < queryRuntime.getInputStreamId().size(); i++) {
                if ((streamRuntime.getSingleStreamRuntimes().get(i)).
                        getProcessStreamReceiver().getStreamId().equals(streamId + key)) {
                    streamJunction.subscribe((streamRuntime.getSingleStreamRuntimes().get(i))
                            .getProcessStreamReceiver());
                }
            }
        }
    }

    private StreamJunction createStreamJunction() {
        return new StreamJunction(streamDefinition, siddhiAppContext.getExecutorThreadFactory(),
                siddhiAppContext.getBufferSize(), null, siddhiAppContext);
    }

    private synchronized StreamEvent borrowEvent() {
        return eventPool.borrowEvent();
    }

    private synchronized void returnEvents(StreamEvent events) {
        eventPool.returnEvents(events);
    }
}
