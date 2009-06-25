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


import java.util.Vector;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.IEntityMovementMode;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.Tank;

/**
 * Contains the options determining quirks of the unit
 * 
 * @author Taharqa (Jay Lawson)
 */
public class WeaponQuirks extends AbstractOptions {
    /**
     * 
     */
    private static final long serialVersionUID = -8455685281028804229L;
    public static final String WPN_QUIRKS = "WeaponQuirks"; //$NON-NLS-1$
  
    public WeaponQuirks() {
        super();
    }

    @Override
    public void initialize() {
        //positive quirks
        IBasicOptionGroup wpnQuirk = addGroup("wpn_quirks", WPN_QUIRKS); //$NON-NLS-1$
        addOption(wpnQuirk, "accurate", false); //$NON-NLS-1$
        addOption(wpnQuirk, "inaccurate", false); //$NON-NLS-1$
        addOption(wpnQuirk, "imp_cooling", false); //$NON-NLS-1$
        addOption(wpnQuirk, "poor_cooling", false); //$NON-NLS-1$
        addOption(wpnQuirk, "no_cooling", false); //$NON-NLS-1$
        //addOption(wpnQuirk, "ammo_feed", false); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.options.AbstractOptions#getOptionsInfoImp()
     */
    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return WeaponQuirksInfo.getInstance();
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
                    && en.getMovementMode() == IEntityMovementMode.HOVER) {
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

    private static class WeaponQuirksInfo extends AbstractOptionsInfo {
        private static AbstractOptionsInfo instance = new WeaponQuirksInfo();

        public static AbstractOptionsInfo getInstance() {
            return instance;
        }

        protected WeaponQuirksInfo() {
            super("WeaponQuirksInfo"); //$NON-NLS-1$
        }
    }
    
    
    
}
