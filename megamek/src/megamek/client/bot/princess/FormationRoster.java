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
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.orders.FormationOrder;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Who is in each formation (HammerGS, 2026-09-27): its units in action in slot order, the acting leader - the
 * commander, then the second-in-command, then the next in line - units out of action or falling behind, which their
 * lance stops waiting for, and the radio calls when that changes. Part of {@link UnitOrdersFollower}.
 */
class FormationRoster {

    private static final MMLogger LOGGER = MMLogger.create(FormationRoster.class);

    private final Princess owner;
    private final UnitOrdersFollower follower;

    /** Rounds a unit may lie prone before its lance counts it out of action and moves on without it. */
    static final int PRONE_ROUNDS_BEFORE_DROPPED = 2;

    /** Rounds a unit may go without getting any closer to its slot before its lance stops waiting for it. */
    static final int ROUNDS_WITHOUT_PROGRESS = 3;

    /** The place of a lance's second-in-command, who takes command when the commander is lost. */
    private static final int SECOND_IN_COMMAND_PLACE = 2;

    /**
     * How a formation unit is getting on toward its slot: the best it has done, and since when.
     *
     * @param slot       the slot it is making for
     * @param bestCost   the least movement it has needed from where it stood to reach the slot
     * @param sinceRound the round it last got closer
     */
    private record SlotProgress(Coords slot, int bestCost, int sinceRound) {}

    /** Each formation unit's progress toward its slot, by unit id; not saved. */
    private final Map<Integer, SlotProgress> slotProgress = new HashMap<>();

    /** The round each formation unit was first seen prone, by unit id; not saved. */
    private final Map<Integer, Integer> proneSinceRounds = new HashMap<>();

    /** Units already called out of action, so the radio calls it once; not saved. */
    private final Set<Integer> outOfActionUnitIds = new HashSet<>();

    /** Units already called as falling behind, so the radio calls it once; not saved. */
    private final Set<Integer> fallingBehindUnitIds = new HashSet<>();

    /** The unit last seen leading each formation, by the formation's leader id, to call a change of command. */
    private final Map<Integer, Integer> commandingUnitIds = new HashMap<>();

    /**
     * @param owner    the bot whose units follow orders
     * @param follower the orders follower this works for
     */
    FormationRoster(Princess owner, UnitOrdersFollower follower) {
        this.owner = owner;
        this.follower = follower;
    }

    private int currentRound() {
        return follower.currentRound();
    }

    /**
     * @param entity a unit of the bot
     *
     * @return the unit leading the unit's formation, if the unit is in one and is not leading it
     */
    Optional<Entity> formationLeaderOf(Entity entity) {
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        if (formation.isEmpty() || follower.activeFormation(entity).isEmpty()) {
            // out of formation on this leg: the unit follows its own route like a unit on its own
            return Optional.empty();
        }
        List<Entity> members = formationMembers(entity, formation.get().getLeaderId());
        if ((members.size() < 2) || (members.get(0).getId() == entity.getId()) || !members.contains(entity)) {
            return Optional.empty();
        }
        return Optional.of(members.get(0));
    }

    /**
     * @return {@code true} if the unit follows a formation leader on this leg, so the leader decides for it
     */
    boolean isFormationFollower(Entity entity) {
        return formationLeaderOf(entity).isPresent();
    }

    /**
     * A unit its lance no longer counts: one that cannot move - shut down, crew unconscious, stuck, or with no
     * movement left - or one that has lain prone {@link #PRONE_ROUNDS_BEFORE_DROPPED} rounds. It holds and fights
     * where it is while the others close up and move on; once it can move again it rejoins (HammerGS, 2026-09-27).
     *
     * @param unit a unit in a formation
     *
     * @return {@code true} if the unit is out of action
     */
    boolean isOutOfAction(Entity unit) {
        if (unit.isImmobile() || unit.isStuck() || unit.isPermanentlyImmobilized(false)
              || ((unit.getWalkMP() <= 0) && (unit.getJumpMP() <= 0))) {
            return true;
        }
        Integer proneSince = proneSinceRounds.get(unit.getId());
        return unit.isProne() && (proneSince != null)
              && ((currentRound() - proneSince) >= PRONE_ROUNDS_BEFORE_DROPPED);
    }

