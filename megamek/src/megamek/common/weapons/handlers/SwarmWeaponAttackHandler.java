/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Mek;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jay Lawson
 * @since Feb 21, 2013
 */
public class SwarmWeaponAttackHandler extends WeaponHandler {
    @Serial
    private static final long serialVersionUID = -2439937071168853215L;

    /**
     *
     */
    public SwarmWeaponAttackHandler(ToHitData toHit, WeaponAttackAction waa, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(toHit, waa, g, m);
        generalDamageType = HitData.DAMAGE_NONE;
    }

    @Override
    protected int calcDamagePerHit() {
        int damage = 0;
        int tsmBonusDamage = 0;
        if (attackingEntity instanceof BattleArmor battleArmor) {
            damage = battleArmor.calculateSwarmDamage();
            // TSM Implant adds +1 damage per trooper for same-hex attacks
            if (attackingEntity.hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
                tsmBonusDamage = battleArmor.getTroopers();
                damage += tsmBonusDamage;
            }
        }
        // should this be affected by direct blows?
        // assume so for now
        if (bDirect) {
            damage = Math.min(damage + (toHit.getMoS() / 3), damage * 2);
        }
        // Report TSM Implant bonus damage (after direct blow calculation for accurate base)
        if (tsmBonusDamage > 0) {
            int baseDamage = damage - tsmBonusDamage;
            Report tsmReport = new Report(3418);
            tsmReport.subject = subjectId;
            tsmReport.indent(2);
            tsmReport.add(baseDamage);
            tsmReport.add(tsmBonusDamage);
            calcDmgPerHitReport.addElement(tsmReport);
        }
        return damage;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        return 1;
    }

    /**
     * Override to handle zero-damage swarm attacks. Per BattleTech rules, critical hits are a separate outcome from
     * damage - even with zero damage, swarm attacks should still roll for critical hits. See issue #5584.
     */
    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport,
          IBuilding bldg, int hits, int nCluster, int bldgAbsorbs) {
        // Determine if this is a positive damage attack.
        // nDamPerHit is normally set by parent's handle() method. If called directly (e.g., in unit tests),
        // we calculate from the BattleArmor to determine the correct code path without adding duplicate reports.
        int damageForPathDecision = nDamPerHit;
        if (damageForPathDecision == 0 && attackingEntity instanceof BattleArmor battleArmor) {
            damageForPathDecision = battleArmor.calculateSwarmDamage();
            if (attackingEntity.hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
                damageForPathDecision += battleArmor.getTroopers();
            }
        }

        // If we have damage, use normal handling which includes crit rolls
        if (damageForPathDecision > 0) {
            super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
            return;
        }

        // Zero damage swarm attack - still roll for critical hits per BattleTech rules
        // "Critical hits are a separate outcome from damage"
        missed = false;

        // Roll hit location using swarm hit table
        initHit(entityTarget);

        // Report the hit location
        Report report = new Report(3405);
        report.subject = subjectId;
        report.add(toHit.getTableDesc());
        report.add(entityTarget.getLocationAbbr(hit));
        vPhaseReport.addElement(report);

        // Report zero damage
        Report noDamageReport = new Report(3365);
        noDamageReport.subject = subjectId;
        noDamageReport.indent(2);
        vPhaseReport.addElement(noDamageReport);

        // Only Meks can take critical hits from swarm attacks
        if (entityTarget instanceof Mek) {
            // Roll for critical hit - swarm attacks can cause crits even with 0 damage
            vPhaseReport.addAll(gameManager.criticalEntity(entityTarget, hit.getLocation(),
                  hit.isRear(), 0, 0));
        }
    }
}
