/*
 * Copyright (c) 2018 The MegaMek Team. All rights reserved.
 *
 * This file is part of MegaMek.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.building;

import java.util.List;

import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.IBoard;
import megamek.common.IHex;
import megamek.common.Report;
import megamek.common.Terrains;

/**
 * This class collects functionality originally in Building that is exclusively
 * used from Server and needs further cleanup/refactoring.
 */
public class BuildingServerHelper {

    private BuildingServerHelper() {
        // no instances
    }

    public static void collapseBasement(Building b, Coords coords, IBoard board, List<Report> vPhaseReport) {
        BuildingSection bs = b.sectionAt(coords).get();

        if (bs.getBasementType() == BasementType.NONE || bs.getBasementType() == BasementType.ONE_DEEP_NORMALINFONLY) {
            System.err.println("hex has no basement to collapse"); //$NON-NLS-1$
            return;
        }
        if (b.getBasementCollapsed(coords)) {
            System.err.println("hex has basement that already collapsed"); //$NON-NLS-1$
            return;
        }
        Report r = new Report(2112, Report.PUBLIC);
        r.add(b.getName());
        r.add(coords.getBoardNum());
        vPhaseReport.add(r);
        System.err.println("basement " + bs.getBasementType() + "is collapsing, hex:" //$NON-NLS-1$ //$NON-NLS-2$
                + coords.toString() + " set terrain!"); //$NON-NLS-1$
        board.getHex(coords).addTerrain(Terrains.getTerrainFactory().createTerrain(
                Terrains.BLDG_BASE_COLLAPSED, 1));
        b.setBasementCollapsed(coords, true);
    }

    /**
     * Roll what kind of basement this building has
     * @param coords the <code>Coords</code> of theb building to roll for
     * @param vPhaseReport the <code>Vector<Report></code> containing the phasereport
     * @return a <code>boolean</code> indicating wether the hex and building was changed or not
     */
    public static boolean rollBasement(Building b, Coords coords, IBoard board, List<Report> vPhaseReport) {
        BuildingSection bs = b.sectionAt(coords).get();
        if (bs.getBasementType() != BasementType.UNKNOWN) return false;

        IHex hex = board.getHex(coords);
        Report r = new Report(2111, Report.PUBLIC);
        r.add(b.getName());
        r.add(coords.getBoardNum());

        int basementRoll = Compute.d6(2);
        r.add(basementRoll);

        BasementType newType = BasementType.basementsTable(basementRoll);
        bs.setBasementType(newType);
        hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.BLDG_BASEMENT_TYPE, newType.getId()));
        r.add(newType.getDesc());

        vPhaseReport.add(r);
        return true;
    }

}
