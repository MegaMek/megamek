package megamek.client.bot;

import megamek.client.ClientGUI;
import megamek.client.ConfirmDialog;
import megamek.client.GameEvent;
import megamek.client.GameListener;
import megamek.common.Game;
import megamek.common.Settings;

public class BotGUI implements GameListener {

    private ClientGUI clientgui;
    private BotClient bot;
    private static boolean WarningShown;
    
    public BotGUI(ClientGUI clientgui, BotClient bot) {
        this.clientgui = clientgui;
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
		if (clientgui.getClient().game.getPhase() == Game.PHASE_LOUNGE) {
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
			ConfirmDialog confirm = new ConfirmDialog(clientgui.frame, title, body, true);
			confirm.show();

			if (!confirm.getShowAgain()) {
				Settings.nagForBotReadme = false;
				Settings.save();
			}

			if (confirm.getAnswer()) {
				clientgui.showHelp("ai-readme.txt");
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

}
