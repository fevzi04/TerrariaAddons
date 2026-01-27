package de.fevzi.TerrariaAddons.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;



/**
 * Manages persistent player data storage.
 * Handles loading, saving, and caching of player-specific data such as
 * Life Crystal and Mana Crystal usage counts. Data is persisted to disk
 * as JSON files.
 */
public class PlayerDataManager {
    private static final String DATA_DIR = "PlayerData";
    private static final Map<UUID, PlayerData> PLAYER_DATA_CACHE = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    static {
        loadAllPlayerData();
    }

    public static PlayerData getPlayerData(UUID playerUUID) {
        return PLAYER_DATA_CACHE.computeIfAbsent(playerUUID, PlayerDataManager::loadPlayerData);
    }

 
    public static void savePlayerData(UUID playerUUID, PlayerData data) {
        PLAYER_DATA_CACHE.put(playerUUID, data);
        savePlayerDataToDisk(playerUUID, data);
    }

    public static void updatePlayerData(UUID playerUUID, PlayerDataUpdater updater) {
        PlayerData data = getPlayerData(playerUUID);
        updater.update(data);
        savePlayerData(playerUUID, data);
    }

    private static void loadAllPlayerData() {
        try {
            Path dataDir = Paths.get(System.getProperty("user.dir"), DATA_DIR);
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
                return;
            }

            Files.walk(dataDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(PlayerDataManager::loadPlayerDataFromFile);
        } catch (Exception e) {
        }
    }

    private static PlayerData loadPlayerData(UUID playerUUID) {
        return loadPlayerDataFromFile(getPlayerDataFile(playerUUID));
    }

    private static PlayerData loadPlayerDataFromFile(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                return new PlayerData();
            }

            String json = Files.readString(filePath, StandardCharsets.UTF_8);
            PlayerData data = GSON.fromJson(json, PlayerData.class);

            return data;
        } catch (Exception e) {
            return new PlayerData();
        }
    }

    private static void savePlayerDataToDisk(UUID playerUUID, PlayerData data) {
        try {
            Path dataDir = Paths.get(System.getProperty("user.dir"), DATA_DIR);
            Files.createDirectories(dataDir);

            Path filePath = getPlayerDataFile(playerUUID);
            String json = GSON.toJson(data);
            Files.writeString(filePath, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
        }
    }

    private static Path getPlayerDataFile(UUID playerUUID) {
        Path dataDir = Paths.get(System.getProperty("user.dir"), DATA_DIR);
        return dataDir.resolve(playerUUID.toString() + ".json");
    }


    @FunctionalInterface
    public interface PlayerDataUpdater {
        void update(PlayerData data);
    }
}