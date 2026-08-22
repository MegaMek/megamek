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

package megamek.server.totalWarfare;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.board.HexEditSpec;
import megamek.common.board.HexEditValidator;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;

/**
 * Applies a gamemaster's edits to a single hex: adding a terrain, changing its level, or clearing the hex.
 *
 * <p>The change is tried on a copy of the hex first and only kept if the result is a hex the game considers valid, so
 * an edit can never leave the board in a state the rest of the engine does not expect - rapids outside water, for
 * instance, or a woods level that does not exist.</p>
 *
 * <p>Changing the depth of water in a hex that units are standing in is refused. There is no rule for what happens to
 * a unit when the ground beneath it floods, and inventing one here would be a house rule; that is being decided
 * separately.</p>
 */
public class HexEditHandler extends AbstractTWRuleHandler {
    private static final MMLogger LOGGER = MMLogger.create(HexEditHandler.class);

    /** Passed as the terrain level to remove a terrain rather than add it. */
    public static final int REMOVE_TERRAIN_LEVEL = 0;

    /** The woods and jungle level that stands taller than the rest, and the foliage elevations the two cases take. */
    private static final int ULTRA_FOLIAGE_LEVEL = 3;
    private static final int ULTRA_FOLIAGE_ELEVATION = 3;
    private static final int STANDARD_FOLIAGE_ELEVATION = 2;

    /** Report ids from {@code report-messages.properties} for the three edits this handler makes. */
    private static final int REPORT_TERRAIN_SET = 1250;
    private static final int REPORT_TERRAIN_REMOVED = 1251;
    private static final int REPORT_HEX_CLEARED = 1252;
    private static final int REPORT_TERRAIN_FACTOR_SET = 1254;
    private static final int REPORT_HEXES_EDITED = 1255;
    private static final int REPORT_HEX_EDIT_UNDONE = 1256;

    /**
     * The hexes as they stood before the most recent edit, so it can be taken back. Only the last edit is kept: undo
     * is for a change the gamemaster has just looked at and thought better of.
     */
    private final Map<Coords, Hex> hexesBeforeLastEdit = new LinkedHashMap<>();

    /** The board the remembered hexes are on. */
    private int boardOfLastEdit;

    HexEditHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Sets one terrain in one hex to the given level, removing that terrain when the level is
     * {@link #REMOVE_TERRAIN_LEVEL}.
     *
     * @param coords        The hex to change
     * @param boardId       The board the hex is on
     * @param terrainType   The terrain to set, from {@link Terrains}
     * @param terrainLevel  The level to set it to, or {@link #REMOVE_TERRAIN_LEVEL} to remove it
     * @param gamemasterName The name of the gamemaster making the change, for the report
     *
     * @return A description of why the edit was refused, or {@code null} when it was applied
     */
    public String setTerrain(Coords coords, int boardId, int terrainType, int terrainLevel, String gamemasterName) {
        Hex hex = getGame().getHex(coords, boardId);
        if (hex == null) {
            LOGGER.debug("[GMTerrain] {}: refused - hex {} does not exist on board {}",
                  gamemasterName, coords.getBoardNum(), boardId);
            return "that hex is not on the board";
        }

        Hex edited = hex.duplicate();
        applyTerrain(edited, terrainType, terrainLevel);

        String refusal = refusalFor(hex, edited, coords, boardId, gamemasterName);
        if (refusal != null) {
            return refusal;
        }

        applyTerrain(hex, terrainType, terrainLevel);
        gameManager.sendChangedHex(coords, boardId);
        reportChange(coords, terrainType, terrainLevel, gamemasterName);
        LOGGER.info("[GMTerrain] {} set {} to level {} in hex {}",
              gamemasterName, Terrains.getName(terrainType), terrainLevel, coords.getBoardNum());
        return null;
    }

    /**
     * Applies a gamemaster's edit to every hex it names, or to none of them.
     *
     * <p>Each hex is checked first and the whole edit is refused if any one of them would end up invalid. A
     * gamemaster flooding a valley wants the valley flooded or a reason why not; half a valley flooded, with the
     * hexes that failed left dry among the ones that worked, is the worst of both.</p>
     *
     * @param spec           The hexes to change and the terrain they should end up holding
     * @param gamemasterName The name of the gamemaster making the change, for the report
     *
     * @return A description of why the edit was refused, or {@code null} when it was applied
     */
    public String applyHexEdit(HexEditSpec spec, String gamemasterName) {
        if (spec.isUndoingLastEdit()) {
            return undoLastEdit(gamemasterName);
        }
        if (spec.getCoords().isEmpty()) {
            return "no hexes were chosen";
        }
        String refusal = firstRefusalIn(spec, gamemasterName);
        if (refusal != null) {
            return refusal;
        }

        rememberHexesBeforeEdit(spec);
        for (Coords hexCoords : spec.getCoords()) {
            Hex hex = getGame().getHex(hexCoords, spec.getBoardId());
            writeTerrain(hex, spec);
            gameManager.sendChangedHex(hexCoords, spec.getBoardId());
        }
        reportHexEdit(spec, gamemasterName);
        LOGGER.info("[GMTerrain] {} changed {} hex(es) to hold {}",
              gamemasterName, spec.getCoords().size(), describeTerrain(spec));
        return null;
    }

    /**
     * Keeps a copy of each hex as it was, so the edit can be taken back. Only the most recent edit is remembered: an
     * undo is for looking at a change and deciding against it, not for walking back through an evening's work.
     */
    private void rememberHexesBeforeEdit(HexEditSpec spec) {
        hexesBeforeLastEdit.clear();
        boardOfLastEdit = spec.getBoardId();
        for (Coords hexCoords : spec.getCoords()) {
            Hex hex = getGame().getHex(hexCoords, spec.getBoardId());
            if (hex != null) {
                hexesBeforeLastEdit.put(hexCoords, hex.duplicate());
            }
        }
    }

    /**
     * Puts the hexes back the way they were before the last edit.
     *
     * <p>What is restored is the hex as it stood at that moment, so anything that has happened to those hexes since -
     * a fire spreading into one, a building coming down in another - is undone along with the edit. An undo is meant
     * to be used straight away, on a change the gamemaster has just looked at and thought better of.</p>
     *
     * @param gamemasterName The name of the gamemaster taking the edit back, for the report
     *
     * @return A description of why the undo was refused, or {@code null} when it was applied
     */
    private String undoLastEdit(String gamemasterName) {
        if (hexesBeforeLastEdit.isEmpty()) {
            LOGGER.debug("[GMTerrain] {}: nothing to undo", gamemasterName);
            return "there is no terrain change to undo";
        }

        List<String> hexNumbers = new ArrayList<>();
        for (Map.Entry<Coords, Hex> before : hexesBeforeLastEdit.entrySet()) {
            getGame().getBoard(boardOfLastEdit).setHex(before.getKey(), before.getValue());
            gameManager.sendChangedHex(before.getKey(), boardOfLastEdit);
            hexNumbers.add(String.valueOf(before.getKey().getBoardNum()));
        }

        Report report = new Report(REPORT_HEX_EDIT_UNDONE, Report.PUBLIC);
        report.add(gamemasterName);
        report.add(String.join(", ", hexNumbers));
        addReport(report);

        LOGGER.info("[GMTerrain] {} took back the last terrain change, putting {} hex(es) back",
              gamemasterName, hexesBeforeLastEdit.size());
        hexesBeforeLastEdit.clear();
        return null;
    }

    /**
     * Checks every hex the edit names, so that nothing is changed unless all of it can be.
     *
     * @return the reason the first hex that cannot take the edit cannot take it, or {@code null} when all of them can
     */
    private String firstRefusalIn(HexEditSpec spec, String gamemasterName) {
        for (Coords hexCoords : spec.getCoords()) {
            Hex hex = getGame().getHex(hexCoords, spec.getBoardId());
            if (hex == null) {
                return "hex " + hexCoords.getBoardNum() + " is not on the board";
            }
            Hex edited = hex.duplicate();
            writeTerrain(edited, spec);
            String refusal = refusalFor(hex, edited, hexCoords, spec.getBoardId(), gamemasterName);
            if (refusal != null) {
                return "hex " + hexCoords.getBoardNum() + ": " + refusal;
            }
        }
        return null;
    }

    /**
     * Leaves the hex holding exactly the terrain the edit names, plus any structure that was already standing there.
     * The edit describes the ground, not what has been built on it.
     */
    private static void writeTerrain(Hex hex, HexEditSpec spec) {
        List<Terrain> structures = new ArrayList<>();
        for (int structureTerrain : HexEditValidator.structureTerrains()) {
            Terrain existing = hex.getTerrain(structureTerrain);
            if (existing != null) {
                structures.add(existing);
            }
        }
        hex.removeAllTerrains();
        for (Map.Entry<Integer, Integer> terrain : spec.getTerrainLevels().entrySet()) {
            hex.addTerrain(new Terrain(terrain.getKey(), terrain.getValue()));
        }
        for (Terrain structure : structures) {
            hex.addTerrain(structure);
        }
    }

    /** @return the terrain the edit leaves behind, in words, for the report and the log */
    private static String describeTerrain(HexEditSpec spec) {
        if (spec.isClearingHexes()) {
            return "bare ground";
        }
        List<String> described = new ArrayList<>();
        for (Map.Entry<Integer, Integer> terrain : spec.getTerrainLevels().entrySet()) {
            described.add(Terrains.getDisplayName(terrain.getKey(), terrain.getValue()));
        }
        return String.join(", ", described);
    }

    /** Tells every player what changed and where, so a hex changing under them is never unexplained. */
    private void reportHexEdit(HexEditSpec spec, String gamemasterName) {
        List<String> hexNumbers = new ArrayList<>();
        for (Coords hexCoords : spec.getCoords()) {
            hexNumbers.add(String.valueOf(hexCoords.getBoardNum()));
        }
        Report report = new Report(REPORT_HEXES_EDITED, Report.PUBLIC);
        report.add(gamemasterName);
        report.add(describeTerrain(spec));
        report.add(String.join(", ", hexNumbers));
        addReport(report);
    }

    /**
     * Sets how much punishment a terrain in one hex can still take, leaving what the terrain is alone. Woods that have
     * been shelled but not yet flattened are the usual case: the hex is still light woods, but there is less of it
     * left to burn or blast away.
     *
     * @param coords         The hex holding the terrain
     * @param boardId        The board the hex is on
     * @param terrainType    The terrain to weaken or restore, from {@link Terrains}
     * @param terrainFactor  The terrain factor to leave it at
     * @param gamemasterName The name of the gamemaster making the change, for the report
     *
     * @return A description of why the edit was refused, or {@code null} when it was applied
     */
    public String setTerrainFactor(Coords coords, int boardId, int terrainType, int terrainFactor,
          String gamemasterName) {
        Hex hex = getGame().getHex(coords, boardId);
        if (hex == null) {
            LOGGER.debug("[GMTerrain] {}: refused - hex {} does not exist on board {}",
                  gamemasterName, coords.getBoardNum(), boardId);
            return "that hex is not on the board";
        }
        Terrain terrain = hex.getTerrain(terrainType);
        if (terrain == null) {
            LOGGER.debug("[GMTerrain] {}: refused - hex {} holds no {}",
                  gamemasterName, coords.getBoardNum(), Terrains.getName(terrainType));
            return "there is no " + Terrains.getName(terrainType) + " in that hex to modify";
        }

        int previousFactor = terrain.getTerrainFactor();
        terrain.setTerrainFactor(terrainFactor);
        gameManager.sendChangedHex(coords, boardId);

        Report report = new Report(REPORT_TERRAIN_FACTOR_SET, Report.PUBLIC);
        report.add(gamemasterName);
        report.add(Terrains.getDisplayName(terrainType, terrain.getLevel()));
        report.add(coords.getBoardNum());
        report.add(terrainFactor);
        addReport(report);

        LOGGER.info("[GMTerrain] {} set the terrain factor of {} in hex {} from {} to {}",
              gamemasterName, Terrains.getName(terrainType), coords.getBoardNum(), previousFactor, terrainFactor);
        return null;
    }

    /**
     * Clears every terrain from one hex, leaving bare ground at the hex's own level.
     *
     * @param coords         The hex to clear
     * @param boardId        The board the hex is on
     * @param gamemasterName The name of the gamemaster making the change, for the report
     *
     * @return A description of why the edit was refused, or {@code null} when it was applied
     */
    public String clearHex(Coords coords, int boardId, String gamemasterName) {
        Hex hex = getGame().getHex(coords, boardId);
        if (hex == null) {
            LOGGER.debug("[GMTerrain] {}: refused - hex {} does not exist on board {}",
                  gamemasterName, coords.getBoardNum(), boardId);
            return "that hex is not on the board";
        }
        boolean clearingWaterUnderUnits = HexEditValidator.wouldMoveTheWaterUnderUnits(hex, new Hex(),
              !getGame().getEntitiesVector(coords, boardId).isEmpty());
        if (clearingWaterUnderUnits) {
            LOGGER.debug("[GMTerrain] {}: refused clearing hex {} - it holds water and units are in it",
                  gamemasterName, coords.getBoardNum());
            return "that hex holds water with units in it; move them first";
        }

        hex.removeAllTerrains();
        gameManager.sendChangedHex(coords, boardId);
        Report report = new Report(REPORT_HEX_CLEARED, Report.PUBLIC);
        report.add(gamemasterName);
        report.add(coords.getBoardNum());
        addReport(report);
        LOGGER.info("[GMTerrain] {} cleared hex {}", gamemasterName, coords.getBoardNum());
        return null;
    }

    /** Adds the terrain at the given level, or removes it when the level says to. */
    private static void applyTerrain(Hex hex, int terrainType, int terrainLevel) {
        hex.removeTerrain(terrainType);
        if (terrainLevel != REMOVE_TERRAIN_LEVEL) {
            hex.addTerrain(new Terrain(terrainType, terrainLevel));
        }
        keepFoliageElevationInStep(hex, terrainType, terrainLevel);
    }

    /**
     * Keeps a hex's foliage elevation matched to its woods or jungle. The two are a pair: a hex is invalid if it has a
     * foliage elevation with nothing growing in it, or a foliage elevation that the growth is too tall or too short
     * for. Editing one without the other would refuse edits that ought to work - taking woods out of a hex, most
     * obviously, which would otherwise leave the elevation behind and make the hex invalid.
     *
     * @param hex          The hex being edited
     * @param terrainType  The terrain that was just set or removed
     * @param terrainLevel The level it was set to, or {@link #REMOVE_TERRAIN_LEVEL} if it was removed
     */
    private static void keepFoliageElevationInStep(Hex hex, int terrainType, int terrainLevel) {
        if ((terrainType != Terrains.WOODS) && (terrainType != Terrains.JUNGLE)) {
            return;
        }
        boolean nothingGrowsHereNow = !hex.containsTerrain(Terrains.WOODS) && !hex.containsTerrain(Terrains.JUNGLE);
        if (nothingGrowsHereNow || (terrainLevel == REMOVE_TERRAIN_LEVEL)) {
            hex.removeTerrain(Terrains.FOLIAGE_ELEV);
            return;
        }
        hex.removeTerrain(Terrains.FOLIAGE_ELEV);
        hex.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, foliageElevationFor(terrainLevel)));
    }

