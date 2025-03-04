/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.testng.annotations.Test;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.exception.StoreQueryCreationException;
import org.wso2.siddhi.query.api.exception.SiddhiAppValidationException;

public class AggregationFindQueryValidationTestCase {

    private static final Logger LOG = Logger.getLogger(AggregationFindQueryValidationTestCase.class);

    @Test(expectedExceptions = StoreQueryCreationException.class)
    public void storeQueryValidationTestCase1() {
        LOG.info("storeQueryValidationTestCase1");
        SiddhiManager siddhiManager = new SiddhiManager();
        String stockStream = "" +
                " define stream stockStream (arrival long, symbol string, price float, volume int); ";

        String query = "" +
                " @info(name = 'query3') " +
                " define aggregation stockAggregation " +
                " from stockStream " +
                " select sum(price) as sumPrice " +
                " group by price " +
                " aggregate every sec, min, hour, day";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);
        try {
            siddhiAppRuntime.start();
            siddhiAppRuntime.query("" +
                    "from stockAggregation " +
                    "within '2018-**-** **:**:**' " +
                    "per 'test' " +
                    "select *");
        } finally {
            siddhiAppRuntime.shutdown();
        }
    }

    @Test(expectedExceptions = SiddhiAppValidationException.class)
    public void storeQueryValidationTestCase2() {
        LOG.info("storeQueryValidationTestCase2");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream = "" +
                " define stream stockStream (arrival long, symbol string, price float, volume int); ";

        String triggerStream = "" +
                " define stream triggerStream (arrival int); ";

        String query = "" +
                " @info(name = 'query3') " +
                " define aggregation stockAggregation " +
                " from stockStream " +
                " select sum(price) as sumPrice " +
                " group by price " +
                " aggregate every sec, min, hour, day; " +
                "" +
                "from triggerStream join stockAggregation " +
                "within '2018-**-** **:**:**' " +
                "per 'test' " +
                "select * " +
                "insert into ignore;";

        siddhiManager.createSiddhiAppRuntime(stockStream + triggerStream + query);
    }

    @Test
    public void storeQueryValidationTestCase3() {
        LOG.info("storeQueryValidationTestCase3");
        SiddhiManager siddhiManager = new SiddhiManager();
        String stockStream = "" +
                " define stream stockStream (arrival long, symbol string, price float, volume int); ";

        String query = "" +
                " @info(name = 'query3') " +
                " define aggregation stockAggregation " +
                " from stockStream " +
                " select sum(price) as sumPrice " +
                " group by price " +
                " aggregate every sec, min, hour, day";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);
        try {
            siddhiAppRuntime.start();
            siddhiAppRuntime.query("" +
                    "from stockAggregation " +
                    "within '2018-**-** **:**:**' " +
                    "per 'sec' " +
                    "select *");
            siddhiAppRuntime.query("" +
                    "from stockAggregation " +
                    "within '2018-**-** **:**:**' " +
                    "per 'second' " +
                    "select *");
            siddhiAppRuntime.query("" +
                    "from stockAggregation " +
                    "within '2018-**-** **:**:**' " +
                    "per 'seconds' " +
                    "select *");
        } finally {
            siddhiAppRuntime.shutdown();
        }
    }
}
