package de.fevzi.TerrariaAddons.items.MagmaStone;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.fevzi.TerrariaAddons.items.AccessoryPouch.AccessoryPouchSharedContainer;

import javax.annotation.Nonnull;

public class MagmaStoneSystem extends DamageEventSystem {
    private static final String MAGMA_STONE_ITEM_ID = "MagmaStone";
    private static final String[] BURN_EFFECT_IDS = {
            "Status/MagmaStone_Burn",
            "MagmaStone_Burn",
            "Burn",
            "Status/Burn"
    };
    private static final float FALLBACK_DURATION_SECONDS = 3.0f;
    private static EntityEffect cachedBurnEffect;

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
        if (damage.isCancelled() || damage.getAmount() <= 0f) {
            return;
        }

        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
        if (targetRef == null || !targetRef.isValid()) {
            return;
        }

        Ref<EntityStore> attackerRef = resolveAttackerRef(damage);
        if (attackerRef == null || !attackerRef.isValid() || attackerRef.equals(targetRef)) {
            return;
        }

        Player attacker = (Player) store.getComponent(attackerRef, Player.getComponentType());
        if (attacker == null || attacker.isWaitingForClientReady()) {
            return;
        }

        PlayerRef attackerRefComponent = (PlayerRef) store.getComponent(attackerRef, PlayerRef.getComponentType());
        if (attackerRefComponent == null) {
            return;
        }

        if (!AccessoryPouchSharedContainer.hasItemInPouch(attacker.getInventory(), attackerRefComponent.getUuid(), MAGMA_STONE_ITEM_ID)) {
            return;
        }

        EffectControllerComponent effects = (EffectControllerComponent) store.getComponent(
                targetRef, EffectControllerComponent.getComponentType());
        if (effects == null) {
            return;
        }

        EntityEffect burnEffect = resolveBurnEffect();
        if (burnEffect == null) {
            return;
        }

        float duration = burnEffect.getDuration();
        if (duration <= 0f) {
            duration = FALLBACK_DURATION_SECONDS;
        }
        OverlapBehavior overlapBehavior = burnEffect.getOverlapBehavior();
        if (overlapBehavior == null) {
            overlapBehavior = OverlapBehavior.OVERWRITE;
        }

        effects.addEffect(targetRef, burnEffect, duration, overlapBehavior, store);
    }

    private static Ref<EntityStore> resolveAttackerRef(Damage damage) {
        Damage.Source source = damage.getSource();
        if (source instanceof Damage.EntitySource entitySource) {
            return entitySource.getRef();
        }
        return null;
    }

    private static EntityEffect resolveBurnEffect() {
        if (cachedBurnEffect != null) {
            return cachedBurnEffect;
        }

        var map = EntityEffect.getAssetMap();
        if (map == null) {
            return null;
        }

        for (String id : BURN_EFFECT_IDS) {
            int index = map.getIndexOrDefault(id, -1);
            if (index >= 0) {
                EntityEffect effect = map.getAsset(index);
                if (effect != null) {
                    cachedBurnEffect = effect;
                    return effect;
                }
            }
        }

        return null;
    }

}
