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

import static java.lang.Math.floor;

import java.io.Serial;
import java.util.Vector;

import megamek.common.CriticalSlot;
import megamek.common.HitData;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.weapons.DamageType;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sept 5, 2005
 */
public class PPCHandler extends EnergyWeaponHandler {
    @Serial
    private static final long serialVersionUID = 5545991061428671743L;
    private int chargedCapacitor = 0;

    public PPCHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
        // remember capacitor state and turn it off here,
        // so a crit in the firing phase does not cause an explosion, per the
        // rules in TO
        if (weapon.hasChargedCapacitor() != 0) {
            if (weapon.hasChargedCapacitor() == 2) {
                chargedCapacitor = 2;
                weapon.getCrossLinkedBy().setMode("Off");
            }
            if (weapon.hasChargedCapacitor() == 1) {
                chargedCapacitor = 1;
                weapon.getLinkedBy().setMode("Off");
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.EnergyWeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        double toReturn = weaponType.getDamage(nRange);

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ENERGY_WEAPONS)
              && weapon.hasModes()) {
            toReturn = Compute.dialDownDamage(weapon, weaponType, nRange);
        }

        if (chargedCapacitor != 0) {
            toReturn += 5;
        }
        // during a swarm, all damage gets applied as one block to one location
        if ((attackingEntity instanceof BattleArmor)
              && (weapon.getLocation() == BattleArmor.LOC_SQUAD)
              && !(weapon.isSquadSupportWeapon())
              && (attackingEntity.getSwarmTargetId() == target.getId())) {
            toReturn *= ((BattleArmor) attackingEntity).getShootingStrength();
        }

        // Check for Altered Damage from Energy Weapons (TacOps, pg.83)
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
        if (game.getOptions().booleanOption(
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE)
              && (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
            toReturn = (int) Math.floor(toReturn * .75);
        }

        if ((target instanceof Entity)
              && ((Entity) target).hasActiveBlueShield()) {
            toReturn = (int) Math.max(Math.floor(toReturn / 2.0), 1);
        }

        if (target.isConventionalInfantry()) {
            toReturn = Compute.directBlowInfantryDamage(toReturn,
                  bDirect ? toHit.getMoS() / 3 : 0,
                  weaponType.getInfantryDamageClass(),
                  ((Infantry) target).isMechanized(),
                  toHit.getThruBldg() != null, attackingEntity.getId(), calcDmgPerHitReport);
        } else if (bDirect) {
            toReturn = Math.min(toReturn + (int) floor(toHit.getMoS() / 3.0), toReturn * 2);
        }

        toReturn = applyGlancingBlowModifier(toReturn, target.isConventionalInfantry());

        return (int) Math.ceil(toReturn);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#doChecks(java.util.Vector)
     */
    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        // Resolve roll for disengaged field inhibitors on PPCs, if needed
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_PPC_INHIBITORS)
              && weapon.hasModes()
              && weapon.curMode().equals("Field Inhibitor OFF")) {
            int rollTarget = 0;
            Roll diceRoll = Compute.rollD6(2);
            int distance = Compute.effectiveDistance(game, attackingEntity, target);

            if (distance >= 3) {
                rollTarget = 3;
            } else if (distance == 2) {
                rollTarget = 6;
            } else if (distance == 1) {
                rollTarget = 10;
            }
            // roll to avoid damage
            Report.addNewline(vPhaseReport);
            Report r = new Report(3175);
            r.subject = subjectId;
            r.indent(2);
            vPhaseReport.addElement(r);
            r = new Report(3180);
            r.subject = subjectId;
            r.indent();
            r.add(rollTarget);
            r.add(diceRoll);

            if (diceRoll.getIntValue() < rollTarget) {
                // Oops, we ruined our day...
                int wLocation = weapon.getLocation();
                weapon.setHit(true);
                // Destroy the first unmarked critical for the PPC
                for (int i = 0; i < attackingEntity.getNumberOfCriticalSlots(wLocation); i++) {
                    CriticalSlot slot1 = attackingEntity.getCritical(wLocation, i);
                    if ((slot1 == null)
                          || (slot1.getType() == CriticalSlot.TYPE_SYSTEM)
                          || slot1.isHit()) {
                        continue;
                    }
                    Mounted<?> mounted = slot1.getMount();
                    if (mounted.equals(weapon)) {
                        slot1.setHit(true);
                        break;
                    }
                }
                // Bug 1066147 : damage is *not* like an ammo explosion,
                // but it *does* get applied directly to the IS.
                r.choose(false);
                r.indent(2);
                vPhaseReport.addElement(r);
                Vector<Report> newReports = gameManager.damageEntity(attackingEntity,
                      new HitData(wLocation), 10, false, DamageType.NONE,
                      true);
                for (Report rep : newReports) {
                    rep.indent(2);
                }
                vPhaseReport.addAll(newReports);
                // Deal 2 damage to the pilot
                vPhaseReport.addAll(gameManager.damageCrew(attackingEntity,
                      2,
                      attackingEntity.getCrew().getCurrentPilotIndex()));
                r = new Report(3185);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            } else {
                r.choose(true);
                vPhaseReport.addElement(r);
            }
        }
        // resolve roll for charged capacitor
        if (chargedCapacitor != 0) {
            if (roll.getIntValue() == 2) {
                Report r = new Report(3178);
                r.subject = attackingEntity.getId();
                r.indent();
                vPhaseReport.add(r);
                // Oops, we ruined our day...
                int wLocation = weapon.getLocation();
                weapon.setHit(true);
                for (int i = 0; i < attackingEntity.getNumberOfCriticalSlots(wLocation); i++) {
                    CriticalSlot slot = attackingEntity.getCritical(wLocation, i);
                    if ((slot == null)
                          || (slot.getType() == CriticalSlot.TYPE_SYSTEM)) {
                        continue;
                    }
                    // Only one Crit needs to be damaged.
                    Mounted<?> mounted = slot.getMount();
                    if (mounted.equals(weapon)) {
                        slot.setDestroyed(true);
                        break;
                    }
                }
            }
        }
        return false;
    }
}
