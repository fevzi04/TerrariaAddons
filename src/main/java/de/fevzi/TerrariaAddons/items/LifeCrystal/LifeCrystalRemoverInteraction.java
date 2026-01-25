package de.fevzi.TerrariaAddons.items.LifeCrystal;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import de.fevzi.TerrariaAddons.data.PlayerData;
import de.fevzi.TerrariaAddons.data.PlayerDataManager;

import javax.annotation.Nonnull;
import java.util.UUID;

public class LifeCrystalRemoverInteraction extends SimpleInstantInteraction {
    private static final float HEALTH_DECREASE = 5.0f;

    public static final BuilderCodec<LifeCrystalRemoverInteraction> CODEC = BuilderCodec.builder(LifeCrystalRemoverInteraction.class, LifeCrystalRemoverInteraction::new, SimpleInstantInteraction.CODEC).build();

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldown) {
        Ref<EntityStore> ref = context.getEntity();
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();

        Player player = (Player) commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        PlayerRef playerRef = (PlayerRef) commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        UUID playerUUID = playerRef.getUuid();
        PlayerData playerData = PlayerDataManager.getPlayerData(playerUUID);
        int currentUses = playerData.getLifeCrystalUses();

        if (currentUses <= 0) {
            return;
        }

        EntityStatMap stats = (EntityStatMap) commandBuffer.getComponent(ref, EntityStatMap.getComponentType());
        if (stats != null) {
            stats.removeModifier(DefaultEntityStatTypes.getHealth(), "life_crystal_" + (currentUses - 1));
        }

        playerData.setLifeCrystalUses(currentUses - 1);
        PlayerDataManager.savePlayerData(playerUUID, playerData);

        Inventory inventory = player.getInventory();
        if (inventory != null) {
            ItemStack heldItem = context.getHeldItem();
            if (heldItem != null && !heldItem.isEmpty()) {
                byte slot = context.getHeldItemSlot();
                ItemContainer container = inventory.getCombinedHotbarFirst();

                container.removeItemStackFromSlot(slot);

                if (heldItem.getQuantity() > 1) {
                    ItemStack remainingStack = heldItem.withQuantity(heldItem.getQuantity() - 1);
                    container.addItemStackToSlot(slot, remainingStack);
                }
            }
        }
    }
}