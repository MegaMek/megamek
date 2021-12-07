/*
* MegaMek -
* Copyright (C) 2020 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package megamek.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.client.bot.princess.FireControl;
import megamek.client.bot.princess.MinefieldUtil;
import megamek.common.pathfinder.BoardClusterTracker.MovementType;

/**
 * An extension of the MovePath class that stores information about terrain that needs
 * to be destroyed in order to move along the specified route.
 * @author NickAragua
 */
public class BulldozerMovePath extends MovePath {
    private static final long serialVersionUID = 1346716014573707012L;

    public static final int CANNOT_LEVEL = -1;

    Map<Coords, Integer> coordLevelingCosts = new HashMap<>();
    Map<Coords, Integer> additionalCosts = new HashMap<>();
    List<Coords> coordsToLevel = new ArrayList<>();
    double maxPointBlankDamage = -1;

    public BulldozerMovePath(Game game, Entity entity) {
        super(game, entity);
    }

    /**
     * Any additional costs of this move paths, such as stepping into water or
     * other factors that would increase the number of turns to complete it without increasing the actual MP used.
     */
    public int getAdditionalCost() {
        int totalCost = 0;

        for (int additionalCost : additionalCosts.values()) {
            totalCost += additionalCost;
        }

        return totalCost;
    }
    
    /**
     * An estimation of how many MP we would "waste" blowing down obstacles.
     */
    public int getLevelingCost() {
        int totalCost = 0;

        for (int levelingCost : coordLevelingCosts.values()) {
            totalCost += levelingCost;
        }

        return totalCost;
    }

    /**
     * Override of the MovePath.addStep method, calculates leveling and other extra costs 
     * associated with this bulldozer move path
     */
    @Override
    public MovePath addStep(final MoveStepType type) {
        BulldozerMovePath mp = (BulldozerMovePath) super.addStep(type);
        Hex hex = mp.getGame().getBoard().getHex(mp.getFinalCoords());
        int hexWaterDepth = ((hex != null) && hex.containsTerrain(Terrains.WATER)) ?
                hex.depth() : Integer.MIN_VALUE;
        
        if (!mp.isMoveLegal() && !mp.isJumping()) {
            // here, we will check if the step is illegal because the unit in question
            // is attempting to move through illegal terrain for its movement type, but
            // would be able to get through if the terrain was "reduced"
            // don't bother to do this for jumping paths
            int levelingCost = calculateLevelingCost(mp.getFinalCoords(), getEntity());
            if (levelingCost > CANNOT_LEVEL) {
                coordLevelingCosts.put(mp.getFinalCoords(), levelingCost);
                coordsToLevel.add(mp.getFinalCoords());
            }
            
            // we want to make note of when we're going into water (if we are capable of it)
            // it may look cheaper, but it slows you down to max walking speed or worse, 
            // and we should flag it as costing extra, that extra being the difference between walking and running speed
            if (hexWaterDepth > 0) {
                MovementType mType = MovementType.getMovementType(mp.getEntity());
                if (mType == MovementType.Walker || mType == MovementType.WheeledAmphi || mType == MovementType.TrackedAmphi) {
                    additionalCosts.put(mp.getFinalCoords(), 1);
                }
            }
        }

        if (mp.isJumping()) {
            // if we are jumping, but not on top of a bridge (because jumping always goes to the top of a bridge)
            // and are jumping into terrain that would impede jump jet functionality (aka water)
            // then we are impeding future jump movement and should add an extra cost to this step
            if ((hex != null) && !hex.containsTerrain(Terrains.BRIDGE)) {
                // special case - mech jumping into depth 1 water might not be all that bad, jump mp cost wise
                if (hexWaterDepth == 1) {
                    additionalCosts.put(mp.getFinalCoords(), mp.getCachedEntityState().getJumpMP() - mp.getCachedEntityState().getTorsoJumpJets());
                // jumping into water that submerges you entirely pretty much ruins jump MP for at least a turn while you clamber out
                } else if (hexWaterDepth > 1) {
                    additionalCosts.put(mp.getFinalCoords(), mp.getCachedEntityState().getJumpMP());
                }
            }
        }
        
        // we want to discourage running over minefields
        double minefieldFactor = MinefieldUtil.calcMinefieldHazardForHex(mp.getLastStep(), mp.getEntity(), 
                mp.isJumping(), false);
        
        if (minefieldFactor > 0) {
            additionalCosts.put(mp.getFinalCoords(), (int) Math.ceil(minefieldFactor));
        }

        return mp;
    }
    
    /**
     * Removes the last step from the path and updates its internal data structures accordingly
     */
    @Override
    public void removeLastStep() {
        Coords prevFinalCoords = getFinalCoords();
        
        super.removeLastStep();
        
        // if removing the last step changes the destination coordinates
        // we need to clear out some of the data we have for this path.
        if (!getFinalCoords().equals(prevFinalCoords)) {
            coordLevelingCosts.remove(prevFinalCoords);
            additionalCosts.remove(prevFinalCoords);
            coordsToLevel.remove(prevFinalCoords);
        }
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
        copy.coordLevelingCosts = new HashMap<>(coordLevelingCosts);
        copy.additionalCosts = new HashMap<>(additionalCosts);
        copy.coordsToLevel = new ArrayList<>(coordsToLevel);
        copy.maxPointBlankDamage = maxPointBlankDamage;
        return copy;
    }

