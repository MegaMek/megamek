/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common;

import java.io.*;
import java.util.*;

import megamek.common.options.*;

/**
 * Contains the options determining abilities of the pilot
 *
 * @author Cord
 */
public class PilotOptions extends Options implements Serializable {
    public static final String LVL3_ADVANTAGES = "lvl3Advantages";
    
    public void initialize() {
        OptionGroup adv = new OptionGroup("Advantages", LVL3_ADVANTAGES);
        addGroup(adv);
//        addOption(adv, new GameOption("bullseye_marksman", "Bull's-Eye Marksman", "NOTE: Not implemented. Awaiting targeting computer code.\n\nEnables the unit to simulate having a targetting computer and be able to target a specific location on an enemy mech. The unit may not move or execute physical attacks and may only fire a single weapon in the round using this ability.", false));
        addOption(adv, new GameOption("dodge_maneuver", "Dodge Maneuver", "Enables the unit to make a dodge maneuver instead of a physical attack. This maneuver adds +2 to the BTH to physical attacks against the unit.\n\nNOTE: The dodge maneuver is declared during the weapons phase. Note: This ability is only used for BattleMechs.", false));
        addOption(adv, new GameOption("maneuvering_ace", "Maneuvering Ace", "Enables the unit to move laterally like a Quad. Units also receive a -1 BTH to rolls against skidding.", false));
        addOption(adv, new GameOption("melee_specialist", "Melee Specialist", "Enables the unit to do 1 additional point of damage with physical attacks and subtracts one from the attacker movement modifier (to a minimum of zero).\n\nNote: This ability is only used for BattleMechs.", false));
        addOption(adv, new GameOption("pain_resistance", "Pain Resistance", "When making consciousness rolls, 1 is added to all rolls. Also, damage received from ammo explosions is reduced by one point.\n\nNote: This ability is only used for BattleMechs.", false));
//        addOption(adv, new GameOption("sixth_sense", "Sixth Sense", "NOTE: Not implemented. Enables a pilot to move his/her initiative all the way to the end of the list one time per fight.", false));
//        addOption(adv, new GameOption("speed_demon", "Speed Demon", "NOTE: Not implemented. Enables a pilot to add one to his/her running/flank speed. Enabling this disallows any weapons or physical attacks for the round.", false));
        addOption(adv, new GameOption("tactical_genius", "Tactical Genius", "A pilot who has a Tactical Genius may reroll their initiative once per turn.  The second roll must be accepted.\n\nNote: Only one Tactical Genius may be utilized per team.", false));
        addOption(adv, new GameOption("weapon_specialist", "Weapon Specialist", "A pilot who specializes in a particular weapon receives a -2 to hit modifier on all attacks with that weapon.", new Vector()));
        addOption(adv, new GameOption("gunnery_laser", "Gunnery/Laser", "NOTE: This is a unofficial rule. Pilot gets a -1 to-hit bonus on all energy-based weapons (Laser, PPC, and Flamer).", false));
        addOption(adv, new GameOption("gunnery_missile", "Gunnery/Missile", "NOTE: This is a unofficial rule. Pilot gets a -1 to-hit bonus on all missile weapons (LRM, SRM, MRM, RL and ATM).", false));
        addOption(adv, new GameOption("gunnery_ballistic", "Gunnery/Ballistic", "NOTE: This is a unofficial rule. Pilot gets a -1 to-hit bonus on all ballistic weapons (MGs, all ACs, Gaussrifles).", false));
        addOption(adv, new GameOption("iron_man", "Iron Man", "NOTE: This is a unofficial rule. A pilot with this skill receives only 1 pilot hit from ammunition explosions.", false));
        
        
    }
}
