package de.fevzi.TerrariaAddons.items.potions.wormhole;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceBasePage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceElement;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.World;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import javax.annotation.Nonnull;
import java.util.Collection;

public class PlayerTeleportPage extends ChoiceBasePage {

    private static final String UI_LAYOUT = "Pages/PlayerTeleportPage.ui";

    public PlayerTeleportPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, createPlayerList(playerRef), UI_LAYOUT);
    }

    private static ChoiceElement[] createPlayerList(PlayerRef self) {
        ObjectArrayList<ChoiceElement> buttons = new ObjectArrayList<>();

        World selfWorld = null;
        if (self.isValid()) {
            Store<EntityStore> selfStore = self.getReference().getStore();
            if (selfStore != null) {
                selfWorld = ((EntityStore) selfStore.getExternalData()).getWorld();
            }
        }

        if (selfWorld == null) return new ChoiceElement[0];

        Collection<PlayerRef> allPlayers = Universe.get().getPlayers();

        for (PlayerRef target : allPlayers) {

            if (target.isValid() && !target.equals(self)) {


                Store<EntityStore> targetStore = target.getReference().getStore();
                if (targetStore == null) continue;

                World targetWorld = ((EntityStore) targetStore.getExternalData()).getWorld();

                if (!selfWorld.equals(targetWorld)) {
                    continue;
                }

                String displayName = target.getUsername().toString();

                TeleportInteraction action = new TeleportInteraction(target.getReference());
                buttons.add(new PlayerTeleportElement(displayName, action));
            }
        }

        return buttons.toArray(new ChoiceElement[0]);
    }
}