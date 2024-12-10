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

import java.io.Serial;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static megamek.common.options.OptionsConstants.*;

/**
 * Contains the options determining Unit Quirks of a unit (but not weapon quirks). When changing this, note
 * that all options should remain boolean options.
 *
 * @author Taharqa (Jay Lawson)
 */
public class Quirks extends AbstractOptions {
    @Serial
    private static final long serialVersionUID = 7618380522964885740L;
    public static final String POS_QUIRKS = "PosQuirks";
    public static final String NEG_QUIRKS = "NegQuirks";

    //not yet implemented
    //Docking Arms (docking unimplemented)
    //Fast Reload (no game effect at present)
    //Improved Communications
    //Variable Range Targeting
    //VTOL Rotor Arrangement (no vee adv move rules)
    //Compact Mek

    //quirks not implemented yet
    //Exposed Weapon Linkage (weapon-specific, sort of)
    //Gas Hog
    //Large Dropship (no docking)
    //Un-streamlined
    //Weak Head Armor
    //Weak Undercarriage (no landing)
    //Ramshackle
    public enum QuirkCategory {
        POSITIVE, NEGATIVE
    }

    public enum Quirk {
        // Positive quirks
        QUIRK_POS_ANIMALISTIC("animalistic", QuirkCategory.POSITIVE),
        QUIRK_POS_ANTI_AIR("anti_air", QuirkCategory.POSITIVE),
        QUIRK_POS_ATMO_FLYER("atmo_flyer", QuirkCategory.POSITIVE),
        QUIRK_POS_BATTLE_COMP("battle_computer", QuirkCategory.POSITIVE),
        QUIRK_POS_BARREL_FIST_LA("barrel_fists_la", QuirkCategory.POSITIVE),
        QUIRK_POS_BARREL_FIST_RA("barrel_fists_ra", QuirkCategory.POSITIVE),
        QUIRK_POS_BATTLE_FIST_LA("battle_fists_la", QuirkCategory.POSITIVE),
        QUIRK_POS_BATTLE_FIST_RA("battle_fists_ra", QuirkCategory.POSITIVE),
        QUIRK_POS_COMBAT_COMPUTER("combat_computer", QuirkCategory.POSITIVE),
        QUIRK_POS_COMMAND_MEK("command_mech", QuirkCategory.POSITIVE),
        QUIRK_POS_COMPACT("compact_mech", QuirkCategory.POSITIVE),
        QUIRK_POS_COWL("cowl", QuirkCategory.POSITIVE),
        QUIRK_POS_DIRECTIONAL_TORSO_MOUNT("directional_torso_mount", QuirkCategory.POSITIVE),
        QUIRK_POS_DISTRACTING("distracting", QuirkCategory.POSITIVE),
        QUIRK_POS_DOCKING_ARMS("docking_arms", QuirkCategory.POSITIVE),
        QUIRK_POS_EASY_MAINTAIN("easy_maintain", QuirkCategory.POSITIVE),
        QUIRK_POS_EASY_PILOT("easy_pilot", QuirkCategory.POSITIVE),
        QUIRK_POS_EXT_TWIST("ext_twist", QuirkCategory.POSITIVE),
        QUIRK_POS_FAST_RELOAD("fast_reload", QuirkCategory.POSITIVE),
        QUIRK_POS_FINE_MANIPULATORS("fine_manipulators", QuirkCategory.POSITIVE),
        QUIRK_POS_GOOD_REP_1("good_rep_1", QuirkCategory.POSITIVE),
        QUIRK_POS_GOOD_REP_2("good_rep_2", QuirkCategory.POSITIVE),
        QUIRK_POS_HYPER_ACTUATOR("hyper_actuator", QuirkCategory.POSITIVE),
        QUIRK_POS_IMP_COM("imp_com", QuirkCategory.POSITIVE),
        QUIRK_POS_IMP_LIFE_SUPPORT("imp_life_support", QuirkCategory.POSITIVE),
        QUIRK_POS_IMP_TARG_L("imp_target_long", QuirkCategory.POSITIVE),
        QUIRK_POS_IMP_TARG_M("imp_target_med", QuirkCategory.POSITIVE),
        QUIRK_POS_IMP_TARG_S("imp_target_short", QuirkCategory.POSITIVE),
        QUIRK_POS_IMPROVED_SENSORS("imp_sensors", QuirkCategory.POSITIVE),
        QUIRK_POS_INTERNAL_BOMB("internal_bomb", QuirkCategory.POSITIVE),
        QUIRK_POS_LOW_PROFILE("low_profile", QuirkCategory.POSITIVE),
        QUIRK_POS_MULTI_TRAC("multi_trac", QuirkCategory.POSITIVE),
        QUIRK_POS_NIMBLE_JUMPER("nimble_jumper", QuirkCategory.POSITIVE),
        QUIRK_POS_OVERHEAD_ARMS("overhead_arms", QuirkCategory.POSITIVE),
        QUIRK_POS_POWER_REVERSE("power_reverse", QuirkCategory.POSITIVE),
        QUIRK_POS_PRO_ACTUATOR("pro_actuator", QuirkCategory.POSITIVE),
        QUIRK_POS_REINFORCED_LEGS("reinforced_legs", QuirkCategory.POSITIVE),
        QUIRK_POS_RUGGED_1("rugged_1", QuirkCategory.POSITIVE),
        QUIRK_POS_RUGGED_2("rugged_2", QuirkCategory.POSITIVE),
        QUIRK_POS_RUMBLE_SEAT("rumble_seat", QuirkCategory.POSITIVE),
        QUIRK_POS_SCOUT_BIKE("scout_bike", QuirkCategory.POSITIVE),
        QUIRK_POS_SEARCHLIGHT("searchlight", QuirkCategory.POSITIVE),
        QUIRK_POS_STABLE("stable", QuirkCategory.POSITIVE),
        QUIRK_POS_TRAILER_HITCH("trailer_hitch", QuirkCategory.POSITIVE),
        QUIRK_POS_UBIQUITOUS_IS("ubiquitous_is", QuirkCategory.POSITIVE),
        QUIRK_POS_UBIQUITOUS_CLAN("ubiquitous_clan", QuirkCategory.POSITIVE),
        QUIRK_POS_VAR_RNG_TARG_L("variable_range_long", QuirkCategory.POSITIVE),
        QUIRK_POS_VAR_RNG_TARG_S("variable_range_short", QuirkCategory.POSITIVE),
        QUIRK_POS_VESTIGIAL_HANDS_LA("vestigial_hands_la", QuirkCategory.POSITIVE),
        QUIRK_POS_VESTIGIAL_HANDS_RA("vestigial_hands_ra", QuirkCategory.POSITIVE),
        QUIRK_POS_VTOL_ROTOR_COAXIAL("vtol_rotor_coaxial", QuirkCategory.POSITIVE),
        QUIRK_POS_VTOL_ROTOR_DUAL("vtol_rotor_dual", QuirkCategory.POSITIVE),

