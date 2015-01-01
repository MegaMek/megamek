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

import gd.xml.ParseException;
import gd.xml.tiny.ParsedXML;
import gd.xml.tiny.TinyParser;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import megamek.common.Board;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.net.Packet;

import com.Ostermiller.util.Base64;

/**
 * Objects of this class can encode a <code>Packet</code> object as XML into
 * an output writer and decode one from a parsed XML node. It is used when
 * saving games into a version- neutral format.
 * 
 * @author James Damour <suvarov454@users.sourceforge.net>
 */
public class PacketEncoder {
    /**
     * Helper function to encode the packet's data.
     * 
     * @param packet - the <code>Packet</code> to be encoded. This value must
     *            not be <code>null</code>.
     * @param out - the <code>Writer</code> that will receive the XML. This
     *            value must not be <code>null</code>.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IOException</code> if there's any error on write.
     */
    private static void encodeData(Packet packet, Writer out)
            throws IOException {
        // Packet data encoding is based on data type.
        Object[] data = packet.getData();
        for (int loop = 0; loop < data.length; loop++) {
            if (null == data[loop]) {
                out.write("<null />");
            } else if (data[loop].getClass().equals(Integer.class)) {
                out.write("<integer value=\"");
                out.write(data[loop].toString());
                out.write("\" />");
            } else if (data[loop].getClass().equals(IBoard.class)) {
                BoardEncoder.encode((Board) data[loop], out);
            }
        }

    }

    /**
     * Helper function to decode the packet's data.
     * 
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param game - the <code>IGame</code> the decoded object belongs to.
     * @return the <code>Object</code> corresponding to the node. This value
     *         may be <code>null</code>.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IllegalStateException</code> if the node does not contain
     *             a valid <code>Packet</code>.
     * @throws <code>NumberFormatException</code> if the value of a numeric
     *             data element is not in a valid format.
     */
    private static Object decodeData(ParsedXML node, IGame game) {
        Object retval = null;

        // Decoding is base the node's name.
        if (node.getName().equals("integer")) {
            retval = new Integer(node.getAttribute("value"));
        } else if (node.getName().equals("board")) {
            retval = BoardEncoder.decode(node, game);
        }
        return retval;
    }

    /**
     * Encode a <code>Packet</code> object to an output writer.
     * 
     * @param packet - the <code>Packet</code> to be encoded. This value must
     *            not be <code>null</code>.
     * @param out - the <code>Writer</code> that will receive the XML. This
     *            value must not be <code>null</code>.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IOException</code> if there's any error on write.
     */
    public static void encode(Packet packet, Writer out) throws IOException {
        // First, validate our input.
        if (null == packet) {
            throw new IllegalArgumentException("The packet is null.");
        }
        if (null == out) {
            throw new IllegalArgumentException("The writer is null.");
        }

        // Encode the packet object to the stream.
        out.write("<packet type=\"");
        out.write(Integer.toString(packet.getCommand()));
        out.write("\" >");

        // Is the packet zipped?
        // boolean zipped = packet.isZipped();
        boolean zipped = false;

        // Do we have any data in this packet?
        // N.B. this action will unzip the packet.
        Object[] data = packet.getData();
        if (null != data) {

            // Should we compress the data?
            if (zipped) {

                // XML encode the data and GZIP it.
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Writer zipOut = new BufferedWriter(new OutputStreamWriter(
                        new GZIPOutputStream(baos)));
                PacketEncoder.encodeData(packet, zipOut);
                zipOut.close();

                // Base64 encode the commpressed data.
                // Please note, I couldn't get anything other than a
                // straight stream-to-stream encoding to work.
                byte[] zipData = baos.toByteArray();
                ByteArrayOutputStream base64 = new ByteArrayOutputStream(
                        (4 * zipData.length + 2) / 3);
                Base64.encode(new ByteArrayInputStream(zipData), base64, false);

                /***************************************************************
                 * begin debug code int loop; for ( loop = 0; loop <
                 * zipData.length; loop++ ) { System.out.print( (char)
                 * zipData[loop] ); } System.out.println( "" ); String zipStr =
                 * baos.toString(); for ( loop = 0; loop < zipStr.length();
                 * loop++ ) { System.out.print( zipStr.charAt(loop) ); }
                 * System.out.println( "" ); zipStr = base64.toString(); for (
                 * loop = 0; loop < zipStr.length(); loop++ ) {
                 * System.out.print( zipStr.charAt(loop) ); }
                 * System.out.println( "" ); end debug code *
                 **************************************************************/

                // Save the compressed data as the packetData CDATA.
                out.write("<packetData count=\"");
                out.write(Integer.toString(data.length));
                out.write("\" isGzipped=\"true\" >");
                out.write(base64.toString());
                out.write("</packetData>");

            } // End packet-is-zipped
            else {
                // Don't compress the XML.
                out.write("<packetData count=\"");
                out.write(Integer.toString(data.length));
                out.write("\" isGzipped=\"false\" >");
                PacketEncoder.encodeData(packet, out);
                out.write("</packetData>");
            }

        } // End have-data

        // Finish off the packet.
        out.write("</packet>");
    }

