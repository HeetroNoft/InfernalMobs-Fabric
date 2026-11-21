package atomicstryker.infernalmobs.client;

import fr.heetstudio.confee.api.SuggestionProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * Suggestions of the "entity tags" blacklist: the scoreboard tags ({@code /tag}) carried by the entities of the loaded
 * world. Confee calls it on the client when the config screen opens; from the title screen there is no world, Confee
 * then tells the player to open one for more suggestions.
 */
public class EntityTagSuggestions implements SuggestionProvider {

    @Override
    public Collection<String> suggestions() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return Set.of();
        }
        Set<String> tags = new TreeSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            tags.addAll(entity.entityTags());
        }
        return tags;
    }

    @Override
    public boolean needsWorld() {
        return true;
    }
}
