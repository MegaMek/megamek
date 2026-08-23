/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.board;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Hex;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;

/**
 * Says whether a hex a gamemaster is building would be a legal hex, and why not when it would not.
 *
 * <p>The rules live here rather than on either side of the connection because both sides need them and they must
 * agree. The dialog asks as the gamemaster builds an edit, so an illegal hex can be refused before it is sent and the
 * reason shown while there is still something to change; the server asks again when the edit arrives, because it is
 * the server that decides what the board is and the hex may have changed in between.</p>
 *
 * <h2>Status and known limits</h2>
 *
 * <p>Note for anyone picking this up: the gamemaster terrain tools are a first, deliberately simple pass. They change
 * terrain and they check the obvious things, but they have not had much testing yet and the validation is thinner
 * than it looks.</p>
 *
 * <p>Most of what is checked comes from {@link megamek.common.Hex#isValid}, which asks whether a hex is well formed -
 * a building that has its construction factor, a woods level that exists, rapids that sit in water - and almost
 * nothing about whether the hex makes sense on a battlefield. Only two rules beyond that are enforced here: a
 * structure may not stand in water, and the water under a unit may not be moved. Both were added because a
 * gamemaster hit them, not because the list was worked through.</p>
 *
 * <p>So a gamemaster can still build combinations nobody has thought hard about: terrain that parses but reads as
 * nonsense on the ground, changes that leave a unit at an elevation the rules have no answer for, and edits that cut
 * across fire, smoke and the other terrain the engine maintains for itself.</p>
 *
 * <p>TODO: widen the validation beyond the two rules here, and get both the terrain modification and the terrain
 * change paths properly playtested. Neither has had more than a first look.</p>
 */
public final class HexEditValidator {

    /**
     * The terrains that pick the special image a hex is drawn with. These say nothing about what the hex is made of
     * and everything about how it looks, and boards use them heavily - a warehouse district is ordinary medium
     * buildings with {@code bldg_fluff} picking the artwork. A terrain edit must carry them through, or repainting
     * the ground under a building turns a drawn building into a generic box.
     */
    private static final List<Integer> FLUFF_TERRAINS = List.of(
          Terrains.FLUFF, Terrains.BLDG_FLUFF, Terrains.ROAD_FLUFF, Terrains.GROUND_FLUFF, Terrains.WATER_FLUFF);

    /**
     * The terrains that make up a structure. A structure standing in a hex constrains what the ground under it may
     * become, and is not itself changed by a terrain edit.
     */
    private static final List<Integer> STRUCTURE_TERRAINS = List.of(
          Terrains.BUILDING, Terrains.BLDG_CF, Terrains.BLDG_ELEV, Terrains.BLDG_ARMOR, Terrains.BLDG_CLASS,
          Terrains.BLDG_BASEMENT_TYPE, Terrains.BLDG_BASE_COLLAPSED,
          Terrains.BRIDGE, Terrains.BRIDGE_CF, Terrains.BRIDGE_ELEV,
          Terrains.FUEL_TANK, Terrains.FUEL_TANK_CF, Terrains.FUEL_TANK_ELEV, Terrains.FUEL_TANK_MAGN);

    private HexEditValidator() {
    }

    /** @return the terrains that make up a structure, which a terrain edit carries through unchanged */
    public static List<Integer> structureTerrains() {
        return STRUCTURE_TERRAINS;
    }

    /**
     * @return every terrain a terrain edit carries through rather than replacing: the structures standing in the hex,
     *       and the fluff terrains that choose how it is drawn
     */
    public static List<Integer> carriedThroughTerrains() {
        List<Integer> carried = new ArrayList<>(STRUCTURE_TERRAINS);
        carried.addAll(FLUFF_TERRAINS);
        return carried;
    }

    /**
     * Lists everything wrong with a hex a gamemaster has built.
     *
     * @param edited The hex as it would be after the edit
     *
     * @return the problems with it, in words a gamemaster can act on; empty when the hex is legal
     */
    public static List<String> problemsWith(Hex edited) {
        List<String> problems = new ArrayList<>();
        edited.isValid(problems);
        if (standsAStructureInWater(edited)) {
            problems.add("A building or fuel tank cannot stand in water.");
        }
        return problems;
    }

    /**
     * Lists everything wrong with a change to a hex, including the things that depend on what the hex was before and
     * on what is standing in it.
     *
     * <p>Every rule a gamemaster can break belongs here rather than on one side of the connection, so that the dialog
     * can report a refusal while there is still something to change instead of the server refusing an edit the dialog
     * has already called legal.</p>
     *
     * @param original   The hex as it is now
     * @param edited     The hex as it would be after the edit
     * @param isOccupied Whether any unit is standing in the hex
     *
     * @return the problems with the change, in words a gamemaster can act on; empty when the change is legal
     */
    public static List<String> problemsWithChange(Hex original, Hex edited, boolean isOccupied) {
        List<String> problems = problemsWith(edited);
        if (wouldMoveTheWaterUnderUnits(original, edited, isOccupied)) {
            problems.add("The water depth cannot be changed while units are standing in the hex; move them first.");
        }
        problems.addAll(terrainsStrongerThanTheRulesAllow(original, edited));
        return problems;
    }

