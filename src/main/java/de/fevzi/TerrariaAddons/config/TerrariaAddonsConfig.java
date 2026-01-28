package de.fevzi.TerrariaAddons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration manager for TerrariaAddons plugin.
 * Handles loading and saving of configuration values from/to config.json.
 */
public class TerrariaAddonsConfig {

    private static TerrariaAddonsConfig instance;
    private static final String CONFIG_FILE_NAME = "terraria_addons_config.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeSpecialFloatingPointValues()
            .create();

    private ConfigData configData;
    private File configFile;

    private TerrariaAddonsConfig() {
        loadConfig();
    }


    public static TerrariaAddonsConfig getInstance() {
        if (instance == null) {
            instance = new TerrariaAddonsConfig();
        }
        return instance;
    }


    private void loadConfig() {
        try {
            Path runDir = Paths.get("run");
            if (!Files.exists(runDir)) {
                runDir = Paths.get(".");
            }

            configFile = runDir.resolve(CONFIG_FILE_NAME).toFile();

            if (!configFile.exists()) {
                configData = new ConfigData();
                saveConfig();
                System.out.println("[TerrariaAddons] Created default configuration file: " + configFile.getAbsolutePath());
            } else {
                try (FileReader reader = new FileReader(configFile)) {
                    configData = GSON.fromJson(reader, ConfigData.class);
                    if (configData == null) {
                        configData = new ConfigData();
                    }
                    if (configData.coinItemId == null || configData.coinItemId.isEmpty() ||
                        configData.coinItemId.equals("Currency_Coin") || configData.coinItemId.equals("Ingredient_Coin")) {
                        if (configData.coinItemId != null && (configData.coinItemId.equals("Currency_Coin") || configData.coinItemId.equals("Ingredient_Coin"))) {
                            System.out.println("[TerrariaAddons] Migrated coinItemId to Ingredient_Coin_Copper (coins now always drop as copper)");
                        }
                        configData.coinItemId = "Ingredient_Coin_Copper";
                    }
                    System.out.println("[TerrariaAddons] Loaded configuration from: " + configFile.getAbsolutePath());
                    saveConfig();
                } catch (Exception e) {
                    System.err.println("[TerrariaAddons] Error reading config file, using defaults: " + e.getMessage());
                    configData = new ConfigData();
                    saveConfig();
                }
            }
        } catch (Exception e) {
            System.err.println("[TerrariaAddons] Error initializing config: " + e.getMessage());
            configData = new ConfigData();
        }
    }


    public void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(configData, writer);
            System.out.println("[TerrariaAddons] Configuration saved to: " + configFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[TerrariaAddons] Error saving config: " + e.getMessage());
        }
    }


    public void reloadConfig() {
        loadConfig();
    }



    public double getFallingStarsSpawnRate() {
        return configData.fallingStarsSpawnRatePercent / 100.0;
    }


    public void setFallingStarsSpawnRate(double percentRate) {
        configData.fallingStarsSpawnRatePercent = percentRate;
        saveConfig();
    }

    public double getFallingStarsSpawnRange() {
        return configData.fallingStarsSpawnRange;
    }

    public void setFallingStarsSpawnRange(double range) {
        configData.fallingStarsSpawnRange = range;
        saveConfig();
    }

    public String getCoinItemId() {
        return configData.coinItemId;
    }

    public void setCoinItemId(String coinItemId) {
        configData.coinItemId = coinItemId;
        saveConfig();
    }

    public int getMinCoinsPerKill() {
        return configData.minCoinsPerKill;
    }

    public void setMinCoinsPerKill(int minCoins) {
        configData.minCoinsPerKill = minCoins;
        saveConfig();
    }

    public int getMaxCoinsPerKill() {
        return configData.maxCoinsPerKill;
    }

    public void setMaxCoinsPerKill(int maxCoins) {
        configData.maxCoinsPerKill = maxCoins;
        saveConfig();
    }

    public float getCoinsPerHealth() {
        return configData.coinsPerHealth;
    }

    public void setCoinsPerHealth(float coinsPerHealth) {
        configData.coinsPerHealth = coinsPerHealth;
        saveConfig();
    }


    private static class ConfigData {

        private ConfigDescription _description = new ConfigDescription();

        private double fallingStarsSpawnRatePercent = 0.04;

        private double fallingStarsSpawnRange = 50.0;

        private String coinItemId = "Ingredient_Coin_Copper";

        private int minCoinsPerKill = 1;

        private int maxCoinsPerKill = 5;

        private float coinsPerHealth = 10.0f;

        public ConfigData() {
        }
    }


    private static class ConfigDescription {
        private String info = "TerrariaAddons Configuration File - Edit values below";
        private String fallingStarsSpawnRatePercent = "Spawn rate as percentage per tick";
        private String fallingStarsSpawnRange = "Radius in blocks around player where falling stars can spawn";
        private String coinItemId = "Item ID for the coin currency to drop from slain mobs";
        private String minCoinsPerKill = "Minimum number of coins dropped per kill (fallback if health unavailable)";
        private String maxCoinsPerKill = "Maximum number of coins dropped per kill (fallback if health unavailable)";
        private String coinsPerHealth = "Health points per coin (e.g., 10.0 = 1 coin per 10 max health)";

        public ConfigDescription() {
        }
    }
}
