package megamek.client.ui.swing.lobby;

import megamek.common.IGame;
import megamek.common.IPlayer;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;

public class LobbyUtility {

    /**
     * Returns true when the starting position of the given player is valid
     * in the given game. This is not the case only when the options "Double Blind"
     * and "Exclusive Starting Positions" are on and the starting position overlaps
     * with that of other players.
     * <P>See also {@link #startPosOverlap(IPlayer, IPlayer)}
     */
    public static boolean isValidStartPos(IGame game, IPlayer player) {
        return !(isExclusiveDeployment(game) 
                && game.getPlayersVector().stream().filter(p -> !p.equals(player))
                        .anyMatch(p -> startPosOverlap(player, p)));
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
    
    private static boolean startPosOverlap(IPlayer player1, IPlayer player2) {
        return startPosOverlap(player1.getStartingPos(), player2.getStartingPos());
    }
    
    /** 
     * Returns true when the two starting positions overlap, i.e.
     * if they are equal or adjacent (e.g. E and NE, SW and S).
     * ANY overlaps all others. 
     */
    public static boolean startPosOverlap(int pos1, int pos2) {
        if (pos1 > 10) {
            pos1 -= 10;
        }
        if (pos2 > 10) {
            pos2 -= 10;
        }
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
        // NW = 1 and W = 8 also overlap
        return ((a - b == 1) || (a == 8 && b == 1));
    }
}
