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

import megamek.client.bot.Messages;
import megamek.client.bot.princess.Princess;
import megamek.common.equipment.GunEmplacement;
import megamek.common.game.InGameObject;
import megamek.common.units.BuildingEntity;
import megamek.server.commands.arguments.Arguments;

/**
 * Command to ignore all turrets in the game
 *
 * @author Luana Coppio
 */
public class IgnoreTurretsCommand implements ChatCommand {

    @Override
    public void execute(Princess princess, Arguments arguments) {
        princess.getGame()
                .getInGameObjects()
                .stream()
                .filter(e -> e instanceof GunEmplacement || e instanceof BuildingEntity)
                .map(InGameObject::getId)
              .forEach(i -> princess.getBehaviorSettings().addIgnoredUnitTarget(i));
        princess.sendChat(Messages.getString("Princess.command.ignoreTurrets.ignoringAllTurrets"));
    }
}
