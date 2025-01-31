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

/**
 * @author Deric Page (deric.page@nisc.coop) (ext 2335)
 * @since 10/24/2014 9:57 AM
 */
public enum ChatCommands {
    FLEE("fl", "princessName: fl", "Flee - Causes princess-controlled units to start fleeing the board, regardless of " +
                                     "damage level or Forced Withdrawal setting."),
    VERBOSE("ve", "princessName: ve: <error/warning/info/debug>", "Sets princess's verbosity level."),
    BEHAVIOR("be", "princessName: be: behaviorName", "Behavior - Change's princess's behavior to the named behavior."),
    CAUTION("ca", "princessName: ca: +/-", "Caution - Modifies princess's Piloting Caution setting. Each '+' increases " +
                                                   "it by 1 and each '-' decreases it by one."),
    AVOID("av", "princessName: av: +/-", "Avoid - Modifies princess's Self Preservation setting. Each '+' increases it " +
                                               "by 1 and each '-' decreases it by one."),
    AGGRESSION("ag", "princessName: ag : +/-", "Aggression - Modifies princess's Aggression setting. Each '+' increases " +
                                                         "it by 1 and each '-' decreases it by one."),
    HERDING("he", "princessName: he: +/-", "Herd - Modifies princess's Herding setting. Each '+' increases it by 1 and " +
                                                "each '-' decreases it by one."),
    BRAVERY("br", "princessName: br: +/-", "Brave - Modifies princess's Bravery setting. Each '+' increases it by 1 " +
                                                 "and each '-' decreases it by one."),
    TARGET("ta", "princessName: ta: hexNumber", "Target Hex - Adds the specified hex to princess's list of Strategic Targets."),
    PRIORITIZE("pr", "princessName: pr: unitId", "Priority Target - Adds the specified unit to princess's Priority Targets " +
                                                          "list."),
    SHOW_BEHAVIOR("sh", "princessName: sh", "Show Behavior - Princess will state the name of her current behavior."),
    LIST__COMMANDS("li", "princessName: li", "List Commands - Displays this list of commands."),
    IGNORE_TARGET("ig", "princessName: ig: unitId", "Ignore Target - Will not fire on the entity with this ID."),
    SHOW_DISHONORED("di", "princessName: di", "Show Dishonored - Show the players on the dishonored enemies list."),
    CLEAR_IGNORED_TARGETS("cl", "princessName: cl", "Clear Ignored Target - Clears the list of ignored targets."),
    BLOOD_FEUD("bf", "princessName: bf: playerId", "Blood Feud - Adds player to the dishonored enemies list."),
    ADD_WAYPOINT("aw", "princessName: aw: unitID hexNumber ", "Add Waypoint - Add a waypoint destination to the princess's entity."),
    REMOVE_WAYPOINT("rw", "princessName: rw: unitID", "Remove Last Waypoint for unit - Remove last waypoint added to the entity list of waypoints."),
    CLEAR_WAYPOINT("cw", "princessName: cw: unitID", "Clear Units Waypoints - Remove all waypoints for unit."),
    CLEAR_ALL_WAYPOINTS("nw", "princessName: nw", "Clear All Waypoints - Remove all waypoints for this princess."),
    SET_WAYPOINT("sw", "princessName: sw: unitID hexNumber [hexNumber...]", "Set Waypoint - Set a waypoint to the entity, removing all other waypoints it had."),;

    private final String abbreviation;
    private final String syntax;
    private final String description;

    ChatCommands(String abbreviation, String syntax, String description) {
        this.abbreviation = abbreviation;
        this.syntax = syntax;
        this.description = description;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getSyntax() {
        return syntax;
    }

    public String getDescription() {
        return description;
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
