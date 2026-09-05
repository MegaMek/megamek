/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess.commands;

import java.util.List;

import megamek.client.bot.Messages;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.EnumArgument;

/**
 * Command to set the bot to flee to a specific edge of the map. Ordering a flee to {@link CardinalEdge#NONE} cancels
 * a previous flee order.
 *
 * @author Luana Coppio
 */
public class FleeCommand implements ChatCommand {
    private static final String EDGE = "edge";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
              new EnumArgument<>(EDGE, Messages.getString("Princess.command.flee.edge"), CardinalEdge.class)
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
        CardinalEdge edge = arguments.getEnum(EDGE, CardinalEdge.class);
        if (edge == CardinalEdge.NONE) {
            cancelFleeOrder(princess);
        } else {
            issueFleeOrder(princess, edge);
        }
    }

    private void issueFleeOrder(Princess princess, CardinalEdge edge) {
        String reason = "Received flee order - " + edge.name();
        princess.sendChat(Messages.getString("Princess.command.flee.fleeing", edge.name()));
        princess.getBehaviorSettings().setDestinationEdge(edge);
        // Auto-flee plus a destination edge is what actually makes healthy units move toward the edge
        // (see UnitBehavior#calculateUnitBehavior), and the flee-board flag allows them to leave the map.
        princess.getBehaviorSettings().setAutoFlee(true);
        princess.setFallBack(true, reason);
        princess.setFleeBoard(true, reason);
    }

    private void cancelFleeOrder(Princess princess) {
        String reason = "Received cancel flee order";
        princess.sendChat(Messages.getString("Princess.command.flee.canceled"));
        princess.getBehaviorSettings().setDestinationEdge(CardinalEdge.NONE);
        princess.getBehaviorSettings().setAutoFlee(false);
        princess.setFallBack(false, reason);
        princess.setFleeBoard(false, reason);
    }
}
