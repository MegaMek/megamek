/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2026 The MegaMek Team. All Rights Reserved.
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
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.server.totalWarfare.TWGameManager;

public class EnergyWeaponHandler extends WeaponHandler {
    @Serial
    private static final long serialVersionUID = 2452514543790235562L;

    /**
     *
     */
    public EnergyWeaponHandler(ToHitData toHit, WeaponAttackAction waa, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(toHit, waa, g, m);
        generalDamageType = HitData.DAMAGE_ENERGY;
    }

    /**
     * Applies Edge to a RISC equipment malfunction (explosion) check: if the attacker has the RISC Edge trigger enabled
     * with Edge remaining, spends one Edge point and rerolls the malfunction check once. The caller re-evaluates its
     * own explosion threshold against the returned value.
     *
     * @param attacker     the firing unit
     * @param subjectId    the report subject id
     * @param reportVector the report vector to append the Edge reports to
     *
     * @return the rerolled 2d6 value if Edge was spent, or -1 if Edge was not used
     */
    // package-private static for testing
    static int rerollRiscMalfunctionWithEdge(Entity attacker, int subjectId, Vector<Report> reportVector) {
        if (!attacker.shouldUseEdge(OptionsConstants.EDGE_WHEN_RISC_FAIL)) {
            return -1;
        }
        attacker.getCrew().decreaseEdge();
        Report report = new Report(3168);
        report.subject = subjectId;
        report.add(attacker.getCrew().getOptions().intOption(OptionsConstants.EDGE));
        reportVector.addElement(report);

        Roll reroll = Compute.rollD6(2);
        report = new Report(3169);
        report.subject = subjectId;
        report.add(reroll);
        reportVector.addElement(report);
        return reroll.getIntValue();
    }

    /**
     * Decides whether a RISC equipment malfunction still occurs after an Edge reroll. The malfunction stands unless
     * Edge was used ({@code edgeReroll >= 0}) and the rerolled value is above the explosion threshold.
     *
     * @param edgeReroll         the rerolled value from {@link #rerollRiscMalfunctionWithEdge}, or -1 if Edge was not
     *                           used
     * @param explosionThreshold the value at or below which the equipment malfunctions
     *
     * @return true if the malfunction (explosion) still occurs
     */
    // package-private static for testing
    static boolean riscStillMalfunctions(int edgeReroll, int explosionThreshold) {
        // A negative reroll means Edge was not used, so the original malfunction stands.
        return (edgeReroll < 0) || (edgeReroll <= explosionThreshold);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        double toReturn = weaponType.getDamage(nRange);

        if ((game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ENERGY_WEAPONS)
              && weapon.hasModes()) || weaponType.hasFlag(WeaponType.F_BOMBAST_LASER)) {
            toReturn = Compute.dialDownDamage(weapon, weaponType, nRange);
        }

        // Apply Gothic Dazzle Mode damage reduction
        if (weapon.curMode().getName().contains("Dazzle")) {
            // TODO: Check if target is Abomination when that system is implemented
            // For now, assume all targets are BattleMechs (not Abominations)
            boolean isAbomination = false; // Stub for future implementation

            if (!isAbomination) {
                // Half damage vs BattleMechs (rounded down, min 1)
                toReturn = Math.max(1, toReturn / 2);
            }
            // Full damage vs Abominations (no change to toReturn)
        }

        // during a swarm, all damage gets applied as one block to one location
        if ((attackingEntity instanceof BattleArmor)
              && (weapon.getLocation() == BattleArmor.LOC_SQUAD)
              && !(weapon.isSquadSupportWeapon())
              && (attackingEntity.getSwarmTargetId() == target.getId())) {
            toReturn *= ((BattleArmor) attackingEntity).getShootingStrength();
        }
        // Check for Altered Damage from Energy Weapons (TacOp, pg.83)
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ALTERNATIVE_DAMAGE)) {
            if (nRange <= 1) {
                toReturn++;
            } else if (nRange > weaponType.getMediumRange() && nRange <= weaponType.getLongRange()) {
                toReturn--;
            }
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE)
              && (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_LONG])) {
            toReturn -= 1;
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE)
              && (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
            toReturn = (int) Math.floor(toReturn * .75);
        }


        if (target.isConventionalInfantry()) {
            toReturn = Compute.directBlowInfantryDamage(
                  toReturn, bDirect ? toHit.getMoS() / 3 : 0,
                  weaponType.getInfantryDamageClass(),
                  ((Infantry) target).isMechanized(),
                  toHit.getThruBldg() != null, attackingEntity.getId(), calcDmgPerHitReport);
        } else if (bDirect) {
            toReturn = Math.min(toReturn + (int) floor(toHit.getMoS() / 3.0), toReturn * 2);
        }

        toReturn = applyGlancingBlowModifier(toReturn, target.isConventionalInfantry());
        return (int) Math.ceil(toReturn);
    }

}
