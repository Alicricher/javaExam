package com.dentistrybot.admin.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory brute-force guard for /api/auth/login, keyed by client IP.
 * Locks an IP out for {@link #LOCK_SECONDS} after {@link #MAX_ATTEMPTS} failures
 * within a {@link #WINDOW_SECONDS} sliding window. A single process, in-memory
 * store is fine at this app's scale; if it ever runs as multiple instances behind
 * a load balancer, this would need to move to a shared store (e.g. Redis).
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 15 * 60;
    private static final long LOCK_SECONDS = 15 * 60;
    private static final long STALE_SECONDS = 60 * 60;

    private static final class Attempt {
        final AtomicInteger count = new AtomicInteger();
        volatile Instant windowStart = Instant.now();
        volatile Instant lockedUntil;
    }

    private final Map<String, Attempt> attemptsByIp = new ConcurrentHashMap<>();

    public boolean isLocked(String ip) {
        Attempt a = attemptsByIp.get(ip);
        return a != null && a.lockedUntil != null && Instant.now().isBefore(a.lockedUntil);
    }

    public long secondsUntilUnlock(String ip) {
        Attempt a = attemptsByIp.get(ip);
        if (a == null || a.lockedUntil == null) return 0;
        return Math.max(a.lockedUntil.getEpochSecond() - Instant.now().getEpochSecond(), 0);
    }

    public void recordFailure(String ip) {
        Instant now = Instant.now();
        Attempt a = attemptsByIp.computeIfAbsent(ip, k -> new Attempt());
        synchronized (a) {
            if (now.isAfter(a.windowStart.plusSeconds(WINDOW_SECONDS))) {
                a.count.set(0);
                a.windowStart = now;
                a.lockedUntil = null;
            }
            if (a.count.incrementAndGet() >= MAX_ATTEMPTS) {
                a.lockedUntil = now.plusSeconds(LOCK_SECONDS);
            }
        }
        cleanupOccasionally();
    }

    public void recordSuccess(String ip) {
        attemptsByIp.remove(ip);
    }

    /** Opportunistic sweep so scanners hammering random IPs don't grow this map forever. */
    private void cleanupOccasionally() {
        if (attemptsByIp.size() < 1000 || Math.random() > 0.01) return;
        Instant cutoff = Instant.now().minusSeconds(STALE_SECONDS);
        attemptsByIp.entrySet().removeIf(e -> {
            Attempt a = e.getValue();
            Instant lastActivity = a.lockedUntil != null ? a.lockedUntil : a.windowStart;
            return lastActivity.isBefore(cutoff);
        });
    }
}
