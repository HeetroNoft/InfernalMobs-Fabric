package atomicstryker.infernalmobs.client;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shedaniel.clothconfig2.api.Tooltip;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import me.shedaniel.math.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * One loot list entry in the config screen, editing an item stack SNBT string as three fields:
 * the item (in game name, with suggestions), the additional SNBT (everything but id and count) and the count.
 * Used by {@link LootListGuiProvider}, the suggestions are drawn in lateRender, see
 * {@link atomicstryker.infernalmobs.mixin.client.BaseListEntryMixin}
 */
public class LootStackEntry extends TooltipListEntry<String> {

    private static final String ERROR_KEY = "text.autoconfig.InfernalMobs-common.error.";
    private static final int ICON_WIDTH = 20;
    private static final int COUNT_WIDTH = 36;
    private static final int GAP = 4;
    private static final int SUGGESTION_HEIGHT = 18;
    private static final int MAX_SUGGESTIONS_SHOWN = 8;
    private static final int MAX_MATCHES = 200;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int ERROR_COLOR = 0xFFFF5555;

    /**
     * an item as offered in the suggestions, searchable by its in game name and its id.
     * The item components are only bound once a world is loaded: from the title screen there is no stack
     * (no icon) and the name comes from the item's translation key.
     */
    public record ItemChoice(Item item, @Nullable ItemStack stack, String name, String id, String searchKey) {

