/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
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
package megamek.client.ui.swing.lobby;

import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.Princess;
import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.dialogs.BotConfigDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.panels.SkillGenerationOptionsPanel;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.IStartingPositions;
import megamek.common.Player;
import megamek.common.options.OptionsConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static megamek.client.ui.Messages.getString;
import static megamek.client.ui.swing.lobby.LobbyUtility.isValidStartPos;
import static megamek.client.ui.swing.util.UIUtil.*;

/**
 * A dialog that can be used to adjust advanced player settings like initiative,
 * minefields, and maybe other things in the future like force abilities.
 * 
 * @author Jay Lawson
 * @author Simon (Juliez)
 */
public class PlayerSettingsDialog extends AbstractButtonDialog {

    public PlayerSettingsDialog(ClientGUI cg, Client cl) {
        super(cg.frame, "PlayerSettingsDialog", "PlayerSettingsDialog.title");
        client = cl;
        clientgui = cg;
        currentPlayerStartPos = cl.getLocalPlayer().getStartingPos();
        if (currentPlayerStartPos > 10) {
            currentPlayerStartPos -= 10;
        }
        initialize();
        UIUtil.adjustDialog(this);
    }
    
    /** Returns the chosen initiative modifier. */
    public int getInit() {
        return parseField(fldInit);
    }

    /** Returns the chosen conventional mines. */
    public int getCnvMines() {
        return parseField(fldConventional);
    }
    
    /** Returns the chosen inferno mines. */
    public int getInfMines() {
        return parseField(fldInferno);
    }
    
    /** Returns the chosen active mines. */
    public int getActMines() {
        return parseField(fldActive);
    }
    
    /** Returns the chosen vibrabombs. */
    public int getVibMines() {
        return parseField(fldVibrabomb);
    }
    
    /** Returns the chosen deployment position. */
    public int getStartPos() {
        return currentPlayerStartPos;
    }

    /**
     * @return the current {@link SkillGenerationOptionsPanel}
     */
    public SkillGenerationOptionsPanel getSkillGenerationOptionsPanel() {
        return skillGenerationOptionsPanel;
    }

    /** Returns the player's email address. */
    public String getEmail() {
        return fldEmail.getText().trim();
    }

    // PRIVATE

    private final Client client;
    private final ClientGUI clientgui;
    
    // Initiative Section
    private final JLabel labInit = new TipLabel(Messages.getString("PlayerSettingsDialog.initMod"), SwingConstants.RIGHT);
    private final TipTextField fldInit = new TipTextField(3);

    // Mines Section
    private final JLabel labConventional = new JLabel(getString("PlayerSettingsDialog.labConventional"), SwingConstants.RIGHT);
    private final JLabel labVibrabomb = new JLabel(getString("PlayerSettingsDialog.labVibrabomb"), SwingConstants.RIGHT);
    private final JLabel labActive = new JLabel(getString("PlayerSettingsDialog.labActive"), SwingConstants.RIGHT);
    private final JLabel labInferno = new JLabel(getString("PlayerSettingsDialog.labInferno"), SwingConstants.RIGHT);
    private final JTextField fldConventional = new JTextField(3);
    private final JTextField fldVibrabomb = new JTextField(3);
    private final JTextField fldActive = new JTextField(3);
    private final JTextField fldInferno = new JTextField(3);

    // Skills Section
    private SkillGenerationOptionsPanel skillGenerationOptionsPanel;

    // Email section
    private final JLabel labEmail = new JLabel(getString("PlayerSettingsDialog.labEmail"), SwingConstants.RIGHT);
    private final JTextField fldEmail = new JTextField(20);

    // Deployment Section
    private final JPanel panStartButtons = new JPanel();
    private final TipButton[] butStartPos = new TipButton[11];

    // Bot Settings Section
    private final JButton butBotSettings = new JButton(Messages.getString("PlayerSettingsDialog.botSettings"));
    
    private int currentPlayerStartPos;

