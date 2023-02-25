/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server;

import java.net.InetAddress;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.eclipse.milo.opcua.sdk.server.diagnostics.SessionDiagnostics;
import org.eclipse.milo.opcua.sdk.server.diagnostics.SessionSecurityDiagnostics;
import org.eclipse.milo.opcua.sdk.server.services.DefaultAttributeHistoryServiceSet;
import org.eclipse.milo.opcua.sdk.server.services.DefaultAttributeServiceSet;
import org.eclipse.milo.opcua.sdk.server.services.DefaultMethodServiceSet;
import org.eclipse.milo.opcua.sdk.server.services.DefaultMonitoredItemServiceSet;
import org.eclipse.milo.opcua.sdk.server.services.DefaultNodeManagementServiceSet;
import org.eclipse.milo.opcua.sdk.server.services.DefaultQueryServiceSet;
import org.eclipse.milo.opcua.sdk.server.services.DefaultSubscriptionServiceSet;
import org.eclipse.milo.opcua.sdk.server.services.DefaultViewServiceSet;
import org.eclipse.milo.opcua.sdk.server.services.helpers.BrowseHelper.BrowseContinuationPoint;
import org.eclipse.milo.opcua.sdk.server.subscriptions.SubscriptionManager;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.UserTokenType;
import org.eclipse.milo.opcua.stack.core.types.structured.AnonymousIdentityToken;
import org.eclipse.milo.opcua.stack.core.types.structured.ApplicationDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.CancelResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.IssuedIdentityToken;
import org.eclipse.milo.opcua.stack.core.types.structured.UserIdentityToken;
import org.eclipse.milo.opcua.stack.core.types.structured.UserNameIdentityToken;
import org.eclipse.milo.opcua.stack.core.types.structured.X509IdentityToken;
import org.eclipse.milo.opcua.stack.core.util.CertificateUtil;
import org.eclipse.milo.opcua.stack.server.services.NodeManagementServiceSet;
import org.eclipse.milo.opcua.stack.server.services.ServiceRequest;
import org.eclipse.milo.opcua.stack.server.services.SessionServiceSet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class Session implements SessionServiceSet {

    private static final int IDENTITY_HISTORY_MAX_SIZE = 10;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<LifecycleListener> listeners = Lists.newCopyOnWriteArrayList();

    private final SubscriptionManager subscriptionManager;

    private final LinkedList<String> clientUserIdHistory = new LinkedList<>();

    private final Map<ByteString, BrowseContinuationPoint> browseContinuationPoints = Maps.newConcurrentMap();

    private volatile Object identityObject;
    private volatile UserIdentityToken identityToken;

    private volatile ByteString lastNonce = ByteString.NULL_VALUE;

    private volatile long lastActivityNanos = System.nanoTime();
    private volatile ScheduledFuture<?> checkTimeoutFuture;

    private final DefaultAttributeServiceSet attributeServiceSet;
    private final DefaultAttributeHistoryServiceSet attributeHistoryServiceSet;
    private final DefaultMethodServiceSet methodServiceSet;
    private final DefaultMonitoredItemServiceSet monitoredItemServiceSet;
    private final DefaultNodeManagementServiceSet nodeManagementServiceSet;
    private final DefaultQueryServiceSet queryServiceSet;
    private final DefaultSubscriptionServiceSet subscriptionServiceSet;
    private final DefaultViewServiceSet viewServiceSet;

    private volatile EndpointDescription endpoint;
    private volatile long secureChannelId;
    private volatile SecurityConfiguration securityConfiguration;
    private volatile InetAddress clientAddress;
    private volatile String[] localeIds;
    private volatile DateTime lastContactTime;

    private final DateTime connectTime = DateTime.now();
    private final SessionDiagnostics sessionDiagnostics;
    private final SessionSecurityDiagnostics sessionSecurityDiagnostics;

    private final OpcUaServer server;
    private final NodeId sessionId;
    private final String sessionName;
    private final Duration sessionTimeout;
    private final ApplicationDescription clientDescription;
    private final String serverUri;
    private final UInteger maxResponseMessageSize;

    public Session(
        OpcUaServer server,
        NodeId sessionId,
        String sessionName,
        Duration sessionTimeout,
        ApplicationDescription clientDescription,
        String serverUri,
        UInteger maxResponseMessageSize,
        EndpointDescription endpoint,
        long secureChannelId,
        SecurityConfiguration securityConfiguration
    ) {

        this.server = server;
        this.sessionId = sessionId;
        this.sessionName = sessionName;
        this.sessionTimeout = sessionTimeout;
        this.clientDescription = clientDescription;
        this.serverUri = serverUri;
        this.maxResponseMessageSize = maxResponseMessageSize;
        this.secureChannelId = secureChannelId;
        this.securityConfiguration = securityConfiguration;
        this.endpoint = endpoint;

        sessionDiagnostics = new SessionDiagnostics(this);
        sessionSecurityDiagnostics = new SessionSecurityDiagnostics(this);

        subscriptionManager = new SubscriptionManager(this, server);

        attributeServiceSet = new DefaultAttributeServiceSet();
        attributeHistoryServiceSet = new DefaultAttributeHistoryServiceSet();
        methodServiceSet = new DefaultMethodServiceSet();
        monitoredItemServiceSet = new DefaultMonitoredItemServiceSet(subscriptionManager);
        nodeManagementServiceSet = new DefaultNodeManagementServiceSet();
        queryServiceSet = new DefaultQueryServiceSet();
        subscriptionServiceSet = new DefaultSubscriptionServiceSet(subscriptionManager);
        viewServiceSet = new DefaultViewServiceSet(server.getConfig().getExecutor());

        checkTimeoutFuture = server.getScheduledExecutorService().schedule(
            this::checkTimeout, sessionTimeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    public OpcUaServer getServer() {
        return server;
    }

    public long getSecureChannelId() {
        return secureChannelId;
    }

    public SecurityConfiguration getSecurityConfiguration() {
        return securityConfiguration;
    }

    public EndpointDescription getEndpoint() {
        return endpoint;
    }

    @Nullable
    public Object getIdentityObject() {
        return identityObject;
    }

    @Nullable
    public UserIdentityToken getIdentityToken() {
        return identityToken;
    }

    @Nullable
    public UserTokenType getTokenType() {
        UserIdentityToken token = identityToken;

        return token != null ? getTokenType(token) : null;
    }

    /**
     * The client user id identifies the user of the client requesting an action. The client user id is obtained from
     * the UserIdentityToken passed in the ActivateSession call.
     * <p>
     * If the UserIdentityToken is a UserNameIdentityToken then the ClientUserId is the UserName.
     * <p>
     * If the UserIdentityToken is an X509IdentityToken then the ClientUserId is the X509 Subject Name of the
     * Certificate.
     * <p>
     * If the UserIdentityToken is an IssuedIdentityToken then the ClientUserId shall be a string that represents the
     * owner of the token. The best choice for the string depends on the type of IssuedIdentityToken.
     * <p>
     * If an AnonymousIdentityToken was used, the value is null.
     *
     * @return the clientUserId of this {@link Session}.
     */
    @Nullable
    public String getClientUserId() {
        return getClientUserId(identityToken);
    }

    /**
     * @return a list containing the (possibly abbreviated) history of client user ids. This list may contain null
     * entries.
     * @see #getClientUserId()
     */
    public List<String> getClientUserIdHistory() {
        synchronized (clientUserIdHistory) {
            return new ArrayList<>(clientUserIdHistory);
        }
    }

    public Map<ByteString, BrowseContinuationPoint> getBrowseContinuationPoints() {
        return browseContinuationPoints;
    }

    public void setSecureChannelId(long secureChannelId) {
        this.secureChannelId = secureChannelId;
    }

    public void setIdentityObject(Object identityObject, UserIdentityToken identityToken) {
        this.identityObject = identityObject;
        this.identityToken = identityToken;

        synchronized (clientUserIdHistory) {
            clientUserIdHistory.addLast(getClientUserId(identityToken));

            while (clientUserIdHistory.size() > IDENTITY_HISTORY_MAX_SIZE) {
                clientUserIdHistory.removeFirst();
            }
        }
    }

    public void setEndpoint(EndpointDescription endpoint) {
        this.endpoint = endpoint;
    }

    public void setSecurityConfiguration(SecurityConfiguration securityConfiguration) {
        this.securityConfiguration = securityConfiguration;
    }

    public void setClientAddress(InetAddress clientAddress) {
        this.clientAddress = clientAddress;
    }

    /**
     * Get the {@link InetAddress} of the client that activated this session.
     * <p>
     * The address is set or updated when time the session is activated or re-activated.
     *
     * @return the {@link InetAddress} of the client that activated this session.
     */
    public InetAddress getClientAddress() {
        return clientAddress;
    }

    public SessionDiagnostics getSessionDiagnostics() {
        return sessionDiagnostics;
    }

    public SessionSecurityDiagnostics getSessionSecurityDiagnostics() {
        return sessionSecurityDiagnostics;
    }

    public void addLifecycleListener(LifecycleListener listener) {
        listeners.add(listener);
    }

    void updateLastActivity() {
        lastActivityNanos = System.nanoTime();
        lastContactTime = DateTime.now();
    }

    public ApplicationDescription getClientDescription() {
        return clientDescription;
    }

    public String getServerUri() {
        return serverUri;
    }

    public Double getSessionTimeout() {
        return (double) sessionTimeout.toMillis();
    }

    public UInteger getMaxResponseMessageSize() {
        return maxResponseMessageSize;
    }

    public DateTime getConnectionTime() {
        return connectTime;
    }

    public DateTime getLastContactTime() {
        return lastContactTime;
    }

    void setLastNonce(ByteString lastNonce) {
        this.lastNonce = lastNonce;
    }

    public ByteString getLastNonce() {
        return lastNonce;
    }

    private void checkTimeout() {
        long elapsed = Math.abs(System.nanoTime() - lastActivityNanos);

        if (elapsed > sessionTimeout.toNanos()) {
            logger.debug("Session id={} lifetime expired ({}ms).", sessionId, sessionTimeout.toMillis());

            close(true);

            server.getDiagnosticsSummary().getSessionTimeoutCount().increment();
        } else {
            long remaining = sessionTimeout.toNanos() - elapsed;
            logger.trace("Session id={} timeout scheduled for +{}s.",
                sessionId, Duration.ofNanos(remaining).getSeconds());

            checkTimeoutFuture = server.getScheduledExecutorService()
                .schedule(this::checkTimeout, remaining, TimeUnit.NANOSECONDS);
        }
    }

    public NodeId getSessionId() {
        return sessionId;
    }

    public String getSessionName() {
        return sessionName;
    }

    @Nullable
    public String[] getLocaleIds() {
        return localeIds;
    }

    public void setLocaleIds(@Nullable String[] localeIds) {
        this.localeIds = localeIds;
    }

    public DefaultAttributeServiceSet getAttributeServiceSet() {
        return attributeServiceSet;
    }

    public DefaultAttributeHistoryServiceSet getAttributeHistoryServiceSet() {
        return attributeHistoryServiceSet;
    }

    public DefaultMethodServiceSet getMethodServiceSet() {
        return methodServiceSet;
    }

    public DefaultMonitoredItemServiceSet getMonitoredItemServiceSet() {
        return monitoredItemServiceSet;
    }

    public NodeManagementServiceSet getNodeManagementServiceSet() {
        return nodeManagementServiceSet;
    }

    public DefaultQueryServiceSet getQueryServiceSet() {
        return queryServiceSet;
    }

    public DefaultSubscriptionServiceSet getSubscriptionServiceSet() {
        return subscriptionServiceSet;
    }

    public DefaultViewServiceSet getViewServiceSet() {
        return viewServiceSet;
    }

    public SubscriptionManager getSubscriptionManager() {
        return subscriptionManager;
    }
    //region Session Services

    @Override
    public void onCreateSession(ServiceRequest serviceRequest) {
        serviceRequest.setServiceFault(StatusCodes.Bad_InternalError);
    }

    @Override
    public void onActivateSession(ServiceRequest serviceRequest) {
        serviceRequest.setServiceFault(StatusCodes.Bad_InternalError);
    }

    @Override
    public void onCloseSession(ServiceRequest serviceRequest) {
        serviceRequest.setServiceFault(StatusCodes.Bad_InternalError);
    }

    void close(boolean deleteSubscriptions) {
        if (checkTimeoutFuture != null) {
            checkTimeoutFuture.cancel(false);
        }

        subscriptionManager.sessionClosed(deleteSubscriptions);

        listeners.forEach(listener -> listener.onSessionClosed(this, deleteSubscriptions));
    }

    @Override
    public void onCancel(ServiceRequest serviceRequest) {
        serviceRequest.setResponse(new CancelResponse(serviceRequest.createResponseHeader(), uint(0)));
    }
    //endregion

    @Nullable
    private static String getClientUserId(UserIdentityToken identityToken) {
        UserTokenType tokenType = getTokenType(identityToken);

        if (tokenType == null) {
            return null;
        }

        switch (tokenType) {
            case Anonymous:
                return null;

            case UserName:
                return ((UserNameIdentityToken) identityToken).getUserName();

            case Certificate: {
                try {
                    ByteString bs = ((X509IdentityToken) identityToken).getCertificateData();
                    X509Certificate certificate = CertificateUtil.decodeCertificate(bs.bytesOrEmpty());
                    return certificate.getSubjectX500Principal().getName();
                } catch (Throwable t) {
                    return null;
                }
            }
            case IssuedToken:
                return "IssuedToken";

            default:
                throw new IllegalStateException("unhandled UserIdentityToken: " + identityToken);
        }
    }

    private static UserTokenType getTokenType(UserIdentityToken identityToken) {
        UserTokenType identityType = null;
        if (identityToken instanceof AnonymousIdentityToken) {
            identityType = UserTokenType.Anonymous;
        } else if (identityToken instanceof UserNameIdentityToken) {
            identityType = UserTokenType.UserName;
        } else if (identityToken instanceof X509IdentityToken) {
            identityType = UserTokenType.Certificate;
        } else if (identityToken instanceof IssuedIdentityToken) {
            identityType = UserTokenType.IssuedToken;
        }
        return identityType;
    }

    public interface LifecycleListener {
        void onSessionClosed(Session session, boolean subscriptionsDeleted);
    }
}
