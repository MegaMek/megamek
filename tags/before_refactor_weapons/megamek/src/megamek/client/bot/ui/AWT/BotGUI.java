package megamek.client.bot;

import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;

import megamek.client.CommonHelpDialog;
import megamek.client.ConfirmDialog;
import megamek.client.GameEvent;
import megamek.client.GameListener;
import megamek.common.Game;
import megamek.common.Settings;

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
        if (Settings.nagForBotReadme && !WarningShown) {
            WarningShown = true;
            String title = "Please read the ai-readme.txt";
            String body =
                "The bot does not work with all units or game options.\n"
                    + "Please read the ai-readme.txt file before using the bot.\n"
                    + " \nWould you like to read the AI documentation now?\n";
            Dimension screenSize = frame.getToolkit().getScreenSize();
            frame.pack();
            frame.setLocation(
                screenSize.width / 2 - frame.getSize().width / 2,
                screenSize.height / 2 - frame.getSize().height / 2);
			ConfirmDialog confirm = new ConfirmDialog(frame, title, body, true);
            confirm.show();

            if (!confirm.getShowAgain()) {
                Settings.nagForBotReadme = false;
                Settings.save();
            }

            if (confirm.getAnswer()) {
                File helpfile = new File("ai-readme.txt");
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
