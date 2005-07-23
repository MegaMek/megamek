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

import java.util.*;
import java.io.Serializable;

/**
 * ArtilleryTracker--one held by every entity, it holds a list of the 
 * artillery weapons an entity controls, and the mods they get to 
 * hit certain hexes.
 */
public class ArtilleryTracker implements Serializable {
    
    /**
     * Maps WeaponID's of artillery weapons to a Vector of ArtilleryModifiers, 
     * for all the different coords it's got mods to.
     */
    private Hashtable weapons;

    /**
     * Creates new instance of the tracker
     *
     */
    public ArtilleryTracker() {
        weapons=new Hashtable();
    }

    /**
     * Adds new weapon
     * @param mounted new weapon
     */
    public void addWeapon(Mounted mounted) {
        weapons.put(mounted, new Vector());
    }

    /**
     * Sets the modifier for the given weapon
     * @param weapon weapon to set modifier for
     * @param modifier
     * @param coords
     */
    public void setModifier(Mounted weapon, int modifier, Coords coords) {
        Vector weaponMods = getWeaponModifiers(weapon);
        ArtilleryModifier am = getModifierByCoords(weaponMods, coords);
        if(am != null) {
            am.setModifier(modifier);
        } else {
            am = new ArtilleryModifier(coords, modifier);
            weaponMods.addElement(am);
        }
    }

    /**
     * Returns the modifier for the given weapon
     * @param weapon weapon to get modifier for
     * @param coords
     * @return
     */
    public int getModifier(Mounted weapon, Coords coords) {
        Vector weaponMods = getWeaponModifiers(weapon);
        ArtilleryModifier am = getModifierByCoords(weaponMods, coords);
        if(am != null) {
            return am.getModifier();
        } else {
            am = new ArtilleryModifier(coords,0);
            weaponMods.addElement(am);
            return 0;
        }
    }

    /**
     * Returns the <code>Vector</code> of the modifiers for the given weapon 
     * @param mounted weapon to get modifiers for
     * @return the <code>Vector</code> of the modifiers for the given weapon
     */
    protected Vector getWeaponModifiers(Mounted mounted) {
        Vector result = (Vector)weapons.get(mounted);
        if(result ==null ) {
            result = new Vector();
            weapons.put(mounted, result);            
        }
        return result;
    }

    /**
     * Search the given vector of modifires for the modifier which coords equals
     * to the given coords 
     * @param modifiers <code>Vector</code> of the modifiers to process
     * @param coords coordinates of the modifire looked for
     * @return modifier with coords equals to the given on <code>null</code> if not found
     */
    protected ArtilleryModifier getModifierByCoords(Vector modifiers, Coords coords) { 
        for(Enumeration i=modifiers.elements();i.hasMoreElements();) {
            ArtilleryModifier mod=(ArtilleryModifier)i.nextElement();
            if(mod.getCoords().equals(coords)) {
                return mod;
            }
        }
        return null;
    }

    /**
     * Small collector...just holds a Coords and a modifier 
     * (either ToHitData.AUTOMATIC_SUCCESS or just a modifier.
     */
    private static class ArtilleryModifier implements Serializable {

        private Coords coords;
        private int modifier;

        public ArtilleryModifier() {
            coords=new Coords();
            setModifier(0);
        }

        public ArtilleryModifier(Coords coords,int modifier) {
            this.coords=coords;
            this.setModifier(modifier);
        }

        /**
         * @param coords The coords to set.
         */
        public void setCoords(Coords coords) {
            this.coords = coords;
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
}
