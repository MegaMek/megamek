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
import megamek.common.units.Entity;
import megamek.common.units.TemporarySkillModifiers;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.commands.arguments.UnitArgument;
import megamek.server.totalWarfare.TWGameManager;

/**
 * The gamemaster command "/skillMod" that gives a unit's crew a temporary skill change: a delta to gunnery and
 * piloting, and a delta to the unit's individual initiative roll, lasting a set number of rounds or until cleared.
 * <p>
 * Skill deltas are added to the skill number, so a negative delta improves the crew (a -1 makes a 4 gunner a 3) and
 * a positive delta worsens it; the initiative delta is added to the roll, where a positive delta is the improvement. A
 * duration of {@link TemporarySkillModifiers#PERMANENT} lasts until cleared, and a duration of 0 clears whatever is
 * active. The stored skills are never touched, so the change reverses itself completely when it expires.
 * </p>
 */
public class SkillModifierCommand extends GamemasterServerCommand {

    public static final String UNIT_ID = "unitID";
    public static final String GUNNERY = "gunnery";
    public static final String PILOTING = "piloting";
    public static final String INITIATIVE = "init";
    public static final String ROUNDS = "rounds";

    /** The deltas a gamemaster may set, wide enough to swing any skill across its whole range. */
    private static final int MAX_DELTA = 8;
    private static final int MAX_ROUNDS = 100;

    public SkillModifierCommand(Server server, TWGameManager gameManager) {
        super(server,
              gameManager,
              "skillMod",
              Messages.getString("Gamemaster.cmd.skillMod.help"),
              Messages.getString("Gamemaster.cmd.skillMod.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
              new UnitArgument(UNIT_ID, Messages.getString("Gamemaster.cmd.skillMod.unitID")),
              new IntegerArgument(GUNNERY, Messages.getString("Gamemaster.cmd.skillMod.gunnery"),
                    -MAX_DELTA, MAX_DELTA, 0),
              new IntegerArgument(PILOTING, Messages.getString("Gamemaster.cmd.skillMod.piloting"),
                    -MAX_DELTA, MAX_DELTA, 0),
              new IntegerArgument(INITIATIVE, Messages.getString("Gamemaster.cmd.skillMod.init"),
                    -MAX_DELTA, MAX_DELTA, 0),
              new IntegerArgument(ROUNDS, Messages.getString("Gamemaster.cmd.skillMod.rounds"),
                    TemporarySkillModifiers.PERMANENT, MAX_ROUNDS, TemporarySkillModifiers.PERMANENT));
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        int unitId = (int) args.get(UNIT_ID).getValue();
        int gunneryDelta = (int) args.get(GUNNERY).getValue();
        int pilotingDelta = (int) args.get(PILOTING).getValue();
        int initiativeDelta = (int) args.get(INITIATIVE).getValue();
        int rounds = (int) args.get(ROUNDS).getValue();

        Entity entity = gameManager.getGame().getEntity(unitId);
        if ((entity == null) || (entity.getCrew() == null)) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.skillMod.unitNotFound"));
            return;
        }

        boolean clearing = (rounds == 0) || ((gunneryDelta == 0) && (pilotingDelta == 0) && (initiativeDelta == 0));
        if (clearing) {
            entity.getCrew().getSkillModifiers().clear();
            server.sendServerChat(Messages.getString("Gamemaster.cmd.skillMod.cleared", entity.getDisplayName()));
        } else {
            entity.getCrew().getSkillModifiers().set(gunneryDelta, pilotingDelta, initiativeDelta, rounds);
            String duration = (rounds == TemporarySkillModifiers.PERMANENT)
                  ? Messages.getString("Gamemaster.cmd.skillMod.untilCleared")
                  : Messages.getString("Gamemaster.cmd.skillMod.forRounds", rounds);
            server.sendServerChat(Messages.getString("Gamemaster.cmd.skillMod.success",
                  entity.getDisplayName(), signed(gunneryDelta), signed(pilotingDelta), signed(initiativeDelta),
                  duration));
        }
        gameManager.entityUpdate(entity.getId());
    }

    /** @return the delta with an explicit sign, so a bonus and a malus read apart in the announcement */
    private static String signed(int delta) {
        return (delta > 0) ? "+" + delta : String.valueOf(delta);
    }
}
