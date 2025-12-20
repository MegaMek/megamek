/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons.handlers;

import static java.lang.Math.floor;

import java.io.Serial;

import megamek.common.RangeType;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jay Lawson
 */
public class AmmoBayWeaponHandler extends BayWeaponHandler {

    @Serial
    private static final long serialVersionUID = -1618484541772117621L;

    protected AmmoBayWeaponHandler() {
        // deserialization only
    }

    /**
     *
     */
    public AmmoBayWeaponHandler(ToHitData t, WeaponAttackAction w, Game g,
          TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
    }

    /**
     * Calculate the attack value based on range This needs to do additional work for Weapon Bays with ammo. I need to
     * use the ammo within this function because I may run out of ammo while going through the loop Sine this function
     * is called in the WeaponHandler constructor it should be ok to use the ammo here
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {

        double av = 0;
        int range = RangeType.rangeBracket(nRange, weaponType.getATRanges(), true, false);

        for (WeaponMounted bayW : weapon.getBayWeapons()) {
            // check the currently loaded ammo
            AmmoMounted bayWAmmo = bayW.getLinkedAmmo();
            if (null == bayWAmmo || bayWAmmo.getUsableShotsLeft() < 1) {
                // try loading something else
                weaponEntity.loadWeaponWithSameAmmo(bayW);
                bayWAmmo = bayW.getLinkedAmmo();
            }
            if (!bayW.isBreached()
                  && !bayW.isDestroyed()
                  && !bayW.isJammed()
                  && bayWAmmo != null
                  && weaponEntity.getTotalAmmoOfType(bayWAmmo.getType()) >= bayW.getCurrentShots()) {
                WeaponType bayWType = bayW.getType();
                // need to cycle through weapons and add av
                double current_av = 0;
                AmmoType ammoType = bayWAmmo.getType();

                if (range == WeaponType.RANGE_SHORT) {
                    current_av = bayWType.getShortAV();
                } else if (range == WeaponType.RANGE_MED) {
                    current_av = bayWType.getMedAV();
                } else if (range == WeaponType.RANGE_LONG) {
                    current_av = bayWType.getLongAV();
                } else if (range == WeaponType.RANGE_EXT) {
                    current_av = bayWType.getExtAV();
                }
                current_av = updateAVForAmmo(current_av, ammoType, bayWType,
                      range, bayW.getEquipmentNum());
                av = av + current_av;
                // now use the ammo that we had loaded
                if (current_av > 0) {
                    int shots = bayW.getCurrentShots();
                    for (int i = 0; i < shots; i++) {
                        if (null == bayWAmmo
                              || bayWAmmo.getUsableShotsLeft() < 1) {
                            // try loading something else
                            weaponEntity.loadWeaponWithSameAmmo(bayW);
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
            av = Math.min(av + (int) floor(toHit.getMoS() / 3.0), av * 2);
        }
        av = applyGlancingBlowModifier(av, false);
        av = (int) Math.floor(getBracketingMultiplier() * av);
        return (int) Math.ceil(av);
    }

    /*
     * check for special munitions and their effect on av
     */
    protected double updateAVForAmmo(double current_av, AmmoType ammoType, WeaponType bayWType, int range, int wId) {

        if (ammoType.getMunitionType().contains(Munitions.M_CLUSTER)) {
            current_av = Math.floor(0.6 * current_av);
        }

        return current_av;
    }
}
