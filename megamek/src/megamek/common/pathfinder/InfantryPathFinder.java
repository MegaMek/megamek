package megamek.common.pathfinder;

import megamek.client.bot.princess.AeroPathUtil;
import megamek.client.bot.princess.FireControl;
import megamek.common.*;
import megamek.common.MovePath.MoveStepType;
import org.apache.logging.log4j.LogManager;

import java.util.*;

/**
 * This set of classes is intended to be used by AI players to generate paths for infantry units.
 * This includes both foot and jump paths.
 * @author NickAragua
 */
public class InfantryPathFinder {
    private Game game;
    private List<MovePath> infantryPaths;

    private Set<Coords> visitedCoords = new HashSet<>();
    
    
    private InfantryPathFinder(Game game) {
        this.game = game;
    }

    public Collection<MovePath> getAllComputedPathsUncategorized() {
        return infantryPaths;
    }

    /**
     * Computes paths to nodes in the graph.
     * 
     * @param startingEdge the starting node. Should be empty.
     */
    public void run(MovePath startingEdge) {
        try {
            infantryPaths = new ArrayList<>();
            // add an option to stand still
            infantryPaths.add(startingEdge);
            
            // if, for some reason, the entity has already moved this turn or otherwise can't move, don't bother calculating paths for it
            if (!startingEdge.getEntity().isSelectableThisTurn()) {
                return;
            }
            
            // for an infantry unit with n MP, the total number of paths should be 6 * n*(n+1)/2 + 1 (triangular rule, plus "stand still")
            infantryPaths.addAll(generateChildren(startingEdge));
            
            visitedCoords.clear();
            
            // add possible jump paths, if any
            MovePath jumpEdge = startingEdge.clone();
            jumpEdge.addStep(MoveStepType.START_JUMP);
            infantryPaths.addAll(generateChildren(jumpEdge));
            
            // now that we've got all our possible destinations, make sure to try every possible rotation,
            // since facing matters for field guns and if using the "dig in" and "vehicle cover" tacops rules.
            List<MovePath> rotatedPaths = new ArrayList<>();
            
            for (MovePath path : infantryPaths) {
                rotatedPaths.addAll(AeroPathUtil.generateValidRotations(path));
            }
            
            infantryPaths.addAll(rotatedPaths);
            
            // add "flee" option if we haven't done anything else
            if (game.getBoard().isOnBoardEdge(startingEdge.getFinalCoords()) &&
                    startingEdge.getStepVector().size() == 0) {
                MovePath fleePath = startingEdge.clone();
                fleePath.addStep(MoveStepType.FLEE);
                infantryPaths.add(fleePath);
            }
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
            LogManager.getLogger().error("", e); //do something, don't just swallow the exception, good lord
        }
    }
    
    public static InfantryPathFinder getInstance(Game game) {

        return new InfantryPathFinder(game);
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
        if (visitedCoords.contains(startingPath.getFinalCoords()) ||
                (!startingPath.isJumping() && (startingPath.getMpUsed() >= startingPath.getEntity().getRunMP()))||
                (startingPath.isJumping() && (startingPath.getMpUsed() >= startingPath.getEntity().getJumpMP()))) {
            return retval;
        }
        
        visitedCoords.add(startingPath.getFinalCoords());
        
        // generate all possible children, add them to list
        // for infantry, facing changes are free, so children are always
        // forward, left-forward, left-left-forward, right-forward, right-right-forward, right-right-right-forward
        // there is never a reason to "back up"
        for (int direction = 0; direction <= 5; direction++) {
            // carry out some preliminary checks:
            // are we going off board?
            // are we going into a building?
            // are we going onto a bridge?
            // make sure we're adjusting facing relative to the unit's current facing
            Hex destinationHex = game.getBoard().getHexInDir(startingPath.getFinalCoords(), 
                    FireControl.correctFacing(startingPath.getFinalFacing() + direction));
            
            // if we're going off board, we may as well not bother continuing
            // additionally, if we're definitely going to collapse a bridge we're stepping on let's just stop right here.
            // we're walking *through* buildings, so collapsing them isn't going to be a problem
            if (destinationHex == null || 
                    destinationHex.containsTerrain(Terrains.BRIDGE_CF) && 
                    (destinationHex.getTerrain(Terrains.BRIDGE_CF).getLevel() < startingPath.getEntity().getWeight())) {
                continue;
            }
            
            MovePath childPath = startingPath.clone();
            
            // for each child, we first turn in the appropriate direction
            for (MoveStepType stepType : AeroPathUtil.TURNS.get(direction)) {
                childPath.addStep(stepType);
            }
            
            // then, if we're going to wind up on a building or bridge, set CLIMB_MODE appropriately
            // go *through* buildings
            if (destinationHex.containsTerrain(Terrains.BLDG_CF) && childPath.getFinalClimbMode()) {
                childPath.addStep(MoveStepType.CLIMB_MODE_OFF);
            // go *over* bridges
            } else if (destinationHex.containsTerrain(Terrains.BRIDGE_CF) && !childPath.getFinalClimbMode()) {
                childPath.addStep(MoveStepType.CLIMB_MODE_ON);
            }
            
            // finally, move forward
            childPath.addStep(MoveStepType.FORWARDS);
            
            // having generated the child, we add it and (recursively) any of its children to the list of children to be returned            
            // of course, if it winds up not being legal anyway for some other reason, then we discard it and move on
            if (!childPath.isMoveLegal()) {
                continue;
            }
            
            retval.add(childPath.clone());
            retval.addAll(generateChildren(childPath));
        }
                
        return retval;
    }
}
