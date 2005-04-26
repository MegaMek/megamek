package megamek.client.bot;

import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;

import megamek.client.CommonHelpDialog;
import megamek.client.ConfirmDialog;
import megamek.client.GUIPreferences;
import megamek.common.Game;
import megamek.common.event.GameAttackEvent;
import megamek.common.event.GameBoardChangeEvent;
import megamek.common.event.GameBoardNewEvent;
import megamek.common.event.GameEndEvent;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.event.GameEntityNewEvent;
import megamek.common.event.GameEntityNewOffboardEvent;
import megamek.common.event.GameEntityRemoveEvent;
import megamek.common.event.GameListener;
import megamek.common.event.GameMapQueryEvent;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GamePlayerChangeEvent;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.event.GamePlayerConnectedEvent;
import megamek.common.event.GamePlayerDisconnectedEvent;
import megamek.common.event.GameReportEvent;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.event.GameTurnChangeEvent;

public class BotGUI implements GameListener {

    private BotClient bot;
    private Frame frame = new Frame();
    private static boolean WarningShown;

    public BotGUI(BotClient bot) {
        this.bot = bot;
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gamePhaseChange(megamek.common.GamePhaseChangeEvent)
     */
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        if (bot.game.getPhase() == Game.PHASE_LOUNGE || bot.game.getPhase() == Game.PHASE_STARTING_SCENARIO) {
            notifyOfBot();
        }
    }

    public void notifyOfBot() {
        if (GUIPreferences.getInstance().getNagForBotReadme() && !WarningShown) {
            WarningShown = true;
            String title = Messages.getString("BotGUI.notifyOfBot.title"); //$NON-NLS-1$
            String body = Messages.getString("BotGUI.notifyOfBot.message"); //$NON-NLS-1$
            Dimension screenSize = frame.getToolkit().getScreenSize();
            frame.pack();
            frame.setLocation(
                screenSize.width / 2 - frame.getSize().width / 2,
                screenSize.height / 2 - frame.getSize().height / 2);
            ConfirmDialog confirm = new ConfirmDialog(frame, title, body, true);
            confirm.show();

            if (!confirm.getShowAgain()) {
                GUIPreferences.getInstance().setNagForBotReadme(false);
            }

            if (confirm.getAnswer()) {
                File helpfile = new File("ai-readme.txt"); //$NON-NLS-1$
                new CommonHelpDialog(frame, helpfile).show();
            }
        }
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gamePlayerConnected(megamek.common.GamePlayerConnectedEvent)
     */
    public void gamePlayerConnected(GamePlayerConnectedEvent e) {
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gamePlayerDisconnected(megamek.common.GamePlayerDisconnectedEvent)
     */
    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gamePlayerChange(megamek.common.GamePlayerChangeEvent)
     */
    public void gamePlayerChange(GamePlayerChangeEvent e) {
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gamePlayerChat(megamek.common.GamePlayerChatEvent)
     */
    public void gamePlayerChat(GamePlayerChatEvent e) {
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameTurnChange(megamek.common.GameTurnChangeEvent)
     */
    public void gameTurnChange(GameTurnChangeEvent e) {
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameReport(megamek.common.GameReportEvent)
     */
    public void gameReport(GameReportEvent e) {
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameEnd(megamek.common.GameEndEvent)
     */
    public void gameEnd(GameEndEvent e) {
    }

    
    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameBoardNew(megamek.common.GameBoardNewEvent)
     */
    public void gameBoardNew(GameBoardNewEvent e) {
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameBoardChanged(megamek.common.GameBoardChangeEvent)
     */
    public void gameBoardChanged(GameBoardChangeEvent e) {
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameSettingsChange(megamek.common.GameSettingsChangeEvent)
     */
    public void gameSettingsChange(GameSettingsChangeEvent e) {
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameMapQuery(megamek.common.GameMapQueryEvent)
     */
    public void gameMapQuery(GameMapQueryEvent e) {
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameEntityNew(megamek.common.GameEntityNewEvent)
     */
    public void gameEntityNew(GameEntityNewEvent e) {
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameEntityNewOffboard(megamek.common.GameEntityNewOffboardEvent)
     */
    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameEntityChange(megamek.common.GameEntityChangeEvent)
     */
    public void gameEntityChange(GameEntityChangeEvent e) {
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameAttack(megamek.common.GameAttackEvent)
     */
    public void gameAttack(GameAttackEvent e) {
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameEntityRemove(megamek.common.GameEntityRemoveEvent)
     */
    public void gameEntityRemove(GameEntityRemoveEvent e) {
    }

}
