/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.HitData;
import megamek.common.Game;
import megamek.common.Mech;
import megamek.common.Report;
import megamek.common.Tank;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.GameManager;
import megamek.server.Server;
import org.apache.logging.log4j.LogManager;

public class PopUpMineLauncherHandler extends AmmoWeaponHandler {
    private static final long serialVersionUID = -6179453250580148965L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public PopUpMineLauncherHandler(ToHitData toHit, WeaponAttackAction waa,
            Game g, GameManager m) {
        super(toHit, waa, g, m);
        sSalvoType = " mine(s) ";
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if (target.isConventionalInfantry()) {
            if (ae instanceof BattleArmor) {
                bSalvo = true;
                return ((BattleArmor) ae).getShootingStrength();
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
     * megamek.common.weapons.WeaponHandler#handleEntityDamage(megamek.common
     * .Entity, java.util.Vector, megamek.common.Building, int, int, int, int)
     */
    @Override
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                toHit.getSideTable(), waa.getAimedLocation(),
                waa.getAimingMode(), toHit.getCover());
        hit.setAttackerId(getAttackerId());
        if (target instanceof Mech) {
            hit = new HitData(Mech.LOC_CT);
        } else { // te instanceof Tank
            hit = new HitData(Tank.LOC_FRONT);
        }
        hit.setGeneralDamageType(generalDamageType);
        // Do criticals.
        Vector<Report> specialDamageReport = gameManager
                .criticalEntity(
                        entityTarget,
                        hit.getLocation(), hit.isRear(),
                        entityTarget.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HARDENED ? -2
                                : 0, 4);

        // Replace "no effect" results with 4 points of damage.
        if ((specialDamageReport.lastElement()).messageId == 6005) {
            int damage = 4;
            // ASSUMPTION: buildings CAN'T absorb *this* damage.
            // specialDamage = damageEntity(entityTarget, hit, damage);
            specialDamageReport = gameManager
                    .damageEntity(
                            entityTarget,
                            hit,
                            damage,
                            false,
                            ae.getSwarmTargetId() == entityTarget.getId() ? DamageType.IGNORE_PASSENGER
                                    : damageType, false, false, throughFront,
                            underWater);
        } else {
            // add newline _before_ last report
            try {
                (specialDamageReport.elementAt(specialDamageReport.size() - 2)).newlines++;
            } catch (Exception ignored) {
                LogManager.getLogger().error("No previous report when trying to add newline");
            }
        }
        // Report the result
        vPhaseReport.addAll(specialDamageReport);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#useAmmo()
     */
    @Override
    protected void useAmmo() {
        setDone();
        checkAmmo();
        // how many shots are we firing?
        int nShots = weapon.getCurrentShots();

        // do we need to revert to single shot?
        if (nShots > 1) {
            int nAvail = ae.getTotalAmmoOfType(ammo.getType());
            while (nAvail < nShots) {
                nShots--;
            }
        }

        // use up ammo
        for (int i = 0; i < nShots; i++) {
            if (ammo.getUsableShotsLeft() <= 0) {
                ae.loadWeaponWithSameAmmo(weapon);
                ammo = weapon.getLinked();
            }
            ammo.setShotsLeft(ammo.getBaseShotsLeft() - 1);
        }
    }
}
