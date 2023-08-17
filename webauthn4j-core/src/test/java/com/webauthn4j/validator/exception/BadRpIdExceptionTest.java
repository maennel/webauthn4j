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

import com.webauthn4j.util.MessageDigestUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class BadRpIdExceptionTest {

    private final RuntimeException cause = new RuntimeException();

    private final MessageDigest messageDigest = MessageDigestUtil.createSHA256();
    private final byte[] expected = messageDigest.digest("https://example.com".getBytes(StandardCharsets.UTF_8));
    private final byte[] actual = messageDigest.digest("https://example.org".getBytes(StandardCharsets.UTF_8));

    @Test
    void test() {
        BadRpIdException exception1 = new BadRpIdException("dummy", expected, actual, cause);
        BadRpIdException exception2 = new BadRpIdException("dummy", expected, actual);
        BadRpIdException exception3 = new BadRpIdException("dummy", cause);
        BadRpIdException exception4 = new BadRpIdException("dummy");
        BadRpIdException exception5 = new BadRpIdException(cause);

        assertAll(
                () -> assertThat(exception1.getMessage()).isEqualTo("dummy"),
                () -> assertThat(exception1.getExpected()).isEqualTo(expected),
                () -> assertThat(exception1.getActual()).isEqualTo(actual),
                () -> assertThat(exception1.getCause()).isEqualTo(cause),

                () -> assertThat(exception2.getMessage()).isEqualTo("dummy"),
                () -> assertThat(exception2.getExpected()).isEqualTo(expected),
                () -> assertThat(exception2.getActual()).isEqualTo(actual),
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
