package de.fevzi.TerrariaAddons.shop.coin;

import com.hypixel.hytale.builtin.adventure.shop.barter.BarterItemStack;
import com.hypixel.hytale.builtin.adventure.shop.barter.BarterShopAsset;
import com.hypixel.hytale.builtin.adventure.shop.barter.BarterShopState;
import com.hypixel.hytale.builtin.adventure.shop.barter.BarterTrade;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceBasePage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceElement;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import de.fevzi.TerrariaAddons.items.coinPouch.CoinPouchCurrency;
import de.fevzi.TerrariaAddons.items.coinPouch.CoinPouchSharedContainer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.UUID;

public class CoinShopPage extends ChoiceBasePage {
    private static final String UI_LAYOUT = "Pages/CoinShopPage.ui";
    private final String shopId;

    private CoinShopPage(@Nonnull PlayerRef playerRef,
                         @Nonnull ChoiceElement[] elements,
                         @Nonnull String shopId) {
        super(playerRef, elements, UI_LAYOUT);
        this.shopId = shopId;
    }

    public static CoinShopPage create(@Nonnull Store<EntityStore> store,
                                      @Nonnull Ref<EntityStore> playerEntityRef,
                                      @Nonnull PlayerRef playerRef,
                                      @Nonnull String shopId) {
        ObjectArrayList<ChoiceElement> elements = new ObjectArrayList<>();

        BarterShopAsset asset = (BarterShopAsset) BarterShopAsset.getAssetMap().getAsset(shopId);
        if (asset == null) {
            elements.add(new CoinShopElement(null, "Shop not found", "", "", "Unknown", 0, 0, 0, 0, 0, null));
            return new CoinShopPage(playerRef, elements.toArray(new ChoiceElement[0]), shopId);
        }

        WorldTimeResource worldTime = (WorldTimeResource) store.getResource(WorldTimeResource.getResourceType());
        Instant gameTime = worldTime != null ? worldTime.getGameTime() : Instant.now();

        BarterTrade[] trades = BarterShopState.get().getResolvedTrades(asset, gameTime);
        int[] stock = BarterShopState.get().getStockArray(asset, gameTime);

        for (int i = 0; i < trades.length; i++) {
            BarterTrade trade = trades[i];
            int priceCopper = getCoinPrice(trade.getInput());
            Message itemNameMessage = getItemNameMessage(trade.getOutput());
            String itemText = formatOutput(trade.getOutput());
            int stockLeft = (i < stock.length) ? stock[i] : -1;
            String stockText = stockLeft >= 0 ? ("Stock: " + stockLeft) : "Stock: Unlimited";

            if (priceCopper <= 0) {
                String outputItemId = trade.getOutput() != null ? trade.getOutput().getItemId() : null;
                int outputQty = trade.getOutput() != null ? trade.getOutput().getQuantity() : 0;
                elements.add(new CoinShopElement(itemNameMessage, itemText, "Non-coin price", stockText, outputItemId, outputQty, 0, 0, 0, 0, null));
                continue;
            }

            String priceText = CoinPouchCurrency.formatCopper(priceCopper);
            int[] breakdown = getPriceBreakdown(priceCopper);
            CoinShopPurchaseInteraction interaction = new CoinShopPurchaseInteraction(shopId, i);
            String outputItemId = trade.getOutput() != null ? trade.getOutput().getItemId() : null;
            int outputQty = trade.getOutput() != null ? trade.getOutput().getQuantity() : 0;
            elements.add(new CoinShopElement(
                    itemNameMessage,
                    itemText,
                    priceText,
                    stockText,
                    outputItemId,
                    outputQty,
                    breakdown[0],
                    breakdown[1],
                    breakdown[2],
                    breakdown[3],
                    interaction));
        }

        return new CoinShopPage(playerRef, elements.toArray(new ChoiceElement[0]), shopId);
    }

    public boolean isForShop(@Nonnull String shopId) {
        return this.shopId.equals(shopId);
    }