    /**
     * Worker function that calculates the "MP cost" of moving into the given set of coords
     * if we were to stand still for the number of turns required to reduce the terrain there
     * to a form through which the current unit can move
     */
    public static int calculateLevelingCost(Coords finalCoords, Entity entity) {
        Board board = entity.getGame().getBoard();
        Hex destHex = board.getHex(finalCoords);
        int levelingCost = CANNOT_LEVEL;
        
        if (destHex == null) {
            return levelingCost;
        }
        
        EntityMovementMode movementMode = entity.getMovementMode();
        boolean isTracked = movementMode == EntityMovementMode.TRACKED && !entity.hasETypeFlag(Entity.ETYPE_QUADVEE);
        boolean isHovercraft = movementMode == EntityMovementMode.HOVER;
        boolean isMech = movementMode == EntityMovementMode.BIPED || movementMode == EntityMovementMode.TRIPOD ||
                movementMode == EntityMovementMode.QUAD;
        
        double damageNeeded = 0;
        
        // tracked tanks can move through light woods, rough and rubble, so any terrain that can be reduced to that
        // can eventually be moved through
        if (isTracked) {
            if (destHex.terrainLevel(Terrains.JUNGLE) > 0) {
                damageNeeded += Terrains.getTerrainFactor(Terrains.JUNGLE, destHex.terrainLevel(Terrains.JUNGLE));           
            }
            
            if (destHex.terrainLevel(Terrains.WOODS) > 1) {
                // just what we need to reduce it to light woods
                damageNeeded += Terrains.getTerrainFactor(Terrains.WOODS, destHex.terrainLevel(Terrains.WOODS)) -
                        Terrains.getTerrainFactor(Terrains.WOODS, 1); 
            }
            
            if (destHex.containsTerrain(Terrains.BLDG_CF)) {
                damageNeeded += board.getBuildingAt(finalCoords).getCurrentCF(finalCoords);
            }
        }
        
        // mechs can't go through ultra-heavy terrain, so must reduce it to heavy terrain
        // may as well consider blowing buildings away
        if (isMech) {
            if (destHex.terrainLevel(Terrains.JUNGLE) > 2) {
                damageNeeded += Terrains.getTerrainFactor(Terrains.JUNGLE, destHex.terrainLevel(Terrains.JUNGLE)) -
                        Terrains.getTerrainFactor(Terrains.JUNGLE, 2);            
            }
            
            if (destHex.terrainLevel(Terrains.WOODS) > 2) {
                damageNeeded += Terrains.getTerrainFactor(Terrains.WOODS, destHex.terrainLevel(Terrains.WOODS)) -
                        Terrains.getTerrainFactor(Terrains.WOODS, 2); 
            }
            
            if (destHex.containsTerrain(Terrains.BLDG_CF)) {
                damageNeeded += board.getBuildingAt(finalCoords).getCurrentCF(finalCoords);
            }
        }
        
        // hovertanks can move through rough and rubble, so any terrain that can be reduced to that
        // can eventually be moved through
        if (isHovercraft) {
            if (destHex.terrainLevel(Terrains.JUNGLE) > 0) {
                damageNeeded += Terrains.getTerrainFactor(Terrains.JUNGLE, destHex.terrainLevel(Terrains.JUNGLE));           
            }
            
            if (destHex.terrainLevel(Terrains.WOODS) > 0) {
                damageNeeded += Terrains.getTerrainFactor(Terrains.WOODS, destHex.terrainLevel(Terrains.WOODS)); 
            }
            
            if (destHex.containsTerrain(Terrains.BLDG_CF)) {
                damageNeeded += board.getBuildingAt(finalCoords).getCurrentCF(finalCoords);
            }
        }
        
        if (damageNeeded > 0) {
            // basically, the MP cost of leveling this terrain is equal to how many turns we're going to waste
            // shooting at it instead of moving.
            levelingCost = (int) Math.round(damageNeeded / getMaxPointBlankDamage(entity)) * entity.getRunMP();
        }
        
        
        return levelingCost;
    }
    
    /**
     * Helper function that lazy-calculates an entity's max damage at point blank range.
     */
    private static double getMaxPointBlankDamage(Entity entity) {
        return FireControl.getMaxDamageAtRange(entity, 1, false, false);
    }
    
    /**
     * Whether this path will require terrain reduction to fully accomplish
     */
    public boolean needsLeveling() {
        return coordLevelingCosts.size() > 0;
    }
    
    /**
     * The coordinates which need to be leveled for this path to be performed by its unit
     */
    public List<Coords> getCoordsToLevel() {
        return coordsToLevel;
    }
    
    @Override
    public String toString() {
        return super.toString() + " Leveling Cost: " + getLevelingCost() + " Additional Cost: " + getAdditionalCost();
    }
    
    /**
     * Comparator implementation useful in comparing two bulldozer move paths by
     * how many MP it'll take to accomplish that path, including time wasted leveling any obstacles
     * @author NickAragua
     *
     */
    public static class MPCostComparator implements Comparator<BulldozerMovePath> {
        /**
         * compare the first move path to the second
         * Favors paths that spend less mp total
         * in case of tie, favors paths that use more hexes
         */
        @Override
        public int compare(BulldozerMovePath first, BulldozerMovePath second) {
            int dd = (first.getMpUsed() + first.getLevelingCost() + first.getAdditionalCost()) - 
                    (second.getMpUsed() + second.getLevelingCost() + second.getAdditionalCost());
    
            if (dd != 0) {
                return dd;
            } else {
                return first.getHexesMoved() - second.getHexesMoved();
            }           
        }
    }
}
