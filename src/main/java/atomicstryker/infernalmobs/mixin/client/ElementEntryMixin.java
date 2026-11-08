package atomicstryker.infernalmobs.mixin.client;

import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import me.shedaniel.clothconfig2.gui.widget.DynamicElementListWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cloth Config unfocuses then refocuses the focused child when it is focused again, but refocusing a list cell
 * does not restore the focus inside it. Clicking a loot list row that already had the focus
 * (see {@link atomicstryker.infernalmobs.client.LootListGuiProvider}) unfocused its text field.
 * Nested lists now keep their focused cell as it is.
 */
@Mixin(DynamicElementListWidget.ElementEntry.class)
public abstract class ElementEntryMixin {

    @Shadow
    public abstract GuiEventListener getFocused();

    @Inject(method = "setFocused(Lnet/minecraft/client/gui/components/events/GuiEventListener;)V", at = @At("HEAD"), cancellable = true)
    private void infernalmobs$keepFocusedCell(GuiEventListener listener, CallbackInfo ci) {
        if ((Object) this instanceof NestedListListEntry<?, ?> && listener != null && listener == getFocused()) {
            ci.cancel();
        }
    }
}
