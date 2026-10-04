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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.moves.MovePath;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Each formation's leg to its leader's next flag, worked out once when the leg starts (HammerGS, 2026-10-01): which unit
 * takes which place at the flag, by least travel then fewest crossings, and whether the way runs through a town. On a
 * town leg the lance breaks formation, each unit takes its own street to its place, held together by a leash of
 * weapon range, the slowest keeping to the open lanes and units waiting their turn at a narrow door. Part of
 * {@link UnitOrdersFollower}.
 */
class FormationLegs {

    private static final MMLogger LOGGER = MMLogger.create(FormationLegs.class);

    private final Princess owner;
    private final UnitOrdersFollower follower;

    /**
     * A formation's leg to its leader's next flag, worked out once when the leg starts.
     *
     * @param anchor    the flag the leg leads to
     * @param memberIds the formation's units in action when it was worked out, the leader first
     * @param isTown    {@code true} if the way runs through a town, so the lance breaks formation until the flag
     * @param spots     each unit's place at the flag by unit id, the leader's being the flag; on a town leg the unit
     *                  makes straight for it
     * @param places    each unit's place in the formation by unit id, the leader's being 0
     * @param round     the round the places were last shared out
     */
    record FormationLeg(Coords anchor, List<Integer> memberIds, boolean isTown, Map<Integer, Coords> spots,
          Map<Integer, Integer> places, int round) {}

    /** The movement points a new sharing out of places must save before units still coming change places. */
    private static final int REPAIR_MARGIN_MP = 2;

    /** Each formation's current leg, by the formation's leader id; not saved. */
    private final Map<Integer, FormationLeg> formationLegs = new HashMap<>();

    /** The narrow hexes of each board, by board id, worked out once a round; not saved. */
    private final Map<Integer, Map<Coords, Integer>> narrowHexesByBoard = new HashMap<>();
    private int narrowHexesRound = -1;

    // a place a unit cannot reach, counted as far off rather than overflowing a sum
    private static final int UNREACHABLE_PLACE_COST = 10_000;

    /**
     * @param owner    the bot whose units follow orders
     * @param follower the orders follower this works for
     */
    FormationLegs(Princess owner, UnitOrdersFollower follower) {
        this.owner = owner;
        this.follower = follower;
    }

