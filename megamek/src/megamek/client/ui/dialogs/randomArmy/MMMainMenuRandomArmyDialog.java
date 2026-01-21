/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
import megamek.common.units.Entity;
import megamek.common.units.EntityListFile;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static megamek.client.ui.clientGUI.ClientGUI.CG_FILEPATH_MUL;

/**
 * This Random Army Dialog is shown in MM's main menu. It allows generating armies and saving the chosen units to a MUL
 * file. It can be used in other places.
 */
public class MMMainMenuRandomArmyDialog extends AbstractRandomArmyDialog {

    /**
     * Creates a random army dialog for the given parent frame. It has a button that allows saving the chosen units
     * to a MUL file.
     *
     * @param parent   A parent frame for the dialog
     */
    public MMMainMenuRandomArmyDialog(JFrame parent) {
        super(parent);
    }

    @Override
    protected JComponent createButtonsPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(new JButton(saveAction));
        return buttonPanel;
    }

    // TODO: Unify MUL saving

    Action saveAction = new AbstractAction(Messages.getString("ChatLounge.butSaveList")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            List<Entity> unitList;
            if (tabbedPane.getSelectedIndex() == TAB_FORCE_GENERATOR) {
                unitList = m_pForceGen.getChosenUnits();
            } else {
                unitList = armyModel.getAllUnits().stream().map(MekSummary::loadEntity).toList();
            }
            if (unitList == null || unitList.isEmpty()) {
                return;
            }

            var dlgSaveList = new JFileChooser(".");

            dlgSaveList.setDialogTitle(Messages.getString("ClientGUI.saveUnitListFileDialog.title"));
            FileNameExtensionFilter filter = new FileNameExtensionFilter(Messages.getString(
                  "ClientGUI.descriptionMULFiles"), CG_FILEPATH_MUL);
            dlgSaveList.setFileFilter(filter);

            int returnVal = dlgSaveList.showSaveDialog(parentFrame);
            File unitFile = dlgSaveList.getSelectedFile();
            if ((returnVal != JFileChooser.APPROVE_OPTION) || (unitFile == null)) {
                return;
            }

            try {
                EntityListFile.saveTo(unitFile, new ArrayList<>(unitList));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parentFrame,
                      "Error saving unitListFile",
                      "Error",
                      JOptionPane.ERROR_MESSAGE);
            }
        }
   };
}
