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

package org.wso2.siddhi.core.query.ratelimit;


import org.apache.log4j.Logger;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.output.StreamCallback;
import org.wso2.siddhi.core.util.EventPrinter;

public class TimeOutputRateLimitTestCase {
    private static final Logger log = Logger.getLogger(TimeOutputRateLimitTestCase.class);
    private volatile int count;
    private volatile int inEventCount;
    private volatile int removeEventCount;
    private volatile boolean eventArrived;

    @BeforeMethod
    public void init() {
        count = 0;
        eventArrived = false;
        inEventCount = 0;
        removeEventCount = 0;
    }

    @Test
    public void testTimeOutputRateLimitQuery1() throws InterruptedException {
        log.info("TimeOutputRateLimit test1");

        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@app:name('EventOutputRateLimitTest1') " +
                "" +
                "define stream LoginEvents (timestamp long, ip string);" +
                "" +
                "@info(name = 'query1') " +
                "from LoginEvents " +
                "select ip " +
                "output every 1 sec " +
                "insert into uniqueIps ;";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        log.info("Running : " + siddhiAppRuntime.getName());

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timestamp, inEvents, removeEvents);
                if (inEvents != null) {
                    count += inEvents.length;
                } else {
                    AssertJUnit.fail("Remove events emitted");
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("LoginEvents");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.3"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.9"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.30"});
        Thread.sleep(2000);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.40"});
        Thread.sleep(1000);

        AssertJUnit.assertEquals("Event arrived", true, eventArrived);
        AssertJUnit.assertEquals("Number of output event value", 6, count);
        siddhiAppRuntime.shutdown();

    }

    @Test
    public void testTimeOutputRateLimitQuery2() throws InterruptedException {
        log.info("TimeOutputRateLimit test2");

        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@app:name('EventOutputRateLimitTest2') " +
                "" +
                "define stream LoginEvents (timestamp long, ip string);" +
                "" +
                "@info(name = 'query1') " +
                "from LoginEvents " +
                "select ip " +
                "output all every 1 sec " +
                "insert into uniqueIps ;";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        log.info("Running : " + siddhiAppRuntime.getName());

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timestamp, inEvents, removeEvents);
                if (inEvents != null) {
                    count += inEvents.length;
                } else {
                    AssertJUnit.fail("Remove events emitted");
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("LoginEvents");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.3"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.9"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.30"});
        Thread.sleep(1500);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.40"});
        Thread.sleep(1000);

        AssertJUnit.assertEquals("Event arrived", true, eventArrived);
        AssertJUnit.assertEquals("Number of output event value", 6, count);
        siddhiAppRuntime.shutdown();

    }

    @Test
    public void testTimeOutputRateLimitQuery3() throws InterruptedException {
        log.info("TimeOutputRateLimit test3");

        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@app:name('EventOutputRateLimitTest3') " +
                "" +
                "define stream LoginEvents (timestamp long, ip string);" +
                "" +
                "@info(name = 'query1') " +
                "from LoginEvents " +
                "select ip " +
                "output every 1 sec " +
                "insert into uniqueIps ;";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        log.info("Running : " + siddhiAppRuntime.getName());

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timestamp, inEvents, removeEvents);
                if (inEvents != null) {
                    count += inEvents.length;
                } else {
                    AssertJUnit.fail("Remove events emitted");
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("LoginEvents");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.3"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.9"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.30"});
        Thread.sleep(1000);

        AssertJUnit.assertEquals("Event arrived", true, eventArrived);
        AssertJUnit.assertEquals("Number of output event value", 8, count);
        siddhiAppRuntime.shutdown();

    }

