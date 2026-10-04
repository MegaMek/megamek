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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import megamek.common.OffBoardDirection;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.UnitOrders;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * How a formation moves as one (HammerGS, 2026-09-27 to 2026-10-01): assembling at its first waypoint, its leader
 * waiting at a flag until the lance has formed up, holds and assembly points, every unit kept to the formation's pace
 * and the last unit kept in reach, no move that brings a building down, and the lance leaving the board together at the
 * end of its route. Part of {@link UnitOrdersFollower}.
 */
class FormationMarch {

    private static final MMLogger LOGGER = MMLogger.create(FormationMarch.class);

    private final Princess owner;
    private final UnitOrdersFollower follower;

    /** A formation that keeps together counts as formed when every unit is this close to its slot. */
    static final int REFORM_SLACK = 2;

    /**
     * The most rounds a leader waits at a waypoint for its formation, however far the last unit has to come, so one
     * stuck unit cannot hold the rest.
     */
    static final int MAXIMUM_REFORM_WAIT_ROUNDS = 6;

    /**
     * A formation leader waiting at a waypoint for its formation to form up.
     *
     * @param waypoint   the waypoint it waits at
     * @param sinceRound the round it first waited there
     * @param maxRounds  the rounds to wait at most: until the last unit should have arrived, by its path and speed
     */
    private record ReformWait(Coords waypoint, int sinceRound, int maxRounds) {}

    /** The leaders waiting at a waypoint for their formation, by unit id; the bot's own bookkeeping, not saved. */
    private final Map<Integer, ReformWait> reformWaits = new HashMap<>();

    /** The leaders holding at a waypoint until their formation assembles, by unit id; not saved. */
    private final Map<Integer, ReformWait> assemblyWaits = new HashMap<>();

    /**
     * The formations that have formed up since their route was given, by the formation's leader id; not saved. Until
     * then the lance is assembling: deployment is usually done before the move order, so the lance starts scattered and
     * its first waypoint is where it assembles (HammerGS, 2026-10-01).
     */
    private final Set<Integer> assembledFormations = new HashSet<>();

    /**
     * The most rounds a lance assembling at its first waypoint waits for its last unit; a stuck unit drops out sooner.
     */
    static final int MAXIMUM_ASSEMBLY_WAIT_ROUNDS = 12;

    /**
     * @param owner    the bot whose units follow orders
     * @param follower the orders follower this works for
     */
    FormationMarch(Princess owner, UnitOrdersFollower follower) {
        this.owner = owner;
        this.follower = follower;
    }

    private int currentRound() {
        return follower.currentRound();
    }

    /**
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit leads a formation that keeps together and is waiting at a waypoint for the
     *       others to form up
     */
    public boolean isWaitingForFormation(Entity entity) {
        ReformWait wait = reformWaits.get(entity.getId());
        Optional<Coords> waypoint = entity.getUnitOrders().getNextWaypoint();
        return (wait != null) && waypoint.isPresent() && wait.waypoint().equals(waypoint.get());
    }

