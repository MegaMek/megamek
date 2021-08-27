package megamek.common;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SBFFormation {
    
    private List<SBFUnit> units = new ArrayList<>(); 
    private SBFElementType type;
    private int size;
    private int tmm;
    private Map<String, Integer> movement;
    private int tactics;
    private int morale;
    private int skill;
    private int pointValue;
    private EnumMap<BattleForceSPA, Integer> specialAbilities = new EnumMap<>(BattleForceSPA.class);
    
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
    public Map<String, Integer> getMovement() {
        return movement;
    }
    public void setMovement(Map<String, Integer> movement) {
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
    public EnumMap<BattleForceSPA, Integer> getSpecialAbilities() {
        return specialAbilities;
    }
    public void setSpecialAbilities(EnumMap<BattleForceSPA, Integer> specialAbilities) {
        this.specialAbilities = specialAbilities;
    }
    public List<SBFUnit> getUnits() {
        return units;
    }
    public void setUnits(List<SBFUnit> units) {
        this.units = units;
    }
    
    
}
