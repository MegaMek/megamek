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
package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.Princess;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.dialog.DialogButton;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.EntitySelector;
import megamek.common.IPlayer;
import megamek.common.IStartingPositions;
import megamek.common.OffBoardDirection;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;

/**
 * A dialog that can be used to adjust advanced player settings like initiative,
 * minefields, and maybe other things in the future like force abilities.
 * 
 * @author Jay Lawson
 */
public class PlayerSettingsDialog extends ClientDialog implements ActionListener {

    private static final long serialVersionUID = -4597870528499580517L;

    private final Client client;
    private final ClientGUI clientgui;

    private JLabel labInit = new JLabel(
            Messages.getString("PlayerSettingsDialog.ConstantBonus"),
            SwingConstants.RIGHT);
    private JLabel labMines = new JLabel(
            Messages.getString("PlayerSettingsDialog.Minefields"),
            SwingConstants.CENTER);
    private JLabel labConventional = new JLabel(
            Messages.getString("PlayerSettingsDialog.labConventional"), 
            SwingConstants.RIGHT); 
    private JLabel labVibrabomb = new JLabel(
            Messages.getString("PlayerSettingsDialog.labVibrabomb"),
            SwingConstants.RIGHT); 
    private JLabel labActive = new JLabel(
            Messages.getString("PlayerSettingsDialog.labActive"),
            SwingConstants.RIGHT); 
    private JLabel labInferno = new JLabel(
            Messages.getString("PlayerSettingsDialog.labInferno"),
            SwingConstants.RIGHT); 

    private JTextField texInit = new JTextField(3);
    private JTextField fldConventional = new JTextField(3);
    private JTextField fldVibrabomb = new JTextField(3);
    private JTextField fldActive = new JTextField(3);
    private JTextField fldInferno = new JTextField(3);
    
    private JPanel panStartButtons = new JPanel();
    private JButton[] butStartPos = new JButton[11];
    private JButton butBotSettings = new JButton("Bot Settings...");
    
    private int currentPlayerStartPos;

    public PlayerSettingsDialog(ClientGUI clientgui, Client client) {
        super(clientgui.frame, Messages.getString("PlayerSettingsDialog.title"), true, true);
        this.client = client;
        this.clientgui = clientgui;
        currentPlayerStartPos = client.getLocalPlayer().getStartingPos();
        
        fillInValues();
        
        JPanel mainPanel = new JPanel();
        add(mainPanel, BorderLayout.CENTER);
        
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        if (client instanceof BotClient) {
            mainPanel.add(botPanel());
        }
        mainPanel.add(startPanel());
        mainPanel.add(initiativePanel());
        if (client.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_MINEFIELDS)) {
            mainPanel.add(minePanel());
        }
        
        // Buttons
        JPanel panButtons = new JPanel(new FlowLayout());
        DialogButton butOkay = new DialogButton(new OkayAction(this));
        panButtons.add(butOkay);
        panButtons.add(new DialogButton(new CancelAction(this)));
        add(panButtons, BorderLayout.PAGE_END);
        