    /**
     * Decides, once a formation leader that keeps together, or that changes shape at this waypoint, has reached a
     * waypoint part-way along its route, whether it waits there for its formation: until every other unit is within
     * {@link #REFORM_SLACK} of its slot, for at most {@link #MAXIMUM_REFORM_WAIT_ROUNDS} rounds. Then the formation
     * moves on to the next waypoint together.
     *
     * @return {@code true} to wait another round
     */
    boolean shouldWaitForFormation(Entity leader, Coords waypoint) {
        Optional<FormationOrder> formation = follower.activeFormation(leader);
        // a formation that changes shape at this waypoint re-forms here before moving on, kept together or not
        boolean isReformingHere = leader.getUnitOrders().hasRoute()
              && (leader.getUnitOrders().getWaypointOrder(0).getArrivalFormation() != null);
        boolean isAssembling = isAssembling(leader);
        if (formation.isEmpty() || (!formation.get().isKeepTogether() && !isReformingHere && !isAssembling)) {
            return false;
        }
        // one kept together stops to re-form only where it matters: at the end of the route, and where its shape
        // changes. Stopping at every waypoint along the way cost 12 of 16 rounds on a route with waypoints a few hexes
        // apart; between the stops the leader keeps pace with the last unit instead (HammerGS, 2026-09-27)
        List<Coords> route = leader.getUnitOrders().getRoute();
        boolean isRouteEnd = route.size() <= 1;
        if (!isRouteEnd && !isReformingHere && !changesShapeAfter(leader.getUnitOrders())) {
            String reasonToStop = isAssembling ? "assembling at the first waypoint since the order"
                  : reasonToFormUpHere(leader, formation.get(), waypoint, route);
            if (reasonToStop == null) {
                LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_PASS at {} - same shape on the next leg and "
                            + "the lance is together; not stopping to re-form", leader.getDisplayName(),
                      leader.getId(), currentRound(), waypoint.getBoardNum());
                reformWaits.remove(leader.getId());
                return false;
            }
            if (!reformWaits.containsKey(leader.getId())) {
                LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_STOP at {} - {}; forming up before moving on",
                      leader.getDisplayName(), leader.getId(), currentRound(), waypoint.getBoardNum(), reasonToStop);
            }
        }
        List<Entity> members = follower.roster().formationMembers(leader, formation.get().getLeaderId());
        boolean isLeading = (members.size() >= 2) && (members.get(0).getId() == leader.getId());
        boolean isBroken = FireReaction.isBrokenToFight(leader);
        if (!isLeading || isBroken) {
            reformWaits.remove(leader.getId());
            return false;
        }
        int outOfPlace = countOutOfPlace(members);
        ReformWait wait = reformWaits.get(leader.getId());
        if ((wait == null) || !wait.waypoint().equals(waypoint)) {
            wait = new ReformWait(waypoint, currentRound(), assemblyWaitRounds(leader, waypoint,
                  isAssembling ? MAXIMUM_ASSEMBLY_WAIT_ROUNDS : MAXIMUM_REFORM_WAIT_ROUNDS));
        }
        int roundsWaited = currentRound() - wait.sinceRound();
        if (outOfPlace == 0) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_FORMED at {} - moving on together{}",
                  leader.getDisplayName(), leader.getId(), currentRound(), waypoint.getBoardNum(),
                  isAssembling ? "; assembled, now marching" : "");
            reformWaits.remove(leader.getId());
            noteAssembled(formation.get().getLeaderId());
            if (route.size() > 1) {
                // at the end of the route the exit call says it all
                owner.getOrdersRadio().report(leader, OrdersRadio.RadioEvent.FORMED,
                      follower.navLabel(leader, waypoint), follower.navLabel(leader, route.get(1)));
            }
            return false;
        }
        if (roundsWaited >= wait.maxRounds()) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_WAIT_OVER at {} - {} unit(s) still out of place "
                        + "after {} round(s); moving on", leader.getDisplayName(), leader.getId(), currentRound(),
                  waypoint.getBoardNum(), outOfPlace, roundsWaited);
            reformWaits.remove(leader.getId());
            noteAssembled(formation.get().getLeaderId());
            return false;
        }
        LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_WAIT at {} - {} of {} unit(s) still forming up",
              leader.getDisplayName(), leader.getId(), currentRound(), waypoint.getBoardNum(), outOfPlace,
              members.size() - 1);
        reformWaits.put(leader.getId(), wait);
        return true;
    }

    /**
     * Why a lance keeping together should stop to form up at a flag where its shape does not change, or {@code null}
     * to pass on. It stops where it has come apart - a unit more than a turn's move from its place - and before a leg
     * through a town, as a unit assembles before going through the doors (HammerGS's playtest, 2026-10-01: a lance
     * that never formed up passed its first flag and went into the town strung out over half the map). A lance that
     * is together still passes through.
     */
    private @Nullable String reasonToFormUpHere(Entity leader, FormationOrder formation, Coords flag,
          List<Coords> route) {
        Board board = owner.getGame().getBoard(leader);
        if ((board != null) && (route.size() > 1) && (formation.getShape() != FormationShape.COLUMN)) {
            Coords nextFlag = route.get(1);
            int townHexes = TownLegPlanner.hexesBesideBuildings(board, flag, nextFlag,
                  hex -> follower.distances().routeCostFrom(leader, nextFlag, hex));
            if (townHexes >= TownLegPlanner.TOWN_HEXES) {
                return "the next leg runs through a town (" + townHexes + " hexes in or beside buildings)";
            }
        }
        for (Entity member : follower.roster().formationMembers(leader, formation.getLeaderId())) {
            if ((member.getId() == leader.getId()) || follower.roster().isFallingBehind(member)) {
                continue;
            }
            Optional<Coords> slot = follower.slots().getFormationSlot(member);
            if (slot.isEmpty() || (member.getPosition() == null)) {
                continue;
            }
            int cost = follower.distances().routeCostFrom(member, slot.get(), member.getPosition());
            int turnsMove = Math.max(1, paceMovementPoints(member, formation.getPace()));
            if ((cost != WaypointDistanceField.UNREACHABLE) && (cost > turnsMove)) {
                return member.getShortName() + " is " + cost + " MP from its place at " + slot.get().getBoardNum();
            }
        }
        return null;
    }

    /**
     * @return {@code true} if the unit is more than a turn's walk from its place in its formation
     */
    private boolean isLagging(Entity entity) {
        Optional<Coords> slot = follower.slots().getFormationSlot(entity);
        if (slot.isEmpty() || (entity.getPosition() == null) || entity.getPosition().equals(slot.get())) {
            return false;
        }
        int cost = follower.distances().routeCostFrom(entity, slot.get(), entity.getPosition());
        if (cost == WaypointDistanceField.UNREACHABLE) {
            cost = entity.getPosition().distance(slot.get());
        }
        return cost > entity.getWalkMP();
    }

    /**
     * Ends a leader's wait at a flag for its formation, if it was waiting.
     *
     * @param entity a unit of the bot
     *
     * @return {@code true} if it was waiting
     */
    boolean endReformWait(Entity entity) {
        return reformWaits.remove(entity.getId()) != null;
    }

    /**
     * Has the unit's formation assemble again before it marches, after a changed order.
     *
     * @param entity a unit of the bot
     */
    void assembleAgain(Entity entity) {
        entity.getUnitOrders().getFormation().ifPresent(order -> assembledFormations.remove(order.getLeaderId()));
    }

    /**
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit's formation has not yet formed up since its route was given, so it is assembling
     *       at its first waypoint rather than marching
     */
    boolean isAssembling(Entity entity) {
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        return formation.isPresent() && !assembledFormations.contains(formation.get().getLeaderId());
    }

    /**
     * Marks a formation as formed up: from here on it marches, keeping its places beside its leader.
     *
     * @param formationId the formation's leader id
     */
    void noteAssembled(int formationId) {
        assembledFormations.add(formationId);
    }

    /**
     * @return {@code true} if the leg after the next waypoint is set to travel in a different formation from the leg
     *       ending there, so the lance changes shape at the waypoint
     */
    private static boolean changesShapeAfter(UnitOrders orders) {
        if (orders.getRoute().size() < 2) {
            return false;
        }
        return !Objects.equals(orders.getWaypointOrder(0).getFormation(), orders.getWaypointOrder(1).getFormation());
    }

    /**
     * How many turns until the last of a formation's other units reaches its slot: the movement points of the way it
     * can really go there, over the movement points it spends a turn at the formation's pace. A unit that cannot reach
     * its slot at all is not waited for.
     *
     * @param leader the formation's leader
     *
     * @return the turns until the last one arrives; 0 when all are in place
     */
    int estimatedAssemblyTurns(Entity leader) {
        Optional<FormationOrder> formation = follower.activeFormation(leader);
        if (formation.isEmpty()) {
            return 0;
        }
        List<Entity> members = follower.roster().formationMembers(leader, formation.get().getLeaderId());
        int longest = 0;
        for (Entity member : members.subList(Math.min(1, members.size()), members.size())) {
            Optional<Coords> slot = follower.slots().getFormationSlot(member);
            if (slot.isEmpty() || member.getPosition().equals(slot.get())
                  || follower.roster().isFallingBehind(member)) {
                continue;
            }
            int cost = follower.distances().routeCostFrom(member, slot.get(), member.getPosition());
            if (cost == WaypointDistanceField.UNREACHABLE) {
                continue;
            }
            int perTurn = Math.max(1, paceMovementPoints(member, formation.get().getPace()));
            int turns = (cost + perTurn - 1) / perTurn;
            LOGGER.info("[BotOrders] {} (ID {}) round {}: ASSEMBLY_ESTIMATE - {} is {} MP from its slot at {}, {} MP a "
                        + "turn: {} turn(s)", leader.getDisplayName(), leader.getId(), currentRound(),
                  member.getDisplayName(), cost, slot.get().getBoardNum(), perTurn, turns);
            longest = Math.max(longest, turns);
        }
        return longest;
    }

    /**
     * @return the rounds to wait at a waypoint for the formation: until the last unit should have arrived, plus one
     *       for luck, at least one and at most the cap
     */
    private int assemblyWaitRounds(Entity leader, Coords waypoint, int cap) {
        int rounds = Math.clamp(estimatedAssemblyTurns(leader) + 1, 1, Math.max(1, cap));
        LOGGER.info("[BotOrders] {} (ID {}) round {}: waits at {} up to {} round(s) for the formation (at most {})",
              leader.getDisplayName(), leader.getId(), currentRound(), waypoint.getBoardNum(), rounds, cap);
        return rounds;
    }

    /**
     * @return {@code true} if a unit waiting at a waypoint for its formation has waited as long as the last unit should
     *       have needed to arrive
     */
    private boolean isAssemblyWaitOver(Entity entity, Coords waypoint) {
        ReformWait wait = assemblyWaits.get(entity.getId());
        return (wait != null) && wait.waypoint().equals(waypoint)
              && (currentRound() - wait.sinceRound() >= wait.maxRounds());
    }

    /**
     * @param members a formation's units, its leader first
     *
     * @return how many of the others are more than {@link #REFORM_SLACK} hexes from their slots
     */
    private int countOutOfPlace(List<Entity> members) {
        int outOfPlace = 0;
        for (Entity member : members.subList(1, members.size())) {
            Optional<Coords> slot = follower.slots().getFormationSlot(member);
            if (slot.isPresent() && (member.getPosition().distance(slot.get()) > REFORM_SLACK)
                  && !follower.roster().isFallingBehind(member)) {
                outOfPlace++;
            }
        }
        return outOfPlace;
    }

    /**
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit's formation has assembled: every other unit within {@link #REFORM_SLACK} of
     *       its slot; a unit out of formation, or leading no one, counts as assembled
     */
    boolean isFormationAssembled(Entity entity) {
        Optional<FormationOrder> formation = follower.activeFormation(entity);
        if (formation.isEmpty()) {
            return true;
        }
        List<Entity> members = follower.roster().formationMembers(entity, formation.get().getLeaderId());
        return (members.size() < 2) || (countOutOfPlace(members) == 0);
    }

    /**
     * Orders a unit off the board by the edge nearest the last waypoint of its route, and with it every unit of the
     * formation it leads.
     */
    void exitWithFormation(Entity entity, Coords lastWaypoint) {
        // a convoy's exit hex sits on its exit edge, but a corner is as near another; it leaves by its own
        OffBoardDirection edge = follower.isConvoy(entity) ? follower.roleOf(entity).getExitEdge()
              : nearestEdge(entity, lastWaypoint);
        List<Entity> leaving = new ArrayList<>(List.of(entity));
        Optional<FormationOrder> formation = follower.activeFormation(entity);
        if (formation.isPresent()) {
            List<Entity> members = follower.roster().formationMembers(entity, formation.get().getLeaderId());
            if (!members.isEmpty() && (members.get(0).getId() == entity.getId())) {
                leaving = members;
            }
        }
        for (Entity unit : leaving) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: end of route at {} - leaving by the {} edge",
                  unit.getDisplayName(), unit.getId(), currentRound(), lastWaypoint.getBoardNum(), edge);
            follower.orderExit(unit, edge);
        }
        owner.getOrdersRadio().report(entity, OrdersRadio.RadioEvent.EXITING, edge.name().toLowerCase(Locale.ROOT));
    }

    /**
     * @return the board edge nearest the hex; ties go north, south, west, then east
     */
    private OffBoardDirection nearestEdge(Entity entity, Coords hex) {
        Board board = owner.getGame().getBoard(entity);
        if (board == null) {
            return OffBoardDirection.NORTH;
        }
        int toNorth = hex.getY();
        int toSouth = board.getHeight() - 1 - hex.getY();
        int toWest = hex.getX();
        int toEast = board.getWidth() - 1 - hex.getX();
        int nearest = Math.min(Math.min(toNorth, toSouth), Math.min(toWest, toEast));
        if (toNorth == nearest) {
            return OffBoardDirection.NORTH;
        } else if (toSouth == nearest) {
            return OffBoardDirection.SOUTH;
        } else if (toWest == nearest) {
            return OffBoardDirection.WEST;
        }
        return OffBoardDirection.EAST;
    }

    /**
     * Starts a unit's hold when it stands on a waypoint set to hold, and moves it on once it has held for the turns
     * set: reaching a two-turn hold in round 3, it holds in rounds 4 and 5 and moves on in round 6.
     */
    void advanceHold(Entity entity, Coords waypoint) {
        UnitOrders orders = entity.getUnitOrders();
        int holdTurns = orders.getWaypointOrder(0).getHoldTurns();
        boolean isAssemble = orders.getWaypointOrder(0).isAssemble();
        if (orders.getHoldSinceRound() == UnitOrders.NO_ROUND) {
            if (entity.getPosition().equals(waypoint) && isAssemble && isFormationAssembled(entity)) {
                LOGGER.info("[BotOrders] {} (ID {}) round {}: reached {} with the formation assembled; moving on",
                      entity.getDisplayName(), entity.getId(), currentRound(), waypoint.getBoardNum());
                follower.change(entity, UnitOrderAction.REACHED);
            } else if (entity.getPosition().equals(waypoint)) {
                if (isAssemble) {
                    // wait until the last unit should have arrived, by its path and speed, never past the turns set
                    assemblyWaits.put(entity.getId(), new ReformWait(waypoint, currentRound(),
                          assemblyWaitRounds(entity, waypoint, holdTurns)));
                }
                LOGGER.info("[BotOrders] {} (ID {}) round {}: reached {} and holds {} turn(s), through round {}",
                      entity.getDisplayName(), entity.getId(), currentRound(), waypoint.getBoardNum(), holdTurns,
                      currentRound() + holdTurns);
                follower.change(entity, UnitOrderAction.HOLD_STARTED);
                owner.getOrdersRadio().report(entity, OrdersRadio.RadioEvent.HOLDING,
                      follower.navLabel(entity, waypoint));
            }
            return;
        }
        if (orders.isHoldDone(currentRound())) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: held {} turn(s) at {}; moving on", entity.getDisplayName(),
                  entity.getId(), currentRound(), holdTurns, waypoint.getBoardNum());
            follower.change(entity, UnitOrderAction.REACHED);
        } else if (isAssemble && isAssemblyWaitOver(entity, waypoint)) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: the formation should have assembled at {} by now; moving on",
                  entity.getDisplayName(), entity.getId(), currentRound(), waypoint.getBoardNum());
            assemblyWaits.remove(entity.getId());
            follower.change(entity, UnitOrderAction.REACHED);
        } else if (isAssemble && isFormationAssembled(entity)) {
            assemblyWaits.remove(entity.getId());
            LOGGER.info("[BotOrders] {} (ID {}) round {}: formation assembled at {}; moving on before the {} turn(s) "
                  + "were up", entity.getDisplayName(), entity.getId(), currentRound(), waypoint.getBoardNum(),
                  holdTurns);
            follower.change(entity, UnitOrderAction.REACHED);
        }
    }

    /**
     * Keeps each unit in a formation to the formation's pace, which sets how its units move, not how fast: at a Walk
     * pace every unit, the leader included, may use up to its own walking movement points, and at a Run pace up to
     * its own running movement points. A jump within those movement points is allowed. A formation that has broken on
     * contact is not paced, and a unit with no move within the pace keeps every move.
     *
     * <p>A formation that keeps together also holds its leader to the slowest unit's speed, so the others can keep
     * their slots; without it, a Grasshopper leading a Longbow reached each waypoint in a turn or two and the Longbow,
     * chasing slots around ever-further waypoints, fell hopelessly behind (HammerGS's playtest, 2026-09-26). A
     * formation that does not keep together lets each unit move at its own speed, so units form up sooner.</p>
     *
     * @param entity the unit about to move
     * @param paths  its candidate moves
     *
     * @return the moves within the formation's pace, or all of them when the unit is in no formation
     */
    List<MovePath> limitToFormationPace(Entity entity, List<MovePath> paths) {
        Optional<FormationOrder> formation = follower.activeFormation(entity);
        if (formation.isEmpty()) {
            return paths;
        }
        List<Entity> members = follower.roster().formationMembers(entity, formation.get().getLeaderId());
        if ((members.size() < 2) || !members.contains(entity)) {
            return paths;
        }
        Entity leader = members.get(0);
        if (FireReaction.isBrokenToFight(leader)) {
            LOGGER.debug("[BotOrders] {} (ID {}) round {}: the lance is fighting - not paced",
                  entity.getDisplayName(), entity.getId(), currentRound());
            return paths;
        }
        int paceLimit = paceMovementPoints(entity, formation.get().getPace());
        boolean isTownLeg = follower.townLegs().isOnTownLeg(entity);
        boolean isHeldToSlowest = formation.get().isKeepTogether() && (leader.getId() == entity.getId())
              && !isTownLeg && !isAssembling(entity);
        if ((formation.get().getPace() == FormationPace.WALK) && !isHeldToSlowest && isLagging(entity)) {
            // at a walk, a unit more than a turn's walk from its place may run or jump to catch up (HammerGS,
            // 2026-10-01); one in its place, or nearly, keeps to the walk
            paceLimit = Math.max(entity.getRunMP(), entity.getJumpMP());
            LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_CATCH_UP - more than a turn's walk from its place; "
                        + "may run or jump, up to {} MP", entity.getDisplayName(), entity.getId(), currentRound(),
                  paceLimit);
        }
        if (isHeldToSlowest) {
            // a formation keeping together advances no faster than its slowest unit can follow
            for (Entity member : members) {
                if (!follower.roster().isFallingBehind(member)) {
                    paceLimit = Math.min(paceLimit, paceMovementPoints(member, formation.get().getPace()));
                }
            }
        }
        List<MovePath> pacedPaths = new ArrayList<>();
        for (MovePath path : paths) {
            if (path.getMpUsed() <= paceLimit) {
                pacedPaths.add(path);
            }
        }
        LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_PACE - {} up to {} {} MP, {} of {} moves kept",
              entity.getDisplayName(), entity.getId(), currentRound(), formation.get().getPace(),
              isHeldToSlowest ? "the slowest unit's" : "its own", paceLimit, pacedPaths.size(), paths.size());
        List<MovePath> keptPaths = pacedPaths.isEmpty() ? paths : pacedPaths;
        keptPaths = withoutBuildingCollapses(entity, keptPaths);
        if (isTownLeg) {
            List<MovePath> atDoor = follower.townLegs().stackAtDoor(entity, members, keptPaths);
            return follower.townLegs().keepFriendsInReach(entity, members, atDoor);
        }
        return isHeldToSlowest ? keepLastUnitInReach(entity, keptPaths, paceLimit) : keptPaths;
    }

    /**
     * Drops the moves that walk the unit into a building too weak to hold it, while any other move is left. Narrowed
     * to a few moves by the formation's pace, the bot could be left choosing between standing still facing the wrong
     * way and stepping into a light building, and chose the building (HammerGS's playtest, 2026-09-27: an 85-ton
     * Stalker into the CF 15 building at 1119, which came down and left it prone).
     *
     * @return the moves that bring no building down, or all of them when every move would
     */
    private List<MovePath> withoutBuildingCollapses(Entity entity, List<MovePath> paths) {
        Board board = owner.getGame().getBoard(entity);
        if ((board == null) || entity.isAirborne() || entity.hasETypeFlag(Entity.ETYPE_VTOL)) {
            return paths;
        }
        List<MovePath> safePaths = new ArrayList<>();
        for (MovePath path : paths) {
            if (!bringsDownBuilding(entity, board, path)) {
                safePaths.add(path);
            }
        }
        if (safePaths.isEmpty() || (safePaths.size() == paths.size())) {
            return paths;
        }
        LOGGER.info("[BotOrders] {} (ID {}) round {}: BUILDING_AVOIDED - {} of {} moves would bring a building down "
                    + "under it; dropped", entity.getDisplayName(), entity.getId(), currentRound(),
              paths.size() - safePaths.size(), paths.size());
        return safePaths;
    }

    private static boolean bringsDownBuilding(Entity entity, Board board, MovePath path) {
        Coords start = entity.getPosition();
        for (MoveStep step : path.getStepVector()) {
            Coords stepPosition = step.getPosition();
            // a unit already standing in a building is not brought down by leaving it
            if ((stepPosition != null) && !stepPosition.equals(start)
                  && WaypointDistanceField.wouldBringDownBuilding(entity, board, stepPosition)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Keeps a leader whose formation keeps together from reaching its waypoint more than a turn before the last of its
     * units can reach its slot there. Holding the leader to the slowest unit's movement points is not enough on its
     * own: when the shape changes, a unit's new slot can lie well off to the side, and in rougher ground, so it has
     * further to go than the leader (HammerGS's playtest, 2026-09-27: a Longbow walking 3 reached its Line slot six
     * turns after the Grasshopper leading it).
     *
     * @param leader      the formation's leader, about to move
     * @param paths       its moves within the pace
     * @param leaderPace  the movement points it may spend a turn
     *
     * @return the moves that leave it at least as many turns from its waypoint as the last unit needs, less one, but
     *       never more than it is now: a leader too far ahead waits where it stands rather than walking back
     */
    private List<MovePath> keepLastUnitInReach(Entity leader, List<MovePath> paths, int leaderPace) {
        Optional<Coords> waypoint = leader.getUnitOrders().getNextWaypoint();
        if (waypoint.isEmpty() || paths.isEmpty()) {
            return paths;
        }
        int lastUnitTurns = estimatedAssemblyTurns(leader);
        // the others may already have moved this turn, so the leader can be one turn ahead of the last of them; a
        // leader already too far ahead is not sent back, only held where it is
        int turnsNow = turnsToWaypoint(leader, waypoint.get(), leader.getPosition(), leaderPace);
        int turnsToKeep = Math.min(lastUnitTurns - 1, turnsNow);
        if (turnsToKeep <= 0) {
            return paths;
        }
        // held back, the leader stays on its hex, turning at most: any move that only kept its distance let it wander
        // sideways, as a Grasshopper did from 1906 to 1604 and back (HammerGS's playtest, 2026-09-27). Allowed to
        // close in, it may go no further back than it is now
        boolean isHeldInPlace = turnsToKeep >= turnsNow;
        Coords position = leader.getPosition();
        List<MovePath> kept = new ArrayList<>();
        for (MovePath path : paths) {
            Coords end = path.getFinalCoords();
            if ((end == null) || end.equals(position)) {
                kept.add(path);
                continue;
            }
            int turnsLeft = turnsToWaypoint(leader, waypoint.get(), end, leaderPace);
            if (!isHeldInPlace && (turnsLeft >= turnsToKeep) && (turnsLeft < turnsNow)) {
                kept.add(path);
            }
        }
        if (kept.isEmpty()) {
            return paths;
        }
        LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_MARCH - the last unit needs {} turn(s) to its slot at "
                    + "{}; keeping {} turn(s) out{}, {} of {} moves kept", leader.getDisplayName(), leader.getId(),
              currentRound(), lastUnitTurns, waypoint.get().getBoardNum(), turnsToKeep,
              isHeldInPlace ? ", holding in place" : "", kept.size(), paths.size());
        return kept;
    }

    /**
     * @return the turns the unit needs from the position to the waypoint by the way it can really go, at the given
     *       movement points a turn; by the straight line where no route is known
     */
    private int turnsToWaypoint(Entity unit, Coords waypoint, Coords position, int movementPointsPerTurn) {
        int cost = follower.distances().routeCostFrom(unit, waypoint, position);
        if (cost == WaypointDistanceField.UNREACHABLE) {
            cost = position.distance(waypoint);
        }
        int perTurn = Math.max(1, movementPointsPerTurn);
        return (cost + perTurn - 1) / perTurn;
    }

    static int paceMovementPoints(Entity unit, FormationPace pace) {
        return (pace == FormationPace.RUN) ? unit.getRunMP() : unit.getWalkMP();
    }
}
