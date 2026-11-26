/*
 * Copyright (C) 2019-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.preferences;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;

import megamek.codeUtilities.StringUtility;
import megamek.logging.MMLogger;

/**
 * JTablePreference keeps the sort of a JTable: every sort key, not only the column sorted last, so a table sorted by
 * rank and then by profession comes back sorted by profession and, within each profession, by rank. It sets the saved
 * sort when the table is loaded and follows it as it changes.
 * <p>
 * The value is the sort keys from the primary key down, each as {@code column|ORDER}, joined by {@code |}: for example
 * {@code 4|ASCENDING|2|DESCENDING}. A value with a single key is the format older versions wrote, and older versions
 * reading this format take the primary key and ignore the rest.
 * <p>
 * Call preferences.manage(new JTablePreference(JTable)) to use this preference, on a JTable that has called setName
 * and has its row sorter set.
 */
public class JTablePreference extends PreferenceElement implements RowSorterListener {
    private final static MMLogger logger = MMLogger.create(JTablePreference.class);

    private static final String SEPARATOR = "|";

    // region Variable Declarations
    private final WeakReference<JTable> weakReference;
    private final WeakReference<RowSorter<?>> rowSorterReference;
    private List<RowSorter.SortKey> sortKeys;
    // endregion Variable Declarations

    // region Constructors
    public JTablePreference(final JTable table) throws Exception {
        super(table.getName());

        weakReference = new WeakReference<>(table);
        final RowSorter<?> rowSorter = table.getRowSorter();
        rowSorterReference = new WeakReference<>(rowSorter);
        setSortKeys(rowSorter.getSortKeys());
        rowSorter.addRowSorterListener(this);
    }
    // endregion Constructors

    // region Getters/Setters
    public WeakReference<JTable> getWeakReference() {
        return weakReference;
    }

    /**
     * @return the sort keys to save, from the primary key down; never empty
     */
    public List<RowSorter.SortKey> getSortKeys() {
        return sortKeys;
    }

    /**
     * Keeps a copy of the given sort keys, leaving out unsorted keys. With no sort keys left it keeps the first column
     * ascending, as this preference always has.
     *
     * @param sortKeys the table's sort keys, from the primary key down
     */
    public void setSortKeys(final List<? extends RowSorter.SortKey> sortKeys) {
        final List<RowSorter.SortKey> keys = new ArrayList<>();
        for (final RowSorter.SortKey key : sortKeys) {
            if (key.getSortOrder() != SortOrder.UNSORTED) {
                keys.add(key);
            }
        }

        if (keys.isEmpty()) {
            keys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        }
        this.sortKeys = List.copyOf(keys);
    }

    /**
     * @return the model index of the primary sort column
     */
    public int getColumnIndex() {
        return getSortKeys().getFirst().getColumn();
    }

    /**
     * @return the sort order of the primary sort column
     */
    public SortOrder getSortOrder() {
        return getSortKeys().getFirst().getSortOrder();
    }
    // endregion Getters/Setters

    // region PreferenceElement
    @Override
    protected String getValue() {
        final List<String> parts = new ArrayList<>();
        for (final RowSorter.SortKey key : getSortKeys()) {
            parts.add(Integer.toString(key.getColumn()));
            parts.add(key.getSortOrder().name());
        }
        return String.join(SEPARATOR, parts);
    }

    @Override
    protected void initialize(final String value) throws Exception {
        if (StringUtility.isNullOrBlank(value)) {
            logger.error("Cannot create a JTablePreference because of a null or blank input value");
            throw new Exception();
        }

        final JTable element = getWeakReference().get();
        final RowSorter<?> rowSorter = rowSorterReference.get();
        if ((element == null) || (rowSorter == null)) {
            return;
        }

        final List<RowSorter.SortKey> keys = parseSortKeys(value, element.getModel().getColumnCount());
        if (keys.isEmpty()) {
            logger.warn("Cannot restore the sort of table {} from {}", getName(), value);
            return;
        }

        // the sort keys are taken up by the row sorter listener
        rowSorter.setSortKeys(keys);
    }

    /**
     * Reads the saved sort keys, leaving out any that are broken, refer to a column the table doesn't have, or repeat
     * a column already read.
     *
     * @param value       the saved value
     * @param columnCount the number of columns in the table's model
     *
     * @return the sort keys, from the primary key down
     */
    static List<RowSorter.SortKey> parseSortKeys(final String value, final int columnCount) {
        final String[] parts = value.split("\\|", -1);
        final List<RowSorter.SortKey> keys = new ArrayList<>();
        final Set<Integer> columns = new HashSet<>();

        for (int i = 0; i + 1 < parts.length; i += 2) {
            try {
                final int column = Integer.parseInt(parts[i].trim());
                final SortOrder sortOrder = SortOrder.valueOf(parts[i + 1].trim());
                if ((column >= 0) && (column < columnCount) && (sortOrder != SortOrder.UNSORTED)
                          && columns.add(column)) {
                    keys.add(new RowSorter.SortKey(column, sortOrder));
                }
            } catch (IllegalArgumentException ex) {
                logger.warn("Skipping a broken sort key in {}", value);
            }
        }
        return keys;
    }

    @Override
    protected void dispose() {
        final RowSorter<?> rowSorter = rowSorterReference.get();
        if (rowSorter != null) {
            rowSorter.removeRowSorterListener(this);
        }
        rowSorterReference.clear();
        getWeakReference().clear();
    }
    // endregion PreferenceElement

    // region RowSorterListener
    @Override
    public void sorterChanged(final RowSorterEvent event) {
        if (event.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED) {
            setSortKeys(event.getSource().getSortKeys());
        }
    }
    // endregion RowSorterListener
}
