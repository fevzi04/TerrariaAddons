package de.fevzi.TerrariaAddons.items.Voidbag;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemStackContainerConfig;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ItemStackContainerWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemStackItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenItemStackContainerInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class Voidbag extends OpenItemStackContainerInteraction {

    public static final BuilderCodec<Voidbag> CODEC;

    static {
        CODEC = BuilderCodec.builder(Voidbag.class, Voidbag::new, OpenItemStackContainerInteraction.CODEC).build();
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        Ref<EntityStore> ref = context.getEntity();
        Store<EntityStore> store = ref.getStore();

        Player playerComponent = (Player) commandBuffer.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) return;

        PageManager pageManager = playerComponent.getPageManager();
        if (pageManager.getCustomPage() != null) return;

        ItemStack heldItem = context.getHeldItem();
        if (ItemStack.isEmpty(heldItem)) return;

        byte heldItemSlot = context.getHeldItemSlot();
        ItemContainer inventory = playerComponent.getInventory().getSectionById(context.getHeldItemSectionId());
        if (inventory == null) return;

        ItemStackContainerConfig config = heldItem.getItem().getItemStackContainerConfig();
        ItemStackItemContainer trashContainer = ItemStackItemContainer.ensureConfiguredContainer(inventory, (short) heldItemSlot, config);

        if (trashContainer != null) {

            trashContainer.clear();

            pageManager.setPageWithWindows(ref, store, Page.Bench, true, new Window[]{new ItemStackContainerWindow(trashContainer)});
        }
    }

}
