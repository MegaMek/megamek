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
package megamek.client.bot.princess.commands;

import java.util.List;

import megamek.client.bot.Messages;
import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.BooleanArgument;

/**
 * Turns the bot's radio chatter on or off: with it on, the bot reports on its units' orders with short radio calls
 * and callsigns; with it off, it uses plain replies.
 */
public class RadioChatterCommand implements ChatCommand {
    private static final String ON = "on";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(new BooleanArgument(ON, Messages.getString("Princess.command.radioChatter.on"), true));
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
        boolean isOn = arguments.get(ON, BooleanArgument.class).getValue();
        princess.getOrdersRadio().setRadioChatter(isOn);
        princess.sendChat(Messages.getString(isOn ? "Princess.command.radioChatter.enabled"
              : "Princess.command.radioChatter.disabled"));
    }
}
