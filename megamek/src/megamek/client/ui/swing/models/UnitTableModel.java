/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.client.ui.swing.models;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

import megamek.client.ui.Messages;
import megamek.common.MekSummary;

/**
 * A table model for displaying units
 */
public class UnitTableModel extends AbstractTableModel {
    private static final int COL_UNIT = 0;
    private static final int COL_BV = 1;
    private static final int COL_MOVE = 2;
    private static final int COL_TECH_BASE = 3;
    private static final int COL_UNIT_ROLE = 4;
    private static final int N_COL = 5;

    private List<MekSummary> data;

    public UnitTableModel() {
        data = new ArrayList<>();
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    public void clearData() {
        data = new ArrayList<>();
        fireTableDataChanged();
    }

    @Override
    public int getColumnCount() {
        return N_COL;
    }

    public void addUnit(MekSummary m) {
        data.add(m);
        fireTableDataChanged();
    }

    public void setData(List<MekSummary> meks) {
        data = meks;
        fireTableDataChanged();
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case COL_UNIT -> Messages.getString("RandomArmyDialog.colUnit");
            case COL_MOVE -> Messages.getString("RandomArmyDialog.colMove");
            case COL_BV -> Messages.getString("RandomArmyDialog.colBV");
            case COL_TECH_BASE -> Messages.getString("RandomArmyDialog.colTechBase");
            case COL_UNIT_ROLE -> Messages.getString("RandomArmyDialog.colUnitRole");
            default -> "??";
        };
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    @Override
    public Object getValueAt(int row, int col) {
        MekSummary m = getUnitAt(row);

        if (m == null) {
            return "?";
        }

        String value = "";

        if (col == COL_BV) {
            value += m.getBV();
        } else if (col == COL_MOVE) {
            value += m.getWalkMp() + "/" + m.getRunMp() + "/" + m.getJumpMp();
        } else if (col == COL_TECH_BASE) {
            value += m.getTechBase();
        } else if (col == COL_UNIT_ROLE) {
            value += m.getRole();
        } else {
            return m.getName();
        }

        return value;
    }

    public MekSummary getUnitAt(int row) {
        if (data.size() <= row) {
            return null;
        }

        return data.get(row);
    }

    public List<MekSummary> getAllUnits() {
        return data;
    }
}
