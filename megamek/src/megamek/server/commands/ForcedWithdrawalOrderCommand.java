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
package megamek.server.commands;

import java.util.List;

import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.enums.ForcedWithdrawalOrder;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.EnumArgument;
import megamek.server.commands.arguments.UnitArgument;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Gives a bot-controlled unit a standing Forced Withdrawal order: follow the bot's own rules, withdraw now whatever its
 * condition, or fight to the death even when crippled. The Edit Damage dialog sends it; a gamemaster can also type it.
 * The unit's bot reads the order from the unit, so it takes effect on the bot's next decision.
 */
public class ForcedWithdrawalOrderCommand extends GamemasterServerCommand {

    private static final MMLogger LOGGER = MMLogger.create(ForcedWithdrawalOrderCommand.class);

    public static final String UNIT_ID = "unitID";
    public static final String ORDER = "order";

    public ForcedWithdrawalOrderCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "withdrawOrder", Messages.getString("Gamemaster.cmd.withdrawOrder.help"),
              Messages.getString("Gamemaster.cmd.withdrawOrder.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(new UnitArgument(UNIT_ID, Messages.getString("Gamemaster.cmd.withdrawOrder.unitID")),
              new EnumArgument<>(ORDER, Messages.getString("Gamemaster.cmd.withdrawOrder.order"),
                    ForcedWithdrawalOrder.class));
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        int unitId = (int) args.get(UNIT_ID).getValue();
        ForcedWithdrawalOrder order = (ForcedWithdrawalOrder) args.get(ORDER).getValue();
        Entity entity = gameManager.getGame().getEntity(unitId);
        if (entity == null) {
            LOGGER.debug("[ForcedWithdrawal] Order {} refused: no unit {}", order, unitId);
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.missingUnit"));
            return;
        }
        Player owner = entity.getOwner();
        if ((owner == null) || !owner.isBot()) {
            LOGGER.debug("[ForcedWithdrawal] Order {} refused: {} is not controlled by a bot", order,
                  entity.getDisplayName());
            server.sendServerChat(connId,
                  Messages.getString("Gamemaster.cmd.withdrawOrder.notBot", entity.getDisplayName()));
            return;
        }
        entity.setForcedWithdrawalOrder(order);
        LOGGER.info("[ForcedWithdrawal] {} given order {}", entity.getDisplayName(), order);
        server.sendServerChat(Messages.getString("Gamemaster.cmd.withdrawOrder.success", entity.getDisplayName(),
              Messages.getString("ForcedWithdrawalOrder." + order.name())));
        gameManager.entityUpdate(entity.getId());
    }
}
