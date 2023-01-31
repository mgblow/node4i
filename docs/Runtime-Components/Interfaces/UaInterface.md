# UaInterface

UaInterface is a Java API that enables interaction between the Opc Ua Address Space (UDT in term) and Script Engine (
Graalvm Python, JS).
## Benefits

1. The UaInterface API provides a simple and convenient way to interact with the Opc Ua Address Space from a script
   engine.
2. The use of synchronized methods ensures thread-safety and prevents race conditions.
3. The API provides methods for firing events, retrieving event information, acknowledging events, and creating OPC UA
   clients.
4. The JSON string returned by the eventInfo method is easy to parse and use in other applications.
5. The exception handling in the methods ensures that errors are properly logged and reported.

# Available functionalities in UaInterface

The following are the list of java methods in `UaInterface` that allow communication with UDT nodes and trigger functions:

## Event Management
- `public synchronized String eventInfo(String eventDefinitionId)`
- `public synchronized void ack(String eventId, String comment)`
- `public synchronized boolean state(String eventDefinitionId)`
- `public synchronized void fire(String eventDefinitionId)`

## Data Management
- `public synchronized void writeValueToOpcUa(String deviceName, String identifier, int namespaceIndex, String identifierDataType, String value, String dataType)`
- `public synchronized Map<String, String> getNodeValues(String[] identifiers)`
- `public synchronized String getNodeValue(String identifier)`
- `public synchronized String getOutputFromComponent(String identifier)`
- `public synchronized String getSimpleArchivedValue(String identifier, Long startTime, Long endTime, Long offset, Long limit)`

## Messaging
- `public synchronized void sendMessageToKafka(String ioName, String topic, String message)`
- `public synchronized void sendMessageToMqtt(String ioName, String topic, Object message)`
- `public synchronized void sendMessageToRabbitMQ(String deviceName, String queueName, String message)`
- `public synchronized void sendMessageToActiveMQ(String ioName, String queueName, String message)`

## Data Persistence
- `public synchronized void saveNode(String location, String name, String value)`


## `eventInfo`
The `eventInfo` method returns a string representation of the event information for a given event definition identifier.

## `ack`
The `ack` method is used to acknowledge the occurrence of an event. The method takes the event identifier and a comment as input arguments.

## `state`
The `state` method returns a boolean indicating the state of a given event definition identifier.

## `fire`
The `fire` method triggers an event based on the given event definition identifier.

## `writeValueToOpcUa`
The `writeValueToOpcUa` method writes a value to an OPC UA node. It takes the device name, identifier, namespace index, identifier data type, value, and data type as input arguments.

## `sendMessageToKafka`
The `sendMessageToKafka` method sends a message to a Kafka topic. It takes the I/O name, topic, and message as input arguments.

## `sendMessageToMqtt`
The `sendMessageToMqtt` method sends a message to an MQTT topic. It takes the I/O name, topic, and message as input arguments.

## `sendMessageToRabbitMQ`
The `sendMessageToRabbitMQ` method sends a message to a RabbitMQ queue. It takes the device name, queue name, and message as input arguments.

## `sendMessageToActiveMQ`
The `sendMessageToActiveMQ` method sends a message to an ActiveMQ queue. It takes the I/O name, queue name, and message as input arguments.

## `saveNode`
The `saveNode` method saves a node at a given location with a given name and value.

## `getNodeValues`
The `getNodeValues` method returns a map of node values for the given node identifiers.

## `getNodeValue`
The `getNodeValue` method returns the value of a node for the given identifier.

## `getOutputFromComponent`
The `getOutputFromComponent` method returns the output of a component for a given identifier.

## `getSimpleArchivedValue`
The `getSimpleArchivedValue` method returns the archived value for a given identifier, start time, end time, offset, and limit.
