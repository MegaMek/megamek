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
import java.util.PriorityQueue;

import jakarta.annotation.Nullable;
import megamek.common.Hex;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.moves.MoveStep;
import megamek.common.orders.LanceRole;
import megamek.common.orders.LanceRoles;
import megamek.common.pathfinder.BoardClusterTracker;
import megamek.common.pathfinder.BoardEdgePathFinder;
import megamek.common.pathfinder.BoardEdgePathFinder.GroundElevationLimits;
import megamek.common.pathfinder.MovementType;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;

/**
 * How many movement points every hex of a board is from one waypoint, by the cheapest way a unit can actually walk,
 * drive or hover there. It is worked out once, outward from the waypoint, so every unit heading for the same waypoint
 * shares it.
 *
 * <p>The bot still picks each turn's move from the moves it can legally make; this only tells it which of those
 * moves leave it closest to the waypoint by the real route. That is what gets a unit round a horseshoe lake or out of
 * a dead-end street: the hexes along the way round really are fewer movement points from the waypoint than the shore
 * facing it across the water (issue #7615). The technique is a "Dijkstra map", as described at
 * https://www.redblobgames.com/pathfinding/tower-defense/.</p>
 *
 * <p>Entering a hex costs one point, plus the terrain's extra cost, plus one per level of elevation change. The
 * hexes a unit can enter and the climbs it can make follow {@link BoardClusterTracker}, so this field never treats as
 * reachable a hex the bot's reachability check would refuse. The costs are a guide, not the rules: facing changes
 * and movement-mode details are left to the legal moves the bot chooses among.</p>
 */
final class WaypointDistanceField {

    private static final MMLogger LOGGER = MMLogger.create(WaypointDistanceField.class);

    /** The cost of a hex the unit cannot reach from the waypoint at all. */
    static final int UNREACHABLE = Integer.MAX_VALUE;

    // the waypoint measured to, or null for a field to a board edge (MM @Nullable is not applicable to fields)
    private final Coords waypoint;
    private final Map<Coords, Integer> costToWaypoint = new HashMap<>();
    private final Map<Coords, Map<Integer, Integer>> bridgeCostsByElevation = new HashMap<>();

    private WaypointDistanceField(@Nullable Coords waypoint) {
        this.waypoint = waypoint;
    }

    /**
     * Works out the field for one unit's way of moving.
     *
     * @param mover    a unit whose movement type decides which hexes can be entered and climbed
     * @param waypoint the hex to measure to
     *
     * @return the field
     */
    static WaypointDistanceField build(Entity mover, Coords waypoint) {
        return build(mover, waypoint, Map.of());
    }

    /**
     * Works out the field for one unit's way of moving, with some hexes costing more to enter, such as those in front
     * of units already in their places (see {@link TownLegPlanner#frontOfUnitsInPlace}).
     *
     * @param mover     a unit whose movement type decides which hexes can be entered and climbed
     * @param waypoint  the hex to measure to
     * @param extraCost the extra movement points to enter each such hex
     *
     * @return the field
     */
    static WaypointDistanceField build(Entity mover, Coords waypoint, Map<Coords, Integer> extraCost) {
        Board board = (mover.getGame() == null) ? null : mover.getGame().getBoard(mover);
        if ((board == null) || !board.contains(waypoint)) {
            LOGGER.debug("[BotOrders] {}: waypoint {} cannot be entered; no distance field", mover.getDisplayName(),
                  waypoint.getBoardNum());
            return new WaypointDistanceField(waypoint);
        }
        return fill(mover, board, waypoint, List.of(waypoint), waypoint.getBoardNum(), extraCost);
    }

    /**
     * Works out the field to a whole board edge: the movement points from every hex to the nearest hex of the edge the
     * unit can stand on, by the cheapest way round. A unit behind a lake then takes the way round to the edge instead
     * of freezing on the shore nearest it (HammerGS's playtest, 2026-09-26).
     *
     * @param mover a unit whose movement type decides which hexes can be entered and climbed
     * @param edge  the edge to measure to
     *
     * @return the field; it reaches nothing when the unit can stand on no hex of the edge
     */
    static WaypointDistanceField buildToEdge(Entity mover, CardinalEdge edge) {
        Board board = (mover.getGame() == null) ? null : mover.getGame().getBoard(mover);
        if (board == null) {
            return new WaypointDistanceField(null);
        }
        return fill(mover, board, null, edgeHexes(board, edge), edge.name() + " edge", Map.of());
    }

