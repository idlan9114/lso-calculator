package com.prudential.pamb.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prudential.pamb.core.exceptions.JwtSigningException;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Native RS256 JWT signing with the JDK security APIs (no third-party JWT library). Shared by the
 * Fund Prices and Legacy Blueprint token services, which own their own claims and caching.
 */
public final class RsaJwtSigner {

    public static final String JWT_ALGORITHM = "RS256";
    public static final String JWT_TYPE = "JWT";

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String KEY_ALGORITHM = "RSA";
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RsaJwtSigner() {
    }

    /**
     * Signs the given claims as a compact RS256 JWT.
     *
     * @param claims     the JWT payload
     * @param rawKey     RSA private key as PKCS#8 or PKCS#1 PEM (literal {@code \n} escapes and quotes tolerated)
     * @param keyVarName environment variable that supplies the key, used only in error messages
     * @return the signed token
     * @throws JwtSigningException if the key is missing, unresolved or invalid, or signing fails
     */
    public static String sign(ObjectNode claims, String rawKey, String keyVarName) throws JwtSigningException {
        PrivateKey privateKey = loadPrivateKey(rawKey, keyVarName);
        try {
            ObjectNode header = MAPPER.createObjectNode();
            header.put("alg", JWT_ALGORITHM);
            header.put("typ", JWT_TYPE);

            String signingInput = encode(MAPPER.writeValueAsBytes(header))
                    + "." + encode(MAPPER.writeValueAsBytes(claims));

            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return signingInput + "." + encode(signature.sign());
        } catch (Exception e) {
            throw new JwtSigningException("Failed to sign JWT.", e);
        }
    }

    /** Returns true when an OSGi value is blank or still an unsubstituted {@code $[env:...]} placeholder. */
    public static boolean isUnresolved(String value) {
        return StringUtils.isBlank(value) || value.contains("$[env:") || value.contains("$[secret:");
    }

    static PrivateKey loadPrivateKey(String rawKey, String keyVarName) throws JwtSigningException {
        if (StringUtils.isBlank(rawKey)) {
            throw new JwtSigningException("Cannot generate JWT: signing key (" + keyVarName + ") is not configured.");
        }
        // Placeholder left unsubstituted means the env var is not set on this instance.
        if (isUnresolved(rawKey)) {
            throw new JwtSigningException("Cannot generate JWT: the signing key still contains an unresolved "
                    + "placeholder (" + rawKey.trim() + "). Set the " + keyVarName + " environment "
                    + "variable (or override the OSGi config value) for this instance.");
        }

        String pem = normalisePem(rawKey);
        boolean pkcs1 = pem.contains("BEGIN RSA PRIVATE KEY");
        String base64Body = pem
                .replaceAll("-----BEGIN (RSA )?PRIVATE KEY-----", StringUtils.EMPTY)
                .replaceAll("-----END (RSA )?PRIVATE KEY-----", StringUtils.EMPTY)
                .replaceAll("\\s", StringUtils.EMPTY);

        byte[] der;
        try {
            der = Base64.getDecoder().decode(base64Body);
        } catch (IllegalArgumentException e) {
            throw new JwtSigningException("Signing key is not valid Base64 after normalisation. Ensure "
                    + keyVarName + " holds a PKCS#8 (or PKCS#1) PEM; single-line values may use "
                    + "\\n as the line separator.", e);
        }

        try {
            if (pkcs1) {
                der = wrapPkcs1AsPkcs8(der);
            }
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new JwtSigningException("Invalid RSA private key in " + keyVarName + ".", e);
        }
    }

    // Handles keys stored as a single line with literal \n escapes and/or wrapped in quotes.
    private static String normalisePem(String rawKey) {
        String pem = rawKey
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\r", "\n")
                .replace("\\t", " ")
                .trim();
        if (pem.length() >= 2 && pem.startsWith("\"") && pem.endsWith("\"")) {
            pem = pem.substring(1, pem.length() - 1).trim();
        }
        return pem;
    }

    // Wraps a PKCS#1 RSAPrivateKey in a PKCS#8 PrivateKeyInfo so PKCS8EncodedKeySpec can read it.
    private static byte[] wrapPkcs1AsPkcs8(byte[] pkcs1) {
        byte[] version = {0x02, 0x01, 0x00}; // INTEGER 0
        // AlgorithmIdentifier { OID 1.2.840.113549.1.1.1 (rsaEncryption), NULL }
        byte[] algorithmId = {0x30, 0x0d, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48,
                (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00};
        byte[] privateKeyOctet = derTlv(0x04, pkcs1); // OCTET STRING { pkcs1 }
        return derTlv(0x30, concat(version, algorithmId, privateKeyOctet)); // SEQUENCE
    }

    private static byte[] derTlv(int tag, byte[] content) {
        byte[] length = derLength(content.length);
        return concat(new byte[]{(byte) tag}, length, content);
    }

    private static byte[] derLength(int length) {
        if (length < 0x80) {
            return new byte[]{(byte) length};
        }
        if (length < 0x100) {
            return new byte[]{(byte) 0x81, (byte) length};
        }
        if (length < 0x10000) {
            return new byte[]{(byte) 0x82, (byte) (length >> 8), (byte) length};
        }
        return new byte[]{(byte) 0x83, (byte) (length >> 16), (byte) (length >> 8), (byte) length};
    }

    private static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] part : parts) {
            total += part.length;
        }
        byte[] result = new byte[total];
        int position = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, position, part.length);
            position += part.length;
        }
        return result;
    }

    private static String encode(byte[] bytes) {
        return BASE64_URL.encodeToString(bytes);
    }
}
