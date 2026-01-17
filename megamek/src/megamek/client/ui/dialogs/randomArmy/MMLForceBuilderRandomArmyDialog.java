/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMekLab.
 *
 * MegaMekLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMekLab is distributed in the hope that it will be useful,
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
 * MechWarrior Copyright Microsoft Corporation. MegaMekLab was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.client.ui.dialogs.randomArmy;

import java.awt.FlowLayout;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import megamek.client.ratgenerator.RATGenerator;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;

/**
 * This Random Army Dialog is shown in MML's force builder UI. It allows generating armies and saving the chosen units
 * to a MUL file.
 */
public class MMLForceBuilderRandomArmyDialog extends AbstractRandomArmyDialog {

    private final Consumer<List<Entity>> unitsReceiver;

    public MMLForceBuilderRandomArmyDialog(JFrame parent, Consumer<List<Entity>> consumer) {
        super(parent);
        this.unitsReceiver = consumer;
        RATGenerator.getInstance();
    }

    @Override
    protected JComponent createButtonsPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton addButton = new JButton("Add to force");
        buttonPanel.add(addButton);
        addButton.addActionListener(e -> addAndClose());
        return buttonPanel;
    }

    private void addAndClose() {
        List<Entity> unitList;
        if (tabbedPane.getSelectedIndex() == TAB_FORCE_GENERATOR) {
            unitList = m_pForceGen.getChosenUnits();
        } else {
            unitList = armyModel.getAllUnits().stream().map(MekSummary::loadEntity).toList();
        }
        if (unitList != null && !unitList.isEmpty()) {
            unitsReceiver.accept(unitList);
            clearData();
            setVisible(false);
        }
    }
}
