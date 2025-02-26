/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.bot.princess.commands;

import megamek.client.bot.Messages;
import megamek.client.bot.princess.ArtilleryCommandAndControl;
import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.*;

import java.util.List;

/**
 * Command to ignore a target unit for the bot.
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
        if (!artilleryOrder.equals(ArtilleryCommandAndControl.ArtilleryOrder.AUTO) && !artilleryOrder.equals(ArtilleryCommandAndControl.ArtilleryOrder.HALT)) {
            if (multiHexNumberArgument.getValue().isEmpty()) {
                princess.sendChat(Messages.getString("Princess.command.artillery.noTargets"));
                return;
            }
            artilleryCommandAndControl.addArtilleryTargets(multiHexNumberArgument.getValue());
        }
        artilleryCommandAndControl.setArtilleryOrder(artilleryOrder, specialAmmo);
    }
}
