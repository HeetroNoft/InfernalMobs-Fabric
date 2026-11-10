package atomicstryker.infernalmobs.mixin.client;

import me.shedaniel.clothconfig2.api.ReferenceProvider;
import me.shedaniel.clothconfig2.gui.entries.BaseListEntry;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/**
 * Cloth Config only calls lateRender on top level entries, so a dropdown nested in a list
 * (the loot lists, see {@link atomicstryker.infernalmobs.client.LootListGuiProvider}) never draws its suggestions.
 * This forwards lateRender to the entries nested in the list cells.
 */
@Mixin(BaseListEntry.class)
public abstract class BaseListEntryMixin {

    @Shadow
    @Final
    protected List<?> cells;

    @Shadow
    public abstract boolean isExpanded();

    public void lateRender(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!isExpanded()) {
            return;
        }
        for (Object cell : cells) {
            if (cell instanceof ReferenceProvider<?> provider) {
                provider.provideReferenceEntry().lateRender(graphics, mouseX, mouseY, delta);
            }
        }
    }
}
