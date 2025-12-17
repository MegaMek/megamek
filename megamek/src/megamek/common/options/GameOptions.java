/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.options;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serial;
import java.util.Enumeration;
import java.util.Vector;
import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import megamek.common.TechConstants;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Contains the options determining play in the current game.
 *
 * @author Ben
 */
public class GameOptions extends BasicGameOptions {
    private static final MMLogger logger = MMLogger.create(GameOptions.class);

    @Serial
    private static final long serialVersionUID = 4916321960852747706L;
    private static final String GAME_OPTIONS_FILE_NAME = "mmconf/gameoptions.xml";

    public GameOptions() {
        super();
    }

    @Override
    public synchronized void initialize() {
        super.initialize();

        IBasicOptionGroup base = addGroup("basic");
        // Change this to false for normal release
        addOption(base, OptionsConstants.PLAYTEST_1, false);
        addOption(base, OptionsConstants.PLAYTEST_2, false);
        addOption(base, OptionsConstants.PLAYTEST_3, false);
        addOption(base, OptionsConstants.BASE_PUSH_OFF_BOARD, true);
        addOption(base, OptionsConstants.BASE_DUMPING_FROM_ROUND, 1);
        addOption(base, OptionsConstants.BASE_LOBBY_AMMO_DUMP, false);
        addOption(base, OptionsConstants.BASE_SHOW_BAY_DETAIL, false);
        addOption(base, OptionsConstants.BASE_INDIRECT_FIRE, true);
        addOption(base, OptionsConstants.BASE_FLAMER_HEAT, false);
        addOption(base, OptionsConstants.BASE_INFANTRY_DAMAGE_HEAT, false);
        addOption(base, OptionsConstants.BASE_AUTO_AMS, true);
        addOption(base, OptionsConstants.BASE_RANDOM_BASEMENTS, true);
        addOption(base, OptionsConstants.BASE_BREEZE, false);

        IBasicOptionGroup victory = addGroup("victory");
        addOption(victory, OptionsConstants.VICTORY_SKIP_FORCED_VICTORY, false);
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
        addOption(allowed, OptionsConstants.ALLOWED_TECH_LEVEL, IOption.CHOICE,
              TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_STANDARD]);
        addOption(allowed, OptionsConstants.ALLOWED_ERA_BASED, false);
        addOption(allowed, OptionsConstants.ALLOWED_ALLOW_ILLEGAL_UNITS, false);
        addOption(allowed, OptionsConstants.ALLOWED_SHOW_EXTINCT, true);
        addOption(allowed, OptionsConstants.ALLOWED_ALL_AMMO_MIXED_TECH, false);
        addOption(allowed, OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL, false);
        addOption(allowed, OptionsConstants.ALLOWED_ALLOW_NUKES, false);
        addOption(allowed, OptionsConstants.ALLOWED_REALLY_ALLOW_NUKES, false);

        IBasicOptionGroup advancedRules = addGroup("advancedRules");
        addOption(advancedRules, OptionsConstants.ADVANCED_MINEFIELDS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_HIDDEN_UNITS, true);
        addOption(advancedRules, OptionsConstants.ADVANCED_BLACK_ICE, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_LIGHTNING_STORM_TARGETS_UNITS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_DOUBLE_BLIND, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TAC_OPS_SENSORS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_SUPPRESS_ALL_DB_MESSAGES, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_SUPPRESS_DB_BV, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TEAM_VISION, true);
        addOption(advancedRules, OptionsConstants.ADVANCED_TAC_OPS_BAP, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TAC_OPS_ECCM, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TAC_OPS_GHOST_TARGET, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_GHOST_TARGET_MAX, 5);
        addOption(advancedRules, OptionsConstants.ADVANCED_TAC_OPS_DIG_IN, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TAC_OPS_BA_WEIGHT, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TAC_OPS_TAKE_COVER, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TAC_OPS_ANGEL_ECM, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TAC_OPS_BATTLE_WRECK, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TAC_OPS_SKIN_OF_THE_TEETH_EJECTION, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TAC_OPS_MOBILE_HQS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TAC_OPS_FATIGUE, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TAC_OPS_FUMBLES, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TAC_OPS_SELF_DESTRUCT, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_TAC_OPS_TANK_CREWS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_STRATOPS_QUIRKS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_STRATOPS_PARTIAL_REPAIRS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_ASSAULT_DROP, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_PARATROOPERS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_INCLUSIVE_SENSOR_RANGE, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_SENSORS_DETECT_ALL, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_MAG_SCAN_NO_HILLS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_WOODS_BURN_DOWN, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_WOODS_BURN_DOWN_AMOUNT, 5);
        addOption(advancedRules, OptionsConstants.ADVANCED_NO_IGNITE_CLEAR, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_ALL_HAVE_EI_COCKPIT, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_EXTREME_TEMPERATURE_SURVIVAL, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_ARMED_MEKWARRIORS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_PILOTS_VISUAL_RANGE_ONE, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_PILOTS_CANNOT_SPOT, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_METAL_CONTENT, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_BA_GRAB_BARS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_MAX_TECH_MOVEMENT_MODS, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_ALTERNATE_MASC, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED, false);
        addOption(advancedRules, OptionsConstants.ADVANCED_SINGLE_BLIND_BOTS, false);

