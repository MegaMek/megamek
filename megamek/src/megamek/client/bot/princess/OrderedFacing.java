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

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.UnitOrders;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Which way a unit on orders faces (HammerGS, 2026-09-27): the facing a player set on a waypoint or for the route, the
 * way along the route toward the next flag, an escort facing its convoy's way, how far a torso twist or turret may
 * cover the rest, and when the facing gives way to a threat behind. Part of {@link UnitOrdersFollower}.
 */
class OrderedFacing {

    private static final MMLogger LOGGER = MMLogger.create(OrderedFacing.class);

    private final Princess owner;
    private final UnitOrdersFollower follower;

    /** Hex facings on each side of the ordered one that still count as its front arc. */
    private static final int FRONT_ARC_HALF_WIDTH = 1;

    /** How many hexsides from a unit's facing its rear arc lies: straight behind. */
    private static final int REAR_SIDES_APART = 3;

    /**
     * @param owner    the bot whose units follow orders
     * @param follower the orders follower this works for
     */
    OrderedFacing(Princess owner, UnitOrdersFollower follower) {
        this.owner = owner;
        this.follower = follower;
    }

    /**
     * The facing the player ordered for the end of this move, or {@link UnitOrders#FACING_AUTO}. A move that ends at
     * the last waypoint, or a unit holding, takes the "when stopped" facing; any other move takes the "while moving"
     * facing.
     *
     * @param entity    the unit
     * @param finalHex  where the move ends
     *
     * @return the ordered facing 0-5, or {@link UnitOrders#FACING_AUTO}
     */
    int orderedFacing(Entity entity, Coords finalHex) {
        int facing = playerOrderedFacing(entity, finalHex);
        if (facing == UnitOrders.FACING_AUTO) {
            // an escort faces the way its convoy is going, toward the convoy's next waypoint, as the convoy itself
            // does (HammerGS, 2026-10-03)
            OptionalInt escortFacing = follower.convoyEscorts().escortFacing(entity);
            if (escortFacing.isPresent()) {
                return escortFacing.getAsInt();
            }
        }
        if ((facing == UnitOrders.FACING_AUTO) && entity.getUnitOrders().hasRoute()) {
            // a waypoint with no facing set faces toward the next flag, enemies or not: the player has a plan for
            // which way the units go, and a unit turning round to look behind it broke it (HammerGS, 2026-09-27).
            // At the end of the route the unit faces the way it came.
            int alongRoute = facingAlongRoute(entity, finalHex);
            if ((alongRoute == UnitOrders.FACING_AUTO) && (entity.getPosition() != null)
                  && !entity.getPosition().equals(finalHex)) {
                return entity.getPosition().direction(finalHex);
            }
            return alongRoute;
        }
        if ((facing == UnitOrders.FACING_AUTO) && owner.getEnemyEntities().isEmpty()) {
            // with no enemy to face, Auto faces along the route rather than wherever the move happens to end
            return facingAlongRoute(entity, finalHex);
        }
        return facing;
    }

    /**
     * @return {@code true} if the unit's leg is set to Turn and fire and its lance was hit last turn
     */
    private boolean isTurningToFire(Entity entity) {
        Optional<FormationOrder> formation = follower.activeFormation(entity);
        if (formation.isEmpty() || (formation.get().getContactRule() != ContactRule.TURN_AND_FIRE)) {
            return false;
        }
        Entity leader = owner.getGame().getEntity(formation.get().getLeaderId());
        return follower.fireReaction().isLanceHit((leader == null) ? entity : leader);
    }

