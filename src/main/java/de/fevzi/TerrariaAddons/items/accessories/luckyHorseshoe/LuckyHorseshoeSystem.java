package de.fevzi.TerrariaAddons.items.accessories.luckyHorseshoe;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.fevzi.TerrariaAddons.items.accessoryPouch.AccessoryPouchSharedContainer;

import javax.annotation.Nonnull;

/**
 * System that handles the Lucky Horseshoe accessory effect.
 * When a player with the Lucky Horseshoe equipped takes fall damage,
 * this system cancels the damage, providing fall damage immunity.
 */
public class LuckyHorseshoeSystem extends DamageEventSystem {
    private static final String LUCKY_HORSESHOE_ITEM_ID = "LuckyHorseshoe";

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        if (damage.isCancelled()) {
            return;
        }

         DamageCause cause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (cause == null || !cause.getId().equalsIgnoreCase("Fall")) {
            return;
        }

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) {
            return;
        }

        Player player = (Player) store.getComponent(ref, Player.getComponentType());
        if (player == null || player.isWaitingForClientReady()) {
            return;
        }
        PlayerRef playerRef = (PlayerRef) store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        if (!AccessoryPouchSharedContainer.hasItemInPouch(player.getInventory(), playerRef.getUuid(), LUCKY_HORSESHOE_ITEM_ID)) {
            return;
        }

        damage.setAmount(0.0f);
        damage.setCancelled(true);
    }

}
