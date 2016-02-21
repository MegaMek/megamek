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
import megamek.common.EquipmentType;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.MiscType;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.WeaponType;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.EnergyWeapon;

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
        IBasicOptionGroup wpnQuirk = addGroup("wpn_quirks", WPN_QUIRKS); //$NON-NLS-1$
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_POS_ACCURATE, false); //$NON-NLS-1$
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_INACCURATE, false); //$NON-NLS-1$
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_POS_IMP_COOLING, false); //$NON-NLS-1$
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_POOR_COOLING, false); //$NON-NLS-1$
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_NO_COOLING, false); //$NON-NLS-1$
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_EXPOSED_LINKAGE, false); //$NON-NLS-1$
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_AMMO_FEED_PROBLEMS, false); //$NON-NLS-1$
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_EM_INTERFERENCE, false); //$NON-NLS-1$
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_POS_JETTISON_CAPABLE, false); //$NON-NLS-1$
        addOption(wpnQuirk, OptionsConstants.QUIRK_POS_FAST_RELOAD, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_NON_FUNCTIONAL, false);
        addOption(wpnQuirk, OptionsConstants.QUIRK_WEAP_NEG_STATIC_FEED, false);
    }
    
    //unimplemented
    //ammo feed problem
    //EM Interference
    //jettison-capable Weapon
    //non-functional

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.options.AbstractOptions#getOptionsInfoImp()
     */
    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return WeaponQuirksInfo.getInstance();
    }

    public static boolean isQuirkLegalFor(IOption quirk, Entity en,
            EquipmentType etype) {
        String qName = quirk.getName();
        // There may be some non-WeaponType quirks, specifically melee weapons
        if (!(etype instanceof WeaponType) && !etype.hasFlag(MiscType.F_CLUB)) {
            return false;
        } else if (etype.hasFlag(MiscType.F_CLUB)) {
            if (qName.equals(OptionsConstants.QUIRK_WEAP_NEG_AMMO_FEED_PROBLEMS)
                    || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_EM_INTERFERENCE)
                    || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_NO_COOLING)
                    || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_POOR_COOLING)
                    || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_STATIC_FEED)
                    || qName.equals(OptionsConstants.QUIRK_WEAP_POS_IMP_COOLING)
                    || qName.equals(OptionsConstants.QUIRK_POS_FAST_RELOAD)) {
                return false;
            }
            return true;
        }

        // Anything else is a WeaponType
        WeaponType wtype = (WeaponType) etype;

        if (!(wtype instanceof AmmoWeapon)) {
            if (qName.equals(OptionsConstants.QUIRK_WEAP_NEG_AMMO_FEED_PROBLEMS)) {
                return false;
            }
            if (qName.equals(OptionsConstants.QUIRK_WEAP_NEG_STATIC_FEED)) {
                return false;
            }
        }

        if (!(wtype instanceof EnergyWeapon) && qName.equals(OptionsConstants.QUIRK_WEAP_NEG_EM_INTERFERENCE)) {
            return false;
        }
        
        if (en instanceof Protomech) {
            if (qName.equals(OptionsConstants.QUIRK_POS_FAST_RELOAD)
                || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_STATIC_FEED)) {
                return false;
            }
        }

        if (en instanceof Tank || en instanceof BattleArmor || en instanceof Protomech ) {
            if (qName.equals(OptionsConstants.QUIRK_WEAP_POS_IMP_COOLING)
                || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_POOR_COOLING)
                || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_NO_COOLING)) {
                return false;
            }
        }

        if (en instanceof Infantry && !(en instanceof BattleArmor)) {
            return false;
        }

        if (wtype.getHeat() == 0) {
            if (qName.equals(OptionsConstants.QUIRK_WEAP_POS_IMP_COOLING)
                || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_POOR_COOLING)
                || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_NO_COOLING)) {
                return false;
            }
        }

        if (qName.equals(OptionsConstants.QUIRK_WEAP_POS_JETTISON_CAPABLE)) {
            if (en instanceof Protomech
                || en instanceof Aero
                || en instanceof Jumpship
                || en instanceof Dropship) {

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
