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

import megamek.common.game.Game;
import megamek.common.rules.RulesEquipment;
import megamek.common.units.Mek;

public class CoreRulesEquipment extends RulesEquipment {
    // AMS can shoot twice. Core P.206
    public boolean getAmsMultiShot() {return true;}

    // AMS can reduce to 0. Core P.206
    public boolean getAMSReduction(boolean toAdvancedAMS) { return true; }

    // Shields are reset at end of phase, unless you are charging. Core p.195
    public boolean phaseChangeShield() {return true;}

    // HD Gyros take 4 hits to destroy. Core p.98
    public int hitsToDestroyGyro(int gyroType) {
        if (gyroType == Mek.GYRO_HEAVY_DUTY) {
            return 4;
        }
        return 2;
    }

    // get the masc failure roll from the escalating chart
    public int getMascFailure(int nLevel) {
        return Game.rulesManager.getRulesCharts().escalatingFailure(nLevel);
    }

    // Blue Shield uses escalating failure for rounds after 6. Core p.207
    public int getBlueShieldTarget(int blueShieldRounds) {
        return Game.rulesManager.getRulesCharts().escalatingFailure(blueShieldRounds - 6);
    }

    // get the radical heat sink
    public int radicalHeatSinkSuccessTarget(int consecutiveRounds) {
        return Game.rulesManager.getRulesCharts().escalatingFailure(consecutiveRounds);
    }
}
