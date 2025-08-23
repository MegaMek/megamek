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

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.rolls.Roll;
import megamek.common.units.Infantry;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public class NarcExplosiveHandler extends MissileWeaponHandler {
    @Serial
    private static final long serialVersionUID = -1655014339855184419L;

    /**
     *
     */
    public NarcExplosiveHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) {
        super(t, w, g, m);
        sSalvoType = " explosive pod ";
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        getAMSHitsMod(vPhaseReport);
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if (target.isConventionalInfantry()) {
            if (attackingEntity instanceof BattleArmor) {
                bSalvo = true;
                return ((BattleArmor) attackingEntity).getShootingStrength();
            }
            return 1;
        }
        bSalvo = true;
        if (attackingEntity instanceof BattleArmor) {
            if (amsEngaged) {
                return Compute.missilesHit(
                      ((BattleArmor) attackingEntity).getShootingStrength(), -2);
            }
            return Compute
                  .missilesHit(((BattleArmor) attackingEntity).getShootingStrength());
        }

        if (amsEngaged) {
            Report r = new Report(3235);
            r.subject = subjectId;
            vPhaseReport.add(r);
            r = new Report(3230);
            r.indent(1);
            r.subject = subjectId;
            vPhaseReport.add(r);
            Roll diceRoll = Compute.rollD6(1);

            if (diceRoll.getIntValue() <= 3) {
                r = new Report(3240);
                r.subject = subjectId;
                r.add("pod");
                r.add(diceRoll);
                vPhaseReport.add(r);
                return 0;
            }
            r = new Report(3241);
            r.add("pod");
            r.add(diceRoll);
            r.subject = subjectId;
            vPhaseReport.add(r);
        }
        return 1;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcnCluster()
     */
    @Override
    protected int calculateNumCluster() {
        return 1;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        AmmoType ammoType = ammo.getType();
        double toReturn;
        if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.INARC) {
            toReturn = 6;
        } else {
            toReturn = 4;
        }
        if (target.isConventionalInfantry()) {
            toReturn = Compute.directBlowInfantryDamage(toReturn,
                  bDirect ? toHit.getMoS() / 3 : 0,
                  WeaponType.WEAPON_DIRECT_FIRE,
                  ((Infantry) target).isMechanized(),
                  toHit.getThruBldg() != null, attackingEntity.getId(), calcDmgPerHitReport);
            toReturn = Math.ceil(toReturn);
        }

        toReturn = applyGlancingBlowModifier(toReturn, target.isConventionalInfantry());
        return (int) toReturn;
    }
}