    @Test
    public void testTimeOutputRateLimitQuery4() throws InterruptedException {
        log.info("TimeOutputRateLimit test4");

        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@app:name('EventOutputRateLimitTest4') " +
                "" +
                "define stream LoginEvents (timestamp long, ip string);" +
                "" +
                "@info(name = 'query1') " +
                "from LoginEvents " +
                "select ip " +
                "output first every 1 sec " +
                "insert into uniqueIps ;";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        log.info("Running : " + siddhiAppRuntime.getName());

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timestamp, inEvents, removeEvents);
                if (inEvents != null) {
                    count += inEvents.length;
                    AssertJUnit.assertTrue("192.10.1.5".equals(inEvents[0].getData(0)) ||
                            "192.10.1.9".equals(inEvents[0].getData(0)) ||
                            "192.10.1.30".equals(inEvents[0].getData(0)));
                } else {
                    AssertJUnit.fail("Remove events emitted");
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("LoginEvents");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.3"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.9"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.30"});
        Thread.sleep(1000);

        AssertJUnit.assertEquals("Event arrived", true, eventArrived);
        AssertJUnit.assertEquals("Number of output event value", 3, count);

        siddhiAppRuntime.shutdown();

    }

    @Test
    public void testTimeOutputRateLimitQuery5() throws InterruptedException {
        log.info("TimeOutputRateLimit test5");

        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@app:name('EventOutputRateLimitTest5') " +
                "" +
                "define stream LoginEvents (timestamp long, ip string);" +
                "" +
                "@info(name = 'query1') " +
                "from LoginEvents " +
                "select ip " +
                "output last every 1 sec " +
                "insert into uniqueIps ;";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        log.info("Running : " + siddhiAppRuntime.getName());

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timestamp, inEvents, removeEvents);
                if (inEvents != null) {
                    count += inEvents.length;
                    AssertJUnit.assertTrue("192.10.1.5".equals(inEvents[0].getData(0)) ||
                            "192.10.1.3".equals(inEvents[0].getData(0)) || "192.10.1.4".equals(inEvents[0].getData(0))
                            || "192.10.1.30".equals(inEvents[0].getData(0)));
                } else {
                    AssertJUnit.fail("Remove events emitted");
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("LoginEvents");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.3"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.9"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.30"});
        Thread.sleep(1000);

        AssertJUnit.assertEquals("Event arrived", true, eventArrived);
        AssertJUnit.assertTrue("Number of output event value", 3 <= count);

        siddhiAppRuntime.shutdown();

    }

    @Test
    public void testTimeOutputRateLimitQuery6() throws InterruptedException {
        log.info("TimeOutputRateLimit test6");

        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@app:name('EventOutputRateLimitTest6') " +
                "" +
                "define stream LoginEvents (timestamp long, ip string);" +
                "" +
                "@info(name = 'query1') " +
                "from LoginEvents " +
                "select ip " +
                "group by ip " +
                "output first every 1 sec " +
                "insert into uniqueIps ;";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        log.info("Running : " + siddhiAppRuntime.getName());

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timestamp, inEvents, removeEvents);
                if (inEvents != null) {
                    count += inEvents.length;
                } else {
                    AssertJUnit.fail("Remove events emitted");
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("LoginEvents");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.3"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.9"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.30"});
        Thread.sleep(1000);

        AssertJUnit.assertEquals("Event arrived", true, eventArrived);
        AssertJUnit.assertEquals("Number of output event value", 6, count);

        siddhiAppRuntime.shutdown();

    }

    @Test
    public void testTimeOutputRateLimitQuery7() throws InterruptedException {
        log.info("TimeOutputRateLimit test7");

        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@app:name('EventOutputRateLimitTest7') " +
                "" +
                "define stream LoginEvents (timestamp long, ip string);" +
                "" +
                "@info(name = 'query1') " +
                "from LoginEvents " +
                "select ip " +
                "group by ip " +
                "output last every 1 sec " +
                "insert into uniqueIps ;";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        log.info("Running : " + siddhiAppRuntime.getName());

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timestamp, inEvents, removeEvents);
                if (inEvents != null) {
                    count += inEvents.length;
                } else {
                    AssertJUnit.fail("Remove events emitted");
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("LoginEvents");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.3"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.9"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.30"});
        Thread.sleep(1000);

        AssertJUnit.assertEquals("Event arrived", true, eventArrived);
        AssertJUnit.assertEquals("Number of output event value", 6, count);

        siddhiAppRuntime.shutdown();

    }

