/*
 * Copyright (c) 2019 The MegaMek Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.client.bot.princess.AeroPathUtil;
import megamek.common.Coords;
import megamek.common.IAero;
import megamek.common.Game;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.logging.LogLevel;
import megamek.common.logging.MMLogger;

/**
 * This set of classes is intended to be used by AI players to generate paths for units behaving
 * like spheroid dropships in atmosphere. Remarkably similar to a jumping infantry unit.
 * @author NickAragua
 *
 */
public class SpheroidPathFinder {
    private Game game;
    private List<MovePath> spheroidPaths;
    private MMLogger logger;
    private static final String LOGGER_CATEGORY = "megamek.common.pathfinder.SpheroidPathFinder";
    
    private Set<Coords> visitedCoords = new HashSet<>();
    
    private SpheroidPathFinder(Game game) {
        this.game = game;
        getLogger().setLogLevel(LOGGER_CATEGORY, LogLevel.DEBUG);
    }

    public Collection<MovePath> getAllComputedPathsUncategorized() {
        return spheroidPaths;
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
        try {
            spheroidPaths = new ArrayList<MovePath>();

            // can't do anything if the unit is out of control.
            if(((IAero) startingEdge.getEntity()).isOutControlTotal()) {
                return;
            }
            
            // total number of paths should be ~217 * n on a ground map or 7 * n on a low atmo map
            // where n is the number of possible altitude changes
            List<MovePath> altitudePaths = AeroPathUtil.generateValidAltitudeChanges(startingEdge);
            MovePath hoverPath = generateHoverPath(startingEdge);
            for(MovePath altitudePath : altitudePaths) {
                // since we are considering paths across multiple altitudes that cross the same coordinates
                // we want to clear this out before every altitude to avoid discarding altitude changing paths
                // so that our dropships can maneuver vertically if necessary.
                visitedCoords.clear();
                
                // we don't really want to consider a non-hovering path, we will add it as a special case
                if(altitudePath.length() != 0) {
                    spheroidPaths.addAll(generateChildren(altitudePath));
                } else {
                    spheroidPaths.addAll(generateChildren(hoverPath));
                }
            }
            
            spheroidPaths.addAll(altitudePaths);
            spheroidPaths.add(hoverPath);
            
            List<MovePath> validRotations = new ArrayList<>();
            // now that we've got all our possible destinations, make sure to try every possible rotation
            // at the end of the path
            for(MovePath path : spheroidPaths) {
                validRotations.addAll(AeroPathUtil.generateValidRotations(path));
            }
            
            spheroidPaths.addAll(validRotations);
            
            visitedCoords.clear();
            
            // add "flee" option if we haven't done anything else
            if(game.getBoard().isOnBoardEdge(startingEdge.getFinalCoords()) &&
                    startingEdge.getStepVector().size() == 0) {
                MovePath fleePath = startingEdge.clone();
                fleePath.addStep(MoveStepType.FLEE);
                spheroidPaths.add(fleePath);
            }
        } catch (OutOfMemoryError e) {
            /*
             * Some implementations can run out of memory if they consider and
             * save in memory too many paths. Usually we can recover from this
             * by ending prematurely while preserving already computed results.
             */

            final String memoryMessage = "Not enough memory to analyse all options."//$NON-NLS-1$
                    + " Try setting time limit to lower value, or "//$NON-NLS-1$
                    + "increase java memory limit.";
            
            getLogger().error(memoryMessage, e);
        } catch(Exception e) {
            getLogger().error(e); //do something, don't just swallow the exception, good lord
        }
    }
    
    public static SpheroidPathFinder getInstance(Game game) {
        SpheroidPathFinder ipf = new SpheroidPathFinder(game);

        return ipf;
    }
    
    private MovePath generateHoverPath(MovePath startingPath) {
        MovePath hoverPath = startingPath.clone();
        hoverPath.addStep(MoveStepType.HOVER);
        
        // if we can hover, then hover. If not (due to battle damage or whatever), then we fall down.
        if(hoverPath.isMoveLegal()) {
            return hoverPath;
        } else {
            hoverPath.removeLastStep();
            return hoverPath;
        }
    }
    
    /**
     * Recursive method that generates the possible child paths from the given path.
     * Eliminates paths to hexes we've already visited.
     * Generates *shortest* paths to destination hexes
     * @param startingPath
     * @return
     */
    private List<MovePath> generateChildren(MovePath startingPath) {
        List<MovePath> retval = new ArrayList<>();
        
        // terminator conditions:
        // we've visited this hex already
        // we've moved further than 1 hex on a low-atmo map
        // we've moved further than 8 hexes on a ground map
        if(visitedCoords.contains(startingPath.getFinalCoords()) ||
                (startingPath.getMpUsed() > startingPath.getEntity().getRunMP())) {
            return retval;
        }
        
        visitedCoords.add(startingPath.getFinalCoords());
        
        // generate all possible children, add them to list
        // for units acting as in-atmo spheroid jumpships, facing changes are free, so children are always
        // forward, left-forward, left-left-forward, right-forward, right-right-forward, right-right-right-forward
        // there is never a reason to "back up"
        // there are also very little built-in error control, since these things are flying
        for(int direction = 0; direction <= 5; direction++) {
            MovePath childPath = startingPath.clone();
            
            // for each child, we first turn in the appropriate direction
            for(MoveStepType stepType : AeroPathUtil.TURNS.get(direction)) {
                childPath.addStep(stepType);
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
