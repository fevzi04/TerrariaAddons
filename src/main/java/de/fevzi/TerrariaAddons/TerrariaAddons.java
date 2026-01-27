package de.fevzi.TerrariaAddons;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.registry.CodecMapRegistry.Assets;
import de.fevzi.TerrariaAddons.dps.DpsMeterSystem;
import de.fevzi.TerrariaAddons.items.AccessoryPouch.AccessoryPouch;
import de.fevzi.TerrariaAddons.items.AccessoryPouch.AccessoryPouchUpgradeLargeInteraction;
import de.fevzi.TerrariaAddons.items.AccessoryPouch.AccessoryPouchUpgradeMediumInteraction;
import de.fevzi.TerrariaAddons.items.Balloon.BalloonSystem;
import de.fevzi.TerrariaAddons.items.BandOfManaRegeneration.BandOfManaRegenerationSystem;
import de.fevzi.TerrariaAddons.items.BandOfRegeneration.BandOfRegenerationSystem;
import de.fevzi.TerrariaAddons.items.BlizzardInABottle.BlizzardInABottle;
import de.fevzi.TerrariaAddons.items.CloudInABottle.CloudInABottle;
import de.fevzi.TerrariaAddons.items.FallingStarSystem.FallingStarSystem;
import de.fevzi.TerrariaAddons.items.HermesBoots.HermesBootsSystem;
import de.fevzi.TerrariaAddons.items.LavaCharm.LavaCharmHudSystem;
import de.fevzi.TerrariaAddons.items.LavaCharm.LavaCharmSystem;
import de.fevzi.TerrariaAddons.items.LifeCrystal.LifeCrystalInteraction;
import de.fevzi.TerrariaAddons.items.LifeCrystal.LifeCrystalRemoverInteraction;
import de.fevzi.TerrariaAddons.items.LuckyHorseshoe.LuckyHorseshoeSystem;
import de.fevzi.TerrariaAddons.items.MagmaStone.MagmaStoneSystem;
import de.fevzi.TerrariaAddons.items.ManaCrystal.ManaCrystalInteraction;
import de.fevzi.TerrariaAddons.items.ManaCrystal.ManaCrystalRemoverInteraction;
import de.fevzi.TerrariaAddons.items.SandstormInABottle.SandstormInABottle;
import de.fevzi.TerrariaAddons.items.magicmirror.MirrorRecallInteraction;
import de.fevzi.TerrariaAddons.items.potions.recall.RecallInteraction;
import de.fevzi.TerrariaAddons.items.potions.wormhole.PlayerTeleportPageSupplier;
import de.fevzi.TerrariaAddons.items.Voidbag.Voidbag;
import de.fevzi.TerrariaAddons.items.Starfury.StarfuryInteraction;
import de.fevzi.TerrariaAddons.housing.HousingCheckerSystem;
import de.fevzi.TerrariaAddons.housing.HousingRevalidationBreakSystem;
import de.fevzi.TerrariaAddons.housing.HousingRevalidationPlaceSystem;
import de.fevzi.TerrariaAddons.housing.HousingRevalidationTickSystem;
import de.fevzi.TerrariaAddons.housing.commands.ValidHousingCommand;
import de.fevzi.TerrariaAddons.housing.HousingRegistrySystem;
import de.fevzi.TerrariaAddons.npc.commands.NPCDebugCommand;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import de.fevzi.TerrariaAddons.housing.ui.HousingCheckPageSupplier;
import de.fevzi.TerrariaAddons.npc.NPCBehaviorSystem;
import de.fevzi.TerrariaAddons.npc.NPCDeathMonitorSystem;
import de.fevzi.TerrariaAddons.npc.NPCSpawnSystem;

import javax.annotation.Nonnull;

/**
 * Main plugin class for TerrariaAddons.
 * Registers all custom interactions, systems, and commands.
 */
public class TerrariaAddons extends JavaPlugin {
    public TerrariaAddons(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {

        Assets<Interaction, ? extends Codec<? extends Interaction>> interactions = this.getCodecRegistry(Interaction.CODEC);
        OpenCustomUIInteraction.PAGE_CODEC.register("PlayerTeleport", PlayerTeleportPageSupplier.class, PlayerTeleportPageSupplier.CODEC);
        OpenCustomUIInteraction.PAGE_CODEC.register("HousingCheck", HousingCheckPageSupplier.class, HousingCheckPageSupplier.CODEC);
        interactions.register("Recall", RecallInteraction.class, RecallInteraction.CODEC);
        interactions.register("MirrorRecall", MirrorRecallInteraction.class, MirrorRecallInteraction.CODEC);
        interactions.register("LifeCrystal", LifeCrystalInteraction.class, LifeCrystalInteraction.CODEC);
        interactions.register("ManaCrystal", ManaCrystalInteraction.class, ManaCrystalInteraction.CODEC);
        interactions.register("ManaCrystalRemover", ManaCrystalRemoverInteraction.class, ManaCrystalRemoverInteraction.CODEC);
        interactions.register("LifeCrystalRemover", LifeCrystalRemoverInteraction.class, LifeCrystalRemoverInteraction.CODEC);
        interactions.register("AccessoryPouch", AccessoryPouch.class, AccessoryPouch.CODEC);
        interactions.register("AccessoryPouchUpgradeMedium", AccessoryPouchUpgradeMediumInteraction.class, AccessoryPouchUpgradeMediumInteraction.CODEC);
        interactions.register("AccessoryPouchUpgradeLarge", AccessoryPouchUpgradeLargeInteraction.class, AccessoryPouchUpgradeLargeInteraction.CODEC);
        interactions.register("Voidbag", Voidbag.class, Voidbag.CODEC);
        interactions.register("Starfury", StarfuryInteraction.class, StarfuryInteraction.CODEC);
        getEntityStoreRegistry().registerSystem(new DpsMeterSystem());
        getEntityStoreRegistry().registerSystem(new HermesBootsSystem());
        getEntityStoreRegistry().registerSystem(new LuckyHorseshoeSystem());
        getEntityStoreRegistry().registerSystem(new BalloonSystem());
        getEntityStoreRegistry().registerSystem(new CloudInABottle());
        getEntityStoreRegistry().registerSystem(new BlizzardInABottle());
        getEntityStoreRegistry().registerSystem(new SandstormInABottle());
        getEntityStoreRegistry().registerSystem(new BandOfRegenerationSystem());
        getEntityStoreRegistry().registerSystem(new MagmaStoneSystem());
        getEntityStoreRegistry().registerSystem(new LavaCharmSystem());
        getEntityStoreRegistry().registerSystem(new LavaCharmHudSystem());
        getEntityStoreRegistry().registerSystem(new FallingStarSystem());
        getEntityStoreRegistry().registerSystem(new BandOfManaRegenerationSystem());
        getEntityStoreRegistry().registerSystem(new HousingCheckerSystem());
        getEntityStoreRegistry().registerSystem(new HousingRegistrySystem());
        getEntityStoreRegistry().registerSystem(new HousingRevalidationPlaceSystem());
        getEntityStoreRegistry().registerSystem(new HousingRevalidationBreakSystem());
        getEntityStoreRegistry().registerSystem(new HousingRevalidationTickSystem());
        getEntityStoreRegistry().registerSystem(new NPCSpawnSystem());
        getEntityStoreRegistry().registerSystem(new NPCDeathMonitorSystem());
        getEntityStoreRegistry().registerSystem(new NPCBehaviorSystem());

        CommandManager.get().register(new NPCDebugCommand());
        CommandManager.get().register(new ValidHousingCommand());

    }
}
