/*
 * MegaMek - Copyright (C) 2003, 2004, 2005 Ben Mazur (bmazur@sev.org)
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
import gd.xml.ParseException;

/**
 * This class provides static methods to save a list of <code>Entity</code>s
 * to, and load a list of <code>Entity</code>s from a file.
 */
public class EntityListFile {

    /**
     * Produce a string describing this armor value.  Valid output values
     * are any integer from 0 to 100, N/A, or Destroyed.
     *
     * @param   points - the <code>int</code> value of the armor.  This
     *          value may be any valid value of entity armor (including
     *          NA, DOOMED, and DESTROYED).
     * @return  a <code>String</code> that matches the armor value.
     */
    private static String formatArmor( int points ) {
        // Is the armor destroyed or doomed?
        if ( points == IArmorState.ARMOR_DOOMED ||
             points == IArmorState.ARMOR_DESTROYED ) {
            return "Destroyed";
        }

        // Was there armor to begin with?
        if ( points == IArmorState.ARMOR_NA ) {
            return "N/A";
        }

        // Translate the int to a String.
        return String.valueOf( points );
    }

    /**
     * Produce a string describing the equipment in a critical slot.
     *
     * @param   index - the <code>String</code> index of the slot.  This
     *          value should be a positive integer or "N/A".
     * @param   mount - the <code>Mounted</code> object of the equipment.
     *          This value should be <code>null</code> for a slot with
     *          system equipment.
     * @param   isHit - a <code>boolean</code> that identifies this slot
     *          as having taken a hit.
     * @param   isDestroyed - a <code>boolean</code> that identifies the
     *          equipment as having been destroyed.  Note that a single
     *          slot in a multi-slot piece of equipment can be destroyed
     *          but not hit; it is still available to absorb additional
     *          critical hits.
     * @return  a <code>String</code> describing the slot.
     */
    private static String formatSlot( String index, Mounted mount,
                                      boolean isHit, boolean isDestroyed ) {
        StringBuffer output = new StringBuffer();

        output.append( "         <slot index=\"" );
        output.append( index );
        output.append( "\" type=\"" );
        if ( mount == null ) {
            output.append( "System" );
        } else {
            output.append( mount.getType().getInternalName() );
            if ( mount.isRearMounted() ) {
                output.append( "\" isRear=\"true" );
            }
            if ( mount.getType() instanceof AmmoType ) {
                output.append( "\" shots=\"" );
                output.append( String.valueOf
                                (mount.getShotsLeft()) );
            }
            if ( mount.getType() instanceof WeaponType &&
                 ((WeaponType)mount.getType()).hasFlag(WeaponType.F_ONESHOT)) {
                output.append( "\" munition=\"" );
                output.append( mount.getLinked().getType().getInternalName() );
            }
        }
        if ( isHit ) {
            output.append( "\" isHit=\"" );
            output.append( String.valueOf(isHit) );
        }
        output.append( "\" isDestroyed=\"" );
        output.append( String.valueOf(isDestroyed) );
        output.append( "\"/>" );
        output.append( CommonConstants.NL );

        // Return a String.
        return output.toString();
    }