    private static List<Coords> edgeHexes(Board board, CardinalEdge edge) {
        List<Coords> hexes = new ArrayList<>();
        int width = board.getWidth();
        int height = board.getHeight();
        switch (edge) {
            case NORTH -> {
                for (int x = 0; x < width; x++) {
                    hexes.add(new Coords(x, 0));
                }
            }
            case SOUTH -> {
                for (int x = 0; x < width; x++) {
                    hexes.add(new Coords(x, height - 1));
                }
            }
            case WEST -> {
                for (int y = 0; y < height; y++) {
                    hexes.add(new Coords(0, y));
                }
            }
            case EAST -> {
                for (int y = 0; y < height; y++) {
                    hexes.add(new Coords(width - 1, y));
                }
            }
            default -> {
            }
        }
        return hexes;
    }

    /** The margin Princess allows over a unit's weight before it trusts a building to bear it. */
    private static final int COLLAPSE_MARGIN_TONS = 10;

    /**
     * What a building hex adds to the way through it, in movement points: enough that a way round a few hexes longer
     * wins, while a route with no way round still goes through.
     */
    static final int BUILDING_DETOUR_COST = 6;

    /**
     * What a hex of water a unit stands in up to its knees or deeper adds to the way through it, in movement points,
     * for a unit that wades: the way round wins unless it is much longer (HammerGS, 2026-10-01).
     */
    static final int WATER_DETOUR_COST = 4;

    /** What a hex of shallow water adds to the way through it, for a unit that wades. */
    static final int SHALLOW_WATER_DETOUR_COST = 1;

    /**
     * Spreads the cost outward from the goal hexes, which cost nothing, walking backward: a unit in each neighbour
     * would step into the hex already reached.
     */
    private static WaypointDistanceField fill(Entity mover, Board board, @Nullable Coords waypoint, List<Coords> goals,
          String goalName, Map<Coords, Integer> extraCost) {
        Map<Coords, List<Surface>> surfaces = new HashMap<>();
        MovementType movementType = MovementType.getMovementType(mover);
        GroundElevationLimits limits = GroundElevationLimits.forEntity(mover);
        LanceRole role = LanceRoles.effectiveRole(mover);
        boolean convoy = role != null && role.isConvoy();

        // A bridge can carry one route and span a different one. Retain the elevation until the search is finished.
        Map<Surface, Integer> distances = new HashMap<>();
        PriorityQueue<Visit> frontier = new PriorityQueue<>(Comparator.comparingInt(Visit::cost));
        for (Coords goal : goals) {
            for (Surface surface : surfaces.computeIfAbsent(goal, c -> surfacesAt(mover, movementType, board, c))) {
                distances.put(surface, 0);
                frontier.add(new Visit(surface, 0));
            }
        }
        while (!frontier.isEmpty()) {
            Visit visit = frontier.remove();
            Surface into = visit.surface();
            if (visit.cost() != distances.get(into)) {
                continue;
            }
            Coords current = into.coords();
            for (int direction = 0; direction < 6; direction++) {
                Coords neighbor = current.translated(direction);
                if (!board.contains(neighbor)) {
                    continue;
                }
                for (Surface from : surfaces.computeIfAbsent(neighbor, c -> surfacesAt(mover, movementType, board, c))) {
                    int step = stepCost(from, into, limits, convoy, extraCost.getOrDefault(current, 0));
                    if (step != UNREACHABLE && visit.cost() + step < distances.getOrDefault(from, UNREACHABLE)) {
                        int cost = visit.cost() + step;
                        distances.put(from, cost);
                        frontier.add(new Visit(from, cost));
                    }
                }
            }
        }
        WaypointDistanceField result = new WaypointDistanceField(waypoint);
        distances.forEach((surface, cost) -> {
            Coords coords = surface.coords();
            result.costToWaypoint.merge(coords, cost, Math::min);
            if (surface.hex().hasBridge()) {
                result.bridgeCostsByElevation.computeIfAbsent(coords, c -> new HashMap<>())
                      .put(surface.elevation(), cost);
            }
        });
        LOGGER.debug("[BotOrders] distance field to {} for {}: {} hexes reachable", goalName, movementType,
              result.costToWaypoint.size());
        return result;
    }

