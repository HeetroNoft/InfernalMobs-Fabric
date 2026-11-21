package atomicstryker.infernalmobs.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.heetstudio.confee.api.Theme;
import fr.heetstudio.confee.api.client.ConfeeDraw;
import fr.heetstudio.confee.api.client.ConfeeIcons;
import fr.heetstudio.confee.api.client.OptionWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Confee widget of the loot lists (droppedItemIDs*): one line per item stack SNBT string, edited as three fields:
 * the item (in game name, with suggestions), the additional SNBT (everything but id and count) and the count.
 * <p>The list stays a list of SNBT strings in the file. A line that is not edited is written back unchanged.
 * While a line has an error, Confee shows the summary under the widget and "Save" is disabled. Empty new lines are
 * not saved.</p>
 * <p>The item suggestions are drawn as an overlay over the next options; the panel scrolls to the focused line only
 * ({@link #focusBounds()}).</p>
 */
public class LootListWidget extends OptionWidget<List<String>> {

    private static final String KEY = "infernalmobs.config.loot.";
    private static final int FIELD_H = 18;
    private static final int ROW_H = FIELD_H + 2;
    private static final int ERROR_H = 10;
    private static final int ICON_W = 20;
    private static final int COUNT_W = 34;
    private static final int REMOVE_W = 18;
    private static final int GAP = 3;
    private static final int SUGGESTION_H = 18;
    private static final int VISIBLE_SUGGESTIONS = 6;
    private static final int MAX_MATCHES = 200;
    private static final int ADD_H = 18;

    @Nullable
    private List<LootItemChoice> choices;
    private final List<Row> rows = new ArrayList<>();
    // the value last read or written, the lines are rebuilt when the value changes elsewhere (reset buttons)
    @Nullable
    private List<String> shown;

    @Nullable
    private Row focusedRow;
    @Nullable
    private EditBox focusedBox;

    private boolean suggestionsOpen;
    private List<LootItemChoice> matches = List.of();
    private int suggestionScroll;
    private int highlighted;

    // layout: y of the "+ Add" button relative to the widget, screen area of the suggestions (from the last frame)
    private int addY;
    private int[] suggestionArea = {0, 0, 0, 0};

    // ------------------------------------------------------------------ one line

    private final class Row {
        final EditBox itemBox;
        final EditBox extrasBox;
        final EditBox countBox;
        // the value as read from the config, written back unchanged as long as no field is edited
        final String original;
        final String originalItemText;
        final String originalExtras;
        final String originalCount;
        @Nullable
        LootItemChoice selected;
        // relative to the top of the widget
        int y;

        Row(String value) {
            original = value;
            String id = "";
            String count = "1";
            String extras = "";
            if (!value.isEmpty()) {
                try {
                    CompoundTag tag = TagParser.parseTag(value);
                    id = tag.getString("id");
                    // "Count" is the key of the 1.20.1 item stacks, entries written for this version may use it
                    count = String.valueOf(tag.contains("count") ? tag.getInt("count") : tag.contains("Count") ? tag.getByte("Count") : 1);
                    tag.remove("id");
                    tag.remove("count");
                    tag.remove("Count");
                    extras = tag.isEmpty() ? "" : tag.toString();
                } catch (CommandSyntaxException e) {
                    // keep the broken text visible so it can be fixed
                    extras = value;
                }
            }
            selected = LootItemChoice.find(choices(), id);
            itemBox = box(selected != null ? selected.name() : id, "item");
            extrasBox = box(extras, "extras");
            countBox = box(count, "count");
            originalItemText = itemBox.getValue();
            originalExtras = extras;
            originalCount = count;
            itemBox.setResponder(text -> {
                selected = LootItemChoice.find(choices(), text);
                openSuggestions(text);
                push();
            });
            extrasBox.setResponder(text -> push());
            countBox.setResponder(text -> push());
        }

        List<EditBox> boxes() {
            return List.of(itemBox, extrasBox, countBox);
        }

        boolean isBlank() {
            return itemBox.getValue().isBlank() && extrasBox.getValue().isBlank();
        }

        String value() {
            if (itemBox.getValue().equals(originalItemText) && extrasBox.getValue().equals(originalExtras)
                    && countBox.getValue().equals(originalCount)) {
                return original;
            }
            String id = selected != null ? selected.id() : itemBox.getValue().trim();
            String extras = extrasBox.getValue().trim();
            if (extras.startsWith("{") && extras.endsWith("}")) {
                extras = extras.substring(1, extras.length() - 1).trim();
            }
            return "{id:'" + id + "',count:" + countBox.getValue().trim() + (extras.isEmpty() ? "" : "," + extras) + "}";
        }

        /** Error of the line; an empty new line has none, it is not saved. */
        @Nullable
        Component error() {
            if (isBlank()) {
                return null;
            }
            if (selected == null) {
                return itemBox.getValue().isBlank() ? Component.translatable(KEY + "error.noItem")
                        : Component.translatable(KEY + "error.unknownItem", itemBox.getValue());
            }
            if (!validCount()) {
                return Component.translatable(KEY + "error.invalidCount");
            }
            if (!validExtras()) {
                return Component.translatable(KEY + "error.invalidSnbt");
            }
            return null;
        }

        boolean validCount() {
            try {
                return Integer.parseInt(countBox.getValue().trim()) > 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        boolean validExtras() {
            String extras = extrasBox.getValue().trim();
            if (extras.isEmpty()) {
                return true;
            }
            try {
                TagParser.parseTag(extras);
                return true;
            } catch (CommandSyntaxException e) {
                return false;
            }
        }

        int height() {
            return ROW_H + (error() != null ? ERROR_H : 0);
        }
    }

    private EditBox box(String text, String hint) {
        EditBox box = new EditBox(font(), 0, 0, 100, 9, Component.translatable(KEY + hint));
        box.setBordered(false);
        box.setMaxLength(Integer.MAX_VALUE);
        box.setValue(text);
        box.moveCursorToStart();
        box.setHint(Component.translatable(KEY + hint).withStyle(style -> style.withColor(theme().textDisabled())));
        return box;
    }

    private List<LootItemChoice> choices() {
        if (choices == null) {
            choices = LootItemChoice.all();
        }
        return choices;
    }

    // ------------------------------------------------------------------ value

    private void syncFromValue() {
        List<String> value = value();
        if (value.equals(shown)) {
            return;
        }
        rows.clear();
        focusedRow = null;
        focusedBox = null;
        suggestionsOpen = false;
        for (String entry : value) {
            rows.add(new Row(entry));
        }
        shown = List.copyOf(value);
    }

    private void push() {
        List<String> value = new ArrayList<>();
        for (Row row : rows) {
            if (!row.isBlank()) {
                value.add(row.value());
            }
        }
        if (setValue(value)) {
            shown = List.copyOf(value);
        }
    }

    /** Summary of the lines with an error; Confee shows it under the widget and disables "Save". */
    @Override
    @Nullable
    public Component error() {
        int invalid = 0;
        Component first = null;
        for (Row row : rows) {
            Component error = row.error();
            if (error != null) {
                invalid++;
                if (first == null) {
                    first = error;
                }
            }
        }
        if (invalid == 0) {
            return null;
        }
        return invalid == 1 ? first : Component.translatable(KEY + "error.lines", invalid);
    }

    // ------------------------------------------------------------------ suggestions

    private void openSuggestions(String text) {
        String search = text.toLowerCase(Locale.ROOT).trim();
        matches = choices().stream().filter(choice -> choice.searchKey().contains(search)).limit(MAX_MATCHES).toList();
        suggestionScroll = 0;
        highlighted = 0;
        suggestionsOpen = true;
    }

    private boolean suggestionsShown() {
        return suggestionsOpen && focused() && focusedRow != null && focusedBox == focusedRow.itemBox && !matches.isEmpty();
    }

    private int shownSuggestions() {
        return Math.min(VISIBLE_SUGGESTIONS, matches.size());
    }

    private void select(LootItemChoice choice) {
        Row row = focusedRow;
        if (row == null) {
            return;
        }
        row.itemBox.setValue(choice.name());
        row.itemBox.moveCursorToStart();
        // set after setValue: its responder looks the item up by name, and two items can share one
        row.selected = choice;
        suggestionsOpen = false;
        push();
        playClick();
    }

    // ------------------------------------------------------------------ layout

    private int layout() {
        int y = 0;
        for (Row row : rows) {
            row.y = y;
            y += row.height();
        }
        addY = y + 2;
        return addY + ADD_H;
    }

    @Override
    public int height(int width) {
        syncFromValue();
        return layout();
    }

    private int textWidth() {
        return (width() - ICON_W - COUNT_W - REMOVE_W - 3 * GAP) / 2;
    }

    private void focus(@Nullable Row row, @Nullable EditBox box) {
        if (focusedBox != null && focusedBox != box) {
            focusedBox.setFocused(false);
        }
        focusedRow = row;
        focusedBox = box;
        if (box != null) {
            box.setFocused(true);
        }
    }

    /** The panel scrolls to the focused line, not to the bottom of the widget. */
    @Override
    @Nullable
    public int[] focusBounds() {
        if (focusedRow == null) {
            return null;
        }
        return new int[]{x(), y() + focusedRow.y, width(), focusedRow.height()};
    }

    /** Plain keys ([, ], Page Up...) are text while a field is focused. */
    @Override
    public boolean capturesText() {
        return focusedBox != null;
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(GuiGraphics g, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
        syncFromValue();
        layout();
        Theme t = theme();
        boolean inside = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height && !overSuggestions(mouseX, mouseY);
        int textW = (width - ICON_W - COUNT_W - REMOVE_W - 3 * GAP) / 2;

        for (Row row : rows) {
            int rowY = y + row.y;
            String id = row.selected != null ? row.selected.id() : "";
            if (!ConfeeIcons.drawItem(g, id, x + 1, rowY + 1, 16)) {
                outline(g, x + 3, rowY + 3, 12, 12, t.textDisabled());
            }
            int itemX = x + ICON_W;
            int extrasX = itemX + textW + GAP;
            int countX = extrasX + textW + GAP;
            int removeX = countX + COUNT_W + GAP;
            field(g, row.itemBox, itemX, rowY, textW, row.isBlank() || row.selected != null, mouseX, mouseY, delta);
            field(g, row.extrasBox, extrasX, rowY, textW, row.validExtras(), mouseX, mouseY, delta);
            field(g, row.countBox, countX, rowY, COUNT_W, row.validCount(), mouseX, mouseY, delta);

            boolean removeHover = inside && over(mouseX, mouseY, removeX, rowY, REMOVE_W, FIELD_H);
            ConfeeDraw.button(g, t, removeX, rowY, REMOVE_W, FIELD_H, Component.literal("×"), removeHover, false, enabled());

            Component error = row.error();
            if (error != null) {
                ConfeeDraw.textFit(g, error, x + ICON_W, rowY + ROW_H, width - ICON_W, t.error());
            }
        }

        boolean addHover = inside && over(mouseX, mouseY, x, y + addY, width, ADD_H);
        ConfeeDraw.button(g, t, x, y + addY, width, ADD_H, Component.literal("+ ").append(Component.translatable(KEY + "add")),
                addHover, false, enabled());

        // where the suggestions go: under the item field of the focused line
        if (focusedRow != null) {
            int sy = y + focusedRow.y + FIELD_H + 1;
            suggestionArea = new int[]{x + ICON_W, sy, Math.max(2 * textW + GAP, 160), shownSuggestions() * SUGGESTION_H};
        }
    }

    private void field(GuiGraphics g, EditBox box, int x, int y, int width, boolean valid,
                       int mouseX, int mouseY, float delta) {
        Theme t = theme();
        boolean active = focused() && box == focusedBox;
        int border = !valid ? t.error() : active ? t.accent() : t.textDisabled();
        g.fill(x, y, x + width, y + FIELD_H, t.panelBgDeep());
        outline(g, x, y, width, FIELD_H, border);
        box.setX(x + 4);
        box.setY(y + (FIELD_H - 8) / 2);
        box.setWidth(width - 8);
        box.setTextColor(valid ? t.textOnDark() : t.error());
        box.setEditable(enabled());
        box.setFocused(active);
        box.render(g, mouseX, mouseY, delta);
    }

    @Override
    @Nullable
    public int[] overlayBounds() {
        return suggestionsShown() ? suggestionArea : null;
    }

    private boolean overSuggestions(double mouseX, double mouseY) {
        int[] area = overlayBounds();
        return area != null && over(mouseX, mouseY, area[0], area[1], area[2], area[3]);
    }

    /** The item suggestions, over the next lines and options. */
    @Override
    public void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int[] area = overlayBounds();
        if (area == null) {
            return;
        }
        Theme t = theme();
        int x = area[0];
        int y = area[1];
        int width = area[2];
        int shown = shownSuggestions();
        int height = shown * SUGGESTION_H;
        g.fill(x - 1, y - 1, x + width + 1, y + height + 1, t.outline());
        g.fill(x, y, x + width, y + height, t.panelBgDeep());
        for (int i = 0; i < shown; i++) {
            int index = suggestionScroll + i;
            LootItemChoice choice = matches.get(index);
            int rowY = y + i * SUGGESTION_H;
            boolean hovered = over(mouseX, mouseY, x, rowY, width, SUGGESTION_H);
            boolean current = index == highlighted;
            if (current) {
                g.fill(x, rowY, x + width, rowY + SUGGESTION_H, t.accent());
            } else if (hovered) {
                g.fill(x, rowY, x + width, rowY + SUGGESTION_H, t.panelBg());
            }
            ConfeeIcons.drawItem(g, choice.id(), x + 1, rowY + 1, 16);
            int textX = x + ICON_W;
            int nameEnd = ConfeeDraw.text(g, Component.literal(font().plainSubstrByWidth(choice.name(), width - ICON_W - 4)),
                    textX, rowY + 5, current ? t.onAccentText() : t.textOnDark());
            // the id after the name when it fits
            int idX = nameEnd + 6;
            if (idX + 20 < x + width) {
                ConfeeDraw.textFit(g, Component.literal(choice.id()), idX, rowY + 5, x + width - 4 - idX,
                        current ? t.onAccentText() : t.textDisabled());
            }
        }
        if (matches.size() > shown) {
            int barH = Math.max(4, height * shown / matches.size());
            int barY = y + (height - barH) * suggestionScroll / (matches.size() - shown);
            g.fill(x + width - 2, barY, x + width, barY + barH, t.textDisabled());
        }
    }

    @Override
    public boolean overlayClicked(double mouseX, double mouseY, int button) {
        int[] area = overlayBounds();
        if (area == null || button != 0) {
            return false;
        }
        int index = suggestionScroll + (int) ((mouseY - area[1]) / SUGGESTION_H);
        if (index >= 0 && index < matches.size()) {
            select(matches.get(index));
        }
        return true;
    }

    @Override
    public boolean overlayScrolled(double mouseX, double mouseY, double delta) {
        if (overlayBounds() == null) {
            return false;
        }
        suggestionScroll = clamp(suggestionScroll - (int) Math.signum(delta), 0, matches.size() - shownSuggestions());
        return true;
    }

    private static void outline(GuiGraphics g, int x, int y, int width, int height, int color) {
        g.fill(x, y, x + width, y + 1, color);
        g.fill(x, y + height - 1, x + width, y + height, color);
        g.fill(x, y + 1, x + 1, y + height - 1, color);
        g.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean over(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        int textW = textWidth();
        if (over(mouseX, mouseY, x(), y() + addY, width(), ADD_H)) {
            playClick();
            Row row = new Row("");
            rows.add(row);
            focus(row, row.itemBox);
            openSuggestions("");
            layout();
            revealFocus();
            return true;
        }
        for (Row row : List.copyOf(rows)) {
            int rowY = y() + row.y;
            int removeX = x() + ICON_W + 2 * textW + COUNT_W + 3 * GAP;
            if (over(mouseX, mouseY, removeX, rowY, REMOVE_W, FIELD_H)) {
                playClick();
                rows.remove(row);
                if (focusedRow == row) {
                    focus(null, null);
                }
                push();
                return true;
            }
            for (EditBox box : row.boxes()) {
                if (over(mouseX, mouseY, box.getX() - 4, rowY, box.getWidth() + 8, FIELD_H)) {
                    boolean wasFocused = focusedBox == box;
                    focus(row, box);
                    box.mouseClicked(mouseX, mouseY, button);
                    if (box == row.itemBox) {
                        // a second click on the item field closes the suggestions
                        if (wasFocused && suggestionsOpen) {
                            suggestionsOpen = false;
                        } else {
                            openSuggestions(box.getValue());
                        }
                    }
                    return true;
                }
            }
        }
        // a click on the free space keeps the widget focused, without a field
        focus(null, null);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (focusedBox == null || focusedRow == null) {
            return false;
        }
        if (suggestionsShown()) {
            switch (key) {
                case InputConstants.KEY_DOWN, InputConstants.KEY_UP -> {
                    highlighted = clamp(highlighted + (key == InputConstants.KEY_DOWN ? 1 : -1), 0, matches.size() - 1);
                    // keep the highlighted suggestion in view
                    suggestionScroll = clamp(suggestionScroll, highlighted - shownSuggestions() + 1, highlighted);
                    return true;
                }
                case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                    select(matches.get(highlighted));
                    return true;
                }
                case InputConstants.KEY_ESCAPE -> {
                    suggestionsOpen = false;
                    return true;
                }
                default -> {
                }
            }
        }
        if (key == InputConstants.KEY_UP || key == InputConstants.KEY_DOWN) {
            // same field on the line above or below
            int index = rows.indexOf(focusedRow) + (key == InputConstants.KEY_DOWN ? 1 : -1);
            if (index >= 0 && index < rows.size()) {
                Row row = rows.get(index);
                int field = focusedRow.boxes().indexOf(focusedBox);
                suggestionsOpen = false;
                focus(row, row.boxes().get(field));
                revealFocus();
            }
            return true;
        }
        // Ctrl+Tab, Ctrl+F... go to the screen when the field does not use them
        return focusedBox.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (focusedBox == null) {
            return false;
        }
        if (focusedRow != null && focusedBox == focusedRow.countBox && !Character.isDigit(c)) {
            return true;
        }
        return focusedBox.charTyped(c, modifiers);
    }

    @Override
    public Component narration() {
        if (focusedBox != null) {
            return Component.empty().append(focusedBox.getMessage()).append(": ").append(focusedBox.getValue());
        }
        return Component.translatable(KEY + "narration", label(), value().size());
    }
}
