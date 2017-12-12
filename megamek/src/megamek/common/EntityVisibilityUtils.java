package megamek.common;

import megamek.common.options.OptionsConstants;

/**
 * Class containing static functions that perform visibility computations related to an entity
 * without the need to be a part of the Entity class itself.
 * @author NickAragua
 *
 */
public class EntityVisibilityUtils {
    /**
     * Logic lifted from BoardView1.redrawEntity() that checks whether the given player playing the given game
     * can see the given entity. Takes into account double blind, hidden units, team vision, etc.
     * @param localPlayer The player to check.
     * @param game Game object
     * @param entity The entity to check
     * @return Whether or not the player can see the entity.
     */
    public static boolean hasVisual(IPlayer localPlayer, IGame game, Entity entity) {
        boolean canSee = (localPlayer == null)
                || !game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
                || !entity.getOwner().isEnemyOf(localPlayer)
                || entity.hasSeenEntity(localPlayer)
                || entity.hasDetectedEntity(localPlayer);

        canSee &= (localPlayer == null)
                || !game.getOptions().booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)
                || !entity.getOwner().isEnemyOf(localPlayer)
                || !entity.isHidden();
        
        return canSee;
    }
}
