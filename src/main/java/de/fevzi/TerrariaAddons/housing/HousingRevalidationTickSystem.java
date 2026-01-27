package de.fevzi.TerrariaAddons.housing;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * System that processes queued housing revalidation requests.
 * When blocks are placed or broken near housing checkers, this system
 * processes the revalidation queue with a small delay to batch updates
 * and avoid redundant validation checks.
 */
public class HousingRevalidationTickSystem extends EntityTickingSystem<EntityStore> {
    private static final int DELAY_TICKS = 1;
    private static final ConcurrentLinkedQueue<PendingRevalidation> QUEUE = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean PROCESSING = new AtomicBoolean(false);

    public static void enqueue(Ref<EntityStore> playerRef, Vector3i changed) {
        if (playerRef == null || changed == null) {
            return;
        }
        QUEUE.add(new PendingRevalidation(playerRef, new Vector3i(changed), DELAY_TICKS));
    }

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
        if (!PROCESSING.compareAndSet(false, true)) {
            return;
        }
        try {
            int size = QUEUE.size();
            for (int i = 0; i < size; i++) {
                PendingRevalidation pending = QUEUE.poll();
                if (pending == null) {
                    break;
                }

                if (--pending.delayTicks > 0) {
                    QUEUE.add(pending);
                    continue;
                }

                if (!pending.playerRef.isValid()) {
                    continue;
                }

                Player player = (Player) store.getComponent(pending.playerRef, Player.getComponentType());
                if (player == null) {
                    continue;
                }

                EntityStore entityStore = (EntityStore) store.getExternalData();
                if (entityStore == null) {
                    continue;
                }

                World world = entityStore.getWorld();
                if (world == null) {
                    continue;
                }

                HousingCheckerSystem.revalidateNearby(world, pending.changed, player);
            }
        } finally {
            PROCESSING.set(false);
        }
    }

    private static final class PendingRevalidation {
        private final Ref<EntityStore> playerRef;
        private final Vector3i changed;
        private int delayTicks;

        private PendingRevalidation(Ref<EntityStore> playerRef, Vector3i changed, int delayTicks) {
            this.playerRef = playerRef;
            this.changed = changed;
            this.delayTicks = delayTicks;
        }
    }


}
