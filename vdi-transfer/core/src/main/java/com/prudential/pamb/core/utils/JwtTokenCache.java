package com.prudential.pamb.core.utils;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe, in-memory cache for a single JWT. The token is reused until its refresh time
 * ({@code exp - buffer}); concurrent callers that find it expired sign only one replacement.
 */
public final class JwtTokenCache {

    @FunctionalInterface
    public interface TokenGenerator<E extends Exception> {
        String generate(long iat, long exp) throws E;
    }

    private final long iatSkewSeconds;
    private final long ttlSeconds;
    private final long refreshBufferSeconds;
    private final AtomicReference<CachedToken> cache = new AtomicReference<>();

    public JwtTokenCache(long iatSkewSeconds, long ttlSeconds, long refreshBufferSeconds) {
        this.iatSkewSeconds = iatSkewSeconds;
        this.ttlSeconds = ttlSeconds;
        this.refreshBufferSeconds = refreshBufferSeconds;
    }

    public <E extends Exception> String get(long now, TokenGenerator<E> generator) throws E {
        CachedToken cached = cache.get();
        if (cached != null && cached.isValid(now)) {
            return cached.token;
        }
        return regenerate(now, generator);
    }

    /** Called after an HTTP 401 so the next {@link #get} signs a fresh token. */
    public void invalidate() {
        cache.set(null);
    }

    private synchronized <E extends Exception> String regenerate(long now, TokenGenerator<E> generator) throws E {
        // Another thread may have refreshed while we waited for the lock.
        CachedToken cached = cache.get();
        if (cached != null && cached.isValid(now)) {
            return cached.token;
        }
        long iat = now - iatSkewSeconds;
        long exp = iat + ttlSeconds;
        String token = generator.generate(iat, exp);
        cache.set(new CachedToken(token, exp - refreshBufferSeconds));
        return token;
    }

    private static final class CachedToken {
        private final String token;
        private final long refreshAtEpochSeconds;

        private CachedToken(String token, long refreshAtEpochSeconds) {
            this.token = token;
            this.refreshAtEpochSeconds = refreshAtEpochSeconds;
        }

        private boolean isValid(long nowEpochSeconds) {
            return nowEpochSeconds < refreshAtEpochSeconds;
        }
    }
}
