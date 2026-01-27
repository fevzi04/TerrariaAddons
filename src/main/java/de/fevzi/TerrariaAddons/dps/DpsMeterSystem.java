package de.fevzi.TerrariaAddons.dps;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import de.fevzi.TerrariaAddons.items.AccessoryPouch.AccessoryPouchSharedContainer;
import de.fevzi.TerrariaAddons.ui.MultipleHudBridge;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * System that tracks and displays damage per second (DPS) for players.
 * When a player has the DPS Meter accessory equipped, this system calculates
 * their damage output over a sliding time window and displays it on a HUD.
 * The HUD automatically hides after a period of inactivity.
 */
public class DpsMeterSystem extends DamageEventSystem {
    private static final String DPS_METER_ITEM_ID = "DPSMeter";
    private static final long WINDOW_MILLIS = 5_000L;
    private static final long SEND_INTERVAL_MILLIS = 1_000L;
    private static final long HIDE_AFTER_MILLIS = 5_000L;
    private static final long HIDE_CHECK_INTERVAL_MILLIS = 1_000L;

    private final Map<String, Deque<DamageSample>> samplesByPlayer = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSendByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, DpsHud> hudByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDamageByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerRef> playerByUuid = new ConcurrentHashMap<>();
    private final Map<UUID, Player> playerComponentByUuid = new ConcurrentHashMap<>();
    private final Map<UUID, HudManager> hudManagerByUuid = new ConcurrentHashMap<>();
    private final Object hudLock = new Object();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "dps-meter-hud");
        thread.setDaemon(true);
        return thread;
    });
    private ScheduledFuture<?> hideTask;

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        if (damage.isCancelled()) {
            return;
        }

        float amount = damage.getAmount();
        if (amount <= 0f) {
            return;
        }

        Ref<EntityStore> attackerRef = resolveAttackerRef(damage);
        if (attackerRef == null || !attackerRef.isValid()) {
            return;
        }

        PlayerRef attacker = findPlayerRef(attackerRef);
        if (attacker == null || !attacker.isValid()) {
            return;
        }

        Player attackerComponent = (Player) store.getComponent(attackerRef, Player.getComponentType());
        if (attackerComponent == null) {
            return;
        }

        if (attackerComponent.isWaitingForClientReady()) {
            return;
        }

        long now = System.currentTimeMillis();
        String key = attacker.getUsername().toString();

        Deque<DamageSample> samples = samplesByPlayer.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        double totalDamage;
        synchronized (samples) {
            samples.addLast(new DamageSample(now, amount));
            totalDamage = trimAndSum(samples, now);
        }

        long lastSent = lastSendByPlayer.getOrDefault(key, 0L);
        if (now - lastSent < SEND_INTERVAL_MILLIS) {
            return;
        }

        lastSendByPlayer.put(key, now);
        double dps = totalDamage / (WINDOW_MILLIS / 1000.0);
        String message = String.format("DPS: %.1f", dps);
        if (!AccessoryPouchSharedContainer.hasItemInPouch(attackerComponent.getInventory(), attacker.getUuid(), DPS_METER_ITEM_ID)) {
            hideHud(attacker);
            return;
        }
        updateHud(attacker, attackerComponent.getHudManager(), attackerComponent, message);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void onSystemRegistered() {
        if (hideTask != null) {
            return;
        }
        hideTask = scheduler.scheduleAtFixedRate(this::hideInactiveHud,
                HIDE_CHECK_INTERVAL_MILLIS,
                HIDE_CHECK_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void onSystemUnregistered() {
        if (hideTask != null) {
            hideTask.cancel(false);
            hideTask = null;
        }
        scheduler.shutdownNow();
    }

    private static double trimAndSum(Deque<DamageSample> samples, long now) {
        while (!samples.isEmpty() && now - samples.peekFirst().timestampMillis > WINDOW_MILLIS) {
            samples.removeFirst();
        }

        double total = 0.0;
        for (DamageSample sample : samples) {
            total += sample.amount;
        }
        return total;
    }

    private static Ref<EntityStore> resolveAttackerRef(Damage damage) {
        Damage.Source source = damage.getSource();
        if (source instanceof Damage.EntitySource entitySource) {
            return entitySource.getRef();
        }
        return null;
    }

    private static PlayerRef findPlayerRef(Ref<EntityStore> attackerRef) {
        for (PlayerRef player : Universe.get().getPlayers()) {
            if (!player.isValid()) {
                continue;
            }

            Ref<EntityStore> playerRef = player.getReference();
            if (playerRef != null && attackerRef.equals(playerRef)) {
                return player;
            }
        }
        return null;
    }

    private void updateHud(PlayerRef playerRef, HudManager hudManager, Player playerComponent, String message) {
        synchronized (hudLock) {
            UUID uuid = playerRef.getUuid();
            long now = System.currentTimeMillis();
            lastDamageByPlayer.put(uuid, now);
            playerByUuid.put(uuid, playerRef);
            playerComponentByUuid.put(uuid, playerComponent);
            hudManagerByUuid.put(uuid, hudManager);

            boolean usesMultipleHud = MultipleHudBridge.isAvailable();
            DpsHud hud = hudByPlayer.computeIfAbsent(uuid, ignored -> {
                DpsHud created = new DpsHud(playerRef);
                attachHud(playerComponent, playerRef, hudManager, created, usesMultipleHud);
                created.show();
                return created;
            });

            if (!usesMultipleHud && hudManager.getCustomHud() != hud) {
                hudManager.setCustomHud(playerRef, hud);
                hud.show();
            }

            hud.setVisible(true);
            hud.setText(message);
        }
    }

    private void hideHud(PlayerRef playerRef) {
        synchronized (hudLock) {
            UUID uuid = playerRef.getUuid();
            DpsHud hud = hudByPlayer.get(uuid);
            if (hud != null) {
                hud.setVisible(false);
                markHidden(uuid);
            }
        }
    }

    private void attachHud(Player player, PlayerRef playerRef, HudManager hudManager, DpsHud hud, boolean usesMultipleHud) {
        if (usesMultipleHud) {
            MultipleHudBridge.setCustomHud(player, playerRef, "TerrariaAddons_Dps", hud);
            return;
        }
        hudManager.setCustomHud(playerRef, hud);
    }

    private void hideInactiveHud() {
        synchronized (hudLock) {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, Long> entry : lastDamageByPlayer.entrySet()) {
                UUID uuid = entry.getKey();
                PlayerRef playerRef = playerByUuid.get(uuid);
                HudManager hudManager = hudManagerByUuid.get(uuid);
                DpsHud hud = hudByPlayer.get(uuid);

                if (playerRef == null || hudManager == null || hud == null || !playerRef.isValid()) {
                    cleanupPlayer(uuid);
                    continue;
                }

                Player playerComponent = playerComponentByUuid.get(uuid);
                if (playerComponent == null || !AccessoryPouchSharedContainer.hasItemInPouch(playerComponent.getInventory(), uuid, DPS_METER_ITEM_ID)) {
                    hud.setVisible(false);
                    markHidden(uuid);
                    continue;
                }

                if (now - entry.getValue() <= HIDE_AFTER_MILLIS) {
                    continue;
                }

                hud.setVisible(false);
                markHidden(uuid);
            }
        }
    }

    private void markHidden(UUID uuid) {
        lastDamageByPlayer.remove(uuid);
        lastSendByPlayer.remove(uuid);
    }


    private void cleanupPlayer(UUID uuid) {
        lastDamageByPlayer.remove(uuid);
        lastSendByPlayer.remove(uuid);
        hudByPlayer.remove(uuid);
        playerByUuid.remove(uuid);
        playerComponentByUuid.remove(uuid);
        hudManagerByUuid.remove(uuid);
    }

    private static final class DamageSample {
        private final long timestampMillis;
        private final double amount;

        private DamageSample(long timestampMillis, double amount) {
            this.timestampMillis = timestampMillis;
            this.amount = amount;
        }
    }
}
