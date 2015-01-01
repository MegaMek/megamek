/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.net.marshall;

import gd.xml.ParseException;
import gd.xml.tiny.ParsedXML;
import gd.xml.tiny.TinyParser;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Enumeration;

import megamek.common.net.Packet;
import megamek.common.xml.PacketEncoder;

/**
 * Marshaller that uses XML for <code>Packet</code>representation.
 */
public class XMLMarshaller extends PacketMarshaller {

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.net.marshall.PacketMarshaller#marshall(megamek.common.net.Packet,
     *      java.io.OutputStream)
     */
    public void marshall(Packet packet, OutputStream stream) throws Exception {
        OutputStreamWriter out = new OutputStreamWriter(stream);
        PacketEncoder.encode(packet, out);
        out.flush();
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.net.marshall.PacketMarshaller#unmarshall(java.io.InputStream)
     */
    public Packet unmarshall(InputStream stream) throws Exception {
        ParsedXML root = TinyParser.parseXML(stream);
        Enumeration<?> rootChildren = root.elements();
        if (!rootChildren.hasMoreElements()) {
            throw new ParseException("No children of the root.");
        }
        ParsedXML rootNode = (ParsedXML) rootChildren.nextElement();
        Packet packet = PacketEncoder.decode(rootNode, null);
        return packet;
    }

}
