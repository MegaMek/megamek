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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationShape;
import megamek.common.orders.UnitOrders;
import megamek.common.orders.WaypointOrder;
import megamek.common.units.Entity;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;

/**
 * Where each unit of a formation stands (HammerGS, 2026-09-27): its slot laid out round its leader's next flag in the
 * formation's shape and heading, never ahead of a leader still on its way, a Column falling in on the hexes its
 * commander walked, a slot that would cost far more than the column place folding into the column, and the way the
 * formation faces. Part of {@link UnitOrdersFollower}.
 */
class FormationSlots {

    private static final MMLogger LOGGER = MMLogger.create(FormationSlots.class);

    private final Princess owner;
    private final UnitOrdersFollower follower;

    /** How far off its slot a formation unit may stand when the slot itself is blocked. */
    static final int FORMATION_SLACK = 1;

    /** The hexes of a leader's walk a Column remembers, enough for a long column at wide spacing. */
    private static final int MAXIMUM_TRAIL_LENGTH = 48;

    /** The longest move filled in hex by hex; anything longer, such as a unit set down elsewhere, restarts the trail. */
    private static final int MAXIMUM_TRAIL_GAP = 20;

    /** The least extra movement a slot may cost over the unit's column place before the unit folds into the column. */
    private static final int MINIMUM_FOLD_MARGIN_MP = 3;

    // marks a cached Column slot taken from the leader's trail rather than laid out by heading
    private static final int TRAIL_HEADING = -1;

    private final Map<Integer, SlotChoice> slotChoices = new HashMap<>();
    // slots laid out around a leader still on its way, never ahead of it; kept like slotChoices
    private final Map<Integer, SlotChoice> movingSlotChoices = new HashMap<>();
    // the hexes each formation leader has walked, newest first and hex by hex, for a Column to follow; not saved
    private final Map<Integer, Deque<Coords>> leaderTrails = new HashMap<>();

    /**
     * A formation unit's worked-out slot, kept while the leader's waypoint, the formation's heading and the unit's
     * place in it stay the same. A unit leaving or joining the formation moves the others up or down a place.
     *
     * @param anchor      the leader's waypoint the slot is laid out around
     * @param heading     the formation's heading, 0-5
     * @param slotIndex   the unit's place in the formation, the leader being 0
     * @param memberCount how many units the formation had
     * @param slot        the slot, or {@code null} when no hex would do
     */
    private record SlotChoice(Coords anchor, int heading, int slotIndex, int memberCount, @Nullable Coords slot) {}

    /** The heading a formation takes at its final waypoint when no stopped facing is ordered, fixed once seen. */
    private final Map<String, Integer> finalHeadings = new HashMap<>();

    /** The round each unit was last found past its place in a column, by unit id, to log it once a round. */
    private final Map<Integer, Integer> heldPastPlaceRounds = new HashMap<>();

    /**
     * @param owner    the bot whose units follow orders
     * @param follower the orders follower this works for
     */
    FormationSlots(Princess owner, UnitOrdersFollower follower) {
        this.owner = owner;
        this.follower = follower;
    }

