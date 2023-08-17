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

package com.webauthn4j.validator;

import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.CoreRegistrationData;
import com.webauthn4j.data.CoreRegistrationParameters;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.extension.authenticator.AuthenticationExtensionsAuthenticatorOutputs;
import com.webauthn4j.data.extension.authenticator.RegistrationExtensionAuthenticatorOutput;
import com.webauthn4j.server.CoreServerProperty;
import com.webauthn4j.util.AssertUtil;
import com.webauthn4j.validator.attestation.statement.AttestationStatementValidator;
import com.webauthn4j.validator.attestation.trustworthiness.certpath.CertPathTrustworthinessValidator;
import com.webauthn4j.validator.attestation.trustworthiness.self.SelfAttestationTrustworthinessValidator;
import com.webauthn4j.validator.exception.ConstraintViolationException;
import com.webauthn4j.validator.exception.NotAllowedAlgorithmException;
import com.webauthn4j.validator.exception.UserNotPresentException;
import com.webauthn4j.validator.exception.UserNotVerifiedException;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.stream.Collectors;

public class CoreRegistrationDataValidator {

    // ~ Instance fields
    // ================================================================================================

    private final RpIdHashValidator rpIdHashValidator = new RpIdHashValidator();
    private final AuthenticatorExtensionValidator authenticatorExtensionValidator = new AuthenticatorExtensionValidator();
    private final AttestationValidator attestationValidator;
    private final List<CustomCoreRegistrationValidator> customRegistrationValidators;

    public CoreRegistrationDataValidator(
            @NonNull List<AttestationStatementValidator> attestationStatementValidators,
            @NonNull CertPathTrustworthinessValidator certPathTrustworthinessValidator,
            @NonNull SelfAttestationTrustworthinessValidator selfAttestationTrustworthinessValidator,
            @NonNull List<CustomCoreRegistrationValidator> customRegistrationValidators,
            @NonNull ObjectConverter objectConverter) {
        AssertUtil.notNull(attestationStatementValidators, "attestationStatementValidators must not be null");
        AssertUtil.notNull(certPathTrustworthinessValidator, "certPathTrustworthinessValidator must not be null");
        AssertUtil.notNull(selfAttestationTrustworthinessValidator, "selfAttestationTrustworthinessValidator must not be null");
        AssertUtil.notNull(customRegistrationValidators, "customRegistrationValidators must not be null");
        AssertUtil.notNull(objectConverter, "objectConverter must not be null");

        this.attestationValidator = new AttestationValidator(
                attestationStatementValidators,
                certPathTrustworthinessValidator,
                selfAttestationTrustworthinessValidator);
        this.customRegistrationValidators = customRegistrationValidators;
    }


