/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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

package megamek.common.xml;

import java.io.Writer;
import java.io.IOException;
import java.util.Enumeration;
import gd.xml.tiny.ParsedXML;
import megamek.common.*;

/**
 * Objects of this class can encode a <code>Entity</code> object as XML
 * into an output writer and decode one from a parsed XML node.  It is used
 * when saving games into a version- neutral format.
 *
 * @author      James Damour <suvarov454@users.sourceforge.net>
 */
public class EntityEncoder {

    /**
     * Encode a <code>Entity</code> object to an output writer.
     *
     * @param   entity - the <code>Entity</code> to be encoded.
     *          This value must not be <code>null</code>.
     * @param   out - the <code>Writer</code> that will receive the XML.
     *          This value must not be <code>null</code>.
     * @throws  <code>IllegalArgumentException</code> if the entity is
     *          <code>null</code>.
     * @throws  <code>IOException</code> if there's any error on write.
     */
    public static void encode( Entity entity, Writer out )
        throws IOException
    {
        Enumeration iter; // used when marching through a list of sub-elements
        Coords coords;
        int turns;
        String substr;

        // First, validate our input.
        if ( null == entity ) {
            throw new IllegalArgumentException( "The entity is null." );
        }
        if ( null == out ) {
            throw new IllegalArgumentException( "The writer is null." );
        }

        // Make sure any transported entities are written first.
        iter = entity.getLoadedUnits().elements();
        while( iter.hasMoreElements() ) {
            EntityEncoder.encode( (Entity) iter.nextElement(), out );
        }

        // Start writing this entity to the file.
        out.write( "<entity chassis=\"" );
        out.write( entity.getChassis() );
        out.write( "\" model=\"" );
        out.write( entity.getModel() );
        out.write( "\" type=\"" );
        out.write( entity.getMovementTypeAsString() );
        out.write( "\" techBase=\"" );
        out.write( entity.isClan() ? "Clan" : "IS" );
        out.write( "\" year=\"" );
        out.write( String.valueOf(entity.getYear()) );
        out.write( "\" mass=\"" );
        out.write( String.valueOf(entity.getWeight()) );
        out.write( "\" walkMp=\"" );
        out.write( String.valueOf(entity.getOriginalWalkMP()) );
        out.write( "\" jumpMp=\"" );
        out.write( String.valueOf(entity.getOriginalJumpMP()) );
        out.write( "\">" );

        // Add the crew this entity.
        final Pilot crew = entity.getCrew();
        out.write( "<pilot name=\"" );
        out.write( crew.getName() );
        out.write( "\" gunnery=\"" );
        out.write( String.valueOf(crew.getGunnery()) );
        out.write( "\" piloting=\"" );
        out.write( String.valueOf(crew.getPiloting()) );
        if ( crew.isDead() || crew.getHits() > 5 ) {
            out.write( "\" hits=\"Dead" );
        }
        else if ( crew.getHits() > 0 ) {
            out.write( "\" hits=\"" );
            out.write( String.valueOf(crew.getHits()) );
        }
        if ( crew.countAdvantages() > 0 ) {
            out.write( "\" advantages=\"" );
            out.write( String.valueOf(crew.getAdvantageList(" ")) );
        }
        out.write( "\"/>" );

        // Write the game-specific data.
        out.write( "<entityData gameId=\"" );
        out.write( String.valueOf(entity.getId()) );
        out.write( "\" externalId=\"" );
        out.write( String.valueOf(entity.getExternalId()) );
        out.write( "\" ownerId=\"" );
        out.write( String.valueOf(entity.getOwnerId()) );
        out.write( "\" facing=\"" );
        out.write( String.valueOf(entity.getFacing()) );
        out.write( "\" secondaryFacing=\"" );
        out.write( String.valueOf(entity.getSecondaryFacing()) );
        out.write( "\" walkMpCurrent=\"" );
        out.write( String.valueOf(entity.getWalkMP()) );
        out.write( "\" isOmni=\"" );
        out.write( entity.isOmni() ? "true" : "false" );
        out.write( "\" jumpMpCurrent=\"" );
        out.write( String.valueOf(entity.getJumpMP()) );
        out.write( "\" C3MasterId=\"" );
        out.write( String.valueOf(entity.getC3MasterId()) );
        out.write( "\" transportId=\"" );
        out.write( String.valueOf(entity.getTransportId()) );
        out.write( "\" swarmTargetId=\"" );
        out.write( String.valueOf(entity.getSwarmTargetId()) );
        out.write( "\" swarmAttackerId=\"" );
        out.write( String.valueOf(entity.getSwarmAttackerId()) );
        out.write( "\" removalCondition=\"" );
        out.write( String.valueOf(entity.getRemovalCondition()) );
        out.write( "\" deployRound=\"" );
        out.write( String.valueOf(entity.getDeployRound()) );

        out.write( "\" isShutDown=\"" );
        out.write( entity.isShutDown() ? "true" : "false" );
        out.write( "\" isDoomed=\"" );
        out.write( entity.isDoomed() ? "true" : "false" );
        out.write( "\" isDestroyed=\"" );
        out.write( entity.isDestroyed() ? "true" : "false" );
        out.write( "\" isDone=\"" );
        out.write( entity.isDone() ? "true" : "false" );
        out.write( "\" isProne=\"" );
        out.write( entity.isProne() ? "true" : "false" );
        out.write( "\" isFindingClub=\"" );
        out.write( entity.isFindingClub() ? "true" : "false" );
        out.write( "\" isArmsFlipped=\"" );
        out.write( entity.getArmsFlipped() ? "true" : "false" );
        out.write( "\" isUnjammingRAC=\"" );
        out.write( entity.isUnjammingRAC() ? "true" : "false" );
        out.write( "\" isSpotting=\"" );
        out.write( entity.isSpotting() ? "true" : "false" );
        out.write( "\" isClearingMinefield=\"" );
        out.write( entity.isClearingMinefield() ? "true" : "false" );
        out.write( "\" isSelected=\"" );
        out.write( entity.isSelected() ? "true" : "false" );
        out.write( "\" isSalvage=\"" );
        out.write( entity.isSalvage() ? "true" : "false" );
        out.write( "\" isDeployed=\"" );
        out.write( entity.isDeployed() ? "true" : "false" );
        out.write( "\" isUnloadedThisTurn=\"" );
        out.write( entity.isUnloadedThisTurn() ? "true" : "false" );

        out.write( "\" heat=\"" );
        out.write( String.valueOf(entity.heat) );
        out.write( "\" heatBuildup=\"" );
        out.write( String.valueOf(entity.heatBuildup) );
        out.write( "\" delta_distance=\"" );
        out.write( String.valueOf(entity.delta_distance) );
        out.write( "\" mpUsed=\"" );
        out.write( String.valueOf(entity.mpUsed) );
        out.write( "\" moved=\"" );
        out.write( String.valueOf(entity.moved) );
        out.write( "\" damageThisPhase=\"" );
        out.write( String.valueOf(entity.damageThisPhase) );
        out.write( "\" engineHitsThisRound=\"" );
        out.write( String.valueOf(entity.engineHitsThisRound) );
        out.write( "\" rolledForEngineExplosion=\"" );
        out.write( entity.rolledForEngineExplosion ? "true" : "false" );
        out.write( "\" dodging=\"" );
        out.write( entity.dodging ? "true" : "false" );
        out.write( "\" >" );

        // Now save the entity's coordinates.
        coords = entity.getPosition();
        if ( null != coords ) CoordsEncoder.encode( coords, out );

        // Is the entity performing a displacement attack?
        if ( entity.hasDisplacementAttack() ) {
            EntityActionEncoder.encode( entity.getDisplacementAttack(), out );
        }

        // Add the narc pods attached to this entity (if any are needed).
        substr = getNarcString( entity );
        if ( null != substr ) out.write( substr );

        // Encode the infernos burning on this entity.
        if ( entity.infernos.isStillBurning() ) {
            // Encode the infernos on this entity.
            out.write( "<inferno>" );
            turns = entity.infernos.getArrowIVTurnsLeftToBurn();
            // This value may be zero.
            if ( turns > 0 ) {
                out.write( "<arrowiv turns=\"" );
                out.write( String.valueOf(turns) );
                out.write( "\" />" );
            }
            // -(Arrow IV turns - All Turns) = Standard Turns.
            turns -= entity.infernos.getTurnsLeftToBurn();
            turns = -turns;
            if ( turns > 0 ) {
                out.write( "<standard turns=\"" );
                out.write( String.valueOf(turns) );
                out.write( "\" />" );
            }
            out.write( "</inferno>" );
        }

        // Record the IDs of all transported units (if any).
        iter = entity.getLoadedUnits().elements();
        if ( iter.hasMoreElements() ) {
            out.write( "<loadedUnits>" );
            while ( iter.hasMoreElements() ) {
                Entity loaded = (Entity) iter.nextElement();
                out.write( "<entityRef gameId=\"" );
                out.write( String.valueOf(loaded.getId()) );
                out.write( "\" />" );
            }
            out.write( "</loadedUnits>" );
        }

        // Handle sub-classes of Entity.
        out.write( "<class name=\"" );
        if ( entity instanceof BipedMech ) {
            out.write( "BipedMech\">" );
            BipedMechEncoder.encode( entity, out );
        }
        else if ( entity instanceof QuadMech ) {
            out.write( "QuadMech\">" );
            QuadMechEncoder.encode( entity, out );
        }
        else if ( entity instanceof Tank ) {
            out.write( "Tank\">" );
            TankEncoder.encode( entity, out );
        }
        else if ( entity instanceof BattleArmor ) {
            out.write( "BattleArmor\">" );
            BattleArmorEncoder.encode( entity, out );
        }
        else if ( entity instanceof Infantry ) {
            out.write( "Infantry\">" );
            InfantryEncoder.encode( entity, out );
        }
        else if ( entity instanceof Protomech ) {
            out.write( "Protomech\">" );
            ProtomechEncoder.encode( entity, out );
        }
        else {
            throw new IllegalStateException
                ( "Unexpected entity type " + entity.getClass().getName() );
        }
        out.write( "</class>" );
        
        // Finish the game-specific data.
        out.write( "</entityData>" );

        // Encode this unit's equipment.
        iter = entity.getEquipment();
        if ( iter.hasMoreElements() ) {
            out.write( "<entityEquipment>" );
            int index = 0;
            while ( iter.hasMoreElements() ) {
                substr = EntityEncoder.formatEquipment
                    ( index, (Mounted) iter.nextElement(), entity );
                if ( null != substr ) out.write( substr );
                index++;
            }
            out.write( "</entityEquipment>" );
        }

        // Add the locations of this entity (if any are needed).
        substr = getLocString( entity );
        if ( null != substr ) out.write( substr );

        // Finish the XML stream for this entity.
        out.write( "</entity>" );
    }

