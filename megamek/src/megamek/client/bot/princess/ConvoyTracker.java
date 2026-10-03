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
package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import megamek.common.OffBoardDirection;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.LanceRole;
import megamek.common.units.Entity;

/**
 * Everything the bot knows about a convoy, in one place: which of its units are on the board, which one leads, the way
 * it is going, where it will be once it has moved, the edge it leaves by and the edge it left by. The convoy's own
 * deployment, its escorts' places and their facing all read it from here, so there is one convoy and not one per
 * feature (HammerGS, 2026-10-03).
 */
final class ConvoyTracker {

    /** A convoy is aimed at the first waypoint further off than this, so one close by does not swing it round. */
    static final int LOOK_AHEAD_HEXES = 5;

    private static final int DIRECTIONS = 6;

    /**
     * The convoy as its escorts see it.
     *
     * @param head    the front unit, where it will be
     * @param middle  the unit in the middle of the column, moved on with the front
     * @param tail    the last unit of the column, moved on with the front
     * @param heading the way the convoy is going
     */
    record Shape(Coords head, Coords middle, Coords tail, int heading) {}

    /** The way a convoy is going, and the round it was set. */
    private record Heading(int round, int heading) {}

    private final Princess owner;
    // by convoy force id
    private final Map<Integer, Heading> headings = new HashMap<>();
    private final Map<Integer, Coords> lastCenters = new HashMap<>();

    ConvoyTracker(Princess owner) {
        this.owner = owner;
    }

    /**
     * @return the convoy lance's units on the board, alive
     */
    List<Entity> unitsOnBoard(int convoyForceId, int boardId) {
        List<Entity> convoy = new ArrayList<>();
        for (Entity unit : owner.getGame().getEntitiesVector()) {
            if ((unit.getForceId() == convoyForceId) && isOnBoardAndAlive(unit) && (unit.getBoardId() == boardId)) {
                convoy.add(unit);
            }
        }
        return convoy;
    }

    static boolean isOnBoardAndAlive(Entity unit) {
        return (unit.getPosition() != null) && !unit.isOffBoard() && !unit.isDestroyed() && !unit.isDoomed()
              && unit.isDeployed();
    }

    /**
     * @param convoy the convoy's units on the board; at least one
     *
     * @return the front unit: the one its Column forms on, else the first
     */
    static Entity head(List<Entity> convoy) {
        for (Entity unit : convoy) {
            Optional<FormationOrder> formation = unit.getUnitOrders().getFormation();
            if (formation.isPresent() && (formation.get().getLeaderId() == unit.getId())) {
                return unit;
            }
        }
        Entity first = convoy.get(0);
        for (Entity unit : convoy) {
            if (unit.getId() < first.getId()) {
                first = unit;
            }
        }
        return first;
    }

    /**
     * @return {@code true} if the convoy's front unit has still to move this turn
     */
    boolean isStillToMove(Entity head) {
        return owner.getGame().getPhase().isMovement() && !head.isDone();
    }

    /**
     * The convoy where it will be once it has moved this turn - a walk on toward its next waypoint - or where it is,
     * if it has moved. Notes its middle, so escorts set to hold where it fell know where that was.
     *
     * @param convoyForceId the convoy lance
     * @param convoy        its units on the board; at least one
     *
     * @return the shape
     */
    Shape expectedShape(int convoyForceId, List<Entity> convoy) {
        Entity head = head(convoy);
        Board board = owner.getGame().getBoard(head);
        Coords headNow = head.getPosition();
        int heading = heading(convoyForceId, head);
        int steps = isStillToMove(head) ? head.getWalkMP() : 0;
        Optional<Coords> waypoint = head.getUnitOrders().getNextWaypoint();
        Coords headThen = clampToBoard(waypoint.isPresent() ? stepToward(headNow, waypoint.get(), steps)
              : headNow.translated(heading, steps), board);
        Shape shape = shape(convoy, head, headThen, heading, board);
        lastCenters.put(convoyForceId, shape.middle());
        return shape;
    }

    /**
     * @return the convoy where it stands now
     */
    Shape currentShape(int convoyForceId, List<Entity> convoy) {
        Entity head = head(convoy);
        return shape(convoy, head, head.getPosition(), heading(convoyForceId, head), owner.getGame().getBoard(head));
    }

    private static Shape shape(List<Entity> convoy, Entity head, Coords headThen, int heading, Board board) {
        Coords headNow = head.getPosition();
        int shift = headNow.distance(headThen);
        List<Entity> byDistance = new ArrayList<>(convoy);
        byDistance.sort(Comparator.comparingInt(unit -> unit.getPosition().distance(headNow)));
        Coords middle = clampToBoard(byDistance.get(byDistance.size() / 2).getPosition().translated(heading, shift),
              board);
        Coords tail = clampToBoard(byDistance.get(byDistance.size() - 1).getPosition().translated(heading, shift),
              board);
        return new Shape(headThen, middle, tail, heading);
    }

    /**
     * @return where the convoy's middle was last seen on the board, or empty if it never was
     */
    Optional<Coords> lastCenter(int convoyForceId) {
        return Optional.ofNullable(lastCenters.get(convoyForceId));
    }

