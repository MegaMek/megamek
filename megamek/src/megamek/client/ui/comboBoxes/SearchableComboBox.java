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
package megamek.client.ui.comboBoxes;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.ComboPopup;

import megamek.client.ui.Messages;
import megamek.common.annotations.Nullable;

/**
 * A combo box that can be searched by typing. The user can still open the list and pick an entry with the mouse or
 * the arrow keys, but typing into the box narrows the list to the entries whose text contains what was typed, which
 * makes long lists (dozens of munition types, for example) quick to navigate.
 * <p>
 * Behaviour while typing:
 * <ul>
 *     <li>Focusing the box selects its text, so typing replaces the shown entry.</li>
 *     <li>Typed text is matched ignoring case, anywhere in the entry text.</li>
 *     <li>{@code Enter} picks the highlighted entry, or the first match when nothing is highlighted.</li>
 *     <li>{@code Escape}, or leaving the box, drops the search text and shows the current selection again
 *     without changing it.</li>
 *     <li>The selection only ever changes to a real entry; stray text can never become the selected value.</li>
 * </ul>
 * Entry text comes from the display function handed to the constructor, so the box never relies on
 * {@code toString()}.
 *
 * @param <E> the type of item held by the combo box
 */
public class SearchableComboBox<E> extends JComboBox<E> {

    private static final String ACTION_ACCEPT_SEARCH = "acceptSearch";
    private static final String ACTION_CANCEL_SEARCH = "cancelSearch";
    /** How long after gaining focus a mouse release still counts as the click that focused the box. */
    private static final long FOCUSING_CLICK_WINDOW_MILLIS = 300;

    private final FilteredComboBoxModel<E> filteredModel;
    private final JTextField editorField;
    private boolean isUpdatingEditorText = false;
    private boolean isRefreshingPopup = false;
    private boolean isSearchPending = false;
    private long focusGainedAtMillis = 0;

    /**
     * @param name            the component name, used for look-ups in tests and tooling
     * @param items           the entries to offer, in display order
     * @param displayFunction converts an entry into the text shown to, and searched by, the user
     */
    public SearchableComboBox(String name, List<E> items, Function<E, String> displayFunction) {
        this(name, new FilteredComboBoxModel<>(items, displayFunction));
    }

    private SearchableComboBox(String name, FilteredComboBoxModel<E> model) {
        super(model);
        filteredModel = model;
        setName(name);

        DisplayTextEditor editor = new DisplayTextEditor();
        editorField = editor.getTextField();
        setEditor(editor);
        setEditable(true);
        setRenderer(new DisplayTextRenderer());
        keepWidthOfWidestEntry();

        editorField.setToolTipText(Messages.getString("SearchableComboBox.tooltip"));
        editorField.getDocument().addDocumentListener(new EditorTextListener());
        editorField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                focusGainedAtMillis = System.currentTimeMillis();
                SwingUtilities.invokeLater(editorField::selectAll);
            }

