/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.options;

import jakarta.xml.bind.*;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import megamek.common.TechConstants;
import megamek.utilities.xml.MMXMLUtility;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Contains the options determining play in the current game.
 *
 * @author Ben
 */
public class GameOptions extends AbstractOptions {
    private static final long serialVersionUID = 4916321960852747706L;
    private static final String GAME_OPTIONS_FILE_NAME = "mmconf/gameoptions.xml";

    public GameOptions() {
        super();
    }

    @Override
    public synchronized void initialize() {
        IBasicOptionGroup base = addGroup("basic");
        addOption(base, OptionsConstants.BASE_FRIENDLY_FIRE, false);
        addOption(base, OptionsConstants.BASE_SKIP_INELIGABLE_MOVEMENT, false);
        addOption(base, OptionsConstants.BASE_SKIP_INELIGABLE_FIRING, false);
        addOption(base, OptionsConstants.BASE_SKIP_INELIGABLE_PHYSICAL, true);
        addOption(base, OptionsConstants.BASE_PUSH_OFF_BOARD, true);
        addOption(base, OptionsConstants.BASE_TEAM_INITIATIVE, true);
        addOption(base, OptionsConstants.BASE_AUTOSAVE_MSG, true);
        addOption(base, OptionsConstants.BASE_PARANOID_AUTOSAVE, false);
        addOption(base, OptionsConstants.BASE_EXCLUSIVE_DB_DEPLOYMENT, true);
        addOption(base, OptionsConstants.BASE_BLIND_DROP, false);
        addOption(base, OptionsConstants.BASE_REAL_BLIND_DROP, false);
        addOption(base, OptionsConstants.BASE_LOBBY_AMMO_DUMP, false);
        addOption(base, OptionsConstants.BASE_DUMPING_FROM_ROUND, 1);
        addOption(base, OptionsConstants.BASE_SET_ARTY_PLAYER_HOMEEDGE, false);
        addOption(base, OptionsConstants.BASE_SET_DEFAULT_TEAM_1, false);
        addOption(base, OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER0, false);
        addOption(base, OptionsConstants.BASE_RESTRICT_GAME_COMMANDS, false);
        addOption(base, OptionsConstants.BASE_DISABLE_LOCAL_SAVE, false);
        addOption(base, OptionsConstants.BASE_BRIDGECF, 0);
        addOption(base, OptionsConstants.BASE_SHOW_BAY_DETAIL, false);
        addOption(base, OptionsConstants.BASE_RNG_TYPE, 1);
        addOption(base, OptionsConstants.BASE_RNG_LOG, false);
        addOption(base, OptionsConstants.BASE_FLAMER_HEAT, false);
        addOption(base, OptionsConstants.BASE_INFANTRY_DAMAGE_HEAT, false);
        addOption(base, OptionsConstants.BASE_INDIRECT_FIRE, true);
        addOption(base, OptionsConstants.BASE_BREEZE, false);
        addOption(base, OptionsConstants.BASE_RANDOM_BASEMENTS, true);
        addOption(base, OptionsConstants.BASE_AUTO_AMS, true);
        addOption(base, OptionsConstants.BASE_TURN_TIMER, 0);
        addOption(base, OptionsConstants.BASE_SUPPRESS_UNIT_TOOLTIP_IN_REPORT_LOG, false);
        addOption(base, OptionsConstants.BASE_GM_CONTROLS_DONE_REPORT_PHASE, false);
        addOption(base, OptionsConstants.BASE_HIDE_UNOFFICIAL, false);
        addOption(base, OptionsConstants.BASE_HIDE_LEGACY, false);

        IBasicOptionGroup victory = addGroup("victory");
        addOption(victory, OptionsConstants.VICTORY_SKIP_FORCED_VICTORY, false);
        addOption(victory, OptionsConstants.VICTORY_CHECK_VICTORY, true);
        addOption(victory, OptionsConstants.VICTORY_ACHIEVE_CONDITIONS, 1);
        addOption(victory, OptionsConstants.VICTORY_USE_BV_DESTROYED, false);
        addOption(victory, OptionsConstants.VICTORY_BV_DESTROYED_PERCENT, 100);
        addOption(victory, OptionsConstants.VICTORY_USE_BV_RATIO, false);
        addOption(victory, OptionsConstants.VICTORY_BV_RATIO_PERCENT, 300);
        addOption(victory, OptionsConstants.VICTORY_USE_GAME_TURN_LIMIT, false);
        addOption(victory, OptionsConstants.VICTORY_GAME_TURN_LIMIT, 10);
        addOption(victory, OptionsConstants.VICTORY_USE_KILL_COUNT, false);
        addOption(victory, OptionsConstants.VICTORY_GAME_KILL_COUNT, 4);
        addOption(victory, OptionsConstants.VICTORY_COMMANDER_KILLED, false);

        IBasicOptionGroup allowed = addGroup("allowedUnits");
        addOption(allowed, OptionsConstants.ALLOWED_CANON_ONLY, false);
        addOption(allowed, OptionsConstants.ALLOWED_YEAR, 3150);
        addOption(allowed, OptionsConstants.ALLOWED_TECHLEVEL, IOption.CHOICE,
                TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_STANDARD]);
        addOption(allowed, OptionsConstants.ALLOWED_ERA_BASED, false);
        addOption(allowed, OptionsConstants.ALLOWED_ALLOW_ILLEGAL_UNITS, false);
        addOption(allowed, OptionsConstants.ALLOWED_SHOW_EXTINCT, true);
        addOption(allowed, OptionsConstants.ALLOWED_CLAN_IGNORE_EQ_LIMITS, false); 
        addOption(allowed, OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL, false); 
        addOption(allowed, OptionsConstants.ALLOWED_ALLOW_NUKES, false); 
        addOption(allowed, OptionsConstants.ALLOWED_REALLY_ALLOW_NUKES, false); 
           
