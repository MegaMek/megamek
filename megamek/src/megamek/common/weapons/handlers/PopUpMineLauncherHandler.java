/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.weapons.DamageType;
import megamek.server.totalWarfare.TWGameManager;

public class PopUpMineLauncherHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = -6179453250580148965L;

    /**
     *
     */
    public PopUpMineLauncherHandler(ToHitData toHit, WeaponAttackAction waa,
          Game g, TWGameManager m) throws EntityLoadingException {
        super(toHit, waa, g, m);
        sSalvoType = " mine(s) ";
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if (target.isConventionalInfantry()) {
            if (attackingEntity instanceof BattleArmor) {
                bSalvo = true;
                return ((BattleArmor) attackingEntity).getShootingStrength();
            }
            return 1;
        }
        int hits = weapon.getCurrentShots();
        if (!allShotsHit()) {
            hits = Compute.missilesHit(hits);
        }
        bSalvo = true;
        Report r = new Report(3325);
        r.subject = subjectId;
        r.add(hits);
        r.add(sSalvoType);
        r.add(toHit.getTableDesc());
        r.newlines = 0;
        vPhaseReport.addElement(r);
        r = new Report(3345);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        return hits;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.handlers.WeaponHandler#handleEntityDamage(megamek.common
     * .Entity, java.util.Vector, megamek.common.units.Building, int, int, int, int)
     */
    @Override
    protected void handleEntityDamage(Entity entityTarget,
          Vector<Report> vPhaseReport, IBuilding bldg, int hits, int nCluster,
          int bldgAbsorbs) {
        // Per TW p.229: "four points of damage are assigned per mine that struck the location"
        // "In addition to those 4 damage points, the attacker rolls 2D6 for each pop-up mine"
        // Process each mine that hit
        while (hits > 0) {
            // Per TW p.229: mines automatically hit center torso (Mek), front (vehicle), or nose (grounded fighter)
            HitData hit;
            if (target instanceof Mek) {
                hit = new HitData(Mek.LOC_CENTER_TORSO);
            } else if (target instanceof Aero) {
                hit = new HitData(Aero.LOC_NOSE);
            } else { // Tank or other vehicle
                hit = new HitData(Tank.LOC_FRONT);
            }
            hit.setAttackerId(getAttackerId());
            hit.setGeneralDamageType(generalDamageType);

            // Deal 4 damage per mine (may cause additional crit if it hits internal structure)
            Vector<Report> damageReport = gameManager
                  .damageEntity(
                        entityTarget,
                        hit,
                        4,
                        false,
                        weaponEntity.getSwarmTargetId() == entityTarget.getId() ? DamageType.IGNORE_PASSENGER
                              : damageType,
                        false, false, throughFront,
                        underWater);
            vPhaseReport.addAll(damageReport);

            // Roll for critical hit (2D6 on Determining Critical Hits Table, TW p.229)
            // Always show the roll - if 8+, crits are applied by criticalEntity()
            Report critHeader = new Report(3343);
            critHeader.subject = entityTarget.getId();
            critHeader.indent(2);
            vPhaseReport.addElement(critHeader);

            Vector<Report> critReport = gameManager
                  .criticalEntity(
                        entityTarget,
                        hit.getLocation(), hit.isRear(),
                        entityTarget.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HARDENED ? -2
                              : 0,
                        4);
            vPhaseReport.addAll(critReport);

            hits--;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#useAmmo()
     */
    @Override
    protected void useAmmo() {
        setDone();
        checkAmmo();
        // how many shots are we firing?
        int nShots = weapon.getCurrentShots();

        // do we need to revert to single shot?
        if (nShots > 1) {
            int nAvail = weaponEntity.getTotalAmmoOfType(ammo.getType());
            while (nAvail < nShots) {
                nShots--;
            }
        }

        // use up ammo
        for (int i = 0; i < nShots; i++) {
            if (ammo.getUsableShotsLeft() <= 0) {
                weaponEntity.loadWeaponWithSameAmmo(weapon);
                ammo = (AmmoMounted) weapon.getLinked();
            }
            ammo.setShotsLeft(ammo.getBaseShotsLeft() - 1);
        }
    }
}
