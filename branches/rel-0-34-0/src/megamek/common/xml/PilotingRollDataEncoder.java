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

import gd.xml.tiny.ParsedXML;

import java.io.IOException;
import java.io.Writer;

import megamek.common.IGame;
import megamek.common.PilotingRollData;

/**
 * Objects of this class can encode a <code>PilotingRollData</code> object as
 * XML into an output writer and decode one from a parsed XML node. It is used
 * when saving games into a version- neutral format.
 * 
 * @author James Damour <suvarov454@users.sourceforge.net>
 */
public class PilotingRollDataEncoder {

    /**
     * Encode a <code>PilotingRollData</code> object to an output writer.
     * 
     * @param option - the <code>PilotingRollData</code> to be encoded. This
     *            value must not be <code>null</code>.
     * @param out - the <code>Writer</code> that will receive the XML. This
     *            value must not be <code>null</code>.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IOException</code> if there's any error on write.
     */
    public static void encode(PilotingRollData option, Writer out)
            throws IOException {
    }

    /**
     * Decode a <code>PilotingRollData</code> object from the passed node.
     * 
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param game - the <code>IGame</code> the decoded object belongs to.
     * @return the <code>PilotingRollData</code> object based on the node.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IllegalStateException</code> if the node does not contain
     *             a valid <code>PilotingRollData</code>.
     */
    public static PilotingRollData decode(ParsedXML node, IGame game) {
        return null;
    }

}
