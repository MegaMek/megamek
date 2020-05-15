package megamek.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import megamek.client.bot.princess.FireControl;
import megamek.common.MovePath.MoveStepType;

public class BulldozerMovePath extends MovePath {
    private static final int CANNOT_LEVEL = -1;

    Map<Coords, Integer> coordLevelingCosts = new HashMap<>();
    double maxPointBlankDamage = -1;

    public BulldozerMovePath(IGame game, Entity entity) {
        super(game, entity);
    }

    @Override
    public int getMpUsed() {
        int totalCost = super.getMpUsed();

        for (int levelingCost : coordLevelingCosts.values()) {
            totalCost += levelingCost;
        }

        return totalCost;
    }

    @Override
    public MovePath addStep(final MoveStepType type) {
        MovePath mp = super.addStep(type);

        if (!mp.isMoveLegal()) {
            // here, we will check if the step is illegal because the unit in question
            // is attempting to move through illegal terrain for its movement type, but
            // would be able to get through if the terrain was "reduced"
            int levelingCost = calculateLevelingCost(mp.getFinalCoords());
            if(levelingCost > CANNOT_LEVEL) {
                coordLevelingCosts.put(mp.getFinalCoords(), levelingCost);
            }
        }
        
        return mp;
    }
    
    /**
     * Clones this path, will contain a new clone of the steps so that the clone
     * is independent from the original.
     *
     * @return the cloned MovePath
     */
    @Override
    public BulldozerMovePath clone() {
        final BulldozerMovePath copy = new BulldozerMovePath(getGame(), getEntity());
        copyFields(copy);        
        coordLevelingCosts = new HashMap<>(coordLevelingCosts);
        return copy;
    }

    /**
     * Worker function that calculates the "MP cost" of moving into the given set of coords
     * if we were to stand still for the number of turns required to reduce the terrain there
     * to a form through which the current unit can move
     */
    private int calculateLevelingCost(Coords finalCoords) {
        IHex destHex = this.getGame().getBoard().getHex(finalCoords);
        int levelingCost = CANNOT_LEVEL;
        
        if(destHex == null) {
            return levelingCost;
        }
        
        EntityMovementMode movementMode = getEntity().getMovementMode();
        boolean isTracked = movementMode == EntityMovementMode.TRACKED && !getEntity().hasETypeFlag(Entity.ETYPE_QUADVEE);
        boolean isHovercraft = movementMode == EntityMovementMode.HOVER;
        boolean isMech = movementMode == EntityMovementMode.BIPED || movementMode == EntityMovementMode.TRIPOD ||
                movementMode == EntityMovementMode.QUAD;
        
        double damageNeeded = 0;
        
        // tracked tanks can move through light woods, rough and rubble, so any terrain that can be reduced to that
        // can eventually be moved through
        if(isTracked) {
            if (destHex.terrainLevel(Terrains.JUNGLE) > 0) {
                damageNeeded += Terrains.getTerrainFactor(Terrains.JUNGLE, destHex.terrainLevel(Terrains.JUNGLE));           
            }
            
            if (destHex.terrainLevel(Terrains.WOODS) > 1) {
                // just what we need to reduce it to light woods
                damageNeeded += Terrains.getTerrainFactor(Terrains.WOODS, destHex.terrainLevel(Terrains.WOODS)) -
                        Terrains.getTerrainFactor(Terrains.WOODS, 1); 
            }
            
            if (destHex.containsTerrain(Terrains.BLDG_CF)) {
                damageNeeded += getGame().getBoard().getBuildingAt(finalCoords).getCurrentCF(finalCoords);
            }
        }
        
        // mechs can't go through ultra-heavy terrain, so must reduce it to heavy terrain
        // may as well consider blowing buildings away
        if(isMech) {
            if (destHex.terrainLevel(Terrains.JUNGLE) > 2) {
                damageNeeded += Terrains.getTerrainFactor(Terrains.JUNGLE, destHex.terrainLevel(Terrains.JUNGLE)) -
                        Terrains.getTerrainFactor(Terrains.JUNGLE, 2);            
            }
            
            if (destHex.terrainLevel(Terrains.WOODS) > 2) {
                damageNeeded += Terrains.getTerrainFactor(Terrains.WOODS, destHex.terrainLevel(Terrains.WOODS)) -
                        Terrains.getTerrainFactor(Terrains.WOODS, 2); 
            }
            
            if (destHex.containsTerrain(Terrains.BLDG_CF)) {
                damageNeeded += getGame().getBoard().getBuildingAt(finalCoords).getCurrentCF(finalCoords);
            }
        }
        
        // hovertanks can move through rough and rubble, so any terrain that can be reduced to that
        // can eventually be moved through
        if(isHovercraft) {
            if (destHex.terrainLevel(Terrains.JUNGLE) > 0) {
                damageNeeded += Terrains.getTerrainFactor(Terrains.JUNGLE, destHex.terrainLevel(Terrains.JUNGLE));           
            }
            
            if (destHex.terrainLevel(Terrains.WOODS) > 0) {
                damageNeeded += Terrains.getTerrainFactor(Terrains.WOODS, destHex.terrainLevel(Terrains.WOODS)); 
            }
            
            if (destHex.containsTerrain(Terrains.BLDG_CF)) {
                damageNeeded += getGame().getBoard().getBuildingAt(finalCoords).getCurrentCF(finalCoords);
            }
        }
        
        if(damageNeeded > 0) {
            // basically, the MP cost of leveling this terrain is equal to how many turns we're going to waste
            // shooting at it instead of moving.
            levelingCost = (int) Math.floor(damageNeeded / getMaxPointBlankDamage()) * getEntity().getRunMP();
        }
        
        
        return levelingCost;
    }
    
    /**
     * Helper function that lazy-calculates an entity's max damage at point blank range.
     */
    private double getMaxPointBlankDamage() {
        if(maxPointBlankDamage < 0) {
            maxPointBlankDamage = FireControl.getMaxDamageAtRange(getEntity(), 1, false, false);
        }
        
        return maxPointBlankDamage;
    }
    
    public boolean needsLeveling() {
        return coordLevelingCosts.size() > 0;
    }
}
