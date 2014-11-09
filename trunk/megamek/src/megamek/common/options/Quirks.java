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
        addOption(posQuirk, OptionsConstants.QUIRK_POS_ANTI_AIR, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_ATMO_FLYER, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_BATTLE_COMP, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_BATTLE_FIST, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_COMBAT_COMPUTER, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_COMMAND_MECH, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_COMPACT, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_COWL, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_DISTRACTING, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_DOCKING_ARMS, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_EASY_MAINTAIN, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_EASY_PILOT, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_EXT_TWIST, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_FAST_RELOAD, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_HEAD_EJECT, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_HYPER_ACTUATOR, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_IMP_COM, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_IMP_LIFE_SUPPORT, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_IMP_TARG_L, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_IMP_TARG_M, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_IMP_TARG_S, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_IMPROVED_SENSORS, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_INTERNAL_BOMB, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_LOW_PROFILE, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_MOD_WEAPONS, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_MULTI_TRAC, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_OVERHEAD_ARMS, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_POWER_REVERSE, false); //Not Implemented
        addOption(posQuirk, OptionsConstants.QUIRK_POS_PRO_ACTUATOR, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_REINFORCED_LEGS, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_RUMBLE_SEAT, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_SEARCHLIGHT, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_STABLE, false); // Not Implemented
        addOption(posQuirk, OptionsConstants.QUIRK_POS_TRAILER_HITCH, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_VAR_RNG_TARG_L, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_VAR_RNG_TARG_S, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_VTOL_ROTOR, false);

        
        //not yet implemented
        //Docking Arms (docking unimplemented)
        //Fast Reload (no game effect at present)
        //Improved Communications
        //Internal Bomb Bay
        //Variable Range Targeting
        //VTOL Rotor Arrangement (no vee adv move rules)
        //Combat Mech

        // negative quirks
        IBasicOptionGroup negQuirk = addGroup("neg_quirks", NEG_QUIRKS); //$NON-NLS-1$
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_BAD_REP, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_DIFFICULT_EJECT, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_DIFFICULT_MAINTAIN, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_EXP_ACTUATOR, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_FLAWED_COOLING, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_FRAGILE_FUEL, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_GAS_HOG, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_HARD_PILOT, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_ILLEGAL_DESIGN, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_LARGE_DROPPER, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_LOW_ARMS, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_NO_ARMS, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_NO_EJECT, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_NO_TWIST, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_NON_STANDARD, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_OBSOLETE, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_POOR_LIFE_SUPPORT, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_POOR_PERFORMANCE, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_POOR_SEALING, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_POOR_TARG_L, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_POOR_TARG_M, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_POOR_TARG_S, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_POOR_WORK, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_PROTOTYPE, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_SENSOR_GHOSTS, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_SUSCEPTIBLE_CWS, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_UNBALANCED, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_UNSTREAMLINED, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_WEAK_HEAD_1, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_WEAK_HEAD_2, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_WEAK_HEAD_3, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_WEAK_HEAD_4, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_WEAK_HEAD_5, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_WEAK_LEGS, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_WEAK_UNDERCARRIAGE,false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_ATMO_INSTABILITY, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_OVERSIZED, false);
        
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
        String qName = quirk.getName();
        
        if ((en.getEngine() != null) &&
                ((en.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE) ||
                (en.getEngine().getEngineType() == Engine.FUEL_CELL)) &&
                qName.equals("gas_hog")) {
            return true;
        }

        if(en instanceof Mech) {
            if (qName.equals(OptionsConstants.QUIRK_POS_BATTLE_FIST)) {
                // Mechs with a hand actuator can have battlefists
                if (en.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM) 
                        || en.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM)) {
                    return true;
                } else {
                    return false;
                }
            }
            if(qName.equals("atmo_flyer")
                    || qName.equals("atmo_instability")
                    || qName.equals("docking_arms")
                    || qName.equals("fragile_fuel")
                    || qName.equals("internal_bomb")
                    || qName.equals("trailer_hitch")
                    || qName.equals("large_dropper")
                    || qName.equals("weak_undercarriage")
                    || qName.equals("vtol_rotor")
                    || qName.equals("gas_hog")
                    || qName.equals("power_reverse")
                    || qName.equals("unstreamlined")) {
                return false;
            }
            return true;
        }

        if(en instanceof GunEmplacement) {
            return false;
        }

        if(en instanceof Tank) {
            if(qName.equals("atmo_flyer")
                    || qName.equals("combat_computer")
                    || qName.equals(OptionsConstants.QUIRK_POS_COMMAND_MECH)
                    || qName.equals("cowl")
                    || qName.equals("docking_arms")
                    || qName.equals(OptionsConstants.QUIRK_POS_EASY_PILOT)
                    || qName.equals("ext_twist")
                    || qName.equals("hyper_actuator")
                    || qName.equals("imp_life_support")
                    || qName.equals("internal_bomb")
                    || qName.equals("multi_trac")
                    || qName.equals("pro_actuator")
                    || qName.equals("reinforced_legs")
                    || qName.equals("stable")
                    || qName.equals("atmo_instability")
                    || qName.equals("cramped_cockpit")
                    || qName.equals("difficult_eject")
                    || qName.equals("exp_actuator")
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_ARMS)
                    || qName.equals("no_eject")
                    || qName.equals("no_twist")
                    || qName.equals("large_dropper")
                    || qName.equals("poor_life_support")
                    || qName.equals("unbalanced")
                    || qName.equals("weak_legs")
                    || qName.equals("weak_undercarriage")
                    || qName.equals("vtol_rotor")
                    || qName.equals("flawed_cooling")
                    || qName.equals("unstreamlined")
                    || qName.equals("gas_hog")
                    || qName.equals("head_eject")
                    || qName.equals("weak_head_1")
                    || qName.equals("weak_head_2")
                    || qName.equals("weak_head_3")
                    || qName.equals("weak_head_4")
                    || qName.equals("weak_head_5")
                    || qName.equals("stable")
                    || qName.equals("compact_mech")
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_FIST)) {
                return false;
            }
            if(!en.getEngine().isFusion() && qName.equals("fragile_fuel")) {
                return false;
            }
            if(qName.equals("trailer_hitch")
                    && (en.getMovementMode() == EntityMovementMode.HOVER)) {
                return false;
            }
            return true;
        }

        if(en instanceof BattleArmor) {
            if(qName.equals("atmo_flyer")
                    || qName.equals(OptionsConstants.QUIRK_POS_ANTI_AIR)
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_COMP)
                    || qName.equals("combat_computer")
                    || qName.equals(OptionsConstants.QUIRK_POS_COMMAND_MECH)
                    || qName.equals("cowl")
                    || qName.equals("docking_arms")
                    || qName.equals("ext_twist")
                    || qName.equals("hyper_actuator")
                    || qName.equals(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS)
                    || qName.equals("imp_life_support")
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_TARG_S)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_TARG_M)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_TARG_L)
                    || qName.equals("internal_bomb")
                    || qName.equals("multi_trac")
                    || qName.equals("pro_actuator")
                    || qName.equals(OptionsConstants.QUIRK_POS_LOW_PROFILE)
                    || qName.equals("reinforced_legs")
                    || qName.equals("stable")
                    || qName.equals("searchlight")
                    || qName.equals("trailer_hitch")
                    || qName.equals("atmo_instability")
                    || qName.equals("cramped_cockpit")
                    || qName.equals("difficult_eject")
                    || qName.equals("exp_actuator")
                    || qName.equals("fragile_fuel")
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_ARMS)
                    || qName.equals("no_eject")
                    || qName.equals("head_eject")
                    || qName.equals("no_twist")
                    || qName.equals("large_dropper")
                    || qName.equals("poor_life_support")
                    || qName.equals("unbalanced")
                    || qName.equals("weak_legs")
                    || qName.equals("weak_undercarriage")
                    || qName.equals("vtol_rotor")
                    || qName.equals("flawed_cooling")
                    || qName.equals("unstreamlined")
                    || qName.equals("weak_head_1")
                    || qName.equals("weak_head_2")
                    || qName.equals("weak_head_3")
                    || qName.equals("weak_head_4")
                    || qName.equals("weak_head_5")
                    || qName.equals("gas_hog")
                    || qName.equals("rumble_seat")
                    || qName.equals("poor_performance")
                    || qName.equals("distracting")
                    || qName.equals("power_reverse")
                    || qName.equals("stable")
                    || qName.equals("compact_mech")
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_FIST)) {
                return false;
            }
            return true;
        }

        if(en instanceof Jumpship) {
            if(qName.equals("atmo_flyer")
                    || qName.equals(OptionsConstants.QUIRK_POS_ANTI_AIR)
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_COMP)
                    || qName.equals("combat_computer")
                    || qName.equals(OptionsConstants.QUIRK_POS_COMMAND_MECH)
                    || qName.equals("cowl")
                    || qName.equals("docking_arms")
                    || qName.equals("ext_twist")
                    || qName.equals("fast_reload")
                    || qName.equals("hyper_actuator")
                    || qName.equals(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS)
                    || qName.equals("imp_life_support")
                    || qName.equals("internal_bomb")
                    || qName.equals("mod_weapons")
                    || qName.equals("multi_trac")
                    || qName.equals("pro_actuator")
                    || qName.equals(OptionsConstants.QUIRK_POS_LOW_PROFILE)
                    || qName.equals("reinforced_legs")
                    || qName.equals("stable")
                    || qName.equals("trailer_hitch")
                    || qName.equals("searchlight")
                    || qName.equals("atmo_instability")
                    || qName.equals("cramped_cockpit")
                    || qName.equals("difficult_eject")
                    || qName.equals("exp_actuator")
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_ARMS)
                    || qName.equals("no_eject")
                    || qName.equals("head_eject")
                    || qName.equals("no_twist")
                    || qName.equals("large_dropper")
                    || qName.equals("poor_life_support")
                    || qName.equals("unbalanced")
                    || qName.equals("weak_legs")
                    || qName.equals("weak_undercarriage")
                    || qName.equals("vtol_rotor")
                    || qName.equals("flawed_cooling")
                    || qName.equals("unstreamlined")
                    || qName.equals("weak_head_1")
                    || qName.equals("weak_head_2")
                    || qName.equals("weak_head_3")
                    || qName.equals("weak_head_4")
                    || qName.equals("weak_head_5")
                    || qName.equals("gas_hog")
                    || qName.equals("rumble_seat")
                    || qName.equals("poor_performance")
                    || qName.equals("distracting")
                    || qName.equals("poor_sealing")
                    || qName.equals("power_reverse")
                    || qName.equals("stable")
                    || qName.equals("compact_mech")
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_FIST)) {
                return false;
            }
            return true;
        } else if (en instanceof Dropship) {
            if (qName.equals(OptionsConstants.QUIRK_POS_ANTI_AIR)
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_COMP)
                    || qName.equals("combat_computer")
                    || qName.equals(OptionsConstants.QUIRK_POS_COMMAND_MECH)
                    || qName.equals("cowl")
                    || qName.equals("ext_twist")
                    || qName.equals("fast_reload")
                    || qName.equals("hyper_actuator")
                    || qName.equals(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS)
                    || qName.equals("imp_life_support")
                    || qName.equals("multi_trac")
                    || qName.equals("pro_actuator")
                    || qName.equals(OptionsConstants.QUIRK_POS_LOW_PROFILE)
                    || qName.equals("reinforced_legs")
                    || qName.equals("stable")
                    || qName.equals("searchlight")
                    || qName.equals("trailer_hitch")
                    || qName.equals("cramped_cockpit")
                    || qName.equals("difficult_eject")
                    || qName.equals("exp_actuator")
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_ARMS)
                    || qName.equals("no_eject")
                    || qName.equals("head_eject")
                    || qName.equals("no_twist")
                    || qName.equals("poor_life_support")
                    || qName.equals("unbalanced")
                    || qName.equals("weak_undercarriage")
                    || qName.equals("weak_legs")
                    || qName.equals("vtol_rotor")
                    || qName.equals("flawed_cooling")
                    || qName.equals("rumble_seat")
                    || qName.equals("weak_head_1")
                    || qName.equals("weak_head_2")
                    || qName.equals("weak_head_3")
                    || qName.equals("weak_head_4")
                    || qName.equals("weak_head_5")
                    || qName.equals("poor_sealing")
                    || qName.equals("power_reverse")
                    || qName.equals("stable")
                    || qName.equals("compact_mech")
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_FIST)) {
                return false;
            }
            return true;
        } else if (en instanceof Aero) {
            if (qName.equals(OptionsConstants.QUIRK_POS_ANTI_AIR)
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_COMP)
                    || qName.equals(OptionsConstants.QUIRK_POS_COMMAND_MECH)
                    || qName.equals("cowl")
                    || qName.equals("docking_arms")
                    || qName.equals("ext_twist")
                    || qName.equals("hyper_actuator")
                    || qName.equals(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS)
                    || qName.equals("imp_life_support")
                    || qName.equals("multi_trac")
                    || qName.equals("pro_actuator")
                    || qName.equals(OptionsConstants.QUIRK_POS_LOW_PROFILE)
                    || qName.equals("stable")
                    || qName.equals("searchlight")
                    || qName.equals("trailer_hitch")
                    || qName.equals("reinforced_legs")
                    || qName.equals("exp_actuator")
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_ARMS)
                    || qName.equals("no_twist")
                    || qName.equals("large_dropper")
                    || qName.equals("unbalanced")
                    || qName.equals("weak_legs")
                    || qName.equals("head_eject")
                    || qName.equals("vtol_rotor")
                    || qName.equals("weak_head_1")
                    || qName.equals("weak_head_2")
                    || qName.equals("weak_head_3")
                    || qName.equals("weak_head_4")
                    || qName.equals("weak_head_5")
                    || qName.equals("poor_sealing")
                    || qName.equals("power_reverse")
                    || qName.equals("stable")
                    || qName.equals("compact_mech")
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_FIST)) {
                return false;
            }
            return true;
        } else if (en instanceof Protomech) {
            if (qName.equals(OptionsConstants.QUIRK_WEAP_POS_ACCURATE)
                    || qName.equals("distracting")
                    || qName.equals("easy_maintain")
                    || qName.equals("ext_twist")
                    || qName.equals("imp_com")
                    || qName.equals(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_TARG_S)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_TARG_M)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_TARG_L)
                    || qName.equals(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_L)
                    || qName.equals("ammo_feed")
                    || qName.equals("bad_rep")
                    || qName.equals("difficult_maintain")
                    || qName.equals("exposed_linkage")
                    || qName.equals("hard_pilot")
                    || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_INACCURATE)
                    || qName.equals("no_twist")
                    || qName.equals("head_eject")
                    || qName.equals("obsolete")
                    || qName.equals("poor_performance")
                    || qName.equals("poor_sealing")
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_TARG_S)
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_TARG_M)
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_TARG_L)
                    || qName.equals("poor_work")
                    || qName.equals("prototype")
                    || qName.equals("sensor_ghosts"))
            		{
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