    /**
     * A unit that has gone {@link #ROUNDS_WITHOUT_PROGRESS} rounds without getting any closer to its slot - wading,
     * blocked, or going back and forth. It keeps its place in the shape and follows on, but its lance no longer waits
     * for it or keeps to its pace; it rejoins once it reaches its slot (HammerGS, 2026-09-27).
     *
     * @param unit a unit in a formation
     *
     * @return {@code true} if the lance has stopped waiting for the unit
     */
    boolean isFallingBehind(Entity unit) {
        SlotProgress progress = slotProgress.get(unit.getId());
        return (progress != null) && ((currentRound() - progress.sinceRound()) >= ROUNDS_WITHOUT_PROGRESS);
    }

    private static int placeOf(Entity member) {
        return member.getUnitOrders().getFormation().map(FormationOrder::getSlot).orElse(Integer.MAX_VALUE);
    }

    /**
     * Keeps count, once the bot's units have moved, of each formation unit lying prone and of its progress toward its
     * slot, and calls on the radio a unit going out of action, one falling behind, and a new commander taking over.
     */
    void trackFormationUnits() {
        for (Entity unit : owner.getEntitiesOwned()) {
            Optional<FormationOrder> formation = unit.getUnitOrders().getFormation();
            if (formation.isEmpty() || (unit.getPosition() == null)) {
                continue;
            }
            if (unit.isProne()) {
                proneSinceRounds.putIfAbsent(unit.getId(), currentRound());
            } else {
                proneSinceRounds.remove(unit.getId());
            }
            trackOutOfAction(unit);
            trackProgress(unit);
            trackCommand(unit, formation.get().getLeaderId());
        }
    }

    private void trackOutOfAction(Entity unit) {
        if (!isOutOfAction(unit)) {
            outOfActionUnitIds.remove(unit.getId());
            return;
        }
        if (outOfActionUnitIds.add(unit.getId())) {
            String hex = unit.getPosition().getBoardNum();
            LOGGER.info("[BotOrders] {} (ID {}) round {}: OUT_OF_ACTION at {} (immobile {}, stuck {}, prone since {}); "
                        + "the lance moves on without it", unit.getDisplayName(), unit.getId(), currentRound(), hex,
                  unit.isImmobile(), unit.isStuck(), proneSinceRounds.get(unit.getId()));
            owner.getOrdersRadio().report(unit, OrdersRadio.RadioEvent.UNIT_DOWN, unit.getShortName(), hex);
        }
    }

    private void trackProgress(Entity unit) {
        Optional<Coords> slot = follower.getFormationSlot(unit);
        if (slot.isEmpty() || unit.getPosition().equals(slot.get())) {
            slotProgress.remove(unit.getId());
            fallingBehindUnitIds.remove(unit.getId());
            return;
        }
        int cost = follower.distances().routeCostFrom(unit, slot.get(), unit.getPosition());
        if (cost == WaypointDistanceField.UNREACHABLE) {
            cost = unit.getPosition().distance(slot.get());
        }
        SlotProgress progress = slotProgress.get(unit.getId());
        if ((progress == null) || !progress.slot().equals(slot.get()) || (cost < progress.bestCost())) {
            // a new slot starts the count again: a leader still moving moves the slot, and a leader held back for
            // this unit holds it still, so a unit truly stuck is still found
            slotProgress.put(unit.getId(), new SlotProgress(slot.get(), cost, currentRound()));
            fallingBehindUnitIds.remove(unit.getId());
            return;
        }
        if (isFallingBehind(unit) && fallingBehindUnitIds.add(unit.getId())) {
            String hex = unit.getPosition().getBoardNum();
            LOGGER.info("[BotOrders] {} (ID {}) round {}: FALLING_BEHIND at {} - no closer to its slot at {} since "
                        + "round {}; the lance stops waiting for it", unit.getDisplayName(), unit.getId(),
                  currentRound(), hex, slot.get().getBoardNum(), progress.sinceRound());
            owner.getOrdersRadio().report(unit, OrdersRadio.RadioEvent.FALLING_BEHIND, unit.getShortName(), hex);
        }
    }

