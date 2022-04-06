package megamek.common.pathfinder;

import megamek.common.Game;
import megamek.common.IAero;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.pathfinder.MovePathFinder.CoordsWithFacing;
import org.apache.logging.log4j.LogManager;

import java.util.*;

/**
 * This set of classes is intended to be used by AI players to generate paths for infantry units.
 * This includes both foot and jump paths.
 * @author NickAragua
 */
public class NewtonianAerospacePathFinder {
    private Game game;
    protected List<MovePath> aerospacePaths;
    protected MovePath offBoardPath;

    // This is a map containing coordinates-with-facing, and the length of the path it took to get there
    protected Map<CoordsWithFacing, Integer> visitedCoords = new HashMap<>();
    // This is a list of all possible moves
    protected List<MoveStepType> moves;
    
    protected NewtonianAerospacePathFinder(Game game) {
        this.game = game;

        initializeMoveList();
    }
    
    /**
     * Worker method to put together a pre-defined array of possible moves
     */
    protected void initializeMoveList() {
        moves = new ArrayList<>();
        moves.add(MoveStepType.TURN_RIGHT);
        moves.add(MoveStepType.TURN_LEFT);
        moves.add(MoveStepType.THRUST);
    }

    public Collection<MovePath> getAllComputedPathsUncategorized() {
        return aerospacePaths;
    }

    /**
     * Computes paths to nodes in the graph.
     * 
     * @param startingEdge the starting node. Should be empty.
     */
    public void run(MovePath startingEdge) {
        try {
            aerospacePaths = new ArrayList<>();
            
            // can't do anything if the unit is out of control.
            if (((IAero) startingEdge.getEntity()).isOutControlTotal()) {
                return;
            }
            
            for (MovePath path : generateStartingPaths(startingEdge)) {
                aerospacePaths.addAll(generateChildren(path));
            }
            
            // it's possible that we generated some number of paths that go off-board
            // now is the time to clean those out.
            // except for the one that we designated as the shortest off-board path
            aerospacePaths.removeIf(path -> !startingEdge.getGame().getBoard().contains(path.getFinalCoords()));
            if (offBoardPath != null) {
                aerospacePaths.add(offBoardPath);
            }
            
            visitedCoords.clear();
        } catch (OutOfMemoryError e) {
            /*
             * Some implementations can run out of memory if they consider and
             * save in memory too many paths. Usually we can recover from this
             * by ending prematurely while preserving already computed results.
             */

            final String memoryMessage = "Not enough memory to analyse all options."
                    + " Try setting time limit to lower value, or "
                    + "increase java memory limit.";
            
            LogManager.getLogger().error(memoryMessage, e);
        } catch (Exception e) {
            LogManager.getLogger().error("", e); // do something, don't just swallow the exception, good lord
        }
    }
    
    public static NewtonianAerospacePathFinder getInstance(Game game) {
        return new NewtonianAerospacePathFinder(game);
    }
        
    /** 
     * Generates a list of possible step combinations that should be done at the beginning of a path
     * Has side effect of updating the visited coordinates map and adding it to the list of generated paths
     * @return List of all possible "starting" paths
     */
    protected List<MovePath> generateStartingPaths(MovePath startingEdge) {
        List<MovePath> startingPaths = new ArrayList<>();
        
        MovePath defaultPath = startingEdge.clone();
        aerospacePaths.add(defaultPath);
        visitedCoords.put(new CoordsWithFacing(defaultPath), defaultPath.getMpUsed());
        startingPaths.add(defaultPath);
        
        MovePath reverseEdge = startingEdge.clone();
        reverseEdge.addStep(MoveStepType.YAW);
        aerospacePaths.add(reverseEdge);
        visitedCoords.put(new CoordsWithFacing(reverseEdge), reverseEdge.getMpUsed());
        startingPaths.add(defaultPath);
        
        return startingPaths;
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
        
        // generate all possible children, add them to list
        // for aerospace units, these are:
        // turn left
        // turn right
        // thrust
        for (int moveType = 0; moveType < moves.size(); moveType++) {
            MovePath childPath = startingPath.clone();

            // two things we want to avoid:
            // 1: turning back and forth
            // 2: turning more than 2 hexes in a row
            MoveStepType nextStepType = moves.get(moveType);
            if (tooMuchTurning(childPath, nextStepType)) {
                continue;
            }
            
            childPath.addStep(nextStepType);
            
            CoordsWithFacing pathDestination = new CoordsWithFacing(childPath);
            if (discardPath(childPath, pathDestination)) {
                continue;
            }
            
            // keep track of a single path that takes us off board, if there is such a thing
            // this should always be the shortest one.
            if (game.getBoard().getHex(pathDestination.getCoords()) == null &&
                    (offBoardPath == null || childPath.getMpUsed() < offBoardPath.getMpUsed())) {        
                offBoardPath = childPath;
            }
            
            if (!isIntermediatePath(childPath)) {
                visitedCoords.put(pathDestination, childPath.getMpUsed());
            
                retval.add(childPath.clone());
            }
            
            retval.addAll(generateChildren(childPath));
        }
                
        return retval;
    }
    
    /**
     * "Worker" function to determine whether the path being examined is an intermediate path.
     * This means that the path, as is, is not a valid path, but its children may be.
     * This mainly applies to aero paths that have not used all their velocity.
     * For newtonian space flight, it is never an intermediate path.
     * @param path The move path to consider.
     * @return Whether it is an intermediate path or not.
     */
    protected boolean isIntermediatePath(MovePath path) {
        return false;
    }
    
    /**
     * Worker function to determine whether we should discard the current path 
     * (due to it being illegal or redundant) or keep generating child nodes
     * @param path The move path to consider
     * @return Whether to keep or dicsard.
     */
    protected boolean discardPath(MovePath path, CoordsWithFacing pathDestination) {
        boolean maxMPExceeded = path.getMpUsed() > path.getEntity().getRunMP();
        
        // having generated the child, we add it and (recursively) any of its children to the list of children to be returned            
        // unless it exceeds max MP, in which case we discard it
        if (maxMPExceeded) {
            return true;
        }
        
        // terminator conditions:
        // we've visited this hex already and the path we are considering is longer than the previous path that visited this hex
        if (visitedCoords.containsKey(pathDestination) && (visitedCoords.get(pathDestination).intValue() < path.getMpUsed())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Worker function to calculate whether, if the given move step is added to the given move path, there will be
     * "too much turning", where a turn of 180 degrees or more is considered too much (we can yaw instead)
     * @param path The move path to consider
     * @param stepType The step type to consider
     * @return Whether we're turning too much
     */
    private boolean tooMuchTurning(MovePath path, MoveStepType stepType) {
        if (path.getLastStep() == null || path.getSecondLastStep() == null) {
            return false;
        }
        
        // more than two turns in a row is no good
        if ((stepType == MoveStepType.TURN_LEFT || stepType == MoveStepType.TURN_RIGHT)
                && (path.getSecondLastStep().getType() == path.getLastStep().getType()) 
                && (path.getLastStep().getType() == stepType)) {
            return true;
        }
        
        // turning back and forth in place is no good
        if ((stepType == MoveStepType.TURN_LEFT && path.getLastStep().getType() == MoveStepType.TURN_RIGHT) ||
                (stepType == MoveStepType.TURN_RIGHT && path.getLastStep().getType() == MoveStepType.TURN_LEFT)) {
            return true;
        }
        
        return false;
    }
}
