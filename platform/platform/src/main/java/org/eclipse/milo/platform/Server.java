package org.eclipse.milo.platform;
/*
 * Copyright (c) 2021 the FANAP Infrastructure Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the FANAP INFRA.
 * which is available at https://www.fanap.ir/iiot/terms
 *
 * SPDX-License-Identifier: NONE
 * development-1.2
 */

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.CompositeValidator;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaRuntimeException;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.DefaultTrustListManager;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.util.CertificateUtil;
import org.eclipse.milo.opcua.stack.core.util.NonceUtil;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedHttpsCertificateBuilder;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;
import org.eclipse.milo.opcua.stack.server.security.DefaultServerCertificateValidator;
import org.eclipse.milo.platform.runtime.interfaces.UaInterface;
import org.eclipse.milo.platform.util.Props;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.*;

public class Server {

    private static int TCP_BIND_PORT = Integer.parseInt(Props.getProperty("app-tcp-port").toString());
    private static int HTTPS_BIND_PORT = Integer.parseInt(Props.getProperty("app-https-port").toString());
    private static int COMPONENT_THREAD_POOL = Integer.parseInt(Props.getProperty("component-thread-pool-size").toString());
    private static String APP_NAME = null;
    private static String HOST_NAME = Props.getProperty("app-host-name").toString();
    private static String REDIS_URL = Props.getProperty("redis-url").toString();

    static {
        // Required for SecurityPolicy.Aes256_Sha256_RsaPss s
        Security.addProvider(new BouncyCastleProvider());

        try {
            NonceUtil.blockUntilSecureRandomSeeded(10, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 4) {
            APP_NAME = args[0];
            HOST_NAME = args[1];
            TCP_BIND_PORT = Integer.parseInt(args[2]);
            HTTPS_BIND_PORT = Integer.parseInt(args[3]);
        } else if (args.length > 0) {
            throw new Exception("Too few arguments for startups!");
        }
        Server server = new Server();
        server.startup().get();

        final CompletableFuture<Void> future = new CompletableFuture<>();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> future.complete(null)));

