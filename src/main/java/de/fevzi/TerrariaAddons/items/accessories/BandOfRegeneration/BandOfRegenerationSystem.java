package de.fevzi.TerrariaAddons.items.accessories.BandOfRegeneration;

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
import de.fevzi.TerrariaAddons.items.accessoryPouch.AccessoryPouchSharedContainer;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * System that handles the Band of Regeneration accessory effect.
 * When a player has the Band of Regeneration equipped in their accessory pouch,
 * this system provides passive health regeneration over time.
 * Heals 1 HP per second while equipped.
 */
public class BandOfRegenerationSystem extends EntityTickingSystem<EntityStore> {
    private static final String BAND_ITEM_ID = "BandOfRegeneration";
    private static final float HEAL_PER_SECOND = 1.0f;
    private static final Map<UUID, Float> HEAL_REMAINDERS = new ConcurrentHashMap<>();

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
            HEAL_REMAINDERS.remove(uuid);
            return;
        }

        EntityStatMap stats = (EntityStatMap) commandBuffer.getComponent(ref, EntityStatMap.getComponentType());
        if (stats == null) {
            return;
        }

        float remainder = HEAL_REMAINDERS.getOrDefault(uuid, 0f);
        float toHeal = remainder + (delta * HEAL_PER_SECOND);
        if (toHeal < 1f) {
            HEAL_REMAINDERS.put(uuid, toHeal);
            return;
        }

        float amount = (float) Math.floor(toHeal);
        float newRemainder = toHeal - amount;
        stats.addStatValue(DefaultEntityStatTypes.getHealth(), amount);
        HEAL_REMAINDERS.put(uuid, newRemainder);
    }


}
