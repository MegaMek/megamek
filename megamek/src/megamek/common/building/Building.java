/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.building;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import megamek.common.Coords;
import megamek.common.IBoard;
import megamek.common.ITerrain;
import megamek.common.Report;
import megamek.common.Terrains;

/**
 * Represents a single, possibly multi-hex building on the board.
 *
 * @author Suvarov454@sourceforge.net (James A. Damour )
 */
public class Building implements Serializable {

    private static final long serialVersionUID = -8236017592012683793L;

    /** @deprecated use {@link ConstructionType#LIGHT}    instead */ @Deprecated public static final int LIGHT = 1;
    /** @deprecated use {@link ConstructionType#MEDIUM}   instead */ @Deprecated public static final int MEDIUM = 2;
    /** @deprecated use {@link ConstructionType#HEAVY}    instead */ @Deprecated public static final int HEAVY = 3;
    /** @deprecated use {@link ConstructionType#HARDENED} instead */ @Deprecated public static final int HARDENED = 4;
    /** @deprecated use {@link ConstructionType#WALL}     instead */ @Deprecated public static final int WALL = 5;

    /** @deprecated use {@link BuildingClass#STANDARD}        instead */ @Deprecated public static final int STANDARD = 0;
    /** @deprecated use {@link BuildingClass#HANGAR}          instead */ @Deprecated public static final int HANGAR = 1;
    /** @deprecated use {@link BuildingClass#FORTRESS}        instead */ @Deprecated public static final int FORTRESS = 2;
    /** @deprecated use {@link BuildingClass#GUN_EMPLACEMENT} instead */ @Deprecated public static final int GUN_EMPLACEMENT = 3;

    Building( int id,
              int structureType,
              ConstructionType constructionType,
              BuildingClass buildingClass,
              OptionalInt explosionMagnitude,
              Map<Coords, BuildingSection> sections,
              int originalHexes,
              List<DemolitionCharge> demolitionCharges ) {
        this.id = id;
        this.structureType = structureType;
        this.constructionType = constructionType;
        this.buildingClass = buildingClass;
        this.explosionMagnitude = explosionMagnitude.isPresent()
                                ? explosionMagnitude.getAsInt()
                                : null;
        this.sections = sections;
        this.originalHexes = originalHexes;
        this.demolitionCharges = demolitionCharges;
    }

    private final int id;
    private final int structureType;
    private final ConstructionType constructionType;
    private final BuildingClass buildingClass; // nullable - can't use Optional    as it's not Serializable
    private final Integer explosionMagnitude;  // nullable - can't use OptionalInt as it's not Serializable

    private final Map<Coords, BuildingSection> sections;
    private final int originalHexes;

    private List<DemolitionCharge> demolitionCharges = new ArrayList<>();

    public Optional<BuildingSection> sectionAt(Coords coordinates) {
        return Optional.ofNullable(sections.get(coordinates));
    }

    /**
     * Determine if the building occupies given coordinates. Multi-hex buildings
     * will occupy multiple coordinates. Only one building per hex.
     *
     * @param coords
     *            - the <code>Coords</code> being examined.
     * @return <code>true</code> if the building occupies the coordinates.
     *         <code>false</code> otherwise.
     */
    public boolean isIn(Coords coords) {
        return sectionAt(coords).isPresent();
    }

    /**
     * @deprecated use {@link #isIn(Coords)} instead
     */
    @Deprecated public boolean hasCFIn(Coords coords) {
        // This method baffles me... the original implementation was
        //    return currentCF.containsKey(coords);
        // but I see no way how this is different from isIn(coords),
        // whose implementation was
        //    return coordinates.contains(coords);
        return isIn(coords);
    }

    /** @deprecated use {@link #iterateCoords()} instead */
    @Deprecated public Enumeration<Coords> getCoords() {
        return Collections.enumeration(sections.keySet());
    }

    public Iterator<Coords> iterateCoords() {
        return Collections.unmodifiableCollection(sections.keySet()).iterator();
    }

    /**
     * @return the structure type of this building
     *         ({@linkplain Terrains#BUILDING},
     *          {@linkplain Terrains#FUEL_TANK} or
     *          {@linkplain Terrains#BRIDGE})
     */
    public int getStructureType() {
        return structureType;
    }

    public ConstructionType getConstructionType() {
        return constructionType;
    } 

    /** @deprecated use {@link #getConstructionType()} instead */
    @Deprecated public int getType() { return constructionType.getId(); }

    public Optional<BuildingClass> getBuildingClass() {
        return Optional.ofNullable(buildingClass);
    } 

    /** @deprecated use {@link #getBuildingClass()} instead */
    @Deprecated public int getBldgClass() { return getBuildingClass().map(BuildingClass::getId).orElse(ITerrain.LEVEL_NONE); }