    /**
     * Decides whether an ordered facing stands against the fire the bot expects. A unit on its way along a route keeps
     * the route's facing whatever is behind or beside it, and twists its torso or turret onto what it can reach in the
     * fire phase (HammerGS, 2026-09-27); anywhere else - holding the end of its route, or with no route - the usual
     * rule applies, see {@link #facingThatStands}.
     *
     * @param entity        the unit
     * @param orderedFacing the ordered facing 0-5, or {@link UnitOrders#FACING_AUTO}
     * @param position      where the unit ends its move
     * @param threat        where the bot expects fire from, or {@code null} when it knows of no enemy
     *
     * @return the ordered facing if it stands, otherwise {@link UnitOrders#FACING_AUTO}
     */
    public int facingThatStandsFor(Entity entity, int orderedFacing, Coords position, @Nullable Coords threat) {
        // an escort keeping its place round a moving convoy is on its way too
        boolean isOnItsWay = (entity.getUnitOrders().hasRoute() && !follower.isAtRouteEnd(entity))
              || follower.convoyEscorts().isEscorting(entity);
        if (isOnItsWay && (orderedFacing != UnitOrders.FACING_AUTO) && isTurningToFire(entity)) {
            // Turn and fire: hit last turn, the unit turns to bring its attackers into its front arc
            return facingThatStands(orderedFacing, position, threat);
        }
        if (isOnItsWay && (orderedFacing != UnitOrders.FACING_AUTO)) {
            if (isInRearArc(orderedFacing, position, threat)) {
                // never the rear arc into the line of fire (HammerGS, 2026-09-27): a threat squarely behind the route
                // facing turns the unit as the bot would; one off to a side leaves the route facing as it is
                // asked for every move scored: trace only
                LOGGER.trace("[BotOrders] {} (ID {}): the threat at {} would be behind facing {}; turning to it",
                      entity.getDisplayName(), entity.getId(), threat.getBoardNum(), orderedFacing);
                return UnitOrders.FACING_AUTO;
            }
            LOGGER.trace("[BotOrders] {} (ID {}): keeps the route facing {} on its way", entity.getDisplayName(),
                  entity.getId(), orderedFacing);
            return orderedFacing;
        }
        return facingThatStands(orderedFacing, position, threat);
    }

    /**
     * @return {@code true} if the threat lies straight behind the facing, in the unit's rear arc
     */
    static boolean isInRearArc(int facing, Coords position, @Nullable Coords threat) {
        if ((threat == null) || threat.equals(position)) {
            return false;
        }
        return sidesApart(facing, position.direction(threat)) == REAR_SIDES_APART;
    }

    /**
     * The facing a player set for the end of this move - on the waypoint it ends on, for the end of the route, or
     * while moving - without the bot's own choices.
     *
     * @param entity   the unit
     * @param finalHex where the move ends
     *
     * @return the facing 0-5, or {@link UnitOrders#FACING_AUTO} when the player left it to the bot
     */
    int playerOrderedFacing(Entity entity, Coords finalHex) {
        UnitOrders orders = entity.getUnitOrders();
        List<Coords> route = orders.getRoute();
        // a move that ends on the next waypoint takes the facing set on it
        if (!route.isEmpty() && finalHex.equals(route.get(0))
              && (orders.getWaypointOrder(0).getFacing() != UnitOrders.FACING_AUTO)) {
            return orders.getWaypointOrder(0).getFacing();
        }
        boolean endsStopped = route.isEmpty()
              || ((route.size() == 1) && (finalHex.distance(route.get(0)) <= Princess.DISTANCE_TO_WAYPOINT));
        return endsStopped ? follower.stoppedFacing(entity) : orders.getFacingWhileMoving();
    }

    /**
     * How many hexsides a unit can turn its weapons without turning its legs: one for a torso twist, more for an
     * extended twist, three - any way at all - for a turret; none for a unit that cannot. Turning its weapons is free,
     * where turning in place spends movement and counts as having moved.
     *
     * @param entity the unit
     *
     * @return the hexsides either side of its facing that its torso or turret can reach, 0-3
     */
    static int twistReach(Entity entity) {
        if (!entity.canChangeSecondaryFacing()) {
            return 0;
        }
        int reach = 0;
        for (int sides = 1; sides <= 3; sides++) {
            boolean canReach = entity.isValidSecondaryFacing((entity.getFacing() + sides) % 6)
                  && entity.isValidSecondaryFacing((entity.getFacing() + 6 - sides) % 6);
            if (!canReach) {
                break;
            }
            reach = sides;
        }
        return reach;
    }

