/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
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
