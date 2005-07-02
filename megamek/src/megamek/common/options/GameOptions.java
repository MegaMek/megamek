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
import megamek.common.CommonConstants;

/**
 * Contains the options determining play in the current game.
 *
 * @author Ben
 */
public class GameOptions extends AbstractOptions implements Serializable {

    private static final String GAME_OPTIONS_FILE_NAME = "mmconf/gameoptions.xml"; //$NON-NLS-1$
    
    public GameOptions() {
        super();
    }


    public void initialize() {
        OptionGroup base = addGroup("base"); //$NON-NLS-1$
        addOption(base,"friendly_fire", false); //$NON-NLS-1$
        addOption(base,"skip_ineligable_movement", false); //$NON-NLS-1$
        addOption(base,"skip_ineligable_firing", false); //$NON-NLS-1$
        addOption(base,"skip_ineligable_physical", true); //$NON-NLS-1$
        addOption(base,"push_off_board", true); //$NON-NLS-1$
        addOption(base,"check_victory", true); //$NON-NLS-1$
        addOption(base,"rng_type", 1); //$NON-NLS-1$
        addOption(base,"team_initiative", true);  //$NON-NLS-1$
        addOption(base,"is_eq_limits", true);  //$NON-NLS-1$
        addOption(base,"autosave_msg", true);  //$NON-NLS-1$
        addOption(base,"paranoid_autosave", false);  //$NON-NLS-1$
        addOption(base,"very_paranoid_autosave", false);  //$NON-NLS-1$
        addOption(base,"maps_include_subdir", false); //$NON-NLS-1$
        
        OptionGroup level2 = addGroup("level2"); //$NON-NLS-1$
        addOption(level2,"flamer_heat", true); //$NON-NLS-1$
        addOption(level2,"fire", true); //$NON-NLS-1$
        addOption(level2,"indirect_fire", true); //$NON-NLS-1$
        addOption(level2,"minefields", true); //$NON-NLS-1$
        addOption(level2,"temperature", 25); //$NON-NLS-1$
        addOption(level2,"gravity", (float)1.0); //$NON-NLS-1$
        addOption(level2,"vacuum", false); //$NON-NLS-1$
        addOption(level2,"night_battle", false); //$NON-NLS-1$

        OptionGroup level3 = addGroup("level3"); //$NON-NLS-1$
        addOption(level3,"allow_level_3_units", false);
        addOption(level3,"allow_level_3_ammo", false);
        addOption(level3,"double_blind", false); //$NON-NLS-1$
        addOption(level3,"dusk", false); //$NON-NLS-1$
        addOption(level3,"team_vision", true); //$NON-NLS-1$
        addOption(level3,"floating_crits", false); //$NON-NLS-1$
        addOption(level3,"engine_explosions", false); //$NON-NLS-1$
        addOption(level3,"pilot_advantages", false); //$NON-NLS-1$
        addOption(level3,"maxtech_physical_BTH", false); //$NON-NLS-1$
        addOption(level3,"maxtech_physical_psr", false); //$NON-NLS-1$
        addOption(level3,"maxtech_round_damage", false); //$NON-NLS-1$
        addOption(level3,"maxtech_prone_fire", false); //$NON-NLS-1$
        addOption(level3,"maxtech_target_modifiers", false); //$NON-NLS-1$
        addOption(level3,"maxtech_leg_damage", false); //$NON-NLS-1$
        addOption(level3,"maxtech_fire", false); //$NON-NLS-1$
        addOption(level3,"maxtech_range", false); //$NON-NLS-1$
        addOption(level3,"maxtech_LOS1", false); //$NON-NLS-1$
        addOption(level3,"maxtech_altdmg", false); //$NON-NLS-1$
        addOption(level3,"maxtech_mslhitpen", false); //$NON-NLS-1$
        addOption(level3,"maxtech_ppc_inhibitors", false); //$NON-NLS-1$
        addOption(level3,"maxtech_charge_damage", false); //$NON-NLS-1$
        addOption(level3,"maxtech_glancing_blows", false); //$NON-NLS-1$
        addOption(level3,"maxtech_burst", false); //$NON-NLS-1$
        addOption(level3,"maxtech_heat", false); //$NON-NLS-1$
        addOption(level3,"maxtech_mulekicks", false); //$NON-NLS-1$
        addOption(level3,"maxtech_partial_cover", false); //$NON-NLS-1$
        addOption(level3,"allow_level_3_targsys", false); //$NON-NLS-1$ 

        OptionGroup ruleBreakers = addGroup("ruleBreakers"); //$NON-NLS-1$
        addOption(ruleBreakers,"no_tac", false); //$NON-NLS-1$
        addOption(ruleBreakers,"no_immobile_vehicles", false); //$NON-NLS-1$
        addOption(ruleBreakers,"vehicles_can_eject", false); //$NON-NLS-1$
        addOption(ruleBreakers,"inf_move_even", false); //$NON-NLS-1$
        addOption(ruleBreakers,"inf_deploy_even", false); //$NON-NLS-1$
        addOption(ruleBreakers,"inf_move_later", false); //$NON-NLS-1$
        addOption(ruleBreakers,"inf_move_multi", false); //$NON-NLS-1$
        addOption(ruleBreakers,"protos_move_even", false); //$NON-NLS-1$
        addOption(ruleBreakers,"protos_deploy_even", false); //$NON-NLS-1$
        addOption(ruleBreakers,"protos_move_later", false); //$NON-NLS-1$
        addOption(ruleBreakers,"protos_move_multi", false); //$NON-NLS-1$
        addOption(ruleBreakers,"inf_proto_move_multi", 3); //$NON-NLS-1$
        addOption(ruleBreakers,"blind_drop", false); //$NON-NLS-1$
        addOption(ruleBreakers,"real_blind_drop", false); //$NON-NLS-1$
        addOption(ruleBreakers,"clan_ignore_eq_limits", false); //$NON-NLS-1$
        addOption(ruleBreakers,"no_clan_physical", false); //$NON-NLS-1$
        addOption(ruleBreakers,"no_hover_charge", false); //$NON-NLS-1$
        addOption(ruleBreakers,"woods_burn_down", false); //$NON-NLS-1$
        addOption(ruleBreakers,"vehicles_safe_from_infernos", false); //$NON-NLS-1$
        addOption(ruleBreakers,"protos_safe_from_infernos", false); //$NON-NLS-1$
        addOption(ruleBreakers,"lobby_ammo_dump", false); //$NON-NLS-1$
        addOption(ruleBreakers,"set_arty_player_homeedge", false); //$NON-NLS-1$
        addOption(ruleBreakers,"no_premove_vibra", false); //$NON-NLS-1$
        addOption(ruleBreakers,"auto_spot", false); //$NON-NLS-1$
        addOption(ruleBreakers,"margin_scatter_distance", false); //$NON-NLS-1$
        addOption(ruleBreakers,"allow_illegal_units", false); //$NON-NLS-1$
        addOption(ruleBreakers,"ejected_pilots_flee", false); //$NON-NLS-1$
        addOption(ruleBreakers,"a4homing_target_area", false); //$NON-NLS-1$
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
        System.out.println("Error parsing game options xml file.");  //$NON-NLS-1$
        e.printStackTrace(System.out);
      }
    
