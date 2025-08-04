/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;

import megamek.codeUtilities.StringUtility;
import megamek.logging.MMLogger;

/**
 * JTablePreference monitors the latest sort column and sort order of a JTable. It sets the saved values when a dialog
 * is loaded and changes them as they change.
 * <p>
 * Call preferences.manage(new JTablePreference(JTable)) to use this preference, on a JTable that has called setName
 */
public class JTablePreference extends PreferenceElement implements MouseListener {
    private final static MMLogger logger = MMLogger.create(JTablePreference.class);

    // region Variable Declarations
    private final WeakReference<JTable> weakReference;
    private int columnIndex;
    private SortOrder sortOrder;
    // endregion Variable Declarations

    // region Constructors
    public JTablePreference(final JTable table) throws Exception {
        super(table.getName());

        if (!table.getRowSorter().getSortKeys().isEmpty()) {
            setColumnIndex(table.getRowSorter().getSortKeys().get(0).getColumn());
            setSortOrder(table.getRowSorter().getSortKeys().get(0).getSortOrder());
        } else {
            setColumnIndex(0);
            setSortOrder(SortOrder.ASCENDING);
        }

        weakReference = new WeakReference<>(table);
        table.getTableHeader().addMouseListener(this);
    }
    // endregion Constructors

    // region Getters/Setters
    public WeakReference<JTable> getWeakReference() {
        return weakReference;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public void setColumnIndex(final int columnIndex) {
        this.columnIndex = columnIndex;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(final SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }
    // endregion Getters/Setters

    // region PreferenceElement
    @Override
    protected String getValue() {
        return String.format("%d|%s", getColumnIndex(), getSortOrder().name());
    }

    @Override
    protected void initialize(final String value) throws Exception {
        if (StringUtility.isNullOrBlank(value)) {
            logger.error("Cannot create a JTablePreference because of a null or blank input value");
            throw new Exception();
        }

        final JTable element = getWeakReference().get();
        if (element != null) {
            final String[] parts = value.split("\\|", -1);

            setColumnIndex(Integer.parseInt(parts[0]));
            setSortOrder(SortOrder.valueOf(parts[1]));

            final List<RowSorter.SortKey> sortKeys = new ArrayList<>();
            sortKeys.add(new RowSorter.SortKey(getColumnIndex(), getSortOrder()));

            element.getRowSorter().setSortKeys(sortKeys);
        }
    }

    @Override
    protected void dispose() {
        final JTable element = getWeakReference().get();
        if (element != null) {
            element.removeMouseListener(this);
            getWeakReference().clear();
        }
    }
    // endregion PreferenceElement

    // region MouseListener
    @Override
    public void mouseClicked(final MouseEvent evt) {
        final JTable table = getWeakReference().get();
        if (table != null) {
            final int uiIndex = table.getColumnModel().getColumnIndexAtX(evt.getX());
            if (uiIndex == -1) {
                return;
            }

            setColumnIndex(table.getColumnModel().getColumn(uiIndex).getModelIndex());
            for (final RowSorter.SortKey key : table.getRowSorter().getSortKeys()) {
                if (key.getColumn() == getColumnIndex()) {
                    setSortOrder(key.getSortOrder());
                    break;
                }
            }
        }
    }

    @Override
    public void mousePressed(final MouseEvent evt) {

    }

    @Override
    public void mouseReleased(final MouseEvent evt) {

    }

    @Override
    public void mouseEntered(final MouseEvent evt) {

    }

    @Override
    public void mouseExited(final MouseEvent evt) {

    }
    // endregion MouseListener
}
