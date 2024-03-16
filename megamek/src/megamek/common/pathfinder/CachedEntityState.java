package megamek.common.pathfinder;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import megamek.common.*;

/**
 * A transient class used to lazy-load "calculated" information from an entity
 * 
 */
public class CachedEntityState {
    private Entity backingEntity;
    
    private Integer walkMP;
    private Integer runMP;
    private Integer runMPWithOneMasc;
    private Integer runMPWithoutMasc;
    private Integer runMPNoGravity;
    private Integer sprintMP;
    private Integer sprintMPWithOneMasc;
    private Integer sprintMPWithoutMasc;
    private Integer jumpMP;
    private Integer jumpMPWithTerrain;
    private Map<BigInteger, Boolean> hasWorkingMisc;
    private Integer torsoJumpJets;
    private Integer jumpMPNoGravity;
    private Integer numBreachedLegs;
    
    public CachedEntityState(Entity entity) {
        backingEntity = entity;
        hasWorkingMisc = new HashMap<>();
    }
    
    public int getWalkMP() {
        if (walkMP == null) {
            walkMP = backingEntity.getWalkMP();
        }
        
        return walkMP;
    }
    
    public int getRunMP() {
        if (runMP == null) {
            runMP = backingEntity.getRunMP();
        }
        
        return runMP;
    }
    
    public int getRunMPwithoutMASC() {
        if (runMPWithoutMasc == null) {
            runMPWithoutMasc = backingEntity.getRunMPwithoutMASC();
        }
        
        return runMPWithoutMasc;
    }

    public int getRunMPwithOneMASC() {
        if (runMPWithOneMasc == null) {
            runMPWithOneMasc = backingEntity.getRunMP(MPCalculationSetting.ONE_MASC);
        }

        return runMPWithOneMasc;
    }
    
    public int getSprintMP() {
        if (sprintMP == null) {
            sprintMP = backingEntity.getSprintMP();
        }
        
        return sprintMP;
    }

    public int getSprintMPwithOneMASC() {
        if (sprintMPWithOneMasc == null) {
            sprintMPWithOneMasc = backingEntity.getSprintMPwithOneMASC();
        }

        return sprintMPWithOneMasc;
    }

    public int getSprintMPwithoutMASC() {
        if (sprintMPWithoutMasc == null) {
            sprintMPWithoutMasc = backingEntity.getSprintMPwithoutMASC();
        }
        
        return sprintMPWithoutMasc;
    }
    
    public int getJumpMP() {
        if (jumpMP == null) {
            jumpMP = backingEntity.getJumpMP();
        }
        
        return jumpMP;
    }
    
    public int getJumpMPWithTerrain() {
        if (jumpMPWithTerrain == null) {
            jumpMPWithTerrain = backingEntity.getJumpMPWithTerrain();
        }
        
        return jumpMPWithTerrain;
    }
    
    public boolean hasWorkingMisc(BigInteger flag) {
        if (!hasWorkingMisc.containsKey(flag)) {
            hasWorkingMisc.put(flag, backingEntity.hasWorkingMisc(flag));
        }
        
        return hasWorkingMisc.get(flag);
    }
    
    public int getTorsoJumpJets() {
        if (torsoJumpJets == null) {
            if (backingEntity instanceof Mech) {
                torsoJumpJets = ((Mech) backingEntity).torsoJumpJets();
            } else {
                torsoJumpJets = 0;
            }
        }
        
        return torsoJumpJets;
    }
    
    public int getJumpMPNoGravity() {
        if (jumpMPNoGravity == null) {
            jumpMPNoGravity = backingEntity.getJumpMP(MPCalculationSetting.NO_GRAVITY);
        }
        
        return jumpMPNoGravity;
    }
    
    public int getRunMPNoGravity() {
        if (runMPNoGravity == null) {
            runMPNoGravity = backingEntity.getRunningGravityLimit();
        }
        
        return runMPNoGravity;
    }
    
    /**
     * Convenience property to determine if the backing entity is amphibious.
     */
    public boolean isAmphibious() {
        return hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS) || 
                hasWorkingMisc(MiscType.F_AMPHIBIOUS) ||
                hasWorkingMisc(MiscType.F_LIMITED_AMPHIBIOUS);
    }
    
    /**
     * Convenience property to determine how many armor-breached legs this entity has.
     * By default, this is 0 unless the unit is a mech.
     */
    public int getNumBreachedLegs() {
        if (numBreachedLegs == null) {
            if (backingEntity instanceof QuadMech) {
                numBreachedLegs = 
                        ((backingEntity.getArmor(QuadMech.LOC_LLEG) > 0) ? 0 : 1) +
                        ((backingEntity.getArmor(QuadMech.LOC_LARM) > 0) ? 0 : 1) +
                        ((backingEntity.getArmor(QuadMech.LOC_RLEG) > 0) ? 0 : 1) +
                        ((backingEntity.getArmor(QuadMech.LOC_RARM) > 0) ? 0 : 1);
            } else if (backingEntity instanceof TripodMech) {
                numBreachedLegs = 
                        ((backingEntity.getArmor(TripodMech.LOC_LLEG) > 0) ? 0 : 1) +
                        ((backingEntity.getArmor(TripodMech.LOC_CLEG) > 0) ? 0 : 1) +
                        ((backingEntity.getArmor(TripodMech.LOC_RLEG) > 0) ? 0 : 1);
            } else if (backingEntity instanceof Mech) {
                numBreachedLegs = 
                        ((backingEntity.getArmor(Mech.LOC_LLEG) > 0) ? 0 : 1) +
                        ((backingEntity.getArmor(Mech.LOC_RLEG) > 0) ? 0 : 1);
            } else {
                numBreachedLegs = 0;
            }
        }
        
        return numBreachedLegs;
    }
}