    /**
     * Produce a string describing all NARC pods on the entity.
     *
     * @param   entity - the <code>Entity</code> being examined.
     *          This value may be <code>null</code>.
     * @return  a <code>String</code> describing the equipment.
     *          This value may be <code>null</code>.
     */
    private static String getNarcString( Entity entity ) {
        // null in, null out
        if ( null == entity ) return null;

        // Show all teams that have NARCed the entity.
        StringBuffer output = new StringBuffer();
        boolean narced = false;
        for ( int team = Player.TEAM_NONE; team < Player.MAX_TEAMS; team++ ) {
            if ( entity.isNarcedBy( team ) ) {

                // Is this the first narc on the entity?
                if ( !narced ) {
                    output.append( "<narcs>" );
                    narced = true;
                }

                // Add this team to the NARC pods.
                output.append( "<narc type=\"Standard\" team=\"" );
                output.append( String.valueOf( team ) );
                output.append( "\" />" );
            }
        }

        // If the entity wasn't narced, return a null.
        if ( !narced ) return null;

        // Finish off this section, and return the string.
        output.append( "</narcs>" );
        return output.toString();
    }

    /**
     * Produce a string describing a piece of equipment.
     *
     * @param   index - the <code>int</code> index of this equipment
     *          on the given entity.
     * @param   mount - the <code>Mounted</code> object of the equipment.
     *          This value should not be <code>null</code>.
     * @param   entity - the <code>Entity</code> that has this mount.
     * @return  a <code>String</code> describing the equipment.
     *          This value will be <code>null</code> if a <code>null</code>
     *          was passed.
     */
    private static String formatEquipment( int index, Mounted mount,
                                           Entity entity ) {
        StringBuffer output = new StringBuffer();

        // null in, null out.
        if ( null == mount ) return null;

        // Format this piece of equipment.
        output.append( "<equipment index=\"" );
        output.append( String.valueOf(index) );
        output.append( "\" type=\"" );
        output.append( mount.getType().getInternalName() );
        output.append( "\" location=\"" );
        output.append( String.valueOf(mount.getLocation()) );
        output.append( "\" isRear=\"" );
        output.append( mount.isRearMounted() ? "true" : "false" );
        if ( mount.getType() instanceof AmmoType ) {
            output.append( "\" shots=\"" );
            output.append( String.valueOf(mount.getShotsLeft()) );
        }
        output.append( "\" curMode=\"" );
        output.append( mount.curMode() );
        output.append( "\" pendingMode=\"" );
        output.append( mount.pendingMode() );
        output.append( "\" linkedRef=\"" );
        if ( null == mount.getLinked() ) {
            output.append( "N/A" );
        } else {
            output.append( String.valueOf( entity.getEquipmentNum
                                           (mount.getLinked()) ) );
        }
        output.append( "\" foundCrits=\"" );
        output.append( String.valueOf(mount.getFoundCrits()) );

        output.append( "\" isUsedThisRound=\"" );
        output.append( mount.isUsedThisRound() ? "true" : "false" );
        output.append( "\" isBreached=\"" );
        output.append( mount.isBreached() ? "true" : "false" );
        output.append( "\" isHit=\"" );
        output.append( mount.isHit() ? "true" : "false" );
        output.append( "\" isDestroyed=\"" );
        output.append( mount.isDestroyed() ? "true" : "false" );
        output.append( "\" isMissing=\"" );
        output.append( mount.isMissing() ? "true" : "false" );
        output.append( "\" isJammed=\"" );
        output.append( mount.isJammed() ? "true" : "false" );
        output.append( "\" isPendingDump=\"" );
        output.append( mount.isPendingDump() ? "true" : "false" );
        output.append( "\" isDumping=\"" );
        output.append( mount.isDumping() ? "true" : "false" );
        output.append( "\" isSplit=\"" );
        output.append( mount.isSplit() ? "true" : "false" );
        output.append( "\" isFired=\"" );
        output.append( mount.isFired() ? "true" : "false" );
        output.append( "\"/>" );

        // Return a String.
        return output.toString();
    }

