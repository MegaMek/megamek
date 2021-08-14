/*
 * Copyright (c) 2020 - The MegaMek Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek.  If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.dialog;

import megamek.client.Client;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.UnitFailureDialog;
import megamek.client.ui.swing.UnitLoadingDialog;
import megamek.common.*;
import megamek.common.enums.Gender;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class MegaMekUnitSelectorDialog extends AbstractUnitSelectorDialog {

    private static final long serialVersionUID = -5717009055093904636L;
    //region Variable Declarations
    private ClientGUI clientGUI;
    private JComboBox<String> comboPlayer = new JComboBox<>();
    //endregion Variable Declarations

    public MegaMekUnitSelectorDialog(ClientGUI clientGUI, UnitLoadingDialog unitLoadingDialog) {
        super(clientGUI.getFrame(), unitLoadingDialog);
        this.clientGUI = clientGUI;

        updateOptionValues();

        initialize();
    }

    @Override
    public void updateOptionValues() {
        gameOptions = clientGUI.getClient().getGame().getOptions();
        enableYearLimits = gameOptions.booleanOption(OptionsConstants.ALLOWED_ERA_BASED);
        allowedYear = gameOptions.intOption(OptionsConstants.ALLOWED_YEAR);
        canonOnly = gameOptions.booleanOption(OptionsConstants.ALLOWED_CANON_ONLY);
        allowInvalid = gameOptions.booleanOption(OptionsConstants.ALLOWED_ALLOW_ILLEGAL_UNITS);
        gameTechLevel = TechConstants.getSimpleLevel(gameOptions.stringOption("techlevel"));
    }

    //region Button Methods
    @Override
    protected JPanel createButtonsPanel() {
        JPanel panelButtons = new JPanel(new GridBagLayout());

        buttonSelect = new JButton(Messages.getString("MechSelectorDialog.m_bPick"));
        buttonSelect.addActionListener(this);
        panelButtons.add(buttonSelect, new GridBagConstraints());

        buttonSelectClose = new JButton(Messages.getString("MechSelectorDialog.m_bPickClose"));
        buttonSelectClose.addActionListener(this);
        panelButtons.add(buttonSelectClose, new GridBagConstraints());

        buttonClose = new JButton(Messages.getString("Close"));
        buttonClose.addActionListener(this);
        panelButtons.add(buttonClose, new GridBagConstraints());

        JLabel labelPlayer = new JLabel(Messages.getString("MechSelectorDialog.m_labelPlayer"),
                SwingConstants.RIGHT);
        panelButtons.add(labelPlayer, new GridBagConstraints());

        panelButtons.add(comboPlayer, new GridBagConstraints());

        buttonShowBV = new JButton(Messages.getString("MechSelectorDialog.BV"));
        buttonShowBV.addActionListener(this);
        panelButtons.add(buttonShowBV, new GridBagConstraints());

        return panelButtons;
    }

    @Override
    protected void select(boolean close) {
        Entity e = getSelectedEntity();
        if (e != null) {
            Client client = null;
            if (comboPlayer.getSelectedIndex() > 0) {
                String name = (String) comboPlayer.getSelectedItem();
                client = clientGUI.getBots().get(name);
            }

            if (client == null) {
                client = clientGUI.getClient();
            }
            autoSetSkillsAndName(e, client.getLocalPlayer());
            e.setOwner(client.getLocalPlayer());
            client.sendAddEntity(e);
        }
        if (close) {
            setVisible(false);
        }
    }

    private void autoSetSkillsAndName(Entity e, IPlayer player) {
        IClientPreferences cs = PreferenceManager.getClientPreferences();

        if (cs.useAverageSkills()) {
            clientGUI.getClient().getSkillGenerator().setRandomSkills(e, true);
        }

        for (int i = 0; i < e.getCrew().getSlotCount(); i++) {
            if (cs.generateNames()) {
                Gender gender = RandomGenderGenerator.generate();
                e.getCrew().setGender(gender, i);
                e.getCrew().setName((player != null)
                        ? RandomNameGenerator.getInstance().generate(gender, player.getName())
                        : RandomNameGenerator.getInstance().generate(gender), i);
            }
        }
    }

    private void updatePlayerChoice() {
        String lastChoice = (String) comboPlayer.getSelectedItem();
        String clientName = clientGUI.getClient().getName();
        comboPlayer.removeAllItems();
        comboPlayer.setEnabled(true);
        comboPlayer.addItem(clientName);
        for (Client client : clientGUI.getBots().values()) {
            comboPlayer.addItem(client.getName());
        }
        if (comboPlayer.getItemCount() == 1) {
            comboPlayer.setEnabled(false);
        }
        comboPlayer.setSelectedItem(lastChoice);
        if (comboPlayer.getSelectedIndex() < 0) {
            comboPlayer.setSelectedIndex(0);
        }
    }
    //endregion Button Methods

    @Override
    protected Entity refreshUnitView() {
        Entity selectedEntity = super.refreshUnitView(); //we first want it to run through the same code as its parent
        if (selectedEntity != null) {
            clientGUI.loadPreviewImage(labelImage, selectedEntity, clientGUI.getClient().getLocalPlayer());
        }
        return selectedEntity;
    }

    @Override
    public void run() {
        super.run();
        // In some cases, it's possible to get here without an initialized
        // instance (loading a saved game without a cache).  In these cases,
        // we don't care about the failed loads.
        if (mscInstance.isInitialized()) {
            final Map<String, String> hFailedFiles = MechSummaryCache.getInstance().getFailedFiles();
            if ((hFailedFiles != null) && (hFailedFiles.size() > 0)) {
                // self-showing dialog
                new UnitFailureDialog(frame, hFailedFiles);
            }
        }
    }

    @Override
    public void setVisible(boolean visible) {
        // Set the cursor in the text filter and mark the content so it can be directly replaced
        textFilter.grabFocus();
        textFilter.select(0, textFilter.getText().length());
        updatePlayerChoice();
        super.setVisible(visible);
    }
}
