package de.fevzi.TerrariaAddons.items.ManaCrystal;

import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;

public final class ManaCrystalStats {
    public static final int MAX_USES = 10;
    public static final float MANA_PERCENT_PER_USE = 0.1f;
    public static final String MODIFIER_KEY = "mana_crystal_multiplier";

    private ManaCrystalStats() {
    }

    public static void apply(EntityStatMap stats, int uses) {
        if (stats == null) {
            return;
        }

        for (int i = 0; i < MAX_USES; i++) {
            stats.removeModifier(DefaultEntityStatTypes.getMana(), "mana_crystal_" + i);
        }

        stats.removeModifier(DefaultEntityStatTypes.getMana(), MODIFIER_KEY);

        if (uses <= 0) {
            return;
        }

        int clamped = Math.min(uses, MAX_USES);
        float multiplier = 1.0f + (MANA_PERCENT_PER_USE * clamped);

        StaticModifier manaModifier = new StaticModifier(
                Modifier.ModifierTarget.MAX,
                StaticModifier.CalculationType.MULTIPLICATIVE,
                multiplier
        );
        stats.putModifier(DefaultEntityStatTypes.getMana(), MODIFIER_KEY, manaModifier);
    }
}