            @Override
            public void focusLost(FocusEvent event) {
                if (!event.isTemporary()) {
                    restoreEditorFromSelection();
                }
            }
        });
        editorField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                // When the click also activated the window, focus arrives before the press and the press then
                // moves the caret; re-select after the release so typing still replaces the shown entry.
                boolean isFocusingClick = (System.currentTimeMillis() - focusGainedAtMillis)
                      < FOCUSING_CLICK_WINDOW_MILLIS;
                if (isFocusingClick) {
                    SwingUtilities.invokeLater(editorField::selectAll);
                }
            }
        });
        installSearchKeys();
        addPopupMenuListener(new PopupClosedListener());
    }

    /**
     * Sizes the box for its widest entry, so narrowing the list while typing does not make the box shrink and
     * grow with every keystroke.
     */
    private void keepWidthOfWidestEntry() {
        Optional<E> widestEntry = filteredModel.getAllItems()
              .stream()
              .max(Comparator.comparingInt(item -> filteredModel.getDisplayText(item).length()));
        widestEntry.ifPresent(this::setPrototypeDisplayValue);
    }

    /**
     * Binds {@code Enter} to accepting the search and {@code Escape} to cancelling it. Both bindings sit on the
     * editor itself so they run before the text field's and the combo box's own handling of those keys.
     */
    private void installSearchKeys() {
        editorField.getInputMap(JComponent.WHEN_FOCUSED)
              .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), ACTION_ACCEPT_SEARCH);
        editorField.getActionMap().put(ACTION_ACCEPT_SEARCH, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                acceptSearch();
            }
        });
        editorField.getInputMap(JComponent.WHEN_FOCUSED)
              .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ACTION_CANCEL_SEARCH);
        editorField.getActionMap().put(ACTION_CANCEL_SEARCH, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                cancelSearch();
            }
        });
    }

    /**
     * Selects the entry the user has arrived at: the entry highlighted in the open list if there is one, otherwise
     * the best match for the typed text. The search text is then dropped.
     */
    private void acceptSearch() {
        E chosenEntry = getHighlightedEntry().orElseGet(() -> resolveEditorText(editorField.getText(), true));
        setPopupVisible(false);
        if (chosenEntry != null) {
            setSelectedItem(chosenEntry);
        }
        restoreEditorFromSelection();
    }

    /** Closes the list and drops the search text, leaving the selection as it was. */
    private void cancelSearch() {
        setPopupVisible(false);
        restoreEditorFromSelection();
    }

    /**
     * @return the entry currently highlighted in the open drop-down list, if the list is open and one is
     *       highlighted
     */
    @SuppressWarnings("unchecked")
    private Optional<E> getHighlightedEntry() {
        if (!isPopupVisible()) {
            return Optional.empty();
        }
        Object popupChild = getUI().getAccessibleChild(this, 0);
        if (!(popupChild instanceof ComboPopup popup)) {
            return Optional.empty();
        }
        Object highlighted = popup.getList().getSelectedValue();
        boolean isEntry = (highlighted != null) && filteredModel.getAllItems().contains(highlighted);
        return isEntry ? Optional.of((E) highlighted) : Optional.empty();
    }

    /**
     * Resolves whatever is in the editor to a real entry.
     *
     * @param editorText        the text currently in the editor
     * @param allowPartialMatch whether the first entry that passes the search may stand in for text that names no
     *                          entry exactly ({@code true} when the user pressed {@code Enter}; {@code false} when
     *                          they merely left the box)
     *
     * @return the entry the text stands for, or the current selection, or {@code null} when there is neither
     */
    private @Nullable E resolveEditorText(String editorText, boolean allowPartialMatch) {
        Optional<E> exactMatch = filteredModel.findByDisplayText(editorText);
        if (exactMatch.isPresent()) {
            return exactMatch.get();
        }
        boolean hasSearchText = !editorText.isBlank();
        Optional<E> firstVisibleMatch = filteredModel.getFirstVisibleItem();
        if (allowPartialMatch && hasSearchText && firstVisibleMatch.isPresent()) {
            return firstVisibleMatch.get();
        }
        return getSelectedItem();
    }

    /**
     * Applies the text now in the editor as the search filter and re-opens the list so it shows only the matches.
     * Reads the editor at run time rather than taking a snapshot, so quick typing is coalesced into one search.
     */
    private void applySearchText() {
        isSearchPending = false;
        String typedText = editorField.getText();
        isUpdatingEditorText = true;
        try {
            filteredModel.setFilter(typedText);
            // Swing resets the editor to the selected entry when the model changes; put the search text back.
            if (!typedText.equals(editorField.getText())) {
                editorField.setText(typedText);
            }
        } finally {
            isUpdatingEditorText = false;
        }
        refreshPopup();
    }

    /**
     * Closes and re-opens the list so its height matches the number of entries that pass the search. The list is
     * hidden entirely when nothing matches.
     */
    private void refreshPopup() {
        if (!isShowing() || !editorField.isFocusOwner()) {
            return;
        }
        isRefreshingPopup = true;
        try {
            if (isPopupVisible()) {
                setPopupVisible(false);
            }
            if (filteredModel.getSize() > 0) {
                setPopupVisible(true);
            }
        } finally {
            isRefreshingPopup = false;
        }
    }

    /**
     * Re-reads every entry's display text. Call this when what an entry should show has changed (for example an
     * ammo bin that was just given a different munition), so the list, the box and its width all reflect the new
     * text.
     */
    public void refreshDisplayTexts() {
        isUpdatingEditorText = true;
        try {
            filteredModel.refreshContents();
            getEditor().setItem(getSelectedItem());
        } finally {
            isUpdatingEditorText = false;
        }
        keepWidthOfWidestEntry();
    }

    /**
     * Drops any search text, shows the full list again and puts the current selection back in the editor.
     */
    private void restoreEditorFromSelection() {
        isUpdatingEditorText = true;
        try {
            filteredModel.clearFilter();
            getEditor().setItem(getSelectedItem());
        } finally {
            isUpdatingEditorText = false;
        }
    }

    /**
     * Selects an entry. Raw text (as produced by the editor) is first resolved to the entry it names exactly, so
     * the selection can never become a value that is not one of the entries.
     *
     * @param anObject the entry to select, editor text naming an entry, or {@code null} to clear the selection
     */
    @Override
    public void setSelectedItem(@Nullable Object anObject) {
        Object itemToSelect = (anObject instanceof String editorText)
              ? resolveEditorText(editorText, false)
              : anObject;
        super.setSelectedItem(itemToSelect);
    }

    /**
     * @return the selected entry, or {@code null} when nothing is selected
     */
    @Override
    @SuppressWarnings("unchecked")
    public @Nullable E getSelectedItem() {
        Object selected = super.getSelectedItem();
        return (selected == null) ? null : (E) selected;
    }

    /**
     * Editor that shows an entry's display text rather than its {@code toString()}, and hands back a real entry
     * (never raw text) when asked for its value.
     */
    private class DisplayTextEditor extends BasicComboBoxEditor {

        JTextField getTextField() {
            return editor;
        }

        @Override
        public void setItem(@Nullable Object anObject) {
            boolean wasUpdating = isUpdatingEditorText;
            isUpdatingEditorText = true;
            try {
                super.setItem(filteredModel.displayTextOf(anObject));
            } finally {
                isUpdatingEditorText = wasUpdating;
            }
        }

        @Override
        public @Nullable Object getItem() {
            return resolveEditorText(editor.getText(), false);
        }
    }

    /** Renders each list entry with its display text. */
    private class DisplayTextRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            return super.getListCellRendererComponent(list, filteredModel.displayTextOf(value), index, isSelected,
                  cellHasFocus);
        }
    }

    /**
     * Turns edits made by the user into a search. The search runs after the document event has finished, because
     * Swing forbids changing the editor text while it is still being notified of the previous change.
     */
    private class EditorTextListener implements DocumentListener {

        private void onTextChanged() {
            if (isUpdatingEditorText || isSearchPending) {
                return;
            }
            isSearchPending = true;
            SwingUtilities.invokeLater(SearchableComboBox.this::applySearchText);
        }

        @Override
        public void insertUpdate(DocumentEvent event) {
            onTextChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            onTextChanged();
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            onTextChanged();
        }
    }

    /** Drops the search once the list closes for good, so the next opening shows every entry again. */
    private class PopupClosedListener implements PopupMenuListener {

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent event) {}

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
            if (!isRefreshingPopup) {
                SwingUtilities.invokeLater(SearchableComboBox.this::restoreEditorFromSelection);
            }
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent event) {}
    }
}
