package de.fevzi.TerrariaAddons;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.registry.CodecMapRegistry.Assets;
import com.hypixel.hytale.server.npc.NPCPlugin;
import java.util.logging.Level;
import de.fevzi.TerrariaAddons.config.TerrariaAddonsConfig;
import de.fevzi.TerrariaAddons.items.accessories.dpsMeter.DpsMeterSystem;
import de.fevzi.TerrariaAddons.items.accessoryPouch.AccessoryPouch;
import de.fevzi.TerrariaAddons.items.accessoryPouch.AccessoryPouchUpgradeLargeInteraction;
import de.fevzi.TerrariaAddons.items.accessoryPouch.AccessoryPouchUpgradeMediumInteraction;
import de.fevzi.TerrariaAddons.items.accessories.balloon.BalloonSystem;
import de.fevzi.TerrariaAddons.items.accessories.bandOfManaRegeneration.BandOfManaRegenerationSystem;
import de.fevzi.TerrariaAddons.items.accessories.bandOfRegeneration.BandOfRegenerationSystem;
import de.fevzi.TerrariaAddons.items.accessories.blizzardInABottle.BlizzardInABottle;
import de.fevzi.TerrariaAddons.items.accessories.cloudInABottle.CloudInABottle;
import de.fevzi.TerrariaAddons.coinDropSystem.CoinDropSystem;
import de.fevzi.TerrariaAddons.items.coinPouch.CoinPouch;
import de.fevzi.TerrariaAddons.fallingStarSystem.FallingStarSystem;
import de.fevzi.TerrariaAddons.items.accessories.speed.SpeedBoostSystem;
import de.fevzi.TerrariaAddons.items.accessories.rocketBoots.RocketBootsSystem;
import de.fevzi.TerrariaAddons.items.accessories.lavaCharm.LavaCharmHudSystem;
import de.fevzi.TerrariaAddons.items.accessories.lavaCharm.LavaCharmSystem;
import de.fevzi.TerrariaAddons.items.consumables.lifeCrystal.LifeCrystalInteraction;
import de.fevzi.TerrariaAddons.items.consumables.lifeCrystal.LifeCrystalRemoverInteraction;
import de.fevzi.TerrariaAddons.items.accessories.luckyHorseshoe.LuckyHorseshoeSystem;
import de.fevzi.TerrariaAddons.items.accessories.magmaStone.MagmaStoneSystem;
import de.fevzi.TerrariaAddons.items.consumables.manaCrystal.ManaCrystalInteraction;
import de.fevzi.TerrariaAddons.items.consumables.manaCrystal.ManaCrystalRemoverInteraction;
import de.fevzi.TerrariaAddons.items.accessories.sandstormInABottle.SandstormInABottle;
import de.fevzi.TerrariaAddons.items.magicmirror.MirrorRecallInteraction;
import de.fevzi.TerrariaAddons.items.consumables.potions.recall.RecallInteraction;
import de.fevzi.TerrariaAddons.items.consumables.potions.teleportation.TeleportationPotionInteraction;
import de.fevzi.TerrariaAddons.items.consumables.potions.wormhole.PlayerTeleportPageSupplier;
import de.fevzi.TerrariaAddons.items.voidbag.Voidbag;
import de.fevzi.TerrariaAddons.items.weapons.starfury.StarfuryInteraction;
import de.fevzi.TerrariaAddons.items.weapons.starwrath.StarWrathInteraction;
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
import de.fevzi.TerrariaAddons.npc.NPCMapMarkerProvider;
import de.fevzi.TerrariaAddons.npc.commands.NPCKillCommand;
import de.fevzi.TerrariaAddons.shop.coin.BuilderActionOpenCoinShop;

