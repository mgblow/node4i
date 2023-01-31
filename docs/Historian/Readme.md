# Node4i Historian

Node4i provides a powerful historian feature that enables users to store, retrieve and analyze large amounts of historical data from industrial automation systems. The historian allows you to monitor performance and trends over time, providing a valuable tool for improving processes and making informed decisions.

The historian in Node4i is designed to handle high volumes of data from multiple sources, making it ideal for use in a wide range of industrial applications. With its ability to store and analyze data over long periods of time, the historian provides valuable insights into the performance of your systems, allowing you to make informed decisions and take corrective action where necessary.

## Benefits of Node4i Historian
- Store historical data in a centralized location
- Easy access to data for analysis and reporting
- Retrieve historical data in real-time for decision making and troubleshooting
- Keep track of changes and trends over time
- Improved efficiency and productivity

## How Node4i Historian Works
Node4i Historian is integrated with the Node4i platform, allowing for seamless storage and retrieval of historical data. The historian captures data from IO management, real-time data processing, and other components, and stores it in a centralized database.

Node4i offers two ways for archiving your desired nodes:

1. Simple Archive Way
   This method takes a snapshot of the node's value at the time of every change.

2. Swing Door Linear Algorithm Compression
   This method allows you to specify pre-configured parameters for the swing door linear (SDL) algorithm and archive the node's value based on that algorithm.


## Use Cases of Node4i Historian in Industrial Automation
- In a manufacturing environment, the historian can store production data and track performance over time to identify areas for improvement.
- In a mining environment, the historian can store data related to the production and performance of mining equipment, helping to optimize processes and improve efficiency.
- In a melting environment, the historian can store data related to the temperature and pressure of the melting process, helping to ensure quality and safety.

## Integrating with 3rd Party Applications
Node4i Historian can be integrated with 3rd party applications, such as data visualization tools, to provide a comprehensive solution for real-time data analysis and reporting. By leveraging the historian's data, these tools can provide valuable insights and help make informed decisions.



Node4i Historian allows you to choose which nodes you want to archive, either through a simple archive method or by using the swing door linear (SDL) compression algorithm.

## Simple Archive Method

The simple archive method takes a snapshot of a node's value whenever it changes, allowing you to track its value over time.

## Swing Door Linear Compression Algorithm

The SDL compression algorithm allows you to specify pre-configured parameters and archive a node's value based on the algorithm's compression. This enables you to minimize the amount of data stored while still preserving the information you need.

## Accessing Archived Data

There are two ways to access Node4i's archived data:

1. UaInterface

The [UaInterface](../Runtime-Components/Interfaces/UaInterface.md) provides a connection between Node4i's Universal Data Trees (UDT) and its Runtime & Components, enabling you to execute function scripts and access historical node data in time.

2. Dashboard

The dashboard provides a graphical table of archived values and allows you to export your data to an Excel file. This provides a convenient way to access and analyze your archived data.