        IBasicOptionGroup advancedCombat = addGroup("advancedCombat");
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_AMS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_MANUAL_AMS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_FLOATING_CRITS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CRIT_ROLL, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ENGINE_EXPLOSIONS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CALLED_SHOTS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_PRONE_FIRE, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_START_FIRE, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DEAD_ZONES, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ALTERNATIVE_DAMAGE, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CLUSTER_HIT_PEN, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_PPC_INHIBITORS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CHARGE_DAMAGE, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_GLANCING_BLOWS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DIRECT_BLOW, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BURST, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HEAT, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_PARTIAL_COVER, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BA_CRITICAL_SLOTS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HOT_LOAD, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RAPID_AC, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_KIND_RAPID_AC, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_GRAPPLING, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_JUMP_JET_ATTACK, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_TRIP_ATTACK, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ENERGY_WEAPONS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_GOTHIC_DAZZLE_MODE, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_GAUSS_WEAPONS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RETRACTABLE_BLADES, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_AMMUNITION, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_WOODS_COVER, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VEHICLE_EFFECTIVE, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VEHICLE_ARCS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VTOL_ATTACKS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ADVANCED_MEK_HIT_LOCATIONS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_COOLANT_FAILURE, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BA_VS_BA, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_NO_TAC, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_VTOL_STRAFING, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_VEHICLES_SAFE_FROM_INFERNOS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_PROTOMEKS_SAFE_FROM_INFERNOS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_INDIRECT_ALWAYS_POSSIBLE, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_INCREASED_AC_DMG, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_UNJAM_UAC, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_TAC_OPS_UAC_TWO_ROLLS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_CLUBS_PUNCH, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_ON_MAP_PREDESIGNATE, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_NUM_HEXES_PREDESIGNATE, 5);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_MAP_AREA_PREDESIGNATE, 1088);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_MAX_EXTERNAL_HEAT, 15);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_CASE_PILOT_DAMAGE, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_NO_FORCED_PRIMARY_TARGETS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_FULL_ROTOR_HITS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_FOREST_FIRES_NO_SMOKE, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_HOT_LOAD_IN_GAME, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_MULTI_USE_AMS, false);
        addOption(advancedCombat, OptionsConstants.ADVANCED_COMBAT_PICKING_UP_AND_THROWING_UNITS, false);

        IBasicOptionGroup advancedGroundMovement = addGroup("advancedGroundMovement");
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_SPRINT, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_STANDING_STILL, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_EVADE, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_SKILLED_EVASION, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEAPING, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_PHYSICAL_PSR, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_PHYSICAL_ATTACK_PSR, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_TAKING_DAMAGE, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEG_DAMAGE, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_WALK_BACKWARDS, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_FAST_INFANTRY_MOVE, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_TAC_OPS_INF_PAVE_BONUS, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_LANCE_MOVEMENT, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_LANCE_MOVEMENT_NUMBER, 4);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_ACCELERATION, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_REVERSE_GEAR, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_TURN_MODE, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_ADVANCED_MANEUVERS, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_HULL_DOWN, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_FALLING_EXPANDED, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_ATTEMPTING_STAND, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_CAREFUL_STAND, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_ZIPLINES, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_MEK_LANCE_MOVEMENT, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_MEK_LANCE_MOVEMENT_NUMBER, 4);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_UNOFF_NO_IMMOBILE_VEHICLES, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLES_CAN_EJECT, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_EJECTED_PILOTS_FLEE, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_AUTO_ABANDON_UNIT, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_NO_HOVER_CHARGE, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_NO_PRE_MOVE_VIBRA, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_FALLS_END_MOVEMENT, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_PSR_JUMP_HEAVY_WOODS, false);
        addOption(advancedGroundMovement, OptionsConstants.ADVANCED_GROUND_MOVEMENT_NO_NIGHT_MOVE_PEN, false);

        IBasicOptionGroup advAeroRules = addGroup("advancedAeroRules");
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_AERO_GROUND_MOVE, true);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_CAPITAL_FIGHTER, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_FUEL_CONSUMPTION, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_CONV_FUSION_BONUS, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_HARJEL, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_GRAV_EFFECTS, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_ADVANCED_MOVEMENT, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_HEAT_BY_BAY, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_ATMOSPHERIC_CONTROL, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_AMMO_EXPLOSIONS, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_AAA_LASER, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADV_POINT_DEFENSE, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_BRACKET_FIRE, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ECM, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_SENSOR_SHADOW, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_OVER_PENETRATE, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_SPACE_BOMB, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_BEARINGS_ONLY_LAUNCH, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_BEARINGS_ONLY_VELOCITY, 50);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_WAYPOINT_LAUNCH, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADVANCED_SENSORS, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_VARIABLE_DAMAGE_THRESH, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_AT2_NUKES, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_RETURN_FLYOVER, true);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_CLIMB_OUT, true);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_AA_FIRE, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_AA_MOVE_MOD, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_ALLOW_LARGE_SQUADRONS, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_SINGLE_NO_CAP, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_AERO_ARTILLERY_MUNITIONS, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_CRASHED_DROPSHIPS_SURVIVE, false);
        addOption(advAeroRules, OptionsConstants.ADVANCED_AERO_RULES_EXPANDED_KF_DRIVE_DAMAGE, false);
        addOption(advAeroRules, OptionsConstants.UNOFFICIAL_ADV_ATMOSPHERIC_CONTROL, false);

        IBasicOptionGroup initiative = addGroup("initiative");
        addOption(initiative, OptionsConstants.INIT_INF_MOVE_EVEN, false);
        addOption(initiative, OptionsConstants.INIT_INF_DEPLOY_EVEN, false);
        addOption(initiative, OptionsConstants.INIT_INF_MOVE_LATER, false);
        addOption(initiative, OptionsConstants.INIT_INF_MOVE_MULTI, false);
        addOption(initiative, OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN, false);
        addOption(initiative, OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN, false);
        addOption(initiative, OptionsConstants.INIT_PROTOMEKS_MOVE_LATER, false);
        addOption(initiative, OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI, false);
        addOption(initiative, OptionsConstants.INIT_INF_PROTO_MOVE_MULTI, 3);
        addOption(initiative, OptionsConstants.INIT_SIMULTANEOUS_DEPLOYMENT, false);
        //addOption(initiative, OptionsConstants.INIT_SIMULTANEOUS_MOVEMENT, false);
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
            logger.info(logMessages.toString());
        } catch (Exception e) {
            logger.error("Error loading XML for game options: {}", e.getMessage(), e);
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
                        logger.warn("Error trying to load option {} with a value of {}!", name, value);
                    }
                }
            } else {
                logger.warn("Invalid option '{}' when trying to load options file!", name);
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
     * @param file    string with the name of the file
     */
    public static void saveOptions(Vector<IBasicOption> options, String file) {
        try {
            JAXBContext jc = JAXBContext.newInstance(GameOptionsXML.class, Option.class, BasicOption.class);

            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            // The default header has the encoding and standalone properties
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.setProperty("org.glassfish.jaxb.xmlHeaders", "<?xml version=\"1.0\"?>");

            JAXBElement<GameOptionsXML> element = new JAXBElement<>(new QName("options"), GameOptionsXML.class,
                  new GameOptionsXML(options));

            marshaller.marshal(element, new File(file));
        } catch (Exception ex) {
            logger.error("Failed writing Game Options XML", ex);
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
        private static final AbstractOptionsInfo instance = new GameOptionsInfo();

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

    // region MekHQ I/O

    /**
     * This is used by MekHQ to write the game options to the standard file
     *
     * @param pw     the PrintWriter to write to
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
     *
     * @param nl the node list to parse
     */
    public void fillFromXML(final NodeList nl) {
        for (int x = 0; x < nl.getLength(); x++) {
            try {
                final Node wn = nl.item(x);
                if ((wn.getNodeType() != Node.ELEMENT_NODE) || !wn.hasChildNodes()) {
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
                logger.error("Failed to parse Game Option Node", e);
            }
        }
    }
    // endregion MekHQ I/O
}
