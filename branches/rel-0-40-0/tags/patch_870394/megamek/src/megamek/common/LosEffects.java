/*
 * MegaMek - Copyright (C) 2002-2003 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */

/*
 * LosEffects.java
 *
 * Created on October 14, 2002, 11:19 PM
 */

package megamek.common;

/**
 * Keeps track of the cumulative effects of intervening terrain on LOS
 *
 * @author  Ben
 */
public class LosEffects {
    
    public static class AttackInfo {
    	public boolean attUnderWater;
		public boolean attInWater;
		public boolean attOnLand;
		public boolean targetUnderWater;
		public boolean targetInWater;
		public boolean targetOnLand;
		public boolean underWaterCombat;
		public boolean targetEntity = true;
		public boolean targetInfantry;
		public Coords attackPos;
		public Coords targetPos;
		public int attackAbsHeight;
		public int targetAbsHeight;
		public int attackHeight;
		public int targetHeight;
    }

    boolean blocked = false;
    int lightWoods = 0;
    int heavyWoods = 0;
    int smoke = 0;
    boolean targetCover = false;  // that means partial cover
    boolean attackerCover = false;  // ditto
    Building thruBldg = null;
    
    /** Creates a new instance of LosEffects */
    public LosEffects() {
        ;
    }
    
    public void add(LosEffects other) {
        this.blocked |= other.blocked;
        this.lightWoods += other.lightWoods;
        this.heavyWoods += other.heavyWoods;
        this.smoke += other.smoke;
        this.targetCover |= other.targetCover;
        this.attackerCover |= other.attackerCover;
        if ( null != this.thruBldg &&
             !this.thruBldg.equals(other.thruBldg) ) {
            this.thruBldg = null;
        }
    }
    
    public int getLightWoods() {
    	return lightWoods;
    }
    
    public int getHeavyWoods() {
    	return heavyWoods;
    }
    
    public int getSmoke() {
    	return smoke;
    }
    
    public boolean isBlocked() {
    	return blocked;
    }
    /** Getter for property targetCover.
     * @return Value of property targetCover.
     */
    public boolean isTargetCover() {
        return targetCover;
    }
    
    /** Setter for property targetCover.
     * @param targetCover New value of property targetCover.
     */
    public void setTargetCover(boolean targetCover) {
        this.targetCover = targetCover;
    }

    /** Getter for property attackerCover.
     * @return Value of property attackerCover.
     */
    public boolean isAttackerCover() {
        return attackerCover;
    }
    
    /** Setter for property attackerCover.
     * @param attackerCover New value of property attackerCover.
     */
    public void setAttackerCover(boolean attackerCover) {
        this.attackerCover = attackerCover;
    }

    /** Getter for property thruBldg.
     * @return Value of property thruBldg.
     */
    public Building getThruBldg() {
        return thruBldg;
    }
    
    /** Setter for property thruBldg.
     * @param thruBldg New value of property thruBldg.
     */
    public void setThruBldg(Building thruBldg) {
        this.thruBldg = thruBldg;
    }
    
	/**
	 * LOS check from ae to te.
	 */
	public boolean canSee() {
		return !blocked && lightWoods + ((heavyWoods + smoke) * 2) < 3;
	}

    /**
     * Returns a LosEffects object representing the LOS effects of interveing
     * terrain between the attacker and target.
     *
     * Checks to see if the attacker and target are at an angle where the LOS
     * line will pass between two hexes.  If so, calls losDivided, otherwise 
     * calls losStraight.
     */
    public static LosEffects calculateLos(Game game, int attackerId, Targetable target) {
        final Entity ae = game.getEntity(attackerId);
                
        // LOS fails if one of the entities is not deployed.
        if (null == ae.getPosition() || null == target.getPosition()) {
            LosEffects los = new LosEffects();
            los.blocked = true; // TODO: come up with a better "impossible"
            return los;
        }
        
        final AttackInfo ai = new AttackInfo();
        ai.attackPos = ae.getPosition();
        ai.targetPos = target.getPosition();
        ai.targetEntity = target.getTargetType() == Targetable.TYPE_ENTITY;
        ai.targetInfantry = target instanceof Infantry;
    	ai.attackHeight = ae.getHeight();
    	ai.targetHeight = target.getHeight();
    
        Hex attHex = game.board.getHex(ae.getPosition());
        Hex targetHex = game.board.getHex(target.getPosition());
        
        int attEl = ae.absHeight();
        int targEl;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ||
             target.getTargetType() == Targetable.TYPE_BUILDING ||
             target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ) {
            targEl = target.absHeight();
        } else {
            targEl = game.board.getHex(target.getPosition()).floor();
        }
    	
