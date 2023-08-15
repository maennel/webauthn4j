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

import org.checkerframework.checker.nullness.qual.Nullable;

import java.security.cert.X509Certificate;

/**
 * Thrown if certificate problems happen
 */
@SuppressWarnings("squid:S110")
public class CertificateException extends ValidationException {

    private final X509Certificate certificate;

    public CertificateException(@Nullable String message, @Nullable X509Certificate certificate, @Nullable Throwable cause) {
        super(message, cause);
        this.certificate = certificate;
    }

    public CertificateException(@Nullable String message, @Nullable X509Certificate certificate) {
        super(message);
        this.certificate = certificate;
    }

    public CertificateException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
        this.certificate = null;
    }

    public CertificateException(@Nullable String message) {
        super(message);
        this.certificate = null;
    }

    public CertificateException(@Nullable Throwable cause) {
        super(cause);
        this.certificate = null;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }
}
