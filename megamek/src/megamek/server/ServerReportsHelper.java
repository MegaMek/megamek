/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server;

import megamek.common.*;

public class ServerReportsHelper {

    public static int getUnitCount(Player player, Game game) {
        return Math.toIntExact(game.getPlayerEntities(player, false).stream()
                .filter(entity -> !entity.isDestroyed() && !entity.isTrapped() && !(entity instanceof EjectedCrew)).count());
    }

    public static int getUnitDamageCount(Player player, int damageLevel, Game game) {
        return Math.toIntExact(game.getPlayerEntities(player, false).stream()
                .filter(entity -> !entity.isDestroyed() && !entity.isTrapped() && (entity.getDamageLevel() == damageLevel)
                        && !(entity instanceof EjectedCrew)).count());
    }

    public static int getUnitDestroyedCount(Player player, Game game) {
        return Math.toIntExact(game.getOutOfGameEntitiesVector().stream()
                .filter(entity -> player.equals(entity.getOwner()) && entity.isDestroyed() && !(entity instanceof EjectedCrew)).count());
    }

    public static int getUnitCrewEjectedCount(Player player, Game game) {
        return Math.toIntExact(game.getOutOfGameEntitiesVector().stream()
                .filter(entity -> player.equals(entity.getOwner()) && entity.getCrew().isEjected() && !(entity instanceof EjectedCrew)).count());
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
                        (((entity instanceof MechWarrior) && ((MechWarrior) entity).getPickedUpById() == Entity.NONE) ||
                                ((entity instanceof EjectedCrew) && !(entity instanceof MechWarrior)))).count());
    }

    public static int getEjectedCrewPickedUpByTeamCount(Player player, Game game) {
        return Math.toIntExact(game.getPlayerEntities(player, false).stream()
                .filter(entity -> !entity.isDestroyed() && !entity.isTrapped() &&
                        ((entity instanceof MechWarrior) && ((MechWarrior) entity).getPickedUpById() != Entity.NONE
                                && game.getEntity(((MechWarrior) entity).getPickedUpById()).getOwner().getTeam() == player.getTeam())).count());
    }

    public static int getEjectedCrewPickedUpByEnemyTeamCount(Player player, Game game) {
        return Math.toIntExact(game.getPlayerEntities(player, false).stream()
                .filter(entity -> !entity.isDestroyed() && !entity.isTrapped() &&
                        ((entity instanceof MechWarrior) && ((MechWarrior) entity).getPickedUpById() != Entity.NONE
                                && game.getEntity(((MechWarrior) entity).getPickedUpById()).getOwner().getTeam() != player.getTeam())).count());
    }

    public static int getEjectedCrewKilledCount(Player player, Game game) {
        return Math.toIntExact(game.getOutOfGameEntitiesVector().stream()
                .filter(entity -> entity.getOwner().equals(player) && entity.isDestroyed() && (entity instanceof EjectedCrew)).count());
    }

    /**
     * get the total BV (unmodified by force size mod) for the units of this
     * player that have fled the field
     *
     * @return the BV
     * @param player
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

    private ServerReportsHelper() { }
}