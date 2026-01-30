package de.fevzi.TerrariaAddons.items;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the priority and state of double-jump accessories.
 * Tracks which bottle accessories (Cloud, Blizzard, Sandstorm) have been used,
 * handles crouch detection for activation, and ensures proper ordering
 * when multiple bottle accessories are equipped.
 */
public final class BoostOrderManager {
    public enum ActiveBoost {
        NONE,
        BOOTS,
        BLIZZARD,
        SANDSTORM
    }

    private static final Map<PlayerRef, Boolean> usedCloud = new ConcurrentHashMap<>();
    private static final Map<PlayerRef, Boolean> usedBlizzard = new ConcurrentHashMap<>();
    private static final Map<PlayerRef, Boolean> usedSandstorm = new ConcurrentHashMap<>();
    private static final Map<PlayerRef, ActiveBoost> activeBoost = new ConcurrentHashMap<>();
    private static final Map<PlayerRef, Boolean> bootsFuelAvailable = new ConcurrentHashMap<>();

    private static final Map<PlayerRef, Boolean> lastCrouchState = new ConcurrentHashMap<>();
    private static final Map<PlayerRef, Boolean> crouchEdgeAvailable = new ConcurrentHashMap<>();
    private static final Map<PlayerRef, Boolean> crouchPressConsumed = new ConcurrentHashMap<>();


    private BoostOrderManager() {
    }

    public static void reset(PlayerRef playerRef) {
        usedCloud.put(playerRef, false);
        usedBlizzard.put(playerRef, false);
        usedSandstorm.put(playerRef, false);
        activeBoost.put(playerRef, ActiveBoost.NONE);
        bootsFuelAvailable.put(playerRef, false);
        lastCrouchState.put(playerRef, false);
        crouchEdgeAvailable.put(playerRef, false);
        crouchPressConsumed.put(playerRef, false);
    }

    public static void setBootsFuelAvailable(PlayerRef playerRef, boolean available) {
        bootsFuelAvailable.put(playerRef, available);
    }

    public static boolean canUseBoots(PlayerRef playerRef, boolean hasBoots, boolean hasFuel) {
        if (!hasBoots || !hasFuel) {
            return false;
        }
        ActiveBoost current = activeBoost.getOrDefault(playerRef, ActiveBoost.NONE);
        return current == ActiveBoost.NONE || current == ActiveBoost.BOOTS;
    }

    public static void markCloudUsed(PlayerRef playerRef) {
        usedCloud.put(playerRef, true);
    }

    public static boolean canUseCloud(PlayerRef playerRef, boolean hasCloud) {
        if (!hasCloud || usedCloud.getOrDefault(playerRef, false)) {
            return false;
        }
        if (bootsFuelAvailable.getOrDefault(playerRef, false)) {
            return false;
        }
        return true;
    }

    public static boolean canUseBlizzard(PlayerRef playerRef, boolean hasCloud, boolean hasBlizzard) {
        if (!hasBlizzard) return false;

        if (activeBoost.getOrDefault(playerRef, ActiveBoost.NONE) == ActiveBoost.BLIZZARD) {
            return true;
        }

        if (usedBlizzard.getOrDefault(playerRef, false)) {
            return false;
        }

        if (bootsFuelAvailable.getOrDefault(playerRef, false)) {
            return false;
        }

        if (hasCloud && !usedCloud.getOrDefault(playerRef, false)) {
            return false;
        }

        return activeBoost.getOrDefault(playerRef, ActiveBoost.NONE) != ActiveBoost.SANDSTORM;
    }

    public static boolean canUseSandstorm(PlayerRef playerRef, boolean hasCloud, boolean hasBlizzard, boolean hasSandstorm) {
        if (!hasSandstorm) return false;

        if (activeBoost.getOrDefault(playerRef, ActiveBoost.NONE) == ActiveBoost.SANDSTORM) {
            return true;
        }

        if (usedSandstorm.getOrDefault(playerRef, false)) {
            return false;
        }

        if (bootsFuelAvailable.getOrDefault(playerRef, false)) {
            return false;
        }

        if (hasCloud && !usedCloud.getOrDefault(playerRef, false)) {
            return false;
        }

        if (hasBlizzard && !usedBlizzard.getOrDefault(playerRef, false)) {
            return false;
        }

        return activeBoost.getOrDefault(playerRef, ActiveBoost.NONE) != ActiveBoost.BLIZZARD;
    }

    public static void setBlizzardActive(PlayerRef playerRef) {
        usedBlizzard.put(playerRef, true);
        activeBoost.put(playerRef, ActiveBoost.BLIZZARD);
    }

    public static void setBootsActive(PlayerRef playerRef) {
        activeBoost.put(playerRef, ActiveBoost.BOOTS);
    }

    public static void setSandstormActive(PlayerRef playerRef) {
        usedSandstorm.put(playerRef, true);
        activeBoost.put(playerRef, ActiveBoost.SANDSTORM);
    }

    public static boolean isActive(PlayerRef playerRef, ActiveBoost expected) {
        return activeBoost.getOrDefault(playerRef, ActiveBoost.NONE) == expected;
    }

    public static boolean isCrouchPressed(PlayerRef playerRef, boolean holdingCrouch) {
        boolean wasCrouching = lastCrouchState.getOrDefault(playerRef, false);
        if (!holdingCrouch) {
            lastCrouchState.put(playerRef, false);
            crouchEdgeAvailable.put(playerRef, false);
            crouchPressConsumed.put(playerRef, false);
            return false;
        }
        if (!wasCrouching) {
            lastCrouchState.put(playerRef, true);
            crouchEdgeAvailable.put(playerRef, true);
            crouchPressConsumed.put(playerRef, false);
        }
        return crouchEdgeAvailable.getOrDefault(playerRef, false)
                && !crouchPressConsumed.getOrDefault(playerRef, false);
    }

    public static void consumeCrouchPress(PlayerRef playerRef) {
        crouchPressConsumed.put(playerRef, true);
    }

    public static void clearActiveBoost(PlayerRef playerRef, ActiveBoost expected) {
        if (activeBoost.getOrDefault(playerRef, ActiveBoost.NONE) == expected) {
            activeBoost.put(playerRef, ActiveBoost.NONE);
        }
    }

}
