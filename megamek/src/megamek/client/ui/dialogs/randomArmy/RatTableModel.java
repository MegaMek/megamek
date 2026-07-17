/*
 * Copyright (C) 2006 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2006-2026 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.dialogs.randomArmy;

import megamek.client.ui.Messages;
import megamek.common.loaders.MekSummary;

import javax.swing.table.AbstractTableModel;

/**
 * A table model for displaying a generated RAT
 */
public class RatTableModel extends AbstractTableModel {
    static final int COL_WEIGHT = 0;
    static final int COL_UNIT = 1;
    static final int COL_CL_IS = 2;
    static final int COL_BV = 3;
    static final int COL_UNIT_ROLE = 4;
    static final int N_COL = 5;

    private final RandomArmyRatGenTab randomArmyRatGenTab;

    public RatTableModel(RandomArmyRatGenTab randomArmyRatGenTab) {
        this.randomArmyRatGenTab = randomArmyRatGenTab;
    }

    @Override
    public int getRowCount() {
        return (randomArmyRatGenTab.generatedRAT == null) ? 0 : randomArmyRatGenTab.generatedRAT.getNumEntries();
    }

    public void refreshData() {
        fireTableDataChanged();
    }

    @Override
    public int getColumnCount() {
        return N_COL;
    }

    public int getPreferredWidth(int col) {
        return switch (col) {
            case COL_WEIGHT -> 12;
            case COL_UNIT -> 240;
            case COL_BV -> 18;
            case COL_CL_IS, COL_UNIT_ROLE -> 20;
            default -> 0;
        };
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case COL_WEIGHT -> Messages.getString("RandomArmyDialog.colWeight");
            case COL_UNIT -> Messages.getString("RandomArmyDialog.colUnit");
            case COL_BV -> Messages.getString("RandomArmyDialog.colBV");
            case COL_CL_IS -> Messages.getString("RandomArmyDialog.colCLIS");
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
        if (randomArmyRatGenTab.generatedRAT != null && randomArmyRatGenTab.generatedRAT.getNumEntries() > 0) {
            return switch (col) {
                case COL_WEIGHT -> randomArmyRatGenTab.generatedRAT.getEntryWeight(row);
                case COL_UNIT -> randomArmyRatGenTab.generatedRAT.getEntryText(row);
                case COL_BV -> {
                    int bv = randomArmyRatGenTab.generatedRAT.getBV(row);
                    yield (bv > 0) ? String.valueOf(bv) : "";
                }
                case COL_CL_IS -> randomArmyRatGenTab.generatedRAT.getTechBase(row);
                case COL_UNIT_ROLE -> randomArmyRatGenTab.generatedRAT.getUnitRole(row);
                default -> "";
            };
        }
        return "";
    }

    public MekSummary getUnitAt(int row) {
        if (randomArmyRatGenTab.generatedRAT != null && randomArmyRatGenTab.generatedRAT.getNumEntries() > 0) {
            return randomArmyRatGenTab.generatedRAT.getMekSummary(row);
        }
        return null;
    }
}
