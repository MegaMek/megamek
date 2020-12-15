package megamek.client.ui.swing.lobby;

import megamek.common.IGame;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;

public class LobbyUtility {

    public static boolean isValidStartPos(IGame game, int pos) {
return true;
    }

    /**
     * Returns true when double blind and exclusive deployment are on,
     * meaning that player's deployment zones may not overlap.
     */
    public static boolean isExclusiveDeployment(IGame game) {
        final GameOptions gOpts = game.getOptions();
        return gOpts.booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
                && gOpts.booleanOption(OptionsConstants.BASE_EXCLUSIVE_DB_DEPLOYMENT);
    }    
}