        future.get();
    }

    private final OpcUaServer server;
    private final Namespace applicationNamespace;

    public Server() throws Exception {
        Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "server", "security");
        Files.createDirectories(securityTempDir);
        if (!Files.exists(securityTempDir)) {
            throw new Exception("unable to create security temp dir: " + securityTempDir);
        }

        File pkiDir = securityTempDir.resolve("pki").toFile();

        LoggerFactory.getLogger(getClass()).info("security dir: {}", securityTempDir.toAbsolutePath());
        LoggerFactory.getLogger(getClass()).info("security pki dir: {}", pkiDir.getAbsolutePath());

        KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);

        DefaultCertificateManager certificateManager = new DefaultCertificateManager(loader.getServerKeyPair(), loader.getServerCertificateChain());

        DefaultTrustListManager trustListManager = new DefaultTrustListManager(pkiDir);

        DefaultServerCertificateValidator certificateValidator = new DefaultServerCertificateValidator(trustListManager);

        KeyPair httpsKeyPair = SelfSignedCertificateGenerator.generateRsaKeyPair(2048);

        SelfSignedHttpsCertificateBuilder httpsCertificateBuilder = new SelfSignedHttpsCertificateBuilder(httpsKeyPair);
        httpsCertificateBuilder.setCommonName(HostnameUtil.getHostname());
        HostnameUtil.getHostnames(HOST_NAME).forEach(httpsCertificateBuilder::addDnsName);
        X509Certificate httpsCertificate = httpsCertificateBuilder.build();

        UsernameIdentityValidator identityValidator = new UsernameIdentityValidator(
                true,
                authChallenge -> {
                    String username = authChallenge.getUsername();
                    String password = authChallenge.getPassword();

                    boolean userOk = "user".equals(username) && "password1".equals(password);
                    boolean adminOk = "admin".equals(username) && "password2".equals(password);

                    return userOk || adminOk;
                }
        );

        X509IdentityValidator x509IdentityValidator = new X509IdentityValidator(c -> true);

        // If you need to use multiple certificates you'll have to be smarter than this.
        X509Certificate certificate = certificateManager.getCertificates().stream().findFirst().orElseThrow(() -> new UaRuntimeException(StatusCodes.Bad_ConfigurationError, "no certificate found"));

        // The configured application URI must match the one in the certificate(s)
        String applicationUri = CertificateUtil.getSanUri(certificate).orElseThrow(() -> new UaRuntimeException(StatusCodes.Bad_ConfigurationError, "certificate is missing the application URI"));

        Set<EndpointConfiguration> endpointConfigurations = createEndpointConfigurations(certificate);

        String productUri = "urn:fanap:ua-platform:" + UUID.randomUUID();
        OpcUaServerConfig serverConfig = OpcUaServerConfig.builder().setApplicationUri(applicationUri)
                .setApplicationName(LocalizedText.english(APP_NAME))
                .setEndpoints(endpointConfigurations)
                .setRedisUri(REDIS_URL)
                .setBuildInfo(new BuildInfo(productUri, "FANAP", APP_NAME, OpcUaServer.SDK_VERSION, "1.0.0", DateTime.now()))
                .setCertificateManager(certificateManager)
                .setTrustListManager(trustListManager)
                .setCertificateValidator(certificateValidator)
                .setHttpsKeyPair(httpsKeyPair)
                .setHttpsCertificateChain(new X509Certificate[]{httpsCertificate})
                .setIdentityValidator(new CompositeValidator(identityValidator, x509IdentityValidator))
                .setComponentsThreadPoolCount(COMPONENT_THREAD_POOL)
                .setProductUri(productUri).build();

        server = new OpcUaServer(serverConfig);

        applicationNamespace = new Namespace(server);
        applicationNamespace.startup();
        loadEventsAndComponents();

    }

    private void loadEventsAndComponents() {
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        UaInterface.getInstance(applicationNamespace.returnNodeContext()).loadComponents();
                        UaInterface.getInstance(applicationNamespace.returnNodeContext()).loadEvents();
                    }
                },
                5000
        );
    }

    private Set<EndpointConfiguration> createEndpointConfigurations(X509Certificate certificate) {
        Set<EndpointConfiguration> endpointConfigurations = new LinkedHashSet<>();
        List<String> bindAddresses = newArrayList();
        bindAddresses.add(HOST_NAME);
        Set<String> hostnames = new LinkedHashSet<>();
        hostnames.add(HostnameUtil.getHostname());
        hostnames.addAll(HostnameUtil.getHostnames(HOST_NAME));
        for (String bindAddress : bindAddresses) {
            for (String hostname : hostnames) {
                EndpointConfiguration.Builder builder = EndpointConfiguration.newBuilder().setBindAddress(bindAddress).setHostname(hostname).setPath("/" + APP_NAME).setCertificate(certificate).addTokenPolicies(USER_TOKEN_POLICY_ANONYMOUS, USER_TOKEN_POLICY_USERNAME, USER_TOKEN_POLICY_X509);


                EndpointConfiguration.Builder noSecurityBuilder = builder.copy()
                        .setSecurityPolicy(SecurityPolicy.None)
                        .setSecurityMode(MessageSecurityMode.None);

                endpointConfigurations.add(buildTcpEndpoint(noSecurityBuilder));
                endpointConfigurations.add(buildHttpsEndpoint(noSecurityBuilder));

                // TCP Basic256 / SignAndEncrypt
                endpointConfigurations.add(buildTcpEndpoint(builder.copy()
                        .setSecurityPolicy(SecurityPolicy.Basic256)
                        .setSecurityMode(MessageSecurityMode.SignAndEncrypt)));
                // TCP Basic256Sha256 / SignAndEncrypt
                endpointConfigurations.add(buildTcpEndpoint(builder.copy()
                        .setSecurityPolicy(SecurityPolicy.Basic256Sha256)
                        .setSecurityMode(MessageSecurityMode.SignAndEncrypt)));
                // TCP Basic128Rsa15 / SignAndEncrypt
                endpointConfigurations.add(buildTcpEndpoint(builder.copy()
                        .setSecurityPolicy(SecurityPolicy.Basic128Rsa15)
                        .setSecurityMode(MessageSecurityMode.SignAndEncrypt)));
                // TCP Aes128_Sha256_RsaOaep / SignAndEncrypt
                endpointConfigurations.add(buildTcpEndpoint(builder.copy()
                        .setSecurityPolicy(SecurityPolicy.Aes128_Sha256_RsaOaep)
                        .setSecurityMode(MessageSecurityMode.SignAndEncrypt)));
                // TCP Aes256_Sha256_RsaPss / SignAndEncrypt
                endpointConfigurations.add(buildTcpEndpoint(builder.copy()
                        .setSecurityPolicy(SecurityPolicy.Aes256_Sha256_RsaPss)
                        .setSecurityMode(MessageSecurityMode.SignAndEncrypt)));

                EndpointConfiguration.Builder discoveryBuilder = builder.copy().setPath("/" + APP_NAME + "/discovery").setSecurityPolicy(SecurityPolicy.None).setSecurityMode(MessageSecurityMode.None);

                endpointConfigurations.add(buildTcpEndpoint(discoveryBuilder));
            }
        }
        return endpointConfigurations;
    }

    private static EndpointConfiguration buildTcpEndpoint(EndpointConfiguration.Builder base) {
        return base.copy().setTransportProfile(TransportProfile.TCP_UASC_UABINARY).setBindPort(TCP_BIND_PORT).build();
    }

    private static EndpointConfiguration buildHttpsEndpoint(EndpointConfiguration.Builder base) {
        return base.copy().setTransportProfile(TransportProfile.HTTPS_UABINARY).setBindPort(HTTPS_BIND_PORT).build();
    }

    public OpcUaServer getServer() {
        return server;
    }

    public CompletableFuture<OpcUaServer> startup() {
        return server.startup();
    }

    public CompletableFuture<OpcUaServer> shutdown() {
        applicationNamespace.shutdown();

        return server.shutdown();
    }

}
