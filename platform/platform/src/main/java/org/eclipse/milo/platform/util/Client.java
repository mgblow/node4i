package org.eclipse.milo.platform.util;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.client.security.DefaultClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.DefaultTrustListManager;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public interface Client {


    default OpcUaClient uaConnectToServer(String ip, String port, String username, String password, String policy) throws Exception {
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
        return OpcUaClient.create(
                ip + ":" + port,
                endpoints -> endpoints.stream().filter(e -> SecurityPolicy.valueOf(policy).getUri().equals(e.getSecurityPolicyUri())).findFirst(),
                configBuilder ->
                        configBuilder
                                .setApplicationName(LocalizedText.english("ua-platform master-server opc ua client"))
                                .setApplicationUri("urn:fanap:ua-platform:master-server:client")
                                .setKeyPair(loader.getClientKeyPair())
                                .setCertificate(loader.getClientCertificate())
                                .setCertificateChain(loader.getClientCertificateChain())
                                .setCertificateValidator(certificateValidator)
                                .setIdentityProvider(new UsernameProvider(username, password))
                                .setRequestTimeout(uint(5000))
                                .build()
        );
    }
}
