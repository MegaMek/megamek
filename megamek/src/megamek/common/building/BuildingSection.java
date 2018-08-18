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

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Supplier;

import megamek.common.Coords;
import megamek.common.IHex;
import megamek.common.Terrains;
import megamek.common.logging.DefaultMmLogger;

/**
 * Represents one hex worth of building
 */
public class BuildingSection implements Serializable {

    private static final long serialVersionUID = 1L;

    public static BuildingSection at(IHex hex, int structureType, BasementType defaultBasementType) {

        if (!hex.getConstructionType(structureType).isPresent()) {
            String msg = String.format("No construction type at: %s", hex.getCoords()); //$NON-NLS-1$
            DefaultMmLogger.getInstance().warning(BuildingSection.class, "at", msg); //$NON-NLS-1$
        }

        Supplier<IllegalArgumentException> noBuilding = () -> {
            return new IllegalArgumentException("No building at " + hex.getCoords()); //$NON-NLS-1$
        };


        BasementType basementType = structureType == Terrains.BUILDING
                                  ? BasementType.ofRequiredId(hex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE))
                                  : defaultBasementType;

        int cf;
        switch (structureType) {
        case Terrains.BUILDING:  cf = hex.terrainLevel(Terrains.BLDG_CF);      break;
        case Terrains.BRIDGE:    cf = hex.terrainLevel(Terrains.BRIDGE_CF);    break;
        case Terrains.FUEL_TANK: cf = hex.terrainLevel(Terrains.FUEL_TANK_CF); break;
        default:
            String msg = String.format("Hex %s has an unnexpected structure type of %s - assuming Terrains.BUILDING (%s) instead", hex.getCoords().getBoardNum(), structureType, Terrains.BUILDING); //$NON-NLS-1$
            DefaultMmLogger.getInstance().warning(BuildingSection.class, "at", msg); //$NON-NLS-1$
            cf = hex.terrainLevel(Terrains.BLDG_CF);
        }
        
        ConstructionType ct = hex.getConstructionType(structureType).orElseThrow(noBuilding);
        if (cf < 0) {
            cf = ct.getId();
        }

        // BuildingClass bc = hex.getBuildingClass().orElseThrow(noBuilding); // can actually be missing

        boolean collapsed = hex.terrainLevel(Terrains.BLDG_BASE_COLLAPSED) == 1;

        int armor = hex.containsTerrain(Terrains.BLDG_ARMOR)
                  ? hex.terrainLevel(Terrains.BLDG_ARMOR)
                  : 0;

        return new BuildingSection( hex.getCoords(),
                                    basementType,
                                    cf,      // current CF
                                    cf,      // phase CF
                                    armor,
                                    collapsed,
                                    false ); // burning?
    }

    BuildingSection( Coords coordinates,
                     BasementType basementType,
                     int currentCF,
                     int phaseCF,
                     int armor,
                     boolean basementCollapsed,
                     boolean burning ) {
        this.coordinates  = Objects.requireNonNull(coordinates);
        this.basementType = Objects.requireNonNull(basementType);
        // XXX add validation - must determine what it should be first  :)
        this.currentCF    = currentCF;
        this.phaseCF      = phaseCF;
        this.armor        = armor;
        this.basementCollapsed = basementCollapsed;
        this.burning           = burning;
    }

    private final Coords coordinates;
    private BasementType basementType;
    private int currentCF; // any damage immediately updates this value
    private int phaseCF ; // cf at start of phase - damage is applied at the end of the phase it was received in
    private int armor;
    private boolean basementCollapsed;
    private boolean burning;

    public Coords getCoordinates() {
        return coordinates;
    }

    public BasementType getBasementType() {
        return basementType;
    }

    public void setBasementType(BasementType basementType) {
        this.basementType = Objects.requireNonNull(basementType);
    }

    public int getCurrentCF() {
        return currentCF;
    }

    public void setCurrentCF(int currentCF) {
        this.currentCF = currentCF;
    }

    public int getPhaseCF() {
        return phaseCF;
    }

    public void setPhaseCF(int phaseCF) {
        this.phaseCF = phaseCF;
    }

    public int getArmor() {
        return armor;
    }

    public void setArmor(int armor) {
        this.armor = armor;
    }

    public boolean isBasementCollapsed() {
        return basementCollapsed;
    }

    public void setBasementCollapsed(boolean basementCollapsed) {
        this.basementCollapsed = basementCollapsed;
    }

    public boolean isBurning() {
        return burning;
    }

    public void setBurning(boolean burning) {
        this.burning = burning;
    }

    @Override
    public int hashCode() {
        return Objects.hash( coordinates,
                             basementType,
                             currentCF,
                             phaseCF,
                             armor,
                             basementCollapsed,
                             burning );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        BuildingSection other = (BuildingSection) obj;
        return coordinates.equals(other.coordinates)
            && armor == other.armor
            && currentCF == other.currentCF
            && phaseCF == other.phaseCF
            && burning == other.burning
            && basementCollapsed == other.basementCollapsed;
    }

}