        // Negative quirks
        QUIRK_NEG_ATMO_INSTABILITY("atmo_instability", QuirkCategory.NEGATIVE),
        QUIRK_NEG_BAD_REP_IS("bad_rep_is", QuirkCategory.NEGATIVE),
        QUIRK_NEG_BAD_REP_CLAN("bad_rep_clan", QuirkCategory.NEGATIVE),
        QUIRK_NEG_CRAMPED_COCKPIT("cramped_cockpit", QuirkCategory.NEGATIVE),
        QUIRK_NEG_DIFFICULT_EJECT("difficult_eject", QuirkCategory.NEGATIVE),
        QUIRK_NEG_DIFFICULT_MAINTAIN("difficult_maintain", QuirkCategory.NEGATIVE),
        QUIRK_NEG_EM_INTERFERENCE_WHOLE("em_inter_whole", QuirkCategory.NEGATIVE),
        QUIRK_NEG_EXP_ACTUATOR("exp_actuator", QuirkCategory.NEGATIVE),
        QUIRK_NEG_FLAWED_COOLING("flawed_cooling", QuirkCategory.NEGATIVE),
        QUIRK_NEG_FRAGILE_FUEL("fragile_fuel", QuirkCategory.NEGATIVE),
        QUIRK_NEG_GAS_HOG("gas_hog", QuirkCategory.NEGATIVE),
        QUIRK_NEG_HARD_PILOT("hard_pilot", QuirkCategory.NEGATIVE),
        QUIRK_NEG_ILLEGAL_DESIGN("illegal_design", QuirkCategory.NEGATIVE),
        QUIRK_NEG_LARGE_DROPPER("large_dropper", QuirkCategory.NEGATIVE),
        QUIRK_NEG_LOW_ARMS("low_arms", QuirkCategory.NEGATIVE),
        QUIRK_NEG_NO_ARMS("no_arms", QuirkCategory.NEGATIVE),
        QUIRK_NEG_NO_EJECT("no_eject", QuirkCategory.NEGATIVE),
        QUIRK_NEG_NO_TWIST("no_twist", QuirkCategory.NEGATIVE),
        QUIRK_NEG_NON_STANDARD("non_standard", QuirkCategory.NEGATIVE),
        QUIRK_NEG_OBSOLETE("obsolete", QuirkCategory.NEGATIVE),
        QUIRK_NEG_OVERSIZED("oversized", QuirkCategory.NEGATIVE),
        QUIRK_NEG_POOR_LIFE_SUPPORT("poor_life_support", QuirkCategory.NEGATIVE),
        QUIRK_NEG_POOR_PERFORMANCE("poor_performance", QuirkCategory.NEGATIVE),
        QUIRK_NEG_POOR_SEALING("poor_sealing", QuirkCategory.NEGATIVE),
        QUIRK_NEG_POOR_TARG_L("poor_target_long", QuirkCategory.NEGATIVE),
        QUIRK_NEG_POOR_TARG_M("poor_target_med", QuirkCategory.NEGATIVE),
        QUIRK_NEG_POOR_TARG_S("poor_target_short", QuirkCategory.NEGATIVE),
        QUIRK_NEG_POOR_WORK("poor_work", QuirkCategory.NEGATIVE),
        QUIRK_NEG_PROTOTYPE("prototype", QuirkCategory.NEGATIVE),
        QUIRK_NEG_RAMSHACKLE("ramshackle", QuirkCategory.NEGATIVE),
        QUIRK_NEG_SENSOR_GHOSTS("sensor_ghosts", QuirkCategory.NEGATIVE),
        QUIRK_NEG_SUSCEPTIBLE_CWS("susceptible_cws", QuirkCategory.NEGATIVE),
        QUIRK_NEG_UNBALANCED("unbalanced", QuirkCategory.NEGATIVE),
        QUIRK_NEG_UNSTREAMLINED("unstreamlined", QuirkCategory.NEGATIVE),
        QUIRK_NEG_WEAK_HEAD_1("weak_head_1", QuirkCategory.NEGATIVE),
        QUIRK_NEG_WEAK_HEAD_2("weak_head_2", QuirkCategory.NEGATIVE),
        QUIRK_NEG_WEAK_HEAD_3("weak_head_3", QuirkCategory.NEGATIVE),
        QUIRK_NEG_WEAK_HEAD_4("weak_head_4", QuirkCategory.NEGATIVE),
        QUIRK_NEG_WEAK_HEAD_5("weak_head_5", QuirkCategory.NEGATIVE),
        QUIRK_NEG_WEAK_LEGS("weak_legs", QuirkCategory.NEGATIVE),
        QUIRK_NEG_WEAK_UNDERCARRIAGE("weak_undercarriage", QuirkCategory.NEGATIVE);

