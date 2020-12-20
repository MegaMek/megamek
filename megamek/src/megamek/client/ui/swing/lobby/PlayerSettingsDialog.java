/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
 * MegaMek - Copyright (C) 2020 - The MegaMek Team 
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.ui.swing.lobby;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.SwingConstants;
import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.Princess;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.*;
import megamek.client.ui.swing.dialog.DialogButton;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.options.OptionsConstants;
import static megamek.client.ui.swing.lobby.LobbyUtility.*;
import static megamek.client.ui.swing.util.UIUtil.*;

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
    
    // PRIVATE

    private final Client client;
    private final ClientGUI clientgui;
    
    private static final int TOOLTIP_WIDTH = 300;
    private static final String PSD = "PlayerSettingsDialog.";
    private JLabel labInit = new TipLabel(Messages.getString(PSD + "initMod"), SwingConstants.RIGHT);
    private JLabel labConventional = new JLabel(Messages.getString(PSD + "labConventional"), SwingConstants.RIGHT); 
    private JLabel labVibrabomb = new JLabel(Messages.getString(PSD + "labVibrabomb"), SwingConstants.RIGHT); 
    private JLabel labActive = new JLabel(Messages.getString(PSD + "labActive"), SwingConstants.RIGHT); 
    private JLabel labInferno = new JLabel(Messages.getString(PSD + "labInferno"), SwingConstants.RIGHT); 

    private JTextField fldInit = new TipTextField(3);
    private JTextField fldConventional = new JTextField(3);
    private JTextField fldVibrabomb = new JTextField(3);
    private JTextField fldActive = new JTextField(3);
    private JTextField fldInferno = new JTextField(3);
    
    private JPanel panStartButtons = new JPanel();
    private JButton[] butStartPos = new JButton[11];
    private JButton butBotSettings = new JButton(Messages.getString(PSD + "botSettings"));
    private DialogButton butOkay = new DialogButton(Messages.getString("Okay"));
    
    private int currentPlayerStartPos;
    
    private boolean userResponse;
    
    private void setupDialog() {
        setupValues();
        
        JPanel mainPanel = new JPanel();
        add(mainPanel, BorderLayout.CENTER);
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
    }
    
    private JPanel headerSection() {
        JPanel result = new JPanel();
        result.setAlignmentX(Component.LEFT_ALIGNMENT);
        Content panContent = new Content(new FlowLayout());
        result.add(panContent);
        Icon playerIcon = client.getLocalPlayer().getCamouflage().getImageIcon(UIUtil.scaleForGUI(40));
        JLabel playerLabel = new JLabel(client.getLocalPlayer().getName(), playerIcon, SwingConstants.CENTER);
        playerLabel.setIconTextGap(UIUtil.scaleForGUI(12));
        panContent.add(playerLabel);
        return result;
    }

    private JPanel botSection() {
        JPanel result = new OptionPanel("botPlayer");
        Content panContent = new Content(new FlowLayout());
        result.add(panContent);
        panContent.add(butBotSettings);
        butBotSettings.addActionListener(listener);
        return result;
    }
    
    private JPanel startSection() {
        JPanel result = new OptionPanel("startPos");
        Content panContent = new Content(new GridLayout(1, 1));
        result.add(panContent);
        setupStartGrid();
        panContent.add(panStartButtons);
        return result;
    }
    
    private JPanel initiativeSection() {
        JPanel result = new OptionPanel("initMod");
        Content panContent = new Content(new GridLayout(1, 2, 10, 5));
        result.add(panContent);
        panContent.add(labInit);
        panContent.add(fldInit);
        labInit.setToolTipText(formatTooltip(Messages.getString(PSD + "initModTT")));
        fldInit.setToolTipText(formatTooltip(Messages.getString(PSD + "initModTT")));
        return result;
    }
    
    private JPanel mineSection() {
        JPanel result = new OptionPanel("minefields");
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
    }
    
    private void setupStartGrid() {
        panStartButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < 11; i++) {
            butStartPos[i] = new TipButton();
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
                if (pos > 10) {
                    pos -= 10;
                }
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
                BotConfigDialog bcd = new BotConfigDialog(clientgui.frame, (BotClient) client);
                bcd.setVisible(true);
                if (!bcd.dialogAborted && client instanceof Princess) {
                    ((Princess) client).setBehaviorSettings(bcd.getBehaviorSettings());
                }
            }
        }
    };

    /** A specialized panel for the header of a section. */
    private static class Header extends JPanel {
        private static final long serialVersionUID = -6235772150005269143L;
        Header(String text) {
            super();
            setLayout(new GridLayout(1, 1, 0, 0));
            add(new JLabel("\u29C9  " + Messages.getString(PSD + "header." + text)));
            setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBackground(alternateTableBGColor());
        }
    }
    
    /** A panel for the content of a subsection of the dialog. */
    private static class Content extends JPanel {
        private static final long serialVersionUID = -6605053283642217306L;

        Content(LayoutManager layout) {
            this();
            setLayout(layout);
        }
        
        Content() {
            super();
            setBorder(BorderFactory.createEmptyBorder(8, 8, 5, 8));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }
    }
    
    /** A panel for a subsection of the dialog, e.g. Minefields. */
    private static class OptionPanel extends JPanel {
        private static final long serialVersionUID = -7168700339882132428L;

        OptionPanel(String header) {
            super();
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            add(new Header(header));
        }
    }
    
    /** A JTextField with a specialized tooltip display. */
    private class TipTextField extends JTextField {
        private static final long serialVersionUID = -8918021639314853562L;

        public TipTextField(int cols) {
            super(cols);
        }

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            PlayerSettingsDialog psd = PlayerSettingsDialog.this;
            int x = -getLocation().x + psd.getWidth();
            int y = -getLocation().y;
            return new Point(x, y);
        }
        
        @Override
        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setBackground(alternateTableBGColor());
            return tip;
        }
    }
    
    /** A JLabel with a specialized tooltip display. */
    private class TipLabel extends JLabel {
        private static final long serialVersionUID = -338233022633675883L;

        public TipLabel(String text, int align) {
            super(text, align);
        }

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            PlayerSettingsDialog psd = PlayerSettingsDialog.this;
            int x = -getLocation().x + psd.getWidth();
            int y = -getLocation().y;
            return new Point(x, y);
        }
        
        @Override
        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setBackground(alternateTableBGColor());
            return tip;
        }
    }
    
    /** A JButton with a specialized tooltip display. */
    private class TipButton extends JButton {
        private static final long serialVersionUID = -2441468942513824896L;

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            PlayerSettingsDialog psd = PlayerSettingsDialog.this;
            int x = -getLocation().x + psd.getWidth();
            int y = -getLocation().y;
            return new Point(x, y);
        }
        
        @Override
        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setBackground(alternateTableBGColor());
            return tip;
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

    /** 
     * Completes the tooltip for this dialog, setting its width and adding
     * HTML tags.
     */
    private String formatTooltip(String text) {
        String result = "<P WIDTH=" + scaleForGUI(TOOLTIP_WIDTH) + ">" + text;
        return scaleStringForGUI(result);
    }
    
 }