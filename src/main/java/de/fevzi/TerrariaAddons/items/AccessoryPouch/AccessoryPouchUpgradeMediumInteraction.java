package de.fevzi.TerrariaAddons.items.AccessoryPouch;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Interaction handler for the Medium Accessory Pouch upgrade item.
 * Increases the player's accessory pouch capacity from 2 to 4 slots.
 * Requires the player to have an accessory pouch in their inventory.
 */
public class AccessoryPouchUpgradeMediumInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<AccessoryPouchUpgradeMediumInteraction> CODEC;
    private static final short MEDIUM_CAPACITY = 4;

    private static final Message ALREADY_UPGRADED = Message.raw("Your accessory pouch is already upgraded.");
    private static final Message MISSING_POUCH = Message.raw("You need an Accessory Pouch to use this upgrade.");

    static {
        CODEC = BuilderCodec.builder(AccessoryPouchUpgradeMediumInteraction.class,
                AccessoryPouchUpgradeMediumInteraction::new,
                SimpleInstantInteraction.CODEC).build();
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type,
                            @Nonnull InteractionContext context,
                            @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        Ref<EntityStore> ref = context.getEntity();
        PlayerRef playerRef = (PlayerRef) ref.getStore().getComponent(ref, PlayerRef.getComponentType());
        Player player = (Player) commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }

        if (!AccessoryPouchSharedContainer.hasPouch(inventory)) {
            player.sendMessage(MISSING_POUCH);
            return;
        }

        if (AccessoryPouchSharedContainer.getCapacity(inventory, playerRef.getUuid()) >= MEDIUM_CAPACITY) {
            player.sendMessage(ALREADY_UPGRADED);
            return;
        }
        if (AccessoryPouchSharedContainer.upgradeContainer(inventory, playerRef.getUuid(), MEDIUM_CAPACITY)) {
            consumeUpgradeItem(context, inventory);
        }
    }

    private static void consumeUpgradeItem(InteractionContext context, Inventory inventory) {
        ItemStack heldItem = context.getHeldItem();
        if (heldItem == null || ItemStack.isEmpty(heldItem)) {
            return;
        }

        ItemContainer section = inventory.getSectionById(context.getHeldItemSectionId());
        if (section == null) {
            return;
        }

        short slot = context.getHeldItemSlot();
        section.removeItemStackFromSlot(slot);
        if (heldItem.getQuantity() > 1) {
            section.addItemStackToSlot(slot, heldItem.withQuantity(heldItem.getQuantity() - 1));
        }
    }

}
