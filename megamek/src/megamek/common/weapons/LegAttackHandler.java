/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons;

import java.util.Vector;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sept 23, 2004
 */
public class LegAttackHandler extends WeaponHandler {
    private static final long serialVersionUID = 4429993211361286138L;

    public LegAttackHandler(ToHitData toHit, WeaponAttackAction waa, Game g, TWGameManager m) {
        super(toHit, waa, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        return 1;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget,
          Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
          int bldgAbsorbs) {
        HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(),
              toHit.getSideTable(), waa.getAimedLocation(),
              waa.getAimingMode(), toHit.getCover());
        hit.setAttackerId(getAttackerId());
        // If a leg attacks hit a leg that isn't
        // there, then hit the other leg.
        if (entityTarget.getInternal(hit) <= 0) {
            if (hit.getLocation() == Mek.LOC_RLEG) {
                hit = new HitData(Mek.LOC_LLEG);
            } else {
                hit = new HitData(Mek.LOC_RLEG);
            }
        }
        hit.setGeneralDamageType(generalDamageType);

        Report r = new Report(3405);
        r.subject = subjectId;
        r.add(toHit.getTableDesc());
        r.add(entityTarget.getLocationAbbr(hit));
        vPhaseReport.addElement(r);

        int damage = 4;
        if (ae instanceof BattleArmor) {
            damage += ((BattleArmor) ae).getVibroClaws();
            if (((BattleArmor) ae).hasMyomerBooster()) {
                damage += ((BattleArmor) ae).getTroopers() * 2;
            }
        }

        // ASSUMPTION: buildings CAN'T absorb *this* damage.
        vPhaseReport.addAll(gameManager.damageEntity(entityTarget, hit, damage,
              false, damageType, false, false, throughFront, underWater));
        Report.addNewline(vPhaseReport);
        // Do criticals.
        int critMod = 0;
        if (entityTarget.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HARDENED) {
            critMod -= 2;
        }
        if (ae.hasAbility(OptionsConstants.MISC_HUMAN_TRO, Crew.HUMANTRO_MEK)) {
            critMod += 1;
        }
        vPhaseReport.addAll(gameManager.criticalEntity(entityTarget, hit.getLocation(), hit.isRear(), critMod, damage));
    }
}