        IBasicOptionGroup advancedRules = addGroup("advancedRules"); 
        addOption(advancedRules, OptionsConstants.ADVANCED_MINEFIELDS, false); 
        addOption(advancedRules, OptionsConstants.ADVANCED_HIDDEN_UNITS, true); 
        addOption(advancedRules, OptionsConstants.ADVANCED_BLACK_ICE, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_DOUBLE_BLIND, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_SENSORS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_SUPRESS_ALL_DB_MESSAGES, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_SUPPRESS_DB_BV, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TEAM_VISION, true);
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_BAP, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_ECCM, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_GHOST_TARGET, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_GHOST_TARGET_MAX, 5);
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_DIG_IN, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_BA_WEIGHT, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_TAKE_COVER, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_ANGEL_ECM, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_BATTLE_WRECK, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_SKIN_OF_THE_TEETH_EJECTION, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_MOBILE_HQS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_FATIGUE, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_FUMBLES, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_SELF_DESTRUCT, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TACOPS_TANK_CREWS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_STRATOPS_QUIRKS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_STRATOPS_PARTIALREPAIRS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_ASSAULT_DROP, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_PARATROOPERS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_INCLUSIVE_SENSOR_RANGE, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_SENSORS_DETECT_ALL, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_MAGSCAN_NOHILLS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_WOODS_BURN_DOWN, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_WOODS_BURN_DOWN_AMOUNT, 5);
        addOption(advancedRules, OptionsConstants.ADVANCED_NO_IGNITE_CLEAR, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_ALL_HAVE_EI_COCKPIT, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_EXTREME_TEMPERATURE_SURVIVAL, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_ARMED_MECHWARRIORS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_PILOTS_VISUAL_RANGE_ONE, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_PILOTS_CANNOT_SPOT, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_METAL_CONTENT, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_BA_GRAB_BARS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_MAXTECH_MOVEMENT_MODS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_ALTERNATE_MASC, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_SINGLE_BLIND_BOTS, false);

        IBasicOptionGroup advancedCombat = addGroup("advancedCombat");
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_AMS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_MANUAL_AMS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_FLOATING_CRITS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_CRIT_ROLL, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_ENGINE_EXPLOSIONS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_CALLED_SHOTS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_PRONE_FIRE, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_START_FIRE, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_RANGE, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_DEAD_ZONES, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_LOS1, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_ALTDMG, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_CLUSTERHITPEN, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_PPC_INHIBITORS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_CHARGE_DAMAGE, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_BURST, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_HEAT, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_PARTIAL_COVER, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_BA_CRITICALS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_RAPID_AC, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_KIND_RAPID_AC, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_GRAPPLING, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_JUMP_JET_ATTACK, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_TRIP_ATTACK, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_ENERGY_WEAPONS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_GAUSS_WEAPONS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_RETRACTABLE_BLADES, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_AMMUNITION, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_WOODS_COVER, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_EFFECTIVE, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_VTOL_ATTACKS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_ADVANCED_MECH_HIT_LOCATIONS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_COOLANT_FAILURE, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_TACOPS_BA_VS_BA, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_NO_TAC, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD_VARIABLE, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD_DIVISOR, 10);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_VTOL_STRAFING, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_VEHICLES_SAFE_FROM_INFERNOS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_PROTOS_SAFE_FROM_INFERNOS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_INDIRECT_ALWAYS_POSSIBLE, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_INCREASED_AC_DMG, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_INCREASED_ISERLL_RANGE, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_UNJAM_UAC, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_UAC_TWOROLLS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_CLUBS_PUNCH, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_ON_MAP_PREDESIGNATE, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_NUM_HEXES_PREDESIGNATE, 5);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_MAP_AREA_PREDESIGNATE, 1088);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_MAX_EXTERNAL_HEAT, 15);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_CASE_PILOT_DAMAGE, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_NO_FORCED_PRIMARY_TARGETS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_FULL_ROTOR_HITS, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_FOREST_FIRES_NO_SMOKE, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_HOTLOAD_IN_GAME, false);
        addOption(advancedCombat, OptionsConstants.ADVCOMBAT_MULTI_USE_AMS, false);

        IBasicOptionGroup advancedGroundMovement = addGroup("advancedGroundMovement");
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_TACOPS_SPRINT, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_TACOPS_STANDING_STILL, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_TACOPS_EVADE, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_TACOPS_SKILLED_EVASION, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_TACOPS_LEAPING, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_TACOPS_PHYSICAL_PSR, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_TACOPS_PHYSICAL_ATTACK_PSR, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_TACOPS_TAKING_DAMAGE, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_TACOPS_WALK_BACKWARDS, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_TACOPS_FAST_INFANTRY_MOVE, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_TACOPS_INF_PAVE_BONUS, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_VEHICLE_LANCE_MOVEMENT, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_VEHICLE_LANCE_MOVEMENT_NUMBER, 4);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_VEHICLE_ACCELERATION, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_REVERSE_GEAR, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_TURN_MODE, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_VEHICLE_ADVANCED_MANEUVERS, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_TACOPS_FALLING_EXPANDED, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_TACOPS_ATTEMPTING_STAND, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_TACOPS_CAREFUL_STAND, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_TACOPS_ZIPLINES, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_MEK_LANCE_MOVEMENT, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_MEK_LANCE_MOVEMENT_NUMBER, 4);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_NO_IMMOBILE_VEHICLES, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_VEHICLES_CAN_EJECT, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_EJECTED_PILOTS_FLEE, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_AUTO_ABANDON_UNIT, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_NO_HOVER_CHARGE, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_NO_PREMOVE_VIBRA, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_FALLS_END_MOVEMENT, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_PSR_JUMP_HEAVY_WOODS, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVGRNDMOV_NO_NIGHT_MOVE_PEN, false);

        IBasicOptionGroup advAeroRules = addGroup("advancedAeroRules");
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_AERO_GROUND_MOVE, true);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_STRATOPS_CAPITAL_FIGHTER, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_FUEL_CONSUMPTION, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_STRATOPS_CONV_FUSION_BONUS, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_STRATOPS_HARJEL, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_STRATOPS_GRAV_EFFECTS, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_ADVANCED_MOVEMENT, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_HEAT_BY_BAY, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_ATMOSPHERIC_CONTROL, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_STRATOPS_AMMO_EXPLOSIONS, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_STRATOPS_AAA_LASER, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_STRATOPS_ADV_POINTDEF, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_STRATOPS_BRACKET_FIRE, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_STRATOPS_ECM, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_STRATOPS_SENSOR_SHADOW, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_STRATOPS_OVER_PENETRATE, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_STRATOPS_SPACE_BOMB, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_STRATOPS_BEARINGS_ONLY_LAUNCH, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_STRATOPS_BEARINGS_ONLY_VELOCITY, 50);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_STRATOPS_WAYPOINT_LAUNCH, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_VARIABLE_DAMAGE_THRESH, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_AT2_NUKES, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_AERO_SANITY, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_RETURN_FLYOVER, true);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_STRATOPS_AA_FIRE, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_AA_MOVE_MOD, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_ALLOW_LARGE_SQUADRONS, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_SINGLE_NO_CAP, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_AERO_ARTILLERY_MUNITIONS, false);
        addOption(advAeroRules, OptionsConstants.ADVAERORULES_EXPANDED_KF_DRIVE_DAMAGE, false);

        IBasicOptionGroup initiative = addGroup("initiative");
        addOption(initiative, OptionsConstants.INIT_INF_MOVE_EVEN, false);
        addOption(initiative, OptionsConstants.INIT_INF_DEPLOY_EVEN, false);
        addOption(initiative, OptionsConstants.INIT_INF_MOVE_LATER, false);
        addOption(initiative, OptionsConstants.INIT_INF_MOVE_MULTI, false);
        addOption(initiative, OptionsConstants.INIT_PROTOS_MOVE_EVEN, false);
        addOption(initiative, OptionsConstants.INIT_PROTOS_MOVE_EVEN, false);
        addOption(initiative, OptionsConstants.INIT_PROTOS_MOVE_LATER, false);
        addOption(initiative, OptionsConstants.INIT_PROTOS_MOVE_MULTI, false);
        addOption(initiative, OptionsConstants.INIT_INF_PROTO_MOVE_MULTI, 3);
        addOption(initiative, OptionsConstants.INIT_SIMULTANEOUS_DEPLOYMENT, false);
        addOption(initiative, OptionsConstants.INIT_SIMULTANEOUS_MOVEMENT, false);
        addOption(initiative, OptionsConstants.INIT_SIMULTANEOUS_TARGETING, false);
        addOption(initiative, OptionsConstants.INIT_SIMULTANEOUS_FIRING, false);
        addOption(initiative, OptionsConstants.INIT_SIMULTANEOUS_PHYSICAL, false);
        addOption(initiative, OptionsConstants.INIT_FRONT_LOAD_INITIATIVE, false);
        addOption(initiative, OptionsConstants.INIT_INITIATIVE_STREAK_COMPENSATION, false);

        IBasicOptionGroup rpg = addGroup("rpg");
        addOption(rpg, OptionsConstants.RPG_PILOT_ADVANTAGES, false);
        addOption(rpg, OptionsConstants.EDGE, false);
        addOption(rpg, OptionsConstants.RPG_MANEI_DOMINI, false);
        addOption(rpg, OptionsConstants.RPG_INDIVIDUAL_INITIATIVE, false);
        addOption(rpg, OptionsConstants.RPG_COMMAND_INIT, false);
        addOption(rpg, OptionsConstants.RPG_RPG_GUNNERY, false);
        addOption(rpg, OptionsConstants.RPG_ARTILLERY_SKILL, false);
        addOption(rpg, OptionsConstants.RPG_TOUGHNESS, false);
        addOption(rpg, OptionsConstants.RPG_CONDITIONAL_EJECTION, false);
        addOption(rpg, OptionsConstants.RPG_MANUAL_SHUTDOWN, false);
        addOption(rpg, OptionsConstants.RPG_BEGIN_SHUTDOWN, false);
    }

    public Vector<IOption> loadOptions() {
        return loadOptions(new File(GAME_OPTIONS_FILE_NAME), true);
    }

    public synchronized Vector<IOption> loadOptions(File file, boolean print) {
        Vector<IOption> changedOptions = new Vector<>(1, 1);

        if (!file.exists()) {
            return changedOptions;
        }

        try {
            JAXBContext jc = JAXBContext.newInstance(GameOptionsXML.class, Option.class, BasicOption.class);

            Unmarshaller um = jc.createUnmarshaller();
            InputStream is = new FileInputStream(file);
            GameOptionsXML opts = (GameOptionsXML) um.unmarshal(MMXMLUtility.createSafeXmlSource(is));

            StringBuilder logMessages = new StringBuilder("\n");
            for (IBasicOption bo : opts.getOptions()) {
                changedOptions.add(parseOptionNode(bo, print, logMessages));
            }
            LogManager.getLogger().info(logMessages.toString());
        } catch (Exception e) {
            LogManager.getLogger().error("Error loading XML for game options: " + e.getMessage(), e);
        }

        return changedOptions;
    }

    private IOption parseOptionNode(final IBasicOption node, final boolean print, final StringBuilder logMessages) {
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
                                tempOption.setValue(Boolean.valueOf(value.toString()));
                                break;
                            case IOption.INTEGER:
                                tempOption.setValue(Integer.valueOf(value.toString()));
                                break;
                            case IOption.FLOAT:
                                tempOption.setValue(Float.valueOf(value.toString()));
                                break;
                        }

                        if (print) {
                            logMessages.append(String.format("\tSet option '%s' to '%s'\n", name, value));
                        }

                        option = tempOption;
                    } catch (Exception ex) {
                        LogManager.getLogger().error(String.format(
                                "Error trying to load option '%s' with a value of '%s'!", name, value));
                    }
                }
            } else {
                LogManager.getLogger().warn("Invalid option '" + name + "' when trying to load options file!");
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
            marshaller.setProperty("org.glassfish.jaxb.xmlHeaders", "<?xml version=\"1.0\"?>");

            JAXBElement<GameOptionsXML> element = new JAXBElement<>(new QName("options"), GameOptionsXML.class, new GameOptionsXML(options));

            marshaller.marshal(element, new File(file));
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed writing Game Options XML", ex);
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
            super("GameOptionsInfo");
        }

        public static AbstractOptionsInfo getInstance() {
            return instance;
        }
    }

    /**
     * A helper class for the XML binding.
     */
    @XmlRootElement(name = "options")
    @XmlAccessorType(value = XmlAccessType.FIELD)
    private static class GameOptionsXML {
        @XmlElement(name = "gameoption", type = BasicOption.class)
        private Vector<IBasicOption> options;

        GameOptionsXML(final Vector<IBasicOption> options) {
            this.options = options;
        }

        /**
         * Required for JAXB.
         */
        @SuppressWarnings(value = "unused")
        private GameOptionsXML() {

        }

        public Vector<IBasicOption> getOptions() {
            return options;
        }
    }

    //region MekHQ I/O
    /**
     * This is used by MekHQ to write the game options to the standard file
     *
     * @param pw the PrintWriter to write to
     * @param indent the indent to write at
     */
    public void writeToXML(final PrintWriter pw, int indent) {
        MMXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "gameOptions");
        for (final Enumeration<IOptionGroup> groups = getGroups(); groups.hasMoreElements(); ) {
            final IOptionGroup group = groups.nextElement();
            for (final Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
                final IOption option = options.nextElement();
                MMXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "gameOption");
                MMXMLUtility.writeSimpleXMLTag(pw, indent, "name", option.getName());
                MMXMLUtility.writeSimpleXMLTag(pw, indent, "value", option.getValue().toString());
                MMXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "gameOption");
            }
        }
        MMXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "gameOptions");
    }

    /**
     * This is used to fill a GameOptions object from an XML node list written using writeToXML.
     * @param nl the node list to parse
     */
    public void fillFromXML(final NodeList nl) {
        for (int x = 0; x < nl.getLength(); x++) {
            try {
                final Node wn = nl.item(x);
                if ((wn.getNodeType() != Node.ELEMENT_NODE) || !wn.hasChildNodes())  {
                    continue;
                }

                final NodeList nl2 = wn.getChildNodes();
                IOption option = null;
                for (int y = 0; y < nl2.getLength(); y++) {
                    final Node wn2 = nl2.item(y);
                    switch (wn2.getNodeName()) {
                        case "name":
                            option = getOption(wn2.getTextContent().trim());
                            break;
                        case "value":
                            final String value = wn2.getTextContent().trim();
                            if ((option != null) && !option.getValue().toString().equals(value)) {
                                switch (option.getType()) {
                                    case IOption.STRING:
                                    case IOption.CHOICE:
                                        option.setValue(value);
                                        break;
                                    case IOption.BOOLEAN:
                                        option.setValue(Boolean.valueOf(value));
                                        break;
                                    case IOption.INTEGER:
                                        option.setValue(Integer.valueOf(value));
                                        break;
                                    case IOption.FLOAT:
                                        option.setValue(Float.valueOf(value));
                                        break;
                                    default:
                                        break;
                                }
                                option = null;
                            }
                            break;
                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                LogManager.getLogger().error("Failed to parse Game Option Node", e);
            }
        }
    }
    //endregion MekHQ I/O
}
