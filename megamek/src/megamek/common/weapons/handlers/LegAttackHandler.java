/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
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
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Mek;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sept 23, 2004
 */
public class LegAttackHandler extends WeaponHandler {
    @Serial
    private static final long serialVersionUID = 4429993211361286138L;

    public LegAttackHandler(ToHitData toHit, WeaponAttackAction waa, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(toHit, waa, g, m);
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

    @Override
    protected void handleEntityDamage(Entity entityTarget,
          Vector<Report> vPhaseReport, IBuilding bldg, int hits, int nCluster,
          int bldgAbsorbs) {
        HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(),
              toHit.getSideTable(), weaponAttackAction.getAimedLocation(),
              weaponAttackAction.getAimingMode(), toHit.getCover());
        hit.setAttackerId(getAttackerId());
        // If a leg attacks hit a leg that isn't
        // there, then hit the other leg.
        if (entityTarget.getInternal(hit) <= 0) {
            if (hit.getLocation() == Mek.LOC_RIGHT_LEG) {
                hit = new HitData(Mek.LOC_LEFT_LEG);
            } else {
                hit = new HitData(Mek.LOC_RIGHT_LEG);
            }
        }
        hit.setGeneralDamageType(generalDamageType);

        Report report = new Report(3405);
        report.subject = subjectId;
        report.add(toHit.getTableDesc());
        report.add(entityTarget.getLocationAbbr(hit));
        vPhaseReport.addElement(report);

        int damage = 4;
        int tsmBonusDamage = 0;
        if (attackingEntity instanceof BattleArmor battleArmor) {
            damage += attackingEntity.getVibroClaws();
            if (battleArmor.hasMyomerBooster()) {
                damage += battleArmor.getTroopers() * 2;
            }
            // TSM Implant adds +1 damage per trooper for same-hex attacks
            if (attackingEntity.hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
                tsmBonusDamage = battleArmor.getTroopers();
                damage += tsmBonusDamage;
            }
        }

        // Report TSM Implant bonus damage
        if (tsmBonusDamage > 0) {
            int baseDamage = damage - tsmBonusDamage;
            Report tsmReport = new Report(3418);
            tsmReport.subject = subjectId;
            tsmReport.indent(2);
            tsmReport.add(baseDamage);
            tsmReport.add(tsmBonusDamage);
            vPhaseReport.addElement(tsmReport);
        }

        // ASSUMPTION: buildings CAN'T absorb *this* damage.
        vPhaseReport.addAll(gameManager.damageEntity(entityTarget, hit, damage,
              false, damageType, false, false, throughFront, underWater));
        Report.addNewline(vPhaseReport);
        // Do criticalSlots.
        int critMod = 0;
        if (entityTarget.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HARDENED) {
            critMod -= 2;
        }
        if (attackingEntity.hasAbility(OptionsConstants.MISC_HUMAN_TRO, Crew.HUMAN_TRO_MEK)) {
            critMod += 1;
        }
        vPhaseReport.addAll(gameManager.criticalEntity(entityTarget, hit.getLocation(), hit.isRear(), critMod, damage));
    }
}
