package org.eclipse.milo.platform.gateway.interfaces;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.client.security.DefaultClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.DefaultTrustListManager;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.platform.util.KeyStoreLoader;
import org.eclipse.milo.platform.util.StringUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.Predicate;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public interface OpcUaDeviceClient {
    default OpcUaClient createOpcUaClient(String url, String policy, String username, String password) throws Exception {
        SecurityPolicy securityPolicy = SecurityPolicy.valueOf(policy);
        Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "client", "security");
        Files.createDirectories(securityTempDir);
        if (!Files.exists(securityTempDir)) {
            throw new Exception("unable to create security dir: " + securityTempDir);
        }

        LoggerFactory.getLogger(getClass()).info("security temp dir: {}", securityTempDir.toAbsolutePath());
        File pkiDir = securityTempDir.resolve("pki").toFile();
        KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);
        DefaultTrustListManager trustListManager = new DefaultTrustListManager(pkiDir);
        DefaultClientCertificateValidator certificateValidator = new DefaultClientCertificateValidator(trustListManager);
        SecurityPolicy finalSecurityPolicy = securityPolicy;
        String finalUsername = username;
        String finalPassword = password;
        return OpcUaClient.create(url, endpoints -> endpoints.stream().filter(this.endpointFilter(finalSecurityPolicy)).findFirst(), configBuilder -> {
                    if (StringUtils.isNullOrEmpty(finalUsername) || StringUtils.isNullOrEmpty(finalPassword)) {
                        return configBuilder.setApplicationName(LocalizedText.english("unified OPC UA client")).setApplicationUri("urn:fanap:unified:opc-ua:client-" + UUID.randomUUID()).setKeyPair(loader.getClientKeyPair()).setCertificate(loader.getClientCertificate()).setCertificateChain(loader.getClientCertificateChain()).setCertificateValidator(certificateValidator).setRequestTimeout(uint(10000)).build();
                    } else {
                        return configBuilder.setApplicationName(LocalizedText.english("unified OPC UA client")).setApplicationUri("urn:fanap:unified:opc-ua:client-" + UUID.randomUUID()).setKeyPair(loader.getClientKeyPair()).setCertificate(loader.getClientCertificate()).setCertificateChain(loader.getClientCertificateChain()).setCertificateValidator(certificateValidator).setIdentityProvider(this.getIdentityProvider(finalUsername, finalPassword)).setRequestTimeout(uint(10000)).build();
                    }
                }

        );
    }


    default Predicate<EndpointDescription> endpointFilter(SecurityPolicy securityPolicy) {
        return e -> securityPolicy.getUri().equals(e.getSecurityPolicyUri());
    }

    default IdentityProvider getIdentityProvider(String username, String password) {
        return new UsernameProvider(username, password);
    }

}
