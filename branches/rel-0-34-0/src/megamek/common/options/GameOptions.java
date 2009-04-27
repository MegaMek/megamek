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

import megamek.common.CommonConstants;

/**
 * Contains the options determining play in the current game.
 *
 * @author Ben
 */
public class GameOptions extends AbstractOptions implements Serializable {

    private static final long serialVersionUID = 4916321960852747706L;
    private static final String GAME_OPTIONS_FILE_NAME = "mmconf/gameoptions.xml"; //$NON-NLS-1$

    public GameOptions() {
        super();
    }

    @Override
    public void initialize() {
        IBasicOptionGroup base = addGroup("basic"); //$NON-NLS-1$
        addOption(base, "friendly_fire", false); //$NON-NLS-1$
        addOption(base, "skip_ineligable_movement", false); //$NON-NLS-1$
        addOption(base, "skip_ineligable_firing", false); //$NON-NLS-1$
        addOption(base, "skip_ineligable_physical", true); //$NON-NLS-1$
        addOption(base, "push_off_board", true); //$NON-NLS-1$
        addOption(base, "team_initiative", true); //$NON-NLS-1$
        addOption(base, "autosave_msg", true); //$NON-NLS-1$
        addOption(base, "paranoid_autosave", false); //$NON-NLS-1$
        addOption(base, "no_force_size_mod", false); //$NON-NLS-1$
        addOption(base, "exclusive_db_deployment", true); //$NON-NLS-1$
        addOption(base, "deep_deployment", false); //$NON-NLS-1$
        addOption(base, "blind_drop", false); //$NON-NLS-1$
        addOption(base, "real_blind_drop", false); //$NON-NLS-1$
        addOption(base, "lobby_ammo_dump", false); //$NON-NLS-1$
        addOption(base, "dumping_from_round", 1); //$NON-NLS-1$
        addOption(base, "set_arty_player_homeedge", false); //$NON-NLS-1$
        addOption(base, "restrict_game_commands", false); //$NON-NLS-1$
        addOption(base, "disable_local_save", false); //$NON-NLS-1$
        addOption(base, "bridgeCF", 0); //$NON-NLS-1$
        addOption(base, "show_bay_detail", false); //$NON-NLS-1$
        addOption(base, "rng_type", 1); //$NON-NLS-1$
        addOption(base, "flamer_heat", true); //$NON-NLS-1$
        addOption(base, "indirect_fire", true); //$NON-NLS-1$  
        
        IBasicOptionGroup victory = addGroup("victory"); //$NON-NLS-1$
        addOption(victory, "skip_forced_victory", false); //$NON-NLS-1$
        addOption(victory, "check_victory", true); //$NON-NLS-1$
        addOption(victory, "achieve_conditions", 1); //$NON-NLS-1$
        addOption(victory, "use_bv_destroyed", false); //$NON-NLS-1$
        addOption(victory, "bv_destroyed_percent", 100); //$NON-NLS-1$
        addOption(victory, "use_bv_ratio", false); //$NON-NLS-1$
        addOption(victory, "bv_ratio_percent", 300); //$NON-NLS-1$
        addOption(victory, "use_game_turn_limit", false); //$NON-NLS-1$
        addOption(victory, "game_turn_limit", 10); //$NON-NLS-1$
        addOption(victory, "commander_killed", false); //$NON-NLS-1$

        IBasicOptionGroup allowed = addGroup("allowedUnits"); //$NON-NLS-1$
        addOption(allowed, "canon_only", false); //$NON-NLS-1$
        addOption(allowed, "allow_advanced_units", false);
        addOption(allowed, "allow_illegal_units", false); //$NON-NLS-1$
        addOption(allowed, "allow_advanced_ammo", false); //$NON-NLS-1$
        addOption(allowed, "is_eq_limits", true); //$NON-NLS-1$
        addOption(allowed, "clan_ignore_eq_limits", false); //$NON-NLS-1$
        addOption(allowed, "no_clan_physical", false); //$NON-NLS-1$
        addOption(allowed, "allow_nukes", false); //$NON-NLS-1$
        addOption(allowed, "really_allow_nukes", false); //$NON-NLS-1$
        
        IBasicOptionGroup advancedRules = addGroup("advancedRules"); //$NON-NLS-1$
        addOption(advancedRules, "minefields", false); //$NON-NLS-1$
//        addOption(advancedRules, "hidden_units", false); //$NON-NLS-1$
        addOption(advancedRules, "double_blind", false); //$NON-NLS-1$
        addOption(advancedRules, "supress_all_double_blind_messages", false); //$NON-NLS-1$
        addOption(advancedRules, "team_vision", true); //$NON-NLS-1$
        addOption(advancedRules, "tacops_bap", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_eccm", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_ghost_target", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_dig_in", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_angel_ecm", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_battle_wreck", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_skin_of_the_teeth_ejection", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_mobile_hqs", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_fatigue", false); //$NON-NLS-1$
        addOption(advancedRules, "assault_drop", false); //$NON-NLS-1$
        addOption(advancedRules, "paratroopers", false); //$NON-NLS-1$
        addOption(advancedRules, "inclusive_sensor_range", false); //$NON-NLS-1$
        addOption(advancedRules, "woods_burn_down", false); //$NON-NLS-1$
        addOption(advancedRules, "woods_burn_down_amount", 5); //$NON-NLS-1$
        addOption(advancedRules, "no_ignite_clear", false); //$NON-NLS-1$
        addOption(advancedRules, "a4homing_target_area", false); //$NON-NLS-1$
        addOption(advancedRules, "all_have_ei_cockpit", false); //$NON-NLS-1$
        addOption(advancedRules, "extreme_temperature_survival", false); //$NON-NLS-1$
        addOption(advancedRules, "armed_mechwarriors", false); //$NON-NLS-1$
        addOption(advancedRules, "pilots_visual_range_one", false); //$NON-NLS-1$
        addOption(advancedRules, "pilots_cannot_spot", false); //$NON-NLS-1$

        IBasicOptionGroup advancedCombat = addGroup("advancedCombat"); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_ams", false); //$NON-NLS-1$
        addOption(advancedCombat, "floating_crits", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_engine_explosions", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_prone_fire", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_start_fire", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_range", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_LOS1", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_altdmg", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_clusterhitpen", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_ppc_inhibitors", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_charge_damage", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_glancing_blows", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_direct_blow", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_burst", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_heat", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_partial_cover", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_ba_criticals", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_hotload", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_rapid_ac", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_grappling", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_jump_jet_attack", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_trip_attack", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_energy_weapons", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_gauss_weapons", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_retractable_blades", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_ammunition", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_woods_cover", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_vehicle_effective", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_vehicle_arcs", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_advanced_mech_hit_locations", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_coolant_failure", false); //$NON-NLS-1$
        addOption(advancedCombat, "allow_level_3_targsys", false); //$NON-NLS-1$
        addOption(advancedCombat, "tank_level_3_targsys", false); //$NON-NLS-1$
        addOption(advancedCombat, "no_tac", false); //$NON-NLS-1$
        addOption(advancedCombat, "vehicles_safe_from_infernos", false); //$NON-NLS-1$
        addOption(advancedCombat, "protos_safe_from_infernos", false); //$NON-NLS-1$
        addOption(advancedCombat, "indirect_always_possible", false); //$NON-NLS-1$      

        IBasicOptionGroup advancedGroundMovement = addGroup("advancedGroundMovement"); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_standing_still", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_evade", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_skilled_evasion", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_physical_psr", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_attack_physical_psr", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_taking_damage", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_leg_damage", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_walk_backwards", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "vehicle_lance_movement", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "vehicle_lance_movement_number", 4); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_hull_down", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_falling_expanded", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_attempting_stand", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_careful_stand", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "no_immobile_vehicles", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "vehicles_can_eject", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "ejected_pilots_flee", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "auto_abandon_unit", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "no_hover_charge", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "no_premove_vibra", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "falls_end_movement", false); //$NON-NLS-1$

