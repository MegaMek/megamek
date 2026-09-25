/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.loaders;

import megamek.common.board.CubeCoords;
import megamek.common.compute.Compute;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.enums.StructureEngine;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.MobileStructure;
import megamek.common.util.BuildingBlock;
import megamek.logging.MMLogger;

public class BLKStructureFile extends BLKFile implements IMekLoader {
    private static final MMLogger LOGGER = MMLogger.create(BLKStructureFile.class);

    public BLKStructureFile(BuildingBlock block) {
        dataFile = block;
    }

    /**
     * @return A valid mek, matching the file to the best of MegaMek's current capabilities
     *
     * @throws Exception when the file type isn't understood or the file can't be parsed.
     */
    @Override
    public Entity getEntity() throws Exception {
        if (!dataFile.exists("building_type")) {
            throw new EntityLoadingException("Could not find building_type block.");
        }
        BuildingType buildingType = BuildingType.getType(dataFile.getDataAsInt("building_type")[0]);


        if (!dataFile.exists("building_class")) {
            throw new EntityLoadingException("Could not find building_class block.");
        }
        int buildingClass = dataFile.getDataAsInt("building_class")[0];


        AbstractBuildingEntity be = dataFile.getDataAsString("UnitType")[0].equalsIgnoreCase("MobileStructure")
              ? new MobileStructure(buildingType, buildingClass) : new BuildingEntity(buildingType, buildingClass);
        setBasicEntityData(be);

        if (be instanceof MobileStructure mobile) {
            for (String field : new String[] { "motion_type", "cruiseMP", "power_system", "operating_range" }) {
                if (!dataFile.exists(field)) {
                    throw new EntityLoadingException("Missing Mobile Structure " + field + " block.");
                }
            }
            try {
                mobile.setMovementMode(EntityMovementMode.parseFromString(dataFile.getDataAsString("motion_type")[0]));
                mobile.setMaximumMP(Double.parseDouble(dataFile.getDataAsString("cruiseMP")[0]));
                mobile.setPowerSystem(StructureEngine.valueOf(dataFile.getDataAsString("power_system")[0]));
                mobile.setOperatingRange(Double.parseDouble(dataFile.getDataAsString("operating_range")[0]));
            } catch (IllegalArgumentException ex) {
                throw new EntityLoadingException("Invalid Mobile Structure propulsion: " + ex.getMessage());
            }
        } else {
            be.setEngine(new Engine(0, Engine.NONE, 0));
        }

        if (!dataFile.exists("height")) {
            throw new EntityLoadingException("Could not find height block.");
        }
        be.getInternalBuilding().setBuildingHeight(dataFile.getDataAsInt("height")[0]);

        if (!dataFile.exists("cf")) {
            throw new EntityLoadingException("Could not find cf block.");
        }
        int cf = dataFile.getDataAsInt("cf")[0];


        if (!dataFile.exists("armor")) {
            throw new EntityLoadingException("Could not find armor block.");
        }
        int armor = dataFile.getDataAsInt("armor")[0];

        if (!dataFile.exists("coords")) {
            throw new EntityLoadingException("Could not find coords block.");
        }
        if (be.getInternalBuilding().getBuildingHeight() < 1) {
            throw new EntityLoadingException("Building height must be positive.");
        }
        CubeCoords[] coordinates = dataFile.getDataAsCubeCoords("coords");
        if (coordinates.length == 0) {
            throw new EntityLoadingException("A building needs at least one hex.");
        }
        for (CubeCoords coords : coordinates) {
            be.getInternalBuilding().addHex(coords, cf, armor, BasementType.NONE, false);
        }
        if (be instanceof MobileStructure && dataFile.exists("hex_heights")) {
            int[] heights = dataFile.getDataAsInt("hex_heights");
            if (heights.length != coordinates.length) {
                throw new EntityLoadingException("Mobile Structure hex_heights must match the coords block.");
            }
            for (int index = 0; index < heights.length; index++) {
                if (heights[index] < 1 || heights[index] > be.getInternalBuilding().getBuildingHeight()) {
                    throw new EntityLoadingException("Each hex height must be between 1 and the structure height.");
                }
                be.getInternalBuilding().setHeight(heights[index], coordinates[index]);
            }
        }

        if (be instanceof MobileStructure mobile && dataFile.exists("fuel_locations")) {
            var fuel = new java.util.LinkedHashMap<CubeCoords, Double>();
            try {
                for (String line : dataFile.getDataAsString("fuel_locations")) {
                    String[] parts = line.trim().split(";", -1);
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("Expected q,r,s;tons");
                    }
                    String[] xyz = parts[0].split(",", -1);
                    if (xyz.length != 3) {
                        throw new IllegalArgumentException("Expected three cube coordinates");
                    }
                    int q = new java.math.BigDecimal(xyz[0].trim()).intValueExact();
                    int r = new java.math.BigDecimal(xyz[1].trim()).intValueExact();
                    int s = new java.math.BigDecimal(xyz[2].trim()).intValueExact();
                    if ((long) q + r + s != 0 || fuel.put(new CubeCoords(q, r, s),
                          Double.parseDouble(parts[1].trim())) != null) {
                        throw new IllegalArgumentException("Invalid or repeated fuel coordinate");
                    }
                }
                mobile.setFuelLocations(fuel);
            } catch (IllegalArgumentException | ArithmeticException ex) {
                throw new EntityLoadingException("Invalid fuel_locations: " + ex.getMessage());
            }
        }


        if (!dataFile.exists("year")) {
            throw new EntityLoadingException("Could not find year block.");
        }
        be.setYear(dataFile.getDataAsInt("year")[0]);

        if (dataFile.exists("crew")) {
            be.setCrewCount(dataFile.getDataAsInt("crew")[0]);
        }


        be.refreshLocations();
        be.refreshAdditionalLocations();

        // Once all coords are loaded, we can start loading equipment.
        // Origin, ground floor will be the first location. We then iterate through the rest of the floors for the
        // origin, then continue from there.
        // Building doesn't need to have any equipment though.
        for (CubeCoords coords : be.getInternalBuilding().getCoordsList()) {
            int index = be.getInternalBuilding().getCoordsList().indexOf(coords);
            int height = be.getInternalBuilding().getBuildingHeight();
            for (int floor = 0; floor < height; floor++) {
                int loc = floor + (index * height);
                // Armor & internal are set but only used to display to the user
                be.initializeInternal(cf, loc);
                be.initializeArmor(armor, loc);

                String equipmentBlockName = be.getConstructionLocationName(loc);
                loadEquipment(be, equipmentBlockName, loc);
            }
        }

        addTransports(be);
        BuildingDesignCodec.read(dataFile, be);
        loadQuirks(be);

        // Reset our armor type & tech level now that we have all our locations set up
        be.recalculateTechAdvancement();
        sizeCrewToHeadCount(be);


        return be;
    }

    /**
     * Gives the building's crew object the head-count the building actually has, so anything that counts or
     * removes people, an infantry action inside the building for one, works on the real crew rather than the single
     * commander slot the crew type provides.
     */
    private static void sizeCrewToHeadCount(AbstractBuildingEntity building) {
        int headCount = Compute.getFullCrewSize(building);
        building.getCrew().setSize(headCount);
        building.getCrew().setCurrentSize(headCount);
        LOGGER.debug("[BuildingCrew] {}: crew {} ({}), {} bay personnel, crew object sized to {}",
              building.getShortName(), building.getNCrew(),
              building.hasExplicitCrewCount() ? "from the unit file" : "Advanced Building Minimum Crew Table",
              building.getBayPersonnel(), headCount);
    }
}
