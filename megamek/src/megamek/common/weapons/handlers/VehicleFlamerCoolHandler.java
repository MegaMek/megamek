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

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.rolls.Roll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.common.weapons.flamers.clan.CLHeavyFlamer;
import megamek.common.weapons.flamers.innerSphere.ISHeavyFlamer;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks Created on Sep 23, 2004
 */
public class VehicleFlamerCoolHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = 4856089237895318515L;

    /**
     * @param toHitData          The {@link ToHitData} to use.
     * @param weaponAttackAction The {@link WeaponAttackAction} to use.
     * @param game               The {@link Game} object to use.
     * @param twGameManager      A {@link TWGameManager} to use.
     */
    public VehicleFlamerCoolHandler(ToHitData toHitData, WeaponAttackAction weaponAttackAction, Game game,
          TWGameManager twGameManager) throws EntityLoadingException {
        super(toHitData, weaponAttackAction, game, twGameManager);
    }

    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport, Entity entityTarget) {
        if (!bMissed && (target.getTargetType() == Targetable.TYPE_HEX_EXTINGUISH)) {
            // Coolant douses an ordinary hex fire on a 2D6 roll of 4+ (12 for an Inferno fire) (TO:AUE p.173).
            FluidFireSuppression.extinguishHex(game, gameManager, target, subjectId, 4, vPhaseReport);
            return true;
        }
        return false;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport, IBuilding bldg, int hits,
          int nCluster, int bldgAbsorbs) {
        if (entityTarget.isConventionalInfantry()) {
            // 1 point direct-fire ballistic
            nDamPerHit = Compute.directBlowInfantryDamage(1,
                  bDirect ? toHit.getMoS() / 3 : 0,
                  WeaponType.WEAPON_DIRECT_FIRE,
                  ((Infantry) target).isMechanized(),
                  toHit.getThruBldg() != null);
            super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
        }
        Report report = new Report(3390);
        report.subject = subjectId;
        vPhaseReport.addElement(report);
        boolean infernoFireOnTank = (target instanceof Tank tank) && tank.isOnFire() && tank.isInfernoFire();
        if (entityTarget.infernos.isStillBurning() || infernoFireOnTank) {
            // Fires created by Inferno munitions are only doused by coolant on a roll of 12 (TO:AUE p.173).
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
        } else if ((target instanceof Tank tank) && tank.isOnFire()) {
            // Ordinary (non-Inferno) fires are doused by coolant on a roll of 4+ (TO:AUE p.173).
            report = new Report(3550);
            report.subject = subjectId;
            report.addDesc(entityTarget);
            report.indent(3);
            Roll diceRoll = Compute.rollD6(2);
            report.add(diceRoll);

            if (diceRoll.getIntValue() >= 4) {
                report.choose(true);
                tank.extinguishAll();
            } else {
                report.choose(false);
            }
            vPhaseReport.add(report);
        }

        // Coolant reduces the heat of any heat-tracking target by 3 points per hit - 4 from a Heavy Flamer
        // (the heat phase caps total external cooling at 9 per turn) (TO:AUE p.173).
        if (entityTarget.tracksHeat()) {
            int cooling = coolingPointsFor(weaponType);
            report = new Report(3400);
            report.subject = subjectId;
            report.indent(2);
            report.add(cooling);
            report.choose(false);
            vPhaseReport.add(report);
            entityTarget.coolFromExternal += cooling;
        }
    }

    /**
     * @param weaponType the flamer firing Coolant Ammo
     *
     * @return the heat a single Coolant hit removes from a heat-tracking target: 4 from a Heavy Flamer,
     *       3 from a standard Vehicle Flamer (TO:AUE p.173)
     */
    static int coolingPointsFor(WeaponType weaponType) {
        return ((weaponType instanceof ISHeavyFlamer) || (weaponType instanceof CLHeavyFlamer)) ? 4 : 3;
    }
}
