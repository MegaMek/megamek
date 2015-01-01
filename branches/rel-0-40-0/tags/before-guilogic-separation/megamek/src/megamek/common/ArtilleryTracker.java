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
 * ArtilleryTracker--one held by every entity, it holds a list of the artillery weapons an entity controls, and the mods they get to hit certain hexes.
 *
 */
public class ArtilleryTracker
implements Serializable
{
    Hashtable artyWeaps;//Maps WeaponID's of artillery weapons to a Vector of ArtilleryModifiers, for all the different coords it's got mods to.
    public ArtilleryTracker() {
        artyWeaps=new Hashtable();
    }
    public void addWeapon(Mounted mounted) {
        Vector weaponMods=new Vector();
        artyWeaps.put(mounted,weaponMods);
    }
    public void setModifier(Mounted mounted, int modifier,Coords coords) {
        Vector weaponMods=(Vector)artyWeaps.get(mounted);
        ArtilleryModifier correct=null;
        if(weaponMods!=null) {
            for(Enumeration i=weaponMods.elements();i.hasMoreElements();) {
                ArtilleryModifier mod=(ArtilleryModifier)i.nextElement();
                if(mod.coords.equals(coords))
                {
                    correct=mod;
                }

            }
            if(correct!=null) {
                correct.modifier=modifier;
            } else {
                correct=new ArtilleryModifier(coords,modifier);
                weaponMods.addElement(correct);
            }

        } else {
            weaponMods = new Vector();
            weaponMods.addElement(new ArtilleryModifier(coords,modifier));
            artyWeaps.put(mounted,weaponMods);
        }
    }
    public int getModifier(Mounted mounted,Coords coords) {
        Vector weaponMods=(Vector)artyWeaps.get(mounted);
        ArtilleryModifier correct=null;
        if(weaponMods!=null) {
            for(Enumeration i=weaponMods.elements();i.hasMoreElements();) {
                ArtilleryModifier mod=(ArtilleryModifier)i.nextElement();
                if(mod.coords.equals(coords))
                {
                    correct=mod;
                }

            }
            if(correct!=null) {
                return correct.modifier;
            } else {
                correct=new ArtilleryModifier(coords,0);
                weaponMods.addElement(correct);
                return 0;
            }

        } else {
            weaponMods = new Vector();
            weaponMods.addElement(new ArtilleryModifier(coords,0));
            artyWeaps.put(mounted,weaponMods);
            return 0;
        }

    }



}
/**
 * Small collector...just holds a Coords and a modifier (either ToHitData.AUTOMATIC_SUCCESS or just a modifier.
 */
class ArtilleryModifier
implements Serializable
{
    public Coords coords;
    public int modifier;
    public ArtilleryModifier() {
        coords=new Coords();
        modifier=0;
    }
    public ArtilleryModifier(Coords coords,int modifier) {
        this.coords=coords;
        this.modifier=modifier;
    }

}