    /**
     * How many hexsides of an ordered facing a move may leave to a torso twist or turret. A unit stopping - holding,
     * or ending its route - may: turning in place would spend movement for nothing its weapons cannot already reach.
     * A unit on its way along a route faces it with its legs, so it walks on toward the next flag; ending a move one
     * side off because the twist would cover it left units side-on to the way ahead (HammerGS's playtest,
     * 2026-09-27).
     *
     * @param entity   the unit
     * @param finalHex where the move ends
     *
     * @return the hexsides the twist may cover, 0 on the way along a route
     */
    int twistAllowance(Entity entity, Coords finalHex) {
        UnitOrders orders = entity.getUnitOrders();
        List<Coords> route = orders.getRoute();
        if (follower.convoyEscorts().isEscorting(entity)) {
            // an escort moves with its convoy and faces the convoy's way with its legs, as the convoy does
            return 0;
        }
        if (route.isEmpty()) {
            return twistReach(entity);
        }
        boolean endsRoute = (route.size() == 1) && (finalHex.distance(route.get(0)) <= Princess.DISTANCE_TO_WAYPOINT);
        boolean stopsForHold = orders.getWaypointOrder(0).isHold() && finalHex.equals(route.get(0));
        return (endsRoute || stopsForHold || follower.isHolding(entity)) ? twistReach(entity) : 0;
    }

    /**
     * @param fromFacing the facing turned from, 0-5
     * @param toFacing   the facing turned to, 0-5
     *
     * @return how many hexsides apart the two are, 0-3
     */
    static int sidesApart(int fromFacing, int toFacing) {
        int sides = Math.abs(fromFacing - toFacing) % 6;
        return Math.min(sides, 6 - sides);
    }

    /**
     * The way a unit should twist its torso or turret this fire phase to face the way a player ordered, when it has
     * nothing better to aim at.
     *
     * @param entity a unit of the bot
     *
     * @return the facing to twist to, 0-5, or {@link UnitOrders#FACING_AUTO} when there is no order it can reach
     */
    public int orderedTwist(Entity entity) {
        if (entity.getPosition() == null) {
            return UnitOrders.FACING_AUTO;
        }
        int ordered = follower.isHolding(entity) ? follower.stoppedFacing(entity)
              : playerOrderedFacing(entity, entity.getPosition());
        boolean canReach = (ordered != UnitOrders.FACING_AUTO) && (ordered != entity.getSecondaryFacing())
              && entity.canChangeSecondaryFacing() && entity.isValidSecondaryFacing(ordered);
        return canReach ? ordered : UnitOrders.FACING_AUTO;
    }

    /**
     * @return the direction from the hex toward the next flag of the route - the leader's, for a unit in formation,
     *       so the lance faces the same way rather than toward each unit's own slot - or the flag after it when the
     *       move ends on it; {@link UnitOrders#FACING_AUTO} when there is none
     */
    private int facingAlongRoute(Entity entity, Coords finalHex) {
        Optional<Coords> townSpot = follower.townLegs().townSpotOf(entity);
        if (townSpot.isPresent() && !townSpot.get().equals(finalHex)) {
            // through a town each unit faces the way its own street goes, not the formation's heading
            return wayOnToward(entity, finalHex, townSpot.get());
        }
        int formationFacing = follower.slots().formationFacing(entity);
        if (formationFacing != UnitOrders.FACING_AUTO) {
            return formationFacing;
        }
        Entity routeOwner = follower.roster().formationLeaderOf(entity).orElse(entity);
        List<Coords> route = routeOwner.getUnitOrders().getRoute();
        if (route.isEmpty()) {
            return UnitOrders.FACING_AUTO;
        }
        Coords next = route.get(0);
        // once the leader stands on its flag, the lance looks on to the flag after it; followers waiting in their
        // slots faced the flag they were waiting at, turning their backs on the way ahead (HammerGS, 2026-09-27)
        boolean isAtFlag = next.equals(finalHex) || next.equals(routeOwner.getPosition());
        if (isAtFlag) {
            if (route.size() < 2) {
                return UnitOrders.FACING_AUTO;
            }
            next = route.get(1);
        }
        return next.equals(finalHex) ? UnitOrders.FACING_AUTO : wayOnToward(entity, finalHex, next);
    }