        private final QuirkCategory category;
        private final String identifier;

        Quirk(String identifier, QuirkCategory category) {
            this.identifier = identifier;
            this.category = category;
        }

        public QuirkCategory getCategory() {
            return category;
        }

        public String getIdentifier() {
            return identifier;
        }

        public static Quirk fromIdentifier(String id) {
            for (Quirk q : values()) {
                if (q.identifier.equals(id)) {
                    return q;
                }
            }
            throw new IllegalArgumentException("Unknown quirk identifier: " + id);
        }
    }

    @Override
    public synchronized void initialize() {
        var optionInfo = getOptionsInfoImp();
        IBasicOptionGroup posQuirk = addGroup("pos_quirks", POS_QUIRKS);
        IBasicOptionGroup negQuirk = addGroup("neg_quirks", NEG_QUIRKS);

        for (Quirk q : Quirk.values()) {
            if (q.getCategory() == QuirkCategory.POSITIVE) {
                addOption(optionInfo, posQuirk, q.getIdentifier());
            } else {
                addOption(optionInfo, negQuirk, q.getIdentifier());
            }
        }
    }

    private void addOption(AbstractOptionsInfo quirksInfo, IBasicOptionGroup group, String name) {
        optionsHash.put(name, new Option(this, name, IOption.BOOLEAN, false));
        quirksInfo.addOptionInfo(group, name);
    }

    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return QuirksInfo.getInstance();
    }

    /** @return A list of unit quirks that are active in this Quirks object. */
    public List<IOption> activeQuirks() {
        return getOptionsList().stream().filter(IOption::booleanValue).collect(toList());
    }

    public static boolean isQuirkIllegalFor(IOption quirk, Entity en) {
        String qName = quirk.getName();

        if (qName.equals(QUIRK_NEG_GAS_HOG)) {
            return !en.hasEngine() ||
                ((en.getEngine().getEngineType() != Engine.COMBUSTION_ENGINE)
                    && (en.getEngine().getEngineType() != Engine.FUEL_CELL));
        }

        if (en instanceof Mek) {
            return !switch (qName) {
                case QUIRK_POS_BATTLE_FIST_LA -> en.hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM);
                case QUIRK_POS_BATTLE_FIST_RA -> en.hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM);
                case QUIRK_POS_BARREL_FIST_RA -> en.hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_RARM)
                    && !en.hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM);
                case QUIRK_POS_BARREL_FIST_LA -> en.hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_LARM)
                    && !en.hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM);
                case QUIRK_NEG_OVERSIZED -> en.getWeight() >= 60;
                case QUIRK_POS_COMPACT -> en.getWeight() <= 55;
                default -> quirk.isNoneOf(
                    QUIRK_POS_ATMO_FLYER, QUIRK_NEG_ATMO_INSTABILITY, QUIRK_POS_DOCKING_ARMS,
                    QUIRK_NEG_FRAGILE_FUEL, QUIRK_POS_INTERNAL_BOMB, QUIRK_POS_TRAILER_HITCH,
                    QUIRK_NEG_LARGE_DROPPER, QUIRK_NEG_WEAK_UNDERCARRIAGE, QUIRK_POS_VTOL_ROTOR_COAXIAL,
                    QUIRK_POS_VTOL_ROTOR_DUAL, QUIRK_POS_POWER_REVERSE, QUIRK_NEG_UNSTREAMLINED);
            };
        }

        if (en instanceof Tank) {
            if (en instanceof GunEmplacement) {
                // Nov 2016 - Reviewed the idea of quirks with Ray from CGL. The working
                // made sense to him. Uncertain at this time if CGL would adopt them but
                // including them since Quirks is already an option. Hammer
                return !quirk.isAnyOf(
                    QUIRK_POS_ANTI_AIR, QUIRK_POS_IMP_COM, QUIRK_POS_IMPROVED_SENSORS,
                    QUIRK_POS_IMP_TARG_S, QUIRK_POS_IMP_TARG_M, QUIRK_POS_IMP_TARG_L,
                    QUIRK_POS_LOW_PROFILE, QUIRK_NEG_POOR_TARG_M, QUIRK_NEG_EM_INTERFERENCE_WHOLE,
                    QUIRK_NEG_POOR_TARG_S, QUIRK_NEG_POOR_TARG_L, QUIRK_NEG_POOR_WORK,
                    QUIRK_NEG_SENSOR_GHOSTS);
            }

            switch (qName) {
                case QUIRK_POS_POWER_REVERSE:
                    return !en.getMovementMode().isTrackedOrWheeled() || en.isSupportVehicle();
                case QUIRK_NEG_FRAGILE_FUEL:
                    return (!en.hasEngine() || !en.getEngine().isICE());
                case QUIRK_POS_TRAILER_HITCH:
                    return en.getMovementMode().isHover() || en.getMovementMode().isVTOL();
                case QUIRK_POS_SCOUT_BIKE:
                    return (!en.getMovementMode().isHover() && !en.getMovementMode().isWheeled())
                        || (!(en.getWeight() <= 10.0));
                case QUIRK_POS_VTOL_ROTOR_COAXIAL:
                case QUIRK_POS_VTOL_ROTOR_DUAL:
                    return !(en instanceof VTOL);
                default:
                    return !quirk.isAnyOf(
                        QUIRK_POS_ANTI_AIR, QUIRK_POS_BATTLE_COMP, QUIRK_POS_EASY_MAINTAIN,
                        QUIRK_POS_FAST_RELOAD, QUIRK_POS_GOOD_REP_1, QUIRK_POS_GOOD_REP_2,
                        QUIRK_POS_IMP_COM, QUIRK_POS_IMPROVED_SENSORS,
                        QUIRK_POS_IMP_TARG_S, QUIRK_POS_SEARCHLIGHT,
                        QUIRK_POS_IMP_TARG_M, QUIRK_POS_IMP_TARG_L, QUIRK_POS_LOW_PROFILE,
                        QUIRK_NEG_BAD_REP_IS, QUIRK_NEG_BAD_REP_CLAN, QUIRK_NEG_DIFFICULT_MAINTAIN,
                        QUIRK_NEG_NON_STANDARD, QUIRK_NEG_POOR_PERFORMANCE, QUIRK_NEG_HARD_PILOT,
                        QUIRK_NEG_POOR_TARG_S, QUIRK_NEG_POOR_TARG_M, QUIRK_NEG_POOR_TARG_L,
                        QUIRK_NEG_POOR_WORK, QUIRK_NEG_PROTOTYPE, QUIRK_NEG_SENSOR_GHOSTS,
                        QUIRK_POS_UBIQUITOUS_IS, QUIRK_POS_UBIQUITOUS_CLAN);
            }
        }

        if (en instanceof BattleArmor) {
            return !quirk.isAnyOf(
                QUIRK_POS_EASY_MAINTAIN, QUIRK_POS_EASY_PILOT, QUIRK_POS_GOOD_REP_1,
                QUIRK_POS_GOOD_REP_2, QUIRK_POS_IMP_COM, QUIRK_POS_RUGGED_1,
                QUIRK_POS_RUGGED_2, QUIRK_POS_UBIQUITOUS_IS, QUIRK_POS_UBIQUITOUS_CLAN,
                QUIRK_NEG_BAD_REP_IS, QUIRK_NEG_BAD_REP_CLAN, QUIRK_NEG_DIFFICULT_MAINTAIN,
                QUIRK_NEG_HARD_PILOT, QUIRK_NEG_ILLEGAL_DESIGN, QUIRK_NEG_NON_STANDARD,
                QUIRK_NEG_OBSOLETE, QUIRK_NEG_POOR_SEALING, QUIRK_NEG_POOR_TARG_S,
                QUIRK_NEG_POOR_TARG_M, QUIRK_NEG_POOR_TARG_L, QUIRK_NEG_POOR_WORK,
                QUIRK_NEG_PROTOTYPE, QUIRK_NEG_SENSOR_GHOSTS);
        }

        if (en instanceof Aero) {
            if (quirk.isAnyOf(
                    QUIRK_POS_ATMO_FLYER, QUIRK_POS_COMBAT_COMPUTER, QUIRK_POS_EASY_MAINTAIN,
                    QUIRK_POS_EASY_PILOT, QUIRK_POS_GOOD_REP_1, QUIRK_POS_GOOD_REP_2,
                    QUIRK_POS_IMP_COM, QUIRK_POS_IMP_LIFE_SUPPORT, QUIRK_POS_IMP_TARG_L,
                    QUIRK_POS_IMP_TARG_M, QUIRK_POS_IMP_TARG_S, QUIRK_POS_INTERNAL_BOMB,
                    QUIRK_POS_RUGGED_1, QUIRK_POS_RUGGED_2, QUIRK_POS_RUMBLE_SEAT,
                    QUIRK_POS_UBIQUITOUS_IS, QUIRK_POS_UBIQUITOUS_CLAN, QUIRK_NEG_ATMO_INSTABILITY,
                    QUIRK_NEG_BAD_REP_IS, QUIRK_NEG_BAD_REP_CLAN, QUIRK_NEG_CRAMPED_COCKPIT,
                    QUIRK_NEG_DIFFICULT_EJECT, QUIRK_NEG_DIFFICULT_MAINTAIN, QUIRK_NEG_FRAGILE_FUEL,
                    QUIRK_NEG_HARD_PILOT, QUIRK_NEG_ILLEGAL_DESIGN, QUIRK_NEG_NO_EJECT,
                    QUIRK_NEG_NON_STANDARD, QUIRK_NEG_OBSOLETE, QUIRK_NEG_POOR_LIFE_SUPPORT,
                    QUIRK_NEG_POOR_PERFORMANCE, QUIRK_NEG_POOR_TARG_S, QUIRK_NEG_POOR_TARG_M,
                    QUIRK_NEG_POOR_TARG_L, QUIRK_NEG_POOR_WORK, QUIRK_NEG_PROTOTYPE,
                    QUIRK_NEG_RAMSHACKLE, QUIRK_NEG_SENSOR_GHOSTS, QUIRK_NEG_UNSTREAMLINED,
                    QUIRK_NEG_WEAK_UNDERCARRIAGE)) {
                return false;
            }

            if (en instanceof Warship) {
                return !quirk.is(QUIRK_NEG_POOR_PERFORMANCE);
            } else if (en instanceof Jumpship) {
                return !quirk.is(QUIRK_POS_DOCKING_ARMS);
            } else if (en instanceof Dropship) {
                return !quirk.isAnyOf(
                    QUIRK_NEG_ATMO_INSTABILITY, QUIRK_NEG_EM_INTERFERENCE_WHOLE,
                    QUIRK_NEG_LARGE_DROPPER, QUIRK_NEG_UNSTREAMLINED, QUIRK_NEG_WEAK_UNDERCARRIAGE,
                    QUIRK_POS_ATMO_FLYER, QUIRK_POS_INTERNAL_BOMB, QUIRK_NEG_POOR_PERFORMANCE);
            } else { // Fighter/SmallCraft
                return !quirk.isAnyOf(
                    QUIRK_NEG_ATMO_INSTABILITY, QUIRK_NEG_CRAMPED_COCKPIT, QUIRK_NEG_DIFFICULT_EJECT,
                    QUIRK_NEG_EM_INTERFERENCE_WHOLE, QUIRK_NEG_POOR_LIFE_SUPPORT,
                    QUIRK_NEG_POOR_PERFORMANCE, QUIRK_NEG_UNSTREAMLINED, QUIRK_NEG_WEAK_UNDERCARRIAGE,
                    QUIRK_POS_ATMO_FLYER, QUIRK_POS_COMBAT_COMPUTER, QUIRK_POS_FAST_RELOAD,
                    QUIRK_POS_IMP_LIFE_SUPPORT, QUIRK_POS_INTERNAL_BOMB, QUIRK_NEG_NO_EJECT);
            }
        }

        if (en instanceof ProtoMek) {
            return !quirk.isAnyOf(
                QUIRK_POS_EASY_MAINTAIN, QUIRK_POS_EASY_PILOT, QUIRK_POS_GOOD_REP_1,
                QUIRK_POS_GOOD_REP_2, QUIRK_POS_IMP_COM, QUIRK_POS_RUGGED_1,
                QUIRK_POS_RUGGED_2, QUIRK_POS_UBIQUITOUS_IS, QUIRK_POS_UBIQUITOUS_CLAN,
                QUIRK_NEG_BAD_REP_IS, QUIRK_NEG_BAD_REP_CLAN, QUIRK_NEG_DIFFICULT_MAINTAIN,
                QUIRK_NEG_HARD_PILOT, QUIRK_NEG_ILLEGAL_DESIGN, QUIRK_NEG_NON_STANDARD,
                QUIRK_NEG_OBSOLETE, QUIRK_NEG_POOR_SEALING, QUIRK_NEG_POOR_TARG_S,
                QUIRK_NEG_POOR_TARG_M, QUIRK_NEG_POOR_TARG_L, QUIRK_NEG_POOR_WORK,
                QUIRK_NEG_PROTOTYPE, QUIRK_NEG_SENSOR_GHOSTS);
        }

        return true;
    }

    @Override
    public Map<String, IOption> getOptionsHash() {
        return Map.of();
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
