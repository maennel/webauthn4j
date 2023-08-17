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

import com.webauthn4j.test.TestAttestationUtil;
import com.webauthn4j.test.TestDataUtil;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

class CertificateExceptionTest {

    private final RuntimeException cause = new RuntimeException();
    private final X509Certificate certificate = TestAttestationUtil.load3tierTestAuthenticatorAttestationCertificate();

    @Test
    void test() {
        CertificateException exception1 = new CertificateException("dummy", certificate, cause);
        CertificateException exception2 = new CertificateException("dummy", certificate);
        CertificateException exception3 = new CertificateException("dummy", cause);
        CertificateException exception4 = new CertificateException("dummy");
        CertificateException exception5 = new CertificateException(cause);

        assertAll(
                () -> assertThat(exception1.getMessage()).isEqualTo("dummy"),
                () -> assertThat(exception1.getCause()).isEqualTo(cause),

                () -> assertThat(exception2.getMessage()).isEqualTo("dummy"),
                () -> assertThat(exception2.getCause()).isNull(),

                () -> assertThat(exception3.getMessage()).isEqualTo("dummy"),
                () -> assertThat(exception3.getCause()).isEqualTo(cause),

                () -> assertThat(exception4.getMessage()).isEqualTo("dummy"),
                () -> assertThat(exception4.getCause()).isNull(),

                () -> assertThat(exception5.getMessage()).isEqualTo(cause.toString()),
                () -> assertThat(exception5.getCause()).isEqualTo(cause)
        );
    }
}