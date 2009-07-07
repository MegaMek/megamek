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
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;

/**
 * Objects of this class can encode a <code>GameOptions</code> object as XML
 * into an output writer and decode one from a parsed XML node. It is used when
 * saving games into a version- neutral format.
 * 
 * @author James Damour <suvarov454@users.sourceforge.net>
 */
public class GameOptionsEncoder {

    /**
     * Encode a <code>GameOptions</code> object to an output writer.
     * 
     * @param options - the <code>GameOptions</code> to be encoded. This value
     *            must not be <code>null</code>.
     * @param out - the <code>Writer</code> that will receive the XML. This
     *            value must not be <code>null</code>.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IOException</code> if there's any error on write.
     */
    public static void encode(GameOptions options, Writer output)
            throws IOException {
        // First, validate our input.
        if (null == options) {
            throw new IllegalArgumentException("The game options is null.");
        }
        if (null == output) {
            throw new IllegalArgumentException("The writer is null.");
        }

        // Start the XML stream for this set of game options.
        output.write("<options version=\"1.0\">");

        // Now the options themselves
        Enumeration<IOptionGroup> groups = options.getGroups();
        while (groups.hasMoreElements()) {
            final IOptionGroup group = groups.nextElement();

            Enumeration<IOption> iter = group.getOptions();
            while (iter.hasMoreElements()) {
                final IOption option = iter.nextElement();

                // Encode this option.
                output.write("<gameoption><optionname>");
                output.write(option.getName());
                output.write("</optionname><optionvalue>");
                output.write(option.getValue().toString());
                output.write("</optionvalue></gameoption>");
            }

        } // Handle the next option group.

        // Finish writing.
        output.write("</options>");
    }

    /**
     * Decode a <code>GameOptions</code> object from the passed node.
     * 
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param game - the <code>Game</code> the decoded object belongs to.
     * @return the <code>GameOptions</code> object based on the node.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IllegalStateException</code> if the node does not contain
     *             a valid <code>GameOptions</code>.
     */
    public static GameOptions decode(ParsedXML node, IGame game) {
        return null;
    }

}