import javax.annotation.Nonnull;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;

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

        TerrariaAddonsConfig.getInstance();

        Assets<Interaction, ? extends Codec<? extends Interaction>> interactions = this.getCodecRegistry(Interaction.CODEC);
        OpenCustomUIInteraction.PAGE_CODEC.register("PlayerTeleport", PlayerTeleportPageSupplier.class, PlayerTeleportPageSupplier.CODEC);
        OpenCustomUIInteraction.PAGE_CODEC.register("HousingCheck", HousingCheckPageSupplier.class, HousingCheckPageSupplier.CODEC);
        interactions.register("Recall", RecallInteraction.class, RecallInteraction.CODEC);
        interactions.register("MirrorRecall", MirrorRecallInteraction.class, MirrorRecallInteraction.CODEC);
        interactions.register("TeleportationPotion", TeleportationPotionInteraction.class, TeleportationPotionInteraction.CODEC);
        interactions.register("LifeCrystal", LifeCrystalInteraction.class, LifeCrystalInteraction.CODEC);
        interactions.register("ManaCrystal", ManaCrystalInteraction.class, ManaCrystalInteraction.CODEC);
        interactions.register("ManaCrystalRemover", ManaCrystalRemoverInteraction.class, ManaCrystalRemoverInteraction.CODEC);
        interactions.register("LifeCrystalRemover", LifeCrystalRemoverInteraction.class, LifeCrystalRemoverInteraction.CODEC);
        interactions.register("AccessoryPouch", AccessoryPouch.class, AccessoryPouch.CODEC);
        interactions.register("AccessoryPouchUpgradeMedium", AccessoryPouchUpgradeMediumInteraction.class, AccessoryPouchUpgradeMediumInteraction.CODEC);
        interactions.register("AccessoryPouchUpgradeLarge", AccessoryPouchUpgradeLargeInteraction.class, AccessoryPouchUpgradeLargeInteraction.CODEC);
        interactions.register("CoinPouch", CoinPouch.class, CoinPouch.CODEC);
        interactions.register("Voidbag", Voidbag.class, Voidbag.CODEC);
        interactions.register("Starfury", StarfuryInteraction.class, StarfuryInteraction.CODEC);
        interactions.register("StarWrath", StarWrathInteraction.class, StarWrathInteraction.CODEC);
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin != null) {
            npcPlugin.registerCoreComponentType("OpenCoinShop", BuilderActionOpenCoinShop::new);
        } else {
            getLogger().at(Level.WARNING).log("NPCPlugin not ready; OpenCoinShop not registered");
        }
        getEntityStoreRegistry().registerSystem(new DpsMeterSystem());
        getEntityStoreRegistry().registerSystem(new SpeedBoostSystem());
        getEntityStoreRegistry().registerSystem(new LuckyHorseshoeSystem());
        getEntityStoreRegistry().registerSystem(new BalloonSystem());
        getEntityStoreRegistry().registerSystem(new CloudInABottle());
        getEntityStoreRegistry().registerSystem(new BlizzardInABottle());
        getEntityStoreRegistry().registerSystem(new SandstormInABottle());
        getEntityStoreRegistry().registerSystem(new RocketBootsSystem());
        getEntityStoreRegistry().registerSystem(new BandOfRegenerationSystem());
        getEntityStoreRegistry().registerSystem(new MagmaStoneSystem());
        getEntityStoreRegistry().registerSystem(new LavaCharmSystem());
        getEntityStoreRegistry().registerSystem(new LavaCharmHudSystem());
        getEntityStoreRegistry().registerSystem(new FallingStarSystem());
        getEntityStoreRegistry().registerSystem(new BandOfManaRegenerationSystem());
        getEntityStoreRegistry().registerSystem(new CoinDropSystem());
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
        CommandManager.get().register(new NPCKillCommand());

        getEventRegistry().registerGlobal(AddWorldEvent.class, event ->
            event.getWorld().getWorldMapManager().addMarkerProvider("npcMarkers", NPCMapMarkerProvider.INSTANCE)
        );

    }
}
