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


import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Dropship;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.GunEmplacement;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.Tank;

/**
 * Contains the options determining quirks of the unit
 *
 * @author Taharqa (Jay Lawson)
 */
public class Quirks extends AbstractOptions {
    private static final long serialVersionUID = 7618380522964885740L;
    public static final String POS_QUIRKS = "PosQuirks"; //$NON-NLS-1$
    public static final String NEG_QUIRKS = "NegQuirks"; //$NON-NLS-1$

    @Override
    public synchronized void initialize() {
        //positive quirks
        IBasicOptionGroup posQuirk = addGroup("pos_quirks", POS_QUIRKS); //$NON-NLS-1$
        addOption(posQuirk, "anti_air", false); //$NON-NLS-1$
        addOption(posQuirk, "atmo_flyer", false); //$NON-NLS-1$
        addOption(posQuirk, "battle_computer", false); //$NON-NLS-1$
        addOption(posQuirk, "combat_computer", false); //$NON-NLS-1$
        addOption(posQuirk, "command_mech", false); //$NON-NLS-1$
        addOption(posQuirk, "cowl", false); //$NON-NLS-1$
        addOption(posQuirk, "docking_arms", false); //$NON-NLS-1$
        addOption(posQuirk, "easy_maintain", false); //$NON-NLS-1$
        addOption(posQuirk, "easy_pilot", false); //$NON-NLS-1$
        addOption(posQuirk, "ext_twist", false); //$NON-NLS-1$
        addOption(posQuirk, "fast_reload", false); //$NON-NLS-1$
        addOption(posQuirk, "low_profile", false); //$NON-NLS-1$
        addOption(posQuirk, "hyper_actuator", false); //$NON-NLS-1$
        addOption(posQuirk, "imp_sensors", false); //$NON-NLS-1$
        addOption(posQuirk, "imp_life_support", false); //$NON-NLS-1$
        addOption(posQuirk, "imp_target_short", false); //$NON-NLS-1$
        addOption(posQuirk, "imp_target_med", false); //$NON-NLS-1$
        addOption(posQuirk, "imp_target_long", false); //$NON-NLS-1$
        addOption(posQuirk, "internal_bomb", false); //$NON-NLS-1$
        addOption(posQuirk, "mod_weapons", false); //$NON-NLS-1$
        addOption(posQuirk, "multi_trac", false); //$NON-NLS-1$
        addOption(posQuirk, "pro_actuator", false); //$NON-NLS-1$
        addOption(posQuirk, "reinforced_legs", false); //$NON-NLS-1$
        addOption(posQuirk, "searchlight", false); //$NON-NLS-1$
        addOption(posQuirk, "stable", false); //$NON-NLS-1$
        addOption(posQuirk, "trailer_hitch", false); //$NON-NLS-1$
        addOption(posQuirk, "imp_com", false); //$NON-NLS-1$
        addOption(posQuirk, "variable_range_long", false); //$NON-NLS-1$
        addOption(posQuirk, "variable_range_short", false); //$NON-NLS-1$
        addOption(posQuirk, "vtol_rotor", false); //$NON-NLS-1$
        addOption(posQuirk, "rumble_seat", false);
        addOption(posQuirk, "distracting", false);
        addOption(posQuirk, "head_eject", false);
        //not yet implemented
        //Docking Arms (docking unimplemented)
        //Fast Reload (no game effect at present)
        //Improved Communications
        //Internal Bomb Bay
        //Searchlight
        //Variable Range Targeting
        //VTOL Rotor Arrangement (no vee adv move rules)

        // negative quirks
        IBasicOptionGroup negQuirk = addGroup("neg_quirks", NEG_QUIRKS); //$NON-NLS-1$
        addOption(negQuirk, "atmo_instability", false); //$NON-NLS-1$
        addOption(negQuirk, "bad_rep", false); //$NON-NLS-1$
        addOption(negQuirk, "cramped_cockpit", false); //$NON-NLS-1$
        addOption(negQuirk, "difficult_eject", false); //$NON-NLS-1$
        addOption(negQuirk, "difficult_maintain", false); //$NON-NLS-1$
        addOption(negQuirk, "exp_actuator", false); //$NON-NLS-1$
        addOption(negQuirk, "fragile_fuel", false); //$NON-NLS-1$
        addOption(negQuirk, "hard_pilot", false); //$NON-NLS-1$
        addOption(negQuirk, "no_arms", false); //$NON-NLS-1$
        addOption(negQuirk, "no_eject", false); //$NON-NLS-1$
        addOption(negQuirk, "no_twist", false); //$NON-NLS-1$
        addOption(negQuirk, "non_standard", false); //$NON-NLS-1$
        addOption(negQuirk, "large_dropper", false); //$NON-NLS-1$
        addOption(negQuirk, "poor_life_support", false); //$NON-NLS-1$
        addOption(negQuirk, "poor_target_short", false); //$NON-NLS-1$
        addOption(negQuirk, "poor_target_med", false); //$NON-NLS-1$
        addOption(negQuirk, "poor_target_long", false); //$NON-NLS-1$
        addOption(negQuirk, "poor_work", false); //$NON-NLS-1$
        addOption(negQuirk, "prototype", false); //$NON-NLS-1$
        addOption(negQuirk, "sensor_ghosts", false); //$NON-NLS-1$
        addOption(negQuirk, "unbalanced", false); //$NON-NLS-1$
        addOption(negQuirk, "weak_legs", false); //$NON-NLS-1$
        addOption(negQuirk, "weak_undercarriage", false); //$NON-NLS-1$
        addOption(negQuirk, "flawed_cooling", false); //$NON-NLS-1$
        addOption(negQuirk, "gas_hog", false); //$NON-NLS-1$
        addOption(negQuirk, "poor_performance", false); //$NON-NLS-1$
        addOption(negQuirk, "unstreamlined", false); //$NON-NLS-1$
        addOption(negQuirk, "weak_head_1", false); //$NON-NLS-1$
        addOption(negQuirk, "weak_head_2", false); //$NON-NLS-1$
        addOption(negQuirk, "weak_head_3", false); //$NON-NLS-1$
        addOption(negQuirk, "weak_head_4", false); //$NON-NLS-1$
        addOption(negQuirk, "weak_head_5", false); //$NON-NLS-1$
        addOption(negQuirk, "obsolete", false); //$NON-NLS-1$
        addOption(negQuirk, "poor_sealing", false); //$NON-NLS-1$
        //quirks not implemented yet
        //Cooling System Flaws
        //Exposed Weapon Linkage (weapon-specific, sort of)
        //Gas Hog
        //Large Dropship (no docking)
        //Poor Performance
        //Un-streamlined
        //Weak Head Armor
        //Weak Undercarriage (no landing)
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.options.AbstractOptions#getOptionsInfoImp()
     */
    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return QuirksInfo.getInstance();
    }

