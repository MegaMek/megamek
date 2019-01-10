package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.client.bot.princess.AeroPathUtil;
import megamek.common.IAero;
import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.pathfinder.MovePathFinder.CoordsWithFacing;

/**
 * This class generates move paths suitable for use by an aerospace unit
 * operating on a space map, with 'advanced flight' turned off.
 * @author NickAragua
 *
 */
public class AeroSpacePathFinder extends NewtonianAerospacePathFinder {

    protected static final String LOGGER_CATEGORY = "megamek.common.pathfinder.AeroSpacePathFinder";
    
    protected AeroSpacePathFinder(IGame game) {
        super(game);
    }
    
    /**
     * Worker method to put together a pre-defined array of possible moves
     */
    @Override
    protected void initializeMoveList() {
        moves = new ArrayList<>();
        moves.add(MoveStepType.TURN_RIGHT);
        moves.add(MoveStepType.TURN_LEFT);
        moves.add(MoveStepType.FORWARDS);
    }
    
    public static AeroSpacePathFinder getInstance(IGame game) {
        AeroSpacePathFinder asf = new AeroSpacePathFinder(game);

        return asf;
    }
    
    /** 
     * Generates a list of possible step combinations that should be done at the beginning of a path
     * This implementation generates exactly one path, which is either no moves or one hex forward when velocity > 0
     * @return "List" of all possible "starting" paths
     */
    @Override
    protected List<MovePath> generateStartingPaths(MovePath startingEdge) {
        List<MovePath> startingPaths = new ArrayList<>();
        
        // calculate max and min safe velocity
        // in space, we can go as slow or as fast as we want.
        IAero aero = (IAero) startingEdge.getEntity();
        int maxThrust = AeroPathUtil.calculateMaxSafeThrust(aero);  
        int maxVelocity = aero.getCurrentVelocity() + maxThrust;
        int minVelocity = Math.max(0, aero.getCurrentVelocity() - maxThrust);
        startingPaths.addAll(AeroPathUtil.generateValidAccelerations(startingEdge, minVelocity, maxVelocity));
        
        // all non-zero-velocity paths must move at least one hex forward
        for(MovePath path : startingPaths) {
            if(path.getFinalVelocity() > 0) {
                path.addStep(MoveStepType.FORWARDS);
            }
        }
        
        
        return startingPaths;
    }
    
    /**
     * "Worker" function to determine whether the path being examined is an intermediate path.
     * This means that the path, as is, is not a valid path, but its children may be.
     * This mainly applies to aero paths that have not used all their velocity.
     * @param path The move path to consider.
     * @return Whether it is an intermediate path or not.
     */
    @Override
    protected boolean isIntermediatePath(MovePath path) {
        return path.getFinalVelocityLeft() > 0;
    }
    
    /**
     * Worker function to determine whether we should discard the current path 
     * (due to it being illegal or redundant) or keep generating child nodes
     * @param path The move path to consider
     * @return Whether to keep or dicsard.
     */
    @Override
    protected boolean discardPath(MovePath path, CoordsWithFacing pathDestination) {
        boolean maxMPExceeded = path.getMpUsed() > path.getEntity().getRunMP();
        
        // having generated the child, we add it and (recursively) any of its children to the list of children to be returned            
        // unless it moves too far or exceeds max thrust
        if(path.getFinalVelocityLeft() < 0 || maxMPExceeded) {
            return true;
        }
        
        // terminator conditions:
        // we've visited this hex already and the path we are considering is longer than the previous path that visited this hex
        if(visitedCoords.containsKey(pathDestination) && visitedCoords.get(pathDestination).intValue() < path.getMpUsed()) {
            return true;
        }
        
        // there's no reason to consider off-board paths in the standard flight model.
        if(!path.getGame().getBoard().contains(pathDestination.getCoords())) {
            return true;
        }
        
        return false;
    }
}
