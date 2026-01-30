package de.fevzi.TerrariaAddons.items.coinPouch;

/**
 * Shared coin constants and helpers for coin pouch and coin-based shops.
 */
public final class CoinPouchCurrency {
    public static final String COPPER_COIN_ID = "Ingredient_Coin_Copper";
    public static final String SILVER_COIN_ID = "Ingredient_Coin_Silver";
    public static final String GOLD_COIN_ID = "Ingredient_Coin_Gold";
    public static final String PLATINUM_COIN_ID = "Ingredient_Coin_Platinum";

    public static final int COPPER_VALUE = 1;
    public static final int SILVER_VALUE = 100;
    public static final int GOLD_VALUE = 10000;
    public static final int PLATINUM_VALUE = 1000000;
    public static final int CONVERSION_RATE = 100;

    private CoinPouchCurrency() {
    }

    public static boolean isCoin(String itemId) {
        return COPPER_COIN_ID.equals(itemId) ||
               SILVER_COIN_ID.equals(itemId) ||
               GOLD_COIN_ID.equals(itemId) ||
               PLATINUM_COIN_ID.equals(itemId);
    }

    public static int getValueInCopper(String itemId, int quantity) {
        if (quantity <= 0) {
            return 0;
        }
        if (COPPER_COIN_ID.equals(itemId)) {
            return quantity * COPPER_VALUE;
        }
        if (SILVER_COIN_ID.equals(itemId)) {
            return quantity * SILVER_VALUE;
        }
        if (GOLD_COIN_ID.equals(itemId)) {
            return quantity * GOLD_VALUE;
        }
        if (PLATINUM_COIN_ID.equals(itemId)) {
            return quantity * PLATINUM_VALUE;
        }
        return 0;
    }

    public static String formatCopper(int totalCopper) {
        if (totalCopper <= 0) {
            return "0c";
        }
        int remaining = totalCopper;
        int platinum = remaining / PLATINUM_VALUE;
        remaining %= PLATINUM_VALUE;
        int gold = remaining / GOLD_VALUE;
        remaining %= GOLD_VALUE;
        int silver = remaining / SILVER_VALUE;
        remaining %= SILVER_VALUE;
        int copper = remaining;

        StringBuilder sb = new StringBuilder();
        if (platinum > 0) {
            sb.append(platinum).append('p');
        }
        if (gold > 0) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(gold).append('g');
        }
        if (silver > 0) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(silver).append('s');
        }
        if (copper > 0 || sb.length() == 0) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(copper).append('c');
        }
        return sb.toString();
    }
}
