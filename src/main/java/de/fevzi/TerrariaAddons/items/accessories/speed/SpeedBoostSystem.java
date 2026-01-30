package de.fevzi.TerrariaAddons.items.accessories.speed;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.fevzi.TerrariaAddons.items.accessoryPouch.AccessoryPouchSharedContainer;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Applies stacked movement speed bonuses from accessories in the pouch.
 * Add new speed items by updating SPEED_MULTIPLIERS.
 */
public class SpeedBoostSystem extends EntityTickingSystem<EntityStore> {
    private static final float MULTIPLIER_EPSILON = 0.001f;
    private static final Map<String, Float> SPEED_MULTIPLIERS;

    static {
        Map<String, Float> multipliers = new HashMap<>();
        multipliers.put("HermesBoots", 1.2f);
        multipliers.put("Aglet", 1.05f);
        SPEED_MULTIPLIERS = Collections.unmodifiableMap(multipliers);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void tick(float delta,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) {
            return;
        }

        Player player = (Player) store.getComponent(ref, Player.getComponentType());
        if (player == null || player.isWaitingForClientReady()) {
            return;
        }
        PlayerRef playerRef = (PlayerRef) store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        MovementManager movementManager = (MovementManager) commandBuffer.getComponent(ref, MovementManager.getComponentType());
        if (movementManager == null) {
            return;
        }

        float targetMultiplier = 1.0f;
        boolean hasAnySpeedItem = false;
        for (Map.Entry<String, Float> entry : SPEED_MULTIPLIERS.entrySet()) {
            if (AccessoryPouchSharedContainer.hasItemInPouch(player.getInventory(), playerRef.getUuid(), entry.getKey())) {
                targetMultiplier *= entry.getValue();
                hasAnySpeedItem = true;
            }
        }

        float currentMultiplier = getCurrentSpeedMultiplier(movementManager);
        if (Math.abs(currentMultiplier - targetMultiplier) < MULTIPLIER_EPSILON) {
            return;
        }

        if (hasAnySpeedItem) {
            applySpeedMultiplier(movementManager, targetMultiplier);
        } else {
            removeSpeedBoost(movementManager);
        }
        movementManager.update(playerRef.getPacketHandler());
    }

    private static void applySpeedMultiplier(MovementManager movementManager, float multiplier) {
        MovementSettings settings = movementManager.getSettings();
        MovementSettings defaults = movementManager.getDefaultSettings();
        if (settings == null || defaults == null) {
            return;
        }
        settings.baseSpeed = defaults.baseSpeed * multiplier;
        settings.forwardWalkSpeedMultiplier = defaults.forwardWalkSpeedMultiplier * multiplier;
        settings.backwardWalkSpeedMultiplier = defaults.backwardWalkSpeedMultiplier * multiplier;
        settings.strafeWalkSpeedMultiplier = defaults.strafeWalkSpeedMultiplier * multiplier;
        settings.forwardRunSpeedMultiplier = defaults.forwardRunSpeedMultiplier * multiplier;
        settings.backwardRunSpeedMultiplier = defaults.backwardRunSpeedMultiplier * multiplier;
        settings.strafeRunSpeedMultiplier = defaults.strafeRunSpeedMultiplier * multiplier;
        settings.forwardSprintSpeedMultiplier = defaults.forwardSprintSpeedMultiplier * multiplier;
        settings.maxSpeedMultiplier = defaults.maxSpeedMultiplier * multiplier;
    }

    private static void removeSpeedBoost(MovementManager movementManager) {
        MovementSettings settings = movementManager.getSettings();
        MovementSettings defaults = movementManager.getDefaultSettings();
        if (settings == null || defaults == null) {
            return;
        }

        settings.baseSpeed = defaults.baseSpeed;
        settings.forwardWalkSpeedMultiplier = defaults.forwardWalkSpeedMultiplier;
        settings.backwardWalkSpeedMultiplier = defaults.backwardRunSpeedMultiplier;
        settings.strafeWalkSpeedMultiplier = defaults.strafeWalkSpeedMultiplier;
        settings.forwardRunSpeedMultiplier = defaults.forwardRunSpeedMultiplier;
        settings.backwardRunSpeedMultiplier = defaults.backwardRunSpeedMultiplier;
        settings.strafeRunSpeedMultiplier = defaults.strafeRunSpeedMultiplier;
        settings.forwardSprintSpeedMultiplier = defaults.forwardSprintSpeedMultiplier;
        settings.maxSpeedMultiplier = defaults.maxSpeedMultiplier;
    }

    private static float getCurrentSpeedMultiplier(MovementManager movementManager) {
        MovementSettings settings = movementManager.getSettings();
        MovementSettings defaults = movementManager.getDefaultSettings();
        if (settings == null || defaults == null) {
            return 1.0f;
        }
        if (defaults.baseSpeed == 0f) {
            return 1.0f;
        }
        return settings.baseSpeed / defaults.baseSpeed;
    }
}
