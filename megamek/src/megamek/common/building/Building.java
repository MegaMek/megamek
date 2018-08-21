/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *                     (C) 2018 The MegaMek Team
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static final long serialVersionUID = 1L;

    Building( int id,
              int structureType,
              ConstructionType constructionType,
              BuildingClass buildingClass,
              OptionalInt explosionMagnitude,
              Map<Coords, BuildingSection> sections,
              int originalHexes ) {
        this.id = id;
        this.structureType = structureType;
        this.constructionType = constructionType;
        this.buildingClass = buildingClass;
        this.explosionMagnitude = explosionMagnitude.isPresent()
                                ? explosionMagnitude.getAsInt()
                                : null;
        this.sections = sections;
        this.originalHexes = originalHexes;
    }

    private final int id;
    private final int structureType;
    private final ConstructionType constructionType;
    private final BuildingClass buildingClass; // nullable - can't use Optional    as it's not Serializable
    private final Integer explosionMagnitude;  // nullable - can't use OptionalInt as it's not Serializable

    private final Map<Coords, BuildingSection> sections;
    private final int originalHexes;

    /**
     * @return an {@linkplain Stream} of this building's sections
     */
    public Stream<BuildingSection> streamSections() {
        return sections.values().stream();
    }

    /**
     * @return the section at the given coordinates, if present
     */
    public Optional<BuildingSection> sectionAt(Coords coordinates) {
        return Optional.ofNullable(sections.get(coordinates));
    }

    /**
     * @return the section at the given coordinates
     * 
     * @throws IllegalArgumentException if none is present
     */
    public BuildingSection requireSectionAt(Coords coordinates) {
        return sectionAt(coordinates).orElseThrow( () -> new IllegalArgumentException(String.format("Building %s has no section at %s" )) ); //$NON-NLS-1$
    }

    /**
     * Removes the section at the given coordinates.
     * 
     * @return whether this building's sections have changed as a result of
     *         this call
     */
    public boolean removeSectionAt(Coords coords) {
        return sections.remove(coords) != null;
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

    /**
     * @return the construction type of this building
     */
    public ConstructionType getConstructionType() {
        return constructionType;
    }

    /**
     * @return the building class of this building, if any
     */
    public Optional<BuildingClass> getBuildingClass() {
        return Optional.ofNullable(buildingClass);
    }

    /**
     * @returns the magnitude of the explosion this building causes when destroyed, if any
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

    /**
     * @return the name of this building
     */
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
        buffer.append(id); // LATER a better name that "Building #1231312" would be desirable
        return buffer.toString();
    }

    /**
     * @return the number of hexes this building originally spanned
     */
    public int getOriginalHexCount() {
        return originalHexes;
    }

    /**
     * @return the number of hexes this building currently spans
     */
    public int getCollapsedHexCount() {
        return originalHexes - sections.size();
    }

    // LATER fix equals/hashCode
    //
    // Basing equality on id equality does not make sense on a mutable class.
    // This will need to be addressed, but to do so one must check all places
    // where equality is used (eg: calls to equals() and use in collections).

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
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

    //=== here be deprecated stuff==============================================

    /** @deprecated use {@link ConstructionType#LIGHT}    instead */ @Deprecated public static final int LIGHT = 1;
    /** @deprecated use {@link ConstructionType#MEDIUM}   instead */ @Deprecated public static final int MEDIUM = 2;
    /** @deprecated use {@link ConstructionType#HEAVY}    instead */ @Deprecated public static final int HEAVY = 3;
    /** @deprecated use {@link ConstructionType#HARDENED} instead */ @Deprecated public static final int HARDENED = 4;
    /** @deprecated use {@link ConstructionType#WALL}     instead */ @Deprecated public static final int WALL = 5;

    /** @deprecated use {@link BuildingClass#STANDARD}        instead */ @Deprecated public static final int STANDARD = 0;
    /** @deprecated use {@link BuildingClass#HANGAR}          instead */ @Deprecated public static final int HANGAR = 1;
    /** @deprecated use {@link BuildingClass#FORTRESS}        instead */ @Deprecated public static final int FORTRESS = 2;
    /** @deprecated use {@link BuildingClass#GUN_EMPLACEMENT} instead */ @Deprecated public static final int GUN_EMPLACEMENT = 3;

    /** @deprecated use {@code sectionAt(coords).isPresent() } instead */
    @Deprecated public boolean isIn(Coords coords) {
        return sectionAt(coords).isPresent();
    }

    /** @deprecated use {@code sectionAt(coords).isPresent() } instead */
    @Deprecated public boolean hasCFIn(Coords coords) {
        return sectionAt(coords).isPresent();
    }

    /** @deprecated use {@link #iterateCoords()} instead */
    @Deprecated public Enumeration<Coords> getCoords() {
        return Collections.enumeration(sections.keySet());
    }

    /** @deprecated use {@link #removeSectionAt(Coords)} instead */
    @Deprecated public void removeHex(Coords coords) {
        removeSectionAt(coords);
    }

    /** @deprecated use {@link #getConstructionType()} instead */
    @Deprecated public int getType() {
        return constructionType.getId();
    }

    /** @deprecated use {@link #getBuildingClass()} instead */
    @Deprecated public int getBldgClass() {
        return getBuildingClass().map(BuildingClass::getId).orElse(ITerrain.LEVEL_NONE);
    }

    /** @deprecated use {@link #sectionAt(Coords)} and {@link BuildingSection#isBasementCollapsed()} instead */
    @Deprecated public boolean getBasementCollapsed(Coords coords) {
        return sectionAt(coords).get().isBasementCollapsed();
    }

    /** @deprecated use {@link BuildingServerHelper#collapseBasement(Building, Coords, IBoard, List)} instead */
    @SuppressWarnings("deprecation") @Deprecated public void collapseBasement(Coords coords, IBoard board, List<Report> vPhaseReport) {
        BuildingServerHelper.collapseBasement(this, coords, board, vPhaseReport);
    }

    /** @deprecated use {@link BuildingServerHelper#rollBasement(Building, Coords, IBoard, List)} instead */
    @SuppressWarnings("deprecation") @Deprecated public boolean rollBasement(Coords coords, IBoard board, List<Report> vPhaseReport) {
        return BuildingServerHelper.rollBasement(this, coords, board, vPhaseReport);
    }

    /** @deprecated use {@link #sectionAt(Coords)} and {@link BuildingSection#getCurrentCF()} instead */
    @Deprecated public int getCurrentCF(Coords coords) {
        return sectionAt(coords).get().getCurrentCF();
    }

    /** @deprecated use {@link #sectionAt(Coords)} and {@link BuildingSection#setCurrentCF(int)} instead */
    @Deprecated public void setCurrentCF(int cf, Coords coords) {
        sectionAt(coords).get().setCurrentCF(cf);
    }

    /** @deprecated use {@link #sectionAt(Coords)} and {@link BuildingSection#getPhaseCF()} instead */
    @Deprecated public int getPhaseCF(Coords coords) {
        return sectionAt(coords).get().getPhaseCF();
    }

    /** @deprecated use {@link #sectionAt(Coords)} and {@link BuildingSection#setPhaseCF(int)} instead */
    @Deprecated public void setPhaseCF(int cf, Coords coords) {
        sectionAt(coords).get().setPhaseCF(cf);
    }

    /** @deprecated use {@link #sectionAt(Coords)} and {@link BuildingSection#getArmor()} instead */
    @Deprecated public int getArmor(Coords coords) {
        return sectionAt(coords).get().getArmor();
    }

    /** @deprecated use {@link #sectionAt(Coords)} and {@link BuildingSection#setArmor(int)} instead */
    @Deprecated public void setArmor(int armor, Coords coords) {
        sectionAt(coords).get().setArmor(armor);
    }

    /** @deprecated use {@link #sectionAt(Coords)} and {@link BuildingSection#isBurning()} instead */
    @Deprecated public boolean isBurning(Coords coords) {
        return sectionAt(coords).get().isBurning();
    }

    /** @deprecated use {@link #sectionAt(Coords)} and {@link BuildingSection#setBurning(boolean)} instead */
    @Deprecated public void setBurning(boolean onFire, Coords coords) {
        sectionAt(coords).get().setBurning(onFire);
    }

    /** @deprecated use {@link #getBldgClass()} and {@link BuildingClass#getDamageFromScaleMultiplier()} instead */
    @Deprecated public double getDamageFromScale() {
        return getBuildingClass().map(BuildingClass::getDamageFromScaleMultiplier)
                                 .orElse(1.0);
    }

    /** @deprecated use {@link #getBldgClass()} and {@link BuildingClass#getDamageToScaleMultiplier()} instead */
    @Deprecated public double getDamageToScale() {
        return getBuildingClass().map(BuildingClass::getDamageToScaleMultiplier)
                .orElse(1.0);
    }

    /** @deprecated use {@link #sectionAt(Coords)} and {@link BuildingSection#getAbsorbtion()} instead */
    @Deprecated public int getAbsorbtion(Coords pos) {
        return sectionAt(pos).get().getAbsorbtion();
    }

    /** @deprecated use {@link #getConstructionType()} and {@link ConstructionType#getDamageReductionFromInside()} instead */
    @Deprecated public double getInfDmgFromInside() {
        return constructionType.getDamageReductionFromInside();
    }

    /** @deprecated use {@link #getConstructionType()} and {@link ConstructionType#getDamageReductionFromOutside()} instead */
    @Deprecated public float getDamageReductionFromOutside() {
        return constructionType.getDamageReductionFromOutside();
    }

    /** @deprecated use {@link #sectionAt(Coords)} and {@link BuildingSection#getBasementType()} instead */
    @Deprecated public BasementType getBasement(Coords coords) {
        return sectionAt(coords).get().getBasementType();
    }

    /** @deprecated use {@link #sectionAt(Coords)} and {@link BuildingSection#setBasementType(BasementType)} instead */
    @Deprecated public void setBasement(Coords coords, BasementType basement) {
        sectionAt(coords).get().setBasementType(basement);
    }

    /** @deprecated use {@link #sectionAt(Coords)} and {@link BuildingSection#setBasementCollapsed(boolean)} instead */
    @Deprecated public void setBasementCollapsed(Coords coords, boolean collapsed) {
        sectionAt(coords).get().setBasementCollapsed(collapsed);
    }

    /** @deprecated use {@link ConstructionType} instead */
    @Deprecated public static int getDefaultCF(int type) {
        return ConstructionType.ofId(type).map(ConstructionType::getDefaultCF).orElse(-1);
    }

    /** @deprecated use {@code streamSections().flatMap(BuildingSection::streamDemolitionCharges)} instead */
    @Deprecated public List<DemolitionCharge> getDemolitionCharges() {
        return streamSections().flatMap(BuildingSection::streamDemolitionCharges)
                               .collect(Collectors.toList());
    }

    /** @deprecated with no direct replacement (see implementation) */
    @Deprecated public void setDemolitionCharges(List<DemolitionCharge> charges) {
        sections.values().forEach(section -> {
            section.setDemolitionCharges(Collections.emptyList());
        });
        charges.stream().collect(Collectors.groupingBy(DemolitionCharge::getPos))
                        .forEach( (pos, poscharges) -> requireSectionAt(pos).setDemolitionCharges(poscharges) );
    }

    /** @deprecated use {@code requireSectionAt(pos).addDemolitionCharge(playerId, damage)} instead */
    @Deprecated public void addDemolitionCharge(int playerId, int damage, Coords pos) {
        requireSectionAt(pos).addDemolitionCharge(playerId, damage);
    }

    /** @deprecated with no direct replacement (see implementation) */
    @Deprecated public boolean removeDemolitionCharge(DemolitionCharge charge) {
        for (BuildingSection section : sections.values()) {
            if (section.removeDemolitionCharge(charge)) {
                return true;
            }
        }
        return false;
    }

}