    @Override
    protected Container createCenterPane() {
        setupValues();
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(headerSection());
        if (client instanceof BotClient) {
            mainPanel.add(botSection());
        }
        mainPanel.add(startSection());
        mainPanel.add(initiativeSection());
        if (client.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_MINEFIELDS)) {
            mainPanel.add(mineSection());
        }
        mainPanel.add(skillsSection());
        if (!(client instanceof BotClient)) {
            mainPanel.add(emailSection());
        }
        mainPanel.add(Box.createVerticalGlue());

        var scrMain = new JScrollPane(mainPanel);
        scrMain.getVerticalScrollBar().setUnitIncrement(16);
        scrMain.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrMain.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrMain.setBorder(null);
        return scrMain;
    }
    
    private JPanel headerSection() {
        JPanel result = new FixedYPanel();
        result.setAlignmentX(Component.LEFT_ALIGNMENT);
        Icon playerIcon = client.getLocalPlayer().getCamouflage().getImageIcon(UIUtil.scaleForGUI(40));
        JLabel playerLabel = new JLabel(client.getLocalPlayer().getName(), playerIcon, SwingConstants.CENTER);
        playerLabel.setIconTextGap(UIUtil.scaleForGUI(12));
        playerLabel.setBorder(new EmptyBorder(15, 0, 10, 0));
        result.add(playerLabel);
        return result;
    }

    private JPanel botSection() {
        JPanel result = new OptionPanel("PlayerSettingsDialog.header.botPlayer");
        Content panContent = new Content(new FlowLayout());
        result.add(panContent);
        panContent.add(butBotSettings);
        butBotSettings.addActionListener(listener);
        return result;
    }
    
    private JPanel startSection() {
        JPanel result = new OptionPanel("PlayerSettingsDialog.header.startPos");
        Content panContent = new Content(new GridLayout(1, 1));
        result.add(panContent);
        setupStartGrid();
        panContent.add(panStartButtons);
        return result;
    }
    
    private JPanel initiativeSection() {
        JPanel result = new OptionPanel("PlayerSettingsDialog.header.initMod");
        Content panContent = new Content(new GridLayout(1, 2, 10, 5));
        result.add(panContent);
        panContent.add(labInit);
        panContent.add(fldInit);
        labInit.setToolTipText(Messages.getString("PlayerSettingsDialog.initModTT"));
        fldInit.setToolTipText(Messages.getString("PlayerSettingsDialog.initModTT"));
        return result;
    }

    private JPanel mineSection() {
        JPanel result = new OptionPanel("PlayerSettingsDialog.header.minefields");
        Content panContent = new Content(new GridLayout(4, 2, 10, 5));
        result.add(panContent);
        panContent.add(labConventional);
        panContent.add(fldConventional);
        panContent.add(labVibrabomb);
        panContent.add(fldVibrabomb);
        panContent.add(labActive);
        panContent.add(fldActive);
        panContent.add(labInferno);
        panContent.add(fldInferno);
        return result;
    }

    private JPanel skillsSection() {
        final JPanel skillsPanel = new OptionPanel("PlayerSettingsDialog.header.skills");
        skillsPanel.setName("skillsPanel");

        skillGenerationOptionsPanel = new SkillGenerationOptionsPanel(clientgui.getFrame(), clientgui, client);
        skillGenerationOptionsPanel.setBorder(BorderFactory.createEmptyBorder(8, 25, 5, 25));
        skillGenerationOptionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        skillsPanel.add(skillGenerationOptionsPanel);

        return skillsPanel;
    }

    private JPanel emailSection() {
        JPanel result = new OptionPanel("PlayerSettingsDialog.header.email");
        Content panContent = new Content(new GridLayout(1, 2, 10, 5));
        result.add(panContent);
        panContent.add(labEmail);
        panContent.add(fldEmail);
        return result;
    }

    private void setupValues() {
        Player player = client.getLocalPlayer();
        fldInit.setText(Integer.toString(player.getConstantInitBonus()));
        fldConventional.setText(Integer.toString(player.getNbrMFConventional()));
        fldVibrabomb.setText(Integer.toString(player.getNbrMFVibra()));
        fldActive.setText(Integer.toString(player.getNbrMFActive()));
        fldInferno.setText(Integer.toString(player.getNbrMFInferno()));
        fldEmail.setText(player.getEmail());
    }

    private void setupStartGrid() {
        panStartButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < 11; i++) {
            butStartPos[i] = new TipButton("");
            butStartPos[i].addActionListener(listener);
        }
        panStartButtons.setLayout(new GridLayout(4, 3));
        panStartButtons.add(butStartPos[1]);
        panStartButtons.add(butStartPos[2]);
        panStartButtons.add(butStartPos[3]);
        panStartButtons.add(butStartPos[8]);
        panStartButtons.add(butStartPos[10]);
        panStartButtons.add(butStartPos[4]);
        panStartButtons.add(butStartPos[7]);
        panStartButtons.add(butStartPos[6]);
        panStartButtons.add(butStartPos[5]);
        panStartButtons.add(butStartPos[0]);
        panStartButtons.add(butStartPos[9]);
        updateStartGrid();
    }
    
    /** Assigns texts and tooltips to the starting positions grid. */
    private void updateStartGrid() {
        StringBuilder[] butText = new StringBuilder[11];
        StringBuilder[] butTT = new StringBuilder[11];
        boolean[] hasPlayer = new boolean[11];

        for (int i = 0; i < 11; i++) {
            butText[i] = new StringBuilder();
            butTT[i] = new StringBuilder();
        }

        for (int i = 0; i < 11; i++) {
            butText[i].append("<HTML><P ALIGN=CENTER>");
            if (!isValidStartPos(client.getGame(), client.getLocalPlayer(), i)) {
                butText[i].append(guiScaledFontHTML(uiYellow()));
                butTT[i].append(Messages.getString("PlayerSettingsDialog.invalidStartPosTT"));
            } else {
                butText[i].append(guiScaledFontHTML());
            }
            butText[i].append(IStartingPositions.START_LOCATION_NAMES[i]).append("</FONT><BR>");
        }

        for (Player player : client.getGame().getPlayersVector()) {
            int pos = player.getStartingPos(); 
            if (!player.equals(client.getLocalPlayer()) && (pos >= 0) && (pos <= 19)) { 
                int index = pos > 10 ? pos - 10 : pos;
                butText[index].append(guiScaledFontHTML(teamColor(player, client.getLocalPlayer())));
                butText[index].append("\u25A0</FONT>");
                if (!hasPlayer[index]) {
                    if (butTT[index].length() > 0) {
                        butTT[index].append("<BR><BR>");
                    }
                    butTT[index].append(Messages.getString("PlayerSettingsDialog.deployingHere"));
                    hasPlayer[index] = true;
                }
                butTT[index].append("<BR>").append(player.getName());
            }
        }
        
        butText[currentPlayerStartPos].append(guiScaledFontHTML(GUIPreferences.getInstance().getMyUnitColor()));
        butText[currentPlayerStartPos].append("\u2B24</FONT>");

        for (int i = 0; i < 11; i++) {
            butStartPos[i].setText(butText[i].toString());
            if (butTT[i].length() > 0) {
                butStartPos[i].setToolTipText(butTT[i].toString());
            }
        }
    }

    ActionListener listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Deployment buttons
            for (int i = 0; i < 11; i++) {
                if (butStartPos[i].equals(e.getSource())) {
                    currentPlayerStartPos = i;
                    updateStartGrid();
                }
            }

            // Bot settings button
            if (butBotSettings.equals(e.getSource()) && client instanceof Princess) {
                BehaviorSettings behavior = ((Princess) client).getBehaviorSettings();
                var bcd = new BotConfigDialog(clientgui.frame, client.getLocalPlayer().getName(), behavior, clientgui);
                bcd.setVisible(true);
                if (bcd.getResult() == DialogResult.CONFIRMED) {
                    ((Princess) client).setBehaviorSettings(bcd.getBehaviorSettings());
                }
            }
        }
    };
    
    /** 
     * Parse the given field and return the integer it contains or 0, if
     * the field cannot be parsed.
     */
    private int parseField(JTextField field) {
        try {
            return Integer.parseInt(field.getText());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

}
