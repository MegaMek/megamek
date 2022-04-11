/*
 * Copyright (c) 2019-2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.preferences;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * JTablePreference monitors the latest sort column and sort order of a JTable. It sets the saved
 * values when a dialog is loaded and changes them as they change.
 *
 * Call preferences.manage(new JTablePreference(JTable)) to use this preference, on a JTable that
 * has called setName
 */
public class JTablePreference extends PreferenceElement implements MouseListener {
    //region Variable Declarations
    private final WeakReference<JTable> weakReference;
    private int columnIndex;
    private SortOrder sortOrder;
    //endregion Variable Declarations

    //region Constructors
    public JTablePreference(final JTable table) {
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
    //endregion Constructors

    //region Getters/Setters
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
    //endregion Getters/Setters

    //region PreferenceElement
    @Override
    protected String getValue() {
        return String.format("%d|%s", getColumnIndex(), getSortOrder().name());
    }

    @Override
    protected void initialize(final String value) {
        assert (value != null) && !value.isBlank();

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
    //endregion PreferenceElement

    //region MouseListener
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
    //endregion MouseListener
}
