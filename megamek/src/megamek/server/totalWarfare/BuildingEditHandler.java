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

import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.annotations.Nullable;
import megamek.common.enums.BasementType;
import megamek.common.board.Board;
import megamek.common.board.BuildingEditSpec;
import megamek.common.board.Coords;
import megamek.common.units.BuildingTerrain;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;

/**
 * Applies a gamemaster's edits to the building standing in one hex: setting how much of it is left standing, or
 * bringing it down.
 *
 * <p>A building's construction factor is held per hex, so a large building can be weakened or knocked through in one
 * hex while the rest of it stands. Setting the factor to zero brings that hex down, because that is what happens to a
 * building whose construction factor reaches zero, and it is resolved through the same collapse the rules use rather
 * than by editing the board - so anything standing on or inside it is dealt with properly.</p>
 */
public class BuildingEditHandler extends AbstractTWRuleHandler {
    private static final MMLogger LOGGER = MMLogger.create(BuildingEditHandler.class);

    /** The construction factor at which a building stops standing up. */
    public static final int COLLAPSING_CONSTRUCTION_FACTOR = 0;

    /** Report ids from {@code report-messages.properties}. */
    private static final int REPORT_BUILDING_CF_SET = 1253;
    private static final int REPORT_BUILDING_RAISED = 1257;
    private static final int REPORT_BUILDING_REMOVED = 1258;

    /**
     * The collapse rules this handler brings a building down through. It holds no state of its own beyond the game
     * manager, so it is built here rather than reaching for the one the game manager keeps, which would mean widening
     * that class to expose it.
     */
    private final BuildingCollapseHandler collapseHandler;

    BuildingEditHandler(TWGameManager gameManager) {
        super(gameManager);
        this.collapseHandler = new BuildingCollapseHandler(gameManager);
    }

    /**
     * The values of a building a gamemaster may change, with {@code null} meaning "leave this one alone". These are
     * the values the map editor sets on a building that can also be changed while a game is running; what kind of
     * building it is and what class it belongs to are settled when the board is built and are not among them.
     *
     * @param constructionFactor How much of the building is left standing in the hex, or {@code null} to leave it
     * @param armor              The building's armor in the hex, or {@code null} to leave it
     * @param height             How many levels the building stands in the hex, or {@code null} to leave it
     * @param basement           The kind of basement under the hex, or {@code null} to leave it
     */
    public record BuildingEdit(@Nullable Integer constructionFactor, @Nullable Integer armor,
                               @Nullable Integer height, @Nullable BasementType basement) {
    }


    /**
     * Puts the building the gamemaster describes into a hex, whatever is there now.
     *
     * <p>One entry point covers all of it, because the gamemaster is saying what should be standing in the hex rather
     * than which operation to perform. An empty hex gets a building put up; a hex whose building already matches the
     * type and class asked for has its condition changed; a hex whose building is of another type is rebuilt, since
     * what a building is made of is fixed when the board makes it and cannot be set afterwards.</p>
     *
     * @param spec           What should be standing in the hex when the edit is done
     * @param gamemasterName The name of the gamemaster making the change, for the report
     *
     * @return A description of why the edit was refused, or {@code null} when it was applied
     */
    public String applyBuildingSpec(BuildingEditSpec spec, String gamemasterName) {
        Board board = getGame().getBoard(spec.getBoardId());
        if (board == null) {
            return "that board is not in play";
        }
        Hex hex = board.getHex(spec.getCoords());
        if (hex == null) {
            return "that hex is not on the board";
        }
        IBuilding existing = board.getBuildingAt(spec.getCoords());

        if (spec.isRemovingBuilding()) {
            return removeBuilding(board, existing, spec, gamemasterName);
        }
        if (hex.depth() > 0) {
            LOGGER.debug("[GMBuilding] {}: refused - hex {} holds water", gamemasterName,
                  spec.getCoords().getBoardNum());
            return "a building cannot stand in water";
        }
        if (existing == null) {
            return raiseBuilding(board, hex, spec, gamemasterName);
        }
        if (needsRebuilding(existing, spec)) {
            LOGGER.info("[GMBuilding] {} rebuilds the building in hex {} as a {}",
                  gamemasterName, spec.getCoords().getBoardNum(), spec.getBuildingType());
            board.removeBuilding(existing);
            gameManager.sendRemovedBuildings(oneBuilding(existing));
            return raiseBuilding(board, hex, spec, gamemasterName);
        }
        return applyEdit(spec.getCoords(),
              new BuildingEdit(spec.getConstructionFactor(), spec.getArmor(), spec.getHeight(), spec.getBasement()),
              gamemasterName);
    }

    /**
     * @return {@code true} when the building standing there is not the kind the gamemaster asked for, so it has to be
     *       taken down and put up again rather than adjusted
     */
    private static boolean needsRebuilding(IBuilding existing, BuildingEditSpec spec) {
        return (existing.getBuildingType() != spec.getBuildingType())
              || (existing.getBldgClass() != spec.getBuildingClass());
    }

