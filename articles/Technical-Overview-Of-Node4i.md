# Under the Hood: A Technical Overview of Node4i Platform Technologies

Node4i is a software platform designed specifically for industrial and IT/OT environments. With a focus on data management, data processing, data analysis, and visualization, Node4i provides organizations with the tools they need to effectively collect, process, and store IIoT data in the cloud and edge. Under the hood, Node4i is built on a combination of cutting-edge technologies that provide the scalability, security, and performance required for modern IIoT solutions.

## Core Platform: OPC UA

At the core of Node4i is the OPC UA standards, a comprehensive software development kit that provides access to the platform's various APIs and tools. The OPC UA allows developers to integrate Node4i into their existing systems, allowing for seamless data transfer and processing.

## Database: Redis

Node4i uses Redis as its database, which is an in-memory data structure store used as a database, cache, and message broker. Redis is well-known for its high performance and scalability, making it a suitable choice for IIoT data storage and processing.

Mapping the OPC UA address space to Redis and back provides several benefits for industrial applications:

    1. Improved Data Accessibility: By mapping the OPC UA address space to Redis, data becomes more accessible and easier to retrieve. This allows for quicker decision making and more efficient data processing.

    2. Scalability: Redis is a highly scalable database, making it an ideal choice for industrial applications that require high performance and scalability.

    3. Enhanced Data Processing: Redis allows for real-time data processing, making it possible to quickly analyze and visualize large amounts of data.

    4. Improved Data Management: The mapping of the OPC UA address space to Redis provides improved data management capabilities, making it easier to manage and store large amounts of data.

    5. Security: Node4i provides RSA and AES encryption, ensuring that data is secure and protected.

    6. Interoperability: OPC UA is a widely used industrial communication standard, and by mapping the address space to Redis, Node4i provides increased interoperability with other industrial systems.

    7. Reduced Latency: By utilizing Redis, the latency involved in accessing and processing data is reduced, making it possible to access data in real-time.

mapping the OPC UA address space to Redis and back provides several benefits for industrial applications, including improved data accessibility, scalability, enhanced data processing, improved data management, security, interoperability, and reduced latency.

## Dashboard: Graphical User Dashboard in Vue

Node4i features a graphical user dashboard built using the Vue.js JavaScript framework. The dashboard provides a visual representation of the data being collected, processed, and stored in Node4i, making it easy for users to monitor and manage their IIoT data.

## Search-Engine: Redis-Search

Node4i also uses Redis as its search engine, allowing for quick and efficient searching and retrieval of data stored in the platform. This makes it easy for users to access the data they need, when they need it.

Redis-search is an index-based search engine that is built on top of Redis, an in-memory data structure store. One of the main benefits of using Redis-search is its performance, as it can handle high-speed search operations in real-time due to the in-memory nature of Redis.

    In the context of address space mapping in the Node4i platform, Redis-search provides a highly efficient way of searching for specific nodes in the address space, which is especially important for industrial IIoT applications where real-time data is critical. By indexing the address space in Redis-search, search operations can be executed in a matter of milliseconds, providing quick access to the information required for decision-making and control.

    Redis-search also offers several other benefits, such as full-text search capabilities, support for multiple languages, and the ability to sort and filter search results. These features provide an easy-to-use and powerful search solution that can be easily integrated into the Node4i platform.

    In terms of security, the data stored in Redis-search can be encrypted using the RSA or AES encryption algorithms provided by Node4i, ensuring that sensitive data remains secure even if the search engine is compromised. Additionally, Redis-search can be configured to use secure socket layer (SSL) or transport layer security (TLS) protocols to further enhance the security of data transmission.

    In conclusion, the use of Redis-search in the Node4i platform provides a highly efficient and secure way of searching for nodes in the address space. With its in-memory performance, full-text search capabilities, and support for encryption, it offers a powerful and flexible solution for IIoT applications.

## Security

Node4i places a strong emphasis on security, providing RSA and AES encryption to ensure the data stored and processed in the platform is secure. This is particularly important in industrial and IT/OT environments, where sensitive data is often being collected and processed.

Node4i provides various security mechanisms to secure communication between the devices and the platform. Some of the commonly used encryption methods and algorithms in Node4i include:

    - TLS (Transport Layer Security) - This is a protocol that provides secure communication over the internet. It encrypts the data that is being transmitted and authenticates the identity of the communicating parties.

    - RSA (Rivest-Shamir-Adleman) - This is a public-key cryptography algorithm that is commonly used for secure data transmission. In Node4i, RSA is used to secure the communication between the devices and the platform by encrypting the messages transmitted between them.

    - AES (Advanced Encryption Standard) - This is a symmetric encryption algorithm that is widely used for secure data transmission. In Node4i, AES is used to encrypt the data stored in the platform, ensuring that it is protected from unauthorized access.

    - HMAC (Hash-based Message Authentication Code) - This is a type of message authentication code that is calculated using a hash function. In Node4i, HMAC is used to verify the integrity of the messages transmitted between the devices and the platform, ensuring that they have not been tampered with during transmission.

In addition to these encryption methods, Node4i also provides access control mechanisms and authentication methods to further enhance the security of the platform.

## Performance Key Factors

Performance is a key factor in Node4i, as organizations are looking to collect, process, and store large amounts of IIoT data in real-time. To ensure the platform is able to handle the demands of industrial and IT/OT environments, Node4i has been optimized for performance, including the use of high-performance databases and search engines.

In conclusion, Node4i is a comprehensive software platform designed specifically for industrial and IT/OT environments. With its focus on data management, data processing, data analysis, and visualization, Node4i provides organizations with the tools they need to effectively collect, process, and store IIoT data in the cloud, while ensuring the data is secure and performance is optimized.

In our experiments, we aimed to evaluate the performance of Node4i for IoT data processing and management. We tested the platform with various scenarios and components, including reading 8000 data blocks from S7 Siemens with a Raspberry Pi, running 3000 components with a 1 second update rate and 3 GB of memory, and running 2000 components with 2 GB of memory in a Raspberry Pi.

The results showed that Node4i was able to handle the large amounts of data efficiently and effectively, with the Raspberry Pi having a RAM usage of 50 MB with a 1 second update rate and the 11th Gen Intel Core i7-1165G7 with a 2.80GHz CPU and 16 GB of RAM having a utilization rate of 64%. The platform also showed stability, with an uptime of 1:12:23:13 and a total of 280 processes and 4474 threads running.

In terms of memory management, Node4i was able to utilize the available resources effectively, with 2 GB of memory being available, 2 GB being cached, and 21.1 GB being committed. The paged pool was 392 MB and the non-paged pool was 549 MB, with the memory in use being 13.6 GB (compressed to 222 MB).

Overall, the experiments demonstrated the capabilities of Node4i in handling large amounts of IoT data and its efficiency in utilizing system resources. The platform's performance and stability make it an ideal choice for organizations looking to deploy IoT solutions.




