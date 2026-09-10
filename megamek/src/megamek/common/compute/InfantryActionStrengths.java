/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.compute;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;

/**
 * Who is in a building and what they are worth, for an infantry vs. infantry action (TO:AR pp. 169 to 172). The
 * client uses it to show a player what a declaration commits and faces; the server uses the same answers to book
 * the action, so the dialog never offers what the server would refuse.
 */
public final class InfantryActionStrengths {

    private InfantryActionStrengths() {
        // static use only
    }

    /**
     * Whether a unit stands in one of the building's hexes.
     *
     * @param unit     the unit
     * @param building the building
     *
     * @return {@code true} when the unit's position is a hex of the building on the same board
     */
    public static boolean isInside(@Nullable Entity unit, AbstractBuildingEntity building) {
        if ((unit == null) || (unit.getPosition() == null)) {
            return false;
        }
        boolean sameBoard = unit.getBoardId() == building.getBoardId();
        return sameBoard && building.getSecondaryPositions().containsValue(unit.getPosition());
    }

    /**
     * The infantry standing in the building that could fight for the given player: not the player's enemies, not
     * already in an action. The book makes everyone present the attacking force and lets the player hold units back,
     * so this is the list to offer.
     *
     * @param game     the game
     * @param player   the player declaring
     * @param building the building
     *
     * @return the units, in id order
     */
    public static List<Infantry> unengagedFriendlyInfantryInside(Game game, Player player,
          AbstractBuildingEntity building) {
        List<Infantry> units = new ArrayList<>();
        for (Entity entity : game.getEntitiesVector()) {
            boolean candidate = (entity instanceof Infantry)
                  && !entity.getOwner().isEnemyOf(player)
                  && (entity.getInfantryCombatTargetId() == Entity.NONE)
                  && isInside(entity, building);
            if (candidate) {
                units.add((Infantry) entity);
            }
        }
        return units;
    }

    /**
     * The infantry standing in the building that are the given player's enemies: what a declaration would face.
     *
     * @param game     the game
     * @param player   the player declaring
     * @param building the building
     *
     * @return the units, in id order
     */
    public static List<Infantry> enemyInfantryInside(Game game, Player player, AbstractBuildingEntity building) {
        List<Infantry> units = new ArrayList<>();
        for (Entity entity : game.getEntitiesVector()) {
            boolean enemyInside = (entity instanceof Infantry)
                  && entity.getOwner().isEnemyOf(player)
                  && isInside(entity, building);
            if (enemyInside) {
                units.add((Infantry) entity);
            }
        }
        return units;
    }

    /**
     * Whether the building has anyone to defend it: crew, bay personnel or marines.
     *
     * @param building the building
     *
     * @return {@code true} when the building itself counts as a defender
     */
    public static boolean hasCrewToDefend(AbstractBuildingEntity building) {
        return (building.getNCrew() + building.getBayPersonnel() + building.getNMarines()) > 0;
    }

    /**
     * The units already fighting in the building on one side, read from their own state, which every client holds.
     *
     * @param game      the game
     * @param building  the building
     * @param attackers {@code true} for the attackers, {@code false} for the defenders
     *
     * @return the engaged units on that side
     */
    public static List<Entity> engaged(Game game, AbstractBuildingEntity building, boolean attackers) {
        List<Entity> units = new ArrayList<>();
        for (Entity entity : game.getEntitiesVector()) {
            boolean onSide = (entity.getInfantryCombatTargetId() == building.getId())
                  && (entity.isInfantryCombatAttacker() == attackers);
            if (onSide) {
                units.add(entity);
            }
        }
        return units;
    }

    /**
     * What a unit is worth in Marine Points.
     *
     * @param unit             the unit
     * @param defendedBuilding the building it defends, for the building modifier, or {@code null} for an attacker
     *
     * @return the Marine Points, fractions kept
     */
    public static double points(Entity unit, @Nullable AbstractBuildingEntity defendedBuilding) {
        return MarinePointsScoreCalculator.calculateScore(unit, defendedBuilding);
    }

    /**
     * What a side is worth in Marine Points, fractions kept; the action rounds a side's total up.
     *
     * @param units            the units on the side
     * @param defendedBuilding the building they defend, or {@code null} for attackers
     *
     * @return the total
     */
    public static double total(List<? extends Entity> units, @Nullable AbstractBuildingEntity defendedBuilding) {
        double total = 0;
        for (Entity unit : units) {
            total += points(unit, defendedBuilding);
        }
        return total;
    }

