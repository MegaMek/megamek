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
package megamek.server.commands.arguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import megamek.client.ui.Messages;
import megamek.common.board.Coords;
import megamek.common.orders.WaypointOrder;

/**
 * An optional route: hexes separated by {@code -}, each optionally followed by what the unit does there - a facing
 * (N, NE, SE, S, SW, NW, or A for the bot's choice) and a number of turns to hold, each after a {@code /}.
 *
 * <p>Example: {@code 1709-1706/NE/2-2005-2204/N} - pass 1709, hold two turns at 1706 facing northeast, pass 2005, end
 * at 2204 facing north. A plain hex list such as {@code 1508-1504} still reads as before.</p>
 */
public class RouteArgument extends Argument<List<RouteArgument.RouteStep>> {

    /**
     * One waypoint of a parsed route. Only used while reading a command, never saved.
     *
     * @param hex   the waypoint
     * @param order what the unit does there
     */
    public record RouteStep(Coords hex, WaypointOrder order) {}

    public RouteArgument(String name, String description) {
        super(name, description);
    }

    @Override
    public void parse(String input) throws IllegalArgumentException {
        if (input == null) {
            value = Collections.emptyList();
            return;
        }
        List<RouteStep> steps = new ArrayList<>();
        for (String waypoint : input.split("-")) {
            String[] parts = waypoint.split("/");
            Coords hex;
            try {
                hex = Coords.parseHexNumber(parts[0]);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(getName() + " must be hex numbers, such as 1709-1706/NE/2.");
            }
            if ((hex.getX() < 0) || (hex.getY() < 0)) {
                throw new IllegalArgumentException(getName() + " must be a positive coordinate.");
            }
            steps.add(new RouteStep(hex, WaypointOrder.parse(Arrays.asList(parts).subList(1, parts.length))));
        }
        value = steps;
    }

    /**
     * @return the route's hexes, in order
     */
    public List<Coords> getHexes() {
        List<Coords> hexes = new ArrayList<>();
        for (RouteStep step : value) {
            hexes.add(step.hex());
        }
        return hexes;
    }

    /**
     * @return what the unit does at each hex, in the same order
     */
    public List<WaypointOrder> getWaypointOrders() {
        List<WaypointOrder> orders = new ArrayList<>();
        for (RouteStep step : value) {
            orders.add(step.order());
        }
        return orders;
    }

    @Override
    public String getHelp() {
        return getDescription() + " " + Messages.getString("Gamemaster.cmd.params.optional");
    }
}
