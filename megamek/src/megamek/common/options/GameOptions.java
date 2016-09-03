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
        addOption(base, OptionsConstants.BASE_FRIENDLY_FIRE, false); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_SKIP_INELIGABLE_MOVEMENT, false); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_SKIP_INELIGABLE_FIRING, false); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_SKIP_INELIGABLE_PHYSICAL, true); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_PUSH_OFF_BOARD, true); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_TEAM_INITIATIVE, true); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_AUTOSAVE_MSG, true); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_PARANOID_AUTOSAVE, false); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_EXCLUSIVE_DB_DEPLOYMENT, true); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_DEEP_DEPLOYMENT, false); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_BLIND_DROP, false); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_REAL_BLIND_DROP, false); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_LOBBY_AMMO_DUMP, false); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_DUMPING_FROM_ROUND, 1); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_SET_ARTY_PLAYER_HOMEEDGE, false); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_RESTRICT_GAME_COMMANDS, false); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_DISABLE_LOCAL_SAVE, false); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_BRIDGECF, 0); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_SHOW_BAY_DETAIL, false); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_RNG_TYPE, 1); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_RNG_LOG, false); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_FLAMER_HEAT, true); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_INDIRECT_FIRE, true); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_BREEZE, false); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_RANDOM_BASEMENTS, true); //$NON-NLS-1$
        addOption(base, OptionsConstants.BASE_AUTO_AMS, true); //$NON-NLS-1$
        
        IBasicOptionGroup victory = addGroup("victory"); //$NON-NLS-1$
        addOption(victory, OptionsConstants.VICTORY_SKIP_FORCED_VICTORY, false); //$NON-NLS-1$
        addOption(victory, OptionsConstants.VICTORY_CHECK_VICTORY, true); //$NON-NLS-1$
        addOption(victory, OptionsConstants.VICTORY_ACHIEVE_CONDITIONS, 1); //$NON-NLS-1$
        addOption(victory, OptionsConstants.VICTORY_USE_BV_DESTROYED, false); //$NON-NLS-1$
        addOption(victory, OptionsConstants.VICTORY_BV_DESTROYED_PERCENT, 100); //$NON-NLS-1$
        addOption(victory, OptionsConstants.VICTORY_USE_BV_RATIO, false); //$NON-NLS-1$
        addOption(victory, OptionsConstants.VICTORY_BV_RATIO_PERCENT, 300); //$NON-NLS-1$
        addOption(victory, OptionsConstants.VICTORY_USE_GAME_TURN_LIMIT, false); //$NON-NLS-1$
        addOption(victory, OptionsConstants.VICTORY_GAME_TURN_LIMIT, 10); //$NON-NLS-1$
        addOption(victory, OptionsConstants.VICTORY_USE_KILL_COUNT, false); //$NON-NLS-1$
        addOption(victory, OptionsConstants.VICTORY_GAME_KILL_COUNT, 4); //$NON-NLS-1$
        addOption(victory, OptionsConstants.VICTORY_COMMANDER_KILLED, false); //$NON-NLS-1$

        IBasicOptionGroup allowed = addGroup("allowedUnits"); //$NON-NLS-1$
        addOption(allowed, OptionsConstants.ALLOWED_CANON_ONLY, false); //$NON-NLS-1$
        addOption(allowed, OptionsConstants.ALLOWED_YEAR, 3150); //$NON-NLS-1$
        addOption(allowed, "techlevel", IOption.CHOICE, TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_STANDARD]); //$NON-NLS-1$
        addOption(allowed, OptionsConstants.ALLOWED_ALLOW_ILLEGAL_UNITS, false); //$NON-NLS-1$
        addOption(allowed, OptionsConstants.ALLOWED_CLAN_IGNORE_EQ_LIMITS, false); //$NON-NLS-1$
        addOption(allowed, OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL, false); //$NON-NLS-1$
        addOption(allowed, OptionsConstants.ALLOWED_ALLOW_NUKES, false); //$NON-NLS-1$
        addOption(allowed, OptionsConstants.ALLOWED_REALLY_ALLOW_NUKES, false); //$NON-NLS-1$
           
        IBasicOptionGroup advancedRules = addGroup("advancedRules"); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_MINEFIELDS, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_HIDDEN_UNITS, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_DOUBLE_BLIND, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_SENSORS, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_SUPRESS_ALL_DB_MESSAGES, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_SUPPRESS_DB_BV, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_TEAM_VISION, true); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_BAP, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_ECCM, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_GHOST_TARGET, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_GHOST_TARGET_MAX, 5); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_DIG_IN, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_BA_WEIGHT, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_TAKE_COVER, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_ANGEL_ECM, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_BATTLE_WRECK, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_SKIN_OF_THE_TEETH_EJECTION, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_MOBILE_HQS, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_FATIGUE, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_FUMBLES, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_SELF_DESTRUCT, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_TANK_CREWS, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_STRATOPS_QUIRKS, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_STRATOPS_PARTIALREPAIRS, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_ASSAULT_DROP, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_PARATROOPERS, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_INCLUSIVE_SENSOR_RANGE, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_SENSORS_DETECT_ALL, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_MAGSCAN_NOHILLS, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_WOODS_BURN_DOWN, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_WOODS_BURN_DOWN_AMOUNT, 5); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_NO_IGNITE_CLEAR, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_ALL_HAVE_EI_COCKPIT, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_EXTREME_TEMPERATURE_SURVIVAL, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_ARMED_MECHWARRIORS, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_PILOTS_VISUAL_RANGE_ONE, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_PILOTS_CANNOT_SPOT, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_METAL_CONTENT, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_BA_GRAB_BARS, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_MAXTECH_MOVEMENT_MODS, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_ALTERNATE_MASC, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_GEOMETRIC_MEAN_BV, false); //$NON-NLS-1$\
        addOption(advancedRules, OptionsConstants.ADVANCED_REDUCED_OVERHEAT_MODIFIER_BV, false); //$NON-NLS-1$
        addOption(advancedRules, OptionsConstants.ADVANCED_ALTERNATE_PILOT_BV_MOD, false); //$NON-NLS-1$
        

        IBasicOptionGroup advancedCombat = addGroup("advancedCombat"); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_AMS, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_FLOATING_CRITS, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_CRIT_ROLL, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_ENGINE_EXPLOSIONS, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_CALLED_SHOTS, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_PRONE_FIRE, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_START_FIRE, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_RANGE, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_DEAD_ZONES, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_LOS1, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_ALTDMG, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_CLUSTERHITPEN, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_PPC_INHIBITORS, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_CHARGE_DAMAGE, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_BURST, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_HEAT, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_PARTIAL_COVER, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_BA_CRITICALS, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD, false); //$NON-NLS-1$
        
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_RAPID_AC, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_KIND_RAPID_AC, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_GRAPPLING, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_JUMP_JET_ATTACK, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_TRIP_ATTACK, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_ENERGY_WEAPONS, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_GAUSS_WEAPONS, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_RETRACTABLE_BLADES, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_AMMUNITION, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_WOODS_COVER, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_EFFECTIVE, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS, false); //$NON-NLS-1$
        
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_ADVANCED_MECH_HIT_LOCATIONS, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_COOLANT_FAILURE, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_BA_VS_BA, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_NO_TAC, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD_VARIABLE, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD_DIVISOR, 10); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_VEHICLES_SAFE_FROM_INFERNOS, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_PROTOS_SAFE_FROM_INFERNOS, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_INDIRECT_ALWAYS_POSSIBLE, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_INCREASED_AC_DMG, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_INCREASED_ISERLL_RANGE, false); //$NON-NLS-1$
        
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
        addOption(advancedCombat, "hotload_in_game", false); //$NON-NLS-1$
        
        




        

/*        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_UNJAM_UAC, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_UAC_TWOROLLS, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_CLUBS_PUNCH, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_ON_MAP_PREDESIGNATE, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_NUM_HEXES_PREDESIGNATE, 5); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_MAP_AREA_PREDESIGNATE, 1088); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_MAX_EXTERNAL_HEAT, 15); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_CASE_PILOT_DAMAGE, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_NO_FORCED_PRIMARY_TARGETS, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_FULL_ROTOR_HITS, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_FOREST_FIRES_NO_SMOKE, false); //$NON-NLS-1$
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_HOTLOAD_IN_GAME, false); //$NON-NLS-1$
*/
        
        
        
        
        
        
        
        
        
        
        
        
        

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
        addOption(rpg, OptionsConstants.EDGE, false); //$NON-NLS-1$
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
