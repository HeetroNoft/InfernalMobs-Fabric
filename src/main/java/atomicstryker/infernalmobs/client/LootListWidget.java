package atomicstryker.infernalmobs.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.heetstudio.confee.api.Theme;
import fr.heetstudio.confee.api.client.OptionWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Confee widget of the loot lists (droppedItemIDs*): one line per item stack SNBT string, edited as three fields:
 * the item (in game name, with suggestions), the additional SNBT (everything but id and count) and the count.
 * <p>The list stays a list of SNBT strings in the file. A line that is not edited is written back unchanged.
 * Confee does not let a custom widget block "Save": a line with an error is shown in red and still saved, the
 * loot parser logs it and skips it when the world loads. Empty new lines are not saved.</p>
 * <p>The widget is never taller than the option panel, a long list scrolls inside the widget.</p>
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
    private static final int VISIBLE_SUGGESTIONS = 5;
    private static final int MAX_MATCHES = 200;
    private static final int ADD_H = 18;
    private static final int MAX_HEIGHT = 360;

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

    // layout, in content coordinates (0 = top of the widget when it is not scrolled)
    private int contentHeight;
    private int suggestionsY = -1;
    private int addY;
    private double innerScroll;

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

        @Nullable
        Component error() {
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
        suggestionsY = -1;
        for (Row row : rows) {
            row.y = y;
            y += ROW_H;
            if (row.error() != null) {
                y += ERROR_H;
            }
            if (row == focusedRow && suggestionsShown()) {
                suggestionsY = y;
                y += shownSuggestions() * SUGGESTION_H + 2;
            }
        }
        addY = y + 2;
        contentHeight = addY + ADD_H;
        return contentHeight;
    }

    @Override
    public int height(int width) {
        syncFromValue();
        return Math.min(maxHeight(), layout());
    }

    /**
     * The widget must fit in Confee's option panel: a focused widget taller than the panel is scrolled to its bottom,
     * which would move the line just clicked out of view. Title bar, bottom bar and the option name take ~110 pixels.
     */
    private static int maxHeight() {
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return Math.max(4 * ROW_H + ADD_H, Math.min(MAX_HEIGHT, screenHeight - 110));
    }

    private double maxScroll() {
        return Math.max(0, contentHeight - height());
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

    /** Keeps a content range in view, e.g. the focused line and its suggestions. */
    private void reveal(int top, int bottom) {
        if (top < innerScroll) {
            innerScroll = top;
        } else if (bottom > innerScroll + height()) {
            innerScroll = bottom - height();
        }
        innerScroll = Math.max(0, Math.min(maxScroll(), innerScroll));
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(GuiGraphics g, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
        syncFromValue();
        layout();
        innerScroll = Math.max(0, Math.min(maxScroll(), innerScroll));
        Theme t = theme();
        boolean scrolls = contentHeight > height;
        boolean inside = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        if (scrolls) {
            g.enableScissor(x, y, x + width, y + height);
        }
        int top = y - (int) innerScroll;
        int contentW = width - (scrolls ? 4 : 0);
        int textW = (contentW - ICON_W - COUNT_W - REMOVE_W - 3 * GAP) / 2;

        for (Row row : rows) {
            int rowY = top + row.y;
            if (row.selected != null && row.selected.stack() != null) {
                g.renderItem(row.selected.stack(), x + 1, rowY + 1);
            } else {
                outline(g, x + 3, rowY + 3, 12, 12, t.textDisabled());
            }
            int itemX = x + ICON_W;
            int extrasX = itemX + textW + GAP;
            int countX = extrasX + textW + GAP;
            int removeX = countX + COUNT_W + GAP;
            field(g, row, row.itemBox, itemX, rowY, textW, row.selected != null, mouseX, mouseY, delta);
            field(g, row, row.extrasBox, extrasX, rowY, textW, row.validExtras(), mouseX, mouseY, delta);
            field(g, row, row.countBox, countX, rowY, COUNT_W, row.validCount(), mouseX, mouseY, delta);

            boolean removeHover = inside && enabled() && over(mouseX, mouseY, removeX, rowY, REMOVE_W, FIELD_H);
            button(g, removeX, rowY, REMOVE_W, FIELD_H, "×", removeHover);

            Component error = row.error();
            if (error != null) {
                g.drawString(font(), font().plainSubstrByWidth(error.getString(), contentW - ICON_W), x + ICON_W, rowY + ROW_H, t.error());
            }
        }

        if (suggestionsY >= 0) {
            renderSuggestions(g, x + ICON_W, top + suggestionsY, Math.max(textW * 2 + GAP, 160), inside, mouseX, mouseY);
        }

        boolean addHover = inside && enabled() && over(mouseX, mouseY, x, top + addY, contentW, ADD_H);
        button(g, x, top + addY, contentW, ADD_H, "+ " + Component.translatable(KEY + "add").getString(), addHover);

        if (scrolls) {
            g.disableScissor();
            int barH = Math.max(12, height * height / contentHeight);
            int barY = y + (int) ((height - barH) * innerScroll / maxScroll());
            g.fill(x + width - 2, y, x + width, y + height, t.panelBgDeep());
            g.fill(x + width - 2, barY, x + width, barY + barH, t.textDisabled());
        }
    }

    private void field(GuiGraphics g, Row row, EditBox box, int x, int y, int width, boolean valid,
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

    private void renderSuggestions(GuiGraphics g, int x, int y, int width, boolean inside, int mouseX, int mouseY) {
        Theme t = theme();
        int shown = shownSuggestions();
        int height = shown * SUGGESTION_H;
        g.fill(x - 1, y - 1, x + width + 1, y + height + 1, t.outline());
        g.fill(x, y, x + width, y + height, t.panelBgDeep());
        for (int i = 0; i < shown; i++) {
            int index = suggestionScroll + i;
            LootItemChoice choice = matches.get(index);
            int rowY = y + i * SUGGESTION_H;
            boolean hovered = inside && over(mouseX, mouseY, x, rowY, width, SUGGESTION_H);
            boolean current = index == highlighted;
            if (current) {
                g.fill(x, rowY, x + width, rowY + SUGGESTION_H, t.accent());
            } else if (hovered) {
                g.fill(x, rowY, x + width, rowY + SUGGESTION_H, t.panelBg());
            }
            if (choice.stack() != null) {
                g.renderItem(choice.stack(), x + 1, rowY + 1);
            }
            int textColor = current ? t.onAccentText() : t.textOnDark();
            int nameW = font().width(choice.name());
            int textX = x + ICON_W;
            int room = width - ICON_W - 4;
            g.drawString(font(), font().plainSubstrByWidth(choice.name(), room), textX, rowY + 5, textColor);
            // the id after the name when it fits
            int idX = textX + nameW + 6;
            if (idX + 20 < x + width) {
                g.drawString(font(), font().plainSubstrByWidth(choice.id(), x + width - 4 - idX), idX, rowY + 5,
                        current ? t.onAccentText() : t.textDisabled());
            }
        }
        if (matches.size() > shown) {
            int barH = Math.max(4, height * shown / matches.size());
            int barY = y + (height - barH) * suggestionScroll / (matches.size() - shown);
            g.fill(x + width - 2, barY, x + width, barY + barH, t.textDisabled());
        }
    }

    /** Confee button look: light face, darker bottom edge. */
    private void button(GuiGraphics g, int x, int y, int width, int height, String text, boolean hover) {
        Theme t = theme();
        int face = !enabled() ? t.toggleOffTrack() : hover ? t.buttonFaceHover() : t.buttonFace();
        g.fill(x, y, x + width, y + height, t.outline());
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, t.buttonShadow());
        g.fill(x + 1, y + 1, x + width - 1, y + height - 3, face);
        int textW = font().width(text);
        g.drawString(font(), text, x + (width - textW) / 2, y + (height - 2 - 8) / 2, t.textOnLight());
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
        int top = y() - (int) innerScroll;
        boolean scrolls = contentHeight > height();
        int contentW = width() - (scrolls ? 4 : 0);
        int textW = (contentW - ICON_W - COUNT_W - REMOVE_W - 3 * GAP) / 2;

        if (suggestionsY >= 0) {
            int sx = x() + ICON_W;
            int sy = top + suggestionsY;
            int sw = Math.max(textW * 2 + GAP, 160);
            if (over(mouseX, mouseY, sx, sy, sw, shownSuggestions() * SUGGESTION_H)) {
                select(matches.get(suggestionScroll + (int) ((mouseY - sy) / SUGGESTION_H)));
                return true;
            }
        }
        if (over(mouseX, mouseY, x(), top + addY, contentW, ADD_H)) {
            playClick();
            Row row = new Row("");
            rows.add(row);
            focus(row, row.itemBox);
            openSuggestions("");
            layout();
            reveal(row.y, contentHeight);
            return true;
        }
        for (Row row : List.copyOf(rows)) {
            int rowY = top + row.y;
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (suggestionsY >= 0) {
            int sy = y() - (int) innerScroll + suggestionsY;
            if (mouseY >= sy && mouseY < sy + shownSuggestions() * SUGGESTION_H) {
                suggestionScroll = clamp(suggestionScroll - (int) Math.signum(delta), 0, matches.size() - shownSuggestions());
                return true;
            }
        }
        if (maxScroll() <= 0) {
            return false;
        }
        double before = innerScroll;
        innerScroll = Math.max(0, Math.min(maxScroll(), innerScroll - delta * ROW_H));
        // at either end, the screen scrolls instead
        return innerScroll != before;
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
                layout();
                reveal(row.y, row.y + ROW_H);
            }
            return true;
        }
        if (key == InputConstants.KEY_ESCAPE) {
            if (suggestionsOpen) {
                suggestionsOpen = false;
                return true;
            }
            return false;
        }
        if (focusedBox.keyPressed(key, scanCode, modifiers)) {
            return true;
        }
        // a plain key is text while a field is focused, it must not trigger a screen shortcut;
        // combinations (Ctrl+Tab to change category...) go to the screen
        int combination = GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SUPER;
        return (modifiers & combination) == 0;
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
