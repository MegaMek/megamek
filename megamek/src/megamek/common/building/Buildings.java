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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Supplier;

import megamek.common.Coords;
import megamek.common.IBoard;
import megamek.common.IHex;
import megamek.common.Terrains;

/**
 * Utility class for instantiating buildings from an {@link IBoard} 
 */
public class Buildings {

    private Buildings() {
        // no instances
    }

    /**
     * Constructs a new building at the given coordinates, fetching info from the given board
     */
    public static Building newBuildingAt(Coords coords, IBoard board) {
        return Buildings.buildingAt(board, coords, Terrains.BUILDING);
    }

    /**
     * Constructs a new bridge at the given coordinates, fetching info from the given board
     */
    public static Building newBridgeAt(Coords coords, IBoard board) {
        return Buildings.buildingAt(board, coords, Terrains.BRIDGE);
    }

    /**
     * Constructs a new fuel tank at the given coordinates, fetching info from the given board
     */
    public static Building newFuelTankAt(Coords coords, IBoard board) {
        return Buildings.buildingAt(board, coords, Terrains.FUEL_TANK);
    }

    private static Building buildingAt(IBoard board, Coords coords, int structureType) {

        IHex initialHex = board.getHex(coords);

        int id = coords.hashCode();
        ConstructionType constructionType = initialHex.getConstructionType(structureType).orElseThrow(IllegalArgumentException::new);
        BuildingClass    buildingClass    = initialHex.getBuildingClass().orElse(null);

        OptionalInt explosionMagnitude = structureType == Terrains.FUEL_TANK
                                       ? OptionalInt.of(initialHex.getTerrain(Terrains.FUEL_TANK_MAGN).getLevel())
                                       : OptionalInt.empty();

        Map<Coords,BuildingSection> sections = new LinkedHashMap<>();
        getSpannedHexes(initialHex, board, structureType).values().forEach(hex -> {
            sections.put(hex.getCoords(), sectionAt(hex, structureType));
        });

        return new Building( id,
                             structureType,
                             constructionType,
                             buildingClass,
                             explosionMagnitude,
                             sections,
                             sections.size() );
    }

    private static BuildingSection sectionAt(IHex hex, int structureType) {

        if ((structureType != Terrains.BUILDING) && (structureType != Terrains.BRIDGE) && (structureType != Terrains.FUEL_TANK)) {
            throw new IllegalArgumentException("Unexpected structure type: " + structureType); //$NON-NLS-1$
        }

        ConstructionType constructionType = hex.getConstructionType(structureType)
                                               .orElseThrow(structureNotFoundException(hex, structureType));

        BasementType basementType = structureType == Terrains.BUILDING
                                  ? BasementType.ofId(hex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE)).orElse(BasementType.UNKNOWN)
                                  : BasementType.NONE;

        int cf; {
            switch (structureType) {
                case Terrains.BUILDING:  cf = hex.terrainLevel(Terrains.BLDG_CF);      break;
                case Terrains.BRIDGE:    cf = hex.terrainLevel(Terrains.BRIDGE_CF);    break;
                case Terrains.FUEL_TANK: cf = hex.terrainLevel(Terrains.FUEL_TANK_CF); break;
                default:                 throw new AssertionError(); // can't reach here: structureType has been validated
            }
            if (cf < 0) {
                cf = constructionType.getDefaultCF(); // THINKME shouldn't we throw instead?
            }
        }

        // BuildingClass bc = hex.getBuildingClass().orElseThrow(noBuilding); // can actually be missing

        boolean collapsed = hex.terrainLevel(Terrains.BLDG_BASE_COLLAPSED) == 1;

        int armor = hex.containsTerrain(Terrains.BLDG_ARMOR)
                  ? hex.terrainLevel(Terrains.BLDG_ARMOR)
                  : 0;

        return BuildingSection.of( hex.getCoords(),
                                   basementType,
                                   cf,      // current CF
                                   cf,      // phase CF
                                   armor,
                                   collapsed,
                                   false ); // burning?
    }

    private static Supplier<RuntimeException> structureNotFoundException(IHex hex, int structureType) {
        return () -> {
            String msg = String.format("No structure of type %s in hex %s", structureType, hex.getCoords().getBoardNum()); //$NON-NLS-1$
            return new IllegalArgumentException(msg);
        };
    }

    private static Map<Coords,IHex> getSpannedHexes(IHex hex , IBoard board, int structureType) {

        if (!(hex.containsTerrain(structureType))) {
            String msg = String.format("Hex %s does not contain structure %s", hex.getCoords(), structureType); //$NON-NLS-1$
            throw new IllegalArgumentException(msg);
        }

        Map<Coords,IHex> receptacle = new HashMap<>();
        getSpannedHexesRecurse(hex, board, structureType, receptacle);
        return receptacle;

    }

    private static void getSpannedHexesRecurse(IHex hex , IBoard board, int structureType, Map<Coords,IHex> receptacle) {

        receptacle.put(hex.getCoords(), hex);

        for (int dir = 0; dir < 6; dir++) {
            if (hex.containsTerrainExit(structureType, dir)) {
                Coords nextCoords = hex.getCoords().translated(dir);
                if (!receptacle.containsKey(nextCoords)) {
                    IHex nextHex = board.getHex(nextCoords);
                    if (nextHex != null && nextHex.containsTerrain(structureType)) {
                        getSpannedHexesRecurse(nextHex, board, structureType, receptacle);
                    }
                }
            }
        }

    }

}
