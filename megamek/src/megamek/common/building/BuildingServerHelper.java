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
import java.util.function.Consumer;

import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.IBoard;
import megamek.common.IHex;
import megamek.common.Report;
import megamek.common.Terrains;
import megamek.common.logging.DefaultMmLogger;

/**
 * This class collects functionality originally in Building that is exclusively
 * used from Server and needs further cleanup/refactoring.
 */
public class BuildingServerHelper {

    private BuildingServerHelper() {
        // no instances
    }

    /** @deprecated use {@link #collapseBasement(Building, IHex, Consumer)} instead */
    @Deprecated public static void collapseBasement(Building b, Coords coords, IBoard board, List<Report> vPhaseReport) {
        collapseBasement(b, board.getHex(coords), vPhaseReport::add);
    }

    public static void collapseBasement(Building b, IHex hex, Consumer<Report> disseminator) {
        BuildingSection bs = b.sectionAt(hex.getCoords()).get();

        if (bs.getBasementType() == BasementType.NONE || bs.getBasementType() == BasementType.ONE_DEEP_NORMALINFONLY) {

            String msg = String.format("Basement at %s cannot collapse (basement typet: %s)", hex.getCoords().getBoardNum(), bs.getBasementType()); //$NON-NLS-1$
            DefaultMmLogger.getInstance().warning(BuildingServerHelper.class, "collapseBasement", msg); //$NON-NLS-1$

        } else if (bs.isBasementCollapsed()) {

            String msg = String.format("Basement at %s already collapsed (basement typet: %s)", hex.getCoords().getBoardNum(), bs.getBasementType()); //$NON-NLS-1$
            DefaultMmLogger.getInstance().warning(BuildingServerHelper.class, "collapseBasement", msg); //$NON-NLS-1$

        } else {

            String msg = String.format("Basement at %s is collapsing (basement type: %s)", hex.getCoords().getBoardNum(), bs.getBasementType()); //$NON-NLS-1$
            DefaultMmLogger.getInstance().info(BuildingServerHelper.class, "collapseBasement", msg); //$NON-NLS-1$

            disseminator.accept(newHexCollapsedReport(b, bs));
            hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.BLDG_BASE_COLLAPSED, 1));
            bs.setBasementCollapsed(true);

        }
    }

    /**
     * Roll what kind of basement this building has
     * @param coords the <code>Coords</code> of theb building to roll for
     * @param vPhaseReport the <code>Vector<Report></code> containing the phasereport
     * @return a <code>boolean</code> indicating wether the hex and building was changed or not
     * 
     * @deprecated use {@link #rollBasement(Building, IHex, Consumer)} instead
     */
    @Deprecated public static boolean rollBasement(Building b, Coords coords, IBoard board, List<Report> vPhaseReport) {
        return rollBasement(b, board.getHex(coords), vPhaseReport::add);
    }

    public static boolean rollBasement(Building b, IHex hex, Consumer<Report> disseminator) {
        BuildingSection bs = b.sectionAt(hex.getCoords()).get();
        if (bs.getBasementType() != BasementType.UNKNOWN) {
            return false;
        } else {
            int roll = Compute.d6(2);
            BasementType bt = BasementType.basementsTable(roll);
            bs.setBasementType(bt);
            hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.BLDG_BASEMENT_TYPE, bt.getId()));
            disseminator.accept(newBasementRolledReport(b, bs, roll, bt));
            return true;
        }
    }

    // LATER these need to be moved in some ReportFactory of sorts

    private static Report newHexCollapsedReport(Building b, BuildingSection bs) {
        Report r = new Report(2112, Report.PUBLIC);
        r.add(b.getName());
        r.add(bs.getCoordinates().getBoardNum());
        return r;
    }

    private static Report newBasementRolledReport(Building b, BuildingSection bs, int basementRoll, BasementType newType) {
        Report r = new Report(2111, Report.PUBLIC);
        r.add(b.getName());
        r.add(bs.getCoordinates().getBoardNum());
        r.add(basementRoll);
        r.add(newType.getDesc());
        return r;
    }

}
