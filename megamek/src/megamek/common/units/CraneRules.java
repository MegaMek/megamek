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
package megamek.common.units;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Hex;
import megamek.common.annotations.Nullable;
import megamek.common.bays.Bay;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;

/**
 * Stateless rules for loading and unloading units with the cranes of a grounded Small Craft or DropShip (TW p.90-91).
 * VTOLs, Small Craft and fighters cannot mount or dismount under their own power. A unit waiting in an adjacent hex,
 * within two levels, is loaded over four turns; a carried unit is unloaded into an adjacent hex over three. The client
 * (button and step legality) and the server (declaration and End Phase work) share these checks.
 */
public final class CraneRules {

    /** A crane can reach a unit up to two levels above or below the carrier's hex (TW p.90-91). */
    public static final int MAXIMUM_LEVEL_DIFFERENCE = 2;

    private CraneRules() {}

    /**
     * Checks whether a unit must be loaded and unloaded by crane instead of mounting or dismounting under its own power
     * (TW p.90-91). This covers VTOLs, fighters (including conventional fighters) and Small Craft; a DropShip is not a
     * Small Craft here.
     *
     * @param unit the unit to check
     *
     * @return {@code true} if the unit may only board or leave a Small Craft or DropShip by crane
     */
    public static boolean isCraneOnlyUnit(Entity unit) {
        return (unit instanceof VTOL) || unit.isFighter() || unit.isSmallCraft();
    }

    /**
     * Checks whether a unit is a Small Craft or DropShip that can work its cranes: on the ground, on the board and not
     * destroyed.
     *
     * @param carrier the possible carrier, may be {@code null}
     *
     * @return {@code true} if the unit is a grounded, operational Small Craft or DropShip
     */
    public static boolean isGroundedCarrier(@Nullable Entity carrier) {
        return (carrier instanceof SmallCraft)
              && !carrier.isAirborne()
              && !carrier.isDestroyed()
              && !carrier.isDoomed()
              && (carrier.getPosition() != null);
    }