    /**
     * Produce a string describing the equipment in a critical slot.
     *
     * @param   slot - the <code>CriticalSlot</code> being encoded.
     * @param   mount - the <code>Mounted</code> object of the equipment.
     *          This value should be <code>null</code> for a slot with
     *          system equipment.
     * @return  a <code>String</code> describing the slot.
     */
    private static String formatSlot( CriticalSlot slot, Mounted mount ) {
        StringBuffer output = new StringBuffer();

        // Don't forget... slots start at index 1.
        output.append( "<slot index=\"" );
        output.append( String.valueOf(slot.getIndex()+1) );
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
        }
        output.append( "\" isHit=\"" );
        output.append( slot.isHit() ? "true" : "false" );
        output.append( "\" isDestroyed=\"" );
        output.append( slot.isDestroyed() ? "true" : "false" );
        output.append( "\" isMissing=\"" );
        output.append( slot.isMissing() ? "true" : "false" );
        output.append( "\" isBreached=\"" );
        output.append( slot.isBreached() ? "true" : "false" );
        output.append( "\" isHittable=\"" );
        output.append( slot.isEverHittable() ? "true" : "false" );
        output.append( "\"/>" );

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
        StringBuffer output = new StringBuffer();

        // Walk through the locations for the entity,
        // and only record damage and ammo.
        for ( int loc = 0; loc < entity.locations(); loc++ ) {

            // Add this location to the output string.
            output.append( "<location index=\"" );
            output.append( String.valueOf(loc) );
            output.append( "\"> " );

            // Record values of armor and internal structure,
            // unless the section never has armor.
            if ( entity.getOInternal(loc) != Entity.ARMOR_NA ) {
                output.append( "<armor points=\"" );
                output.append( String.valueOf(entity.getArmor(loc)) );
                output.append( "\"/>" );
                output.append( "<armor points=\"" );
                output.append( String.valueOf(entity.getInternal(loc)) );
                output.append( "\" type=\"Internal\"/>" );
                if ( entity.hasRearArmor(loc) ) {
                    output.append( "<armor points=\"" );
                    output.append(String.valueOf(entity.getArmor(loc, true)));
                    output.append( "\" type=\"Rear\"/>" );
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
                        output.append( "<slot index=\"" );
                        output.append( String.valueOf(loop+1) );
                        output.append( "\" type=\"Empty\"/>" );
                    }

                } else {

                    // Yup.  If the equipment isn't a system, get it.
                    Mounted mount = null;
                    if ( CriticalSlot.TYPE_EQUIPMENT == slot.getType() ) {
                        mount = entity.getEquipment( slot.getIndex() );
                    }

                    // Format the slot.
                    output.append( formatSlot( slot, mount ) );

                } // End have-slot

            } // Check the next slot in this location

