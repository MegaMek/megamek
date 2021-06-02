/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.Princess;
import megamek.client.generator.RandomSkillsGenerator;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.*;
import megamek.client.ui.swing.dialog.DialogButton;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.options.OptionsConstants;
import static megamek.client.ui.swing.lobby.LobbyUtility.*;
import static megamek.client.ui.swing.util.UIUtil.*;
import static megamek.client.ui.Messages.getString;

/**
 * A dialog that can be used to adjust advanced player settings like initiative,
 * minefields, and maybe other things in the future like force abilities.
 * 
 * @author Jay Lawson
 */
public class PlayerSettingsDialog extends ClientDialog {

    private static final long serialVersionUID = -4597870528499580517L;
    
    public PlayerSettingsDialog(ClientGUI cg, Client cl) {
        super(cg.frame, Messages.getString("PlayerSettingsDialog.title"), true, true);
        client = cl;
        clientgui = cg;
        currentPlayerStartPos = cl.getLocalPlayer().getStartingPos();
        if (currentPlayerStartPos > 10) {
            currentPlayerStartPos -= 10;
        }
        setupDialog();
    }
    
    /** Sets the dialog visible and returns true if the user pressed the Okay button. */
    public boolean showDialog() {
        userResponse = false;
        setVisible(true);
        return userResponse;
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
    
    /** Returns the chosen random skill roll method. */
    public int getMethod() {
        return cmbMethod.getSelectedIndex();
    }
    
    /** Returns the chosen random skill roll pilot type. */
    public int getPilot() {
        return cmbPilot.getSelectedIndex();
    }
    
    /** Returns the chosen random skill roll experience. */
    public int getXP() {
        return cmbXP.getSelectedIndex();
    }
    
    /** Returns the chosen random skill roll experience. */
    public boolean getForceGP() {
        return butForceGP.isSelected();
    }
    
    // PRIVATE

    private final Client client;
    private final ClientGUI clientgui;
    
    private static final int TOOLTIP_WIDTH = 300;
    private static final String PSD = "PlayerSettingsDialog.";
    
    // Initiative Section
    private JLabel labInit = new TipLabel(Messages.getString(PSD + "initMod"), SwingConstants.RIGHT, this);
    private TipTextField fldInit = new TipTextField(3, this);

    // Mines Section
    private JLabel labConventional = new JLabel(getString(PSD + "labConventional"), SwingConstants.RIGHT); 
    private JLabel labVibrabomb = new JLabel(getString(PSD + "labVibrabomb"), SwingConstants.RIGHT); 
    private JLabel labActive = new JLabel(getString(PSD + "labActive"), SwingConstants.RIGHT); 
    private JLabel labInferno = new JLabel(getString(PSD + "labInferno"), SwingConstants.RIGHT); 
    private JTextField fldConventional = new JTextField(3);
    private JTextField fldVibrabomb = new JTextField(3);
    private JTextField fldActive = new JTextField(3);
    private JTextField fldInferno = new JTextField(3);

    // Skills Section
    private JLabel labMethod = new JLabel(getString(PSD + "labMethod"), SwingConstants.RIGHT); 
    private JLabel labPilot = new JLabel(getString(PSD + "labPilot"), SwingConstants.RIGHT); 
    private JLabel labXP = new JLabel(getString(PSD + "labXP"), SwingConstants.RIGHT);
    private TipCombo<String> cmbMethod = new TipCombo<String>(this);
    private JComboBox<String> cmbPilot = new JComboBox<String>();
    private JComboBox<String> cmbXP = new JComboBox<String>();
    private MMToggleButton butForceGP = new MMToggleButton(getString(PSD + "butForceGP"));
    
    private JPanel panStartButtons = new JPanel();
    private TipButton[] butStartPos = new TipButton[11];
    private JButton butBotSettings = new JButton(Messages.getString(PSD + "botSettings"));
    private DialogButton butOkay = new DialogButton(Messages.getString("Okay"));
    
    private int currentPlayerStartPos;
    
    private boolean userResponse;
    
    private void setupDialog() {
        setupValues();
        
        JPanel mainPanel = new JPanel();
        ContentScrollPane scrMain = new ContentScrollPane(mainPanel);
        add(scrMain, BorderLayout.CENTER);
        add(buttonPanel(), BorderLayout.PAGE_END);
        
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
        mainPanel.add(Box.createVerticalGlue());
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
        JPanel result = new OptionPanel(PSD + "header.botPlayer");
        Content panContent = new Content(new FlowLayout());
        result.add(panContent);
        panContent.add(butBotSettings);
        butBotSettings.addActionListener(listener);
        return result;
    }
    
    private JPanel startSection() {
        JPanel result = new OptionPanel(PSD + "header.startPos");
        Content panContent = new Content(new GridLayout(1, 1));
        result.add(panContent);
        setupStartGrid();
        panContent.add(panStartButtons);
        return result;
    }
    
    private JPanel initiativeSection() {
        JPanel result = new OptionPanel(PSD + "header.initMod");
        Content panContent = new Content(new GridLayout(1, 2, 10, 5));
        result.add(panContent);
        panContent.add(labInit);
        panContent.add(fldInit);
        labInit.setToolTipText(formatTooltip(Messages.getString(PSD + "initModTT")));
        fldInit.setToolTipText(formatTooltip(Messages.getString(PSD + "initModTT")));
        return result;
    }
    
    private JPanel mineSection() {
        JPanel result = new OptionPanel(PSD + "header.minefields");
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
        JPanel result = new OptionPanel(PSD + "header.skills");
        Content panContent = new Content(new GridLayout(4, 2, 10, 5));
        result.add(panContent);
        panContent.add(labMethod);
        panContent.add(cmbMethod);
        panContent.add(labPilot);
        panContent.add(cmbPilot);
        panContent.add(labXP);
        panContent.add(cmbXP);
        panContent.add(new JLabel());
        panContent.add(butForceGP);
        return result;
    }
    
    private JPanel buttonPanel() {
        JPanel result = new JPanel(new FlowLayout());
        butOkay.addActionListener(listener);
        result.add(butOkay);
        result.add(new DialogButton(new CancelAction(this)));
        return result;
    }

    private void setupValues() {
        IPlayer player = client.getLocalPlayer();
        fldInit.setText(Integer.toString(player.getConstantInitBonus()));
        fldConventional.setText(Integer.toString(player.getNbrMFConventional()));
        fldVibrabomb.setText(Integer.toString(player.getNbrMFVibra()));
        fldActive.setText(Integer.toString(player.getNbrMFActive()));
        fldInferno.setText(Integer.toString(player.getNbrMFInferno()));
        for (int i = 0; i < RandomSkillsGenerator.M_SIZE; i++) {
            cmbMethod.addItem(RandomSkillsGenerator.getMethodDisplayableName(i));
        }
        for (int i = 0; i < RandomSkillsGenerator.T_SIZE; i++) {
            cmbPilot.addItem(RandomSkillsGenerator.getTypeDisplayableName(i));
        }
        for (int i = 0; i < RandomSkillsGenerator.L_SIZE; i++) {
            cmbXP.addItem(RandomSkillsGenerator.getLevelDisplayableName(i));
        }
        cmbMethod.setSelectedIndex(client.getRandomSkillsGenerator().getMethod());
        adjustSkillMethodTip();
        cmbPilot.setSelectedIndex(client.getRandomSkillsGenerator().getType());
        cmbXP.setSelectedIndex(client.getRandomSkillsGenerator().getLevel());
        butForceGP.setSelected(client.getRandomSkillsGenerator().isClose());
        cmbMethod.addItemListener(e -> adjustSkillMethodTip());
    }
    
    private void setupStartGrid() {
        panStartButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < 11; i++) {
            butStartPos[i] = new TipButton("", this);
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
                butTT[i].append(Messages.getString(PSD + "invalidStartPosTT"));
            } else {
                butText[i].append(guiScaledFontHTML());
            }
            butText[i].append(IStartingPositions.START_LOCATION_NAMES[i] + "</FONT><BR>");
        }
        
