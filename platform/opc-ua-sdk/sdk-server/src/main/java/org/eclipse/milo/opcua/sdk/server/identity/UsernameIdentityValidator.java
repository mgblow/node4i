/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.identity;

import java.util.function.Predicate;

import org.eclipse.milo.opcua.sdk.server.Session;
import org.jetbrains.annotations.Nullable;

public class UsernameIdentityValidator extends AbstractUsernameIdentityValidator<String> {

    private final boolean anonymousAccessAllowed;
    private final Predicate<AuthenticationChallenge> predicate;

    public UsernameIdentityValidator(
        boolean anonymousAccessAllowed,
        Predicate<AuthenticationChallenge> predicate
    ) {

        this.anonymousAccessAllowed = anonymousAccessAllowed;
        this.predicate = predicate;
    }

    @Nullable
    @Override
    protected String authenticateAnonymous(Session session) {
        if (anonymousAccessAllowed) {
            return String.format(
                "anonymous_%s_%s",
                session.getSessionName(),
                session.getSessionId().toParseableString()
            );
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    protected String authenticateUsernamePassword(Session session, String username, String password) {
        AuthenticationChallenge challenge =
            new AuthenticationChallenge(username, password);

        return predicate.test(challenge) ? username : null;
    }

    public static final class AuthenticationChallenge {

        private final String username;
        private final String password;

        AuthenticationChallenge(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

    }

}
