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
package megamek.common.orders;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;

/**
 * Groups bot units by the route they follow, the way a player sees their orders: the units of one formation share
 * their leader's route, and units sent along identical hexes share it too. The map's waypoint flags and the menu on
 * them both group routes this way, so a flag and its menu always speak for the same units.
 */
public final class RouteGroups {

    /**
     * Units following one route.
     *
     * @param guide the unit whose route the group follows: a formation's leader, else the first unit
     * @param units every unit of the group, in id order
     */
    public record RouteGroup(Entity guide, List<Entity> units) {

        /**
         * @return who follows the route, e.g. {@code GHR-5H +3} for a Grasshopper and three others
         */
        public String label() {
            String model = guide.getModel();
            String name = ((model == null) || model.isBlank()) ? guide.getChassis() : model;
            return name + ((units.size() > 1) ? (" +" + (units.size() - 1)) : "");
        }

        /**
         * @return the ids of the group's units
         */
        public List<Integer> unitIds() {
            List<Integer> unitIds = new ArrayList<>();
            for (Entity unit : units) {
                unitIds.add(unit.getId());
            }
            return unitIds;
        }
    }

    private RouteGroups() {
    }

    /**
     * @param units  every unit in the game, in id order
     * @param viewer the player looking, or {@code null} for none
     *
     * @return the route groups of the bot units on the viewer's side that have a route; enemy bots' routes are never
     *       shown
     */
    public static List<RouteGroup> visibleTo(List<Entity> units, @Nullable Player viewer) {
        Map<String, List<Entity>> groups = new LinkedHashMap<>();
        for (Entity unit : units) {
            if (isRouteShownTo(unit, viewer)) {
                groups.computeIfAbsent(groupKey(unit), key -> new ArrayList<>()).add(unit);
            }
        }
        List<RouteGroup> routeGroups = new ArrayList<>();
        for (List<Entity> group : groups.values()) {
            routeGroups.add(new RouteGroup(routeGuide(group), group));
        }
        return routeGroups;
    }

    private static boolean isRouteShownTo(Entity unit, @Nullable Player viewer) {
        Player owner = unit.getOwner();
        return (viewer != null) && (owner != null) && owner.isBot() && !viewer.isEnemyOf(owner)
              && (unit.getPosition() != null) && unit.getUnitOrders().hasRoute();
    }

    private static String groupKey(Entity unit) {
        Optional<FormationOrder> formation = unit.getUnitOrders().getFormation();
        if (formation.isPresent()) {
            return "formation " + formation.get().getLeaderId();
        }
        return "route " + unit.getBoardId() + ' ' + unit.getUnitOrders().getRoute();
    }

    private static Entity routeGuide(List<Entity> group) {
        Optional<FormationOrder> formation = group.get(0).getUnitOrders().getFormation();
        if (formation.isPresent()) {
            for (Entity member : group) {
                if (member.getId() == formation.get().getLeaderId()) {
                    return member;
                }
            }
        }
        return group.get(0);
    }
}
