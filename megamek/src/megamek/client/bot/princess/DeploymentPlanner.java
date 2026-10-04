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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.force.Force;
import megamek.common.game.GameTurn;
import megamek.common.orders.EdgeOrder;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationShape;
import megamek.common.orders.LanceRole;
import megamek.common.orders.LanceRoles;
import megamek.common.orders.UnitOrders;
import megamek.common.pathfinder.MovementType;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Where a bot's units on orders deploy: only in hexes from which they can drive to where their lance is going, beside
 * their lance rather than across a river from it, at their formation slot beside a leader already down or at the anchor
 * the leader will take, and which unit deploys next so leaders go first. Part of {@link UnitOrdersFollower}.
 */
class DeploymentPlanner {

    private static final MMLogger LOGGER = MMLogger.create(DeploymentPlanner.class);

    private final Princess owner;
    private final UnitOrdersFollower follower;

    // where each formation deploys, picked by its first unit down while the leader has still to deploy; by leader id
    private final Map<Integer, Coords> deploymentAnchors = new HashMap<>();

    /**
     * Where a unit about to deploy has to be able to drive to: the next waypoint of the lance it moves with - its
     * formation's leader, or for an escort its convoy's front unit - else that unit's edge order, else a convoy's exit
     * edge.
     *
     * @param hex   the waypoint, or {@code null} for an edge
     * @param edge  the edge, or {@code null} for a waypoint
     * @param label how the log names it
     */
    private record DeploymentGoal(@Nullable Coords hex, @Nullable CardinalEdge edge, String label) {}

    /** How near a lance mate a unit has to be able to drive, so a mate in woods a truck cannot enter still counts. */
    private static final int LANCE_REACH_RADIUS = 2;

    /**
     * Part of the board a unit can drive anywhere in.
     *
     * @param field the unit's route field into the area, which reaches every hex of it
     * @param hexes the deployment hexes in it
     */
    private record DrivableArea(WaypointDistanceField field, List<Coords> hexes) {

        boolean reachesNear(Coords target) {
            for (Coords near : target.allAtDistanceOrLess(LANCE_REACH_RADIUS)) {
                if (field.costFrom(near) != WaypointDistanceField.UNREACHABLE) {
                    return true;
                }
            }
            return false;
        }