        for (IPlayer player: client.getGame().getPlayersVector()) {
            int pos = player.getStartingPos(); 
            if (!player.equals(client.getLocalPlayer()) && (pos >= 0) && (pos <= 19)) { 
                int index = pos > 10 ? pos - 10 : pos;
                butText[index].append(guiScaledFontHTML(teamColor(player, client.getLocalPlayer())));
                butText[index].append("\u25A0</FONT>");
                if (!hasPlayer[index]) {
                    if (butTT[index].length() > 0) {
                        butTT[index].append("<BR><BR>");
                    }
                    butTT[index].append(Messages.getString(PSD + "deployingHere"));
                    hasPlayer[index] = true;
                }
                butTT[index].append("<BR>" + player.getName());
            }
        }
        
        butText[currentPlayerStartPos].append(guiScaledFontHTML(GUIPreferences.getInstance().getMyUnitColor()));
        butText[currentPlayerStartPos].append("\u2B24</FONT>");

        for (int i = 0; i < 11; i++) {
            butStartPos[i].setText(butText[i].toString());
            if (butTT[i].length() > 0) {
                butStartPos[i].setToolTipText(formatTooltip(butTT[i].toString()));
            }
        }
    }

    ActionListener listener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            // OKAY
            if (e.getSource().equals(butOkay)) {
                userResponse = true;
                setVisible(false);
            } 

            // Deployment buttons
            for (int i = 0; i < 11; i++) {
                if (butStartPos[i].equals(e.getSource())) {
                    currentPlayerStartPos = i;
                    updateStartGrid();
                }
            }

            // Bot settings button
            if (butBotSettings.equals(e.getSource())) {
                BotConfigDialog bcd = new BotConfigDialog(clientgui.frame, (BotClient) client, false);
                bcd.setVisible(true);
                if (!bcd.dialogAborted && client instanceof Princess) {
                    ((Princess) client).setBehaviorSettings(bcd.getBehaviorSettings());
                }
            }
        }
    };
    
    private void adjustSkillMethodTip() {
        if (cmbMethod.getSelectedIndex() == RandomSkillsGenerator.M_TW) {
            cmbMethod.setToolTipText(formatTooltip(getString("RandomSkillDialog.descTW")));
        } else if (cmbMethod.getSelectedIndex() == RandomSkillsGenerator.M_TAHARQA) {
            cmbMethod.setToolTipText(formatTooltip(getString("RandomSkillDialog.descTaharqa")));
        } else if (cmbMethod.getSelectedIndex() == RandomSkillsGenerator.M_CONSTANT) {
            cmbMethod.setToolTipText(formatTooltip(getString("RandomSkillDialog.descConstant")));
        }
    }
    
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

    /** 
     * Completes the tooltip for this dialog, setting its width and adding
     * HTML tags.
     */
    private String formatTooltip(String text) {
        String result = "<P WIDTH=" + scaleForGUI(TOOLTIP_WIDTH) + " style=padding:5>" + text;
        return scaleStringForGUI(result);
    }

    /** 
     * A specialized JScrollPane that reports 80% of the parent frame's
     * height as its maximum preferred viewport height. This makes the dialog
     * scale to about 80% of MM's window height when needed but not more. 
     */
    private class ContentScrollPane extends JScrollPane {
        private static final long serialVersionUID = -4976675600736422725L;
        
        public ContentScrollPane(Component view) {
            super(view);
            getVerticalScrollBar().setUnitIncrement(16);
            setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            setBorder(null);
        }

        @Override
        public Dimension getPreferredSize() {
            var prefSize = super.getPreferredSize();
            var maxHeight = clientgui.getFrame().getHeight() / 10 * 8;
            return new Dimension(prefSize.width, Math.min(maxHeight, prefSize.height));
        }
    }; 

}