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
import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.rolls.Roll;
import megamek.common.rules.RulesPhysical;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.TripodMek;

public class TWRulesPhysical extends RulesPhysical {

    /**
     * Do shields boost punch damage? No.
     *
     * @param entity the entity attacking
     * @param armLoc the arm location with the shield
     *
     * @return the damage boost from the shield
     */
    @Override
    public int getShieldDamageBoost(Entity entity, int armLoc) {return 0;}

    /**
     * What is the to-hit modifier for attacking when there is a shield on the arm. Shields make it harder to shoot
     *
     * @param toHit    the to-hit data to modify
     * @param attacker the attacking entity
     * @param weapon   the mounted weapon being used
     */
    @Override
    public void getShieldToHitModifier(ToHitData toHit, Entity attacker, Mounted<?> weapon) {
        // time to check passive defense and no defense
        if (attacker.hasLoweredShield(weapon.getLocation(), weapon.isRearMounted())) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.PassiveShield"));
        } else if (attacker.hasNoDefenseShield(weapon.getLocation())) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Shield"));
        }
    }

    /**
     * Return the claw to-hit modifier.
     *
     * @return the claw to-hit modifier
     */
    @Override
    public int getClawToHitModifier() {return 1;}

    /**
     * Should the shield reset with phase change. No
     *
     * @return true if the shield resets with phase change
     */
    @Override
    public boolean phaseChangeShield() {return false;}

    /**
     * Can retractable blades be used during punch attacks? Only if the TO option is enabled
     *
     * @param toRetractableBlade true if checking for retractable blade use
     *
     * @return true if retractable blades can be used
     */
    @Override
    public boolean retractableBladeArmCheck(boolean toRetractableBlade) {
        if (toRetractableBlade) {return true;}
        return false;
    }

    /**
     * Does a retractable blade break when used during the punch attack. Retractable blades in punch break on a 2d6 roll
     * of 10+
     *
     * @return true if the retractable blade breaks
     */
    @Override
    public boolean checkRetractableBladeBroke() {
        return (Compute.d6(2) > 9);
    }

    /**
     * Missed mace attack triggers a PSR
     *
     * @return true if a missed mace causes a piloting skill roll
     */
    @Override
    public boolean getMaceMissedPSR() {return true;}

    /**
     * What is the target number for a lance to do internal damage? 10+
     *
     * @return the target number
     */
    @Override
    public int getLanceTarget() {return 10;}

    /**
     * {@inheritDoc} lances are +1 to hit
     */
    @Override
    public int getLanceToHitModifier() {
        return 1;
    }

    /**
     * Lance doesn't do anything special on a charge
     *
     * @return true if the lance has special charge effects
     */
    @Override
    public boolean isLanceCharging() {return false;}

    /**
     * Does a shield do anything in a charge? No.
     *
     * @param attackingEntity the entity performing the charge
     *
     * @return the hit data from shield charge damage, or null if no shield damage
     */
    @Override
    public HitData shieldChargeDamage(Entity attackingEntity) {
        return null;
    }

    /**
     * Do the spikes break? Spikes break on a 2d6 roll of 9+
     *
     * @param entity the entity with spikes
     * @param loc    the location being checked
     *
     * @return a report of whether spikes broke
     */
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

    /**
     * Any talons create a damage boost on dfa
     *
     * @param entity the entity to check
     *
     * @return true if the entity has talons
     */
    @Override
    public boolean hasTalons(Entity entity) {
        if (entity instanceof BipedMek || entity instanceof TripodMek) {
            return (entity.hasWorkingMisc(MiscType.F_TALON, null, Mek.LOC_RIGHT_LEG) &&
                  entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_RIGHT_LEG)) ||
                  (entity.hasWorkingMisc(MiscType.F_TALON, null, Mek.LOC_LEFT_LEG) &&
                        entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_LEFT_LEG));
        }
        if (entity instanceof QuadMek) {
            return ((entity.hasWorkingMisc(MiscType.F_TALON, null, Mek.LOC_RIGHT_LEG) &&
                  entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_RIGHT_LEG)) ||
                  (entity.hasWorkingMisc(MiscType.F_TALON, null, Mek.LOC_LEFT_LEG) &&
                        entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_LEFT_LEG)) ||
                  (entity.hasWorkingMisc(MiscType.F_TALON, null, Mek.LOC_RIGHT_ARM) &&
                        (entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_RIGHT_ARM)) ||
                        (entity.hasWorkingMisc(MiscType.F_TALON, null, Mek.LOC_LEFT_ARM) &&
                              entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_LEFT_ARM))));
        }
        return false;
    }

    /**
     * What is the kick modifier? -2 to hit
     *
     * @return the kick modifier
     */
    @Override
    public int getKickModifier() {return -2;}

    /**
     * Do we have a modifier for punching? No, 0 modifier
     *
     * @return the punch modifier
     */
    @Override
    public int getPunchModifier() {return 0;}

    /**
     * What is the damage of the charge?
     *
     * @param entity     the attacking entity
     * @param target     the target entity
     * @param tacOps     true if using tactical operations rules
     * @param mos        the margin of success
     * @param hexesMoved the number of hexes moved in the charge
     *
     * @return the charge damage
     */
    @Override
    public int getChargeDamage(Entity entity, Entity target, boolean tacOps, int mos, int hexesMoved) {
        if (!tacOps) {
            if (hexesMoved == 0) {
                hexesMoved = 1;
            }
            return (int) Math
                  .ceil((entity.getWeight() / 10.0)
                        * (hexesMoved - 1)
                        * (entity.getLocationStatus(1) == ILocationExposureStatus.WET ? 0.5
                        : 1));
        }
        return (int) Math
              .floor(((((target.getWeight() * entity.getWeight()) * hexesMoved) / (target
                    .getWeight()
                    + entity
                    .getWeight()))
                    / 10) +
                    mos);
    }

    /**
     * How much damage does the charge attacker take.
     *
     * @param entity                the attacking entity
     * @param effectiveTargetWeight the effective weight of the target
     * @param tacOps                true if using tactical operations rules
     * @param distance              the distance traveled in the charge
     *
     * @return the damage taken by the attacker
     */
    @Override
    public int getChargeDamageTakenBy(Entity entity, double effectiveTargetWeight, boolean tacOps, int distance) {
        if (!tacOps) {
            return (int) Math
                  .ceil((effectiveTargetWeight / 10.0)
                        * (entity.getLocationStatus(1) == ILocationExposureStatus.WET ? 0.5 : 1));
        } else {
            return (int) Math
                  .floor((((effectiveTargetWeight * entity.getWeight()) * distance)
                        / (effectiveTargetWeight + entity.getWeight())) / 10);
        }
    }

    /**
     * Missed charges, the attacker ends up on one side or the other of the target
     *
     * @param game      the game instance
     * @param entityId  the ID of the charging entity
     * @param src       the source coordinates
     * @param direction the direction of the charge
     *
     * @return the final coordinates after a missed charge
     */
    @Override
    public Coords getMissedChargeDisplacement(Game game, int entityId, Coords src, int direction) {
        Coords first = src.translated((direction + 1) % 6);
        Coords second = src.translated((direction + 5) % 6);
        Hex firstHex = game.getBoard().getHex(first);
        Hex secondHex = game.getBoard().getHex(second);
        Entity entity = game.getEntity(entityId);

        if (entity == null) {
            return null;
        }

        if ((firstHex == null) || (secondHex == null)) {
            // leave it, will be handled
        } else if (entity.elevationOccupied(firstHex) > entity.elevationOccupied(secondHex)) {
            // leave it
        } else if (entity.elevationOccupied(firstHex) < entity.elevationOccupied(secondHex)) {
            // switch
            Coords temp = first;
            first = second;
            second = temp;
        } else if (Compute.d6() > 3) {
            // switch randomly
            Coords temp = first;
            first = second;
            second = temp;
        }

        if (Compute.isValidDisplacement(game, entityId, src,
              src.direction(first))
              && game.getBoard().contains(first)) {
            return first;
        } else if (Compute.isValidDisplacement(game, entityId, src,
              src.direction(second))
              && game.getBoard().contains(second)) {
            return second;
        } else {
            return src;
        }
    }

    /**
     * Can you club a prone target? Prone 'Mechs can only be clubbed if they are one level higher than the attacker See
     * BMM 7th Printing, Physical Attacks and Prone 'Mechs
     *
     * @param targetElevation   the elevation of the target
     * @param attackerElevation the elevation of the attacker
     *
     * @return true if the target cannot be clubbed while prone
     */
    @Override
    public boolean cannotClubProne(int targetElevation, int attackerElevation) {
        if (targetElevation - 1 == attackerElevation) {
            return false;
        }
        return true;
    }

    /**
     * For Charge/DFA, get the pilot difference modifier and ignore immobile.
     *
     * @param attackerPiloting the piloting skill of the attacking pilot
     * @param targetPiloting   the piloting skill of the defending pilot
     * @param immobile         true if one of the entities is immobile
     *
     * @return the pilot difference modifier
     */
    @Override
    public int getPilotDiffModifier(int attackerPiloting, int targetPiloting, boolean immobile) {
        return attackerPiloting - targetPiloting;
    }

    /**
     * Charges cannot be cancelled
     *
     * @return always returns false
     */
    @Override
    public boolean canChargeCancel() {return false;}

    /**
     * Get the right table for falls from above. They always hit the punch table
     *
     * @param affaTarget the entity that is the target of a fall from above
     *
     * @return the hit data for fall from above
     */
    @Override
    public HitData getFallFromAboveTable(Entity affaTarget) {
        return affaTarget.rollHitLocation(ToHitData.HIT_PUNCH,
              ToHitData.SIDE_FRONT);
    }

    /**
     * We can only find clubs in the rubble of medium buildings or higher
     *
     * @return building medium type value
     */
    @Override
    public int getClubFindInRubble() {
        return BuildingType.MEDIUM.getTypeValue();
    }

    @Override
    public boolean quadMuleKickImpossible(int leg, Entity entity) {
        if (!entity.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_LEFT_LEG)
              || !entity.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_RIGHT_LEG)
              || !entity.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_LEFT_ARM)
              || !entity.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_RIGHT_ARM)) {
            return true;
        }
        return false;
    }
}
