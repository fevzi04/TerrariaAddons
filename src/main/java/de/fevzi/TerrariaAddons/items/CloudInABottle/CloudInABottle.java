package de.fevzi.TerrariaAddons.items.CloudInABottle;

import java.util.Collections;
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
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import de.fevzi.TerrariaAddons.items.BoostOrderManager;
import de.fevzi.TerrariaAddons.items.AccessoryPouch.AccessoryPouchSharedContainer;

public class CloudInABottle extends EntityTickingSystem<EntityStore> {

    private static final String ITEM_ID = "CloudInABottle";
    private static final double DOUBLE_JUMP_FORCE = 15.0D;
    private static final double FORWARD_BOOST = 10.0D;

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

        MovementStatesComponent movementStatesComponent = store.getComponent(ref, MovementStatesComponent.getComponentType());
        if (movementStatesComponent == null) return;
        
        MovementStates states = movementStatesComponent.getMovementStates();

        if (states.onGround) {
            BoostOrderManager.reset(playerRef);
            return;
        }

        if (states.swimming) {
            return;
        }

        boolean crouchPressed = BoostOrderManager.isCrouchPressed(playerRef, states.crouching);

        if (crouchPressed && !states.onGround && BoostOrderManager.canUseCloud(playerRef, hasItem)) {

            BoostOrderManager.markCloudUsed(playerRef);
            BoostOrderManager.consumeCrouchPress(playerRef);
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
                Vector3d jumpVector = new Vector3d(boostX, DOUBLE_JUMP_FORCE, boostZ);
                velocity.addInstruction(jumpVector, (VelocityConfig) null, ChangeVelocityType.Set);
            }

            int soundIndex = SoundEvent.getAssetMap().getIndex("CloudInABottle_DoubleJump");

            TransformComponent transform = store.getComponent(ref, EntityModule.get().getTransformComponentType());
            if (transform != null) {
                SoundUtil.playSoundEvent3dToPlayer(ref, soundIndex, SoundCategory.UI, transform.getPosition(), store);

                Vector3d center = transform.getPosition();
                double radius = 1.0D;
                for (int i = 0; i < 8; ++i) {
                    double angle = 6.283185307179586D * (double) i / 8.0D;
                    double x = center.x + radius * Math.cos(angle);
                    double z = center.z + radius * Math.sin(angle);
                    double y = center.y + 0.1D;
                    Vector3d pos = new Vector3d(x, y, z);
                    ParticleUtil.spawnParticleEffect(
                            "Block_Break_Snow",
                            pos,
                            Collections.singletonList(ref),
                            commandBuffer);
                }
            }
        }
    }
}
