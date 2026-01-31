package de.fevzi.TerrariaAddons.items.accessories.rocketBoots;

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
 * System that handles the Rocket Boots accessory effect.
 * Provides a sustained forward boost when crouching mid-air.
 * Can be reactivated mid-air as long as fuel remains.
 */
public class RocketBootsSystem extends EntityTickingSystem<EntityStore> {

    private final Map<PlayerRef, Integer> fuelRemaining = new ConcurrentHashMap<>();

    private static final String ROCKET_BOOTS_ID = "RocketBoots";
    private static final String SPECTRE_BOOTS_ID = "SpectreBoots";
    private static final String LIGHTNING_BOOTS_ID = "LightningBoots";
    private static final double LEAP_FORCE = 8.0D;
    private static final double FORWARD_BOOST = 10.0D;
    private static final int MAX_FUEL = 150;
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

        boolean hasItem = AccessoryPouchSharedContainer.hasItemInPouch(player.getInventory(), playerRef.getUuid(), ROCKET_BOOTS_ID)
                || AccessoryPouchSharedContainer.hasItemInPouch(player.getInventory(), playerRef.getUuid(), SPECTRE_BOOTS_ID)
                || AccessoryPouchSharedContainer.hasItemInPouch(player.getInventory(), playerRef.getUuid(), LIGHTNING_BOOTS_ID);
        if (!hasItem) {
            BoostOrderManager.setBootsFuelAvailable(playerRef, false);
            BoostOrderManager.clearActiveBoost(playerRef, BoostOrderManager.ActiveBoost.BOOTS);
            return;
        }

        MovementStatesComponent movementStatesComponent = store.getComponent(ref, MovementStatesComponent.getComponentType());
        if (movementStatesComponent == null) return;

        MovementStates states = movementStatesComponent.getMovementStates();

        if (states.onGround) {
            fuelRemaining.put(playerRef, MAX_FUEL);
            BoostOrderManager.reset(playerRef);
            BoostOrderManager.setBootsFuelAvailable(playerRef, true);
            return;
        }

        if (states.swimming) {
            BoostOrderManager.setBootsFuelAvailable(playerRef, false);
            BoostOrderManager.clearActiveBoost(playerRef, BoostOrderManager.ActiveBoost.BOOTS);
            return;
        }

        boolean holdingCrouch = states.crouching;
        boolean crouchPressed = BoostOrderManager.isCrouchPressed(playerRef, holdingCrouch);
        int currentFuel = fuelRemaining.getOrDefault(playerRef, MAX_FUEL);
        boolean hasFuel = currentFuel >= FUEL_CONSUMPTION;
        BoostOrderManager.setBootsFuelAvailable(playerRef, hasFuel);
        boolean isActive = BoostOrderManager.isActive(playerRef, BoostOrderManager.ActiveBoost.BOOTS);
        boolean shouldActivate = (crouchPressed || isActive);

        if (shouldActivate && holdingCrouch && BoostOrderManager.canUseBoots(playerRef, hasItem, hasFuel)) {
            fuelRemaining.put(playerRef, currentFuel - FUEL_CONSUMPTION);

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
                Vector3d pos = new Vector3d(center.x, center.y - 0.35D, center.z);
                ParticleUtil.spawnParticleEffect(
                        "Block_Break_Stone",
                        pos,
                        Collections.singletonList(ref),
                        commandBuffer);
            }
            BoostOrderManager.setBootsActive(playerRef);
        } else {
            BoostOrderManager.clearActiveBoost(playerRef, BoostOrderManager.ActiveBoost.BOOTS);
        }
    }
}
