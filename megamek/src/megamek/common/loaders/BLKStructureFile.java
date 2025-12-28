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
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.Engine;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Building;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Entity;
import megamek.common.util.BuildingBlock;

public class BLKStructureFile extends BLKFile implements IMekLoader {
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


        AbstractBuildingEntity be = new BuildingEntity(buildingType, buildingClass);
        setBasicEntityData(be);

        // Buildings don't use engines.
        be.setEngine(new Engine(0, Engine.NONE, 0));

        if (!dataFile.exists("height")) {
            throw new EntityLoadingException("Could not find height block.");
        }
        be.getInternalBuilding().setBuildingHeight(dataFile.getDataAsInt("height")[0]);

        if (!dataFile.exists("cf")) {
            throw new EntityLoadingException("Could not find cf block.");
        }
        int cf = dataFile.getDataAsInt("cf")[0];
        be.setInternal(cf, 0);


        if (!dataFile.exists("armor")) {
            throw new EntityLoadingException("Could not find armor block.");
        }
        int armor = dataFile.getDataAsInt("armor")[0];
        be.initializeArmor(armor, 0);

        if (!dataFile.exists("coords")) {
            throw new EntityLoadingException("Could not find coords block.");
        }
        for (CubeCoords coords : dataFile.getDataAsCubeCoords("coords")) {
            be.getInternalBuilding().addHex(coords, cf, armor, BasementType.NONE, false);
        }



        if (!dataFile.exists("year")) {
            throw new EntityLoadingException("Could not find year block.");
        }
        be.setYear(dataFile.getDataAsInt("year")[0]);


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

                String equipmentBlockName = be.getLocationName(loc);
                loadEquipment(be, equipmentBlockName, loc);
            }
        }

        // Reset our armor type & tech level now that we have all our locations set up
        be.recalculateTechAdvancement();


        return be;
    }
}
