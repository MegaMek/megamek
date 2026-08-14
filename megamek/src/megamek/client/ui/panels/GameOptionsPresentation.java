/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels;

import static megamek.client.ui.Messages.CLIENT_BUNDLE;
import static megamek.common.internationalization.I18n.getTextAt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;

/** Explicit presentation-only placement for every registered game option. */
final class GameOptionsPresentation {
    private static final MMLogger LOGGER = MMLogger.create(GameOptionsPresentation.class);
    private static final String BASIC = "basic";
    private static final String GAME_MASTER = "gameMaster";
    private static final String VICTORY = "victory";
    private static final String ALLOWED_UNITS = "allowedUnits";
    private static final String ADVANCED_RULES = "advancedRules";
    private static final String ADVANCED_COMBAT = "advancedCombat";
    private static final String GROUND_MOVEMENT = "advancedGroundMovement";
    private static final String AEROSPACE = "advancedAeroRules";
    private static final String INITIATIVE = "initiative";
    private static final String RPG = "rpg";
    private static final PageDefinition LANDING = directPage("landing", BASIC, false, 0);
    private static final PageDefinition GENERAL_MATCH_SETUP = nestedPage(
          "general.matchSetup", "general", BASIC, false, 10);
    private static final PageDefinition GENERAL_MATCH_FLOW = nestedPage(
          "general.matchFlow", "general", BASIC, false, 20);
    private static final PageDefinition GENERAL_VICTORY_AND_GAME_MASTER = nestedPageWithSingleSourceFallback(
          "general.victoryAndGameMaster", "general", VICTORY, GAME_MASTER, false, 30);
    private static final PageDefinition GENERAL_UNITS_AND_TECHNOLOGY = nestedPage(
          "general.unitsAndTechnology", "general", ALLOWED_UNITS, false, 40);
    private static final PageDefinition RULES_GENERAL = nestedPage("rules.general", "rules", BASIC, false, 60);
    private static final PageDefinition RULES_SENSORS = nestedPage(
          "rules.sensors", "rules", ADVANCED_RULES, true, 70);
    private static final PageDefinition RULES_ENVIRONMENT = nestedPage(
          "rules.environment", "rules", ADVANCED_RULES, true, 80);
    private static final PageDefinition RULES_SPECIAL_SYSTEMS = nestedPage(
          "rules.specialSystems", "rules", ADVANCED_RULES, true, 90);
    private static final PageDefinition COMBAT_TARGETING = nestedPage(
          "combat.targeting", "combat", ADVANCED_COMBAT, true, 100);
    private static final PageDefinition COMBAT_WEAPONS = nestedPage(
          "combat.weapons", "combat", ADVANCED_COMBAT, true, 110);
    private static final PageDefinition COMBAT_DAMAGE = nestedPage(
          "combat.damage", "combat", ADVANCED_COMBAT, true, 120);
    private static final PageDefinition COMBAT_PHYSICAL = nestedPage(
          "combat.physical", "combat", ADVANCED_COMBAT, true, 130);
    private static final PageDefinition COMBAT_UNITS = nestedPage(
          "combat.units", "combat", ADVANCED_COMBAT, true, 140);
    private static final PageDefinition MOVEMENT_MEKS = nestedPage(
          "movement.meks", "movement", GROUND_MOVEMENT, true, 150);
    private static final PageDefinition MOVEMENT_VEHICLES = nestedPage(
          "movement.vehicles", "movement", GROUND_MOVEMENT, true, 160);
    private static final PageDefinition MOVEMENT_INFANTRY = nestedPage(
          "movement.infantry", "movement", GROUND_MOVEMENT, true, 170);
    private static final PageDefinition AEROSPACE_FLIGHT = nestedPage(
          "aerospace.flight", "aerospace", AEROSPACE, true, 180);
    private static final PageDefinition AEROSPACE_TARGETING = nestedPage(
          "aerospace.targeting", "aerospace", AEROSPACE, true, 190);
    private static final PageDefinition AEROSPACE_VESSELS = nestedPage(
          "aerospace.vessels", "aerospace", AEROSPACE, true, 200);
    private static final PageDefinition INITIATIVE_AND_PILOTS_PHASES = nestedPage(
          "initiativeAndPilots.phases", "initiativeAndPilots", INITIATIVE, false, 210);
    private static final PageDefinition INITIATIVE_AND_PILOTS_ABILITIES = nestedPage(
          "initiativeAndPilots.abilities", "initiativeAndPilots", RPG, false, 220);

