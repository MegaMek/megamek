/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.HitData;
import megamek.common.Game;
import megamek.common.Mech;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 * @since Sept 23, 2004
 */
public class LegAttackHandler extends WeaponHandler {
    private static final long serialVersionUID = 4429993211361286138L;

    public LegAttackHandler(ToHitData toHit, WeaponAttackAction waa, Game g, Server s) {
        super(toHit, waa, g, s);
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
            if (hit.getLocation() == Mech.LOC_RLEG) {
                hit = new HitData(Mech.LOC_LLEG);
            } else {
                hit = new HitData(Mech.LOC_RLEG);
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
        vPhaseReport.addAll(server.damageEntity(entityTarget, hit, damage,
                false, damageType, false, false, throughFront, underWater));
        Report.addNewline(vPhaseReport);
        // Do criticals.
        int critMod = 0;
        if (entityTarget.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HARDENED) {
            critMod -= 2;
        }
        if (ae.hasAbility(OptionsConstants.MISC_HUMAN_TRO,Crew.HUMANTRO_MECH)) {
            critMod += 1;
        }
        vPhaseReport.addAll(server.criticalEntity(entityTarget, hit.getLocation(), hit.isRear(), critMod, damage));
    }
}
