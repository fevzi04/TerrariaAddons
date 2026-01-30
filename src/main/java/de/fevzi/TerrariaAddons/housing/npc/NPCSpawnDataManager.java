package de.fevzi.TerrariaAddons.housing.npc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.math.vector.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages NPC spawn data persistence and tracking.
 * Tracks which NPCs are assigned to which housings, their alive status,
 * death timestamps for respawn cooldowns, and persists this data to disk.
 */
public class NPCSpawnDataManager {
    private static final String DATA_DIR = "NPCSpawnData";
    private static final String DATA_FILE = "npc_spawn_data.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object SAVE_LOCK = new Object();
    private static final Map<String, Map<Vector3i, NPCSpawnData>> HOUSING_NPC_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, NPCSpawnData>> NPC_TYPE_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Set<Vector3i>> OCCUPIED_HOUSINGS = new ConcurrentHashMap<>();

    static {
        loadFromDisk();
    }

    @Nullable
    public static NPCSpawnData getNpcAtHousing(@Nonnull String worldKey, @Nonnull Vector3i housingPos) {
        Map<Vector3i, NPCSpawnData> worldMap = HOUSING_NPC_MAP.get(worldKey);
        if (worldMap == null) {
            return null;
        }
        return worldMap.get(housingPos);
    }

    @Nullable
    public static NPCSpawnData getNpcByType(@Nonnull String worldKey, @Nonnull String npcTypeId) {
        Map<String, NPCSpawnData> worldMap = NPC_TYPE_MAP.get(worldKey);
        if (worldMap == null) {
            return null;
        }
        return worldMap.get(npcTypeId);
    }

    public static boolean isHousingOccupied(@Nonnull String worldKey, @Nonnull Vector3i housingPos) {
        Set<Vector3i> occupied = OCCUPIED_HOUSINGS.get(worldKey);
        return occupied != null && occupied.contains(housingPos);
    }

    public static boolean isNpcTypeSpawned(@Nonnull String worldKey, @Nonnull String npcTypeId) {
        Map<String, NPCSpawnData> worldMap = NPC_TYPE_MAP.get(worldKey);
        if (worldMap == null) {
            return false;
        }
        NPCSpawnData data = worldMap.get(npcTypeId);
        return data != null && data.isAlive();
    }

