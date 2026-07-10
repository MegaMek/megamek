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

import megamek.common.board.Coords;

import java.util.ArrayList;

public abstract class RulesEquipment {
    // Does AMS allow for multiple shots
    public abstract boolean getAMSMultiShot();

    // Can AMS reduce the value to 0
    public abstract boolean getAMSReduction(boolean toAdvancedAMS);

    // Does AMS shoot down a single missile/pod
    public abstract boolean checkAMSSingleMissile(int roll);

    // How many hits destroy the gyro
    public abstract int hitsToDestroyGyro( int gyroType);

    // What is the number for masc failure (also used for supercharger)
    public abstract int getMascFailure(int nLevel);

    // What is the target number for Blue shield
    public abstract int getBlueShieldTarget(int blueShieldRounds);

    // What is the target number for radical heat sink
    public abstract int radicalHeatSinkSuccessTarget(int consecutiveRounds);

    // What hexes are affected by ECM
    public abstract ArrayList<Coords> getECMCoordsAffected(final Coords a, final Coords b);
}
