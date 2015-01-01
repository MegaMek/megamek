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

package megamek.common.net;

import gd.xml.*;
import gd.xml.tiny.*;

import java.io.*;
import java.net.Socket;
import java.util.Enumeration;

import megamek.common.Packet;
import megamek.common.xml.PacketEncoder;
import megamek.server.Connection;
import megamek.common.net.ConnectionHandler;

/**
 * Transmit and receive <code>Packet</code>s that are encoded in an XML format.
 *
 * @author      James Damour <suvarov454@users.sourceforge.net>
 */
public class XmlConnection extends Connection {

    /**
     * The input <code>Reader</code> for this connection.
     */
    private Reader in = null;

    /**
     * The output <code>Writer</code> for this connection.
     */
    private Writer out = null;

    /**
     * The <code>DataOutputStream</code> that's tracking the
     * total amount of data written to the connection's output.
     */
    private DataOutputStream counter = null;

    /**
     * Reads a complete net command from the given socket.
     * <p/>
     * Subclasses are encouraged to override this method.
     *
     * @return  the <code>Packet</code> that was sent.
     */
    protected synchronized Packet readPacket() {
        try {
            StringBuffer buf = new StringBuffer();
            /* BEGIN Debug code BEGIN **

            // Write to a test file.
            buf.append( "read" )
                .append( this.getId() )
                .append( ".txt" );
            Writer test = new BufferedWriter
                ( new FileWriter(buf.toString(), true) );
            buf = new StringBuffer();
            /*  END  Debug code  END  */

            // There's a race condition in creating this object.
            while (in == null) {
                wait(100);
                // We can't simply pass the socket's InputStream to the parser,
                // as it expends and end-of-transmission to stop parsing.
                in = new BufferedReader
                    ( new InputStreamReader(socket.getInputStream()) );
            }

            // Wait for a packet.
            // N.B. I can't get blocking to work so I'm going to poll.
            while ( !in.ready() ) {
                wait(100);
            }

            // Read the entire transmission.
            while ( in.ready() ) {
                // Read and store the character.
                final int inChar = in.read();
                buf.append( (char) inChar );
            }

            /* BEGIN Debug code BEGIN **
                // Write the character to the test file.
                test.write( inChar );
            }

            // Finish off the file.
            test.flush();
            test.close();
            /*  END  Debug code  END  */

            // Now decode the packet from the XML.
            ParsedXML root = TinyParser.parseXML 
                ( new ByteArrayInputStream(buf.toString().getBytes()) );
            Enumeration rootChildren = root.elements();
            if ( !rootChildren.hasMoreElements() ) {
                throw new ParseException( "No children of the root." );
            }
            ParsedXML rootNode = (ParsedXML)rootChildren.nextElement();
            Packet packet = PacketEncoder.decode( rootNode, null );
            return packet;
        } catch (IOException ex) {
            System.err.print( "server(" );
            System.err.print( getId() );
            System.err.println( "): IO error reading command" );
            server.disconnected(this);
            return null;
        } catch (ParseException parseEx) {
            System.err.print( "server(" );
            System.err.print( getId() );
            System.err.println( "): Could not parse data" );
            parseEx.printStackTrace( System.err );
            return null;
            /* BEGIN Debug code BEGIN */
        } catch (InterruptedException intEx) {
            System.err.print( "server(" );
            System.err.print( getId() );
            System.err.println( "): Interrupted waiting for data" );
            server.disconnected(this);
            return null;
            /*  END  Debug code  END  */
        }

    }

    /**
     * Sends a packet!
     * <p/>
     * Subclasses are encouraged to override this method.
     *
     * @param   packet - the <code>Packet</code> to be sent.
     * @return  the <code>int</code> number of bytes sent.
     */
    protected int sendPacket(Packet packet) {
        int bytes = 0;
        int startCount = 0;
        try {
            if (out == null) {
                counter = new DataOutputStream( socket.getOutputStream() );
                out = new BufferedWriter( new OutputStreamWriter(counter) );
            }

            // Get the starting counter
            startCount = counter.size();

            // Encode the packet to the connection.
            PacketEncoder.encode( packet, out );

            // Finish off the packet.
            out.flush();
            bytes = counter.size() - startCount;
            /* BEGIN debug code **
            System.out.print( "server(" );
            System.out.print( getId() );
            System.out.print( "): command #" );
            System.out.print( packet.getCommand() );
            System.out.print( " sent with " );
            System.out.print( bytes );
            System.out.println( " bytes data");
            /*  END  debug code */
        } catch(IOException ex) {
            System.err.print( "server(" );
            System.err.print( getId() );
            System.err.print( "): error sending command.  dropping player" );
            System.err.println(ex);
            System.err.println(ex.getMessage());
            server.disconnected(this);
        }
        return bytes;
    }

    /**
     * Initialize this XML-based connection.
     */
    public XmlConnection(ConnectionHandler server, Socket socket, int id) {
        super( server, socket, id );
    }

}
