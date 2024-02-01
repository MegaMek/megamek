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
package megamek.common.options;

import megamek.common.*;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static megamek.common.options.OptionsConstants.*;

/**
 * Contains the options determining Unit Quirks of a unit (but not weapon quirks). When changing this, note
 * that all options should remain boolean options.
 *
 * @author Taharqa (Jay Lawson)
 */
public class Quirks extends AbstractOptions {
    private static final long serialVersionUID = 7618380522964885740L;
    public static final String POS_QUIRKS = "PosQuirks";
    public static final String NEG_QUIRKS = "NegQuirks";

    @Override
    public synchronized void initialize() {
        IBasicOptionGroup posQuirk = addGroup("pos_quirks", POS_QUIRKS);
        addOption(posQuirk, QUIRK_POS_ANIMALISTIC, false);
        addOption(posQuirk, QUIRK_POS_ANTI_AIR, false);
        addOption(posQuirk, QUIRK_POS_ATMO_FLYER, false);
        addOption(posQuirk, QUIRK_POS_BATTLE_COMP, false);
        addOption(posQuirk, QUIRK_POS_BARREL_FIST_LA, false);
        addOption(posQuirk, QUIRK_POS_BARREL_FIST_RA, false);
        addOption(posQuirk, QUIRK_POS_BATTLE_FIST_LA, false);
        addOption(posQuirk, QUIRK_POS_BATTLE_FIST_RA, false);
        addOption(posQuirk, QUIRK_POS_COMBAT_COMPUTER, false);
        addOption(posQuirk, QUIRK_POS_COMMAND_MECH, false);
        addOption(posQuirk, QUIRK_POS_COMPACT, false);
        addOption(posQuirk, QUIRK_POS_COWL, false);
        addOption(posQuirk, QUIRK_POS_DIRECTIONAL_TORSO_MOUNT, false);
        addOption(posQuirk, QUIRK_POS_DISTRACTING, false);
        addOption(posQuirk, QUIRK_POS_DOCKING_ARMS, false);
        addOption(posQuirk, QUIRK_POS_EASY_MAINTAIN, false);
        addOption(posQuirk, QUIRK_POS_EASY_PILOT, false);
        addOption(posQuirk, QUIRK_POS_EXT_TWIST, false);
        addOption(posQuirk, QUIRK_POS_FAST_RELOAD, false);
        addOption(posQuirk, QUIRK_POS_FINE_MANIPULATORS, false);
        addOption(posQuirk, QUIRK_POS_GOOD_REP_1, false);
        addOption(posQuirk, QUIRK_POS_GOOD_REP_2, false);
        addOption(posQuirk, QUIRK_POS_HYPER_ACTUATOR, false);
        addOption(posQuirk, QUIRK_POS_IMP_COM, false);
        addOption(posQuirk, QUIRK_POS_IMP_LIFE_SUPPORT, false);
        addOption(posQuirk, QUIRK_POS_IMP_TARG_L, false);
        addOption(posQuirk, QUIRK_POS_IMP_TARG_M, false);
        addOption(posQuirk, QUIRK_POS_IMP_TARG_S, false);
        addOption(posQuirk, QUIRK_POS_IMPROVED_SENSORS, false);
        addOption(posQuirk, QUIRK_POS_INTERNAL_BOMB, false);
        addOption(posQuirk, QUIRK_POS_LOW_PROFILE, false);
        addOption(posQuirk, QUIRK_POS_MULTI_TRAC, false);
        addOption(posQuirk, QUIRK_POS_NIMBLE_JUMPER, false);
        addOption(posQuirk, QUIRK_POS_OVERHEAD_ARMS, false);
        addOption(posQuirk, QUIRK_POS_POWER_REVERSE, false);
        addOption(posQuirk, QUIRK_POS_PRO_ACTUATOR, false);
        addOption(posQuirk, QUIRK_POS_REINFORCED_LEGS, false);
        addOption(posQuirk, QUIRK_POS_RUGGED_1, false);
        addOption(posQuirk, QUIRK_POS_RUGGED_2, false);
        addOption(posQuirk, QUIRK_POS_RUMBLE_SEAT, false);
        addOption(posQuirk, QUIRK_POS_SCOUT_BIKE, false);
        addOption(posQuirk, QUIRK_POS_SEARCHLIGHT, false);
        addOption(posQuirk, QUIRK_POS_STABLE, false);
        addOption(posQuirk, QUIRK_POS_TRAILER_HITCH, false);
        addOption(posQuirk, QUIRK_POS_UBIQUITOUS_IS, false);
        addOption(posQuirk, QUIRK_POS_UBIQUITOUS_CLAN, false);
        addOption(posQuirk, QUIRK_POS_VAR_RNG_TARG_L, false);
        addOption(posQuirk, QUIRK_POS_VAR_RNG_TARG_S, false);
        addOption(posQuirk, QUIRK_POS_VESTIGIAL_HANDS_LA, false);
        addOption(posQuirk, QUIRK_POS_VESTIGIAL_HANDS_RA, false);
        addOption(posQuirk, QUIRK_POS_VTOL_ROTOR_COAXIAL, false);
        addOption(posQuirk, QUIRK_POS_VTOL_ROTOR_DUAL, false);


        //not yet implemented
        //Docking Arms (docking unimplemented)
        //Fast Reload (no game effect at present)
        //Improved Communications
        //Internal Bomb Bay
        //Variable Range Targeting
        //VTOL Rotor Arrangement (no vee adv move rules)
        //Compact Mech

        IBasicOptionGroup negQuirk = addGroup("neg_quirks", NEG_QUIRKS);
        addOption(negQuirk, QUIRK_NEG_BAD_REP_IS, false);
        addOption(negQuirk, QUIRK_NEG_BAD_REP_CLAN, false);
        addOption(negQuirk, QUIRK_NEG_CRAMPED_COCKPIT, false);
        addOption(negQuirk, QUIRK_NEG_DIFFICULT_EJECT, false);
        addOption(negQuirk, QUIRK_NEG_DIFFICULT_MAINTAIN, false);
        addOption(negQuirk, QUIRK_NEG_EM_INTERFERENCE_WHOLE, false);
        addOption(negQuirk, QUIRK_NEG_EXP_ACTUATOR, false);
        addOption(negQuirk, QUIRK_NEG_FLAWED_COOLING, false);
        addOption(negQuirk, QUIRK_NEG_FRAGILE_FUEL, false);
        addOption(negQuirk, QUIRK_NEG_GAS_HOG, false);
        addOption(negQuirk, QUIRK_NEG_HARD_PILOT, false);
        addOption(negQuirk, QUIRK_NEG_ILLEGAL_DESIGN, false);
        addOption(negQuirk, QUIRK_NEG_LARGE_DROPPER, false);
        addOption(negQuirk, QUIRK_NEG_LOW_ARMS, false);
        addOption(negQuirk, QUIRK_NEG_NO_ARMS, false);
        addOption(negQuirk, QUIRK_NEG_NO_EJECT, false);
        addOption(negQuirk, QUIRK_NEG_NO_TWIST, false);
        addOption(negQuirk, QUIRK_NEG_NON_STANDARD, false);
        addOption(negQuirk, QUIRK_NEG_OBSOLETE, false);
        addOption(negQuirk, QUIRK_NEG_POOR_LIFE_SUPPORT, false);
        addOption(negQuirk, QUIRK_NEG_POOR_PERFORMANCE, false);
        addOption(negQuirk, QUIRK_NEG_POOR_SEALING, false);
        addOption(negQuirk, QUIRK_NEG_POOR_TARG_L, false);
        addOption(negQuirk, QUIRK_NEG_POOR_TARG_M, false);
        addOption(negQuirk, QUIRK_NEG_POOR_TARG_S, false);
        addOption(negQuirk, QUIRK_NEG_POOR_WORK, false);
        addOption(negQuirk, QUIRK_NEG_PROTOTYPE, false);
        addOption(negQuirk, QUIRK_NEG_RAMSHACKLE, false);
        addOption(negQuirk, QUIRK_NEG_SENSOR_GHOSTS, false);
        addOption(negQuirk, QUIRK_NEG_SUSCEPTIBLE_CWS, false);
        addOption(negQuirk, QUIRK_NEG_UNBALANCED, false);
        addOption(negQuirk, QUIRK_NEG_UNSTREAMLINED, false);
        addOption(negQuirk, QUIRK_NEG_WEAK_HEAD_1, false);
        addOption(negQuirk, QUIRK_NEG_WEAK_HEAD_2, false);
        addOption(negQuirk, QUIRK_NEG_WEAK_HEAD_3, false);
        addOption(negQuirk, QUIRK_NEG_WEAK_HEAD_4, false);
        addOption(negQuirk, QUIRK_NEG_WEAK_HEAD_5, false);
        addOption(negQuirk, QUIRK_NEG_WEAK_LEGS, false);
        addOption(negQuirk, QUIRK_NEG_WEAK_UNDERCARRIAGE, false);
        addOption(negQuirk, QUIRK_NEG_ATMO_INSTABILITY, false);
        addOption(negQuirk, QUIRK_NEG_OVERSIZED, false);

        //quirks not implemented yet
        //Exposed Weapon Linkage (weapon-specific, sort of)
        //Gas Hog
        //Large Dropship (no docking)
        //Un-streamlined
        //Weak Head Armor
        //Weak Undercarriage (no landing)
        //Ramshackle
    }

    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return QuirksInfo.getInstance();
    }

    /** @return A list of unit quirks that are active in this Quirks object. */
    public List<IOption> activeQuirks() {
        return getOptionsList().stream().filter(IOption::booleanValue).collect(toList());
    }

    public static boolean isQuirkLegalFor(IOption quirk, Entity en) {
        String qName = quirk.getName();

        if (qName.equals(QUIRK_NEG_GAS_HOG)) {
            return en.hasEngine() &&
                    ((en.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE)
                            || (en.getEngine().getEngineType() == Engine.FUEL_CELL));
        }

        if (en instanceof Mech) {
            switch (qName) {
                case QUIRK_POS_BATTLE_FIST_LA:
                    return en.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM);
                case QUIRK_POS_BATTLE_FIST_RA:
                    return en.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM);
                case QUIRK_POS_BARREL_FIST_RA:
                    return en.hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM)
                            && !en.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM);
                case QUIRK_POS_BARREL_FIST_LA:
                    return en.hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)
                            && !en.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM);
                case QUIRK_NEG_OVERSIZED:
                    return en.getWeight() >= 60;
                case QUIRK_POS_COMPACT:
                    return en.getWeight() <= 55;
                default:
                    return quirk.isNoneOf(
                            QUIRK_POS_ATMO_FLYER,        QUIRK_NEG_ATMO_INSTABILITY,   QUIRK_POS_DOCKING_ARMS,
                            QUIRK_NEG_FRAGILE_FUEL,      QUIRK_POS_INTERNAL_BOMB,      QUIRK_POS_TRAILER_HITCH,
                            QUIRK_NEG_LARGE_DROPPER,     QUIRK_NEG_WEAK_UNDERCARRIAGE, QUIRK_POS_VTOL_ROTOR_COAXIAL,
                            QUIRK_POS_VTOL_ROTOR_DUAL,   QUIRK_POS_POWER_REVERSE,      QUIRK_NEG_UNSTREAMLINED);
            }
        }

        if (en instanceof Tank) {
            if (en instanceof GunEmplacement) {
                // Nov 2016 - Reviewed the idea of quirks with Ray from CGL. The working
                // made sense to him. Uncertain at this time if CGL would adopt them but
                // including them since Quirks is already an option. Hammer
                return quirk.isAnyOf(
                        QUIRK_POS_ANTI_AIR,     QUIRK_POS_IMP_COM,      QUIRK_POS_IMPROVED_SENSORS,
                        QUIRK_POS_IMP_TARG_S,   QUIRK_POS_IMP_TARG_M,   QUIRK_POS_IMP_TARG_L,
                        QUIRK_POS_LOW_PROFILE,  QUIRK_NEG_POOR_TARG_M,  QUIRK_NEG_EM_INTERFERENCE_WHOLE,
                        QUIRK_NEG_POOR_TARG_S,  QUIRK_NEG_POOR_TARG_L,  QUIRK_NEG_POOR_WORK,
                        QUIRK_NEG_SENSOR_GHOSTS);
            }

            switch (qName) {
                case QUIRK_POS_POWER_REVERSE:
                    return en.getMovementMode().isTrackedOrWheeled() && !en.isSupportVehicle();
                case QUIRK_NEG_FRAGILE_FUEL:
                    return !(en.hasEngine() && en.getEngine().isFusion());
                case QUIRK_POS_TRAILER_HITCH:
                    return !(en.getMovementMode().isHover() || en.getMovementMode().isVTOL());
                case QUIRK_POS_SCOUT_BIKE:
                    return (en.getMovementMode().isHover() || en.getMovementMode().isWheeled())
                            && (en.getWeight() <= 10.0);
                case QUIRK_POS_VTOL_ROTOR_COAXIAL:
                case QUIRK_POS_VTOL_ROTOR_DUAL:
                    return en instanceof VTOL;
                default:
                    return quirk.isAnyOf(
                            QUIRK_POS_ANTI_AIR,         QUIRK_POS_BATTLE_COMP,      QUIRK_POS_EASY_MAINTAIN,
                            QUIRK_POS_FAST_RELOAD,      QUIRK_POS_IMP_COM,          QUIRK_POS_IMPROVED_SENSORS,
                            QUIRK_POS_IMP_TARG_S,       QUIRK_POS_SEARCHLIGHT,      QUIRK_NEG_NON_STANDARD,
                            QUIRK_POS_IMP_TARG_M,       QUIRK_POS_IMP_TARG_L,       QUIRK_POS_LOW_PROFILE,
                            QUIRK_NEG_BAD_REP_IS,       QUIRK_NEG_BAD_REP_CLAN,     QUIRK_NEG_DIFFICULT_MAINTAIN,
                            QUIRK_NEG_EM_INTERFERENCE_WHOLE,                        QUIRK_NEG_POOR_PERFORMANCE,
                            QUIRK_NEG_HARD_PILOT,       QUIRK_NEG_POOR_TARG_S,      QUIRK_NEG_POOR_TARG_M,
                            QUIRK_NEG_POOR_TARG_L,      QUIRK_NEG_POOR_WORK,        QUIRK_NEG_PROTOTYPE,
                            QUIRK_NEG_SENSOR_GHOSTS,    QUIRK_POS_UBIQUITOUS_IS,    QUIRK_POS_UBIQUITOUS_CLAN);
            }
        }

        if (en instanceof BattleArmor) {
            return quirk.isAnyOf(
                    QUIRK_NEG_HARD_PILOT,           QUIRK_NEG_BAD_REP_IS,       QUIRK_NEG_BAD_REP_CLAN,
                    QUIRK_NEG_DIFFICULT_MAINTAIN,   QUIRK_NEG_PROTOTYPE,        QUIRK_NEG_POOR_TARG_S,
                    QUIRK_NEG_POOR_TARG_M,          QUIRK_NEG_POOR_TARG_L,      QUIRK_NEG_POOR_WORK,
                    QUIRK_NEG_SENSOR_GHOSTS,        QUIRK_POS_EASY_MAINTAIN,    QUIRK_POS_IMP_COM,
                    QUIRK_POS_EASY_PILOT,           QUIRK_POS_FAST_RELOAD,      QUIRK_NEG_EM_INTERFERENCE_WHOLE,
                    QUIRK_NEG_NON_STANDARD);
        }

        if (en instanceof Aero) {
            if (quirk.isAnyOf(
                    QUIRK_NEG_HARD_PILOT,           QUIRK_NEG_BAD_REP_IS,       QUIRK_NEG_BAD_REP_CLAN,
                    QUIRK_NEG_DIFFICULT_MAINTAIN,   QUIRK_NEG_NON_STANDARD,     QUIRK_NEG_POOR_TARG_S,
                    QUIRK_NEG_POOR_TARG_M,          QUIRK_NEG_POOR_TARG_L,      QUIRK_NEG_POOR_WORK,
                    QUIRK_NEG_SENSOR_GHOSTS,        QUIRK_POS_EASY_MAINTAIN,    QUIRK_POS_IMP_COM,
                    QUIRK_POS_EASY_PILOT,           QUIRK_POS_IMP_TARG_S,       QUIRK_POS_IMP_TARG_M,
                    QUIRK_NEG_FRAGILE_FUEL,         QUIRK_NEG_PROTOTYPE,        QUIRK_POS_IMP_TARG_L)) {
                return true;
            }

            if (en instanceof Warship) {
                return quirk.is(QUIRK_NEG_POOR_PERFORMANCE);
            } else if (en instanceof Jumpship) {
                return quirk.is(QUIRK_POS_DOCKING_ARMS);
            } else if (en instanceof Dropship) {
                return quirk.isAnyOf(
                        QUIRK_NEG_ATMO_INSTABILITY,     QUIRK_NEG_EM_INTERFERENCE_WHOLE,
                        QUIRK_NEG_LARGE_DROPPER,        QUIRK_NEG_UNSTREAMLINED,    QUIRK_NEG_WEAK_UNDERCARRIAGE,
                        QUIRK_POS_ATMO_FLYER,           QUIRK_POS_INTERNAL_BOMB,    QUIRK_NEG_POOR_PERFORMANCE);
            } else { // Fighter/SmallCraft
                return quirk.isAnyOf(
                        QUIRK_NEG_ATMO_INSTABILITY,     QUIRK_NEG_CRAMPED_COCKPIT,  QUIRK_NEG_DIFFICULT_EJECT,
                        QUIRK_NEG_EM_INTERFERENCE_WHOLE,                            QUIRK_NEG_POOR_LIFE_SUPPORT,
                        QUIRK_NEG_POOR_PERFORMANCE,     QUIRK_NEG_UNSTREAMLINED,    QUIRK_NEG_WEAK_UNDERCARRIAGE,
                        QUIRK_POS_ATMO_FLYER,           QUIRK_POS_COMBAT_COMPUTER,  QUIRK_POS_FAST_RELOAD,
                        QUIRK_POS_IMP_LIFE_SUPPORT,     QUIRK_POS_INTERNAL_BOMB,    QUIRK_NEG_NO_EJECT);
            }
        }

        if (en instanceof Protomech) {
            return quirk.isAnyOf(
                    QUIRK_NEG_HARD_PILOT,           QUIRK_NEG_BAD_REP_IS,           QUIRK_NEG_BAD_REP_CLAN,
                    QUIRK_NEG_DIFFICULT_MAINTAIN,   QUIRK_NEG_NON_STANDARD,         QUIRK_NEG_POOR_TARG_S,
                    QUIRK_NEG_POOR_TARG_M,          QUIRK_NEG_POOR_TARG_L,          QUIRK_NEG_POOR_WORK,
                    QUIRK_NEG_SENSOR_GHOSTS,        QUIRK_POS_EASY_MAINTAIN,        QUIRK_POS_IMP_COM,
                    QUIRK_POS_ANIMALISTIC,          QUIRK_POS_DISTRACTING,          QUIRK_POS_EXT_TWIST,
                    QUIRK_POS_IMPROVED_SENSORS,     QUIRK_POS_IMP_TARG_S,           QUIRK_POS_IMP_TARG_M,
                    QUIRK_POS_VAR_RNG_TARG_L,       QUIRK_NEG_NO_TWIST,             QUIRK_NEG_OBSOLETE,
                    QUIRK_NEG_POOR_PERFORMANCE,     QUIRK_NEG_PROTOTYPE,            QUIRK_POS_IMP_TARG_L,
                    QUIRK_NEG_POOR_SEALING,         QUIRK_POS_PRO_ACTUATOR);
        }

        return false;
    }

    private static class QuirksInfo extends AbstractOptionsInfo {
        private static boolean initialized = false;
        private static final AbstractOptionsInfo instance = new QuirksInfo();

        public static AbstractOptionsInfo getInstance() {
            if (!initialized) {
                initialized = true;
                // Create a new dummy Quirks; ensures values initialized
                // Otherwise, could have issues when loading saved games
                new Quirks();
            }
            return instance;
        }

        protected QuirksInfo() {
            super("QuirksInfo");
        }
    }
}