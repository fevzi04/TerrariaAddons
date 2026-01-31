package de.fevzi.TerrariaAddons.npc.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.fevzi.TerrariaAddons.housing.npc.NPCSpawnData;
import de.fevzi.TerrariaAddons.housing.npc.NPCSpawnDataManager;
import de.fevzi.TerrariaAddons.npc.NPCSpawnManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NPCKillCommand extends CommandBase {
    private final RequiredArg<String> npcArg;

    public NPCKillCommand() {
        super("killnpc", "Kills a TerrariaAddons NPC by type");
        this.npcArg = withRequiredArg("npc", "NPC type (e.g. Merchant) or 'all'", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String raw = context.get(npcArg);
        if (raw == null || raw.isBlank()) {
            context.sendMessage(Message.raw("Usage: /killnpc <npcType|all>"));
            return;
        }

        List<String> targetTypes = resolveNpcTypes(raw);
        if (targetTypes.isEmpty()) {
            context.sendMessage(Message.raw("Unknown NPC type: " + raw));
            return;
        }

        Collection<World> worlds = resolveTargetWorlds(context);
        if (worlds.isEmpty()) {
            context.sendMessage(Message.raw("No worlds available."));
            return;
        }

        int killed = 0;
        for (World world : worlds) {
            String worldKey = world.getName();
            if (worldKey == null || worldKey.isEmpty()) {
                continue;
            }

            for (String npcType : targetTypes) {
                NPCSpawnData data = NPCSpawnDataManager.getNpcByType(worldKey, npcType);
                if (data == null || !data.isAlive()) {
                    continue;
                }

                UUID uuid = data.getEntityUuid();
                world.execute(() -> {
                    EntityStore entityStore = world.getEntityStore();
                    Store<EntityStore> store = entityStore.getStore();
                    if (uuid != null) {
                        Ref<EntityStore> ref = entityStore.getRefFromUUID(uuid);
                        if (ref != null && ref.isValid()) {
                            store.removeEntity(ref, RemoveReason.REMOVE);
                        }
                    }
                    NPCSpawnDataManager.markNpcDead(worldKey, npcType);
                });
                killed++;
            }
        }

        if (killed == 0) {
            context.sendMessage(Message.raw("No matching alive NPCs found."));
        } else {
            context.sendMessage(Message.raw("Killed " + killed + " NPC(s)."));
        }
    }

    private static List<String> resolveNpcTypes(@Nonnull String raw) {
        String normalized = raw.trim();
        if ("all".equalsIgnoreCase(normalized)) {
            return new ArrayList<>(NPCSpawnManager.NPC_TYPES);
        }

        for (String npcType : NPCSpawnManager.NPC_TYPES) {
            if (npcType.equalsIgnoreCase(normalized)) {
                return List.of(npcType);
            }
        }
        return List.of();
    }

    private static Collection<World> resolveTargetWorlds(@Nonnull CommandContext context) {
        if (context.isPlayer()) {
            Ref<EntityStore> playerRef = context.senderAsPlayerRef();
            if (playerRef != null && playerRef.isValid()) {
                Store<EntityStore> store = playerRef.getStore();
                EntityStore entityStore = store.getExternalData();
                if (entityStore != null && entityStore.getWorld() != null) {
                    return List.of(entityStore.getWorld());
                }
            }
        }

        Map<String, World> all = Universe.get().getWorlds();
        return all != null ? all.values() : List.of();
    }
}
