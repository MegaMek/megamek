/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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
import megamek.common.Minefield;
import megamek.common.Player;

/**
 * Objects of this class can encode a <code>Player</code> object as XML into
 * an output writer and decode one from a parsed XML node. It is used when
 * saving games into a version- neutral format.
 * 
 * @author James Damour <suvarov454@users.sourceforge.net>
 */
public class PlayerEncoder {

    /**
     * Encode a <code>Player</code> object to an output writer.
     * 
     * @param player - the <code>Player</code> to be encoded. This value must
     *            not be <code>null</code>.
     * @param out - the <code>Writer</code> that will receive the XML. This
     *            value must not be <code>null</code>.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IOException</code> if there's any error on write.
     */
    public static void encode(Player player, Writer out) throws IOException {
        Enumeration<Minefield> iter; // used when marching through a list of sub-elements

        // First, validate our input.
        if (null == player) {
            throw new IllegalArgumentException("The player is null.");
        }
        if (null == out) {
            throw new IllegalArgumentException("The writer is null.");
        }

        // Start the XML stream for this player
        out.write("<player version=\"1.0\" >");

        // Write the player's data to the stream.
        // TODO : write the initiative info to the stream.
        out.write("<playerData id=\"");
        out.write(player.getId());
        out.write("\" name=\"");
        out.write(player.getName());
        out.write("\" team=\"");
        out.write(player.getTeam());
        out.write("\" colorIndex=\"");
        out.write(player.getColorIndex());
        out.write("\" camoFileName=\"");
        out.write(player.getCamoFileName());
        out.write("\" startingPos=\"");
        out.write(player.getStartingPos());
        out.write("\" numConvMF=\"");
        out.write(player.getNbrMFConventional());
        out.write("\" numCommandMF=\"");
        out.write(player.getNbrMFCommand());
        out.write("\" numVibroMF=\"");
        out.write(player.getNbrMFVibra());
        out.write("\" isDone=\"");
        out.write(player.isDone() ? "true" : "false");
        out.write("\" isGhost=\"");
        out.write(player.isGhost() ? "true" : "false");
        out.write("\" isObserver=\"");
        out.write(player.isObserver() ? "true" : "false");
        out.write("\" isSeeAll=\"");
        out.write(player.getSeeAll() ? "true" : "false");
        out.write("\" admitsDefeat=\"");
        out.write(player.admitsDefeat() ? "true" : "false");
        out.write("\" />");

        // Write the coordinate of the player.
        iter = player.getMinefields().elements();
        if (iter.hasMoreElements()) {
            out.write("<minefields>");
            while (iter.hasMoreElements()) {
                MinefieldEncoder.encode(iter.nextElement(), out);
            }
        }
        out.write("</minefields>");

        // Finish the XML stream for this player.
        out.write("</player>");

    }

    /**
     * Decode a <code>Player</code> object from the passed node.
     * 
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param game - the <code>IGame</code> the decoded object belongs to.
     * @return the <code>Player</code> object based on the node.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IllegalStateException</code> if the node does not contain
     *             a valid <code>Player</code>.
     */
    public static Player decode(ParsedXML node, IGame game) {
        return null;
    }

}
