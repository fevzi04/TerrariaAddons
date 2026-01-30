package de.fevzi.TerrariaAddons.items.coinPouch;

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

/**
 * Interaction handler for the Coin Pouch item.
 * Opens a container window where players can store coins.
 * Only coin items can be placed in the pouch.
 * Coins in the pouch are automatically converted when reaching conversion thresholds.
 */
public class CoinPouch extends OpenItemStackContainerInteraction {
    private static final String COPPER_COIN_ID = "Ingredient_Coin_Copper";
    private static final String SILVER_COIN_ID = "Ingredient_Coin_Silver";
    private static final String GOLD_COIN_ID = "Ingredient_Coin_Gold";
    private static final String PLATINUM_COIN_ID = "Ingredient_Coin_Platinum";
    
    private static final SlotFilter COIN_ONLY_FILTER = (action, container, slot, stack) -> {
        if (action == FilterActionType.ADD) {
            if (stack == null || ItemStack.isEmpty(stack)) {
                return true;
            }
            return isCoin(stack);
        }
        return true;
    };

    public static final BuilderCodec<CoinPouch> CODEC;

    static {
        CODEC = BuilderCodec.builder(CoinPouch.class, CoinPouch::new, OpenItemStackContainerInteraction.CODEC)
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
            ItemContainer pouchContainer = CoinPouchSharedContainer.getOrCreateContainer(
                    playerComponent.getInventory(),
                    playerRef.getUuid()
            );
            if (pouchContainer == null) {
                return;
            }
            applyCoinFilter(pouchContainer);
            pageManager.setPageWithWindows(ref, store, Page.Bench, true,
                    new Window[]{new ContainerWindow(pouchContainer)});
        }
    }

    static void applyCoinFilter(ItemContainer pouchContainer) {
        short capacity = pouchContainer.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            pouchContainer.setSlotFilter(FilterActionType.ADD, slot, COIN_ONLY_FILTER);
        }
    }

    private static boolean isCoin(ItemStack stack) {
        if (stack == null || ItemStack.isEmpty(stack)) {
            return false;
        }

        String itemId = stack.getItemId();
        return COPPER_COIN_ID.equals(itemId) ||
               SILVER_COIN_ID.equals(itemId) ||
               GOLD_COIN_ID.equals(itemId) ||
               PLATINUM_COIN_ID.equals(itemId);
    }
}
