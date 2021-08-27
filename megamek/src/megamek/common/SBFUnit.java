package megamek.common;

import java.util.EnumMap;
import java.util.Map;


public class SBFUnit {
    
    private SBFElementType type;
    private int size;
    private int tmm;
    private int movement;
    private String moveType;
    private int jumpMove;
    private int armor;
    private int skill;
    private ASDamageVector damage;
    private int pointValue;
    private EnumMap<BattleForceSPA, Object> specialAbilities = new EnumMap<>(BattleForceSPA.class);
    
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
    public String getMoveType() {
        return moveType;
    }
    public void setMoveType(String moveType) {
        this.moveType = moveType;
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
    public EnumMap<BattleForceSPA, Object> getSpecialAbilities() {
        return specialAbilities;
    }
    public void setSpecialAbilities(EnumMap<BattleForceSPA, Object> specialAbilities) {
        this.specialAbilities = specialAbilities;
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
    
    

}