        boolean reachesNearAny(List<Entity> units) {
            for (Entity unit : units) {
                if (reachesNear(unit.getPosition())) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * @param owner    the bot whose units follow orders
     * @param follower the orders follower this works for
     */
    DeploymentPlanner(Princess owner, UnitOrdersFollower follower) {
        this.owner = owner;
        this.follower = follower;
    }

    private Optional<DeploymentGoal> deploymentGoal(Entity unit) {
        Entity guide = unit;
        LanceRole role = follower.roleOf(unit);
        if ((role != null) && role.isEscort()) {
            guide = follower.convoys().lead(role.getConvoyForceId()).orElse(unit);
        } else if (unit.getUnitOrders().getFormation().isPresent()) {
            Entity leader = owner.getGame().getEntity(unit.getUnitOrders().getFormation().get().getLeaderId());
            guide = (leader == null) ? unit : leader;
        }
        Optional<Coords> waypoint = guide.getUnitOrders().getNextWaypoint();
        if (waypoint.isPresent()) {
            return Optional.of(new DeploymentGoal(waypoint.get(), null, waypoint.get().getBoardNum()));
        }
        if (guide.getUnitOrders().getEdgeOrder() != EdgeOrder.NONE) {
            CardinalEdge edge = UnitOrdersFollower.toCardinalEdge(guide.getUnitOrders().getEdge());
            return Optional.of(new DeploymentGoal(null, edge, "the " + edge + " edge"));
        }
        LanceRole guideRole = follower.roleOf(guide);
        if ((guideRole != null) && guideRole.isConvoy()) {
            CardinalEdge edge = UnitOrdersFollower.toCardinalEdge(guideRole.getExitEdge());
            return Optional.of(new DeploymentGoal(null, edge, "the convoy's " + edge + " exit edge"));
        }
        return Optional.empty();
    }

    private int deploymentCost(Entity mover, DeploymentGoal goal, Coords hex) {
        return (goal.hex() != null) ? follower.distances().routeCost(mover, goal.hex(), hex, false, false)
              : follower.distances().edgeCostFrom(mover, goal.edge(), hex);
    }

    static boolean isFlying(Entity unit) {
        return unit.isAirborne() || (MovementType.getMovementType(unit) == MovementType.Flyer);
    }

    /**
     * Keeps a unit from deploying where it is blocked from where it is going: a wheeled truck on the far bank of a
     * river it cannot ford, or a unit walled in by buildings (HammerGS's playtest, 2026-10-03: a convoy's MASH truck
     * deployed across a river it could never cross). Only the hexes from which the unit can drive to its lance's next
     * waypoint or edge are kept; a unit leading a formation keeps only those every member can drive from, so the lance
     * forms round a hex all of it can leave. A unit with nowhere to go, or that flies, keeps every hex, and so does one
     * blocked from all of them.
     *
     * @param unit  a unit about to deploy
     * @param hexes the legal deployment hexes, in the bot's order
     *
     * @return the hexes it is not blocked from its goal in, in the same order
     */
    List<Coords> keepReachableGoal(Entity unit, List<Coords> hexes) {
        Optional<DeploymentGoal> goal = deploymentGoal(unit);
        if (goal.isEmpty() || isFlying(unit) || hexes.isEmpty()) {
            return hexes;
        }
        List<Entity> movers = new ArrayList<>();
        movers.add(unit);
        Optional<FormationOrder> formation = unit.getUnitOrders().getFormation();
        if (formation.isPresent() && (formation.get().getLeaderId() == unit.getId())) {
            for (Entity member : owner.getGame().getEntitiesVector()) {
                Optional<FormationOrder> memberFormation = member.getUnitOrders().getFormation();
                if ((member.getId() != unit.getId()) && memberFormation.isPresent()
                      && memberFormation.get().sharesLeader(unit.getId()) && !member.isDestroyed()
                      && !isFlying(member)) {
                    movers.add(member);
                }
            }
        }
        List<Coords> kept = new ArrayList<>();
        for (Coords hex : hexes) {
            if (canAllDriveFrom(movers, goal.get(), hex)) {
                kept.add(hex);
            }
        }
        if (kept.isEmpty()) {
            LOGGER.info("[BotOrders] DEPLOY_REACH {} (ID {}): blocked from {} in every legal hex; deploying as before",
                  unit.getDisplayName(), unit.getId(), goal.get().label());
            return hexes;
        }
        if (kept.size() < hexes.size()) {
            LOGGER.info("[BotOrders] DEPLOY_REACH {} (ID {}): {} of {} legal hexes are blocked from {}{}; kept {}",
                  unit.getDisplayName(), unit.getId(), hexes.size() - kept.size(), hexes.size(), goal.get().label(),
                  (movers.size() > 1) ? " for " + movers.size() + " units of its formation" : "", kept.size());
        }
        return kept;
    }

    /**
     * Keeps a unit from deploying where it is blocked from where it is going, or from its own lance. See
     * {@link #keepReachableGoal} for the first and {@link #keepWithLance} for the second.
     *
     * @param unit  a unit about to deploy
     * @param hexes the legal deployment hexes, in the bot's order
     *
     * @return the hexes it is blocked from neither in, in the same order
     */
    public List<Coords> keepReachable(Entity unit, List<Coords> hexes) {
        return keepWithLance(unit, keepReachableGoal(unit, hexes));
    }

    /**
     * Keeps a lance from deploying split across terrain one of its units cannot cross - a wheeled truck's lance
     * across a river - with or without orders (HammerGS's playtest, 2026-10-03: a convoy deployed with trucks on
     * both banks of a river they could never ford, so the lance could never come back together). A unit whose lance
     * has units on the board keeps only the hexes from which it can drive to one of them; the first of a lance down
     * keeps only the hexes every one of its lance mates can drive to, with room round them for the lance. Terrain
     * then picks among those as before, so a lance of units that can all cross deploys as it always did.
     *
     * @param unit  a unit about to deploy
     * @param hexes the deployment hexes left
     *
     * @return the hexes that keep the lance together, in the same order; all of them when none do
     */
    List<Coords> keepWithLance(Entity unit, List<Coords> hexes) {
        if (isFlying(unit) || hexes.isEmpty() || (unit.getForceId() == Force.NO_FORCE)) {
            return hexes;
        }
        List<Entity> mates = new ArrayList<>();
        List<Entity> matesOnBoard = new ArrayList<>();
        for (Entity mate : owner.getGame().getEntitiesVector()) {
            if ((mate.getId() == unit.getId()) || (mate.getForceId() != unit.getForceId())
                  || (mate.getOwnerId() != unit.getOwnerId()) || mate.isDestroyed() || isFlying(mate)) {
                continue;
            }
            mates.add(mate);
            if (mate.isDeployed() && (mate.getPosition() != null) && (mate.getBoardId() == unit.getBoardId())) {
                matesOnBoard.add(mate);
            }
        }
        if (mates.isEmpty()) {
            return hexes;
        }
        List<Coords> kept = new ArrayList<>();
        String rule;
        if (!matesOnBoard.isEmpty()) {
            rule = "can drive to none of its lance on the board";
            for (DrivableArea area : drivableAreas(unit, hexes)) {
                if (area.reachesNearAny(matesOnBoard)) {
                    kept.addAll(area.hexes());
                }
            }
        } else {
            rule = "cannot be driven to by all of its lance";
            Set<Coords> keptForAll = new HashSet<>(hexes);
            for (Entity mover : distinctMovers(mates)) {
                Set<Coords> keptForMover = new HashSet<>();
                for (DrivableArea area : drivableAreas(mover, hexes)) {
                    // the area has to hold the whole lance, or the others are left to deploy elsewhere
                    if (area.hexes().size() > mates.size()) {
                        for (Coords hex : hexes) {
                            if (area.reachesNear(hex)) {
                                keptForMover.add(hex);
                            }
                        }
                    }
                }
                keptForAll.retainAll(keptForMover);
            }
            for (Coords hex : hexes) {
                if (keptForAll.contains(hex)) {
                    kept.add(hex);
                }
            }
        }
        if (kept.isEmpty()) {
            LOGGER.info("[BotOrders] DEPLOY_REACH {} (ID {}): every legal hex {}; deploying as before",
                  unit.getDisplayName(), unit.getId(), rule);
            return hexes;
        }
        // keep the order the hexes came in
        List<Coords> ordered = new ArrayList<>();
        Set<Coords> keptSet = new HashSet<>(kept);
        for (Coords hex : hexes) {
            if (keptSet.contains(hex)) {
                ordered.add(hex);
            }
        }
        if (ordered.size() < hexes.size()) {
            LOGGER.info("[BotOrders] DEPLOY_REACH {} (ID {}): {} of {} legal hexes {}; kept {} so the lance deploys "
                        + "together", unit.getDisplayName(), unit.getId(), hexes.size() - ordered.size(), hexes.size(),
                  rule, ordered.size());
        }
        return ordered;
    }

    /**
     * @return one unit of each way of moving among the units: a field per way of moving does for all of them
     */
    private static List<Entity> distinctMovers(List<Entity> units) {
        List<Entity> movers = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Entity unit : units) {
            // weight counts too: a building that bears a light unit may not bear a heavy one
            String kind = MovementType.getMovementType(unit) + "|" + unit.getMaxElevationChange() + "|"
                  + (int) unit.getWeight();
            if (seen.add(kind)) {
                movers.add(unit);
            }
        }
        return movers;
    }

    /**
     * Splits deployment hexes into the parts of the board a unit can drive about in; a hex the unit cannot enter is
     * in none. One route field per part, so a river makes two.
     */
    private static List<DrivableArea> drivableAreas(Entity mover, List<Coords> hexes) {
        List<DrivableArea> areas = new ArrayList<>();
        Set<Coords> placed = new HashSet<>();
        for (Coords seed : hexes) {
            if (placed.contains(seed)) {
                continue;
            }
            placed.add(seed);
            WaypointDistanceField field = WaypointDistanceField.build(mover, seed);
            if (field.costFrom(seed) == WaypointDistanceField.UNREACHABLE) {
                continue;
            }
            List<Coords> areaHexes = new ArrayList<>();
            for (Coords hex : hexes) {
                if (field.costFrom(hex) != WaypointDistanceField.UNREACHABLE) {
                    areaHexes.add(hex);
                    placed.add(hex);
                }
            }
            areas.add(new DrivableArea(field, areaHexes));
        }
        return areas;
    }

    private boolean canAllDriveFrom(List<Entity> movers, DeploymentGoal goal, Coords hex) {
        for (Entity mover : movers) {
            if (deploymentCost(mover, goal, hex) == WaypointDistanceField.UNREACHABLE) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param entity a unit about to deploy
     *
     * @return its formation's leader, when the leader has still to deploy and no hex has been picked for it yet: the
     *       first of a formation to deploy picks the hex the whole formation forms round, whatever order the turns
     *       come in (HammerGS, 2026-10-03; with individual initiative each turn names one unit, so a member came
     *       before its leader and deployed on terrain alone)
     */
    public Optional<Entity> leaderStillToPlace(Entity entity) {
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        if (formation.isEmpty() || (formation.get().getLeaderId() == entity.getId())
              || deploymentAnchors.containsKey(formation.get().getLeaderId())) {
            return Optional.empty();
        }
        Entity leader = owner.getGame().getEntity(formation.get().getLeaderId());
        if ((leader == null) || leader.isDeployed() || leader.isDestroyed()) {
            return Optional.empty();
        }
        return Optional.of(leader);
    }

    /**
     * Notes the hex a formation forms round while its leader has still to deploy.
     *
     * @param leader   the formation's leader
     * @param hex      the hex picked for it
     * @param pickedBy the member that picked it
     */
    public void setDeploymentAnchor(Entity leader, Coords hex, Entity pickedBy) {
        deploymentAnchors.put(leader.getId(), hex);
        LOGGER.info("[BotOrders] DEPLOY_ANCHOR {} (ID {}): its formation forms round {}, picked by {} (ID {}), the "
              + "first of it to deploy", leader.getDisplayName(), leader.getId(), hex.getBoardNum(),
              pickedBy.getDisplayName(), pickedBy.getId());
    }

    /**
     * @param entity a unit about to deploy
     *
     * @return the hex picked for it as its formation's leader, by the formation's first unit to deploy; empty for a
     *       unit no hex was picked for
     */
    public Optional<Coords> getDeploymentAnchor(Entity entity) {
        if (entity.isDeployed()) {
            return Optional.empty();
        }
        return Optional.ofNullable(deploymentAnchors.get(entity.getId()));
    }

    /**
     * @return where the unit's formation leader stands, or the hex picked for it; {@code null} when neither is known
     */
    private @Nullable Coords formationDeploymentHex(Entity entity) {
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        if (formation.isEmpty()) {
            return null;
        }
        Entity leader = owner.getGame().getEntity(formation.get().getLeaderId());
        if ((leader != null) && leader.isDeployed() && (leader.getPosition() != null)) {
            return leader.getPosition();
        }
        return deploymentAnchors.get(formation.get().getLeaderId());
    }

    /**
     * The hex a formation member should deploy in: its slot beside the formation's leader, once the leader is on the
     * board. The slot comes from the member's place in the formation as set in the lobby, since an undeployed unit
     * has no position yet. The shape faces the leader's ordered facing when stopped, else its first waypoint, else
     * the middle of the enemy's deployment zone. Where the formation's own shape does not fit the deployment zone
     * around the leader, the member takes its place in a Line abreast instead, and the formation forms its shape on
     * the move.
     *
     * @param entity     a unit about to deploy
     * @param legalHexes the hexes the unit may deploy in
     *
     * @return the slot hex, or empty for a unit not in a formation, the leader itself, or a leader not yet deployed
     */
    public Optional<Coords> getDeploymentSlot(Entity entity, List<Coords> legalHexes) {
        Optional<FormationOrder> formation = follower.activeFormation(entity);
        if (formation.isEmpty() || (formation.get().getSlot() == 0)
              || (formation.get().getLeaderId() == entity.getId())) {
            return Optional.empty();
        }
        Entity leader = owner.getGame().getEntity(formation.get().getLeaderId());
        if ((leader == null) || (leader.getBoardId() != entity.getBoardId())) {
            return Optional.empty();
        }
        Coords leaderPosition;
        if (leader.isDeployed() && (leader.getPosition() != null)) {
            leaderPosition = leader.getPosition();
        } else if (deploymentAnchors.containsKey(leader.getId())) {
            // the leader has still to deploy: the slot is round the hex picked for it
            leaderPosition = deploymentAnchors.get(leader.getId());
        } else {
            return Optional.empty();
        }
        Board board = owner.getGame().getBoard(leader);
        if (board == null) {
            return Optional.empty();
        }
        int heading = deploymentHeading(leader, leaderPosition, deploymentFacingTarget(board), board);
        DeploymentFit fit = fittingDeployment(formation.get(), leaderPosition, heading, new HashSet<>(legalHexes),
              memberSlots(leader.getId())).orElse(new DeploymentFit(formation.get().getShape(), heading));
        return Optional.of(FormationPlanner.idealSlot(leaderPosition, fit.heading(), fit.shape(),
              formation.get().getSpacing(), formation.get().getSlot()));
    }

    /**
     * Puts the legal deployment hexes nearest a formation member's slot first, so the bot deploys the lance in its
     * shape. Hexes outside the deployment zone are never added; a slot outside it just draws the member as close as
     * the zone allows.
     *
     * @param entity               the unit about to deploy
     * @param possibleDeployCoords the legal deployment hexes, in the bot's own order
     *
     * @return the same hexes, nearest the slot first, or unchanged for a unit with no deployment slot
     */
    public List<Coords> preferDeploymentSlot(Entity entity, List<Coords> possibleDeployCoords) {
        Optional<Coords> slot = getDeploymentSlot(entity, possibleDeployCoords);
        if (slot.isEmpty()) {
            return possibleDeployCoords;
        }
        List<Coords> ordered = new ArrayList<>(possibleDeployCoords);
        ordered.sort(Comparator.comparingInt(coords -> coords.distance(slot.get())));
        Coords formationHex = formationDeploymentHex(entity);
        if ((formationHex != null) && !isFlying(entity)) {
            // the nearest hex to the slot can lie across water the unit cannot cross: only hexes it can drive to
            // its leader from count
            List<Coords> reachable = new ArrayList<>();
            for (Coords coords : ordered) {
                int cost = follower.distances().routeCost(entity, formationHex, coords, false, false);
                if (cost != WaypointDistanceField.UNREACHABLE) {
                    reachable.add(coords);
                }
            }
            if (!reachable.isEmpty()) {
                ordered = reachable;
            }
        }
        LOGGER.info("[BotOrders] {} (ID {}): deploying in formation, slot {}, nearest legal hex {}",
              entity.getDisplayName(), entity.getId(), slot.get().getBoardNum(),
              ordered.isEmpty() ? "none" : ordered.get(0).getBoardNum());
        return ordered;
    }

    /**
     * Keeps a formation leader's deployment hexes to those its formation fits around, so the members have room for
     * their slots. A shallow zone along a board edge rarely has room for a Vee or a Wedge, whose arms reach several
     * rows; then the leader takes a hex a Line abreast fits around, and the formation forms its shape on the move.
     * The bot still picks among the fitting hexes by terrain.
     *
     * @param entity               the unit about to deploy
     * @param possibleDeployCoords the legal deployment hexes, in the bot's own order
     *
     * @return the hexes the formation fits around, in the same order; unchanged for a unit that leads no formation,
     *       or when the formation fits nowhere
     */
    public List<Coords> preferFormationFit(Entity entity, List<Coords> possibleDeployCoords) {
        Optional<FormationOrder> formation = follower.activeFormation(entity);
        if (formation.isEmpty() || (formation.get().getLeaderId() != entity.getId())) {
            return possibleDeployCoords;
        }
        List<Integer> slots = memberSlots(entity.getId());
        Board board = owner.getGame().getBoard(entity);
        if (slots.isEmpty() || (board == null)) {
            return possibleDeployCoords;
        }
        Set<Coords> legalHexes = new HashSet<>(possibleDeployCoords);
        Coords facingTarget = deploymentFacingTarget(board);
        Map<Coords, DeploymentFit> fits = new HashMap<>();
        for (Coords candidate : possibleDeployCoords) {
            fittingDeployment(formation.get(), candidate, deploymentHeading(entity, candidate, facingTarget, board),
                  legalHexes, slots).ifPresent(fit -> fits.put(candidate, fit));
        }
        for (FormationShape shape : List.of(formation.get().getShape(), FormationShape.LINE)) {
            List<Coords> fitting = new ArrayList<>();
            for (Coords candidate : possibleDeployCoords) {
                DeploymentFit fit = fits.get(candidate);
                if ((fit != null) && (fit.shape() == shape)) {
                    fitting.add(candidate);
                }
            }
            if (!fitting.isEmpty()) {
                LOGGER.info("[BotOrders] {} (ID {}): deploying to lead a {} ({} slots) - {} of {} legal hexes fit it{}",
                      entity.getDisplayName(), entity.getId(), shape, slots.size(), fitting.size(),
                      possibleDeployCoords.size(), (shape == formation.get().getShape()) ? ""
                            : "; the zone is too shallow for a " + formation.get().getShape()
                                  + ", so the formation forms it on the move");
                return fitting;
            }
        }
        LOGGER.info("[BotOrders] {} (ID {}): no legal hex fits a {} or a Line; deploying on terrain alone",
              entity.getDisplayName(), entity.getId(), formation.get().getShape());
        return possibleDeployCoords;
    }

    /**
     * The way a formation faces while it deploys: the leader's ordered facing when stopped, as set in the lobby; else
     * a convoy's exit edge; else toward its first waypoint; else toward the facing target - the middle of the enemy's
     * deployment zone, the way the bot faces the units it deploys.
     */
    private static int deploymentHeading(Entity leader, Coords leaderPosition, Coords facingTarget, Board board) {
        int orderedFacing = leader.getUnitOrders().getFacingWhenStopped();
        if (orderedFacing != UnitOrders.FACING_AUTO) {
            return orderedFacing;
        }
        OptionalInt convoyFacing = ConvoyTracker.exitFacing(leader, leaderPosition, board);
        if (convoyFacing.isPresent()) {
            return convoyFacing.getAsInt();
        }
        Optional<Coords> waypoint = leader.getUnitOrders().getNextWaypoint();
        if (waypoint.isPresent() && !waypoint.get().equals(leaderPosition)) {
            return leaderPosition.direction(waypoint.get());
        }
        return facingTarget.equals(leaderPosition) ? leader.getFacing() : leaderPosition.direction(facingTarget);
    }

    /**
     * @return the middle of the enemy's deployment zone, or the middle of the board when no enemy zone is known
     */
    private Coords deploymentFacingTarget(Board board) {
        return owner.getEnemyDeploymentCenter(board)
              .orElse(new Coords(board.getWidth() / 2, board.getHeight() / 2));
    }

    /**
     * The shape and the line a formation deploys in.
     *
     * @param shape   the formation's own shape, or a Line where it fits no way
     * @param heading the way the shape is laid out, 0-5; the units still face as they are told
     */
    private record DeploymentFit(FormationShape shape, int heading) {}

    /** Turns from the wanted heading, nearest first, that a shape is tried at. */
    private static final int[] TURNS_NEAREST_FIRST = { 0, 1, -1, 2, -2, 3 };

    /**
     * How a formation deploys round its leader so that every member's slot is a legal deployment hex: in its own shape
     * along the wanted heading; else in its own shape turned, the nearest turn first, so a Column set in the lobby
     * deploys as a column along a zone too shallow for it to face the enemy (HammerGS, 2026-10-04: a convoy's Column
     * deployed in a line abreast); else a Line along the wanted heading.
     *
     * @return the fit, or empty when neither fits
     */
    private static Optional<DeploymentFit> fittingDeployment(FormationOrder formation, Coords leaderPosition,
          int heading, Set<Coords> legalHexes, List<Integer> slots) {
        for (int turn : TURNS_NEAREST_FIRST) {
            int tried = (heading + turn + 6) % 6;
            if (fits(formation.getShape(), formation, leaderPosition, tried, legalHexes, slots)) {
                return Optional.of(new DeploymentFit(formation.getShape(), tried));
            }
        }
        if (fits(FormationShape.LINE, formation, leaderPosition, heading, legalHexes, slots)) {
            return Optional.of(new DeploymentFit(FormationShape.LINE, heading));
        }
        return Optional.empty();
    }

    private static boolean fits(FormationShape shape, FormationOrder formation, Coords leaderPosition, int heading,
          Set<Coords> legalHexes, List<Integer> slots) {
        for (int slot : slots) {
            Coords slotHex = FormationPlanner.idealSlot(leaderPosition, heading, shape, formation.getSpacing(), slot);
            if (!legalHexes.contains(slotHex)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the slots of a formation's units other than its leader, deployed or not
     */
    private List<Integer> memberSlots(int leaderId) {
        List<Integer> slots = new ArrayList<>();
        for (Entity unit : owner.getGame().getEntitiesVector()) {
            Optional<FormationOrder> unitFormation = unit.getUnitOrders().getFormation();
            if (unitFormation.isPresent() && unitFormation.get().sharesLeader(leaderId)
                  && (unitFormation.get().getSlot() != 0)) {
                slots.add(unitFormation.get().getSlot());
            }
        }
        return slots;
    }

    /**
     * Picks which unit to deploy this turn so a formation's leader goes down before its members, which then deploy
     * in their slots around it. A member due to deploy is swapped for its leader when the leader may deploy this
     * turn too.
     *
     * @param firstDeployable the unit the game would deploy next
     * @param turn            the bot's deployment turn, or {@code null} when the game has none for it
     *
     * @return the unit to deploy
     */
    public int chooseUnitToDeploy(int firstDeployable, @Nullable GameTurn turn) {
        Entity unit = owner.getGame().getEntity(firstDeployable);
        if ((unit == null) || (turn == null)) {
            return firstDeployable;
        }
        Optional<FormationOrder> formation = unit.getUnitOrders().getFormation();
        if (formation.isEmpty() || (formation.get().getLeaderId() == unit.getId())) {
            return firstDeployable;
        }
        Entity leader = owner.getGame().getEntity(formation.get().getLeaderId());
        if ((leader != null) && !leader.isDeployed() && turn.isValidEntity(leader, owner.getGame())
              && leader.shouldDeploy(owner.getGame().getRoundCount())) {
            LOGGER.info("[BotOrders] deploying formation leader {} (ID {}) before {} (ID {})",
                  leader.getDisplayName(), leader.getId(), unit.getDisplayName(), unit.getId());
            return leader.getId();
        }
        return firstDeployable;
    }

    /**
     * Where a unit deploys when its orders decide it, else what is left to rank by terrain.
     *
     * @param hex        the hex to deploy on, or {@code null} when the orders leave it to the bot
     * @param candidates the hexes left, the ones its formation fits first
     */
    record OrderedDeployment(@Nullable Coords hex, List<Coords> candidates) {}

    /**
     * The hex a unit on orders deploys on: a formation leader on the hex its formation's first unit down picked for
     * it, or the free one nearest; a member in its slot beside its leader; an escort at its place round its convoy. A
     * member first down while its leader has still to deploy picks the leader's hex first, so it can take its slot.
     *
     * @param unit        the unit about to deploy
     * @param candidates  the hexes it may deploy on, reachable and ordered by the bot
     * @param firstValid  the bot's pick of the first hex of a list the unit may really deploy on, or {@code null}
     * @param pickAnchor  picks the hex a leader still to deploy will take, as it would on its own turn; given the
     *                    leader and the unit picking for it
     *
     * @return the hex, or none and the candidates its formation fits first
     */
    OrderedDeployment orderedDeployment(Entity unit, List<Coords> candidates,
          BiFunction<Entity, List<Coords>, Coords> firstValid, BiConsumer<Entity, Entity> pickAnchor) {
        Optional<Coords> anchor = getDeploymentAnchor(unit);
        if (anchor.isPresent()) {
            List<Coords> nearestAnchor = new ArrayList<>(candidates);
            nearestAnchor.sort(Comparator.comparingInt(coords -> coords.distance(anchor.get())));
            Coords anchorHex = firstValid.apply(unit, nearestAnchor);
            if (anchorHex != null) {
                LOGGER.info("[BotOrders] {} (ID {}): deploying at {}, the hex picked for it as leader ({})",
                      unit.getDisplayName(), unit.getId(), anchorHex.getBoardNum(), anchor.get().getBoardNum());
                return new OrderedDeployment(anchorHex, candidates);
            }
        }
        Optional<Entity> leaderToPlace = leaderStillToPlace(unit);
        if (leaderToPlace.isPresent()) {
            pickAnchor.accept(leaderToPlace.get(), unit);
        }
        List<Coords> fitting = preferFormationFit(unit, candidates);
        if (getDeploymentSlot(unit, fitting).isPresent()) {
            // the free hex nearest the slot: ranking the hexes by terrain moved members out of the shape
            // (HammerGS's playtest, 2026-09-26)
            Coords slotHex = firstValid.apply(unit, preferDeploymentSlot(unit, fitting));
            if (slotHex != null) {
                return new OrderedDeployment(slotHex, fitting);
            }
        }
        Optional<Coords> escortPlace = follower.convoyEscorts().getEscortDeploymentPlace(unit);
        if (escortPlace.isPresent()) {
            List<Coords> nearestFirst = new ArrayList<>(fitting);
            nearestFirst.sort(Comparator.comparingInt(coords -> coords.distance(escortPlace.get())));
            Coords escortHex = firstValid.apply(unit, nearestFirst);
            if (escortHex != null) {
                return new OrderedDeployment(escortHex, fitting);
            }
        }
        return new OrderedDeployment(null, fitting);
    }

    /**
     * The way a unit faces as it deploys: the facing a player ordered for when it is stopped, else its convoy's way to
     * its exit edge, else toward the enemy's deployment zone, where the enemy will come from.
     *
     * @param unit            the unit deploying
     * @param hex             the hex it deploys on
     * @param board           its board
     * @param enemyZoneCenter the middle of the enemy's deployment zone, or empty to leave that to the bot
     *
     * @return the facing 0-5, or {@link UnitOrders#FACING_AUTO} to leave it to the bot
     */
    int deploymentFacing(Entity unit, Coords hex, Board board, Optional<Coords> enemyZoneCenter) {
        int facing = unit.getUnitOrders().getFacingWhenStopped();
        OptionalInt convoyFacing = ConvoyTracker.exitFacing(unit, hex, board);
        if (facing != UnitOrders.FACING_AUTO) {
            LOGGER.info("[Deployment] {} deploys at {} facing {}, as ordered", unit.getDisplayName(),
                  hex.getBoardNum(), facing);
        } else if (convoyFacing.isPresent()) {
            facing = convoyFacing.getAsInt();
            LOGGER.info("[Deployment] {} deploys at {} facing {}, toward its convoy's exit edge {}",
                  unit.getDisplayName(), hex.getBoardNum(), facing, LanceRoles.effectiveRole(unit).getExitEdge());
        } else if (enemyZoneCenter.isPresent() && !enemyZoneCenter.get().equals(hex)) {
            facing = hex.direction(enemyZoneCenter.get());
            LOGGER.info("[Deployment] {} deploys at {} facing {}, toward the enemy deployment zone around {}",
                  unit.getDisplayName(), hex.getBoardNum(), facing, enemyZoneCenter.get().getBoardNum());
        }
        return facing;
    }
}