    /** Puts a building up in a hex that has none, by writing what it is into the hex and letting the board build it. */
    private String raiseBuilding(Board board, Hex hex, BuildingEditSpec spec, String gamemasterName) {
        writeBuildingTerrain(hex, spec);
        // put the hex back through the board rather than leaving it changed where it lies, so the board recomputes
        // what it works out for itself - exits, inclines, the map's high and low points - before the building is made
        // from it. Clients do that work when the hex reaches them, so skipping it here leaves the two sides holding
        // different versions of the same hex.
        board.setHex(spec.getCoords(), hex);
        IBuilding raised;
        try {
            raised = new BuildingTerrain(spec.getCoords(), board, Terrains.BUILDING, spec.getBasement());
        } catch (RuntimeException buildFailure) {
            // the hex is put back rather than left holding half a building that the board never made into one
            clearBuildingTerrain(hex);
            board.setHex(spec.getCoords(), hex);
            LOGGER.error(buildFailure, "[GMBuilding] {}: could not raise a building in hex {}",
                  gamemasterName, spec.getCoords().getBoardNum());
            return "that building could not be built there";
        }
        board.addBuildingToBoard(raised);
        gameManager.sendChangedHex(spec.getCoords(), spec.getBoardId());
        gameManager.sendNewBuildings(oneBuilding(raised));

        Report report = new Report(REPORT_BUILDING_RAISED, Report.PUBLIC);
        report.add(gamemasterName);
        report.add(raised.getName());
        report.add(spec.getCoords().getBoardNum());
        addReport(report);

        LOGGER.info("[GMBuilding] {} raised a {} building in hex {} (CF {}, {} levels)",
              gamemasterName, spec.getBuildingType(), spec.getCoords().getBoardNum(),
              spec.getConstructionFactor(), spec.getHeight());
        return null;
    }

    /** Takes a building away without leaving rubble, which is what a gamemaster removing one from the map means. */
    private String removeBuilding(Board board, IBuilding existing, BuildingEditSpec spec, String gamemasterName) {
        if (existing == null) {
            return "there is no building in that hex to remove";
        }
        board.removeBuilding(existing);
        gameManager.sendRemovedBuildings(oneBuilding(existing));
        gameManager.sendChangedHex(spec.getCoords(), spec.getBoardId());

        Report report = new Report(REPORT_BUILDING_REMOVED, Report.PUBLIC);
        report.add(gamemasterName);
        report.add(existing.getName());
        report.add(spec.getCoords().getBoardNum());
        addReport(report);

        LOGGER.info("[GMBuilding] {} removed the building in hex {}",
              gamemasterName, spec.getCoords().getBoardNum());
        return null;
    }

    /** Writes what the building is into the hex, which is what the board reads to make the building itself. */
    private static void writeBuildingTerrain(Hex hex, BuildingEditSpec spec) {
        clearBuildingTerrain(hex);
        hex.addTerrain(new Terrain(Terrains.BUILDING, spec.getBuildingType().getTypeValue()));
        hex.addTerrain(new Terrain(Terrains.BLDG_CF, spec.getConstructionFactor()));
        hex.addTerrain(new Terrain(Terrains.BLDG_ELEV, spec.getHeight()));
        hex.addTerrain(new Terrain(Terrains.BLDG_CLASS, spec.getBuildingClass()));
        if (spec.getArmor() > 0) {
            hex.addTerrain(new Terrain(Terrains.BLDG_ARMOR, spec.getArmor()));
        }
        hex.addTerrain(new Terrain(Terrains.BLDG_BASEMENT_TYPE, spec.getBasement().ordinal()));
        if (spec.getFluffImage() > 0) {
            hex.addTerrain(new Terrain(Terrains.BLDG_FLUFF, spec.getFluffImage()));
        }
    }

    /** Takes every scrap of building out of a hex, so nothing of the old one is left to confuse the new one. */
    private static void clearBuildingTerrain(Hex hex) {
        hex.removeTerrain(Terrains.BUILDING);
        hex.removeTerrain(Terrains.BLDG_CF);
        hex.removeTerrain(Terrains.BLDG_ELEV);
        hex.removeTerrain(Terrains.BLDG_CLASS);
        hex.removeTerrain(Terrains.BLDG_ARMOR);
        hex.removeTerrain(Terrains.BLDG_BASEMENT_TYPE);
        hex.removeTerrain(Terrains.BLDG_BASE_COLLAPSED);
        hex.removeTerrain(Terrains.BLDG_FLUFF);
    }

    /** @return the given building on its own, in the vector the broadcast methods take */
    private static Vector<IBuilding> oneBuilding(IBuilding building) {
        Vector<IBuilding> buildings = new Vector<>();
        buildings.add(building);
        return buildings;
    }

