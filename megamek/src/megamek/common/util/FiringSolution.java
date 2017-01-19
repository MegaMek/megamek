package megamek.common.util;

import megamek.common.ToHitData;

/**
 * Used to track data for displaying "FiringSolution" data in the BoardView.  This contains information about to-hit
 * numbers and any other useful information for determining what units to are the best to shoot at.
 *
 * @author arlith
 *
 */
public class FiringSolution {
    
    private ToHitData toHit;
    
    private boolean targetSpotted;
    
    /**
     * 
     * @param toHit
     * @param targetSpotted
     */
    public FiringSolution (ToHitData toHit, boolean targetSpotted) {
        this.toHit = toHit;
        this.targetSpotted = targetSpotted;
    }
    
    public ToHitData getToHitData() {
        return toHit;
    }
    
    public boolean isTargetSpotted() {
        return targetSpotted;
    }

}
