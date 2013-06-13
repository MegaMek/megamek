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
import java.util.Enumeration;

import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.Team;

/**
 * Objects of this class can encode a <code>Team</code> object as XML into an
 * output writer and decode one from a parsed XML node. It is used when saving
 * games into a version- neutral format.
 * 
 * @author James Damour <suvarov454@users.sourceforge.net>
 */
public class TeamEncoder {

    /**
     * Encode a <code>Team</code> object to an output writer.
     * 
     * @param team - the <code>Team</code> to be encoded. This value must not
     *            be <code>null</code>.
     * @param out - the <code>Writer</code> that will receive the XML. This
     *            value must not be <code>null</code>.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IOException</code> if there's any error on write.
     */
    public static void encode(Team team, Writer out) throws IOException {
        Enumeration<Player> iter; // used when marching through a list of sub-elements

        // First, validate our input.
        if (null == team) {
            throw new IllegalArgumentException("The team is null.");
        }
        if (null == out) {
            throw new IllegalArgumentException("The writer is null.");
        }

        // Start the XML stream for this team
        out.write("<team version=\"1.0\" >");

        // Write the hex array to the stream.
        out.write("<teamData id=\"");
        out.write(team.getId());
        out.write("\">");

        // Write the coordinate of the team.
        iter = team.getPlayers();
        if (iter.hasMoreElements()) {
            out.write("<playerIds>");
            while (iter.hasMoreElements()) {
                final Player player = iter.nextElement();
                out.write("<player id=\"");
                out.write(player.getId());
                out.write("\" />");
            }
        }
        out.write("</playerIds>");

        // TODO : Do I have to do something for Turn_Vectors?

        // Finish the XML stream for this team.
        out.write("</teamData>");
        out.write("</team>");
    }

    /**
     * Decode a <code>Team</code> object from the passed node.
     * 
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param game - the <code>IGame</code> the decoded object belongs to.
     * @return the <code>Team</code> object based on the node.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IllegalStateException</code> if the node does not contain
     *             a valid <code>Team</code>.
     */
    public static Team decode(ParsedXML node, IGame game) {
        return null;
    }

}
