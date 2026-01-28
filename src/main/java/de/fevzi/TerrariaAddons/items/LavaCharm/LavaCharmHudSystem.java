package de.fevzi.TerrariaAddons.items.LavaCharm;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.fevzi.TerrariaAddons.items.AccessoryPouch.AccessoryPouchSharedContainer;
import de.fevzi.TerrariaAddons.ui.MultipleHudBridge;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * System that manages the Lava Charm immunity HUD display.
 * Tracks players' lava immunity duration and displays a progress bar
 * showing remaining immunity time. The HUD is shown when the player
 * is exposed to lava and has the Lava Charm equipped.
 */
public class LavaCharmHudSystem extends EntityTickingSystem<EntityStore> {
    private static final String LAVA_CHARM_ITEM_ID = "LavaCharm";
    private static final String LAVA_CHARM_HUD_KEY = "TerrariaAddons_LavaCharm";
    private static final String[] BURN_EFFECT_IDS = {"Lava_Burn"};
    private static final float BURN_CHECK_INTERVAL_SECONDS = 1.0f;

    private final Map<UUID, LavaCharmHud> hudByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerRef> playerByUuid = new ConcurrentHashMap<>();
    private final Map<UUID, Player> playerComponentByUuid = new ConcurrentHashMap<>();
    private final Map<UUID, Float> burnCheckTimerByUuid = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> hasBurnEffectByUuid = new ConcurrentHashMap<>();
    private final Object hudLock = new Object();

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
        if (playerRef == null) {
            return;
        }

        UUID uuid = playerRef.getUuid();
        boolean hasCharm = AccessoryPouchSharedContainer.hasItemInPouch(player.getInventory(), uuid, LAVA_CHARM_ITEM_ID);

        if (hasCharm) {
            float burnCheckTimer = burnCheckTimerByUuid.getOrDefault(uuid, 0.0f);
            burnCheckTimer += delta;
            boolean currentBurnEffectState = hasBurnEffectByUuid.getOrDefault(uuid, false);

            if (burnCheckTimer >= BURN_CHECK_INTERVAL_SECONDS) {
                burnCheckTimer = 0.0f;
                currentBurnEffectState = hasBurnEffect(player, ref, store);
                hasBurnEffectByUuid.put(uuid, currentBurnEffectState);

                if (currentBurnEffectState && LavaCharmImmunityManager.isImmune(uuid)) {
                    removeBurnEffect(player, ref, store);
                }
            }

            boolean showHud = LavaCharmImmunityManager.tick(uuid, delta, currentBurnEffectState);

            if (showHud) {
                float progress = LavaCharmImmunityManager.getProgress(uuid);
                showHud(playerRef, player, progress);
            } else {
                hideHud(playerRef);
            }

            burnCheckTimerByUuid.put(uuid, burnCheckTimer);
        } else {
            LavaCharmImmunityManager.clearImmunity(uuid);
            hideHud(playerRef);
            burnCheckTimerByUuid.remove(uuid);
            hasBurnEffectByUuid.remove(uuid);
        }
    }

    private void showHud(PlayerRef playerRef, Player playerComponent, float progress) {
        synchronized (hudLock) {
            UUID uuid = playerRef.getUuid();
            playerByUuid.put(uuid, playerRef);
            playerComponentByUuid.put(uuid, playerComponent);

            LavaCharmHud hud = hudByPlayer.computeIfAbsent(uuid, ignored -> {
                return new LavaCharmHud(playerRef);
            });

            MultipleHudBridge.setCustomHud(playerComponent, playerRef, LAVA_CHARM_HUD_KEY, hud);
            hud.setVisible(true);
            hud.setProgress(progress);
        }
    }

    private void hideHud(PlayerRef playerRef) {
        synchronized (hudLock) {
            UUID uuid = playerRef.getUuid();
            LavaCharmHud hud = hudByPlayer.get(uuid);
            if (hud != null) {
                hud.setVisible(false);
            }
        }
    }

    private boolean hasBurnEffect(Player player, Ref<EntityStore> playerRef, Store<EntityStore> store) {
        EffectControllerComponent effects = (EffectControllerComponent) store.getComponent(
                playerRef, EffectControllerComponent.getComponentType());
        if (effects == null) {
            return false;
        }

        ActiveEntityEffect[] activeEffects = effects.getAllActiveEntityEffects();
        if (activeEffects == null) {
            return false;
        }

        var effectMap = EntityEffect.getAssetMap();
        if (effectMap == null) {
            return false;
        }

        for (ActiveEntityEffect activeEffect : activeEffects) {
            int effectIndex = activeEffect.getEntityEffectIndex();
            EntityEffect effect = effectMap.getAsset(effectIndex);
            if (effect != null) {
                String effectId = effect.getId();
                for (String burnId : BURN_EFFECT_IDS) {
                    if (burnId.equals(effectId)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void removeBurnEffect(Player player, Ref<EntityStore> playerRef, Store<EntityStore> store) {
        EffectControllerComponent effects = (EffectControllerComponent) store.getComponent(
                playerRef, EffectControllerComponent.getComponentType());
        if (effects == null) {
            return;
        }

        var effectMap = EntityEffect.getAssetMap();
        if (effectMap == null) {
            return;
        }

        for (String burnId : BURN_EFFECT_IDS) {
            int effectIndex = effectMap.getIndexOrDefault(burnId, -1);
            if (effectIndex >= 0) {
                effects.removeEffect(playerRef, effectIndex, store);
            }
        }
    }


}