    @Test
    public void testTimeOutputRateLimitQuery8() throws InterruptedException {
        log.info("TimeOutputRateLimit test8");

        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@app:name('EventOutputRateLimitTest8') " +
                "" +
                "define stream LoginEvents (timestamp long, ip string);" +
                "" +
                "@info(name = 'query1') " +
                "from LoginEvents#window.lengthBatch(2) " +
                "select ip " +
                "group by ip " +
                "output last every 1 sec " +
                "insert into uniqueIps ;";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        log.info("Running : " + siddhiAppRuntime.getName());

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timestamp, inEvents, removeEvents);
                if (inEvents != null) {
                    count += inEvents.length;
                } else {
                    AssertJUnit.fail("Remove events emitted");
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("LoginEvents");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.3"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.9"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.30"});
        Thread.sleep(1200);

        AssertJUnit.assertEquals("Event arrived", true, eventArrived);
        AssertJUnit.assertEquals("Number of output event value", 5, count);

        siddhiAppRuntime.shutdown();

    }

    @Test
    public void testTimeOutputRateLimitQuery9() throws InterruptedException {
        log.info("TimeOutputRateLimit test9");

        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@app:name('EventOutputRateLimitTest7') " +
                "" +
                "define stream LoginEvents (timestamp long, ip string);" +
                "" +
                "@info(name = 'query1') " +
                "from LoginEvents#window.lengthBatch(2) " +
                "select ip " +
                "group by ip " +
                "output last every 1 sec " +
                "insert expired events into uniqueIps ;";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        log.info("Running : " + siddhiAppRuntime.getName());

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timestamp, inEvents, removeEvents);
                if (removeEvents != null) {
                    count += removeEvents.length;
                } else {
                    AssertJUnit.fail("inEvents emitted");
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("LoginEvents");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.3"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.9"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.30"});
        Thread.sleep(1100);

        AssertJUnit.assertEquals("Event arrived", true, eventArrived);
        AssertJUnit.assertEquals("Number of output event value", 4, count);

        siddhiAppRuntime.shutdown();

    }

    @Test
    public void testTimeOutputRateLimitQuery10() throws InterruptedException {
        log.info("TimeOutputRateLimit test10");

        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@app:name('EventOutputRateLimitTest10') " +
                "" +
                "define stream LoginEvents (timestamp long, ip string);" +
                "" +
                "@info(name = 'query1') " +
                "from LoginEvents#window.lengthBatch(2) " +
                "select ip, count(*) as total " +
                "group by ip " +
                "output first every 1 sec " +
                "insert expired events into uniqueIps ;";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        log.info("Running : " + siddhiAppRuntime.getName());

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timestamp, inEvents, removeEvents);
                if (removeEvents != null) {
                    count += removeEvents.length;
                } else {
                    AssertJUnit.fail("inEvents emitted");
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("LoginEvents");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.3"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.9"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.30"});
        Thread.sleep(1100);

        AssertJUnit.assertEquals("Event arrived", true, eventArrived);
        AssertJUnit.assertEquals("Number of output event value", 4, count);

        siddhiAppRuntime.shutdown();

    }

    @Test
    public void testTimeOutputRateLimitQuery11() throws InterruptedException {
        log.info("TimeOutputRateLimit test11");

        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@app:name('EventOutputRateLimitTest11') " +
                "" +
                "define stream LoginEvents (timestamp long, ip string);" +
                "" +
                "@info(name = 'query1') " +
                "from LoginEvents#window.lengthBatch(2) " +
                "select ip, count(*) as total " +
                "group by ip " +
                "output first every 1 sec " +
                "insert all events into uniqueIps ;";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        log.info("Running : " + siddhiAppRuntime.getName());

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timestamp, inEvents, removeEvents);
                if (inEvents != null) {
                    inEventCount += inEvents.length;
                }
                if (removeEvents != null) {
                    removeEventCount += removeEvents.length;
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("LoginEvents");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.3"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.9"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.30"});
        Thread.sleep(1100);

        AssertJUnit.assertEquals("Event arrived", true, eventArrived);
        AssertJUnit.assertEquals("Number of output in event value", 5, inEventCount);
        AssertJUnit.assertEquals("Number of output remove event value", 2, removeEventCount);

        siddhiAppRuntime.shutdown();

    }