      Enumeration rootChildren = root.elements();
      ParsedXML optionsNode = (ParsedXML)rootChildren.nextElement();
      
      if ( optionsNode.getName().equals("options") ) { //$NON-NLS-1$
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
        System.out.println("Root node of game options file is incorrectly named. Name should be 'options' but name is '" + optionsNode.getName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
      }  
    }
    
    private IOption parseOptionNode(ParsedXML node) {
      IOption option = null;
      
      if ( node.getName().equals("gameoption") ) { //$NON-NLS-1$
        Enumeration children = node.elements();
        String name = null;
        Object value = null;
        
        while (children.hasMoreElements()) {
          ParsedXML child = (ParsedXML)children.nextElement();
          
          if ( child.getName().equals("optionname") ) { //$NON-NLS-1$
            name = ((ParsedXML)child.elements().nextElement()).getContent();
          } else if ( child.getName().equals("optionvalue") ) { //$NON-NLS-1$
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
                
                System.out.println("Set option '" + name + "' to '" + value + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                
                option = tempOption;
              } catch ( IllegalArgumentException iaEx ) {
                System.out.println("Error trying to load option '" + name + "' with a value of '" + value + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
              }
            }
          } else {
            System.out.println("Invalid option '" + name + "' when trying to load options file."); //$NON-NLS-1$ //$NON-NLS-2$
          }
        }
      }
      
      return option;
    }
    
    /**
     * Saves the given <code>Vector</code> of <code>IBasicOption</code>
     * @param options <code>Vector</code> of <code>IBasicOption</code>
     */
    public static void saveOptions( Vector options ) {
      try {
        Writer output = new BufferedWriter( new OutputStreamWriter ( new FileOutputStream(new File(GAME_OPTIONS_FILE_NAME)) ) );

        // Output the doctype and header stuff.
          output.write( "<?xml version=\"1.0\"?>" ); //$NON-NLS-1$
          output.write( CommonConstants.NL );
          output.write( "<options>" ); //$NON-NLS-1$
          output.write( CommonConstants.NL );
  
        // Now the options themselves
          for ( int i = 0; i < options.size(); i++ ) {
            final IBasicOption option = (IBasicOption) options.elementAt(i);
  
            output.write( "   <gameoption>" ); //$NON-NLS-1$

            output.write( CommonConstants.NL );
            output.write( "      <optionname>" ); //$NON-NLS-1$
            output.write( option.getName() );
            output.write( "</optionname>" ); //$NON-NLS-1$
            output.write( CommonConstants.NL );
            output.write( "      <optionvalue>" ); //$NON-NLS-1$
            output.write( option.getValue().toString() );
            output.write( "</optionvalue>" ); //$NON-NLS-1$
            output.write( CommonConstants.NL );
  
            output.write( "   </gameoption>" ); //$NON-NLS-1$
            output.write( CommonConstants.NL );
          }
  
        // Finish writing.
          output.write( "</options>" ); //$NON-NLS-1$
          output.write( CommonConstants.NL );
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
            super("GameOptionsInfo"); //$NON-NLS-1$
        }

        public static AbstractOptionsInfo getInstance() {
            return instance;
        }
    }
}
