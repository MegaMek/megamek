/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.strategicBattleSystems;

import megamek.common.alphaStrike.*;

import java.util.Arrays;

import static megamek.common.alphaStrike.BattleForceSUA.SOA;
import static megamek.common.alphaStrike.BattleForceSUA.SRCH;

/**
 * Represents an SBF Unit (Ground SBF Unit or Aerospace Flight) which contains between 1 and 6 AlphaStrike
 * elements and is the building block of SBF Formations.
 */
public class SBFUnit implements ASSpecialAbilityCollector {

    private String name = "Unknown";
    private SBFElementType type = SBFElementType.UNKNOWN;
    private int size = 0;
    private int tmm = 0;
    private int movement = 0;
    private String movementMode = "";
    private int jumpMove = 0;
    private int trspMovement = 0;
    private int armor = 0;
    private int skill = 4;
    private ASDamageVector damage = ASDamageVector.ZERO;
    private int pointValue = 0;
    private final SBFSpecialAbilityCollection specialAbilities = new SBFSpecialAbilityCollection();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SBFElementType getType() {
        return type;
    }

    public void setType(SBFElementType type) {
        this.type = type;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTmm() {
        return tmm;
    }

    public void setTmm(int tmm) {
        this.tmm = tmm;
    }

    public int getMovement() {
        return movement;
    }

    public void setMovement(int movement) {
        this.movement = movement;
    }

    public int getTrspMovement() {
        return trspMovement;
    }

    public void setTrspMovement(int trspMovement) {
        this.trspMovement = trspMovement;
    }

    public String getMovementMode() {
        return movementMode;
    }

    public void setMovementMode(String movementMode) {
        this.movementMode = movementMode;
    }

    public int getJumpMove() {
        return jumpMove;
    }

    public void setJumpMove(int jumpMove) {
        this.jumpMove = jumpMove;
    }

    public int getSkill() {
        return skill;
    }

    public void setSkill(int skill) {
        this.skill = skill;
    }

    public int getPointValue() {
        return pointValue;
    }

    public void setPointValue(int pointValue) {
        this.pointValue = pointValue;
    }

    public int getArmor() {
        return armor;
    }

    public void setArmor(int armor) {
        this.armor = armor;
    }

    public ASDamageVector getDamage() {
        return damage;
    }

    public void setDamage(ASDamageVector damage) {
        this.damage = damage;
    }

    public SBFSpecialAbilityCollection getSpecialAbilities() {
        return specialAbilities;
    }

    public boolean showSpecial(BattleForceSUA spa) {
        if ((type == SBFElementType.BM) && (spa == SOA || spa == SRCH)) {
            return false;
        }
        if ((type == SBFElementType.V) && (spa == SRCH)) {
            return false;
        }
        return true;
    }

    /** Returns true if this SBF Unit is of the given type. */
    public boolean isType(SBFElementType tp) {
        return type == tp;
    }

    /** Returns true if this SBF Unit is any of the given types. */
    public boolean isAnyTypeOf(SBFElementType type, SBFElementType... types) {
        return isType(type) || Arrays.stream(types).anyMatch(this::isType);
    }

    /** Returns true if this SBF Unit represents a ground Unit. */
    public boolean isGround() {
        return !isAerospace();
    }

    /** Returns true if this SBF Unit represents an aerospace Unit. */
    public boolean isAerospace() {
        return type.isAerospace();
    }

    @Override
    public boolean hasSUA(BattleForceSUA sua) {
        return specialAbilities.hasSUA(sua);
    }

    @Override
    public Object getSUA(BattleForceSUA sua) {
        return specialAbilities.getSUA(sua);
    }

    @Override
    public String getSpecialsDisplayString(String delimiter, ASCardDisplayable element) {
        return specialAbilities.getSpecialsDisplayString(delimiter, this);
    }
}