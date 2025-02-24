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
package megamek.client.bot.princess;

import megamek.client.bot.Messages;
import megamek.client.bot.princess.commands.*;

/**
 * <p>Represents the commands that the princess can execute.</p>
 * <p>Each command has an abbreviation, a command, a syntax and a description.</p>
 * @author Deric Page (deric.page@nisc.coop) (ext 2335)
 * @since 10/24/2014 9:57 AM
 */
public enum ChatCommands {
    FLEE("fl", "flee",
        Messages.getString("Princess.command.flee.description"),
        new FleeCommand()),
    @Deprecated(since="50.04", forRemoval = true)
    VERBOSE("ve", "verbose",
        Messages.getString("Princess.command.verbose.description"),
        new VerboseCommand()),
    BEHAVIOR("be", "behavior",
        Messages.getString("Princess.command.behavior.description"),
        new BehaviorCommand()),
    CAUTION("ca",  "caution",
        Messages.getString("Princess.command.caution.description"),
        new CautionCommand()),
    AVOID("av", "avoid",
        Messages.getString("Princess.command.avoid.description"),
        new AvoidCommand()),
    AGGRESSION("ag", "aggression",
        Messages.getString("Princess.command.aggression.description"),
        new AggressionCommand()),
    HERDING("he", "herding",
        Messages.getString("Princess.command.herding.description"),
        new HerdingCommand()),
    BRAVERY("br", "bravery",
        Messages.getString("Princess.command.bravery.description"),
        new BraveryCommand()),
    TARGET("ta", "target",
        Messages.getString("Princess.command.targetGround.description"),
        new TargetGroundCommand()),
    PRIORITIZE("pr", "prioritize",
        Messages.getString("Princess.command.priorityTarget.description"),
        new PriorityTargetCommand()),
    SHOW_BEHAVIOR("sh", "show-behavior",
        Messages.getString("Princess.command.showBehavior.description"),
        new ShowBehaviorCommand()),
    LIST__COMMANDS("li", "list-commands",
        Messages.getString("Princess.command.listCommands.description"),
        new ListCommands()),
    IGNORE_TARGET("ig", "ignore-target",
        Messages.getString("Princess.command.ignoreTarget.description"),
        new IgnoreTargetCommand()),
    SHOW_DISHONORED("di", "show-dishonored",
        Messages.getString("Princess.command.showDishonored.description"),
        new ShowDishonoredCommand()),
    CLEAR_IGNORED_TARGETS("cl", "clear-ignored-targets",
        Messages.getString("Princess.command.clearIgnoredTargets.description"),
        new ClearIgnoredTargetsCommand()),
    BLOOD_FEUD("bf",  "blood-feud",
        Messages.getString("Princess.command.bloodFeud.description"),
        new BloodFeudCommand()),
    ADD_WAYPOINT("aw", "add-waypoint",
        Messages.getString("Princess.command.addWaypoint.description"),
        new AddWaypointCommand()),
    REMOVE_WAYPOINT("rw", "remove-waypoint",
        Messages.getString("Princess.command.removeWaypoint.description"),
        new RemoveWaypointCommand()),
    CLEAR_WAYPOINT("cw", "clear-waypoints",
        Messages.getString("Princess.command.clearWaypoints.description"),
        new ClearWaypointsCommand()),
    CLEAR_ALL_WAYPOINTS("nw", "clear-all-waypoints",
        Messages.getString("Princess.command.clearAllWaypoints.description"),
        new ClearAllWaypointsCommand()),
    SET_WAYPOINT("sw", "set-waypoints",
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
        return "princessName: " + getAbbreviation() + "/" + getCommand() +
            (chatCommand.defineArguments().isEmpty() ? "" : ": " + chatCommand.getArgumentsRepr());
    }

    public String getDescription() {
        return (chatCommand.defineArguments().isEmpty() ?
            "" : chatCommand.getArgumentsRepr() + " : " + chatCommand.getArgumentsDescription() + " ") + description;
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
