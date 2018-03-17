package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.Coords;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.Terrains;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.logging.LogLevel;
import megamek.common.logging.MMLogger;

/**
 * This set of classes is intended to be used by AI players to generate paths for infantry units.
 * This includes both foot and jump paths.
 * @author NickAragua
 *
 */
public class InfantryPathFinder {
    private IGame game;
    private List<MovePath> infantryPaths;
    private MMLogger logger;
    private static final String LOGGER_CATEGORY = "megamek.common.pathfinder.InfantryPathFinder";
    
    private Set<Coords> visitedCoords = new HashSet<>();
    private List<List<MoveStepType>> turns;
    
    private InfantryPathFinder(IGame game) {
        this.game = game;
        getLogger().setLogLevel(LOGGER_CATEGORY, LogLevel.DEBUG);
        
        // put together a pre-defined array of turns. Indexes correspond to the directional values found in Coords.java
        turns = new ArrayList<>();
        turns.add(new ArrayList<MoveStepType>()); // "no turns"
        
        turns.add(new ArrayList<MoveStepType>());
        turns.get(1).add(MoveStepType.TURN_RIGHT);
        
        turns.add(new ArrayList<MoveStepType>());
        turns.get(2).add(MoveStepType.TURN_RIGHT);
        turns.get(2).add(MoveStepType.TURN_RIGHT);
        
        turns.add(new ArrayList<MoveStepType>());
        turns.get(3).add(MoveStepType.TURN_RIGHT);
        turns.get(3).add(MoveStepType.TURN_RIGHT);
        turns.get(3).add(MoveStepType.TURN_RIGHT);
        
        turns.add(new ArrayList<MoveStepType>());
        turns.get(4).add(MoveStepType.TURN_LEFT);
        turns.get(4).add(MoveStepType.TURN_LEFT);
        
        turns.add(new ArrayList<MoveStepType>());
        turns.get(5).add(MoveStepType.TURN_LEFT);
    }

    public Collection<MovePath> getAllComputedPathsUncategorized() {
        return infantryPaths;
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
            infantryPaths = new ArrayList<MovePath>();
            // add an option to stand still
            infantryPaths.add(startingEdge);
            
            // for an infantry unit with n MP, the total number of paths should be 6 * n*(n+1)/2 + 1 (triangular rule, plus "stand still")
            infantryPaths.addAll(generateChildren(startingEdge));
            
            visitedCoords.clear();
            
            // add possible jump paths, if any
            MovePath jumpEdge = startingEdge.clone();
            jumpEdge.addStep(MoveStepType.START_JUMP);
            infantryPaths.addAll(generateChildren(jumpEdge));
            
            // add "flee" option if we haven't done anything else
            if(startingEdge.getFinalCoords().isOnBoardEdge(game.getBoard()) &&
                    startingEdge.getStepVector().size() == 0) {
                MovePath fleePath = startingEdge.clone();
                fleePath.addStep(MoveStepType.FLEE);
            }
            
            // TODO: but low priority - if we're using "TacOps Dig In" or "Tac Ops Using Non-Infantry Units as Cover"
            // ending facing matters, so we'll need to post-process all the generated move paths and 
            // give them an ending facing
            // this will increase the number of possible paths by a factor of 6, although, since infantry is slow, it's
            // not that big a deal
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
    
    public static InfantryPathFinder getInstance(IGame game) {
        InfantryPathFinder ipf = new InfantryPathFinder(game);

        return ipf;
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
        
        // terminator conditions:
        // we've visited this hex already
        // we are walking and at or past our movement mp
        // we are jumping and at or past our jump mp
        if(visitedCoords.contains(startingPath.getFinalCoords()) ||
                (!startingPath.isJumping() && (startingPath.getMpUsed() >= startingPath.getEntity().getRunMP()))||
                (startingPath.isJumping() && (startingPath.getMpUsed() >= startingPath.getEntity().getJumpMP()))) {
            return retval;
        }
        
        visitedCoords.add(startingPath.getFinalCoords());
        
        // generate all possible children, add them to list
        // for infantry, facing changes are free, so children are always
        // forward, left-forward, left-left-forward, right-forward, right-right-forward, right-right-right-forward
        // there is never a reason to "back up"
        for(int direction = 0; direction <= 5; direction++) {
            // carry out some preliminary checks:
            // are we going off board?
            // are we going into a building?
            // are we going onto a bridge?
            IHex destinationHex = game.getBoard().getHexInDir(startingPath.getFinalCoords(), direction);
            
            // if we're going off board, we may as well not bother continuing
            // TODO: additionally, if we're definitely going to collapse the building or bridge we're stepping on
            // then let's just stop right here
            if(destinationHex == null) {
                continue;
            }
            
            MovePath childPath = startingPath.clone();
            
            // for each child, we first turn in the appropriate direction
            for(MoveStepType stepType : turns.get(direction)) {
                childPath.addStep(stepType);
            }
            
            // then, if we're going to wind up on a building or bridge, set CLIMB_MODE appropriately
            // go *through* buildings
            if(destinationHex.containsTerrain(Terrains.BLDG_CF) && childPath.getFinalClimbMode()) {
                childPath.addStep(MoveStepType.CLIMB_MODE_OFF);
            // go *over* bridges
            } else if (destinationHex.containsTerrain(Terrains.BRIDGE_CF) && !childPath.getFinalClimbMode()) {
                childPath.addStep(MoveStepType.CLIMB_MODE_ON);
            }
            
            // finally, move forward
            childPath.addStep(MoveStepType.FORWARDS);
            
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