    /**
     * returns the magnitude of the explosion this building does when destroyed
     */
    public OptionalInt getExplosionMagnitude() {
        // This is currently an OptionalInt, in order to distinguish
        // fuel tanks from other buildings (reminiscent of how FuelTank
        // was a subclass of Building); there seem to be, however, no hard
        // reason why one couldn't say that all buildings can explode and those
        // that don't have an explosion magnitude of zero.
        // LATER see if using a regular integer is ok (ie: that magnitude=0 isn't use as a magic value somewhere)
        return explosionMagnitude != null ? OptionalInt.of(explosionMagnitude) : OptionalInt.empty();
    }

    /** @deprecated use {@link #getExplosionMagnitude()} instead */
    @Deprecated public int getMagnitude() {
        return getExplosionMagnitude().getAsInt();
    }

    public boolean getBasementCollapsed(Coords coords) {
        return sectionAt(coords).get().isBasementCollapsed();
    }

    /** @deprecated use {@link BuildingServerHelper#collapseBasement(Building, Coords, IBoard, List)} instead */
    @Deprecated public void collapseBasement(Coords coords, IBoard board, List<Report> vPhaseReport) {
        BuildingServerHelper.collapseBasement(this, coords, board, vPhaseReport);
    }

    /** @deprecated use {@link BuildingServerHelper#rollBasement(Building, Coords, IBoard, List)} instead */
    @Deprecated public boolean rollBasement(Coords coords, IBoard board, List<Report> vPhaseReport) {
        return BuildingServerHelper.rollBasement(this, coords, board, vPhaseReport);
    }

    /**
     * Get the current construction factor of the building hex at the passed
     * coords. Any damage immediately updates this value.
     *
     * @param coords
     *            - the <code>Coords> of the hex in question
     *
     * @return the <code>int</code> value of the building hex's current
     *         construction factor. This value will be greater than or equal to
     *         zero.
     */
    public int getCurrentCF(Coords coords) {
        return sectionAt(coords).get().getCurrentCF();
    }

    /**
     * Get the construction factor of the building hex at the passed coords at
     * the start of the current phase. Damage that is received during the phase
     * is applied at the end of the phase.
     *
     * @param coords
     *            - the <code>Coords> of the hex in question
     * @return the <code>int</code> value of the building's construction factor
     *         at the start of this phase. This value will be greater than or
     *         equal to zero.
     */
    public int getPhaseCF(Coords coords) {
        return sectionAt(coords).get().getPhaseCF();
    }

    public int getArmor(Coords coords) {
        return sectionAt(coords).get().getArmor();
    }

    /**
     * Set the current construction factor of the building hex. Call this method
     * immediately when the building sustains any damage.
     *
     * @param coords
     *        the <code>Coords> of the hex in question
     * @param cf
     *        the <code>int</code> value of the building hex's current
     *        construction factor. This value must be greater than or equal
     *        to zero.
     * @throws IllegalArgumentException
     *         if the passed value is less than zero, an
     *          <code>IllegalArgumentException</code> is thrown.
     */
    public void setCurrentCF(int cf, Coords coords) {
        if (cf < 0) {
            throw new IllegalArgumentException("Invalid CF value: " + cf); //$NON-NLS-1$
        }
        sectionAt(coords).get().setCurrentCF(cf);
    }

    /**
     * Set the construction factor of the building hex for the start of the next
     * phase. Call this method at the end of the phase to apply damage sustained
     * by the building during the phase.
     *
     * @param coords
     *        the <code>Coords> of the hex in question
     * @param cf
     *        the <code>int</code> value of the building's current
     *        construction factor. This value must be greater than or equal
     *        to zero.
     *
     * @throws IllegalArgumentException
     *         if the passed value is less than zero, an
     *         <code>IllegalArgumentException</code> is thrown.
     */
    public void setPhaseCF(int cf, Coords coords) {
        if (cf < 0) {
            throw new IllegalArgumentException("Invalid CF value: " + cf); //$NON-NLS-1$
        }
        sectionAt(coords).get().setPhaseCF(cf);
    }

    public void setArmor(int a, Coords coords) {
        if (a < 0) {
            throw new IllegalArgumentException("Invalid armor value: " + a); //$NON-NLS-1$
        }
        sectionAt(coords).get().setArmor(a);
    }

    public String getName() {
        StringBuffer buffer = new StringBuffer();
        if (structureType == Terrains.FUEL_TANK) {
            buffer.append("Fuel Tank #");
        } else if (getType() == Building.WALL) {
            buffer.append("Wall #");
        } else if (structureType == Terrains.BUILDING) {
            buffer.append("Building #");
        } else if (structureType == Terrains.BRIDGE) {
            buffer.append("Bridge #");
        } else {
            buffer.append("Structure #");
        }
        buffer.append(id);
        return buffer.toString();
    }

    /**
     * Determine if this building is on fire.
     *
     * @return <code>true</code> if the building is on fire.
     */
    public boolean isBurning(Coords coords) {
        return sectionAt(coords).get().isBurning();
    }

