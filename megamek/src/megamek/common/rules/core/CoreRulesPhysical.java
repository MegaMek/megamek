package megamek.common.rules.core;
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

import megamek.common.CriticalSlot;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.KickAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.game.Game;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.rolls.Roll;
import megamek.common.rules.RulesPhysical;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

public class CoreRulesPhysical extends RulesPhysical {

    /**
     * {@inheritDoc}
     * Shield rules for damage while punching. Core p.195
     */
    @Override
    public int getShieldDamageBoost(Entity entity, int armLoc) {
        if (entity.hasShield()) {
            for (int slot = 0; slot < entity.getNumberOfCriticalSlots(armLoc); slot++) {
                CriticalSlot cs = entity.getCritical(armLoc, slot);

                if (cs == null) {
                    continue;
                }

                if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                    continue;
                }

                Mounted<?> m = cs.getMount();
                EquipmentType type = m.getType();
                if ((type instanceof MiscType) && ((MiscType) type).isShield()) {
                    if ((((MiscMounted) m).getDamageAbsorption(entity, armLoc) > 0)
                          && (((MiscMounted) m).getCurrentDamageCapacity(entity, armLoc) > 0)) {
                        if (type.hasFlag(MiscTypeFlag.S_SHIELD_LARGE)) {
                            return 3;
                        } else if (type.hasFlag(MiscTypeFlag.S_SHIELD_MEDIUM)) {
                            return 2;
                        } else if (type.hasFlag(MiscTypeFlag.S_SHIELD_SMALL)) {
                            return 1;
                        }
                    } else {
                        // Shield DA or DC is 0, so no bonus
                        return 0;
                    }
                }
            }
        }

