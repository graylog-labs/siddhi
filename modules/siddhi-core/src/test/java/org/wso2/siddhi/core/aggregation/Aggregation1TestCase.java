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
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.UnitTestAppender;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.exception.SiddhiAppCreationException;
import org.wso2.siddhi.core.exception.StoreQueryCreationException;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.StreamJunction;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.util.EventPrinter;
import org.wso2.siddhi.core.util.SiddhiTestHelper;
import org.wso2.siddhi.query.compiler.exception.SiddhiParserException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Aggregation1TestCase {

    private static final Logger LOG = Logger.getLogger(Aggregation1TestCase.class);
    private AtomicInteger inEventCount;
    private AtomicInteger removeEventCount;
    private boolean eventArrived;
    private List<Object[]> inEventsList;
    private List<Object[]> removeEventsList;

    @BeforeMethod
    public void init() {
        inEventCount = new AtomicInteger(0);
        removeEventCount = new AtomicInteger(0);
        eventArrived = false;
        inEventsList = new ArrayList<>();
        removeEventsList = new ArrayList<>();
    }

    @Test
    public void incrementalStreamProcessorTest1() {
        LOG.info("incrementalStreamProcessorTest1");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream = "" +
                " define stream stockStream (arrival long, symbol string, price float, volume int); ";

        String query = "" +
                " @info(name = 'query1') " +
                " define aggregation stockAggregation " +
                " from stockStream " +
                " select sum(price) as sumPrice " +
                " aggregate by arrival every sec ... min";

        siddhiManager.createSiddhiAppRuntime(stockStream + query);
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest1"})
    public void incrementalStreamProcessorTest2() {
        LOG.info("incrementalStreamProcessorTest2");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream = "" +
                " define stream stockStream (arrival long, symbol string, price float, volume int); ";

        String query = "" +
                " @info(name = 'query2') " +
                " define aggregation stockAggregation " +
                " from stockStream " +
                " select sum(price) as sumPrice " +
                " aggregate every sec ... min";

        siddhiManager.createSiddhiAppRuntime(stockStream + query);
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest2"})
    public void incrementalStreamProcessorTest3() {
        LOG.info("incrementalStreamProcessorTest3");
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

        siddhiManager.createSiddhiAppRuntime(stockStream + query);
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest3"})
    public void incrementalStreamProcessorTest4() {
        LOG.info("incrementalStreamProcessorTest4");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream = "" +
                " define stream stockStream (arrival long, symbol string, price float, volume int); ";

        String query = "" +
                " @info(name = 'query3') " +
                " define aggregation stockAggregation " +
                " from stockStream " +
                " select sum(price) as sumPrice " +
                " group by price, volume " +
                " aggregate every sec, min, hour, day";

        siddhiManager.createSiddhiAppRuntime(stockStream + query);
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest4"})
    public void incrementalStreamProcessorTest5() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest5");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = " define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...hour ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
        siddhiAppRuntime.start();

        stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
        stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

        stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289952000L});
        stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289952000L});

        stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289954000L});
        stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289954000L});
        Thread.sleep(2000);

        Event[] events = siddhiAppRuntime.query("from stockAggregation " +
                "within \"2017-06-** **:**:**\" " +
                "per \"seconds\"");
        EventPrinter.print(events);

        Assert.assertNotNull(events, "Queried results cannot be null.");
        AssertJUnit.assertEquals(3, events.length);

        List<Object[]> eventsOutputList = new ArrayList<>();
        for (Event event : events) {
            eventsOutputList.add(event.getData());
        }
        List<Object[]> expected = Arrays.asList(
                new Object[]{1496289952000L, "WSO2", 80.0, 160.0, 1600f},
                new Object[]{1496289950000L, "WSO2", 60.0, 120.0, 700f},
                new Object[]{1496289954000L, "IBM", 100.0, 200.0, 9600f}
        );
        AssertJUnit.assertTrue("In events matched", SiddhiTestHelper.isUnsortedEventsMatch(eventsOutputList, expected));
        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest5"})
    public void incrementalStreamProcessorTest6() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest6");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = "" +
                "define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...year  ; " +

                "define stream inputStream (symbol string, value int, startTime string, endTime string, " +
                "perValue string); " +

                "@info(name = 'query1') " +
                "from inputStream as i join stockAggregation as s " +
                "within i.startTime, i.endTime " +
                "per i.perValue " +
                "select AGG_TIMESTAMP, s.symbol, avgPrice, totalPrice as sumPrice, lastTradeValue  " +
                "order by AGG_TIMESTAMP " +
                "insert all events into outputStream; ";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        try {
            siddhiAppRuntime.addCallback("query1", new QueryCallback() {
                @Override
                public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                    if (inEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : inEvents) {
                            inEventsList.add(event.getData());
                            inEventCount.incrementAndGet();
                        }
                        eventArrived = true;
                    }
                    if (removeEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : removeEvents) {
                            removeEventsList.add(event.getData());
                            removeEventCount.incrementAndGet();
                        }
                    }
                    eventArrived = true;
                }
            });

            InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
            InputHandler inputStreamInputHandler = siddhiAppRuntime.getInputHandler("inputStream");
            siddhiAppRuntime.start();

            // Thursday, June 1, 2017 4:05:50 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

            // Thursday, June 1, 2017 4:05:51 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289951000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289951000L});

            // Thursday, June 1, 2017 4:05:52 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 900f, null, 200L, 60, 1496289952000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 500f, null, 200L, 7, 1496289952000L});

            // Thursday, June 1, 2017 4:05:53 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289953000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289953000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 400f, null, 200L, 9, 1496289953000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 140f, null, 200L, 11, 1496289953000L});

            // Thursday, June 1, 2017 4:05:54 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 600f, null, 200L, 6, 1496289954000L});

            // Thursday, June 1, 2017 4:06:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 1000f, null, 200L, 9, 1496290016000L});

            Thread.sleep(5000);
            inputStreamInputHandler.send(new Object[]{"IBM", 1, "2017-06-01 04:05:50",
                    "2017-06-01 04:06:57", "seconds"});
            Thread.sleep(100);

            List<Object[]> expected = Arrays.asList(
                    new Object[]{1496289950000L, "WSO2", 60.0, 240.0, 700f},
                    new Object[]{1496289951000L, "IBM", 100.0, 200.0, 9600f},
                    new Object[]{1496289952000L, "IBM", 700.0, 1400.0, 3500f},
                    new Object[]{1496289953000L, "WSO2", 100.0, 300.0, 1540f},
                    new Object[]{1496289953000L, "IBM", 400.0, 400.0, 3600f},
                    new Object[]{1496289954000L, "IBM", 600.0, 600.0, 3600f},
                    new Object[]{1496290016000L, "IBM", 1000.0, 1000.0, 9000f}
            );
            SiddhiTestHelper.waitForEvents(100, 7, inEventCount, 60000);
            AssertJUnit.assertTrue("Event arrived", eventArrived);
            AssertJUnit.assertEquals("Number of success events", 7, inEventCount.get());
            AssertJUnit.assertTrue("In events matched", SiddhiTestHelper.isEventsMatch(inEventsList, expected));
        } finally {
            siddhiAppRuntime.shutdown();
        }
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest6"})
    public void incrementalStreamProcessorTest9() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest9");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query =
                "define aggregation stockAggregation " +
                "from stockStream " +
                "select avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) as lastTradeValue, " +
                "count() as count " +
                "aggregate by timestamp every min, day, year ;" +

                "define stream inputStream (symbol string, value int, startTime string, " +
                "endTime string, perValue string); " +

                "@info(name = 'query1') " +
                "from inputStream as i join stockAggregation as s " +
                "within 1496200000000L, 1596434876000L " +
                "per \"days\" " +
                "select AGG_TIMESTAMP, s.avgPrice, totalPrice, lastTradeValue, count " +
                "order by AGG_TIMESTAMP " +
                "insert all events into outputStream; ";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        try {
            siddhiAppRuntime.addCallback("query1", new QueryCallback() {
                @Override
                public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                    if (inEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : inEvents) {
                            inEventsList.add(event.getData());
                            inEventCount.incrementAndGet();
                        }
                        eventArrived = true;
                    }
                    if (removeEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : removeEvents) {
                            removeEventsList.add(event.getData());
                            removeEventCount.incrementAndGet();
                        }
                    }
                    eventArrived = true;
                }
            });
            InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
            InputHandler inputStreamInputHandler = siddhiAppRuntime.getInputHandler("inputStream");
            siddhiAppRuntime.start();

            // Thursday, June 1, 2017 4:05:50 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

            // Thursday, June 1, 2017 4:05:52 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289952000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289952000L});

            // Thursday, June 1, 2017 4:05:54 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289954000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289954000L});

            // Thursday, June 1, 2017 4:05:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 900f, null, 200L, 60, 1496289956000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 500f, null, 200L, 7, 1496289956000L});

            // Thursday, June 1, 2017 4:06:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 400f, null, 200L, 9, 1496290016000L});

            // Thursday, June 1, 2017 4:07:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 600f, null, 200L, 6, 1496290076000L});

            // Thursday, June 1, 2017 5:07:56 AM
            stockStreamInputHandler.send(new Object[]{"CISCO", 700f, null, 200L, 20, 1496293676000L});

            // Thursday, June 1, 2017 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496297276000L});

            // Friday, June 2, 2017 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"CISCO", 800f, null, 100L, 10, 1496383676000L});

            // Saturday, June 3, 2017 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"CISCO", 900f, null, 100L, 15, 1496470076000L});

            // Monday, July 3, 2017 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1499062076000L});

            // Thursday, August 3, 2017 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 400f, null, 200L, 9, 1501740476000L});

            // Friday, August 3, 2018 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 6, 1533276476000L});

            // Saturday, August 3, 2019 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 260f, 44f, 200L, 16, 1564812476000L});

            // Monday, August 3, 2020 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"CISCO", 260f, 44f, 200L, 16, 1596434876000L});

            Thread.sleep(100);

            inputStreamInputHandler.send(new Object[]{"IBM", 1, "2017-06-01 09:35:51 +05:30",
                    "2017-06-01 09:35:52 +05:30", "seconds"});
            Thread.sleep(100);

            List<Object[]> expected = Arrays.asList(
                    new Object[]{1496275200000L, 303.3333333333333, 3640.0, 3360f, 12L},
                    new Object[]{1496361600000L, 800.0, 800.0, 8000f, 1L},
                    new Object[]{1496448000000L, 900.0, 900.0, 13500f, 1L},
                    new Object[]{1499040000000L, 100.0, 100.0, 9600f, 1L},
                    new Object[]{1501718400000L, 400.0, 400.0, 3600f, 1L},
                    new Object[]{1533254400000L, 60.0, 60.0, 360f, 1L},
                    new Object[]{1564790400000L, 260.0, 260.0, 4160f, 1L},
                    new Object[]{1596412800000L, 260.0, 260.0, 4160f, 1L}
            );
            SiddhiTestHelper.waitForEvents(100, 8, inEventCount, 60000);

            AssertJUnit.assertTrue("Event arrived", eventArrived);
            AssertJUnit.assertEquals("Number of success events", 8, inEventCount.get());
            AssertJUnit.assertTrue("In events matched", SiddhiTestHelper.isEventsMatch(inEventsList, expected));

        } finally {
            siddhiAppRuntime.shutdown();
        }
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest9"})
    public void incrementalStreamProcessorTest12() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest12");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = "" +
                "define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, " +
                "(price * quantity) as lastTradeValue, max(price) as maxPrice, min(price) as minPrice " +
                "group by symbol " +
                "aggregate by timestamp every sec...year  ; " +

                "define stream inputStream (symbol string, value int, startTime string, " +
                "endTime string, perValue string); " +

                "from inputStream as i join stockAggregation as s " +
                "within \"2017-06-01 04:05:50\", \"2017-06-01 04:06:57\" " +
                "per \"seconds\" " +
                "select AGG_TIMESTAMP, totalPrice, avgPrice, lastTradeValue, s.symbol, maxPrice, minPrice " +
                "order by AGG_TIMESTAMP " +
                "insert into tempStream; " +

                "@info(name = 'query1') " +
                "from tempStream " +
                "select totalPrice, avgPrice, lastTradeValue, symbol, maxPrice, minPrice " +
                "insert into outputStream ";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        try {
            siddhiAppRuntime.addCallback("query1", new QueryCallback() {
                @Override
                public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                    if (inEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : inEvents) {
                            inEventsList.add(event.getData());
                            inEventCount.incrementAndGet();
                        }
                        eventArrived = true;
                    }
                    if (removeEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : removeEvents) {
                            removeEventsList.add(event.getData());
                            removeEventCount.incrementAndGet();
                        }
                    }
                    eventArrived = true;
                }
            });

            InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
            InputHandler inputStreamInputHandler = siddhiAppRuntime.getInputHandler("inputStream");
            siddhiAppRuntime.start();

            // Thursday, June 1, 2017 4:05:50 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

            // Thursday, June 1, 2017 4:05:53 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289953000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289953000L});

            // Thursday, June 1, 2017 4:05:52 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 900f, null, 200L, 60, 1496289952000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 500f, null, 200L, 7, 1496289952000L});

            // Thursday, June 1, 2017 4:05:51 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289951000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289951000L});

            // Thursday, June 1, 2017 4:05:53 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 400f, null, 200L, 9, 1496289953000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 140f, null, 200L, 11, 1496289953000L});

            // Thursday, June 1, 2017 4:05:50 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

            // Thursday, June 1, 2017 4:05:54 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 600f, null, 200L, 6, 1496289954000L});

            // Thursday, June 1, 2017 4:06:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 1000f, null, 200L, 9, 1496290016000L});

            Thread.sleep(2000);
            inputStreamInputHandler.send(new Object[]{"IBM", 1, "2017-06-01 09:35:51 +05:30",
                    "2017-06-01 09:35:52 +05:30", "seconds"});
            Thread.sleep(100);

            List<Object[]> expected = Arrays.asList(
                    new Object[]{240.0, 60.0, 700f, "WSO2", 70f, 50f},
                    new Object[]{200.0, 100.0, 9600f, "IBM", 100f, 100f},
                    new Object[]{1400.0, 700.0, 3500f, "IBM", 900f, 500f},
                    new Object[]{300.0, 100.0, 1540f, "WSO2", 140f, 60f},
                    new Object[]{400.0, 400.0, 3600f, "IBM", 400f, 400f},
                    new Object[]{600.0, 600.0, 3600f, "IBM", 600f, 600f},
                    new Object[]{1000.0, 1000.0, 9000f, "IBM", 1000f, 1000f}
            );
            SiddhiTestHelper.waitForEvents(100, 7, inEventCount, 60000);

            AssertJUnit.assertTrue("Event arrived", eventArrived);
            AssertJUnit.assertEquals("Number of success events", 7, inEventCount.get());
            AssertJUnit.assertTrue("In events matched", SiddhiTestHelper.isEventsMatch(inEventsList, expected));

        } finally {
            siddhiAppRuntime.shutdown();
        }
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest12"}, expectedExceptions =
            SiddhiAppCreationException.class)
    public void incrementalStreamProcessorTest13() {
        LOG.info("incrementalStreamProcessorTest13");
        SiddhiManager siddhiManager = new SiddhiManager();

        String query = "" +
                " @info(name = 'query1') " +
                " define aggregation stockAggregation " +
                " from stockStream " +
                " select sum(price) as sumPrice " +
                " aggregate by arrival every sec ... min";

        siddhiManager.createSiddhiAppRuntime(query);
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest13"}, expectedExceptions = SiddhiParserException.class)
    public void incrementalStreamProcessorTest14() {
        LOG.info("incrementalStreamProcessorTest14");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream = "" +
                " define stream stockStream (arrival long, symbol string, price float, volume int); ";

        String query =
                " @info(name = 'query1') " +
                        " define aggregation stockAggregation " +
                        " from stockStream " +
                        " select sum(price) as sumPrice " +
                        " aggregate by arrival every week";

        siddhiManager.createSiddhiAppRuntime(stockStream + query);
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest14"})
    public void incrementalStreamProcessorTest15() {
        LOG.info("incrementalStreamProcessorTest15");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream = "" +
                " define stream stockStream (arrival long, symbol string, price float, volume int); ";

        String query = "" +
                " @info(name = 'query3') " +
                " define aggregation stockAggregation " +
                " from stockStream " +
                " select sum(price) as sumPrice " +
                " group by price " +
                " aggregate every sec, hour, day";

        siddhiManager.createSiddhiAppRuntime(stockStream + query);
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest15"})
    public void incrementalStreamProcessorTest16() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest16");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp string);";
        String query = "" +
                "define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, " +
                "(price * quantity) as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...year  ; ";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
        siddhiAppRuntime.start();

        Logger logger = Logger.getLogger(StreamJunction.class);
        UnitTestAppender appender = new UnitTestAppender();
        logger.addAppender(appender);
        try {
            // Thursday, June 1, 2017 4:05:50 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, "June 1, 2017 4:05:50 AM"});

            AssertJUnit.assertTrue(appender.getMessages().contains("'June 1, 2017 4:05:50 AM' doesn't match the " +
                    "supported formats <yyyy>-<MM>-<dd> <HH>:<mm>:<ss> (for GMT time zone) or <yyyy>-<MM>-<dd> " +
                    "<HH>:<mm>:<ss> <Z> (for non GMT time zone). The ISO 8601 UTC offset must be provided for <Z> " +
                    "(ex. +05:30, -11:00). Exception on class 'org.wso2.siddhi.core.executor.incremental." +
                    "IncrementalUnixTimeFunctionExecutor'. Hence, dropping event"));
        } catch (Exception e) {
            Assert.fail("Unexpected exception occurred when testing.", e);
        } finally {
            logger.removeAppender(appender);
            siddhiAppRuntime.shutdown();
        }

        // Thursday, June 1, 2017 4:05:50 AM
        stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, "June 1, 2017 4:05:50 AM"});
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest16"})
    public void incrementalStreamProcessorTest17() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest17");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query =
                "define aggregation stockAggregation " +
                        "from stockStream " +
                        "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, " +
                        "(price * quantity) as lastTradeValue " +
                        "group by symbol " +
                        "aggregate by timestamp every sec...year; " +

                        "define stream inputStream (symbol string, value int, startTime string, " +
                        "endTime string, perValue string); " +

                        "@info(name = 'query1') " +
                        "from inputStream as i join stockAggregation as s " +
                        "within \"2017-01-01 00:00:00\", \"2021-01-01 00:00:00\" " +
                        "per \"months\" " +
                        "select AGG_TIMESTAMP, s.symbol, avgPrice, totalPrice " +
                        "order by AGG_TIMESTAMP " +
                        "insert all events into outputStream; ";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        try {
            siddhiAppRuntime.addCallback("query1", new QueryCallback() {
                @Override
                public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                    if (inEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : inEvents) {
                            inEventsList.add(event.getData());
                            inEventCount.incrementAndGet();
                        }
                        eventArrived = true;
                    }
                    if (removeEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : removeEvents) {
                            removeEventsList.add(event.getData());
                            removeEventCount.incrementAndGet();
                        }
                    }
                    eventArrived = true;
                }
            });
            InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
            InputHandler inputStreamInputHandler = siddhiAppRuntime.getInputHandler("inputStream");
            siddhiAppRuntime.start();

            // Thursday, June 1, 2017 4:05:50 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

            // Thursday, June 1, 2017 4:05:52 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289952000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289952000L});

            // Thursday, June 1, 2017 4:05:50 AM (out of order.)
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

            // Thursday, June 1, 2017 4:05:54 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289954000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289954000L});

            // Thursday, June 1, 2017 4:05:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 900f, null, 200L, 60, 1496289956000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 500f, null, 200L, 7, 1496289956000L});

            // Thursday, June 1, 2017 4:06:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 400f, null, 200L, 9, 1496290016000L});

            // Thursday, June 1, 2017 4:07:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 600f, null, 200L, 6, 1496290076000L});

            // Thursday, June 1, 2017 5:07:56 AM
            stockStreamInputHandler.send(new Object[]{"CISCO", 700f, null, 200L, 20, 1496293676000L});

            // Thursday, June 1, 2017 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496297276000L});

            // Friday, June 2, 2017 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"CISCO", 800f, null, 100L, 10, 1496383676000L});

            // Saturday, June 3, 2017 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"CISCO", 900f, null, 100L, 15, 1496470076000L});

            // Monday, July 3, 2017 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1499062076000L});

            // Thursday, August 3, 2017 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 400f, null, 200L, 9, 1501740476000L});

            // Friday, August 3, 2018 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 6, 1533276476000L});

            // Saturday, August 3, 2019 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 260f, 44f, 200L, 16, 1564812476000L});

            // Monday, August 3, 2020 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"CISCO", 260f, 44f, 200L, 16, 1596434876000L});

            // Monday, December 3, 2020 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"CISCO", 260f, 44f, 200L, 16, 1606975676000L});

            Thread.sleep(100);
            inputStreamInputHandler.send(new Object[]{"IBM", 1, "2017-06-01 09:35:51 +05:30",
                    "2017-06-01 09:35:52 +05:30", "seconds"});
            Thread.sleep(100);

            List<Object[]> expected = Arrays.asList(
                    new Object[]{1496275200000L, "WSO2", 65.71428571428571, 460.0},
                    new Object[]{1496275200000L, "CISCO", 800.0, 2400.0},
                    new Object[]{1496275200000L, "IBM", 433.3333333333333, 2600.0},
                    new Object[]{1498867200000L, "IBM", 100.0, 100.0},
                    new Object[]{1501545600000L, "IBM", 400.0, 400.0},
                    new Object[]{1533081600000L, "WSO2", 60.0, 60.0},
                    new Object[]{1564617600000L, "WSO2", 260.0, 260.0},
                    new Object[]{1596240000000L, "CISCO", 260.0, 260.0},
                    new Object[]{1606780800000L, "CISCO", 260.0, 260.0}
            );
            SiddhiTestHelper.waitForEvents(100, 9, inEventCount, 60000);

            AssertJUnit.assertTrue("Event arrived", eventArrived);
            AssertJUnit.assertEquals("Number of success events", 9, inEventCount.get());
            AssertJUnit.assertTrue("In events matched", SiddhiTestHelper.isEventsMatch(inEventsList, expected));

        } finally {
            siddhiAppRuntime.shutdown();
        }
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest17"})
    public void incrementalStreamProcessorTest18() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest18");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query =
                "define aggregation stockAggregation " +
                        "from stockStream " +
                        "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, " +
                        "(price * quantity) as lastTradeValue " +
                        "group by symbol " +
                        "aggregate by timestamp every sec...year; " +

                        "define stream inputStream (symbol string, value int, startTime string, " +
                        "endTime string, perValue string); " +

                        "@info(name = 'query1') " +
                        "from inputStream as i join stockAggregation as s " +
                        "within \"2017-01-01 00:00:00\", \"2021-01-01 00:00:00\" " +
                        "per \"years\" " +
                        "select AGG_TIMESTAMP, s.symbol, avgPrice, totalPrice " +
                        "order by AGG_TIMESTAMP " +
                        "insert all events into outputStream; ";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        try {
            siddhiAppRuntime.addCallback("query1", new QueryCallback() {
                @Override
                public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                    if (inEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : inEvents) {
                            inEventsList.add(event.getData());
                            inEventCount.incrementAndGet();
                        }
                        eventArrived = true;
                    }
                    if (removeEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : removeEvents) {
                            removeEventsList.add(event.getData());
                            removeEventCount.incrementAndGet();
                        }
                    }
                    eventArrived = true;
                }
            });
            InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
            InputHandler inputStreamInputHandler = siddhiAppRuntime.getInputHandler("inputStream");
            siddhiAppRuntime.start();

            // Thursday, June 1, 2017 4:05:50 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

            // Thursday, June 1, 2017 4:05:52 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289952000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289952000L});

            // Thursday, June 1, 2017 4:05:50 AM (out of order.)
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

            // Thursday, June 1, 2017 4:05:54 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289954000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289954000L});

            // Thursday, June 1, 2017 4:05:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 900f, null, 200L, 60, 1496289956000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 500f, null, 200L, 7, 1496289956000L});

            // Thursday, June 1, 2017 4:06:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 400f, null, 200L, 9, 1496290016000L});

            // Thursday, June 1, 2017 4:07:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 600f, null, 200L, 6, 1496290076000L});

            // Thursday, June 1, 2017 5:07:56 AM
            stockStreamInputHandler.send(new Object[]{"CISCO", 700f, null, 200L, 20, 1496293676000L});

            // Thursday, June 1, 2017 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496297276000L});

            // Friday, June 2, 2017 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"CISCO", 800f, null, 100L, 10, 1496383676000L});

            // Saturday, June 3, 2017 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"CISCO", 900f, null, 100L, 15, 1496470076000L});

            // Monday, July 3, 2017 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1499062076000L});

            // Thursday, August 3, 2017 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 400f, null, 200L, 9, 1501740476000L});

            // Friday, August 3, 2018 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 6, 1533276476000L});

            // Saturday, August 3, 2019 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 260f, 44f, 200L, 16, 1564812476000L});

            // Monday, August 3, 2020 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"CISCO", 260f, 44f, 200L, 16, 1596434876000L});

            // Monday, December 3, 2020 6:07:56 AM
            stockStreamInputHandler.send(new Object[]{"CISCO", 260f, 44f, 200L, 16, 1606975676000L});

            Thread.sleep(100);
            inputStreamInputHandler.send(new Object[]{"IBM", 1, "2017-06-01 09:35:51 +05:30",
                    "2017-06-01 09:35:52 +05:30", "seconds"});
            Thread.sleep(100);

            List<Object[]> expected = Arrays.asList(
                    new Object[]{1483228800000L, "CISCO", 800.0, 2400.0},
                    new Object[]{1483228800000L, "IBM", 387.5, 3100.0},
                    new Object[]{1483228800000L, "WSO2", 65.71428571428571, 460.0},
                    new Object[]{1514764800000L, "WSO2", 60.0, 60.0},
                    new Object[]{1546300800000L, "WSO2", 260.0, 260.0},
                    new Object[]{1577836800000L, "CISCO", 260.0, 520.0}
            );
            SiddhiTestHelper.waitForEvents(100, 6, inEventCount, 60000);

            AssertJUnit.assertTrue("Event arrived", eventArrived);
            AssertJUnit.assertEquals("Number of success events", 6, inEventCount.get());
            AssertJUnit.assertTrue("In events matched", SiddhiTestHelper.isUnsortedEventsMatch(inEventsList, expected));

        } finally {
            siddhiAppRuntime.shutdown();
        }
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest18"}, expectedExceptions = SiddhiParserException.class)
    public void incrementalStreamProcessorTest19() {
        LOG.info("incrementalStreamProcessorTest19");
        SiddhiManager siddhiManager = new SiddhiManager();

        String query = "define stream inputStream (symbol string, value int, startTime string, " +
                "endTime string, perValue string); " +

                "@info(name = 'query1') " +
                "from inputStream as i join stockAggregation as s " +
                "within \"2017-01-01 00:00:00\", \"2021-01-01 00:00:00\" " +
                "per \"months\" " +
                "select s.symbol, avgPrice, totalPrice " +
                "insert all events into outputStream; ";

        siddhiManager.createSiddhiAppRuntime(query);
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest19"}, expectedExceptions =
            StoreQueryCreationException.class)
    public void incrementalStreamProcessorTest20() {
        LOG.info("incrementalStreamProcessorTest20");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream);
        siddhiAppRuntime.start();

        siddhiAppRuntime.query("from stockAggregation " +
                "on symbol == \"IBM\" " +
                "within \"2017-**-** **:**:** +05:30\" " +
                "per \"seconds\"; ");

        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest20"}, expectedExceptions =
            StoreQueryCreationException.class)
    public void incrementalStreamProcessorTest21() {
        LOG.info("incrementalStreamProcessorTest21");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = " define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...hour ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        siddhiAppRuntime.start();

        Event[] events = siddhiAppRuntime.query("from stockAggregation " +
                "within \"2017-06-** **:**:**\" " +
                "per \"days\"");
        EventPrinter.print(events);

        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest21"})
    public void incrementalStreamProcessorTest22() throws InterruptedException {
        // Error gets logged at AbstractStreamProcessor level and event gets dropped. Hence no expectedExceptions for
        // this test case
        LOG.info("incrementalStreamProcessorTest22");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query =
                "define aggregation stockAggregation " +
                        "from stockStream " +
                        "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                        "as lastTradeValue  " +
                        "group by symbol " +
                        "aggregate by timestamp every sec...hour  ; " +

                        "define stream inputStream (symbol string, value int, startTime string, endTime string, " +
                        "perValue string); " +

                        "@info(name = 'query1') " +
                        "from inputStream as i join stockAggregation as s " +
                        "within \"2017-06-** **:**:**\" " +
                        "per \"days\" " +
                        "select s.symbol, avgPrice, totalPrice as sumPrice, lastTradeValue  " +
                        "insert all events into outputStream; ";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        InputHandler inputStreamInputHandler = siddhiAppRuntime.getInputHandler("inputStream");
        siddhiAppRuntime.start();

        inputStreamInputHandler.send(new Object[]{"IBM", 1, "2017-06-01 09:35:51 +05:30",
                "2017-06-01 09:35:52 +05:30", "seconds"});

        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest22"})
    public void incrementalStreamProcessorTest24() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest24");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = " define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...hour ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
        siddhiAppRuntime.start();

        stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
        stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

        stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289952000L});
        stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289952000L});

        stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289954000L});
        stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289954000L});
        Thread.sleep(2000);

        Event[] events = siddhiAppRuntime.query("from stockAggregation " +
                "within \"2017-06-** **:**:**\" " +
                "per \"seconds\"");
        EventPrinter.print(events);

        AssertJUnit.assertNotNull(events);
        AssertJUnit.assertEquals(3, events.length);
        List<Object[]> outputDataList = new ArrayList<>();
        for (Event event : events) {
            outputDataList.add(event.getData());
        }

        List<Object[]> expected = Arrays.asList(
                new Object[]{1496289952000L, "WSO2", 80.0, 160.0, 1600f},
                new Object[]{1496289950000L, "WSO2", 60.0, 120.0, 700f},
                new Object[]{1496289954000L, "IBM", 100.0, 200.0, 9600f}
        );

        AssertJUnit.assertTrue("Data Matched", SiddhiTestHelper.isUnsortedEventsMatch(outputDataList, expected));

        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest24"})
    public void incrementalStreamProcessorTest25() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest25");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = " define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...hour ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
        siddhiAppRuntime.start();

        stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
        stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

        stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289952000L});
        stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289952000L});

        stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289954000L});
        stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289954000L});
        Thread.sleep(2000);

        Event[] events = siddhiAppRuntime.query("from stockAggregation " +
                "within \"2017-06-** **:**:**\" " +
                "per \"seconds\" " +
                "select * " +
                "order by AGG_TIMESTAMP ;");
        EventPrinter.print(events);

        AssertJUnit.assertNotNull(events);
        AssertJUnit.assertEquals(3, events.length);
        for (int i = 0; i < events.length; i++) {
            switch (i) {
                case 0:
                    AssertJUnit.assertArrayEquals(new Object[]{1496289950000L, "WSO2", 60.0, 120.0, 700f},
                            events[i].getData());
                    break;
                case 1:
                    AssertJUnit.assertArrayEquals(new Object[]{1496289952000L, "WSO2", 80.0, 160.0, 1600f},
                            events[i].getData());
                    break;
                case 2:
                    AssertJUnit.assertArrayEquals(new Object[]{1496289954000L, "IBM", 100.0, 200.0, 9600f},
                            events[i].getData());
                    break;
                default:
                    // Never reached
                    AssertJUnit.fail();
            }
        }

        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = "incrementalStreamProcessorTest25")
    public void incrementalStreamProcessorTest26() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest26");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = "" +
                "define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...year  ; " +

                "define stream inputStream (symbol string, value int, startTime string, endTime string, " +
                "perValue string); " +

                "@info(name = 'query1') " +
                "from inputStream as i join stockAggregation as s " +
                "within i.startTime, i.endTime " +
                "per i.perValue " +
                "select s.symbol, avgPrice, totalPrice as sumPrice, lastTradeValue  " +
                "insert all events into outputStream; ";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        try {
            siddhiAppRuntime.addCallback("query1", new QueryCallback() {
                @Override
                public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                    if (inEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : inEvents) {
                            inEventsList.add(event.getData());
                            inEventCount.incrementAndGet();
                        }
                        eventArrived = true;
                    }
                    if (removeEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : removeEvents) {
                            removeEventsList.add(event.getData());
                            removeEventCount.incrementAndGet();
                        }
                    }
                    eventArrived = true;
                }
            });

            InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
            InputHandler inputStreamInputHandler = siddhiAppRuntime.getInputHandler("inputStream");
            siddhiAppRuntime.start();

            // Thursday, June 1, 2017 4:05:50 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

            // Thursday, June 1, 2017 4:05:51 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289951000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289951000L});

            // Thursday, June 1, 2017 4:05:52 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 900f, null, 200L, 60, 1496289952000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 500f, null, 200L, 7, 1496289952000L});

            // Thursday, June 1, 2017 4:05:53 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289953000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289953000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 400f, null, 200L, 9, 1496289953000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 140f, null, 200L, 11, 1496289953000L});

            Thread.sleep(100);
            inputStreamInputHandler.send(new Object[]{"IBM", 1, 1496289951000L, 1496289952001L, "seconds"});
            Thread.sleep(100);

            List<Object[]> expected = Arrays.asList(
                    new Object[]{"IBM", 100.0, 200.0, 9600f},
                    new Object[]{"IBM", 700.0, 1400.0, 3500f}
            );
            SiddhiTestHelper.waitForEvents(100, 2, inEventCount, 60000);

            AssertJUnit.assertTrue("Event arrived", eventArrived);
            AssertJUnit.assertEquals("Number of success events", 2, inEventCount.get());
            AssertJUnit.assertTrue("In events matched", SiddhiTestHelper.isUnsortedEventsMatch(inEventsList, expected));

        } finally {
            siddhiAppRuntime.shutdown();
        }
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest26"}, expectedExceptions =
            StoreQueryCreationException.class)
    public void incrementalStreamProcessorTest27() {
        LOG.info("incrementalStreamProcessorTest27");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = " define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...hour ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        siddhiAppRuntime.start();

        Event[] events = siddhiAppRuntime.query("from stockAggregation " +
                "within \"2017-06-** **:**:**\" " +
                "per 1000");
        EventPrinter.print(events);

        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest27"}, expectedExceptions =
            StoreQueryCreationException.class)
    public void incrementalStreamProcessorTest28() {
        LOG.info("incrementalStreamProcessorTest28");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = " define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...hour ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        siddhiAppRuntime.start();

        Event[] events = siddhiAppRuntime.query("from stockAggregation " +
                "within \"2017-06-02 00:00:00\", \"2017-06-01 00:00:00\" " +
                "per \"hours\"");
        EventPrinter.print(events);

        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest28"}, expectedExceptions =
            StoreQueryCreationException.class)
    public void incrementalStreamProcessorTest29() {
        LOG.info("incrementalStreamProcessorTest29");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = " define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...hour ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        siddhiAppRuntime.start();

        Event[] events = siddhiAppRuntime.query("from stockAggregation " +
                "within \"2017-06-** **:**:**:1000\" " +
                "per \"hours\"");
        EventPrinter.print(events);

        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest29"}, expectedExceptions =
            StoreQueryCreationException.class)
    public void incrementalStreamProcessorTest30() {
        LOG.info("incrementalStreamProcessorTest30");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = " define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...hour ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        siddhiAppRuntime.start();

        Event[] events = siddhiAppRuntime.query("from stockAggregation " +
                "within \"2017-06-** 12:**:**\" " +
                "per \"hours\"");
        EventPrinter.print(events);

        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest30"})
    public void incrementalStreamProcessorTest31() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest31");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = " define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...hour ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
        siddhiAppRuntime.start();

        stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
        stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

        stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289952000L});
        stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289952000L});

        stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289954000L});
        stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289954000L});

        stockStreamInputHandler.send(new Object[]{"CISCO", 100f, null, 200L, 26, 1513578087000L});
        stockStreamInputHandler.send(new Object[]{"CISCO", 100f, null, 200L, 96, 1513578087000L});

        Thread.sleep(2000);

        Event[] events = siddhiAppRuntime.query("from stockAggregation " +
                "within \"2017-**-** **:**:**\" " +
                "per \"seconds\" " +
                "select * " +
                "order by AGG_TIMESTAMP ;");
        EventPrinter.print(events);

        AssertJUnit.assertNotNull(events);
        AssertJUnit.assertEquals(4, events.length);
        for (int i = 0; i < events.length; i++) {
            switch (i) {
                case 0:
                    AssertJUnit.assertArrayEquals(new Object[]{1496289950000L, "WSO2", 60.0, 120.0, 700f},
                            events[i].getData());
                    break;
                case 1:
                    AssertJUnit.assertArrayEquals(new Object[]{1496289952000L, "WSO2", 80.0, 160.0, 1600f},
                            events[i].getData());
                    break;
                case 2:
                    AssertJUnit.assertArrayEquals(new Object[]{1496289954000L, "IBM", 100.0, 200.0, 9600f},
                            events[i].getData());
                    break;
                case 3:
                    AssertJUnit.assertArrayEquals(new Object[]{1513578087000L, "CISCO", 100.0, 200.0, 9600f},
                            events[i].getData());
                    break;
                default:
                    //Not reached
                    AssertJUnit.fail();
            }
        }

        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest31"})
    public void incrementalStreamProcessorTest32() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest32");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = " define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...hour ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
        siddhiAppRuntime.start();

        stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
        stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

        stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289952000L});
        stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289952000L});

        stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289954000L});
        stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289954000L});

        stockStreamInputHandler.send(new Object[]{"CISCO", 100f, null, 200L, 26, 1513578087000L});
        stockStreamInputHandler.send(new Object[]{"CISCO", 100f, null, 200L, 96, 1513578087000L});

        Thread.sleep(100);

        Event[] events = siddhiAppRuntime.query("from stockAggregation " +
                "within \"2017-12-18 **:**:**\" " +
                "per \"seconds\" " +
                "select * " +
                "order by AGG_TIMESTAMP ;");
        EventPrinter.print(events);

        AssertJUnit.assertNotNull(events);
        AssertJUnit.assertEquals(1, events.length);
        AssertJUnit.assertArrayEquals(new Object[]{1513578087000L, "CISCO", 100.0, 200.0, 9600f}, events[0].getData());

        Thread.sleep(100);
        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest32"})
    public void incrementalStreamProcessorTest33() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest33");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = " define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...hour ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
        siddhiAppRuntime.start();

        stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
        stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

        stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289952000L});
        stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289952000L});

        stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289954000L});
        stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289954000L});

        stockStreamInputHandler.send(new Object[]{"CISCO", 100f, null, 200L, 26, 1513578087000L});
        stockStreamInputHandler.send(new Object[]{"CISCO", 100f, null, 200L, 96, 1513578087000L});

        Thread.sleep(2000);

        Event[] events = siddhiAppRuntime.query("from stockAggregation " +
                "within \"2017-12-18 06:**:**\" " +
                "per \"seconds\" " +
                "select * " +
                "order by AGG_TIMESTAMP ;");
        EventPrinter.print(events);

        AssertJUnit.assertNotNull(events);
        AssertJUnit.assertEquals(1, events.length);
        AssertJUnit.assertArrayEquals(new Object[]{1513578087000L, "CISCO", 100.0, 200.0, 9600f}, events[0].getData());

        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest33"})
    public void incrementalStreamProcessorTest34() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest34");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = " define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...hour ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
        siddhiAppRuntime.start();

        stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
        stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

        stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289952000L});
        stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289952000L});

        stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289954000L});
        stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289954000L});

        stockStreamInputHandler.send(new Object[]{"CISCO", 100f, null, 200L, 26, 1513578087000L});
        stockStreamInputHandler.send(new Object[]{"CISCO", 100f, null, 200L, 96, 1513578087000L});

        Thread.sleep(2000);

        Event[] events = siddhiAppRuntime.query("from stockAggregation " +
                "within \"2017-12-18 06:21:**\" " +
                "per \"seconds\" " +
                "select * " +
                "order by AGG_TIMESTAMP ;");
        EventPrinter.print(events);

        AssertJUnit.assertNotNull(events);
        AssertJUnit.assertEquals(1, events.length);
        AssertJUnit.assertArrayEquals(new Object[]{1513578087000L, "CISCO", 100.0, 200.0, 9600f}, events[0].getData());

        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest34"})
    public void incrementalStreamProcessorTest35() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest35");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = " define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...hour ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
        siddhiAppRuntime.start();

        stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
        stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

        stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289952000L});
        stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289952000L});

        stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289954000L});
        stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289954000L});

        stockStreamInputHandler.send(new Object[]{"CISCO", 100f, null, 200L, 26, 1513578087000L});
        stockStreamInputHandler.send(new Object[]{"CISCO", 100f, null, 200L, 96, 1513578087000L});

        Thread.sleep(2000);

        Event[] events = siddhiAppRuntime.query("from stockAggregation " +
                "within \"2017-12-18 11:51:27 +05:30\" " +
                "per \"seconds\" " +
                "select * " +
                "order by AGG_TIMESTAMP ;");
        EventPrinter.print(events);

        AssertJUnit.assertNotNull(events);
        AssertJUnit.assertEquals(1, events.length);
        AssertJUnit.assertArrayEquals(new Object[]{1513578087000L, "CISCO", 100.0, 200.0, 9600f}, events[0].getData());

        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest35"}, expectedExceptions =
            StoreQueryCreationException.class)
    public void incrementalStreamProcessorTest36() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest36");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = " define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...hour ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        siddhiAppRuntime.start();
        Thread.sleep(100);

        Event[] events = siddhiAppRuntime.query("from stockAggregation " +
                "within 1513578087000L " +
                "per \"hours\"");
        EventPrinter.print(events);

        Thread.sleep(100);
        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest36"},
            expectedExceptions = StoreQueryCreationException.class)
    public void incrementalStreamProcessorTest37() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest37");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = " define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "group by symbol " +
                "aggregate by timestamp every sec...hour ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        siddhiAppRuntime.start();
        Thread.sleep(100);

        Event[] events = siddhiAppRuntime.query("from stockAggregation " +
                "within \"2017-12-18 11:51:27 +05:30\", 156 " +
                "per \"hours\"");
        EventPrinter.print(events);

        Thread.sleep(100);
        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest37"})
    public void incrementalStreamProcessorTest38() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest38");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp string);";
        String query = "" +
                "define aggregation stockAggregation " +
                "from stockStream " +
                "select avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "aggregate by timestamp every sec...year; " +

                "define stream inputStream (symbol string, value int, startTime string, " +
                "endTime string, perValue string); " +

                "@info(name = 'query1') " +
                "from inputStream join stockAggregation " +
                "within \"2017-06-01 09:35:00 +05:30\", \"2017-06-01 10:37:57 +05:30\" " +
                "per perValue " +
                "select avgPrice, totalPrice as sumPrice, lastTradeValue  " +
                "insert all events into outputStream; ";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        try {
            siddhiAppRuntime.addCallback("query1", new QueryCallback() {
                @Override
                public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                    if (inEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : inEvents) {
                            inEventsList.add(event.getData());
                            inEventCount.incrementAndGet();
                        }
                        eventArrived = true;
                    }
                    if (removeEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : removeEvents) {
                            removeEventsList.add(event.getData());
                            removeEventCount.incrementAndGet();
                        }
                    }
                    eventArrived = true;
                }
            });

            InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
            InputHandler inputStreamInputHandler = siddhiAppRuntime.getInputHandler("inputStream");
            siddhiAppRuntime.start();

            // Thursday, June 1, 2017 4:05:50 AM (add 5.30 to get corresponding IST time)
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, "2017-06-01 04:05:50"});

            // Thursday, June 1, 2017 4:05:51 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 50f, 60f, 90L, 6, "2017-06-01 04:05:51"});

            // Thursday, June 1, 2017 4:05:52 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, "2017-06-01 04:05:52"});
            stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, "2017-06-01 04:05:52"});

            // Thursday, June 1, 2017 4:05:50 AM (out of order)
            stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, "2017-06-01 04:05:50"});

            // Thursday, June 1, 2017 4:05:54 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, "2017-06-01 04:05:54"});
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, "2017-06-01 04:05:54"});

            // Thursday, June 1, 2017 4:05:50 AM (out of order)
            stockStreamInputHandler.send(new Object[]{"IBM", 50f, 60f, 90L, 6, "2017-06-01 04:05:50"});

            // Thursday, June 1, 2017 4:05:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 900f, null, 200L, 60, "2017-06-01 04:05:56"});
            stockStreamInputHandler.send(new Object[]{"IBM", 500f, null, 200L, 7, "2017-06-01 04:05:56"});

            // Thursday, June 1, 2017 4:06:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 400f, null, 200L, 9, "2017-06-01 04:06:56"});

            // Thursday, June 1, 2017 4:07:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 600f, null, 200L, 6, "2017-06-01 04:07:56"});

            // Thursday, June 1, 2017 5:07:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 700f, null, 200L, 20, "2017-06-01 05:07:56"});

            Thread.sleep(2000);
            inputStreamInputHandler.send(new Object[]{"IBM", 1, "2017-06-01 09:35:51 +05:30",
                    "2017-06-01 09:35:52 +05:30", "minutes"});
            Thread.sleep(100);

            List<Object[]> expected = Arrays.asList(
                    new Object[]{198.0, 1980.0, 3500f},
                    new Object[]{400.0, 400.0, 3600f},
                    new Object[]{700.0, 700.0, 14000f},
                    new Object[]{600.0, 600.0, 3600f}
            );
            SiddhiTestHelper.waitForEvents(100, 4, inEventCount, 60000);

            AssertJUnit.assertTrue("Event arrived", eventArrived);
            AssertJUnit.assertEquals("Number of success events", 4, inEventCount.get());
            AssertJUnit.assertTrue("In events matched", SiddhiTestHelper.isUnsortedEventsMatch(inEventsList, expected));

        } finally {
            siddhiAppRuntime.shutdown();
        }
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest38"})
    public void incrementalStreamProcessorTest39() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest39");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp string);";
        String query = "" +
                "define aggregation stockAggregation " +
                "from stockStream " +
                "select avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "aggregate by timestamp every sec...year; " +

                "define stream inputStream (symbol string, value int, startTime string, " +
                "endTime string, perValue string); " +

                "@info(name = 'query1') " +
                "from inputStream join stockAggregation " +
                "within \"2017-06-01 04:05:49\", \"2017-06-01 05:07:57\" " +
                "per \"seconds\" " +
                "select AGG_TIMESTAMP, avgPrice, totalPrice as sumPrice, lastTradeValue  " +
                "order by AGG_TIMESTAMP " +
                "insert all events into outputStream; ";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        try {
            siddhiAppRuntime.addCallback("query1", new QueryCallback() {
                @Override
                public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                    if (inEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : inEvents) {
                            inEventsList.add(event.getData());
                            inEventCount.incrementAndGet();
                        }
                        eventArrived = true;
                    }
                    if (removeEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : removeEvents) {
                            removeEventsList.add(event.getData());
                            removeEventCount.incrementAndGet();
                        }
                    }
                    eventArrived = true;
                }
            });

            InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
            InputHandler inputStreamInputHandler = siddhiAppRuntime.getInputHandler("inputStream");
            siddhiAppRuntime.start();

            // Thursday, June 1, 2017 4:05:50 AM (add 5.30 to get corresponding IST time)
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, "2017-06-01 04:05:50"});

            // Thursday, June 1, 2017 4:05:51 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 50f, 60f, 90L, 6, "2017-06-01 04:05:51"});

            // Thursday, June 1, 2017 4:05:47 AM (out of order. )
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, "2017-06-01 04:05:47"});

            // Thursday, June 1, 2017 4:05:49 AM (out of order.)
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, "2017-06-01 04:05:49"});

            // Thursday, June 1, 2017 4:05:52 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, "2017-06-01 04:05:52"});

            // Thursday, June 1, 2017 4:05:50 AM (out of order.)
            stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, "2017-06-01 04:05:50"});

            // Thursday, June 1, 2017 4:05:53 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, "2017-06-01 04:05:53"});

            // Thursday, June 1, 2017 4:05:50 AM (out of order. )
            stockStreamInputHandler.send(new Object[]{"IBM", 50f, 60f, 90L, 6, "2017-06-01 04:05:50"});

            // Thursday, June 1, 2017 4:05:54 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, "2017-06-01 04:05:54"});

            // Thursday, June 1, 2017 4:05:50 AM (out of order. )
            stockStreamInputHandler.send(new Object[]{"IBM", 50f, 60f, 90L, 6, "2017-06-01 04:05:50"});

            // Thursday, June 1, 2017 4:05:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 900f, null, 200L, 60, "2017-06-01 04:05:56"});
            stockStreamInputHandler.send(new Object[]{"IBM", 500f, null, 200L, 7, "2017-06-01 04:05:56"});

            // Thursday, June 1, 2017 4:06:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 400f, null, 200L, 9, "2017-06-01 04:06:56"});

            // Thursday, June 1, 2017 4:07:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 600f, null, 200L, 6, "2017-06-01 04:07:56"});

            // Thursday, June 1, 2017 5:07:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 700f, null, 200L, 20, "2017-06-01 05:07:56"});

            Thread.sleep(2000);
            inputStreamInputHandler.send(new Object[]{"IBM", 1, "2017-06-01 09:35:51 +05:30",
                    "2017-06-01 09:35:52 +05:30", "seconds"});
            Thread.sleep(100);

            List<Object[]> expected = Arrays.asList(
                    new Object[]{1496289949000L, 60.0, 60.0, 3360f},
                    new Object[]{1496289950000L, 55.0, 220.0, 300f},
                    new Object[]{1496289951000L, 50.0, 50.0, 300f},
                    new Object[]{1496289952000L, 100.0, 100.0, 1600f},
                    new Object[]{1496289953000L, 100.0, 100.0, 2600f},
                    new Object[]{1496289954000L, 100.0, 100.0, 9600f},
                    new Object[]{1496289956000L, 700.0, 1400.0, 3500f},
                    new Object[]{1496290016000L, 400.0, 400.0, 3600f},
                    new Object[]{1496290076000L, 600.0, 600.0, 3600f},
                    new Object[]{1496293676000L, 700.0, 700.0, 14000f}

            );
            SiddhiTestHelper.waitForEvents(100, 10, inEventCount, 60000);

            AssertJUnit.assertTrue("Event arrived", eventArrived);
            AssertJUnit.assertEquals("Number of success events", 10, inEventCount.get());
            AssertJUnit.assertTrue("In events matched", SiddhiTestHelper.isUnsortedEventsMatch(inEventsList, expected));

        } finally {
            siddhiAppRuntime.shutdown();
        }
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest39"})
    public void incrementalStreamProcessorTest40() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest40");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query =
                "define aggregation stockAggregation " +
                        "from stockStream " +
                        "select symbol, avg(price) as avgPrice, sum(price) as totalPrice, " +
                        "(price * quantity) as lastTradeValue  " +
                        "group by symbol " +
                        "aggregate by timestamp every sec...hour ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        siddhiAppRuntime.start();

        LocalDate currentDate = LocalDate.now();
        String year = String.valueOf(currentDate.getYear());
        String month = String.valueOf(currentDate.getMonth().getValue());
        if (month.length() == 1) {
            month = "0".concat(month);
        }

        Event[] events = siddhiAppRuntime.query("from stockAggregation " +
                "within \"" + year + "-" + month + "-** **:**:** +05:30\" " +
                "per \"seconds\"; ");

        AssertJUnit.assertNull(events);

        Thread.sleep(100);
        siddhiAppRuntime.shutdown();
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest40"})
    public void incrementalStreamProcessorTest41() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest41");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query =
                "define aggregation stockAggregation " +
                        "from stockStream " +
                        "select avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) as " +
                        "lastTradeValue, " +
                        "count() as count " +
                        "aggregate by timestamp every sec...year ;" +

                        "define stream inputStream (symbol string, value int, startTime string, " +
                        "endTime string, perValue string); " +

                        "@info(name = 'query1') " +
                        "from inputStream as i join stockAggregation as s " +
                        "within \"2017-06-01 04:05:**\" " +
                        "per \"seconds\" " +
                        "select AGG_TIMESTAMP, s.avgPrice, totalPrice, lastTradeValue, count " +
                        "order by AGG_TIMESTAMP " +
                        "insert all events into outputStream; ";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        try {
            siddhiAppRuntime.addCallback("query1", new QueryCallback() {
                @Override
                public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                    if (inEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : inEvents) {
                            inEventsList.add(event.getData());
                            inEventCount.incrementAndGet();
                        }
                        eventArrived = true;
                    }
                    if (removeEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : removeEvents) {
                            removeEventsList.add(event.getData());
                            removeEventCount.incrementAndGet();
                        }
                    }
                    eventArrived = true;
                }
            });
            InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
            InputHandler inputStreamInputHandler = siddhiAppRuntime.getInputHandler("inputStream");
            siddhiAppRuntime.start();

            // Thursday, June 1, 2017 4:05:50 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

            // Thursday, June 1, 2017 4:05:49 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289949000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289949000L});

            // Thursday, June 1, 2017 4:05:48 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289948000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289948000L});

            // Thursday, June 1, 2017 4:05:47 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 900f, null, 200L, 60, 1496289947000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 500f, null, 200L, 7, 1496289947000L});

            // Thursday, June 1, 2017 4:05:46 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 400f, null, 200L, 9, 1496289946000L});

            Thread.sleep(2000);

            inputStreamInputHandler.send(new Object[]{"IBM", 1, "2017-06-01 09:35:51 +05:30",
                    "2017-06-01 09:35:52 +05:30", "seconds"});
            Thread.sleep(100);

            List<Object[]> expected = Arrays.asList(
                    new Object[]{1496289946000L, 400.0, 400.0, 3600f, 1L},
                    new Object[]{1496289947000L, 700.0, 1400.0, 3500f, 2L},
                    new Object[]{1496289948000L, 100.0, 200.0, 9600f, 2L},
                    new Object[]{1496289949000L, 80.0, 160.0, 1600f, 2L},
                    new Object[]{1496289950000L, 60.0, 120.0, 700f, 2L}
            );
            SiddhiTestHelper.waitForEvents(100, 5, inEventCount, 60000);

            AssertJUnit.assertTrue("Event arrived", eventArrived);
            AssertJUnit.assertEquals("Number of success events", 5, inEventCount.get());
            AssertJUnit.assertTrue("In events matched", SiddhiTestHelper.isUnsortedEventsMatch(inEventsList, expected));

        } finally {
            siddhiAppRuntime.shutdown();
        }
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest41"})
    public void incrementalStreamProcessorTest42() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest42");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query =
                "define aggregation stockAggregation " +
                        "from stockStream " +
                        "select avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) as " +
                        "lastTradeValue, " +
                        "count() as count " +
                        "aggregate by timestamp every sec...year ;" +

                        "define stream inputStream (symbol string, value int, startTime string, " +
                        "endTime string, perValue string); " +

                        "@info(name = 'query1') " +
                        "from inputStream as i join stockAggregation as s " +
                        "within \"2017-06-01 04:05:**\" " +
                        "per \"seconds\" " +
                        "select AGG_TIMESTAMP, s.avgPrice, totalPrice, lastTradeValue, count " +
                        "order by AGG_TIMESTAMP " +
                        "insert all events into outputStream; ";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        try {
            siddhiAppRuntime.addCallback("query1", new QueryCallback() {
                @Override
                public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                    if (inEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : inEvents) {
                            inEventsList.add(event.getData());
                            inEventCount.incrementAndGet();
                        }
                        eventArrived = true;
                    }
                    if (removeEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : removeEvents) {
                            removeEventsList.add(event.getData());
                            removeEventCount.incrementAndGet();
                        }
                    }
                    eventArrived = true;
                }
            });
            InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
            InputHandler inputStreamInputHandler = siddhiAppRuntime.getInputHandler("inputStream");
            siddhiAppRuntime.start();

            // Thursday, June 1, 2017 4:05:50 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

            // Thursday, June 1, 2017 4:05:49 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289949000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289949000L});

            // Thursday, June 1, 2017 4:05:46 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 400f, null, 200L, 9, 1496289946000L});

            // Thursday, June 1, 2017 4:05:47 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 900f, null, 200L, 60, 1496289947000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 500f, null, 200L, 7, 1496289947000L});

            // Thursday, June 1, 2017 4:05:48 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289948000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289948000L});

            Thread.sleep(2000);

            inputStreamInputHandler.send(new Object[]{"IBM", 1, "2017-06-01 09:35:51 +05:30",
                    "2017-06-01 09:35:52 +05:30", "seconds"});
            Thread.sleep(100);

            List<Object[]> expected = Arrays.asList(
                    new Object[]{1496289946000L, 400.0, 400.0, 3600f, 1L},
                    new Object[]{1496289947000L, 700.0, 1400.0, 3500f, 2L},
                    new Object[]{1496289948000L, 100.0, 200.0, 9600f, 2L},
                    new Object[]{1496289949000L, 80.0, 160.0, 1600f, 2L},
                    new Object[]{1496289950000L, 60.0, 120.0, 700f, 2L}
            );
            SiddhiTestHelper.waitForEvents(100, 5, inEventCount, 60000);

            AssertJUnit.assertTrue("Event arrived", eventArrived);
            AssertJUnit.assertEquals("Number of success events", 5, inEventCount.get());
            AssertJUnit.assertTrue("In events matched", SiddhiTestHelper.isEventsMatch(inEventsList, expected));

        } finally {
            siddhiAppRuntime.shutdown();
        }
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest42"})
    public void incrementalStreamProcessorTest43() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest43");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query =
                "define aggregation stockAggregation " +
                        "from stockStream " +
                        "select avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) as " +
                        "lastTradeValue, " +
                        "count() as count " +
                        "group by symbol " +
                        "aggregate by timestamp every sec...year ;" +

                        "define stream inputStream (symbol string, value int, startTime string, " +
                        "endTime string, perValue string); " +

                        "@info(name = 'query1') " +
                        "from inputStream as i join stockAggregation as s " +
                        "within \"2017-06-01 04:05:**\" " +
                        "per \"seconds\" " +
                        "select AGG_TIMESTAMP, s.avgPrice, totalPrice, lastTradeValue, count " +
                        "order by AGG_TIMESTAMP " +
                        "insert all events into outputStream; ";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        try {
            siddhiAppRuntime.addCallback("query1", new QueryCallback() {
                @Override
                public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                    if (inEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : inEvents) {
                            inEventsList.add(event.getData());
                            inEventCount.incrementAndGet();
                        }
                        eventArrived = true;
                    }
                    if (removeEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : removeEvents) {
                            removeEventsList.add(event.getData());
                            removeEventCount.incrementAndGet();
                        }
                    }
                    eventArrived = true;
                }
            });
            InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
            InputHandler inputStreamInputHandler = siddhiAppRuntime.getInputHandler("inputStream");
            siddhiAppRuntime.start();

            // Thursday, June 1, 2017 4:05:50 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

            // Thursday, June 1, 2017 4:05:51 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289951000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289951011L});

            // Thursday, June 1, 2017 4:05:52 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 400f, null, 200L, 9, 1496289952000L});

            // Thursday, June 1, 2017 4:05:49 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 900f, null, 200L, 60, 1496289949000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 500f, null, 200L, 7, 1496289949000L});

            // Thursday, June 1, 2017 4:05:53 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289953000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289953000L});

            Thread.sleep(2000);

            inputStreamInputHandler.send(new Object[]{"IBM", 1, "2017-06-01 09:35:51 +05:30",
                    "2017-06-01 09:35:52 +05:30", "seconds"});
            Thread.sleep(100);

            List<Object[]> expected = Arrays.asList(
                    new Object[]{1496289949000L, 700.0, 1400.0, 3500f, 2L},
                    new Object[]{1496289950000L, 60.0, 120.0, 700f, 2L},
                    new Object[]{1496289951000L, 80.0, 160.0, 1600f, 2L},
                    new Object[]{1496289952000L, 400.0, 400.0, 3600f, 1L},
                    new Object[]{1496289953000L, 100.0, 200.0, 9600f, 2L}
            );
            SiddhiTestHelper.waitForEvents(100, 5, inEventCount, 60000);

            AssertJUnit.assertTrue("Event arrived", eventArrived);
            AssertJUnit.assertEquals("Number of success events", 5, inEventCount.get());
            AssertJUnit.assertTrue("In events matched", SiddhiTestHelper.isEventsMatch(inEventsList, expected));

        } finally {
            siddhiAppRuntime.shutdown();
        }
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest43"})
    public void incrementalStreamProcessorTest44() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest44");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query =
                "define aggregation stockAggregation " +
                        "from stockStream " +
                        "select avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) as " +
                        "lastTradeValue, " +
                        "count() as count " +
                        "aggregate by timestamp every sec...year ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        try {
            InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
            siddhiAppRuntime.start();

            // Thursday, June 1, 2017 4:05:50 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 70f, null, 40L, 10, 1496289950000L});

            // Thursday, June 1, 2017 4:05:51 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, 1496289951000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, 1496289951011L});

            // Thursday, June 1, 2017 4:05:52 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 400f, null, 200L, 9, 1496289952000L});

            // Thursday, June 1, 2017 4:05:49 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 900f, null, 200L, 60, 1496289949000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 500f, null, 200L, 7, 1496289949000L});

            // Thursday, June 1, 2017 4:05:53 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289953000L});
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, 1496289953000L});

            Thread.sleep(2000);


            siddhiAppRuntime.query("from stockAggregation within 1496289949000L, 1496289950000L per " +
                    "'days' select AGG_TIMESTAMP, avgPrice");

            siddhiAppRuntime.query("from stockAggregation within 1496289949000L, 1496289950000L per " +
                    "'days' select AGG_TIMESTAMP, avgPrice");


        } finally {
            siddhiAppRuntime.shutdown();
        }
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest44"})
    public void incrementalStreamProcessorTest45() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest45 - Testing out of order events greater than buffer size with " +
                "group by");

        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp long);";
        String query = "define aggregation stockAggregation " +
                "from stockStream " +
                "select symbol, sum(price) as totalPrice " +
                "group by symbol " +
                "aggregate by timestamp every sec...year ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        try {
            InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
            siddhiAppRuntime.start();

            // Thursday, June 1, 2017 4:05:50 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, 1496289950000L});

            // Thursday, June 1, 2017 4:05:51 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 16, 1496289951011L});

            // Thursday, June 1, 2017 4:05:52 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 400f, null, 200L, 9, 1496289952000L});

            // Thursday, June 1, 2017 4:05:50 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 900f, null, 200L, 60, 1496289950000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 500f, null, 200L, 7, 1496289951011L});

            // Thursday, June 1, 2017 4:05:53 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, 1496289953000L});
            stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 96, 1496289953000L});

            Thread.sleep(2000);

            Event[] events = siddhiAppRuntime.query("from stockAggregation within 0L, 1496289953000L per " +
                    "'seconds' select AGG_TIMESTAMP, symbol, totalPrice");

            AssertJUnit.assertEquals("Check time windows", 5, events.length);

        } finally {
            siddhiAppRuntime.shutdown();
        }
    }

    @Test(dependsOnMethods = {"incrementalStreamProcessorTest45"})
    public void incrementalStreamProcessorTest46() throws InterruptedException {
        LOG.info("incrementalStreamProcessorTest46 - Two different timezones");
        SiddhiManager siddhiManager = new SiddhiManager();

        String stockStream =
                "define stream stockStream (symbol string, price float, lastClosingPrice float, volume long , " +
                        "quantity int, timestamp string);";
        String query = "" +
                "define aggregation stockAggregation " +
                "from stockStream " +
                "select avg(price) as avgPrice, sum(price) as totalPrice, (price * quantity) " +
                "as lastTradeValue  " +
                "aggregate by timestamp every sec...year; " +

                "define stream inputStream (symbol string, value int, startTime string, " +
                "endTime string, perValue string); " +

                "@info(name = 'query1') " +
                "from inputStream join stockAggregation " +
                "within startTime, endTime " +
                "per perValue " +
                "select AGG_TIMESTAMP, avgPrice, totalPrice as sumPrice " +
                "insert all events into outputStream; ";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(stockStream + query);

        try {
            siddhiAppRuntime.addCallback("query1", new QueryCallback() {
                @Override
                public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                    if (inEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : inEvents) {
                            inEventsList.add(event.getData());
                            inEventCount.incrementAndGet();
                        }
                        eventArrived = true;
                    }
                    if (removeEvents != null) {
                        EventPrinter.print(timestamp, inEvents, removeEvents);
                        for (Event event : removeEvents) {
                            removeEventsList.add(event.getData());
                            removeEventCount.incrementAndGet();
                        }
                    }
                    eventArrived = true;
                }
            });

            InputHandler stockStreamInputHandler = siddhiAppRuntime.getInputHandler("stockStream");
            InputHandler inputStreamInputHandler = siddhiAppRuntime.getInputHandler("inputStream");
            siddhiAppRuntime.start();

            //Wednesday, May 31, 2017 11:05:49 PM (add 5.30 to get corresponding IST time)
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, "2017-06-01 04:35:49 +05:30"});
            // Thursday, June 1, 2017 4:05:50 AM (add 5.30 to get corresponding IST time)
            stockStreamInputHandler.send(new Object[]{"WSO2", 50f, 60f, 90L, 6, "2017-06-01 04:05:50"});

            // Thursday, June 1, 2017 4:05:51 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 50f, 60f, 90L, 6, "2017-06-01 04:05:51"});

            // Thursday, June 1, 2017 4:05:52 AM
            stockStreamInputHandler.send(new Object[]{"WSO2", 60f, 44f, 200L, 56, "2017-06-01 04:05:52"});
            stockStreamInputHandler.send(new Object[]{"WSO2", 100f, null, 200L, 16, "2017-06-01 04:05:52"});

            // Thursday, June 1, 2017 4:05:54 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 26, "2017-06-01 04:05:54"});
            stockStreamInputHandler.send(new Object[]{"IBM", 100f, null, 200L, 96, "2017-06-01 04:05:54"});

            // Thursday, June 1, 2017 4:05:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 900f, null, 200L, 60, "2017-06-01 04:05:56"});
            stockStreamInputHandler.send(new Object[]{"IBM", 500f, null, 200L, 7, "2017-06-01 04:05:56"});

            // Thursday, June 1, 2017 4:06:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 400f, null, 200L, 9, "2017-06-01 04:06:56"});
            stockStreamInputHandler.send(new Object[]{"IBM", 600f, null, 200L, 6, "2017-06-01 09:36:58 +05:30"});

            // Thursday, June 1, 2017 4:07:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 600f, null, 200L, 6, "2017-06-01 04:07:56 +05:30"});

            // Thursday, June 1, 2017 5:07:56 AM
            stockStreamInputHandler.send(new Object[]{"IBM", 700f, null, 200L, 20, "2017-06-01 11:07:56 +05:30"});

            Thread.sleep(2000);
            inputStreamInputHandler.send(new Object[]{"IBM", 1, "2016-05-30 08:35:51 +05:30",
                    "2018-06-02 10:35:52 +05:30", "months"});
            Thread.sleep(100);

            List<Object[]> expected = Arrays.asList(
                    new Object[]{1493596800000L, 325.0, 650.0},
                    new Object[]{1496275200000L, 323.6363636363636, 3560.0}
            );
            SiddhiTestHelper.waitForEvents(100, 2, inEventCount, 60000);

            AssertJUnit.assertTrue("Event arrived", eventArrived);
            AssertJUnit.assertEquals("Number of success events", 2, inEventCount.get());
            AssertJUnit.assertTrue("In events matched", SiddhiTestHelper.isEventsMatch(inEventsList, expected));

        } finally {
            siddhiAppRuntime.shutdown();
        }
    }

}
