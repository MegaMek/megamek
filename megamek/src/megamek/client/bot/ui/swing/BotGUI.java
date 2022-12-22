/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot.ui.swing;

import megamek.client.bot.BotClient;
import megamek.client.bot.Messages;
import megamek.client.ui.dialogs.helpDialogs.BotHelpDialog;
import megamek.client.ui.swing.ConfirmDialog;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.event.*;

import javax.swing.*;

public class BotGUI implements GameListener {
    private BotClient bot;
    private static boolean WarningShown;

    public BotGUI(BotClient bot) {
        this.bot = bot;
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        if (bot.getGame().getPhase().isLounge() || bot.getGame().getPhase().isStartingScenario()) {
            notifyOfBot();
        }
    }

    public void notifyOfBot() {
        if (GUIPreferences.getInstance().getNagForBotReadme() && !WarningShown) {
            WarningShown = true;
            
            JFrame frame = new JFrame();
            String title = Messages.getString("BotGUI.notifyOfBot.title");
            String body = Messages.getString("BotGUI.notifyOfBot.message");
            frame.pack();
            frame.setLocationRelativeTo(null);
            ConfirmDialog confirm = new ConfirmDialog(frame, title, body, true);
            confirm.setVisible(true);

            if (!confirm.getShowAgain()) {
                GUIPreferences.getInstance().setNagForBotReadme(false);
            }

            if (confirm.getAnswer()) {
                new BotHelpDialog(frame).setVisible(true);
            }
            frame.dispose();
        }
    }

    @Override
    public void gamePlayerConnected(GamePlayerConnectedEvent e) {
    }

    @Override
    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
    }

    @Override
    public void gamePlayerChange(GamePlayerChangeEvent e) {
    }

    @Override
    public void gamePlayerChat(GamePlayerChatEvent e) {
    }

    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
    }

    @Override
    public void gameReport(GameReportEvent e) {
    }

    @Override
    public void gameEnd(GameEndEvent e) {
    }

    @Override
    public void gameBoardNew(GameBoardNewEvent e) {
    }

    @Override
    public void gameBoardChanged(GameBoardChangeEvent e) {
    }

    @Override
    public void gameSettingsChange(GameSettingsChangeEvent e) {
    }

    @Override
    public void gameMapQuery(GameMapQueryEvent e) {
    }

    @Override
    public void gameEntityNew(GameEntityNewEvent e) {
    }

    @Override
    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {
    }

    @Override
    public void gameEntityChange(GameEntityChangeEvent e) {
    }

    @Override
    public void gameNewAction(GameNewActionEvent e) {
    }

    @Override
    public void gameEntityRemove(GameEntityRemoveEvent e) {
    }
    
    @Override
    public void gameClientFeedbackRequest(GameCFREvent evt) {
    }

    @Override
    public void gameVictory(GameVictoryEvent e) {       
    }

}