    private static final Map<String, Location> LOCATIONS = new LinkedHashMap<>();
    private static final Map<PageDefinition, Map<String, Integer>> SECTION_ORDERS = new LinkedHashMap<>();
    private static final Map<PageDefinition, Map<String, Integer>> NEXT_OPTION_ORDERS = new LinkedHashMap<>();

    static {
        registerBasicOptions();
        registerGameManagementOptions();
        registerGroundMovementOptions();
        registerAdvancedRulesOptions();
        registerAdvancedCombatOptions();
        registerAerospaceOptions();
        registerInitiativeAndRpgOptions();
    }

    private GameOptionsPresentation() {
    }

    static Location location(String sourceGroupId, String optionName) {
        Location location = LOCATIONS.get(optionName);
        if (location == null) {
            throw new IllegalArgumentException("No Game Options presentation location for " + optionName);
        }
        if (!location.sourceGroupId().equals(sourceGroupId)) {
            throw new IllegalArgumentException(optionName + " is registered in " + sourceGroupId
                  + " but its presentation location expects " + location.sourceGroupId());
        }
        return location;
    }

    static Location locationOrFallback(String sourceGroupId, String optionName) {
        Location location = LOCATIONS.get(optionName);
        if (location != null && location.sourceGroupId().equals(sourceGroupId)) {
            return location;
        }
        if (location == null) {
            LOGGER.warn("No Game Options presentation location for {}; using Uncategorized", optionName);
        } else {
            LOGGER.warn("Game option {} is registered in {} but its presentation location expects {}; "
                        + "using Uncategorized",
                  optionName, sourceGroupId, location.sourceGroupId());
        }
        return new Location(sourceGroupId, GENERAL_UNITS_AND_TECHNOLOGY,
              "allowedUnits.uncategorized", Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    static Set<String> mappedOptionNames() {
        return Set.copyOf(LOCATIONS.keySet());
    }

    private static void registerBasicOptions() {
        register(BASIC, LANDING, "landing.rulesSystem", OptionsConstants.RULES_SYSTEM);
        register(BASIC, GENERAL_MATCH_SETUP, "general.matchSetup.teams",
              OptionsConstants.BASE_TEAM_INITIATIVE,
              OptionsConstants.BASE_SET_DEFAULT_TEAM_1,
              OptionsConstants.BASE_RESTRICT_GAME_COMMANDS,
              OptionsConstants.BASE_GM_CONTROLS_DONE_REPORT_PHASE);
        register(BASIC, GENERAL_MATCH_SETUP, "general.matchSetup.deployment",
              OptionsConstants.BASE_EXCLUSIVE_DB_DEPLOYMENT,
              OptionsConstants.BASE_BLIND_DROP,
              OptionsConstants.BASE_REAL_BLIND_DROP,
              OptionsConstants.BASE_SET_ARTY_PLAYER_HOME_EDGE,
              OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER_0);
        register(BASIC, GENERAL_MATCH_SETUP, "general.matchSetup.interface",
              OptionsConstants.BASE_LOBBY_AMMO_DUMP,
              OptionsConstants.BASE_DUMPING_FROM_ROUND,
              OptionsConstants.BASE_SHOW_BAY_DETAIL,
              OptionsConstants.BASE_SUPPRESS_UNIT_TOOLTIP_IN_REPORT_LOG,
              OptionsConstants.BASE_HIDE_UNOFFICIAL,
              OptionsConstants.BASE_HIDE_LEGACY);

        register(BASIC, GENERAL_MATCH_FLOW, "general.matchFlow.phases",
              OptionsConstants.BASE_SKIP_INELIGIBLE_MOVEMENT,
              OptionsConstants.BASE_SKIP_INELIGIBLE_FIRING,
              OptionsConstants.BASE_SKIP_INELIGIBLE_PHYSICAL);
        register(BASIC, GENERAL_MATCH_FLOW, "general.matchFlow.timers",
              OptionsConstants.BASE_TURN_TIMER_TARGETING,
              OptionsConstants.BASE_TURN_TIMER_MOVEMENT,
              OptionsConstants.BASE_TURN_TIMER_FIRING,
              OptionsConstants.BASE_TURN_TIMER_PHYSICAL,
              OptionsConstants.BASE_TURN_TIMER_ALLOW_EXTENSION);
        register(BASIC, GENERAL_MATCH_FLOW, "general.matchFlow.saves",
              OptionsConstants.BASE_AUTOSAVE_MSG,
              OptionsConstants.BASE_PARANOID_AUTOSAVE,
              OptionsConstants.BASE_MAX_NUMBER_ROUND_SAVES,
              OptionsConstants.BASE_DISABLE_LOCAL_SAVE,
              OptionsConstants.BASE_RNG_TYPE,
              OptionsConstants.BASE_RNG_LOG);
        register(BASIC, RULES_GENERAL, "rules.general.battlefield",
              OptionsConstants.SEARCHLIGHTS_ON,
              OptionsConstants.BASE_PUSH_OFF_BOARD,
              OptionsConstants.BASE_BRIDGE_CF,
              OptionsConstants.BASE_RANDOM_BASEMENTS,
              OptionsConstants.BASE_BREEZE);
        register(BASIC, RULES_GENERAL, "rules.general.combat",
              OptionsConstants.BASE_FRIENDLY_FIRE,
              OptionsConstants.BASE_INDIRECT_FIRE,
              OptionsConstants.BASE_FLAMER_HEAT,
              OptionsConstants.BASE_INFANTRY_DAMAGE_HEAT,
              OptionsConstants.BASE_AUTO_AMS);
    }

    private static void registerGameManagementOptions() {
        register(GAME_MASTER, GENERAL_VICTORY_AND_GAME_MASTER, "gameMaster.control",
              OptionsConstants.GAME_MASTER_ALLOW,
              OptionsConstants.GAME_MASTER_VOTE_THRESHOLD);

        register(VICTORY, GENERAL_VICTORY_AND_GAME_MASTER, "victory.conditions",
              OptionsConstants.VICTORY_CHECK_VICTORY,
              OptionsConstants.VICTORY_SKIP_FORCED_VICTORY,
              OptionsConstants.VICTORY_ACHIEVE_CONDITIONS);
        register(VICTORY, GENERAL_VICTORY_AND_GAME_MASTER, "victory.battleValue",
              OptionsConstants.VICTORY_USE_BV_DESTROYED,
              OptionsConstants.VICTORY_BV_DESTROYED_PERCENT,
              OptionsConstants.VICTORY_USE_BV_RATIO,
              OptionsConstants.VICTORY_BV_RATIO_PERCENT);
        register(VICTORY, GENERAL_VICTORY_AND_GAME_MASTER, "victory.alternate",
              OptionsConstants.VICTORY_USE_GAME_TURN_LIMIT,
              OptionsConstants.VICTORY_GAME_TURN_LIMIT,
              OptionsConstants.VICTORY_USE_KILL_COUNT,
              OptionsConstants.VICTORY_GAME_KILL_COUNT,
              OptionsConstants.VICTORY_COMMANDER_KILLED);

        register(ALLOWED_UNITS, GENERAL_UNITS_AND_TECHNOLOGY, "allowedUnits.availability",
              OptionsConstants.ALLOWED_CANON_ONLY,
              OptionsConstants.ALLOWED_YEAR,
              OptionsConstants.ALLOWED_TECH_LEVEL,
              OptionsConstants.ALLOWED_ERA_BASED);
        register(ALLOWED_UNITS, GENERAL_UNITS_AND_TECHNOLOGY, "allowedUnits.restrictions",
              OptionsConstants.ALLOWED_ALLOW_ILLEGAL_UNITS,
              OptionsConstants.ALLOWED_SHOW_EXTINCT,
              OptionsConstants.ALLOWED_ALL_AMMO_MIXED_TECH,
              OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL,
              OptionsConstants.ALLOWED_ALLOW_NUKES,
              OptionsConstants.ALLOWED_REALLY_ALLOW_NUKES);
    }

    private static void registerAdvancedRulesOptions() {
        register(ADVANCED_RULES, RULES_SENSORS, "rules.sensors.visibility",
              OptionsConstants.ADVANCED_HIDDEN_UNITS,
              OptionsConstants.ADVANCED_DOUBLE_BLIND,
              OptionsConstants.ADVANCED_SUPPRESS_ALL_DB_MESSAGES,
              OptionsConstants.ADVANCED_SUPPRESS_DB_BV,
              OptionsConstants.ADVANCED_TEAM_VISION,
              OptionsConstants.ADVANCED_SINGLE_BLIND_BOTS,
              OptionsConstants.ADVANCED_PILOTS_VISUAL_RANGE_ONE,
              OptionsConstants.ADVANCED_PILOTS_CANNOT_SPOT);
        register(ADVANCED_RULES, RULES_SENSORS, "rules.sensors.detection",
              OptionsConstants.ADVANCED_TAC_OPS_SENSORS,
              OptionsConstants.ADVANCED_TAC_OPS_BAP,
              OptionsConstants.ADVANCED_INCLUSIVE_SENSOR_RANGE,
              OptionsConstants.ADVANCED_SENSORS_DETECT_ALL,
              OptionsConstants.ADVANCED_MAG_SCAN_NO_HILLS,
              OptionsConstants.ADVANCED_METAL_CONTENT);
        register(ADVANCED_RULES, RULES_SENSORS, "rules.sensors.electronicWarfare",
              OptionsConstants.ADVANCED_TAC_OPS_ECCM,
              OptionsConstants.ADVANCED_TAC_OPS_GHOST_TARGET,
              OptionsConstants.ADVANCED_GHOST_TARGET_MODE,
              OptionsConstants.ADVANCED_GHOST_TARGET_MAX,
              OptionsConstants.ADVANCED_TAC_OPS_ANGEL_ECM);

        register(ADVANCED_RULES, RULES_ENVIRONMENT, "rules.environment.hazards",
              OptionsConstants.ADVANCED_MINEFIELDS,
              OptionsConstants.ADVANCED_BLACK_ICE,
              OptionsConstants.ADVANCED_LIGHTNING_STORM_TARGETS_UNITS,
              OptionsConstants.ADVANCED_EXTREME_TEMPERATURE_SURVIVAL);
        register(ADVANCED_RULES, RULES_ENVIRONMENT, "rules.environment.fire",
              OptionsConstants.ADVANCED_WOODS_BURN_DOWN,
              OptionsConstants.ADVANCED_WOODS_BURN_DOWN_AMOUNT,
              OptionsConstants.ADVANCED_NO_IGNITE_CLEAR);
        register(ADVANCED_RULES, RULES_ENVIRONMENT, "rules.environment.engineering",
              OptionsConstants.ADVANCED_TAC_OPS_BATTLE_WRECK,
              OptionsConstants.ADVANCED_BRIDGE_BUILDING_ENGINEERS,
              OptionsConstants.UNOFFICIAL_BRIDGE_REPAIR_ENGINEERS);

        register(ADVANCED_RULES, RULES_SPECIAL_SYSTEMS, "rules.specialSystems.crews",
              OptionsConstants.ADVANCED_TAC_OPS_FATIGUE,
              OptionsConstants.ADVANCED_TAC_OPS_FUMBLES,
              OptionsConstants.ADVANCED_TAC_OPS_TANK_CREWS,
              OptionsConstants.ADVANCED_ARMED_MEKWARRIORS,
              OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE);
        register(ADVANCED_RULES, RULES_SPECIAL_SYSTEMS, "rules.specialSystems.ejection",
              OptionsConstants.ADVANCED_TAC_OPS_SKIN_OF_THE_TEETH_EJECTION,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLES_CAN_EJECT,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_EJECTED_PILOTS_FLEE,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_AUTO_ABANDON_UNIT,
              OptionsConstants.RPG_CONDITIONAL_EJECTION);
        register(ADVANCED_RULES, RULES_SPECIAL_SYSTEMS, "rules.specialSystems.systems",
              OptionsConstants.ADVANCED_TAC_OPS_MOBILE_HQS,
              OptionsConstants.ADVANCED_TAC_OPS_SELF_DESTRUCT,
              OptionsConstants.ADVANCED_STRATOPS_QUIRKS,
              OptionsConstants.ADVANCED_STRATOPS_PARTIAL_REPAIRS,
              OptionsConstants.ADVANCED_MAX_TECH_MOVEMENT_MODS,
              OptionsConstants.ADVANCED_ALTERNATE_MASC,
              OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED);
        register(ADVANCED_RULES, MOVEMENT_INFANTRY, "movement.infantry.positions",
              OptionsConstants.ADVANCED_TAC_OPS_DIG_IN,
              OptionsConstants.ADVANCED_TAC_OPS_TAKE_COVER);
        register(ADVANCED_RULES, MOVEMENT_INFANTRY, "movement.infantry.deployment",
              OptionsConstants.ADVANCED_ASSAULT_DROP,
              OptionsConstants.ADVANCED_PARATROOPERS);
        register(ADVANCED_RULES, MOVEMENT_INFANTRY, "movement.infantry.battleArmor",
              OptionsConstants.ADVANCED_TAC_OPS_BA_WEIGHT,
              OptionsConstants.ADVANCED_BA_GRAB_BARS);
    }

    private static void registerAdvancedCombatOptions() {
        register(ADVANCED_COMBAT, COMBAT_TARGETING, "combat.targeting.range",
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DEAD_ZONES,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_PARTIAL_COVER,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_WOODS_COVER);
        register(ADVANCED_COMBAT, COMBAT_TARGETING, "combat.targeting.resolution",
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CALLED_SHOTS,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_GLANCING_BLOWS,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DIRECT_BLOW,
              OptionsConstants.ADVANCED_COMBAT_GOTHIC_DAZZLE_MODE,
              OptionsConstants.ADVANCED_COMBAT_NO_FORCED_PRIMARY_TARGETS);
        register(ADVANCED_COMBAT, COMBAT_TARGETING, "combat.targeting.artillery",
              OptionsConstants.ADVANCED_COMBAT_INDIRECT_ALWAYS_POSSIBLE,
              OptionsConstants.ADVANCED_COMBAT_ON_MAP_PREDESIGNATE,
              OptionsConstants.ADVANCED_COMBAT_NUM_HEXES_PREDESIGNATE,
              OptionsConstants.ADVANCED_COMBAT_MAP_AREA_PREDESIGNATE,
              OptionsConstants.ADVANCED_COMBAT_ADVANCED_SCATTER);

        register(ADVANCED_COMBAT, COMBAT_WEAPONS, "combat.weapons.missileDefense",
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_AMS,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_MANUAL_AMS,
              OptionsConstants.ADVANCED_COMBAT_MULTI_USE_AMS);
        register(ADVANCED_COMBAT, COMBAT_WEAPONS, "combat.weapons.autocannons",
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RAPID_AC,
              OptionsConstants.ADVANCED_COMBAT_KIND_RAPID_AC,
              OptionsConstants.ADVANCED_COMBAT_INCREASED_AC_DMG,
              OptionsConstants.ADVANCED_COMBAT_UNJAM_UAC,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_UAC_TWO_ROLLS);
        register(ADVANCED_COMBAT, COMBAT_WEAPONS, "combat.weapons.energy",
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_PPC_INHIBITORS,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ENERGY_WEAPONS,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_GAUSS_WEAPONS);
        register(ADVANCED_COMBAT, COMBAT_WEAPONS, "combat.weapons.ammunition",
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BURST,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HOT_LOAD,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_AMMUNITION,
              OptionsConstants.ADVANCED_COMBAT_HOT_LOAD_IN_GAME,
              OptionsConstants.ADVANCED_COMBAT_DISPOSABLE_INFANTRY_WEAPONS);

        register(ADVANCED_COMBAT, COMBAT_DAMAGE, "combat.damage.criticalHits",
              OptionsConstants.ADVANCED_COMBAT_FLOATING_CRITS,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CRIT_ROLL,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ENGINE_EXPLOSIONS,
              OptionsConstants.ADVANCED_COMBAT_NO_TAC);
        register(ADVANCED_COMBAT, COMBAT_DAMAGE, "combat.damage.damage",
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ALTERNATIVE_DAMAGE,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CLUSTER_HIT_PEN,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ADVANCED_MEK_HIT_LOCATIONS,
              OptionsConstants.ADVANCED_COMBAT_CASE_PILOT_DAMAGE);
        register(ADVANCED_COMBAT, COMBAT_DAMAGE, "combat.damage.heatAndFire",
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_START_FIRE,
              OptionsConstants.ADVANCED_COMBAT_FOREST_FIRES_NO_SMOKE,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HEAT,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_COOLANT_FAILURE,
              OptionsConstants.ADVANCED_COMBAT_MAX_EXTERNAL_HEAT);

        register(ADVANCED_COMBAT, COMBAT_PHYSICAL, "combat.physical.attacks",
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CHARGE_DAMAGE,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_GRAPPLING,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_JUMP_JET_ATTACK,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_TRIP_ATTACK,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RETRACTABLE_BLADES,
              OptionsConstants.ADVANCED_COMBAT_PICKING_UP_AND_THROWING_UNITS,
              OptionsConstants.ADVANCED_COMBAT_CLUBS_PUNCH);
        register(ADVANCED_COMBAT, COMBAT_PHYSICAL, "combat.physical.special",
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_PRONE_FIRE,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BULLDOZER,
              OptionsConstants.UNOFFICIAL_BACKHOE_CLEARS_RUBBLE);

        register(ADVANCED_COMBAT, COMBAT_UNITS, "combat.units.vehicles",
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VEHICLE_EFFECTIVE,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VEHICLE_ARCS,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VTOL_ATTACKS,
              OptionsConstants.ADVANCED_COMBAT_VTOL_STRAFING,
              OptionsConstants.ADVANCED_COMBAT_VEHICLES_SAFE_FROM_INFERNOS,
              OptionsConstants.ADVANCED_COMBAT_FULL_ROTOR_HITS);
        register(ADVANCED_COMBAT, COMBAT_UNITS, "combat.units.battleArmor",
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BA_CRITICAL_SLOTS,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BA_VS_BA,
              OptionsConstants.ADVANCED_COMBAT_PROTOMEKS_SAFE_FROM_INFERNOS);
    }

    private static void registerGroundMovementOptions() {
        register(GROUND_MOVEMENT, MOVEMENT_MEKS, "movement.meks.modes",
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_SPRINT,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_STANDING_STILL,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_EVADE,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_SKILLED_EVASION,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEAPING,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_CLIMBING,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_WALK_BACKWARDS);
        register(GROUND_MOVEMENT, MOVEMENT_MEKS, "movement.meks.stability",
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_PHYSICAL_PSR,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_PHYSICAL_ATTACK_PSR,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_TAKING_DAMAGE,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEG_DAMAGE,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_FALLING_EXPANDED,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_FALLS_END_MOVEMENT,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_PSR_JUMP_HEAVY_WOODS);
        register(GROUND_MOVEMENT, MOVEMENT_MEKS, "movement.meks.formations",
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_ATTEMPTING_STAND,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_CAREFUL_STAND,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_MEK_LANCE_MOVEMENT,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_MEK_LANCE_MOVEMENT_NUMBER,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_NO_PRE_MOVE_VIBRA);

        register(GROUND_MOVEMENT, MOVEMENT_VEHICLES, "movement.vehicles.maneuvers",
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_ACCELERATION,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_REVERSE_GEAR,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TURN_MODE,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_ADVANCED_MANEUVERS,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_HULL_DOWN,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_NO_HOVER_CHARGE);
        register(GROUND_MOVEMENT, MOVEMENT_VEHICLES, "movement.vehicles.formations",
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_LANCE_MOVEMENT,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_LANCE_MOVEMENT_NUMBER,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_UNOFF_NO_IMMOBILE_VEHICLES);

        register(GROUND_MOVEMENT, MOVEMENT_INFANTRY, "movement.infantry.movement",
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_FAST_INFANTRY_MOVE,
              OptionsConstants.ADVANCED_TAC_OPS_INF_PAVE_BONUS,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_ZIPLINES,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_NO_NIGHT_MOVE_PEN);
        register(INITIATIVE, MOVEMENT_INFANTRY, "movement.infantry.movement",
              OptionsConstants.INIT_INF_MOVE_MULTI,
              OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI,
              OptionsConstants.INIT_INF_PROTO_MOVE_MULTI);
    }

    private static void registerAerospaceOptions() {
        register(AEROSPACE, AEROSPACE_FLIGHT, "aerospace.flight.movement",
              OptionsConstants.ADVANCED_AERO_RULES_AERO_GROUND_MOVE,
              OptionsConstants.ADVANCED_AERO_RULES_ADVANCED_MOVEMENT,
              OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY,
              OptionsConstants.ADVANCED_AERO_RULES_RETURN_FLYOVER,
              OptionsConstants.ADVANCED_AERO_RULES_CLIMB_OUT,
              OptionsConstants.ADVANCED_AERO_RULES_AA_MOVE_MOD);
        register(AEROSPACE, AEROSPACE_FLIGHT, "aerospace.flight.atmosphere",
              OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_GRAV_EFFECTS,
              OptionsConstants.ADVANCED_AERO_RULES_ATMOSPHERIC_CONTROL,
              OptionsConstants.UNOFFICIAL_ADV_ATMOSPHERIC_CONTROL);
        register(AEROSPACE, AEROSPACE_FLIGHT, "aerospace.flight.formations",
              OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_CAPITAL_FIGHTER,
              OptionsConstants.ADVANCED_AERO_RULES_ALLOW_LARGE_SQUADRONS,
              OptionsConstants.ADVANCED_AERO_RULES_SINGLE_NO_CAP);

        register(AEROSPACE, AEROSPACE_TARGETING, "aerospace.targeting.sensors",
              OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ECM,
              OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_SENSOR_SHADOW,
              OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADVANCED_SENSORS,
              OptionsConstants.ADVANCED_AERO_RULES_VARIABLE_DAMAGE_THRESH);
        register(AEROSPACE, AEROSPACE_TARGETING, "aerospace.targeting.fireControl",
              OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_AAA_LASER,
              OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADV_POINT_DEFENSE,
              OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_BRACKET_FIRE,
              OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_OVER_PENETRATE,
              OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_AA_FIRE);
        register(AEROSPACE, AEROSPACE_TARGETING, "aerospace.targeting.launchControl",
              OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_BEARINGS_ONLY_LAUNCH,
              OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_BEARINGS_ONLY_VELOCITY,
              OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_WAYPOINT_LAUNCH);

        register(AEROSPACE, AEROSPACE_VESSELS, "aerospace.vessels.systems",
              OptionsConstants.ADVANCED_AERO_RULES_FUEL_CONSUMPTION,
              OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_CONV_FUSION_BONUS,
              OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_HARJEL,
              OptionsConstants.ADVANCED_AERO_RULES_HEAT_BY_BAY,
              OptionsConstants.ADVANCED_AERO_RULES_CRASHED_DROPSHIPS_SURVIVE,
              OptionsConstants.ADVANCED_AERO_RULES_EXPANDED_KF_DRIVE_DAMAGE);
        register(AEROSPACE, AEROSPACE_VESSELS, "aerospace.vessels.ordnance",
              OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_AMMO_EXPLOSIONS,
              OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_SPACE_BOMB,
              OptionsConstants.ADVANCED_AERO_RULES_AT2_NUKES,
              OptionsConstants.ADVANCED_AERO_RULES_AERO_ARTILLERY_MUNITIONS);
    }

    private static void registerInitiativeAndRpgOptions() {
        register(INITIATIVE, INITIATIVE_AND_PILOTS_PHASES, "initiative.infantry",
              OptionsConstants.INIT_INF_MOVE_EVEN,
              OptionsConstants.INIT_INF_DEPLOY_EVEN,
              OptionsConstants.INIT_INF_MOVE_LATER);
        register(INITIATIVE, INITIATIVE_AND_PILOTS_PHASES, "initiative.protomeks",
              OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN,
              OptionsConstants.INIT_PROTOMEKS_MOVE_LATER);
        register(INITIATIVE, INITIATIVE_AND_PILOTS_PHASES, "initiative.simultaneous",
              OptionsConstants.INIT_SIMULTANEOUS_DEPLOYMENT,
              OptionsConstants.INIT_SIMULTANEOUS_TARGETING,
              OptionsConstants.INIT_SIMULTANEOUS_FIRING,
              OptionsConstants.INIT_SIMULTANEOUS_PHYSICAL,
              OptionsConstants.INIT_FRONT_LOAD_INITIATIVE,
              OptionsConstants.INIT_INITIATIVE_STREAK_COMPENSATION);

        register(RPG, INITIATIVE_AND_PILOTS_ABILITIES, "rpg.core",
              OptionsConstants.RPG_PILOT_ADVANTAGES,
              OptionsConstants.EDGE,
              OptionsConstants.RPG_MANEI_DOMINI);
        register(RPG, INITIATIVE_AND_PILOTS_ABILITIES, "rpg.initiative",
              OptionsConstants.RPG_INDIVIDUAL_INITIATIVE,
              OptionsConstants.RPG_COMMAND_INIT);
        register(RPG, INITIATIVE_AND_PILOTS_ABILITIES, "rpg.skills",
              OptionsConstants.RPG_RPG_GUNNERY,
              OptionsConstants.RPG_ARTILLERY_SKILL,
              OptionsConstants.RPG_TOUGHNESS);
        register(RPG, INITIATIVE_AND_PILOTS_ABILITIES, "rpg.conditions",
              OptionsConstants.RPG_BEGIN_SHUTDOWN);
    }

    private static void register(String sourceGroupId, PageDefinition page, String sectionId,
          String... optionNames) {
        Map<String, Integer> pageSectionOrders = SECTION_ORDERS.computeIfAbsent(page,
              ignored -> new LinkedHashMap<>());
        int sectionOrder = pageSectionOrders.computeIfAbsent(sectionId, ignored -> pageSectionOrders.size());
        Map<String, Integer> pageOptionOrders = NEXT_OPTION_ORDERS.computeIfAbsent(page,
              ignored -> new LinkedHashMap<>());
        int firstOptionOrder = pageOptionOrders.getOrDefault(sectionId, 0);
        for (int optionOrder = 0; optionOrder < optionNames.length; optionOrder++) {
            String optionName = optionNames[optionOrder];
            Location existing = LOCATIONS.putIfAbsent(optionName,
                  new Location(sourceGroupId, page, sectionId, sectionOrder,
                        firstOptionOrder + optionOrder));
            if (existing != null) {
                throw new IllegalStateException("Duplicate Game Options presentation location for " + optionName);
            }
        }
        pageOptionOrders.put(sectionId, firstOptionOrder + optionNames.length);
    }

    private static PageDefinition nestedPage(String id, String categoryId, String iconGroupId, boolean advanced,
          int order) {
        return new PageDefinition(id, categoryId, iconGroupId, "", advanced, order);
    }
    private static PageDefinition directPage(String id, String iconGroupId, boolean advanced, int order) {
        return new PageDefinition(id, "", iconGroupId, "", advanced, order);
    }

    private static PageDefinition nestedPageWithSingleSourceFallback(String id, String categoryId, String iconGroupId,
          String fallbackSourceGroupId, boolean advanced, int order) {
        return new PageDefinition(id, categoryId, iconGroupId, fallbackSourceGroupId, advanced, order);
    }

    /**
     * @param sourceGroupId source option-group identifier
     * @param page          destination page
     * @param sectionId     destination section identifier
     * @param sectionOrder  section position within the page
     * @param optionOrder   option position within the section; {@link Integer#MAX_VALUE} appends fallback entries
     */
    record Location(String sourceGroupId, PageDefinition page, String sectionId, int sectionOrder, int optionOrder) {
    }

    /**
     * @param id                    page identifier
     * @param categoryId            parent navigation category identifier
     * @param iconGroupId           source group supplying the page icon
     * @param fallbackSourceGroupId source group supplying title/icon when it is the page's only source
     * @param advanced              whether the page uses the advanced-rules marker
     * @param order                 page position in navigation
     */
    record PageDefinition(String id, String categoryId, String iconGroupId, String fallbackSourceGroupId,
          boolean advanced, int order) {
        String title(Map<String, String> sourceGroups) {
            return usesSingleSourceFallback(sourceGroups)
                  ? sourceGroups.get(fallbackSourceGroupId)
                  : getTextAt(CLIENT_BUNDLE, "GameOptionsDialog.page." + id + ".title");
        }

        List<String> path(Map<String, String> sourceGroups) {
            if (categoryId.isBlank()) {
                return List.of(title(sourceGroups));
            }
            return List.of(getTextAt(CLIENT_BUNDLE, "GameOptionsDialog.category." + categoryId),
                  title(sourceGroups));
        }

        List<String> pathIds() {
            if (categoryId.isBlank()) {
                return List.of(routeId());
            }
            return List.of("gameOptions." + categoryId, routeId());
        }

        boolean isDirect() {
            return categoryId.isBlank();
        }

        String effectiveIconGroupId(Map<String, String> sourceGroups) {
            return usesSingleSourceFallback(sourceGroups) ? fallbackSourceGroupId : iconGroupId;
        }

        String routeId() {
            return "gameOptions." + id;
        }

        private boolean usesSingleSourceFallback(Map<String, String> sourceGroups) {
            return !fallbackSourceGroupId.isBlank() && sourceGroups.size() == 1
                  && sourceGroups.containsKey(fallbackSourceGroupId);
        }
    }
}
