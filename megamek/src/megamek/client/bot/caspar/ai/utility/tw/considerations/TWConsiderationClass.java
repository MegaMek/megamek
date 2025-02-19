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
    BackSide(BackSide.class),
    CoverFire(CoverFire.class),
    CrowdingEnemies(CrowdingEnemies.class),
    CrowdingFriends(CrowdingFriends.class),
    CurrentThreat(CurrentThreat.class),
    DamageOutput(DamageOutput.class),
    DecoyValue(DecoyValue.class),
    ECMCoverage(ECMCoverage.class),
    EnemyECMCoverage(EnemyECMCoverage.class),
    EnemyPositioning(EnemyPositioning.class),
    EnvironmentalCover(EnvironmentalCover.class),
    EnvironmentalHazard(EnvironmentalHazard.class),
    FacingTheEnemy(FacingTheEnemy.class),
    FavoriteTargetInRange(FavoriteTargetInRange.class),
    FireExposure(FireExposure.class),
    FlankingPosition(FlankingPosition.class),
    FormationCohesion(FormationCohesion.class),
    FriendlyArtilleryFire(FriendlyArtilleryFire.class),
    FriendlyPositioning(FriendlyPositioning.class),
    FriendsCoverFire(FriendsCoverFire.class),
    FrontSide(FrontSide.class),
    HeatVulnerability(HeatVulnerability.class),
    IsVIPCloser(IsVIPCloser.class),
    KeepDistance(KeepDistance.class),
    LeftSide(LeftSide.class),
    MyUnitBotSettings(MyUnitBotSettings.class),
    MyUnitHeatManagement(MyUnitHeatManagement.class),
    MyUnitIsCrippled(MyUnitIsCrippled.class),
    MyUnitIsMovingTowardsWaypoint(MyUnitIsMovingTowardsWaypoint.class),
    MyUnitMoved(MyUnitMoved.class),
    MyUnitRoleIs(MyUnitRoleIs.class),
    MyUnitTMM(MyUnitTMM.class),
    MyUnitUnderThreat(MyUnitUnderThreat.class),
    OverallArmor(OverallArmor.class),
    PilotingCaution(PilotingCaution.class),
    Retreat(Retreat.class),
    RightSide(RightSide.class),
    Scouting(Scouting.class),
    StandStill(StandStill.class),
    StrategicGoal(StrategicGoal.class),
    TargetUnitsArmor(TargetUnitsArmor.class),
    TargetWithinOptimalRange(TargetWithinOptimalRange.class),
    TargetWithinRange(TargetWithinRange.class),
    TurnsToEncounter(TurnsToEncounter.class);

    private final Class<? extends Consideration> considerationClass;

    TWConsiderationClass(Class<? extends Consideration> considerationClass) {
        this.considerationClass = considerationClass;
    }

    public Class<? extends Consideration> getConsiderationClass() {
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
