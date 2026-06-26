/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.CriticalSlot;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.IBomber;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Andrew Hunter
 */
public class AmmoWeaponHandler extends WeaponHandler {
    @Serial
    private static final long serialVersionUID = -4934490646657484486L;

    private static final MMLogger logger = MMLogger.create(AmmoWeaponHandler.class);

    protected AmmoMounted ammo;

    public AmmoWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
        generalDamageType = HitData.DAMAGE_BALLISTIC;
    }

    @Override
    protected void useAmmo() {
        checkAmmo();
        if (ammo == null) {
            // Can't happen. w/o legal ammo, the weapon *shouldn't* fire.
            logger.error("Handler can't find any ammo! Oh no!", new Exception());
            return;
        }

        // Skip ammo consumption for spawned rapid-fire attacks.
        // The parent RapidFireACWeaponHandler already consumed ammo for all shots.
        if (isSpawnedRapidFireAttack()) {
            super.useAmmo();
            return;
        }

        if (ammo.getUsableShotsLeft() <= 0) {
            weaponEntity.loadWeaponWithSameAmmo(weapon);
            ammo = (AmmoMounted) weapon.getLinked();
        }
        ammo.setShotsLeft(ammo.getBaseShotsLeft() - 1);

        if (weapon.isInternalBomb()) {
            ((IBomber) weaponEntity).increaseUsedInternalBombs(1);
        }

        super.useAmmo();
    }

    /**
     * Checks if this attack was spawned from a rapid-fire AC handler. Spawned attacks use auto-hit and have the
     * rapid-fire spawn marker.
     */
    private boolean isSpawnedRapidFireAttack() {
        return (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS)
              && toHit.getDesc().contains(RapidFireACWeaponHandler.RAPID_FIRE_SPAWN_MARKER);
    }

    protected void checkAmmo() {
        if (weapon.getLinked() instanceof AmmoMounted ammoMounted) {
            ammo = ammoMounted;
        } else {
            weaponEntity.loadWeapon(weapon);
            if (weapon.getLinked() instanceof AmmoMounted ammoMounted) {
                ammo = ammoMounted;
            } else {
                ammo = null;
            }
        }
    }

    /**
     * For ammo weapons, this number can be less than the full number if the amount of ammo is not high enough.
     *
     * @return the number of weapons of this type firing (for squadron weapon groups)
     */
    @Override
    protected int getNumberWeapons() {
        if (ammo == null) {
            // shouldn't happen
            return weapon.getNWeapons();
        }
        int totalShots = weaponEntity.getTotalAmmoOfType(ammo.getType());
        return Math.min(weapon.getNWeapons(),
              (int) Math.floor((double) totalShots / (double) weapon.getCurrentShots()));
    }

    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        return doAmmoFeedProblemCheck(vPhaseReport);
    }

    /**
     * Carry out an 'ammo feed problems' check on the weapon. Return true if it blew up.
     */
    @Override
    protected boolean doAmmoFeedProblemCheck(Vector<Report> vPhaseReport) {
        // don't have neg ammo feed problem quirk
        if (!weapon.hasQuirk(OptionsConstants.QUIRK_WEAPON_NEG_AMMO_FEED_PROBLEMS)) {
            return false;
        } else if ((roll.getIntValue() <= 2) && !weaponEntity.isConventionalInfantry()) {
            // attack roll was a 2, may explode
            Roll diceRoll = Compute.rollD6(2);

            Report r = new Report(3173);
            r.subject = subjectId;
            r.newlines = 0;
            r.add(diceRoll);
            vPhaseReport.addElement(r);

            if (diceRoll.getIntValue() == 12) {
                // round explodes in weapon
                r = new Report(3163);
                r.subject = subjectId;
                vPhaseReport.addElement(r);

                explodeRoundInBarrel(vPhaseReport);
            } else if (diceRoll.getIntValue() >= 10) {
                // plain old weapon jam
                r = new Report(3161);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
                weapon.setJammed(true);
            } else {
                // nothing bad happens
                r = new Report(5041);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
                return false;
            }
        } else {
            // attack roll was not 2, won't explode
            return false;
        }

        return true;
    }

    /**
     * Worker function that explodes a round in the barrel of the attack's weapon
     */
    protected void explodeRoundInBarrel(Vector<Report> vPhaseReport) {
        weapon.setJammed(true);
        weapon.setHit(true);

        int weaponLocation = weapon.getLocation();
        for (int i = 0; i < weaponEntity.getNumberOfCriticalSlots(weaponLocation); i++) {
            CriticalSlot slot1 = weaponEntity.getCritical(weaponLocation, i);
            if ((slot1 == null) || (slot1.getType() == CriticalSlot.TYPE_SYSTEM)) {
                continue;
            }
            Mounted<?> mounted = slot1.getMount();
            if (mounted.equals(weapon)) {
                weaponEntity.hitAllCriticalSlots(weaponLocation, i);
                break;
            }
        }

        // if we're here, the weapon is going to explode whether it's flagged as explosive or not
        vPhaseReport.addAll(gameManager.explodeEquipment(weaponEntity, weaponLocation, weapon, true));
    }
}
