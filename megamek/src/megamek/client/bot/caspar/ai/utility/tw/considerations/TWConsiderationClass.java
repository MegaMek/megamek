/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package megamek.client.bot.caspar.ai.utility.tw.considerations;

import megamek.ai.utility.Consideration;

public enum TWConsiderationClass {
    AttackTactics(AttackTactics.class),
    CaptureEnemyMekWarrior(CaptureEnemyMekWarrior.class),
    CoverFire(CoverFire.class),
    Crowding(Crowding.class),
    DamagedSide(DamagedSide.class),
    DecoyValue(DecoyValue.class),
    DefenseTactics(DefenseTactics.class),
    ECMCoverage(ECMCoverage.class),
    EnemyArtilleryFire(EnemyArtilleryFire.class),
    EnemyPositioning(EnemyPositioning.class),
    EnvironmentalCover(EnvironmentalCover.class),
    EnvironmentalHazard(EnvironmentalHazard.class),
    DamageOutput(DamageOutput.class),
    FacingTheEnemy(FacingTheEnemy.class),
    FavoriteTargetInRange(FavoriteTargetInRange.class),
    IsVIPCloser(IsVIPCloser.class),
    FireExposure(FireExposure.class),
    FlankingPosition(FlankingPosition.class),
    FormationCohesion(FormationCohesion.class),
    FriendlyArtilleryFire(FriendlyArtilleryFire.class),
    FriendsCoverFire(FriendsCoverFire.class),
    HeatVulnerability(HeatVulnerability.class),
    HullDown(HullDown.class),
    KeepDistance(KeepDistance.class),
    KillBox(KillBox.class),
    PilotingCaution(PilotingCaution.class),
    Retreat(Retreat.class),
    Scouting(Scouting.class),
    SensorCoverage(SensorCoverage.class),
    StandStill(StandStill.class),
    StickyMoveType(StickyMoveType.class),
    StrategicGoal(StrategicGoal.class),
    TimeToKill(TimeToKill.class),
    TimeToDie(TimeToDie.class),
    ZombieTactics(ZombieTactics.class),
    MyUnitBotSettings(MyUnitBotSettings.class),
    MyUnitHeatManagement(MyUnitHeatManagement.class),
    MyUnitIsMovingTowardsWaypoint(MyUnitIsMovingTowardsWaypoint.class),
    MyUnitArmor(MyUnitArmor.class),
    MyUnitMoved(MyUnitMoved.class),
    MyUnitTMM(MyUnitTMM.class),
    MyUnitIsCrippled(MyUnitIsCrippled.class),
    MyUnitIsGettingAwayFromDanger(MyUnitIsGettingAwayFromDanger.class),
    MyUnitUnderThreat(MyUnitUnderThreat.class),
    TargetUnitsArmor(TargetUnitsArmor.class),
    MyUnitRoleIs(MyUnitRoleIs.class),
    TargetWithinRange(TargetWithinRange.class),
    TurnsToEncounter(TurnsToEncounter.class),
    TargetWithinOptimalRange(TargetWithinOptimalRange.class);

    private final Class<? extends Consideration<?,?>> considerationClass;

    TWConsiderationClass(Class<? extends Consideration<?,?>> considerationClass) {
        this.considerationClass = considerationClass;
    }

    public Class<? extends Consideration<?,?>> getConsiderationClass() {
        return considerationClass;
    }

    public static TWConsiderationClass fromClass(Class<?> considerationClass) {
        for (TWConsiderationClass twConsiderationClass : values()) {
            if (twConsiderationClass.considerationClass.equals(considerationClass)) {
                return twConsiderationClass;
            }
        }
        return null;
    }
}
