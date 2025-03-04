# Siddhi Streaming SQL Guide 4.0

## Introduction

Siddhi Streaming SQL is designed to process event streams in a streaming manner, detect complex event occurrences, 
and notify them in real-time. 

## Siddhi Application
Streaming processing and Complex Event Processing rules can be written is Siddhi Streaming SQL and they can be put 
together as a `SiddhiApp` in a single file. 

**Purpose**

Each Siddhi Application is an isolated processing unit that allows you to deploy and execute queries independent of other Siddhi applications in the system.

The following diagram depicts how **event flows** work with some of the key Siddhi Streaming SQL elements 
of the Siddhi Application.

![Event Flow](../images/event-flow.png?raw=true "Event Flow")

Below table provides brief description of a few key elements in the Siddhi Streaming SQL Language.

| Elements     | Description |
| ------------- |-------------|
| Stream    | A logical series of events ordered in time with a uniquely identifiable name, and set of defined attributes with specific data types defining its schema. |
| Event     | An event is associated with only one stream, and all events of that stream have an identical set of attributes that are assigned specific types (or the same schema). An event contains a timestamp and set of attribute values according to the schema.|
| Table     | A structured representation of data stored with a defined schema. Stored data can be backed by `In-Memory`, `RDBMs`, `MongoDB`, etc. to be accessed and manipulated at runtime.
| Query	    | A logical construct that processes events in streaming manner by combining existing streams and/or tables, and generates events to an output stream or table. A query consumes one or more input streams, and zero or one table. Then it processes these events in a streaming manner and publishes the output events to streams or tables for further processing or to generate notifications. 
| Source    | A contract that consumes data from external sources (such as `TCP`, `Kafka`, `HTTP`, etc)in the form of events, then converts each event (which can be in `XML`, `JSON`, `binary`, etc. format) to a Siddhi event, and passes that to a Stream for processing.
| Sink      | A contract that takes events arriving at a stream, maps them to a predefined data format (such as `XML`, `JSON`, `binary`, etc), and publishes them to external endpoints (such as `E-mail`, `TCP`, `Kafka`, `HTTP`, etc).
| Input Handler | A mechanism to programmatically inject events into streams. |
| Stream/Query Callback | A mechanism to programmatically consume output events from streams and queries. |
| Partition	| A logical container that isolates the processing of queries based on partition keys. Here, a separate instance of queries is generated for each partition key to achieve isolation. 
| Inner Stream | A positionable stream that connects portioned queries within their partitions, preserving isolation.  

**Grammar**

An element of Siddhi SQL can be composed together as a script in a Siddhi application, Here each construct must be separated 
by a semicolon `( ; )` as shown in the below syntax. 

```
<siddhi app>  : 
        <app annotation> * 
        ( <stream definition> | <table definition> | ... ) + 
        ( <query> | <partition> ) +
        ;
```

**Example**
Siddhi Application named `Temperature-Analytics` defined with a stream named `TempStream` and a query 
named `5minAvgQuery` for processing it.

```sql
@app:name('Temperature-Analytics')

define stream TempStream (deviceID long, roomNo int, temp double);

@name('5minAvgQuery')
from TempStream#window.time(5 min)
select roomNo, avg(temp) as avgTemp
  group by roomNo
insert into OutputStream;
```

## Stream
A stream is a logical series of events ordered in time. Its schema is defined via the **stream definition**.
A stream definition contains a unique name and a set of attributes with specific types and uniquely identifiable names within the stream.
All the events that are selected to be received into a specific stream have the same schema (i.e., have the same attributes in the same order). 

**Purpose**

By defining a schema it unifies common types of events together. This enables them to be processed via queries using their defined attributes in a streaming manner, and allow sinks and sources to map events to/from various data formats.

**Syntax**

The syntax for defining a new stream is as follows.
```sql
define stream <stream name> (<attribute name> <attribute type>, <attribute name> <attribute type>, ... );
```
The following parameters are configured in a stream definition.

| Parameter     | Description |
| ------------- |-------------|
| `stream name`      | The name of the stream created. (It is recommended to define a stream name in `PascalCase`.) |
| `attribute name`   | The schema of an stream is defined by its attributes with uniquely identifiable attribute names. (It is recommended to define attribute names in `camelCase`.)|    |
| `attribute type`   | The type of each attribute defined in the schema. <br/> This can be `STRING`, `INT`, `LONG`, `DOUBLE`, `FLOAT`, `BOOL` or `OBJECT`.     |

To use and refer stream and attribute names that do not follow `[a-zA-Z_][a-zA-Z_0-9]*` format enclose them in ``. E.g. ``` `$test(0)` ```.
 
