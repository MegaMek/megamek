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
import megamek.common.Game;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.server.GameManager;
import megamek.server.Server;
import org.apache.logging.log4j.LogManager;

/**
 * @author Jay Lawson
 */
public class AmmoBayWeaponHandler extends BayWeaponHandler {

    private static final long serialVersionUID = -1618484541772117621L;

    protected AmmoBayWeaponHandler() {
        // deserialization only
    }

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public AmmoBayWeaponHandler(ToHitData t, WeaponAttackAction w, Game g,
            GameManager m) {
        super(t, w, g, m);
    }

    /**
     * Calculate the attack value based on range This needs to do additional
     * work for Weapon Bays with ammo. I need to use the ammo within this
     * function because I may run out of ammo while going through the loop Sine
     * this function is called in the WeaponHandler constructor it should be ok
     * to use the ammo here
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {

        double av = 0;
        int range = RangeType.rangeBracket(nRange, wtype.getATRanges(), true, false);

        for (int wId : weapon.getBayWeapons()) {
            WeaponMounted bayW = ae.getWeapon(wId);
            if (bayW == null) {
                LogManager.getLogger().error("Handler can't find the weapon!");
                return 0;
            }
            // check the currently loaded ammo
            AmmoMounted bayWAmmo = bayW.getLinkedAmmo();
            if (null == bayWAmmo || bayWAmmo.getUsableShotsLeft() < 1) {
                // try loading something else
                ae.loadWeaponWithSameAmmo(bayW);
                bayWAmmo = bayW.getLinkedAmmo();
            }
            if (!bayW.isBreached()
                    && !bayW.isDestroyed()
                    && !bayW.isJammed()
                    && bayWAmmo != null
                    && ae.getTotalAmmoOfType(bayWAmmo.getType()) >= bayW.getCurrentShots()) {
                WeaponType bayWType = ((WeaponType) bayW.getType());
                // need to cycle through weapons and add av
                double current_av = 0;
                AmmoType atype = (AmmoType) bayWAmmo.getType();

                if (range == WeaponType.RANGE_SHORT) {
                    current_av = bayWType.getShortAV();
                } else if (range == WeaponType.RANGE_MED) {
                    current_av = bayWType.getMedAV();
                } else if (range == WeaponType.RANGE_LONG) {
                    current_av = bayWType.getLongAV();
                } else if (range == WeaponType.RANGE_EXT) {
                    current_av = bayWType.getExtAV();
                }
                current_av = updateAVforAmmo(current_av, atype, bayWType,
                        range, wId);
                av = av + current_av;
                // now use the ammo that we had loaded
                if (current_av > 0) {
                    int shots = bayW.getCurrentShots();
                    for (int i = 0; i < shots; i++) {
                        if (null == bayWAmmo
                                || bayWAmmo.getUsableShotsLeft() < 1) {
                            // try loading something else
                            ae.loadWeaponWithSameAmmo(bayW);
                            bayWAmmo = bayW.getLinkedAmmo();
                        }
                        if (null != bayWAmmo) {
                            bayWAmmo.setShotsLeft(bayWAmmo.getBaseShotsLeft() - 1);
                        }
                    }
                }
            }
        }
        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }
        av = applyGlancingBlowModifier(av, false);
        av = (int) Math.floor(getBracketingMultiplier() * av);
        return (int) Math.ceil(av);
    }

    /*
     * check for special munitions and their effect on av
     */
    protected double updateAVforAmmo(double current_av, AmmoType atype,
            WeaponType bayWType, int range, int wId) {

        if (atype.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER)) {
            current_av = Math.floor(0.6 * current_av);
        }
        return current_av;
    }
}
