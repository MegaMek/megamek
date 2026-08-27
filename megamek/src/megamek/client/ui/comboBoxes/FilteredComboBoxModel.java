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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import megamek.common.annotations.Nullable;

/**
 * A combo box model that keeps the full list of items but only shows the ones whose display text contains the
 * current filter text. The selected item is kept even while it is filtered out of view, so typing a search never
 * silently changes what is selected.
 * <p>
 * The display text of an item comes from the display function handed to the constructor, so the model never relies
 * on {@code toString()}.
 *
 * @param <E> the type of item held by the model
 */
public class FilteredComboBoxModel<E> extends AbstractListModel<E> implements ComboBoxModel<E> {

    private final List<E> allItems;
    private final Function<E, String> displayFunction;
    private final List<E> visibleItems = new ArrayList<>();
    private String filterText = "";
    private E selectedItem;

    /**
     * @param items           every item the model can offer, in display order
     * @param displayFunction converts an item into the text shown to the user and matched against the filter
     */
    public FilteredComboBoxModel(List<E> items, Function<E, String> displayFunction) {
        this.allItems = new ArrayList<>(items);
        this.displayFunction = displayFunction;
        visibleItems.addAll(allItems);
    }

    /**
     * Restricts the visible items to those whose display text contains the given text, ignoring case. Blank or
     * {@code null} text shows every item again.
     *
     * @param text the text to filter by, or {@code null} to clear the filter
     */
    public void setFilter(@Nullable String text) {
        String newFilterText = (text == null) ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (newFilterText.equals(filterText)) {
            return;
        }
        filterText = newFilterText;
        rebuildVisibleItems();
    }

    /**
     * Re-reads every item's display text. Call this when the text an item shows has changed (for example a
     * renamed ammo bin), so the visible list and the current filter reflect the new names.
     */
    public void refreshContents() {
        rebuildVisibleItems();
    }

    private void rebuildVisibleItems() {
        int previousSize = visibleItems.size();
        visibleItems.clear();
        for (E item : allItems) {
            if (matchesFilter(item)) {
                visibleItems.add(item);
            }
        }
        int largestSize = Math.max(previousSize, visibleItems.size());
        fireContentsChanged(this, 0, Math.max(0, largestSize - 1));
    }

    /** Shows every item again. */
    public void clearFilter() {
        setFilter(null);
    }

    /**
     * @return {@code true} when a non-blank filter is currently narrowing the visible items
     */
    public boolean isFiltered() {
        return !filterText.isEmpty();
    }

    private boolean matchesFilter(E item) {
        if (filterText.isEmpty()) {
            return true;
        }
        return getDisplayText(item).toLowerCase(Locale.ROOT).contains(filterText);
    }

    /**
     * @param item the item to describe
     *
     * @return the text shown to the user for the item, never {@code null}
     */
    public String getDisplayText(E item) {
        String displayText = displayFunction.apply(item);
        return (displayText == null) ? "" : displayText;
    }

    /**
     * Looks up the display text of an arbitrary value. Values that are items of this model are described with the
     * display function; anything else (for example a {@code String} typed by the user) is returned as its string
     * form.
     *
     * @param value the value to describe, or {@code null}
     *
     * @return the text to show for the value, never {@code null}
     */
    public String displayTextOf(@Nullable Object value) {
        if (value == null) {
            return "";
        }
        int itemIndex = allItems.indexOf(value);
        if (itemIndex >= 0) {
            return getDisplayText(allItems.get(itemIndex));
        }
        return value.toString();
    }

    /**
     * @param text the display text to look for, compared ignoring case and surrounding whitespace
     *
     * @return the item whose display text equals the given text, if any
     */
    public Optional<E> findByDisplayText(@Nullable String text) {
        if (text == null) {
            return Optional.empty();
        }
        String wantedText = text.trim();
        for (E item : allItems) {
            if (getDisplayText(item).equalsIgnoreCase(wantedText)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    /**
     * @return the first item that passes the current filter, if any
     */
    public Optional<E> getFirstVisibleItem() {
        return visibleItems.isEmpty() ? Optional.empty() : Optional.of(visibleItems.getFirst());
    }

    /**
     * @return an unmodifiable view of every item the model holds, regardless of the filter
     */
    public List<E> getAllItems() {
        return List.copyOf(allItems);
    }

    @Override
    public int getSize() {
        return visibleItems.size();
    }

    @Override
    public E getElementAt(int index) {
        return visibleItems.get(index);
    }

    /**
     * Selects the given item. Only {@code null} or an item held by this model is accepted; anything else (such as
     * raw editor text) is ignored so the selection can never become a value of the wrong type.
     *
     * @param anItem the item to select, or {@code null} to clear the selection
     */
    @Override
    @SuppressWarnings("unchecked")
    public void setSelectedItem(@Nullable Object anItem) {
        if ((anItem != null) && !allItems.contains(anItem)) {
            return;
        }
        boolean isUnchanged = (selectedItem == null) ? (anItem == null) : selectedItem.equals(anItem);
        if (isUnchanged) {
            return;
        }
        selectedItem = (E) anItem;
        fireContentsChanged(this, -1, -1);
    }

    @Override
    public @Nullable E getSelectedItem() {
        return selectedItem;
    }
}
