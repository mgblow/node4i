# Security

The Node4i implements a robust security architecture that complies with the OPC UA security specification. This specification outlines the security mechanisms and policies that are required to ensure secure communication between OPC UA clients and servers.

## OPC UA Security Mechanisms in Node4i

- Authentication: The OPC UA protocol supports various authentication methods, such as username/password, X.509 certificate-based authentication, and anonymous authentication.

- Encryption: OPC UA communication is encrypted using Advanced Encryption Standard (AES) algorithms to protect data from unauthorized access during transmission.

- Signing: OPC UA messages can be signed using asymmetric cryptography to ensure the authenticity and integrity of the message.

- Session management: OPC UA defines a secure session between a client and a server, which provides a secure communication channel for the exchange of OPC UA messages.

## Security Policies 

OPC UA security policies in Node4i define the security mechanisms and algorithms that will be used to secure OPC UA communication between a client and a server. The security policy used for a specific OPC UA session is negotiated between the client and the server, and determines the level of security that will be applied to the communication.

Here is a comprehensive explanation of all the OPC UA security policies implemented in Node4i:

- Basic128Rsa15: This security policy uses RSA encryption with a 128-bit key size for both data encryption and message signing. It also uses a 15-byte padding mechanism to enhance the security of the encryption process. The Basic128Rsa15 policy provides a good balance between security and performance, making it a suitable choice for many OPC UA applications.

- Basic256: This security policy uses RSA encryption with a 256-bit key size for both data encryption and message signing. The use of a larger key size provides a higher level of security compared to Basic128Rsa15, but also requires more computational resources. This policy is suitable for OPC UA applications that require a high level of security and have the computational resources to support it.

- Basic256Sha256: This security policy uses RSA encryption with a 256-bit key size for data encryption and SHA-256 for message signing. The use of SHA-256 for message signing provides a higher level of security compared to other policies that use RSA encryption alone. This policy is suitable for OPC UA applications that require a high level of security and have the computational resources to support it.

- Aes128: This security policy uses AES encryption with a 128-bit key size for both data encryption and message signing. The Aes128 policy provides a good balance between security and performance, and is suitable for many OPC UA applications.

- Aes256: This security policy uses AES encryption with a 256-bit key size for both data encryption and message signing. The use of a larger key size provides a higher level of security compared to Aes128, but also requires more computational resources. This policy is suitable for OPC UA applications that require a high level of security and have the computational resources to support it.

In conclusion, the OPC UA security policies implemented in Node4i provide a range of security options that meet the diverse security requirements of OPC UA applications. Developers can choose the policy that best meets their specific requirements for their OPC UA application, taking into account the level of security required and the computational resources available.

# END