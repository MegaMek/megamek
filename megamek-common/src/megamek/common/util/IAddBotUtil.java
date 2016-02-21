package megamek.common.util;

import megamek.common.IGame;

public interface IAddBotUtil {

	String addBot(String[] args, IGame game, String host, int port);

	String getCommand();

	String getUsage();
	

}
