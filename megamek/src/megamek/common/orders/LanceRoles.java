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
package megamek.common.orders;

import java.util.ArrayList;
import java.util.List;

import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.force.Force;
import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * Finding and defaulting lance roles: which lances on a side are convoys an escort can guard, and the edge a new convoy
 * leaves by.
 */
public final class LanceRoles {

    /**
     * A lance an escort can be given: a convoy, or a lance that becomes one when an escort is given it.
     *
     * @param forceId     the lance's force id
     * @param name        its name
     * @param unitCount   how many units it has
     * @param isConvoy    {@code true} if it is a convoy already
     * @param newExitEdge the edge it leaves by if it becomes a convoy: opposite where it deploys
     */
    public record ConvoyChoice(int forceId, String name, int unitCount, boolean isConvoy,
          OffBoardDirection newExitEdge) {

        /**
         * @return a choice for a lance that is a convoy already
         */
        public static ConvoyChoice convoy(int forceId, String name, int unitCount) {
            return new ConvoyChoice(forceId, name, unitCount, true, OffBoardDirection.NORTH);
        }
    }

    private LanceRoles() {
    }

    /**
     * The lances an escort can be given: every other lance on the side with a bot's units, convoys first. One that is not a
     * convoy yet becomes one when an escort is given it, so roles can be set in any order (HammerGS, 2026-10-03).
     * Escort lances are left out.
     *
     * @param game           the game
     * @param side           a player of the side
     * @param excludeForceId a lance to leave out - the escort itself - or -1
     *
     * @return the lances, convoys first, each group in force order
     */
    public static List<ConvoyChoice> convoyChoices(Game game, Player side, int excludeForceId) {
        List<ConvoyChoice> convoys = new ArrayList<>();
        List<ConvoyChoice> others = new ArrayList<>();
        for (Force force : game.getForces().getAllForces()) {
            if (force.getId() == excludeForceId) {
                continue;
            }
            List<Entity> units = new ArrayList<>();
            for (int unitId : force.getEntities()) {
                Entity unit = game.getEntity(unitId);
                if ((unit != null) && (unit.getOwner() != null) && !unit.getOwner().isEnemyOf(side)) {
                    units.add(unit);
                }
            }
            LanceRole role = roleOf(units);
            // only a bot's units take the convoy role, so a lance with none of them cannot become a convoy
            if (!hasBotUnit(units) || ((role != null) && role.isEscort())) {
                continue;
            }
            boolean isConvoy = (role != null) && role.isConvoy();
            ConvoyChoice choice = new ConvoyChoice(force.getId(), force.getName(), units.size(), isConvoy,
                  defaultExitEdge(units.get(0)));
            (isConvoy ? convoys : others).add(choice);
        }
        convoys.addAll(others);
        return convoys;
    }

    private static boolean hasBotUnit(List<Entity> units) {
        for (Entity unit : units) {
            if (unit.getOwner().isBot()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return a lance's units still in the game, deployed or not
     */
    public static List<Entity> unitsOf(Game game, int forceId) {
        List<Entity> units = new ArrayList<>();
        Force force = game.getForces().getForce(forceId);
        if (force == null) {
            return units;
        }
        for (int unitId : force.getEntities()) {
            Entity unit = game.getEntity(unitId);
            if ((unit != null) && !unit.isDestroyed()) {
                units.add(unit);
            }
        }
        return units;
    }

    /**
     * The edge a new convoy leaves by: the one opposite where its units deploy - deploy south, exit north (HammerGS,
     * 2026-10-02). North when the deployment is not along an edge.
     *
     * @param unit one of the convoy's units, or {@code null}
     *
     * @return the edge
     */
    public static OffBoardDirection defaultExitEdge(@Nullable Entity unit) {
        int startingPosition = Board.START_ANY;
        if (unit != null) {
            // a unit with no zone of its own deploys in its owner's
            startingPosition = unit.getStartingPos(unit.getOwner() != null);
        }
        return switch (startingPosition) {
            case Board.START_N, Board.START_NE, Board.START_NW -> OffBoardDirection.SOUTH;
            case Board.START_E -> OffBoardDirection.WEST;
            case Board.START_W -> OffBoardDirection.EAST;
            default -> OffBoardDirection.NORTH;
        };
    }

    /**
     * @param units a lance's units
     *
     * @return the role its units share, or the first one found; {@code null} when none has a role
     */
    public static @Nullable LanceRole roleOf(List<Entity> units) {
        for (Entity unit : units) {
            if (unit.getLanceRole() != null) {
                return unit.getLanceRole();
            }
        }
        return null;
    }
}
