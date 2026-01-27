package de.fevzi.TerrariaAddons.data;

/**
 * Data class storing persistent player information.
 * Tracks usage counts for permanent stat-boosting items like
 * Life Crystals and Mana Crystals.
 */
public class PlayerData {
    private int lifeCrystalUses = 0;
    private int manaCrystalUses = 0;
    public int getLifeCrystalUses() {
        return lifeCrystalUses;
    }
    public void setLifeCrystalUses(int lifeCrystalUses) {
        this.lifeCrystalUses = lifeCrystalUses;
    }
    public int getManaCrystalUses() {
        return manaCrystalUses;
    }
    public void setManaCrystalUses(int manaCrystalUses) { this.manaCrystalUses = manaCrystalUses; }
}
