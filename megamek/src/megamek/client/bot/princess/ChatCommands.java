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
package megamek.client.bot.princess;

import megamek.client.bot.Messages;
import megamek.client.bot.princess.commands.*;

/**
 * <p>
 * Represents the commands that the princess can execute.
 * </p>
 * <p>
 * Each command has an abbreviation, a command, a syntax and a description.
 * </p>
 *
 * @author Deric Page (deric.page@nisc.coop) (ext 2335)
 * @since 10/24/2014 9:57 AM
 */
public enum ChatCommands {
    FLEE("fl", "flee", Messages.getString("Princess.command.flee.description"), new FleeCommand()),
    BEHAVIOR("be", "behavior", Messages.getString("Princess.command.behavior.description"), new BehaviorCommand()),
    CAUTION("ca", "caution", Messages.getString("Princess.command.caution.description"), new CautionCommand()),
    AVOID("av", "avoid", Messages.getString("Princess.command.avoid.description"), new AvoidCommand()),
    ARTILLERY("ar", "artillery", Messages.getString("Princess.command.artillery.description"), new ArtilleryCommand()),
    AGGRESSION("ag",
          "aggression",
          Messages.getString("Princess.command.aggression.description"),
          new AggressionCommand()),
    HERDING("he", "herding", Messages.getString("Princess.command.herding.description"), new HerdingCommand()),
    BRAVERY("br", "bravery", Messages.getString("Princess.command.bravery.description"), new BraveryCommand()),
    TARGET("ta", "target", Messages.getString("Princess.command.targetGround.description"), new TargetGroundCommand()),
    PRIORITIZE("pr",
          "prioritize",
          Messages.getString("Princess.command.priorityTarget.description"),
          new PriorityTargetCommand()),
    SHOW_BEHAVIOR("sh",
          "show-behavior",
          Messages.getString("Princess.command.showBehavior.description"),
          new ShowBehaviorCommand()),
    LIST__COMMANDS("li",
          "list-commands",
          Messages.getString("Princess.command.listCommands.description"),
          new ListCommands()),
    IGNORE_TARGET("ig",
          "ignore-target",
          Messages.getString("Princess.command.ignoreTarget.description"),
          new IgnoreTargetCommand()),
    IGNORE_PLAYER("ip",
          "ignore-player",
          Messages.getString("Princess.command.ignorePlayer.description"),
          new IgnorePlayerCommand()),
    IGNORE_TURRETS("it",
          "ignore-turrets",
          Messages.getString("Princess.command.ignoreTurrets.description"),
          new IgnoreTurretsCommand()),
    SHOW_DISHONORED("di",
          "show-dishonored",
          Messages.getString("Princess.command.showDishonored.description"),
          new ShowDishonoredCommand()),
    CLEAR_IGNORED_TARGETS("cl",
          "clear-ignored-targets",
          Messages.getString("Princess.command.clearIgnoredTargets.description"),
          new ClearIgnoredTargetsCommand()),
    BLOOD_FEUD("bf",
          "blood-feud",
          Messages.getString("Princess.command.bloodFeud.description"),
          new BloodFeudCommand()),
    ADD_WAYPOINT("aw",
          "add-waypoint",
          Messages.getString("Princess.command.addWaypoint.description"),
          new AddWaypointCommand()),
    REMOVE_WAYPOINT("rw",
          "remove-waypoint",
          Messages.getString("Princess.command.removeWaypoint.description"),
          new RemoveWaypointCommand()),
    CLEAR_WAYPOINT("cw",
          "clear-waypoints",
          Messages.getString("Princess.command.clearWaypoints.description"),
          new ClearWaypointsCommand()),
    CLEAR_ALL_WAYPOINTS("nw",
          "clear-all-waypoints",
          Messages.getString("Princess.command.clearAllWaypoints.description"),
          new ClearAllWaypointsCommand()),
    SET_WAYPOINT("sw",
          "set-waypoints",
          Messages.getString("Princess.command.setWaypoints.description"),
          new SetWaypointsCommand());

    private final String abbreviation;
    private final String command;
    private final String description;
    private final ChatCommand chatCommand;

    ChatCommands(String abbreviation, String command, String description, ChatCommand chatCommand) {
        this.abbreviation = abbreviation;
        this.command = command;
        this.description = description;
        this.chatCommand = chatCommand;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getSyntax() {
        return "princessName: " +
                     getAbbreviation() +
                     "/" +
                     getCommand() +
                     (chatCommand.defineArguments().isEmpty() ? "" : ": " + chatCommand.getArgumentsRepr());
    }

    public String getDescription() {
        return (chatCommand.defineArguments().isEmpty() ? "" : chatCommand.getArgumentsDescription() + " ") +
                     description;
    }

    public String getCommand() {
        return command;
    }

    public ChatCommand getChatCommand() {
        return chatCommand;
    }

    public static ChatCommands getByValue(String s) {
        for (ChatCommands cc : ChatCommands.values()) {
            if (cc.getAbbreviation().equals(s)) {
                return cc;
            }
        }

        return null;
    }
}
