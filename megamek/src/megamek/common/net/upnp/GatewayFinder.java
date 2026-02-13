/*
 * Copyright (C) 2015 Federico Dossena (adolfintel.com).
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package megamek.common.net.upnp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.LinkedList;

/**
 * Original Code from https://github.com/adolfintel/WaifUPnP/. Added to Megamek as of Feb 2026 under the GPLv3 license.
 * Change made to package name. Thanks Federico Dossena
 *
 * @author Federico
 */
abstract class GatewayFinder {

    private static final String[] SEARCH_MESSAGES;

    static {
        LinkedList<String> m = new LinkedList<String>();
        for (String type : new String[] { "urn:schemas-upnp-org:device:InternetGatewayDevice:1",
                                          "urn:schemas-upnp-org:service:WANIPConnection:1",
                                          "urn:schemas-upnp-org:service:WANPPPConnection:1" }) {
            m.add("M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nST: "
                  + type
                  + "\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\n\r\n");
        }
        SEARCH_MESSAGES = m.toArray(new String[] {});
    }

    private class GatewayListener extends Thread {

        private Inet4Address ip;
        private String req;

        public GatewayListener(Inet4Address ip, String req) {
            setName("UPnP - Gateway Listener");
            this.ip = ip;
            this.req = req;
        }

        @Override
        public void run() {
            boolean foundgw = false;
            Gateway gw = null;
            try {
                byte[] req = this.req.getBytes();
                DatagramSocket s = new DatagramSocket(new InetSocketAddress(ip, 0));
                s.send(new DatagramPacket(req, req.length, new InetSocketAddress("239.255.255.250", 1900)));
                s.setSoTimeout(3000);
                for (; ; ) {
                    try {
                        DatagramPacket recv = new DatagramPacket(new byte[1536], 1536);
                        s.receive(recv);
                        gw = new Gateway(recv.getData(), ip, recv.getAddress());
                        String extIp = gw.getExternalIP();
                        if ((extIp != null)
                              && (!extIp.equalsIgnoreCase("0.0.0.0"))) { //Exclude gateways without an external IP
                            gatewayFound(gw);
                            foundgw = true;
                        }
                    } catch (SocketTimeoutException t) {
                        break;
                    } catch (Throwable t) {
                    }
                }
            } catch (Throwable t) {
            }
            if ((!foundgw) && (gw != null)) { //Pick the last GW if none have an external IP - internet not up yet??
                gatewayFound(gw);
            }
        }
    }

    private LinkedList<GatewayListener> listeners = new LinkedList<GatewayListener>();

    public GatewayFinder() {
        for (Inet4Address ip : getLocalIPs()) {
            for (String req : SEARCH_MESSAGES) {
                GatewayListener l = new GatewayListener(ip, req);
                l.start();
                listeners.add(l);
            }
        }
    }

    public boolean isSearching() {
        for (GatewayListener l : listeners) {
            if (l.isAlive()) {
                return true;
            }
        }
        return false;
    }

    public abstract void gatewayFound(Gateway g);

    private static Inet4Address[] getLocalIPs() {
        LinkedList<Inet4Address> ret = new LinkedList<Inet4Address>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                try {
                    NetworkInterface iface = ifaces.nextElement();
                    if (!iface.isUp() || iface.isLoopback() || iface.isVirtual() || iface.isPointToPoint()) {
                        continue;
                    }
                    Enumeration<InetAddress> addrs = iface.getInetAddresses();
                    if (addrs == null) {
                        continue;
                    }
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        if (addr instanceof Inet4Address) {
                            ret.add((Inet4Address) addr);
                        }
                    }
                } catch (Throwable t) {
                }
            }
        } catch (Throwable t) {
        }
        return ret.toArray(new Inet4Address[] {});
    }

}
