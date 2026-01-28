package de.fevzi.TerrariaAddons.data;

/**
 * Data class storing persistent player information.
 * Tracks usage counts for permanent stat-boosting items like
 * Life Crystals and Mana Crystals, and coin pouch data.
 */
public class PlayerData {
    private int lifeCrystalUses = 0;
    private int manaCrystalUses = 0;
    
    private int coinPouchCapacity = 4;
    private String coinPouchItemsJson = "[]";
    
    public int getLifeCrystalUses() {
        return lifeCrystalUses;
    }
    public void setLifeCrystalUses(int lifeCrystalUses) {
        this.lifeCrystalUses = lifeCrystalUses;
    }
    public int getManaCrystalUses() {
        return manaCrystalUses;
    }
    public void setManaCrystalUses(int manaCrystalUses) { 
        this.manaCrystalUses = manaCrystalUses; 
    }
    
    public int getCoinPouchCapacity() {
        return coinPouchCapacity;
    }
    public void setCoinPouchCapacity(int coinPouchCapacity) {
        this.coinPouchCapacity = coinPouchCapacity;
    }
    public String getCoinPouchItemsJson() {
        if (coinPouchItemsJson == null) {
            coinPouchItemsJson = "[]";
        }
        return coinPouchItemsJson;
    }
    public void setCoinPouchItemsJson(String coinPouchItemsJson) {
        this.coinPouchItemsJson = coinPouchItemsJson;
    }
}