    /**
     * @param terrainLevel The woods or jungle level growing in the hex
     *
     * @return The foliage elevation that growth stands at: ultra woods and jungle reach a level higher than light and
     *       heavy do
     */
    private static int foliageElevationFor(int terrainLevel) {
        return (terrainLevel == ULTRA_FOLIAGE_LEVEL) ? ULTRA_FOLIAGE_ELEVATION : STANDARD_FOLIAGE_ELEVATION;
    }

    /**
     * Checks the edited hex against everything that would make the change unsafe to keep.
     *
     * @return The reason to refuse the edit, or {@code null} when there is none
     */
    private String refusalFor(Hex original, Hex edited, Coords coords, int boardId, String gamemasterName) {
        boolean isOccupied = !getGame().getEntitiesVector(coords, boardId).isEmpty();
        List<String> hexProblems = HexEditValidator.problemsWithChange(original, edited, isOccupied);
        if (!hexProblems.isEmpty()) {
            LOGGER.debug("[GMTerrain] {}: refused editing hex {} - {}",
                  gamemasterName, coords.getBoardNum(), hexProblems);
            return String.join(" ", hexProblems);
        }
        return null;
    }

    /** Reports the change to every player, so a hex changing under them is never unexplained. */
    private void reportChange(Coords coords, int terrainType, int terrainLevel, String gamemasterName) {
        boolean isRemoval = (terrainLevel == REMOVE_TERRAIN_LEVEL);
        Report report = new Report(isRemoval ? REPORT_TERRAIN_REMOVED : REPORT_TERRAIN_SET, Report.PUBLIC);
        report.add(gamemasterName);
        report.add(isRemoval
              ? Terrains.getName(terrainType)
              : Terrains.getDisplayName(terrainType, terrainLevel));
        report.add(coords.getBoardNum());
        addReport(report);
    }
}