To make the stream process events in asynchronous and multi-threading manner use the `@Async` annotation as shown in 
[Threading and Asynchronous](https://wso2.github.io/siddhi/documentation/siddhi-4.0/#threading-and-asynchronous) configuration section.

**Example**
```sql
define stream TempStream (deviceID long, roomNo int, temp double);
```
The above creates a stream named `TempStream` with the following attributes.

+ `deviceID` of type `long`
+ `roomNo` of type `int` 
+ `temp` of type `double` 

### Source
Sources receive events via multiple transports and in various data formats, and direct them into streams for processing.

A source configuration allows you to define a mapping in order to convert each incoming event from its native data format to a Siddhi event. When customizations to such mappings are not provided, Siddhi assumes that the arriving event adheres to the predefined format based on the stream definition and the selected message format. </br>

**Purpose**

Source allows Siddhi to consume events from external systems, and map the events to adhere to the associated stream. 

**Syntax**

To configure a stream that consumes events via a source, add the source configuration to a stream definition by adding the `@source` annotation with the required parameter values. 
The source syntax is as follows:
```sql
@source(type='source_type', static.option.key1='static_option_value1', static.option.keyN='static_option_valueN',
    @map(type='map_type', static.option_key1='static_option_value1', static.option.keyN='static_option_valueN',
        @attributes( attributeN='attribute_mapping_N', attribute1='attribute_mapping_1')
    )
)
define stream StreamName (attribute1 Type1, attributeN TypeN);
```
This syntax includes the following annotations.
**Source**

The `type` parameter of `@source` defines the source type that receives events. The other parameters to be configured 
depends on the source type selected, some of the the parameters are optional. </br>

For detailed information about the parameters see the documentation for the relevant source.

The following is the list of source types that are currently supported:

* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-http/">HTTP</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-kafka/">Kafka</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-tcp/">TCP</a>
* <a target="_blank" href="https://wso2.github.io/siddhi/api/latest/#inmemory-source">In-memory</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-wso2event/">WSO2Event</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-email/">Email</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-jms/">JMS</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-file/">File</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-rabbitmq/">RabbitMQ</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-mqtt/">MQTT</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-websocket/">WebSocket</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-twitter/">Twitter</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-sqs/">Amazon SQS</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-cdc/">CDC</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-prometheus/">Prometheus</a>

#### Source Mapper

Each `@source` configuration has a mapping denoted by the `@map` annotation that converts the incoming messages format to Siddhi events.

The `type` parameter of the `@map` defines the map type to be used to map the data. The other parameters to be 
configured depends on the mapper selected. Some of these parameters are optional. </br>
For detailed information about the parameters see the documentation for the relevant mapper.

!!! tip 
    When the `@map` annotation is not provided, `@map(type='passThrough')` is used as default. This default mapper type can be used when source consumes Siddhi events and when it does not need any mappings.
    

**Map Attributes**

`@attributes` is an optional annotation used with `@map` to define custom mapping. When `@attributes` is not provided, each mapper
assumes that the incoming events  adhere to its own default data format. By adding the `@attributes` annotation, you 
can configure mappers to extract data from the incoming message selectively, and assign them to attributes. 

There are two ways you can configure map attributes. 

1. Defining attributes as keys and mapping content as values in the following format: <br/>
```@attributes( attributeN='mapping_N', attribute1='mapping_1')``` 
2. Defining the mapping content of all attributes in the same order as how the attributes are defined in stream definition: <br/>
```@attributes( 'mapping_1', 'mapping_N')``` 

**Supported Mapping Types**

The following is a list of currently supported source mapping types:

* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-map-wso2event/">WSO2Event</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-map-xml/">XML</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-map-text/">TEXT</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-map-json/">JSON</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-map-binary/">Binary</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-map-keyvalue/">Key Value</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-map-csv/">CSV</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-map-avro/">Avro</a>

**Example**

This query receives events via the `HTTP` source in the `JSON` data format, and directs them to the `InputStream` stream for processing. 
Here the HTTP source is configured to receive events on all network interfaces on the `8080`port, on the `foo` context, and 
it is secured via basic authentication.

```sql
@source(type='http', receiver.url='http://0.0.0.0:8080/foo', basic.auth.enabled='true', 
  @map(type='json'))
define stream InputStream (name string, age int, country string);
```
### Sink

Sinks publish events from the streams via multiple transports to external endpoints in various data formats.

A sink configuration allows you to define a mapping to convert the Siddhi event to the required output data format (such as `JSON`, `TEXT`, `XML`, etc.).
When customization to such mappings is not provided, Siddhi converts events to its default format based on the stream definition and 
the selected data format to publish the events.

**Purpose**

Sinks provide a way to publish Siddhi events to external systems in the preferred data format. 

**Syntax**

To configure a stream to publish events via a sink, add the sink configuration to a stream definition by adding the `@sink` 
annotation with the required parameter values. The sink syntax is as follows:

```sql
@sink(type='sink_type', static_option_key1='static_option_value1', dynamic_option_key1='{{dynamic_option_value1}}',
    @map(type='map_type', static_option_key1='static_option_value1', dynamic_option_key1='{{dynamic_option_value1}}',
        @payload('payload_mapping')
    )
)
define stream StreamName (attribute1 Type1, attributeN TypeN);
```

!!! Note "Dynamic Properties" 
    The sink and sink mapper properties that are categorized as `dynamic` have the ability to absorb attributes values 
    from their associated streams. This can be done by using the attribute names in double curly braces as `{{...}}` when configuring the property value. 
    
    Some valid dynamic properties values are: 
    
    * `'{{attribute1}}'`
    * `'This is {{attribute1}}'` 
    * `{{attribute1}} > {{attributeN}}`  
    
    Here the attribute names in the double curly braces will be replaced with event values during execution. 

This syntax includes the following annotations.

**Sink**

The `type` parameter of the `@sink` annotation defines the sink type that publishes the events. The other parameters to be configured 
depends on the sink type selected. Some of these parameters are optional, and some can have dynamic values. </br>

For detailed information about the parameters see documentation for the relevant sink.

The following is a list of currently supported sink types.

* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-http/">HTTP</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-kafka/">Kafka</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-tcp/">TCP</a>
* <a target="_blank" href="https://wso2.github.io/siddhi/api/latest/#inmemory-sink">In-memory</a>  
* <a target="_blank" href="https://wso2.github.io/siddhi/api/latest/#log-sink">Log</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-wso2event/">WSO2Event</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-email/">Email</a> 
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-jms/">JMS</a> 
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-file/">File</a> 
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-rabbitmq/">RabbitMQ</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-mqtt/">MQTT</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-websocket/">WebSocket</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-sqs/">Amazon SQS</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-io-prometheus/">Prometheus</a>

#### Distributed Sink

Distributed Sinks publish events from a defined stream to multiple destination endpoints using load balancing and partitioning strategies.

Any ordinary sink can be used as a distributed sink. A distributed sink configuration allows you to define a common mapping to convert the Siddhi events for all its destination endpoints, 
allows you to define a distribution strategy (e.g. `roundRobin`, `partitioned`), and configuration for each specific endpoint destination. 

**Purpose**

Distributed sinks provide a way to publish Siddhi events to multiple destination endpoints in the preferred data format. 

**Syntax**

To configure a stream to publish events via a distributed sink, add the sink configuration to a stream definition by adding the `@sink` 
annotation and add the configuration parameters that are common of all the destination endpoints inside the `@sink` annotation 
along with `@distribution` and `@destination` annotations providing distribution strategy and endpoint specific configurations. 
The distributed sink syntax is as follows:

*RoundRobin Distributed Sink*

Publishes events to defined destinations in a round robin manner. 

```sql
@sink(type='sink_type', common_option_key1='common_option_value1', common_option_key2='{{common_option_value1}}',
    @map(type='map_type', option_key1='option_value1', option_key2='{{option_value1}}',
        @payload('payload_mapping')
    )
    @distribution(strategy='roundRobin',
        @destination(specific_option_key1='specific_option_value1'),
        @destination(specific_option_key1='specific_option_value2')))
)
define stream StreamName (attribute1 Type1, attributeN TypeN);
```

*Partitioned Distributed Sink*

Publishes events to defined destinations by partitioning based on the hashcode of the events partition key. 

```sql
@sink(type='sink_type', common_option_key1='common_option_value1', common_option_key2='{{common_option_value1}}',
    @map(type='map_type', option_key1='option_value1', option_key2='{{option_value1}}',
        @payload('payload_mapping')
    )
    @distribution(strategy='partitioned', partitionKey='partition_key',
        @destination(specific_option_key1='specific_option_value1'),
        @destination(specific_option_key1='specific_option_value2')))
)
define stream StreamName (attribute1 Type1, attributeN TypeN);
```

#### Sink Mapper

Each `@sink` annotation has a mapping denoted by the  `@map` annotation that converts the Siddhi event to an outgoing message format.

The `type` parameter of the `@map` annotation defines the map type based on which the event is mapped. The other parameters to be configured depends on the mapper selected. Some of these parameters are optional and some have dynamic values. </br> 

For detailed information about the parameters see the documentation for the relevant mapping type.

!!! tip 
    When the `@map` annotation is not provided, `@map(type='passThrough')` is used by default. This can be used when the sink publishes in the Siddhi event format, or when it does not need any mappings.

**Map Payload**

`@payload` is an optional annotation used with the `@map` annotation to define a custom mapping. When the `@payload` annotation is not provided, each mapper
maps the outgoing events to its own default data format. By defining the `@payload` annotation you can configure mappers to produce the output payload with attribute names of your choice, using dynamic properties by selectively assigning 
the attributes in your preferred format. 

There are two ways you can configure the `@payload` annotation. 

1. Some mappers such as `XML`, `JSON`, and `Test` accept only one output payload using the following format: <br/>
```@payload( 'This is a test message from {{user}}.' )``` 
2. Some mappers such `key-value` accept series of mapping values defined as follows: <br/>
```@payload( key1='mapping_1', 'key2'='user : {{user}}')``` <br/>
Here, apart from the dotted key names sush as ```a.b.c```, any constant string value such as ```'$abc'``` can also by used as a key. 

**Supported Mapping Types**

The following is a list of currently supported sink mapping types:

* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-map-wso2event/">WSO2Event</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-map-xml/">XML</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-map-text/">TEXT</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-map-json/">JSON</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-map-binary/">Binary</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-map-keyvalue/">Key Value</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-map-csv/">CSV</a>
* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-map-avro/">Avro</a>


**Example**

Following query publishes events from the `OutputStream` stream to an `HTTP` endpoint. Here the events are mapped to the default `JSON` payloads and sent to `http://localhost:8005/endpoint`
 using the `POST` method, with the`Accept` header, and secured via basic authentication where `admin` is both the username and the password.
```sql
@sink(type='http', publisher.url='http://localhost:8005/endpoint', method='POST', headers='Accept-Date:20/02/2017', 
  basic.auth.username='admin', basic.auth.password='admin', basic.auth.enabled='true',
  @map(type='json'))
define stream OutputStream (name string, ang int, country string);
```

Following query publishes events from the `OutputStream` stream to multiple the `HTTP` endpoints using partitioning strategy. Here the events sent to either `http://localhost:8005/endpoint1`
or `http://localhost:8006/endpoint2` based on the partitioning key `country`. It uses default `JSON` mapping, `POST` method, and used `admin` as both the username and the password when publishing to both the endpoints.
```sql
@sink(type='http', method='POST', basic.auth.username='admin', basic.auth.password='admin', 
  basic.auth.enabled='true', @map(type='json'),
  @distribution(strategy='partitioned', partitionKey='country',
     @destination(publisher.url='http://localhost:8005/endpoint1'),
     @destination(publisher.url='http://localhost:8006/endpoint2')))
define stream OutputStream (name string, ang int, country string);
```

## Query

Each Siddhi query can consume one or more streams, and 0-1 tables, process the events in a streaming manner, and then generate an
 output event to a stream or perform a CRUD operation to a table.

**Purpose**

A query enables you to perform complex event processing and stream processing operations by processing incoming events one by one in the order they arrive.

**Syntax**

All queries contain an input and an output section. Some also contain a projection section. A simple query with all three sections is as follows.

```sql
from <input stream> 
select <attribute name>, <attribute name>, ...
insert into <output stream/table>
```
**Example**

This query included in a Siddhi Application consumes events from the `TempStream` stream (that is already defined) and outputs the room temperature and the room number to the `RoomTempStream` stream.

```sql
define stream TempStream (deviceID long, roomNo int, temp double);

from TempStream 
select roomNo, temp
insert into RoomTempStream;
```
!!! tip "Inferred Stream"
    Here, the `RoomTempStream` is an inferred Stream, which means it can be used as any other defined stream 
    without explicitly defining its stream definition. The definition of the `RoomTempStream` is inferred from the 
    first query that produces the stream.  

###Query Projection

Siddhi queries supports the following for query projections.

<table style="width:100%">
    <tr>
        <th>Action</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Selecting required objects for projection</td>
        <td>This involves selecting only some of the attributes from the input stream to be inserted into an output stream.
            <br><br>
            E.g., The following query selects only the `roomNo` and `temp` attributes from the `TempStream` stream.
            <pre style="align:left">from TempStream<br>select roomNo, temp<br>insert into RoomTempStream;</pre>
        </td>
    </tr>
    <tr>
        <td>Selecting all attributes for projection</td>
        <td>Selecting all the attributes in an input stream to be inserted into an output stream. This can be done by using asterisk ( * ) or by omitting the `select` statement.
            <br><br>
            E.g., Both the following queries select all the attributes in the `NewTempStream` stream.
            <pre>from TempStream<br>select *<br>insert into NewTempStream;</pre>
            or
            <pre>from TempStream<br>insert into NewTempStream;</pre>
        </td>
    </tr>
    <tr>
        <td>Renaming attributes</td>
        <td>This selects attributes from the input streams and inserts them into the output stream with different names.
            <br><br>
            E.g., This query renames `roomNo` to `roomNumber` and `temp` to `temperature`.
            <pre>from TempStream <br>select roomNo as roomNumber, temp as temperature<br>insert into RoomTempStream;</pre>
        </td>
    </tr>
    <tr>
        <td>Introducing the constant value</td>
        <td>This adds constant values by assigning it to an attribute using `as`.
            <br></br>
            E.g., This query specifies 'C' to be used as the constant value for `scale` attribute. 
            <pre>from TempStream<br>select roomNo, temp, 'C' as scale<br>insert into RoomTempStream;</pre>
        </td>
    </tr>
    <tr>
        <td>Using mathematical and logical expressions</td>
        <td>This uses attributes with mathematical and logical expressions in the precedence order given below, and assigns them to the output attribute using `as`.
            <br><br>
            <b>Operator precedence</b><br>
            <table style="width:100%">
                <tr>
                    <th>Operator</th>
                    <th>Distribution</th>
                    <th>Example</th>
                </tr>
                <tr>
                    <td>
                        ()
                    </td>
                    <td>
                        Scope
                    </td>
                    <td>
                        <pre>(cost + tax) * 0.05</pre>
                    </td>
                </tr>
                <tr>
                    <td>
                         IS NULL
                    </td>
                    <td>
                        Null check
                    </td>
                    <td>
                        <pre>deviceID is null</pre>
                    </td>
                </tr>
                <tr>
                    <td>
                        NOT
                    </td>
                    <td>
                        Logical NOT
                    </td>
                    <td>
                        <pre>not (price > 10)</pre>
                    </td>
                </tr>
                <tr>
                    <td>
                         *   /   %  
                    </td>
                    <td>
                        Multiplication, division, modulo
                    </td>
                    <td>
                        <pre>temp * 9/5 + 32</pre>
                    </td>
                </tr>
                <tr>
                    <td>
                        +   -  
                    </td>
                    <td>
                        Addition, substraction
                    </td>
                    <td>
                        <pre>temp * 9/5 - 32</pre>
                    </td>
                </tr>
                <tr>
                    <td>
                        <   <=   >   >=
                    </td>
                    <td>
                        Comparators: less-than, greater-than-equal, greater-than, less-than-equal
                    </td>
                    <td>
                        <pre>totalCost >= price * quantity</pre>
                    </td>
                </tr>
                <tr>
                    <td>
                        ==   !=  
                    </td>
                    <td>
                        Comparisons: equal, not equal
                    </td>
                    <td>
                        <pre>totalCost !=  price * quantity</pre>
                    </td>
                </tr>
                <tr>
                    <td>
                        IN
                    </td>
                    <td>
                        Contains in table
                    </td>
                    <td>
                        <pre>roomNo in ServerRoomsTable</pre>
                    </td>
                </tr>
                <tr>
                    <td>
                        AND
                    </td>
                    <td>
                        Logical AND
                    </td>
                    <td>
                        <pre>temp < 40 and (humidity < 40 or humidity >= 60)</pre>
                    </td>
                </tr>
                <tr>
                    <td>
                        OR
                    </td>
                    <td>
                        Logical OR
                    </td>
                    <td>
                        <pre>temp < 40 or (humidity < 40 and humidity >= 60)</pre>
                    </td>
                </tr>
            </table>
            E.g., Converting Celsius to Fahrenheit and identifying rooms with room number between 10 and 15 as server rooms.
            <pre>from TempStream<br>select roomNo, temp * 9/5 + 32 as temp, 'F' as scale, roomNo > 10 and roomNo < 15 as isServerRoom<br>insert into RoomTempStream;</pre>       
    </tr>
    
</table>

###Function

A function consumes zero, one or more parameters and always produces a result value. It can be used in any location where
 an attribute can be used. 

**Purpose**

Functions encapsulates complex execution logic that makes Siddhi applications simple and easy to understand. 

**Function Parameters**

Functions parameters can be attributes, constant values, results of other functions, results of mathematical or logical expressions or time parameters. 
Function parameters vary depending on the function being called.

Time is a special parameter that can be defined using the integer time value followed by its unit as `<int> <unit>`. 
Following are the supported unit types. Upon execution, time returns the value in the scale of milliseconds as a long value. 

<table style="width:100%">
    <tr>
        <th>
            Unit  
        </th>
        <th>
            Syntax
        </th>
    </tr>
    <tr>
        <td>
            Year
        </td>
        <td>
            year | years
        </td>
    </tr>
    <tr>
        <td>
            Month
        </td>
        <td>
            month | months
        </td>
    </tr>
    <tr>
        <td>
            Week
        </td>
        <td>
            week | weeks
        </td>
    </tr>
    <tr>
        <td>
            Day
        </td>
        <td>
            day | days
        </td>
    </tr>
    <tr>
        <td>
            Hour
        </td>
        <td>
           hour | hours
        </td>
    </tr>
    <tr>
        <td>
           Minutes
        </td>
        <td>
           minute | minutes | min
        </td>
    </tr>
    <tr>
        <td>
           Seconds
        </td>
        <td>
           second | seconds | sec
        </td>
    </tr>
    <tr>
        <td>
           Milliseconds
        </td>
        <td>
           millisecond | milliseconds
        </td>
    </tr>
</table>

E.g. Passing 1 hour and 25 minutes to `test()` function.

<pre>test(1 hour 25 min)</pre>

!!! note
    Functions, mathematical expressions, and logical expressions can be used in a nested manner.

Following are some inbuilt functions shipped with Siddhi, for more functions refer execution <a target="_blank" href="https://wso2.github.io/siddhi/extensions/">extensions</a>.

+ [eventTimestamp](https://wso2.github.io/siddhi/api/latest/#eventtimestamp-function)
+ [log](https://wso2.github.io/siddhi/api/latest/#log-stream-processor)
+ [UUID](https://wso2.github.io/siddhi/api/latest/#uuid-function)
+ [default](https://wso2.github.io/siddhi/api/latest/#default-function)
+ [cast](https://wso2.github.io/siddhi/api/latest/#cast-function)
+ [convert](https://wso2.github.io/siddhi/api/latest/#convert-function)
+ [ifThenElse](https://wso2.github.io/siddhi/api/latest/#ifthenelse-function)
+ [minimum](https://wso2.github.io/siddhi/api/latest/#minimum-function)
+ [maximum](https://wso2.github.io/siddhi/api/latest/#maximum-function)
+ [coalesce](https://wso2.github.io/siddhi/api/latest/#coalesce-function)
+ [instanceOfBoolean](https://wso2.github.io/siddhi/api/latest/#instanceofboolean-function)
+ [instanceOfDouble](https://wso2.github.io/siddhi/api/latest/#instanceofdouble-function)
+ [instanceOfFloat](https://wso2.github.io/siddhi/api/latest/#instanceoffloat-function)
+ [instanceOfInteger](https://wso2.github.io/siddhi/api/latest/#instanceofinteger-function)
+ [instanceOfLong](https://wso2.github.io/siddhi/api/latest/#instanceoflong-function)
+ [instanceOfString](https://wso2.github.io/siddhi/api/latest/#instanceofstring-function)

**Example**

The following configuration converts the `roomNo` to `string` and adds a `messageID` to each event using the `convert` and `UUID` functions.
```sql
from TempStream
select convert(roomNo, 'string') as roomNo, temp, UUID() as messageID
insert into RoomTempStream;
```

### Filter

Filters are included in queries to filter information from input streams based on a specified condition.

**Purpose**

A filter allows you to separate events that match a specific condition as the output, or for further processing.

**Syntax**

Filter conditions should be defined in square brackets next to the input stream name as shown below.

```sql
from <input stream>[<filter condition>]
select <attribute name>, <attribute name>, ...
insert into <output stream>
```

**Example**

This query filters all server rooms of which the room number is within the range of 100-210, and having temperature greater than 40 degrees 
from the `TempStream` stream, and inserts the results into the `HighTempStream` stream.

```sql
from TempStream[(roomNo >= 100 and roomNo < 210) and temp > 40]
select roomNo, temp
insert into HighTempStream;
```

### Window

Windows allow you to capture a subset of events based on a specific criterion from an input stream for calculation. 
Each input stream can only have a maximum of one window.

**Purpose**

To create subsets of events within a stream based on time duration, number of events, etc for processing. 
A window can operate in a sliding or tumbling (batch) manner.

**Syntax**

The `#window` prefix should be inserted next to the relevant stream in order to use a window.

```sql
from <input stream>#window.<window name>(<parameter>, <parameter>, ... )
select <attribute name>, <attribute name>, ...
insert <event type> into <output stream>
```
!!! note 
    Filter condition can be applied both before and/or after the window
    
**Example**

If you want to identify the maximum temperature out of the last 10 events, you need to define a `length` window of 10 events.
 This window operates in a sliding mode where the following 3 subsets are calculated when a list of 12 events are received in a sequential order.

|Subset|Event Range|
|------|-----------|
| 1 | 1-10 |
| 2 | 2-11 |
|3| 3-12 |

The following query finds the maximum temperature out of **last 10 events** from the `TempStream` stream, 
and inserts the results into the `MaxTempStream` stream.

```sql
from TempStream#window.length(10)
select max(temp) as maxTemp
insert into MaxTempStream;
```

If you define the maximum temperature reading out of every 10 events, you need to define a `lengthBatch` window of 10 events.
This window operates as a batch/tumbling mode where the following 3 subsets are calculated when a list of 30 events are received in a sequential order.

|Subset|Event Range|
|------|-----------|
| 1    | 1-10      |
| 2    | 11-20     |
| 3    | 21-30     |

The following query finds the maximum temperature out of **every 10 events** from the `TempStream` stream, 
and inserts the results into the `MaxTempStream` stream.

```sql
from TempStream#window.lengthBatch(10)
select max(temp) as maxTemp
insert into MaxTempStream;
```

!!! note
    Similar operations can be done based on time via `time` windows and `timeBatch` windows and for others. 
    Code segments such as `#window.time(10 min)` considers events that arrive during the last 10 minutes in a sliding manner, and the `#window.timeBatch(2 min)` considers events that arrive every 2 minutes in a tumbling manner. 

Following are some inbuilt windows shipped with Siddhi. For more window types, see execution <a target="_blank" href="https://wso2.github.io/siddhi/extensions/">extensions</a>. 

* [time](https://wso2.github.io/siddhi/api/latest/#time-window)
* [timeBatch](https://wso2.github.io/siddhi/api/latest/#timebatch-window)
* [batch](https://wso2.github.io/siddhi/api/latest/#batch-window)
* [timeLength](https://wso2.github.io/siddhi/api/latest/#timelength-window)
* [length](https://wso2.github.io/siddhi/api/latest/#length-window)
* [lengthBatch](https://wso2.github.io/siddhi/api/latest/#lengthbatch-window)
* [sort](https://wso2.github.io/siddhi/api/latest/#sort-window)
* [frequent](https://wso2.github.io/siddhi/api/latest/#frequent-window)
* [lossyFrequent](https://wso2.github.io/siddhi/api/latest/#lossyfrequent-window)
* [session](https://wso2.github.io/siddhi/api/latest/#session-window)
* [cron](https://wso2.github.io/siddhi/api/latest/#cron-window)
* [externalTime](https://wso2.github.io/siddhi/api/latest/#externaltime-window)
* [externalTimeBatch](https://wso2.github.io/siddhi/api/latest/#externaltimebatch-window)
* [delay](https://wso2.github.io/siddhi/api/latest/#delay-window)

**Output event types**<a id="output-event-types" class='anchor' aria-hidden='true'></a> 

Projection of the query depends on the output event types such as, `current` and `expired` event types.
 By default all queries produce `current` events and only queries with windows produce `expired` events 
 when events expire from the window. You can specify whether the output of a query should be only current events, only expired events or both current and expired events.
 
 **Note!** Controlling the output event types does not alter the execution within the query, and it does not affect the accuracy of the query execution.  
 
 The following keywords can be used with the output stream to manipulate output. 
 
| Output event types | Description |
|-------------------|-------------|
| `current events` | Outputs events when incoming events arrive to be processed by the query. </br> This is default when no specific output event type is specified.|
| `expired events` | Outputs events when events expires from the window. |
| `all events` | Outputs events when incoming events arrive to be processed by the query as well as </br> when events expire from the window. |

The output event type keyword can be used between `insert` and `into` as shown in the following example.

**Example**

This query delays all events in a stream by 1 minute.  

```sql
from TempStream#window.time(1 min)
select *
insert expired events into DelayedTempStream
```

### Aggregate function

Aggregate functions perform aggregate calculations in the query. 
When a window is defined the aggregation is restricted within that window. If no window is provided aggregation is performed from the start of the Siddhi application.

**Syntax**

```sql
from <input stream>#window.<window name>(<parameter>, <parameter>, ... )
select <aggregate function>(<parameter>, <parameter>, ... ) as <attribute name>, <attribute2 name>, ...
insert into <output stream>;
```

**Aggregate Parameters**

Aggregate parameters can be attributes, constant values, results of other functions or aggregates, results of mathematical or logical expressions, or time parameters. 
Aggregate parameters configured in a query  depends on the aggregate function being called.

**Example**

The following query calculates the average value for the `temp` attribute of the `TempStream` stream. This calculation is done for the last 10 minutes in a sliding manner, and the result is output as `avgTemp` to the `AvgTempStream` output stream.

```sql
from TempStream#window.time(10 min)
select avg(temp) as avgTemp, roomNo, deviceID
insert into AvgTempStream;
```
Following are some inbuilt aggregation functions shipped with Siddhi, for more aggregation functions, see execution <a target="_blank" href="https://wso2.github.io/siddhi/extensions/">extensions</a>. 

+ [avg](http://wso2.github.io/siddhi/api/latest/#avg-aggregate-function)
+ [sum](http://wso2.github.io/siddhi/api/latest/#sum-aggregate-function)
+ [max](http://wso2.github.io/siddhi/api/latest/#max-aggregate-function)
+ [min](http://wso2.github.io/siddhi/api/latest/#min-aggregate-function)
+ [count](http://wso2.github.io/siddhi/api/latest/#count-aggregate-function)
+ [distinctCount](http://wso2.github.io/siddhi/api/latest/#distinctcount-aggregate-function)
+ [maxForever](http://wso2.github.io/siddhi/api/latest/#maxforever-aggregate-function)
+ [minForever](http://wso2.github.io/siddhi/api/latest/#minforever-aggregate-function)
+ [stdDev](http://wso2.github.io/siddhi/api/latest/#stddev-aggregate-function)

### Group By

Group By allows you to group the aggregate based on specified attributes.

**Syntax**
The syntax for the Group By aggregate function is as follows:

```sql
from <input stream>#window.<window name>(...)
select <aggregate function>( <parameter>, <parameter>, ...) as <attribute1 name>, <attribute2 name>, ...
group by <attribute1 name>, <attribute2 name> ...
insert into <output stream>;
```

**Example**
The following query calculates the average temperature per `roomNo` and `deviceID` combination, for events that arrive at the `TempStream` stream
for a sliding time window of 10 minutes.

```sql
from TempStream#window.time(10 min)
select avg(temp) as avgTemp, roomNo, deviceID
group by roomNo, deviceID
insert into AvgTempStream;
```

### Having

Having allows you to filter events after processing the `select` statement.

**Purpose**
This allows you to filter the aggregation output.

**Syntax**
The syntax for the Having clause is as follows:

```sql
from <input stream>#window.<window name>( ... )
select <aggregate function>( <parameter>, <parameter>, ...) as <attribute1 name>, <attribute2 name>, ...
group by <attribute1 name>, <attribute2 name> ...
having <condition>
insert into <output stream>;
```

**Example**

The following query calculates the average temperature per room for the last 10 minutes, and alerts if it exceeds 30 degrees.
```sql
from TempStream#window.time(10 min)
select avg(temp) as avgTemp, roomNo
group by roomNo
having avgTemp > 30
insert into AlertStream;
```

### Order By

Order By allows you to order the aggregated result in ascending and/or descending order based on specified attributes. By default ordering will be done in
ascending manner. User can use 'desc' keyword to order in descending manner.

**Syntax**
The syntax for the Order By clause is as follows:

```sql
from <input stream>#window.<window name>( ... )
select <aggregate function>( <parameter>, <parameter>, ...) as <attribute1 name>, <attribute2 name>, ...
group by <attribute1 name>, <attribute2 name> ...
having <condition>
order by <attribute1 name> (asc | desc)?, <attribute2 name> (<ascend/descend>)?, ...
insert into <output stream>;
```

**Example**

The following query calculates the average temperature per `roomNo` and `deviceID` combination for every 10 minutes, and generate output events
by ordering them in the ascending order of the room's avgTemp and then by the descending order of roomNo.

```sql
from TempStream#window.timeBatch(10 min)
select avg(temp) as avgTemp, roomNo, deviceID
group by roomNo, deviceID
order by avgTemp, roomNo desc
insert into AvgTempStream;
```

### Limit & Offset

When events are emitted as a batch, offset allows you to offset beginning of the output event batch and limit allows you to limit the number of events in the batch from the defined offset. 
With this users can specify which set of events need be emitted. 

**Syntax**
The syntax for the Limit & Offset clause is as follows:

```sql
from <input stream>#window.<window name>( ... )
select <aggregate function>( <parameter>, <parameter>, ...) as <attribute1 name>, <attribute2 name>, ...
group by <attribute1 name>, <attribute2 name> ...
having <condition>
order by <attribute1 name> (asc | desc)?, <attribute2 name> (<ascend/descend>)?, ...
limit <positive interger>?
offset <positive interger>?
insert into <output stream>;
```

Here both `limit` and `offset` are optional where `limit` by default output all the events and `offset` by default set to `0`.

**Example**
The following query calculates the average temperature per `roomNo` and `deviceID` combination, for events that arrive at the `TempStream` stream
for every 10 minutes and emits two events with highest average temperature.

```sql
from TempStream#window.timeBatch(10 min)
select avg(temp) as avgTemp, roomNo, deviceID
group by roomNo, deviceID
order by avgTemp desc
limit 2
insert into HighestAvgTempStream;
```

The following query calculates the average temperature per `roomNo` and `deviceID` combination, for events that arrive at the `TempStream` stream
for every 10 minutes and emits third, forth and fifth events when sorted in descending order based on their average temperature.

```sql
from TempStream#window.timeBatch(10 min)
select avg(temp) as avgTemp, roomNo, deviceID
group by roomNo, deviceID
order by avgTemp desc
limit 3
offset 2
insert into HighestAvgTempStream;
```

### Join (Stream) 
Joins allow you to get a combined result from two streams in real-time based on a specified condition. 

**Purpose**
Streams are stateless. Therefore, in order to join two streams, they need to be connected to a window so that there is a pool of events that can be used for joining. Joins also accept conditions to join the appropriate events from each stream.
 
During the joining process each incoming event of each stream is matched against all the events in the other 
stream's window based on the given condition, and the output events are generated for all the matching event pairs.

!!! Note
    Join can also be performed with [stored data](#join-table), [aggregation](#join-aggregation) or externally [defined windows](#join-window).

**Syntax**

The syntax for a join is as follows:

```sql
from <input stream>#window.<window name>(<parameter>, ... ) {unidirectional} {as <reference>}
         join <input stream>#window.<window name>(<parameter>,  ... ) {unidirectional} {as <reference>}
    on <join condition>
select <attribute name>, <attribute name>, ...
insert into <output stream>
```
Here, the `<join condition>` allows you to match the attributes from both the streams. 

**Unidirectional join operation**

By default, events arriving at either stream can trigger the joining process. However, if you want to control the 
join execution, you can add the `unidirectional` keyword next to a stream in the join definition as depicted in the 
syntax in order to enable that stream to trigger the join operation. Here, events arriving at other stream only update the 
 window of that stream, and this stream does not trigger the join operation.
 
!!! Note
    The `unidirectional` keyword cannot be applied to both the input streams because the default behaviour already allows both streams to trigger the join operation.

**Example**

Assuming that the temperature of regulators are updated every minute. 
Following is a Siddhi App that controls the temperature regulators if they are not already `on` for all the rooms with a room temperature greater than 30 degrees.  

```sql
define stream TempStream(deviceID long, roomNo int, temp double);
define stream RegulatorStream(deviceID long, roomNo int, isOn bool);
  
from TempStream[temp > 30.0]#window.time(1 min) as T
  join RegulatorStream[isOn == false]#window.length(1) as R
  on T.roomNo == R.roomNo
select T.roomNo, R.deviceID, 'start' as action
insert into RegulatorActionStream;
```

**Supported join types** 

Following are the supported operations of a join clause.

 *  **Inner join (join)** 

    This is the default behaviour of a join operation. `join` is used as the keyword to join both the streams. The output is generated only if there is a matching event in both the streams.

 *  **Left outer join** 

    The left outer join operation allows you to join two streams to be merged based on a condition. `left outer join` is used as the keyword to join both the streams.
   
    Here, it returns all the events of left stream even if there are no matching events in the right stream by 
    having null values for the attributes of the right stream.

     **Example**

    The following query generates output events for all events from the `StockStream` stream regardless of whether a matching 
    symbol exists in the `TwitterStream` stream or not.

    <pre>
    from StockStream#window.time(1 min) as S
      left outer join TwitterStream#window.length(1) as T
      on S.symbol== T.symbol
    select S.symbol as symbol, T.tweet, S.price
    insert into outputStream ;    </pre>

 *  **Right outer join** 

    This is similar to a left outer join. `Right outer join` is used as the keyword to join both the streams.
    It returns all the events of the right stream even if there are no matching events in the left stream. 

 *  **Full outer join** 

    The full outer join combines the results of left outer join and right outer join. `full outer join` is used as the keyword to join both the streams.
    Here, output event are generated for each incoming event even if there are no matching events in the other stream.

    **Example**

    The following query generates output events for all the incoming events of each stream regardless of whether there is a 
    match for the `symbol` attribute in the other stream or not.

    <pre>
    from StockStream#window.time(1 min) as S
      full outer join TwitterStream#window.length(1) as T
      on S.symbol== T.symbol
    select S.symbol as symbol, T.tweet, S.price
    insert into outputStream ;    </pre>



### Pattern

This is a state machine implementation that allows you to detect patterns in the events that arrive over time. This can correlate events within a single stream or between multiple streams. 

**Purpose** 

Patterns allow you to identify trends in events over a time period.

**Syntax**

The following is the syntax for a pattern query:

```sql
from (every)? <event reference>=<input stream>[<filter condition>] -> 
    (every)? <event reference>=<input stream [<filter condition>] -> 
    ... 
    (within <time gap>)?     
select <event reference>.<attribute name>, <event reference>.<attribute name>, ...
insert into <output stream>
```
| Items| Description |
|-------------------|-------------|
| `->` | This is used to indicate an event that should be following another event. The subsequent event does not necessarily have to occur immediately after the preceding event. The condition to be met by the preceding event should be added before the sign, and the condition to be met by the subsequent event should be added after the sign. |
| `<event reference>` | This allows you to add a reference to the the matching event so that it can be accessed later for further processing. |
| `(within <time gap>)?` | The `within` clause is optional. It defines the time duration within which all the matching events should occur. |
| `every` | `every` is an optional keyword. This defines whether the event matching should be triggered for every event arrival in the specified stream with the matching condition. <br/> When this keyword is not used, the matching is carried out only once. |

Siddhi also supports pattern matching with counting events and matching events in a logical order such as (`and`, `or`, and `not`). These are described in detail further below in this guide.

**Example**

This query sends an alert if the temperature of a room increases by 5 degrees within 10 min.

```sql
from every( e1=TempStream ) -> e2=TempStream[ e1.roomNo == roomNo and (e1.temp + 5) <= temp ]
    within 10 min
select e1.roomNo, e1.temp as initialTemp, e2.temp as finalTemp
insert into AlertStream;
```

Here, the matching process begins for each event in the `TempStream` stream (because `every` is used with `e1=TempStream`), 
and if  another event arrives within 10 minutes with a value for the `temp` attribute that is greater than or equal to `e1.temp + 5` 
of the event e1, an output is generated via the `AlertStream`.

####Counting Pattern

Counting patterns allow you to match multiple events that may have been received for the same matching condition.
The number of events matched per condition can be limited via condition postfixes.

**Syntax**

Each matching condition can contain a collection of events with the minimum and maximum number of events to be matched as shown in the syntax below. 

```sql
from (every)? <event reference>=<input stream>[<filter condition>] (<<min count>:<max count>>)? ->  
    ... 
    (within <time gap>)?     
select <event reference>([event index])?.<attribute name>, ...
insert into <output stream>
```

|Postfix|Description|Example
---------|---------|---------
|`<n1:n2>`|This matches `n1` to `n2` events (including `n1` and not more than `n2`).|`1:4` matches 1 to 4 events.
|`<n:>`|This matches `n` or more events (including `n`).|`<2:>` matches 2 or more events.
|`<:n>`|This matches up to `n` events (excluding `n`).|`<:5>` matches up to 5 events.
|`<n>`|This matches exactly `n` events.|`<5>` matches exactly 5 events.

Specific occurrences of the event in a collection can be retrieved by using an event index with its reference.
Square brackets can be used to indicate the event index where `1` can be used as the index of the first event and `last` can be used as the index
 for the `last` available event in the event collection. If you provide an index greater then the last event index,
 the system returns `null`. The following are some valid examples.

+ `e1[3]` refers to the 3rd event.
+ `e1[last]` refers to the last event.
+ `e1[last - 1]` refers to the event before the last event.

**Example**

The following Siddhi App calculates the temperature difference between two regulator events.

```sql
define stream TempStream (deviceID long, roomNo int, temp double);
define stream RegulatorStream (deviceID long, roomNo int, tempSet double, isOn bool);
  
from every( e1=RegulatorStream) -> e2=TempStream[e1.roomNo==roomNo]<1:> -> e3=RegulatorStream[e1.roomNo==roomNo]
select e1.roomNo, e2[0].temp - e2[last].temp as tempDiff
insert into TempDiffStream;
```
#### Logical Patterns

Logical patterns match events that arrive in temporal order and correlate them with logical relationships such as `and`, 
`or` and `not`. 

**Syntax**

```sql
from (every)? (not)? <event reference>=<input stream>[<filter condition>] 
          ((and|or) <event reference>=<input stream>[<filter condition>])? (within <time gap>)? ->  
    ... 
select <event reference>([event index])?.<attribute name>, ...
insert into <output stream>
```

Keywords such as `and`, `or`, or `not` can be used to illustrate the logical relationship.

Key Word|Description
---------|---------
`and`|This allows both conditions of `and` to be matched by two events in any order.
`or`|The state succeeds if either condition of `or` is satisfied. Here the event reference of the other condition is `null`.
`not <condition1> and <condition2>`| When `not` is included with `and`, it identifies the events that match <condition2> arriving before any event that match <condition1>. 
`not <condition> for <time period>`| When `not` is included with `for`, it allows you to identify a situation where no event that matches `<condition1>` arrives during the specified `<time period>`.  e.g.,`from not TemperatureStream[temp > 60] for 5 sec`. 

Here the `not` pattern can be followed by either an `and` clause or the effective period of `not` can be concluded after a given `<time period>`. Further in Siddhi more than two streams cannot be matched with logical conditions using `and`, `or`, or `not` clauses at this point.

##### Detecting Non-occurring Events

Siddhi allows you to detect non-occurring events via multiple combinations of the key words specified above as shown in the table below.

In the patterns listed, P* can be either a regular event pattern, an absent event pattern or a logical pattern.

Pattern|Detected Scenario
---------|---------
`not A for <time period>`|The non-occurrence of event A within `<time period>` after system start up.<br/> e.g., Generating an alert if a taxi has not reached its destination within 30 minutes, to indicate that the passenger might be in danger.
`not A for <time period> and B`|After system start up, event A does not occur within `time period`, but event B occurs at some point in time. <br/> e.g., Generating an alert if a taxi has not reached its destination within 30 minutes, and the passenger marked that he/she is in danger at some point in time. 
`not A for <time period 1> and not B for <time period 2>`|After system start up, event A doess not occur within `time period 1`, and event B also does not occur within `<time period 2>`. <br/> e.g., Generating an alert if the driver of a taxi has not reached the destination within 30 minutes, and the passenger has not marked himself/herself to be in danger within that same time period. 
`not A for <time period> or B`|After system start up, either event A does not occur within `<time period>`, or event B occurs at some point in time. <br/> e.g., Generating an alert if the taxi has not reached its destination within 30 minutes, or if the passenger has marked that he/she is in danger at some point in time.
`not A for <time period 1> or not B for <time period 2>`|After system start up, either event A does not occur within `<time period 1>`, or event B occurs within `<time period 2>`. <br/> e.g., Generating an alert to indicate that the driver is not on an expected route if the taxi has not reached destination A within 20 minutes, or reached destination B within 30 minutes.
`A → not B for <time period>`|Event B does not occur within `<time period>` after the occurrence of event A. e.g., Generating an alert if the taxi has reached its destination, but this was not followed by a payment record.
`P* → not A for <time period> and B`|After the occurrence of P*, event A does not occur within `<time period>`, and event B occurs at some point in time. <br/> 
`P* → not A for <time period 1> and not B for <time period 2>`|After the occurrence of P*, event A does not occur within `<time period 1>`, and event B does not occur within `<time period 2>`.
`P* → not A for <time period> or B`|After the occurrence of P*, either event A does not occur within `<time period>`, or event B occurs at some point in time.
`P* → not A for <time period 1> or not B for <time period 2>`|After the occurrence of P*, either event A does not occur within `<time period 1>`, or event B does not occur within `<time period 2>`.
`not A for <time period> → B`|Event A does occur within `<time period>` after the system start up, but event B occurs after that `<time period>` has elapsed.
`not A for <time period> and B → P*`|Event A does not occur within `<time period>`, and event B occurs at some point in time. Then P* occurs after the `<time period>` has elapsed, and after B has occurred.
`not A for <time period 1> and not B for <time period 2> → P*`|After system start up, event A does not occur within `<time period 1>`, and event B does not occur within `<time period 2>`. However, P* occurs after both A and B.
`not A for <time period> or B → P*`|After system start up, event A does not occur within `<time period>` or event B occurs at some point in time. The P* occurs after `<time period>` has elapsed, or after B has occurred.
`not A for <time period 1> or not B for <time period 2> → P*`|After system start up, either event A does not occur within `<time period 1>`, or event B does not occur within `<time period 2>`. Then P*  occurs after both `<time period 1>` and `<time period 2>` have elapsed.
`not A and B`|Event A does not occur before event B.
`A and not B`|Event B does not occur before event A.


**Example**

Following Siddhi App, sends the `stop` control action to the regulator when the key is removed from the hotel room. 
```sql
define stream RegulatorStateChangeStream(deviceID long, roomNo int, tempSet double, action string);
define stream RoomKeyStream(deviceID long, roomNo int, action string);

  
from every( e1=RegulatorStateChangeStream[ action == 'on' ] ) -> 
      e2=RoomKeyStream[ e1.roomNo == roomNo and action == 'removed' ] or e3=RegulatorStateChangeStream[ e1.roomNo == roomNo and action == 'off']
select e1.roomNo, ifThenElse( e2 is null, 'none', 'stop' ) as action
having action != 'none'
insert into RegulatorActionStream;
```

This Siddhi Application generates an alert if we have switch off the regulator before the temperature reaches 12 degrees.  

```sql
define stream RegulatorStateChangeStream(deviceID long, roomNo int, tempSet double, action string);
define stream TempStream (deviceID long, roomNo int, temp double);

from e1=RegulatorStateChangeStream[action == 'start'] -> not TempStream[e1.roomNo == roomNo and temp < 12] and e2=RegulatorStateChangeStream[action == 'off']
select e1.roomNo as roomNo
insert into AlertStream;
```

This Siddhi Application generates an alert if the temperature does not reduce to 12 degrees within 5 minutes of switching on the regulator.  

```sql
define stream RegulatorStateChangeStream(deviceID long, roomNo int, tempSet double, action string);
define stream TempStream (deviceID long, roomNo int, temp double);

from e1=RegulatorStateChangeStream[action == 'start'] -> not TempStream[e1.roomNo == roomNo and temp < 12] for '5 min'
select e1.roomNo as roomNo
insert into AlertStream;
```


### Sequence

Sequence is a state machine implementation that allows you to detect the sequence of event occurrences over time. 
Here **all matching events need to arrive consecutively** to match the sequence condition, and there cannot be any non-matching events arriving within a matching sequence of events.
This can correlate events within a single stream or between multiple streams. 

**Purpose** 

This allows you to detect a specified event sequence over a specified time period. 

**Syntax**

The syntax for a sequence query is as follows:

```sql
from (every)? <event reference>=<input stream>[<filter condition>], 
    <event reference>=<input stream [<filter condition>], 
    ... 
    (within <time gap>)?     
select <event reference>.<attribute name>, <event reference>.<attribute name>, ...
insert into <output stream>
```

| Items | Description |
|-------------------|-------------|
| `,` | This represents the immediate next event i.e., when an event that matches the first condition arrives, the event that arrives immediately after it should match the second condition. |
| `<event reference>` | This allows you to add a reference to the the matching event so that it can be accessed later for further processing. |
| `(within <time gap>)?` | The `within` clause is optional. It defines the time duration within which all the matching events should occur. |
| `every` | `every` is an optional keyword. This defines whether the matching event should be triggered for every event that arrives at the specified stream with the matching condition. <br/> When this keyword is not used, the matching is carried out only once. |


**Example**

This query generates an alert if the increase in the temperature between two consecutive temperature events exceeds one degree.

```sql
from every e1=TempStream, e2=TempStream[e1.temp + 1 < temp]
select e1.temp as initialTemp, e2.temp as finalTemp
insert into AlertStream;
```

**Counting Sequence**

Counting sequences allow you to match multiple events for the same matching condition.
The number of events matched per condition can be limited via condition postfixes such as **Counting Patterns**, or by using the 
`*`, `+`, and `?` operators.

The matching events can also be retrieved using event indexes, similar to how it is done in **Counting Patterns**.

**Syntax**

Each matching condition in a sequence can contain a collection of events as shown below. 

```sql
from (every)? <event reference>=<input stream>[<filter condition>](+|*|?)?, 
    <event reference>=<input stream [<filter condition>](+|*|?)?, 
    ... 
    (within <time gap>)?     
select <event reference>.<attribute name>, <event reference>.<attribute name>, ...
insert into <output stream>
```

|Postfix symbol|Required/Optional |Description|
|---------|---------|---------|
| `+` | Optional |This matches **one or more** events to the given condition. |
| `*` | Optional |This matches **zero or more** events to the given condition. |
| `?` | Optional |This matches **zero or one** events to the given condition. |


**Example**

This Siddhi application identifies temperature peeks.

```sql
define stream TempStream(deviceID long, roomNo int, temp double);
  
from every e1=TempStream, e2=TempStream[e1.temp <= temp]+, e3=TempStream[e2[last].temp > temp]
select e1.temp as initialTemp, e2[last].temp as peakTemp
insert into PeekTempStream;
```

**Logical Sequence**

Logical sequences identify logical relationships using `and`, `or` and `not` on consecutively arriving events.

**Syntax**
The syntax for a logical sequence is as follows:

```sql
from (every)? (not)? <event reference>=<input stream>[<filter condition>] 
          ((and|or) <event reference>=<input stream>[<filter condition>])? (within <time gap>)?, 
    ... 
select <event reference>([event index])?.<attribute name>, ...
insert into <output stream>
```

Keywords such as `and`, `or`, or `not` can be used to illustrate the logical relationship, similar to how it is done in **Logical Patterns**. 

**Example**

This Siddhi application notifies the state when a regulator event is immediately followed by both temperature and humidity events. 

```sql
define stream TempStream(deviceID long, temp double);
define stream HumidStream(deviceID long, humid double);
define stream RegulatorStream(deviceID long, isOn bool);
  
from every e1=RegulatorStream, e2=TempStream and e3=HumidStream
select e2.temp, e3.humid
insert into StateNotificationStream;
```

### Output rate limiting

Output rate limiting allows queries to output events periodically based on a specified condition.

**Purpose**

This allows you to limit the output to avoid overloading the subsequent executions, and to remove unnecessary information.

**Syntax**

The syntax of an output rate limiting configuration is as follows:

```sql
from <input stream> ...
select <attribute name>, <attribute name>, ...
output <rate limiting configuration>
insert into <output stream>
```
Siddhi supports three types of output rate limiting configurations as explained in the following table: 

Rate limiting configuration|Syntax| Description
---------|---------|--------
Based on time | `<output event> every <time interval>` | This outputs `<output event>` every `<time interval>` time interval.
Based on number of events | `<output event> every <event interval> events` | This outputs `<output event>` for every `<event interval>` number of events.
Snapshot based output | `snapshot every <time interval>`| This outputs all events in the window (or the last event if no window is defined in the query) for every given `<time interval>` time interval.

Here the `<output event>` specifies the event(s) that should be returned as the output of the query. 
The possible values are as follows:
* `first` : Only the first event processed by the query during the specified time interval/sliding window is emitted.
* `last` : Only the last event processed by the query during the specified time interval/sliding window is emitted.
* `all` : All the events processed by the query during the specified time interval/sliding window are emitted. **When no `<output event>` is defined, `all` is used by default.**

**Examples**

+ Returning events based on the number of events

    Here, events are emitted every time the specified number of events arrive. You can also specify whether to emit only the first event/last event, or all the events out of the events that arrived.
    
    In this example, the last temperature per sensor is emitted for every 10 events.
    
    <pre>
    from TempStreamselect 
    select temp, deviceID
    group by deviceID
    output last every 10 events
    insert into LowRateTempStream;    </pre>

+ Returning events based on time

    Here events are emitted for every predefined time interval. You can also specify whether to emit only the first event, last event, or all events out of the events that arrived during the specified time interval.

    In this example, emits all temperature events every 10 seconds  
      
    <pre>
    from TempStreamoutput 
    output every 10 sec
    insert into LowRateTempStream;    </pre>

+ Returning a periodic snapshot of events

    This method works best with windows. When an input stream is connected to a window, snapshot rate limiting emits all the current events that have arrived and do not have corresponding expired events for every predefined time interval. 
    If the input stream is not connected to a window, only the last current event for each predefined time interval is emitted.
    
    This query emits a snapshot of the events in a time window of 5 seconds every 1 second. 

    <pre>
    from TempStream#window.time(5 sec)
    output snapshot every 1 sec
    insert into SnapshotTempStream;    </pre>
    

## Partition

Partitions divide streams and queries into isolated groups in  order to process them in parallel and in isolation. 
A partition can contain one or more queries and there can be multiple instances where the same queries and streams are replicated for each partition. 
Each partition is tagged with a partition key. Those partitions only process the events that match the corresponding partition key. 

**Purpose** 

Partitions allow you to process the events groups in isolation so that event processing can be performed using the same set of queries for each group. 

**Partition key generation**

A partition key can be generated in the following two methods:

* Partition by value
  
    This is created by generating unique values using input stream attributes.

    **Syntax**
    
    <pre>
    partition with ( &lt;expression> of &lt;stream name>, &lt;expression> of &lt;stream name>, ... )
    begin
        &lt;query>
        &lt;query>
        ...
    end; </pre>
    
    **Example**
    
    This query calculates the maximum temperature recorded within the last 10 events per `deviceID`.
    
    <pre>
    partition with ( deviceID of TempStream )
    begin
        from TempStream#window.length(10)
        select roomNo, deviceID, max(temp) as maxTemp
        insert into DeviceTempStream;
    end;
    </pre>

* Partition by range

    This is created by mapping each partition key to a range condition of the input streams numerical attribute.

    **Syntax**
    
    <pre>
    partition with ( &lt;condition> as &lt;partition key> or &lt;condition> as &lt;partition key> or ... of &lt;stream name>, ... )
    begin
        &lt;query>
        &lt;query>
        ...
    end;
    </pre>

    **Example**
    
    This query calculates the average temperature for the last 10 minutes per office area.
    
    <pre>
    partition with ( roomNo >= 1030 as 'serverRoom' or 
                     roomNo < 1030 and roomNo >= 330 as 'officeRoom' or 
                     roomNo < 330 as 'lobby' of TempStream)
    begin
        from TempStream#window.time(10 min)
        select roomNo, deviceID, avg(temp) as avgTemp
        insert into AreaTempStream
    end;
    </pre>  

### Inner Stream

Queries inside a partition block can use inner streams to communicate with each other while preserving partition isolation.
Inner streams are denoted by a "#" placed before the stream name, and these streams cannot be accessed outside a partition block. 

**Purpose**
  
Inner streams allow you to connect queries within the partition block so that the output of a query can be used as an input only by another query 
within the same partition. Therefore, you do not need to repartition the streams if they are communicating within the partition.

**Example**

This partition calculates the average temperature of every 10 events for each sensor, and sends an output to the `DeviceTempIncreasingStream` stream if the consecutive average temperature values increase by more than 
5 degrees.

<pre>
partition with ( deviceID of TempStream )
begin
    from TempStream#window.lengthBatch(10)
    select roomNo, deviceID, avg(temp) as avgTemp
    insert into #AvgTempStream
  
    from every (e1=#AvgTempStream),e2=#AvgTempStream[e1.avgTemp + 5 < avgTemp]
    select e1.deviceID, e1.avgTemp as initialAvgTemp, e2.avgTemp as finalAvgTemp
    insert into DeviceTempIncreasingStream
end;
</pre>

## Table

A table is a stored version of an stream or a table of events. Its schema is defined via the **table definition** that is
similar to a stream definition. These events are by default stored `in-memory`, but Siddhi also provides store extensions to work with data/events stored in various data stores through the 
table abstraction.

**Purpose**

Tables allow Siddhi to work with stored events. By defining a schema for tables Siddhi enables them to be processed by queries using their defined attributes with the streaming data. You can also interactively query the state of the stored events in the table.

**Syntax**

The syntax for a new table definition is as follows:

```sql
define table <table name> (<attribute name> <attribute type>, <attribute name> <attribute type>, ... );
```
The following parameters are configured in a table definition:

| Parameter     | Description |
| ------------- |-------------|
| `table name`      | The name of the table defined. (`PascalCase` is used for table name as a convention.) |
| `attribute name`   | The schema of the table is defined by its attributes with uniquely identifiable attribute names (`camelCase` is used for attribute names as a convention.)|    |
| `attribute type`   | The type of each attribute defined in the schema. <br/> This can be `STRING`, `INT`, `LONG`, `DOUBLE`, `FLOAT`, `BOOL` or `OBJECT`.     |


**Example**

The following defines a table named `RoomTypeTable` with `roomNo` and `type` attributes of data types `int` and `string` respectively.

```sql
define table RoomTypeTable ( roomNo int, type string );
```

**Primary Keys**

Tables can be configured with primary keys to avoid the duplication of data. 

Primary keys are configured by including the `@PrimaryKey( 'key1', 'key2' )` annotation to the table definition. 
Each event table configuration can have only one `@PrimaryKey` annotation. 
The number of attributes supported differ based on the table implementations. When more than one attribute 
 is used for the primary key, the uniqueness of the events stored in the table is determined based on the combination of values for those attributes.

**Examples**

This query creates an event table with the `symbol` attribute as the primary key. 
Therefore each entry in this table must have a unique value for `symbol` attribute.

```sql
@PrimaryKey('symbol')
define table StockTable (symbol string, price float, volume long);
```

**Indexes**
 
Indexes allow tables to be searched/modified much faster. 

Indexes are configured by including the `@Index( 'key1', 'key2' )` annotation to the table definition.
 Each event table configuration can have 0-1 `@Index` annotations. 
 Support for the `@Index` annotation and the number of attributes supported differ based on the table implementations. 
 When more then one attribute is used for index, each one of them is used to index the table for fast access of the data. 
 Indexes can be configured together with primary keys. 

**Examples**

This query creates an indexed event table named `RoomTypeTable` with the `roomNo` attribute as the index key.

```sql
@Index('roomNo')
define table RoomTypeTable (roomNo int, type string);
```

**Operators on Table**

The following operators can be performed on tables.


### Insert

This allows events to be inserted into tables. This is similar to inserting events into streams. 

!!! warning
    If the table is defined with primary keys, and if you insert duplicate data, primary key constrain violations can occur. 
    In such cases use the `update or insert into` operation. 
    
**Syntax**

```sql
from <input stream> 
select <attribute name>, <attribute name>, ...
insert into <table>
```

Similar to streams, you need to use the `current events`, `expired events` or the `all events` keyword between `insert` and `into` keywords in order to insert only the specific output event types. 
For more information, see [output event type](#output-event-types)

**Example**

This query inserts all the events from the `TempStream` stream to the `TempTable` table.

```sql
from TempStream
select *
insert into TempTable;
```

### Join (Table)

This allows a stream to retrieve information from a table in a streaming manner.

!!! Note
    Joins can also be performed with [two streams](#join-stream), [aggregation](#join-aggregation) or against externally [defined windows](#join-window).

**Syntax**

```sql
from <input stream> join <table>
    on <condition>
select (<input stream>|<table>).<attribute name>, (<input stream>|<table>).<attribute name>, ...
insert into <output stream>
```

!!! Note 
    A table can only be joint with a stream. Two tables cannot be joint because there must be at least one active 
    entity to trigger the join operation.

**Example**

This Siddhi App performs a join to retrieve the room type from `RoomTypeTable` table based on the room number, so that it can filter the events related to `server-room`s.

```sql
define table RoomTypeTable (roomNo int, type string);
define stream TempStream (deviceID long, roomNo int, temp double);
   
from TempStream join RoomTypeTable
    on RoomTypeTable.roomNo == TempStream.roomNo
select deviceID, RoomTypeTable.type as roomType, type, temp
    having roomType == 'server-room'
insert into ServerRoomTempStream;
```

### Delete

To delete selected events that are stored in a table.

**Syntax**

```sql
from <input stream> 
select <attribute name>, <attribute name>, ...
delete <table> (for <output event type>)?
    on <condition>
```

The `condition` element specifies the basis on which events are selected to be deleted. 
When specifying the condition, table attributes should be referred to with the table name.
 
To execute delete for specific output event types, use the `current events`, `expired events` or the `all events` keyword with `for` as shown
in the syntax. For more information, see [output event type](#output-event-types)

!!! note 
    Table attributes must be always referred to with the table name as follows: 
    `<table name>.<attibute name>`
    
**Example**

In this example, the script deletes a record in the `RoomTypeTable` table if it has a value for the `roomNo` attribute that matches the value for the `roomNumber` attribute of an event in the `DeleteStream` stream.


```sql
define table RoomTypeTable (roomNo int, type string);

define stream DeleteStream (roomNumber int);
  
from DeleteStream
delete RoomTypeTable
    on RoomTypeTable.roomNo == roomNumber;
```

### Update

This operator updates selected event attributes stored in a table based on a condition. 

**Syntax**

```sql
from <input stream> 
select <attribute name>, <attribute name>, ...
update <table> (for <output event type>)? 
    set <table>.<attribute name> = (<attribute name>|<expression>)?, <table>.<attribute name> = (<attribute name>|<expression>)?, ...
    on <condition>
```

The `condition` element specifies the basis on which events are selected to be updated.
When specifying the `condition`, table attributes must be referred to with the table name.

You can use the `set` keyword to update selected attributes from the table. Here, for each assignment, the attribute specified in the left must be the table attribute, and the one specified in the right can be a stream/table attribute a mathematical operation, or other. When the `set` clause is not provided, all the attributes in the table are updated.
   
To execute an update for specific output event types use the `current events`, `expired events` or the `all events` keyword with `for` as shown
in the syntax. For more information, see [output event type](#output-event-types).

!!! note 
    Table attributes must be always referred to with the table name as shown below:
     `<table name>.<attibute name>`.

**Example**

This Siddhi application updates the room occupancy in the `RoomOccupancyTable` table for each room number based on new arrivals and exits from the `UpdateStream` stream.

```sql
define table RoomOccupancyTable (roomNo int, people int);
define stream UpdateStream (roomNumber int, arrival int, exit int);
  
from UpdateStream
select *
update RoomOccupancyTable
    set RoomOccupancyTable.people = RoomOccupancyTable.people + arrival - exit
    on RoomOccupancyTable.roomNo == roomNumber;
```

### Update or Insert

This allows you update if the event attributes already exist in the table based on a condition, or 
else insert the entry as a new attribute.

**Syntax**

```sql
from <input stream> 
select <attribute name>, <attribute name>, ...
update or insert into <table> (for <output event type>)? 
    set <table>.<attribute name> = <expression>, <table>.<attribute name> = <expression>, ...
    on <condition>
```
The `condition` element specifies the basis on which events are selected for update.
When specifying the `condition`, table attributes should be referred to with the table name. 
If a record that matches the condition does not already exist in the table, the arriving event is inserted into the table.

The `set` clause is only used when an update is performed during the insert/update operation. 
When `set` clause is used, the attribute to the left is always a table attribute, and the attribute to the right can be a stream/table attribute, mathematical 
operation or other. The attribute to the left (i.e., the attribute in the event table) is updated with the value of the attribute to the right if the given condition is met. When the `set` clause is not provided, all the attributes in the table are updated. 

!!! note 
    When the attribute to the right is a table attribute, the operations supported differ based on the database type.
 
To execute update upon specific output event types use the `current events`, `expired events` or the `all events` keyword with `for` as shown
in the syntax. To understand more see [output event type](http://127.0.0.1:8000/documentation/siddhi-4.0/#output-event-types).

!!! note 
    Table attributes should be always referred to with the table name as `<table name>.<attibute name>`.

**Example**

The following query update for events in the `UpdateTable` event table that have room numbers that match the same in the `UpdateStream` stream. When such events are found in the event table, they are updated. When a room number available in the stream is not found in the event table, it is inserted from the stream.
 
```sql
define table RoomAssigneeTable (roomNo int, type string, assignee string);
define stream RoomAssigneeStream (roomNumber int, type string, assignee string);
   
from RoomAssigneeStream
select roomNumber as roomNo, type, assignee
update or insert into RoomAssigneeTable
    set RoomAssigneeTable.assignee = assignee
    on RoomAssigneeTable.roomNo == roomNo;
```

### In
 
This allows the stream to check whether the expected value exists in the table as a part of a conditional operation.

**Syntax**

```sql
from <input stream>[<condition> in <table>]
select <attribute name>, <attribute name>, ...
insert into <output stream>
```

The `condition` element specifies the basis on which events are selected to be compared. 
When constructing the `condition`, the table attribute must be always referred to with the table name as shown below:
`<table>.<attibute name>`.

**Example**

This Siddhi application filters only room numbers that are listed in the `ServerRoomTable` table.

```sql
define table ServerRoomTable (roomNo int);
define stream TempStream (deviceID long, roomNo int, temp double);
   
from TempStream[ServerRoomTable.roomNo == roomNo in ServerRoomTable]
insert into ServerRoomTempStream;
```

## Incremental Aggregation

Incremental aggregation allows you to obtain aggregates in an incremental manner for a specified set of time periods.

This not only allows you to calculate aggregations with varied time granularity, but also allows you to access them in an interactive
 manner for reports, dashboards, and for further processing. Its schema is defined via the **aggregation definition**.
 
 Incremental aggregation granularity data holders are automatically purged every 15 minutes. When carrying out data purging, the retention period you have specified for each granularity in the incremental aggregation query is taken into account. The retention period defined for a granularity needs to be greater than or equal to its minimum retention period as specified in the table below. If no valid retention period is defined for a granularity, the default retention period (as specified in the table below) is applied. 
 
|Granularity           |Default retention      |Minimum retention 
---------------        |--------------         |------------------  
|`second`              |`120` seconds          |`120` seconds
|`minute`              |`24`  hours            |`120` minutes
|`hour`                |`30`  days             |`25`  hours
|`day`                 |`1`   year             |`32`  days
|`month`               |`All`                  |`13`  month
|`year`                |`All`                  |`none` 

**Purpose**

Incremental aggregation allows you to retrieve the aggregate values for different time durations. 
That is, it allows you to obtain aggregates such as `sum`, `count`, `avg`, `min`, `max`, `count` and `distinctCount`
of stream attributes for durations such as `sec`, `min`, `hour`, etc.

This is of considerable importance in many Analytics scenarios because aggregate values are often needed for several time periods. 
Furthermore, this ensures that the aggregations are not lost due to unexpected system failures because aggregates can be stored in different persistence `stores`.

**Syntax**

```sql
@store(type="<store type>", ...)
@purge(enable="<true or false>",interval=<purging interval>,@retentionPeriod(<granularity> = <retention period>, ...) )
define aggregation <aggregator name>
from <input stream>
select <attribute name>, <aggregate function>(<attribute name>) as <attribute name>, ...
    group by <attribute name>
    aggregate by <timestamp attribute> every <time periods> ;
```
The above syntax includes the following:

|Item                          |Description
---------------                |---------
|`@BufferSize`                 |**DEPRECIATED FROM V4.2.0**. This identifies the number of expired events to retain in a buffer in order<br/>to handle out of order event processing. This is an optional<br/>parameter that is applicable only if aggregation is based on external<br/>timestamps (because events aggregated based on event arrival<br/>time cannot be out of order). Siddhi determines whether an event is expired or not<br/>based on the timestamp of the latest event and the most granular duration for<br/>which aggregation is calculated.<br/> e.g., If the aggregation is calculated for `sec…year`, the most granular duration is seconds. Therefore, if the buffer size is `3` and events arrive during 51st, 52nd, 53rd and 54th seconds, all of the older aggregations (i.e., for 51st, 52nd and 53rd seconds) are kept in the buffer because the latest event arrived during the 54th second. <br/>The default value is `0`.
|`@IgnoreEventsOlderThanBuffer`|**DEPRECIATED FROM V4.2.0**.This annotation specifies whether or not to aggregate events older than the <br/>buffer. If this parameter is set to `false` (which is default), any event <br/>older than the buffer is aggregated with the oldest event in buffer. If <br/>this parameter is set to `true`, any event older than the buffer is dropped. This is an optional annotation.
|`@store`                      |This annotation is used to refer to the data store where the calculated <br/>aggregate results are stored. This annotation is optional. When <br/>no annotation is provided, the data is stored in the `in-memory` store.
|`@purge`                      |This annotation is used to configure purging in aggregation granularities.<br/> If this annotation is not provided, the default purging mentioned above is applied.<br/> If you want to disable automatic data purging, you can use this annotation as follows:</br>'@purge(enable=false)</br>/You should disable data purging if the aggregation query in included in the Siddhi application for read-only purposes.
|`@retentionPeriod`            |This annotation is used to specify the length of time the data needs to be retained when carrying out data purging.<br/> If this annotation is not provided, the default retention period is applied.
|`<aggregator name>`           |This specifies a unique name for the aggregation so that it can be referred <br/>when accessing aggregate results.
|`<input stream>`              |The stream that feeds the aggregation. **Note! this stream should be <br/>already defined.**
|`group by <attribute name>`   |The `group by` clause is optional. If it is included in a Siddhi application, aggregate values <br/> are calculated per each `group by` attribute. If it is not used, all the<br/> events are aggregated together.
|`by <timestamp attribute>`    |This clause is optional. This defines the attribute that should be used as<br/> the timestamp. If this clause is not used, the event time is used by default.<br/> The timestamp could be given as either a `string` or a `long` value. If it is a `long` value,<br/> the unix timestamp in milliseconds is expected (e.g. `1496289950000`). If it is <br/>a `string` value, the supported formats are `<yyyy>-<MM>-<dd> <HH>:<mm>:<ss>` <br/>(if time is in GMT) and  `<yyyy>-<MM>-<dd> <HH>:<mm>:<ss> <Z>` (if time is <br/>not in GMT), here the ISO 8601 UTC offset must be provided for `<Z>` .<br/>(e.g., `+05:30`, `-11:00`).
|`<time periods>`              |Time periods can be specified as a range where the minimum and the maximum value are separated by three dots, or as comma-separated values. <br><br> e.g., A range can be specified as sec...year where aggregation is done per second, minute, hour, day, month and year. Comma-separated values can be specified as min, hour. <br><br> Skipping time durations (e.g., min, day where the hour duration is skipped) when specifying comma-separated values is supported only from v4.1.1 onwards

!!! Note
    From V4.2.0 onwards, aggregation is carried out at calendar start times for each granularity with the GMT timezone
    
!!! Note
    From V4.2.6 onwards, the same aggregation can be defined in multiple Siddhi apps for joining, however, *only one siddhi app should carry out the processing* (i.e. the aggregation input stream should only feed events to one aggregation definition). 

**Example**

This Siddhi Application defines an aggregation named `TradeAggregation` to calculate the average and sum for the `price` attribute of events arriving at the `TradeStream` stream. These aggregates are calculated per every time granularity in the second-year range.

```sql
define stream TradeStream (symbol string, price double, volume long, timestamp long);

@purge(enable='true', interval='10 sec',@retentionPeriod(sec='120 sec',min='24 hours',hours='30 days',days='1 year',months='all',years='all'))
define aggregation TradeAggregation
  from TradeStream
  select symbol, avg(price) as avgPrice, sum(price) as total
    group by symbol
    aggregate by timestamp every sec ... year;
```

### Distributed Aggregation

!!! Note
    Distributed Aggregation is only supported after v4.3.0

Distributed Aggregation allows you to partially process aggregations in different shards. This allows Siddhi
app in one shard to be responsible only for processing a part of the aggregation.
However for this, all aggregations must be based on a common physical database(@store).

**Syntax**

```sql
@store(type="<store type>", ...)
@PartitionById
define aggregation <aggregator name>
from <input stream>
select <attribute name>, <aggregate function>(<attribute name>) as <attribute name>, ...
    group by <attribute name>
    aggregate by <timestamp attribute> every <time periods> ;
```

Following table includes the `annotation` to be used to enable distributed aggregation, 

Item | Description
------|------
`@PartitionById` | If the annotation is given, then the distributed aggregation is enabled. Further this can be disabled by using `enable` element, </br>`@PartitionById(enable='false')`.</br>


Further, following system properties are also available,

System Property| Description| Possible Values | Optional | Default Value 
---------|---------|---------|---------|------
shardId| The id of the shard one of the distributed aggregation is running in. This should be unique to a single shard | Any string | No | <Empty_String> 
partitionById| This allows user to enable/disable distributed aggregation for all aggregations running in one siddhi manager .(Available from v4.3.3) | true/false | Yesio | false



!!! Note
    ShardIds should not be changed after the first configuration in order to keep data consistency.

### Join (Aggregation)

This allows a stream to retrieve calculated aggregate values from the aggregation. 

!!! Note
    A join can also be performed with [two streams](#join-stream), with a [table](#join-table) and a stream, or with a stream against externally [defined windows](#join-window).


**Syntax**

A join with aggregation is similer to the join with [table](#join-table), but with additional `within` and `per` clauses. 

```sql
from <input stream> join <aggrigation> 
  on <join condition> 
  within <time range> 
  per <time granularity>
select <attribute name>, <attribute name>, ...
insert into <output stream>;
```
Apart from constructs of [table join](#join-table) this includes the following. Please note that the 'on' condition is optional :

Item|Description
---------|---------
`within  <time range>`| This allows you to specify the time interval for which the aggregate values need to be retrieved. This can be specified by providing the start and end time separated by a comma as `string` or `long` values, or by using the wildcard `string` specifying the data range. For details refer examples.            
`per <time granularity>`|This specifies the time granularity by which the aggregate values must be grouped and returned. e.g., If you specify `days`, the retrieved aggregate values are grouped for each day within the selected time interval.

`within` and `par` clauses also accept attribute values from the stream.

!!! Note
    The timestamp of the aggregations can be accessed through the `AGG_TIMESTAMP` attribute.

**Example**

Following aggregation definition will be used for the examples. 

```sql
define stream TradeStream (symbol string, price double, volume long, timestamp long);

define aggregation TradeAggregation
  from TradeStream
  select symbol, avg(price) as avgPrice, sum(price) as total
    group by symbol
    aggregate by timestamp every sec ... year;
```

This query retrieves daily aggregations within the time range `"2014-02-15 00:00:00 +05:30", "2014-03-16 00:00:00 +05:30"` (Please note that +05:30 can be omitted if timezone is GMT)

```sql
define stream StockStream (symbol string, value int);

from StockStream as S join TradeAggregation as T
  on S.symbol == T.symbol 
  within "2014-02-15 00:00:00 +05:30", "2014-03-16 00:00:00 +05:30" 
  per "days" 
select S.symbol, T.total, T.avgPrice 
insert into AggregateStockStream;
```

This query retrieves hourly aggregations within the day `2014-02-15`.

```sql
define stream StockStream (symbol string, value int);

from StockStream as S join TradeAggregation as T
  on S.symbol == T.symbol 
  within "2014-02-15 **:**:** +05:30"
  per "hours" 
select S.symbol, T.total, T.avgPrice 
insert into AggregateStockStream;
```

This query retrieves all aggregations per `perValue` stream attribute within the time period 
between timestamps `1496200000000` and `1596434876000`.

```sql
define stream StockStream (symbol string, value int, perValue string);

from StockStream as S join TradeAggregation as T
  on S.symbol == T.symbol 
  within 1496200000000L, 1596434876000L
  per S.perValue
select S.symbol, T.total, T.avgPrice 
insert into AggregateStockStream;
```

## _(Defined)_ Window

A defined window is a window that can be shared across multiple queries. 
Events can be inserted to a defined window from one or more queries and it can produce output events based on the defined window type.
 
**Syntax**

The syntax for a defined window is as follows:

```sql
define window <window name> (<attribute name> <attribute type>, <attribute name> <attribute type>, ... ) <window type>(<parameter>, <parameter>, …) <output event type>;
```

The following parameters are configured in a table definition:

| Parameter     | Description |
| ------------- |-------------|
| `window name`      | The name of the window defined. (`PascalCase` is used for window names as a convention.) |
| `attribute name`   | The schema of the window is defined by its attributes with uniquely identifiable attribute names (`camelCase` is used for attribute names as a convention.)|    |
| `attribute type`   | The type of each attribute defined in the schema. <br/> This can be `STRING`, `INT`, `LONG`, `DOUBLE`, `FLOAT`, `BOOL` or `OBJECT`.     |
| `<window type>(<parameter>, ...)`   | The window type associated with the window and its parameters.     |
| `output <output event type>` | This is optional. Keywords such as `current events`, `expired events` and `all events` (the default) can be used to specify when the window output should be exposed. For more information, see [output event type](#output-event-types).
 

**Examples**
 
+ Returning all output when events arrive and when events expire from the window.

    In this query, the output event type is not specified. Therefore, it returns both current and expired events as the output.
    
```sql
  define window SensorWindow (name string, value float, roomNo int, deviceID string) timeBatch(1 second);
```
+ Returning an output only when events expire from the window.

    In this query, the output event type of the window is `expired events`. Therefore, it only returns the events that have expired from the window as the output.
    
```sql
  define window SensorWindow (name string, value float, roomNo int, deviceID string) timeBatch(1 second) output expired events;
```
     

**Operators on Defined Windows**

The following operators can be performed on defined windows.

### Insert

This allows events to be inserted into windows. This is similar to inserting events into streams. 

**Syntax**

```sql
from <input stream> 
select <attribute name>, <attribute name>, ...
insert into <window>
```

To insert only events of a specific output event type, add the `current events`, `expired events` or the `all events` keyword between `insert` and `into` keywords (similar to how it is done for streams).

For more information, see [output event type](#output-event-types).

**Example**

This query inserts all events from the `TempStream` stream to the `OneMinTempWindow` window.

```sql
define stream TempStream(tempId string, temp double);
define window OneMinTempWindow(tempId string, temp double) time(1 min);

from TempStream
select *
insert into OneMinTempWindow;
```

### Join (Window)

To allow a stream to retrieve information from a window based on a condition.

!!! Note
    A join can also be performed with [two streams](#join-stream), [aggregation](#join-aggregation) or with tables [tables](#join-table).

**Syntax**

```sql
from <input stream> join <window>
    on <condition>
select (<input stream>|<window>).<attribute name>, (<input stream>|<window>).<attribute name>, ...
insert into <output stream>
```

**Example**

This Siddhi Application performs a join count the number of temperature events having more then 40 degrees 
 within the last 2 minutes. 

```sql
define window TwoMinTempWindow (roomNo int, temp double) time(2 min);
define stream CheckStream (requestId string);
   
from CheckStream as C join TwoMinTempWindow as T
    on T.temp > 40
select requestId, count(T.temp) as count
insert into HighTempCountStream;
```

### From

A window can be an input to a query, similar to streams. 

Note !!!
     When window is used as an input to a query, another window cannot be applied on top of this.

**Syntax**

```sql
from <window> 
select <attribute name>, <attribute name>, ...
insert into <output stream>
```

**Example**
This Siddhi Application calculates the maximum temperature within the last 5 minutes.

```sql
define window FiveMinTempWindow (roomNo int, temp double) time(5 min);


from FiveMinTempWindow
select max(temp) as maxValue, roomNo
insert into MaxSensorReadingStream;
```

## Trigger

Triggers allow events to be periodically generated. **Trigger definition** can be used to define a trigger. 
A trigger also works like a stream with a predefined schema.

**Purpose**

For some use cases the system should be able to periodically generate events based on a specified time interval to perform 
some periodic executions. 

A trigger can be performed for a `'start'` operation, for a given `<time interval>`, or for a given `'<cron expression>'`.


**Syntax**

The syntax for a trigger definition is as follows.

```sql
define trigger <trigger name> at ('start'| every <time interval>| '<cron expression>');
```

Similar to streams, triggers can be used as inputs. They adhere to the following stream definition and produce the `triggered_time` attribute of the `long` type.

```sql
define stream <trigger name> (triggered_time long);
```

The following types of triggeres are currently supported:

|Trigger type| Description|
|-------------|-----------|
|`'start'`| An event is triggered when Siddhi is started.|
|`every <time interval>`| An event is triggered periodically at the given time interval.
|`'<cron expression>'`| An event is triggered periodically based on the given cron expression. For configuration details, see <a target="_blank" href="http://www.quartz-scheduler.org/documentation/quartz-2.2.x/tutorials/tutorial-lesson-06">quartz-scheduler</a>.
 

**Examples**

+ Triggering events regularly at specific time intervals

    The following query triggers events every 5 minutes.
    
```sql
     define trigger FiveMinTriggerStream at every 5 min;
```

+ Triggering events at a specific time on specified days

    The following query triggers an event at 10.15 AM on every weekdays.
    
```sql
     define trigger FiveMinTriggerStream at '0 15 10 ? * MON-FRI';
```

## Script

Scripts allow you to write functions in other programming languages and execute them within Siddhi queries. 
Functions defined via scripts can be accessed in queries similar to any other inbuilt function. 
**Function definitions** can be used to define these scripts.

Function parameters are passed into the function logic as `Object[]` and with the name `data` . 

**Purpose**

Scripts allow you to define a function operation that is not provided in Siddhi core or its extension. It is not required to write an extension to define the function logic.

**Syntax**

The syntax for a Script definition is as follows.

```sql
define function <function name>[<language name>] return <return type> {
    <operation of the function>
};
```

The following parameters are configured when defining a script.

| Parameter     | Description |
| ------------- |-------------|
| `function name`| 	The name of the function (`camelCase` is used for the function name) as a convention.|
|`language name`| The name of the programming language used to define the script, such as `javascript`, `r` and `scala`.|
| `return type`| The attribute type of the function’s return. This can be `int`, `long`, `float`, `double`, `string`, `bool` or `object`. Here the function implementer should be responsible for returning the output attribute on the defined return type for proper functionality.
|`operation of the function`| Here, the execution logic of the function is added. This logic should be written in the language specified under the `language name`, and it should return the output in the data type specified via the `return type` parameter.

**Examples**

This query performs concatenation using JavaScript, and returns the output as a string.

```sql
define function concatFn[javascript] return string {
    var str1 = data[0];
    var str2 = data[1];
    var str3 = data[2];
    var responce = str1 + str2 + str3;
    return responce;
};
  
define stream TempStream(deviceID long, roomNo int, temp double);
  
from TempStream
select concatFn(roomNo,'-',deviceID) as id, temp 
insert into DeviceTempStream;
```

## Store Query

Siddhi store queries are a set of on-demand queries that can be used to perform operations on Siddhi tables, windows, and aggregators.

**Purpose**

Store queries allow you to execute the following operations on Siddhi tables, windows, and aggregators without the intervention of streams.

Queries supported for tables:

* SELECT
* INSERT
* DELETE
* UPDATE
* UPDATE OR INSERT

Queries supported for windows and aggregators:

* SELECT

This is be done by submitting the store query to the Siddhi application runtime using its `query()` method.

In order to execute store queries, the Siddhi application of the Siddhi application runtime you are using, should have
 a store defined, which contains the table that needs to be queried.


**Example**

If you need to query the table named `RoomTypeTable` the it should have been defined in the Siddhi application.

In order to execute a store query on `RoomTypeTable`, you need to submit the store query using `query()` 
method of `SiddhiAppRuntime` instance as below.

```java
siddhiAppRuntime.query(<store query>);
```

### _(Table/Window)_ Select 

The `SELECT` store query retrieves records from the specified table or window, based on the given condition.

**Syntax**

```sql
from <table/window>
<on condition>?
select <attribute name>, <attribute name>, ...
<group by>? 
<having>? 
<order by>? 
<limit>?
```

**Example**

This query retrieves room numbers and types of the rooms starting from room no 10.

```sql
from roomTypeTable
on roomNo >= 10;
select roomNo, type
```

### _(Aggregation)_ Select 

The `SELECT` store query retrieves records from the specified aggregation, based on the given condition, time range, 
and granularity.

**Syntax**

```sql
from <aggregation>
<on condition>?
within <time range>
per <time granularity>
select <attribute name>, <attribute name>, ...
<group by>? 
<having>? 
<order by>? 
<limit>?
```

**Example**

Following aggregation definition will be used for the examples. 

```sql
define stream TradeStream (symbol string, price double, volume long, timestamp long);

define aggregation TradeAggregation
  from TradeStream
  select symbol, avg(price) as avgPrice, sum(price) as total
    group by symbol
    aggregate by timestamp every sec ... year;
```

This query retrieves daily aggregations within the time range `"2014-02-15 00:00:00 +05:30", "2014-03-16 00:00:00 +05:30"` (Please note that +05:30 can be omitted if timezone is GMT)

```sql
from TradeAggregation
  within "2014-02-15 00:00:00 +05:30", "2014-03-16 00:00:00 +05:30" 
  per "days" 
select symbol, total, avgPrice ;
```

This query retrieves hourly aggregations of "FB" symbol within the day `2014-02-15`.

```sql
from TradeAggregation
  on symbol == "FB" 
  within "2014-02-15 **:**:** +05:30"
  per "hours" 
select symbol, total, avgPrice;
```

### Insert

This allows you to insert a new record to the table with the attribute values you define in the `select` section.

**Syntax**

```sql
select <attribute name>, <attribute name>, ...
insert into <table>;
```

**Example**

This store query inserts a new record to the table `RoomOccupancyTable`, with the specified attribute values.


```sql
select 10 as roomNo, 2 as people
insert into RoomOccupancyTable 
```

### Delete

The `DELETE` store query deletes selected records from a specified table.

**Syntax**

```sql
<select>?  
delete <table>  
on <conditional expresssion>
```

The `condition` element specifies the basis on which records are selected to be deleted.

!!! note
    Table attributes must always be referred to with the table name as shown below: <br />
     `<table name>.<attibute name>`.

**Example**

In this example, query deletes a record in the table named `RoomTypeTable` if it has value for the `roomNo` 
attribute that matches the value for the `roomNumber` attribute of the selection which has 10 as the actual value.

```sql
select 10 as roomNumber
delete RoomTypeTable
on RoomTypeTable.roomNo == roomNumber;
```

```sql
delete RoomTypeTable
on RoomTypeTable.roomNo == 10;
```

### Update

The `UPDATE` store query updates selected attributes stored in a specific table, based on a given condition.

**Syntax**

```sql
select <attribute name>, <attribute name>, ...?
update <table>
    set <table>.<attribute name> = (<attribute name>|<expression>)?, <table>.<attribute name> = (<attribute name>|<expression>)?, ...
    on <condition>
```

The `condition` element specifies the basis on which records are selected to be updated.
When specifying the `condition`, table attributes must be referred to with the table name.

You can use the `set` keyword to update selected attributes from the table. Here, for each assignment, the attribute specified in the left must be the table attribute, and the one specified in the right can be a stream/table attribute a mathematical operation, or other. When the `set` clause is not provided, all the attributes in the table are updated.


!!! note
    Table attributes must always be referred to with the table name as shown below: <br />
     `<table name>.<attibute name>`.

**Example**

The following query updates the room occupancy by increasing the value of `people` by 1, in the `RoomOccupancyTable` 
table for each room number greater than 10.

```sql
select 10 as roomNumber, 1 as arrival
update RoomTypeTable
    set RoomTypeTable.people = RoomTypeTable.people + arrival
    on RoomTypeTable.roomNo == roomNumber;
```

```sql
update RoomTypeTable
    set RoomTypeTable.people = RoomTypeTable.people + 1
    on RoomTypeTable.roomNo == 10;
```

### Update or Insert

This allows you to update selected attributes if a record that meets the given conditions already exists in the specified  table. 
If a matching record does not exist, the entry is inserted as a new record.

**Syntax**

```sql
select <attribute name>, <attribute name>, ...
update or insert into <table>
    set <table>.<attribute name> = <expression>, <table>.<attribute name> = <expression>, ...
    on <condition>
```
The `condition` element specifies the basis on which records are selected for update.
When specifying the `condition`, table attributes should be referred to with the table name.
If a record that matches the condition does not already exist in the table, the arriving event is inserted into the table.

The `set` clause is only used when an update is performed during the insert/update operation.
When `set` clause is used, the attribute to the left is always a table attribute, and the attribute to the right can be a stream/table attribute, mathematical
operation or other. The attribute to the left (i.e., the attribute in the event table) is updated with the value of the attribute to the right if the given condition is met. When the `set` clause is not provided, all the attributes in the table are updated.

!!! note
    Table attributes must always be referred to with the table name as shown below: <br />
     `<table name>.<attibute name>`.

**Example**

The following query tries to update the records in the `RoomAssigneeTable` table that have room numbers that match the
 same in the selection. If such records are not found, it inserts a new record based on the values provided in the selection. 

```sql
select 10 as roomNo, "single" as type, "abc" as assignee
update or insert into RoomAssigneeTable
    set RoomAssigneeTable.assignee = assignee
    on RoomAssigneeTable.roomNo == roomNo;
```

## Extensions

Siddhi supports an extension architecture to enhance its functionality by incorporating other libraries in a seamless manner. 

**Purpose**

Extensions are supported because, Siddhi core cannot have all the functionality that's needed for all the use cases, mostly use cases require 
different type of functionality, and for some cases there can be gaps and you need to write the functionality by yourself.

All extensions have a namespace. This is used to identify the relevant extensions together, and to let you specifically call the extension.

**Syntax**

Extensions follow the following syntax;

```sql
<namespace>:<function name>(<parameter>, <parameter>, ... )
```

The following parameters are configured when referring a script function.

| Parameter     | Description |
| ------------- |-------------|
|`namespace` | Allows Siddhi to identify the extension without conflict|
| `function name`| 	The name of the function referred.|
| `parameter`| 	The function input parameter for function execution.|

<a name="ExtensionTypes"></a>
**Extension Types**

Siddhi supports following extension types:

* **Function**

    For each event, it consumes zero or more parameters as input parameters and returns a single attribute. This can be used to manipulate existing event attributes to generate new attributes like any Function operation.
    
    This is implemented by extending `org.wso2.siddhi.core.executor.function.FunctionExecutor`.
    
    Example : 
    
    `math:sin(x)` 
    
    Here, the `sin` function of `math` extension returns the sin value for the `x` parameter.
    
* **Aggregate Function**

    For each event, it consumes zero or more parameters as input parameters and returns a single attribute with aggregated results. This can be used in conjunction with a window in order to find the aggregated results based on the given window like any Aggregate Function operation. 
    
     This is implemented by extending `org.wso2.siddhi.core.query.selector.attribute.aggregator.AttributeAggregator`.

    Example : 
    
    `custom:std(x)` 
    
    Here, the `std` aggregate function of `custom` extension returns the standard deviation of the `x` value based on its assigned window query. 

* **Window** 

    This allows events to be **collected, generated, dropped and expired anytime** **without altering** the event format based on the given input parameters, similar to any other Window operator. 
    
    This is implemented by extending `org.wso2.siddhi.core.query.processor.stream.window.WindowProcessor`.

    Example : 
    
    `custom:unique(key)` 
    
    Here, the `unique` window of the `custom` extension retains one event for each unique `key` parameter.

* **Stream Function**

    This allows events to be  **generated or dropped only during event arrival** and **altered** by adding one or more attributes to it. 
    
    This is implemented by extending  `org.wso2.siddhi.core.query.processor.stream.function.StreamFunctionProcessor`.
    
    Example :  
    
    `custom:pol2cart(theta,rho)` 
    
    Here, the `pol2cart` function of the `custom` extension returns all the events by calculating the cartesian coordinates `x` & `y` and adding them as new attributes to the events.

* **Stream Processor**
    
    This allows events to be **collected, generated, dropped and expired anytime** by **altering** the event format by adding one or more attributes to it based on the given input parameters. 
    
    Implemented by extending `org.wso2.siddhi.core.query.processor.stream.StreamProcessor`.
    
    Example :  
    
    `custom:perMinResults(<parameter>, <parameter>, ...)` 
    
    Here, the `perMinResults` function of the `custom` extension returns all events by adding one or more attributes to the events based on the conversion logic. Altered events are output every minute regardless of event arrivals.

* **Sink**

    Sinks provide a way to **publish Siddhi events to external systems** in the preferred data format. Sinks publish events from the streams via multiple transports to external endpoints in various data formats.
    
    Implemented by extending `org.wso2.siddhi.core.stream.output.sink.Sink`.

    Example : 

    `@sink(type='sink_type', static_option_key1='static_option_value1')`
    
    To configure a stream to publish events via a sink, add the sink configuration to a stream definition by adding the @sink annotation with the required parameter values. The sink syntax is as above

* **Source**

    Source allows Siddhi to **consume events from external systems**, and map the events to adhere to the associated stream. Sources receive events via multiple transports and in various data formats, and direct them into streams for processing.
    
    Implemented by extending `org.wso2.siddhi.core.stream.input.source.Source`.
    
    Example : 
    
    `@source(type='source_type', static.option.key1='static_option_value1')`
        
    To configure a stream that consumes events via a source, add the source configuration to a stream definition by adding the @source annotation with the required parameter values. The source syntax is as above

* **Store**

    You can use Store extension type to work with data/events **stored in various data stores through the table abstraction**. You can find more information about these extension types under the heading 'Extension types' in this document. 
    
    Implemented by extending `org.wso2.siddhi.core.table.record.AbstractRecordTable`.

* **Script**

    Scripts allow you to **define a function** operation that is not provided in Siddhi core or its extension. It is not required to write an extension to define the function logic. Scripts allow you to write functions in other programming languages and execute them within Siddhi queries. Functions defined via scripts can be accessed in queries similar to any other inbuilt function.
    
    Implemented by extending `org.wso2.siddhi.core.function.Script`.

* **Source Mapper**

    Each `@source` configuration has a mapping denoted by the `@map` annotation that **converts the incoming messages format to Siddhi events**.The type parameter of the @map defines the map type to be used to map the data. The other parameters to be configured depends on the mapper selected. Some of these parameters are optional. 
    
    Implemented by extending `org.wso2.siddhi.core.stream.output.sink.SourceMapper`.

    Example :
   
    `@map(type='map_type', static_option_key1='static_option_value1')`

* **Sink Mapper**

    Each `@sink` configuration has a mapping denoted by the `@map` annotation that **converts the outgoing Siddhi events to configured messages format**.The type parameter of the @map defines the map type to be used to map the data. The other parameters to be configured depends on the mapper selected. Some of these parameters are optional. 

    Implemented by extending `org.wso2.siddhi.core.stream.output.sink.SinkMapper`.

    Example :
   
    `@map(type='map_type', static_option_key1='static_option_value1')`

**Example**

A window extension created with namespace `foo` and function name `unique` can be referred as follows:

```sql
from StockExchangeStream[price >= 20]#window.foo:unique(symbol)
select symbol, price
insert into StockQuote
```

**Available Extensions**

Siddhi currently has several pre written extensions that are available **<a target="_blank" href="https://wso2.github.io/siddhi/extensions/">here</a>**
 
_We value your contribution on improving Siddhi and its extensions further._


### Writing Custom Extensions

Custom extensions can be written in order to cater use case specific logic that are not available in Siddhi out of the box or as an existing extension.

There are five types of Siddhi extensions that you can write to cater your specific use cases. These 
extension types and the related maven archetypes are given below. You can use these archetypes to generate Maven projects for each 
extension type.

* Follow the procedure for the required archetype, based on your project:

!!! Note
    When using the generated archetype please make sure you uncomment @Extension annotation and complete the
    annotation with proper values. This annotation will be used to identify and document the extension, hence your
    extension will not work without @Extension annotation.

**siddhi-execution**

Siddhi-execution provides following extension types:

* Function
* Aggregate Function
* Stream Function
* Stream Processor
* Window

You can use one or more from above mentioned extension types and implement according to your requirement. 

For more information about these extension types, see [Extension Types](#ExtensionTypes).

To install and implement the siddhi-io extension archetype, follow the procedure below:

1. Issue the following command from your CLI.
            
                mvn archetype:generate
                    -DarchetypeGroupId=org.wso2.siddhi.extension.archetype
                    -DarchetypeArtifactId=siddhi-archetype-execution
                    -DgroupId=org.wso2.extension.siddhi.execution
                    -Dversion=1.0.0-SNAPSHOT
            
2. Enter the required execution name in the message that pops up as shown in the example below.
           
            Define value for property 'executionType': ML
            
3. To confirm that all property values are correct, type `Y` in the console. If not, press `N`.
                  
**siddhi-io**

Siddhi-io provides following extension types:

* Sink
* Source

You can use one or more from above mentioned extension types and implement according to your requirement. siddhi-io is generally used to work with IO operations as follows:
 * The Source extension type gets inputs to your Siddhi application.
 * The Sink extension publishes outputs from your Siddhi application. 

For more information about these extension types, see [Extension Types](#ExtensionTypes).

To implement the siddhi-io extension archetype, follow the procedure below:
    
1. Issue the following command from your CLI.
                
          
               mvn archetype:generate
                   -DarchetypeGroupId=org.wso2.siddhi.extension.archetype
                   -DarchetypeArtifactId=siddhi-archetype-io
                   -DgroupId=org.wso2.extension.siddhi.io
                   -Dversion=1.0.0-SNAPSHOT
            
2. Enter the required execution name (the transport type in this scenario) in the message that pops up as shown in the example below.
           
         Define value for property 'typeOf_IO': http

3. To confirm that all property values are correct, type `Y` in the console. If not, press `N`.
         
**siddhi-map**

Siddhi-map provides following extension types,

* Sink Mapper
* Source Mapper

You can use one or more from above mentioned extension types and implement according to your requirement as follows.

* The Source Mapper maps events to a predefined data format (such as XML, JSON, binary, etc), and publishes them to external endpoints (such as E-mail, TCP, Kafka, HTTP, etc).
* The Sink Mapper also maps events to a predefined data format, but it does it at the time of publishing events from a Siddhi application.

For more information about these extension types, see [Extension Types](#ExtensionTypes).

To implement the siddhi-map extension archetype, follow the procedure below:
        
1. Issue the following command from your CLI.                
            
                mvn archetype:generate
                    -DarchetypeGroupId=org.wso2.siddhi.extension.archetype
                    -DarchetypeArtifactId=siddhi-archetype-map
                    -DgroupId=org.wso2.extension.siddhi.map
                    -Dversion=1.0.0-SNAPSHOT
            
2. Enter the required execution name (the map type in this scenario) in the message that pops up as shown in the example below.
       
            Define value for property 'mapType':CSV
    
3. To confirm that all property values are correct, type `Y` in the console. If not, press `N`.
                   
**siddhi-script**

Siddhi-script provides the `Script` extension type.

The script extension type allows you to write functions in other programming languages and execute them within Siddhi queries. Functions defined via scripts can be accessed in queries similar to any other inbuilt function. 

For more information about these extension types, see [Extension Types](#ExtensionTypes).

To implement the siddhi-script extension archetype, follow the procedure below:

1. Issue the following command from your CLI.                   
           
               mvn archetype:generate
                   -DarchetypeGroupId=org.wso2.siddhi.extension.archetype
                   -DarchetypeArtifactId=siddhi-archetype-script
                   -DgroupId=org.wso2.extension.siddhi.script
                   -Dversion=1.0.0-SNAPSHOT
           
2. Enter the required execution name in the message that pops up as shown in the example below.
       
         Define value for property 'typeOfScript':

3. To confirm that all property values are correct, type `Y` in the console. If not, press `N`.
       
**siddhi-store**

Siddhi-store provides the `Store` extension type.

The Store extension type allows you to work with data/events stored in various data stores through the table abstraction. 

For more information about these extension types, see [Extension Types](#ExtensionTypes).

To implement the siddhi-store extension archetype, follow the procedure below:

1. Issue the following command from your CLI.                      
   
               mvn archetype:generate
                  -DarchetypeGroupId=org.wso2.siddhi.extension.archetype
                  -DarchetypeArtifactId=siddhi-archetype-store
                  -DgroupId=org.wso2.extension.siddhi.store
                  -Dversion=1.0.0-SNAPSHOT
           
2. Enter the required execution name in the message that pops up as shown in the example below.
                          
          Define value for property 'storeType': RDBMS
    
3. To confirm that all property values are correct, type `Y` in the console. If not, press `N`.

## Configuring and Monitoring Siddhi Applications

### Threading and Asynchronous

When `@Async` annotation is added to the Streams it enable the Streams to introduce asynchronous and multi-threading 
behaviour. 

```sql
@Async(buffer.size='256', workers='2', batch.size.max='5')
define stream <stream name> (<attribute name> <attribute type>, <attribute name> <attribute type>, ... );
```
The following elements are configured with this annotation.

|Annotation| Description| Default Value|
| ------------- |-------------|-------------|
|`buffer.size`|The size of the event buffer that will be used to handover the execution to other threads. | - |
|`workers`|Number of worker threads that will be be used to process the buffered events.|`1`|
|`batch.size.max`|The maximum number of events that will be processed together by a worker thread at a given time.| `buffer.size`|

### Fault Streams

When the `@OnError` annotation is added to a stream definition, it handles failover scenarios that occur during runtime gracefully.

```sql
@OnError(action='on_error_action')
define stream <stream name> (<attribute name> <attribute type>, <attribute name> <attribute type>, ... );
```

The action parameter of the `@OnError` annotation defines the action to be executed during failure scenarios. 

The following action types can be specified via the `@OnError` annotation when defining a stream. If this annotation is not added, `LOG` is the action type by default.

* `LOG` : Logs the event with an error, and then drops the event.
* `STREAM`: A fault stream is automatically created for the base stream. The definition of the fault stream includes all the attributes of the base stream as well as an additional attribute named `_error`.
The events are inserted into the fault stream during a failure. The error identified is captured as the value for the `_error` attribute.
 
e.g., the following is a Siddhi application that includes the `@OnError` annotation to handle failures during runtime.

```sql
@OnError(name='STREAM')
define stream StreamA (symbol string, volume long);

from StreamA[custom:fault() > volume] 
insert into StreamB;

from !StreamA#log("Error Occured")
insert into tempStream;
``` 

`!StreamA`, fault stream is automatically created when you add the `@OnError` annotation. The definition of the corresponding fault stream is as follows.
```sql
!StreamA(symbol string, volume long, _error object)
``` 

If you include the `on.error` parameter in the sink configuration, failures are handled by Siddhi at the time the events are published from the `Sink`.
```sql
@sink(type='sink_type', on.error='on.error.action')
define stream <stream name> (<attribute name> <attribute type>, <attribute name> <attribute type>, ... );
```  

The action types that can be specified via the `on.error` parameter when configuring a sink are as follows. If this parameter is not included in the sink configuration, `LOG` is the action type by default.

* `LOG` : Logs the event with the error, and then drops the event.
* `WAIT` : The thread waits in the `back-off and re-trying` state, and reconnects once the connection is re-established.
* `STREAM`: Corresponding fault stream is populated with the failed event and the error while publishing. 

### Statistics

Use `@app:statistics` app level annotation to evaluate the performance of an application, you can enable the statistics of a Siddhi application to be published. This is done via the `@app:statistics` annotation that can be added to a Siddhi application as shown in the following example.

```sql
@app:statistics(reporter = 'console')
```
The following elements are configured with this annotation.

|Annotation| Description| Default Value|
| ------------- |-------------|-------------|
|`reporter`|The interface in which statistics for the Siddhi application are published. Possible values are as follows:<br/> `console`<br/> `jmx`|`console`|
|`interval`|The time interval (in seconds) at  which the statistics for the Siddhi application are reported.|`60`|
|`include`|If this parameter is added, only the types of metrics you specify are included in the reporting. The required metric types can be specified as a comma-separated list. It is also possible to use wild cards| All (*.*)|

The metrics are reported in the following format.
`org.wso2.siddhi.SiddhiApps.<SiddhiAppName>.Siddhi.<Component Type>.<Component Name>. <Metrics name>`

The following table lists the types of metrics supported for different Siddhi application component types.

|Component Type|Metrics Type|
| ------------- |-------------|
|Stream|Throughput<br/>The size of the buffer if parallel processing is enabled via the @async annotation.|
|Trigger|Throughput (Trigger and Stream)|
|Source|Throughput|
|Sink|Throughput|
|Mapper|Latency<br/>Input/output throughput<br/>
|Table|Memory<br/>Throughput (For all operations)<br/>Throughput (For all operations)|
|Query|Memory<br/>Latency|
|Window|Throughput (For all operations)<br/>Latency (For all operation)|
|Partition|Throughput (For all operations)<br/>Latency (For all operation)|



e.g., the following is a Siddhi application that includes the `@app` annotation to report performance statistics.

```sql
@App:name('TestMetrics')
@App:Statistics(reporter = 'console')

define stream TestStream (message string);

@info(name='logQuery')
from TestSream#log("Message:")
insert into TempSream;
```

Statistics are reported for this Siddhi application as shown in the extract below.

<details>
  <summary>Click to view the extract</summary>
11/26/17 8:01:20 PM ============================================================

 -- Gauges ----------------------------------------------------------------------
 org.wso2.siddhi.SiddhiApps.TestMetrics.Siddhi.Queries.logQuery.memory
              value = 5760
 org.wso2.siddhi.SiddhiApps.TestMetrics.Siddhi.Streams.TestStream.size
              value = 0
 
 -- Meters ----------------------------------------------------------------------
 org.wso2.siddhi.SiddhiApps.TestMetrics.Siddhi.Sources.TestStream.http.throughput
              count = 0
          mean rate = 0.00 events/second
      1-minute rate = 0.00 events/second
      5-minute rate = 0.00 events/second
     15-minute rate = 0.00 events/second
 org.wso2.siddhi.SiddhiApps.TestMetrics.Siddhi.Streams.TempSream.throughput
              count = 2
          mean rate = 0.04 events/second
      1-minute rate = 0.03 events/second
      5-minute rate = 0.01 events/second
     15-minute rate = 0.00 events/second
 org.wso2.siddhi.SiddhiApps.TestMetrics.Siddhi.Streams.TestStream.throughput
              count = 2
          mean rate = 0.04 events/second
      1-minute rate = 0.03 events/second
      5-minute rate = 0.01 events/second
     15-minute rate = 0.00 events/second
 
 -- Timers ----------------------------------------------------------------------
 org.wso2.siddhi.SiddhiApps.TestMetrics.Siddhi.Queries.logQuery.latency
              count = 2
          mean rate = 0.11 calls/second
      1-minute rate = 0.34 calls/second
      5-minute rate = 0.39 calls/second
     15-minute rate = 0.40 calls/second
                min = 0.61 milliseconds
                max = 1.08 milliseconds
               mean = 0.84 milliseconds
             stddev = 0.23 milliseconds
             median = 0.61 milliseconds
               75% <= 1.08 milliseconds
               95% <= 1.08 milliseconds
               98% <= 1.08 milliseconds
               99% <= 1.08 milliseconds
             99.9% <= 1.08 milliseconds


</details>

### Event Playback

When `@app:playback` annotation is added to the app, the timestamp of the event (specified via an attribute) is treated as the current time. This results in events being processed faster.
The following elements are configured with this annotation.

|Annotation| Description|
| ------------- |-------------|
|`idle.time`|If no events are received during a time interval specified (in milliseconds) via this element, the Siddhi system time is incremented by a number of seconds specified via the `increment` element.|
|`increment`|The number of seconds by which the Siddhi system time must be incremented if no events are received during the time interval specified via the `idle.time` element.|

e.g., In the following example, the Siddhi system time is incremented by two seconds if no events arrive for a time interval of 100 milliseconds.

`@app:playback(idle.time = '100 millisecond', increment = '2 sec') `
