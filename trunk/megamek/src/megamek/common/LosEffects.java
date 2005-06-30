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
        public boolean attOffBoard;
        public Coords attackPos;
        public Coords targetPos;
        public int attackAbsHeight;
        public int targetAbsHeight;
        public int attackHeight;
        public int targetHeight;
        public int minimumWaterDepth = -1;
    }

    //                                                  MAXTECH             BMR
    public static final int COVER_NONE =        0;      //no cover          (none)
    public static final int COVER_LOWLEFT =     0x1;    //25% cover         (partial)
    public static final int COVER_LOWRIGHT =    0x2;    //25% cover         (partial)
    public static final int COVER_LEFT =        0x4;    //vertical cover    (blocked)
    public static final int COVER_RIGHT =       0x8;    //vertical cover    (blocked)
    public static final int COVER_HORIZONTAL =  0x3;    //50% cover         (partial)
    public static final int COVER_UPPER =       0xC;    //blocked           (blocked) - in case of future rule where only legs are exposed
    public static final int COVER_FULL =        0xF;    //blocked           (blocked)
    public static final int COVER_75LEFT =      0x7;    //75% cover         (blocked)
    public static final int COVER_75RIGHT =     0xB;    //75% cover         (blocked)
    
    boolean blocked = false;
    int lightWoods = 0;
    int heavyWoods = 0;
    int lightSmoke = 0;
    int heavySmoke = 0; // heavySmoke is also standard for normal L2 smoke
    int targetCover = COVER_NONE;  // that means partial cover
    int attackerCover = COVER_NONE;  // ditto
    Building thruBldg = null;
    
    /** Creates a new instance of LosEffects */
    public LosEffects() {

    }
    
    public void add(LosEffects other) {
        this.blocked |= other.blocked;
        this.lightWoods += other.lightWoods;
        this.heavyWoods += other.heavyWoods;
        this.lightSmoke += other.lightSmoke;
        this.heavySmoke += other.heavySmoke;
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

    public int getLightSmoke() {
        return lightSmoke;
    }
    
    public int getHeavySmoke() {
        return heavySmoke;
    }

    public boolean isBlocked() {
        return blocked;
    }
    /** Getter for property targetCover.
     * @return Value of property targetCover.
     */
    public boolean isTargetCover() {
        return targetCover >= COVER_HORIZONTAL;
    }
    
    public int getTargetCover() {    
        return targetCover;
    }
    
    /** Setter for property targetCover.
     * @param targetCover New value of property targetCover.
     */
    public void setTargetCover(int targetCover) {
        this.targetCover = targetCover;
    }

    /** Getter for property attackerCover.
     * @return Value of property attackerCover.
     */
    public boolean isAttackerCover() {
        return attackerCover >= COVER_HORIZONTAL;
    }
    
    public int getAttackerCover() {
        return attackerCover;
    }
    
    /** Setter for property attackerCover.
     * @param attackerCover New value of property attackerCover.
     */
    public void setAttackerCover(int attackerCover) {
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
        return !blocked && (lightWoods + lightSmoke) + ((heavyWoods + heavySmoke) * 2) < 3;
    }

    /**
     * Returns a LosEffects object representing the LOS effects of interveing
     * terrain between the attacker and target.
     *
     * Checks to see if the attacker and target are at an angle where the LOS
     * line will pass between two hexes.  If so, calls losDivided, otherwise 
     * calls losStraight.
     */
    public static LosEffects calculateLos(IGame game, int attackerId, Targetable target) {
        final Entity ae = game.getEntity(attackerId);
                
        // LOS fails if one of the entities is not deployed.
        if (null == ae.getPosition() || null == target.getPosition() || ae.isOffBoard()) {
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
    
        IHex attHex = game.getBoard().getHex(ae.getPosition());
        IHex targetHex = game.getBoard().getHex(target.getPosition());
        
        int attEl = ae.absHeight() + attHex.getElevation();
        int targEl;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ||
             target.getTargetType() == Targetable.TYPE_BUILDING ||
             target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ) {
            targEl = target.absHeight() + targetHex.getElevation();
        } else {
            targEl = game.getBoard().getHex(target.getPosition()).floor();
        }
        
        ai.attackAbsHeight = attEl;
        ai.targetAbsHeight = targEl;
        boolean attOffBoard = ae.isOffBoard();
        boolean attUnderWater;
        boolean attInWater;
        boolean attOnLand;
        if (attOffBoard) {
            attUnderWater = true;
            attInWater = false;
            attOnLand = true;
        } else {
            attUnderWater = attHex.containsTerrain(Terrains.WATER) && 
                attHex.depth() > 0 && 
                attEl < attHex.surface();
            attInWater = attHex.containsTerrain(Terrains.WATER) &&
                attHex.depth() > 0 && 
                attEl == attHex.surface();
            attOnLand = !(attUnderWater || attInWater);
        }
        
        boolean targetOffBoard = !game.getBoard().contains(target.getPosition());
        boolean targetUnderWater;
        boolean targetInWater;
        boolean targetOnLand;
        if (targetOffBoard) {
            targetUnderWater = true;
            targetInWater = false;
            targetOnLand = true;
        } else {
            targetUnderWater = targetHex.containsTerrain(Terrains.WATER) && 
                targetHex.depth() > 0 && 
                targEl < targetHex.surface();
            targetInWater = targetHex.containsTerrain(Terrains.WATER) &&
                targetHex.depth() > 0 && 
                targEl == targetHex.surface();
            targetOnLand = !(targetUnderWater || targetInWater);
        }
        
        boolean underWaterCombat = targetUnderWater || attUnderWater;
        
        ai.attUnderWater = attUnderWater;
        ai.attInWater = attInWater;
        ai.attOnLand = attOnLand;
        ai.targetUnderWater = targetUnderWater;
        ai.targetInWater = targetInWater;
        ai.targetOnLand = targetOnLand;
        ai.underWaterCombat = underWaterCombat;
        ai.attOffBoard = attOffBoard;
        
        return calculateLos(game, ai);
    }

    public static LosEffects calculateLos(IGame game, AttackInfo ai) {
        
        if (ai.attOffBoard) {
            LosEffects los = new LosEffects();
            los.blocked = true;
            return los;
        }
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
    public ToHitData losModifiers(IGame game) {
        ToHitData modifiers = new ToHitData();
        if (blocked) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "LOS blocked by terrain.");
        }
        
        if (lightWoods + (heavyWoods * 2) > 2) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "LOS blocked by woods.");
        }

        if (lightSmoke + (heavySmoke * 2) > 2) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "LOS blocked by smoke.");
        }

        if (lightSmoke + (heavySmoke * 2) + lightWoods + (heavyWoods * 2) > 2) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "LOS blocked by smoke and woods.");
        }
        
        if (lightWoods > 0) {
            modifiers.addModifier(lightWoods, lightWoods + " intervening light woods");
        }
        
        if (heavyWoods > 0) {
            modifiers.addModifier(heavyWoods * 2, heavyWoods + " intervening heavy woods");
        }
     
        if (lightSmoke > 0) {
            modifiers.addModifier(lightSmoke, lightSmoke + " intervening light smoke");
        }
        
        if (heavySmoke > 0) {
            StringBuffer text = new StringBuffer(heavySmoke);
            text.append(" intervening");
            if (game.getOptions().booleanOption("maxtech_fire"))
                text.append(" heavy");
            text.append(" smoke");
            modifiers.addModifier(heavySmoke * 2, text.toString());
        }

        if (targetCover != COVER_NONE) {
            if (game.getOptions().booleanOption("maxtech_partial_cover")) {
                if (targetCover == COVER_75LEFT || targetCover == COVER_75RIGHT)
                    modifiers.addModifier(1, "target has 75% cover");
                else if(targetCover >= COVER_HORIZONTAL) 
                    modifiers.addModifier(1, "target has 50% cover");
                else
                    //no bth mod for 25% cover
                    modifiers.addModifier(0, "target has 25% cover");
            } else {
                modifiers.addModifier(3, "target has partial cover");
            }
        }
        
        return modifiers;
    }

    /**
     * Returns LosEffects for a line that never passes exactly between two 
     * hexes.  Since intervening() returns all the coordinates, we just
     * add the effects of all those hexes.
     */
    private static LosEffects losStraight(IGame game, AttackInfo ai) {
        Coords[] in = Coords.intervening(ai.attackPos, ai.targetPos);
        LosEffects los = new LosEffects();
        boolean targetInBuilding = false;
        if (ai.targetEntity) {
            targetInBuilding = Compute.isInBuilding(game, game.getBoard().getHex(ai.targetPos).floor(), ai.targetPos);
        }
    
        // If the target and attacker are both in a
        // building, set that as the first LOS effect.
        if ( targetInBuilding && Compute.isInBuilding( game, game.getBoard().getHex(ai.attackPos).floor(), ai.attackPos ) ) {
            los.setThruBldg( game.getBoard().getBuildingAt( in[0] ) );
        }
    
        for (int i = 0; i < in.length; i++) {
            los.add( LosEffects.losForCoords(game, ai, in[i], los.getThruBldg()) );
        }

        if ((ai.minimumWaterDepth < 1) && ai.underWaterCombat) {
            los.blocked = true;
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
            los.setTargetCover( COVER_HORIZONTAL );
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
    private static LosEffects losDivided(IGame game, AttackInfo ai) {
        Coords[] in = Coords.intervening(ai.attackPos, ai.targetPos, true);
        LosEffects los = new LosEffects();
        boolean targetInBuilding = false;
        if (ai.targetEntity) {
            targetInBuilding = Compute.isInBuilding(game, game.getBoard().getHex(ai.targetPos).floor(), ai.targetPos);
        }

        // If the target and attacker are both in a
        // building, set that as the first LOS effect.
        if ( targetInBuilding && Compute.isInBuilding( game, game.getBoard().getHex(ai.attackPos).floor(), ai.attackPos ) ) {
            los.setThruBldg( game.getBoard().getBuildingAt( in[0] ) );
        }

        // add non-divided line segments
        for (int i = 3; i < in.length - 2; i += 3) {
            los.add( losForCoords(game, ai, in[i], los.getThruBldg()) );
        }

        if ((ai.minimumWaterDepth < 1) && ai.underWaterCombat) {
            los.blocked = true;
        }

        // if blocked already, return that
        if (los.losModifiers(game).getValue() == ToHitData.IMPOSSIBLE) {
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

            if ((ai.minimumWaterDepth < 1) && ai.underWaterCombat) {
                los.blocked = true;
            }

            if ( targetInBuilding && isElevDiff ) {
                 if ( null != left.getThruBldg() ) {
                     left.setTargetCover(COVER_HORIZONTAL);
                 }
                 if ( null != right.getThruBldg() ) {
                     right.setTargetCover(COVER_HORIZONTAL);
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
            int lVal = left.losModifiers(game).getValue();
            int rVal = right.losModifiers(game).getValue();
            if (lVal > rVal || (lVal == rVal && left.isAttackerCover())) {
                los = left;
            } else {
                los = right;
            }
            if (game.getOptions().booleanOption("maxtech_partial_cover")) {
                int cover = (left.targetCover & (COVER_LEFT | COVER_LOWLEFT)) |
                            (right.targetCover & (COVER_RIGHT | COVER_LOWRIGHT));
                if (cover < COVER_FULL && !(left.blocked && right.blocked)) {
                    los.blocked = false;
                    los.targetCover = cover;
                }
                cover = (left.attackerCover & (COVER_LEFT | COVER_LOWLEFT)) |
                        (right.attackerCover & (COVER_RIGHT | COVER_LOWRIGHT));
                if (cover < COVER_FULL && !(left.blocked && right.blocked)) {
                    los.blocked = false;
                    los.attackerCover = cover;
                }
            }
        }
        return los;
    }

    /**
     * Returns a LosEffects object representing the LOS effects of anything at
     * the specified coordinate.  
     */
    private static LosEffects losForCoords(IGame game, AttackInfo ai, 
                                          Coords coords, Building thruBldg) {
        LosEffects los = new LosEffects();        
        // ignore hexes not on board
        if (!game.getBoard().contains(coords)) {
            return los;
        }
    
        // Is there a building in this hex?
        Building bldg = game.getBoard().getBuildingAt(coords);
    
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
    
        IHex hex = game.getBoard().getHex(coords);
        int hexEl = ai.underWaterCombat ? hex.floor() : hex.surface();

        // Handle minimum water depth.
        // Applies to Torpedos.
        if (!(hex.containsTerrain(Terrains.WATER)))
            ai.minimumWaterDepth = 0;
        else if ((hex.terrainLevel(Terrains.WATER) >= 0)
                && ((ai.minimumWaterDepth == -1)
                || (hex.terrainLevel(Terrains.WATER) < ai.minimumWaterDepth)))
            ai.minimumWaterDepth = hex.terrainLevel(Terrains.WATER);
    
        // Handle building elevation.
        // Attacks thru a building are not blocked by that building.
        // ASSUMPTION: bridges don't block LOS.
        int bldgEl = 0;
        if ( null == los.getThruBldg() &&
             hex.containsTerrain( Terrains.BLDG_ELEV ) ) {
            bldgEl = hex.terrainLevel( Terrains.BLDG_ELEV );
        }
    
        // TODO: Identify when LOS travels *above* a building's hex.
        //       Alternatively, force all building hexes to be same height.
    
        // check for block by terrain
        
        //check for LOS according to diagramming rule from MaxTech, page 22
        if (game.getOptions().booleanOption("maxtech_LOS1")) {
            if (hexEl + bldgEl > (ai.targetAbsHeight * ai.attackPos.distance(coords) +
                                  ai.attackAbsHeight * ai.targetPos.distance(coords)) /
                                  (ai.targetPos.distance(coords) +
                                   ai.attackPos.distance(coords))) {
                los.blocked = true;        
            }
        }
        if ((hexEl + bldgEl > ai.attackAbsHeight && hexEl + bldgEl > ai.targetAbsHeight)
        || (hexEl + bldgEl > ai.attackAbsHeight && ai.attackPos.distance(coords) == 1)
        || (hexEl + bldgEl > ai.targetAbsHeight && ai.targetPos.distance(coords) == 1)) {
            los.blocked = true;
        }
        
        // check if there's a clear hex between the targets that's higher than
        // one of them, if we're in underwater combat
        if (ai.underWaterCombat && hex.terrainLevel(Terrains.WATER) == ITerrain.LEVEL_NONE &&
            (hexEl + bldgEl > ai.attackAbsHeight || hexEl + bldgEl > ai.targetAbsHeight)) {
            los.blocked = true;
        }
    
        // check for woods or smoke only if not under water
        if (!ai.underWaterCombat) {
            if ((hexEl + 2 > ai.attackAbsHeight && hexEl + 2 > ai.targetAbsHeight)
            || (hexEl + 2 > ai.attackAbsHeight && ai.attackPos.distance(coords) == 1)
            || (hexEl + 2 > ai.targetAbsHeight && ai.targetPos.distance(coords) == 1)) {
                // smoke overrides any woods in the hex if L3 smoke rule is off
                if (!game.getOptions().booleanOption("maxtech_fire")) {
                  if (hex.containsTerrain(Terrains.SMOKE)) {
                    los.heavySmoke++;
                  }
                  else if (hex.terrainLevel(Terrains.WOODS) == 1) {
                    los.lightWoods++;
                  }
                  else if (hex.terrainLevel(Terrains.WOODS) > 1) {
                    los.heavyWoods++;
                  }
                }
                // if the L3 fire/smoke rule is on, smoke and woods stack for LOS
                // so check them both
                else {
                  if (hex.containsTerrain(Terrains.SMOKE)) {
                    if (hex.terrainLevel(Terrains.SMOKE) == 1) {
                      los.lightSmoke++;
                    }
                    else if (hex.terrainLevel(Terrains.SMOKE) > 1) {
                      los.heavySmoke++;
                    }
                  }

                  if (hex.terrainLevel(Terrains.WOODS) == 1) {
                    los.lightWoods++;
                  }
                  else if (hex.terrainLevel(Terrains.WOODS) > 1) {
                    los.heavyWoods++;
                  }
                }
            }
        }
        
        // check for target partial cover
        if ( ai.targetPos.distance(coords) == 1) {
            if (los.blocked && game.getOptions().booleanOption("maxtech_partial_cover")) {
                los.targetCover = COVER_FULL;
            } 
            else if(hexEl + bldgEl == ai.targetAbsHeight &&
             ai.attackAbsHeight <= ai.targetAbsHeight && ai.targetHeight > 0) {
                los.targetCover |= COVER_HORIZONTAL;
            }
        }
    
        // check for attacker partial cover
        if (ai.attackPos.distance(coords) == 1) {
            if (los.blocked && game.getOptions().booleanOption("maxtech_partial_cover")) {
                los.attackerCover = COVER_FULL;
            } 
            else if(hexEl + bldgEl == ai.attackAbsHeight &&
               ai.attackAbsHeight >= ai.targetAbsHeight && ai.attackHeight > 0) {
                los.attackerCover |= COVER_HORIZONTAL;
            }
        }
        
        return los;
    }
}
