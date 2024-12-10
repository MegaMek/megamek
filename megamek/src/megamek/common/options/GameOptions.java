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

import java.io.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

/**
 * Contains the options determining play in the current game.
 * TODO: This class and every other "Options" need to be refactored, probably using enums?
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

    private static final Option.OptionValue[] BASE_OPTIONS = {
        Option.of(OptionsConstants.BASE_PUSH_OFF_BOARD, true),
        Option.of(OptionsConstants.BASE_DUMPING_FROM_ROUND, 1),
        Option.of(OptionsConstants.BASE_LOBBY_AMMO_DUMP, false),
        Option.of(OptionsConstants.BASE_SHOW_BAY_DETAIL, false),
        Option.of(OptionsConstants.BASE_INDIRECT_FIRE, true),
        Option.of(OptionsConstants.BASE_FLAMER_HEAT, false),
        Option.of(OptionsConstants.BASE_INFANTRY_DAMAGE_HEAT, false),
        Option.of(OptionsConstants.BASE_AUTO_AMS, true),
        Option.of(OptionsConstants.BASE_RANDOM_BASEMENTS, true),
        Option.of(OptionsConstants.BASE_BREEZE, false)
    };

    private static final Option.OptionValue[] VICTORY_OPTIONS = {
        Option.of(OptionsConstants.VICTORY_SKIP_FORCED_VICTORY, false),
        Option.of(OptionsConstants.VICTORY_ACHIEVE_CONDITIONS, 1),
        Option.of(OptionsConstants.VICTORY_USE_BV_DESTROYED, false),
        Option.of(OptionsConstants.VICTORY_BV_DESTROYED_PERCENT, 100),
        Option.of(OptionsConstants.VICTORY_USE_BV_RATIO, false),
        Option.of(OptionsConstants.VICTORY_BV_RATIO_PERCENT, 300),
        Option.of(OptionsConstants.VICTORY_USE_GAME_TURN_LIMIT, false),
        Option.of(OptionsConstants.VICTORY_GAME_TURN_LIMIT, 10),
        Option.of(OptionsConstants.VICTORY_USE_KILL_COUNT, false),
        Option.of(OptionsConstants.VICTORY_GAME_KILL_COUNT, 4),
        Option.of(OptionsConstants.VICTORY_COMMANDER_KILLED, false)
    };

    private static final Option.OptionValue[] ALLOWED_OPTIONS = {
        Option.of(OptionsConstants.ALLOWED_CANON_ONLY, false),
        Option.of(OptionsConstants.ALLOWED_YEAR, 3150),
        Option.of(OptionsConstants.ALLOWED_TECHLEVEL, TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_STANDARD]),
        Option.of(OptionsConstants.ALLOWED_ERA_BASED, false),
        Option.of(OptionsConstants.ALLOWED_ALLOW_ILLEGAL_UNITS, false),
        Option.of(OptionsConstants.ALLOWED_SHOW_EXTINCT, true),
        Option.of(OptionsConstants.ALLOWED_CLAN_IGNORE_EQ_LIMITS, false),
        Option.of(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL, false),
        Option.of(OptionsConstants.ALLOWED_ALLOW_NUKES, false),
        Option.of(OptionsConstants.ALLOWED_REALLY_ALLOW_NUKES, false)
    };

    private static final Option.OptionValue[] ADVANCED_RULES_OPTIONS = {
        Option.of(OptionsConstants.ADVANCED_MINEFIELDS, false),
        Option.of(OptionsConstants.ADVANCED_HIDDEN_UNITS, true),
        Option.of(OptionsConstants.ADVANCED_BLACK_ICE, false),
        Option.of(OptionsConstants.ADVANCED_LIGHTNING_STORM_TARGETS_UNITS, false),
        Option.of(OptionsConstants.ADVANCED_DOUBLE_BLIND, false),
        Option.of(OptionsConstants.ADVANCED_TACOPS_SENSORS, false),
        Option.of(OptionsConstants.ADVANCED_SUPRESS_ALL_DB_MESSAGES, false),
        Option.of(OptionsConstants.ADVANCED_SUPPRESS_DB_BV, false),
        Option.of(OptionsConstants.ADVANCED_TEAM_VISION, true),
        Option.of(OptionsConstants.ADVANCED_TACOPS_BAP, false),
        Option.of(OptionsConstants.ADVANCED_TACOPS_ECCM, false),
        Option.of(OptionsConstants.ADVANCED_TACOPS_GHOST_TARGET, false),
        Option.of(OptionsConstants.ADVANCED_GHOST_TARGET_MAX, 5),
        Option.of(OptionsConstants.ADVANCED_TACOPS_DIG_IN, false),
        Option.of(OptionsConstants.ADVANCED_TACOPS_BA_WEIGHT, false),
        Option.of(OptionsConstants.ADVANCED_TACOPS_TAKE_COVER, false),
        Option.of(OptionsConstants.ADVANCED_TACOPS_ANGEL_ECM, false),
        Option.of(OptionsConstants.ADVANCED_TACOPS_BATTLE_WRECK, false),
        Option.of(OptionsConstants.ADVANCED_TACOPS_SKIN_OF_THE_TEETH_EJECTION, false),
        Option.of(OptionsConstants.ADVANCED_TACOPS_MOBILE_HQS, false),
        Option.of(OptionsConstants.ADVANCED_TACOPS_FATIGUE, false),
        Option.of(OptionsConstants.ADVANCED_TACOPS_FUMBLES, false),
        Option.of(OptionsConstants.ADVANCED_TACOPS_SELF_DESTRUCT, false),
        Option.of(OptionsConstants.ADVANCED_TACOPS_TANK_CREWS, false),
        Option.of(OptionsConstants.ADVANCED_STRATOPS_QUIRKS, false),
        Option.of(OptionsConstants.ADVANCED_STRATOPS_PARTIALREPAIRS, false),
        Option.of(OptionsConstants.ADVANCED_ASSAULT_DROP, false),
        Option.of(OptionsConstants.ADVANCED_PARATROOPERS, false),
        Option.of(OptionsConstants.ADVANCED_INCLUSIVE_SENSOR_RANGE, false),
        Option.of(OptionsConstants.ADVANCED_SENSORS_DETECT_ALL, false),
        Option.of(OptionsConstants.ADVANCED_MAGSCAN_NOHILLS, false),
        Option.of(OptionsConstants.ADVANCED_WOODS_BURN_DOWN, false),
        Option.of(OptionsConstants.ADVANCED_WOODS_BURN_DOWN_AMOUNT, 5),
        Option.of(OptionsConstants.ADVANCED_NO_IGNITE_CLEAR, false),
        Option.of(OptionsConstants.ADVANCED_ALL_HAVE_EI_COCKPIT, false),
        Option.of(OptionsConstants.ADVANCED_EXTREME_TEMPERATURE_SURVIVAL, false),
        Option.of(OptionsConstants.ADVANCED_ARMED_MEKWARRIORS, false),
        Option.of(OptionsConstants.ADVANCED_PILOTS_VISUAL_RANGE_ONE, false),
        Option.of(OptionsConstants.ADVANCED_PILOTS_CANNOT_SPOT, false),
        Option.of(OptionsConstants.ADVANCED_METAL_CONTENT, false),
        Option.of(OptionsConstants.ADVANCED_BA_GRAB_BARS, false),
        Option.of(OptionsConstants.ADVANCED_MAXTECH_MOVEMENT_MODS, false),
        Option.of(OptionsConstants.ADVANCED_ALTERNATE_MASC, false),
        Option.of(OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED, false),
        Option.of(OptionsConstants.ADVANCED_SINGLE_BLIND_BOTS, false)
    };

    private static final Option.OptionValue[] ADVANCED_COMBAT_OPTIONS = {
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_AMS, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_MANUAL_AMS, false),
        Option.of(OptionsConstants.ADVCOMBAT_FLOATING_CRITS, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_CRIT_ROLL, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_ENGINE_EXPLOSIONS, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_CALLED_SHOTS, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_PRONE_FIRE, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_START_FIRE, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_RANGE, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_DEAD_ZONES, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_LOS1, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_ALTDMG, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_CLUSTERHITPEN, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_PPC_INHIBITORS, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_CHARGE_DAMAGE, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_BURST, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_HEAT, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_PARTIAL_COVER, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_BA_CRITICALS, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_RAPID_AC, false),
        Option.of(OptionsConstants.ADVCOMBAT_KIND_RAPID_AC, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_GRAPPLING, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_JUMP_JET_ATTACK, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_TRIP_ATTACK, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_ENERGY_WEAPONS, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_GAUSS_WEAPONS, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_RETRACTABLE_BLADES, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_AMMUNITION, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_WOODS_COVER, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_EFFECTIVE, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_VTOL_ATTACKS, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_ADVANCED_MEK_HIT_LOCATIONS, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_COOLANT_FAILURE, false),
        Option.of(OptionsConstants.ADVCOMBAT_TACOPS_BA_VS_BA, false),
        Option.of(OptionsConstants.ADVCOMBAT_NO_TAC, false),
        Option.of(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD, false),
        Option.of(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD_VARIABLE, false),
        Option.of(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD_DIVISOR, 10),
        Option.of(OptionsConstants.ADVCOMBAT_VTOL_STRAFING, false),
        Option.of(OptionsConstants.ADVCOMBAT_VEHICLES_SAFE_FROM_INFERNOS, false),
        Option.of(OptionsConstants.ADVCOMBAT_PROTOS_SAFE_FROM_INFERNOS, false),
        Option.of(OptionsConstants.ADVCOMBAT_INDIRECT_ALWAYS_POSSIBLE, false),
        Option.of(OptionsConstants.ADVCOMBAT_INCREASED_AC_DMG, false),
        Option.of(OptionsConstants.ADVCOMBAT_UNJAM_UAC, false),
        Option.of(OptionsConstants.ADVCOMBAT_UAC_TWOROLLS, false),
        Option.of(OptionsConstants.ADVCOMBAT_CLUBS_PUNCH, false),
        Option.of(OptionsConstants.ADVCOMBAT_ON_MAP_PREDESIGNATE, false),
        Option.of(OptionsConstants.ADVCOMBAT_NUM_HEXES_PREDESIGNATE, 5),
        Option.of(OptionsConstants.ADVCOMBAT_MAP_AREA_PREDESIGNATE, 1088),
        Option.of(OptionsConstants.ADVCOMBAT_MAX_EXTERNAL_HEAT, 15),
        Option.of(OptionsConstants.ADVCOMBAT_CASE_PILOT_DAMAGE, false),
        Option.of(OptionsConstants.ADVCOMBAT_NO_FORCED_PRIMARY_TARGETS, false),
        Option.of(OptionsConstants.ADVCOMBAT_FULL_ROTOR_HITS, false),
        Option.of(OptionsConstants.ADVCOMBAT_FOREST_FIRES_NO_SMOKE, false),
        Option.of(OptionsConstants.ADVCOMBAT_HOTLOAD_IN_GAME, false),
        Option.of(OptionsConstants.ADVCOMBAT_MULTI_USE_AMS, false)
    };

    private static final Option.OptionValue[] ADVANCED_GROUND_MOVEMENT_OPTIONS = {
        Option.of(OptionsConstants.ADVGRNDMOV_TACOPS_SPRINT, false),
        Option.of(OptionsConstants.ADVGRNDMOV_TACOPS_STANDING_STILL, false),
        Option.of(OptionsConstants.ADVGRNDMOV_TACOPS_EVADE, false),
        Option.of(OptionsConstants.ADVGRNDMOV_TACOPS_SKILLED_EVASION, false),
        Option.of(OptionsConstants.ADVGRNDMOV_TACOPS_LEAPING, false),
        Option.of(OptionsConstants.ADVGRNDMOV_TACOPS_PHYSICAL_PSR, false),
        Option.of(OptionsConstants.ADVGRNDMOV_TACOPS_PHYSICAL_ATTACK_PSR, false),
        Option.of(OptionsConstants.ADVGRNDMOV_TACOPS_TAKING_DAMAGE, false),
        Option.of(OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE, false),
        Option.of(OptionsConstants.ADVGRNDMOV_TACOPS_WALK_BACKWARDS, false),
        Option.of(OptionsConstants.ADVGRNDMOV_TACOPS_FAST_INFANTRY_MOVE, false),
        Option.of(OptionsConstants.ADVANCED_TACOPS_INF_PAVE_BONUS, false),
        Option.of(OptionsConstants.ADVGRNDMOV_VEHICLE_LANCE_MOVEMENT, false),
        Option.of(OptionsConstants.ADVGRNDMOV_VEHICLE_LANCE_MOVEMENT_NUMBER, 4),
        Option.of(OptionsConstants.ADVGRNDMOV_VEHICLE_ACCELERATION, false),
        Option.of(OptionsConstants.ADVGRNDMOV_REVERSE_GEAR, false),
        Option.of(OptionsConstants.ADVGRNDMOV_TURN_MODE, false),
        Option.of(OptionsConstants.ADVGRNDMOV_VEHICLE_ADVANCED_MANEUVERS, false),
        Option.of(OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN, false),
        Option.of(OptionsConstants.ADVGRNDMOV_TACOPS_FALLING_EXPANDED, false),
        Option.of(OptionsConstants.ADVGRNDMOV_TACOPS_ATTEMPTING_STAND, false),
        Option.of(OptionsConstants.ADVGRNDMOV_TACOPS_CAREFUL_STAND, false),
        Option.of(OptionsConstants.ADVGRNDMOV_TACOPS_ZIPLINES, false),
        Option.of(OptionsConstants.ADVGRNDMOV_MEK_LANCE_MOVEMENT, false),
        Option.of(OptionsConstants.ADVGRNDMOV_MEK_LANCE_MOVEMENT_NUMBER, 4),
        Option.of(OptionsConstants.ADVGRNDMOV_NO_IMMOBILE_VEHICLES, false),
        Option.of(OptionsConstants.ADVGRNDMOV_VEHICLES_CAN_EJECT, false),
        Option.of(OptionsConstants.ADVGRNDMOV_EJECTED_PILOTS_FLEE, false),
        Option.of(OptionsConstants.ADVGRNDMOV_AUTO_ABANDON_UNIT, false),
        Option.of(OptionsConstants.ADVGRNDMOV_NO_HOVER_CHARGE, false),
        Option.of(OptionsConstants.ADVGRNDMOV_NO_PREMOVE_VIBRA, false),
        Option.of(OptionsConstants.ADVGRNDMOV_FALLS_END_MOVEMENT, false),
        Option.of(OptionsConstants.ADVGRNDMOV_PSR_JUMP_HEAVY_WOODS, false),
        Option.of(OptionsConstants.ADVGRNDMOV_NO_NIGHT_MOVE_PEN, false)
    };

    private static final Option.OptionValue[] ADVANCED_AERO_RULES_OPTIONS = {
        Option.of(OptionsConstants.ADVAERORULES_AERO_GROUND_MOVE, true),
        Option.of(OptionsConstants.ADVAERORULES_STRATOPS_CAPITAL_FIGHTER, false),
        Option.of(OptionsConstants.ADVAERORULES_FUEL_CONSUMPTION, false),
        Option.of(OptionsConstants.ADVAERORULES_STRATOPS_CONV_FUSION_BONUS, false),
        Option.of(OptionsConstants.ADVAERORULES_STRATOPS_HARJEL, false),
        Option.of(OptionsConstants.ADVAERORULES_STRATOPS_GRAV_EFFECTS, false),
        Option.of(OptionsConstants.ADVAERORULES_ADVANCED_MOVEMENT, false),
        Option.of(OptionsConstants.ADVAERORULES_HEAT_BY_BAY, false),
        Option.of(OptionsConstants.ADVAERORULES_ATMOSPHERIC_CONTROL, false),
        Option.of(OptionsConstants.ADVAERORULES_STRATOPS_AMMO_EXPLOSIONS, false),
        Option.of(OptionsConstants.ADVAERORULES_STRATOPS_AAA_LASER, false),
        Option.of(OptionsConstants.ADVAERORULES_STRATOPS_ADV_POINTDEF, false),
        Option.of(OptionsConstants.ADVAERORULES_STRATOPS_BRACKET_FIRE, false),
        Option.of(OptionsConstants.ADVAERORULES_STRATOPS_ECM, false),
        Option.of(OptionsConstants.ADVAERORULES_STRATOPS_SENSOR_SHADOW, false),
        Option.of(OptionsConstants.ADVAERORULES_STRATOPS_OVER_PENETRATE, false),
        Option.of(OptionsConstants.ADVAERORULES_STRATOPS_SPACE_BOMB, false),
        Option.of(OptionsConstants.ADVAERORULES_STRATOPS_BEARINGS_ONLY_LAUNCH, false),
        Option.of(OptionsConstants.ADVAERORULES_STRATOPS_BEARINGS_ONLY_VELOCITY, 50),
        Option.of(OptionsConstants.ADVAERORULES_STRATOPS_WAYPOINT_LAUNCH, false),
        Option.of(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS, false),
        Option.of(OptionsConstants.ADVAERORULES_VARIABLE_DAMAGE_THRESH, false),
        Option.of(OptionsConstants.ADVAERORULES_AT2_NUKES, false),
        Option.of(OptionsConstants.ADVAERORULES_AERO_SANITY, false),
        Option.of(OptionsConstants.ADVAERORULES_RETURN_FLYOVER, true),
        Option.of(OptionsConstants.ADVAERORULES_STRATOPS_AA_FIRE, false),
        Option.of(OptionsConstants.ADVAERORULES_AA_MOVE_MOD, false),
        Option.of(OptionsConstants.ADVAERORULES_ALLOW_LARGE_SQUADRONS, false),
        Option.of(OptionsConstants.ADVAERORULES_SINGLE_NO_CAP, false),
        Option.of(OptionsConstants.ADVAERORULES_AERO_ARTILLERY_MUNITIONS, false),
        Option.of(OptionsConstants.ADVAERORULES_CRASHED_DROPSHIPS_SURVIVE, false),
        Option.of(OptionsConstants.ADVAERORULES_EXPANDED_KF_DRIVE_DAMAGE, false),
        Option.of(OptionsConstants.UNOFF_ADV_ATMOSPHERIC_CONTROL, false)
    };

    private static final Option.OptionValue[] INITIATIVE_OPTIONS = {
        Option.of(OptionsConstants.INIT_INF_MOVE_EVEN, false),
        Option.of(OptionsConstants.INIT_INF_DEPLOY_EVEN, false),
        Option.of(OptionsConstants.INIT_INF_MOVE_LATER, false),
        Option.of(OptionsConstants.INIT_INF_MOVE_MULTI, false),
        Option.of(OptionsConstants.INIT_PROTOS_MOVE_EVEN, false),
        Option.of(OptionsConstants.INIT_PROTOS_MOVE_LATER, false),
        Option.of(OptionsConstants.INIT_PROTOS_MOVE_MULTI, false),
        Option.of(OptionsConstants.INIT_INF_PROTO_MOVE_MULTI, 3),
        Option.of(OptionsConstants.INIT_SIMULTANEOUS_DEPLOYMENT, false),
        Option.of(OptionsConstants.INIT_SIMULTANEOUS_TARGETING, false),
        Option.of(OptionsConstants.INIT_SIMULTANEOUS_FIRING, false),
        Option.of(OptionsConstants.INIT_SIMULTANEOUS_PHYSICAL, false),
        Option.of(OptionsConstants.INIT_FRONT_LOAD_INITIATIVE, false),
        Option.of(OptionsConstants.INIT_INITIATIVE_STREAK_COMPENSATION, false)
    };

    private static final Option.OptionValue[] RPG_OPTIONS = {
        Option.of(OptionsConstants.RPG_PILOT_ADVANTAGES, false),
        Option.of(OptionsConstants.EDGE, false),
        Option.of(OptionsConstants.RPG_MANEI_DOMINI, false),
        Option.of(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE, false),
        Option.of(OptionsConstants.RPG_COMMAND_INIT, false),
        Option.of(OptionsConstants.RPG_RPG_GUNNERY, false),
        Option.of(OptionsConstants.RPG_ARTILLERY_SKILL, false),
        Option.of(OptionsConstants.RPG_TOUGHNESS, false),
        Option.of(OptionsConstants.RPG_CONDITIONAL_EJECTION, false),
        Option.of(OptionsConstants.RPG_BEGIN_SHUTDOWN, false)
    };

    @Override
    public synchronized void initialize() {
        super.initialize();

        IBasicOptionGroup baseGroup = addGroup("basic");
        addOptions(baseGroup, BASE_OPTIONS);

        IBasicOptionGroup victoryGroup = addGroup("victory");
        addOptions(victoryGroup, VICTORY_OPTIONS);

        IBasicOptionGroup allowedGroup = addGroup("allowedUnits");
        addOptions(allowedGroup, ALLOWED_OPTIONS);

        IBasicOptionGroup advancedRulesGroup = addGroup("advancedRules");
        addOptions(advancedRulesGroup, ADVANCED_RULES_OPTIONS);

        IBasicOptionGroup advancedCombatGroup = addGroup("advancedCombat");
        addOptions(advancedCombatGroup, ADVANCED_COMBAT_OPTIONS);

        IBasicOptionGroup advancedGroundMovementGroup = addGroup("advancedGroundMovement");
        addOptions(advancedGroundMovementGroup, ADVANCED_GROUND_MOVEMENT_OPTIONS);

        IBasicOptionGroup advancedAeroRulesGroup = addGroup("advancedAeroRules");
        addOptions(advancedAeroRulesGroup, ADVANCED_AERO_RULES_OPTIONS);

        IBasicOptionGroup initiativeGroup = addGroup("initiative");
        addOptions(initiativeGroup, INITIATIVE_OPTIONS);

        IBasicOptionGroup rpgGroup = addGroup("rpg");
        addOptions(rpgGroup, RPG_OPTIONS);
    }

    private void addOptions(IBasicOptionGroup group, Option.OptionValue[] options) {
        for (var entry : options) {
            int type = entry.getType();

            if (type == IOption.STRING && OptionsConstants.ALLOWED_TECHLEVEL.equals(entry.getName())) {
                type = IOption.CHOICE;
            }
            addOption(group, entry.getName(), type, entry.getValue());
        }
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
            logger.error("Error loading XML for game options: " + e.getMessage(), e);
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
                        logger.error(String.format(
                                "Error trying to load option '%s' with a value of '%s'!", name, value));
                    }
                }
            } else {
                logger.warn("Invalid option '" + name + "' when trying to load options file!");
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

    @Override
    public Map<String, IOption> getOptionsHash() {
        return this.optionsHash;
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
        for (final Enumeration<IOptionGroup> groups = getGroups(); groups.hasMoreElements();) {
            final IOptionGroup group = groups.nextElement();
            for (final Enumeration<IOption> options = group.getOptions(); options.hasMoreElements();) {
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
     * This is used to fill a GameOptions object from an XML node list written using
     * writeToXML.
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
