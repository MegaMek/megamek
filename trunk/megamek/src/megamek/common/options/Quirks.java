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
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.GunEmplacement;
import megamek.common.Jumpship;
import megamek.common.Mech;
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

    public Quirks() {
        super();
    }

    @Override
    public void initialize() {
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



        if(en instanceof Mech) {
            if(quirk.getName().equals("atmo_flyer")
                    || quirk.getName().equals("atmo_instability")
                    || quirk.getName().equals("docking_arms")
                    || quirk.getName().equals("fragile_fuel")
                    || quirk.getName().equals("internal_bomb")
                    || quirk.getName().equals("trailer_hitch")
                    || quirk.getName().equals("large_dropper")
                    || quirk.getName().equals("weak_undercarriage")) {
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
                    || quirk.getName().equals("weak_undercarriage")) {
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
                    || quirk.getName().equals("no_twist")
                    || quirk.getName().equals("large_dropper")
                    || quirk.getName().equals("poor_life_support")
                    || quirk.getName().equals("unbalanced")
                    || quirk.getName().equals("weak_legs")
                    || quirk.getName().equals("weak_undercarriage")) {
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
                    || quirk.getName().equals("no_twist")
                    || quirk.getName().equals("large_dropper")
                    || quirk.getName().equals("poor_life_support")
                    || quirk.getName().equals("unbalanced")
                    || quirk.getName().equals("weak_legs")
                    || quirk.getName().equals("weak_undercarriage")) {
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
                    || quirk.getName().equals("no_twist")
                    || quirk.getName().equals("poor_life_support")
                    || quirk.getName().equals("unbalanced")
                    || quirk.getName().equals("weak_legs")) {
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
                    || quirk.getName().equals("weak_legs")) {
                return false;
            }
            return true;
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
