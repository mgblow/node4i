/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.stack;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import org.eclipse.milo.opcua.stack.server.security.ServerCertificateValidator;

public class TestServerCertificateValidator implements ServerCertificateValidator {

    private final Set<X509Certificate> trustedCertificates = Sets.newConcurrentHashSet();

    public TestServerCertificateValidator(X509Certificate certificate) {
        trustedCertificates.add(certificate);
    }

    public TestServerCertificateValidator(X509Certificate... certificates) {
        Collections.addAll(trustedCertificates, certificates);
    }

    @Override
    public void validateCertificateChain(List<X509Certificate> certificateChain) {
        // noop
    }

    @Override
    public void validateCertificateChain(List<X509Certificate> certificateChain, String applicationUri) {
        // noop
    }

}
