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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import megamek.client.ui.Messages;
import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.annotations.Nullable;
import megamek.common.enums.BasementType;
import megamek.common.board.Board;
import megamek.common.board.BuildingEditSpec;
import megamek.common.board.Coords;
import megamek.common.equipment.FuelTank;
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
    private static final int REPORT_HEX_RESTORED = 1259;

    /**
     * Each hex as it stood before the first gamemaster edit reached it, so it can be put back. Keyed by hex and kept
     * until it is restored, so a gamemaster who changes a building three times still gets back what was there before
     * they started.
     */
    private final Map<Coords, Hex> hexBeforeFirstEdit = new LinkedHashMap<>();

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
        if (spec.isRestoringOriginal()) {
            return restoreOriginal(board, spec, gamemasterName);
        }
        rememberHexBeforeFirstEdit(board, spec.getCoords());
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
     * Keeps a copy of a hex the first time a gamemaster edit reaches it, so it can be put back the way it was before
     * they started. Only the first is kept: later edits are changes to a hex that is already being worked on, and
     * restoring should undo all of them rather than the most recent one.
     *
     * <p>What the players did to the hex before the gamemaster touched it is part of that copy and comes back with
     * it, which is the point: this restores the hex to how it stood, not to how the board shipped.</p>
     */
    private void rememberHexBeforeFirstEdit(Board board, Coords coords) {
        if (hexBeforeFirstEdit.containsKey(coords)) {
            return;
        }
        Hex hex = board.getHex(coords);
        if (hex != null) {
            hexBeforeFirstEdit.put(coords, hex.duplicate());
        }
    }

    /**
     * Puts a hex back the way it stood before any gamemaster edit reached it, and rebuilds whatever structure that
     * describes.
     *
     * @return A description of why the restore was refused, or {@code null} when it was applied
     */
    private String restoreOriginal(Board board, BuildingEditSpec spec, String gamemasterName) {
        Hex original = hexBeforeFirstEdit.get(spec.getCoords());
        if (original == null) {
            LOGGER.debug("[GMBuilding] {}: nothing to restore in hex {}",
                  gamemasterName, spec.getCoords().getBoardNum());
            return "that hex has not been changed by a gamemaster, so there is nothing to put back";
        }

        IBuilding standing = board.getBuildingAt(spec.getCoords());
        if (standing != null) {
            board.removeBuilding(standing);
            gameManager.sendRemovedBuildings(oneBuilding(standing));
        }
        board.setHex(spec.getCoords(), original.duplicate());

        IBuilding rebuilt = rebuildFromHex(board, spec.getCoords());
        if (rebuilt != null) {
            board.addBuildingToBoard(rebuilt);
            gameManager.sendNewBuildings(oneBuilding(rebuilt));
        }
        gameManager.sendChangedHex(spec.getCoords(), spec.getBoardId());
        hexBeforeFirstEdit.remove(spec.getCoords());

        Report report = new Report(REPORT_HEX_RESTORED, Report.PUBLIC);
        report.add(gamemasterName);
        report.add(spec.getCoords().getBoardNum());
        addReport(report);

        LOGGER.info("[GMBuilding] {} put hex {} back the way it was before it was edited",
              gamemasterName, spec.getCoords().getBoardNum());
        return null;
    }

    /**
     * @return the structure the restored hex describes - a fuel tank, a building, or {@code null} when the hex holds
     *       neither and the restore simply leaves bare ground
     */
    private IBuilding rebuildFromHex(Board board, Coords coords) {
        Hex hex = board.getHex(coords);
        try {
            if (hex.containsTerrain(Terrains.FUEL_TANK)) {
                return new FuelTank(coords, board, Terrains.FUEL_TANK, hex.terrainLevel(Terrains.FUEL_TANK_MAGN));
            }
            if (hex.containsTerrain(Terrains.BUILDING)) {
                return new BuildingTerrain(coords, board, Terrains.BUILDING,
                      BasementType.getType(hex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE)));
            }
        } catch (RuntimeException buildFailure) {
            LOGGER.error(buildFailure, "[GMBuilding] could not rebuild the structure in restored hex {}",
                  coords.getBoardNum());
        }
        return null;
    }

    /**
     * @return {@code true} when the building standing there is not the kind the gamemaster asked for, so it has to be
     *       taken down and put up again rather than adjusted
     */
    private static boolean needsRebuilding(IBuilding existing, BuildingEditSpec spec) {
        boolean wasFuelTank = existing instanceof FuelTank;
        if (wasFuelTank != spec.isFuelTank()) {
            return true;
        }
        if (wasFuelTank) {
            // a fuel tank holds its magnitude as a final field set when the board builds it, so the only way to
            // change how big the explosion is is to put a new tank up
            return ((FuelTank) existing).getMagnitude() != spec.getMagnitude();
        }
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
            raised = spec.isFuelTank()
                  ? new FuelTank(spec.getCoords(), board, Terrains.FUEL_TANK, spec.getMagnitude())
                  : new BuildingTerrain(spec.getCoords(), board, Terrains.BUILDING, spec.getBasement());
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
        if (spec.isFuelTank()) {
            hex.addTerrain(new Terrain(Terrains.FUEL_TANK, 1));
            hex.addTerrain(new Terrain(Terrains.FUEL_TANK_CF, spec.getConstructionFactor()));
            hex.addTerrain(new Terrain(Terrains.FUEL_TANK_ELEV, spec.getHeight()));
            hex.addTerrain(new Terrain(Terrains.FUEL_TANK_MAGN, spec.getMagnitude()));
            return;
        }
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
        hex.removeTerrain(Terrains.FUEL_TANK);
        hex.removeTerrain(Terrains.FUEL_TANK_CF);
        hex.removeTerrain(Terrains.FUEL_TANK_ELEV);
        hex.removeTerrain(Terrains.FUEL_TANK_MAGN);
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
        // a building being brought down is not given a construction factor of zero and then collapsed: the damage
        // that brings it down is worked out from what it still has, so writing zero first would leave nothing to
        // deal and the building would stand there at zero instead of falling
        if ((edit.constructionFactor() != null) && !collapsing) {
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
     * Brings a building down by damaging it to nothing, exactly as a unit shooting at it would.
     *
     * <p>Forcing the collapse directly would skip everything the rules do on the way there. Damage is taken by the
     * building's armor first and then scaled by what the building is made of; a fuel tank reaching zero does not
     * collapse but explodes, taking the neighbourhood with it; a wall reports its own way; and gun emplacements in
     * the building roll for criticals. A gamemaster setting the construction factor to zero means the building comes
     * down the way it would have if someone had shot it, so it is dealt the damage to do that and then checked for
     * collapse the way damage is.</p>
     */
    private void collapse(IBuilding building, Coords coords, String gamemasterName) {
        int damage = damageNeededToFlatten(building, coords);
        LOGGER.info("[GMBuilding] {} brings down {} in hex {} with {} damage, the way an attack would",
              gamemasterName, building.getName(), coords.getBoardNum(), damage);

        addReport(gameManager.damageBuilding(building, damage,
              Messages.getString("Gamemaster.cmd.building.damageReason"), coords));

        Vector<Report> collapseReports = new Vector<>();
        boolean collapsed = collapseHandler.checkForCollapse(building, coords, true, collapseReports);
        addReport(collapseReports);

        if (!collapsed && isStillStandingAtNothing(building, coords)) {
            // checkForCollapse refuses to do anything when no unit is anywhere on the board: it treats an empty
            // position map as a bad argument, because it is written for damage resolution where units exist by
            // definition. A gamemaster flattening a building on an empty map hit exactly that and was left with a
            // building standing at a construction factor of zero, so it is brought down here instead.
            LOGGER.debug("[GMBuilding] {} still stands at zero after damage - bringing it down directly",
                  building.getName());
            Vector<Report> forcedReports = new Vector<>();
            collapseHandler.collapseBuilding(building, getGame().getPositionMapMulti(), coords, forcedReports);
            addReport(forcedReports);
        }
    }

    /**
     * @return {@code true} when the building is still on the board with nothing left to hold it up, which means
     *       something declined to bring it down
     */
    private boolean isStillStandingAtNothing(IBuilding building, Coords coords) {
        Board board = getGame().getBoard(building.getBoardId());
        if ((board == null) || (board.getBuildingAt(coords) == null)) {
            // already gone, which is what a fuel tank does: it explodes rather than collapsing
            return false;
        }
        return building.getCurrentCF(coords) <= COLLAPSING_CONSTRUCTION_FACTOR;
    }

    /**
     * Works out how much damage takes a building from where it is to nothing, so that the same damage the rules
     * apply gets it exactly to zero rather than part way.
     *
     * <p>Armor is taken off first and at face value. What is left goes through the building's damage scaling, which
     * can be less than one for a sturdy building, so more raw damage than the construction factor may be needed to
     * remove it.</p>
     *
     * @return the damage that leaves the building at a construction factor of zero
     */
    private static int damageNeededToFlatten(IBuilding building, Coords coords) {
        int armor = Math.max(0, building.getArmor(coords));
        int constructionFactor = Math.max(0, building.getCurrentCF(coords));
        double scale = building.getDamageToScale();
        int damageThroughScaling = (scale <= 0)
              ? constructionFactor
              : (int) Math.ceil(constructionFactor / scale);
        return armor + damageThroughScaling;
    }

    /** Tells every client the building changed, so its display and tooltip follow the new construction factor. */
    private void broadcast(IBuilding building) {
        Vector<IBuilding> changed = new Vector<>();
        changed.add(building);
        gameManager.sendChangedBuildings(changed);
    }
}
