package de.fevzi.TerrariaAddons.npc;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.fevzi.TerrariaAddons.housing.npc.NPCSpawnData;
import de.fevzi.TerrariaAddons.housing.npc.NPCSpawnDataManager;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * System that manages NPC day/night behavior.
 * During daytime: NPCs wander around their housing area.
 * During nighttime: NPCs return to their home and stay there.
 */
public class NPCBehaviorSystem extends EntityTickingSystem<EntityStore> {

    private static final double DAYTIME_THRESHOLD = 0.3;
    private static final double NIGHTTIME_THRESHOLD = 0.2;
    private static final int WANDER_RADIUS = 30;
    private static final long WANDER_INTERVAL_MS = 10000L;
    private static final long BEHAVIOR_CHECK_INTERVAL_MS = 2000L;
    private static final long STUCK_TELEPORT_MS = 15000L;
    private static final double STUCK_DISTANCE = 1.0;
    private static final double MIN_MOVE_DISTANCE = 0.4;
    private static final double HOME_REACHED_DISTANCE = 0.5;

    private static final Map<UUID, Long> LAST_WANDER_TIME = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> NPC_IS_HOME = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_BEHAVIOR_CHECK = new ConcurrentHashMap<>();
    private static final Map<UUID, Vector3d> LAST_POS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_PROGRESS_TIME = new ConcurrentHashMap<>();

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

        Ref<EntityStore> entityRef = chunk.getReferenceTo(index);
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        NPCEntity npcEntity = store.getComponent(entityRef, NPCEntity.getComponentType());
        if (npcEntity == null) {
            return;
        }

        UUIDComponent uuidComponent = store.getComponent(entityRef, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return;
        }

        UUID entityUuid = uuidComponent.getUuid();
        if (entityUuid == null) {
            return;
        }

        EntityStore entityStore = (EntityStore) store.getExternalData();
        if (entityStore == null) {
            return;
        }

        World world = entityStore.getWorld();
        if (world == null) {
            return;
        }

        String worldKey = world.getName();
        if (worldKey == null || worldKey.isEmpty()) {
            return;
        }

        if (!NPCSpawnManager.isRegisteredNpc(worldKey, entityUuid)) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastBehaviorCheck = LAST_BEHAVIOR_CHECK.get(worldKey + ":" + entityUuid);
        if (lastBehaviorCheck != null && (now - lastBehaviorCheck) < BEHAVIOR_CHECK_INTERVAL_MS) {
            return;
        }
        LAST_BEHAVIOR_CHECK.put(worldKey + ":" + entityUuid, now);

        WorldTimeResource worldTime = commandBuffer.getResource(WorldTimeResource.getResourceType());
        if (worldTime == null) {
            return;
        }