    /**
     * The hex a formation unit heads for: its slot around the hex its leader is heading for, laid out along the
     * leader's heading. It stays the same while the leader moves, so each unit has one fixed hex to make for.
     * Empty for a unit not in a formation, for the unit leading it, and while a formation that breaks on contact has an
     * enemy near.
     *
     * <p>If the slot hex is blocked - off the board, somewhere the unit cannot go, or across deep water from the
     * leader - the unit takes the best hex within {@link #FORMATION_SLACK}. If none will do, the formation folds into
     * a Column behind the leader, which is how it gets through a pass or down a street; it opens out again as soon
     * as the slots are clear.</p>
     *
     * @param entity a unit of the bot
     *
     * @return the hex to head for, or empty when the unit is not following a formation leader
     */
    public Optional<Coords> getFormationSlot(Entity entity) {
        Optional<FormationOrder> formation = follower.activeFormation(entity);
        if (formation.isEmpty() || (entity.getPosition() == null)) {
            return Optional.empty();
        }
        List<Entity> members = follower.roster().formationMembers(entity, formation.get().getLeaderId());
        if (members.size() < 2) {
            return Optional.empty();
        }
        Entity leader = members.get(0);
        if ((leader.getId() == entity.getId()) || (leader.getPosition() == null)) {
            return Optional.empty();
        }
        if (FireReaction.isBrokenToFight(leader)) {
            LOGGER.debug("[BotOrders] {} (ID {}): formation broken - the lance is fighting", entity.getDisplayName(),
                  entity.getId());
            return Optional.empty();
        }
        Optional<Coords> leaderWaypoint = leader.getUnitOrders().getNextWaypoint();
        Coords anchor = leaderWaypoint.orElse(leader.getPosition());
        int heading = formationHeading(leader, anchor);
        // the place in the formation counts only the units still in it, so a unit that leaves closes up the gap
        if (!members.contains(entity)) {
            // out of action: it holds where it is
            return Optional.empty();
        }
        FormationLegs.FormationLeg leg = follower.townLegs().formationLeg(members, anchor, heading, formation.get());
        // the place each unit takes is shared out by the way each would go there, the last arrival soonest
        int slotIndex = leg.places().getOrDefault(entity.getId(), members.indexOf(entity));
        if ((leg.isTown() || follower.isAssembling(entity)) && leg.spots().containsKey(entity.getId())) {
            // through a town the lance breaks formation, and assembling at its first waypoint it has not formed yet:
            // either way each unit makes straight for its place at the flag
            return Optional.of(leg.spots().get(entity.getId()));
        }
        if ((formation.get().getShape() == FormationShape.COLUMN) && !isStoppedOnFlag(leader)) {
            // a Column on the move falls in behind its commander, on the hexes the commander walked (HammerGS,
            // 2026-09-27); stopped on a flag it forms straight behind the way it faces there, below
            Coords trailSlot = trailSlot(entity, leader, anchor, formation.get(), slotIndex);
            if (trailSlot != null) {
                return Optional.of(holdIfPastPlace(entity, anchor, trailSlot));
            }
        }
        SlotChoice cached = slotChoices.get(entity.getId());
        if ((cached != null) && cached.anchor().equals(anchor) && (cached.heading() == heading)
              && (cached.slotIndex() == slotIndex) && (cached.memberCount() == members.size())) {
            // the cached slot is laid out round the flag; a leader still on its way there still leads, so the
            // unit keeps its place beside the leader. Returning the cached slot as it was sent every unit but the
            // first call's on to the flag ahead of its leader (HammerGS's playtest, 2026-09-27: the Griffin stood in
            // its slot at 1610 four rounds before its Stalker came round the buildings)
            return Optional.ofNullable(behindMovingLeader(entity, leader, anchor, cached.slot(), formation.get(),
                  slotIndex));
        }
        if ((cached != null) && (cached.slotIndex() != slotIndex)) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: moves from place {} to place {} of {} in the formation",
                  entity.getDisplayName(), entity.getId(), follower.currentRound(), cached.slotIndex(), slotIndex,
                  members.size());
        }
        Coords slot = chooseSlot(entity, anchor, heading, formation.get(), slotIndex);
        slotChoices.put(entity.getId(), new SlotChoice(anchor, heading, slotIndex, members.size(), slot));
        return Optional.ofNullable(behindMovingLeader(entity, leader, anchor, slot, formation.get(), slotIndex));
    }

    /**
     * A slot laid out around the leader's next flag can lie ahead of a leader still on its way there, and the unit
     * then ran ahead of its commander (HammerGS's playtest, 2026-09-27: a Centurion two hexes in front of the
     * Grasshopper leading it). While the leader is further from the flag than the slot is, the shape is laid out
     * around the leader instead, facing the flag; it settles onto the flag as the leader comes up to it.
     */
    private @Nullable Coords behindMovingLeader(Entity entity, Entity leader, Coords flag, @Nullable Coords slot,
          FormationOrder formation, int slotIndex) {
        Coords leaderPosition = leader.getPosition();
        if ((slot == null) || leaderPosition.equals(flag) || (slot.distance(flag) >= leaderPosition.distance(flag))) {
            return slot;
        }
        int heading = leaderPosition.direction(flag);
        SlotChoice cached = movingSlotChoices.get(entity.getId());
        if ((cached != null) && cached.anchor().equals(leaderPosition) && (cached.heading() == heading)
              && (cached.slotIndex() == slotIndex)) {
            return cached.slot();
        }
        Board board = owner.getGame().getBoard(entity);
        Coords ideal = FormationPlanner.idealSlot(leaderPosition, heading, formation.getShape(), formation.getSpacing(),
              slotIndex);
        Coords moving = (board == null) ? null : settle(entity, board, leaderPosition, ideal);
        Coords chosen = (moving == null) ? slot : moving;
        LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_BEHIND - {} slot {} at {} beside {}, which is still "
                    + "on its way to {}", entity.getDisplayName(), entity.getId(), follower.currentRound(), formation.getShape(),
              slotIndex, chosen.getBoardNum(), leader.getDisplayName(), flag.getBoardNum());
        movingSlotChoices.put(entity.getId(), new SlotChoice(leaderPosition, heading, slotIndex, 0, chosen));
        return chosen;
    }

    /**
     * The way a unit in formation faces: the way the formation itself faces - the facing set where it stops, the next
     * leg where it passes a flag, else from its leader toward the leader's flag. Facing the flag's hex instead turned
     * a unit whose slot lay beyond the flag round to look back at it (HammerGS's town playtest, 2026-09-27: the
     * Centurion at 1410 faced south toward 1512).
     *
     * @return the facing 0-5, or {@link UnitOrders#FACING_AUTO} for a unit not following a formation leader
     */
    int formationFacing(Entity entity) {
        Optional<Entity> leader = follower.roster().formationLeaderOf(entity);
        if (leader.isEmpty() || (leader.get().getPosition() == null)) {
            return UnitOrders.FACING_AUTO;
        }
        List<Coords> route = leader.get().getUnitOrders().getRoute();
        if (route.isEmpty()) {
            return UnitOrders.FACING_AUTO;
        }
        Coords flag = route.get(0);
        if (isStoppedOnFlag(leader.get())) {
            return formationHeading(leader.get(), flag);
        }
        if (leader.get().getPosition().equals(flag)) {
            return (route.size() > 1) ? flag.direction(route.get(1)) : UnitOrders.FACING_AUTO;
        }
        return leader.get().getPosition().direction(flag);
    }

    /**
     * @return {@code true} if the leader stands on its flag and the lance stops there: the end of the route, a hold,
     *       or a wait for the formation to re-form. A Column stopped there forms straight behind the facing set on the
     *       flag, not back along the way it came in (HammerGS's playtest, 2026-09-27: ordered to face north at
     *       2403, the Column trailed off to the south-west along its approach)
     */
    private boolean isStoppedOnFlag(Entity leader) {
        List<Coords> route = leader.getUnitOrders().getRoute();
        if (route.isEmpty() || (leader.getPosition() == null) || !route.get(0).equals(leader.getPosition())) {
            return false;
        }
        return (route.size() == 1) || leader.getUnitOrders().getWaypointOrder(0).isHold()
              || follower.isWaitingForFormation(leader);
    }

    /**
     * A unit already further along the way than its place in the column holds where it is and lets the column come up
     * to it, rather than walking back to its place: a Stalker last in a column walked back south from 1127 to 1129 to
     * stand six hexes behind its commander (HammerGS's playtest, 2026-10-01).
     *
     * @return the unit's own hex when it is past its place, else its place
     */
    private Coords holdIfPastPlace(Entity entity, Coords flag, Coords place) {
        Coords position = entity.getPosition();
        if (position.equals(place)) {
            return place;
        }
        int fromHere = follower.distances().routeCost(entity, flag, position, false, false);
        int fromPlace = follower.distances().routeCost(entity, flag, place, false, false);
        if ((fromHere == WaypointDistanceField.UNREACHABLE) || (fromPlace == WaypointDistanceField.UNREACHABLE)
              || (fromHere >= fromPlace)) {
            return place;
        }
        Integer lastLogged = heldPastPlaceRounds.put(entity.getId(), follower.currentRound());
        if ((lastLogged == null) || (lastLogged != follower.currentRound())) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_HOLD - already past its column place at {}; holding "
                        + "at {} for the column to come up", entity.getDisplayName(), entity.getId(), follower.currentRound(),
                  place.getBoardNum(), position.getBoardNum());
        }
        return position;
    }

    /**
     * A Column's slot: the hex its commander walked {@code place x spacing} hexes back, so the column snakes through
     * the ground behind it rather than lining up behind the next flag. Until the commander has walked that far, the
     * slot lies straight behind it, facing the flag.
     *
     * @return the slot, or {@code null} when neither the trail nor the hexes behind the commander will do
     */
    private @Nullable Coords trailSlot(Entity entity, Entity leader, Coords flag, FormationOrder formation,
          int slotIndex) {
        Board board = owner.getGame().getBoard(entity);
        if (board == null) {
            return null;
        }
        SlotChoice cached = movingSlotChoices.get(entity.getId());
        if ((cached != null) && cached.anchor().equals(leader.getPosition()) && (cached.slotIndex() == slotIndex)
              && (cached.heading() == TRAIL_HEADING)) {
            return cached.slot();
        }
        Deque<Coords> trail = recordTrail(leader);
        int hexesBack = slotIndex * formation.getSpacing();
        Coords ideal = null;
        int stepsBack = 0;
        for (Coords walked : trail) {
            if (stepsBack == hexesBack) {
                ideal = walked;
                break;
            }
            stepsBack++;
        }
        if (ideal == null) {
            int heading = leader.getPosition().equals(flag) ? leader.getFacing() : leader.getPosition().direction(flag);
            ideal = FormationPlanner.idealSlot(leader.getPosition(), heading, FormationShape.COLUMN,
                  formation.getSpacing(), slotIndex);
        }
        Coords settled = settle(entity, board, leader.getPosition(), ideal);
        if (settled != null) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_TRAIL - column place {} at {}, {} hex(es) behind "
                        + "{} along its path", entity.getDisplayName(), entity.getId(), follower.currentRound(), slotIndex,
                  settled.getBoardNum(), hexesBack, leader.getDisplayName());
            movingSlotChoices.put(entity.getId(), new SlotChoice(leader.getPosition(), TRAIL_HEADING, slotIndex, 0,
                  settled));
        }
        return settled;
    }

    /**
     * Adds the leader's hex to its trail if it has moved, filling in the hexes between so the trail runs hex by hex.
     * A jump too long to fill in starts the trail again.
     *
     * @return the trail, newest first
     */
    private Deque<Coords> recordTrail(Entity leader) {
        Deque<Coords> trail = leaderTrails.computeIfAbsent(leader.getId(), id -> new ArrayDeque<>());
        Coords now = leader.getPosition();
        Coords last = trail.peekFirst();
        if (now.equals(last)) {
            return trail;
        }
        if ((last != null) && (last.distance(now) <= MAXIMUM_TRAIL_GAP)) {
            for (Coords between : Coords.intervening(last, now)) {
                if (!between.equals(trail.peekFirst())) {
                    trail.addFirst(between);
                }
            }
        } else {
            trail.clear();
        }
        if (!now.equals(trail.peekFirst())) {
            trail.addFirst(now);
        }
        while (trail.size() > MAXIMUM_TRAIL_LENGTH) {
            trail.removeLast();
        }
        return trail;
    }

    /**
     * The way the formation faces around its leader's waypoint: where the lance stops there (a hold, or the end of the
     * route), the facing set on that waypoint; else toward the next waypoint while more follow; at the last one, the
     * leader's ordered stopped facing, or else the direction of the last leg, fixed the first time it is worked out so
     * the shape does not swing as the leader closes in.
     *
     * <p>A waypoint the lance only passes through lays the shape along the way it is going. Its facing is where the
     * units face if they stop there; turning the shape by it sent the tail of a Column seven hexes off the route
     * (HammerGS's playtest, 2026-09-27).</p>
     */
    private int formationHeading(Entity leader, Coords anchor) {
        List<Coords> leaderRoute = leader.getUnitOrders().getRoute();
        WaypointOrder headOrder = leader.getUnitOrders().getWaypointOrder(0);
        boolean isStopHere = headOrder.isHold() || (leaderRoute.size() == 1);
        if (!leaderRoute.isEmpty() && anchor.equals(leaderRoute.get(0)) && isStopHere
              && (headOrder.getFacing() != UnitOrders.FACING_AUTO)) {
            return headOrder.getFacing();
        }
        if (leaderRoute.size() > 1) {
            return anchor.direction(leaderRoute.get(1));
        }
        int stoppedFacing = leader.getUnitOrders().getFacingWhenStopped();
        if (stoppedFacing != UnitOrders.FACING_AUTO) {
            return stoppedFacing;
        }
        if (leaderRoute.isEmpty()) {
            return leader.getFacing();
        }
        String key = leader.getId() + "|" + anchor.getBoardNum();
        return finalHeadings.computeIfAbsent(key, ignored -> leader.getPosition().equals(anchor)
              ? leader.getFacing() : leader.getPosition().direction(anchor));
    }

    /**
     * Works out a unit's slot around its leader's waypoint. Each unit heads straight for its own slot, so the shape
     * forms where the leader is going, with the full spacing, rather than trailing the leader's moving hex
     * (HammerGS's playtest, 2026-09-26).
     */
    private @Nullable Coords chooseSlot(Entity entity, Coords anchor, int heading, FormationOrder formation,
          int slotIndex) {
        Board board = owner.getGame().getBoard(entity);
        if (board == null) {
            return null;
        }
        Coords ideal = FormationPlanner.idealSlot(anchor, heading, formation.getShape(), formation.getSpacing(),
              slotIndex);
        Coords settled = settle(entity, board, anchor, ideal);
        Coords columnSlot = settle(entity, board, anchor, FormationPlanner.idealSlot(anchor, heading,
              FormationShape.COLUMN, formation.getSpacing(), slotIndex));
        if ((settled != null) && !isFarHarderThan(entity, settled, columnSlot)) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_SLOT - {} slot {} at {} (around {}, facing {})",
                  entity.getDisplayName(), entity.getId(), follower.currentRound(), formation.getShape(), slotIndex,
                  settled.getBoardNum(), anchor.getBoardNum(), heading);
            return settled;
        }
        LOGGER.info("[BotOrders] {} (ID {}) round {}: FORMATION_FOLD - {} slot {} blocked, folding to column at {}",
              entity.getDisplayName(), entity.getId(), follower.currentRound(), formation.getShape(), slotIndex,
              (columnSlot == null) ? anchor.getBoardNum() : columnSlot.getBoardNum());
        owner.getOrdersRadio().report(entity, OrdersRadio.RadioEvent.FOLD, follower.navLabel(entity, anchor));
        return (columnSlot == null) ? anchor : columnSlot;
    }

    /**
     * A slot the unit can stand in may still lie behind a row of buildings or across water, a long way round for a
     * short distance, while its place in a Column behind the leader is along the way the leader came. Such a slot
     * counts as blocked, so the unit folds into the Column and the shape opens out again beyond the obstacle
     * (HammerGS's town playtest, 2026-09-27: a Longbow waded a hex a turn toward a Line slot behind a building row
     * while its lance waited five rounds).
     *
     * @return {@code true} if reaching the slot costs more than a turn's walk beyond reaching the column place
     */
    private boolean isFarHarderThan(Entity entity, Coords slot, @Nullable Coords columnSlot) {
        if ((columnSlot == null) || slot.equals(columnSlot) || (entity.getPosition() == null)) {
            return false;
        }
        int toSlot = follower.distances().routeCostFrom(entity, slot, entity.getPosition());
        int toColumn = follower.distances().routeCostFrom(entity, columnSlot, entity.getPosition());
        if (toColumn == WaypointDistanceField.UNREACHABLE) {
            return false;
        }
        int margin = Math.max(MINIMUM_FOLD_MARGIN_MP, entity.getWalkMP());
        boolean isFarHarder = (toSlot == WaypointDistanceField.UNREACHABLE) || (toSlot > toColumn + margin);
        if (isFarHarder) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: slot {} costs {} MP to reach, its column place {} only {} MP; "
                        + "folding", entity.getDisplayName(), entity.getId(), follower.currentRound(), slot.getBoardNum(),
                  (toSlot == WaypointDistanceField.UNREACHABLE) ? "unreachable" : String.valueOf(toSlot),
                  columnSlot.getBoardNum(), toColumn);
        }
        return isFarHarder;
    }

    /**
     * @return the ideal hex if the unit can stand there, else the best hex within {@link #FORMATION_SLACK} of it,
     *       else {@code null}
     */
    @Nullable Coords settle(Entity entity, Board board, Coords leaderPosition, Coords ideal) {
        if (isUsableSlot(entity, board, leaderPosition, ideal)) {
            return ideal;
        }
        int wantedDistance = ideal.distance(leaderPosition);
        Coords best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int direction = 0; direction < 6; direction++) {
            Coords candidate = ideal.translated(direction, FORMATION_SLACK);
            if (!isUsableSlot(entity, board, leaderPosition, candidate)) {
                continue;
            }
            int score = Math.abs(candidate.distance(leaderPosition) - wantedDistance);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private boolean isUsableSlot(Entity entity, Board board, Coords leaderPosition, Coords slot) {
        // a slot in a building would have the unit walk into it, damaging it or bringing it down; the best hex next to
        // it is taken instead
        return board.contains(slot) && !slot.equals(leaderPosition) && !entity.isLocationProhibited(slot)
              && !board.getHex(slot).containsTerrain(Terrains.BUILDING)
              && FormationSide.sameSide(board, leaderPosition, slot) && !isHeldByOutsider(entity, slot);
    }

    /**
     * @return {@code true} if a unit from outside the formation stands on the hex. Formation members are left out:
     *       they are on the move to their own slots.
     */
    private boolean isHeldByOutsider(Entity entity, Coords slot) {
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        for (Entity occupant : owner.getGame().getEntitiesVector(slot, entity.getBoardId())) {
            Optional<FormationOrder> occupantFormation = occupant.getUnitOrders().getFormation();
            boolean isMember = formation.isPresent() && occupantFormation.isPresent()
                  && occupantFormation.get().sharesLeader(formation.get().getLeaderId());
            if ((occupant.getId() != entity.getId()) && !isMember) {
                return true;
            }
        }
        return false;
    }
}
