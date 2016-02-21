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

import java.io.File;

import javax.swing.JFrame;

import megamek.client.bot.BotClient;
import megamek.client.bot.Messages;
import megamek.client.ui.swing.CommonHelpDialog;
import megamek.client.ui.swing.ConfirmDialog;
import megamek.common.IGame;
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
import megamek.common.preference.GUIPreferences;

public class BotGUI implements GameListener {

    private BotClient bot;
    private JFrame frame = new JFrame();
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
        if (bot.getGame().getPhase() == IGame.Phase.PHASE_LOUNGE
                || bot.getGame().getPhase() == IGame.Phase.PHASE_STARTING_SCENARIO) {
            notifyOfBot();
        }
    }

    public void notifyOfBot() {
        if (GUIPreferences.getInstance().getNagForBotReadme() && !WarningShown) {
            WarningShown = true;
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
                File helpfile = new File("docs/ai-readme.txt"); //$NON-NLS-1$
                new CommonHelpDialog(frame, helpfile).setVisible(true);
            }
        }
    }

    public void gamePlayerConnected(GamePlayerConnectedEvent e) {
    }

    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
    }

    public void gamePlayerChange(GamePlayerChangeEvent e) {
    }

    public void gamePlayerChat(GamePlayerChatEvent e) {
    }

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
    public void gameClientFeedbackRquest(GameCFREvent evt) {
    }

    @Override
    public void gameVictory(GameVictoryEvent e) {       
    }

}
