package de.fevzi.TerrariaAddons.npc.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import de.fevzi.TerrariaAddons.housing.HousingRegistrySystem;
import de.fevzi.TerrariaAddons.housing.npc.NPCSpawnDataManager;
import de.fevzi.TerrariaAddons.npc.NPCSpawnManager;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

public class NPCDebugCommand extends CommandBase {

    public NPCDebugCommand() {
        super("npcstatus", "Shows NPC spawn status information");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        Map<String, Set<com.hypixel.hytale.math.vector.Vector3i>> allHousings = HousingRegistrySystem.snapshotValidHousings();

        if (allHousings.isEmpty()) {
            context.sendMessage(Message.raw("=== NPC Status ==="));
            context.sendMessage(Message.raw("No valid housings registered in any world."));
            return;
        }

        context.sendMessage(Message.raw("=== NPC Status ==="));

        for (Map.Entry<String, Set<com.hypixel.hytale.math.vector.Vector3i>> entry : allHousings.entrySet()) {
            String worldKey = entry.getKey();
            Set<com.hypixel.hytale.math.vector.Vector3i> validHousings = entry.getValue();

            var unoccupied = NPCSpawnDataManager.getUnoccupiedHousings(worldKey, validHousings);
            var onCooldown = NPCSpawnDataManager.getNpcsOnCooldown(worldKey, NPCSpawnManager.RESPAWN_COOLDOWN_MS);
            var available = NPCSpawnManager.getAllAvailableNpcTypes(worldKey);

            context.sendMessage(Message.raw("World: " + worldKey));
            context.sendMessage(Message.raw("  Valid housings: " + validHousings.size()));
            context.sendMessage(Message.raw("  Unoccupied housings: " + unoccupied.size()));
            context.sendMessage(Message.raw("  NPCs on cooldown: " + onCooldown.size()));
            context.sendMessage(Message.raw("  Available NPC types: " + available.size()));

            if (!onCooldown.isEmpty()) {
                context.sendMessage(Message.raw("  On cooldown: " + String.join(", ", onCooldown)));
            }
        }
    }
}
