package de.fevzi.TerrariaAddons.items.accessories.BlizzardInABottle;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import de.fevzi.TerrariaAddons.items.BoostOrderManager;
import de.fevzi.TerrariaAddons.items.accessoryPouch.AccessoryPouchSharedContainer;

/**
 * System that handles the Blizzard in a Bottle accessory effect.
 * Provides a double-jump ability with a sustained forward boost
 * when the player crouches while airborne. Consumes fuel during use and
 * refuels when the player lands. Creates snow particle effects during flight.
 */
public class BlizzardInABottle extends EntityTickingSystem<EntityStore> {

    private final Map<PlayerRef, Integer> fuelRemaining = new ConcurrentHashMap<>();
    private static final String ITEM_ID = "BlizzardInABottle";
    private static final double LEAP_FORCE = 8.0D;
    private static final double FORWARD_BOOST = 10.0D;
    private static final int MAX_FUEL = 100;
    private static final int FUEL_CONSUMPTION = 5;

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
        if (ref == null || !ref.isValid()) return;

        Player player = (Player) store.getComponent(ref, Player.getComponentType());
        if (player == null || player.isWaitingForClientReady()) return;

        PlayerRef playerRef = (PlayerRef) store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        boolean hasItem = AccessoryPouchSharedContainer.hasItemInPouch(player.getInventory(), playerRef.getUuid(), ITEM_ID);
        boolean hasCloud = AccessoryPouchSharedContainer.hasItemInPouch(player.getInventory(), playerRef.getUuid(), "CloudInABottle");

        MovementStatesComponent movementStatesComponent = store.getComponent(ref, MovementStatesComponent.getComponentType());
        if (movementStatesComponent == null) return;

        MovementStates states = movementStatesComponent.getMovementStates();

        if (states.onGround) {
            this.fuelRemaining.put(playerRef, MAX_FUEL);
            BoostOrderManager.reset(playerRef);
            return;
        }

        if (states.swimming) {
            return;
        }

        boolean holdingCrouch = states.crouching;
        boolean crouchPressed = BoostOrderManager.isCrouchPressed(playerRef, holdingCrouch);

        int currentFuel = this.fuelRemaining.getOrDefault(playerRef, MAX_FUEL);

        boolean canUse = BoostOrderManager.canUseBlizzard(playerRef, hasCloud, hasItem);
        boolean isActive = BoostOrderManager.isActive(playerRef, BoostOrderManager.ActiveBoost.BLIZZARD);
        boolean shouldActivate = (crouchPressed || isActive);

        if (shouldActivate && holdingCrouch && !states.onGround && currentFuel >= FUEL_CONSUMPTION && canUse) {

            this.fuelRemaining.put(playerRef, currentFuel - FUEL_CONSUMPTION);

            if (crouchPressed && !isActive) {
                BoostOrderManager.consumeCrouchPress(playerRef);
            }
            Velocity velocity = store.getComponent(ref, Velocity.getComponentType());
            if (velocity != null) {
                double boostX = 0.0D;
                double boostZ = 0.0D;
                TransformComponent transform = store.getComponent(ref, EntityModule.get().getTransformComponentType());
                if (transform != null) {
                    Vector3f rotation = transform.getRotation();
                    if (rotation != null) {
                        double yaw = rotation.y;
                        boostX = -Math.sin(yaw) * FORWARD_BOOST;
                        boostZ = -Math.cos(yaw) * FORWARD_BOOST;
                    }
                }
                Vector3d leapVector = new Vector3d(boostX, LEAP_FORCE, boostZ);
                velocity.addInstruction(leapVector, (VelocityConfig) null, ChangeVelocityType.Set);
            }

            TransformComponent transform = store.getComponent(ref, EntityModule.get().getTransformComponentType());
            if (transform != null) {
                Vector3d center = transform.getPosition();
                Vector3d pos = new Vector3d(center.x, center.y - 0.25D, center.z);
                ParticleUtil.spawnParticleEffect(
                        "Block_Break_Snow",
                        pos,
                        Collections.singletonList(ref),
                        commandBuffer);
            }
            BoostOrderManager.setBlizzardActive(playerRef);
        } else {
            BoostOrderManager.clearActiveBoost(playerRef, BoostOrderManager.ActiveBoost.BLIZZARD);
        }
    }
}