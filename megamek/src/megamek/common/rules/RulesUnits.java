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

import megamek.common.rolls.PilotingRollData;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

public abstract class RulesUnits {
    /**
     * Any modifier for a mule kick.
     *
     * @return the mule kick modifier
     */
    public abstract int getMuleKickModifier();

    /**
     * Does removing legs cause it to be immobile?
     *
     * @param mek the MEK to check
     * @return true if leg removal causes immobile status
     */
    public abstract boolean getDoesLegDestructionCauseImmobile(Mek mek);

    /**
     * Reduce a quad's walk MP for legs destroyed, hip hits, and actuator hits.
     *
     * @param mp the base movement points
     * @param legsDestroyed the number of legs destroyed
     * @param hipHits the number of hip hits
     * @param actuatorHits the number of actuator hits
     * @param bTOLegDamage true if using tactical operations leg damage rules
     * @return the reduced movement points
     */
    public abstract int reduceQuadWalkMP(int mp, int legsDestroyed, int hipHits, int actuatorHits,
          boolean bTOLegDamage);

    /**
     * What modifiers to we add for legs destroyed on a quad.
     *
     * @param destroyedLegs the number of destroyed legs
     * @param roll the piloting roll data to modify
     */
    public abstract void quadPilotModForLegsDestroyed(int destroyedLegs, PilotingRollData roll);

    /**
     * Reduce MP for a mek with hip hits.
     *
     * @param hipHits the number of hip hits
     * @param bTOLegDamage true if using tactical operations leg damage rules
     * @param mp the base movement points
     * @return the reduced movement points
     */
    public abstract int getMekMPReduction(int hipHits, boolean bTOLegDamage, int mp);

    /**
     * Is there a limit to how much we can reduce MP?
     *
     * @param mp the movement points
     * @return the minimum allowed movement points
     */
    public abstract int getMinimumMP(int mp);

    /**
     * Does the unit have bad legs?
     *
     * @param entity the entity to check
     * @return true if the unit has bad legs
     */
    public abstract boolean hasBadLegs(Entity entity);

    /**
     * Are torso twists allowed in the physical phase?
     * @return false by default
     */
    public boolean getPhysicalTwistEnabled() { return false;
    }
}