    @Test
    public void testTimeOutputRateLimitQuery12() throws InterruptedException {
        log.info("TimeOutputRateLimit test12");

        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@app:name('EventOutputRateLimitTest11') " +
                "" +
                "define stream LoginEvents (timestamp long, ip string);" +
                "" +
                "@info(name = 'query1') " +
                "from LoginEvents#window.lengthBatch(2) " +
                "select ip, count(*) as total " +
                "group by ip " +
                "output last every 1 sec " +
                "insert all events into uniqueIps ;";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        log.info("Running : " + siddhiAppRuntime.getName());

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timestamp, inEvents, removeEvents);
                if (inEvents != null) {
                    inEventCount += inEvents.length;
                }
                if (removeEvents != null) {
                    removeEventCount += removeEvents.length;
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("LoginEvents");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.3"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.9"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.30"});
        Thread.sleep(1100);

        AssertJUnit.assertEquals("Event arrived", true, eventArrived);
        AssertJUnit.assertEquals("Number of output in event value", 4, inEventCount);
        AssertJUnit.assertEquals("Number of output remove event value", 3, removeEventCount);

        siddhiAppRuntime.shutdown();

    }

    @Test
    public void testTimeOutputRateLimitQuery13() throws InterruptedException {
        log.info("TimeOutputRateLimit test13");

        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@app:name('EventOutputRateLimitTest11') " +
                "" +
                "define stream LoginEvents (timestamp long, ip string);" +
                "" +
                "@info(name = 'query1') " +
                "from LoginEvents#window.lengthBatch(2) " +
                "select ip, count(*) as total " +
                "group by ip " +
                "output all every 1 sec " +
                "insert all events into uniqueIps ;";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        log.info("Running : " + siddhiAppRuntime.getName());

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timestamp, inEvents, removeEvents);
                if (inEvents != null) {
                    inEventCount += inEvents.length;
                }
                if (removeEvents != null) {
                    removeEventCount += removeEvents.length;
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("LoginEvents");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.3"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.9"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.30"});
        Thread.sleep(1100);

        AssertJUnit.assertEquals("Event arrived", true, eventArrived);
        AssertJUnit.assertEquals("Number of output in event value", 6, inEventCount);
        AssertJUnit.assertEquals("Number of output remove event value", 3, removeEventCount);

        siddhiAppRuntime.shutdown();

    }

    @Test
    public void testTimeOutputRateLimitQuery14() throws InterruptedException {
        log.info("TimeOutputRateLimit test14");

        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@app:name('EventOutputRateLimitTest5') " +
                "" +
                "define stream LoginEvents (timestamp long, ip string, symbol string);" +
                "@info(name = 'query1') " +
                "partition with (symbol of LoginEvents) " +
                "begin " +
                "from LoginEvents " +
                "select ip " +
                "group by symbol " +
                "output last every 1 sec " +
                "insert into uniqueIps; " +
                "end;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        log.info("Running : " + siddhiAppRuntime.getName());

        siddhiAppRuntime.addCallback("uniqueIps", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events);
                count += events.length;
                AssertJUnit.assertTrue("192.10.1.3".equals(events[0].getData(0))
                        || "192.10.1.4".equals(events[0].getData(0))
                        || "192.10.1.30".equals(events[0].getData(0)));
                eventArrived = true;
            }
        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("LoginEvents");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5", "WSO2"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.3", "WSO2"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.9", "WSO2"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4", "WSO2"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.30", "WSO2"});
        Thread.sleep(1000);

