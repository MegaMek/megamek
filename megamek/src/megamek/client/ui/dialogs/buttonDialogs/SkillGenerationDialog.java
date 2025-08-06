/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.buttonDialogs;

import java.awt.Container;
import java.awt.GridLayout;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.client.AbstractClient;
import megamek.client.Client;
import megamek.client.ui.buttons.MMButton;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.panels.abstractPanels.SkillGenerationOptionsPanel;
import megamek.common.Entity;
import megamek.common.Player;

public class SkillGenerationDialog extends AbstractButtonDialog {
    //region Variable Declarations
    private final ClientGUI clientGUI;
    private final List<Entity> entities;
    private SkillGenerationOptionsPanel skillGenerationOptionsPanel;
    //endregion Variable Declarations

    //region Constructors
    public SkillGenerationDialog(final JFrame frame, final ClientGUI clientGUI,
          final List<Entity> entities) {
        super(frame, "SkillGenerationDialog", "SkillGenerationDialog.title");
        this.clientGUI = clientGUI;
        this.entities = entities;
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public ClientGUI getClientGUI() {
        return clientGUI;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public SkillGenerationOptionsPanel getSkillGenerationOptionsPanel() {
        return skillGenerationOptionsPanel;
    }

    public void setSkillGenerationOptionsPanel(final SkillGenerationOptionsPanel skillGenerationOptionsPanel) {
        this.skillGenerationOptionsPanel = skillGenerationOptionsPanel;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        setSkillGenerationOptionsPanel(new SkillGenerationOptionsPanel(getFrame(), getClientGUI(), null));

        final JScrollPane scrollPane = new JScrollPane(getSkillGenerationOptionsPanel());
        scrollPane.setName("skillGenerationPane");
        return scrollPane;
    }

    @Override
    protected JPanel createButtonPanel() {
        final JPanel panel = new JPanel(new GridLayout(1, 0));

        if (!getEntities().isEmpty()) {
            panel.add(new MMButton("btnRandomize", resources.getString("Randomize.text"),
                  resources.getString("SkillGenerationDialog.btnRandomize.toolTipText"),
                  evt -> {
                      getSkillGenerationOptionsPanel().updateClient();
                      final Client client = getSkillGenerationOptionsPanel().getClient();
                      for (final Entity entity : getEntities()) {
                          if (entity.getOwnerId() != client.getLocalPlayer().getId()) {
                              continue;
                          }

                          client.getSkillGenerator().setRandomSkills(entity);
                          client.sendUpdateEntity(entity);
                      }
                      setResult(DialogResult.CONFIRMED);
                      setVisible(false);
                  }));
        }

        panel.add(new MMButton("btnSave", resources.getString("Save.text"),
              resources.getString("SkillGenerationDialog.btnSave.toolTipText"),
              this::okButtonActionPerformed));

        panel.add(new MMButton("btnCancel", resources.getString("Cancel.text"),
              resources.getString("Cancel.toolTipText"), this::cancelActionPerformed));

        final DefaultComboBoxModel<String> clientsModel = new DefaultComboBoxModel<>();
        clientsModel.addElement(getClientGUI().getClient().getName());
        clientsModel.addAll(getClientGUI().getLocalBots().values().stream().map(AbstractClient::getName)
              .collect(Collectors.toList()));
        final MMComboBox<String> comboClients = new MMComboBox<>("comboClients", clientsModel);
        comboClients.setToolTipText(resources.getString("comboClients.toolTipText"));
        comboClients.setSelectedItem(getClientGUI().getClient());
        comboClients.setEnabled(comboClients.getItemCount() > 1);
        comboClients.addActionListener(evt -> getSkillGenerationOptionsPanel().changeClient(
              (comboClients.getSelectedIndex() > 0)
                    ? (Client) getClientGUI().getLocalBots().get(comboClients.getSelectedItem())
                    : getClientGUI().getClient()));
        panel.add(comboClients);
        return panel;
    }
    //endregion Initialization


    //region Button Actions
    @Override
    protected void okAction() {
        super.okAction();
        getSkillGenerationOptionsPanel().updateClient();
        String msg = clientGUI.getClient().getLocalPlayer() + " changed settings for Skill Generation";
        clientGUI.getClient().sendServerChat(Player.PLAYER_NONE, msg);
    }
    //endregion Button Actions
}
