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

package megamek.client.ui.sbf;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;

import megamek.client.ui.clientGUI.tooltip.SBFInGameObjectTooltip;
import megamek.common.annotations.Nullable;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFUnit;

public class SBFUnitAttackSelector {

    private final DefaultListModel<SBFUnit> model = new DefaultListModel<>();
    private final JList<SBFUnit> unitSelector = new JList<>(model);
    private final ListSelectionListener listener;

    public SBFUnitAttackSelector(ListSelectionListener listener) {
        this.listener = listener;
        unitSelector.addListSelectionListener(listener);
        unitSelector.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        unitSelector.setAlignmentX(0);
        DefaultListCellRenderer renderer = new DefaultListCellRenderer() {
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

}