        public static List<ItemChoice> all() {
            return BuiltInRegistries.ITEM.stream()
                    .filter(item -> item != Items.AIR)
                    .map(ItemChoice::of)
                    .sorted(Comparator.comparing(ItemChoice::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }

        private static ItemChoice of(Item item) {
            ItemStack stack = item.builtInRegistryHolder().areComponentsBound() ? new ItemStack(item) : null;
            String name = stack != null ? stack.getHoverName().getString() : Component.translatable(item.getDescriptionId()).getString();
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            return new ItemChoice(item, stack, name, id, (name + " " + id).toLowerCase(Locale.ROOT));
        }
    }

    private final List<ItemChoice> choices;
    private final Font font = Minecraft.getInstance().font;
    private final EditBox itemField;
    private final EditBox extrasField;
    private final EditBox countField;
    private final List<EditBox> fields;

    // the value as read from the config, returned unchanged as long as no field is edited
    private final String original;
    private final String originalItemText;
    private final String originalExtras;
    private final String originalCount;

    private ItemChoice selected;
    private boolean showSuggestions;
    private List<ItemChoice> matches = List.of();
    private int scroll;
    private int highlighted;
    private int popupX;
    private int popupY;
    private int popupWidth;

    public LootStackEntry(String value, List<ItemChoice> choices) {
        super(Component.empty(), null);
        this.choices = choices;
        this.original = value;

        String id = "";
        String count = "1";
        String extras = "";
        try {
            CompoundTag tag = TagParser.parseCompoundFully(value);
            id = tag.getStringOr("id", "");
            count = String.valueOf(tag.getIntOr("count", 1));
            tag.remove("id");
            tag.remove("count");
            extras = tag.isEmpty() ? "" : tag.toString();
        } catch (CommandSyntaxException e) {
            // keep the broken text visible so it can be fixed
            extras = value;
        }
        selected = findChoice(id);

        itemField = createField(selected != null ? selected.name() : id, "item");
        extrasField = createField(extras, "extras");
        countField = createField(count, "count");
        fields = List.of(itemField, extrasField, countField);
        originalItemText = itemField.getValue();
        originalExtras = extras;
        originalCount = count;

        itemField.setResponder(this::onItemTextChanged);
        showSuggestions = id.isEmpty();
        updateMatches(itemField.getValue());
        setTooltipSupplier(this::getRowTooltip);
    }

    private EditBox createField(String text, String hint) {
        EditBox field = new EditBox(font, 0, 0, 100, 18, Component.empty()) {
            @Override
            public void setFocused(boolean focused) {
                // a field losing the focus drops its selection, the cursor stays where it is
                if (!focused && isFocused()) {
                    setHighlightPos(getCursorPosition());
                }
                super.setFocused(focused);
            }
        };
        field.setMaxLength(Integer.MAX_VALUE);
        field.setValue(text);
        // setValue leaves the cursor and the selection start at the end, this clears the selection too
        field.moveCursorToStart(false);
        field.setHint(Component.translatable("text.autoconfig.InfernalMobs-common.loot." + hint));
        return field;
    }

    private ItemChoice findChoice(String text) {
        Identifier identifier = Identifier.tryParse(text);
        String id = identifier != null ? identifier.toString() : text;
        for (ItemChoice choice : choices) {
            if (choice.id().equals(id) || choice.name().equalsIgnoreCase(text)) {
                return choice;
            }
        }
        return null;
    }

    private void onItemTextChanged(String text) {
        selected = findChoice(text);
        showSuggestions = true;
        updateMatches(text);
    }

    private void updateMatches(String text) {
        String search = text.toLowerCase(Locale.ROOT).trim();
        matches = choices.stream().filter(choice -> choice.searchKey().contains(search)).limit(MAX_MATCHES).toList();
        scroll = 0;
        highlighted = 0;
    }

    private void select(ItemChoice choice) {
        itemField.setValue(choice.name());
        itemField.moveCursorToStart(false);
        // set after setValue, its responder looks the item up by name and two items can share one
        selected = choice;
        showSuggestions = false;
    }

    private boolean isPopupOpen() {
        return showSuggestions && itemField.isFocused() && !matches.isEmpty();
    }

    private int shownSuggestions() {
        return Math.min(MAX_SUGGESTIONS_SHOWN, matches.size());
    }

    private boolean isOverPopup(double mouseX, double mouseY) {
        return isPopupOpen() && mouseX >= popupX && mouseX < popupX + popupWidth
                && mouseY >= popupY && mouseY < popupY + shownSuggestions() * SUGGESTION_HEIGHT;
    }

    @Override
    public String getValue() {
        if (itemField.getValue().equals(originalItemText) && extrasField.getValue().equals(originalExtras)
                && countField.getValue().equals(originalCount)) {
            return original;
        }
        String id = selected != null ? selected.id() : itemField.getValue().trim();
        String extras = extrasField.getValue().trim();
        if (extras.startsWith("{") && extras.endsWith("}")) {
            extras = extras.substring(1, extras.length() - 1).trim();
        }
        return "{id:'" + id + "',count:" + countField.getValue().trim() + (extras.isEmpty() ? "" : "," + extras) + "}";
    }

    @Override
    public Optional<String> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public Optional<Component> getError() {
        if (selected == null) {
            return Optional.of(itemField.getValue().isBlank() ? Component.translatable(ERROR_KEY + "noItem")
                    : Component.translatable(ERROR_KEY + "unknownItem", itemField.getValue()));
        }
        if (!isValidCount()) {
            return Optional.of(Component.translatable(ERROR_KEY + "invalidCount"));
        }
        if (!isValidExtras()) {
            return Optional.of(Component.translatable(ERROR_KEY + "invalidSnbt"));
        }
        return Optional.empty();
    }

    private boolean isValidCount() {
        try {
            return Integer.parseInt(countField.getValue().trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidExtras() {
        String extras = extrasField.getValue().trim();
        if (extras.isEmpty()) {
            return true;
        }
        try {
            TagParser.parseCompoundFully(extras);
            return true;
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    /**
     * a finished entry shows its full SNBT when hovered
     */
    private Optional<Component[]> getRowTooltip() {
        if (isPopupOpen() || getError().isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new Component[]{Component.literal(getValue())});
    }

    @Override
    public int getItemHeight() {
        return 22;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.extractRenderState(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        // same total width as the string list cells
        int width = entryWidth - 12;
        int textWidth = (width - ICON_WIDTH - COUNT_WIDTH - 2 * GAP) / 2;
        int itemX = x + ICON_WIDTH;
        int extrasX = itemX + textWidth + GAP;
        int countX = x + width - COUNT_WIDTH;

        if (selected != null && selected.stack() != null) {
            graphics.item(selected.stack(), x + 1, y + 2);
        }
        layout(itemField, itemX, y + 2, textWidth);
        layout(extrasField, extrasX, y + 2, countX - GAP - extrasX);
        layout(countField, countX, y + 2, COUNT_WIDTH);
        itemField.setTextColor(selected == null ? ERROR_COLOR : TEXT_COLOR);
        extrasField.setTextColor(isValidExtras() ? TEXT_COLOR : ERROR_COLOR);
        countField.setTextColor(isValidCount() ? TEXT_COLOR : ERROR_COLOR);
        for (EditBox field : fields) {
            field.setEditable(isEditable());
            field.extractRenderState(graphics, mouseX, mouseY, delta);
        }

        popupX = x;
        popupY = y + 21;
        popupWidth = Math.max(ICON_WIDTH + textWidth, 180);
    }

    private static void layout(EditBox field, int x, int y, int width) {
        field.setX(x);
        field.setY(y);
        field.setWidth(width);
    }

    @Override
    public void lateRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!isPopupOpen()) {
            return;
        }
        int shown = shownSuggestions();
        int height = shown * SUGGESTION_HEIGHT;
        graphics.fill(popupX - 1, popupY - 1, popupX + popupWidth + 1, popupY + height + 1, 0xFFA0A0A0);
        graphics.fill(popupX, popupY, popupX + popupWidth, popupY + height, 0xFF000000);
        for (int i = 0; i < shown; i++) {
            ItemChoice choice = matches.get(scroll + i);
            int rowY = popupY + i * SUGGESTION_HEIGHT;
            boolean hovered = mouseX >= popupX && mouseX < popupX + popupWidth && mouseY >= rowY && mouseY < rowY + SUGGESTION_HEIGHT;
            if (hovered || scroll + i == highlighted) {
                graphics.fill(popupX, rowY, popupX + popupWidth, rowY + SUGGESTION_HEIGHT, 0xFF404040);
            }
            if (choice.stack() != null) {
                graphics.item(choice.stack(), popupX + 1, rowY + 1);
            }
            graphics.text(font, font.plainSubstrByWidth(choice.name(), popupWidth - ICON_WIDTH - 2), popupX + ICON_WIDTH, rowY + 5, 0xFFFFFFFF);
            if (hovered) {
                addTooltip(Tooltip.of(new Point(mouseX, mouseY), Component.literal(choice.id())));
            }
        }
        if (matches.size() > shown) {
            // scroll bar
            int barHeight = Math.max(4, height * shown / matches.size());
            int barY = popupY + (height - barHeight) * scroll / (matches.size() - shown);
            graphics.fill(popupX + popupWidth - 2, barY, popupX + popupWidth, barY + barHeight, 0xFFC0C0C0);
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (isOverPopup(mouseX, mouseY)) {
            return true;
        }
        for (EditBox field : fields) {
            if (field.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (isOverPopup(event.x(), event.y())) {
            select(matches.get(scroll + (int) ((event.y() - popupY) / SUGGESTION_HEIGHT)));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isOverPopup(mouseX, mouseY)) {
            scroll = Math.clamp(scroll - (int) Math.signum(scrollY), 0, matches.size() - shownSuggestions());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isPopupOpen()) {
            if (event.isDown() || event.isUp()) {
                highlighted = Math.clamp(highlighted + (event.isDown() ? 1 : -1), 0, matches.size() - 1);
                // keep the highlighted suggestion in view
                scroll = Math.clamp(scroll, highlighted - shownSuggestions() + 1, highlighted);
                return true;
            }
            if (event.isConfirmation() || event.isCycleFocus()) {
                select(matches.get(highlighted));
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void updateSelected(boolean isSelected) {
        for (EditBox field : fields) {
            field.setFocused(isSelected && getFocused() == field);
        }
    }

    /**
     * Cloth Config unfocuses then refocuses an already focused child, which would deselect its text
     */
    @Override
    public void setFocused(GuiEventListener listener) {
        if (listener == null || listener != getFocused()) {
            super.setFocused(listener);
        }
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return fields;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return fields;
    }
}
