/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.autoresolve.acar.report;

import megamek.common.Roll;
import megamek.common.TargetRoll;
import megamek.common.autoresolve.component.Formation;
import megamek.common.strategicBattleSystems.SBFUnit;

public class DummyAttackReporter implements IAttackReporter {

    private final static DummyAttackReporter INSTANCE = new DummyAttackReporter();

    private DummyAttackReporter() {
    }

    public static DummyAttackReporter instance() {
        return INSTANCE;
    }

    @Override
    public void reportAttackStart(Formation attacker, int unitNumber, Formation target, SBFUnit targetUnit) {

    }

    @Override
    public void reportCannotSucceed(String toHitDesc) {

    }

    @Override
    public void reportToHitValue(TargetRoll toHitValue) {

    }

    @Override
    public void reportAttackRoll(Roll roll, Formation attacker) {

    }

    @Override
    public void reportAttackMiss() {

    }

    @Override
    public void reportAttackHit() {

    }

    @Override
    public void reportDamageDealt(SBFUnit targetUnit, int damage, int newArmor) {

    }

    @Override
    public void reportStressEpisode() {

    }

    @Override
    public void reportUnitDestroyed() {

    }

    @Override
    public void reportCriticalCheck() {

    }

    @Override
    public void reportNoCrit() {

    }

    @Override
    public void reportTargetingCrit(SBFUnit targetUnit) {

    }

    @Override
    public void reportDamageCrit(SBFUnit targetUnit) {

    }

    @Override
    public void reportUnitCrippled() {

    }
}
