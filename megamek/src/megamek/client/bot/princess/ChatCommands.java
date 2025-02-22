/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.bot.princess.commands.*;

/**
 * <p>Represents the commands that the princess can execute.</p>
 * <p>Each command has an abbreviation, a command, a syntax and a description.</p>
 * @author Deric Page (deric.page@nisc.coop) (ext 2335)
 * @since 10/24/2014 9:57 AM
 */
public enum ChatCommands {
    FLEE("fl", "flee",
        "Flee - Causes princess-controlled units to start fleeing the board, regardless of damage level or Forced Withdrawal setting.",
        new FleeCommand()),
    @Deprecated(since="50.04", forRemoval = true)
    VERBOSE("ve", "verbose",
        "Sets princess's verbosity level (unused, set for removal).",
        new VerboseCommand()),
    BEHAVIOR("be", "behavior",
        "Behavior - Change's princess's behavior to the named behavior.",
        new BehaviorCommand()),
    CAUTION("ca",  "caution",
        "Caution - Modifies princess's Piloting Caution setting. Each '+' increases it by 1 and each '-' decreases it by one. Or you can set it numerically from 0 to 10",
        new CautionCommand()),
    AVOID("av", "avoid",
        "Avoid - Modifies princess's Self Preservation setting. Each '+' increases it by 1 and each '-' decreases it by one. Or you can set it numerically from 0 to 10",
        new AvoidCommand()),
    AGGRESSION("ag", "aggression",
        "Aggression - Modifies princess's Aggression setting. Each '+' increases it by 1 and each '-' decreases it by one. Or you can set it numerically from 0 to 10",
        new AggressionCommand()),
    HERDING("he", "herding",
        "Herd - Modifies princess's Herding setting. Each '+' increases it by 1 and each '-' decreases it by one. Or you can set it numerically from 0 to 10",
        new HerdingCommand()),
    BRAVERY("br", "bravery",
        "Brave - Modifies princess's Bravery setting. Each '+' increases it by 1 and each '-' decreases it by one. Or you can set it numerically from 0 to 10",
        new BraveryCommand()),
    TARGET("ta", "target",
        "Target Hex - Adds the specified hex to princess's list of Strategic Targets.",
        new TargetGroundCommand()),
    PRIORITIZE("pr", "prioritize",
        "Priority Target - Adds the specified unit to princess's Priority Targets list.",
        new PriorityTargetCommand()),
    SHOW_BEHAVIOR("sh", "show-behavior",
        "Show Behavior - Princess will state the name of her current behavior.",
        new ShowBehaviorCommand()),
    LIST__COMMANDS("li", "list-commands",
        "List Commands - Displays this list of commands.",
        new ListCommands()),
    IGNORE_TARGET("ig", "ignore-target",
        "Ignore Target - Will not fire on the entity with this ID.",
        new IgnoreTargetCommand()),
    SHOW_DISHONORED("di", "show-dishonored",
        "Show Dishonored - Show the players on the dishonored enemies list.",
        new ShowDishonoredCommand()),
    CLEAR_IGNORED_TARGETS("cl", "clear-ignored-targets",
        "Clear Ignored Target - Clears the list of ignored targets.",
        new ClearIgnoredTargetsCommand()),
    BLOOD_FEUD("bf",  "blood-feud",
        "Blood Feud - Adds player to the dishonored enemies list.",
        new BloodFeudCommand()),
    ADD_WAYPOINT("aw", "add-waypoint",
        "Add Waypoint - Add a waypoint destination to the princess's entity.",
        new AddWaypointCommand()),
    REMOVE_WAYPOINT("rw", "remove-waypoint",
        "Remove Last Waypoint for unit - Remove last waypoint added to the entity list of waypoints.",
        new RemoveWaypointCommand()),
    CLEAR_WAYPOINT("cw", "clear-waypoints",
        "Clear Unit Waypoints - Remove all waypoints for unit.",
        new ClearWaypointsCommand()),
    CLEAR_ALL_WAYPOINTS("nw", "clear-all-waypoints",
        "Clear All Waypoints - Remove all waypoints for this bot.",
        new ClearAllWaypointsCommand()),
    SET_WAYPOINT("sw", "set-waypoints",
        "Set Waypoint - Set a new list of waypoints for a unit. It accepts any number of waypoints and the unit will follow one waypoint at a time",
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
        return "princessName: " + getAbbreviation() + "/" + getCommand()  + (chatCommand.defineArguments().isEmpty() ? "" : ": " + chatCommand.getArgumentsRepr());
    }

    public String getDescription() {
        return (chatCommand.defineArguments().isEmpty() ? "" : chatCommand.getArgumentsRepr() + " ") + description;
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
