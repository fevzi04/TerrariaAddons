package de.fevzi.TerrariaAddons.items.LavaCharm;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LavaCharmImmunityManager {
    public static final float IMMUNITY_DURATION_SECONDS = 7.0f;
    private static final float RECHARGE_SPEED = 1.0f;

    private static final long RECHARGE_DELAY_MS = 1000L;

    private static final Map<UUID, Float> immunityTimers = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastBurnEffectTime = new ConcurrentHashMap<>();

    public static void recordDamage(UUID playerUuid) {
        lastBurnEffectTime.put(playerUuid, System.currentTimeMillis());
        immunityTimers.putIfAbsent(playerUuid, IMMUNITY_DURATION_SECONDS);
    }

    public static boolean isImmune(UUID playerUuid) {
        return getRemainingTime(playerUuid) > 0.0f;
    }

    public static float getRemainingTime(UUID playerUuid) {
        return immunityTimers.getOrDefault(playerUuid, IMMUNITY_DURATION_SECONDS);
    }

    public static float getProgress(UUID playerUuid) {
        return getRemainingTime(playerUuid) / IMMUNITY_DURATION_SECONDS;
    }

    public static boolean tick(UUID playerUuid, float delta, boolean hasBurnEffect) {
        float current = getRemainingTime(playerUuid);
        long lastBurn = lastBurnEffectTime.getOrDefault(playerUuid, 0L);
        long timeSinceBurn = System.currentTimeMillis() - lastBurn;

        float next = current;

        if (hasBurnEffect) {
            lastBurnEffectTime.put(playerUuid, System.currentTimeMillis());
            next = Math.max(0.0f, current - delta);

        } else if (timeSinceBurn < RECHARGE_DELAY_MS) {

        } else {
            next = Math.min(IMMUNITY_DURATION_SECONDS, current + (delta * RECHARGE_SPEED));
        }

        immunityTimers.put(playerUuid, next);

        if (next >= IMMUNITY_DURATION_SECONDS && timeSinceBurn >= RECHARGE_DELAY_MS) {
            immunityTimers.remove(playerUuid);
            lastBurnEffectTime.remove(playerUuid);
            return false;
        }

        return true;
    }

    public static void clearImmunity(UUID playerUuid) {
        immunityTimers.remove(playerUuid);
        lastBurnEffectTime.remove(playerUuid);
    }
}