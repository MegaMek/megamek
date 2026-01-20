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
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import megamek.client.ratgenerator.RATGenerator;

import megamek.client.ui.Messages;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;

/**
 * This Random Army Dialog is shown in MML's force builder UI. It allows generating armies and adding them to the force.
 * It can be used by other callers that can supply a consumer for a list of Entities.
 */
public class MMLForceBuilderRandomArmyDialog extends AbstractRandomArmyDialog {

    // TODO: remember this dialog's size like in MM

    private final Consumer<List<Entity>> unitsReceiver;

    /**
     * Creates a random army dialog for the given parent frame. It has an "Add to force" button; when pressed, the given
     * consumer is called to accept the presently generated list of units and the dialog is closed.
     *
     * @param parent   A parent frame for the dialog
     * @param consumer A method that processes a generated unit list
     */
    public MMLForceBuilderRandomArmyDialog(JFrame parent, Consumer<List<Entity>> consumer) {
        super(parent);
        this.unitsReceiver = consumer;
        RATGenerator.getInstance();
    }

    @Override
    protected JComponent createButtonsPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(new JButton(addAndCloseAction));
        return buttonPanel;
    }

    Action addAndCloseAction = new AbstractAction(Messages.getString("RandomArmyDialog.AddToForce")) {
        @Override
        public void actionPerformed(ActionEvent e) {
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
    };
}
