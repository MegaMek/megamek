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

package megamek.common.options;

import java.io.Serializable;
import java.util.Vector;

/**
 * Contains the options determining abilities of the pilot
 *
 * @author Cord
 */
public class PilotOptions extends AbstractOptions implements Serializable {

    public static final String LVL3_ADVANTAGES = "lvl3Advantages";

    public PilotOptions() {
        super();
    }
    
    public void initialize() {
        OptionGroup adv = addGroup("Advantages", LVL3_ADVANTAGES);
        addOption(adv, "dodge_maneuver", "Dodge Maneuver", "Enables the unit to make a dodge maneuver instead of a physical attack. This maneuver adds +2 to the BTH to physical attacks against the unit.\n\nNOTE: The dodge maneuver is declared during the weapons phase. Note: This ability is only used for BattleMechs.", false);
        addOption(adv, "maneuvering_ace", "Maneuvering Ace", "Enables the unit to move laterally like a Quad. Units also receive a -1 BTH to rolls against skidding.", false);
        addOption(adv, "melee_specialist", "Melee Specialist", "Enables the unit to do 1 additional point of damage with physical attacks and subtracts one from the attacker movement modifier (to a minimum of zero).\n\nNote: This ability is only used for BattleMechs.", false);
        addOption(adv, "pain_resistance", "Pain Resistance", "When making consciousness rolls, 1 is added to all rolls. Also, damage received from ammo explosions is reduced by one point.\n\nNote: This ability is only used for BattleMechs.", false);
        addOption(adv, "tactical_genius", "Tactical Genius", "A pilot who has a Tactical Genius may reroll their initiative once per turn.  The second roll must be accepted.\n\nNote: Only one Tactical Genius may be utilized per team.", false);
        addOption(adv, "weapon_specialist", "Weapon Specialist", "A pilot who specializes in a particular weapon receives a -2 to hit modifier on all attacks with that weapon.", new Vector());
        addOption(adv, "gunnery_laser", "Gunnery/Laser", "NOTE: This is a unofficial rule. Pilot gets a -1 to-hit bonus on all energy-based weapons (Laser, PPC, and Flamer).", false);
        addOption(adv, "gunnery_missile", "Gunnery/Missile", "NOTE: This is a unofficial rule. Pilot gets a -1 to-hit bonus on all missile weapons (LRM, SRM, MRM, RL and ATM).", false);
        addOption(adv, "gunnery_ballistic", "Gunnery/Ballistic", "NOTE: This is a unofficial rule. Pilot gets a -1 to-hit bonus on all ballistic weapons (MGs, all ACs, Gaussrifles).", false);
        addOption(adv, "iron_man", "Iron Man", "NOTE: This is a unofficial rule. A pilot with this skill receives only 1 pilot hit from ammunition explosions.", false);
    }

    /* (non-Javadoc)
     * @see megamek.common.options.AbstractOptions#getOptionsInfoImp()
     */
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return PilotOptionsInfo.getInstance();
    }

    private static class PilotOptionsInfo extends AbstractOptionsInfo {
        private static AbstractOptionsInfo instance = new PilotOptionsInfo(); 

        public static AbstractOptionsInfo getInstance() {
            return instance;
        }
        protected PilotOptionsInfo() {            
        }
    }
}
