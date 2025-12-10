/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Jumpship;
import megamek.common.units.Warship;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author MKerensky
 */
public class ASEWMissileWeaponHandler extends ThunderBoltWeaponHandler {
    @Serial
    private static final long serialVersionUID = 6359291710822171023L;

    /**
     * Weapon handler for Anti Ship Electronic Warfare Missiles Single, large missile - behaves like a thunderbolt
     * except for damage.
     *
     * @param t - ToHit roll data
     * @param w - The weapon attack action for this ASEW missile
     * @param g - The current game
     * @param m - The current GameManager instance
     */
    public ASEWMissileWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget,
          Vector<Report> vPhaseReport, IBuilding bldg, int hits, int nCluster,
          int bldgAbsorbs) {
        missed = false;

        hit = entityTarget.rollHitLocation(toHit.getHitTable(),
              toHit.getSideTable(), weaponAttackAction.getAimedLocation(),
              weaponAttackAction.getAimingMode(), toHit.getCover());
        hit.setGeneralDamageType(generalDamageType);
        hit.setCapital(weaponType.isCapital());
        hit.setBoxCars(roll.getIntValue() == 12);
        hit.setCapMisCritMod(getCapMisMod());
        hit.setFirstHit(firstHit);
        hit.setAttackerId(getAttackerId());
        if (weapon.isWeaponGroup()) {
            hit.setSingleAV(attackValue);
        }
        //Report the hit table and location
        Report r = new Report(3405);
        r.subject = subjectId;
        r.add(toHit.getTableDesc());
        r.add(entityTarget.getLocationAbbr(hit));
        vPhaseReport.addElement(r);
        if (numWeaponsHit > 1) {
            //If the target is hit by multiple ASEW missiles, report it here, even if the effects don't stack
            r.newlines = 1;
            r = new Report(3471);
            r.subject = subjectId;
            r.addDesc(entityTarget);
            r.add(numWeaponsHit);
            vPhaseReport.add(r);
        } else {
            //Otherwise, report a single ASEW missile hit
            r.newlines = 1;
            r = new Report(3470);
            r.subject = subjectId;
            r.addDesc(entityTarget);
            vPhaseReport.add(r);
        }
        //Large craft suffer a to-hit penalty for the location struck.
        if (entityTarget instanceof Dropship dropship) {
            dropship.setASEWAffected(hit.getLocation(), 2);
            //Report the arc affected by the attack and the duration of the effects
            r = new Report(3472);
            r.subject = subjectId;
            r.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.add(r);
        } else if (entityTarget instanceof Jumpship jumpship) {
            int loc = hit.getLocation();
            jumpship.setASEWAffected(loc, 2);
            //If a Warship is hit in the fore or aft side, the broadside arc is also affected
            if ((jumpship instanceof Warship)
                  && (loc == Jumpship.LOC_FLS || loc == Jumpship.LOC_ALS)) {
                jumpship.setASEWAffected(Warship.LOC_LBS, 2);
                //Report the arc hit by the attack and the associated broadside and the duration of the effects
                r = new Report(3474);
                r.subject = subjectId;
                r.add(entityTarget.getLocationAbbr(hit));
                r.add("LBS");
                vPhaseReport.add(r);
            } else if ((jumpship instanceof Warship)
                  && (loc == Jumpship.LOC_FRS || loc == Jumpship.LOC_ARS)) {
                jumpship.setASEWAffected(Warship.LOC_RBS, 2);
                //Report the arc hit by the attack and the associated broadside and the duration of the effects
                r = new Report(3474);
                r.subject = subjectId;
                r.add(entityTarget.getLocationAbbr(hit));
                r.add("RBS");
                vPhaseReport.add(r);
            } else {
                //If the nose or aft is hit, just report the arc affected by the attack and the duration of the effects
                r = new Report(3472);
                r.subject = subjectId;
                r.add(entityTarget.getLocationAbbr(hit));
                vPhaseReport.add(r);
            }
        } else {
            // Other units just suffer a flat +4 penalty until the effects expire
            entityTarget.setASEWAffected(2);
            //Report the duration of the effects
            r = new Report(3473);
            r.subject = subjectId;
            vPhaseReport.add(r);
        }
    }

    @Override
    protected int calcAttackValue() {
        calcCounterAV();
        return 0;
    }
}
