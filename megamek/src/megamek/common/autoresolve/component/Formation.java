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
package megamek.common.autoresolve.component;

import megamek.ai.utility.Memory;
import megamek.common.Entity;
import megamek.common.ToHitData;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.ASRange;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFUnit;


public class Formation extends SBFFormation {

    private int targetFormationId = Entity.NONE;
    private EngagementControl engagementControl;
    private boolean engagementControlFailed;
    private boolean unitIsCrippledLatch = false;
    private final Memory memory = new Memory();
    private boolean clanFormation = false;
    private ASDamageVector stdDamage;
    private boolean highStressEpisode = false;
    public Memory getMemory() {
        return memory;
    }

    public EngagementControl getEngagementControl() {
        return engagementControl;
    }

    public void setEngagementControl(EngagementControl engagementControl) {
        this.engagementControl = engagementControl;
    }

    public int getTargetFormationId() {
        return targetFormationId;
    }

    public void setTargetFormationId(int targetFormationId) {
        this.targetFormationId = targetFormationId;
    }

    public void setIsClan(boolean clanFormation) {
        this.clanFormation = clanFormation;
    }

    public boolean isClan() {
        return this.clanFormation;
    }

    public boolean isEngagementControlFailed() {
        return engagementControlFailed;
    }

    public void setEngagementControlFailed(boolean engagementControlFailed) {
        this.engagementControlFailed = engagementControlFailed;
    }

    public boolean isRangeSet(int formationId) {
        return getMemory().get("range." + formationId).isPresent();
    }

    public ASRange getRange(int formationId) {
        return (ASRange) getMemory().get("range." + formationId).orElse(ASRange.LONG);
    }

    public void setRange(int formationId, ASRange range) {
        this.getMemory().put("range." + formationId, range);
    }

    public void setHighStressEpisode() {
        highStressEpisode = true;
    }

    public void reset() {
        targetFormationId = Entity.NONE;
        engagementControl = null;
        highStressEpisode = false;
        getMemory().clear();
        setDone(false);
    }

    public int getCurrentMovement() {
        return getUnits().stream().mapToInt(u -> Math.max(0, u.getMovement() - u.getMpCrits())).min().orElse(0);
    }

    public boolean hadHighStressEpisode() {
        return highStressEpisode;
    }

    /**
     * Checks if the formation is crippled. Rules as described on Interstellar Operations BETA pg 242 - Crippling Damage
     * @return true in case it is crippled
     */
    public boolean isCrippled() {
        if (!unitIsCrippledLatch) {
            var halfOfUnitsDoZeroDamage = getUnits().stream()
                .filter(u -> !u.getCurrentDamage().hasDamage())
                .count() >= Math.ceil(getUnits().size() / 2.0);

            var hasVeryDamagedMovement = (getCurrentMovement() <= 1) && (getMovement() >= 3);

            var totalUnits = getUnits().size();
            var lessThan20PercentOfArmorOrLess = 0;
            for (var units : getUnits()) {
                if (((double) units.getCurrentArmor() / units.getArmor()) <= 0.2) {
                    lessThan20PercentOfArmorOrLess++;
                }
            }

            var halfOfUnitsHaveTwentyPercentOfArmorOrLess = lessThan20PercentOfArmorOrLess >= Math.ceil(totalUnits / 2.0);

            var halfOfUnitsTookTwoTargetDamageOrMore = getUnits().stream()
                .filter(u -> u.getTargetingCrits() >= 2)
                .count() >= Math.ceil(getUnits().size() / 2.0);

            // Sets the latch for crippled variable so it is not recalculated
            unitIsCrippledLatch = hasVeryDamagedMovement
                || halfOfUnitsDoZeroDamage
                || halfOfUnitsHaveTwentyPercentOfArmorOrLess
                || halfOfUnitsTookTwoTargetDamageOrMore;
        }

        return unitIsCrippledLatch;
    }

    public void setStdDamage(ASDamageVector stdDamage) {
        this.stdDamage = stdDamage;
    }

    @Override
    public ASDamageVector getStdDamage() {
        return stdDamage;
    }

    @Override
    public int getSize() {
        if (getUnits().isEmpty()) {
            return 0;
        }
        return getUnits().stream().mapToInt(SBFUnit::getSize).sum() / getUnits().size();
    }

    @Override
    public int getTmm() {
        if (getUnits().isEmpty()) {
            return 0;
        }
        return getUnits().stream().mapToInt(SBFUnit::getTmm).min().orElse(0);
    }

    @Override
    public int getSkill() {
        if (getUnits().isEmpty()) {
            return ToHitData.AUTOMATIC_FAIL;
        }
        return getUnits().stream().mapToInt(SBFUnit::getSkill).sum() / getUnits().size();
    }

    @Override
    public int getTactics() {
        if (getUnits().isEmpty()) {
            return ToHitData.AUTOMATIC_FAIL;
        }
        var movement = getMovement();
        var skill = getSkill();
        return Math.max(0, 6 - movement + skill);
    }

    @Override
    public int getMovement() {
        if (getUnits().isEmpty()) {
            return 0;
        }
        return getUnits().stream().mapToInt(SBFUnit::getMovement).min().orElse(0);
    }

    @Override
    public int getPointValue() {
        return getUnits().stream().mapToInt(SBFUnit::getPointValue).sum();
    }

    @Override
    public String toString() {
        return "[Formation] " + getName() + ": " + getType() + "; SZ" + getSize() + "; TMM" + getTmm() + "; M" + getMovement()
            + "; T" + getTactics() + "; M " + moraleStatus() + "; " + getPointValue() + "@" + getSkill() + "; " + getUnits().size() + " units"
            + "; " + getSpecialAbilities().getSpecialsDisplayString(this);
    }

}
