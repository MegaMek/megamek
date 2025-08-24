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

package megamek.common.weapons.battleArmor;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Infantry;
import megamek.common.weapons.DamageType;
import megamek.common.weapons.handlers.WeaponHandler;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Sebastian Brockxs
 * @since Oct 20, 2004
 */
public class BAMGHandler extends WeaponHandler {
    @Serial
    private static final long serialVersionUID = 4109377609879352900L;

    /**
     *
     */
    public BAMGHandler(ToHitData toHitData, WeaponAttackAction weaponAttackAction, Game game,
          TWGameManager twGameManager) {
        super(toHitData, weaponAttackAction, game, twGameManager);
        damageType = DamageType.ANTI_INFANTRY;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        if (weapon.isRapidFire() && !(target instanceof Infantry)) {
            // Check for rapid fire Option. Only MGs can be rapid fire.
            switch (weaponType.getDamage()) {
                case 1:
                    nDamPerHit = Math.max(1, Compute.d6() - 1);
                    break;
                case 3:
                    nDamPerHit = Compute.d6() + 1;
                    break;
                default:
                    nDamPerHit = Compute.d6();
                    break;
            }
            numRapidFireHits = nDamPerHit;
            if (bDirect) {
                nDamPerHit = Math.min(nDamPerHit + (toHit.getMoS() / 3),
                      nDamPerHit * 2);
            }
            nDamPerHit = applyGlancingBlowModifier(nDamPerHit, false);
        } else {
            nDamPerHit = super.calcDamagePerHit();
        }
        return nDamPerHit;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#addHeat()
     */
    @Override
    protected void addHeat() {
        if (!(toHit.getValue() == TargetRoll.IMPOSSIBLE)) {
            if (weapon.isRapidFire()) {
                attackingEntity.heatBuildup += nDamPerHit;
            } else {
                super.addHeat();
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#reportMiss(java.util.Vector)
     */
    @Override
    protected void reportMiss(Vector<Report> vPhaseReport) {
        // Report the miss
        Report r = new Report(3220);
        r.subject = subjectId;
        vPhaseReport.add(r);
        if (weapon.isRapidFire() && !target.isConventionalInfantry()) {
            r.newlines = 0;
            r = new Report(3225);
            r.subject = subjectId;
            r.add(nDamPerHit * 3);
            vPhaseReport.add(r);
        }
    }
}
