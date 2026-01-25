package de.fevzi.TerrariaAddons.items.Balloon;

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

public class BalloonSystem extends EntityTickingSystem<EntityStore> {
    private static final String BALLOON_ITEM_ID = "Balloon";
    private static final float JUMP_MULTIPLIER = 1.3f;

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

        boolean hasBalloon = AccessoryPouchSharedContainer.hasItemInPouch(player.getInventory(), playerRef.getUuid(), BALLOON_ITEM_ID);
        boolean isBoosted = isJumpBoostApplied(movementManager);

        if (hasBalloon == isBoosted) {
            return;
        }

        if (hasBalloon) {
            applyJumpBoost(movementManager);
            movementManager.update(playerRef.getPacketHandler());
        } else {
            removeJumpBoost(movementManager);
            movementManager.update(playerRef.getPacketHandler());
        }
    }

    private static void applyJumpBoost(MovementManager movementManager) {
        MovementSettings settings = movementManager.getSettings();
        MovementSettings defaults = movementManager.getDefaultSettings();
        if (settings == null || defaults == null) {
            return;
        }
        settings.jumpForce = defaults.jumpForce * JUMP_MULTIPLIER;
        settings.fallJumpForce = defaults.fallJumpForce * JUMP_MULTIPLIER;
    }

    private static void removeJumpBoost(MovementManager movementManager) {
        MovementSettings settings = movementManager.getSettings();
        MovementSettings defaults = movementManager.getDefaultSettings();
        if (settings == null || defaults == null) {
            return;
        }
        settings.jumpForce = defaults.jumpForce;
        settings.fallJumpForce = defaults.fallJumpForce;
    }

    private static boolean isJumpBoostApplied(MovementManager movementManager) {
        MovementSettings settings = movementManager.getSettings();
        MovementSettings defaults = movementManager.getDefaultSettings();
        if (settings == null || defaults == null) {
            return false;
        }
        return settings.jumpForce > defaults.jumpForce * (JUMP_MULTIPLIER - 0.001f);
    }

}
