package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.client.bot.princess.AeroPathUtil;
import megamek.common.IAero;
import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.MoveStep;
import megamek.common.pathfinder.MovePathFinder.CoordsWithFacing;

/**
 * This class is intended to be used by the bot for generating possible paths for 
 * aerospace units on a low-altitude atmospheric map.
 * @author NickAragua
 *
 */
public class AeroLowAltitudePathFinder extends AeroGroundPathFinder {

    protected static final String LOGGER_CATEGORY = "megamek.common.pathfinder.AeroLowAltitudePathFinder";
    
    protected AeroLowAltitudePathFinder(IGame game) {
        super(game);
    }

    public static AeroLowAltitudePathFinder getInstance(IGame game) {
        AeroLowAltitudePathFinder alf = new AeroLowAltitudePathFinder(game);

        return alf;
    }
    
    @Override
    protected int getMinimumVelocity(IAero mover) {
        return 1;
    }
    
    @Override
    protected int getMaximumVelocity(IAero mover) {
        return mover.getCurrentThrust() * 2;
    }
    
    /**
     * Generate all possible paths given a starting movement path.
     * This includes increases and decreases in elevation.
     */
    @Override
    protected List<MovePath> GenerateAllPaths(MovePath mp) {
        List<MovePath> altitudePaths = AeroPathUtil.generateValidAltitudeChanges(mp);
        List<MovePath> fullMovePaths = new ArrayList<>();
        
        for(MovePath altitudePath : altitudePaths) {
            fullMovePaths.addAll(super.GenerateAllPaths(altitudePath.clone()));
        }
        
        List<MovePath> fullMovePathsWithTurns = new ArrayList<>();
        
        for (MovePath movePath : fullMovePaths) {
            fullMovePathsWithTurns.add(movePath);
            
            MoveStep lastStep = movePath.getLastStep();
            
            if ((lastStep != null) && lastStep.canAeroTurn(game)) {
                MovePath left = movePath.clone();
                left.addStep(MoveStepType.TURN_LEFT);
                fullMovePathsWithTurns.add(left);
                
                MovePath right = movePath.clone();
                right.addStep(MoveStepType.TURN_RIGHT);
                fullMovePathsWithTurns.add(right);
            }
        }
        
        return fullMovePathsWithTurns;
    }
    
    /**
     * Get a list of movement paths with end-of-path altitude adjustments.
     * Irrelevant for low-atmo maps, so simply returns the passed-in list.
     */
    @Override
    protected List<MovePath> getAltitudeAdjustedPaths(List<MovePath> startingPaths) {
        return startingPaths;
    }
    
    // this data structure maps a set of coordinates with facing
    // to a map between height and "used MP".
    private Map<CoordsWithFacing, Map<Integer, Integer>> visitedCoords = new HashMap<CoordsWithFacing, Map<Integer, Integer>>();
    /**
     * Determines whether or not the given move path is "redundant".
     * In this situation, "redundant" means "there is already a shorter path that goes to the ending coordinates/facing/height" combo.
     */
    @Override
    protected boolean pathIsRedundant(MovePath mp) {
        if(!mp.fliesOffBoard()) {
            CoordsWithFacing destinationCoords = new CoordsWithFacing(mp);
            if(!visitedCoords.containsKey(destinationCoords)) {
                visitedCoords.put(destinationCoords, new HashMap<>());
            }
            
            // we may or may not have been to these coordinates before, but we haven't been to this height. Not redundant. 
            if(!visitedCoords.get(destinationCoords).containsKey(mp.getFinalAltitude())) {
                visitedCoords.get(destinationCoords).put(mp.getFinalAltitude(), mp.getMpUsed());
                return false;
            // we *have* been to these coordinates and height before. This is redundant if the previous visit used less MP.
            } else {
                return visitedCoords.get(destinationCoords).get(mp.getFinalAltitude()) < mp.getMpUsed();
            }
        } else {
            return false;
        }
    }
}
