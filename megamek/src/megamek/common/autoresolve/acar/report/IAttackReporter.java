/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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


package megamek.common.autoresolve.acar.report;

import megamek.common.Roll;
import megamek.common.TargetRoll;
import megamek.common.autoresolve.component.Formation;
import megamek.common.strategicBattleSystems.SBFUnit;

public interface IAttackReporter {
    void reportAttackStart(Formation attacker, int unitNumber, Formation target, SBFUnit targetUnit);

    void reportCannotSucceed(String toHitDesc);

    void reportToHitValue(TargetRoll toHitValue);

    void reportAttackRoll(Roll roll, Formation attacker);

    void reportAttackMiss();

    void reportAttackHit();

    void reportDamageDealt(SBFUnit targetUnit, int damage, int newArmor);

    void reportStressEpisode();

    void reportUnitDestroyed();

    void reportCriticalCheck();

    void reportNoCrit();

    void reportTargetingCrit(SBFUnit targetUnit);

    void reportDamageCrit(SBFUnit targetUnit);

    void reportUnitCrippled();
}
