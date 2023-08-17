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

import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.client.ClientDataType;
import com.webauthn4j.data.client.CollectedClientData;
import com.webauthn4j.data.extension.authenticator.AuthenticationExtensionAuthenticatorOutput;
import com.webauthn4j.data.extension.authenticator.AuthenticationExtensionsAuthenticatorOutputs;
import com.webauthn4j.data.extension.client.AuthenticationExtensionClientOutput;
import com.webauthn4j.data.extension.client.AuthenticationExtensionsClientOutputs;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.util.AssertUtil;
import com.webauthn4j.validator.exception.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AuthenticationDataValidator {

    private final ChallengeValidator challengeValidator = new ChallengeValidator();
    private final TokenBindingValidator tokenBindingValidator = new TokenBindingValidator();
    private final RpIdHashValidator rpIdHashValidator = new RpIdHashValidator();
    private final AssertionSignatureValidator assertionSignatureValidator = new AssertionSignatureValidator();
    private final ClientExtensionValidator clientExtensionValidator = new ClientExtensionValidator();
    private final AuthenticatorExtensionValidator authenticatorExtensionValidator = new AuthenticatorExtensionValidator();

    private final List<CustomAuthenticationValidator> customAuthenticationValidators;

    private OriginValidator originValidator = new OriginValidatorImpl();
    private CoreMaliciousCounterValueHandler maliciousCounterValueHandler = new DefaultCoreMaliciousCounterValueHandler();

    private boolean crossOriginAllowed = false;

    public AuthenticationDataValidator(@NonNull List<CustomAuthenticationValidator> customAuthenticationValidators) {
        AssertUtil.notNull(customAuthenticationValidators, "customAuthenticationValidators must not be null");
        this.customAuthenticationValidators = customAuthenticationValidators;
    }

    public AuthenticationDataValidator() {
        this.customAuthenticationValidators = new ArrayList<>();
    }

    @SuppressWarnings("ConstantConditions") // as null check is done by BeanAssertUtil#validate
    public void validate(@NonNull AuthenticationData authenticationData, @NonNull AuthenticationParameters authenticationParameters) {

        BeanAssertUtil.validate(authenticationData);
        AssertUtil.notNull(authenticationParameters, "authenticationParameters must not be null");

        //spec| Step1
        //spec| Let options be a new PublicKeyCredentialRequestOptions structure configured to the Relying Party's needs for the ceremony.
        //      (This step is done on client slide and out of WebAuthn4J responsibility.)

        //spec| Step2
        //spec| Call navigator.credentials.get() and pass options as the publicKey option. Let credential be the result of the successfully resolved promise.
        //spec| If the promise is rejected, abort the ceremony with a user-visible error, or otherwise guide the user experience as might be determinable
        //spec| from the context available in the rejected promise. For information on different error contexts and the circumstances leading to them,
        //spec| see § 6.3.3 The authenticatorGetAssertion Operation.
        //      (This step is done on client slide and out of WebAuthn4J responsibility.)

        //spec| Step3
        //spec| Let response be credential.response. If response is not an instance of AuthenticatorAssertionResponse, abort the ceremony with a user-visible error.
        //      (This step is done on client slide and out of WebAuthn4J responsibility.)

        //spec| Step4
        //spec| Let clientExtensionResults be the result of calling credential.getClientExtensionResults().
        AuthenticationExtensionsClientOutputs<AuthenticationExtensionClientOutput> clientExtensions = authenticationData.getClientExtensions();

        //spec| Step5
        //spec| If options.allowCredentials is not empty, verify that credential.id identifies one of the public key credentials listed in options.allowCredentials.
        byte[] credentialId = authenticationData.getCredentialId();
        List<byte[]> allowCredentials = authenticationParameters.getAllowCredentials();
        validateCredentialId(credentialId, allowCredentials);

        //spec| Step6
        //spec| Identify the user being authenticated and verify that this user is the owner of the public key credential source credentialSource identified by credential.id:
        //spec| - If the user was identified before the authentication ceremony was initiated,
        //spec|   verify that the identified user is the owner of credentialSource. If credential.response.userHandle is present,
        //spec|   let userHandle be its value. Verify that userHandle also maps to the same user.
        //spec| - If the user was not identified before the authentication ceremony was initiated,
        //spec|   verify that response.userHandle is present, and that the user identified by this value is the owner of credentialSource.
        //      (This step is out of WebAuthn4J scope. It's caller's responsibility.)

        //spec| Step7
        //spec| Using credential’s id attribute (or the corresponding rawId, if base64url encoding is inappropriate for your use case),
        //spec| look up the corresponding credential public key and let credentialPublicKey be that credential public key.
        //      (This step is out of WebAuthn4J scope. It's caller's responsibility.)

        //spec| Step8
        //spec| Let cData, aData and sig denote the value of credential’s response's clientDataJSON, authenticatorData,
        //spec| and signature respectively.
        byte[] cData = authenticationData.getCollectedClientDataBytes();
        byte[] aData = authenticationData.getAuthenticatorDataBytes();

        //spec| Step9
        //spec| Let JSONtext be the result of running UTF-8 decode on the value of cData.
        //      (This step is done on caller.)

        //spec| Step10
        //spec| Let C, the client data claimed as used for the signature, be the result of running an implementation-specific JSON parser on JSONtext.
        //      (In the spec, claimed as "C", but use "collectedClientData" here)
        CollectedClientData collectedClientData = authenticationData.getCollectedClientData();

        AuthenticatorData<AuthenticationExtensionAuthenticatorOutput> authenticatorData = authenticationData.getAuthenticatorData();
        ServerProperty serverProperty = authenticationParameters.getServerProperty();

        BeanAssertUtil.validate(collectedClientData);
        BeanAssertUtil.validate(authenticatorData);

        validateAuthenticatorData(authenticatorData);

        Authenticator authenticator = authenticationParameters.getAuthenticator();

        AuthenticationObject authenticationObject = new AuthenticationObject(
                credentialId, authenticatorData, aData, collectedClientData, cData, clientExtensions,
                serverProperty, authenticator
        );

        //spec| Step11
        //spec| Verify that the value of C.type is the string webauthn.get.
        if (!Objects.equals(collectedClientData.getType(), ClientDataType.WEBAUTHN_GET)) {
            throw new InconsistentClientDataTypeException("ClientData.type must be 'get' on authentication, but it isn't.");
        }

        //spec| Step12
        //spec| Verify that the value of C.challenge matches the challenge that was sent to the authenticator in
        //spec| the PublicKeyCredentialRequestOptions passed to the get() call.
        challengeValidator.validate(collectedClientData, serverProperty);

        //spec| Step13
        //spec| Verify that the value of C.origin matches the Relying Party's origin.
        originValidator.validate(authenticationObject);

        // Verify cross origin, which is not defined in the spec
        validateClientDataCrossOrigin(collectedClientData);

        //spec| Step14
        //spec| Verify that the value of C.tokenBinding.status matches the state of Token Binding for the TLS connection over
        //spec| which the attestation was obtained. If Token Binding was used on that TLS connection,
        //spec| also verify that C.tokenBinding.id matches the base64url encoding of the Token Binding ID for the connection.
        tokenBindingValidator.validate(collectedClientData.getTokenBinding(), serverProperty.getTokenBindingId());

        //spec| Step15
        //spec| Verify that the rpIdHash in authData is the SHA-256 hash of the RP ID expected by the Relying Party.
        rpIdHashValidator.validate(authenticatorData.getRpIdHash(), serverProperty);

        //spec| Step16
        //spec| Verify that the User Present bit of the flags in authData is set.
        if (authenticationParameters.isUserPresenceRequired() && !authenticatorData.isFlagUP()) {
            throw new UserNotPresentException("Validator is configured to check user present, but UP flag in authenticatorData is not set.");
        }

        //spec| Step17
        //spec| If user verification is required for this assertion, verify that the User Verified bit of the flags in authData is set.
        if (authenticationParameters.isUserVerificationRequired() && !authenticatorData.isFlagUV()) {
            throw new UserNotVerifiedException("Validator is configured to check user verified, but UV flag in authenticatorData is not set.");
        }

        //spec| Step18
        //spec| Verify that the values of the client extension outputs in clientExtensionResults and the authenticator
        //spec| extension outputs in the extensions in authData are as expected, considering the client extension input
        //spec| values that were given as the extensions option in the get() call. In particular, any extension identifier
        //spec| values in the clientExtensionResults and the extensions in authData MUST be also be present as extension
        //spec| identifier values in the extensions member of options, i.e., no extensions are present that were not requested.
        //spec| In the general case, the meaning of "are as expected" is specific to the Relying Party and which extensions are in use.
        AuthenticationExtensionsAuthenticatorOutputs<AuthenticationExtensionAuthenticatorOutput> authenticationExtensionsAuthenticatorOutputs = authenticatorData.getExtensions();
        clientExtensionValidator.validate(clientExtensions);
        authenticatorExtensionValidator.validate(authenticationExtensionsAuthenticatorOutputs);

        //spec| Step19
        //spec| Let hash be the result of computing a hash over the cData using SHA-256.
        //spec| Step20
        //spec| Using the credential public key, validate that sig is a valid signature over
        //spec| the binary concatenation of the authenticatorData and the hash of the collectedClientData.
        assertionSignatureValidator.validate(authenticationData, authenticator.getAttestedCredentialData().getCOSEKey());

        //spec| Step21
        //spec| Let storedSignCount be the stored signature counter value associated with credential.id.
        //spec| If authData.signCount is nonzero or storedSignCount is nonzero, then run the following sub-step:
        long presentedSignCount = authenticatorData.getSignCount();
        long storedSignCount = authenticator.getCounter();
        if (presentedSignCount > 0 || storedSignCount > 0) {
            //spec| If authData.signCount is
            //spec| greater than storedSignCount:
            if (presentedSignCount > storedSignCount) {

                //spec| Update storedSignCount to be the value of authData.signCount.
                //      (caller need to update the signature counter value based on the value set in the Authenticator instance)
                authenticator.setCounter(presentedSignCount);
            }
            //spec| less than or equal to storedSignCount:
            //spec| This is a signal that the authenticator may be cloned, i.e. at least two copies of the credential private key may exist and are being used in parallel.
            //spec| Relying Parties should incorporate this information into their risk scoring.
            //spec| Whether the Relying Party updates storedSignCount in this case, or not, or fails the authentication ceremony or not, is Relying Party-specific.
            else {
                maliciousCounterValueHandler.maliciousCounterValueDetected(authenticationObject);
            }
        }

        for (CustomAuthenticationValidator customAuthenticationValidator : customAuthenticationValidators) {
            customAuthenticationValidator.validate(authenticationObject);
        }

        //spec| Step18
        //spec| If all the above steps are successful, continue with the authentication ceremony as appropriate. Otherwise, fail the authentication ceremony.


    }

    void validateCredentialId(byte[] credentialId, @Nullable List<byte[]> allowCredentials) {
        // As allowCredentials are known data to client side(potential attacker),
        // there is no need to prevent timing attack and it is OK to use `Arrays.equals` instead of `MessageDigest.isEqual` here.
        if(allowCredentials != null && allowCredentials.stream().noneMatch(item -> Arrays.equals(item, credentialId))){
            throw new NotAllowedCredentialIdException("credentialId not listed in allowCredentials is used.", allowCredentials, credentialId);
        }
    }

    void validateClientDataCrossOrigin(CollectedClientData collectedClientData) {
        if (!crossOriginAllowed && Objects.equals(true, collectedClientData.getCrossOrigin())) {
            throw new CrossOriginException("Cross-origin request is prohibited. Relax AuthenticationDataValidator config if necessary.");
        }
    }

    void validateAuthenticatorData(@NonNull AuthenticatorData<AuthenticationExtensionAuthenticatorOutput> authenticatorData) {
        if (authenticatorData.getAttestedCredentialData() != null) {
            throw new ConstraintViolationException("attestedCredentialData must be null on authentication");
        }
    }

    public @NonNull CoreMaliciousCounterValueHandler getMaliciousCounterValueHandler() {
        return maliciousCounterValueHandler;
    }

    public void setMaliciousCounterValueHandler(@NonNull CoreMaliciousCounterValueHandler maliciousCounterValueHandler) {
        AssertUtil.notNull(maliciousCounterValueHandler, "maliciousCounterValueHandler must not be null");
        this.maliciousCounterValueHandler = maliciousCounterValueHandler;
    }

    public OriginValidator getOriginValidator() {
        return originValidator;
    }

    public void setOriginValidator(OriginValidator originValidator) {
        this.originValidator = originValidator;
    }

    public @NonNull List<CustomAuthenticationValidator> getCustomAuthenticationValidators() {
        return customAuthenticationValidators;
    }

    public boolean isCrossOriginAllowed() {
        return crossOriginAllowed;
    }

    public void setCrossOriginAllowed(boolean crossOriginAllowed) {
        this.crossOriginAllowed = crossOriginAllowed;
    }
}