    public static void registerNpc(@Nonnull String worldKey, @Nonnull NPCSpawnData data) {
        HOUSING_NPC_MAP.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>()).put(data.getHousingPosition(), data);
        NPC_TYPE_MAP.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>()).put(data.getNpcTypeId(), data);
        OCCUPIED_HOUSINGS.computeIfAbsent(worldKey, k -> ConcurrentHashMap.newKeySet()).add(data.getHousingPosition());
        saveToDisk();
    }

    public static void markNpcAlive(@Nonnull String worldKey, @Nonnull String npcTypeId, @Nonnull UUID entityUuid) {
        NPCSpawnData data = getNpcByType(worldKey, npcTypeId);
        if (data != null) {
            data.setEntityUuid(entityUuid);
            data.setAlive(true);
            data.setDeathTimestamp(0L);
            saveToDisk();
        }
    }

    public static void markNpcDead(@Nonnull String worldKey, @Nonnull String npcTypeId) {
        NPCSpawnData data = getNpcByType(worldKey, npcTypeId);
        if (data != null) {
            data.setAlive(false);
            data.setEntityUuid(null);
            data.setDeathTimestamp(System.currentTimeMillis());
            Set<Vector3i> occupied = OCCUPIED_HOUSINGS.get(worldKey);
            if (occupied != null) {
                occupied.remove(data.getHousingPosition());
            }
            HOUSING_NPC_MAP.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>()).remove(data.getHousingPosition());
            saveToDisk();
        }
    }

    public static void unregisterNpc(@Nonnull String worldKey, @Nonnull String npcTypeId) {
        Map<String, NPCSpawnData> typeMap = NPC_TYPE_MAP.get(worldKey);
        if (typeMap != null) {
            NPCSpawnData data = typeMap.remove(npcTypeId);
            if (data != null) {
                Map<Vector3i, NPCSpawnData> housingMap = HOUSING_NPC_MAP.get(worldKey);
                if (housingMap != null) {
                    housingMap.remove(data.getHousingPosition());
                }
                Set<Vector3i> occupied = OCCUPIED_HOUSINGS.get(worldKey);
                if (occupied != null) {
                    occupied.remove(data.getHousingPosition());
                }
            }
        }
        saveToDisk();
    }

    public static Set<Vector3i> getUnoccupiedHousings(@Nonnull String worldKey, @Nonnull Set<Vector3i> allHousings) {
        Set<Vector3i> occupied = OCCUPIED_HOUSINGS.get(worldKey);
        if (occupied == null || occupied.isEmpty()) {
            return new HashSet<>(allHousings);
        }
        Set<Vector3i> unoccupied = new HashSet<>();
        for (Vector3i housing : allHousings) {
            if (!occupied.contains(housing)) {
                unoccupied.add(housing);
            }
        }
        return unoccupied;
    }

    public static boolean canRespawn(@Nonnull String worldKey, @Nonnull String npcTypeId, long respawnCooldownMs) {
        NPCSpawnData data = getNpcByType(worldKey, npcTypeId);
        if (data == null) {
            return true;
        }
        if (data.isAlive()) {
            return false;
        }
        long deathTime = data.getDeathTimestamp();
        if (deathTime == 0L) {
            return true;
        }
        return System.currentTimeMillis() - deathTime >= respawnCooldownMs;
    }

    public static List<String> getNpcsOnCooldown(@Nonnull String worldKey, long respawnCooldownMs) {
        Map<String, NPCSpawnData> worldMap = NPC_TYPE_MAP.get(worldKey);
        if (worldMap == null) {
            return Collections.emptyList();
        }
        List<String> onCooldown = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<String, NPCSpawnData> entry : worldMap.entrySet()) {
            NPCSpawnData data = entry.getValue();
            if (!data.isAlive() && data.getDeathTimestamp() > 0L) {
                if (now - data.getDeathTimestamp() < respawnCooldownMs) {
                    onCooldown.add(entry.getKey());
                }
            }
        }
        return onCooldown;
    }

    @Nonnull
    public static List<NPCSpawnData> getAllNpcData(@Nonnull String worldKey) {
        Map<String, NPCSpawnData> worldMap = NPC_TYPE_MAP.get(worldKey);
        if (worldMap == null || worldMap.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(worldMap.values());
    }

    private static void loadFromDisk() {
        try {
            Path dataDir = Paths.get(System.getProperty("user.dir"), DATA_DIR);
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
                return;
            }
            Path filePath = dataDir.resolve(DATA_FILE);
            if (!Files.exists(filePath)) {
                return;
            }
            String json = Files.readString(filePath, StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<String, List<SavedNPCData>>>() {}.getType();
            Map<String, List<SavedNPCData>> raw = GSON.fromJson(json, type);
            if (raw == null || raw.isEmpty()) {
                return;
            }
            for (Map.Entry<String, List<SavedNPCData>> entry : raw.entrySet()) {
                String worldKey = entry.getKey();
                List<SavedNPCData> savedList = entry.getValue();
                if (worldKey == null || worldKey.isBlank() || savedList == null) {
                    continue;
                }
                for (SavedNPCData saved : savedList) {
                    if (saved == null) {
                        continue;
                    }
                    Vector3i housingPos = new Vector3i(saved.housingX, saved.housingY, saved.housingZ);
                    NPCSpawnData data = new NPCSpawnData(saved.npcTypeId, housingPos);
                    data.setAlive(saved.alive);
                    data.setDeathTimestamp(saved.deathTimestamp);
                    if (saved.entityUuid != null && !saved.entityUuid.isEmpty()) {
                        try {
                            data.setEntityUuid(UUID.fromString(saved.entityUuid));
                        } catch (Exception ignored) {}
                    }
                    HOUSING_NPC_MAP.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>()).put(housingPos, data);
                    NPC_TYPE_MAP.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>()).put(saved.npcTypeId, data);
                    if (saved.alive) {
                        OCCUPIED_HOUSINGS.computeIfAbsent(worldKey, k -> ConcurrentHashMap.newKeySet()).add(housingPos);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static void saveToDisk() {
        synchronized (SAVE_LOCK) {
            try {
                Path dataDir = Paths.get(System.getProperty("user.dir"), DATA_DIR);
                Files.createDirectories(dataDir);
                Map<String, List<SavedNPCData>> snapshot = new HashMap<>();
                for (Map.Entry<String, Map<String, NPCSpawnData>> worldEntry : NPC_TYPE_MAP.entrySet()) {
                    String worldKey = worldEntry.getKey();
                    List<SavedNPCData> list = new ArrayList<>();
                    for (NPCSpawnData data : worldEntry.getValue().values()) {
                        SavedNPCData saved = new SavedNPCData();
                        saved.npcTypeId = data.getNpcTypeId();
                        saved.housingX = data.getHousingPosition().x;
                        saved.housingY = data.getHousingPosition().y;
                        saved.housingZ = data.getHousingPosition().z;
                        saved.alive = data.isAlive();
                        saved.deathTimestamp = data.getDeathTimestamp();
                        saved.entityUuid = data.getEntityUuid() != null ? data.getEntityUuid().toString() : null;
                        list.add(saved);
                    }
                    snapshot.put(worldKey, list);
                }
                Path filePath = dataDir.resolve(DATA_FILE);
                String json = GSON.toJson(snapshot);
                Files.writeString(filePath, json, StandardCharsets.UTF_8);
            } catch (Exception ignored) {}
        }
    }


    @Nonnull
    public static List<NPCSpawnData> getAllAliveNpcs(@Nonnull String worldKey) {
        Map<String, NPCSpawnData> worldMap = NPC_TYPE_MAP.get(worldKey);
        if (worldMap == null || worldMap.isEmpty()) {
            return Collections.emptyList();
        }
        List<NPCSpawnData> aliveNpcs = new ArrayList<>();
        for (NPCSpawnData data : worldMap.values()) {
            if (data.isAlive()) {
                aliveNpcs.add(data);
            }
        }
        return aliveNpcs;
    }


    public static boolean reassignNpcToHousing(@Nonnull String worldKey, @Nonnull String npcTypeId, @Nonnull Vector3i newHousingPos) {
        NPCSpawnData existingData = getNpcByType(worldKey, npcTypeId);
        if (existingData == null || !existingData.isAlive()) {
            return false;
        }

        Vector3i oldHousingPos = existingData.getHousingPosition();

        Map<Vector3i, NPCSpawnData> housingMap = HOUSING_NPC_MAP.get(worldKey);
        if (housingMap != null) {
            housingMap.remove(oldHousingPos);
        }
        Set<Vector3i> occupied = OCCUPIED_HOUSINGS.get(worldKey);
        if (occupied != null) {
            occupied.remove(oldHousingPos);
        }

        NPCSpawnData newData = new NPCSpawnData(npcTypeId, newHousingPos);
        newData.setEntityUuid(existingData.getEntityUuid());
        newData.setAlive(true);
        newData.setDeathTimestamp(0L);

        HOUSING_NPC_MAP.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>()).put(newHousingPos, newData);
        NPC_TYPE_MAP.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>()).put(npcTypeId, newData);
        OCCUPIED_HOUSINGS.computeIfAbsent(worldKey, k -> ConcurrentHashMap.newKeySet()).add(newHousingPos);

        saveToDisk();

        return true;
    }


    @Nullable
    public static String getNpcTypeAtHousing(@Nonnull String worldKey, @Nonnull Vector3i housingPos) {
        NPCSpawnData data = getNpcAtHousing(worldKey, housingPos);
        return data != null ? data.getNpcTypeId() : null;
    }


    @Nonnull
    public static List<String> getHomelessNpcs(@Nonnull String worldKey, @Nonnull Set<Vector3i> validHousings) {
        List<String> homeless = new ArrayList<>();
        Map<String, NPCSpawnData> worldMap = NPC_TYPE_MAP.get(worldKey);
        if (worldMap == null || worldMap.isEmpty()) {
            return homeless;
        }

        Map<Vector3i, NPCSpawnData> housingMap = HOUSING_NPC_MAP.get(worldKey);

        for (NPCSpawnData data : worldMap.values()) {
            if (!data.isAlive()) {
                continue;
            }

            Vector3i housingPos = data.getHousingPosition();


            if (!validHousings.contains(housingPos)) {
                homeless.add(data.getNpcTypeId());
            } else if (housingMap != null) {
                NPCSpawnData occupant = housingMap.get(housingPos);
                if (occupant == null || !occupant.getNpcTypeId().equals(data.getNpcTypeId())) {
                    homeless.add(data.getNpcTypeId());
                }
            }
        }
        return homeless;
    }

    public static boolean assignHousingToHomelessNpc(@Nonnull String worldKey, @Nonnull String npcTypeId, @Nonnull Vector3i newHousingPos) {
        NPCSpawnData existingData = getNpcByType(worldKey, npcTypeId);
        if (existingData == null || !existingData.isAlive()) {
            return false;
        }

        Vector3i oldHousingPos = existingData.getHousingPosition();
        Map<Vector3i, NPCSpawnData> housingMap = HOUSING_NPC_MAP.get(worldKey);
        if (housingMap != null) {
            housingMap.remove(oldHousingPos);
        }
        Set<Vector3i> occupied = OCCUPIED_HOUSINGS.get(worldKey);
        if (occupied != null) {
            occupied.remove(oldHousingPos);
        }

        NPCSpawnData newData = new NPCSpawnData(npcTypeId, newHousingPos);
        newData.setEntityUuid(existingData.getEntityUuid());
        newData.setAlive(true);
        newData.setDeathTimestamp(0L);

        HOUSING_NPC_MAP.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>()).put(newHousingPos, newData);
        NPC_TYPE_MAP.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>()).put(npcTypeId, newData);
        OCCUPIED_HOUSINGS.computeIfAbsent(worldKey, k -> ConcurrentHashMap.newKeySet()).add(newHousingPos);

        saveToDisk();
        return true;
    }


    public static void tryImmediateHousingAssignment(@Nonnull String worldKey, @Nonnull String homelessNpcType, @Nonnull Set<Vector3i> validHousings) {
        Set<Vector3i> unoccupiedHousings = getUnoccupiedHousings(worldKey, validHousings);
        if (!unoccupiedHousings.isEmpty()) {
            Vector3i freeHousing = unoccupiedHousings.iterator().next();
            assignHousingToHomelessNpc(worldKey, homelessNpcType, freeHousing);
        }
    }

    private static class SavedNPCData {
        String npcTypeId;
        int housingX;
        int housingY;
        int housingZ;
        boolean alive;
        long deathTimestamp;
        String entityUuid;
    }
}


