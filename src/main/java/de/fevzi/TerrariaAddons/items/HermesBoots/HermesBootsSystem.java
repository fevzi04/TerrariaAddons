package de.fevzi.TerrariaAddons.items.HermesBoots;

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
import de.fevzi.TerrariaAddons.items.AccessoryPouch.AccessoryPouchSharedContainer;

import javax.annotation.Nonnull;
public class HermesBootsSystem extends EntityTickingSystem<EntityStore> {
    private static final String HERMES_BOOTS_ITEM_ID = "HermesBoots";
    private static final float SPEED_MULTIPLIER = 1.2f;

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

        boolean hasBoots = AccessoryPouchSharedContainer.hasItemInPouch(player.getInventory(), playerRef.getUuid(), HERMES_BOOTS_ITEM_ID);
        boolean isBoosted = isSpeedBoostApplied(movementManager);

        if (hasBoots == isBoosted) {
            return;
        }

        if (hasBoots) {
            applySpeedBoost(movementManager);
            movementManager.update(playerRef.getPacketHandler());
        } else {
            removeSpeedBoost(movementManager);
            movementManager.update(playerRef.getPacketHandler());
        }
    }

    private static void applySpeedBoost(MovementManager movementManager) {
        MovementSettings settings = movementManager.getSettings();
        MovementSettings defaults = movementManager.getDefaultSettings();
        if (settings == null || defaults == null) {
            return;
        }
        settings.baseSpeed = defaults.baseSpeed * SPEED_MULTIPLIER;
        settings.forwardWalkSpeedMultiplier = defaults.forwardWalkSpeedMultiplier * SPEED_MULTIPLIER;
        settings.backwardWalkSpeedMultiplier = defaults.backwardWalkSpeedMultiplier * SPEED_MULTIPLIER;
        settings.strafeWalkSpeedMultiplier = defaults.strafeWalkSpeedMultiplier * SPEED_MULTIPLIER;
        settings.forwardRunSpeedMultiplier = defaults.forwardRunSpeedMultiplier * SPEED_MULTIPLIER;
        settings.backwardRunSpeedMultiplier = defaults.backwardRunSpeedMultiplier * SPEED_MULTIPLIER;
        settings.strafeRunSpeedMultiplier = defaults.strafeRunSpeedMultiplier * SPEED_MULTIPLIER;
        settings.forwardSprintSpeedMultiplier = defaults.forwardSprintSpeedMultiplier * SPEED_MULTIPLIER;
        settings.maxSpeedMultiplier = defaults.maxSpeedMultiplier * SPEED_MULTIPLIER;
    }

    private static void removeSpeedBoost(MovementManager movementManager) {
        MovementSettings settings = movementManager.getSettings();
        MovementSettings defaults = movementManager.getDefaultSettings();
        if (settings == null || defaults == null) {
            return;
        }

        settings.baseSpeed = defaults.baseSpeed;
        settings.forwardWalkSpeedMultiplier = defaults.forwardWalkSpeedMultiplier;
        settings.backwardWalkSpeedMultiplier = defaults.backwardWalkSpeedMultiplier;
        settings.strafeWalkSpeedMultiplier = defaults.strafeWalkSpeedMultiplier;
        settings.forwardRunSpeedMultiplier = defaults.forwardRunSpeedMultiplier;
        settings.backwardRunSpeedMultiplier = defaults.backwardRunSpeedMultiplier;
        settings.strafeRunSpeedMultiplier = defaults.strafeRunSpeedMultiplier;
        settings.forwardSprintSpeedMultiplier = defaults.forwardSprintSpeedMultiplier;
        settings.maxSpeedMultiplier = defaults.maxSpeedMultiplier;
    }

    private static boolean isSpeedBoostApplied(MovementManager movementManager) {
        MovementSettings settings = movementManager.getSettings();
        MovementSettings defaults = movementManager.getDefaultSettings();
        if (settings == null || defaults == null) {
            return false;
        }
        return settings.baseSpeed > defaults.baseSpeed * (SPEED_MULTIPLIER - 0.001f);
    }


}
