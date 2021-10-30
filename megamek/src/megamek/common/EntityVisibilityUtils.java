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
    public static boolean detectedOrHasVisual(IPlayer localPlayer, Game game, Entity entity) {
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
    
    /**
     * Used to determine if this entity is only detected by an enemies
     * sensors and hence should only be a sensor return.
     *
     * @return
     */
    public static boolean onlyDetectedBySensors(IPlayer localPlayer, Entity entity) {
        boolean sensors = (entity.getGame().getOptions().booleanOption(
                OptionsConstants.ADVANCED_TACOPS_SENSORS)
                || entity.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS));
        boolean sensorsDetectAll = entity.getGame().getOptions().booleanOption(
                OptionsConstants.ADVANCED_SENSORS_DETECT_ALL);
        boolean doubleBlind = entity.getGame().getOptions().booleanOption(
                OptionsConstants.ADVANCED_DOUBLE_BLIND);
        boolean hasVisual = entity.hasSeenEntity(localPlayer);
        boolean hasDetected = entity.hasDetectedEntity(localPlayer);

        if (sensors && doubleBlind && !sensorsDetectAll
                && !EntityVisibilityUtils.trackThisEntitiesVisibilityInfo(localPlayer, entity)
                && hasDetected && !hasVisual) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * We only want to show double-blind visibility indicators on our own
     * mechs and teammates mechs (assuming team vision option).
     */
    public static boolean trackThisEntitiesVisibilityInfo(IPlayer localPlayer, Entity e) {
        if (localPlayer == null) {
            return false;
        }
        
        if (e.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND) //$NON-NLS-1$
                && ((e.getOwner().getId() == localPlayer.getId()) || 
                        (e.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_TEAM_VISION) //$NON-NLS-1$
                && (e.getOwner().getTeam() == localPlayer.getTeam())))) {
            return true;
        }
        
        return false;
    }
}
