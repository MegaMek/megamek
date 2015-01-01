package megamek.client.bot;

import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;

import megamek.client.CommonHelpDialog;
import megamek.client.ConfirmDialog;
import megamek.client.GUIPreferences;
import megamek.client.GameEvent;
import megamek.client.GameListener;
import megamek.common.Game;

public class BotGUI implements GameListener {

    private BotClient bot;
    private Frame frame = new Frame();
    private static boolean WarningShown;

    public BotGUI(BotClient bot) {
        this.bot = bot;
    }

    public void gameBoardChanged(GameEvent e) {
        ;
    }

    public void gameDisconnected(GameEvent e) {
        ;
    }

    public void gameEnd(GameEvent e) {
        ;
    }

    public void gameNewEntities(GameEvent e) {
        ;
    }

    public void gameNewSettings(GameEvent e) {
        ;
    }

    public void gamePhaseChange(GameEvent e) {
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

    public void gamePlayerChat(GameEvent e) {
        ;
    }

    public void gamePlayerStatusChange(GameEvent e) {
        ;
    }

    public void gameReport(GameEvent e) {
        ;
    }

    public void gameTurnChange(GameEvent e) {
        ;
    }

    public void gameMapQuery(GameEvent e) {
        ;
    }

}
