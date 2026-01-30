package de.fevzi.TerrariaAddons.shop.coin;

import com.hypixel.hytale.builtin.adventure.shop.barter.BarterItemStack;
import com.hypixel.hytale.builtin.adventure.shop.barter.BarterShopAsset;
import com.hypixel.hytale.builtin.adventure.shop.barter.BarterShopState;
import com.hypixel.hytale.builtin.adventure.shop.barter.BarterTrade;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceInteraction;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.fevzi.TerrariaAddons.items.CoinPouch.CoinPouchCurrency;
import de.fevzi.TerrariaAddons.items.CoinPouch.CoinPouchSharedContainer;

import javax.annotation.Nonnull;
import java.time.Instant;

public class CoinShopPurchaseInteraction extends ChoiceInteraction {
    private final String shopId;
    private final int tradeIndex;

    public CoinShopPurchaseInteraction(@Nonnull String shopId, int tradeIndex) {
        this.shopId = shopId;
        this.tradeIndex = tradeIndex;
    }

    @Override
    public void run(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef) {
        Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }

        Inventory inventory = playerComponent.getInventory();
        if (inventory == null) {
            playerRef.sendMessage(Message.translation("Inventory not available."));
            return;
        }

        BarterShopAsset asset = (BarterShopAsset) BarterShopAsset.getAssetMap().getAsset(shopId);
        if (asset == null) {
            playerRef.sendMessage(Message.translation("Shop not found."));
            return;
        }

        WorldTimeResource worldTime = (WorldTimeResource) store.getResource(WorldTimeResource.getResourceType());
        Instant gameTime = worldTime != null ? worldTime.getGameTime() : Instant.now();

        BarterTrade[] trades = BarterShopState.get().getResolvedTrades(asset, gameTime);
        if (tradeIndex < 0 || tradeIndex >= trades.length) {
            playerRef.sendMessage(Message.translation("Trade unavailable."));
            return;
        }

        BarterTrade trade = trades[tradeIndex];
        int costCopper = getCoinPrice(trade.getInput());
        if (costCopper <= 0) {
            playerRef.sendMessage(Message.translation("This trade does not accept coins."));
            return;
        }

        BarterItemStack output = trade.getOutput();
        if (output != null) {
            ItemStack stack = new ItemStack(output.getItemId(), output.getQuantity());
            int totalCoins = CoinPouchSharedContainer.getTotalCoinsInCopperCombined(inventory, playerRef.getUuid());
            if (totalCoins < costCopper) {
                playerRef.sendMessage(Message.translation("Not enough coins."));
                return;
            }

            ItemStackTransaction addTx = inventory.getCombinedHotbarFirst().addItemStack(stack, true, false, false);
            if (!addTx.succeeded()) {
                playerRef.sendMessage(Message.translation("Inventory full."));
                return;
            }

            if (!CoinPouchSharedContainer.trySpendCoinsPreferInventory(inventory, playerRef.getUuid(), costCopper)) {
                inventory.getCombinedHotbarFirst().removeItemStack(stack, true, false);
                playerRef.sendMessage(Message.translation("Not enough coins."));
                return;
            }
        }


        CustomUIPage currentPage = playerComponent.getPageManager().getCustomPage();
        if (currentPage instanceof CoinShopPage coinShopPage && coinShopPage.isForShop(shopId)) {
            coinShopPage.refreshAfterPurchase(ref, store, tradeIndex);
        } else {
            playerComponent.getPageManager().openCustomPage(
                    ref,
                    store,
                    CoinShopPage.create(store, ref, playerRef, shopId));
        }
    }

    private static int getCoinPrice(BarterItemStack[] input) {
        if (input == null || input.length == 0) {
            return 0;
        }
        int total = 0;
        for (BarterItemStack stack : input) {
            if (stack == null) {
                continue;
            }
            String itemId = stack.getItemId();
            if (!CoinPouchCurrency.isCoin(itemId)) {
                return -1;
            }
            total += CoinPouchCurrency.getValueInCopper(itemId, stack.getQuantity());
        }
        return total;
    }
}
