/*
 *
 *  * Copyright (c) 26.09.21, 11:14 - The MegaMek Team. All Rights Reserved.
 *  *
 *  * This file is part of MegaMek.
 *  *
 *  * MegaMek is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * MegaMek is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package megamek.common.strategicBattleSystems;

import megamek.common.alphaStrike.*;
import megamek.common.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static megamek.common.alphaStrike.BattleForceSUA.*;
import static megamek.common.strategicBattleSystems.SBFElementType.AS;
import static megamek.common.strategicBattleSystems.SBFElementType.LA;

public class SBFFormation {
    
    private List<SBFUnit> units = new ArrayList<>();
    private String name;
    protected SBFElementType type;
    private int size;
    private int tmm;
    private int movement;
    private String moveMode;
    private int jumpMove;
    private int trspMovement;
    private int tactics;
    private int morale;
    private int skill;
    private int pointValue;
    private EnumMap<BattleForceSUA, Object> specialAbilities = new EnumMap<>(BattleForceSUA.class);


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
    public int getJumpMove() {
        return jumpMove;
    }
    public void setJumpMove(int jumpMove) {
        this.jumpMove = jumpMove;
    }
    public int getTrspMovement() {
        return trspMovement;
    }
    public void setTrspMovement(int trspMovement) {
        this.trspMovement = trspMovement;
    }
    public int getMovement() {
        return movement;
    }
    public void setMovement(int movement) {
        this.movement = movement;
    }
    public int getTactics() {
        return tactics;
    }
    public void setTactics(int tactics) {
        this.tactics = tactics;
    }
    public int getMorale() {
        return morale;
    }
    public void setMorale(int morale) {
        this.morale = morale;
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
    public EnumMap<BattleForceSUA, Object> getSpecialAbilities() {
        return specialAbilities;
    }
    public void setSpecialAbilities(EnumMap<BattleForceSUA, Object> specialAbilities) {
        this.specialAbilities = specialAbilities;
    }
    public List<SBFUnit> getUnits() {
        return units;
    }
    public void setUnits(List<SBFUnit> units) {
        this.units = units;
    }

    /**
     * NEW version - Adds a Special Unit Ability that is not associated with any
     * additional information or number, e.g. RCN.
     */
    public void addSPA(BattleForceSUA spa) {
        specialAbilities.put(spa, null);
    }

    /**
     * NEW version - Adds a Special Unit Ability associated with an integer number such as C3M#. If
     * that SPA is already present, the given number is added to the one already present. If the present
     * number is a Double type value, that type is preserved.
     */
    public void addSPA(BattleForceSUA spa, int number) {
        if (!specialAbilities.containsKey(spa)) {
            specialAbilities.put(spa, number);
        } else {
            if (specialAbilities.get(spa) instanceof Integer) {
                specialAbilities.put(spa, (int) specialAbilities.get(spa) + number);
            } else if (specialAbilities.get(spa) instanceof Double) {
                specialAbilities.put(spa, (double) specialAbilities.get(spa) + number);
            }
        }
    }

    /**
     * NEW version - Adds a Special Unit Ability associated with a possibly non-integer number such
     * as MHQ2. If that SPA is already present, the given number is added to the one already present.
     * if the previosly present number was an integer, it will be converted to a Double type value.
     */
    public void addSPA(BattleForceSUA spa, double number) {
        if (!specialAbilities.containsKey(spa)) {
            specialAbilities.put(spa, number);
        } else {
            if (specialAbilities.get(spa) instanceof Integer) {
                specialAbilities.put(spa, (int)specialAbilities.get(spa) + number);
            } else if (specialAbilities.get(spa) instanceof Double) {
                specialAbilities.put(spa, (double)specialAbilities.get(spa) + number);
            }
        }
    }

    /**
     * NEW version - Replaces the value associated with a Special Unit Ability with the given Object.
     * The previously present associated Object, if any, is discarded. If the ability was not present,
     * it is added.
     */
    public void replaceSPA(BattleForceSUA spa, Object newValue) {
        specialAbilities.put(spa, newValue);
    }

    /**
     * NEW version - Adds a Special Unit Ability associated with a single damage value such as IF2. If
     * that SPA is already present, the new damage value replaces the former.
     */
    public void addSPA(BattleForceSUA spa, ASDamage damage) {
        specialAbilities.put(spa, damage);
    }

    /**
     * NEW version - Adds a Special Unit Ability associated with a full damage vector such as LRM1/2/2. If
     * that SPA is already present, the new damage value replaces the former.
     */
    public void addSPA(BattleForceSUA spa, ASDamageVector damage) {
        specialAbilities.put(spa, damage);
    }

    /**
     * NEW version - Adds a Special Unit Ability associated with a whole ASArcSummary such as TUR. If
     * that SPA is already present, the new value replaces the former.
     */
    public void addSPA(BattleForceSUA spa, ASSpecialAbilityCollection value) {
        specialAbilities.put(spa, value);
    }

    /** NEW version - Adds the TUR Special Unit Ability with a List<List<Object>>. */
    public void addTurSPA(List<List<Object>> turAbility) {
        specialAbilities.put(TUR, turAbility);
    }

    /** NEW version - Adds the LAM Special Unit Ability with a LAM movement map. */
    public void addLamSPA(Map<String, Integer> specialMoves) {
        specialAbilities.put(LAM, specialMoves);
    }

    /** NEW version - Adds the BIM Special Unit Ability with a LAM movement map. */
    public void addBimSPA(Map<String, Integer> specialMoves) {
        specialAbilities.put(BIM, specialMoves);
    }

    public Object getSPA(BattleForceSUA spa) {
        return specialAbilities.get(spa);
    }

    public boolean hasSPA(BattleForceSUA spa) {
        return specialAbilities.containsKey(spa);
    }

    public String getSpecialsString() {
        return specialAbilities.keySet().stream()
                .filter(this::showSpecial)
                .map(spa -> formatSPAString(spa, getSPA(spa)))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(","));
    }

    public boolean showSpecial(BattleForceSUA spa) {
        //TODO
        if ((type == SBFElementType.BM) && (spa == SOA || spa == SRCH)) {
            return false;
        }
        if ((type == SBFElementType.V) && (spa == SRCH)) {
            return false;
        }
        return true;
    }

    public static String formatSPAString(BattleForceSUA spa, @Nullable Object spaObject) {
        if (spa == TUR) {
            return "TUR(" + spaObject + ")";
//        } else if (spa == BIM || spa == LAM) {
//            return lamString(spa, spaObject);
        } else if ((spa == C3BSS) || (spa == C3M) || (spa == C3BSM) || (spa == C3EM)
                || (spa == INARC) || (spa == CNARC) || (spa == SNARC)) {
            return spa.toString() + ((int) spaObject == 1 ? "" : (int) spaObject);
        } else {
            return spa.toString() + (spaObject != null ? spaObject : "");
        }
    }


    /**
     * Returns the Artillery Special's SBF damage (standard, not homing missile damage).
     * Returns 0 when the given SPA is not an Artillery SPA.
     */
    public static int getSbfArtilleryDamage(BattleForceSUA spa) {
        switch (spa) {
            case ARTTC:
                return 1;
            case ARTT:
            case ARTBA:
            case ARTSC:
                return 2;
            case ARTAIS:
            case ARTAC:
            case ARTS:
            case ARTLTC:
                return 3;
            case ARTLT:
                return 6;
            case ARTCM5:
                return 8;
            case ARTCM7:
                return 13;
            case ARTCM9:
                return 22;
            case ARTCM12:
                return 36;
            default:
                return 0;
        }
    }

    /**
     * Returns the Artillery Special's SBF damage for homing missiles.
     * Returns 0 when the given SPA is not ARTAIS or ARTAC.
     */
    public static int getSbfArtilleryHomingDamage(BattleForceSUA spa) {
        return spa.isAnyOf(ARTAIS, ARTAC) ? 2 : 0;
    }

    /** Returns true if this SBF Formation represents a ground Team. */
    public boolean isGround() {
        return !isAerospace();
    }

    /** Returns true if this SBF Formation represents an aerospace Team. */
    public boolean isAerospace() {
        return isAnyTypeOf(AS, LA);
    }

    /** Returns true if this SBF Formation is of the given type. */
    public boolean isType(SBFElementType tp) {
        return type == tp;
    }

    /** Returns true if this SBF Formation is any of the given types. */
    public boolean isAnyTypeOf(SBFElementType type, SBFElementType... types) {
        return isType(type) || Arrays.stream(types).anyMatch(this::isType);
    }
}