    /**
     * Decode a <code>Packet</code> object from the passed node.
     * 
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @param game - the <code>IGame</code> the decoded object belongs to.
     * @return the <code>Packet</code> object based on the node.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IllegalStateException</code> if the node does not contain
     *             a valid <code>Packet</code>.
     * @throws <code>NumberFormatException</code> if the value of a numeric
     *             data element is not in a valid format.
     */
    public static Packet decode(ParsedXML node, IGame game) {
        Packet packet = null;
        int command = 0;
        Object[] data = null;

        // Make sure we got a valid packet.
        if (null == node) {
            throw new IllegalArgumentException("The passed node is null.");
        }
        if (!node.getName().equals("packet")) {
            throw new IllegalStateException(
                    "The passed node is not for a packet.");
        }

        // Figure out what type of packet this is.
        String commandStr = node.getAttribute("type");
        if (null == commandStr) {
            throw new IllegalStateException(
                    "Could not determine the packet type.");
        }
        command = Integer.parseInt(commandStr);

        // TODO : perform version checking.

        // Walk the packet node's children. Try to find a "packetData" node.
        Enumeration<?> children = node.elements();
        while (children.hasMoreElements()) {
            ParsedXML subNode = (ParsedXML) children.nextElement();
            if (subNode.getName().equals("packetData")) {

                // How many data elements are in the packet data?
                final int count = Integer.parseInt(subNode
                        .getAttribute("count"));
                data = new Object[count];

                // Do we need to unzip the data elements?
                Enumeration<?> dataElements = null;
                if (subNode.getAttribute("isGzipped").equals("true")) {

                    // Try to find the zipped content.
                    String cdata = subNode.getContent();
                    if (null == cdata) {
                        Enumeration<?> cdataEnum = subNode.elements();
                        while (cdataEnum.hasMoreElements() && null == cdata) {
                            final ParsedXML cdataNode = (ParsedXML) cdataEnum
                                    .nextElement();
                            if (cdataNode.getTypeName().equals("text")) {
                                cdata = cdataNode.getContent();
                            } else if (cdataNode.getTypeName().equals("cdata")) {
                                cdata = cdataNode.getContent();
                            }
                        }
                    } // End look-for-cdata-nodes

                    // Did we find the zipped content?
                    if (null == cdata) {
                        throw new IllegalStateException(
                                "Could not find CDATA for packetData.");
                    }

                    // Yup. Unencode the data from Base64.
                    byte[] unBase64 = Base64.decodeToBytes(cdata);
                    InputStream parseStream;
                    try {
                        // Unzip the data.
                        parseStream = new GZIPInputStream(
                                new ByteArrayInputStream(unBase64));
                    } catch (IOException ioErr) {
                        StringBuffer iobuf = new StringBuffer();
                        iobuf.append("Could not unzip data elements: ").append(
                                ioErr.getMessage());
                        throw new IllegalStateException(iobuf.toString());
                    }

                    try {
                        /*******************************************************
                         * BEGIN debug code try { parseStream.mark(64000); int
                         * inChar = 0; while ( -1 != (inChar =
                         * parseStream.read()) ) { System.out.print( (char)
                         * inChar ); } System.out.println( "" );
                         * parseStream.reset(); } catch ( IOException debugErr ) {
                         * debugErr.printStackTrace(); } END debug code *
                         ******************************************************/

                        // Parse the XML.
                        ParsedXML dummyNode = TinyParser.parseXML(parseStream);
                        dataElements = dummyNode.elements();
                    } catch (ParseException parseErr) {
                        StringBuffer parsebuf = new StringBuffer();
                        parsebuf.append("Could not parse data elements: ")
                                .append(parseErr.getMessage());
                        throw new IllegalStateException(parsebuf.toString());
                    }
                } else {
                    // Nope. Just return the data elements.
                    dataElements = subNode.elements();
                }

                // Walk the children, and decode them into the data array.
                for (int loop = 0; dataElements.hasMoreElements(); loop++) {
                    data[loop] = PacketEncoder.decodeData(
                            (ParsedXML) dataElements.nextElement(), game);
                }

            } // End found-packetData-element

        } // Check the next child of the packet node.

        // Create and return the packet.
        if (null != data) {
            packet = new Packet(command, data);
        } else {
            packet = new Packet(command);
        }
        return packet;
    }

}
