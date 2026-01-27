package de.fevzi.TerrariaAddons.items.magicmirror;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerConfigData;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerRespawnPointData;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Optional;

/**
 * Interaction handler for the Magic Mirror item.
 * Teleports the player to their nearest respawn point (bed or world spawn)
 * without consuming the item, allowing unlimited use.
 */
public class MirrorRecallInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<MirrorRecallInteraction> CODEC;

    static {
        CODEC = BuilderCodec.builder(MirrorRecallInteraction.class, MirrorRecallInteraction::new, SimpleInstantInteraction.CODEC).build();
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldown) {
        Ref<EntityStore> ref = context.getEntity();
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        Player player = (Player) commandBuffer.getComponent(ref, Player.getComponentType());

        if (player != null) {

            Transform transform = getClosestRespawnPoint(player, commandBuffer);

            if (transform != null) {
                commandBuffer.addComponent(
                        player.getReference(),
                        Teleport.getComponentType(),
                        Teleport.createForPlayer(player.getWorld(), transform)
                );
            }
        }
    }

    private static Transform getClosestRespawnPoint(Player player, ComponentAccessor<EntityStore> componentAccessor) {
        World world = player.getWorld();
        Ref<EntityStore> ref = player.getReference();
        PlayerConfigData playerData = player.getPlayerConfigData();

        if (playerData.getPerWorldData(world.getName()) != null) {
            PlayerRespawnPointData[] respawnPoints = playerData.getPerWorldData(world.getName()).getRespawnPoints();

            if (respawnPoints != null && respawnPoints.length != 0) {
                TransformComponent playerTransformPos = (TransformComponent) componentAccessor.getComponent(ref, TransformComponent.getComponentType());
                if (playerTransformPos != null) {
                    Vector3d playerPos = playerTransformPos.getPosition();

                    Optional<PlayerRespawnPointData> nearestPos = Arrays.stream(respawnPoints).min((a, b) -> {
                        Vector3d posA = a.getRespawnPosition();
                        Vector3d posB = b.getRespawnPosition();
                        return Double.compare(
                                playerPos.distanceSquaredTo(posA.x, playerPos.y, posA.z),
                                playerPos.distanceSquaredTo(posB.x, playerPos.y, posB.z)
                        );
                    });

                    if (nearestPos.isPresent()) {
                        return new Transform(nearestPos.get().getRespawnPosition());
                    }
                }
            }
        }

        Transform worldSpawnPoint = world.getWorldConfig().getSpawnProvider().getSpawnPoint(ref, componentAccessor);
        worldSpawnPoint.setRotation(Vector3f.ZERO);
        return worldSpawnPoint;
    }
}