    /**
     * Which side a player is on for a building: the defender when the building is not an enemy's.
     *
     * @param player   the player
     * @param building the building
     *
     * @return {@code true} when the player defends the building
     */
    public static boolean defends(Player player, AbstractBuildingEntity building) {
        return (building.getOwner() == null) || !building.getOwner().isEnemyOf(player);
    }

    /**
     * Whether an action is running in the building: some unit is attacking it.
     *
     * @param game     the game
     * @param building the building
     *
     * @return {@code true} while attackers are booked against it
     */
    public static boolean hasActionRunning(Game game, AbstractBuildingEntity building) {
        return !engaged(game, building, true).isEmpty();
    }

    /**
     * The buildings a player has a declaration to make for this turn: enemy buildings holding the player's infantry,
     * and the player's own buildings with enemy infantry inside or an action running.
     *
     * @param game   the game
     * @param player the player
     *
     * @return the buildings, in id order
     */
    public static List<AbstractBuildingEntity> stakes(Game game, Player player) {
        List<AbstractBuildingEntity> buildings = new ArrayList<>();
        for (Entity entity : game.getEntitiesVector()) {
            if ((entity instanceof AbstractBuildingEntity building) && hasStake(game, player, building)) {
                buildings.add(building);
            }
        }
        return buildings;
    }

    private static boolean hasStake(Game game, Player player, AbstractBuildingEntity building) {
        if (building.isDestroyed() || building.isCarcass()) {
            return false;
        }
        boolean ownUnitsInside = !unengagedFriendlyInfantryInside(game, player, building).isEmpty();
        if (defends(player, building)) {
            boolean threatened = !enemyInfantryInside(game, player, building).isEmpty()
                  || hasActionRunning(game, building);
            boolean somethingToCommit = ownUnitsInside || (building.getCrewAvailableToCommit() > 0);
            return threatened && somethingToCommit;
        }
        boolean ownForceEngaged = engaged(game, building, true).stream()
              .anyMatch(attacker -> attacker.getOwnerId() == player.getId());
        return ownUnitsInside || ownForceEngaged;
    }

    /**
     * Whether a unit gives its player a declaration turn: infantry inside a building where its player has a stake,
     * an engaged attacker that may withdraw, or a building whose owner has a stake in it.
     *
     * @param game the game
     * @param unit the unit
     *
     * @return {@code true} when the unit's player has something to declare because of it
     */
    public static boolean hasStake(Game game, Entity unit) {
        if (unit instanceof AbstractBuildingEntity building) {
            return hasStake(game, building.getOwner(), building);
        }
        if (unit.getInfantryCombatTargetId() != Entity.NONE) {
            return unit.isInfantryCombatAttacker() && !unit.isInfantryCombatWantsWithdrawal();
        }
        for (Entity entity : game.getEntitiesVector()) {
            boolean insideWithStake = (entity instanceof AbstractBuildingEntity building)
                  && isInside(unit, building) && hasStake(game, unit.getOwner(), building);
            if (insideWithStake) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param building the building
     *
     * @return the crew still alive and not yet committed
     */
    public static int crewAvailableToCommit(AbstractBuildingEntity building) {
        return building.getCrewAvailableToCommit();
    }

    /**
     * The Marine Points a building's crew is worth with the given number committed.
     *
     * @param building  the building
     * @param committed the crew committed
     *
     * @return the points
     */
    public static double crewPointsIfCommitted(AbstractBuildingEntity building, int committed) {
        return committed * MarinePointsScoreCalculator.NON_COMBAT_CREW * buildingModifier(building);
    }

    /**
     * The most the building's crew could be worth: every living crew member committed.
     *
     * @param building the building
     *
     * @return the points
     */
    public static double crewPointsIfAllCommitted(AbstractBuildingEntity building) {
        return crewPointsIfCommitted(building, building.getCrew().getCurrentSize());
    }

    /**
     * The crew hits the building would carry with more crew committed (TO:AR p. 170).
     *
     * @param building   the building
     * @param additional the crew about to be committed
     *
     * @return the crew hits
     */
    public static int crewHitsIfCommitted(AbstractBuildingEntity building, int additional) {
        return building.getCrewHitsIfCommitted(additional);
    }

    private static double buildingModifier(AbstractBuildingEntity building) {
        return MarinePointsScoreCalculator.buildingModifier(building);
    }

    /**
     * A side's total as the action counts it: rounded up (TO:AR p. 171).
     *
     * @param total the total with fractions
     *
     * @return the whole number the ratio uses
     */
    public static int roundedUp(double total) {
        return (int) Math.ceil(total);
    }
}