    private record Visit(Surface surface, int cost) {
    }

    private record Surface(Coords coords, Hex hex, int altitude, int enteringCost, boolean requiresRoad,
          boolean mustUseBridgeExits) {
        boolean permitsExit(int direction) {
            return !mustUseBridgeExits || hex.containsTerrainExit(Terrains.BRIDGE, direction);
        }

        int elevation() {
            return altitude - hex.getLevel();
        }
    }

    private static List<Surface> surfacesAt(Entity mover, MovementType type, Board board, Coords coords) {
        Hex hex = board.getHex(coords);
        if (mover instanceof BattlefieldSupportAsset asset && asset.isSnowProhibited(hex)) {
            return List.of();
        }
        boolean hover = type == MovementType.Hover;
        boolean amphibious = type == MovementType.WheeledAmphibious || type == MovementType.TrackedAmphibious;
        boolean wades = !hover && !amphibious && type != MovementType.Water;
        int ground = BoardEdgePathFinder.calculateUnitElevationInHex(hex, mover, hover, amphibious);
        int groundElevation = ground - hex.getLevel();
        List<Surface> surfaces = new ArrayList<>(hex.hasBridge() ? 2 : 1);
        if ((!hex.hasBridge() || (groundElevation != hex.terrainLevel(Terrains.BRIDGE_ELEV)
              && mover.isElevationValid(groundElevation, hex)))
              && !mover.isLocationProhibited(coords, board.getBoardId(), groundElevation)
              && !BoardClusterTracker.buildingPlowThroughRequired(mover, type, coords)
              && !wouldBringDownBuilding(mover, board, coords, groundElevation)) {
            surfaces.add(new Surface(coords, hex, ground, enteringCost(mover, hex, wades),
                  BoardEdgePathFinder.requiresRoad(mover, board, coords, hex, ground), false));
        }
        if (hex.hasBridge() && MovementType.canUseBridge(type) && !hex.containsTerrain(Terrains.IMPASSABLE)) {
            int deck = hex.terrainLevel(Terrains.BRIDGE_ELEV);
            if (!wouldBringDownBuilding(mover, board, coords, deck)) {
                surfaces.add(new Surface(coords, hex, hex.getLevel() + deck, 1, false,
                      MoveStep.mustUseBridgeExits(mover, board.getBoardId(), hex, deck, coords)));
            }
        }
        return surfaces;
    }

    private static int stepCost(Surface from, Surface into, GroundElevationLimits limits, boolean convoy, int extra) {
        int direction = from.coords().direction(into.coords());
        if (!from.permitsExit(direction) || !into.permitsExit((direction + 3) % 6)
              || !limits.permits(from.hex(), from.altitude(), into.hex(), into.altitude(), direction)) {
            return UNREACHABLE;
        }
        boolean road = Compute.isRoadStep(from.hex(), from.elevation(), into.hex(), into.elevation(), direction, false);
        if (!road && (from.requiresRoad() || into.requiresRoad())) {
            return UNREACHABLE;
        }
        return Math.abs(from.altitude() - into.altitude()) + (convoy && road ? 1 : into.enteringCost() + extra);
    }

    /**
     * @return the movement points to step into a hex, the climb aside: its terrain, and the detours that keep units
     *       out of buildings and water
     */
    private static int enteringCost(Entity mover, Hex hex, boolean isWading) {
        int cost = 1 + Math.max(0, hex.movementCost(mover));
        if (hex.containsTerrain(Terrains.BUILDING)) {
            // walk round a building rather than through it: going in damages it and can bring it down on the
            // unit (HammerGS's playtest, 2026-09-27)
            cost += BUILDING_DETOUR_COST;
        }
        if (isWading && hex.containsTerrain(Terrains.WATER)) {
            // walk round water rather than wade it: a 3 MP Stalker waded a lake a hex a turn because the way
            // round cost the same (HammerGS's playtest, 2026-10-01); his own walk never entered water
            cost += (hex.depth() >= 1) ? WATER_DETOUR_COST : SHALLOW_WATER_DETOUR_COST;
        }
        return cost;
    }

