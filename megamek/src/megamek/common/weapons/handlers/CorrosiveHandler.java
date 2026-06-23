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
package megamek.common.weapons.handlers;

import static java.lang.Math.floor;

import java.io.Serial;
import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Resolves an attack by a Fluid Gun or Sprayer firing Corrosive Ammo (TO:AUE p.173). Against any target
 * other than conventional infantry, Corrosive Ammo inflicts 1D6 points of damage in 1-point clusters
 * during the Weapon Attack Phase and a further 1D6/2 (round up) in the End Phase of the same turn.
 * Against conventional infantry it is a 1D6 burst-fire attack.
 *
 * @author The MegaMek Team
 */
public class CorrosiveHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = 6157212447692058194L;

    private boolean endPhaseDamageScheduled = false;

    public CorrosiveHandler(ToHitData toHit, WeaponAttackAction waa, Game game, TWGameManager manager)
          throws EntityLoadingException {
        super(toHit, waa, game, manager);
        generalDamageType = HitData.DAMAGE_BALLISTIC;
    }

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        if (target.isConventionalInfantry()) {
            return 1;
        }
        // Against everything else the Weapon-Phase damage is 1D6, resolved as 1-point clusters.
        return Compute.d6();
    }

    @Override
    protected int calculateNumCluster() {
        return 1;
    }

    @Override
    protected int calcDamagePerHit() {
        if (!target.isConventionalInfantry()) {
            // 1-point damage clusters against non-infantry targets.
            return 1;
        }
        // 1D6 burst-fire attack against conventional infantry.
        double damage = Compute.d6();
        if (bDirect) {
            damage += floor(toHit.getMoS() / 3.0);
        }
        if (((Entity) target).hasAbility(OptionsConstants.MD_PAIN_SHUNT)) {
            damage /= 2;
        }
        damage = applyGlancingBlowModifier(damage, true);
        return (int) damage;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport, IBuilding building,
          int hits, int nCluster, int bldgAbsorbs) {
        super.handleEntityDamage(entityTarget, vPhaseReport, building, hits, nCluster, bldgAbsorbs);

        // Queue the End-Phase corrosive damage once, only on a successful hit against a non-infantry target.
        if (!entityTarget.isConventionalInfantry() && !endPhaseDamageScheduled) {
            endPhaseDamageScheduled = true;
            int delayedDamage = (int) Math.ceil(Compute.d6() / 2.0);
            entityTarget.addPendingCorrosiveDamage(delayedDamage);

            Report report = new Report(3391);
            report.subject = subjectId;
            report.indent(2);
            report.add(delayedDamage);
            vPhaseReport.addElement(report);
        }
    }
}
