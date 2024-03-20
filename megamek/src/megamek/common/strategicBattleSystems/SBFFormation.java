/*
 * Copyright (c) 2022, 2023 - The MegaMek Team. All Rights Reserved.
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

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.common.Entity;
import megamek.common.ForceAssignable;
import megamek.common.Player;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.ASSpecialAbilityCollection;
import megamek.common.alphaStrike.ASSpecialAbilityCollector;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.force.Force;
import megamek.common.jacksonadapters.SBFFormationDeserializer;
import megamek.common.jacksonadapters.SBFFormationSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static megamek.common.alphaStrike.BattleForceSUA.*;
import static megamek.common.strategicBattleSystems.SBFElementType.AS;
import static megamek.common.strategicBattleSystems.SBFElementType.LA;

/**
 * Represents a Strategic Battle Force Formation composed of one or more SBF Units.
 */
@JsonRootName(value = "SBFFormation")
@JsonSerialize(using = SBFFormationSerializer.class)
@JsonDeserialize(using = SBFFormationDeserializer.class)
public class SBFFormation implements ASSpecialAbilityCollector, BattleForceSUAFormatter, ForceAssignable {

    private List<SBFUnit> units = new ArrayList<>();
    private String name;
    protected SBFElementType type;
    private int size;
    private int tmm;
    private int movement;
    private SBFMovementMode movementMode;
    private int jumpMove;
    private SBFMovementMode trspMovementMode;
    private int trspMovement;
    private int tactics;
    private int morale;
    private int skill;
    private int pointValue;
    private CalculationReport conversionReport = new DummyCalculationReport();
    private final ASSpecialAbilityCollection specialAbilities = new ASSpecialAbilityCollection();

    private String forceString = "";
    private int forceId = Force.NO_FORCE;
    private int id = Entity.NONE;
    private int ownerId = Player.PLAYER_NONE;


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

    public ASSpecialAbilityCollection getSpecialAbilities() {
        return specialAbilities;
    }

    public List<SBFUnit> getUnits() {
        return Collections.unmodifiableList(units);
    }

    public void addUnit(SBFUnit newUnit) {
        units.add(newUnit);
    }

    public void setUnits(List<SBFUnit> units) {
        this.units = units;
    }

    public CalculationReport getConversionReport() {
        return conversionReport;
    }

    public void setConversionReport(CalculationReport report) {
        conversionReport = report;
    }

    public SBFMovementMode getMovementMode() {
        return movementMode;
    }

    public String getMovementCode() {
        return movementMode.code;
    }

    public void setMovementMode(SBFMovementMode mode) {
        movementMode = mode;
    }

    public SBFMovementMode getTrspMovementMode() {
        return trspMovementMode;
    }

    public void setTrspMovementMode(SBFMovementMode mode) {
        trspMovementMode = mode;
    }

    public String getTrspMovementCode() {
        return trspMovementMode.code;
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

    @Override
    public boolean isUnitGroup() {
        return true;
    }

    @Override
    public String generalName() {
        return name;
    }

    @Override
    public String specificName() {
        return "";
    }

    /**
     * Returns true if this SBF Formation represents an aerospace Team.
     */
    @Override
    public boolean isAerospace() {
        return isAnyTypeOf(AS, LA);
    }

    /**
     * Returns true if this SBF Formation is of the given type.
     */
    public boolean isType(SBFElementType tp) {
        return type == tp;
    }

    /**
     * Returns true if this SBF Formation is any of the given types.
     */
    public boolean isAnyTypeOf(SBFElementType type, SBFElementType... types) {
        return isType(type) || Arrays.stream(types).anyMatch(this::isType);
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
    public String getSpecialsDisplayString(String delimiter, BattleForceSUAFormatter element) {
        return specialAbilities.getSpecialsDisplayString(delimiter, element);
    }

    @Override
    public String formatSUA(BattleForceSUA sua, String delimiter, ASSpecialAbilityCollector collection) {
        return formatAbility(sua);
    }

    /**
     * Creates the formatted SPA string for the given spa. For turrets this includes everything in that
     * turret. The given collection can be the specials of the AlphaStrikeElement itself, a turret or
     * an arc of a large aerospace unit.
     *
     * @param sua The Special Unit Ability to process
     * @return The complete formatted Special Unit Ability string such as "LRM1/1/-" or "CK15D2".
     */
    private String formatAbility(BattleForceSUA sua) {
        if (!specialAbilities.hasSUA(sua)) {
            return "";
        }
        Object suaObject = specialAbilities.getSUA(sua);
        if (!sua.isValidAbilityObject(suaObject)) {
            return "ERROR - wrong ability object (" + sua + ")";
        } else if (sua.isAnyOf(CAP, SCAP, MSL)) {
            return sua.toString();
        } else if (sua == FLK) {
            ASDamageVector flkDamage = specialAbilities.getFLK();
            return sua.toString() + flkDamage.M.damage + "/" + flkDamage.L.damage;
        } else if (sua.isTransport()) {
            String result = sua + suaObject.toString();
            BattleForceSUA door = sua.getDoor();
            if (isType(SBFElementType.LA)
                    && specialAbilities.hasSUA(door) && ((int) specialAbilities.getSUA(door) > 0)) {
                result += door.toString() + specialAbilities.getSUA(door);
            }
            return result;
        } else {
            return sua.toString() + (suaObject != null ? suaObject : "");
        }
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getOwnerId() {
        return ownerId;
    }

    @Override
    public int getStrength() {
        return getPointValue();
    }

    @Override
    public String getForceString() {
        return forceString;
    }

    @Override
    public void setForceString(String newForceString) {
        forceString = newForceString;
    }

    @Override
    public int getForceId() {
        return forceId;
    }

    @Override
    public void setForceId(int newId) {
        forceId = newId;
    }

    @Override
    public String toString() {
        return "[SBFFormation] " + name + ": " + type + "; SZ" + size + "; TMM" + tmm + "; MV" + movement + movementMode.code
                + (jumpMove > 0 ? "/" + jumpMove + "j" : "")
                + (trspMovement != movement || trspMovementMode != movementMode ? "; TRSP" + trspMovement + trspMovementMode.code : "")
                + "; T" + tactics + "; M" + morale + "; " + pointValue + "@" + skill + "; " + units.size() + " units"
                + "; " + specialAbilities.getSpecialsDisplayString(this);
    }
}