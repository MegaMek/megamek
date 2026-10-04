/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.Princess;
import megamek.logging.MMLogger;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.OptionalIntegerArgument;
import megamek.server.commands.arguments.UnitArgument;

/**
 * Command to set a priority target unit for the bot, optionally with how much it is wanted (1 the most, 5 the least).
 * Without a priority, a new target gets the default and a target already on the list keeps its priority.
 *
 * @author Luana Coppio
 */
public class PriorityTargetCommand implements ChatCommand {
    private static final MMLogger LOGGER = MMLogger.create(PriorityTargetCommand.class);
    private static final String UNIT_ID = "unitID";
    private static final String PRIORITY = "priority";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
              new UnitArgument(UNIT_ID, Messages.getString("Princess.command.priorityTarget.unitID")),
              new OptionalIntegerArgument(PRIORITY, Messages.getString("Princess.command.priorityTarget.priority"),
                    BehaviorSettings.HIGHEST_TARGET_PRIORITY, BehaviorSettings.LOWEST_TARGET_PRIORITY)
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
        int unitId = arguments.get(UNIT_ID, UnitArgument.class).getValue();
        var priorityArgument = arguments.get(PRIORITY, OptionalIntegerArgument.class).getValue();
        BehaviorSettings behaviorSettings = princess.getBehaviorSettings();
        if (priorityArgument.isPresent()) {
            behaviorSettings.addPriorityUnit(unitId, priorityArgument.get());
        } else {
            behaviorSettings.addPriorityUnit(unitId);
        }
        int priority = behaviorSettings.getPriorityUnitLevel(unitId);
        LOGGER.info("{}: unit {} is a priority target at priority {} ({})", princess.getLocalPlayer().getName(),
              unitId, priority, priorityArgument.isPresent() ? "priority given" : "no priority given");
        princess.sendChat(Messages.getString("Princess.command.priorityTarget.success", unitId, priority));
    }
}