    public void refreshAfterPurchase(@Nonnull Ref<EntityStore> playerEntityRef,
                                     @Nonnull Store<EntityStore> store,
                                     int tradeIndex) {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        updateBalanceCounts(playerEntityRef, store, commandBuilder);
        updateStockText(store, tradeIndex, commandBuilder);
        sendUpdate(commandBuilder, false);
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

    private static String formatOutput(BarterItemStack output) {
        if (output == null) {
            return "Unknown item";
        }
        String itemId = output.getItemId();
        int qty = output.getQuantity();
        if (qty > 1) {
            return itemId + " x" + qty;
        }
        return itemId;
    }

    private static int[] getPriceBreakdown(int priceCopper) {
        if (priceCopper <= 0) {
            return new int[]{0, 0, 0, 0};
        }
        int remaining = priceCopper;
        int platinum = remaining / CoinPouchCurrency.PLATINUM_VALUE;
        remaining %= CoinPouchCurrency.PLATINUM_VALUE;
        int gold = remaining / CoinPouchCurrency.GOLD_VALUE;
        remaining %= CoinPouchCurrency.GOLD_VALUE;
        int silver = remaining / CoinPouchCurrency.SILVER_VALUE;
        remaining %= CoinPouchCurrency.SILVER_VALUE;
        int copper = remaining;
        return new int[]{copper, silver, gold, platinum};
    }

    private static Message getItemNameMessage(BarterItemStack output) {
        if (output == null) {
            return null;
        }
        String itemId = output.getItemId();
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }
        Item item = (Item) Item.getAssetMap().getAsset(itemId);
        String key = item != null ? item.getTranslationKey() : null;
        if (key == null || key.isEmpty()) {
            return null;
        }
        return Message.translation(key);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append(getPageLayout());
        commandBuilder.clear("#ItemList");
        commandBuilder.clear("#BalanceContent");
        buildBalanceBar(ref, store, commandBuilder);

        ChoiceElement[] elements = getElements();
        if (elements == null || elements.length == 0) {
            return;
        }

        for (int i = 0; i < elements.length; i++) {
            String selector = "#ItemList[" + i + "]";
            ChoiceElement element = elements[i];
            element.addButton(commandBuilder, eventBuilder, selector, playerRef);
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selector,
                    EventData.of("Index", Integer.toString(i)),
                    false);
        }
    }

    private void buildBalanceBar(@Nonnull Ref<EntityStore> playerEntityRef,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull UICommandBuilder commandBuilder) {
        Ref<EntityStore> resolvedRef = playerRef.getReference();
        if (resolvedRef == null) {
            resolvedRef = playerEntityRef;
        }
        Player playerComponent = (Player) store.getComponent(resolvedRef, Player.getComponentType());
        Inventory inventory = playerComponent != null ? playerComponent.getInventory() : null;
        UUID uuid = playerRef.getUuid();

        int[] counts = CoinPouchSharedContainer.getCoinCountsCombinedForDisplay(inventory, uuid);

        commandBuilder.append("#BalanceContent", "Pages/CoinShopBalanceElement.ui");
        String selector = "#BalanceContent[0]";
        commandBuilder.set(selector + " #CoinCopper.ItemId", CoinPouchCurrency.COPPER_COIN_ID);
        commandBuilder.set(selector + " #CoinSilver.ItemId", CoinPouchCurrency.SILVER_COIN_ID);
        commandBuilder.set(selector + " #CoinGold.ItemId", CoinPouchCurrency.GOLD_COIN_ID);
        commandBuilder.set(selector + " #CoinPlatinum.ItemId", CoinPouchCurrency.PLATINUM_COIN_ID);
        commandBuilder.set(selector + " #CoinCopperCount.Text", String.valueOf(counts[0]));
        commandBuilder.set(selector + " #CoinSilverCount.Text", String.valueOf(counts[1]));
        commandBuilder.set(selector + " #CoinGoldCount.Text", String.valueOf(counts[2]));
        commandBuilder.set(selector + " #CoinPlatinumCount.Text", String.valueOf(counts[3]));
    }

    private void updateBalanceCounts(@Nonnull Ref<EntityStore> playerEntityRef,
                                     @Nonnull Store<EntityStore> store,
                                     @Nonnull UICommandBuilder commandBuilder) {
        Ref<EntityStore> resolvedRef = playerRef.getReference();
        if (resolvedRef == null) {
            resolvedRef = playerEntityRef;
        }
        Player playerComponent = (Player) store.getComponent(resolvedRef, Player.getComponentType());
        Inventory inventory = playerComponent != null ? playerComponent.getInventory() : null;
        UUID uuid = playerRef.getUuid();

        int[] counts = CoinPouchSharedContainer.getCoinCountsCombinedForDisplay(inventory, uuid);
        String selector = "#BalanceContent[0]";
        commandBuilder.set(selector + " #CoinCopperCount.Text", String.valueOf(counts[0]));
        commandBuilder.set(selector + " #CoinSilverCount.Text", String.valueOf(counts[1]));
        commandBuilder.set(selector + " #CoinGoldCount.Text", String.valueOf(counts[2]));
        commandBuilder.set(selector + " #CoinPlatinumCount.Text", String.valueOf(counts[3]));
    }

    private void updateStockText(@Nonnull Store<EntityStore> store,
                                 int tradeIndex,
                                 @Nonnull UICommandBuilder commandBuilder) {
        if (tradeIndex < 0) {
            return;
        }
        BarterShopAsset asset = (BarterShopAsset) BarterShopAsset.getAssetMap().getAsset(shopId);
        if (asset == null) {
            return;
        }

        WorldTimeResource worldTime = (WorldTimeResource) store.getResource(WorldTimeResource.getResourceType());
        Instant gameTime = worldTime != null ? worldTime.getGameTime() : Instant.now();
        int[] stock = BarterShopState.get().getStockArray(asset, gameTime);
        int stockLeft = (tradeIndex < stock.length) ? stock[tradeIndex] : -1;
        String stockText = stockLeft >= 0 ? ("Stock: " + stockLeft) : "Stock: Unlimited";
        commandBuilder.set("#ItemList[" + tradeIndex + "] #Stock.Text", stockText);
    }

}