    /**
     * It is up to caller responsibility to inject challenge into clientData and validate it equals to challenge stored in server side
     *
     * @param registrationData       registration data
     * @param registrationParameters registration parameters
     */
    @SuppressWarnings("ConstantConditions") // as null check is done by BeanAssertUtil#validate
    public void validate(@NonNull CoreRegistrationData registrationData, @NonNull CoreRegistrationParameters registrationParameters) {

        //spec| Step1
        //spec| Let options be a new PublicKeyCredentialCreationOptions structure configured to the Relying Party's needs for the ceremony.
        //      (This step is done on client slide and out of WebAuthn4J responsibility.)

        //spec| Step2
        //spec| Call navigator.credentials.create() and pass options as the publicKey option. Let credential be the result of the successfully resolved promise.
        //spec| If the promise is rejected, abort the ceremony with a user-visible error,
        //spec| or otherwise guide the user experience as might be determinable from the context available in the rejected promise.
        //spec| For example if the promise is rejected with an error code equivalent to "InvalidStateError",
        //spec| the user might be instructed to use a different authenticator.
        //spec| For information on different error contexts and the circumstances leading to them, see § 6.3.2 The authenticatorMakeCredential Operation.
        //      (This step is done on client slide and out of WebAuthn4J responsibility.)

        //spec| Step3
        //spec| Let response be credential.response. If response is not an instance of AuthenticatorAttestationResponse, abort the ceremony with a user-visible error.
        //      (This step is done on client slide and out of WebAuthn4J responsibility.)

        //spec| Step4
        //spec| Let clientExtensionResults be the result of calling credential.getClientExtensionResults().
        //      (This step is only applicable to WebAuthn)

        //spec| Step5
        //spec| Let JSONtext be the result of running UTF-8 decode on the value of response.clientDataJSON.
        //      (This step is only applicable to WebAuthn)

        BeanAssertUtil.validate(registrationData);
        AssertUtil.notNull(registrationParameters, "registrationParameters must not be null");

        //spec| Step6
        //spec| Let C, the client data claimed as collected during the credential creation,
        //spec| be the result of running an implementation-specific JSON parser on JSONtext.
        //      (This step is only applicable to WebAuthn)

        AttestationObject attestationObject = registrationData.getAttestationObject();


        validateAuthenticatorDataField(attestationObject.getAuthenticatorData());

        CoreServerProperty serverProperty = registrationParameters.getServerProperty();

        CoreRegistrationObject registrationObject = createCoreRegistrationObject(registrationData, registrationParameters);

        AuthenticatorData<RegistrationExtensionAuthenticatorOutput> authenticatorData = attestationObject.getAuthenticatorData();

        COSEKey coseKey = authenticatorData.getAttestedCredentialData().getCOSEKey();
        validateCOSEKey(coseKey);

        //spec| Step7
        //spec| Verify that the value of C.type is webauthn.create.
        //      (This step is only applicable to WebAuthn)

        //spec| Step8
        //spec| Verify that the value of C.challenge equals the base64url encoding of options.challenge.
        //      (This step is only applicable to WebAuthn)


        //spec| Step9
        //spec| Verify that the value of C.origin matches the Relying Party's origin.
        //      (This step is only applicable to WebAuthn)

        //spec| Step10
        //spec| Verify that the value of C.tokenBinding.status matches the state of Token Binding for the TLS connection over
        //spec| which the assertion was obtained. If Token Binding was used on that TLS connection, also verify that
        //spec| C.tokenBinding.id matches the base64url encoding of the Token Binding ID for the connection.
        //      (This step is only applicable to WebAuthn)

        //spec| Step11
        //spec| Let hash be the result of computing a hash over response.clientDataJSON using SHA-256.

        //spec| Step12
        //spec| Perform CBOR decoding on the attestationObject field of the AuthenticatorAttestationResponse structure to
        //spec| obtain the attestation statement format fmt, the authenticator data authData, and the attestation statement attStmt.
        //      (This step is done on caller.)

        //spec| Step13
        //spec| Verify that the rpIdHash in authData is the SHA-256 hash of the RP ID expected by the Relying Party.
        rpIdHashValidator.validate(authenticatorData.getRpIdHash(), serverProperty);

        //spec| Step14, 15
        //spec| Verify that the User Present bit of the flags in authData is set.
        //spec| If user verification is required for this registration, verify that the User Verified bit of the flags in authData is set.
        validateUVUPFlags(authenticatorData, registrationParameters.isUserVerificationRequired(), registrationParameters.isUserPresenceRequired());

        //spec| Step16
        //spec| Verify that the "alg" parameter in the credential public key in authData matches the alg attribute of one of the items in options.pubKeyCredParams.
        COSEAlgorithmIdentifier alg = authenticatorData.getAttestedCredentialData().getCOSEKey().getAlgorithm();
        List<PublicKeyCredentialParameters> pubKeyCredParams = registrationParameters.getPubKeyCredParams();
        validateAlg(alg, pubKeyCredParams);

        //spec| Step17
        //spec| Verify that the values of the client extension outputs in clientExtensionResults and the authenticator extension outputs in the extensions in authData are as expected,
        //spec| considering the client extension input values that were given in options.extensions and any specific policy of the Relying Party regarding unsolicited extensions,
        //spec| i.e., those that were not specified as part of options.extensions.
        //spec| In the general case, the meaning of "are as expected" is specific to the Relying Party and which extensions are in use.
        AuthenticationExtensionsAuthenticatorOutputs<RegistrationExtensionAuthenticatorOutput> authenticationExtensionsAuthenticatorOutputs = authenticatorData.getExtensions();
        authenticatorExtensionValidator.validate(authenticationExtensionsAuthenticatorOutputs);

        //spec| Step18-21
        attestationValidator.validate(registrationObject);

        //spec| Step22
        //spec| Check that the credentialId is not yet registered to any other user.
        //spec| If registration is requested for a credential that is already registered to a different user,
        //spec| the Relying Party SHOULD fail this registration ceremony, or it MAY decide to accept the registration,
        //spec| e.g. while deleting the older registration.

        //      (This step is out of WebAuthn4J scope. It's caller's responsibility.)

        //spec| Step23
        //spec| If the attestation statement attStmt verified successfully and is found to be trustworthy,
        //spec| then register the new credential with the account that was denoted in options.user:
        //spec| - Associate the user’s account with the credentialId and credentialPublicKey
        //spec|   in authData.attestedCredentialData, as appropriate for the Relying Party's system.
        //spec| - Associate the credentialId with a new stored signature counter value initialized to the value of authData.signCount.
        //spec| It is RECOMMENDED to also:
        //spec| - Associate the credentialId with the transport hints returned by calling credential.response.getTransports().
        //spec|   This value SHOULD NOT be modified before or after storing it.
        //spec|   It is RECOMMENDED to use this value to populate the transports of the allowCredentials option in future get() calls
        //spec|   to help the client know how to find a suitable authenticator.
        //      (This step is out of WebAuthn4J scope. It's caller's responsibility.)

        //spec| Step24
        //spec| If the attestation statement attStmt successfully verified but is not trustworthy per step 21 above,
        //spec| the Relying Party SHOULD fail the registration ceremony.
        //      (This step is out of WebAuthn4J scope. It's caller's responsibility.)

        // validate with custom logic
        for (CustomCoreRegistrationValidator customRegistrationValidator : customRegistrationValidators) {
            customRegistrationValidator.validate(registrationObject);
        }
    }

