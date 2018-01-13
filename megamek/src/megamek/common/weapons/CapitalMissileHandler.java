/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class CapitalMissileHandler extends AmmoWeaponHandler {

    /**
     *
     */

    private static final long serialVersionUID = -1618484541772117621L;
    boolean advancedPD = false;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public CapitalMissileHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
        advancedPD = g.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADV_POINTDEF);
    }
    
    /**
     * Calculate the attack value based on range
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {
        int av = 0;
        double counterAV = calcCounterAV();
        int armor = wtype.getMissileArmor();
        // if we have a ground firing unit, then AV should not be determined by
        // aero range brackets
        if (!ae.isAirborne() || game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_UAC_TWOROLLS)) {
            if (usesClusterTable()) {
                // for cluster weapons just use the short range AV
                av = wtype.getRoundShortAV();
            } else {
                // otherwise just use the full weapon damage by range
                av = wtype.getDamage(nRange);
            }
        } else {
            // we have an airborne attacker, so we need to use aero range
            // brackets
            int range = RangeType.rangeBracket(nRange, wtype.getATRanges(),
                    true, false);
            if (range == WeaponType.RANGE_SHORT) {
                av = wtype.getRoundShortAV();
            } else if (range == WeaponType.RANGE_MED) {
                av = wtype.getRoundMedAV();
            } else if (range == WeaponType.RANGE_LONG) {
                av = wtype.getRoundLongAV();
            } else if (range == WeaponType.RANGE_EXT) {
                av = wtype.getRoundExtAV();
            }
        }
        
        if (ammo.getType().hasFlag(AmmoType.F_NUCLEAR)) {
            nukeS2S = true;
        }
        
        CapMissileArmor = armor - (int) counterAV;
        CapMissileAMSMod = calcCapMissileAMSMod();
        
        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }
        if (bGlancing) {
            av = (int) Math.floor(av / 2.0);

        }
        av = (int) Math.floor(getBracketingMultiplier() * av);
        
        return av;
    }
  
    @Override
    protected int calcCapMissileAMSMod() {
        CapMissileAMSMod = (int) Math.ceil(CounterAV / 10.0);
        return CapMissileAMSMod;
    }
    
    @Override
    protected int getCapMissileAMSMod() {
        return CapMissileAMSMod;
    }
    
    @Override
    protected int getCapMisMod() {
        return getCritMod((AmmoType) ammo.getType());

    }


    /*
     * get the cap mis mod given a single ammo type
     */
    protected int getCritMod(AmmoType atype) {
        if (atype == null || atype.getAmmoType() == AmmoType.T_PIRANHA
                || atype.getAmmoType() == AmmoType.T_AAA_MISSILE
                || atype.getAmmoType() == AmmoType.T_ASEW_MISSILE
                || atype.getAmmoType() == AmmoType.T_LAA_MISSILE) {
            return 0;
        }
        if (atype.getAmmoType() == AmmoType.T_WHITE_SHARK
                || atype.getAmmoType() == AmmoType.T_WHITE_SHARK_T
                || atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)
                // Santa Anna, per IO rules
                || atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
            return 9;
        } else if (atype.getAmmoType() == AmmoType.T_KILLER_WHALE
                || atype.getAmmoType() == AmmoType.T_KILLER_WHALE_T
                || atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)
                || atype.getAmmoType() == AmmoType.T_MANTA_RAY
                || atype.getAmmoType() == AmmoType.T_ALAMO) {
            return 10;
        } else if (atype.getAmmoType() == AmmoType.T_KRAKEN_T
                || atype.getAmmoType() == AmmoType.T_KRAKENM
                // Peacemaker, per IO rules
                || atype.hasFlag(AmmoType.F_PEACEMAKER)) {
            return 8;
        } else if (atype.getAmmoType() == AmmoType.T_STINGRAY) {
            return 12;
        } else {
            return 11;
        }
    }
    
    /**
     * Checks to see if this point defense/AMS bay can engage a capital missile
     * This should return true. Only when handling capital missile attacks can this be false.
     */
    protected boolean canEngageCapitalMissile(Mounted counter) {
        if (counter.getBayWeapons().size() < 2) {
            return false;
        } else {
            return true;
        }
    }
    
    /**
     * Sets the appropriate AMS Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setAMSBayReportingFlag() {
        amsBayEngagedCap = true;
    }
    
    /**
     * Sets the appropriate PD Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setPDBayReportingFlag() {
        pdBayEngagedCap = true;
    }
}
