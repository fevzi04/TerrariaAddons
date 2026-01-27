package de.fevzi.TerrariaAddons.items.potions.recall;

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
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import java.util.Arrays;
import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * Interaction handler for the Recall Potion.
 * Teleports the player to their nearest respawn point (bed or world spawn)
 * and consumes the potion in the process.
 */
public class RecallInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<RecallInteraction> CODEC;

    static {
        CODEC = BuilderCodec.builder(RecallInteraction.class, RecallInteraction::new, SimpleInstantInteraction.CODEC).build();
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

                consumeHeldPotion(player);
            }
        }
    }

    private void consumeHeldPotion(Player player) {
        if (player.getInventory() != null) {
            Inventory inventory = player.getInventory();
            ItemStack heldItem = inventory.getItemInHand();

            if (heldItem != null && !heldItem.isEmpty()) {
                byte slot = inventory.getActiveHotbarSlot();
                ItemContainer container = inventory.getCombinedHotbarFirst();

                container.removeItemStackFromSlot(slot);

                if (heldItem.getQuantity() > 1) {
                    container.addItemStackToSlot(slot, heldItem.withQuantity(heldItem.getQuantity() - 1));
                }
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