    /**
     * The way the convoy is going. It turns at most one hex side a round and holds through the round: aimed straight
     * at the next waypoint, it swung round in the last hexes before each one and swapped its escorts' flanks over,
     * sending them back and forth across the convoy (HammerGS's playtest, 2026-10-02).
     *
     * @param convoyForceId the convoy lance
     * @param head          its front unit
     *
     * @return the heading 0-5
     */
    int heading(int convoyForceId, Entity head) {
        int round = owner.getGame().getCurrentRound();
        Heading known = headings.get(convoyForceId);
        if ((known != null) && (known.round() == round)) {
            return known.heading();
        }
        int wanted = lookAheadHeading(head, owner.getGame().getBoard(head));
        int heading = (known == null) ? wanted : turnOneSideToward(known.heading(), wanted);
        headings.put(convoyForceId, new Heading(round, heading));
        return heading;
    }

    /**
     * @return toward the first waypoint of the route more than {@link #LOOK_AHEAD_HEXES} hexes off, so a waypoint
     *       close by does not swing it; else toward its exit edge; else toward the end of its route; else its facing
     */
    private static int lookAheadHeading(Entity head, Board board) {
        Coords headNow = head.getPosition();
        List<Coords> route = head.getUnitOrders().getRoute();
        for (Coords waypoint : route) {
            if (headNow.distance(waypoint) > LOOK_AHEAD_HEXES) {
                return headNow.direction(waypoint);
            }
        }
        OptionalInt exitFacing = exitFacing(head, headNow, board);
        if (exitFacing.isPresent()) {
            return exitFacing.getAsInt();
        }
        if (!route.isEmpty() && !route.get(route.size() - 1).equals(headNow)) {
            return headNow.direction(route.get(route.size() - 1));
        }
        return head.getFacing();
    }

    /**
     * The way a convoy unit faces where it stands: straight at its exit edge, so it deploys pointing where it is
     * going (HammerGS, 2026-10-02).
     *
     * @param unit     a unit
     * @param position where it stands
     * @param board    its board
     *
     * @return the facing toward its convoy's exit edge, or empty for a unit that is not in a convoy
     */
    static OptionalInt exitFacing(Entity unit, Coords position, Board board) {
        LanceRole role = unit.getLanceRole();
        if ((role == null) || !role.isConvoy()) {
            return OptionalInt.empty();
        }
        Coords edgePoint = switch (role.getExitEdge()) {
            case NORTH -> new Coords(position.getX(), 0);
            case SOUTH -> new Coords(position.getX(), board.getHeight() - 1);
            case EAST -> new Coords(board.getWidth() - 1, position.getY());
            case WEST -> new Coords(0, position.getY());
            default -> position;
        };
        if (edgePoint.equals(position)) {
            // standing on the edge already: face straight off it
            return OptionalInt.of(switch (role.getExitEdge()) {
                case SOUTH -> 3;
                case EAST -> 2;
                case WEST -> 5;
                default -> 0;
            });
        }
        return OptionalInt.of(position.direction(edgePoint));
    }

    /**
     * @return the edge a convoy left the board by, once one of its units has; empty while none has, or when all
     *       were destroyed
     */
    Optional<OffBoardDirection> exitEdgeLeftBy(int convoyForceId) {
        for (Entity unit : owner.getGame().getOutOfGameEntitiesVector()) {
            int removal = unit.getRemovalCondition();
            boolean hasLeft = (removal == IEntityRemovalConditions.REMOVE_IN_RETREAT)
                  || (removal == IEntityRemovalConditions.REMOVE_PUSHED);
            if ((unit.getForceId() == convoyForceId) && hasLeft && (unit.getLanceRole() != null)
                  && unit.getLanceRole().isConvoy()) {
                return Optional.of(unit.getLanceRole().getExitEdge());
            }
        }
        return Optional.empty();
    }

    /**
     * @return the hex the given steps from a hex toward a target, stopping on the target
     */
    static Coords stepToward(Coords from, Coords target, int steps) {
        Coords position = from;
        for (int step = 0; (step < steps) && !position.equals(target); step++) {
            position = position.translated(position.direction(target));
        }
        return position;
    }

    /**
     * @return the hex, or the nearest board hex in its row and column where it is off the board
     */
    static Coords clampToBoard(Coords hex, Board board) {
        if (board.contains(hex)) {
            return hex;
        }
        int column = Math.max(0, Math.min(board.getWidth() - 1, hex.getX()));
        int row = Math.max(0, Math.min(board.getHeight() - 1, hex.getY()));
        return new Coords(column, row);
    }

    /**
     * @return the heading one hex side round from one heading toward another, the shorter way; the same heading when
     *       they match
     */
    static int turnOneSideToward(int from, int to) {
        int clockwiseSides = (to - from + DIRECTIONS) % DIRECTIONS;
        if (clockwiseSides == 0) {
            return from;
        }
        return (clockwiseSides <= (DIRECTIONS / 2)) ? (from + 1) % DIRECTIONS : (from + DIRECTIONS - 1) % DIRECTIONS;
    }
}