    /**
     * The terrains the change would leave stronger than the rules allow them to be.
     *
     * <p>A terrain factor is how much punishment a terrain can take before it is destroyed, and the rules give each
     * terrain one fixed value: light woods 50, heavy woods 90, paved 200 (Tactical Operations: Advanced Rules,
     * p. 63, Terrain Factor and Conversion Table). Damage takes a terrain below that; nothing puts it above. Woods
     * at 300 would simply never burn down.</p>
     *
     * <p>A hex that already stands above the table is left alone rather than made unfixable: the change is only
     * refused if it would raise the terrain further. A board may say what it likes, and a gamemaster should still be
     * able to edit a hex on it.</p>
     *
     * @param original The hex as it is now
     * @param edited   The hex as it would be after the change
     *
     * @return the problems, in words a gamemaster can act on; empty when nothing would be over-strengthened
     */
    private static List<String> terrainsStrongerThanTheRulesAllow(Hex original, Hex edited) {
        List<String> problems = new ArrayList<>();
        for (int terrainType : edited.getTerrainTypes()) {
            Terrain terrain = edited.getTerrain(terrainType);
            int highestAllowed = highestTerrainFactorFor(original, terrainType, terrain.getLevel());
            if (terrain.getTerrainFactor() > highestAllowed) {
                problems.add(Terrains.getDisplayName(terrainType, terrain.getLevel())
                      + " cannot have a terrain factor above " + highestAllowed + ".");
            }
        }
        return problems;
    }

    /**
     * The highest terrain factor one terrain in a hex may be left at.
     *
     * <p>The rules give each terrain one value and damage only takes it down (Tactical Operations: Advanced Rules,
     * p. 63). A hex already standing above that keeps what it has, so an over-strong hex can be brought down rather
     * than becoming uneditable.</p>
     *
     * <p>Every path that sets a terrain factor asks this, so that the brush and the {@code /modifyterrain} command
     * cannot disagree about what is allowed.</p>
     *
     * @param hex          The hex as it is now, which is what the allowance for an already-strong hex reads
     * @param terrainType  The terrain, from {@link Terrains}
     * @param terrainLevel The level the terrain will be at, which is what the rules value is read from - light woods
     *                     and heavy woods are the same terrain at different levels and have different factors
     *
     * @return the highest factor that terrain may be set to in this hex
     */
    public static int highestTerrainFactorFor(Hex hex, int terrainType, int terrainLevel) {
        int fromTheRules = Terrains.getTerrainFactor(terrainType, terrainLevel);
        Terrain existing = hex.getTerrain(terrainType);
        return (existing == null) ? fromTheRules : Math.max(fromTheRules, existing.getTerrainFactor());
    }

    /**
     * Whether the change would flood or drain the ground under a unit. There is no rule for what happens to a unit
     * when the water beneath it rises or falls, so the change is refused rather than guessed at.
     *
     * @param original   The hex as it is now
     * @param edited     The hex as it would be after the edit
     * @param isOccupied Whether any unit is standing in the hex
     *
     * @return {@code true} when the water depth would change under a unit
     */
    public static boolean wouldMoveTheWaterUnderUnits(Hex original, Hex edited, boolean isOccupied) {
        return isOccupied && (original.depth() != edited.depth());
    }

    /**
     * Whether the hex would leave a structure standing in water. The hex's own validation does not catch this: it
     * checks that a structure in a hex is complete, not that it is somewhere it could have been built.
     *
     * @param edited The hex as it would be after the edit
     *
     * @return {@code true} if the hex would hold both a structure and water for it to stand in
     */
    public static boolean standsAStructureInWater(Hex edited) {
        boolean hasStructure = edited.containsTerrain(Terrains.BUILDING) || edited.containsTerrain(Terrains.FUEL_TANK);
        return hasStructure && (edited.depth() > 0);
    }

    /**
     * Whether a terrain can be set to a level at all, according to the terrain's own rules about which levels it has.
     * This is what makes the difference between offering a gamemaster Light, Heavy and Ultra Woods and offering them a
     * spinner that runs to six.
     *
     * @param terrainType  The terrain, from {@link Terrains}
     * @param terrainLevel The level to test
     *
     * @return {@code true} when that terrain has that level
     */
    public static boolean isLegalLevel(int terrainType, int terrainLevel) {
        return new Terrain(terrainType, terrainLevel).isValid(null);
    }

    /**
     * The levels a terrain can be set to, in order.
     *
     * @param terrainType   The terrain, from {@link Terrains}
     * @param highestToTest The highest level worth offering; terrains with open-ended levels, such as water depth, are
     *                      cut off here rather than running forever
     *
     * @return the legal levels for that terrain
     */
    public static List<Integer> legalLevelsFor(int terrainType, int highestToTest) {
        List<Integer> levels = new ArrayList<>();
        for (int level = 1; level <= highestToTest; level++) {
            if (isLegalLevel(terrainType, level)) {
                levels.add(level);
            }
        }
        return levels;
    }
}
