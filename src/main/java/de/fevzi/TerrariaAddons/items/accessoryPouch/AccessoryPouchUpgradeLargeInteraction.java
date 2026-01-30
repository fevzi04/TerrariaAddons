package de.fevzi.TerrariaAddons.items.accessoryPouch;

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
 * Interaction handler for the Large Accessory Pouch upgrade item.
 * Increases the player's accessory pouch capacity from 4 to 6 slots.
 * Requires the player to have a Medium Accessory Pouch first.
 */
public class AccessoryPouchUpgradeLargeInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<AccessoryPouchUpgradeLargeInteraction> CODEC;
    private static final short LARGE_CAPACITY = 6;

    private static final Message ALREADY_UPGRADED = Message.raw("Your accessory pouch is already upgraded.");
    private static final Message MISSING_POUCH = Message.raw("You need an Accessory Pouch to use this upgrade.");
    private static final Message REQUIRE_MEDIUM = Message.raw("Upgrade to a Medium Accessory Pouch first.");

    static {
        CODEC = BuilderCodec.builder(AccessoryPouchUpgradeLargeInteraction.class,
                AccessoryPouchUpgradeLargeInteraction::new,
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

        short capacity = AccessoryPouchSharedContainer.getCapacity(inventory, playerRef.getUuid());
        if (capacity >= LARGE_CAPACITY) {
            player.sendMessage(ALREADY_UPGRADED);
            return;
        }

        if (capacity < 4) {
            player.sendMessage(REQUIRE_MEDIUM);
            return;
        }

        if (AccessoryPouchSharedContainer.upgradeContainer(inventory, playerRef.getUuid(), LARGE_CAPACITY)) {
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