    /**
     * Works out a formation's leg to its leader's next flag once, when the leg starts or the lance changes: whether the
     * way runs through a town and, if so, which unit takes which place at the flag. A Column is already single file
     * and keeps following its commander.
     */
    FormationLeg formationLeg(List<Entity> members, Coords anchor, int heading, FormationOrder formation) {
        Entity leader = members.get(0);
        int formationId = formation.getLeaderId();
        List<Integer> memberIds = new ArrayList<>();
        for (Entity member : members) {
            memberIds.add(member.getId());
        }
        FormationLeg cached = formationLegs.get(formationId);
        if ((cached != null) && cached.anchor().equals(anchor) && cached.memberIds().equals(memberIds)) {
            if (cached.round() != follower.currentRound()) {
                return repairStillComing(cached, members, formationId);
            }
            return cached;
        }
        Map<Integer, Coords> spots = pairPlaces(members, anchor, heading, formation);
        Map<Integer, Integer> places = placeNumbers(spots, members, anchor, heading, formation);
        FormationLeg leg = new FormationLeg(anchor, memberIds, false, spots, places, follower.currentRound());
        Board board = owner.getGame().getBoard(leader);
        if ((board != null) && (formation.getShape() != FormationShape.COLUMN)) {
            int mostTownHexes = 0;
            for (Entity member : members) {
                Coords position = member.getPosition();
                if (position != null) {
                    mostTownHexes = Math.max(mostTownHexes, TownLegPlanner.hexesBesideBuildings(board, position,
                          anchor, hex -> follower.distances().routeCostFrom(member, anchor, hex)));
                }
            }
            if (mostTownHexes >= TownLegPlanner.TOWN_HEXES) {
                leg = new FormationLeg(anchor, memberIds, true, spots, places, follower.currentRound());
            }
            LOGGER.info("[BotOrders] {} (ID {}) round {}: {} to {} - {} hex(es) in or beside buildings on the way "
                        + "(a town leg from {}){}", leader.getDisplayName(), leader.getId(), follower.currentRound(),
                  leg.isTown() ? "TOWN_LEG" : "OPEN_LEG", anchor.getBoardNum(), mostTownHexes,
                  TownLegPlanner.TOWN_HEXES, "; places " + describeSpots(leg.spots()));
        } else {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: COLUMN_LEG to {}; places {}", leader.getDisplayName(),
                  leader.getId(), follower.currentRound(), anchor.getBoardNum(), describeSpots(leg.spots()));
        }
        formationLegs.put(formationId, leg);
        return leg;
    }

    /**
     * @return the formation's slots at the flag, place 1 first, as the shape lays them out
     */
    private List<Coords> slotsAtFlag(List<Entity> members, Coords anchor, int heading, FormationOrder formation) {
        Board board = owner.getGame().getBoard(members.get(0));
        List<Coords> slots = new ArrayList<>();
        for (int place = 1; place < members.size(); place++) {
            Coords ideal = FormationPlanner.idealSlot(anchor, heading, formation.getShape(), formation.getSpacing(),
                  place);
            Coords settled = (board == null) ? null : follower.settle(members.get(place), board, anchor, ideal);
            slots.add((settled == null) ? ideal : settled);
        }
        return slots;
    }

    /**
     * @return each unit's number in the formation from its place at the flag: the leader 0, the others by which of the
     *       shape's slots they were given
     */
    private Map<Integer, Integer> placeNumbers(Map<Integer, Coords> spots, List<Entity> members, Coords anchor,
          int heading, FormationOrder formation) {
        List<Coords> slots = slotsAtFlag(members, anchor, heading, formation);
        Map<Integer, Integer> places = new HashMap<>();
        places.put(members.get(0).getId(), 0);
        for (int index = 1; index < members.size(); index++) {
            int slot = slots.indexOf(spots.get(members.get(index).getId()));
            places.put(members.get(index).getId(), (slot < 0) ? index : (slot + 1));
        }
        return places;
    }

    /**
     * @return the pace's movement points a turn for each unit, for pairing units with places by when they arrive
     */
    private int turnsMovement(Entity unit) {
        Optional<FormationOrder> formation = unit.getUnitOrders().getFormation();
        return FormationMarch.paceMovementPoints(unit, formation.map(FormationOrder::getPace).orElse(FormationPace.WALK));
    }

    /**
     * The places at the flag: the leader's is the flag itself, and the others' are the formation's slots, shared out
     * by the way each unit would go to them so the last one arrives soonest. HammerGS did not mind which Mek took
     * which place, only that they could support each other (2026-10-01); kept in lobby order, the slow Stalker
     * deployed furthest west drew the far east end of the Line and crossed the whole lance.
     */
    private Map<Integer, Coords> pairPlaces(List<Entity> members, Coords anchor, int heading,
          FormationOrder formation) {
        List<Entity> followers = members.subList(1, members.size());
        List<Coords> slots = slotsAtFlag(members, anchor, heading, formation);
        Map<Integer, Coords> spots = TownLegPlanner.assignSpots(followers, slots, this::pairingCost,
              this::turnsMovement);
        if (spots.isEmpty()) {
            // too many to pair by trying every way: each keeps its own place
            for (int place = 1; place < members.size(); place++) {
                spots.put(members.get(place).getId(), slots.get(place - 1));
            }
        }
        spots.put(members.get(0).getId(), anchor);
        return spots;
    }

    /**
     * Shares out again, once a round, the places of the units still coming among the places still free, from where
     * they stand now. A unit in its place keeps it. HammerGS did not fix the places at the start of his walk through
     * the town: the first Mek through took the nearest place and the last took what was left (2026-10-01). The units
     * change places only when that saves {@link #REPAIR_MARGIN_MP} or more, so two do not swap back and forth.
     */
    private FormationLeg repairStillComing(FormationLeg leg, List<Entity> members, int formationId) {
        Map<Integer, Coords> spots = new HashMap<>(leg.spots());
        List<Entity> stillComing = new ArrayList<>();
        List<Coords> freeSpots = new ArrayList<>();
        for (Entity member : members.subList(1, members.size())) {
            Coords spot = spots.get(member.getId());
            if ((spot == null) || spot.equals(member.getPosition())) {
                continue;
            }
            stillComing.add(member);
            freeSpots.add(spot);
        }
        Map<Integer, Integer> places = new HashMap<>(leg.places());
        if (stillComing.size() >= 2) {
            Map<Integer, Coords> pairing = TownLegPlanner.assignSpots(stillComing, freeSpots, this::pairingCost,
                  this::turnsMovement);
            int costNow = 0;
            int costAfter = 0;
            int lastArrivalNow = 0;
            int lastArrivalAfter = 0;
            for (Entity unit : stillComing) {
                int perTurn = Math.max(1, turnsMovement(unit));
                int now = pairingCost(unit, spots.get(unit.getId()));
                costNow += now;
                lastArrivalNow = Math.max(lastArrivalNow, (now + perTurn - 1) / perTurn);
                if (!pairing.isEmpty()) {
                    int after = pairingCost(unit, pairing.get(unit.getId()));
                    costAfter += after;
                    lastArrivalAfter = Math.max(lastArrivalAfter, (after + perTurn - 1) / perTurn);
                }
            }
            boolean isQuicker = (lastArrivalAfter < lastArrivalNow)
                  || ((lastArrivalAfter == lastArrivalNow) && ((costAfter + REPAIR_MARGIN_MP) <= costNow));
            if (!pairing.isEmpty() && isQuicker) {
                Map<Coords, Integer> placeOfSpot = new HashMap<>();
                for (Entity unit : stillComing) {
                    placeOfSpot.put(spots.get(unit.getId()), places.get(unit.getId()));
                }
                for (Entity unit : stillComing) {
                    Coords newSpot = pairing.get(unit.getId());
                    spots.put(unit.getId(), newSpot);
                    places.put(unit.getId(), placeOfSpot.getOrDefault(newSpot, places.get(unit.getId())));
                }
                String gain = (lastArrivalAfter < lastArrivalNow)
                      ? ("the last in by turn " + lastArrivalAfter + " instead of " + lastArrivalNow)
                      : ("the last in by the same turn, " + (costNow - costAfter) + " MP less in all");
                LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_PLACES - the units still coming change places, "
                            + "{}: {}", members.get(0).getDisplayName(), members.get(0).getId(), follower.currentRound(), gain,
                      describeSpots(pairing));
            }
        }
        FormationLeg repaired = new FormationLeg(leg.anchor(), leg.memberIds(), leg.isTown(), spots, places,
              follower.currentRound());
        formationLegs.put(formationId, repaired);
        return repaired;
    }

    /**
     * @return the movement points from where the unit stands to a place, by the way it can really go; a place it
     *       cannot reach counts as far off
     */
    private int pairingCost(Entity unit, Coords spot) {
        int cost = follower.distances().routeCost(unit, spot, unit.getPosition(), false, isSlowestOfLance(unit));
        return (cost == WaypointDistanceField.UNREACHABLE) ? UNREACHABLE_PLACE_COST : cost;
    }

    /**
     * @return {@code true} if the unit is one of its lance's slowest at the lance's pace, with a faster unit in it
     */
    boolean isSlowestOfLance(Entity unit) {
        Optional<FormationOrder> formation = follower.activeFormation(unit);
        if (formation.isEmpty()) {
            return false;
        }
        FormationPace pace = formation.get().getPace();
        return TownLegPlanner.isSlowest(unit, follower.roster().formationMembers(unit, formation.get().getLeaderId()),
              member -> FormationMarch.paceMovementPoints(member, pace));
    }

    /**
     * @return the narrow hexes of the unit's board, worked out once a round since buildings can come down
     */
    Map<Coords, Integer> narrowHexes(Entity unit) {
        Board board = owner.getGame().getBoard(unit);
        if (board == null) {
            return Map.of();
        }
        if (narrowHexesRound != follower.currentRound()) {
            narrowHexesByBoard.clear();
            narrowHexesRound = follower.currentRound();
        }
        return narrowHexesByBoard.computeIfAbsent(unit.getBoardId(), ignored -> TownLegPlanner.narrowHexes(board));
    }

    /**
     * The movement points a move ending in cover counts as saving, for a unit on a town leg: where two moves are about
     * as good, the one ending in woods or on higher ground wins (HammerGS's town walk, 2026-10-01: the Griffin stopped
     * on the wooded hill at 1420 on purpose).
     *
     * @param entity   the unit about to move
     * @param finalHex where the move ends
     *
     * @return {@link TownLegPlanner#COVER_DISCOUNT_MP} for a move ending in cover on a town leg, else 0
     */
    int townCoverDiscount(Entity entity, Coords finalHex) {
        Board board = owner.getGame().getBoard(entity);
        if ((board == null) || (entity.getPosition() == null) || (finalHex == null) || !isOnTownLeg(entity)
              || !board.contains(entity.getPosition())) {
            return 0;
        }
        int startLevel = board.getHex(entity.getPosition()).getLevel();
        return TownLegPlanner.isCover(board, finalHex, startLevel) ? TownLegPlanner.COVER_DISCOUNT_MP : 0;
    }

    private String describeSpots(Map<Integer, Coords> spots) {
        StringBuilder description = new StringBuilder();
        for (Map.Entry<Integer, Coords> spot : spots.entrySet()) {
            Entity unit = owner.getGame().getEntity(spot.getKey());
            description.append((description.length() == 0) ? "" : ", ")
                  .append((unit == null) ? ("unit " + spot.getKey()) : unit.getShortName())
                  .append(' ').append(spot.getValue().getBoardNum());
        }
        return description.toString();
    }

    /**
     * @return {@code true} if the unit's formation is on a town leg, broken up until its flag
     */
    boolean isOnTownLeg(Entity entity) {
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        if (formation.isEmpty() || follower.activeFormation(entity).isEmpty()) {
            return false;
        }
        FormationLeg leg = formationLegs.get(formation.get().getLeaderId());
        Optional<Coords> anchor = formationAnchor(entity, formation.get());
        return (leg != null) && leg.isTown() && anchor.isPresent() && leg.anchor().equals(anchor.get())
              && leg.spots().containsKey(entity.getId());
    }

    /**
     * @return the unit's own place at its flag while its formation is on a town leg
     */
    Optional<Coords> townSpotOf(Entity entity) {
        if (!isOnTownLeg(entity)) {
            return Optional.empty();
        }
        return Optional.of(formationLegs.get(entity.getUnitOrders().getFormation().get().getLeaderId()).spots()
              .get(entity.getId()));
    }

    boolean isTownSpotOf(Entity entity, Coords hex) {
        // the path ranker asks this for every move it weighs: rule most hexes out before looking at the lance
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        if (formation.isEmpty()) {
            return false;
        }
        FormationLeg leg = formationLegs.get(formation.get().getLeaderId());
        if ((leg == null) || !leg.isTown() || !hex.equals(leg.spots().get(entity.getId()))) {
            return false;
        }
        return isOnTownLeg(entity);
    }

    private Optional<Coords> formationAnchor(Entity entity, FormationOrder formation) {
        List<Entity> members = follower.roster().formationMembers(entity, formation.getLeaderId());
        if (members.isEmpty()) {
            return Optional.empty();
        }
        Entity leader = members.get(0);
        return Optional.of(leader.getUnitOrders().getNextWaypoint().orElse(leader.getPosition()));
    }

    /**
     * @return the other units of the unit's lance standing in their places on a town leg
     */
    List<Entity> unitsInPlaceBeside(Entity entity) {
        List<Entity> inPlace = new ArrayList<>();
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        if (formation.isEmpty()) {
            return inPlace;
        }
        FormationLeg leg = formationLegs.get(formation.get().getLeaderId());
        if (leg == null) {
            return inPlace;
        }
        for (Entity member : follower.roster().formationMembers(entity, formation.get().getLeaderId())) {
            Coords spot = leg.spots().get(member.getId());
            if ((member.getId() != entity.getId()) && (spot != null) && spot.equals(member.getPosition())) {
                inPlace.add(member);
            }
        }
        return inPlace;
    }

    /**
     * On a town leg, units whose ways into the streets go through the same door stack up outside it and go through
     * one after another, the nearest first (HammerGS, 2026-10-01: "when infantry are going through a door, they queue
     * up then go in"). See {@link TownLegPlanner#keepPlaceInStack}.
     */
    List<MovePath> stackAtDoor(Entity entity, List<Entity> members, List<MovePath> paths) {
        Board board = owner.getGame().getBoard(entity);
        FormationLeg leg = formationLegs.get(entity.getUnitOrders().getFormation().map(FormationOrder::getLeaderId)
              .orElse(-1));
        if ((board == null) || (leg == null) || !leg.isTown()) {
            return paths;
        }
        Coords ownDoor = null;
        int ownStepsToDoor = 0;
        List<Entity> ahead = new ArrayList<>();
        Map<Integer, Integer> stepsToDoor = new HashMap<>();
        Map<Integer, Coords> doors = new HashMap<>();
        for (Entity member : members) {
            Coords spot = leg.spots().get(member.getId());
            Coords position = member.getPosition();
            if ((spot == null) || (position == null) || position.equals(spot)) {
                continue;
            }
            List<Coords> way = TownLegPlanner.wayTo(board, position, spot, hex -> follower.distances().routeCostFrom(member, spot, hex));
            Coords door = TownLegPlanner.doorOn(board, way);
            if (door != null) {
                doors.put(member.getId(), door);
                stepsToDoor.put(member.getId(), way.indexOf(door));
            }
        }
        ownDoor = doors.get(entity.getId());
        if (ownDoor == null) {
            return paths;
        }
        ownStepsToDoor = stepsToDoor.get(entity.getId());
        for (Entity member : members) {
            Integer memberSteps = stepsToDoor.get(member.getId());
            if ((member.getId() == entity.getId()) || !ownDoor.equals(doors.get(member.getId()))) {
                continue;
            }
            boolean isNearer = (memberSteps < ownStepsToDoor)
                  || ((memberSteps == ownStepsToDoor) && (member.getId() < entity.getId()));
            if (isNearer) {
                ahead.add(member);
            }
        }
        if (ahead.isEmpty()) {
            if (stepsToDoor.values().size() > 1) {
                LOGGER.info("[BotOrders] {} (ID {}) round {}: TOWN_DOOR - first through the door at {}",
                      entity.getDisplayName(), entity.getId(), follower.currentRound(), ownDoor.getBoardNum());
            }
            return paths;
        }
        List<MovePath> kept = TownLegPlanner.keepPlaceInStack(paths, ownDoor, ahead.size());
        LOGGER.info("[BotOrders] {} (ID {}) round {}: TOWN_DOOR - number {} in the stack for the door at {}, behind {}; "
                    + "{} of {} moves kept", entity.getDisplayName(), entity.getId(), follower.currentRound(), ahead.size() + 1,
              ownDoor.getBoardNum(), ahead.get(ahead.size() - 1).getShortName(), kept.size(), paths.size());
        return kept;
    }

    /**
     * On a town leg, keeps each unit within its leash of a friend (see {@link TownLegPlanner#keepWithinReach}).
     */
    List<MovePath> keepFriendsInReach(Entity entity, List<Entity> members, List<MovePath> paths) {
        List<Coords> friends = new ArrayList<>();
        for (Entity member : members) {
            if ((member.getId() != entity.getId()) && (member.getPosition() != null)) {
                friends.add(member.getPosition());
            }
        }
        int leash = TownLegPlanner.leash(entity);
        List<MovePath> kept = TownLegPlanner.keepWithinReach(entity, paths, friends, leash);
        LOGGER.info("[BotOrders] {} (ID {}) round {}: TOWN_LEASH - within {} hexes of a friend, {} of {} moves kept",
              entity.getDisplayName(), entity.getId(), follower.currentRound(), leash, kept.size(), paths.size());
        return kept;
    }
}
