package com.trade.platform.security;

import java.util.UUID;

/**
 * Per-request user context populated by {@link JwtAuthenticationFilter}
 * after a valid JWT (issued by the NestJS BFF) is presented.
 */
public final class UserContext {

    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UUID userId) {
        CURRENT_USER.set(userId);
    }

    public static UUID get() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}