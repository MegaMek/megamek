/*
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.util;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import megamek.common.board.Coords;
import megamek.common.enums.BuildingType;
import megamek.common.units.IBuilding;

/**
 * Building template, for placing on the map during map generation.
 *
 * @author coelocanth
 */
public class BuildingTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = -911419490135815472L;

    public static final int BASEMENT_RANDOM = -1;

    private final ArrayList<Coords> coordsList;
    private final BuildingType type;
    private final int CF;
    private int height = 2;
    private int basement = BASEMENT_RANDOM;

    /**
     * Constructor for a building template.
     *
     * @param type   type of the building {@link BuildingType}
     * @param coords vector containing Coords of all hexes the building covers
     *
     * @deprecated Unused, will be removed in future versions. Use the other constructor
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public BuildingTemplate(BuildingType type, ArrayList<Coords> coords) {
        this.type = type;
        coordsList = coords;
        CF = IBuilding.getDefaultCF(type);
    }

    public BuildingTemplate(BuildingType type, ArrayList<Coords> coords, int CF, int height, int basement) {
        this.type = type;
        this.coordsList = coords;
        this.CF = CF;
        this.height = height;
        this.basement = basement;
    }

    /**
     * @return vector containing Coords of all hexes the building covers
     */
    public Iterator<Coords> getCoords() {
        return coordsList.iterator();
    }

    /**
     * @return type of the building (Building.LIGHT - Building.HARDENED)
     */
    public BuildingType getType() {
        return type;
    }

    /**
     * @return construction factor, used to initialise BLDG_CF
     */
    public int getCF() {
        return CF;
    }

    /**
     * @return height of the building, used to initialise BLDG_ELEV
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return basement settings - basements arent implemented yet
     */
    public int getBasement() {
        return basement;
    }

    public boolean containsCoords(Coords c) {
        return coordsList.contains(c);
    }
}
