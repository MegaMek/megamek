/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.advancedsearch;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

import megamek.common.TechConstants;
import megamek.common.equipment.WeaponType;

/**
 * A table model for displaying weapons
 */
class WeaponsTableModel extends AbstractTableModel {

    static final int COL_NAME = 0;
    static final int COL_DMG = 1;
    static final int COL_HEAT = 2;
    static final int COL_MIN = 3;
    static final int COL_SHORT = 4;
    static final int COL_MED = 5;
    static final int COL_LONG = 6;
    static final int COL_IS_CLAN = 7;
    static final int COL_LEVEL = 8;
    static final int N_COL = 9;
    static final int COL_INTERNAL_NAME = 9;

    private final TWAdvancedSearchPanel twAdvancedSearchPanel;
    private final List<WeaponType> weapons = new ArrayList<>();

    WeaponsTableModel(TWAdvancedSearchPanel twAdvancedSearchPanel) {
        this.twAdvancedSearchPanel = twAdvancedSearchPanel;
    }

    @Override
    public int getRowCount() {
        return weapons.size();
    }

    @Override
    public int getColumnCount() {
        return N_COL;
    }

    int getPreferredWidth(int col) {
        return switch (col) {
            case COL_NAME -> 310;
            case COL_IS_CLAN -> 75;
            case COL_DMG, COL_HEAT, COL_SHORT, COL_MED, COL_LONG, COL_MIN -> 50;
            case COL_LEVEL -> 100;
            default -> 0;
        };
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case COL_NAME -> "Weapon Name";
            case COL_IS_CLAN -> "Class";
            case COL_DMG -> "Dmg";
            case COL_MIN -> "Min";
            case COL_HEAT -> "Heat";
            case COL_SHORT -> "S";
            case COL_MED -> "M";
            case COL_LONG -> "L";
            case COL_LEVEL -> "Lvl";
            default -> "?";
        };
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    void setData(List<WeaponType> wps) {
        weapons.clear();
        weapons.addAll(wps);
        fireTableDataChanged();
    }

    WeaponType getWeaponTypeAt(int row) {
        return weapons.get(row);
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (row >= weapons.size()) {
            return null;
        }
        WeaponType wp = weapons.get(row);
        return switch (col) {
            case COL_NAME -> wp.getName();
            case COL_IS_CLAN -> TechConstants.getTechName(wp.getTechLevel(twAdvancedSearchPanel.gameYear));
            case COL_DMG -> wp.getDamage();
            case COL_HEAT -> wp.getHeat();
            case COL_MIN -> Math.max(wp.getMinimumRange(), 0);
            case COL_SHORT -> wp.getShortRange();
            case COL_MED -> wp.getMediumRange();
            case COL_LONG -> wp.getLongRange();
            case COL_LEVEL -> TechConstants.getSimpleLevelName(
                  TechConstants.convertFromNormalToSimple(wp.getTechLevel(twAdvancedSearchPanel.gameYear)));
            case COL_INTERNAL_NAME -> wp.getInternalName();
            default -> "?";
        };
    }
}