        AssertJUnit.assertEquals("Event arrived", true, eventArrived);
        AssertJUnit.assertTrue("Number of output event value", 3 == count);

        siddhiAppRuntime.shutdown();

    }

    @Test(dependsOnMethods = "testTimeOutputRateLimitQuery14")
    public void testTimeOutputRateLimitQuery15() throws InterruptedException {
        log.info("TimeOutputRateLimit test15");

        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@app:name('EventOutputRateLimitTest15') " +
                "" +
                "define stream LoginEvents (timestamp long, ip string);" +
                "" +
                "@info(name = 'query1') " +
                "from LoginEvents " +
                "select ip, count(*) as total " +
                "output first every 1 sec " +
                "insert all events into uniqueIps ;";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        log.info("Running : " + siddhiAppRuntime.getName());

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timestamp, inEvents, removeEvents);
                if (inEvents != null) {
                    inEventCount += (inEvents.length);
                }
                if (removeEvents != null) {
                    removeEventCount += (removeEvents.length);
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("LoginEvents");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.3"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.9"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});

        AssertJUnit.assertEquals("Event arrived", true, eventArrived);
        AssertJUnit.assertEquals("Number of output in event value", 1, inEventCount);
        AssertJUnit.assertEquals("Number of output remove event value", 0, removeEventCount);

        siddhiAppRuntime.shutdown();

    }

    @Test//(dependsOnMethods = "testTimeOutputRateLimitQuery15")
    public void testTimeOutputRateLimitQuery16() throws InterruptedException {
        log.info("TimeOutputRateLimit test16");

        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@app:name('EventOutputRateLimitTest16') " +
                "" +
                "define stream LoginEvents (timestamp long, ip string);" +
                "" +
                "@info(name = 'query1') " +
                "from LoginEvents " +
                "select ip, count(*) as total " +
                "group by ip " +
                "output first every 1 sec " +
                "insert all events into uniqueIps ;";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        log.info("Running : " + siddhiAppRuntime.getName());

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timestamp, inEvents, removeEvents);
                if (inEvents != null) {
                    inEventCount += inEvents.length;
                }
                if (removeEvents != null) {
                    removeEventCount += removeEvents.length;
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("LoginEvents");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.3"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.9"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});

        AssertJUnit.assertEquals("Event arrived", true, eventArrived);
        AssertJUnit.assertEquals("Number of output in event value", 4, inEventCount);
        AssertJUnit.assertEquals("Number of output remove event value", 0, removeEventCount);

        siddhiAppRuntime.shutdown();

    }

    @Test(dependsOnMethods = "testTimeOutputRateLimitQuery16")
    public void testTimeOutputRateLimitQuery17() throws InterruptedException {
        log.info("TimeOutputRateLimit test17");

        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@app:name('EventOutputRateLimitTest11') " +
                "" +
                "define stream LoginEvents (timestamp long, ip string);" +
                "" +
                "@info(name = 'query1') " +
                "from LoginEvents#window.lengthBatch(2) " +
                "select ip, count(*) as total " +
                "group by ip " +
                "output first every 1 sec " +
                "insert into uniqueIps ;";


        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        log.info("Running : " + siddhiAppRuntime.getName());

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timestamp, inEvents, removeEvents);
                if (inEvents != null) {
                    inEventCount += inEvents.length;
                }
                if (removeEvents != null) {
                    removeEventCount += removeEvents.length;
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("LoginEvents");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.3"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.9"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        Thread.sleep(1100);
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.4"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.5"});
        inputHandler.send(new Object[]{System.currentTimeMillis(), "192.10.1.30"});
        Thread.sleep(1100);

        AssertJUnit.assertEquals("Event arrived", true, eventArrived);
        AssertJUnit.assertEquals("Number of output in event value", 6, inEventCount);
        AssertJUnit.assertEquals("Number of output remove event value", 0, removeEventCount);

        siddhiAppRuntime.shutdown();

    }


}
