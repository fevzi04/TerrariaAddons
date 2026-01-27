package de.fevzi.TerrariaAddons.items.LavaCharm;

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
import de.fevzi.TerrariaAddons.items.AccessoryPouch.AccessoryPouchSharedContainer;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * System that handles the Lava Charm accessory effect.
 * When a player with the Lava Charm equipped takes fire/lava damage,
 * this system cancels the damage if the player has remaining immunity time.
 * Works in conjunction with LavaCharmHudSystem to track and display immunity duration.
 */
public class LavaCharmSystem extends DamageEventSystem {
    private static final String LAVA_CHARM_ITEM_ID = "LavaCharm";
    private static final String[] FIRE_DAMAGE_CAUSES = {"Fire", "Lava", "Burn", "Magma"};

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
        if (cause == null) {
            return;
        }

        String causeId = cause.getId();
        boolean isFireDamage = false;
        for (String fireCause : FIRE_DAMAGE_CAUSES) {
            if (causeId.equalsIgnoreCase(fireCause)) {
                isFireDamage = true;
                break;
            }
        }

        if (!isFireDamage) {
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

        UUID playerUuid = playerRef.getUuid();
        if (!AccessoryPouchSharedContainer.hasItemInPouch(player.getInventory(), playerUuid, LAVA_CHARM_ITEM_ID)) {
            return;
        }

        LavaCharmImmunityManager.recordDamage(playerUuid);

        if (LavaCharmImmunityManager.isImmune(playerUuid)) {
            damage.setAmount(0.0f);
            damage.setCancelled(true);
        }
    }

}