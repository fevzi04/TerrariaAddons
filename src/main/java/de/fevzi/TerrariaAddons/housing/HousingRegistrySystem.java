package de.fevzi.TerrariaAddons.housing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;


public class HousingRegistrySystem extends EntityTickingSystem<EntityStore> {
    private static final String DATA_DIR = "HousingData";
    private static final String DATA_FILE = "valid_housings.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private static final ConcurrentMap<String, Set<Vector3i>> VALID_HOUSINGS = new ConcurrentHashMap<>();
    private static final Object SAVE_LOCK = new Object();

    static {
        loadValidHousingsFromDisk();
    }

    public static boolean registerValidHousing(World world, Vector3i checkerPos) {
        String worldKey = worldKey(world);
        if (worldKey == null || checkerPos == null) {
            return false;
        }
        Set<Vector3i> housings = VALID_HOUSINGS.computeIfAbsent(worldKey, key -> ConcurrentHashMap.newKeySet());
        boolean added = housings.add(checkerPos);
        if (added) {
            saveValidHousingsToDisk();
        }
        return added;
    }

    public static boolean unregisterHousing(World world, Vector3i checkerPos) {
        String worldKey = worldKey(world);
        if (worldKey == null || checkerPos == null) {
            return false;
        }
        Set<Vector3i> housings = VALID_HOUSINGS.get(worldKey);
        if (housings == null) {
            return false;
        }
        boolean removed = housings.remove(checkerPos);
        if (removed && housings.isEmpty()) {
            VALID_HOUSINGS.remove(worldKey, housings);
        }
        if (removed) {
            saveValidHousingsToDisk();
        }
        return removed;
    }

    public static boolean updateHousing(World world, Vector3i checkerPos, boolean valid) {
        if (valid) {
            return registerValidHousing(world, checkerPos);
        }
        return unregisterHousing(world, checkerPos);
    }

    public static Set<Vector3i> getValidHousings(World world) {
        String worldKey = worldKey(world);
        if (worldKey == null) {
            return Collections.emptySet();
        }
        Set<Vector3i> housings = VALID_HOUSINGS.get(worldKey);
        if (housings == null || housings.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(housings));
    }

    public static Vector3i pickRandom(World world) {
        String worldKey = worldKey(world);
        if (worldKey == null) {
            return null;
        }
        Set<Vector3i> housings = VALID_HOUSINGS.get(worldKey);
        if (housings == null || housings.isEmpty()) {
            return null;
        }
        int targetIndex = ThreadLocalRandom.current().nextInt(housings.size());
        Iterator<Vector3i> iterator = housings.iterator();
        Vector3i current = null;
        for (int i = 0; i <= targetIndex; i++) {
            current = iterator.next();
        }
        return current;
    }

    public static Map<String, Set<Vector3i>> snapshotValidHousings() {
        if (VALID_HOUSINGS.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Set<Vector3i>> snapshot = new HashMap<>();
        VALID_HOUSINGS.forEach((world, housings) -> {
            if (!housings.isEmpty()) {
                snapshot.put(world, Collections.unmodifiableSet(new HashSet<>(housings)));
            }
        });
        return snapshot.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(snapshot);
    }

    private static String worldKey(World world) {
        if (world == null) {
            return null;
        }
        String name = world.getName();
        return (name == null || name.isEmpty()) ? null : name;
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
    }

    private static void loadValidHousingsFromDisk() {
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
            Type type = new TypeToken<Map<String, List<SavedHousing>>>() {}.getType();
            Map<String, List<SavedHousing>> raw = GSON.fromJson(json, type);
            if (raw == null || raw.isEmpty()) {
                return;
            }

            raw.forEach((world, list) -> {
                if (world == null || world.isBlank() || list == null || list.isEmpty()) {
                    return;
                }
                Set<Vector3i> set = ConcurrentHashMap.newKeySet();
                for (SavedHousing saved : list) {
                    if (saved != null) {
                        set.add(new Vector3i(saved.x, saved.y, saved.z));
                    }
                }
                if (!set.isEmpty()) {
                    VALID_HOUSINGS.put(world, set);
                }
            });
        } catch (Exception e) {
        }
    }

    private static void saveValidHousingsToDisk() {
        synchronized (SAVE_LOCK) {
            try {
                Path dataDir = Paths.get(System.getProperty("user.dir"), DATA_DIR);
                Files.createDirectories(dataDir);

                Map<String, List<SavedHousing>> snapshot = new HashMap<>();
                VALID_HOUSINGS.forEach((world, housings) -> {
                    if (housings == null || housings.isEmpty()) {
                        return;
                    }
                    List<SavedHousing> list = new ArrayList<>(housings.size());
                    for (Vector3i pos : housings) {
                        list.add(new SavedHousing(pos.x, pos.y, pos.z));
                    }
                    snapshot.put(world, list);
                });

                Path filePath = dataDir.resolve(DATA_FILE);
                String json = GSON.toJson(snapshot);
                Files.writeString(filePath, json, StandardCharsets.UTF_8);
            } catch (Exception e) {
            }
        }
    }

    private static final class SavedHousing {
        private final int x;
        private final int y;
        private final int z;

        private SavedHousing(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