            // Tanks don't have slots, and Protomechs only have
            // system slots, so we have to handle their ammo specially.
            if ( entity instanceof Tank ||
                 entity instanceof Protomech ) {
                Enumeration ammo = entity.getAmmo();
                while ( ammo.hasMoreElements() ) {

                    // Is this ammo in the current location?
                    Mounted mount = (Mounted) ammo.nextElement();
                    if ( mount.getLocation() == loc ) {
                        output.append( "<slot index=\"N/A\" type=\"" );
                        output.append( mount.getType().getInternalName() );
                        output.append( "\" shots=\"" );
                        output.append( String.valueOf(mount.getShotsLeft()) );
                        output.append( "\"/>" );
                    }

                } // Check the next ammo.

            } // End is-tank-or-proto

            // Finish off the location
            output.append( "</location>" );

        } // Handle the next location

        // Convert the output into a String and return it.
        return output.toString();

    } // End private static String getLocString( Entity )

    /**
     * Decode a <code>Entity</code> object from the passed node.
     *
     * @param   node - the <code>ParsedXML</code> node for this object.
     *          This value must not be <code>null</code>.
     * @param   game - the <code>Game</code> the decoded object belongs to.
     * @return  the <code>Entity</code> object based on the node.
     * @throws  <code>IllegalArgumentException</code> if the node is
     *          <code>null</code>.
     * @throws  <code>IllegalStateException</code> if the node does not
     *          contain a valid <code>Entity</code>.
     */
    public static Entity decode( ParsedXML node, Game game ) {
        return null;
    }

}