    /**
     * Helper function that generates a string identifying the state of
     * the locations for an entity.
     *
     * @param   entity - the <code>Entity</code> whose location state is needed
     */
    private static String getLocString( Entity entity ) {
        boolean isMech = entity instanceof Mech;
        boolean haveSlot = false;
        StringBuffer output = new StringBuffer();
        StringBuffer thisLoc = new StringBuffer();
        boolean isDestroyed = false;
        boolean blownOff = false;

        // Walk through the locations for the entity,
        // and only record damage and ammo.
        for ( int loc = 0; loc < entity.locations(); loc++ ) {

            // Record destroyed locations.
            if ( entity.getOInternal(loc) != IArmorState.ARMOR_NA &&
                 entity.getInternal(loc) <= 0 ) {
                isDestroyed = true;
            }

            // Record damage to armor and internal structure.
            // Destroyed locations have lost all their armor and IS.
            if ( !isDestroyed ) {
                if ( entity.getOArmor(loc) != entity.getArmor(loc) ) {
                    thisLoc.append( "         <armor points=\"" );
                    thisLoc.append( formatArmor(entity.getArmor(loc)) );
                    thisLoc.append( "\"/>" );
                    thisLoc.append( CommonConstants.NL );
                }
                if ( entity.getOInternal(loc) != entity.getInternal(loc) ) {
                    thisLoc.append( "         <armor points=\"" );
                    thisLoc.append( formatArmor(entity.getInternal(loc)) );
                    thisLoc.append( "\" type=\"Internal\"/>" );
                    thisLoc.append( CommonConstants.NL );
                }
                if ( entity.hasRearArmor(loc) && entity.getOArmor(loc, true) !=
                     entity.getArmor(loc, true) ) {
                    thisLoc.append( "         <armor points=\"" );
                    thisLoc.append( formatArmor(entity.getArmor(loc, true)) );
                    thisLoc.append( "\" type=\"Rear\"/>" );
                    thisLoc.append( CommonConstants.NL );
                }
            }

            // Walk through the slots in this location.
            for ( int loop = 0; loop < entity.getNumberOfCriticals(loc);
                  loop++ ) {

                // Get this slot.
                CriticalSlot slot = entity.getCritical( loc, loop );

                // Did we get a slot?
                if ( null == slot ) {

                    // Nope.  Record missing actuators on Biped Mechs.
                    if ( isMech && !entity.entityIsQuad() &&
                         ( loc == Mech.LOC_RARM || loc == Mech.LOC_LARM ) &&
                         ( loop == 2 || loop == 3 ) ) {
                        thisLoc.append( "         <slot index=\"" );
                        thisLoc.append( String.valueOf(loop+1) );
                        thisLoc.append( "\" type=\"Empty\"/>" );
                        thisLoc.append( CommonConstants.NL );
                        haveSlot = true;
                    }

                } else {

                    // Yup.  If the equipment isn't a system, get it.
                    Mounted mount = null;
                    if ( CriticalSlot.TYPE_EQUIPMENT == slot.getType() ) {
                        mount = entity.getEquipment( slot.getIndex() );
                    }

                    // Destroyed locations on Mechs that contain slots
                    // that are missing but not hit or destroyed must
                    // have been blown off.
                    if ( isDestroyed && isMech && slot.isMissing() &&
                         !slot.isHit() && !slot.isDestroyed() ) {
                        thisLoc.append( formatSlot( String.valueOf(loop+1),
                                                    mount,
                                                    slot.isHit(),
                                                    slot.isDestroyed() ) );
                        haveSlot = true;
                        blownOff = true;
                    }

                    // Record damaged slots in undestroyed locations.
                    else if ( !isDestroyed && slot.isDamaged() ) {
                        thisLoc.append( formatSlot( String.valueOf(loop+1),
                                                    mount,
                                                    slot.isHit(),
                                                    slot.isDestroyed() ) );
                        haveSlot = true;
                    }

                    // Record ammunition slots in undestroyed locations.
                    // N.B. the slot CAN\"T be damaged at this point.
                    else if ( !isDestroyed && mount != null &&
                              mount.getType() instanceof AmmoType ) {
                        thisLoc.append( "         <slot index=\"" );
                        thisLoc.append( String.valueOf(loop+1) );
                        thisLoc.append( "\" type=\"" );
                        thisLoc.append( mount.getType().getInternalName() );
                        thisLoc.append( "\" shots=\"" );
                        thisLoc.append( String.valueOf(mount.getShotsLeft()) );
                        thisLoc.append( "\"/>" );
                        thisLoc.append( CommonConstants.NL );
                        haveSlot = true;
                    }

                    // Record the munition type of oneshot launchers
                    else if ( !isDestroyed && mount != null &&
                              mount.getType() instanceof WeaponType &&
                              ((WeaponType)mount.getType()).hasFlag(WeaponType.F_ONESHOT)) {
                        thisLoc.append( formatSlot( String.valueOf(loop+1),
                                                    mount,
                                                    slot.isHit(),
                                                    slot.isDestroyed() ) );
                        haveSlot = true;
                    }

                } // End have-slot

            } // Check the next slot in this location

            // Tanks don't have slots, and Protomechs only have
            // system slots, so we have to handle the ammo specially.
            if ( entity instanceof Tank ||
                 entity instanceof Protomech ) {
                Enumeration ammo = entity.getAmmo();
                while ( ammo.hasMoreElements() ) {

                    // Is this ammo in the current location?
                    Mounted mount = (Mounted) ammo.nextElement();
                    if ( mount.getLocation() == loc ) {
                        thisLoc.append( formatSlot( "N/A",
                                                    mount,
                                                    false,
                                                    false ) );
                        haveSlot = true;
                    }

                } // Check the next ammo.

                // TODO: handle slotless equipment.

                // TODO: handle tank crits.

            } // End is-tank-or-proto

            // Did we record information for this location?
            if ( thisLoc.length() > 0 ) {

                // Add this location to the output string.
                output.append( "      <location index=\"" );
                output.append( String.valueOf(loc) );
                if ( isDestroyed ) {
                    output.append( "\" isDestroyed=\"true" );
                }
                output.append( "\"> " );
                output.append( entity.getLocationName(loc) );
                if ( blownOff ) {
                    output.append( " has been blown off." );
                }
                output.append( CommonConstants.NL );
                output.append( thisLoc.toString() );
                output.append( "      </location>" );
                output.append( CommonConstants.NL );

                // Reset the location buffer.
                thisLoc = new StringBuffer();
                blownOff = false;

            } // End output-location

            // If the location is completely destroyed, log it anyway.
            else if ( isDestroyed ) {

                // Add this location to the output string.
                output.append( "      <location index=\"" );
                output.append( String.valueOf(loc) );
                output.append( "\" isDestroyed=\"true\" /> " );
                output.append( entity.getLocationName(loc) );
                output.append( CommonConstants.NL );

            } // End location-completely-destroyed

            // Reset the "location is destroyed" flag.
            isDestroyed = false;

        } // Handle the next location

        // If there is no location string, return a null.
        if ( output.length() == 0 ) {
            return null;
        }

        // If we recorded a slot, remind the player that slots start at 1.
        if ( haveSlot ) {
            output.insert( 0, CommonConstants.NL );
            output.insert
                ( 0, "      The first slot in a location is at index=\"1\"." );

            // Tanks do wierd things with ammo.
            if ( entity instanceof Tank ) {
                output.insert( 0, CommonConstants.NL );
                output.insert( 0, "      Tanks have special needs, so don't delete any ammo slots." );
            }
        }

        // Convert the output into a String and return it.
        return output.toString();

    } // End private static String getLocString( Entity )

