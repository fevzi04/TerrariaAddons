package de.fevzi.TerrariaAddons.shop.coin;

import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceElement;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceInteraction;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public class CoinShopElement extends ChoiceElement {
    private final Message itemNameMessage;
    private final String itemNameText;
    private final String priceText;
    private final String stockText;
    private final String itemId;
    private final int itemQuantity;
    private final int priceCopper;
    private final int priceSilver;
    private final int priceGold;
    private final int pricePlatinum;

    public CoinShopElement(Message itemNameMessage,
                           @Nonnull String itemNameText,
                           @Nonnull String priceText,
                           @Nonnull String stockText,
                           @Nonnull String itemId,
                           int itemQuantity,
                           int priceCopper,
                           int priceSilver,
                           int priceGold,
                           int pricePlatinum,
                           ChoiceInteraction interaction) {
        this.itemNameMessage = itemNameMessage;
        this.itemNameText = itemNameText;
        this.priceText = priceText;
        this.stockText = stockText;
        this.itemId = itemId;
        this.itemQuantity = itemQuantity;
        this.priceCopper = priceCopper;
        this.priceSilver = priceSilver;
        this.priceGold = priceGold;
        this.pricePlatinum = pricePlatinum;
        if (interaction != null) {
            this.interactions = new ChoiceInteraction[]{interaction};
        } else {
            this.interactions = new ChoiceInteraction[0];
        }
    }

    @Override
    public void addButton(@Nonnull UICommandBuilder commandBuilder,
                          UIEventBuilder eventBuilder,
                          String selector,
                          PlayerRef playerRef) {
        commandBuilder.append("#ItemList", "Pages/CoinShopElement.ui");
        commandBuilder.set(selector + " #ItemIcon.ItemId", getSafeItemId(itemId));
        commandBuilder.set(selector + " #ItemQuantity.Text", itemQuantity > 1 ? ("x" + itemQuantity) : "");
        if (itemNameMessage != null) {
            commandBuilder.set(selector + " #ItemName.Text", itemNameMessage);
        } else {
            commandBuilder.set(selector + " #ItemName.Text", itemNameText);
        }
        commandBuilder.set(selector + " #PriceCopperIcon.ItemId", "Ingredient_Coin_Copper");
        commandBuilder.set(selector + " #PriceSilverIcon.ItemId", "Ingredient_Coin_Silver");
        commandBuilder.set(selector + " #PriceGoldIcon.ItemId", "Ingredient_Coin_Gold");
        commandBuilder.set(selector + " #PricePlatinumIcon.ItemId", "Ingredient_Coin_Platinum");
        commandBuilder.set(selector + " #PriceCopperText.Text", String.valueOf(priceCopper));
        commandBuilder.set(selector + " #PriceSilverText.Text", String.valueOf(priceSilver));
        commandBuilder.set(selector + " #PriceGoldText.Text", String.valueOf(priceGold));
        commandBuilder.set(selector + " #PricePlatinumText.Text", String.valueOf(pricePlatinum));
        commandBuilder.set(selector + " #Stock.Text", stockText);
    }

    private static String getSafeItemId(String rawItemId) {
        if (rawItemId == null || rawItemId.isBlank()) {
            return "Unknown";
        }
        return ItemModule.exists(rawItemId) ? rawItemId : "Unknown";
    }
}
