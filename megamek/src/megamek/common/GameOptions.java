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
 * Contains the options determining play in the current game.
 *
 * @author Ben
 */
public class GameOptions implements Serializable {
    
    private Vector groups = new Vector();
    private Vector allOptions = new Vector();
    private Hashtable optionsHash = new Hashtable();
    
    public GameOptions() {
        ;
    }
    
    public void initialize() {
        // set up game options
        OptionGroup base = new OptionGroup("Base Options");
        addGroup(base);
        addOption(base, new GameOption("friendly_fire", "Friendly fire", "If checked, the game considers mechs owned by a player, or on the same team as a player, as valid targets.\n\nDefaults to checked, but unchecks when a second player joins the server.", true));
//        addOption(base, new GameOption("skip_ineligable_firing", "Skip ineligable during firing", "If checked, the game will skip a unit during the firing phase if it has no targets within range or LOS.\n\nUnchecked by default.", false));
        addOption(base, new GameOption("skip_ineligable_physical", "Skip ineligable during physical", "If checked, the game will skip a unit during the physical phase if no attacks are possible or there are no valid targets.\n\nChecked by default.", true));
        addOption(base, new GameOption("push_off_board", "Allow pushing off the map", "This options allows a mech to be pushed off the map and out of the game by push, charge or DFA attacks.\n\nChecked by default.", true));
        addOption(base, new GameOption("check_victory", "Check for victory", "If checked, the server will enter the victory phase at the end of any turn where victory conditions are met.  Even if unchecked or conditions are not met, server admins can force victory with the /victory command.\n\nDefaults to checked.", true));
        addOption(base, new GameOption("rng_type", "RNG Type", "Note: any type other than 0 or 1 is completely unofficial.\n\nValid types:\n0 - SunRandom: Sun regular RNG\n1 - CryptoRandom: Java crypto-strength RNG\n2 - Pool36Random: Pool of 36 values, randomly shuffled\n\nDefaults to 1.", 1));
        addOption(base, new GameOption("team_initiative", "Teams roll initiative", "When checked, teams roll initiative as one group.  This team initative is used to order the units according to the normal method.  Player order on a team is determined by their own initiative.\n\nChecked by default", true)); 
        
        OptionGroup level2 = new OptionGroup("Optional Rules (Level 2)");
        addGroup(level2);
        addOption(level2, new GameOption("flamer_heat", "Flamers can deal heat instead of damage", "If checked, flamers can increase the heat of their target by 2 instead of dealing 2 damage.\n\nChecked by default.", true));
        addOption(level2, new GameOption("fire", "Fire and smoke", "If checked, fires may be set accidentally or intentionally.\n\nChecked by default.", true));

        OptionGroup level3 = new OptionGroup("Optional Rules (Level 3)");
        addGroup(level3);
        addOption(level3, new GameOption("double_blind", "Double blind", "If checked, enemy units will only be visible if they are in line of sight of one or more of your units.\n\nUnchecked by default.", false));
        addOption(level3, new GameOption("team_vision", "Teams share vision", "If checked, teams will share vision in double-blind mode.  Only valid in double-blind mode.\n\nChecked by default.", true));
        addOption(level3, new GameOption("floating_crits", "Through-armor criticals will 'float'", "If checked, rolls of '2' on hit location will result in a new location being rolled for a critical hit, instead of just hitting the local torso.\n\nUnchecked by default.", false));
        
        OptionGroup ruleBreakers = new OptionGroup("Optional Rules (unofficial)");
        addGroup(ruleBreakers);
        addOption(ruleBreakers, new GameOption("no_tac", "No through-armor criticals", "If checked, rolls of '2' on hit location will only result in a torso hit, and no critical roll.  Only applies to mechs.  Supercedes the floating criticals option.\n\nUnchecked by default.", false));
        addOption(ruleBreakers, new GameOption("no_immobile_vehicles", "Vehicles not immobilized by crits", "If checked, vehicles with a drive or engine hit will not be counted as 'immobile' for purposes of determining to-hit numbers.\n\nUnchecked by default.", false));
	addOption(ruleBreakers, new GameOption("inf_move_last", "Infantry move after Meks and Vehicles", "If checked, all Meks and Vehicles will move before the first Infantry platoon.  The move order of Meks and Vehicles ignores the presence of Infantry.\n\nMutually exclusive with \"" + Game.INF_MOVE_MULTI + " Infantry for every Mek or Vehicle\".\n\nUnchecked by default.", false));
	addOption(ruleBreakers, new GameOption("inf_move_multi", Game.INF_MOVE_MULTI + " Infantry for every Mek or Vehicle", "If checked, " + Game.INF_MOVE_MULTI + " platoons will have to move in place of a single Mek or Vehicle.  If there are less than " + Game.INF_MOVE_MULTI + " platoons remaining, they all must move.  The move order includes the presence of Infantry.\n\nMutually exclusive with \"Infantry move after Meks and Vehicles\".\n\nUnchecked by default.", false));
        addOption(ruleBreakers, new GameOption("blind_drop", "Blind Drop", "If checked, the configuration of a Mech won't be shown in the Chatroom to your opponents.", false)); 
    }
    
    private void addGroup(OptionGroup group) {
        groups.addElement(group);
    }
    
    public Enumeration groups() {
        return groups.elements();
    }
    
    private void addOption(OptionGroup group, GameOption option) {
        group.addOption(option);
        allOptions.addElement(option);
        optionsHash.put(option.getShortName(), option);
    }
    
    public GameOption getOption(String name) {
        return (GameOption)optionsHash.get(name);
    }
    
    public boolean booleanOption(String name) {
        return getOption(name).booleanValue();
    }
    
    public int intOption(String name) {
        return getOption(name).intValue();
    }
    
    public float floatOption(String name) {
        return getOption(name).floatValue();
    }
}
