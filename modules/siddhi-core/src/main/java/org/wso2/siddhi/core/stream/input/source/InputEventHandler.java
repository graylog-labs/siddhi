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

package org.wso2.siddhi.core.stream.input.source;

import org.apache.log4j.Logger;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.util.ExceptionUtil;
import org.wso2.siddhi.core.util.statistics.LatencyTracker;
import org.wso2.siddhi.core.util.statistics.metrics.Level;
import org.wso2.siddhi.core.util.timestamp.TimestampGenerator;

import java.util.List;

/**
 * This class wraps {@link InputHandler} class in order to guarantee exactly once processing
 */
public class InputEventHandler {

    private static final Logger LOG = Logger.getLogger(InputEventHandler.class);

    private final ThreadLocal<String[]> trpProperties;
    private final TimestampGenerator timestampGenerator;
    private ThreadLocal<String[]> trpSyncProperties;
    private String sourceType;
    private LatencyTracker latencyTracker;
    private SiddhiAppContext siddhiAppContext;
    private InputHandler inputHandler;
    private List<AttributeMapping> transportMapping;
    private InputEventHandlerCallback inputEventHandlerCallback;

    InputEventHandler(InputHandler inputHandler, List<AttributeMapping> transportMapping,
                      ThreadLocal<String[]> trpProperties, ThreadLocal<String[]> trpSyncProperties, String sourceType,
                      LatencyTracker latencyTracker, SiddhiAppContext siddhiAppContext,
                      InputEventHandlerCallback inputEventHandlerCallback) {
        this.inputHandler = inputHandler;
        this.transportMapping = transportMapping;
        this.trpProperties = trpProperties;
        this.trpSyncProperties = trpSyncProperties;
        this.sourceType = sourceType;
        this.latencyTracker = latencyTracker;
        this.siddhiAppContext = siddhiAppContext;
        this.inputEventHandlerCallback = inputEventHandlerCallback;
        this.timestampGenerator = siddhiAppContext.getTimestampGenerator();
    }

    public void sendEvent(Event event) throws InterruptedException {
        try {
            if (latencyTracker != null && Level.DETAIL.compareTo(siddhiAppContext.getRootMetricsLevel()) <= 0) {
                latencyTracker.markOut();
            }
            String[] transportProperties = trpProperties.get();
            trpProperties.remove();
            String[] transportSyncProperties = trpSyncProperties.get();
            trpSyncProperties.remove();
            if (event.getTimestamp() == -1) {
                long currentTimestamp = timestampGenerator.currentTime();
                event.setTimestamp(currentTimestamp);
            }
            for (int i = 0; i < transportMapping.size(); i++) {
                AttributeMapping attributeMapping = transportMapping.get(i);
                event.getData()[attributeMapping.getPosition()] = transportProperties[i];
            }
            inputEventHandlerCallback.sendEvent(event, transportSyncProperties);
        } catch (RuntimeException e) {
            LOG.error(ExceptionUtil.getMessageWithContext(e, siddhiAppContext) +
                    " Error in applying transport property mapping for '" + sourceType
                    + "' source at '" + inputHandler.getStreamId() + "' stream.", e);
        } finally {
            trpProperties.remove();
            trpSyncProperties.remove();
        }
    }

    public void sendEvents(Event[] events) throws InterruptedException {
        try {
            if (latencyTracker != null && Level.DETAIL.compareTo(siddhiAppContext.getRootMetricsLevel()) <= 0) {
                latencyTracker.markOut();
            }
            String[] transportProperties = trpProperties.get();
            trpProperties.remove();
            String[] transportSyncProperties = trpSyncProperties.get();
            trpSyncProperties.remove();
            long currentTimestamp = timestampGenerator.currentTime();
            for (Event event : events) {
                if (event.getTimestamp() == -1) {
                    event.setTimestamp(currentTimestamp);
                }
                for (int i = 0; i < transportMapping.size(); i++) {
                    AttributeMapping attributeMapping = transportMapping.get(i);
                    event.getData()[attributeMapping.getPosition()] = transportProperties[i];
                }
            }
            inputEventHandlerCallback.sendEvents(events, transportSyncProperties);
        } catch (RuntimeException e) {
            LOG.error(ExceptionUtil.getMessageWithContext(e, siddhiAppContext) +
                    " Error in applying transport property mapping for '" + sourceType
                    + "' source at '" + inputHandler.getStreamId() + "' stream.", e);
        } finally {
            trpProperties.remove();
            trpSyncProperties.remove();
        }
    }
}
