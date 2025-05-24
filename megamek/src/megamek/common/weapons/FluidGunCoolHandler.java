/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

import java.io.Serial;
import java.util.Vector;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Sebastian Brocks Created on Sep 23, 2004
 */
public class FluidGunCoolHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = 4856089237895318515L;

    /**
     * @param toHit              The {@link ToHitData} to use.
     * @param weaponAttackAction The {@link WeaponAttackAction} to use.
     * @param game               The {@link Game} object to use.
     * @param twGameManager      A {@link TWGameManager} to use.
     */
    public FluidGunCoolHandler(ToHitData toHit, WeaponAttackAction weaponAttackAction, Game game,
          TWGameManager twGameManager) {
        super(toHit, weaponAttackAction, game, twGameManager);
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport, Building building, int hits,
          int nCluster, int bldgAbsorbs) {
        if (entityTarget.isConventionalInfantry()) {
            // 1 point direct-fire ballistic
            nDamPerHit = Compute.directBlowInfantryDamage(1,
                  bDirect ? toHit.getMoS() / 3 : 0,
                  WeaponType.WEAPON_DIRECT_FIRE,
                  ((Infantry) target).isMechanized(),
                  toHit.getThruBldg() != null);
            super.handleEntityDamage(entityTarget, vPhaseReport, building, hits, nCluster, bldgAbsorbs);
        }

        Report report = new Report(3390);
        report.subject = subjectId;
        vPhaseReport.addElement(report);
        if (entityTarget.infernos.isStillBurning() ||
                  ((target instanceof Tank tankTarget) && tankTarget.isOnFire() && tankTarget.isInfernoFire())) {
            report = new Report(3545);
            report.subject = subjectId;
            report.addDesc(entityTarget);
            report.indent(3);
            Roll diceRoll = Compute.rollD6(2);
            report.add(diceRoll);

            if (diceRoll.getIntValue() == 12) {
                report.choose(true);
                entityTarget.infernos.clear();
            } else {
                report.choose(false);
            }
            vPhaseReport.add(report);
        } else if ((target instanceof Tank) && ((Tank) target).isOnFire()) {
            report = new Report(3550);
            report.subject = subjectId;
            report.addDesc(entityTarget);
            report.indent(3);
            Roll diceRoll = Compute.rollD6(2);
            report.add(diceRoll);

            if (diceRoll.getIntValue() >= 4) {
                report.choose(true);
                for (int i = 0; i < entityTarget.locations(); i++) {
                    ((Tank) target).extinguishAll();
                }
            } else {
                report.choose(false);
            }
            vPhaseReport.add(report);
        }
        // coolant also reduces heat of meks
        if (target instanceof Mek) {
            int nDamage = 3;
            report = new Report(3400);
            report.subject = subjectId;
            report.indent(2);
            report.add(nDamage);
            report.choose(false);
            vPhaseReport.add(report);
            entityTarget.coolFromExternal += nDamage;
        }
    }
}
