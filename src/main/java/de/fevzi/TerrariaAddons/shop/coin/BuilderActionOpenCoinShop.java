package de.fevzi.TerrariaAddons.shop.coin;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringNotEmptyValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;

import javax.annotation.Nonnull;

public class BuilderActionOpenCoinShop extends BuilderActionBase {
    private final StringHolder shopId = new StringHolder();

    @Nonnull
    @Override
    public Builder<Action> readConfig(@Nonnull JsonElement data) {
        requireString(data, "Shop", shopId, StringNotEmptyValidator.get(),
                BuilderDescriptorState.Stable, "Barter shop id to open", null);
        return this;
    }

    @Override
    public Action build(@Nonnull BuilderSupport support) {
        return new ActionOpenCoinShop(this, support);
    }

    public String getShopId(@Nonnull BuilderSupport support) {
        return shopId.get(support.getExecutionContext());
    }

    @Override
    public String getShortDescription() {
        return "Open a coin-backed shop";
    }

    @Override
    public String getLongDescription() {
        return "Opens a custom coin-backed shop page that reads BarterShop trades.";
    }

    @Override
    public BuilderDescriptorState getBuilderDescriptorState() {
        return BuilderDescriptorState.Stable;
    }
}
