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

import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.units.IBuilding;
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
     * Sets the construction factor of the building in one hex, collapsing that hex of the building when the factor is
     * set to {@link #COLLAPSING_CONSTRUCTION_FACTOR}.
     *
     * @param coords            The hex whose part of the building is being changed
     * @param constructionFactor The construction factor to leave the building at in that hex
     * @param gamemasterName    The name of the gamemaster making the change, for the report
     *
     * @return A description of why the edit was refused, or {@code null} when it was applied
     */
    public String setConstructionFactor(Coords coords, int constructionFactor, String gamemasterName) {
        IBuilding building = getGame().getBoard().getBuildingAt(coords);
        if (building == null) {
            LOGGER.debug("[GMBuilding] {}: refused - no building in hex {}", gamemasterName, coords.getBoardNum());
            return "there is no building in that hex";
        }

        int previousFactor = building.getCurrentCF(coords);
        // both factors move together: the current one is the building's standing state, and the phase one is what
        // damage during this phase is measured against. Leaving the phase factor behind would let damage already
        // dealt this phase be counted against the old value.
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

        if (constructionFactor <= COLLAPSING_CONSTRUCTION_FACTOR) {
            collapse(building, coords, gamemasterName);
        } else {
            broadcast(building);
        }
        return null;
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
