package com.cognivio.ai.common.support

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

import java.time.Instant
import java.util.Date

/**
 * Signs test JWTs with a locally-generated RSA key and decodes them with a
 * NimbusJwtDecoder built from the matching public key — so the security tests
 * exercise real signature verification without reaching any live Cognito JWKS.
 */
class TestJwtFactory {

    final RSAKey rsaKey
    final NimbusJwtDecoder decoder

    TestJwtFactory() {
        this.rsaKey = new RSAKeyGenerator(2048).keyID('test-key').generate()
        this.decoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build()
    }

    /** Serialize a signed JWT with the given claims. */
    String signedToken(Map<String, Object> claims) {
        def builder = new JWTClaimsSet.Builder()
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
        claims.each { k, v ->
            if (k == 'sub') {
                builder.subject(v as String)
            } else {
                builder.claim(k, v)
            }
        }
        def signed = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.keyID).build(),
                builder.build())
        signed.sign(new RSASSASigner(rsaKey))
        return signed.serialize()
    }

    /** Sign AND verify-decode, returning the verified Spring {@link Jwt}. */
    Jwt verifiedJwt(Map<String, Object> claims) {
        return decoder.decode(signedToken(claims))
    }
}
