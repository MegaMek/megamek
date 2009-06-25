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


import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Tank;
import megamek.common.WeaponType;

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
    //unimplemented
    //ammo feed problem
    //EM Interference

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.options.AbstractOptions#getOptionsInfoImp()
     */
    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return WeaponQuirksInfo.getInstance();
    }
    
    public static boolean isQuirkLegalFor(IOption quirk, Entity en, WeaponType wtype) {
        
        if(en instanceof Tank || en instanceof BattleArmor) {
            if(quirk.getName().equals("imp_cooling")
                    || quirk.getName().equals("poor_cooling")
                    || quirk.getName().equals("no_cooling")) {
                return false;
            }
        }
        
        if(en instanceof Infantry && !(en instanceof BattleArmor)) {
            return false;
        }
        
        if(wtype.getHeat() == 0) {
            if(quirk.getName().equals("imp_cooling")
                    || quirk.getName().equals("poor_cooling")
                    || quirk.getName().equals("no_cooling")) {
                return false;
            }
        }
        
        
        
        return true;
        
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
