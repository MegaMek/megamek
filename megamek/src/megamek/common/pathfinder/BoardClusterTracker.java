package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.BulldozerMovePath;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IBoard;
import megamek.common.MiscType;

public class BoardClusterTracker {
    private static enum MovementType {
        Walker,
        Wheeled,
        WheeledAmphi,
        Tracked,
        TrackedAmphi,
        Hover,
        Jump,
        Flyer,
        Water,
        None;
        
        /**
         * Figures out the relevant entity movement mode (for path caching) based on properties 
         * of the entity. Mostly just movement mode, but some complications exist for tracked/wheeled vehicles.
         */
        public MovementType getMovementType(Entity entity) {
            switch(entity.getMovementMode()) {
                case BIPED:
                case TRIPOD:
                case QUAD:
                case INF_LEG:
                    return Walker;
                case TRACKED:
                    return entity.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS) ? TrackedAmphi : Tracked;
                    //technically  MiscType.F_AMPHIBIOUS and MiscType.F_LIMITED_AMPHIBIOUS apply here too, but are not implemented in general
                case WHEELED:
                    return entity.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS) ? WheeledAmphi : Wheeled;
                case HOVER:
                    return Hover;
                case BIPED_SWIM:
                case QUAD_SWIM:
                case INF_UMU:
                case SUBMARINE:
                case NAVAL:
                case HYDROFOIL:
                    return Water;
                case VTOL:
                    return Flyer;
                default:
                    return None;
            }
        }
    }
    
    //private Map<MovementType, List<BoardCluster>> movableAreas;
    //private Map<MovementType, List<BoardCluster>> movableAreasWithTerrainReduction;
    
    private Map<Coords, BoardCluster> clustersByCoordinates;
    private Map<Coords, BoardCluster> clustersByCoordinatesWithBulldozer;
    
    public Map<Coords, BoardCluster> generateClusters(Entity entity, boolean destructionAware) { 
        Map<Coords, BoardCluster> clusters = new HashMap<>();
        
        // start with (0, 0)
        // for each hex:
        // if hex is not accessible to this entity, move on
        // if hex is accessible, check if its neighbors are in a cluster
        // for each neighbor in a cluster, check if we can move from this hex to the neighbor and back 
        //      if yes, flag for joining neighbor's cluster
        //      note that if any other neighbor is in a different cluster, and we can move back and forth, we merge the two clusters
        // if no neighbors or cannot move back and forth between hex and neighbors, start new cluster
        
        IBoard board = entity.getGame().getBoard();
        int clusterID = 0;
        
        for(int x = 0; x < board.getWidth(); x++) {
            for(int y = 0; y < board.getHeight(); y++) {
                Coords c = new Coords(x, y);
                
                // hex is either inaccessible
                // or it is inaccessible AND we can't level it, then we move on
                if (entity.isLocationProhibited(c) && 
                        (!destructionAware || (destructionAware && !canLevel(entity, c)))) {
                    continue;
                }
                
                List<Coords> neighborsToJoin = new ArrayList<>();
                
                // hex is accessible one way or another
                for(int direction = 0; direction < 6; direction++) {
                    Coords neighbor = c.translated(direction);
                    
                    if(clusters.containsKey(neighbor)) {
                        int neighborElevation = BoardEdgePathFinder.calculateUnitElevationInHex(board.getHex(neighbor), entity);
                        int myElevation = BoardEdgePathFinder.calculateUnitElevationInHex(board.getHex(c), entity);
                        
                        // if we can't reach from here to the neighbor due to elevation differences, move on
                        int elevationDiff = Math.abs(neighborElevation - myElevation);
                        if(elevationDiff > entity.getMaxElevationChange()) {
                            continue;
                        }
                        
                        // we can "freely" go back and forth between ourselves and the neighbor, so let's join that cluster 
                        neighborsToJoin.add(neighbor);
                    }
                }
                
                // start up a new cluster if we have no mutually accessible neighbors
                if(neighborsToJoin.isEmpty()) {
                    BoardCluster newCluster = new BoardCluster(clusterID++);
                    newCluster.contents.add(c);
                    clusters.put(c, newCluster);
                // otherwise, join an existing cluster, bringing any other mutually accessible neighbors and their clusters with me 
                } else {
                    BoardCluster newCluster = clusters.get(neighborsToJoin.get(0));
                    newCluster.contents.add(c);
                    clusters.put(c, newCluster);
                    
                    // merge any other clusters belonging to joined neighbors to this cluster
                    for(int neighborIndex = 1; neighborIndex < neighborsToJoin.size(); neighborIndex++) {
                        BoardCluster oldCluster = clusters.get(neighborsToJoin.get(neighborIndex));
                        
                        for(Coords member : oldCluster.contents) {
                            newCluster.contents.add(member);
                            clusters.put(member, newCluster);
                        }
                    }
                    
                }
            }
        }
        
        return clusters;
    }
    
    private boolean canLevel(Entity entity, Coords c) {
        return BulldozerMovePath.calculateLevelingCost(c, entity) > BulldozerMovePath.CANNOT_LEVEL;
    }
    
    // algorithm notes:
    // for current entity
    //      find the cluster to which the entity belongs
    //      if the cluster also contains the destination region / point
    //          find destruction aware path to destination region / point
    //      else
    //          fuck it, return to milling around
    // consider jumping units
    // "spruce up" long range paths with rotations
    
    
    public static class BoardCluster {
        public Set<Coords> contents = new HashSet<>();
        public int id;
        
        public BoardCluster(int id) {
            this.id = id;
        }
        
        public Set<Coords> getIntersectingHexes(int xStart, int xEnd, int yStart, int yEnd) {
            Set<Coords> retVal = new HashSet<>();
            
            for(int x = xStart; x < xEnd; x++) {
                for(int y = yStart; y < yEnd; y++) {
                    Coords coords = new Coords(x, y);
                    if(contents.contains(coords)) {
                        retVal.add(coords);
                    }
                }
            }
            
            return retVal;
        }
    }
}