    /**
     * Set the flag that indicates that this building is on fire.
     *
     * @param onFire
     *        a <code>boolean</code> value that indicates whether this
     *        building is on fire.
     */
    public void setBurning(boolean onFire, Coords coords) {
        sectionAt(coords).get().setBurning(onFire);
    }

    public void addDemolitionCharge(int playerId, int damage, Coords pos) {
        DemolitionCharge charge = new DemolitionCharge(playerId, damage, pos);
        demolitionCharges.add(charge);
    }

    public void removeDemolitionCharge(DemolitionCharge charge) {
        demolitionCharges.remove(charge);
    }

    public List<DemolitionCharge> getDemolitionCharges() {
        return demolitionCharges;
    }

    public void setDemolitionCharges(List<DemolitionCharge> charges) {
        demolitionCharges = charges;
    }

    /**
     * Remove one building hex from the building
     *
     * @param coords
     *        the <code>Coords</code> of the hex to be removed
     */
    public void removeHex(Coords coords) {
        sections.remove(coords);
    }

    public int getOriginalHexCount() {
        return originalHexes;
    }

    public int getCollapsedHexCount() {
        return originalHexes - sections.size();
    }

    /**
     * @return the damage scale multiplier for units passing through this
     *         building
     *
     * @deprecated use {@link #getBldgClass()} and {@link BuildingClass#getDamageFromScaleMultiplier()} instead
     */
    @Deprecated public double getDamageFromScale() {
        return getBuildingClass().map(BuildingClass::getDamageFromScaleMultiplier)
                                 .orElse(1.0);
    }

    /**
     * @return the damage scale multiplier for damage applied to this building
     *         (and occupants)
     *
     * @deprecated use {@link #getBldgClass()} and {@link BuildingClass#getDamageToScaleMultiplier()} instead
     */
    @Deprecated public double getDamageToScale() {
        return getBuildingClass().map(BuildingClass::getDamageToScaleMultiplier)
                .orElse(1.0);
    }

    /**
     * @return the amount of damage the building absorbs
     */
    public int getAbsorbtion(Coords pos) {
        return sectionAt(pos).get().getAbsorbtion();
    }

    /**
     * Returns the percentage of damage done to the building for attacks against
     * infantry in the building from other units within the building.  TW pg175.
     *
     * @deprecated use {@link #getConstructionType()} and {@link ConstructionType#getDamageReductionFromInside()} instead
     */
    @Deprecated public double getInfDmgFromInside() {
        return constructionType.getDamageReductionFromInside();
    }

    /**
     * Per page 172 of Total Warfare, this is the fraction of a weapon's damage that
     * passes through to infantry inside the building.
     * @return Damage fraction.
     * 
     * @deprecated use {@link #getConstructionType()} and {@link ConstructionType#getDamageReductionFromOutside()} instead
     */
    @Deprecated public float getDamageReductionFromOutside() {
        return constructionType.getDamageReductionFromOutside();
    }

    public BasementType getBasement(Coords coords) {
        return sectionAt(coords).get().getBasementType();
    }

    public void setBasement(Coords coords, BasementType basement) {
        sectionAt(coords).get().setBasementType(basement);
    }

    public void setBasementCollapsed(Coords coords, boolean collapsed) {
        sectionAt(coords).get().setBasementCollapsed(collapsed);
    }

    // LATER fix equals/hashCode
    //
    // Basing equality on id equality does not make sense on a mutable class.
    // This will need to be addressed, but to do so one must check all places
    // where equality is used (eg: calls to equals() and use in collections).
    //
    // Also note the comment "True until we're talking about more than one
    // Board per Game" below, which seems to imply that building ids are not
    // necessarily unique in multi-board setups.

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        // True until we're talking about more than one Board per Game.
        final Building other = (Building) obj;
        return (id == other.id);
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        
        // Not sure of what impact (if any) changing this will have, but
        // it would be nice if it included at least some sort of human-readable
        // information to identify the building (eg: the hexes it covers?)
        // LATER investigate and -if possible- edit
        
        StringBuffer buf = new StringBuffer();

        switch (constructionType) {
            case LIGHT:    buf.append("Light ");    break;
            case MEDIUM:   buf.append("Medium ");   break;
            case HEAVY:    buf.append("Heavy ");    break;
            case HARDENED: buf.append("Hardened "); break;
            case WALL:     // fall-through
            default:       // do nothing
        }

        getBuildingClass().ifPresent(bc -> {
            switch (bc) {
            case HANGAR:          buf.append("Hangar "); break;
            case FORTRESS:        buf.append("Fortress "); break;
            case GUN_EMPLACEMENT: buf.append("Gun Emplacement"); break;
            case STANDARD:        // fall-through
            default:              buf.append("Standard ");
            }
        });

        buf.append(getName());

        return buf.toString();
    }

}