        IBasicOptionGroup advAeroRules = addGroup("advancedAeroRules"); //$NON-NLS-1$
        addOption(advAeroRules,"stratops_capital_fighter", false); //$NON-NLS-1$
        addOption(advAeroRules,"fuel_consumption", false); //$NON-NLS-1$
        addOption(advAeroRules,"stratops_conv_fusion_bonus", false); //$NON-NLS-1$
        addOption(advAeroRules,"stratops_harjel", false); //$NON-NLS-1$
        addOption(advAeroRules,"stratops_grav_effects", false); //$NON-NLS-1$
        addOption(advAeroRules,"advanced_movement", false); //$NON-NLS-1$
        addOption(advAeroRules,"heat_by_bay", false); //$NON-NLS-1$
        addOption(advAeroRules,"atmospheric_control", false); //$NON-NLS-1$
        addOption(advAeroRules,"ammo_explosions", false); //$NON-NLS-1$
        addOption(advAeroRules,"stratops_aaa_laser", false); //$NON-NLS-1$
        addOption(advAeroRules,"stratops_bracket_fire", false); //$NON-NLS-1$
        addOption(advAeroRules,"stratops_ecm", false); //$NON-NLS-1$
        addOption(advAeroRules,"stratops_sensor_shadow", false); //$NON-NLS-1$
        addOption(advAeroRules,"stratops_over_penetrate", false); //$NON-NLS-1$
        addOption(advAeroRules,"stratops_space_bomb", false); //$NON-NLS-1$
        addOption(advAeroRules,"variable_damage_thresh", false); //$NON-NLS-1$
        addOption(advAeroRules,"at2_nukes", false); //$NON-NLS-1$
        
