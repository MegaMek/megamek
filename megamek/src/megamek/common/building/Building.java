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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.IBoard;
import megamek.common.IHex;
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

    /**
     * Constructs a new building at the given coordinates, fetching info from the given board 
     */
    public static Building newBuildingAt(Coords coords, IBoard board) {
        IHex curHex = board.getHex(coords);
        requirePresent(curHex, Terrains.BUILDING);
        return new Building(coords, board, buildingIdFromCoordinates(coords), Terrains.BUILDING, OptionalInt.empty());
    }

    /**
     * Constructs a new bridge at the given coordinates, fetching info from the given board 
     */
    public static Building newBridgeAt(Coords coords, IBoard board) {
        IHex curHex = board.getHex(coords);
        requirePresent(curHex, Terrains.BRIDGE);
        return new Building(coords, board, buildingIdFromCoordinates(coords), Terrains.BRIDGE, OptionalInt.empty());
    }

    /**
     * Constructs a new fuel tank at the given coordinates, fetching info from the given board 
     */
    public static Building newFuelTankAt(Coords coords, IBoard board) {
        IHex curHex = board.getHex(coords);
        requirePresent(curHex, Terrains.FUEL_TANK);
        int magnitude = curHex.getTerrain(Terrains.FUEL_TANK_MAGN).getLevel();
        return new Building(coords, board, buildingIdFromCoordinates(coords), Terrains.FUEL_TANK, OptionalInt.of(magnitude));
    }

    /**
     * Construct a building for the given coordinates from the board's
     * information. If the building covers multiple hexes, every hex will be
     * included in the building.
     *
     * @param coords
     *        the <code>Coords</code> of a hex of the building. If the
     *        building covers multiple hexes, this constructor will include
     *        them all in this building automatically.
     * @param board
     *        the game's <code>Board</code> object.
     *
     * @throws IllegalArgumentException
     *        if the given coordinates do not contain a building, or if the
     *        building covers multiple hexes with different CFs.
     */
    protected Building(Coords coords, IBoard board, int id, int structureType, OptionalInt explosionMagnitude) {

        IHex initialHex = board.getHex(coords);

        this.id                 = id;
        this.structureType      = structureType;
        this.constructionType   = initialHex.getConstructionType(structureType).orElseThrow(IllegalArgumentException::new);
        this.buildingClass      = initialHex.getBuildingClass().orElse(null);
        this.explosionMagnitude = explosionMagnitude.isPresent() ? explosionMagnitude.getAsInt() : null;

        getSpannedHexes(initialHex, board, structureType).values().forEach(hex -> {
            sections.put(hex.getCoords(), BuildingSection.at(hex, structureType));
        });

        originalHexes = sections.size();
    }

    private final int id;
    private final int structureType;
    private final ConstructionType constructionType;
    private final BuildingClass buildingClass; // nullable - can't use Optional    as it's not Serializable
    private final Integer explosionMagnitude;  // nullable - can't use OptionalInt as it's not Serializable

    private int originalHexes = 0;

    private List<DemolitionCharge> demolitionCharges = new ArrayList<>();

    private final Map<Coords, BuildingSection> sections = new LinkedHashMap<>(); // not actually sure we need to preserve ordering

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

    public OptionalInt getExplosionMagnitude() {
        return explosionMagnitude != null ? OptionalInt.of(explosionMagnitude) : OptionalInt.empty();
    }

    /** @deprecated use {@link #getExplosionMagnitude()} instead */
    @Deprecated public int getMagnitude() {
        return getExplosionMagnitude().getAsInt();
    }

    public boolean getBasementCollapsed(Coords coords) {
        return sectionAt(coords).get().isBasementCollapsed();
    }

    public void collapseBasement(Coords coords, IBoard board, List<Report> vPhaseReport) {
        BuildingSection bs = sectionAt(coords).get();
        
        if (bs.getBasementType() == BasementType.NONE || bs.getBasementType() == BasementType.ONE_DEEP_NORMALINFONLY) {
            System.err.println("hex has no basement to collapse"); //$NON-NLS-1$
            return;
        }
        if (getBasementCollapsed(coords)) {
            System.err.println("hex has basement that already collapsed"); //$NON-NLS-1$
            return;
        }
        Report r = new Report(2112, Report.PUBLIC);
        r.add(getName());
        r.add(coords.getBoardNum());
        vPhaseReport.add(r);
        System.err.println("basement " + bs.getBasementType() + "is collapsing, hex:" //$NON-NLS-1$ //$NON-NLS-2$
                + coords.toString() + " set terrain!"); //$NON-NLS-1$
        board.getHex(coords).addTerrain(Terrains.getTerrainFactory().createTerrain(
                Terrains.BLDG_BASE_COLLAPSED, 1));
        setBasementCollapsed(coords, true);
    }

    /**
     * Roll what kind of basement this building has
     * @param coords the <code>Coords</code> of theb building to roll for
     * @param vPhaseReport the <code>Vector<Report></code> containing the phasereport
     * @return a <code>boolean</code> indicating wether the hex and building was changed or not
     */
    public boolean rollBasement(Coords coords, IBoard board, List<Report> vPhaseReport) {
        // XXX move out of here
        BuildingSection bs = sectionAt(coords).get();
        if (bs.getBasementType() != BasementType.UNKNOWN) return false;

        IHex hex = board.getHex(coords);
        Report r = new Report(2111, Report.PUBLIC);
        r.add(getName());
        r.add(coords.getBoardNum());

        int basementRoll = Compute.d6(2);
        r.add(basementRoll);

        BasementType newType = BasementType.basementsTable(basementRoll);
        bs.setBasementType(newType);
        hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.BLDG_BASEMENT_TYPE, newType.getId()));
        r.add(newType.getDesc());

        vPhaseReport.add(r);
        return true;
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

    public static Map<Coords,IHex> getSpannedHexes(IHex hex , IBoard board, int structureType) {

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
                    if (nextHex.containsTerrain(structureType)) {
                        getSpannedHexesRecurse(nextHex, board, structureType, receptacle);
                    }
                }
            }
        }

    }

    /** @deprecated this will be removed in a future refactoring */
    @Deprecated protected static int buildingIdFromCoordinates(Coords coordinates) {
        // FIXME This is an unlucky idea, especially considering that id is used
        //       as the only factor to check for equality and that (apparently?)
        //       coords can repeat in multi-map setups
        return coordinates.hashCode();
    }

    private static void requirePresent(IHex hex, int structureType) {
        if (!hex.containsTerrain(structureType)) {
            String msg = String.format("Structure type %s expected at %s", structureType, hex.getCoords().getBoardNum()); //$NON-NLS-1$
            throw new IllegalArgumentException(msg);
        }
    }

}