    /**
     * Checks whether a unit in the given hex is where the carrier's cranes can reach it: in a hex adjacent to one of the
     * carrier's hexes, and within two levels of that hex (TW p.90).
     *
     * @param unit         the unit to be loaded
     * @param unitPosition the hex the unit is in, may be {@code null}
     * @param carrier      the grounded Small Craft or DropShip
     * @param game         the game
     *
     * @return {@code true} if the cranes can reach the unit
     */
    public static boolean isInReach(Entity unit, @Nullable Coords unitPosition, Entity carrier, Game game) {
        if (!isGroundedCarrier(carrier) || (unitPosition == null) || (carrier.getBoardId() != unit.getBoardId())) {
            return false;
        }
        Board board = game.getBoard(unit.getBoardId());
        Hex unitHex = board.getHex(unitPosition);
        if (unitHex == null) {
            return false;
        }
        int unitLevel = unitHex.getLevel() + unit.getElevation();
        for (Coords carrierHex : carrier.getOccupiedCoords()) {
            if (carrierHex.distance(unitPosition) != 1) {
                continue;
            }
            Hex hex = board.getHex(carrierHex);
            if ((hex != null) && (Math.abs(hex.getLevel() - unitLevel) <= MAXIMUM_LEVEL_DIFFERENCE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds a bay in the carrier that can take the unit. Crane loading does not use a door (TW p.90), but the carrier
     * still needs an appropriate bay with room for the unit.
     *
     * @param carrier the Small Craft or DropShip
     * @param unit    the unit to be loaded
     *
     * @return a bay that can load the unit, or {@code null} if there is none
     */
    public static @Nullable Bay findBayFor(Entity carrier, Entity unit) {
        for (Bay bay : carrier.getTransportBays()) {
            if (bay.canLoad(unit)) {
                return bay;
            }
        }
        return null;
    }

    /**
     * Lists the friendly grounded Small Craft and DropShips whose cranes can load the unit where it stands now.
     *
     * @param unit the VTOL, fighter or Small Craft to be loaded
     * @param game the game
     *
     * @return the carriers that could load the unit, possibly empty
     */
    public static List<SmallCraft> carriersInReach(Entity unit, Game game) {
        List<SmallCraft> carriers = new ArrayList<>();
        Coords position = unit.getPosition();
        if ((position == null) || !isCraneOnlyUnit(unit) || unit.isAirborne()) {
            return carriers;
        }
        // At most six hexes, each holding a handful of units
        for (Coords adjacent : position.allAdjacent()) {
            for (Entity other : game.getEntitiesVector(adjacent, unit.getBoardId())) {
                if ((other instanceof SmallCraft carrier)
                      && !carrier.equals(unit)
                      && !carriers.contains(carrier)
                      && !carrier.isEnemyOf(unit)
                      && isInReach(unit, position, carrier, game)
                      && (findBayFor(carrier, unit) != null)) {
                    carriers.add(carrier);
                }
            }
        }
        return carriers;
    }

    /**
     * Lists the hexes a carrier's cranes can put a unit down in: the hexes adjacent to the carrier, within two levels of
     * its hex, that the unit can enter without breaking the stacking rules (TW p.91).
     *
     * @param carrier the grounded Small Craft or DropShip
     * @param unit    the carried unit
     * @param game    the game
     *
     * @return the legal unloading hexes, possibly empty
     */
    public static List<Coords> unloadPositions(SmallCraft carrier, Entity unit, Game game) {
        List<Coords> positions = new ArrayList<>();
        Coords centre = carrier.getPosition();
        if (centre == null) {
            return positions;
        }
        int boardId = carrier.getBoardId();
        Hex carrierHex = game.getHex(centre, boardId);
        if (carrierHex == null) {
            return positions;
        }
        // A grounded DropShip covers its centre hex and the six around it, so its neighbours are two hexes out
        List<Coords> candidates = (carrier instanceof Dropship) ? centre.allAtDistance(2) : centre.allAdjacent();
        for (Coords candidate : candidates) {
            Hex hex = game.getHex(candidate, boardId);
            if ((hex != null)
                  && (Math.abs(hex.getLevel() - carrierHex.getLevel()) <= MAXIMUM_LEVEL_DIFFERENCE)
                  && Compute.isAcceptableUnloadPosition(candidate, boardId, unit, game, carrierHex.getLevel())) {
                positions.add(candidate);
            }
        }
        return positions;
    }

    /**
     * Lists the units a carrier holds that only its cranes can unload and that the cranes are not already working on.
     *
     * @param carrier the Small Craft or DropShip
     *
     * @return the units that could be unloaded by crane, possibly empty
     */
    public static List<Entity> craneUnloadableUnits(SmallCraft carrier) {
        List<Entity> units = new ArrayList<>();
        for (Entity loadedUnit : carrier.getLoadedUnits()) {
            if (isCraneOnlyUnit(loadedUnit) && (carrier.getCraneOperations().findFor(loadedUnit.getId()) == null)) {
                units.add(loadedUnit);
            }
        }
        return units;
    }

    /**
     * Checks a declared LOAD_BY_CRANE step.
     *
     * @param unit        the unit asking to be loaded
     * @param target      the carrier named by the step, may be {@code null}
     * @param isFirstStep {@code true} if the unit has taken no other action this move
     * @param game        the game
     *
     * @return why the step is illegal, for logging, or {@code null} if it is legal
     */
    public static @Nullable String loadByCraneIllegalReason(Entity unit, @Nullable Targetable target,
          boolean isFirstStep, Game game) {
        if (!isCraneOnlyUnit(unit)) {
            return "only VTOLs, fighters and small craft are loaded by crane";
        }
        if (unit.isAirborne()) {
            return "the unit is airborne";
        }
        if (!isFirstStep) {
            return "crane loading must be the unit's only action";
        }
        if (!(target instanceof SmallCraft carrier)) {
            return "no small craft or DropShip was named";
        }
        if (!isGroundedCarrier(carrier)) {
            return "the carrier is not grounded";
        }
        if (carrier.isEnemyOf(unit)) {
            return "the carrier is not friendly";
        }
        if (!isInReach(unit, unit.getPosition(), carrier, game)) {
            return "the unit is not in an adjacent hex within two levels of the carrier";
        }
        if (findBayFor(carrier, unit) == null) {
            return "the carrier has no suitable bay space";
        }
        return null;
    }

    /**
     * Checks a declared UNLOAD_BY_CRANE step.
     *
     * @param carrier     the unit declaring the step
     * @param target      the carried unit named by the step, may be {@code null}
     * @param position    the hex chosen for the unit, may be {@code null}
     * @param isFirstStep {@code true} if the carrier has taken no other action this move
     * @param game        the game
     *
     * @return why the step is illegal, for logging, or {@code null} if it is legal
     */
    public static @Nullable String unloadByCraneIllegalReason(Entity carrier, @Nullable Targetable target,
          @Nullable Coords position, boolean isFirstStep, Game game) {
        if (!(carrier instanceof SmallCraft smallCraft)) {
            return "only a small craft or DropShip unloads by crane";
        }
        if (!isGroundedCarrier(smallCraft)) {
            return "the carrier is not grounded";
        }
        if (!isFirstStep) {
            return "crane unloading must be the carrier's only action";
        }
        if (!(target instanceof Entity unit) || (unit.getTransportId() != carrier.getId())) {
            return "the unit is not carried by this carrier";
        }
        if (!isCraneOnlyUnit(unit)) {
            return "only VTOLs, fighters and small craft are unloaded by crane";
        }
        if ((position == null) || !unloadPositions(smallCraft, unit, game).contains(position)) {
            return "the chosen hex is not a legal hex beside the carrier";
        }
        return null;
    }

    /**
     * Checks whether a unit fired any weapon this round, which cancels crane loading.
     *
     * @param unit the unit waiting to be loaded
     *
     * @return {@code true} if any of its weapons were used this round
     */
    public static boolean hasFiredWeapons(Entity unit) {
        for (WeaponMounted weapon : unit.getWeaponList()) {
            if (weapon.isUsedThisRound()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the carrier that is currently loading or unloading a unit by crane.
     *
     * @param unitId the id of the unit
     * @param game   the game
     *
     * @return the carrier working on the unit, or {@code null} if no crane operation involves it
     */
    public static @Nullable SmallCraft findCarrierWorkingOn(int unitId, Game game) {
        for (Entity entity : game.getEntitiesVector()) {
            if ((entity instanceof SmallCraft carrier) && (carrier.getCraneOperations().findFor(unitId) != null)) {
                return carrier;
            }
        }
        return null;
    }
}
