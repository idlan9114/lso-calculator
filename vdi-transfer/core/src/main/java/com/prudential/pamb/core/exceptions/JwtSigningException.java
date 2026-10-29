package com.prudential.pamb.core.exceptions;

/**
 * Thrown by {@link com.prudential.pamb.core.utils.RsaJwtSigner} when a JWT cannot be signed, e.g. the
 * private key is missing, still an unresolved environment placeholder, or not a valid RSA key.
 * Callers wrap it in their own API exception.
 */
public class JwtSigningException extends Exception {

    private static final long serialVersionUID = 1L;

    public JwtSigningException(String message) {
        super(message);
    }

    public JwtSigningException(String message, Throwable cause) {
        super(message, cause);
    }
}
