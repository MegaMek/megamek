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

package megamek.common;

import java.util.Vector;
import java.util.Enumeration;
import java.io.*;

/**
 * This class provides static methods to save a list of <code>Entity</code>s
 * to, and load a list of <code>Entity</code>s from a file.
 */
public class EntityListFile {

    /**
     * Save the <code>Entity</code>s in the list to the given file.
     * <p/>
     * The <code>Entity</code>s' pilots, damage, ammo loads, ammo usage, and
     * other campaign-related information are retained but data specific to
     * a particular game is ignored.
     *
     * @param   fileName - the <code>String</code> name of the file.  The
     *          contents of the file will be replaced with the list's
     *          <code>Entity</code>s.
     * @param   list - a <code>Vector</code> containing <code>Entity</code>s
     *          to be stored in a file.
     * @exception <code>IOException</code> is thrown on any error.
     */
    public static void saveTo( String fileName, Vector list )
        throws IOException {

        // Open up the file.
        ObjectOutputStream listStream = new ObjectOutputStream
            ( new FileOutputStream(fileName) );

        // FOR NOW write the list to the file.
        listStream.writeObject( list );
        listStream.flush();
        listStream.close();
    }

    /**
     * Load a list of <code>Entity</code>s from the given file.
     * <p/>
     * The <code>Entity</code>s' pilots, damage, ammo loads, ammo usage, and
     * other campaign-related information are retained but data specific to
     * a particular game is ignored.
     *
     * @param   fileName - the <code>String</code> name of the file.
     * @return  A <code>Vector</code> containing <code>Entity</code>s
     *          loaded from the file.
     * @exception <code>IOException</code> is thrown on any error.
     */
    public static Vector loadFrom( String fileName ) throws IOException {
        Vector output = new Vector();
        Vector temp = null;

        // Open up the file.
        ObjectInputStream listStream = new ObjectInputStream
            ( new FileInputStream(fileName) );

        // FOR NOW, read a Vector from the file.
        try {
            temp = (Vector) listStream.readObject();
            listStream.close();
        }
        catch ( ClassNotFoundException excep ) {
            excep.printStackTrace( System.err );
            throw new IOException( "Unable to read from: " + fileName );
        }

        // Create new Entity objects to match the ones loaded from the file.
        for ( Enumeration iter = temp.elements(); iter.hasMoreElements(); ) {

            // Try to get a copy of the entity.
            final Entity entity = (Entity) iter.nextElement();
            String name = entity.getShortName();
            MechSummary ms = MechSummaryCache.getInstance().getMech( name );
            try {
                final Entity newEnt = new MechFileParser
                    (ms.getSourceFile(), ms.getEntryName()).getEntity();

                // Copy the damage and crew to the new Entity.
                newEnt.setCrew( entity.getCrew() );
                for ( int loc = 0; loc < newEnt.locations(); loc++ ) {

                    // Sanity check the armor values before setting them.
                    boolean locDestroyed = false;
                    if ( newEnt.getOArmor(loc) < entity.getArmor(loc) ) {
                        throw new IOException
                            ( "Invalid armor value: " + 
                              entity.getArmor(loc) + " in " + 
                              newEnt.getLocationAbbr(loc) +
                              " of " + entity.getShortName() +
                              " for " + newEnt.getShortName() );
                    }
                    newEnt.setArmor( entity.getArmor(loc), loc );
                    if ( Entity.ARMOR_DOOMED == newEnt.getArmor(loc) ) {
                        newEnt.setArmor( Entity.ARMOR_DESTROYED, loc );
                    }
                    if ( newEnt.getOInternal(loc) < entity.getInternal(loc) ) {
                        throw new IOException
                            ( "Invalid internal structure value: " + 
                              entity.getInternal(loc) + " in " + 
                              newEnt.getLocationAbbr(loc) +
                              " of " + entity.getShortName() +
                              " for " + newEnt.getShortName() );
                    }
                    newEnt.setInternal( entity.getInternal(loc), loc );
                    if ( Entity.ARMOR_DOOMED == newEnt.getInternal(loc) ) {
                        newEnt.setInternal( Entity.ARMOR_DESTROYED, loc );
                    }
                    if ( Entity.ARMOR_DESTROYED == newEnt.getInternal(loc) ) {
                        locDestroyed = true;
                    }
                    if ( newEnt.hasRearArmor(loc) ) {
                        if ( newEnt.getOArmor(loc) < entity.getArmor(loc) ) {
                            throw new IOException
                                ( "Invalid rear armor value: " + 
                                  entity.getArmor(loc, true) + " in " + 
                                  newEnt.getLocationAbbr(loc) +
                                  " of " + entity.getShortName() +
                                  " for " + newEnt.getShortName() );
                        }
                        newEnt.setArmor(entity.getArmor(loc, true), loc, true);
                        if ( Entity.ARMOR_DOOMED == 
                             newEnt.getArmor(loc, true) ) {
                            newEnt.setArmor(Entity.ARMOR_DESTROYED, loc, true);
                        }
                    }

                    // Walk through all slots in this location.
                    for ( int slot = 0;
                          slot < newEnt.getNumberOfCriticals(loc);
                          slot++ ) {
                        final CriticalSlot crit =
                            entity.getCritical( loc, slot );
                        final CriticalSlot newCrit =
                            newEnt.getCritical( loc, slot );

                        // Do we have a critical at this slot?
                        if ( null != newCrit ) {

                            // We should have a critical slot.
                            if ( null == crit ) {
                                throw new IOException
                                    ( "Could not find slot #" + slot + " in " +
                                      newEnt.getLocationAbbr(loc) +
                                      " of " + entity.getShortName() +
                                      " for " + newEnt.getShortName() );
                            }

                            // Destroy it if necessary
                            newCrit.setDestroyed( locDestroyed ||
                                                  crit.isDestroyed() ||
                                                  crit.isHit() );

                            // Is this slot some equipment?
                            if ( newCrit.getType() ==
                                 CriticalSlot.TYPE_EQUIPMENT ) {
                                final Mounted mounted = entity.getEquipment
                                    ( crit.getIndex() );
                                final Mounted newMount = newEnt.getEquipment
                                    ( newCrit.getIndex() );

                                // Reset transient values.
                                mounted.restore();

                                // Is this an ammo slot?
                                if ( newMount.getType() instanceof AmmoType ) {

                                    // We should have an ammo slot.
                                    if ( !(mounted.getType() 
                                           instanceof AmmoType) ) {
                                        throw new IOException
                                            ( "Looking for " +
                                              newMount.getType().getName() +
                                              " in " +
                                              newEnt.getLocationAbbr(loc) +
                                              " for " + newEnt.getShortName() +
                                              " but found " +
                                              mounted.getType().getName() +
                                              " in " + entity.getShortName());
                                    }

                                    // Match the saved ammo type and shots.
                                    newMount.changeAmmoType
                                        ( (AmmoType) mounted.getType() );
                                    newMount.setShotsLeft
                                        ( mounted.getShotsLeft() );
                                }

                                // Destroy it if necessary.
                                newMount.setDestroyed( locDestroyed || 
                                                       mounted.isDestroyed() ||
                                                       mounted.isHit() );
                            }
                        }
                            
                    } // Handle the next critical slot

                } // Handle the next location

                // Add the copy to the output vector.
                output.addElement( newEnt );
            } catch (EntityLoadingException excep) {
                excep.printStackTrace( System.err );
                throw new IOException( "Unable to load mech: " +
                                       ms.getSourceFile() + ": " +
                                       ms.getEntryName());
            }

        } // Handle the next entity in the list

        // Return the new list.
        return output;
    }

}