        IBasicOptionGroup initiative = addGroup("initiative"); //$NON-NLS-1$
        addOption(initiative, "individual_initiative", false); //$NON-NLS-1$
        addOption(initiative, "command_init", false); //$NON-NLS-1$
        addOption(initiative, "inf_move_even", false); //$NON-NLS-1$
        addOption(initiative, "inf_deploy_even", false); //$NON-NLS-1$
        addOption(initiative, "inf_move_later", false); //$NON-NLS-1$
        addOption(initiative, "inf_move_multi", false); //$NON-NLS-1$
        addOption(initiative, "protos_move_even", false); //$NON-NLS-1$
        addOption(initiative, "protos_deploy_even", false); //$NON-NLS-1$
        addOption(initiative, "protos_move_later", false); //$NON-NLS-1$
        addOption(initiative, "protos_move_multi", false); //$NON-NLS-1$
        addOption(initiative, "inf_proto_move_multi", 3); //$NON-NLS-1$
        //addOption(initiative, "simultaneous_deployment", false); //$NON-NLS-1$
        addOption(initiative, "simultaneous_targeting", false); //$NON-NLS-1$
        //addOption(initiative, "simultaneous_movement", false); //$NON-NLS-1$
        addOption(initiative, "simultaneous_firing", false); //$NON-NLS-1$
        addOption(initiative, "simultaneous_physical", false); //$NON-NLS-1$

        IBasicOptionGroup rpg = addGroup("rpg"); //$NON-NLS-1$
        addOption(rpg, "rpg_gunnery", false); //$NON-NLS-1$
        addOption(rpg, "pilot_advantages", false); //$NON-NLS-1$
        addOption(rpg, "manei_domini", false); //$NON-NLS-1$
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        

