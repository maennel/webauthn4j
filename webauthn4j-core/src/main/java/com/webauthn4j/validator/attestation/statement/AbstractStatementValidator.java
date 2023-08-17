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

package com.webauthn4j.validator.attestation.statement;

import com.webauthn4j.data.attestation.statement.AttestationStatement;
import com.webauthn4j.util.AssertUtil;
import com.webauthn4j.validator.CoreRegistrationObject;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@SuppressWarnings({"squid:S2326", "unused"})
public abstract class AbstractStatementValidator<T extends AttestationStatement> implements AttestationStatementValidator {

    private final Class<?> parameterizedTypeClass;

    protected AbstractStatementValidator() {
        ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
        if (parameterizedType.getActualTypeArguments().length == 0) {
            // Throw an exception if the class is not extending AttestationStatement
            throw new IllegalStateException("Inheriting class must extend AttestationStatement");
        }
        Type actualTypeArgument = parameterizedType.getActualTypeArguments()[0];

        if (actualTypeArgument instanceof Class) {
            this.parameterizedTypeClass = (Class<?>) actualTypeArgument;
        }
        else {
            // Throw an exception if the type is not a Class<?>
            throw new IllegalStateException("Inheriting class must extend AttestationStatement");
        }
    }

    @Override
    public boolean supports(@NonNull CoreRegistrationObject registrationObject) {
        AssertUtil.notNull(registrationObject, "registrationObject must not be null");
        AttestationStatement attestationStatement = registrationObject.getAttestationObject().getAttestationStatement();
        if (attestationStatement == null) {
            return false;
        }
        return this.parameterizedTypeClass.isAssignableFrom(attestationStatement.getClass());
    }


}
