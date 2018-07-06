package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.logging.LogLevel;
import megamek.common.logging.MMLogger;
import megamek.common.pathfinder.MovePathFinder.CoordsWithFacing;

/**
 * This set of classes is intended to be used by AI players to generate paths for infantry units.
 * This includes both foot and jump paths.
 * @author NickAragua
 *
 */
public class NewtonianAerospacePathFinder {
    private IGame game;
    private List<MovePath> aerospacePaths;
    private MovePath offBoardPath;
    private MMLogger logger;
    private static final String LOGGER_CATEGORY = "megamek.common.pathfinder.NewtonianAerospacePathFinder";
    
    // This is a map containing coordinates-with-facing, and the length of the path it took to get there
    private Map<CoordsWithFacing, Integer> visitedCoords = new HashMap<>();
    private List<MoveStepType> moves;
    
    private NewtonianAerospacePathFinder(IGame game) {
        this.game = game;
        getLogger().setLogLevel(LOGGER_CATEGORY, LogLevel.DEBUG);
        
        // put together a pre-defined array of possible moves
        moves = new ArrayList<>();
        moves.add(MoveStepType.TURN_RIGHT);
        moves.add(MoveStepType.TURN_LEFT);
        moves.add(MoveStepType.THRUST);
    }

    public Collection<MovePath> getAllComputedPathsUncategorized() {
        return aerospacePaths;
    }
    
    private MMLogger getLogger() {
        return logger == null ? logger = DefaultMmLogger.getInstance() : logger;
    }
    
    /**
     * Computes paths to nodes in the graph.
     * 
     * @param startingEdge the starting node. Should be empty.
     */
    public void run(MovePath startingEdge) {
        final String METHOD_NAME = "run";
        
        try {
            aerospacePaths = new ArrayList<MovePath>();
            // add an option to stand still
            aerospacePaths.add(startingEdge);
            
            /*MovePath testPath = startingEdge.clone();
            testPath.addStep(MoveStepType.TURN_LEFT);
            testPath.addStep(MoveStepType.THRUST);
            testPath.addStep(MoveStepType.THRUST);
            testPath.addStep(MoveStepType.THRUST);
            testPath.addStep(MoveStepType.THRUST);
            aerospacePaths.add(testPath);
            
            testPath = startingEdge.clone();
            testPath.addStep(MoveStepType.TURN_RIGHT);
            testPath.addStep(MoveStepType.THRUST);
            testPath.addStep(MoveStepType.THRUST);
            testPath.addStep(MoveStepType.THRUST);
            testPath.addStep(MoveStepType.THRUST);
            aerospacePaths.add(testPath);
            
            testPath = startingEdge.clone();
            testPath.addStep(MoveStepType.THRUST);
            testPath.addStep(MoveStepType.THRUST);
            testPath.addStep(MoveStepType.THRUST);
            testPath.addStep(MoveStepType.THRUST);
            testPath.addStep(MoveStepType.THRUST);
            aerospacePaths.add(testPath);
            
            testPath = startingEdge.clone();
            testPath.addStep(MoveStepType.YAW);
            testPath.addStep(MoveStepType.THRUST);
            testPath.addStep(MoveStepType.THRUST);
            testPath.addStep(MoveStepType.THRUST);
            aerospacePaths.add(testPath);*/
            
            aerospacePaths.addAll(generateChildren(startingEdge));
            
            visitedCoords.clear();
        } catch (OutOfMemoryError e) {
            /*
             * Some implementations can run out of memory if they consider and
             * save in memory too many paths. Usually we can recover from this
             * by ending prematurely while preserving already computed results.
             */

            final String memoryMessage = "Not enough memory to analyse all options."//$NON-NLS-1$
                    + " Try setting time limit to lower value, or "//$NON-NLS-1$
                    + "increase java memory limit.";
            
            getLogger().log(this.getClass(), METHOD_NAME, LogLevel.ERROR, memoryMessage, e);
        } catch(Exception e) {
            getLogger().log(this.getClass(), METHOD_NAME, e); //do something, don't just swallow the exception, good lord
        }
    }
    
    public static NewtonianAerospacePathFinder getInstance(IGame game) {
        NewtonianAerospacePathFinder npf = new NewtonianAerospacePathFinder(game);

        return npf;
    }
    
    /**
     * Recursive method that generates the possible child paths from the given path.
     * Eliminates paths to hexes we've already visited.
     * Generates *shortest* paths to destination hexes, because, look, infantry isn't going to get beyond a move 1 mod anyway.
     * @param startingPath
     * @return
     */
    private List<MovePath> generateChildren(MovePath startingPath) {
        List<MovePath> retval = new ArrayList<>();
        
        CoordsWithFacing pathDestination = new CoordsWithFacing(startingPath);
        
        // terminator conditions:
        // we've visited this hex already and the path we are considering is longer than the previous path that visited this hex
        // OR we have used all our MP
        if((visitedCoords.containsKey(pathDestination) && visitedCoords.get(pathDestination).intValue() < startingPath.getMpUsed()) ||
                (startingPath.getMpUsed() >= startingPath.getEntity().getRunMP())) {
            return retval;
        }
        
        // keep track of a single path that takes us off board, if there is such a thing
        // this should always be the shortest one.
        if(game.getBoard().getHex(pathDestination.getCoords()) == null &&
                (offBoardPath == null || startingPath.getMpUsed() < offBoardPath.getMpUsed())) {        
            offBoardPath = startingPath;
        }
        
        visitedCoords.put(pathDestination, startingPath.getMpUsed());
        
        // generate all possible children, add them to list
        // for aerospace units, these are:
        // turn left
        // turn right
        // thrust
        for(int moveType = 0; moveType < moves.size(); moveType++) {
            MovePath childPath = startingPath.clone();

            // ensure we're not repeatedly turning back and forth
            MoveStepType nextStepType = moves.get(moveType);
            if((childPath.getLastStep() != null) &&
                    (((nextStepType == MoveStepType.TURN_LEFT) && (childPath.getLastStep().getType() == MoveStepType.TURN_RIGHT)) ||
                    ((nextStepType == MoveStepType.TURN_RIGHT) && (childPath.getLastStep().getType() == MoveStepType.TURN_LEFT)))) {
                continue;
            }
            
            childPath.addStep(nextStepType);
            
            // having generated the child, we add it and (recursively) any of its children to the list of children to be returned            
            // of course, if it winds up not being legal anyway for some other reason, then we discard it and move on
            if(!childPath.isMoveLegal()) {
                continue;
            }
            
            retval.add(childPath.clone());
            retval.addAll(generateChildren(childPath));
        }
                
        return retval;
    }
}
