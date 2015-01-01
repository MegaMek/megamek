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
     * @throws  <code>IllegalArgumentException</code> if the node is
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
        out.write( entity.getYear() );
        out.write( "\" mass=\"" );
        out.write( Float.toString(entity.getWeight()) );
        out.write( "\" walkMp=\"" );
        out.write( entity.getOriginalWalkMP() );
        out.write( "\" jumpMp=\"" );
        out.write( entity.getOriginalJumpMP() );
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
        out.write( entity.getId() );
        out.write( "\" externalId=\"" );
        out.write( entity.getExternalId() );
        out.write( "\" ownerId=\"" );
        out.write( entity.getOwnerId() );
        out.write( "\" facing=\"" );
        out.write( entity.getFacing() );
        out.write( "\" secondaryFacing=\"" );
        out.write( entity.getSecondaryFacing() );
        out.write( "\" walkMpCurrent=\"" );
        out.write( entity.getWalkMP() );
        out.write( "\" isOmni=\"" );
        out.write( entity.isOmni() ? "true" : "false" );
        out.write( "\" jumpMpCurrent=\"" );
        out.write( entity.getJumpMP() );
        out.write( "\" C3MasterId=\"" );
        out.write( entity.getC3MasterId() );
        out.write( "\" transportId=\"" );
        out.write( entity.getTransportId() );
        out.write( "\" swarmTargetId=\"" );
        out.write( entity.getSwarmTargetId() );
        out.write( "\" swarmAttackerId=\"" );
        out.write( entity.getSwarmAttackerId() );
        out.write( "\" removalCondition=\"" );
        out.write( entity.getRemovalCondition() );
        out.write( "\" deployRound=\"" );
        out.write( entity.getDeployRound() );

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
        // TODO : encode unloadedThisTurn

        out.write( "\" heat=\"" );
        out.write( entity.heat );
        out.write( "\" heatBuildup=\"" );
        out.write( entity.heatBuildup );
        out.write( "\" delta_distance=\"" );
        out.write( entity.delta_distance );
        out.write( "\" mpUsed=\"" );
        out.write( entity.mpUsed );
        out.write( "\" moved=\"" );
        out.write( entity.moved );
        out.write( "\" damageThisPhase=\"" );
        out.write( entity.damageThisPhase );
        out.write( "\" engineHitsThisRound=\"" );
        out.write( entity.engineHitsThisRound );
        out.write( "\" rolledForEngineExplosion=\"" );
        out.write( entity.rolledForEngineExplosion ? "true" : "false" );
        out.write( "\" dodging=\"" );
        out.write( entity.dodging ? "true" : "false" );
        out.write( "\" >" );

        // Now save the entity's coordinates.
        coords = entity.getPosition();
        if ( null != coords ) {
            CoordsEncoder.encode( coords, out );
        }

        // Is the entity performing a displacement attack?
        if ( entity.hasDisplacementAttack() ) {
            EntityActionEncoder.encode( entity.getDisplacementAttack(), out );
        }

        // Add the narc pods attached to this entity (if any are needed).
        substr = getNarcString( entity );
        if ( null != substr ) {
            out.write( substr );
        }

        // Encode the infernos burning on this entity.
        if ( entity.infernos.isStillBurning() ) {
            // Encode the infernos on this entity.
            out.write( "<inferno>" );
            turns = entity.infernos.getArrowIVTurnsLeftToBurn();
            // This value may be zero.
            if ( turns > 0 ) {
                out.write( "<arrowiv turns=\"" );
                out.write( turns );
                out.write( "\" />" );
            }
            // -(Arrow IV turns - All Turns) = Standard Turns.
            turns -= entity.infernos.getTurnsLeftToBurn();
            turns = -turns;
            if ( turns > 0 ) {
                out.write( "<standard turns=\"" );
                out.write( turns );
                out.write( "\" />" );
            }
            out.write( "</inferno>" );

            // TODO : Encode this entity's transports.

            // TODO : Handle sub-classes
        }

        // Finish the game-specific data.
        out.write( "</entityData>" );

        // TODO : Do something about equipment.

        // Add the locations of this entity (if any are needed).
        substr = getLocString( entity );
        if ( null != substr ) {
            out.write( substr );
        }

        // Finish the XML stream for this entity.
        out.write( "</entity>" );
    }

    private static String getNarcString( Entity entity ) {
        return null;
    }

    private static String getLocString( Entity entity ) {
        return null;
    }

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