        NPCSpawnData npcData = findNpcDataByUuid(worldKey, entityUuid);
        if (npcData == null) {
            return;
        }

        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }

        Vector3i housingPos = npcData.getHousingPosition();
        if (housingPos == null) {
            return;
        }

        String currentOccupant = NPCSpawnDataManager.getNpcTypeAtHousing(worldKey, housingPos);
        if (currentOccupant == null || !currentOccupant.equals(npcData.getNpcTypeId())) {
            NPC_IS_HOME.put(entityUuid, false);
            return;
        }

        double sunlightFactor = worldTime.getSunlightFactor();
        boolean isDaytime = sunlightFactor > DAYTIME_THRESHOLD;
        boolean isNighttime = sunlightFactor <= NIGHTTIME_THRESHOLD;

        if (isDaytime) {
            handleDaytimeBehavior(npcEntity, entityUuid, housingPos, now);
        } else if (isNighttime) {
            handleNighttimeBehavior(npcEntity, entityUuid, housingPos, transform.getPosition(), now, store, entityRef);
        }
    }

    private void handleDaytimeBehavior(@Nonnull NPCEntity npcEntity,
                                        @Nonnull UUID entityUuid,
                                        @Nonnull Vector3i housingPos,
                                        long now) {
        NPC_IS_HOME.put(entityUuid, false);

        Long lastWander = LAST_WANDER_TIME.get(entityUuid);
        if (lastWander != null && (now - lastWander) < WANDER_INTERVAL_MS) {
            return;
        }

        Vector3d wanderTarget = generateWanderPosition(housingPos);
        npcEntity.setLeashPoint(wanderTarget);
        LAST_WANDER_TIME.put(entityUuid, now);
    }

    private void handleNighttimeBehavior(@Nonnull NPCEntity npcEntity,
                                         @Nonnull UUID entityUuid,
                                         @Nonnull Vector3i housingPos,
                                         @Nonnull Vector3d currentPos,
                                         long now,
                                         @Nonnull Store<EntityStore> store,
                                         @Nonnull Ref<EntityStore> entityRef) {
        Vector3d homePosition = new Vector3d(
            housingPos.x + 0.5,
            housingPos.y + 1.0,
            housingPos.z + 0.5
        );

        double distanceToHome = distance(currentPos, homePosition);
        if (distanceToHome <= HOME_REACHED_DISTANCE) {
            NPC_IS_HOME.put(entityUuid, true);
            LAST_WANDER_TIME.remove(entityUuid);
            LAST_PROGRESS_TIME.remove(entityUuid);
            LAST_POS.remove(entityUuid);
            return;
        }

        NPC_IS_HOME.put(entityUuid, false);

        updateMovementProgress(entityUuid, currentPos, now);

        Long lastProgress = LAST_PROGRESS_TIME.get(entityUuid);
        if (distanceToHome > STUCK_DISTANCE && lastProgress != null && (now - lastProgress) >= STUCK_TELEPORT_MS) {
            teleportNpcToHome(store, entityRef, homePosition);
            NPC_IS_HOME.put(entityUuid, true);
            LAST_WANDER_TIME.remove(entityUuid);
            LAST_PROGRESS_TIME.remove(entityUuid);
            LAST_POS.remove(entityUuid);
            return;
        }

        npcEntity.setLeashPoint(homePosition);
        LAST_WANDER_TIME.remove(entityUuid);
    }

    @Nonnull
    private Vector3d generateWanderPosition(@Nonnull Vector3i housingPos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int offsetX = random.nextInt(-WANDER_RADIUS, WANDER_RADIUS + 1);
        int offsetZ = random.nextInt(-WANDER_RADIUS, WANDER_RADIUS + 1);

        return new Vector3d(
            housingPos.x + offsetX + 0.5,
            housingPos.y + 1.0,
            housingPos.z + offsetZ + 0.5
        );
    }

    @javax.annotation.Nullable
    private NPCSpawnData findNpcDataByUuid(@Nonnull String worldKey, @Nonnull UUID entityUuid) {
        for (String npcType : getNpcTypes()) {
            NPCSpawnData data = NPCSpawnDataManager.getNpcByType(worldKey, npcType);
            if (data != null && entityUuid.equals(data.getEntityUuid())) {
                return data;
            }
        }
        return null;
    }

    private java.util.List<String> getNpcTypes() {
        return NPCSpawnManager.NPC_TYPES;
    }

    private void updateMovementProgress(@Nonnull UUID entityUuid, @Nonnull Vector3d currentPos, long now) {
        Vector3d lastPos = LAST_POS.get(entityUuid);
        if (lastPos == null) {
            LAST_POS.put(entityUuid, currentPos);
            LAST_PROGRESS_TIME.put(entityUuid, now);
            return;
        }

        if (distance(currentPos, lastPos) >= MIN_MOVE_DISTANCE) {
            LAST_POS.put(entityUuid, currentPos);
            LAST_PROGRESS_TIME.put(entityUuid, now);
        }
    }

    private void teleportNpcToHome(@Nonnull Store<EntityStore> store,
                                   @Nonnull Ref<EntityStore> entityRef,
                                   @Nonnull Vector3d homePosition) {
        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        transform.setPosition(homePosition);
    }


    private static double distance(@Nonnull Vector3d a, @Nonnull Vector3d b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
    }

    public static void cleanupNpc(@Nonnull UUID entityUuid) {
        LAST_WANDER_TIME.remove(entityUuid);
        NPC_IS_HOME.remove(entityUuid);
        LAST_POS.remove(entityUuid);
        LAST_PROGRESS_TIME.remove(entityUuid);
        LAST_BEHAVIOR_CHECK.entrySet().removeIf(entry -> entry.getKey().endsWith(":" + entityUuid.toString()));
    }
}