    public static boolean isQuirkLegalFor(IOption quirk, Entity en) {

        if ((en.getEngine() != null) &&
                ((en.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE) ||
                (en.getEngine().getEngineType() == Engine.FUEL_CELL)) &&
                quirk.getName().equals("gas_hog")) {
            return true;
        }

        if(en instanceof Mech) {
            if(quirk.getName().equals("atmo_flyer")
                    || quirk.getName().equals("atmo_instability")
                    || quirk.getName().equals("docking_arms")
                    || quirk.getName().equals("fragile_fuel")
                    || quirk.getName().equals("internal_bomb")
                    || quirk.getName().equals("trailer_hitch")
                    || quirk.getName().equals("large_dropper")
                    || quirk.getName().equals("weak_undercarriage")
                    || quirk.getName().equals("vtol_rotor")
                    || quirk.getName().equals("gas_hog")
                    || quirk.getName().equals("unstreamlined")) {
                return false;
            }
            return true;
        }

        if(en instanceof GunEmplacement) {
            return false;
        }

        if(en instanceof Tank) {
            if(quirk.getName().equals("atmo_flyer")
                    || quirk.getName().equals("combat_computer")
                    || quirk.getName().equals("command_mech")
                    || quirk.getName().equals("cowl")
                    || quirk.getName().equals("docking_arms")
                    || quirk.getName().equals("easy_pilot")
                    || quirk.getName().equals("ext_twist")
                    || quirk.getName().equals("hyper_actuator")
                    || quirk.getName().equals("imp_life_support")
                    || quirk.getName().equals("internal_bomb")
                    || quirk.getName().equals("multi_trac")
                    || quirk.getName().equals("pro_actuator")
                    || quirk.getName().equals("reinforced_legs")
                    || quirk.getName().equals("stable")
                    || quirk.getName().equals("atmo_instability")
                    || quirk.getName().equals("cramped_cockpit")
                    || quirk.getName().equals("difficult_eject")
                    || quirk.getName().equals("exp_actuator")
                    || quirk.getName().equals("no_arms")
                    || quirk.getName().equals("no_eject")
                    || quirk.getName().equals("no_twist")
                    || quirk.getName().equals("large_dropper")
                    || quirk.getName().equals("poor_life_support")
                    || quirk.getName().equals("unbalanced")
                    || quirk.getName().equals("weak_legs")
                    || quirk.getName().equals("weak_undercarriage")
                    || quirk.getName().equals("vtol_rotor")
                    || quirk.getName().equals("flawed_cooling")
                    || quirk.getName().equals("unstreamlined")
                    || quirk.getName().equals("gas_hog")
                    || quirk.getName().equals("head_eject")
                    || quirk.getName().equals("weak_head_1")
                    || quirk.getName().equals("weak_head_2")
                    || quirk.getName().equals("weak_head_3")
                    || quirk.getName().equals("weak_head_4")
                    || quirk.getName().equals("weak_head_5")) {
                return false;
            }
            if(!en.getEngine().isFusion() && quirk.getName().equals("fragile_fuel")) {
                return false;
            }
            if(quirk.getName().equals("trailer_hitch")
                    && (en.getMovementMode() == EntityMovementMode.HOVER)) {
                return false;
            }
            return true;
        }

        if(en instanceof BattleArmor) {
            if(quirk.getName().equals("atmo_flyer")
                    || quirk.getName().equals("anti_air")
                    || quirk.getName().equals("battle_computer")
                    || quirk.getName().equals("combat_computer")
                    || quirk.getName().equals("command_mech")
                    || quirk.getName().equals("cowl")
                    || quirk.getName().equals("docking_arms")
                    || quirk.getName().equals("ext_twist")
                    || quirk.getName().equals("hyper_actuator")
                    || quirk.getName().equals("imp_sensors")
                    || quirk.getName().equals("imp_life_support")
                    || quirk.getName().equals("imp_target_short")
                    || quirk.getName().equals("imp_target_med")
                    || quirk.getName().equals("imp_target_long")
                    || quirk.getName().equals("internal_bomb")
                    || quirk.getName().equals("multi_trac")
                    || quirk.getName().equals("pro_actuator")
                    || quirk.getName().equals("low_profile")
                    || quirk.getName().equals("reinforced_legs")
                    || quirk.getName().equals("stable")
                    || quirk.getName().equals("searchlight")
                    || quirk.getName().equals("trailer_hitch")
                    || quirk.getName().equals("atmo_instability")
                    || quirk.getName().equals("cramped_cockpit")
                    || quirk.getName().equals("difficult_eject")
                    || quirk.getName().equals("exp_actuator")
                    || quirk.getName().equals("fragile_fuel")
                    || quirk.getName().equals("no_arms")
                    || quirk.getName().equals("no_eject")
                    || quirk.getName().equals("head_eject")
                    || quirk.getName().equals("no_twist")
                    || quirk.getName().equals("large_dropper")
                    || quirk.getName().equals("poor_life_support")
                    || quirk.getName().equals("unbalanced")
                    || quirk.getName().equals("weak_legs")
                    || quirk.getName().equals("weak_undercarriage")
                    || quirk.getName().equals("vtol_rotor")
                    || quirk.getName().equals("flawed_cooling")
                    || quirk.getName().equals("unstreamlined")
                    || quirk.getName().equals("weak_head_1")
                    || quirk.getName().equals("weak_head_2")
                    || quirk.getName().equals("weak_head_3")
                    || quirk.getName().equals("weak_head_4")
                    || quirk.getName().equals("weak_head_5")
                    || quirk.getName().equals("gas_hog")
                    || quirk.getName().equals("rumble_seat")
                    || quirk.getName().equals("poor_performance")
                    || quirk.getName().equals("distracting")) {
                return false;
            }
            return true;
        }

        if(en instanceof Jumpship) {
            if(quirk.getName().equals("atmo_flyer")
                    || quirk.getName().equals("anti_air")
                    || quirk.getName().equals("battle_computer")
                    || quirk.getName().equals("combat_computer")
                    || quirk.getName().equals("command_mech")
                    || quirk.getName().equals("cowl")
                    || quirk.getName().equals("docking_arms")
                    || quirk.getName().equals("ext_twist")
                    || quirk.getName().equals("fast_reload")
                    || quirk.getName().equals("hyper_actuator")
                    || quirk.getName().equals("imp_sensors")
                    || quirk.getName().equals("imp_life_support")
                    || quirk.getName().equals("internal_bomb")
                    || quirk.getName().equals("mod_weapons")
                    || quirk.getName().equals("multi_trac")
                    || quirk.getName().equals("pro_actuator")
                    || quirk.getName().equals("low_profile")
                    || quirk.getName().equals("reinforced_legs")
                    || quirk.getName().equals("stable")
                    || quirk.getName().equals("trailer_hitch")
                    || quirk.getName().equals("searchlight")
                    || quirk.getName().equals("atmo_instability")
                    || quirk.getName().equals("cramped_cockpit")
                    || quirk.getName().equals("difficult_eject")
                    || quirk.getName().equals("exp_actuator")
                    || quirk.getName().equals("no_arms")
                    || quirk.getName().equals("no_eject")
                    || quirk.getName().equals("head_eject")
                    || quirk.getName().equals("no_twist")
                    || quirk.getName().equals("large_dropper")
                    || quirk.getName().equals("poor_life_support")
                    || quirk.getName().equals("unbalanced")
                    || quirk.getName().equals("weak_legs")
                    || quirk.getName().equals("weak_undercarriage")
                    || quirk.getName().equals("vtol_rotor")
                    || quirk.getName().equals("flawed_cooling")
                    || quirk.getName().equals("unstreamlined")
                    || quirk.getName().equals("weak_head_1")
                    || quirk.getName().equals("weak_head_2")
                    || quirk.getName().equals("weak_head_3")
                    || quirk.getName().equals("weak_head_4")
                    || quirk.getName().equals("weak_head_5")
                    || quirk.getName().equals("gas_hog")
                    || quirk.getName().equals("rumble_seat")
                    || quirk.getName().equals("poor_performance")
                    || quirk.getName().equals("distracting")
                    || quirk.getName().equals("poor_sealing")) {
                return false;
            }
            return true;
        } else if (en instanceof Dropship) {
            if(quirk.getName().equals("anti_air")
                    || quirk.getName().equals("battle_computer")
                    || quirk.getName().equals("combat_computer")
                    || quirk.getName().equals("command_mech")
                    || quirk.getName().equals("cowl")
                    || quirk.getName().equals("ext_twist")
                    || quirk.getName().equals("fast_reload")
                    || quirk.getName().equals("hyper_actuator")
                    || quirk.getName().equals("imp_sensors")
                    || quirk.getName().equals("imp_life_support")
                    || quirk.getName().equals("multi_trac")
                    || quirk.getName().equals("pro_actuator")
                    || quirk.getName().equals("low_profile")
                    || quirk.getName().equals("reinforced_legs")
                    || quirk.getName().equals("stable")
                    || quirk.getName().equals("searchlight")
                    || quirk.getName().equals("trailer_hitch")
                    || quirk.getName().equals("cramped_cockpit")
                    || quirk.getName().equals("difficult_eject")
                    || quirk.getName().equals("exp_actuator")
                    || quirk.getName().equals("no_arms")
                    || quirk.getName().equals("no_eject")
                    || quirk.getName().equals("head_eject")
                    || quirk.getName().equals("no_twist")
                    || quirk.getName().equals("poor_life_support")
                    || quirk.getName().equals("unbalanced")
                    || quirk.getName().equals("weak_undercarriage")
                    || quirk.getName().equals("weak_legs")
                    || quirk.getName().equals("vtol_rotor")
                    || quirk.getName().equals("flawed_cooling")
                    || quirk.getName().equals("rumble_seat")
                    || quirk.getName().equals("weak_head_1")
                    || quirk.getName().equals("weak_head_2")
                    || quirk.getName().equals("weak_head_3")
                    || quirk.getName().equals("weak_head_4")
                    || quirk.getName().equals("weak_head_5")
                    || quirk.getName().equals("poor_sealing")) {
                return false;
            }
            return true;
        } else if (en instanceof Aero) {
            if(quirk.getName().equals("anti_air")
                    || quirk.getName().equals("battle_computer")
                    || quirk.getName().equals("command_mech")
                    || quirk.getName().equals("cowl")
                    || quirk.getName().equals("docking_arms")
                    || quirk.getName().equals("ext_twist")
                    || quirk.getName().equals("hyper_actuator")
                    || quirk.getName().equals("imp_sensors")
                    || quirk.getName().equals("imp_life_support")
                    || quirk.getName().equals("multi_trac")
                    || quirk.getName().equals("pro_actuator")
                    || quirk.getName().equals("low_profile")
                    || quirk.getName().equals("stable")
                    || quirk.getName().equals("searchlight")
                    || quirk.getName().equals("trailer_hitch")
                    || quirk.getName().equals("reinforced_legs")
                    || quirk.getName().equals("exp_actuator")
                    || quirk.getName().equals("no_arms")
                    || quirk.getName().equals("no_twist")
                    || quirk.getName().equals("large_dropper")
                    || quirk.getName().equals("unbalanced")
                    || quirk.getName().equals("weak_legs")
                    || quirk.getName().equals("head_eject")
                    || quirk.getName().equals("vtol_rotor")
                    || quirk.getName().equals("weak_head_1")
                    || quirk.getName().equals("weak_head_2")
                    || quirk.getName().equals("weak_head_3")
                    || quirk.getName().equals("weak_head_4")
                    || quirk.getName().equals("weak_head_5")
                    || quirk.getName().equals("poor_sealing")) {
                return false;
            }
            return true;
        } else if (en instanceof Protomech) {
            if (quirk.getName().equals("accurate")
                    || quirk.getName().equals("distracting")
                    || quirk.getName().equals("easy_maintain")
                    || quirk.getName().equals("ext_twist")
                    || quirk.getName().equals("imp_com")
                    || quirk.getName().equals("imp_sensors")
                    || quirk.getName().equals("imp_target_short")
                    || quirk.getName().equals("imp_target_med")
                    || quirk.getName().equals("imp_target_long")
                    || quirk.getName().equals("variable_range_long")
                    || quirk.getName().equals("ammo_feed")
                    || quirk.getName().equals("bad_rep")
                    || quirk.getName().equals("difficult_maintain")
                    || quirk.getName().equals("exposed_linkage")
                    || quirk.getName().equals("hard_pilot")
                    || quirk.getName().equals("inaccurate")
                    || quirk.getName().equals("no_twist")
                    || quirk.getName().equals("head_eject")
                    || quirk.getName().equals("obsolete")
                    || quirk.getName().equals("poor_performance")
                    || quirk.getName().equals("poor_sealing")
                    || quirk.getName().equals("poor_target_short")
                    || quirk.getName().equals("poor_target_med")
                    || quirk.getName().equals("poor_target_long")
                    || quirk.getName().equals("poor_work")
                    || quirk.getName().equals("prototype")
                    || quirk.getName().equals("sensor_ghosts")) {
                return true;
            }
            return false;
        }


        return false;

    }

    private static class QuirksInfo extends AbstractOptionsInfo {
        private static AbstractOptionsInfo instance = new QuirksInfo();

        public static AbstractOptionsInfo getInstance() {
            return instance;
        }

        protected QuirksInfo() {
            super("QuirksInfo"); //$NON-NLS-1$
        }
    }
}
