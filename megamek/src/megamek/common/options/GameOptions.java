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

import java.io.File;
import java.util.Vector;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import megamek.common.TechConstants;

/**
 * Contains the options determining play in the current game.
 *
 * @author Ben
 */
public class GameOptions extends AbstractOptions {
    private static final long serialVersionUID = 4916321960852747706L;
    private static final String GAME_OPTIONS_FILE_NAME = "mmconf/gameoptions.xml"; //$NON-NLS-1$

    public GameOptions() {
        super();
    }

    @Override
    public synchronized void initialize() {
        IBasicOptionGroup base = addGroup("basic"); //$NON-NLS-1$
        addOption(base, "friendly_fire", false); //$NON-NLS-1$
        addOption(base, "skip_ineligable_movement", false); //$NON-NLS-1$
        addOption(base, "skip_ineligable_firing", false); //$NON-NLS-1$
        addOption(base, "skip_ineligable_physical", true); //$NON-NLS-1$
        addOption(base, "push_off_board", true); //$NON-NLS-1$
        addOption(base, "team_initiative", true); //$NON-NLS-1$
        addOption(base, "autosave_msg", true); //$NON-NLS-1$
        addOption(base, "paranoid_autosave", false); //$NON-NLS-1$
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
        addOption(base, "rng_log", false); //$NON-NLS-1$
        addOption(base, "flamer_heat", true); //$NON-NLS-1$
        addOption(base, "indirect_fire", true); //$NON-NLS-1$
        addOption(base, "breeze", false); //$NON-NLS-1$
        addOption(base, "random_basements", true); //$NON-NLS-1$
        addOption(base, "auto_ams", true); //$NON-NLS-1$

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
        addOption(victory, "use_kill_count", false); //$NON-NLS-1$
        addOption(victory, "game_kill_count", 4); //$NON-NLS-1$
        addOption(victory, "commander_killed", false); //$NON-NLS-1$

        IBasicOptionGroup allowed = addGroup("allowedUnits"); //$NON-NLS-1$
        addOption(allowed, "canon_only", false); //$NON-NLS-1$
        addOption(allowed, "year", 3150); //$NON-NLS-1$
        addOption(allowed, "techlevel", IOption.CHOICE, TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_STANDARD]); //$NON-NLS-1$
        addOption(allowed, "allow_illegal_units", false); //$NON-NLS-1$
        addOption(allowed, "clan_ignore_eq_limits", false); //$NON-NLS-1$
        addOption(allowed, "no_clan_physical", false); //$NON-NLS-1$
        addOption(allowed, "allow_nukes", false); //$NON-NLS-1$
        addOption(allowed, "really_allow_nukes", false); //$NON-NLS-1$

