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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import megamek.common.Coords;

/**
 * Represents one hex worth of building
 */
public class BuildingSection implements Serializable {

    private static final long serialVersionUID = 1L;

    BuildingSection( Coords coordinates,
                     BasementType basementType,
                     int currentCF,
                     int phaseCF,
                     int armor,
                     boolean basementCollapsed,
                     boolean burning ) {
        this.coordinates  = coordinates;
        this.basementType = basementType;
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
    private Set<DemolitionCharge> demolitionCharges = new LinkedHashSet<>();

    public Coords getCoordinates() {
        return coordinates;
    }

   /**
    * @return the amount of damage the building absorbs
    */
   public int getAbsorbtion() {
       return (int) Math.ceil(phaseCF / 10.0);
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
        if (currentCF < 0) {
            throw new IllegalArgumentException("Invalid CF value: " + currentCF); //$NON-NLS-1$
        }
        this.currentCF = currentCF;
    }

    public int getPhaseCF() {
        return phaseCF;
    }

    public void setPhaseCF(int phaseCF) {
        if (phaseCF < 0) {
            throw new IllegalArgumentException("Invalid CF value: " + phaseCF); //$NON-NLS-1$
        }
        this.phaseCF = phaseCF;
    }

    public int getArmor() {
        return armor;
    }

    public void setArmor(int armor) {
        if (armor < 0) {
            throw new IllegalArgumentException("Invalid armor value: " + armor); //$NON-NLS-1$
        }
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

    public Stream<DemolitionCharge> streamDemolitionCharges() {
        return demolitionCharges.stream();
    }

    public void setDemolitionCharges(List<DemolitionCharge> charges) {
        demolitionCharges.clear();
        demolitionCharges.addAll(charges);
    }

    public void addDemolitionCharge(int playerId, int damage) {
        demolitionCharges.add(new DemolitionCharge(playerId, damage, null));
    }

    public boolean removeDemolitionCharge(DemolitionCharge charge) {
        return demolitionCharges.remove(charge);
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
