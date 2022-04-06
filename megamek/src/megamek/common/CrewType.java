/*
* MegaMek -
* Copyright (C) 2017 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package megamek.common;

/**
 * Contains data used by crew to determine how many distinct crew members to track for name, damage,
 * and skills, and which crew normally execute which roles. Note that this applies to various special
 * multi-crew cockpits in which crew members can take over duties of another that has been incapacitated;
 * units with multiple crew that do not track them separately (such as vehicles and aerospace vessels)
 * still use the SINGLE value, since they do not track damage to individual crew members (outside
 * specific criticals) and only have a single set of skills.
 *
 * @author Neoancient
 *
 */

public enum CrewType {
    SINGLE (new String[] {"Pilot"}, 0, 0, -1, -1, 1),
    CREW (new String[] {"Commander"}, 0, 0, -1, -1, 1),
    VESSEL (new String[] {"Commander"}, 0, 0, -1, -1, -1),
    TRIPOD (new String[] {"Pilot", "Gunner"}, 0, 1, -1, -1, 3),
    SUPERHEAVY_TRIPOD (new String[] {"Pilot", "Gunner", "Tech Officer"}, 0, 1, 2, 2, 3),
    QUADVEE (new String[] {"Pilot", "Gunner"}, 0, 1, -1, -1, 3),
    DUAL (new String[] {"Pilot", "Gunner"}, 0, 1, -1, -1, 2),
    COMMAND_CONSOLE (new String[] {"Pilot", "Commander"}, 0, 0, 1, -1, 1);

    private String[] roleNames;
    private int pilotPos;
    private int gunnerPos;
    private int commanderPos;
    private int techPos;
    private int maxPrimaryTargets;

    CrewType(String[] roleNames, int pilotPos, int gunnerPos, int commanderPos, int techPos,
            int maxPrimaryTargets) {
        this.roleNames = roleNames;
        this.pilotPos = pilotPos;
        this.gunnerPos = gunnerPos;
        this.commanderPos = commanderPos;
        this.techPos = techPos;
        this.maxPrimaryTargets = maxPrimaryTargets;
    }

    /**
     * @return The number of distinct crew members in the cockpit.
     */
    public int getCrewSlots() {
        return roleNames.length;
    }

    /**
     * @param index The index of the crew member in question.
     * @return A name that designates the role played by that crew member.
     */
    public String getRoleName(int index) {
        return roleNames[index];
    }

    /**
     * @return The index of the crew member that typically acts in the role of pilot.
     */
    public int getPilotPos() {
        return pilotPos;
    }

    /**
     * @return The index of the crew member that typically acts in the role of gunner.
     */
    public int getGunnerPos() {
        return gunnerPos;
    }

    /**
     * @return The index of the crew member that that provides a command bonus. A value &lt; 0
     * indicates there is no such position.
     */
    public int getCommanderPos() {
        return commanderPos;
    }

    /**
     * @return The index of the crew member that that acts as a technical/tactical officer. A value
     * &lt; 0 indicates there is no such position.
     */
    public int getTechPos() {
        return techPos;
    }

    /**
     * @return The number of targets that can be attacked without incurring a secondary target penalty
     *         with a dedicated gunner. A value &lt; 0 indicates that there is no limit.
     */
    public int getMaxPrimaryTargets() {
        return maxPrimaryTargets;
    }
}
