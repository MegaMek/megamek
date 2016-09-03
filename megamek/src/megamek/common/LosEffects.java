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

import java.util.ArrayList;
import java.util.Vector;

import megamek.client.ui.Messages;
import megamek.common.options.OptionsConstants;
import megamek.server.SmokeCloud;

/**
 * Keeps track of the cumulative effects of intervening terrain on LOS
 *
 * @author Ben
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
        public boolean targetIsMech;
        public boolean attackerIsMech;
        public boolean attOffBoard;
        public Coords attackPos;
        public Coords targetPos;
        
        /**
         * The absolute elevation of the attacker, i.e. the number of levels
         * attacker is placed above a level 0 hex.
         */
        public int attackAbsHeight;
        
        /**
         * The absolute elevation of the target, i.e. the number of levels
         * target is placed above a level 0 hex.
         */
        public int targetAbsHeight;
        
        /**
         * The height of the attacker, that is, how many levels above its
         * elevation it is for LOS purposes.
         */
        public int attackHeight;
        
        /**
         * The height of the target, that is, how many levels above its
         * elevation it is for LOS purposes.
         */
        public int targetHeight;
        public int attackerId;
        public int targetId;
        int minimumWaterDepth = -1;
    }

    // MAXTECH BMR
    public static final int COVER_NONE = 0; // no cover (none)
    public static final int COVER_LOWLEFT = 0x1; // 25% cover (partial)
    public static final int COVER_LOWRIGHT = 0x2; // 25% cover (partial)
    public static final int COVER_LEFT = 0x4; // vertical cover (blocked)
    public static final int COVER_RIGHT = 0x8; // vertical cover (blocked)
    public static final int COVER_HORIZONTAL = 0x3; // 50% cover (partial)
    // Upper: for underwater attacks against 'mechs standing in depth 1, TW 109
    public static final int COVER_UPPER = 0xC; // 50% cover (partial)
    public static final int COVER_FULL = 0xF; // blocked (blocked)
    public static final int COVER_75LEFT = 0x7; // 75% cover (blocked)
    public static final int COVER_75RIGHT = 0xB; // 75% cover (blocked)
    
    public static final int DAMAGABLE_COVER_NONE = 0;
    public static final int DAMAGABLE_COVER_DROPSHIP = 0x1;
    public static final int DAMAGABLE_COVER_BUILDING = 0x2;

    boolean blocked = false;
    boolean deadZone = false;
    boolean infProtected = false;
    boolean hasLoS = true;
    int plantedFields = 0;
    int heavyIndustrial = 0;
    int lightWoods = 0;
    int heavyWoods = 0;
    int ultraWoods = 0;
    int lightSmoke = 0;
    int heavySmoke = 0;
    int screen = 0;
    int softBuildings = 0;
    int hardBuildings = 0;
    int buildingLevelsOrHexes = 0;
    boolean blockedByHill = false;
    boolean blockedByWater = false;
    int targetCover = COVER_NONE; // that means partial cover
    int attackerCover = COVER_NONE; // ditto
    Building thruBldg = null;
    Coords targetLoc;
    /**
     * Indicates if the primary cover is damagable.
     */
    int damagableCoverTypePrimary   = DAMAGABLE_COVER_NONE;
    /**
     * Indicates if the secondary cover is damagable
     */   
    int damagableCoverTypeSecondary = DAMAGABLE_COVER_NONE;
    /**
     * Keeps track of the building that provides cover.  This is used
     * to assign damage for shots that hit cover.  The primary cover is used 
     * if there is a sole piece of cover (horizontal cover, 25% cover).
     * In the case of a primary and secondary, the primary cover protects the 
     * right side.
     */
    Building coverBuildingPrimary = null;
    /**
     * Keeps track of the building that provides cover.  This is used
     * to assign damage for shots that hit cover.  The secondary cover is used
     * if there are two buildings that provide cover, like in the case of 75%
     * cover or two buildings providing 25% cover for a total of horizontal 
     * cover.  The secondary cover protects the left side.
     */
    Building coverBuildingSecondary = null;
    /**
     * Keeps track of the grounded Dropship that provides cover.  This is
     * used to assign damage for shots that hit cover. The primary cover is used 
     * if there is a sole piece of cover (horizontal cover, 25% cover).
     * In the case of a primary and secondary, the primary cover protects the 
     * right side.
     */
    Entity coverDropshipPrimary = null;
    /**
     * Keeps track of the grounded Dropship that provides cover.  This is
     * used to assign damage for shots that hit cover. The secondary cover is used
     * if there are two buildings that provide cover, like in the case of 75%
     * cover or two buildings providing 25% cover for a total of horizontal 
     * cover.  The secondary cover protects the left side.
     */
    Entity coverDropshipSecondary = null;    
    /**
     * Stores the hex location of the primary cover.
     */
    Coords coverLocPrimary = null;
    /**
     * Stores the hex location of the secondary cover.
     */
    Coords coverLocSecondary = null;
    int minimumWaterDepth = -1;
    boolean arcedShot = false;

    
    public Coords getTargetPosition() {
        return targetLoc;
    }
    
    public int getMinimumWaterDepth() {
        return minimumWaterDepth;
    }

    public void setMinimumWaterDepth(int inVal) {
        minimumWaterDepth = inVal;
    }

    public void add(LosEffects other) {
        // We need to check if we should update damagable cover
        //  We need to update cover if it's present, but we don't want to
        //  remove cover if no new cover is present
        //  this assumes that LoS is being drawn from attacker to target
        if (other.damagableCoverTypePrimary != DAMAGABLE_COVER_NONE && 
                other.targetCover >= targetCover){
            damagableCoverTypePrimary = other.damagableCoverTypePrimary;
            coverDropshipPrimary = other.coverDropshipPrimary;
            coverBuildingPrimary = other.coverBuildingPrimary;
            coverLocPrimary = other.coverLocPrimary;
            damagableCoverTypeSecondary = other.damagableCoverTypeSecondary;            
            coverDropshipSecondary = other.coverDropshipSecondary;        
            coverBuildingSecondary = other.coverBuildingSecondary;   
            coverLocSecondary = other.coverLocSecondary;
        }           
        
        blocked |= other.blocked;
        infProtected |= other.infProtected;
        plantedFields += other.plantedFields;
        heavyIndustrial += other.heavyIndustrial;
        lightWoods += other.lightWoods;
        heavyWoods += other.heavyWoods;
        ultraWoods += other.ultraWoods;
        lightSmoke += other.lightSmoke;
        heavySmoke += other.heavySmoke;
        buildingLevelsOrHexes += other.buildingLevelsOrHexes;
        screen += other.screen;
        softBuildings += other.softBuildings;
        hardBuildings += other.hardBuildings;
        blockedByHill |= other.blockedByHill;
        blockedByWater |= other.blockedByWater;
        targetCover |= other.targetCover;
        attackerCover |= other.attackerCover;
        if ((null != thruBldg) && !thruBldg.equals(other.thruBldg)) {
            thruBldg = null;
        }     
    }

    public int getPlantedFields() {
        return plantedFields;
    }

    public int getHeavyIndustrial() {
        return heavyIndustrial;
    }

    public int getLightWoods() {
        return lightWoods;
    }

    public int getHeavyWoods() {
        return heavyWoods;
    }

    public int getUltraWoods() {
        return ultraWoods;
    }

    public int getLightSmoke() {
        return lightSmoke;
    }

    public int getHeavySmoke() {
        return heavySmoke;
    }

    public int getScreen() {
        return screen;
    }

    public int getSoftBuildings() {
        return softBuildings;
    }

    public int getHardBuildings() {
        return hardBuildings;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public boolean isBlockedByHill() {
        return blockedByHill;
    }

    public boolean isBlockedByWater() {
        return blockedByWater;
    }

    /**
     * Getter for property targetCover.
     *
     * @return Value of property targetCover.
     */
    public boolean isTargetCover() {
        return targetCover >= COVER_LOWLEFT;
    }

    public int getTargetCover() {
        return targetCover;
    }

    /**
     * Setter for property targetCover.
     *
     * @param targetCover New value of property targetCover.
     */
    public void setTargetCover(int targetCover) {
        this.targetCover = targetCover;
    }

    /**
     * Getter for property attackerCover.
     *
     * @return Value of property attackerCover.
     */
    public boolean isAttackerCover() {
        return attackerCover >= COVER_LOWLEFT;
    }

    public int getAttackerCover() {
        return attackerCover;
    }

    /**
     * Setter for property attackerCover.
     *
     * @param attackerCover New value of property attackerCover.
     */
    public void setAttackerCover(int attackerCover) {
        this.attackerCover = attackerCover;
    }

    /**
     * Getter for property thruBldg.
     *
     * @return Value of property thruBldg.
     */
    public Building getThruBldg() {
        return thruBldg;
    }

    /**
     * Setter for property thruBldg.
     *
     * @param thruBldg New value of property thruBldg.
     */
    public void setThruBldg(Building thruBldg) {
        this.thruBldg = thruBldg;
    }

    /**
     * LOS check from ae to te.
     */
    public boolean canSee() {
        return hasLoS;// !blocked && (lightWoods + lightSmoke) + ((heavyWoods
                        // + heavySmoke) * 2) < 3;
    }

    /**
     * Returns a LosEffects object representing the LOS effects of interveing
     * terrain between the attacker and target. Checks to see if the attacker
     * and target are at an angle where the LOS line will pass between two
     * hexes. If so, calls losDivided, otherwise calls losStraight.
     */
    public static LosEffects calculateLos(IGame game, int attackerId,
            Targetable target) {
        return calculateLos(game, attackerId, target, false);
    }

    public static LosEffects calculateLos(IGame game, int attackerId,
            Targetable target, boolean spotting) {
        //we need an extra step here, because units with secondary position can calculate LoS
        //from hexes other than that returned from getPosition()
        final Entity ae = game.getEntity(attackerId);
        //create a vector of attacker position and a vector of target positions - double loop through them
        //both and select the best one
        Vector<Coords> attackPos = new Vector<Coords>();
        attackPos.add(ae.getPosition());
        Vector<Coords> targetPos = new Vector<Coords>();
        targetPos.add(target.getPosition());
        //if a grounded dropship is the attacker, then it gets to choose the best secondary position for LoS
        if(ae instanceof Dropship && !ae.getSecondaryPositions().isEmpty()) {
            attackPos = new Vector<Coords>();
            for(int key : ae.getSecondaryPositions().keySet()) {
                attackPos.add(ae.getSecondaryPositions().get(key));
            }
        }
        //if a grounded dropship is the target, then the attacker chooses the best secondary position
        if(target instanceof Dropship && !((Entity)target).getSecondaryPositions().isEmpty()) {
            targetPos = new Vector<Coords>();
            for(int key : ((Entity)target).getSecondaryPositions().keySet()) {
                targetPos.add(((Entity)target).getSecondaryPositions().get(key));
            }
        }
        LosEffects bestLos = null;  
        for(Coords apos : attackPos) {
            for(Coords tpos : targetPos) {         
                LosEffects newLos = calculateLos(game, attackerId, target, apos, tpos, spotting);
                //is the new one better?
                if(null == bestLos 
                        || bestLos.isBlocked() 
                        || newLos.losModifiers(game).getValue() < bestLos.losModifiers(game).getValue()) {
                    bestLos = newLos;
                }
            }
        }
        bestLos.targetLoc = target.getPosition();
        return bestLos;
    }
    

    public static LosEffects calculateLos(IGame game, int attackerId, Targetable target,
            Coords attackPos, Coords targetPos, boolean spotting) {
        final Entity ae = game.getEntity(attackerId);

        // LOS fails if one of the entities is not deployed.
        if ((null == attackPos) || (null == targetPos)
                || ae.isOffBoard() || target.isOffBoard()) {
            LosEffects los = new LosEffects();
            los.blocked = true; // TODO: come up with a better "impossible"
            los.hasLoS = false;
            los.targetLoc = target.getPosition();
            return los;
        }

        IHex attHex = game.getBoard().getHex(attackPos);
        IHex targetHex = game.getBoard().getHex(targetPos);
        if ((attHex == null) || (targetHex == null)) {
            LosEffects los = new LosEffects();
            los.blocked = true; // TODO: come up with a better "impossible"
            los.hasLoS = false;
            los.targetLoc = target.getPosition();
            return los;
        }

        final AttackInfo ai = new AttackInfo();
        ai.attackerIsMech = ae instanceof Mech;
        ai.attackPos = attackPos;
        ai.attackerId = ae.getId();
        ai.targetPos = targetPos;
        ai.targetEntity = target.getTargetType() == Targetable.TYPE_ENTITY;
        if(ai.targetEntity) {
            ai.targetId = ((Entity)target).getId();
            ai.targetIsMech = target instanceof Mech;
        }else {
            ai.targetIsMech = false;
        }
        
        ai.targetInfantry = target instanceof Infantry;
        ai.attackHeight = ae.getHeight();
        ai.targetHeight = target.getHeight();

        int attEl = ae.relHeight() + attHex.getLevel();
        // for spotting, a mast mount raises our elevation by 1
        if (spotting && ae.hasWorkingMisc(MiscType.F_MAST_MOUNT, -1)) {
            attEl += 1;
        }
        int targEl = target.relHeight() + targetHex.getLevel();

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
            attUnderWater = attHex.containsTerrain(Terrains.WATER)
                    && (attHex.depth() > 0) && (attEl < attHex.surface());
            attInWater = attHex.containsTerrain(Terrains.WATER)
                    && (attHex.depth() > 0) && (attEl == attHex.surface());
            attOnLand = !(attUnderWater || attInWater);
        }

        boolean targetOffBoard = !game.getBoard()
                .contains(targetPos);
        boolean targetUnderWater;
        boolean targetInWater;
        boolean targetOnLand;
        if (targetOffBoard) {
            targetUnderWater = true;
            targetInWater = false;
            targetOnLand = true;
        } else {
            targetUnderWater = targetHex.containsTerrain(Terrains.WATER)
                    && (targetHex.depth() > 0) && (targEl < targetHex.surface());
            targetInWater = targetHex.containsTerrain(Terrains.WATER)
                    && (targetHex.depth() > 0) && (targEl == targetHex.surface());
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
        // Handle minimum water depth.
        // Applies to Torpedos.
        if (ai.attOnLand || ai.targetOnLand) {
            ai.minimumWaterDepth = 0;
        } else if (ai.attInWater || ai.targetInWater) {
            ai.minimumWaterDepth = 1;
        } else if (ai.attUnderWater || ai.targetUnderWater) {
            ai.minimumWaterDepth = Math.min(
                    attHex.terrainLevel(Terrains.WATER), targetHex
                            .terrainLevel(Terrains.WATER));
        }

        //if this is an air to ground or ground to air attack or a ground to air,
        //treat the attacker's position as the same as the target's
        if(Compute.isAirToGround(ae, target) || Compute.isGroundToAir(ae, target)) {
            ai.attackPos = ai.targetPos;
        }

        LosEffects finalLoS = calculateLos(game, ai);
        finalLoS.setMinimumWaterDepth(ai.minimumWaterDepth);
        
        finalLoS.targetLoc = target.getPosition();
        return finalLoS;
    }

    public static LosEffects calculateLos(IGame game, AttackInfo ai) {
        if (ai.attOffBoard) {
            LosEffects los = new LosEffects();
            los.blocked = true;
            los.hasLoS = false;
            los.targetLoc = ai.targetPos;
            return los;
        }
        if ((ai.attOnLand && ai.targetUnderWater) || (ai.attUnderWater
                && ai.targetOnLand)) {
            LosEffects los = new LosEffects();
            los.blocked = true;
            los.hasLoS = false;
            los.blockedByWater = true;
            los.targetLoc = ai.targetPos;
            return los;
        }

        if(game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DEAD_ZONES) && isDeadZone(game, ai)) {
            LosEffects los = new LosEffects();
            los.blocked = true;
            los.blockedByHill = true;
            los.deadZone = true;
            los.hasLoS = false;
            los.targetLoc = ai.targetPos;
            return los;
        }
        
        boolean diagramLos = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS1);
        boolean partialCover = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_PARTIAL_COVER);
        double degree = ai.attackPos.degree(ai.targetPos);
        LosEffects finalLoS;
        if (degree % 60 == 30) {
            finalLoS = LosEffects.losDivided(game, ai, diagramLos, partialCover);
        } else {
            finalLoS = LosEffects.losStraight(game, ai, diagramLos, partialCover);
        }
        
        finalLoS.hasLoS = !finalLoS.blocked && 
                            (finalLoS.screen < 1) && 
                            (finalLoS.plantedFields < 6) && 
                            (finalLoS.heavyIndustrial < 3) && 
                           ((finalLoS.lightWoods + finalLoS.lightSmoke)
                             + ((finalLoS.heavyWoods + finalLoS.heavySmoke) * 2)
                             + (finalLoS.ultraWoods * 3) < 3);
        
        finalLoS.targetLoc = ai.targetPos;
        return finalLoS;
    }

    /**
     * Returns ToHitData indicating the modifiers to fire for the specified LOS
     * effects data.
     */
    public ToHitData losModifiers(IGame game) {
        return losModifiers(game, 0, false);
    }
    
    public ToHitData losModifiers(IGame game, boolean underWaterWeapon) {
        return losModifiers(game, 0, underWaterWeapon);
    }

    public ToHitData losModifiers(IGame game, int eistatus, boolean underwaterWeapon) {
        ToHitData modifiers = new ToHitData();

        if ( arcedShot ) {
            return modifiers;
        }

        /*
        if (deadZone) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "LOS blocked by dead zone.");
        }
        */

        if (blocked) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "LOS blocked by terrain.");
        }

        if (infProtected) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Infantry protected by building.");
        }

        if (buildingLevelsOrHexes > 2) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "LOS blocked by building hexes or levels.");
        }

        if ((ultraWoods >= 1) || (lightWoods + (heavyWoods * 2) > 2)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "LOS blocked by woods.");
        }

        if (!underwaterWeapon && (lightSmoke + (heavySmoke * 2) > 2)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "LOS blocked by smoke.");
        }

        if(plantedFields > 5) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "LOS blocked by planted fields.");
        }

        if(heavyIndustrial > 2) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "LOS blocked by heavy industrial zones.");
        }

        if (screen > 0) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "LOS blocked by screen.");
        }

        if (!underwaterWeapon && (lightSmoke + (heavySmoke * 2) + lightWoods + (heavyWoods * 2) > 2)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "LOS blocked by smoke and woods.");
        }

        if(plantedFields > 0) {
            modifiers.addModifier((int)Math.floor(plantedFields / 2.0), plantedFields
                    + " intervening planted fields");
        }

        if(heavyIndustrial > 0) {
            modifiers.addModifier(heavyIndustrial, heavyIndustrial
                    + " intervening heavy industrial zones");
        }

        if (lightWoods > 0) {
            if (eistatus > 0) {
                modifiers.addModifier(1,
                        "firing through light woods with EI system");
            } else {
                modifiers.addModifier(lightWoods, lightWoods
                        + " intervening light woods");
            }
        }

        if (buildingLevelsOrHexes > 0) {
            if (eistatus > 0) {
                modifiers.addModifier(1,
                        "firing through building hex/level with EI system");
            } else {
                modifiers.addModifier(buildingLevelsOrHexes, buildingLevelsOrHexes
                        + " intervening building levels or hexes");
            }
        }

        if (heavyWoods > 0) {
            if (eistatus > 0) {
                modifiers.addModifier(heavyWoods, heavyWoods
                        + " intervening heavy woods");
            } else {
                modifiers.addModifier(heavyWoods * 2, heavyWoods
                        + " intervening heavy woods");
            }
        }

        if (lightSmoke > 0 && !underwaterWeapon) {
            modifiers.addModifier(lightSmoke, lightSmoke
                    + " intervening light smoke");
        }

        if (heavySmoke > 0 && !underwaterWeapon) {
            StringBuffer text = new StringBuffer(heavySmoke);
            text.append(" intervening");
            text.append(" heavy");
            text.append(" smoke");
            if (eistatus > 0) {
                modifiers.addModifier(heavySmoke, text.toString());
            } else {
                modifiers.addModifier(heavySmoke * 2, text.toString());
            }
        }

        if (targetCover != COVER_NONE) {
            if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_PARTIAL_COVER)) {
                if ((targetCover == COVER_75LEFT) || (targetCover == COVER_75RIGHT)) {
                    modifiers.addModifier(1, "target has 75% cover");
                } else if (targetCover >= COVER_HORIZONTAL) {
                    modifiers.addModifier(1, "target has 50% cover");
                } else {
                    // no bth mod for 25% cover
                    modifiers.addModifier(0, "target has 25% cover");
                }
            } else {
                modifiers.addModifier(1, "target has partial cover");
                modifiers.setHitTable(ToHitData.HIT_PARTIAL_COVER);
            }
        }

        return modifiers;
    }

    /**
     * Returns LosEffects for a line that never passes exactly between two
     * hexes. Since intervening() returns all the coordinates, we just add the
     * effects of all those hexes.
     */
    private static LosEffects losStraight(IGame game, AttackInfo ai, 
            boolean diagramLoS, boolean partialCover) {
        ArrayList<Coords> in = Coords.intervening(ai.attackPos, ai.targetPos);
        LosEffects los = new LosEffects();
        boolean targetInBuilding = false;
        if (ai.targetEntity) {
            targetInBuilding = Compute.isInBuilding(game, ai.targetAbsHeight
                    - game.getBoard().getHex(ai.targetPos).surface(),
                    ai.targetPos);
        }

        // If the target and attacker are both in a
        // building, set that as the first LOS effect.
        if (targetInBuilding
                && Compute.isInBuilding(game, ai.attackAbsHeight
                        - game.getBoard().getHex(ai.attackPos).surface(),
                        ai.attackPos)) {
            los.setThruBldg(game.getBoard().getBuildingAt(in.get(0)));
            //elevation differences count as building hexes passed through
            los.buildingLevelsOrHexes += (Math.abs((ai.attackAbsHeight-ai.attackHeight) - (ai.targetAbsHeight-ai.targetHeight)));
        }

        for (Coords c : in) {
            los.add(LosEffects.losForCoords(game, ai, c, los.getThruBldg(),
                    diagramLoS, partialCover));
        }      

        if ((ai.minimumWaterDepth < 1) && ai.underWaterCombat) {
            los.blocked = true;
        }

        // Infantry inside a building can only be
        // targeted by units in the same building.
        if (ai.targetInfantry && targetInBuilding && (null == los.getThruBldg())) {
            los.infProtected = true;
        }

        return los;
    }

    /**
     * Returns LosEffects for a line that passes between two hexes at least
     * once. The rules say that this situation is resolved in favor of the
     * defender.
     *
     * The intervening() function returns both hexes in these
     * circumstances, and, when they are in line order, it's not hard to figure
     * out which hexes are split and which are not.
     *
     * The line always looks like:
     *        ___     ___
     *    ___/ 1 \___/...\___
     *   / 0 \___/ 3 \___/etc\
     *   \___/ 2 \___/...\___/
     *       \___/   \___/
     * We go thru and figure out the modifiers for the non-split hexes first.
     * Then we go to each of the two split hexes and determine
     * which gives us the bigger modifier. We use the bigger modifier.
     *
     * This is not perfect as it takes partial cover as soon as it can, when
     * perhaps later might be better.
     * Also, it doesn't account for the fact that
     * attacker partial cover blocks leg weapons, as we want to return the same
     * sequence regardless of what weapon is attacking.
     */
    private static LosEffects losDivided(IGame game, AttackInfo ai,
            boolean diagramLoS, boolean partialCover) {
        ArrayList<Coords> in = Coords.intervening(ai.attackPos, ai.targetPos,
                true);
        LosEffects los = new LosEffects();
        boolean targetInBuilding = false;
        if (ai.targetEntity) {
            targetInBuilding = Compute.isInBuilding(game, ai.targetAbsHeight
                    - game.getBoard().getHex(ai.targetPos).surface(),
                    ai.targetPos);
        }

        // If the target and attacker are both in a
        // building, set that as the first LOS effect.
        if (targetInBuilding
                && Compute.isInBuilding(game, ai.attackAbsHeight
                        - game.getBoard().getHex(ai.attackPos).surface(),
                        ai.attackPos)) {
            los.setThruBldg(game.getBoard().getBuildingAt(in.get(0)));
            //elevation differences count as building hexes passed through
            los.buildingLevelsOrHexes += (Math
                    .abs((ai.attackAbsHeight - ai.attackHeight)
                            - (ai.targetAbsHeight - ai.targetHeight)));
        }

        // add non-divided line segments
        for (int i = 3; i < in.size() - 2; i += 3) {
            los.add(losForCoords(game, ai, in.get(i), los.getThruBldg(),
                    diagramLoS, partialCover));
        }

        if ((ai.minimumWaterDepth < 1) && ai.underWaterCombat) {
            los.blocked = true;
        }

        // if blocked already, return that
        if (los.losModifiers(game).getValue() == TargetRoll.IMPOSSIBLE) {
            return los;
        }

        // If there src & dst hexes are the same, nothing to do
        if (in.size() < 2) {
            return los;
        }

        // go through divided line segments
        LosEffects totalLeftLos = new LosEffects();
        LosEffects totalRightLos = new LosEffects();
        for (int i = 1; i < in.size() - 2; i += 3) {
            LosEffects leftLos = losForCoords(game, ai, in.get(i), los
                    .getThruBldg(), diagramLoS, partialCover);
            LosEffects rightLos = losForCoords(game, ai, in.get(i + 1), los
                    .getThruBldg(), diagramLoS, partialCover);

            // Infantry inside a building can only be
            // targeted by units in the same building.
            if (ai.targetInfantry && targetInBuilding) {
                if (null == leftLos.getThruBldg()) {
                    leftLos.infProtected = true;
                } else if (null == rightLos.getThruBldg()) {
                    rightLos.infProtected = true;
                }
            }
       
            // Check for advanced cover, only 'mechs can get partial cover
            if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_PARTIAL_COVER) && 
                    ai.targetIsMech) {
                // 75% and vertical cover will have blocked LoS
                boolean losBlockedByCover = false;
                if(leftLos.targetCover == COVER_HORIZONTAL && 
                        rightLos.targetCover == COVER_NONE) {
                    //25% cover, left
                    leftLos.targetCover  = COVER_LOWLEFT;
                    rightLos.targetCover = COVER_LOWLEFT;
                    rightLos.setCoverBuildingPrimary(leftLos.getCoverBuildingPrimary());
                    rightLos.setCoverDropshipPrimary(leftLos.getCoverDropshipPrimary());
                    rightLos.setDamagableCoverTypePrimary(leftLos.getDamagableCoverTypePrimary());
                    rightLos.setCoverLocPrimary(leftLos.getCoverLocPrimary());
                }else if ((leftLos.targetCover == COVER_NONE && 
                          rightLos.targetCover == COVER_HORIZONTAL)) {
                    //25% cover, right
                    leftLos.targetCover  = COVER_LOWRIGHT;
                    rightLos.targetCover = COVER_LOWRIGHT;
                    leftLos.setCoverBuildingPrimary(rightLos.getCoverBuildingPrimary());
                    leftLos.setCoverDropshipPrimary(rightLos.getCoverDropshipPrimary());
                    leftLos.setDamagableCoverTypePrimary(rightLos.getDamagableCoverTypePrimary());
                    leftLos.setCoverLocPrimary(rightLos.getCoverLocPrimary());
                } else if(leftLos.targetCover == COVER_FULL && 
                         rightLos.targetCover == COVER_NONE){
                    //vertical cover, left
                    leftLos.targetCover  = COVER_LEFT;
                    rightLos.targetCover = COVER_LEFT;
                    rightLos.setCoverBuildingPrimary(leftLos.getCoverBuildingPrimary());
                    rightLos.setCoverDropshipPrimary(leftLos.getCoverDropshipPrimary());
                    rightLos.setDamagableCoverTypePrimary(leftLos.getDamagableCoverTypePrimary());
                    rightLos.setCoverLocPrimary(leftLos.getCoverLocPrimary());
                    losBlockedByCover = true;
                }else if (leftLos.targetCover == COVER_NONE && 
                         rightLos.targetCover == COVER_FULL) {
                    //vertical cover, right
                    leftLos.targetCover  = COVER_RIGHT;
                    rightLos.targetCover = COVER_RIGHT;
                    leftLos.setCoverBuildingPrimary(rightLos.getCoverBuildingPrimary());
                    leftLos.setCoverDropshipPrimary(rightLos.getCoverDropshipPrimary());
                    leftLos.setDamagableCoverTypePrimary(rightLos.getDamagableCoverTypePrimary());
                    leftLos.setCoverLocPrimary(rightLos.getCoverLocPrimary());
                    losBlockedByCover = true;
                } else if(leftLos.targetCover == COVER_FULL && 
                         rightLos.targetCover == COVER_HORIZONTAL){
                    //75% cover, left
                    leftLos.targetCover  = COVER_75LEFT;
                    rightLos.targetCover = COVER_75LEFT;
                    setSecondaryCover(leftLos,rightLos);                                       
                    losBlockedByCover = true;                    
                } else if (leftLos.targetCover == COVER_HORIZONTAL && 
                          rightLos.targetCover == COVER_FULL) { 
                    //75% cover, right
                    leftLos.targetCover  = COVER_75RIGHT;
                    rightLos.targetCover = COVER_75RIGHT;
                    setSecondaryCover(leftLos,rightLos);
                    losBlockedByCover = true;
                } else if (leftLos.targetCover == COVER_HORIZONTAL && 
                        rightLos.targetCover == COVER_HORIZONTAL) { 
                    //50% cover
                    //Cover will be set properly, but we need to set secondary
                    // cover in case there are two buildings providing 25% cover
                    setSecondaryCover(leftLos,rightLos);
                }
                //In the case of vertical and 75% cover, LoS will be blocked.  
                // We need to unblock it, unless Los is already blocked.
                if (!los.blocked && (!leftLos.blocked || !rightLos.blocked) &&
                        losBlockedByCover){                   
                    leftLos.blocked = false;
                    rightLos.blocked = false;
                }                
            }
            
            if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_PARTIAL_COVER) && 
                    ai.attackerIsMech) {
                // 75% and vertical cover will have blocked LoS
                boolean losBlockedByCover = false;
                if(leftLos.attackerCover == COVER_HORIZONTAL && 
                        rightLos.attackerCover == COVER_NONE) {
                    //25% cover, left
                    leftLos.attackerCover  = COVER_LOWLEFT;
                    rightLos.attackerCover = COVER_LOWLEFT;
                    rightLos.targetCover = COVER_LOWLEFT;
                    rightLos.setCoverBuildingPrimary(leftLos.getCoverBuildingPrimary());
                    rightLos.setCoverDropshipPrimary(leftLos.getCoverDropshipPrimary());
                    rightLos.setDamagableCoverTypePrimary(leftLos.getDamagableCoverTypePrimary());
                    rightLos.setCoverLocPrimary(leftLos.getCoverLocPrimary());
                }else if ((leftLos.attackerCover == COVER_NONE && 
                          rightLos.attackerCover == COVER_HORIZONTAL)) {
                    //25% cover, right
                    leftLos.attackerCover  = COVER_LOWRIGHT;
                    rightLos.attackerCover = COVER_LOWRIGHT;
                    leftLos.setCoverBuildingPrimary(rightLos.getCoverBuildingPrimary());
                    leftLos.setCoverDropshipPrimary(rightLos.getCoverDropshipPrimary());
                    leftLos.setDamagableCoverTypePrimary(rightLos.getDamagableCoverTypePrimary());
                    leftLos.setCoverLocPrimary(rightLos.getCoverLocPrimary());
                } else if(leftLos.attackerCover == COVER_FULL && 
                         rightLos.attackerCover == COVER_NONE){
                    //vertical cover, left
                    leftLos.attackerCover  = COVER_LEFT;
                    rightLos.attackerCover = COVER_LEFT;
                    losBlockedByCover = true;
                }else if (leftLos.attackerCover == COVER_NONE && 
                         rightLos.attackerCover == COVER_FULL) {
                    //vertical cover, right
                    leftLos.attackerCover  = COVER_RIGHT;
                    rightLos.attackerCover = COVER_RIGHT;
                    losBlockedByCover = true;
                } else if(leftLos.attackerCover == COVER_FULL && 
                         rightLos.attackerCover == COVER_HORIZONTAL){
                    //75% cover, left
                    leftLos.attackerCover  = COVER_75LEFT;
                    rightLos.attackerCover = COVER_75LEFT;   
                    losBlockedByCover = true;
                } else if (leftLos.attackerCover == COVER_HORIZONTAL && 
                          rightLos.attackerCover == COVER_FULL) { 
                    //75% cover, right
                    leftLos.attackerCover  = COVER_75RIGHT;
                    rightLos.attackerCover = COVER_75RIGHT;
                    losBlockedByCover = true;
                }
                
                //In the case of vertical and 75% cover, LoS will be blocked.  
                // We need to unblock it, unless Los is already blocked.
                if (!los.blocked && (!leftLos.blocked || !rightLos.blocked) &&
                        losBlockedByCover){                   
                    leftLos.blocked = false;
                    rightLos.blocked = false;
                }                
            }
            totalLeftLos.add(leftLos);
            totalRightLos.add(rightLos);           
        }
        //Determine whether left or right is worse and update los with it
        int lVal = totalLeftLos.losModifiers(game).getValue();
        int rVal = totalRightLos.losModifiers(game).getValue();
        if ((lVal > rVal) || 
                ((lVal == rVal) && totalLeftLos.isAttackerCover())) {
            los.add(totalLeftLos);
        } else {
            los.add(totalRightLos);
        }
        return los;
    }
    
    /**
     * Convenience method for setting the secondary cover values.  The left LoS
     * has retains it's primary cover, and its secondary cover becomes the 
     * primary of the right los while the right los has its primary become 
     * secondary and its primary becomes the primary of the left side.
     * This ensures that the primary protects the left side and the secondary
     * protects the right side which is important to determine which to pick
     * later on when damage is handled.
     * 
     * @param leftLos  The left side of the line of sight for a divided hex 
     *                  LoS computation
     * @param rightLos The right side of the line of sight for a dividied hex
     *                  LoS computation
     */
    private static void setSecondaryCover(LosEffects leftLos, LosEffects rightLos){
        //Set left secondary to right primary
        leftLos.setDamagableCoverTypeSecondary(rightLos.getDamagableCoverTypePrimary());
        leftLos.setCoverBuildingSecondary(rightLos.getCoverBuildingPrimary());
        leftLos.setCoverDropshipSecondary(rightLos.getCoverDropshipPrimary());
        leftLos.setCoverLocSecondary(rightLos.getCoverLocPrimary());        
        //Set right secondary to right primary
        rightLos.setDamagableCoverTypeSecondary(rightLos.getDamagableCoverTypePrimary());
        rightLos.setCoverBuildingSecondary(rightLos.getCoverBuildingPrimary());
        rightLos.setCoverDropshipSecondary(rightLos.getCoverDropshipPrimary());
        rightLos.setCoverLocSecondary(rightLos.getCoverLocPrimary());
        //Set right primary to left primary
        rightLos.setDamagableCoverTypePrimary(leftLos.getDamagableCoverTypePrimary());
        rightLos.setCoverBuildingPrimary(leftLos.getCoverBuildingPrimary());
        rightLos.setCoverDropshipPrimary(leftLos.getCoverDropshipPrimary());        
        rightLos.setCoverLocPrimary(leftLos.getCoverLocPrimary());
    }

    /**
     * Returns a LosEffects object representing the LOS effects of anything at
     * the specified coordinate.
     */
    private static LosEffects losForCoords(IGame game, AttackInfo ai,
            Coords coords, Building thruBldg, 
            boolean diagramLoS, boolean partialCover) {
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
        if ((bldg != null) && bldg.equals(thruBldg)) {
            los.setThruBldg(thruBldg);
        }

        // ignore hexes the attacker or target are in
        if (coords.equals(ai.attackPos) || coords.equals(ai.targetPos)) {
            return los;
        }

        // we are an attack in a building, +1 for each building hex between the
        // 2 units
        if ((game.getBoard().getBuildingAt(ai.attackPos) != null)
                && (game.getBoard().getBuildingAt(ai.targetPos) != null)
                && (thruBldg != null)
                && game.getBoard().getBuildingAt(ai.attackPos).equals(game.getBoard().getBuildingAt(ai.targetPos))
                && ai.targetEntity && thruBldg.equals(game.getBoard().getBuildingAt(ai.attackPos))) {
            los.buildingLevelsOrHexes += 1;
        }

        IHex hex = game.getBoard().getHex(coords);
        int hexEl = ai.underWaterCombat ? hex.floor() : hex.surface();

        // Handle minimum water depth.
        // Applies to Torpedos.
        if (!(hex.containsTerrain(Terrains.WATER))) {
            ai.minimumWaterDepth = 0;
        } else if ((hex.terrainLevel(Terrains.WATER) >= 0)
                && ((ai.minimumWaterDepth == -1) || (hex
                        .terrainLevel(Terrains.WATER) < ai.minimumWaterDepth))) {
            ai.minimumWaterDepth = hex.terrainLevel(Terrains.WATER);
        }

        // Handle building elevation.
        // Attacks thru a building are not blocked by that building.
        // ASSUMPTION: bridges don't block LOS.
        int bldgEl = 0;
        if ((null == los.getThruBldg())
                && hex.containsTerrain(Terrains.BLDG_ELEV)) {
            bldgEl = hex.terrainLevel(Terrains.BLDG_ELEV);
        }
        
        if ((null == los.getThruBldg())
                && hex.containsTerrain(Terrains.FUEL_TANK_ELEV)
                && hex.terrainLevel(Terrains.FUEL_TANK_ELEV) > bldgEl) {
            bldgEl = hex.terrainLevel(Terrains.FUEL_TANK_ELEV);
        }

        boolean coveredByDropship = false;
        Entity coveringDropship = null;
        //check for grounded dropships - treat like a building 10 elevations tall
        if(bldgEl < 10) {
            for (Entity inHex : game.getEntitiesVector(coords)) {
                if(ai.attackerId == inHex.getId() || ai.targetId == inHex.getId()) {
                    continue;
                }
                if(inHex instanceof Dropship && !inHex.isAirborne() && !inHex.isSpaceborne()) {
                    bldgEl = 10;
                    coveredByDropship = true;
                    coveringDropship = inHex;
                }
            }
        }
        
        // TODO: Identify when LOS travels *above* a building's hex.
        // Alternatively, force all building hexes to be same height.

        // check for block by terrain

        // check for LOS according to diagramming rule from MaxTech, page 22
        int totalEl = hexEl + bldgEl;
        if (diagramLoS) {
            if (totalEl > (ai.targetAbsHeight
                    * ai.attackPos.distance(coords) + ai.attackAbsHeight
                    * ai.targetPos.distance(coords))
                    / (ai.targetPos.distance(coords) + ai.attackPos
                            .distance(coords))) {
                los.blocked = true;
                if(hex.terrainLevel(Terrains.BLDG_CF) > 90) {
                    los.hardBuildings++;
                } else if(bldgEl > 0) {
                    los.softBuildings++;
                } else {
                    los.blockedByHill = true;
                }
            }
        // check for LOS according to normal rules
        } else if (((totalEl > ai.attackAbsHeight) && 
                    (totalEl > ai.targetAbsHeight))
                || ((totalEl > ai.attackAbsHeight) && 
                        (ai.attackPos.distance(coords) == 1))
                || ((totalEl > ai.targetAbsHeight) && 
                        (ai.targetPos.distance(coords) == 1))) {
            los.blocked = true;
            if (hex.terrainLevel(Terrains.BLDG_CF) > 90) {
                los.hardBuildings++;
            } else if (bldgEl > 0) {
                los.softBuildings++;
            } else {
                los.blockedByHill = true;
            }
        }

        // check if there's a clear hex between the targets that's higher than
        // one of them, if we're in underwater combat
        if (ai.underWaterCombat
                && (hex.terrainLevel(Terrains.WATER) == ITerrain.LEVEL_NONE)
                && ((totalEl > ai.attackAbsHeight) || (totalEl > ai.targetAbsHeight))) {
            los.blocked = true;
        }

        // check for woods or smoke only if not under water
        if (!ai.underWaterCombat) {
            if(hex.containsTerrain(Terrains.SCREEN)) {
                //number of screens doesn't matter. One is enough to block
                los.screen++;
            }
            //heavy industrial zones can vary in height up to 10 levels, so lets
            //put all of this into a for loop
            int industrialLevel = hex.terrainLevel(Terrains.INDUSTRIAL);
            if (industrialLevel != ITerrain.LEVEL_NONE) {
                for (int level = 1; level < 11; level++) {
                    if (((hexEl + level > ai.attackAbsHeight) && (hexEl + level > ai.targetAbsHeight))
                            || ((hexEl + level > ai.attackAbsHeight) && (ai.attackPos
                                    .distance(coords) == 1))
                            || ((hexEl + level > ai.targetAbsHeight) && (ai.targetPos
                                    .distance(coords) == 1))) {
                        // check industrial zone
                        if (industrialLevel == level) {
                            los.heavyIndustrial++;
                        }
                    }
                }
            }
            //planted fields only rise one level above the terrain
            if (hex.containsTerrain(Terrains.FIELDS)) {
                if (((hexEl + 1 > ai.attackAbsHeight) && (hexEl + 2 > ai.targetAbsHeight))
                        || ((hexEl + 1 > ai.attackAbsHeight) && (ai.attackPos
                                .distance(coords) == 1))
                        || ((hexEl + 1 > ai.targetAbsHeight) && (ai.targetPos
                                .distance(coords) == 1))) {
                    los.plantedFields++;

                }
            }
            int smokeLevel = hex.terrainLevel(Terrains.SMOKE);
            int woodsLevel = hex.terrainLevel(Terrains.WOODS);
            int jungleLevel = hex.terrainLevel(Terrains.JUNGLE);
            // Check smoke, woods and jungle
            if ((smokeLevel != ITerrain.LEVEL_NONE) 
                    || (woodsLevel != ITerrain.LEVEL_NONE)
                    || (jungleLevel != ITerrain.LEVEL_NONE)) {
                // Regular smoke/woods/jungle rise 2 levels above the hex level
                int terrainEl = hexEl + 2;
                boolean affectsLoS;
                if (diagramLoS) {
                    affectsLoS = terrainEl > (ai.targetAbsHeight
                            * ai.attackPos.distance(coords) + ai.attackAbsHeight
                            * ai.targetPos.distance(coords))
                            / (ai.targetPos.distance(coords) + ai.attackPos
                                    .distance(coords));
                } else {
                    affectsLoS = ((terrainEl > ai.attackAbsHeight) && (terrainEl > ai.targetAbsHeight))
                            || ((terrainEl > ai.attackAbsHeight) && (ai.attackPos
                                    .distance(coords) == 1))
                            || ((terrainEl > ai.targetAbsHeight) && (ai.targetPos
                                    .distance(coords) == 1));
                }
                if (affectsLoS) {
                    // smoke and woods stack for LOS so check them both
                    if ((smokeLevel == SmokeCloud.SMOKE_LIGHT)
                            || (smokeLevel == SmokeCloud.SMOKE_LI_LIGHT)
                            || (smokeLevel == SmokeCloud.SMOKE_LI_HEAVY)
                            || (smokeLevel == SmokeCloud.SMOKE_CHAFF_LIGHT)) {
                        los.lightSmoke++;
                    } else if ((smokeLevel == SmokeCloud.SMOKE_HEAVY)) {
                        los.heavySmoke++;
                    }
                    // Check woods/jungle
                    if ((woodsLevel == 1) || (jungleLevel == 1)) {
                        los.lightWoods++;
                    } else if ((woodsLevel == 2) || (jungleLevel == 2)) {
                        los.heavyWoods++;
                    }
                }
                
                // Ultra woods/jungle rise 3 levels above the hex level
                terrainEl = hexEl + 3;
                if (diagramLoS) {
                    affectsLoS = terrainEl > (ai.targetAbsHeight
                            * ai.attackPos.distance(coords) + ai.attackAbsHeight
                            * ai.targetPos.distance(coords))
                            / (ai.targetPos.distance(coords) + ai.attackPos
                                    .distance(coords));
                } else {
                    affectsLoS = ((terrainEl > ai.attackAbsHeight) && (terrainEl > ai.targetAbsHeight))
                            || ((terrainEl > ai.attackAbsHeight) && (ai.attackPos
                                    .distance(coords) == 1))
                            || ((terrainEl > ai.targetAbsHeight) && (ai.targetPos
                                    .distance(coords) == 1));
                }
                if (affectsLoS) {
                    if ((woodsLevel == 3) || (jungleLevel == 3)) {
                        los.ultraWoods++;
                    }
                }
            }
            
        }

        // Partial Cover related code        
        boolean potentialCover = false;
        // check for target partial cover
        if (ai.targetPos.distance(coords) == 1 && ai.targetIsMech){
            if (los.blocked && partialCover) {
                los.targetCover = COVER_FULL; 
                potentialCover = true;
            } else if  ((totalEl == ai.targetAbsHeight)
                    && (ai.attackAbsHeight <= ai.targetAbsHeight)
                    && (ai.targetHeight > 0)) {
                los.targetCover |= COVER_HORIZONTAL; 
                potentialCover = true;
            }
        }
        // check for attacker partial (horizontal) cover
        if (ai.attackPos.distance(coords) == 1 && ai.attackerIsMech) {
            if (los.blocked && partialCover) {
                los.attackerCover = COVER_FULL; 
                potentialCover = true;
            } else if  ((totalEl == ai.attackAbsHeight)
                    && (ai.attackAbsHeight >= ai.targetAbsHeight)
                    && (ai.attackHeight > 0)) {
                los.attackerCover |= COVER_HORIZONTAL; 
                potentialCover = true;
            }
        }    
        
        //If there's a partial cover situation, we may need to keep track of 
        // damagable assets that are providing cover, so we can damage them if
        // they block a shot.
        if (potentialCover){
            if (coveredByDropship){
                los.setDamagableCoverTypePrimary(DAMAGABLE_COVER_DROPSHIP);
                los.coverDropshipPrimary = coveringDropship;
            }else if (bldg != null){
                los.setDamagableCoverTypePrimary(DAMAGABLE_COVER_BUILDING);
                los.coverBuildingPrimary = bldg;                    
            }
            else {
                los.setDamagableCoverTypePrimary(DAMAGABLE_COVER_NONE);
            }
            los.coverLocPrimary = coords;
        }      

        return los;
    }

    public static boolean hasFireBetween(Coords start, Coords end, IGame game) {

        ArrayList<Coords> in = Coords.intervening(start, end);
        for ( Coords hex : in ) {
            // ignore off-board hexes
            if (!game.getBoard().contains(hex)) {
                continue;
            }
            if ( game.getBoard().getHex(hex).containsTerrain(Terrains.FIRE) ) {
                return true;
            }
        }
        return false;
    }

    public void setArcedAttack(boolean attack){
        arcedShot = attack;
    }

    /**
     * Build line of sight effects between coordinates c1 and c2 at height h1
     * and h2 respectivly.
     *
     * @param c1 the source coordiantes.
     * @param c2 the target coordinates.
     * @param h1 the height in the source tile that is being shot from.
     * @param h2 the height of the target tile to shoot for.
     * @return an attackInfo object that describes the apliable modifiers.
     */
    public static LosEffects.AttackInfo buildAttackInfo(Coords c1, Coords c2, int h1,
            int h2, int h1Floor, int h2Floor) {
        LosEffects.AttackInfo ai = new LosEffects.AttackInfo();
        ai.attackPos = c1;
        ai.targetPos = c2;
        ai.attackHeight = h1;
        ai.targetHeight = h2;
        ai.attackAbsHeight = h1Floor + h1;
        ai.targetAbsHeight = h2Floor + h2;
        return ai;
    }

    /**
     * find out if the left or right side of the divided LOS is better for the
     * target
     * return 0 if right is better, 1 if left is better, 2 if both are equal
     * @param in
     * @param game
     * @param ai
     * @param targetInBuilding
     * @param los
     */
    public static int dividedLeftBetter(ArrayList<Coords> in, IGame game,
            AttackInfo ai, boolean targetInBuilding, LosEffects los) {
        boolean diagramLos = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS1);
        boolean partialCover = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_PARTIAL_COVER);
        LosEffects leftTotal = new LosEffects();
        LosEffects rightTotal = new LosEffects();
        for (int i = 1; i < in.size() - 2; i += 3) {
            // get effects of each side
            LosEffects left = losForCoords(game, ai, in.get(i), los
                    .getThruBldg(), diagramLos, partialCover);
            LosEffects right = losForCoords(game, ai, in.get(i + 1), los
                    .getThruBldg(), diagramLos, partialCover);

            // Include all previous LOS effects.
            left.add(los);
            right.add(los);

            // Infantry inside a building can only be
            // targeted by units in the same building.
            if (ai.targetInfantry && targetInBuilding) {
                if (null == left.getThruBldg()) {
                    left.infProtected = true;
                } else if (null == right.getThruBldg()) {
                    right.infProtected = true;
                }
            }

            // which is better?
            int lVal = left.losModifiers(game).getValue();
            int rVal = right.losModifiers(game).getValue();
            if ((lVal > rVal) || ((lVal == rVal) && left.isAttackerCover())) {
                leftTotal.add(left);
            } else {
                rightTotal.add(right);
            }
        }
        int leftTotalValue = leftTotal.losModifiers(game).getValue();
        int rightTotalValue = rightTotal.losModifiers(game).getValue();
        if (leftTotalValue > rightTotalValue) {
            return 1;
        } else if (leftTotalValue < rightTotalValue) {
            return 0;
        } else {
            return 2;
        }
    }

    private static boolean isDeadZone(IGame game, AttackInfo ai) {
        //determine who is higher and who is lower
        int highElev = ai.attackAbsHeight;
        int lowElev = ai.targetAbsHeight;
        Coords highPos = ai.attackPos;
        Coords lowPos = ai.targetPos;
        if(highElev < lowElev) {
            highElev = ai.targetAbsHeight;
            lowElev = ai.attackAbsHeight;
            highPos = ai.targetPos;
            lowPos = ai.attackPos;
        }
        //TODO: check if this works right for splits (thinks like expanded partial cover for example)
        ArrayList<Coords> in = Coords.intervening(lowPos, highPos, true);
        int IntElev = lowElev;
        Coords IntPos = lowPos;
        for(Coords c : in) {
            // ignore off-board coords
            if (!game.getBoard().contains(c)) {
                continue;
            }
           if(!c.equals(lowPos)) {
               IHex hex = game.getBoard().getHex(c);
               int hexEl = ai.underWaterCombat ? hex.floor() : hex.surface();
               // Handle building elevation.
               // Attacks thru a building are not blocked by that building.
               // ASSUMPTION: bridges don't block LOS.
               int bldgEl = 0;
               if (hex.containsTerrain(Terrains.BLDG_ELEV)) {
                   bldgEl = hex.terrainLevel(Terrains.BLDG_ELEV);
               }
               //check for grounded dropships - treat like a building 10 elevations tall
               if(bldgEl < 10) {
                   for (Entity inHex : game.getEntitiesVector(c)) {
                       if(ai.attackerId == inHex.getId() || ai.targetId == inHex.getId()) {
                           continue;
                       }
                       if(inHex instanceof Dropship && !inHex.isAirborne() && !inHex.isSpaceborne()) {
                           bldgEl = 10;
                       }
                   }
               }
               int totalEl = hexEl + bldgEl;
               if(totalEl > IntElev) {
                   IntElev = totalEl;
                   IntPos = c;
               }
           }
        }
        //the intervening hex cannot be either the low or high position
        if(!IntPos.equals(lowPos) && !IntPos.equals(highPos)) {
            return  0 < 2 * (2*IntElev - highElev - lowElev) + IntPos.distance(highPos) - IntPos.distance(lowPos);
        }
        return false;
    }
    
     
    /**
     * Returns the text name of a particular type of cover, given its id.
     * TacOps partial cover is assigned from the perspective of the attacker,
     * so it's possible that the sides should be switched to make sense
     * from the perspective of the target.
     * 
     * @param cover  The int id that represents the cover type.
     * @param switchSides A boolean that determines if left/right side should
     *                     be switched.  This is useful since cover is given
     *                     from the perspective of the attacker, and the sides
     *                     need to be switched for the target.
     * @return
     */
    static public String getCoverName(int cover, boolean switchSides){
        switch (cover)
        {
            case COVER_NONE:
                return Messages.getString("LosEffects.name_cover_none");
            case COVER_LOWLEFT:
                if (switchSides)
                    return Messages.getString("LosEffects.name_cover_lowright");
                else
                    return Messages.getString("LosEffects.name_cover_lowleft");
            case COVER_LOWRIGHT:
                if (switchSides)
                    return Messages.getString("LosEffects.name_cover_lowleft");
                else
                    return Messages.getString("LosEffects.name_cover_lowright");
            case COVER_LEFT:
                if (switchSides)
                    return Messages.getString("LosEffects.name_cover_right");
                else
                    return Messages.getString("LosEffects.name_cover_left");
            case COVER_RIGHT:
                if (switchSides)
                    return Messages.getString("LosEffects.name_cover_left");
                else
                    return Messages.getString("LosEffects.name_cover_right");
            case COVER_HORIZONTAL:
                return Messages.getString("LosEffects.name_cover_horizontal");
            case COVER_UPPER:
                return Messages.getString("LosEffects.name_cover_upper");                                                  
            case COVER_FULL:
                return Messages.getString("LosEffects.name_cover_full");
            case COVER_75LEFT:
                if (switchSides)
                    return Messages.getString("LosEffects.name_cover_75right");
                else
                    return Messages.getString("LosEffects.name_cover_75left");
            case COVER_75RIGHT:
                if (switchSides)
                    return Messages.getString("LosEffects.name_cover_75left");
                else
                    return Messages.getString("LosEffects.name_cover_75right");
            default:
                return Messages.getString("LosEffects.name_cover_unknown");
        }
    }
    
    public Building getCoverBuildingPrimary() {
        return coverBuildingPrimary;
    }

    public void setCoverBuildingPrimary(Building coverBuilding) {
        this.coverBuildingPrimary = coverBuilding;
    }

    public Entity getCoverDropshipPrimary() {
        return coverDropshipPrimary;
    }

    public void setCoverDropshipPrimary(Entity coverDropship) {
        this.coverDropshipPrimary = coverDropship;
    }

    public int getDamagableCoverTypePrimary() {
        return damagableCoverTypePrimary;
    }

    public void setDamagableCoverTypePrimary(int damagableCover) {
        this.damagableCoverTypePrimary = damagableCover;
    }

    public Coords getCoverLocPrimary() {
        return coverLocPrimary;
    }

    public void setCoverLocPrimary(Coords coverLoc) {
        this.coverLocPrimary = coverLoc;
    }

    public Building getCoverBuildingSecondary() {
        return coverBuildingSecondary;
    }

    public void setCoverBuildingSecondary(Building coverBuildingSecondary) {
        this.coverBuildingSecondary = coverBuildingSecondary;
    }

    public Entity getCoverDropshipSecondary() {
        return coverDropshipSecondary;
    }

    public void setCoverDropshipSecondary(Entity coverDropshipSecondary) {
        this.coverDropshipSecondary = coverDropshipSecondary;
    }

    public int getDamagableCoverTypeSecondary() {
        return damagableCoverTypeSecondary;
    }

    public void setDamagableCoverTypeSecondary(int damagableCoverTypeSecondary) {
        this.damagableCoverTypeSecondary = damagableCoverTypeSecondary;
    }

    public Coords getCoverLocSecondary() {
        return coverLocSecondary;
    }

    public void setCoverLocSecondary(Coords coverLocSecondary) {
        this.coverLocSecondary = coverLocSecondary;
    }    
}

