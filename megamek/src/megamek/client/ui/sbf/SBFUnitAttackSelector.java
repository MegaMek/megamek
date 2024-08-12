/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.sbf;

import megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip;
import megamek.common.annotations.Nullable;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFUnit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

public class SBFUnitAttackSelector {

    private final DefaultListModel<SBFUnit> model = new DefaultListModel<>();
    private final JList<SBFUnit> unitSelector = new JList<>(model);
    private final ListSelectionListener listener;

    public SBFUnitAttackSelector(ListSelectionListener listener) {
        this.listener = listener;
        unitSelector.addListSelectionListener(listener);
        unitSelector.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        unitSelector.setAlignmentX(0);
        unitSelector.setCellRenderer(renderer);
        unitSelector.setOpaque(false);
        unitSelector.setBorder(new EmptyBorder(0, 3, 0, 0));
    }

    public void setFormation(@Nullable SBFFormation formation) {
        unitSelector.removeListSelectionListener(listener);
        model.clear();
        if (formation != null) {
            model.addAll(formation.getUnits());
        }
        unitSelector.addListSelectionListener(listener);
    }

    public JComponent getComponent() {
        return unitSelector;
    }

    public int getSelectedUnitIndex() {
        return unitSelector.getSelectedIndex();
    }

    private final DefaultListCellRenderer renderer = new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            SBFUnit unit = (SBFUnit) value;
            String unitLine = "<HTML><HEAD><STYLE>" +
                    SBFInGameObjectTooltip.styles() +
                    "</STYLE></HEAD><BODY>" + "<TABLE>" +
                    SBFInGameObjectTooltip.unitLine(unit) +
                    "</TABLE></BODY></HTML>";
            super.getListCellRendererComponent(list, unitLine, index, isSelected, cellHasFocus);
            if (!isSelected) {
                setBackground(UIManager.getColor("Label.background"));
            }
            return this;
        }
    };
}