        IBasicOptionGroup advancedRules = addGroup("advancedRules"); //$NON-NLS-1$
        addOption(advancedRules, "minefields", false); //$NON-NLS-1$
        addOption(advancedRules, "hidden_units", false); //$NON-NLS-1$
        addOption(advancedRules, "double_blind", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_sensors", false); //$NON-NLS-1$
        addOption(advancedRules, "supress_all_double_blind_messages", false); //$NON-NLS-1$
        addOption(advancedRules, "suppress_double_blind_bv", false); //$NON-NLS-1$
        addOption(advancedRules, "team_vision", true); //$NON-NLS-1$
        addOption(advancedRules, "tacops_bap", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_eccm", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_ghost_target", false); //$NON-NLS-1$
        addOption(advancedRules, "ghost_target_max", 5); //$NON-NLS-1$
        addOption(advancedRules, "tacops_dig_in", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_ba_weight", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_take_cover", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_angel_ecm", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_battle_wreck", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_skin_of_the_teeth_ejection", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_mobile_hqs", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_fatigue", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_fumbles", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_self_destruct", false); //$NON-NLS-1$
        addOption(advancedRules, "tacops_tank_crews", false); //$NON-NLS-1$
        addOption(advancedRules, "stratops_quirks", false); //$NON-NLS-1$
        addOption(advancedRules, "stratops_partialrepairs", false); //$NON-NLS-1$
        addOption(advancedRules, "assault_drop", false); //$NON-NLS-1$
        addOption(advancedRules, "paratroopers", false); //$NON-NLS-1$
        addOption(advancedRules, "inclusive_sensor_range", false); //$NON-NLS-1$
        addOption(advancedRules, "sensors_detect_all", false); //$NON-NLS-1$
        addOption(advancedRules, "magscan_nohills", false); //$NON-NLS-1$
        addOption(advancedRules, "woods_burn_down", false); //$NON-NLS-1$
        addOption(advancedRules, "woods_burn_down_amount", 5); //$NON-NLS-1$
        addOption(advancedRules, "no_ignite_clear", false); //$NON-NLS-1$
        addOption(advancedRules, "all_have_ei_cockpit", false); //$NON-NLS-1$
        addOption(advancedRules, "extreme_temperature_survival", false); //$NON-NLS-1$
        addOption(advancedRules, "armed_mechwarriors", false); //$NON-NLS-1$
        addOption(advancedRules, "pilots_visual_range_one", false); //$NON-NLS-1$
        addOption(advancedRules, "pilots_cannot_spot", false); //$NON-NLS-1$
        addOption(advancedRules, "metal_content", false); //$NON-NLS-1$
        addOption(advancedRules, "ba_grab_bars", false); //$NON-NLS-1$
        addOption(advancedRules, "maxtech_movement_mods", false); //$NON-NLS-1$
        addOption(advancedRules, "alternate_masc", false); //$NON-NLS-1$
        addOption(advancedRules, "alternate_masc_enhanced", false); //$NON-NLS-1$
        addOption(advancedRules, "geometric_mean_bv", false); //$NON-NLS-1$\
        addOption(advancedRules, "reduced_overheat_modifier_bv", false); //$NON-NLS-1$
        addOption(advancedRules, "alternate_pilot_bv_mod", false); //$NON-NLS-1$

        IBasicOptionGroup advancedCombat = addGroup("advancedCombat"); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_ams", false); //$NON-NLS-1$
        addOption(advancedCombat, "floating_crits", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_crit_roll", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_engine_explosions", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_called_shots", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_prone_fire", false); //$NON-NLS-1$
        addOption(advancedCombat, "tacops_start_fire", false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.AC_TAC_OPS_RANGE, false);
        addOption(advancedCombat, OptionsConstants.AC_TAC_OPS_LOS_RANGE, false);
        addOption(advancedCombat, "tacops_dead_zones", false); //$NON-NLS-1$
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
        addOption(advancedCombat, "kind_rapid_ac", false); //$NON-NLS-1$
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
        addOption(advancedCombat, "tacops_ba_vs_ba", false); //$NON-NLS-1$
        addOption(advancedCombat, "no_tac", false); //$NON-NLS-1$
        addOption(advancedCombat, "vehicles_threshold", false); //$NON-NLS-1$
        addOption(advancedCombat, "vehicles_threshold_variable", false); //$NON-NLS-1$
        addOption(advancedCombat, "vehicles_threshold_divisor", 10); //$NON-NLS-1$
        addOption(advancedCombat, "vehicles_safe_from_infernos", false); //$NON-NLS-1$
        addOption(advancedCombat, "protos_safe_from_infernos", false); //$NON-NLS-1$
        addOption(advancedCombat, "indirect_always_possible", false); //$NON-NLS-1$
        addOption(advancedCombat, "increased_ac_dmg", false); //$NON-NLS-1$
        addOption(advancedCombat, "increased_iserll_range", false); //$NON-NLS-1$
        addOption(advancedCombat, "unjam_uac", false); //$NON-NLS-1$
        addOption(advancedCombat, "uac_tworolls", false); //$NON-NLS-1$
        addOption(advancedCombat, "clubs_punch", false); //$NON-NLS-1$
        addOption(advancedCombat, "on_map_predesignate", false); //$NON-NLS-1$
        addOption(advancedCombat, "num_hexes_predesignate", 5); //$NON-NLS-1$
        addOption(advancedCombat, "map_area_predesignate", 1088); //$NON-NLS-1$
        addOption(advancedCombat, "max_external_heat", 15); //$NON-NLS-1$
        addOption(advancedCombat, "case_pilot_damage", false); //$NON-NLS-1$
        addOption(advancedCombat, "no_forced_primary_targets", false); //$NON-NLS-1$
        addOption(advancedCombat, "full_rotor_hits", false); //$NON-NLS-1$
        addOption(advancedCombat, "forest_fires_no_smoke", false); //$NON-NLS-1$

        IBasicOptionGroup advancedGroundMovement = addGroup("advancedGroundMovement"); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_sprint", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, OptionsConstants.AGM_TAC_OPS_STANDING_STILL, false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_evade", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_skilled_evasion", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_leaping", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_physical_psr", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, OptionsConstants.AGM_TAC_OPS_PHYSICAL_ATTACK_PSR, false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_taking_damage", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_leg_damage", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_walk_backwards", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_fast_infantry_move", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "vehicle_lance_movement", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "vehicle_lance_movement_number", 4); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_hull_down", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_falling_expanded", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_attempting_stand", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_careful_stand", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "tacops_ziplines", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "mek_lance_movement", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "mek_lance_movement_number", 4); //$NON-NLS-1$
        addOption(advancedGroundMovement, "no_immobile_vehicles", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, OptionsConstants.AGM_VEHICLES_CAN_EJECT, false);
        addOption(advancedGroundMovement, OptionsConstants.AGM_EJECTED_PILOTS_FLEE, false);
        addOption(advancedGroundMovement, "auto_abandon_unit", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "no_hover_charge", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "no_premove_vibra", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "falls_end_movement", false); //$NON-NLS-1$
        addOption(advancedGroundMovement, "psr_jump_heavy_woods", false);
        addOption(advancedGroundMovement, "no_night_move_pen", false); //$NON-NLS-1$


        IBasicOptionGroup advAeroRules = addGroup("advancedAeroRules"); //$NON-NLS-1$
        addOption(advAeroRules, "aero_ground_move", true); //$NON-NLS-1$
        addOption(advAeroRules, "stratops_capital_fighter", false); //$NON-NLS-1$
        addOption(advAeroRules, "fuel_consumption", false); //$NON-NLS-1$
        addOption(advAeroRules, "stratops_conv_fusion_bonus", false); //$NON-NLS-1$
        addOption(advAeroRules, "stratops_harjel", false); //$NON-NLS-1$
        addOption(advAeroRules, "stratops_grav_effects", false); //$NON-NLS-1$
        addOption(advAeroRules, "advanced_movement", false); //$NON-NLS-1$
        addOption(advAeroRules, "heat_by_bay", false); //$NON-NLS-1$
        addOption(advAeroRules, "atmospheric_control", false); //$NON-NLS-1$
        addOption(advAeroRules, "ammo_explosions", false); //$NON-NLS-1$
        addOption(advAeroRules, "stratops_aa_fire", false); //$NON-NLS-1$
        addOption(advAeroRules, "stratops_aaa_laser", false); //$NON-NLS-1$
        addOption(advAeroRules, "stratops_bracket_fire", false); //$NON-NLS-1$
        addOption(advAeroRules, "stratops_ecm", false); //$NON-NLS-1$
        addOption(advAeroRules, "stratops_sensor_shadow", false); //$NON-NLS-1$
        addOption(advAeroRules, "stratops_over_penetrate", false); //$NON-NLS-1$
        addOption(advAeroRules, "stratops_space_bomb", false); //$NON-NLS-1$
        addOption(advAeroRules, "variable_damage_thresh", false); //$NON-NLS-1$
        addOption(advAeroRules, "at2_nukes", false); //$NON-NLS-1$
        addOption(advAeroRules, "aero_sanity", false); //$NON-NLS-1$
        addOption(advAeroRules, "ind_weapons_grounded_dropper", false); //$NON-NLS-1$
        addOption(advAeroRules, "return_flyover", false); //$NON-NLS-1$
        addOption(advAeroRules, "aa_move_mod", false); //$NON-NLS-1$
        addOption(advAeroRules, "allow_large_squadrons", false); //$NON-NLS-1$
        addOption(advAeroRules, "single_no_cap", false); //$NON-NLS-1$

        IBasicOptionGroup initiative = addGroup("initiative"); //$NON-NLS-1$
        addOption(initiative, "inf_move_even", false); //$NON-NLS-1$
        addOption(initiative, "inf_deploy_even", false); //$NON-NLS-1$
        addOption(initiative, "inf_move_later", false); //$NON-NLS-1$
        addOption(initiative, "inf_move_multi", false); //$NON-NLS-1$
        addOption(initiative, "protos_move_even", false); //$NON-NLS-1$
        addOption(initiative, "protos_deploy_even", false); //$NON-NLS-1$
        addOption(initiative, "protos_move_later", false); //$NON-NLS-1$
        addOption(initiative, "protos_move_multi", false); //$NON-NLS-1$
        addOption(initiative, "inf_proto_move_multi", 3); //$NON-NLS-1$
        addOption(initiative, "simultaneous_deployment", false); //$NON-NLS-1$
        addOption(initiative, "simultaneous_movement", false); //$NON-NLS-1$
        addOption(initiative, "simultaneous_targeting", false); //$NON-NLS-1$
        addOption(initiative, "simultaneous_firing", false); //$NON-NLS-1$
        addOption(initiative, "simultaneous_physical", false); //$NON-NLS-1$
        addOption(initiative, "front_load_initiative", false); //$NON-NLS-1$
        addOption(initiative, "initiative_streak_compensation", false); //$NON-NLS-1$

        IBasicOptionGroup rpg = addGroup("rpg"); //$NON-NLS-1
        addOption(rpg, "pilot_advantages", false); //$NON-NLS-1$
        addOption(rpg, "edge", false); //$NON-NLS-1$
        addOption(rpg, "manei_domini", false); //$NON-NLS-1$
        addOption(rpg, "individual_initiative", false); //$NON-NLS-1$
        addOption(rpg, "command_init", false); //$NON-NLS-1$
        addOption(rpg, "rpg_gunnery", false); //$NON-NLS-1$
        addOption(rpg, "artillery_skill", false); //$NON-NLS-1$
        addOption(rpg, "toughness", false); //$NON-NLS-1$
        addOption(rpg, "conditional_ejection", false); //$NON-NLS-1$
        addOption(rpg, "manual_shutdown", false); //$NON-NLS-1$
        addOption(rpg, "begin_shutdown", false); //$NON-NLS-1$

        //IBasicOptionGroup advancedBuildings = addGroup("advancedBuildings"); //$NON-NLS-1$

    }

    public Vector<IOption> loadOptions() {
        return loadOptions(new File(GAME_OPTIONS_FILE_NAME), true);
    }

    public synchronized Vector<IOption> loadOptions(File file, boolean print) {
        Vector<IOption> changedOptions = new Vector<IOption>(1, 1);

        if (!file.exists()) {
            return changedOptions;
        }
        
        try {
            JAXBContext jc = JAXBContext.newInstance(GameOptionsXML.class, Option.class, BasicOption.class);
            
            Unmarshaller um = jc.createUnmarshaller();
            GameOptionsXML opts = (GameOptionsXML) um.unmarshal(file);

            for (IBasicOption bo : opts.getOptions()) {
                changedOptions.add(parseOptionNode(bo, print));
            }
        } catch (JAXBException ex) {
            System.err.println("Error loading XML for game options: " + ex.getMessage()); //$NON-NLS-1$
            ex.printStackTrace();
        }

        return changedOptions;
    }

    private IOption parseOptionNode(final IBasicOption node, final boolean print) {
        IOption option = null;

        String name = node.getName();
        Object value = node.getValue();
        if ((null != name) && (null != value)) {
            IOption tempOption = getOption(name);

            if (null != tempOption) {
                if (!tempOption.getValue().toString()
                        .equals(value.toString())) {
                    try {
                        switch (tempOption.getType()) {
                            case IOption.STRING:
                            case IOption.CHOICE:
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
                        if (print) {
                            System.out.println("Set option '" + name //$NON-NLS-1$
                                    + "' to '" + value + "'."); //$NON-NLS-1$ //$NON-NLS-2$
                        }

                        option = tempOption;
                    } catch (IllegalArgumentException iaEx) {
                        System.out.println("Error trying to load option '"
                                + name + "' with a value of '" + value
                                + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    }
                }
            } else {
                System.out.println("Invalid option '" + name
                        + "' when trying to load options file.");
                //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return option;
    }

    public static void saveOptions(Vector<IBasicOption> options) {
        saveOptions(options, GAME_OPTIONS_FILE_NAME);
    }

    /**
     * Saves the given <code>Vector</code> of <code>IBasicOption</code>
     *
     * @param options <code>Vector</code> of <code>IBasicOption</code>
     * @param file
     */
    public static void saveOptions(Vector<IBasicOption> options, String file) {
        try {
            JAXBContext jc = JAXBContext.newInstance(GameOptionsXML.class, Option.class, BasicOption.class);
            
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            
            // The default header has the encoding and standalone properties
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.setProperty("com.sun.xml.internal.bind.xmlHeaders", "<?xml version=\"1.0\"?>");
            
            JAXBElement<GameOptionsXML> element = new JAXBElement<>(new QName("options"), GameOptionsXML.class, new GameOptionsXML(options));
            
            marshaller.marshal(element, new File(file));
        } catch (JAXBException ex) {
            System.err.println("Error writing XML for game options: " + ex.getMessage()); //$NON-NLS-1$
            ex.printStackTrace();
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
    
    /**
     * A helper class for the XML binding.
     */
    @XmlRootElement(name = "options")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class GameOptionsXML {

        @XmlElement(name = "gameoption", type = BasicOption.class)
        private Vector<IBasicOption> options;
        
        GameOptionsXML(final Vector<IBasicOption> options) {
            this.options = options;
        }
        
        /**
         * Required for JAXB.
         */
        @SuppressWarnings("unused")
        private GameOptionsXML() {
        }

        public Vector<IBasicOption> getOptions() {
            return options;
        }
    }
}
