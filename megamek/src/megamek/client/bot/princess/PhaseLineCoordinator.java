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

import megamek.common.board.Coords;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.PhaseLine;
import megamek.common.orders.UnitOrders;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Phase lines (HammerGS, 2026-10-02): a lance that reaches a waypoint on a phase line holds there until every friendly
 * lance with a waypoint on the same line is in, then all move on together; the radio calls the hold, reminds the player
 * whom it waits for, and calls the release. Part of {@link UnitOrdersFollower}.
 */
class PhaseLineCoordinator {

    private static final MMLogger LOGGER = MMLogger.create(PhaseLineCoordinator.class);

    private final Princess owner;
    private final UnitOrdersFollower follower;

    /**
     * A unit holding at a phase line.
     *
     * @param phaseLine  the phase line's name
     * @param sinceRound the round it began holding
     * @param loggedRound the round its wait was last logged
     */
    private record PhaseLineHold(String phaseLine, int sinceRound, int loggedRound) {}

    /** The units holding at a phase line, by unit id; to call the hold, the reminders and the release. */
    private final Map<Integer, PhaseLineHold> phaseLineHolds = new HashMap<>();

    /** How often, in rounds, a lance still holding at a phase line reminds the player whom it is waiting for. */
    static final int PHASE_LINE_REMINDER_ROUNDS = 3;

    /**
     * @param owner    the bot whose units follow orders
     * @param follower the orders follower this works for
     */
    PhaseLineCoordinator(Princess owner, UnitOrdersFollower follower) {
        this.owner = owner;
        this.follower = follower;
    }

    private int currentRound() {
        return follower.currentRound();
    }

    /**
     * Whether a unit holds at its phase line: its next waypoint, part-way along its route, is on a phase line, it has
     * reached it, and some other friendly lance with a waypoint on the same line has not yet reached its own. A
     * formation's units hold with their leader. Every lance on the line moves on together once all are in (HammerGS,
     * 2026-10-02: lances of different speeds arrive in step without guessing a number of turns).
     *
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit is holding at a phase line
     */
    boolean isWaitingAtPhaseLine(Entity entity) {
        UnitOrders orders = entity.getUnitOrders();
        if ((orders.getRoute().size() < 2) || (entity.getPosition() == null)
              || follower.roster().isFormationFollower(entity)) {
            return false;
        }
        String phaseLine = orders.getWaypointOrder(0).getPhaseLine();
        if ((phaseLine == null) || !hasReachedPhaseLine(entity)) {
            return false;
        }
        List<Entity> stillComing = stillComingToPhaseLine(entity, phaseLine);
        String label = PhaseLine.display(phaseLine);
        if (stillComing.isEmpty()) {
            if (phaseLineHolds.remove(entity.getId()) != null) {
                LOGGER.info("[BotOrders] {} (ID {}) round {}: PHASE_LINE_CLEAR - every lance is in at {}; moving on",
                      entity.getDisplayName(), entity.getId(), currentRound(), label);
                owner.getOrdersRadio().report(entity, OrdersRadio.RadioEvent.PHASE_LINE_CLEAR, label);
            }
            return false;
        }
        StringBuilder waitingFor = new StringBuilder();
        for (Entity other : stillComing) {
            waitingFor.append((waitingFor.length() == 0) ? "" : ", ").append(other.getShortName());
        }
        PhaseLineHold hold = phaseLineHolds.get(entity.getId());
        if ((hold == null) || !PhaseLine.isSame(hold.phaseLine(), phaseLine)) {
            hold = new PhaseLineHold(phaseLine, currentRound(), -1);
            owner.getOrdersRadio().report(entity, OrdersRadio.RadioEvent.PHASE_LINE_HOLD, label,
                  orders.getRoute().get(0).getBoardNum());
        }
        if (hold.loggedRound() != currentRound()) {
            int roundsHeld = currentRound() - hold.sinceRound();
            LOGGER.info("[BotOrders] {} (ID {}) round {}: PHASE_LINE_WAIT at {} ({}) - waiting for {}, {} round(s) "
                        + "so far", entity.getDisplayName(), entity.getId(), currentRound(), label,
                  orders.getRoute().get(0).getBoardNum(), waitingFor, roundsHeld);
            if ((roundsHeld > 0) && ((roundsHeld % PHASE_LINE_REMINDER_ROUNDS) == 0)) {
                // the lance waits as long as it takes (HammerGS, 2026-10-02); the player hears whom for, and can
                // send it on with Resume or new orders
                owner.getOrdersRadio().report(entity, OrdersRadio.RadioEvent.PHASE_LINE_STILL_HOLDING, label,
                      waitingFor.toString(), String.valueOf(roundsHeld));
            }
            hold = new PhaseLineHold(phaseLine, hold.sinceRound(), currentRound());
        }
        phaseLineHolds.put(entity.getId(), hold);
        return true;
    }

    /**
     * @return {@code true} if the unit stands at its next waypoint: on it for a formation's leader, else within
     *       {@link Princess#DISTANCE_TO_WAYPOINT}
     */
    private boolean hasReachedPhaseLine(Entity entity) {
        Coords waypoint = entity.getUnitOrders().getRoute().get(0);
        int reachedWithin = follower.isLeadingFormationOnRoute(entity) ? follower.flagRadius(entity, waypoint)
              : Princess.DISTANCE_TO_WAYPOINT;
        return entity.getPosition().distance(waypoint) <= reachedWithin;
    }

    /**
     * The friendly units, of this bot or another on the same side, that lead a route with a waypoint on the phase
     * line still ahead of them: further along than their next waypoint, or their next waypoint and not yet reached.
     * A formation's other units go with their leader and are not counted.
     *
     * @param entity    the unit holding at the line
     * @param phaseLine the phase line's name
     *
     * @return the units still to come
     */
    List<Entity> stillComingToPhaseLine(Entity entity, String phaseLine) {
        List<Entity> stillComing = new ArrayList<>();
        for (Entity other : owner.getGame().getEntitiesVector()) {
            // a lance that cannot come is not waited for: gone, off the board, out of action, or one that must
            // withdraw rather than follow its route
            if ((other.getId() == entity.getId()) || (other.getPosition() == null) || other.isDestroyed()
                  || other.isDoomed() || other.isOffBoard() || (other.getOwner() == null)
                  || other.getOwner().isEnemyOf(entity.getOwner()) || isLedByAnother(other)
                  || follower.isOutOfAction(other)) {
                continue;
            }
            List<Coords> route = other.getUnitOrders().getRoute();
            for (int index = 0; index < route.size(); index++) {
                if (!PhaseLine.isSame(phaseLine, other.getUnitOrders().getWaypointOrder(index).getPhaseLine())) {
                    continue;
                }
                boolean isIn = (index == 0)
                      && (other.getPosition().distance(route.get(0)) <= Princess.DISTANCE_TO_WAYPOINT);
                if (!isIn) {
                    stillComing.add(other);
                }
                break;
            }
        }
        return stillComing;
    }

    /**
     * @return {@code true} if the unit follows another unit of its formation, which leads its route
     */
    private boolean isLedByAnother(Entity unit) {
        Optional<FormationOrder> formation = unit.getUnitOrders().getFormation();
        if (formation.isEmpty() || (formation.get().getLeaderId() == unit.getId())) {
            return false;
        }
        Entity leader = owner.getGame().getEntity(formation.get().getLeaderId());
        return (leader != null) && (leader.getPosition() != null) && !leader.isDestroyed() && !leader.isDoomed();
    }
}
