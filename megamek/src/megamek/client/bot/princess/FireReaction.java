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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import megamek.common.board.Coords;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FightState;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.UnitOrderAction;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * How lances on a route react to coming under fire (HammerGS, 2026-09-27): tells who was hit last turn by any enemy
 * fire, breaks a lance set to Break and fight off its route, holds it once the fight is over, and on the Resume order
 * skips the waypoints it fought past. Part of {@link UnitOrdersFollower}.
 */
class FireReaction {

    private static final MMLogger LOGGER = MMLogger.create(FireReaction.class);

    private final Princess owner;
    private final UnitOrdersFollower follower;

    // each unit's armor and structure at the start of this round and the one before, to tell who was hit in between
    private final Map<Integer, Integer> healthThisRound = new HashMap<>();
    private final Map<Integer, Integer> healthLastRound = new HashMap<>();
    private int healthRound = -1;
    // units holding after a fight, awaiting the Resume order; on Resume they skip the waypoints they fought past
    private final Set<Integer> awaitingResume = new HashSet<>();

    /**
     * @param owner    the bot whose units follow orders
     * @param follower the orders follower this works for
     */
    FireReaction(Princess owner, UnitOrdersFollower follower) {
        this.owner = owner;
        this.follower = follower;
    }

    private int currentRound() {
        return follower.currentRound();
    }

    /**
     * Snapshots every one of the bot's units' armor and structure the first time it is asked in a round, keeping the
     * round before, so {@link #wasHitLastTurn} can tell who was hit in between - by any enemy fire, artillery and
     * unseen shooters included.
     */
    private void recordHealth() {
        if (healthRound == currentRound()) {
            return;
        }
        healthLastRound.clear();
        healthLastRound.putAll(healthThisRound);
        healthThisRound.clear();
        for (Entity unit : owner.getEntitiesOwned()) {
            healthThisRound.put(unit.getId(), unit.getTotalArmor() + unit.getTotalInternal());
        }
        healthRound = currentRound();
    }

    /**
     * @param entity a unit of the bot
     *
     * @return {@code true} if the unit lost armor or structure between the start of last round and this one
     */
    boolean wasHitLastTurn(Entity entity) {
        recordHealth();
        Integer before = healthLastRound.get(entity.getId());
        Integer now = healthThisRound.get(entity.getId());
        return (before != null) && (now != null) && (now < before);
    }

    /**
     * @param leader a formation's leader
     *
     * @return {@code true} if any unit of its formation was hit last turn: the lance reacts together when one of it
     *       comes under fire
     */
    boolean isLanceHit(Entity leader) {
        Optional<FormationOrder> formation = leader.getUnitOrders().getFormation();
        if (formation.isEmpty()) {
            return wasHitLastTurn(leader);
        }
        for (Entity member : follower.roster().formationMembers(leader, formation.get().getLeaderId())) {
            if (wasHitLastTurn(member)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param leader a formation's leader
     *
     * @return {@code true} if the lance has broken off its route to fight, or holds after the fight
     */
    static boolean isBrokenToFight(Entity leader) {
        return leader.getUnitOrders().getFightState().isPresent();
    }

    /**
     * What each lance does on the leg it is on when it comes under fire (HammerGS, 2026-09-27). Set to Break and fight,
     * a lance hit last turn leaves its route to fight its attackers; once it has gone a full turn without being hit it
     * holds where it is, keeping its route, and calls for orders. Push through and Turn and fire keep it on its route.
     */
    void reactToFire() {
        for (Entity leader : owner.getEntitiesOwned()) {
            if ((leader.getPosition() == null) || !follower.isLeadingFormationOnRoute(leader)) {
                continue;
            }
            Optional<FormationOrder> formation = follower.activeFormation(leader);
            Optional<FightState> fightState = leader.getUnitOrders().getFightState();
            List<Entity> members = follower.roster().formationMembers(leader,
                  leader.getUnitOrders().getFormation().get().getLeaderId());
            boolean isHit = isLanceHit(leader);
            boolean isBreakAndFight = formation.isPresent() && (formation.get().getContactRule() == ContactRule.BREAK);
            if (fightState.isEmpty() && isBreakAndFight && isHit && follower.isConvoy(leader)) {
                // a convoy pushes on whatever its legs are set to: guarding it is its escorts' job (HammerGS,
                // 2026-10-02)
                LOGGER.info("[BotOrders] {} (ID {}) round {}: CONVOY_PUSHES_ON - the convoy was hit; a convoy never "
                      + "breaks off to fight", leader.getDisplayName(), leader.getId(), currentRound());
                continue;
            }
            if (fightState.isEmpty() && isBreakAndFight && isHit) {
                LOGGER.info("[BotOrders] {} (ID {}) round {}: UNDER_FIRE - the lance was hit; breaking off the route "
                      + "to fight", leader.getDisplayName(), leader.getId(), currentRound());
                for (Entity member : members) {
                    follower.change(member, UnitOrderAction.BREAK_TO_FIGHT);
                }
                owner.getOrdersRadio().report(leader, OrdersRadio.RadioEvent.BREAKING,
                      follower.navLabel(leader, leader.getUnitOrders().getRoute().get(0)));
            } else if ((fightState.orElse(null) == FightState.FIGHTING) && !isHit) {
                LOGGER.info("[BotOrders] {} (ID {}) round {}: FIGHT_OVER - no hits for a turn; holding for the Resume "
                      + "order", leader.getDisplayName(), leader.getId(), currentRound());
                for (Entity member : members) {
                    follower.change(member, UnitOrderAction.FIGHT_OVER);
                    awaitingResume.add(member.getId());
                }
                owner.getOrdersRadio().report(leader, OrdersRadio.RadioEvent.CONTACT_BROKEN,
                      leader.getPosition().getBoardNum());
            } else if (fightState.isPresent()) {
                LOGGER.debug("[BotOrders] {} (ID {}) round {}: lance {}", leader.getDisplayName(), leader.getId(),
                      currentRound(), fightState.get());
            }
        }
    }

    /**
     * On the first update after the Resume order that ends a hold after a fight, skips the waypoints the unit fought
     * past.
     *
     * @param entity a unit of the bot on the board, not fighting
     */
    void resumeAfterFight(Entity entity) {
        if (awaitingResume.remove(entity.getId())) {
            skipWaypointsFoughtPast(entity);
        }
    }

    /**
     * After the Resume order that ends a hold after a fight, drops the waypoints the unit is already past - ones that
     * lie further from the next waypoint than the unit does - so it goes on rather than back.
     */
    private void skipWaypointsFoughtPast(Entity entity) {
        List<Coords> route = entity.getUnitOrders().getRoute();
        while ((route.size() > 1)
              && (entity.getPosition().distance(route.get(1)) < route.get(0).distance(route.get(1)))) {
            LOGGER.info("[BotOrders] {} (ID {}) round {}: resuming past {} - already beyond it after the fight",
                  entity.getDisplayName(), entity.getId(), currentRound(), route.get(0).getBoardNum());
            follower.change(entity, UnitOrderAction.SKIP);
            route = entity.getUnitOrders().getRoute();
        }
    }
}
