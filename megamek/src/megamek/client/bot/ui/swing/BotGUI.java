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

import javax.swing.JFrame;

import megamek.client.bot.BotClient;
import megamek.client.bot.Messages;
import megamek.client.ui.dialogs.helpDialogs.BotHelpDialog;
import megamek.client.ui.swing.ConfirmDialog;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Game;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameBoardChangeEvent;
import megamek.common.event.GameBoardNewEvent;
import megamek.common.event.GameCFREvent;
import megamek.common.event.GameEndEvent;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.event.GameEntityNewEvent;
import megamek.common.event.GameEntityNewOffboardEvent;
import megamek.common.event.GameEntityRemoveEvent;
import megamek.common.event.GameListener;
import megamek.common.event.GameMapQueryEvent;
import megamek.common.event.GameNewActionEvent;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GamePlayerChangeEvent;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.event.GamePlayerConnectedEvent;
import megamek.common.event.GamePlayerDisconnectedEvent;
import megamek.common.event.GameReportEvent;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.event.GameVictoryEvent;

public class BotGUI implements GameListener {

    private BotClient bot;
    private static boolean WarningShown;

    public BotGUI(BotClient bot) {
        this.bot = bot;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.GameListener#gamePhaseChange(megamek.common.GamePhaseChangeEvent)
     */
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        if (bot.getGame().getPhase() == GamePhase.LOUNGE
                || bot.getGame().getPhase() == GamePhase.STARTING_SCENARIO) {
            notifyOfBot();
        }
    }

    public void notifyOfBot() {
        if (GUIPreferences.getInstance().getNagForBotReadme() && !WarningShown) {
            WarningShown = true;
            
            JFrame frame = new JFrame();
            String title = Messages.getString("BotGUI.notifyOfBot.title"); //$NON-NLS-1$
            String body = Messages.getString("BotGUI.notifyOfBot.message"); //$NON-NLS-1$
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

    public void gameReport(GameReportEvent e) {
    }

    public void gameEnd(GameEndEvent e) {
    }

    public void gameBoardNew(GameBoardNewEvent e) {
    }

    public void gameBoardChanged(GameBoardChangeEvent e) {
    }

    public void gameSettingsChange(GameSettingsChangeEvent e) {
    }

    public void gameMapQuery(GameMapQueryEvent e) {
    }

    public void gameEntityNew(GameEntityNewEvent e) {
    }

    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {
    }

    public void gameEntityChange(GameEntityChangeEvent e) {
    }

    public void gameNewAction(GameNewActionEvent e) {
    }

    public void gameEntityRemove(GameEntityRemoveEvent e) {
    }
    
    @Override
    public void gameClientFeedbackRequest(GameCFREvent evt) {
    }

    @Override
    public void gameVictory(GameVictoryEvent e) {       
    }

}
