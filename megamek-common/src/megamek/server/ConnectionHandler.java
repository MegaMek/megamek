/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *     Nicholas Walczak (walczak@cs.umn.edu)
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
package megamek.server;

import megamek.common.net.IConnection;

/**
 * Thread that runs and checks to see if there's any incoming packets from a 
 * connection.
 * 
 * @author arlith
 *
 */
public class ConnectionHandler implements Runnable {

    IConnection connection;
    
    boolean shouldStop = false;
    
    ConnectionHandler(IConnection c){
        connection = c;
    }
    
    /**
     * Called when the IConnection disconnects and signals the thread to stop.
     */
    public void signalStop(){
        shouldStop = true;
    }
    
    @Override
    public void run() {
        while (!shouldStop){
            // Write out any queued packets
            connection.flush();
            // Wait for input
            connection.update();            
            if (connection.isClosed()){
                shouldStop = true;
            }
        }        
    }

}
