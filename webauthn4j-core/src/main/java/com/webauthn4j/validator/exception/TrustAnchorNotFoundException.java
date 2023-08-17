/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webauthn4j.validator.exception;

import com.webauthn4j.data.attestation.authenticator.AAGUID;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Thrown if no trust anchor chained to the attestation certificate is found
 */
@SuppressWarnings("squid:S110")
public class TrustAnchorNotFoundException extends ValidationException {

    private final byte[] subjectKeyIdentifier;
    private final AAGUID aaguid;

    public TrustAnchorNotFoundException(@Nullable String message, @Nullable byte[] subjectKeyIdentifier) {
        super(message);
        this.subjectKeyIdentifier = subjectKeyIdentifier;
        this.aaguid = null;
    }

    public TrustAnchorNotFoundException(@Nullable String message, @Nullable AAGUID aaguid) {
        super(message);
        this.subjectKeyIdentifier = null;
        this.aaguid = aaguid;
    }

    public TrustAnchorNotFoundException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
        this.subjectKeyIdentifier = null;
        this.aaguid = null;
    }

    public TrustAnchorNotFoundException(@Nullable String message) {
        super(message);
        this.subjectKeyIdentifier = null;
        this.aaguid = null;
    }

    public TrustAnchorNotFoundException(@Nullable Throwable cause) {
        super(cause);
        this.subjectKeyIdentifier = null;
        this.aaguid = null;
    }

    public byte[] getSubjectKeyIdentifier() {
        return subjectKeyIdentifier;
    }

    public AAGUID getAaguid() {
        return aaguid;
    }
}
