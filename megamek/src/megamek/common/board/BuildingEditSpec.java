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

package megamek.common.board;

import java.io.Serial;
import java.io.Serializable;

import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;

/**
 * A Game Master's edit of the building in one hex: what should be standing there when the edit is done.
 *
 * <p>This says what the hex should end up with rather than what to change, so the same message covers putting a
 * building where there was none, changing the one that is there, and taking it away. The server works out which of
 * those it is by looking at what the hex holds when the edit arrives.</p>
 *
 * <p>Only the ordinary map buildings are described here - the kind that live in the hex as terrain. The advanced
 * buildings are units in their own right and are placed the way units are placed, which is a different job.</p>
 *
 * <p>This travels between client and server, so everything it holds must be serializable. It is a message rather than
 * game state and is never written to a savegame.</p>
 */
public class BuildingEditSpec implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The hex the building stands in. */
    private final Coords coords;

    /** The board the hex is on. */
    private final int boardId;

    /** Whether the edit takes the building away rather than putting one there. */
    private boolean removingBuilding;

    /** What the building is made of, which decides how much punishment it takes. */
    private BuildingType buildingType = BuildingType.MEDIUM;

    /**
     * What the building is for: standard, hangar, fortress or gun emplacement, from the class constants on
     * {@link megamek.common.units.IBuilding}.
     */
    private int buildingClass;

    /** How much of the building is left standing. */
    private int constructionFactor = BuildingType.MEDIUM.getDefaultCF();

    /** The building's armor, which is nothing on an ordinary building. */
    private int armor;

    /** How many levels the building stands. */
    private int height = 1;

    /** What is underneath it. */
    private BasementType basement = BasementType.NONE;

    /**
     * Whether the structure is a fuel tank rather than a building. A fuel tank is built from its own terrain, has no
     * class or basement, and does not collapse when it is destroyed - it explodes.
     */
    private boolean fuelTank;

    /**
     * How big the explosion is when a fuel tank goes up. Meaningless on an ordinary building.
     *
     * <p>A fuel tank holds this as a final field set when the board builds it, so changing it means taking the tank
     * down and putting a new one up rather than adjusting the one that is there.</p>
     */
    private int magnitude = 1;

    /**
     * Which special image the building is drawn with, or zero for the ordinary artwork for its type. Boards use this
     * to give a district its look - a row of warehouses is ordinary medium buildings with a fluff image on each.
     */
    private int fluffImage;

    /**
     * Describes the building that should stand in the given hex.
     *
     * @param coords  The hex the building stands in
     * @param boardId The board the hex is on
     */
    public BuildingEditSpec(Coords coords, int boardId) {
        this.coords = coords;
        this.boardId = boardId;
    }

    public Coords getCoords() {
        return coords;
    }

    public int getBoardId() {
        return boardId;
    }

    /** @return {@code true} when the edit takes the building away rather than putting one there */
    public boolean isRemovingBuilding() {
        return removingBuilding;
    }

    public void setRemovingBuilding(boolean removingBuilding) {
        this.removingBuilding = removingBuilding;
    }

    public BuildingType getBuildingType() {
        return buildingType;
    }

    public void setBuildingType(BuildingType buildingType) {
        this.buildingType = buildingType;
    }

    public int getBuildingClass() {
        return buildingClass;
    }

    public void setBuildingClass(int buildingClass) {
        this.buildingClass = buildingClass;
    }

    public int getConstructionFactor() {
        return constructionFactor;
    }

    public void setConstructionFactor(int constructionFactor) {
        this.constructionFactor = constructionFactor;
    }

    public int getArmor() {
        return armor;
    }

    public void setArmor(int armor) {
        this.armor = armor;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public BasementType getBasement() {
        return basement;
    }

    /** @return {@code true} when the structure is a fuel tank rather than a building */
    public boolean isFuelTank() {
        return fuelTank;
    }

    public void setFuelTank(boolean fuelTank) {
        this.fuelTank = fuelTank;
    }

    /** @return how big the explosion is when the fuel tank goes up; meaningless on an ordinary building */
    public int getMagnitude() {
        return magnitude;
    }

    public void setMagnitude(int magnitude) {
        this.magnitude = magnitude;
    }

    /** @return which special image the building is drawn with, or zero for the ordinary artwork for its type */
    public int getFluffImage() {
        return fluffImage;
    }

    public void setFluffImage(int fluffImage) {
        this.fluffImage = fluffImage;
    }

    public void setBasement(BasementType basement) {
        this.basement = basement;
    }
}