    /**
     * What one step costs a unit by the same rules the field is worked out by, so a way planned along the field can
     * be checked hex by hex.
     *
     * @param mover the unit
     * @param board its board
     * @param from  the hex it steps from
     * @param into  the neighbouring hex it steps into
     *
     * @return the movement points, or {@link #UNREACHABLE} when it cannot step there
     */
    static int stepCost(Entity mover, Board board, Coords from, Coords into) {
        if (!board.contains(from) || !board.contains(into)) {
            return UNREACHABLE;
        }
        MovementType type = MovementType.getMovementType(mover);
        GroundElevationLimits limits = GroundElevationLimits.forEntity(mover);
        List<Surface> destinations = surfacesAt(mover, type, board, into);
        // Looking up a lance role scans the game. Only convoys can benefit from a road on both ends.
        LanceRole role = board.getHex(from).hasPavementOrRoad() && board.getHex(into).hasPavementOrRoad()
              ? LanceRoles.effectiveRole(mover) : null;
        int cheapest = UNREACHABLE;
        for (Surface source : surfacesAt(mover, type, board, from)) {
            if (from.equals(mover.getPosition()) && source.hex().hasBridge()
                  && source.elevation() != mover.getElevation()) {
                continue;
            }
            for (Surface destination : destinations) {
                cheapest = Math.min(cheapest, stepCost(source, destination, limits, role != null && role.isConvoy(), 0));
            }
        }
        return cheapest;
    }

    /**
     * The rule Princess moves by: it never takes a path through a building that could not bear the unit's weight plus
     * a ten-ton margin ({@code PathRanker.willBuildingCollapse}). The route must not lead through one either, or it
     * points the unit where it will never step and the unit stands still: a 65-ton Longbow froze in front of a CF 15
     * building for two rounds (HammerGS's playtest, 2026-09-27).
     */
    static boolean wouldBringDownBuilding(Entity mover, Board board, Coords coords, int elevation) {
        if (passesUnderBridge(board, coords, elevation)) {
            return false;
        }
        // most hexes hold no building: look for one before asking anything of the unit
        IBuilding building = board.getBuildingAt(coords);
        if ((building == null) || mover.isAirborne() || mover.hasETypeFlag(Entity.ETYPE_VTOL)) {
            return false;
        }
        return (mover.getWeight() + COLLAPSE_MARGIN_TONS) > building.getCurrentCF(coords);
    }

    static boolean passesUnderBridge(Board board, Coords coords, int elevation) {
        Hex hex = board.getHex(coords);
        return hex != null && hex.hasBridge() && elevation < hex.terrainLevel(Terrains.BRIDGE_ELEV);
    }

    /**
     * @return the waypoint this field measures to, or {@code null} for a field to a board edge
     */
    @Nullable Coords getWaypoint() {
        return waypoint;
    }

    /**
     * @param position a hex on the board
     *
     * @return the movement points from that hex to the waypoint by the cheapest route, or {@link #UNREACHABLE}
     */
    int costFrom(Coords position) {
        return costToWaypoint.getOrDefault(position, UNREACHABLE);
    }

    /** Use the actual surface for the moving unit; hypothetical coordinates alone can refer to either surface. */
    int costFrom(Entity mover, Coords position) {
        return position.equals(mover.getPosition()) ? costFrom(position, mover.getElevation()) : costFrom(position);
    }

    int costFrom(Coords position, int elevation) {
        Map<Integer, Integer> bridgeCosts = bridgeCostsByElevation.get(position);
        return bridgeCosts == null ? costFrom(position) : bridgeCosts.getOrDefault(elevation, UNREACHABLE);
    }
}