        // if there is no shield, or fallback
        return 0;
    }

    /**
     * {@inheritDoc}
     * Core rules shield no longer modifies shooting out
     */
    @Override
    public void getShieldToHitModifier(ToHitData toHit, Entity attacker, Mounted<?> weapon) {}


    /**
     * {@inheritDoc}
     * Claws now have a TN modifier of 0. Core p.194
     */
    @Override
    public int getClawToHitModifier() { return 0; }

    /**
     * {@inheritDoc}
     * Shields reset their state at the end of the phase
     */
    @Override
    public boolean phaseChangeShield() {
        return true;
    }

    /**
     * {@inheritDoc}
     * Retractable blades can be extended for punches. Core p.194
     */
    @Override
    public boolean retractableBladeArmCheck(boolean toRetractableBlade) {
        return true;
    }

    /**
     * {@inheritDoc}
     * Retractable blade breaks if it is used in a punch. Core p.195
     */
    @Override
    public boolean checkRetractableBladeBroke() {
        return (Compute.d6(1) == 1);
    }

    /**
     * {@inheritDoc}
     * Missed mace attacks do not cause PSR Core p.194
     */
    @Override
    public boolean getMaceMissedPSR() {
        return false;
    }

    /**
     * {@inheritDoc}
     * Lance does internal damage on 9+ Core p.194
     */
    @Override
    public int getLanceTarget() { return 9; }

    /**
     * {@inheritDoc}
     * Lance can penetrate on a charge. Core p.194
     */
    @Override
    public boolean isLanceCharging() { return true; }

    /**
     * {@inheritDoc}
     * Charge with shield allocated damage to shield. Core p.195
     */
    @Nullable
    @Override
    public HitData shieldChargeDamage(Entity attackingEntity) {
        int[] armLocations = { Mek.LOC_LEFT_ARM, Mek.LOC_RIGHT_ARM };
        HitData hit;

        for (int armLoc : armLocations) {
            for (MiscMounted m : attackingEntity.getMisc()) {
                MiscType type = m.getType();
                if (m.getLocation() == armLoc
                      && type.isShield()
                      && !m.isInoperable()
                      && m.getCurrentDamageCapacity(attackingEntity, armLoc) > 0
                      && m.curMode().equals(MiscType.S_ACTIVE_SHIELD)) {
                    return new HitData(armLoc);
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * Check if the spikes break on a 1. Core p.196
     */
    @Override
    public Report checkBreakSpikes(Entity entity, int loc) {
        Report r;
        Roll diceRoll = Compute.rollD6(1);
        if (diceRoll.getIntValue() > 1) {
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

    /**
     * {@inheritDoc}
     * Require all limbs to have talons for a DFA damage boost. Core p.196
     */
    @Override
    public boolean hasTalons(Entity entity) {
        if (entity instanceof BipedMek) {
            return (entity.hasWorkingMisc(MiscType.F_TALON, null, Mek.LOC_RIGHT_LEG) &&
                  entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_RIGHT_LEG)) &&
                  (entity.hasWorkingMisc(MiscType.F_TALON, null, Mek.LOC_LEFT_LEG) &&
                        entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_LEFT_LEG));
        }
        int countTalons = 0;
        int[] locs = { Mek.LOC_RIGHT_LEG, Mek.LOC_LEFT_LEG, Mek.LOC_RIGHT_ARM, Mek.LOC_LEFT_ARM, Mek.LOC_CENTER_LEG };
        for (int loc : locs) {
            if ((entity.hasWorkingMisc(MiscType.F_TALON, null, loc) &&
                  entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, loc))) {
                countTalons++;
            }
        }
        if (countTalons >= 2) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * Kicks are -1 to hit. Core p.81
     */
    @Override
    public int getKickModifier() { return -1; }
    
    /**
     * {@inheritDoc}
     * Punches are -1 to hit. Core p.82
     */
    @Override
    public int getPunchModifier() { return -1;}

    /**
     * {@inheritDoc}
     * Charges deal different damage Core p.78
     */
    @Override
    public int getChargeDamage(Entity entity, Entity target, boolean tacOps, int mos, int hexesMoved) {
        return (int) Math
                  .ceil((entity.getWeight() / 5.0)
                        * (Compute.getTargetMovementModifier(hexesMoved, false, false, entity.getGame()).getValue() + 1)
                  * (entity.getLocationStatus(1) == ILocationExposureStatus.WET ? 0.5 : 1));
    }

    /**
     * {@inheritDoc}
     * Attacker damage from a charge. Core p.78
     */
    @Override
    public int getChargeDamageTakenBy(Entity entity, double effectiveTargetWeight, boolean tacOps, int distance) {
        return (int) Math
                  .ceil((effectiveTargetWeight / 10.0)
                        * (entity.getLocationStatus(1) == ILocationExposureStatus.WET ? 0.5 : 1));
    }

    /**
     * {@inheritDoc}
     * Missed charges the attacker remains in place. Core p.78
     */
    @Override
    public Coords getMissedChargeDisplacement(Game game, int entityId, Coords src, int direction) {
        return src;
    }

    /**
     * {@inheritDoc}
     * Can club prone enemies that are elevated or at your feet. Core p.77
     */
    @Override
    public boolean cannotClubProne(int targetElevation, int attackerElevation) {
        if (attackerElevation == targetElevation || attackerElevation == targetElevation - 1) {
            return false;
        }
        return true;
    }
    
    /**
     * {@inheritDoc}
     * Charges and DFAs when attacking an immobile unit use a value of 4 for their piloting. Core p.77
     */
    @Override
    public int getPilotDiffModifier(int attackerPiloting, int targetPiloting, boolean immobile) {
        if (immobile) {
            return attackerPiloting - 4;
        }
        return attackerPiloting - targetPiloting;
    }
    
    /**
     * {@inheritDoc}
     * Can charge cancel under some circumstances. Core p.78
     */
    @Override
    public boolean canChargeCancel() {
        return true;
    }

    /**
     * {@inheritDoc}
     * Falls from above if the target is prone changes table. Core p.115
     */
    @Override
    public HitData getFallFromAboveTable(Entity affaTarget) {
        if (affaTarget.isProne()) {
            return affaTarget.rollHitLocation(ToHitData.HIT_NORMAL,
                  ToHitData.SIDE_FRONT);
        }
        return affaTarget.rollHitLocation(ToHitData.HIT_PUNCH,
              ToHitData.SIDE_FRONT);
    }

    /**
     * {@inheritDoc}
     * We can find clubs in any rubble. Core p.79
     */
    @Override
    public int getClubFindInRubble() {
        return 1;
    }

    /**
     * {@inheritDoc}
     * Core p.238
     */
    @Override
    public boolean quadMuleKickImpossible(int leg, Entity entity) {
        if ((leg == KickAttackAction.LEFT_MULE || leg == KickAttackAction.RIGHT_MULE) &&
              (!entity.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_LEFT_LEG)
                    || !entity.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_RIGHT_LEG))) {
            return true;
        } else if ((leg == KickAttackAction.LEFT || leg == KickAttackAction.RIGHT) &&
              (!entity.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_LEFT_ARM)
                    || !entity.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_RIGHT_ARM))) {
            return true;
        }
        return false;
    }
}
