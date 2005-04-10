/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

import gd.xml.ParseException;
import gd.xml.tiny.ParsedXML;
import gd.xml.tiny.TinyParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Vector;

import megamek.client.Client;
import megamek.common.Settings;

/**
 * Contains the options determining play in the current game.
 *
 * @author Ben
 */
public class GameOptions extends AbstractOptions implements Serializable {

    private static final String GAME_OPTIONS_FILE_NAME = "gameoptions.xml";
    
    public GameOptions() {
        super();
    }


    public void initialize() {
        OptionGroup base = addGroup("Base Options");
        addOption(base, "friendly_fire", "Friendly fire", "If checked, the game considers mechs owned by a player, or on the same team as a player, as valid targets.\n\nDefaults to checked, but unchecks when a second player joins the server.", true);
        addOption(base, "skip_ineligable_movement", "Skip ineligable during movement", "If checked, the game will skip a unit during the movement phase if it is immobile or otherwise inactive.\n\nUnchecked by default.", false);
        addOption(base, "skip_ineligable_firing", "Skip ineligable during firing", "If checked, the game will skip a unit during the firing phase if it is inactive.\n\nUnchecked by default.", false);
        addOption(base, "skip_ineligable_physical", "Skip ineligable during physical", "If checked, the game will skip a unit during the physical phase if no attacks are possible or there are no valid targets.\n\nChecked by default.", true);
        addOption(base, "push_off_board", "Allow pushing off the map", "This options allows a mech to be pushed off the map and out of the game by push, charge or DFA attacks.\n\nChecked by default.", true);
        addOption(base, "check_victory", "Check for victory", "If checked, the server will enter the victory phase at the end of any turn where victory conditions are met.  Even if unchecked or conditions are not met, server admins can force victory with the /victory command.\n\nDefaults to checked.", true);
        addOption(base, "rng_type", "RNG Type", "Note: any type other than 0 or 1 is completely unofficial.\n\nValid types:\n0 - SunRandom: Sun regular RNG\n1 - CryptoRandom: Java crypto-strength RNG\n2 - Pool36Random: Pool of 36 values, randomly shuffled\n\nDefaults to 1.", 1);
        addOption(base, "team_initiative", "Teams roll initiative", "When checked, teams roll initiative as one group.  This team initative is used to order the units according to the normal method.  Player order on a team is determined by their own initiative.\n\nChecked by default", true); 
        addOption(base, "is_eq_limits", "Ammo & Equipment Limits", "If checked, Inner Sphere units will be limited to ammo & equipment available at their build year.  Turning this option off will not remove invalid equipment.\n\nChecked by default", true); 
        addOption(base, "autosave_msg", "Remind on Autosave", "If checked, the server will send a message each time an auto-save is performed.\n\nChecked by default", true); 
        addOption(base, "paranoid_autosave", "Paranoid Autosave", "If checked, the game will auto-save every phase.\n\nUnchecked by default", false); 
        
        OptionGroup level2 = addGroup("Optional Rules (Level 2)");
        addOption(level2, "flamer_heat", "Flamers can deal heat instead of damage", "If checked, flamers can increase the heat of their target by 2 instead of dealing 2 damage.\n\nChecked by default.", true);
        addOption(level2, "fire", "Fire and smoke", "If checked, fires may be set accidentally or intentionally.\n\nChecked by default.", true);
        addOption(level2, "indirect_fire", "Indirect fire", "If checked, LRMs may be fire indirectly, and players may choose to spot for indirect fire instead of attacking.\n\nChecked by default.", true);
        addOption(level2, "minefields", "Minefields", "If checked, minefields can be used.\n\nChecked by default.", true);
        addOption(level2, "temperature", "Temperature", "The temperature the game takes place at, for use with the Extreme Temperatures rule.\n\nDefaults to 25 degrees Celsius, thus having no effect.", 25);
        addOption(level2, "gravity", "Gravity", "The gravity of the world the game takes place at, for use with the High/Low Gravity rule.\n\nDefaults to 1 G, thus having no effect.", (float)1.0);
        addOption(level2, "vacuum", "Vacuum", "If checked, the game takes place in Vacuum.\n\nUnchecked by default.", false);
        addOption(level2, "night_battle", "Night Battle", "If checked, the game takes place at night (+2 to-hit-modifier to all attacks unless attacker or attacked unit are using spotlights. Defaults to off.", false);

        OptionGroup level3 = addGroup("Optional Rules (Level 3)");
        addOption(level3, "double_blind", "Double blind", "If checked, enemy units will only be visible if they are in line of sight of one or more of your units.\n\nUnchecked by default.", false);
        addOption(level3, "team_vision", "Teams share vision", "If checked, teams will share vision in double-blind mode.  Only valid in double-blind mode.\n\nChecked by default.", true);
        addOption(level3, "floating_crits", "Through-armor criticals will 'float'", "If checked, rolls of '2' on hit location will result in a new location being rolled for a critical hit, instead of just hitting the local torso.\n\nUnchecked by default.", false);
        addOption(level3, "engine_explosions", "MaxTech engine explosions", "If checked, any time a mech takes 2 or more engine crits in one round, a roll of '12' will cause a cascading engine explosion.", false);
        addOption(level3, "pilot_advantages", "MaxTech pilot advantages", "If checked, players can add additional advantages to their pilots through the 'configure mech' window.", false);
        addOption(level3, "maxtech_physical_BTH", "MaxTech physical BTHs", "If checked, BTHs for physical attacks will use MaxTech levels. These levels take into account the piloting skill of the attacking unit.", false);
        addOption(level3, "maxtech_physical_psr", "MaxTech physical PSR weight difference", "If checked, after being kicked or pushed the PSR is modified by the weight classes of the two mechs. The PSR is modified by 1 point per weight class difference. The difference is added to the PSR if the attacker is heavier or subtracted if the attacker is lighter.", false);
        addOption(level3, "maxtech_round_damage", "MaxTech damage per round", "If checked, units will have +1 to their piloting skill roll for every 20 damage taken, not just the first damage. Also, BTH is altered by weight class. Lights get +1 to BTH where as assaults get -2. Mediums and heavies are in between.", false);
        addOption(level3, "maxtech_prone_fire", "MaxTech firing while prone", "If checked, mechs that are prone can fire if they are missing one arm. Standard rules disallow firing when prone and missing an arm. All weapons are +1 BTH and weapons mounted in propping arm can not be fired.", false);
        addOption(level3, "maxtech_target_modifiers", "MaxTech target movement modifiers", "If checked, the target movement modifiers table is enhanced by 3 additional steps (14-18 hexes => +5, 19-24 hexes => +6 25+ hexes => +7.", false);
        addOption(level3, "maxtech_leg_damage", "MaxTech Leg Damage Rule", "If checked, hip criticals are cummulative with other damage to legs, but reduce movement by 2 instead of halving. Destroying both hips does not reduce MPs to zero.", false);
        addOption(level3, "maxtech_fire", "MaxTech Fire/Smoke Rules", "If checked, fires create smoke that drifts and dissipates, instead of remaining static.  In addition, smoke is split into light and heavy varieties.", false);
        addOption(level3, "maxtech_range", "MaxTech Extreme Range Rules", "If checked, Weapons have an extreme range bracket past their long range distance.", false);
        addOption(level3, "maxtech_LOS1", "MaxTech Diagramming Line of Sight Rules", "If checked, LOS will be calculated using the Diagramming LOS rules from MaxTech revised, page 22", false);
        addOption(level3, "maxtech_altdmg", "MaxTech Altered Energy Weapons Damage", "If checked, the damage inflicted by energy weapons (Laser, Flamer, PPC) is altered as follows: Half damage (rounded up) at extreme range, -1 damage at long range, +1 damage at range <= 1. Defaults to false.", false);
        addOption(level3, "maxtech_mslhitpen", "MaxTech Missile Hit Penalties", "If checked, apply the following penalties to the roll to determine number of missiles hit: +1 (Range <= 1 hex); 0 (Short range); -1 (Medium range); -2 (Long and Extreme range). Does not apply to Streak SRMs. If the roll is increased above 12, all missiles hit. If the roll is reduced below 2, only 1 missile hits.", false);
        addOption(level3, "maxtech_ppc_inhibitors", "MaxTech PPC Field Inhibitor Disengage", "If checked, the field inhibitor of a PPC can be disengaged to disregard the minimum range modifiers. However, after each shot, there is a chance that the PPC is destroyed in the process and the IS of the PPC's location takes 10 points of damage. The chance for this depends on range, roll 2d6 to avoid on: 3+ for >=3 hex range; 6+ for 2 hex range and 10+ for 1 hex range. Defaults to off.", false);
        addOption(level3, "maxtech_charge_damage", "MaxTech Charge Damage", "If checked, the attacker in a charge will receive damage proportional to the distance of the charge. Defaults to off.", false);
        addOption(level3, "maxtech_glancing_blows", "MaxTech Glancing Blows", "If checked, attacks that roll for to-hit and roll exactly the target number required deal only reduced damage: Normal attacks deal only half damage, while missile attacks get a -2 for the roll on the # of missiles roll. If the roll is reduced below 2, only 1 missile hits. Additionally, if a glancing blow deals a critical hit, apply a -2 modifier to the check. Attacks that do not roll for attack (Falling damage, ...), attacks that deal no damage (TAG), and Streak SRMs are unaffected. Defaults to false.", false);
        addOption(level3, "maxtech_burst", "MaxTech Burst Fire MGs", "If checked, MGs may use burst fire rules: They inflict 1D6 of damage, create an equal amount of heat and use triple the amount of ammunition. Click the 'Configure Unit' button to configure MGs. Defaults to false.", false);
        
        OptionGroup ruleBreakers = addGroup("Optional Rules (unofficial)");
        addOption(ruleBreakers, "no_tac", "No through-armor criticals", "If checked, rolls of '2' on hit location will only result in a torso hit, and no critical roll.  Only applies to mechs.  Supercedes the floating criticals option.\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "no_immobile_vehicles", "Vehicles not immobilized by crits", "If checked, vehicles with a drive or engine hit will not be counted as 'immobile' for purposes of determining to-hit numbers.\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "vehicles_can_eject", "Vehicles can be abandoned", "If checked, vehicle crews can leave their unit (effectively removing it from the game).\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "inf_move_even", "Infantry don't count for movement initiative", "If checked, Infantry units no longer count towards the initiative for the player's team in the movement phase, unless that team has no other units.  Instead, their moves are distributed 'evenly' throughout the turn.  The move order of Meks and Vehicles ignores the presence of Infantry.  The order of the fire phase is unaffected.\n\nMutually exclusive with \"multiple Infantry for every Mek or Vehicle\".\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "inf_deploy_even", "Infantry don't count for deployment initiative", "If checked, Infantry units no longer count towards the initiative for the player's team in the deployment phase, unless that team has no other units.  Instead, their deployments are distributed 'evenly' throughout the turn.  The deployment order of Meks and Vehicles ignores the presence of Infantry.\n\nThe above option, \"Infantry move after Meks and Vehicles\" must also be checked to use this option.\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "inf_move_later", "Infantry moves after that players other units", "If checked, each player must move all their other units before they move their Infantry.  If \"Protomechs move after that team's other units\" is also checked, then Infantry and Protomechs are lumped together into the same category.\n\nMutually exclusive with \"multiple Infantry for every Mek or Vehicle\" and \"Infantry don't count for movement initiative\".\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "inf_move_multi", "move multiple Infantry for every Mek or Vehicle", "If checked, multiple infantry units will have to move in place of a single Mek or Vehicle.  Set the number per mech in the appropriate Game Option.  If there are less than above units remaining, they all must move.  The move order includes the presence of Infantry.\n\nMutually exclusive with \"Infantry move after Meks and Vehicles\".\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "protos_move_even", "Protomechs don't count for movement initiative", "If checked, Protomech units no longer count towards the initiative for the player's team in the movement phase, unless that team has no other units.  Instead, their moves are distributed 'evenly' throughout the turn.  The move order of Meks and Vehicles ignores the presence of Protomechs.  The order of the fire phase is unaffected.\n\nMutually exclusive with \"multiple Protomechs for every Mek or Vehicle\".\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "protos_deploy_even", "Protomechs don't count for deployment initiative", "If checked, Protomech units no longer count towards the initiative for the player's team in the deployment phase, unless that team has no other units.  Instead, their deployments are distributed 'evenly' throughout the turn.  The deployment order of Meks and Vehicles ignores the presence of Protomechs.\n\nThe above option, \"Protomechs move after Meks and Vehicles\" must also be checked to use this option.\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "protos_move_later", "Protomechs move after that players other units", "If checked, each player must move all their other units before they move their Protomechs.  If \"Protomechs moves after that team's other units\" is also checked, then Infantry and Protomechs are lumped together into the same category.\n\nMutually exclusive with \"multiple Protomechs for every Mek or Vehicle\" and \"Protomechs don't count for movement initiative\".\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "protos_move_multi", "move multiple Protomechs for every Mek or Vehicle", "If checked, multiple protomechs will have to move in place of a single Mek or Vehicle. Set the number per mech in the appropriate Game Option.  If there are less than above specified protos remaining, they all must move.  The move order includes the presence of Protomechs.\n\nMutually exclusive with \"Protomechs move after Meks and Vehicles\".\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "inf_proto_move_multi", "number of Infantry/Protomech to move per Mek", "The number of Infantry units/Protomechs that have to move for every Mek or Vehicle, if the inf_move_multi or the protos_move_multi option is selected.\n\nDefault of 3.", 3);
        addOption(ruleBreakers, "blind_drop", "Blind Drop", "If checked, the configuration of a Mech won't be shown in the Chatroom to your opponents.", false);
        addOption(ruleBreakers, "real_blind_drop", "Real Blind Drop", "If checked, only own units are displayed. Defaults to false.", false);
        addOption(ruleBreakers, "clan_ignore_eq_limits", "Ignore Clan Ammo Limitations", "If checked, Clan units can use ammo normally limited to IS units only; for example, Thunder-Augmented, Thunder-Inferno, and Thunder-Active LRM rounds.", false);
        addOption(ruleBreakers, "no_clan_physical", "No physical attacks for the clans", "If checked, clan Meks may not make physical attacks.\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "no_hover_charge", "No charge attacks for Hover vehicles", "If checked, Hover vehicles may not make rams/charges.\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "woods_burn_down", "Woods have a chance to burn down", "If checked, woods will burn down as if cleared on a roll of 11+.\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "vehicles_safe_from_infernos", "Vehicles may not be the target of an Inferno missile attack.", "If checked, Vehicles cannot be the target of an Inferno SRM attack.\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "protos_safe_from_infernos", "Protomechs may not be the target of an Inferno missile attack.", "If checked, Protomechs cannot be the target of an Inferno SRM attack.\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "lobby_ammo_dump", "Allow Ammo Dumping in the Lobby", "If checked, Players may dump their Mech's ammo before the game starts.\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "set_arty_player_homeedge", "Automatically set artillery home edge", "If checked, all of the players' artillery units will have their homeedge set to the deployment edge of the player,\nNW and NE are North, SW and SE are South.\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "no_premove_vibra", "Do not damage Mek by Vibrabomb if it has not yet moved.", "If checked, vibrabombs that explode will not damage Meks that have not yet finished their move.\n\nUnchecked by default.", false);
        addOption(ruleBreakers, "auto_spot", "Any unit that does not do anything special is a spotter.", "If checked, units that don't fire weapons/search for clubs/etc automatically spot for indirect LRMs.\n\nUnchecked by default.", false);
    }

    public void loadOptions(Client client, String password) {
      ParsedXML root = null;
      InputStream is = null;
      
      try {
        is = new FileInputStream(new File(GAME_OPTIONS_FILE_NAME));
      } catch (FileNotFoundException e) {
        return;
      }

      try {
        root = TinyParser.parseXML(is);
      } catch (ParseException e) {
        System.out.println("Error parsing game options xml file."); 
        e.printStackTrace(System.out);
      }
    
      Enumeration rootChildren = root.elements();
      ParsedXML optionsNode = (ParsedXML)rootChildren.nextElement();
      
      if ( optionsNode.getName().equals("options") ) {
        Enumeration children = optionsNode.elements();
        Vector changedOptions = new Vector();
        
        while (children.hasMoreElements()) {
          IOption option = parseOptionNode((ParsedXML)children.nextElement());
          
          if ( null != option )
            changedOptions.addElement(option);
        } 
        
        if ( changedOptions.size() > 0 &&
             client != null && password != null ) {
          client.sendGameOptions(password, changedOptions);
        }
      } else {
        System.out.println("Root node of game options file is incorrectly named. Name should be 'options' but name is '" + optionsNode.getName() + "'");
      }  
    }
    
    private IOption parseOptionNode(ParsedXML node) {
      IOption option = null;
      
      if ( node.getName().equals("gameoption") ) {
        Enumeration children = node.elements();
        String name = null;
        Object value = null;
        
        while (children.hasMoreElements()) {
          ParsedXML child = (ParsedXML)children.nextElement();
          
          if ( child.getName().equals("optionname") ) {
            name = ((ParsedXML)child.elements().nextElement()).getContent();
          } else if ( child.getName().equals("optionvalue") ) {
            value = ((ParsedXML)child.elements().nextElement()).getContent();
          }
        }
        
        if ( (null != name) && (null != value) ) {
          IOption tempOption = this.getOption(name);
          
          if ( null != tempOption ) {
            if ( !tempOption.getValue().toString().equals(value.toString()) ) {
              try {
                switch ( tempOption.getType() ) {
                  case IOption.STRING:
                    tempOption.setValue((String)value);
                    break;
                    
                  case IOption.BOOLEAN:
                    tempOption.setValue(new Boolean(value.toString()));
                    break;
                    
                  case IOption.INTEGER:
                    tempOption.setValue(new Integer(value.toString()));
                    break;
                    
                  case IOption.FLOAT:
                    tempOption.setValue(new Float(value.toString()));
                    break;
                }
                
                System.out.println("Set option '" + name + "' to '" + value + "'.");
                
                option = tempOption;
              } catch ( IllegalArgumentException iaEx ) {
                System.out.println("Error trying to load option '" + name + "' with a value of '" + value + "'.");
              }
            }
          } else {
            System.out.println("Invalid option '" + name + "' when trying to load options file.");
          }
        }
      }
      
      return option;
    }
    
    public static void saveOptions( Vector options ) {
      try {
        Writer output = new BufferedWriter( new OutputStreamWriter ( new FileOutputStream(new File(GAME_OPTIONS_FILE_NAME)) ) );

        // Output the doctype and header stuff.
          output.write( "<?xml version=\"1.0\"?>" );
          output.write( Settings.NL );
          output.write( "<options>" );
          output.write( Settings.NL );
  
        // Now the options themselves
          for ( int i = 0; i < options.size(); i++ ) {
            final IOption option = (IOption) options.elementAt(i);
  
            output.write( "   <gameoption>" );

            output.write( Settings.NL );
            output.write( "      <optionname>" );
            output.write( option.getName() );
            output.write( "</optionname>" );
            output.write( Settings.NL );
            output.write( "      <optionvalue>" );
            output.write( option.getValue().toString() );
            output.write( "</optionvalue>" );
            output.write( Settings.NL );
  
            output.write( "   </gameoption>" );
            output.write( Settings.NL );
          }
  
        // Finish writing.
          output.write( "</options>" );
          output.write( Settings.NL );
          output.flush();
          output.close();
      } catch (IOException e) {}
    }
    
    
    /* (non-Javadoc)
     * @see megamek.common.options.AbstractOptions#getOptionsInfoImp()
     */
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return GameOptionsInfo.getInstance();
    }

    private static class GameOptionsInfo extends AbstractOptionsInfo {

        private static AbstractOptionsInfo instance = new GameOptionsInfo(); 

        protected GameOptionsInfo() {            
        }

        public static AbstractOptionsInfo getInstance() {
            return instance;
        }
    }
}
