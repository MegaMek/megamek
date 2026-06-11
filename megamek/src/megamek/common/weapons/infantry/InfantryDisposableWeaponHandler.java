/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.infantry;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.weapons.DamageType;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Resolves a Disposable Infantry Weapon attack (TO:AuE p.116, Corrected Sixth Printing). Conventional infantry and
 * battle armor may make a single once-per-scenario attack with their one-shot Disposable Weapons instead of the
 * platoon's standard weapon attack.
 * <p>
 * The damage formula differs from a standard infantry weapon attack: the total damage equals three times the
 * disposable weapon's per-trooper damage value, multiplied by the number of troopers who hit on the Cluster Hits
 * Table, rounded normally. None of the standard conventional-infantry range-0 bonuses (TSM, prosthetic enhancements,
 * extraneous limbs, tail or beast-mount damage) and none of the primary-weapon damage caps apply: these are fired
 * weapons resolved strictly by the formula.
 * </p>
 *
 * @author HammerGS
 */
public class InfantryDisposableWeaponHandler extends InfantryWeaponHandler {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The per-trooper damage of a Disposable Weapon attack is multiplied by this value before being scaled by the
     * number of troopers who hit (TO:AuE p.116, Corrected Sixth Printing).
     */
    public static final double DISPOSABLE_DAMAGE_MULTIPLIER = 3.0;

    public InfantryDisposableWeaponHandler(ToHitData toHitData, WeaponAttackAction weaponAttackAction, Game game,
          TWGameManager twGameManager) throws EntityLoadingException {
        super(toHitData, weaponAttackAction, game, twGameManager);
    }

    @Override
    protected int calcHits(Vector<Report> phaseReportVector) {
        int hitModifierCount = 0;
        if (bGlancing) {
            hitModifierCount -= 4;
        }
        if (bLowProfileGlancing) {
            hitModifierCount -= 4;
        }

        int troopersHit;
        if (attackingEntity.getSwarmTargetId() == target.getId()) {
            // when swarming all troopers hit
            troopersHit = ((Infantry) attackingEntity).getShootingStrength();
        } else if (!(attackingEntity instanceof Infantry)) {
            troopersHit = 1;
        } else {
            troopersHit = Compute.missilesHit(((Infantry) attackingEntity).getShootingStrength(), hitModifierCount);
        }

        double damagePerTrooper = DISPOSABLE_DAMAGE_MULTIPLIER * ((InfantryWeapon) weaponType).getInfantryDamage();
        int damageDealt = (int) Math.round(damagePerTrooper * troopersHit);

        if ((target instanceof Infantry targetInfantry) && targetInfantry.isMechanized()) {
            damageDealt /= 2;
        }
        if ((target instanceof IBuilding) && weaponType.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageDealt = 0;
        }
        if (weaponType.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageType = DamageType.NONPENETRATING;
        }

        // Only infantry can carry Disposable Weapons, so no non-infantry report variant is needed.
        Report report = new Report(3301);
        report.subject = subjectId;
        report.add(troopersHit);
        report.add(toHit.getTableDesc());
        report.add(damageDealt);
        report.newlines = 0;
        phaseReportVector.addElement(report);

        if (target.isConventionalInfantry()) {
            nDamPerHit = damageDealt;
            return 1;
        }
        return damageDealt;
    }

    /**
     * A Disposable Weapon is expended for the rest of the scenario once it is fired. Mark the firing weapon as fired so
     * {@link megamek.common.equipment.Mounted#canFire()} blocks any further use this game.
     */
    @Override
    public void useAmmo() {
        weapon.setFired(true);
        setDone();
    }
}