        getRootPane().setDefaultButton(butOkay);
        pack();
        center();
    }
    
    private JPanel botPanel() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
        result.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        result.add(new HeaderLabel("Bot Player"));
        JPanel panContent = new JPanel(new FlowLayout());
        panContent.setAlignmentX(Component.LEFT_ALIGNMENT);
        result.add(panContent);
        panContent.add(butBotSettings);
        butBotSettings.addActionListener(this);
        return result;
    }
    
    private JPanel startPanel() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
        result.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        result.add(new HeaderLabel("Deployment Area"));
        setupStartGrid();
        result.add(panStartButtons);
        return result;
    }
    
    private JPanel initiativePanel() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
        result.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JLabel header2 = new JLabel("<HTML>Use this to give a player lore-based <BR>or other modifiers, e.g. a company <BR>initiative bonus.");
        header2.setBorder(BorderFactory.createEmptyBorder(5, 30, 5, 10));
        header2.setAlignmentX(Component.LEFT_ALIGNMENT);
        result.add(new HeaderLabel("Initiative Modifier"));
        result.add(header2);
        
        JPanel panContent = new JPanel(new GridLayout(1, 2));
        panContent.setAlignmentX(Component.LEFT_ALIGNMENT);
        result.add(panContent);
        
        panContent.add(labInit);
        panContent.add(texInit);
        
        return result;
    }
    
    private JPanel minePanel() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        result.add(new HeaderLabel("Minefields"));
        
        JPanel panContent = new JPanel(new GridLayout(4, 2, 10, 5));
        panContent.setAlignmentX(Component.LEFT_ALIGNMENT);
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

    private void fillInValues() {
        IPlayer player = client.getLocalPlayer();
        texInit.setText(Integer.toString(player.getConstantInitBonus()));
        fldConventional.setText(Integer.toString(player.getNbrMFConventional()));
        fldVibrabomb.setText(Integer.toString(player.getNbrMFVibra()));
        fldActive.setText(Integer.toString(player.getNbrMFActive()));
        fldInferno.setText(Integer.toString(player.getNbrMFInferno()));
    }
    
    private void setupStartGrid() {
        panStartButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < 11; i++) {
            butStartPos[i] = new JButton();
            butStartPos[i].addActionListener(this);
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
    
    private void updateStartGrid() {
        Vector<IPlayer> players = client.getGame().getPlayersVector();

        for (int i = 0; i < 11; i++) {
            String butText = "<HTML><P ALIGN=CENTER>";
            if (isExclusiveDeployment()) {
                final int pos = i;
                if (players.stream().filter(p -> !p.equals(client.getLocalPlayer()))
                        .anyMatch(p -> startPosOverlap(pos, p.getStartingPos()))) {
                    butText += UIUtil.guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor()); 
//                    butText +=MekTableCellFormatter.WARNING_SIGN + "</FONT>";
                }
            }
            butText += IStartingPositions.START_LOCATION_NAMES[i] + "<BR>";
            butStartPos[i].setText(butText);
        }

        for (IPlayer player: players) {
            int pos = player.getStartingPos(); 
            if (!player.equals(client.getLocalPlayer()) && (pos >= 0) && (pos <= 10)) { 
                String butText = "";
                if (player.isEnemyOf(client.getLocalPlayer())) {
                    butText += UIUtil.guiScaledFontHTML(GUIPreferences.getInstance().getEnemyUnitColor());
                    butText += "\u2BC1<FONT>";
                } else {
                    butText += UIUtil.guiScaledFontHTML(GUIPreferences.getInstance().getAllyUnitColor());
                    butText += "\u2BC1<FONT>";
//                    butText += "\u25A0<FONT>";
                }
                JButton button = butStartPos[pos];
                button.setText(button.getText() + butText);
            }
        }
        JButton button = butStartPos[currentPlayerStartPos];
        String butText = UIUtil.guiScaledFontHTML(GUIPreferences.getInstance().getMyUnitColor());
        butText += "\u2B24<FONT>";
        button.setText(button.getText() + butText);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(OkayAction.OKAY)) {
            String init = texInit.getText();
            int initB = 0;
            try {
                if ((init != null) && (init.length() != 0)) {
                    initB = Integer.parseInt(init);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(clientgui.frame, 
                        Messages.getString("PlayerSettingsDialog.ConstantInitAlert.message"), //$NON-NLS-1$
                        Messages.getString("PlayerSettingsDialog.ConstantInitAlert.title"), //$NON-NLS-1$
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            client.getLocalPlayer().setConstantInitBonus(initB);

            String conv = fldConventional.getText();
            String vibra = fldVibrabomb.getText();
            String active = fldActive.getText();
            String inferno = fldInferno.getText();

            int nbrConv = 0;
            int nbrVibra = 0;
            int nbrActive = 0;
            int nbrInferno = 0;

            try {
                if ((conv != null) && (conv.length() != 0)) {
                    nbrConv = Integer.parseInt(conv);
                }
                if ((vibra != null) && (vibra.length() != 0)) {
                    nbrVibra = Integer.parseInt(vibra);
                }
                if ((active != null) && (active.length() != 0)) {
                    nbrActive = Integer.parseInt(active);
                }
                if ((inferno != null) && (inferno.length() != 0)) {
                    nbrInferno = Integer.parseInt(inferno);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(clientgui.frame, 
                        Messages.getString("PlayerSettingsDialog.MinefieldAlert.message"),
                        Messages.getString("PlayerSettingsDialog.MinefieldAlert.title"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if ((nbrConv < 0) || (nbrVibra < 0) || (nbrActive < 0) || (nbrInferno < 0)) {
                JOptionPane.showMessageDialog(clientgui.frame, 
                        Messages.getString("PlayerSettingsDialog.MinefieldAlert.message"),
                        Messages.getString("PlayerSettingsDialog.MinefieldAlert.title"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            client.getLocalPlayer().setNbrMFConventional(nbrConv);
            client.getLocalPlayer().setNbrMFVibra(nbrVibra);
            client.getLocalPlayer().setNbrMFActive(nbrActive);
            client.getLocalPlayer().setNbrMFInferno(nbrInferno);
            setStartPos();

            client.sendPlayerInfo();
            setVisible(false);
        } 
        for (int i = 0; i < 11; i++) {
            if (butStartPos[i].equals(e.getSource())) {
                currentPlayerStartPos = i;
                updateStartGrid();
            }
        }
        if (butBotSettings.equals(e.getSource())) {
            BotConfigDialog bcd = new BotConfigDialog(clientgui.frame, (BotClient) client);
            bcd.setVisible(true);
            if (!bcd.dialogAborted && client instanceof Princess) {
                ((Princess) client).setBehaviorSettings(bcd.getBehaviorSettings());
            }
        }
    }

    private static class HeaderLabel extends JLabel {
        private static final long serialVersionUID = -6235772150005269143L;
        HeaderLabel(String text) {
            super(" \u2B9E " + text);
            setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }
    }
    
    
    
    private void setStartPos() {
        final GameOptions gOpts = client.getGame().getOptions();
        if (gOpts.booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
                && gOpts.booleanOption(OptionsConstants.BASE_EXCLUSIVE_DB_DEPLOYMENT)) {
            if (currentPlayerStartPos == 0) {
                clientgui.doAlertDialog(
                        Messages.getString("ChatLounge.ExclusiveDeploy.title"),
                        Messages.getString("ChatLounge.ExclusiveDeploy.msg"));
                return;
            }
            for (Enumeration<IPlayer> e = client.getGame().getPlayers(); e
                    .hasMoreElements();) {
                IPlayer player = e.nextElement();
                if (player.getStartingPos() == 0) {
                    continue;
                }
                // CTR and EDG don't overlap
                if (((player.getStartingPos() == 9) && (currentPlayerStartPos == 10))
                        || ((player.getStartingPos() == 10) && (currentPlayerStartPos == 9))) {
                    continue;
                }

                // check for overlapping starting directions
                if (((player.getStartingPos() == currentPlayerStartPos)
                        || ((player.getStartingPos() + 1) == currentPlayerStartPos) || ((player
                                .getStartingPos() - 1) == currentPlayerStartPos))
                        && (player.getId() != client.getLocalPlayer()
                        .getId())) {
                    clientgui.doAlertDialog(
                            Messages.getString("ChatLounge.OverlapDeploy.title"),
                            Messages.getString("ChatLounge.OverlapDeploy.msg"));
                    return;
                }
            }
        }
        if (gOpts.booleanOption(OptionsConstants.BASE_DEEP_DEPLOYMENT)
                && (currentPlayerStartPos > 0) && (currentPlayerStartPos <= 9)) {
            currentPlayerStartPos += 10;
        }
        client.getLocalPlayer().setStartingPos(currentPlayerStartPos);
        client.sendPlayerInfo();
        // If the gameoption set_arty_player_homeedge is set,
        // set all the player's offboard arty units to be behind the
        // newly
        // selected home edge.
        if (gOpts.booleanOption(OptionsConstants.BASE_SET_ARTY_PLAYER_HOMEEDGE)) { //$NON-NLS-1$
            OffBoardDirection direction = OffBoardDirection.NONE;
            switch (currentPlayerStartPos) {
            case 0:
                break;
            case 1:
            case 2:
            case 3:
                direction = OffBoardDirection.NORTH;
                break;
            case 4:
                direction = OffBoardDirection.EAST;
                break;
            case 5:
            case 6:
            case 7:
                direction = OffBoardDirection.SOUTH;
                break;
            case 8:
                direction = OffBoardDirection.WEST;
                break;
            case 11:
            case 12:
            case 13:
                direction = OffBoardDirection.NORTH;
                break;
            case 14:
                direction = OffBoardDirection.EAST;
                break;
            case 15:
            case 16:
            case 17:
                direction = OffBoardDirection.SOUTH;
                break;
            case 18:
                direction = OffBoardDirection.WEST;
                break;
            default:
            }
            Iterator<Entity> thisPlayerArtyUnits = client.getGame()
                    .getSelectedEntities(new EntitySelector() {
                        public boolean accept(Entity entity) {
                            if (entity.getOwnerId() == client
                                    .getLocalPlayer().getId()) {
                                return true;
                            }
                            return false;
                        }
                    });
            while (thisPlayerArtyUnits.hasNext()) {
                Entity entity = thisPlayerArtyUnits.next();
                if (entity.getOffBoardDirection() != OffBoardDirection.NONE) {
                    if (direction != OffBoardDirection.NONE) {
                        entity.setOffBoard(entity.getOffBoardDistance(),
                                direction);
                    }
                }
            }
        }
    }

    /** 
     * Returns true when double blind and exclusive deployment is on,
     * meaning that player's deployment zones may not overlap.
     */
    private boolean isExclusiveDeployment() {
        final GameOptions gOpts = client.getGame().getOptions();
        return gOpts.booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
                && gOpts.booleanOption(OptionsConstants.BASE_EXCLUSIVE_DB_DEPLOYMENT);
    }
    
    /** 
     * Returns true when the two starting positions overlap, i.e.
     * if they are equal or adjacent (e.g. E and NE, SW and S).
     * ANY overlaps all others. 
     */
    private boolean startPosOverlap(int pos1, int pos2) {
        if (pos1 == pos2) {
            return true;
        }
        int a = Math.max(pos1, pos2);
        int b = Math.min(pos1, pos2);
        // Out of bounds values:
        if (b < 0 || a > 10) {
            return false;
        }
        // ANY overlaps all others, EDG overlaps all others but CTR
        if (b == 0 || a == 9) {
            return true;
        }
        // EDG and CTR don't overlap
        if (a == 10 && b == 9) {
            return false;
        }
        // the rest of the positions overlap if they're 1 apart
        return (a - b == 1);
    }

}