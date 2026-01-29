package de.fevzi.TerrariaAddons.shop.coin;

import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceElement;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public class CoinShopBalanceElement extends ChoiceElement {
    private final int copper;
    private final int silver;
    private final int gold;
    private final int platinum;
    private final String copperItemId;
    private final String silverItemId;
    private final String goldItemId;
    private final String platinumItemId;

    public CoinShopBalanceElement(int copper, int silver, int gold, int platinum,
                                  @Nonnull String copperItemId,
                                  @Nonnull String silverItemId,
                                  @Nonnull String goldItemId,
                                  @Nonnull String platinumItemId) {
        this.copper = copper;
        this.silver = silver;
        this.gold = gold;
        this.platinum = platinum;
        this.copperItemId = copperItemId;
        this.silverItemId = silverItemId;
        this.goldItemId = goldItemId;
        this.platinumItemId = platinumItemId;
        this.interactions = new com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceInteraction[0];
    }

    @Override
    public void addButton(@Nonnull UICommandBuilder commandBuilder,
                          UIEventBuilder eventBuilder,
                          String selector,
                          PlayerRef playerRef) {
        commandBuilder.append("#ElementList", "Pages/CoinShopBalanceElement.ui");
        commandBuilder.set(selector + " #CoinCopper.ItemId", copperItemId);
        commandBuilder.set(selector + " #CoinSilver.ItemId", silverItemId);
        commandBuilder.set(selector + " #CoinGold.ItemId", goldItemId);
        commandBuilder.set(selector + " #CoinPlatinum.ItemId", platinumItemId);
        commandBuilder.set(selector + " #CoinCopperCount.Text", String.valueOf(copper));
        commandBuilder.set(selector + " #CoinSilverCount.Text", String.valueOf(silver));
        commandBuilder.set(selector + " #CoinGoldCount.Text", String.valueOf(gold));
        commandBuilder.set(selector + " #CoinPlatinumCount.Text", String.valueOf(platinum));
    }
}
