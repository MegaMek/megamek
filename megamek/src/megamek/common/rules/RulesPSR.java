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

import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.MekWithArms;

import java.util.ArrayList;

/*
This abstract class is to handle rules that are in relation to PSRs. 
 */
public abstract class RulesPSR {
    
    // Entity calls this when trying to run with damage
    public abstract void checkRunningWithDamage(Entity entity, PilotingRollData roll, int gyroDamage,
          EntityMovementType overallMoveType, int distance);

    // Any modifier for standing
    public abstract void standing(PilotingRollData roll);

    // Do we need to change facing when we fall?
    public abstract void facingChangeAfterFall(Entity entity, int facing);
    
    // Apply leg damage modifiers to PSR
    public abstract void legDamageModifiers(MekWithArms unit, PilotingRollData roll, boolean toLegDamage);

    // Do we need to reduce potential PSR rolls
    public abstract void checkLegActuatorPsrRolls(Game game, Entity entity);

    // Remove the highest roll from the roll list
    public abstract void rollRemoveHighest(ArrayList<PilotingRollData> rollList);
    
    // PSRs for hit actuators
    public abstract void hitActuator(Game game, Entity entity, int loc, int hitPart);
    
    // Hip Penalties
    public abstract int getHipPenalty();
    
    // Foot Actuator PSR?
    public abstract boolean getFootActuatorPsr();

    // What is the penalty got the gyro
    public abstract int getGyroModifier(int gyroHits, int gyroType);

    // What is the penalty for leg destroyed
    public abstract int getLegDestroyedModifier();

    // Handle HD Gyro Hits
    public abstract void handleHDGyroHits(Game game, Entity en, int actualGyroHits);

    // When entering water, do we trigger a PSR
    public abstract boolean psrForWaterEntry(EntityMovementType overallMoveType);

    // What is the PSR mod for a successful DFA
    public abstract int getSuccessfulDFAModifier();
}
