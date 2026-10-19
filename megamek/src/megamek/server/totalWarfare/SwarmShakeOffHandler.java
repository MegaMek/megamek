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
package megamek.server.totalWarfare;

import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.units.Entity;

/**
 * Damage to swarming infantry that is shaken loose (TW p.222, Fighting Off Swarm Attacks, as changed by errata v12.0).
 * Conventional infantry take a fixed 3D6. Battle armor take damage on each trooper instead:
 * <ul>
 *     <li>off a jumping unit, 1 point per Jump MP it used;</li>
 *     <li>off a vehicle on the ground, 1 point;</li>
 *     <li>off an airborne VTOL or WiGE, 1 point per elevation it is at.</li>
 * </ul>
 * Swarmers with Jump or VTOL movement take no damage when shaken off an airborne VTOL or WiGE. The same applies when
 * the VTOL or WiGE is destroyed in the air while swarmed. The methods return their reports so callers keep control of
 * report order.
 */
class SwarmShakeOffHandler extends AbstractTWRuleHandler {

    /** The fixed damage conventional infantry take when shaken loose, as a report string. */
    private static final String CONVENTIONAL_INFANTRY_DAMAGE = "3d6";

    /** Report: the swarmer is dislodged and suffers the given damage. */
    private static final int DISLODGED_DAMAGE = 2140;
    /** Report: the battle armor is dislodged and each trooper suffers the given damage. */
    private static final int DISLODGED_DAMAGE_PER_TROOPER = 2141;
    /** Report: the swarmer is dislodged but its jump or VTOL movement saves it from harm. */
    private static final int DISLODGED_UNHARMED = 2142;

    SwarmShakeOffHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * TW p.222 (errata v12.0): the damage each battle armor trooper takes when a vehicle shakes it off. A vehicle on the
     * ground deals 1 point; an airborne VTOL or WiGE deals 1 point for every elevation it is at. Example: a WiGE at
     * elevation 2 deals 2 points to each trooper.
     *
     * @param vehicle the vehicle shaking off its swarmers, at the end of its move
     *
     * @return the damage for each trooper
     */
    static int vehicleShakeOffDamagePerTrooper(Entity vehicle) {
        if (vehicle.isAirborneVTOLorWIGE()) {
            return vehicle.getElevation();
        }
        return 1;
    }

    /**
     * TW p.222 (errata v12.0): swarming infantry with Jump or VTOL movement take no damage when shaken loose from an
     * airborne VTOL or WiGE.
     *
     * @param carrier the swarmed unit
     * @param swarmer the swarming infantry
     *
     * @return {@code true} if the swarmer takes no damage
     */
    static boolean isUnharmedByShakeOff(Entity carrier, Entity swarmer) {
        if (!carrier.isAirborneVTOLorWIGE()) {
            return false;
        }
        return (swarmer.getJumpMP() > 0) || swarmer.getMovementMode().isVTOL();
    }

    /**
     * Damages swarming infantry that has just been shaken loose. The swarm link must already be cleared.
     *
     * @param carrier                     the unit the infantry was swarming
     * @param swarmer                     the dislodged infantry
     * @param battleArmorDamagePerTrooper the damage each battle armor trooper takes; conventional infantry ignore it
     *                                    and take 3D6
     *
     * @return the reports
     */
    Vector<Report> damageDislodgedSwarmer(Entity carrier, Entity swarmer, int battleArmorDamagePerTrooper) {
        Vector<Report> reports = new Vector<>();
        if (isUnharmedByShakeOff(carrier, swarmer)) {
            reports.add(dislodgedReport(DISLODGED_UNHARMED, carrier, swarmer));
            return reports;
        }
        if (swarmer instanceof BattleArmor battleArmor) {
            Report report = dislodgedReport(DISLODGED_DAMAGE_PER_TROOPER, carrier, swarmer);
            report.add(battleArmorDamagePerTrooper);
            reports.add(report);
            if (battleArmorDamagePerTrooper > 0) {
                for (int trooper = BattleArmor.LOC_TROOPER_1; trooper < battleArmor.locations(); trooper++) {
                    if (battleArmor.isTrooperActive(trooper)) {
                        reports.addAll(gameManager.damageEntity(battleArmor, new HitData(trooper),
                              battleArmorDamagePerTrooper));
                    }
                }
            }
            return reports;
        }
        // ASSUMPTION : damage should not be doubled.
        Report report = dislodgedReport(DISLODGED_DAMAGE, carrier, swarmer);
        report.add(CONVENTIONAL_INFANTRY_DAMAGE);
        reports.add(report);
        reports.addAll(gameManager.damageEntity(swarmer,
              swarmer.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT), Compute.d6(3)));
        return reports;
    }

    /**
     * TW p.222 (errata v12.0): a VTOL or WiGE destroyed in the air while swarmed shakes its swarmers loose as if they
     * were knocked off. A unit destroyed on the ground does not harm its swarmers. The swarm link must already be
     * cleared, and the carrier must not yet be marked destroyed (doomed is fine).
     *
     * @param carrier the destroyed unit
     * @param swarmer the infantry that was swarming it
     *
     * @return the reports; empty if the carrier was not an airborne VTOL or WiGE
     */
    Vector<Report> damageSwarmerOfDestroyedUnit(Entity carrier, Entity swarmer) {
        if (!carrier.isAirborneVTOLorWIGE()) {
            return new Vector<>();
        }
        return damageDislodgedSwarmer(carrier, swarmer, carrier.getElevation());
    }

    private static Report dislodgedReport(int messageId, Entity carrier, Entity swarmer) {
        Report report = new Report(messageId);
        report.subject = carrier.getId();
        report.indent();
        report.addDesc(swarmer);
        return report;
    }
}
