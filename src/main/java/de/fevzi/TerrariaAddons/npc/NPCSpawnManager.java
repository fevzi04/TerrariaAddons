package de.fevzi.TerrariaAddons.npc;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.fevzi.TerrariaAddons.housing.npc.NPCSpawnData;
import de.fevzi.TerrariaAddons.housing.npc.NPCSpawnDataManager;
import it.unimi.dsi.fastutil.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class NPCSpawnManager {
    public static final long RESPAWN_COOLDOWN_MS = 30000L;
    public static final int SPAWN_RADIUS = 30;
    public static final double DAYTIME_THRESHOLD = 0.3;

    private static final List<String> NPC_TYPES = Arrays.asList(
        "ArmsDealer",
        "GoblinTinkerer"
    );

    @Nullable
    public static String getNextAvailableNpcType(@Nonnull String worldKey) {
        for (String npcType : NPC_TYPES) {
            if (!NPCSpawnDataManager.isNpcTypeSpawned(worldKey, npcType)
                && NPCSpawnDataManager.canRespawn(worldKey, npcType, RESPAWN_COOLDOWN_MS)) {
                return npcType;
            }
        }
        return null;
    }

    public static List<String> getAllAvailableNpcTypes(@Nonnull String worldKey) {
        List<String> available = new ArrayList<>();
        for (String npcType : NPC_TYPES) {
            if (!NPCSpawnDataManager.isNpcTypeSpawned(worldKey, npcType)
                && NPCSpawnDataManager.canRespawn(worldKey, npcType, RESPAWN_COOLDOWN_MS)) {
                available.add(npcType);
            }
        }
        return available;
    }

    @Nonnull
    public static Vector3d findSpawnPosition(@Nonnull World world, @Nonnull Vector3i housingPos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempts = 0; attempts < 50; attempts++) {
            int offsetX = random.nextInt(-SPAWN_RADIUS, SPAWN_RADIUS + 1);
            int offsetZ = random.nextInt(-SPAWN_RADIUS, SPAWN_RADIUS + 1);
            int x = housingPos.x + offsetX;
            int z = housingPos.z + offsetZ;
            for (int yOffset = -10; yOffset <= 10; yOffset++) {
                int y = housingPos.y + yOffset;
                if (isValidSpawnLocation(world, x, y, z)) {
                    return new Vector3d(x + 0.5, y + 0.1, z + 0.5);
                }
            }
        }
        return new Vector3d(housingPos.x + 0.5, housingPos.y + 1.0, housingPos.z + 0.5);
    }

    private static boolean isValidSpawnLocation(@Nonnull World world, int x, int y, int z) {
        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            BlockAccessor accessor = world.getChunkIfLoaded(chunkIndex);
            if (accessor == null) {
                return false;
            }
            int blockBelow = accessor.getBlock(x, y - 1, z);
            int blockAtFeet = accessor.getBlock(x, y, z);
            int blockAtHead = accessor.getBlock(x, y + 1, z);
            return blockBelow != 0 && blockAtFeet == 0 && blockAtHead == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static void spawnNpc(@Nonnull CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull World world,
                                   @Nonnull String worldKey,
                                   @Nonnull String npcTypeId,
                                   @Nonnull Vector3i housingPos) {
        Vector3d spawnPos = findSpawnPosition(world, housingPos);

        commandBuffer.run(store -> {
            NPCPlugin npcPlugin = NPCPlugin.get();
            if (npcPlugin == null || !npcPlugin.hasRoleName(npcTypeId)) {
                return;
            }

            int roleIndex = npcPlugin.getIndex(npcTypeId);
            Pair<Ref<EntityStore>, NPCEntity> npcPair = npcPlugin.spawnEntity(store, roleIndex, spawnPos, null, null, null);
            if (npcPair == null) {
                return;
            }

            Ref<EntityStore> npcRef = npcPair.first();
            NPCEntity npcEntity = npcPair.second();
            if (npcRef == null || !npcRef.isValid()) {
                return;
            }

            UUIDComponent uuidComponent = store.getComponent(npcRef, UUIDComponent.getComponentType());
            if (uuidComponent == null) {
                return;
            }

            if (npcEntity != null) {
                Vector3d leashPos = new Vector3d(
                    housingPos.x + 0.5,
                    housingPos.y + 1.0,
                    housingPos.z + 0.5
                );
                TransformComponent transform = store.getComponent(npcRef, TransformComponent.getComponentType());
                if (transform != null) {
                    npcEntity.saveLeashInformation(leashPos, transform.getRotation());
                } else {
                    npcEntity.saveLeashInformation(leashPos, new com.hypixel.hytale.math.vector.Vector3f(0f, 0f, 0f));
                }
            }

            NPCSpawnData data = new NPCSpawnData(npcTypeId, housingPos);
            data.setAlive(true);
            data.setDeathTimestamp(0L);
            data.setEntityUuid(uuidComponent.getUuid());
            NPCSpawnDataManager.registerNpc(worldKey, data);

            broadcastMoveInMessage(formatNpcName(npcTypeId));
        });
    }

    private static void broadcastMoveInMessage(@Nonnull String displayName) {
        String message = displayName + " has moved in!";
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            if (playerRef == null || !playerRef.isValid()) {
                continue;
            }
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                continue;
            }
            playerRef.sendMessage(Message.raw(message));
        }
    }

    private static String formatNpcName(@Nonnull String npcTypeId) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < npcTypeId.length(); i++) {
            char c = npcTypeId.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                result.append(' ');
            }
            result.append(c);
        }
        return result.toString();
    }

    public static void onNpcDeath(@Nonnull String worldKey, @Nonnull UUID entityUuid) {
        for (String npcType : NPC_TYPES) {
            var data = NPCSpawnDataManager.getNpcByType(worldKey, npcType);
            if (data != null && entityUuid.equals(data.getEntityUuid())) {
                NPCSpawnDataManager.markNpcDead(worldKey, npcType);
                break;
            }
        }
    }

    public static boolean isRegisteredNpc(@Nonnull String worldKey, @Nonnull UUID entityUuid) {
        for (String npcType : NPC_TYPES) {
            var data = NPCSpawnDataManager.getNpcByType(worldKey, npcType);
            if (data != null && entityUuid.equals(data.getEntityUuid())) {
                return true;
            }
        }
        return false;
    }
}
