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

package com.webauthn4j.validator.attestation.statement.u2f;

import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import com.webauthn4j.data.attestation.statement.AttestationStatement;
import com.webauthn4j.data.attestation.statement.AttestationType;
import com.webauthn4j.data.attestation.statement.FIDOU2FAttestationStatement;
import com.webauthn4j.util.AssertUtil;
import com.webauthn4j.util.ECUtil;
import com.webauthn4j.util.MessageDigestUtil;
import com.webauthn4j.validator.CoreRegistrationObject;
import com.webauthn4j.validator.attestation.statement.AbstractStatementValidator;
import com.webauthn4j.validator.exception.BadAttestationStatementException;
import com.webauthn4j.validator.exception.BadSignatureException;
import com.webauthn4j.validator.exception.CertificateException;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;

/**
 * Validates the specified {@link AttestationStatement} is a valid FIDO-U2F attestation
 */
public class FIDOU2FAttestationStatementValidator extends AbstractStatementValidator<FIDOU2FAttestationStatement> {

    /**
     * {@link AttestationType}.BASIC is always returned as RP cannot differentiate between BASIC and Attestation CA from the attestation data,
     *
     * @return AttestationType.BASIC
     */
    @Override
    public @NonNull AttestationType validate(@NonNull CoreRegistrationObject registrationObject) {
        AssertUtil.notNull(registrationObject, "registrationObject must not be null");
        if (!supports(registrationObject)) {
            throw new IllegalArgumentException("Specified format is not supported by " + this.getClass().getName());
        }

        FIDOU2FAttestationStatement attestationStatement =
                (FIDOU2FAttestationStatement) registrationObject.getAttestationObject().getAttestationStatement();
        validateAttestationStatementNotNull(attestationStatement);
        validateAttestationStatement(attestationStatement);
        validateSignature(registrationObject);
        return AttestationType.BASIC;
    }

    void validateAttestationStatementNotNull(FIDOU2FAttestationStatement attestationStatement) {
        if (attestationStatement == null) {
            throw new BadAttestationStatementException("attestation statement is not found.", attestationStatement);
        }
    }

    void validateAttestationStatement(@NonNull FIDOU2FAttestationStatement attestationStatement) {
        if (attestationStatement.getX5c().size() != 1) {
            throw new BadAttestationStatementException("FIDO-U2F attestation statement must have only one certificate.", attestationStatement);
        }
        X509Certificate certificate = attestationStatement.getX5c().getEndEntityAttestationCertificate().getCertificate();
        validatePublicKey(certificate);
    }

    void validatePublicKey(@NonNull X509Certificate certificate) {
        PublicKey publicKey = certificate.getPublicKey();
        if (!publicKey.getAlgorithm().equals("EC")) {
            throw new CertificateException("FIDO-U2F attestation statement supports ECDSA only.", certificate);
        }
        if (!((ECPublicKey) publicKey).getParams().equals(ECUtil.P_256_SPEC)) {
            throw new CertificateException("FIDO-U2F attestation statement supports secp256r1 curve only.", certificate);
        }
    }

    private void validateSignature(@NonNull CoreRegistrationObject registrationObject) {
        FIDOU2FAttestationStatement attestationStatement = (FIDOU2FAttestationStatement) registrationObject.getAttestationObject().getAttestationStatement();

        byte[] signedData = getSignedData(registrationObject);
        byte[] signature = attestationStatement.getSig();
        PublicKey publicKey = getPublicKey(attestationStatement);

        try {
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(publicKey);
            verifier.update(signedData);
            if (verifier.verify(signature)) {
                return;
            }
            throw new BadSignatureException("`sig` in attestation statement is not valid signature. Please refer U2F Raw Message Formats. https://fidoalliance.org/specs/fido-u2f-v1.1-id-20160915/fido-u2f-raw-message-formats-v1.1-id-20160915.html");
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new BadSignatureException("`sig` in attestation statement is not valid signature. Please refer U2F Raw Message Formats. https://fidoalliance.org/specs/fido-u2f-v1.1-id-20160915/fido-u2f-raw-message-formats-v1.1-id-20160915.html");
        }
    }

    private byte[] getSignedData(@NonNull CoreRegistrationObject registrationObject) {

        String rpId = registrationObject.getServerProperty().getRpId();
        MessageDigest messageDigest = MessageDigestUtil.createSHA256();

        AttestationObject attestationObject = registrationObject.getAttestationObject();
        //noinspection ConstantConditions as null check is already done in caller
        EC2COSEKey credentialPublicKey =
                (EC2COSEKey) attestationObject.getAuthenticatorData().getAttestedCredentialData().getCOSEKey();

        byte[] rpIdBytes = rpId.getBytes(StandardCharsets.UTF_8);

        byte[] applicationParameter = messageDigest.digest(rpIdBytes);
        byte[] challengeParameter = registrationObject.getClientDataHash();
        byte[] keyHandle = attestationObject.getAuthenticatorData().getAttestedCredentialData().getCredentialId();
        byte[] userPublicKeyBytes = getPublicKeyBytes(credentialPublicKey);

        ByteBuffer byteBuffer = ByteBuffer.allocate(1 + 32 + 32 + keyHandle.length + 65);
        byteBuffer.put((byte) 0x00); //RFU
        byteBuffer.put(applicationParameter);
        byteBuffer.put(challengeParameter);
        byteBuffer.put(keyHandle);
        byteBuffer.put(userPublicKeyBytes);
        return byteBuffer.array();
    }

    private byte[] getPublicKeyBytes(@NonNull EC2COSEKey ec2CoseKey) {
        byte[] x = ec2CoseKey.getX();
        byte[] y = ec2CoseKey.getY();
        byte format = 0x04;
        return ByteBuffer.allocate(1 + x.length + y.length).put(format).put(x).put(y).array();
    }

    private PublicKey getPublicKey(@NonNull FIDOU2FAttestationStatement attestationStatement) {
        Certificate cert = attestationStatement.getX5c().getEndEntityAttestationCertificate().getCertificate();
        return cert.getPublicKey();
    }

}
