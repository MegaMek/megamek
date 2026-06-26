/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.units;

import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;

/**
 * Class containing static functions that perform visibility computations related to an entity without the need to be a
 * part of the Entity class itself.
 *
 * @author NickAragua
 */
public class EntityVisibilityUtils {
    private static final MMLogger LOGGER = MMLogger.create(EntityVisibilityUtils.class);

    /**
     * Logic lifted from BoardView1.redrawEntity() that checks whether the given player playing the given game can see
     * the given entity. Takes into account double-blind, hidden units, team vision, etc. Game Master is excluded.
     *
     * @param localPlayer The player to check.
     * @param game        The current {@link Game}
     * @param entity      The entity to check
     *
     * @return Whether the player can see the entity.
     */
    public static boolean detectedOrHasVisual(Player localPlayer, Game game, Entity entity) {
        boolean doubleBlind = game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND);
        boolean isEnemy = (localPlayer != null) && entity.getOwner().isEnemyOf(localPlayer);
        boolean hasSeen = (localPlayer != null) && entity.hasSeenEntity(localPlayer);
        boolean hasDetected = (localPlayer != null) && entity.hasDetectedEntity(localPlayer);

        boolean canSee = (localPlayer == null)
              || !doubleBlind
              || !isEnemy
              || hasSeen
              || hasDetected;

        canSee &= (localPlayer == null)
              || !game.getOptions().booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)
              || !isEnemy
              || !entity.isHidden();

        LOGGER.debug(
              "detectedOrHasVisual: entity={} viewer={} doubleBlind={} isEnemy={} hasSeen={} hasDetected={} canSee={}",
              entity.getDisplayName(),
              (localPlayer != null ? localPlayer.getName() : "null"),
              doubleBlind,
              isEnemy,
              hasSeen,
              hasDetected,
              canSee);
        return canSee;
    }

    /**
     * Used to determine if this entity is only detected by an enemies sensors and hence should only be a sensor
     * return.
     *
     */
    public static boolean onlyDetectedBySensors(@Nullable Player localPlayer, Entity entity) {
        boolean usesAdvancedTacOpsSensors = entity.getGame()
              .getOptions()
              .booleanOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS);
        boolean usesAdvancedStratOpsSensors = entity.getGame()
              .getOptions()
              .booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADVANCED_SENSORS);

        boolean usesSensors = usesAdvancedTacOpsSensors || usesAdvancedStratOpsSensors;

        boolean sensorsDetectAll = entity.getGame()
              .getOptions()
              .booleanOption(OptionsConstants.ADVANCED_SENSORS_DETECT_ALL);
        boolean doubleBlind = entity.gameOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND);

        if (!doubleBlind) {
            return false;
        }

        if (localPlayer == null) {
            return true;
        }

        boolean hasVisual = entity.hasSeenEntity(localPlayer);
        boolean hasDetected = entity.hasDetectedEntity(localPlayer);
        boolean doesNotTrackThisEntitiesVisibilityInfo = !EntityVisibilityUtils.trackThisEntitiesVisibilityInfo(
              localPlayer,
              entity);
        boolean result = usesSensors
              && !sensorsDetectAll
              && doesNotTrackThisEntitiesVisibilityInfo
              && hasDetected
              && !hasVisual;
        LOGGER.debug("onlyDetectedBySensors: entity={} viewer={} usesSensors={} sensorsDetectAll={} "
                    + "doubleBlind={} hasVisual={} hasDetected={} doesNotTrackVis={} result={}",
              entity.getDisplayName(),
              localPlayer.getName(),
              usesSensors, sensorsDetectAll, doubleBlind,
              hasVisual, hasDetected, doesNotTrackThisEntitiesVisibilityInfo, result);
        return result;
    }

    /**
     * We only want to show double-blind visibility indicators on our own meks and teammates meks (assuming team vision
     * option).
     */
    public static boolean trackThisEntitiesVisibilityInfo(Player localPlayer, Entity e) {
        if (localPlayer == null) {
            return false;
        }

        return e.gameOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
              && ((e.getOwner().getId() == localPlayer.getId()) ||
              (e.gameOptions().booleanOption(OptionsConstants.ADVANCED_TEAM_VISION)
                    && (e.getOwner().getTeam() == localPlayer.getTeam())));
    }
}
