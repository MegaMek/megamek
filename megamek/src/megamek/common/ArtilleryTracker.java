/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
package megamek.common;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Vector;

/**
 * ArtilleryTracker--one held by every entity, it holds a list of the artillery
 * weapons an entity controls, and the mods they get to hit certain hexes.
 */
public class ArtilleryTracker implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -6913144265531983734L;
    /**
     * Maps WeaponID's of artillery weapons to a Vector of ArtilleryModifiers,
     * for all the different coords it's got mods to.
     */
    private Hashtable<Mounted, Vector<ArtilleryModifier>> weapons;
    
    private boolean spotterIsForwardObs;

    /**
     * Creates new instance of the tracker
     */
    public ArtilleryTracker() {
        weapons = new Hashtable<Mounted, Vector<ArtilleryModifier>>();
    }

    /**
     * Adds new weapon
     *
     * @param mounted new weapon
     */
    public void addWeapon(Mounted mounted) {
        weapons.put(mounted, new Vector<ArtilleryModifier>());
    }

    /**
     * Sets the modifier for artillery weapons on this unit. All weapons use the
     * same modifier due to artillery fire adjustment being handled on a per-unit
     * basis.
     *
     * @param modifier
     * @param coords
     */
    public void setModifier(int modifier, Coords coords) {
        for (Mounted weapon : weapons.keySet()) {
            Vector<ArtilleryModifier> weaponMods = getWeaponModifiers(weapon);
            ArtilleryModifier am = getModifierByCoords(weaponMods, coords);
            if (am != null) {
                am.setModifier(modifier);
            } else {
                am = new ArtilleryModifier(coords, modifier);
                weaponMods.addElement(am);
            }
        }
    }

    /**
     * Returns the modifier for the given weapon
     *
     * @param weapon weapon to get modifier for
     * @param coords
     * @return
     */
    public int getModifier(Mounted weapon, Coords coords) {
        Vector<ArtilleryModifier> weaponMods = getWeaponModifiers(weapon);
        ArtilleryModifier am = getModifierByCoords(weaponMods, coords);
        if (am != null) {
            return am.getModifier();
        }
        return 0;
    }

    /**
     * Returns the <code>Vector</code> of the modifiers for the given weapon
     *
     * @param mounted weapon to get modifiers for
     * @return the <code>Vector</code> of the modifiers for the given weapon
     */
    public Vector<ArtilleryModifier> getWeaponModifiers(Mounted mounted) {
        Vector<ArtilleryModifier> result = weapons.get(mounted);
        if (result == null) {
            result = new Vector<ArtilleryModifier>();
            weapons.put(mounted, result);
        }
        return result;
    }

    /**
     * Search the given vector of modifires for the modifier which coords equals
     * to the given coords
     *
     * @param modifiers <code>Vector</code> of the modifiers to process
     * @param coords coordinates of the modifire looked for
     * @return modifier with coords equals to the given on <code>null</code>
     *         if not found
     */
    protected ArtilleryModifier getModifierByCoords(Vector<ArtilleryModifier> modifiers,
            Coords coords) {
        for (ArtilleryModifier mod : modifiers) {
            if (mod.getCoords().equals(coords)) {
                return mod;
            }
        }
        return null;
    }

    /**
     * Small collector...just holds a Coords and a modifier (either
     * ToHitData.AUTOMATIC_SUCCESS or just a modifier.
     */
    public static class ArtilleryModifier implements Serializable {

        /**
         *
         */
        private static final long serialVersionUID = 4913880091708068708L;
        private Coords coords;
        private int modifier;

        public ArtilleryModifier(Coords coords, int modifier) {
            this.coords = coords;
            this.setModifier(modifier);
        }

        /**
         * @return Returns the coords.
         */
        public Coords getCoords() {
            return coords;
        }

        /**
         * @param modifier The modifier to set.
         */
        public void setModifier(int modifier) {
            this.modifier = modifier;
        }

        /**
         * @return Returns the modifier.
         */
        public int getModifier() {
            return modifier;
        }

    }
    
    public boolean getSpotterHasForwardObs() {
        return spotterIsForwardObs;
    }
    
    public void setSpotterHasForwardObs(boolean forwardObserver) {
        spotterIsForwardObs = forwardObserver;
    }
}
