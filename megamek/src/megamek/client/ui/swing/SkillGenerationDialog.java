/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing;

import megamek.client.Client;
import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.panels.SkillGenerationOptionsPanel;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

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
        clientsModel.addAll(getClientGUI().getBots().values().stream().map(Client::getName)
                .collect(Collectors.toList()));
        final MMComboBox<String> comboClients = new MMComboBox<>("comboClients", clientsModel);
        comboClients.setToolTipText(resources.getString("comboClients.toolTipText"));
        comboClients.setSelectedItem(getClientGUI().getClient());
        comboClients.setEnabled(comboClients.getItemCount() > 1);
        comboClients.addActionListener(evt -> getSkillGenerationOptionsPanel().changeClient(
                (comboClients.getSelectedIndex() > 0)
                        ? getClientGUI().getBots().get(comboClients.getSelectedItem())
                        : getClientGUI().getClient()));
        panel.add(comboClients);
        return panel;
    }
    //endregion Initialization

    @Override
    protected void finalizeInitialization() throws Exception {
        super.finalizeInitialization();
        adaptToGUIScale();
    }

    //region Button Actions
    @Override
    protected void okAction() {
        super.okAction();
        getSkillGenerationOptionsPanel().updateClient();
        clientGUI.getClient().sendChat(clientGUI.getClient().getLocalPlayer() + " changed settings for Skill Generation");
    }
    //endregion Button Actions

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this, UIUtil.FONT_SCALE1);
    }
}
