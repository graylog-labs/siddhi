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

package org.wso2.siddhi.core.util.statistics;

/**
 * This interface will have the necessary methods to calculate the events buffered.
 */
public interface BufferedEventsTracker {
    /**
     * Register the EventBufferHolder that needs to be measured the used capacity usage
     *
     * @param eventBufferHolder EventBufferHolder
     * @param name              An unique value to identify the object.
     */
    void registerEventBufferHolder(EventBufferHolder eventBufferHolder, String name);

    void enableEventBufferHolderMetrics();

    void disableEventBufferHolderMetrics();

    /**
     * @param eventBufferHolder Event Buffer holder
     * @return Name of the buffered event tracker.
     */
    String getName(EventBufferHolder eventBufferHolder);
}
