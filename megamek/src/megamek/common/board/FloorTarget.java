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
package megamek.common.board;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

import megamek.common.Hex;
import megamek.common.Messages;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Targetable;

/**
 * This class represents a single, targetable hex and floor of a building. The building itself may occupy multiple
 * hexes.
 *
 * @author Luana Coppio
 */
public class FloorTarget implements Targetable {
    @Serial
    private static final long serialVersionUID = 64327660924039635L;

    /**
     * The coordinates of the hex being targeted.
     */
    private final Coords position;
    private int id;
    private final int elevation;
    private final String name;
    private final int targetElevation;

    /**
     * Target a single hex of a building.
     *
     * @param coords          - the <code>Coords</code> of the hex being targeted.
     * @param board           - the game's <code>Board</code> object.
     * @param targetElevation - Kind where in the building you want to aim at.
     *
     * @throws IllegalArgumentException will be thrown if the given coordinates do not contain a building.
     */
    public FloorTarget(Coords coords, Board board, int targetElevation) {
        position = coords;

        IBuilding bldg = board.getBuildingAt(position);
        if (bldg == null) {
            throw new IllegalArgumentException("The coordinates, " + position.getBoardNum()
                  + ", do not contain a building.");
        }
        // Save the building's ID.
        id = FloorTarget.coordsToId(coords);

        // Generate a name.
        name = "Hex " + position.getBoardNum() + " of " + bldg.getName() + Messages.getString(
              "BuildingTarget.Ignite");
        Hex targetHex = board.getHex(position);
        this.elevation = targetHex.floor();
        this.targetElevation = targetElevation;
    }

    @Override
    public int getTargetType() {
        return Targetable.TYPE_BUILDING;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int newId) {
        id = newId;
    }

    @Override
    public int getOwnerId() {
        return Player.PLAYER_NONE;
    }

    @Override
    public void setOwnerId(int newOwnerId) {}

    @Override
    public int getStrength() {
        return 0;
    }

    @Override
    public Coords getPosition() {
        return position;
    }

    @Override
    public Map<Integer, Coords> getSecondaryPositions() {
        return new HashMap<>();
    }

    @Override
    public int relHeight() {
        return getHeight() + getElevation();
    }

    @Override
    public int getHeight() {
        return elevation + targetElevation;
    }

    @Override
    public int getElevation() {
        return elevation;
    }

    @Override
    public boolean isImmobile() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    /**
     * Creates an id for this building based on its location as well as a building code. The transformation encodes the
     * y value in the top 5 decimal digits and the x value in the bottom 5. Could more efficiently encode this by
     * partitioning the binary representation, but this is more human-readable and still allows for a 99999x99999 hex
     * map.
     */
    public static int coordsToId(Coords c) {
        return Targetable.TYPE_BUILDING * 1000000 + c.getY() * 1000 + c.getX();
    }

    // decode 1 number into 2
    public static Coords idToCoords(int id) {
        int idNoType = id - Targetable.TYPE_BUILDING * 1000000;
        int y = (idNoType) / 1000;
        return new Coords(idNoType - (y * 1000), y);
    }

    @Override
    public int sideTable(Coords src) {
        return ToHitData.SIDE_FRONT;
    }

    @Override
    public int sideTable(Coords src, boolean usePrior) {
        return sideTable(src);
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.units.Targetable#isOffBoard()
     */
    @Override
    public boolean isOffBoard() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.units.Targetable#isAirborne()
     */
    @Override
    public boolean isAirborne() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.units.Targetable#isAirborneVTOLorWIGE()
     */
    @Override
    public boolean isAirborneVTOLorWIGE() {
        return false;
    }

    @Override
    public int getAltitude() {
        return 0;
    }

    @Override
    public boolean isEnemyOf(Entity other) {
        return true;
    }

    @Override
    public String generalName() {
        return name;
    }

    @Override
    public String specificName() {
        return "";
    }
}
