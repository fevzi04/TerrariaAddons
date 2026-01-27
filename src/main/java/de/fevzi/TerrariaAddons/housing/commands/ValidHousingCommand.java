package de.fevzi.TerrariaAddons.housing.commands;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import de.fevzi.TerrariaAddons.housing.HousingRegistrySystem;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

/**
 * Command that lists all valid housing locations.
 * Displays a formatted list of all registered valid housings
 * grouped by world, showing their coordinates.
 */
public class ValidHousingCommand extends CommandBase {

    public ValidHousingCommand() {
        super("validhousings", "Lists every currently valid housing location");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        Map<String, Set<Vector3i>> snapshot = HousingRegistrySystem.snapshotValidHousings();
        if (snapshot.isEmpty()) {
            context.sendMessage(Message.raw("No valid housings registered."));
            return;
        }

        snapshot.forEach((world, housings) -> {
            context.sendMessage(Message.raw("World '" + world + "' has " + housings.size() + " valid housings:"));
            housings.forEach(pos -> context.sendMessage(Message.raw("  - " + formatPosition(pos))));
        });
    }

    private static String formatPosition(Vector3i pos) {
        if (pos == null) {
            return "unknown position";
        }
        return "x=" + pos.x + ", y=" + pos.y + ", z=" + pos.z;
    }
}