        ai.attackAbsHeight = attEl;
        ai.targetAbsHeight = targEl;
        
        boolean attUnderWater = attHex.contains(Terrain.WATER) && 
        						attHex.depth() > 0 && 
        						attEl < attHex.surface();
        boolean attInWater = attHex.contains(Terrain.WATER) &&
        						attHex.depth() > 0 && 
        						attEl == attHex.surface();
        boolean attOnLand = !(attUnderWater || attInWater);
        
        boolean targetUnderWater = targetHex.contains(Terrain.WATER) && 
        						targetHex.depth() > 0 && 
        						targEl < targetHex.surface();
        boolean targetInWater = targetHex.contains(Terrain.WATER) &&
        						targetHex.depth() > 0 && 
        						targEl == targetHex.surface();
        boolean targetOnLand = !(targetUnderWater || targetInWater);
		
        boolean underWaterCombat = targetUnderWater || attUnderWater;
        
        ai.attUnderWater = attUnderWater;
        ai.attInWater = attInWater;
        ai.attOnLand = attOnLand;
        ai.targetUnderWater = targetUnderWater;
        ai.targetInWater = targetInWater;
        ai.targetOnLand = targetOnLand;
        ai.underWaterCombat = underWaterCombat;
        
		return calculateLos(game, ai);
    }

    public static LosEffects calculateLos(Game game, AttackInfo ai) {
        // good time to ensure hex cache
		IdealHex.ensureCacheSize(game.board.width + 1, game.board.height + 1);
        
		if (ai.attOnLand && ai.targetUnderWater ||
			ai.attUnderWater && ai.targetOnLand) {
			LosEffects los = new LosEffects();
			los.blocked = true;
			return los;        	
		}
		
        double degree = ai.attackPos.degree(ai.targetPos);
        if (degree % 60 == 30) {
            return LosEffects.losDivided(game, ai);
        } else {
            return LosEffects.losStraight(game, ai);
        }
    }

    /**
     * Returns ToHitData indicating the modifiers to fire for the specified
     * LOS effects data.
     */
    public ToHitData losModifiers() {
        ToHitData modifiers = new ToHitData();
        if (blocked) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "LOS blocked by terrain.");
        }
        
        if (lightWoods + (heavyWoods * 2) > 2) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "LOS blocked by woods.");
        }
        
        if (smoke > 1) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "LOS blocked by smoke.");
        }
        
        if (smoke == 1) {
            if (lightWoods + heavyWoods > 0) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "LOS blocked by smoke and woods.");
            } else {
                modifiers.addModifier(2, "intervening smoke");
            }
        }
        
        if (lightWoods > 0) {
            modifiers.addModifier(lightWoods, lightWoods + " intervening light woods");
        }
        
        if (heavyWoods > 0) {
            modifiers.addModifier(heavyWoods * 2, heavyWoods + " intervening heavy woods");
        }
        
        if (targetCover) {
            modifiers.addModifier(3, "target has partial cover");
        }
        
        return modifiers;
    }

    /**
     * Returns LosEffects for a line that never passes exactly between two 
     * hexes.  Since intervening() returns all the coordinates, we just
     * add the effects of all those hexes.
     */
    private static LosEffects losStraight(Game game, AttackInfo ai) {
        Coords[] in = Compute.intervening(ai.attackPos, ai.targetPos);
        LosEffects los = new LosEffects();
        boolean targetInBuilding = false;
        if (ai.targetEntity) {
            targetInBuilding = Compute.isInBuilding(game, game.board.getHex(ai.targetPos).floor(), ai.targetPos);
        }
    
        // If the target and attacker are both in a
        // building, set that as the first LOS effect.
        if ( targetInBuilding && Compute.isInBuilding( game, game.board.getHex(ai.attackPos).floor(), ai.attackPos ) ) {
            los.setThruBldg( game.board.getBuildingAt( in[0] ) );
        }
    
        for (int i = 0; i < in.length; i++) {
            los.add( LosEffects.losForCoords(game, ai, in[i], los.getThruBldg()) );
        }
    
        // Infantry inside a building can only be
        // targeted by units in the same building.
        if ( ai.targetInfantry && targetInBuilding &&
             null == los.getThruBldg() ) {
            los.blocked = true;
        }
    
        // If a target Entity is at a different elevation as its
        // attacker, and if the attack is through a building, the
        // target has cover.
        if ( null != los.getThruBldg() &&
             ai.attackAbsHeight != ai.targetAbsHeight ) {
            los.setTargetCover( true );
        }
    
        return los;
    }

    /**
     * Returns LosEffects for a line that passes between two hexes at least
     * once.  The rules say that this situation is resolved in favor of the
     * defender.
     *
     * The intervening() function returns both hexes in these circumstances,
     * and, when they are in line order, it's not hard to figure out which hexes 
     * are split and which are not.
     *
     * The line always looks like:
     *       ___     ___
     *   ___/ 1 \___/...\___
     *  / 0 \___/ 3 \___/etc\
     *  \___/ 2 \___/...\___/
     *      \___/   \___/
     *
     * We go thru and figure out the modifiers for the non-split hexes first.
     * Then we go to each of the two split hexes and determine which gives us
     * the bigger modifier.  We use the bigger modifier.
     *
     * This is not perfect as it takes partial cover as soon as it can, when
     * perhaps later might be better.
     * Also, it doesn't account for the fact that attacker partial cover blocks
     * leg weapons, as we want to return the same sequence regardless of
     * what weapon is attacking.
     */
    private static LosEffects losDivided(Game game, AttackInfo ai) {
		Coords[] in = Compute.intervening(ai.attackPos, ai.targetPos);
		LosEffects los = new LosEffects();
		boolean targetInBuilding = false;
		if (ai.targetEntity) {
			targetInBuilding = Compute.isInBuilding(game, game.board.getHex(ai.targetPos).floor(), ai.targetPos);
		}
		    
        // If the target and attacker are both in a
        // building, set that as the first LOS effect.
		if ( targetInBuilding && Compute.isInBuilding( game, game.board.getHex(ai.attackPos).floor(), ai.attackPos ) ) {
			los.setThruBldg( game.board.getBuildingAt( in[0] ) );
		}
    
        // add non-divided line segments
        for (int i = 3; i < in.length - 2; i += 3) {
            los.add( losForCoords(game, ai, in[i], los.getThruBldg()) );
        }
        
        // if blocked already, return that
        if (los.losModifiers().getValue() == ToHitData.IMPOSSIBLE) {
            return los;
        }
        
        // go through divided line segments
        for (int i = 1; i < in.length - 2; i += 3) {
            // get effects of each side
            LosEffects left = losForCoords( game, ai, in[i], los.getThruBldg());
            LosEffects right = losForCoords( game, ai, in[i+1], los.getThruBldg());
    
            // If a target Entity is at a different elevation as its
            // attacker, and if the attack is through a building, the
            // target has cover.
			final boolean isElevDiff = ai.attackAbsHeight != ai.targetAbsHeight;
            
            if ( targetInBuilding && isElevDiff ) {
                 if ( null != left.getThruBldg() ) {
                     left.setTargetCover(true);
                 }
                 if ( null != right.getThruBldg() ) {
                     right.setTargetCover(true);
                 }
            }
    
            // Include all previous LOS effects.
            left.add(los);
            right.add(los);
    
            // Infantry inside a building can only be
            // targeted by units in the same building.
            if ( ai.targetInfantry && targetInBuilding ) {
                if ( null == left.getThruBldg() ) {
                    left.blocked = true;
                }
                else if ( null == right.getThruBldg() ) {
                    right.blocked = true;
                }
            }
    
            // which is better?
            int lVal = left.losModifiers().getValue();
            int rVal = right.losModifiers().getValue();
            if (lVal > rVal || (lVal == rVal && left.isAttackerCover())) {
                los = left;
            } else {
                los = right;
            }
        }
        
        return los;
    }

    /**
     * Returns a LosEffects object representing the LOS effects of anything at
     * the specified coordinate.  
     */
    private static LosEffects losForCoords(Game game, AttackInfo ai, 
                                          Coords coords, Building thruBldg) {
        LosEffects los = new LosEffects();        
        // ignore hexes not on board
        if (!game.board.contains(coords)) {
            return los;
        }
    
        // Is there a building in this hex?
        Building bldg = game.board.getBuildingAt(coords);
    
        // We're only tracing thru a single building if there
        // is a building in this hex, and if it isn't the same
        // building that we'be been tracing LOS thru.
        if ( bldg != null && bldg.equals(thruBldg) ) {
            los.setThruBldg( thruBldg );
        }
    
        // ignore hexes the attacker or target are in
        if ( coords.equals(ai.attackPos) ||
             coords.equals(ai.targetPos) ) {
            return los;
        }
    
        Hex hex = game.board.getHex(coords);
        int hexEl = ai.underWaterCombat ? hex.floor() : hex.surface();
    
        // Handle building elevation.
        // Attacks thru a building are not blocked by that building.
        // ASSUMPTION: bridges don't block LOS.
        int bldgEl = 0;
        if ( null == los.getThruBldg() &&
             hex.contains( Terrain.BLDG_ELEV ) ) {
            bldgEl = hex.levelOf( Terrain.BLDG_ELEV );
        }
    
        // TODO: Identify when LOS travels *above* a building's hex.
        //       Alternatively, force all building hexes to be same height.
    
        // check for block by terrain
        if ((hexEl + bldgEl > ai.attackAbsHeight && hexEl + bldgEl > ai.targetAbsHeight)
        || (hexEl + bldgEl > ai.attackAbsHeight && ai.attackPos.distance(coords) == 1)
        || (hexEl + bldgEl > ai.targetAbsHeight && ai.targetPos.distance(coords) == 1)) {
            los.blocked = true;
        }
    
        // check for woods or smoke only if not under water
        if (!ai.underWaterCombat) {
            if ((hexEl + 2 > ai.attackAbsHeight && hexEl + 2 > ai.targetAbsHeight)
            || (hexEl + 2 > ai.attackAbsHeight && ai.attackPos.distance(coords) == 1)
            || (hexEl + 2 > ai.targetAbsHeight && ai.targetPos.distance(coords) == 1)) {
                // smoke overrides any woods in the hex
                if (hex.contains(Terrain.SMOKE)) {
                    los.smoke++;
                } else if (hex.levelOf(Terrain.WOODS) == 1) {
                    los.lightWoods++;
                } else if (hex.levelOf(Terrain.WOODS) > 1) {
                    los.heavyWoods++;
                }
            }
        }
        
        // check for target partial cover
        if ( ai.targetPos.distance(coords) == 1 &&
             hexEl + bldgEl == ai.targetAbsHeight &&
             ai.attackAbsHeight <= ai.targetAbsHeight && ai.targetHeight > 0) {
            los.targetCover = true;
        }
    
        // check for attacker partial cover
        if (ai.attackPos.distance(coords) == 1 &&
            hexEl + bldgEl == ai.attackAbsHeight &&
            ai.attackAbsHeight >= ai.targetAbsHeight && ai.attackHeight > 0) {
            los.attackerCover = true;
        }
        
        return los;
    }
}