        //IBasicOptionGroup advancedBuildings = addGroup("advancedBuildings"); //$NON-NLS-1$

    }

    public Vector<IOption> loadOptions() {
        return loadOptions(new File(GAME_OPTIONS_FILE_NAME));
    }

    public Vector<IOption> loadOptions(File file) {
        ParsedXML root = null;
        InputStream is = null;
        Vector<IOption> changedOptions = new Vector<IOption>(1, 1);

        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            return changedOptions;
        }

        try {
            root = TinyParser.parseXML(is);
        } catch (ParseException e) {
            System.out.println("Error parsing game options xml file."); //$NON-NLS-1$
            e.printStackTrace(System.out);
            return changedOptions;
        }

        Enumeration<?> rootChildren = root.elements();
        ParsedXML optionsNode = (ParsedXML) rootChildren.nextElement();

        if (optionsNode.getName().equals("options")) { //$NON-NLS-1$
            Enumeration<?> children = optionsNode.elements();

            while (children.hasMoreElements()) {
                IOption option = parseOptionNode((ParsedXML) children
                        .nextElement());

                if (null != option) {
                    changedOptions.addElement(option);
                }
            }

            return changedOptions;
        }
        System.out
                .println("Root node of game options file is incorrectly named. Name should be 'options' but name is '" + optionsNode.getName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        return changedOptions;
    }

    private IOption parseOptionNode(ParsedXML node) {
        IOption option = null;

        if (node.getName().equals("gameoption")) { //$NON-NLS-1$
            Enumeration<?> children = node.elements();
            String name = null;
            Object value = null;

            while (children.hasMoreElements()) {
                ParsedXML child = (ParsedXML) children.nextElement();

                if (child.getName().equals("optionname")) { //$NON-NLS-1$
                    name = ((ParsedXML) child.elements().nextElement())
                            .getContent();
                } else if (child.getName().equals("optionvalue")) { //$NON-NLS-1$
                    value = ((ParsedXML) child.elements().nextElement())
                            .getContent();
                }
            }

            if ((null != name) && (null != value)) {
                IOption tempOption = getOption(name);

                if (null != tempOption) {
                    if (!tempOption.getValue().toString().equals(
                            value.toString())) {
                        try {
                            switch (tempOption.getType()) {
                                case IOption.STRING:
                                    tempOption.setValue((String) value);
                                    break;

                                case IOption.BOOLEAN:
                                    tempOption.setValue(new Boolean(value
                                            .toString()));
                                    break;

                                case IOption.INTEGER:
                                    tempOption.setValue(new Integer(value
                                            .toString()));
                                    break;

                                case IOption.FLOAT:
                                    tempOption.setValue(new Float(value
                                            .toString()));
                                    break;
                            }

                            System.out
                                    .println("Set option '" + name + "' to '" + value + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

                            option = tempOption;
                        } catch (IllegalArgumentException iaEx) {
                            System.out
                                    .println("Error trying to load option '" + name + "' with a value of '" + value + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        }
                    }
                } else {
                    System.out
                            .println("Invalid option '" + name + "' when trying to load options file."); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }

        return option;
    }

    /**
     * Saves the given <code>Vector</code> of <code>IBasicOption</code>
     *
     * @param options <code>Vector</code> of <code>IBasicOption</code>
     */
    public static void saveOptions(Vector<IBasicOption> options) {
        try {
            Writer output = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(new File(GAME_OPTIONS_FILE_NAME))));

            // Output the doctype and header stuff.
            output.write("<?xml version=\"1.0\"?>"); //$NON-NLS-1$
            output.write(CommonConstants.NL);
            output.write("<options>"); //$NON-NLS-1$
            output.write(CommonConstants.NL);

            // Now the options themselves
            for (int i = 0; i < options.size(); i++) {
                final IBasicOption option = options.elementAt(i);

                output.write("   <gameoption>"); //$NON-NLS-1$

                output.write(CommonConstants.NL);
                output.write("      <optionname>"); //$NON-NLS-1$
                output.write(option.getName());
                output.write("</optionname>"); //$NON-NLS-1$
                output.write(CommonConstants.NL);
                output.write("      <optionvalue>"); //$NON-NLS-1$
                output.write(option.getValue().toString());
                output.write("</optionvalue>"); //$NON-NLS-1$
                output.write(CommonConstants.NL);

                output.write("   </gameoption>"); //$NON-NLS-1$
                output.write(CommonConstants.NL);
            }

            // Finish writing.
            output.write("</options>"); //$NON-NLS-1$
            output.write(CommonConstants.NL);
            output.flush();
            output.close();
        } catch (IOException e) {
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.options.AbstractOptions#getOptionsInfoImp()
     */
    @Override
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