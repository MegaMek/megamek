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
public class InfantryEncoder {

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
        int value;
        Infantry inf = (Infantry) entity;

        // First, validate our input.
        if ( null == entity ) {
            throw new IllegalArgumentException( "The entity is null." );
        }
        if ( null == out ) {
            throw new IllegalArgumentException( "The writer is null." );
        }

        // Our EntityEncoder already gave us our root element.
        out.write( "<shootingStrength value=\"" );
        value = inf.getShootingStrength();
        out.write( "\" />" );
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
        Infantry entity = new Infantry();
        return entity;
    }

}