    /**
     * Save the <code>Entity</code>s in the list to the given file.
     * <p/>
     * The <code>Entity</code>s\" pilots, damage, ammo loads, ammo usage, and
     * other campaign-related information are retained but data specific to
     * a particular game is ignored.
     *
     * @param   fileName - the <code>String</code> name of the file.  The
     *          current contents of the file will be discarded and all
     *          <code>Entity</code>s in the list will be written to the file.
     * @param   list - a <code>Vector</code> containing <code>Entity</code>s
     *          to be stored in a file.
     * @exception <code>IOException</code> is thrown on any error.
     */
    public static void saveTo( String filePath, String fileName, Vector list )
        throws IOException {

        // Open up the file.  Produce UTF-8 output.
        Writer output = new BufferedWriter( new OutputStreamWriter
            ( new FileOutputStream(new File(filePath, fileName)), "UTF-8" )
            );

        // Output the doctype and header stuff.
        output.write( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
        output.write( CommonConstants.NL );
        output.write( CommonConstants.NL );
        output.write( "<unit>" );
        output.write( CommonConstants.NL );
        output.write( CommonConstants.NL );

        // Walk through the list of entities.
        Enumeration items = list.elements();
        while ( items.hasMoreElements() ) {
            final Entity entity = (Entity) items.nextElement();

            // Start writing this entity to the file.
            output.write( "   <entity chassis=\"" );
            output.write( entity.getChassis() );
            output.write( "\" model=\"" );
            output.write( entity.getModel() );
            output.write( "\" type=\"" );
            output.write( entity.getMovementModeAsString() );
            output.write( "\">" );
            output.write( CommonConstants.NL );

            // Add the crew this entity.
            final Pilot crew = entity.getCrew();
            output.write( "      <pilot name=\"" );
            output.write( crew.getName() );
            output.write( "\" gunnery=\"" );
            output.write( String.valueOf(crew.getGunnery()) );
            output.write( "\" piloting=\"" );
            output.write( String.valueOf(crew.getPiloting()) );
            if ( crew.isDead() || crew.getHits() > 5 ) {
                output.write( "\" hits=\"Dead" );
            }
            else if ( crew.getHits() > 0 ) {
                output.write( "\" hits=\"" );
                output.write( String.valueOf(crew.getHits()) );
            }
            if ( crew.countAdvantages() > 0 ) {
                output.write( "\" advantages=\"" );
                output.write( String.valueOf(crew.getAdvantageList("::")) );
            }
            if (entity instanceof Mech) {
                if ( ((Mech)entity).isAutoEject() ) {
                    output.write( "\" autoeject=\"true" );
                } else {
                    output.write ("\" autoeject=\"false" );
                }
            }
            output.write( "\"/>" );
            output.write( CommonConstants.NL );

            // Add the locations of this entity (if any are needed).
            String loc = getLocString( entity );
            if ( null != loc ) {
                output.write( loc );
            }

            // Finish writing this entity to the file.
            output.write( "   </entity>" );
            output.write( CommonConstants.NL );
            output.write( CommonConstants.NL );

        } // Handle the next entity

        // Finish writing.
        output.write( "</unit>" );
        output.write( CommonConstants.NL );
        output.flush();
        output.close();
    }

    /**
     * Load a list of <code>Entity</code>s from the given file.
     * <p/>
     * The <code>Entity</code>s\" pilots, damage, ammo loads, ammo usage, and
     * other campaign-related information are retained but data specific to
     * a particular game is ignored.
     *
     * @param   fileName - the <code>String</code> name of the file.
     * @return  A <code>Vector</code> containing <code>Entity</code>s
     *          loaded from the file.  This vector may be empty, but
     *          it will not be <code>null</code>.
     * @exception <code>IOException</code> is thrown on any error.
     */
    public static Vector loadFrom( String filePath, String fileName ) 
        throws IOException {

        // Create an empty parser.
        XMLStreamParser parser = new XMLStreamParser();

        // Open up the file.
        InputStream listStream = new FileInputStream
            ( new File(filePath, fileName) );

        // Read a Vector from the file.
        try {
            parser.parse( listStream );
            listStream.close();
        }
        catch ( ParseException excep ) {
            excep.printStackTrace( System.err );
            throw new IOException( "Unable to read from: " + fileName );
        }

        // Was there any error in parsing?
        if ( parser.hasWarningMessage() ) {
            System.out.println( parser.getWarningMessage() );
        }

        // Return the entities.
        return parser.getEntities();
    }

}
