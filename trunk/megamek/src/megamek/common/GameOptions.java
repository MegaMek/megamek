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

import gd.xml.ParseException;
import gd.xml.tiny.ParsedXML;
import gd.xml.tiny.TinyParser;
import java.util.Enumeration;
import java.util.Vector;
import megamek.client.Client;
import megamek.common.options.GameOption;
import megamek.common.options.OptionGroup;

/**
 * Contains the options determining play in the current game.
 *
 * @author Ben
 */
public class GameOptions extends Options implements Serializable {
    private static final String NL = "\r\n";
    private static final String GAME_OPTIONS_FILE_NAME = "gameoptions.xml";
    
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
        addOption(level3, new GameOption("engine_explosions", "MaxTech engine explosions", "If checked, any time a mech takes 2 or more engine crits in one round, a roll of '12' will cause a cascading engine explosion.", false));
        addOption(level3, new GameOption("pilot_advantages", "MaxTech pilot advantages", "If checked, players can add additional advantages to their pilots through the 'configure mech' window.", false));
        addOption(level3, new GameOption("maxtech_physical_BTH", "MaxTech physical BTHs", "If checked, BTHs for physical attacks will use MaxTech levels. These levels take into account the piloting skill of the attacking unit.", false));
        addOption(level3, new GameOption("maxtech_round_damage", "MaxTech damage per round", "If checked, units will have +1 to their piloting skill roll for every 20 damage taken, not just the first damage. Also, BTH is altered by weight class. Lights get +1 to BTH where as assaults get -2. Mediums and heavies are in between.", false));
        addOption(level3, new GameOption("maxtech_prone_fire", "MaxTech firing while prone", "If checked, mechs that are prone can fire if they are missing one arm. Standard rules disallow firing when prone and missing an arm. All weapons are +1 BTH and weapons mounted in propping arm can not be fired.", false));
        
        OptionGroup ruleBreakers = new OptionGroup("Optional Rules (unofficial)");
        addGroup(ruleBreakers);
        addOption(ruleBreakers, new GameOption("no_tac", "No through-armor criticals", "If checked, rolls of '2' on hit location will only result in a torso hit, and no critical roll.  Only applies to mechs.  Supercedes the floating criticals option.\n\nUnchecked by default.", false));
        addOption(ruleBreakers, new GameOption("no_immobile_vehicles", "Vehicles not immobilized by crits", "If checked, vehicles with a drive or engine hit will not be counted as 'immobile' for purposes of determining to-hit numbers.\n\nUnchecked by default.", false));
        addOption(ruleBreakers, new GameOption("inf_move_last", "Infantry move after Meks and Vehicles", "If checked, all Meks and Vehicles will move before the first Infantry platoon.  The move order of Meks and Vehicles ignores the presence of Infantry.\n\nMutually exclusive with \"" + Game.INF_MOVE_MULTI + " Infantry for every Mek or Vehicle\".\n\nUnchecked by default.", false));
        addOption(ruleBreakers, new GameOption("inf_move_multi", Game.INF_MOVE_MULTI + " Infantry for every Mek or Vehicle", "If checked, " + Game.INF_MOVE_MULTI + " platoons will have to move in place of a single Mek or Vehicle.  If there are less than " + Game.INF_MOVE_MULTI + " platoons remaining, they all must move.  The move order includes the presence of Infantry.\n\nMutually exclusive with \"Infantry move after Meks and Vehicles\".\n\nUnchecked by default.", false));
        addOption(ruleBreakers, new GameOption("blind_drop", "Blind Drop", "If checked, the configuration of a Mech won't be shown in the Chatroom to your opponents.", false)); 
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
          GameOption option = parseOptionNode((ParsedXML)children.nextElement());
          
          if ( null != option )
            changedOptions.addElement(option);
        } 
        
        if ( changedOptions.size() > 0 ) {
          client.sendGameOptions(password, changedOptions);
        }
      } else {
        System.out.println("Root node of game options file is incorrectly named. Name should be 'options' but name is '" + optionsNode.getName() + "'");
      }  
    }
    
    private GameOption parseOptionNode(ParsedXML node) {
      GameOption option = null;
      
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
          GameOption tempOption = this.getOption(name);
          
          if ( null != tempOption ) {
            if ( !tempOption.getValue().toString().equals(value.toString()) ) {
              try {
                switch ( tempOption.getType() ) {
                  case GameOption.STRING:
                    tempOption.setValue((String)value);
                    break;
                    
                  case GameOption.BOOLEAN:
                    tempOption.setValue(new Boolean(value.toString()));
                    break;
                    
                  case GameOption.INTEGER:
                    tempOption.setValue(new Integer(value.toString()));
                    break;
                    
                  case GameOption.FLOAT:
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
          output.write( NL );
          output.write( "<options>" );
          output.write( NL );
  
        // Now the options themselves
          for ( int i = 0; i < options.size(); i++ ) {
            final GameOption option = (GameOption) options.elementAt(i);
  
            output.write( "   <gameoption>" );

            output.write( NL );
            output.write( "      <optionname>" );
            output.write( option.getShortName() );
            output.write( "</optionname>" );
            output.write( NL );
            output.write( "      <optionvalue>" );
            output.write( option.getValue().toString() );
            output.write( "</optionvalue>" );
            output.write( NL );
  
            output.write( "   </gameoption>" );
            output.write( NL );
          }
  
        // Finish writing.
          output.write( "</options>" );
          output.write( NL );
          output.flush();
          output.close();
      } catch (IOException e) {}
    }
}
