package megamek.common.pathfinder;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.MiscType;

/**
 * A transient class used to lazy-load "calculated" information from an entity
 * 
 */
public class CachedEntityState {
    private Entity backingEntity;
    
    private Integer walkMP;
    private Integer runMP;
    private Integer runMPWithoutMasc;
    private Integer runMPNoGravity;
    private Integer sprintMP;
    private Integer sprintMPWithoutMasc;
    private Integer jumpMP;
    private Integer jumpMPWithTerrain;
    private Map<BigInteger, Boolean> hasWorkingMisc;
    private Integer torsoJumpJets;
    private Integer jumpMPNoGravity;
    
    public CachedEntityState(Entity entity) {
        backingEntity = entity;
        hasWorkingMisc = new HashMap<>();
    }
    
    public int getWalkMP() {
        if(walkMP == null) {
            walkMP = backingEntity.getWalkMP();
        }
        
        return walkMP;
    }
    
    public int getRunMP() {
        if(runMP == null) {
            runMP = backingEntity.getRunMP();
        }
        
        return runMP;
    }
    
    public int getRunMPwithoutMASC() {
        if(runMPWithoutMasc == null) {
            runMPWithoutMasc = backingEntity.getRunMPwithoutMASC();
        }
        
        return runMPWithoutMasc;
    }
    
    public int getSprintMP() {
        if(sprintMP == null) {
            sprintMP = backingEntity.getSprintMP();
        }
        
        return sprintMP;
    }
    
    public int getSprintMPwithoutMASC() {
        if(sprintMPWithoutMasc == null) {
            sprintMPWithoutMasc = backingEntity.getSprintMPwithoutMASC();
        }
        
        return sprintMPWithoutMasc;
    }
    
    public int getJumpMP() {
        if(jumpMP == null) {
            jumpMP = backingEntity.getJumpMP();
        }
        
        return jumpMP;
    }
    
    public int getJumpMPWithTerrain() {
        if(jumpMPWithTerrain == null) {
            jumpMPWithTerrain = backingEntity.getJumpMPWithTerrain();
        }
        
        return jumpMPWithTerrain;
    }
    
    public boolean hasWorkingMisc(BigInteger flag) {
        if(!hasWorkingMisc.containsKey(flag)) {
            hasWorkingMisc.put(flag, backingEntity.hasWorkingMisc(flag));
        }
        
        return hasWorkingMisc.get(flag);
    }
    
    public int getTorsoJumpJets() {
        if(torsoJumpJets == null) {
            if(backingEntity instanceof Mech) {
                torsoJumpJets = ((Mech) backingEntity).torsoJumpJets();
            } else {
                torsoJumpJets = 0;
            }
        }
        
        return torsoJumpJets;
    }
    
    public int getJumpMPNoGravity() {
        if (jumpMPNoGravity == null) {
            jumpMPNoGravity = backingEntity.getJumpMP(false);
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
}
