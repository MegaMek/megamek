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
        public boolean attOffBoard;
        public Coords attackPos;
        public Coords targetPos;
        public int attackAbsHeight;
        public int targetAbsHeight;
        public int attackHeight;
        public int targetHeight;
        int minimumWaterDepth = -1;
    }

    // MAXTECH BMR
    public static final int COVER_NONE = 0; // no cover (none)
    public static final int COVER_LOWLEFT = 0x1; // 25% cover (partial)
    public static final int COVER_LOWRIGHT = 0x2; // 25% cover (partial)
    public static final int COVER_LEFT = 0x4; // vertical cover (blocked)
    public static final int COVER_RIGHT = 0x8; // vertical cover (blocked)
    public static final int COVER_HORIZONTAL = 0x3; // 50% cover (partial)
    public static final int COVER_UPPER = 0xC; // blocked (blocked) - in case
                                                // of future rule where only
                                                // legs are exposed
    public static final int COVER_FULL = 0xF; // blocked (blocked)
    public static final int COVER_75LEFT = 0x7; // 75% cover (blocked)
    public static final int COVER_75RIGHT = 0xB; // 75% cover (blocked)

    boolean blocked = false;
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
    int minimumWaterDepth = -1;
    boolean arcedShot = false;

    /** Creates a new instance of LosEffects */
    public LosEffects() {

    }

    public int getMinimumWaterDepth() {
        return minimumWaterDepth;
    }

    public void setMinimumWaterDepth(int inVal) {
        minimumWaterDepth = inVal;
    }

    public void add(LosEffects other) {
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
        return targetCover >= COVER_HORIZONTAL;
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
        return attackerCover >= COVER_HORIZONTAL;
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
        final Entity ae = game.getEntity(attackerId);

        // LOS fails if one of the entities is not deployed.
        if ((null == ae.getPosition()) || (null == target.getPosition())
                || ae.isOffBoard() || target.isOffBoard()) {
            LosEffects los = new LosEffects();
            los.blocked = true; // TODO: come up with a better "impossible"
            los.hasLoS = false;
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
        if ((target.getTargetType() == Targetable.TYPE_ENTITY)
                || (target.getTargetType() == Targetable.TYPE_FUEL_TANK)
                || (target.getTargetType() == Targetable.TYPE_FUEL_TANK_IGNITE)
                || (target.getTargetType() == Targetable.TYPE_BUILDING)
                || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)) {
            targEl = target.absHeight() + targetHex.getElevation();
        } else {
            targEl = game.getBoard().getHex(target.getPosition()).surface();
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
            attUnderWater = attHex.containsTerrain(Terrains.WATER)
                    && (attHex.depth() > 0) && (attEl < attHex.surface());
            attInWater = attHex.containsTerrain(Terrains.WATER)
                    && (attHex.depth() > 0) && (attEl == attHex.surface());
            attOnLand = !(attUnderWater || attInWater);
        }

        boolean targetOffBoard = !game.getBoard()
                .contains(target.getPosition());
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

        LosEffects finalLoS = calculateLos(game, ai);
        finalLoS.setMinimumWaterDepth(ai.minimumWaterDepth);
        finalLoS.hasLoS = !finalLoS.blocked && (finalLoS.screen < 1) && (finalLoS.plantedFields < 6)
                && (finalLoS.heavyIndustrial < 3)
                && ((finalLoS.lightWoods + finalLoS.lightSmoke)
                        + ((finalLoS.heavyWoods + finalLoS.heavySmoke) * 2)
                        + (finalLoS.ultraWoods * 3) < 3);

        return finalLoS;
    }

    public static LosEffects calculateLos(IGame game, AttackInfo ai) {
        if (ai.attOffBoard) {
            LosEffects los = new LosEffects();
            los.blocked = true;
            los.hasLoS = false;
            return los;
        }
        if ((ai.attOnLand && ai.targetUnderWater) || (ai.attUnderWater
                && ai.targetOnLand)) {
            LosEffects los = new LosEffects();
            los.blocked = true;
            los.hasLoS = false;
            los.blockedByWater = true;
            return los;
        }

        double degree = ai.attackPos.degree(ai.targetPos);
        if (degree % 60 == 30) {
            return LosEffects.losDivided(game, ai);
        }
        return LosEffects.losStraight(game, ai);
    }

    /**
     * Returns ToHitData indicating the modifiers to fire for the specified LOS
     * effects data.
     */
    public ToHitData losModifiers(IGame game) {
        return losModifiers(game, 0);
    }

    public ToHitData losModifiers(IGame game, int eistatus) {
        ToHitData modifiers = new ToHitData();

        if ( arcedShot ) {
            return modifiers;
        }

        if (blocked) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "LOS blocked by terrain.");
        }

        if (infProtected) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Infantry protected by building.");
        }

        if (buildingLevelsOrHexes > 2) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "LOS blocked by buildin hexes or levels.");
        }

        if ((ultraWoods >= 1) || (lightWoods + (heavyWoods * 2) > 2)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "LOS blocked by woods.");
        }

        if (lightSmoke + (heavySmoke * 2) > 2) {
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

        if (lightSmoke + (heavySmoke * 2) + lightWoods + (heavyWoods * 2) > 2) {
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

        if (lightSmoke > 0) {
            modifiers.addModifier(lightSmoke, lightSmoke
                    + " intervening light smoke");
        }

        if (heavySmoke > 0) {
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
            if (game.getOptions().booleanOption("tacops_partial_cover")) {
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
    private static LosEffects losStraight(IGame game, AttackInfo ai) {
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
        }

        for (Coords c : in) {
            los.add(LosEffects.losForCoords(game, ai, c, los.getThruBldg()));
        }

        if ((ai.minimumWaterDepth < 1) && ai.underWaterCombat) {
            los.blocked = true;
        }

        // Infantry inside a building can only be
        // targeted by units in the same building.
        if (ai.targetInfantry && targetInBuilding && (null == los.getThruBldg())) {
            los.infProtected = true;
        }

        // If a target Entity is at a different elevation as its
        // attacker, and if the attack is through a building, the
        // target has cover.
        if ((null != los.getThruBldg())
                && (ai.attackAbsHeight != ai.targetAbsHeight)) {
            los.setTargetCover(COVER_HORIZONTAL);
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
    private static LosEffects losDivided(IGame game, AttackInfo ai) {
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
        }

        // add non-divided line segments
        for (int i = 3; i < in.size() - 2; i += 3) {
            los.add(losForCoords(game, ai, in.get(i), los.getThruBldg()));
        }

        if ((ai.minimumWaterDepth < 1) && ai.underWaterCombat) {
            los.blocked = true;
        }

        // if blocked already, return that
        if (los.losModifiers(game).getValue() == TargetRoll.IMPOSSIBLE) {
            return los;
        }

        // go through divided line segments
        // we do this twice, because we should stick to one of the
        // sides
        // we later use the side that has more hexes
        int leftBetter = dividedLeftBetter(in, game, ai, targetInBuilding, los);
        for (int i = 1; i < in.size() - 2; i += 3) {
            // get effects of the side that is better
            LosEffects toUse;
            boolean usingLeft;
            // we'll use left if both are equal
            if ((leftBetter == 1) || (leftBetter == 2)) {
                toUse = losForCoords(game, ai, in.get(i), los
                        .getThruBldg());
                usingLeft = true;
            } else {
                toUse = losForCoords(game, ai, in.get(i + 1), los
                        .getThruBldg());
                usingLeft = false;
            }

            // If a target Entity is at a different elevation as its
            // attacker, and if the attack is through a building, the
            // target has cover.
            final boolean isElevDiff = ai.attackAbsHeight != ai.targetAbsHeight;

            if ((ai.minimumWaterDepth < 1) && ai.underWaterCombat) {
                los.blocked = true;
            }

            if (targetInBuilding && isElevDiff) {
                if (null != toUse.getThruBldg()) {
                    toUse.setTargetCover(COVER_HORIZONTAL);
                }
            }

            // Include all previous LOS effects.
            toUse.add(los);

            // Infantry inside a building can only be
            // targeted by units in the same building.
            if (ai.targetInfantry && targetInBuilding) {
                if (null == toUse.getThruBldg()) {
                    toUse.infProtected = true;
                }
            }
            los = toUse;

            if (game.getOptions().booleanOption("tacops_partial_cover")) {
                int cover;
                if (usingLeft) {
                    cover = (toUse.targetCover & (COVER_LEFT | COVER_LOWLEFT));
                } else {
                    cover = (toUse.targetCover & (COVER_RIGHT | COVER_LOWRIGHT));
                }
                if ((cover < COVER_FULL) && !(toUse.blocked)) {
                    los.blocked = false;
                    los.targetCover = cover;
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
        if ((bldg != null) && bldg.equals(thruBldg)) {
            los.setThruBldg(thruBldg);
        }

        // ignore hexes the attacker or target are in
        if (coords.equals(ai.attackPos) || coords.equals(ai.targetPos)) {
            if (los.getThruBldg() != null) {
                // attacker and target in building at different height:
                // +1 for each level of difference
                if (ai.attackPos.equals(ai.targetPos) && ai.targetEntity) {
                    los.buildingLevelsOrHexes += (Math.abs(ai.attackHeight - ai.targetHeight));
                }
            }
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

        // TODO: Identify when LOS travels *above* a building's hex.
        // Alternatively, force all building hexes to be same height.

        // check for block by terrain

        // check for LOS according to diagramming rule from MaxTech, page 22
        if (game.getOptions().booleanOption("tacops_LOS1")) {
            if (hexEl + bldgEl > (ai.targetAbsHeight
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
        }

        if (((hexEl + bldgEl > ai.attackAbsHeight) && (hexEl + bldgEl > ai.targetAbsHeight))
                || ((hexEl + bldgEl > ai.attackAbsHeight) && (ai.attackPos
                        .distance(coords) == 1))
                || ((hexEl + bldgEl > ai.targetAbsHeight) && (ai.targetPos
                        .distance(coords) == 1))) {
            los.blocked = true;
            if(hex.terrainLevel(Terrains.BLDG_CF) > 90) {
                los.hardBuildings++;
            } else if(bldgEl > 0) {
                los.softBuildings++;
            } else {
                los.blockedByHill = true;
            }
        }

        // check if there's a clear hex between the targets that's higher than
        // one of them, if we're in underwater combat
        if (ai.underWaterCombat
                && (hex.terrainLevel(Terrains.WATER) == ITerrain.LEVEL_NONE)
                && ((hexEl + bldgEl > ai.attackAbsHeight) || (hexEl + bldgEl > ai.targetAbsHeight))) {
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
            for(int level = 1; level < 11; level++) {
                if(((hexEl + level > ai.attackAbsHeight) && (hexEl + level > ai.targetAbsHeight))
                        || ((hexEl + level > ai.attackAbsHeight) && (ai.attackPos
                                .distance(coords) == 1))
                        || ((hexEl + level > ai.targetAbsHeight) && (ai.targetPos
                                .distance(coords) == 1))) {
                    //check industrial zone
                    if(hex.terrainLevel(Terrains.INDUSTRIAL) == level) {
                        los.heavyIndustrial++;
                    }
                    //TODO: might as well put everything in here to save some time
                }
            }
            //planted fields only rise one level above the terrain
            if(((hexEl + 1 > ai.attackAbsHeight) && (hexEl + 2 > ai.targetAbsHeight))
                    || ((hexEl + 1 > ai.attackAbsHeight) && (ai.attackPos
                            .distance(coords) == 1))
                    || ((hexEl + 1 > ai.targetAbsHeight) && (ai.targetPos
                            .distance(coords) == 1))) {
                if (hex.containsTerrain(Terrains.FIELDS)) {
                    los.plantedFields++;
                }
            }
            if (((hexEl + 2 > ai.attackAbsHeight) && (hexEl + 2 > ai.targetAbsHeight))
                    || ((hexEl + 2 > ai.attackAbsHeight) && (ai.attackPos
                            .distance(coords) == 1))
                    || ((hexEl + 2 > ai.targetAbsHeight) && (ai.targetPos
                            .distance(coords) == 1))) {
                // smoke and woods stack for
                // LOS
                // so check them both
                if (hex.containsTerrain(Terrains.SMOKE)) {
                    if (hex.terrainLevel(Terrains.SMOKE) == 1) {
                        los.lightSmoke++;
                    } else if (hex.terrainLevel(Terrains.SMOKE) > 1) {
                        los.heavySmoke++;
                    }
                }

                if ((hex.terrainLevel(Terrains.WOODS) == 1)
                        || (hex.terrainLevel(Terrains.JUNGLE) == 1)) {
                    los.lightWoods++;
                } else if ((hex.terrainLevel(Terrains.WOODS) == 2)
                        || (hex.terrainLevel(Terrains.JUNGLE) == 2)) {
                    los.heavyWoods++;
                }
            }
            //ultra woods/jungle rise three levels above the terrain
            if (((hexEl + 3 > ai.attackAbsHeight) && (hexEl + 3 > ai.targetAbsHeight))
                    || ((hexEl + 3 > ai.attackAbsHeight) && (ai.attackPos
                            .distance(coords) == 1))
                    || ((hexEl + 3 > ai.targetAbsHeight) && (ai.targetPos
                            .distance(coords) == 1))) {

                if ((hex.terrainLevel(Terrains.WOODS) == 3)
                        || (hex.terrainLevel(Terrains.JUNGLE) == 3)) {
                    los.ultraWoods++;
                }
            }
        }


        // check for target partial cover
        if (ai.targetPos.distance(coords) == 1) {
            if (los.blocked
                    && game.getOptions().booleanOption("tacops_partial_cover")) {
                los.targetCover = COVER_FULL;
            } else if ((hexEl + bldgEl == ai.targetAbsHeight)
                    && (ai.attackAbsHeight <= ai.targetAbsHeight)
                    && (ai.targetHeight > 0)) {
                los.targetCover |= COVER_HORIZONTAL;
            }
        }

        // check for attacker partial cover
        if (ai.attackPos.distance(coords) == 1) {
            if (los.blocked
                    && game.getOptions().booleanOption("tacops_partial_cover")) {
                los.attackerCover = COVER_FULL;
            } else if ((hexEl + bldgEl == ai.attackAbsHeight)
                    && (ai.attackAbsHeight >= ai.targetAbsHeight)
                    && (ai.attackHeight > 0)) {
                los.attackerCover |= COVER_HORIZONTAL;
            }
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
    public static int dividedLeftBetter(ArrayList<Coords> in, IGame game, AttackInfo ai, boolean targetInBuilding, LosEffects los) {
        LosEffects leftTotal = new LosEffects();
        LosEffects rightTotal = new LosEffects();
        for (int i = 1; i < in.size() - 2; i += 3) {
            // get effects of each side
            LosEffects left = losForCoords(game, ai, in.get(i), los
                    .getThruBldg());
            LosEffects right = losForCoords(game, ai, in.get(i + 1), los
                    .getThruBldg());

            // If a target Entity is at a different elevation as its
            // attacker, and if the attack is through a building, the
            // target has cover.
            final boolean isElevDiff = ai.attackAbsHeight != ai.targetAbsHeight;

            if (targetInBuilding && isElevDiff) {
                if (null != left.getThruBldg()) {
                    left.setTargetCover(COVER_HORIZONTAL);
                }
                if (null != right.getThruBldg()) {
                    right.setTargetCover(COVER_HORIZONTAL);
                }
            }

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
}