    void validateAlg(COSEAlgorithmIdentifier alg, List<PublicKeyCredentialParameters> pubKeyCredParams) {
        if(pubKeyCredParams == null){
            return;
        }
        List<COSEAlgorithmIdentifier> expected = pubKeyCredParams.stream().map(PublicKeyCredentialParameters::getAlg).collect(Collectors.toList());
        if(expected.stream().noneMatch(item -> item.equals(alg))){
            throw new NotAllowedAlgorithmException("alg not listed in options.pubKeyCredParams is used.", expected, alg);
        }
    }

    void validateCOSEKey(COSEKey coseKey) {
        if (coseKey.getPublicKey() == null) {
            throw new ConstraintViolationException("coseKey doesn't contain public key");
        }
    }

    @SuppressWarnings("ConstantConditions") // as null check is done by BeanAssertUtil#validate
    protected CoreRegistrationObject createCoreRegistrationObject(@NonNull CoreRegistrationData registrationData, @NonNull CoreRegistrationParameters registrationParameters) {
        return new CoreRegistrationObject(
                registrationData.getAttestationObject(),
                registrationData.getAttestationObjectBytes(),
                registrationData.getClientDataHash(),
                registrationParameters.getServerProperty()
        );
    }

    void validateAuthenticatorDataField(@NonNull AuthenticatorData<RegistrationExtensionAuthenticatorOutput> authenticatorData) {
        // attestedCredentialData must be present on registration
        if (authenticatorData.getAttestedCredentialData() == null) {
            throw new ConstraintViolationException("attestedCredentialData must not be null on registration");
        }
    }

    void validateUVUPFlags(@NonNull AuthenticatorData<RegistrationExtensionAuthenticatorOutput> authenticatorData, boolean isUserVerificationRequired, boolean isUserPresenceRequired) {
        //spec| Step10
        //spec| If user verification is required for this registration, verify that the User Verified bit of the flags in authData is set.
        if (isUserVerificationRequired && !authenticatorData.isFlagUV()) {
            throw new UserNotVerifiedException("Validator is configured to check user verified, but UV flag in authenticatorData is not set.");
        }

        //spec| Step11
        //spec| Verify that the User Present bit of the flags in authData is set.
        if (isUserPresenceRequired && !authenticatorData.isFlagUP()) {
            throw new UserNotPresentException("Validator is configured to check user present, but UP flag in authenticatorData is not set.");
        }
    }

    public @NonNull List<CustomCoreRegistrationValidator> getCustomRegistrationValidators() {
        return customRegistrationValidators;
    }

}