    /**
     * The way on toward a flag from a hex: the side leading to the neighbour from which the flag is cheapest to reach
     * by the unit's route, which bends round buildings and water. Facing the flag in a straight line put a leader
     * rounding a building block side-on to the way it had to go, and with 3 MP it could not both step round the corner
     * and turn back to the flag, so it rocked between two hexes beside the block for ten rounds (HammerGS's playtest,
     * 2026-09-27: the Stalker at 1415 and 1516, south of the buildings at 1514).
     *
     * <p>The turn away from the flag goes no further than keeps the flag in the unit's front arc: HammerGS faced each
     * Mek through the town "a mix of both" - the way it was going next, and toward the objective (2026-10-01). At 1415,
     * with the way round the buildings north-west and the flag at 1611 north-east, the unit faces north.</p>
     *
     * @return the facing 0-5; the straight line where the route cannot say, and where it does no better
     */
    private int wayOnToward(Entity entity, Coords finalHex, Coords flag) {
        int straightOn = finalHex.direction(flag);
        int bestCost = follower.routeCostFrom(entity, flag, finalHex);
        if (bestCost == WaypointDistanceField.UNREACHABLE) {
            return straightOn;
        }
        int bestDirection = straightOn;
        // the straight line first, so it keeps any tie
        for (int turn = 0; turn < 6; turn++) {
            int direction = (straightOn + turn) % 6;
            int cost = follower.routeCostFrom(entity, flag, finalHex.translated(direction));
            if (cost < bestCost) {
                bestCost = cost;
                bestDirection = direction;
            }
        }
        if (sidesApart(bestDirection, straightOn) <= 1) {
            return bestDirection;
        }
        // one hexside from the flag, on the side the route turns to
        int clockwise = (bestDirection - straightOn + 6) % 6;
        return (clockwise <= 3) ? ((straightOn + 1) % 6) : ((straightOn + 5) % 6);
    }

    /**
     * Decides whether the player's ordered facing stands, given where the bot expects fire from. The order stands
     * while that fire falls in the ordered facing's front arc; otherwise the bot turns to the threat, which protects
     * the unit's side and rear armor.
     *
     * @param orderedFacing the ordered facing 0-5, or {@link UnitOrders#FACING_AUTO}
     * @param position      where the unit ends its move
     * @param threat        where the bot expects fire from, or {@code null} when it knows of no enemy
     *
     * @return the ordered facing if it stands, otherwise {@link UnitOrders#FACING_AUTO}
     */
    static int facingThatStands(int orderedFacing, Coords position, @Nullable Coords threat) {
        if (orderedFacing == UnitOrders.FACING_AUTO) {
            return UnitOrders.FACING_AUTO;
        }
        if ((threat == null) || threat.equals(position)) {
            return orderedFacing;
        }
        int threatDirection = position.direction(threat);
        int sidesApart = Math.abs(threatDirection - orderedFacing) % 6;
        sidesApart = Math.min(sidesApart, 6 - sidesApart);
        return (sidesApart <= FRONT_ARC_HALF_WIDTH) ? orderedFacing : UnitOrders.FACING_AUTO;
    }
}
