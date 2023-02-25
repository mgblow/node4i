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

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.eclipse.milo.opcua.stack.core.channel.EncodingLimits;
import org.eclipse.milo.opcua.stack.core.security.CertificateManager;
import org.eclipse.milo.opcua.stack.core.security.TrustListManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.ApplicationDescription;
import org.eclipse.milo.opcua.stack.server.security.ServerCertificateValidator;

public interface UaStackServerConfig {

    /**
     * @return the {@link EndpointConfiguration}s for this server.
     */
    Set<EndpointConfiguration> getEndpoints();

    /**
     * Get the application name for the server.
     * <p/>
     * This will be used in the {@link ApplicationDescription} returned to clients.
     *
     * @return the application name for the server.
     */
    LocalizedText getApplicationName();

    /**
     * Get the application uri for the server.
     * <p/>
     * This will be used in the {@link ApplicationDescription} returned to clients.
     * <p/>
     * <b>The application uri must match the application uri used on the server's application instance certificate.</b>
     *
     * @return the application uri for the server.
     */
    String getApplicationUri();


    String getRedisUri();

    /**
     * Get the product uri for the server.
     * <p/>
     * This will be used in the {@link ApplicationDescription} returned to clients.
     *
     * @return the product uri for the server.
     */
    String getProductUri();

    /**
     * @return the configured {@link EncodingLimits}.
     */
    EncodingLimits getEncodingLimits();

    /**
     * @return the minimum allowable secure channel lifetime, in milliseconds.
     */
    UInteger getMinimumSecureChannelLifetime();

    /**
     * @return the maximum allowable secure channel lifetime, in milliseconds.
     */
    UInteger getMaximumSecureChannelLifetime();

    /**
     * @return the {@link CertificateManager} for this server.
     */
    CertificateManager getCertificateManager();

    /**
     * @return the {@link TrustListManager} for this server.
     */
    TrustListManager getTrustListManager();

    Integer getComponentsThreadPoolCount();

    /**
     * @return the {@link ServerCertificateValidator} for this server.
     */
    ServerCertificateValidator getCertificateValidator();

    /**
     * @return the {@link KeyPair} used for SSL/TLS with HTTPS endpoints.
     */
    Optional<KeyPair> getHttpsKeyPair();

    /**
     * @return the {@link X509Certificate} used for SSL/TLS with HTTPS endpoints.
     * @deprecated This will only return the leaf certificate, use @{{@link #getHttpsCertificateChain()}} to get the
     * full chain.
     */
    @Deprecated
    default Optional<X509Certificate> getHttpsCertificate() {
        return getHttpsCertificateChain().flatMap(chain -> Optional.ofNullable(chain[0]));
    }

    /**
     * @return the {@link X509Certificate} used for SSL/TLS with HTTPS endpoints.
     */
    Optional<X509Certificate[]> getHttpsCertificateChain();

    /**
     * @return the {@link ExecutorService} for this server.
     */
    ExecutorService getExecutor();

    /**
     * @return a new {@link UaStackServerConfigBuilder}.
     */
    static UaStackServerConfigBuilder builder() {
        return new UaStackServerConfigBuilder();
    }

    /**
     * Copy the values from an existing {@link UaStackServerConfig} into a new {@link UaStackServerConfigBuilder}.
     * <p>
     * This builder can be used to make any desired modifications before invoking
     * {@link UaStackServerConfigBuilder#build()} to produce a new config.
     *
     * @param config the {@link UaStackServerConfig} to copy from.
     * @return a {@link UaStackServerConfigBuilder} pre-populated with values from {@code config}.
     */
    static UaStackServerConfigBuilder copy(UaStackServerConfig config) {
        UaStackServerConfigBuilder builder = builder();

        builder.setEndpoints(config.getEndpoints());
        builder.setApplicationName(config.getApplicationName());
        builder.setApplicationUri(config.getApplicationUri());
        builder.setProductUri(config.getProductUri());
        builder.setEncodingLimits(config.getEncodingLimits());
        builder.setMinimumSecureChannelLifetime(config.getMinimumSecureChannelLifetime());
        builder.setMaximumSecureChannelLifetime(config.getMaximumSecureChannelLifetime());
        builder.setCertificateManager(config.getCertificateManager());
        builder.setTrustListManager(config.getTrustListManager());
        builder.setCertificateValidator(config.getCertificateValidator());
        builder.setHttpsKeyPair(config.getHttpsKeyPair().orElse(null));
        builder.setHttpsCertificateChain(config.getHttpsCertificateChain().orElse(null));
        builder.setExecutor(config.getExecutor());

        return builder;
    }

    /**
     * Copy the values from an existing {@link UaStackServerConfig} into a new {@link UaStackServerConfigBuilder}
     * and then submit the builder to the provided consumer for modification.
     *
     * @param config   the {@link UaStackServerConfig} to copy from.
     * @param consumer a {@link Consumer} that may modify the builder.
     * @return a {@link UaStackServerConfig} built from the builder provided to {@code consumer}.
     */
    static UaStackServerConfig copy(
        UaStackServerConfig config,
        Consumer<UaStackServerConfigBuilder> consumer) {

        UaStackServerConfigBuilder builder = copy(config);

        consumer.accept(builder);

        return builder.build();
    }

}
