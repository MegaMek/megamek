package megamek.common.rules;

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

import megamek.common.Messages;
import megamek.common.game.Game;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;
import megamek.common.units.MekWithArms;
import megamek.common.units.QuadMek;

import java.util.ArrayList;

/*
This abstract class is to handle rules that are in relation to PSRs. 
 */
public abstract class RulesPSR {
    
    /**
     * Entity calls this when trying to run with damage.
     *
     * @param entity the entity attempting to run with damage
     * @param roll the piloting roll data
     * @param gyroDamage the amount of gyro damage
     * @param overallMoveType the type of overall movement
     * @param distance the distance traveled
     */
    public abstract void checkRunningWithDamage(Entity entity, PilotingRollData roll, int gyroDamage,
          EntityMovementType overallMoveType, int distance);

    /**
     * Any modifier for standing.
     *
     * @param roll the piloting roll data to modify
     */
    public abstract void standing(PilotingRollData roll);

    /**
     * Do we need to change facing when we fall?
     *
     * @param entity the entity that fell
     * @param facing the new facing direction
     */
    public abstract void facingChangeAfterFall(Entity entity, int facing);
    
    /**
     * Apply leg damage modifiers to PSR.
     *
     * @param unit the unit with arms to check
     * @param roll the piloting roll data to modify
     * @param toLegDamage true if applying leg damage
     */
    public abstract void legDamageModifiers(MekWithArms unit, PilotingRollData roll, boolean toLegDamage);

    /**
     * Do we need to reduce potential PSR rolls.
     *
     * @param game the game instance
     * @param entity the entity to check
     */
    public abstract void checkLegActuatorPsrRolls(Game game, Entity entity);

    /**
     * Remove the highest roll from the roll list.
     *
     * @param rollList the list of piloting rolls
     */
    public void rollRemoveHighest(ArrayList<PilotingRollData> rollList) {}
    
    /**
     * PSRs for hit actuators.
     *
     * @param game the game instance
     * @param entity the entity with hit actuators
     * @param loc the location of the hit
     * @param hitPart the part that was hit
     */
    public void hitActuator(final Game game, Entity entity, int loc, int hitPart) {
        String psrText = Game.rulesManager.getRulesCharts().getLocationName(loc,(entity instanceof QuadMek));
        if (hitPart == Mek.ACTUATOR_FOOT) {
            psrText += Messages.getString("ActuatorHits.Foot");
        }
        if (hitPart == Mek.ACTUATOR_HIP) {
            psrText += Messages.getString("ActuatorHits.Hip");
        }
        psrText += Messages.getString("ActuatorHits.Actuator");

        int psrPenalty = 1;

        if (hitPart == Mek.ACTUATOR_HIP) {
            psrPenalty = getHipPenalty();
        }

        if (getFootActuatorPsr() && hitPart == Mek.ACTUATOR_FOOT) {
            game.addPSR(new PilotingRollData(entity.getId(), psrPenalty, psrText, loc));
        } else if (hitPart != Mek.ACTUATOR_FOOT) {
            game.addPSR(new PilotingRollData(entity.getId(), psrPenalty, psrText, loc));
        }
    }
    
    /**
     * Hip Penalties.
     *
     * @return the hip penalty modifier
     */
    public abstract int getHipPenalty();
    
    /**
     * Foot Actuator PSR?
     *
     * @return true if foot actuator damage causes PSR
     */
    public abstract boolean getFootActuatorPsr();

    /**
     * What is the penalty for the gyro.
     *
     * @param gyroHits the number of gyro hits
     * @param gyroType the type of gyro
     * @return the gyro modifier
     */
    public abstract int getGyroModifier(int gyroHits, int gyroType);

    /**
     * What is the penalty for leg destroyed.
     *
     * @return the leg destroyed modifier
     */
    public abstract int getLegDestroyedModifier();

    /**
     * Handle HD Gyro Hits.
     *
     * @param game the game instance
     * @param en the entity with HD gyro
     * @param actualGyroHits the actual number of gyro hits
     */
    public abstract void handleHDGyroHits(Game game, Entity en, int actualGyroHits);

    /**
     * When entering water, do we trigger a PSR.
     *
     * @param overallMoveType the type of overall movement
     * @return true if a PSR is triggered when entering water
     */
    public abstract boolean psrForWaterEntry(EntityMovementType overallMoveType);

    /**
     * What is the PSR mod for a successful DFA.
     *
     * @return the modifier for successful Death from Above
     */
    public abstract int getSuccessfulDFAModifier();

    /**
     * Does a club impact cause a PSR?
     *
     * @param game the game instance
     * @param entity the entity being impacted by a club
     */
    public abstract void clubImpact(Game game, Entity entity);

    /**
     * Special gyro jump modifier.
     *
     * @param gyroHits the number of gyro hits
     * @param gyroType the type of gyro
     * @return the gyro jump modifier
     */
    public abstract int getGyroJumpModifier(final int gyroHits, final int gyroType);

    /**
     * Do you need a PSR for walking with a leg destroyed.
     *
     * @param entity the entity attempting to walk
     * @param overallMoveType the type of overall movement
     * @param hexesMoved the number of hexes moved
     * @return the piloting roll data for the PSR, or null if no PSR required
     */
    public abstract PilotingRollData checkWalkWithLegDestroyed(Entity entity, EntityMovementType overallMoveType,
          int hexesMoved);

    public record FallDamageInWater(int damage, int waterDamage) {}

    /**
     * Calculate the damage a falling unit in water receives
     *
     * @param damage how much damage is it supposed to take
     * @param waterDepth the depth of the water
     * @param fallHeight how big was the fall
     * @param weight how much does the unit weigh
     * @return the modified damage
     */
    public abstract FallDamageInWater reduceFallDamageIntoWater(int damage, int waterDepth, int fallHeight,
          double weight);
}
