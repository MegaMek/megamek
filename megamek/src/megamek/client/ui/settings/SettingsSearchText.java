/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.settings;

import java.awt.Component;
import java.awt.Container;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.View;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/** Extracts rendered Swing text and maps normalized search matches back to source offsets. */
final class SettingsSearchText {
    private SettingsSearchText() {
    }

    static TextSource from(JComponent component) {
        String rawText = componentText(component);
        if (rawText == null || rawText.isBlank()) {
            return TextSource.EMPTY;
        }
        View htmlView = htmlView(component);
        if (htmlView == null) {
            return new TextSource(rawText, null);
        }
        Document document = htmlView.getDocument();
        try {
            return new TextSource(document.getText(0, document.getLength()), htmlView);
        } catch (BadLocationException exception) {
            return TextSource.EMPTY;
        }
    }

    static String renderedText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (!BasicHTML.isHTMLString(text)) {
            return text.trim();
        }
        return from(new JLabel(text)).text().trim();
    }

    static String collect(Component root) {
        StringBuilder text = new StringBuilder();
        Set<Component> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        collect(root, text, visited);
        return text.toString().trim();
    }

    static List<String> tokens(String normalizedFilter) {
        if (normalizedFilter == null || normalizedFilter.isBlank()) {
            return List.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : normalizedFilter.split("\\s+")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return List.copyOf(tokens);
    }

    static List<TextRange> ranges(String sourceText, List<String> normalizedTokens) {
        if (sourceText == null || sourceText.isBlank() || normalizedTokens.isEmpty()) {
            return List.of();
        }
        NormalizedText normalizedText = normalizeWithOffsets(sourceText);
        List<TextRange> ranges = new ArrayList<>();
        for (String token : normalizedTokens) {
            int fromIndex = 0;
            while (fromIndex < normalizedText.text().length()) {
                int matchIndex = normalizedText.text().indexOf(token, fromIndex);
                if (matchIndex < 0) {
                    break;
                }
                int sourceStart = normalizedText.sourceOffsets()[matchIndex];
                int normalizedEnd = matchIndex + token.length();
                int sourceEnd = normalizedEnd < normalizedText.sourceOffsets().length
                      ? normalizedText.sourceOffsets()[normalizedEnd]
                      : sourceText.length();
                ranges.add(new TextRange(sourceStart, Math.max(sourceStart + 1, sourceEnd)));
                fromIndex = matchIndex + token.length();
            }
        }
        ranges.sort((first, second) -> Integer.compare(first.start(), second.start()));
        return List.copyOf(ranges);
    }

    private static String componentText(JComponent component) {
        if (component instanceof JLabel label) {
            return label.getText();
        }
        if (component instanceof AbstractButton button) {
            return button.getText();
        }
        if (component instanceof JEditorPane editorPane && !editorPane.isEditable()) {
            try {
                return editorPane.getDocument().getText(0, editorPane.getDocument().getLength());
            } catch (BadLocationException exception) {
                return null;
            }
        }
        return null;
    }

    private static void collect(Component component, StringBuilder text, Set<Component> visited) {
        if (!visited.add(component)) {
            return;
        }
        if (component instanceof JTable table) {
            collectTable(table, text, visited);
            return;
        }
        if (component instanceof JTableHeader header) {
            collectTableHeader(header, text);
            return;
        }
        if (component instanceof JComponent swingComponent) {
            TextSource source = from(swingComponent);
            append(text, source.text());
            if (component instanceof JLabel || component instanceof AbstractButton
                  || component instanceof JEditorPane) {
                return;
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collect(child, text, visited);
            }
        }
    }

    private static void collectTable(JTable table, StringBuilder text, Set<Component> visited) {
        JTableHeader header = table.getTableHeader();
        if (header != null && visited.add(header)) {
            collectTableHeader(header, text);
        }
        Set<Integer> indexedModelRows = new LinkedHashSet<>();
        for (int row = 0; row < table.getRowCount(); row++) {
            indexedModelRows.add(table.convertRowIndexToModel(row));
            for (int column = 0; column < table.getColumnCount(); column++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component rendererComponent = table.prepareRenderer(renderer, row, column);
                append(text, collect(rendererComponent));
            }
        }
        for (int modelRow = 0; modelRow < table.getModel().getRowCount(); modelRow++) {
            if (indexedModelRows.contains(modelRow)) {
                continue;
            }
            for (int modelColumn = 0; modelColumn < table.getModel().getColumnCount(); modelColumn++) {
                Object value = table.getModel().getValueAt(modelRow, modelColumn);
                append(text, value == null ? "" : renderedText(value.toString()));
            }
        }
    }

    private static void collectTableHeader(JTableHeader header, StringBuilder text) {
        JTable table = header.getTable();
        if (table == null) {
            return;
        }
        for (int column = 0; column < table.getColumnCount(); column++) {
            TableColumn tableColumn = table.getColumnModel().getColumn(column);
            TableCellRenderer renderer = tableColumn.getHeaderRenderer();
            if (renderer == null) {
                renderer = header.getDefaultRenderer();
            }
            Component rendererComponent = renderer.getTableCellRendererComponent(table,
                  tableColumn.getHeaderValue(), false, false, -1, column);
            append(text, collect(rendererComponent));
        }
    }

    private static void append(StringBuilder destination, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!destination.isEmpty()) {
            destination.append(' ');
        }
        destination.append(value.trim());
    }

    private static View htmlView(JComponent component) {
        Object view = component.getClientProperty(BasicHTML.propertyKey);
        return view instanceof View htmlView ? htmlView : null;
    }

    private static NormalizedText normalizeWithOffsets(String sourceText) {
        StringBuilder normalized = new StringBuilder(sourceText.length());
        List<Integer> offsets = new ArrayList<>(sourceText.length() + 1);
        boolean separatorPending = false;
        int separatorOffset = 0;
        Character.UnicodeScript baseScript = Character.UnicodeScript.COMMON;
        for (int sourceOffset = 0; sourceOffset < sourceText.length();) {
            int codePoint = sourceText.codePointAt(sourceOffset);
            int nextOffset = sourceOffset + Character.charCount(codePoint);
            String decomposed = Normalizer.normalize(new String(Character.toChars(codePoint)), Normalizer.Form.NFD);
            StringBuilder retained = new StringBuilder(decomposed.length());
            for (int offset = 0; offset < decomposed.length();) {
                int decomposedCodePoint = decomposed.codePointAt(offset);
                offset += Character.charCount(decomposedCodePoint);
                int type = Character.getType(decomposedCodePoint);
                boolean combiningMark = type == Character.NON_SPACING_MARK
                      || type == Character.COMBINING_SPACING_MARK
                      || type == Character.ENCLOSING_MARK;
                if (!combiningMark || baseScript != Character.UnicodeScript.LATIN) {
                    retained.appendCodePoint(decomposedCodePoint);
                }
                if (!combiningMark) {
                    baseScript = Character.UnicodeScript.of(decomposedCodePoint);
                }
            }
            String searchableUnit = Normalizer.normalize(retained, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
            boolean appendedLetterOrDigit = false;
            for (int offset = 0; offset < searchableUnit.length();) {
                int normalizedCodePoint = searchableUnit.codePointAt(offset);
                offset += Character.charCount(normalizedCodePoint);
                if (Character.isLetterOrDigit(normalizedCodePoint)) {
                    if (separatorPending && !normalized.isEmpty()) {
                        normalized.append(' ');
                        offsets.add(separatorOffset);
                    }
                    normalized.appendCodePoint(normalizedCodePoint);
                    for (int unit = 0; unit < Character.charCount(normalizedCodePoint); unit++) {
                        offsets.add(sourceOffset);
                    }
                    separatorPending = false;
                    appendedLetterOrDigit = true;
                }
            }
            if (!appendedLetterOrDigit && !normalized.isEmpty()) {
                separatorPending = true;
                separatorOffset = sourceOffset;
            }
            sourceOffset = nextOffset;
        }
        offsets.add(sourceText.length());
        return new NormalizedText(normalized.toString(), offsets.stream().mapToInt(Integer::intValue).toArray());
    }

    record TextSource(String text, View htmlView) {
        private static final TextSource EMPTY = new TextSource("", null);

        boolean isHtml() {
            return htmlView != null;
        }
    }

    record TextRange(int start, int end) {
    }

    private record NormalizedText(String text, int[] sourceOffsets) {
    }
}
