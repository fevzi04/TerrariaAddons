package de.fevzi.TerrariaAddons.items.AccessoryPouch;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterActionType;
import com.hypixel.hytale.server.core.inventory.container.filter.SlotFilter;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenItemStackContainerInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class AccessoryPouch extends OpenItemStackContainerInteraction {
    private static final String ACCESSORY_CATEGORY = "Items.Accessories";
    private static final SlotFilter ACCESSORY_ONLY_FILTER = (action, container, slot, stack) -> {
        if (action == FilterActionType.ADD) {
            return isAccessory(stack);
        }
        return true;
    };

    public static final BuilderCodec<AccessoryPouch> CODEC;

    static {
        CODEC = BuilderCodec.builder(AccessoryPouch.class, AccessoryPouch::new, OpenItemStackContainerInteraction.CODEC)
                .build();
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type,
                            @Nonnull InteractionContext context,
                            @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        Ref<EntityStore> ref = context.getEntity();
        Store<EntityStore> store = ref.getStore();

        Player playerComponent = (Player) commandBuffer.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }
        PlayerRef playerRef = (PlayerRef) store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        PageManager pageManager = playerComponent.getPageManager();
        if (pageManager.getCustomPage() != null) {
            return;
        }

        ItemStack heldItem = context.getHeldItem();
        if (ItemStack.isEmpty(heldItem)) {
            return;
        }

        byte heldItemSlot = context.getHeldItemSlot();
        if (heldItemSlot >= 0) {
            ItemContainer pouchContainer = AccessoryPouchSharedContainer.getOrCreateContainer(
                    playerComponent.getInventory(),
                    playerRef.getUuid()
            );
            if (pouchContainer == null) {
                return;
            }
            applyAccessoryFilter(pouchContainer);
            pageManager.setPageWithWindows(ref, store, Page.Bench, true,
                    new Window[]{new ContainerWindow(pouchContainer)});
        }
    }

    static void applyAccessoryFilter(ItemContainer pouchContainer) {
        short capacity = pouchContainer.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            pouchContainer.setSlotFilter(FilterActionType.ADD, slot, ACCESSORY_ONLY_FILTER);
        }
    }

    private static boolean isAccessory(ItemStack stack) {
        if (stack == null || ItemStack.isEmpty(stack)) {
            return false;
        }

        String[] categories = stack.getItem().getCategories();
        if (categories == null) {
            return false;
        }

        for (String category : categories) {
            if (ACCESSORY_CATEGORY.equals(category)) {
                return true;
            }
        }

        return false;
    }
}
