/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.stack.server;

import org.eclipse.milo.opcua.stack.core.Stack;
import org.eclipse.milo.opcua.stack.core.channel.EncodingLimits;
import org.eclipse.milo.opcua.stack.core.security.CertificateManager;
import org.eclipse.milo.opcua.stack.core.security.TrustListManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.server.security.ServerCertificateValidator;
import org.jetbrains.annotations.Nullable;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class UaStackServerConfigBuilder {

    private Set<EndpointConfiguration> endpoints = new HashSet<>();

    private LocalizedText applicationName = LocalizedText.english("server application name not configured");

    private String applicationUri = "server application uri not configured";

    private String productUri = "server product uri not configured";

    private String redisUri = "default redis uri to access persistence";

    private EncodingLimits encodingLimits = EncodingLimits.DEFAULT;

    private UInteger minimumSecureChannelLifetime = uint(60_000);
    private UInteger maximumSecureChannelLifetime = uint(60_000 * 60 * 24);

    private CertificateManager certificateManager;
    private TrustListManager trustListManager;
    private Integer componentsThreadPoolCount;
    private ServerCertificateValidator certificateValidator;

    private KeyPair httpsKeyPair;
    private X509Certificate[] httpsCertificate;

    private ExecutorService executor;

    public UaStackServerConfigBuilder setEndpoints(Set<EndpointConfiguration> endpointConfigurations) {
        this.endpoints = endpointConfigurations;
        return this;
    }
    public UaStackServerConfigBuilder setComponentsThreadPoolCount(Integer componentsThreadPoolCount) {
        this.componentsThreadPoolCount = componentsThreadPoolCount;
        return this;
    }
    public Integer getComponentsThreadPoolCount() {
        return componentsThreadPoolCount;
    }

    public UaStackServerConfigBuilder setApplicationName(LocalizedText applicationName) {
        this.applicationName = applicationName;
        return this;
    }

    public UaStackServerConfigBuilder setApplicationUri(String applicationUri) {
        this.applicationUri = applicationUri;
        return this;
    }

    public UaStackServerConfigBuilder setProductUri(String productUri) {
        this.productUri = productUri;
        return this;
    }

    public UaStackServerConfigBuilder setRedisUri(String redisUri) {
        this.redisUri = redisUri;
        return this;
    }

    public UaStackServerConfigBuilder setEncodingLimits(EncodingLimits encodingLimits) {
        this.encodingLimits = encodingLimits;
        return this;
    }

    public UaStackServerConfigBuilder setMinimumSecureChannelLifetime(UInteger minimumSecureChannelLifetime) {
        this.minimumSecureChannelLifetime = minimumSecureChannelLifetime;
        return this;
    }

    public UaStackServerConfigBuilder setMaximumSecureChannelLifetime(UInteger maximumSecureChannelLifetime) {
        this.maximumSecureChannelLifetime = maximumSecureChannelLifetime;
        return this;
    }

    public UaStackServerConfigBuilder setCertificateManager(CertificateManager certificateManager) {
        this.certificateManager = certificateManager;
        return this;
    }

    public UaStackServerConfigBuilder setTrustListManager(TrustListManager trustListManager) {
        this.trustListManager = trustListManager;
        return this;
    }

    public UaStackServerConfigBuilder setCertificateValidator(ServerCertificateValidator certificateValidator) {
        this.certificateValidator = certificateValidator;
        return this;
    }

    public UaStackServerConfigBuilder setHttpsKeyPair(KeyPair httpsKeyPair) {
        this.httpsKeyPair = httpsKeyPair;
        return this;
    }

    public UaStackServerConfigBuilder setHttpsCertificateChain(X509Certificate[] httpsCertificate) {
        this.httpsCertificate = httpsCertificate;
        return this;
    }

    @Deprecated
    public UaStackServerConfigBuilder setHttpsCertificate(X509Certificate httpsCertificate) {
        this.httpsCertificate = new X509Certificate[]{httpsCertificate};
        return this;
    }

    public UaStackServerConfigBuilder setExecutor(ExecutorService executor) {
        this.executor = executor;
        return this;
    }

    public UaStackServerConfig build() {
        if (executor == null) {
            executor = Stack.sharedExecutor();
        }

        return new UaStackServerConfigImpl(endpoints, applicationName, applicationUri, productUri, redisUri, encodingLimits, minimumSecureChannelLifetime, maximumSecureChannelLifetime, certificateManager, trustListManager, certificateValidator, httpsKeyPair, httpsCertificate,componentsThreadPoolCount, executor);
    }


    private static class UaStackServerConfigImpl implements UaStackServerConfig {

        private final Set<EndpointConfiguration> endpointConfigurations;

        private final LocalizedText applicationName;
        private final String applicationUri;
        private final String productUri;

        private final String redisUri;

        private final EncodingLimits encodingLimits;

        private final UInteger minimumSecureChannelLifetime;
        private final UInteger maximumSecureChannelLifetime;

        private final CertificateManager certificateManager;
        private final ServerCertificateValidator certificateValidator;
        private final TrustListManager trustListManager;

        private final KeyPair httpsKeyPair;
        private final X509Certificate[] httpsCertificate;

        private final ExecutorService executor;

        private final Integer componentsThreadPoolCount;

        UaStackServerConfigImpl(Set<EndpointConfiguration> endpointConfigurations, LocalizedText applicationName, String applicationUri, String productUri, String redisUri, EncodingLimits encodingLimits, UInteger minimumSecureChannelLifetime, UInteger maximumSecureChannelLifetime, CertificateManager certificateManager, TrustListManager trustListManager, ServerCertificateValidator certificateValidator, @Nullable KeyPair httpsKeyPair, @Nullable X509Certificate[] httpsCertificate , Integer componentsThreadPoolCount, ExecutorService executor) {

            this.endpointConfigurations = endpointConfigurations;
            this.applicationName = applicationName;
            this.applicationUri = applicationUri;
            this.productUri = productUri;
            this.redisUri = redisUri;
            this.encodingLimits = encodingLimits;
            this.minimumSecureChannelLifetime = minimumSecureChannelLifetime;
            this.maximumSecureChannelLifetime = maximumSecureChannelLifetime;
            this.trustListManager = trustListManager;
            this.certificateManager = certificateManager;
            this.certificateValidator = certificateValidator;
            this.httpsKeyPair = httpsKeyPair;
            this.httpsCertificate = httpsCertificate;
            this.componentsThreadPoolCount = componentsThreadPoolCount;
            this.executor = executor;
        }

        @Override
        public Set<EndpointConfiguration> getEndpoints() {
            return endpointConfigurations;
        }

        @Override
        public LocalizedText getApplicationName() {
            return applicationName;
        }

        @Override
        public String getApplicationUri() {
            return applicationUri;
        }

        @Override
        public String getRedisUri() {
            return redisUri;
        }

        @Override
        public String getProductUri() {
            return productUri;
        }

        @Override
        public EncodingLimits getEncodingLimits() {
            return encodingLimits;
        }

        @Override
        public UInteger getMinimumSecureChannelLifetime() {
            return minimumSecureChannelLifetime;
        }

        @Override
        public UInteger getMaximumSecureChannelLifetime() {
            return maximumSecureChannelLifetime;
        }

        @Override
        public CertificateManager getCertificateManager() {
            return certificateManager;
        }

        @Override
        public TrustListManager getTrustListManager() {
            return trustListManager;
        }

        @Override
        public Integer getComponentsThreadPoolCount() {
            return componentsThreadPoolCount ;
        }

        @Override
        public ServerCertificateValidator getCertificateValidator() {
            return certificateValidator;
        }

        @Override
        public Optional<KeyPair> getHttpsKeyPair() {
            return Optional.ofNullable(httpsKeyPair);
        }

        @Override
        public Optional<X509Certificate[]> getHttpsCertificateChain() {
            return Optional.ofNullable(httpsCertificate);
        }

        @Override
        public ExecutorService getExecutor() {
            return executor;
        }

    }

}
