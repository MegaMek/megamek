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

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.weapons.handlers.capitalMissile.CapitalMissileBayHandler;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jay Lawson
 */
public class TeleMissileHandler extends CapitalMissileBayHandler {
    private static final MMLogger logger = MMLogger.create(TeleMissileHandler.class);

    @Serial
    private static final long serialVersionUID = -1618484541772117621L;

    /**
     *
     */
    public TeleMissileHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
    }

    private int missileArmor = 0;

    /**
     * Method that collects the linked ammo type for a weapon bay We need this to pass through to server without using
     * the ammo in the process
     */
    private AmmoType getBayAmmoType() {
        AmmoType at = null;
        for (WeaponMounted bayW : weapon.getBayWeapons()) {
            // check the currently loaded ammo
            AmmoMounted bayWAmmo = bayW.getLinkedAmmo();

            if (bayWAmmo == null) {
                logger.debug("getBayAmmoTypes - Handler can't find any ammo! Oh no!");
                continue;
            }
            // Once we have some ammo to send to the server, stop looking
            at = bayWAmmo.getType();
            break;
        }
        return at;
    }

    private int calcBayDamageAndHeat() {
        int damage = 0;
        for (WeaponMounted bayW : weapon.getBayWeapons()) {
            WeaponType bayWType = bayW.getType();
            damage += (int) bayWType.getShortAV();
            weaponEntity.heatBuildup += bayW.getCurrentHeat();
            missileArmor = bayWType.getMissileArmor();
        }
        return damage;
    }

    @Override
    protected void useAmmo() {
        for (WeaponMounted bayW : weapon.getBayWeapons()) {
            // check the currently loaded ammo
            AmmoMounted bayWAmmo = bayW.getLinkedAmmo();

            if (bayWAmmo == null) {// Can't happen. w/o legal ammo, the weapon
                // *shouldn't* fire.
                logger.debug("Handler can't find any ammo! Oh no!");
            }
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

    /**
     * handle this weapons firing
     *
     * @return a <code>boolean</code> value indicating whether this should be kept or not
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        // just launch the tele-missile
        gameManager.deployTeleMissile(weaponEntity,
              weaponType,
              getBayAmmoType(),
              weaponEntity.getEquipmentNum(weapon),
              getCapMisMod(), calcBayDamageAndHeat(), missileArmor, vPhaseReport);

        return false;

    }

}
