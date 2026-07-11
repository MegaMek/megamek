package megamek.common.rules.totalwarfare;
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

import megamek.client.ui.Messages;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.compute.Compute;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.rolls.Roll;
import megamek.common.rules.core.CoreRulesPhysical;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

public class TWRulesPhysical extends CoreRulesPhysical {

    // Shields do not provide punch damage bonus
    @Override
    public int getShieldDamageBoost(Entity entity, int armLoc) { return 0; }

    // Shields make it harder to shoot
    @Override
    public void getShieldToHitModifier(ToHitData toHit, Entity attacker, Mounted<?> weapon) {
        // time to check passive defense and no defense
        if (attacker.hasLoweredShield(weapon.getLocation(), weapon.isRearMounted())) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.PassiveShield"));
        } else if (attacker.hasNoDefenseShield(weapon.getLocation())) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Shield"));
        }
    }

    // Do claws modify the to-hit number
    @Override
    public int getClawToHitModifier() { return 1; }
    
    // Shields stay for round
    @Override
    public boolean phaseChangeShield() { return false; }

    // Retractable blades are only used if TO option is enabled
    @Override
    public boolean retractableBladeArmCheck(boolean toRetractableBlade) {
        if (toRetractableBlade) { return true; }
        return false;
    }

    // Retractable blades in punch break on a 2d6 roll of 9+
    @Override
    public boolean checkRetractableBladeBroke() {
        return (Compute.d6(2) > 9);
    }

    // Missed mace attacks cause a PSR
    @Override
    public boolean getMaceMissedPSR() { return true; }

    // Lance does internal damage on 10+
    @Override
    public int getLanceTarget() { return 10; }

    // Lance doesn't do anything special on a charge
    @Override
    public boolean isLanceCharging() { return false; }

    // Shields do nothing in a charge
    @Override
    public HitData shieldChargeDamage(Entity attackingEntity) {
        return null;
    }

    // Spikes break on a 2d6 roll of 9+
    @Override
    public Report checkBreakSpikes(Entity entity, int loc) {
        Report r;
        Roll diceRoll = Compute.rollD6(2);
        if (diceRoll.getIntValue() < 9) {
            r = new Report(4445);
            r.indent(2);
            r.add(diceRoll);
            r.subject = entity.getId();
        } else {
            r = new Report(4440);
            r.indent(2);
            r.add(diceRoll);
            r.subject = entity.getId();

            for (Mounted<?> m : entity.getMisc()) {
                if (m.getType().hasFlag(MiscType.F_SPIKES) && (m.getLocation() == loc)) {
                    m.setHit(true);
                }
            }
        }
        return r;
    }

    // Any talons create a damage boost on dfa
    @Override
    public boolean hasTalons(Entity entity) {
        if (entity instanceof BipedMek) {
            return (entity.hasWorkingMisc(MiscType.F_TALON, null, Mek.LOC_RIGHT_LEG) &&
                  entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_RIGHT_LEG)) ||
                  (entity.hasWorkingMisc(MiscType.F_TALON, null, Mek.LOC_LEFT_LEG) &&
                        entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_LEFT_LEG));
        }
        return (entity.hasWorkingMisc(MiscType.F_TALON, null, Mek.LOC_RIGHT_LEG) &&
              entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_RIGHT_LEG)) ||
              (entity.hasWorkingMisc(MiscType.F_TALON, null, Mek.LOC_LEFT_LEG) &&
                    entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_LEFT_LEG)) ||
              ((entity.hasWorkingMisc(MiscType.F_TALON, null, Mek.LOC_RIGHT_ARM)) &&
                    (entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_RIGHT_ARM) ||
                          (entity.hasWorkingMisc(MiscType.F_TALON, null, Mek.LOC_LEFT_ARM) &&
                                entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_LEFT_ARM))));
    }

    // Kick is -2 to hit
    @Override
    public int getKickModifier() { return -2; }
}
