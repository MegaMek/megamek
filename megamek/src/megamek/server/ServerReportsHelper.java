/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server;

import megamek.common.EjectedCrew;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.MekWarrior;
import megamek.common.Player;

public class ServerReportsHelper {

    public static int getUnitCount(Player player, Game game) {
        return Math.toIntExact(game.getPlayerEntities(player, false)
              .stream()
              .filter(entity -> !entity.isDestroyed() && !entity.isTrapped() && !(entity instanceof EjectedCrew))
              .count());
    }

    public static int getUnitDamageCount(Player player, int damageLevel, Game game) {
        return Math.toIntExact(game.getPlayerEntities(player, false).stream()
              .filter(entity -> !entity.isDestroyed() && !entity.isTrapped() && (entity.getDamageLevel() == damageLevel)
                    && !(entity instanceof EjectedCrew)).count());
    }

    public static int getUnitDestroyedCount(Player player, Game game) {
        return Math.toIntExact(game.getOutOfGameEntitiesVector().stream()
              .filter(entity -> player.equals(entity.getOwner())
                    && entity.isDestroyed()
                    && !(entity instanceof EjectedCrew)).count());
    }

    public static int getUnitCrewEjectedCount(Player player, Game game) {
        return Math.toIntExact(game.getOutOfGameEntitiesVector().stream()
              .filter(entity -> player.equals(entity.getOwner())
                    && entity.getCrew().isEjected()
                    && !(entity instanceof EjectedCrew)).count());
    }

    public static int getUnitCrewTrappedCount(Player player, Game game) {
        return Math.toIntExact(game.getOutOfGameEntitiesVector().stream()
              .filter(entity -> player.equals(entity.getOwner()) && entity.isDestroyed() && !entity.getCrew().isDead()
                    && !entity.getCrew().isEjected() && !(entity instanceof EjectedCrew)).count());
    }

    public static int getUnitCrewKilledCount(Player player, Game game) {
        return Math.toIntExact(game.getOutOfGameEntitiesVector().stream()
              .filter(entity -> player.equals(entity.getOwner()) && entity.getCrew().isDead()
                    && !entity.getCrew().isEjected() && !(entity instanceof EjectedCrew)).count());
    }

    public static int getEjectedCrewCount(Player player, Game game) {
        return Math.toIntExact(game.getPlayerEntities(player, false).stream()
              .filter(entity -> !entity.isDestroyed() && !entity.isTrapped() &&
                    (((entity instanceof MekWarrior) && ((MekWarrior) entity).getPickedUpById() == Entity.NONE) ||
                          ((entity instanceof EjectedCrew) && !(entity instanceof MekWarrior)))).count());
    }

    public static int getEjectedCrewPickedUpByTeamCount(Player player, Game game) {
        return Math.toIntExact(game.getPlayerEntities(player, false).stream()
              .filter(entity -> !entity.isDestroyed() && !entity.isTrapped() &&
                    ((entity instanceof MekWarrior) && ((MekWarrior) entity).getPickedUpById() != Entity.NONE
                          && game.getEntity(((MekWarrior) entity).getPickedUpById()).getOwner().getTeam()
                          == player.getTeam())).count());
    }

    public static int getEjectedCrewPickedUpByEnemyTeamCount(Player player, Game game) {
        return Math.toIntExact(game.getPlayerEntities(player, false).stream()
              .filter(entity -> !entity.isDestroyed() && !entity.isTrapped() &&
                    ((entity instanceof MekWarrior) && ((MekWarrior) entity).getPickedUpById() != Entity.NONE
                          && game.getEntity(((MekWarrior) entity).getPickedUpById()).getOwner().getTeam()
                          != player.getTeam())).count());
    }

    public static int getEjectedCrewKilledCount(Player player, Game game) {
        return Math.toIntExact(game.getOutOfGameEntitiesVector().stream()
              .filter(entity -> entity.getOwner().equals(player)
                    && entity.isDestroyed()
                    && (entity instanceof EjectedCrew)).count());
    }

    /**
     * get the total BV (unmodified by force size mod) for the units of this player that have fled the field
     *
     * @param player
     *
     * @return the BV
     */
    public static int getFledBV(Player player, Game game) {
        //TODO: I'm not sure how squadrons are treated here - see getBV()
        return game.getPlayerRetreatedEntities(player).stream()
              .filter(entity -> !entity.isDestroyed())
              .mapToInt(Entity::calculateBattleValue).sum();
    }

    public static int getFledUnitsCount(Player player, Game game) {
        //TODO: I'm not sure how squadrons are treated here - see getBV()
        return Math.toIntExact(game.getPlayerRetreatedEntities(player).stream()
              .filter(entity -> !entity.isDestroyed() && !(entity instanceof EjectedCrew))
              .mapToInt(Entity::calculateBattleValue).count());
    }

    public static int getFledEjectedCrew(Player player, Game game) {
        //TODO: I'm not sure how squadrons are treated here - see getBV()
        return Math.toIntExact(game.getPlayerRetreatedEntities(player).stream()
              .filter(entity -> !entity.isDestroyed() && (entity instanceof EjectedCrew))
              .mapToInt(Entity::calculateBattleValue).count());
    }

    private ServerReportsHelper() {}
}
