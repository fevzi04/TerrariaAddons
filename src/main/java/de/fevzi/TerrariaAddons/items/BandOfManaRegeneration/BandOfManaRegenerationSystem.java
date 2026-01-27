package de.fevzi.TerrariaAddons.items.BandOfManaRegeneration;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.fevzi.TerrariaAddons.items.AccessoryPouch.AccessoryPouchSharedContainer;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * System that handles the Band of Mana Regeneration accessory effect.
 * When a player has the Band of Mana Regeneration equipped in their accessory pouch,
 * this system provides passive mana regeneration over time.
 * Regenerates 3 mana per second while equipped.
 */
public class BandOfManaRegenerationSystem extends EntityTickingSystem<EntityStore> {
    private static final String BAND_ITEM_ID = "BandOfManaRegeneration";
    private static final float MANA_PER_SECOND = 3.0f;
    private static final Map<UUID, Float> MANA_REMAINDERS = new ConcurrentHashMap<>();

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void tick(float delta,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) {
            return;
        }

        Player player = (Player) store.getComponent(ref, Player.getComponentType());
        if (player == null || player.isWaitingForClientReady()) {
            return;
        }

        PlayerRef playerRef = (PlayerRef) store.getComponent(ref, PlayerRef.getComponentType());
        UUID uuid = playerRef == null ? null : playerRef.getUuid();
        if (uuid == null) {
            return;
        }

        if (!AccessoryPouchSharedContainer.hasItemInPouch(player.getInventory(), uuid, BAND_ITEM_ID)) {
            MANA_REMAINDERS.remove(uuid);
            return;
        }

        EntityStatMap stats = (EntityStatMap) commandBuffer.getComponent(ref, EntityStatMap.getComponentType());
        if (stats == null) {
            return;
        }

        float remainder = MANA_REMAINDERS.getOrDefault(uuid, 0f);
        float toManaRegen = remainder + (delta * MANA_PER_SECOND);
        if (toManaRegen < 1f) {
            MANA_REMAINDERS.put(uuid, toManaRegen);
            return;
        }

        float amount = (float) Math.floor(toManaRegen);
        float newRemainder = toManaRegen - amount;
        stats.addStatValue(DefaultEntityStatTypes.getMana(), amount);
        MANA_REMAINDERS.put(uuid, newRemainder);
    }


}
