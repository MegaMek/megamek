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
import megamek.client.bot.princess.ArtilleryCommandAndControl;
import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.EnumArgument;
import megamek.server.commands.arguments.NotRequiredMultiHexNumberArgument;

/**
 * Command to ignore a target unit for the bot.
 *
 * @author Luana Coppio
 */
public class ArtilleryCommand implements ChatCommand {
    private static final String ORDER = "order";
    private static final String AMMO = "ammo";
    private static final String TARGET = "targets";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
              new EnumArgument<>(ORDER,
                    Messages.getString("Princess.command.artillery.order"),
                    ArtilleryCommandAndControl.ArtilleryOrder.class),
              new EnumArgument<>(AMMO,
                    Messages.getString("Princess.command.artillery.ammo"),
                    ArtilleryCommandAndControl.SpecialAmmo.class,
                    ArtilleryCommandAndControl.SpecialAmmo.NONE),
              new NotRequiredMultiHexNumberArgument(TARGET,
                    Messages.getString("Princess.command.artillery.targetHex"))
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
        var artilleryOrder = arguments.getEnum(ORDER, ArtilleryCommandAndControl.ArtilleryOrder.class);
        var specialAmmo = arguments.getEnum(AMMO, ArtilleryCommandAndControl.SpecialAmmo.class);
        var multiHexNumberArgument = arguments.get(TARGET, NotRequiredMultiHexNumberArgument.class);
        var artilleryCommandAndControl = princess.getArtilleryCommandAndControl();

        artilleryCommandAndControl.reset();
        if (!artilleryOrder.equals(ArtilleryCommandAndControl.ArtilleryOrder.AUTO) && !artilleryOrder.equals(
              ArtilleryCommandAndControl.ArtilleryOrder.HALT)) {
            if (multiHexNumberArgument.getValue().isEmpty()) {
                princess.sendChat(Messages.getString("Princess.command.artillery.noTargets"));
                return;
            }
            artilleryCommandAndControl.addArtilleryTargets(multiHexNumberArgument.getValue());
        }
        artilleryCommandAndControl.setArtilleryOrder(artilleryOrder, specialAmmo);
    }
}