    private void trackCommand(Entity unit, int leaderId) {
        List<Entity> members = formationMembers(unit, leaderId);
        if (members.isEmpty() || (members.get(0).getId() != unit.getId())) {
            return;
        }
        Integer previous = commandingUnitIds.put(leaderId, unit.getId());
        if ((previous != null) && (previous != unit.getId())) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: TAKES_COMMAND of the formation led by unit {}",
                  unit.getDisplayName(), unit.getId(), currentRound(), leaderId);
            owner.getOrdersRadio().report(unit, OrdersRadio.RadioEvent.COMMAND, unit.getShortName());
        }
    }

    /**
     * @return the formation's units still on the board and in action, any owner on the same side, in slot order; the
     *       first is the acting leader, which is the original leader while it is in action, then the
     *       second-in-command, then the next in line. A player's unit being followed leads without a formation order
     *       of its own.
     */
    List<Entity> formationMembers(Entity entity, int leaderId) {
        List<Entity> members = new ArrayList<>();
        for (Entity candidate : owner.getGame().getEntitiesVector()) {
            Optional<FormationOrder> candidateFormation = candidate.getUnitOrders().getFormation();
            if (candidateFormation.isEmpty() || !candidateFormation.get().sharesLeader(leaderId)) {
                continue;
            }
            if ((candidate.getPosition() == null) || candidate.isDestroyed() || candidate.isDoomed()
                  || candidate.getOwner().isEnemyOf(entity.getOwner()) || isOutOfAction(candidate)) {
                continue;
            }
            members.add(candidate);
        }
        members.sort(Comparator.comparingInt(FormationRoster::placeOf));
        if (!members.isEmpty() && (members.get(0).getId() != leaderId)) {
            // the commander is lost: as in a tank platoon, the second-in-command takes over, and failing that the
            // next in line (HammerGS, 2026-09-27)
            for (Entity member : members) {
                if (placeOf(member) == SECOND_IN_COMMAND_PLACE) {
                    members.remove(member);
                    members.add(0, member);
                    break;
                }
            }
        }
        Entity leader = owner.getGame().getEntity(leaderId);
        if (isFollowablePlayerUnit(leader, entity) && !members.contains(leader)) {
            members.add(0, leader);
        }
        return members;
    }

    /**
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit is ordered to follow a player's unit: its formation leader belongs to a human
     *       player on its side and is on the board
     */
    public boolean isFollowingPlayerUnit(Entity entity) {
        Optional<FormationOrder> formation = entity.getUnitOrders().getFormation();
        if (formation.isEmpty() || (formation.get().getLeaderId() == entity.getId())) {
            return false;
        }
        return isFollowablePlayerUnit(owner.getGame().getEntity(formation.get().getLeaderId()), entity);
    }

    /**
     * @return {@code true} if the leader is a human player's unit on the follower's side, alive and on the board; a
     *       bot's unit leads only while it has a formation order, so a leader that left its formation does not
     */
    private static boolean isFollowablePlayerUnit(@Nullable Entity leader, Entity follower) {
        return (leader != null) && (leader.getPosition() != null) && !leader.isDestroyed() && !leader.isDoomed()
              && (leader.getOwner() != null) && !leader.getOwner().isBot()
              && !leader.getOwner().isEnemyOf(follower.getOwner());
    }
}