    /**
     * Applies a gamemaster's changes to the building in one hex, collapsing that hex of the building when the
     * construction factor is set to {@link #COLLAPSING_CONSTRUCTION_FACTOR}.
     *
     * @param coords         The hex whose part of the building is being changed
     * @param edit           The values to change, each of which may be left alone
     * @param gamemasterName The name of the gamemaster making the change, for the report
     *
     * @return A description of why the edit was refused, or {@code null} when it was applied
     */
    public String applyEdit(Coords coords, BuildingEdit edit, String gamemasterName) {
        IBuilding building = getGame().getBoard().getBuildingAt(coords);
        if (building == null) {
            LOGGER.debug("[GMBuilding] {}: refused - no building in hex {}", gamemasterName, coords.getBoardNum());
            return "there is no building in that hex";
        }

        boolean hexChanged = false;
        if (edit.armor() != null) {
            LOGGER.info("[GMBuilding] {} set the armor of {} in hex {} from {} to {}",
                  gamemasterName, building.getName(), coords.getBoardNum(), building.getArmor(coords), edit.armor());
            building.setArmor(edit.armor(), coords);
            hexChanged |= writeHexTerrain(coords, building.getBoardId(), Terrains.BLDG_ARMOR, edit.armor());
        }
        if (edit.height() != null) {
            LOGGER.info("[GMBuilding] {} set the height of {} in hex {} from {} to {}",
                  gamemasterName, building.getName(), coords.getBoardNum(), building.getHeight(coords), edit.height());
            building.setHeight(edit.height(), coords);
            hexChanged |= writeHexTerrain(coords, building.getBoardId(), Terrains.BLDG_ELEV, edit.height());
        }
        if (edit.basement() != null) {
            LOGGER.info("[GMBuilding] {} set the basement of {} in hex {} from {} to {}",
                  gamemasterName, building.getName(), coords.getBoardNum(),
                  building.getBasement(coords), edit.basement());
            building.setBasement(coords, edit.basement());
            hexChanged |= writeHexTerrain(coords, building.getBoardId(),
                  Terrains.BLDG_BASEMENT_TYPE, edit.basement().ordinal());
        }

        boolean collapsing = (edit.constructionFactor() != null)
              && (edit.constructionFactor() <= COLLAPSING_CONSTRUCTION_FACTOR);
        if (edit.constructionFactor() != null) {
            setConstructionFactor(building, coords, edit.constructionFactor(), gamemasterName);
        }

        if (collapsing) {
            collapse(building, coords, gamemasterName);
        } else {
            if (hexChanged) {
                gameManager.sendChangedHex(coords, building.getBoardId());
            }
            broadcast(building);
        }
        return null;
    }

    /**
     * Writes the construction factor to the building and says so. Both factors move together: the current one is the
     * building's standing state, and the phase one is what damage during this phase is measured against. Leaving the
     * phase factor behind would let damage already dealt this phase be counted against the old value.
     */
    private void setConstructionFactor(IBuilding building, Coords coords, int constructionFactor,
          String gamemasterName) {
        int previousFactor = building.getCurrentCF(coords);
        building.setCurrentCF(constructionFactor, coords);
        building.setPhaseCF(constructionFactor, coords);

        Report report = new Report(REPORT_BUILDING_CF_SET, Report.PUBLIC);
        report.add(gamemasterName);
        report.add(building.getName());
        report.add(coords.getBoardNum());
        report.add(constructionFactor);
        addReport(report);

        LOGGER.info("[GMBuilding] {} set the construction factor of {} in hex {} from {} to {}",
              gamemasterName, building.getName(), coords.getBoardNum(), previousFactor, constructionFactor);
    }

    /**
     * Keeps the hex's own record of a building value in step with the building's. The two are separate: the building
     * holds the value the rules read, while the hex terrain holds what the board is drawn from. Changing only the
     * building would leave a two-storey block still drawn four storeys tall.
     *
     * @return {@code true} if the hex was changed and needs sending to the clients
     */
    private boolean writeHexTerrain(Coords coords, int boardId, int terrainType, int level) {
        Hex hex = getGame().getHex(coords, boardId);
        if (hex == null) {
            return false;
        }
        hex.removeTerrain(terrainType);
        hex.addTerrain(new Terrain(terrainType, level));
        return true;
    }

    /**
     * Brings down the building in one hex through the same collapse the rules use, so that whatever is standing on it,
     * inside it, or in its basement is resolved rather than left hanging in the air.
     */
    private void collapse(IBuilding building, Coords coords, String gamemasterName) {
        LOGGER.info("[GMBuilding] {} collapses {} in hex {}",
              gamemasterName, building.getName(), coords.getBoardNum());
        Vector<Report> collapseReports = new Vector<>();
        collapseHandler.collapseBuilding(building, getGame().getPositionMapMulti(), coords, collapseReports);
        addReport(collapseReports);
    }

    /** Tells every client the building changed, so its display and tooltip follow the new construction factor. */
    private void broadcast(IBuilding building) {
        Vector<IBuilding> changed = new Vector<>();
        changed.add(building);
        gameManager.sendChangedBuildings(changed);
    }
}
