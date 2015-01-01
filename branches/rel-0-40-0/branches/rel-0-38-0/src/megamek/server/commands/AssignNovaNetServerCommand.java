/**
 *
 */
package megamek.server.commands;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.server.Server;

/**
 * @author dirk This command exists to print entity information to the chat
 *         window, it's primarily intended for vissually impaired users.
 */

public class AssignNovaNetServerCommand extends ServerCommand {

    public AssignNovaNetServerCommand(Server server) {
        super(
                server,
                "nova",
                "This command allows you to link NovaCEWS units.\nDo not use this command unless you can link something.\nCall #nova for detailed help.");
        /*
         * This command will change the nova net at end of turn. /nova print
         * will print info about your current nova linksp /nova print ID will
         * print the network for ID /nova link ID ID will link the two IDs into
         * their own network. Unlinks from all other networks /nova link ID ID
         * ID will link the 3 IDs in their own network. Unlinks from all other
         * networks /nova unlink will unlink all /nova unlink ID will unlink ID
         */
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connID, String[] args) {

        if (args.length == 1) {
            server.sendServerChat(connID, "Server Side nova command");
            server.sendServerChat(
                    connID,
                    "/nova print : will print all of your current nova networks and unlinked units.");
            server.sendServerChat(connID,
                    "/nova print id : will print the network status for the Unit with ID id.");
            server.sendServerChat(connID,
                    "/nova link id1 id2 : will link the units with ID id1 and id2.");
            server.sendServerChat(connID,
                    "+++Will Disconnect them from all prior nets.");
            server.sendServerChat(connID,
                    "/nova link id1 id2 id3 : will link the three units with ID id1 id2 and id3.");
            server.sendServerChat(connID,
                    "+++Will Disconnect them from all prior nets.");
            server.sendServerChat(connID,
                    "/nova unlink : will unlink all your novaCEWS units.");
            server.sendServerChat(connID,
                    "/nova unlink id : will unlink unit with ID id from all nova networks.");
        }
        try {
            if (args.length > 1) {
                // we have a command!
                String cmd = args[1];
                if (cmd.equalsIgnoreCase("print")) {
                    if (args.length > 2) {
                        // do /nova print ID
                        int id = Integer.parseInt(args[2]);
                        server.sendServerChat(connID,
                                strListNetwork(connID, id, true));
                    } else {
                        // do /nova print
                        server.sendServerChat(connID,
                                strListNetworks(connID, true));
                    }

                } else if (cmd.equalsIgnoreCase("printcurrent")) {
                    if (args.length > 2) {
                        // do /nova print ID
                        int id = Integer.parseInt(args[2]);
                        server.sendServerChat(connID,
                                strListNetwork(connID, id, false));
                    } else {
                        // do /nova print
                        server.sendServerChat(connID,
                                strListNetworks(connID, false));
                    }

                } else if (cmd.equalsIgnoreCase("debug")) {
                    debug(connID, server);

                } else if (cmd.equalsIgnoreCase("link")) {
                    if (args.length > 4) {
                        int id1 = Integer.parseInt(args[2]);
                        int id2 = Integer.parseInt(args[3]);
                        int id3 = Integer.parseInt(args[4]);
                        // do /nova link ID ID ID
                        server.sendServerChat(connID,
                                strLink3(connID, id1, id2, id3));
                    } else if (args.length > 3) {
                        // do /nova link ID ID
                        int id1 = Integer.parseInt(args[2]);
                        int id2 = Integer.parseInt(args[3]);
                        server.sendServerChat(connID,
                                strLink2(connID, id1, id2));
                    }
                } else if (cmd.equalsIgnoreCase("unlink")) {
                    if (args.length > 2) {
                        // do /nova unlink ID
                        int id = Integer.parseInt(args[2]);
                        server.sendServerChat(connID, strUnlinkID(connID, id));
                    } else {
                        // do /nova unlink
                        server.sendServerChat(connID, strUnlinkAll(connID));
                    }
                } else {
                    String failstr = args[0];
                    for (int i = 1; i < args.length; i++) {
                        failstr += " " + args[i];
                    }
                    server.sendServerChat(connID, "I do not understand "
                            + failstr + ". /nova for help.\n");
                }
            }
        } catch (NumberFormatException nfe) {
            server.sendServerChat(connID, "Error parsing the command. NFE");
        } catch (NullPointerException npe) {
            server.sendServerChat(connID, "Error parsing the command. NPE");
        } catch (IndexOutOfBoundsException ioobe) {
            server.sendServerChat(connID, "Error parsing the command. IOOBE");
        }

    }

    private void debug(int connID, Server server) {
        // TODO Auto-generated method stub
        server.sendServerChat(connID, "Called /nova Debug");
        server.sendServerChat(connID,
                "Check if server really thinks that stuff is connected");
        List<Entity> novaUnits = getMyNovaUnits(connID);
        List<Entity> opponent = server.getGame().getEntitiesVector();
        for (Entity e : novaUnits) {
            List<Entity> curNetwork = listNetwork(connID, e, false);

            for (Entity t : curNetwork) {
                if (e.getId() != t.getId()) {
                    server.sendServerChat(connID, "Checking ID " + e.getId()
                            + " and " + t.getId());
                    if (!e.onSameC3NetworkAs(t)) {
                        server.sendServerChat(connID, "ID " + e.getId()
                                + " and " + t.getId()
                                + " network Error with ECM.");
                    }
                    if (!e.onSameC3NetworkAs(t, true)) {
                        server.sendServerChat(connID, "ID " + e.getId()
                                + " and " + t.getId()
                                + " network Error without ECM.");
                    }
                }
            }

            for (Entity t : opponent) {
                if (t.getOwnerId() != e.getOwnerId()) {
                    // we are hostile
                    Entity s = Compute.exposed_findC3Spotter(server.getGame(),
                            e, t);
                    if (s.getId() != e.getId()) {
                        server.sendServerChat(connID, "ID " + e.getId()
                                + " gets bonus from ID " + s.getId());
                    } else {
                        server.sendServerChat(connID, "ID " + e.getId()
                                + " does not have a good spotter");
                    }
                }
            }
        }

    }

    private void setNewNetworkID(int connID, Entity ent, String net) {
        ent.setNewRoundNovaNetworkString(net);
        // TODO: Send packet to client.

        server.send_Nova_Change(ent.getId(), net);
    }

    private String strLink3(int connID, int id1, int id2, int id3) {
        String rval = "";

        Entity ent1 = server.getGame().getEntity(id1);
        Entity ent2 = server.getGame().getEntity(id2);
        Entity ent3 = server.getGame().getEntity(id3);

        if ((ent1 == null) || (ent2 == null) || (ent3 == null)) {
            return "ID Mismatch!\n";
        }

        if (ent1.getOwnerId() != connID) {
            return "This unit doesn't belong to you!\n";
        }
        if (ent2.getOwnerId() != connID) {
            return "This unit doesn't belong to you!\n";
        }
        if (ent3.getOwnerId() != connID) {
            return "This unit doesn't belong to you!\n";
        }
        if (!ent1.hasActiveNovaCEWS()) {
            return "ID: " + id1 + " has no active nova CEWS. Aborting.";
        }
        if (!ent2.hasActiveNovaCEWS()) {
            return "ID: " + id2 + " has no active nova CEWS. Aborting.";
        }
        if (!ent3.hasActiveNovaCEWS()) {
            return "ID: " + id3 + " has no active nova CEWS. Aborting.";
        }
        rval += strUnlinkID(connID, id1);
        rval += strUnlinkID(connID, id2);
        rval += strUnlinkID(connID, id3);

        setNewNetworkID(connID, ent2, ent1.getNewRoundNovaNetworkString());
        setNewNetworkID(connID, ent3, ent1.getNewRoundNovaNetworkString());

        return rval + "New Network! Linked Units: " + id1 + ", " + id2 + ", "
                + id3 + "\n";
    }

    private String strLink2(int connID, int id1, int id2) {
        String rval = "";

        Entity ent1 = server.getGame().getEntity(id1);
        Entity ent2 = server.getGame().getEntity(id2);
        if ((ent1 == null) || (ent2 == null)) {
            return "ID Mismatch!\n";
        }
        if (ent1.getOwnerId() != connID) {
            return "This unit doesn't belong to you!\n";
        }
        if (ent2.getOwnerId() != connID) {
            return "This unit doesn't belong to you!\n";
        }
        if (!ent1.hasActiveNovaCEWS()) {
            return "ID: " + id1 + " has no active nova CEWS. Aborting.";
        }
        if (!ent2.hasActiveNovaCEWS()) {
            return "ID: " + id2 + " has no active nova CEWS. Aborting.";
        }

        rval += strUnlinkID(connID, id1);
        rval += strUnlinkID(connID, id2);
        setNewNetworkID(connID, ent2, ent1.getNewRoundNovaNetworkString());

        return rval + "New Network! Linked Units: " + id1 + ", " + id2 + "\n";
    }

    private String strUnlinkID(int connID, int id) {
        Entity ent = server.getGame().getEntity(id);

        if (ent == null) {
            return "ID Mismatch\n";
        }

        if (ent.getOwnerId() != connID) {
            return "This unit doesn't belong to you!\n";
        }

        List<Entity> network = listNetwork(connID, ent, true);
        if (network.size() < 2) {
            // no other member, we're done.
            return "Unit " + id + " was allready unlinked\n";
        }
        // there are other members in that network. Need to find a different ID
        // for them.
        // first remove the unit from the network list.
        for (Iterator<Entity> i = network.iterator(); i.hasNext();) {
            Entity e = i.next();
            if (e.getId() == id) {
                i.remove();
            }
        }
        // now set the network ID of the remaining units to something different.
        String newID = network.get(0).getOriginalNovaC3NetId(); // this resets
                                                                // the C3i
                                                                // network name
                                                                // to the
                                                                // default
                                                                // 'Nova.ID'
        for (Entity e : network) {
            setNewNetworkID(connID, e, newID);
        }
        // finally set the unlinked units network ID to default value.
        setNewNetworkID(connID, ent, ent.getOriginalNovaC3NetId());
        return "Unit " + id + " unlinked\n";
    }

    private String strUnlinkAll(int connID) {
        List<Entity> novaUnits = getMyNovaUnits(connID);
        for (Entity e : novaUnits) {
            setNewNetworkID(connID, e, e.getOriginalNovaC3NetId());
        }
        return "Everything unlinked";
    }

    private String strListNetworks(int connID, boolean planned) {
        String rval = "";

        List<Integer> allreadyReported = new LinkedList<Integer>();
        List<Entity> novaUnits = getMyNovaUnits(connID);
        List<Entity> network;

        String noLink = "";

        for (Entity ent : novaUnits) {
            if (!allreadyReported.contains(ent.getId())) {
                network = listNetwork(connID, ent, planned);
                if (network.size() > 1) // we actually have more than one member
                                        // in this network
                {
                    rval += "Network ID '" + ent.getC3NetId() + "' contains:\n";
                    for (Entity re : network) {
                        rval += "+ " + re.getId() + " " + re.getDisplayName()
                                + "\n";
                        allreadyReported.add(re.getId());
                    }
                    rval += "+-----\n";
                } else {
                    noLink += "+ " + ent.getId() + " " + ent.getDisplayName()
                            + "\n";
                    allreadyReported.add(ent.getId());
                }
            }

        }
        if (rval == "") {
            // no networks found
            rval = "No Networks found. Create some with the /nova command\n";
        }
        if (noLink != "") {
            // we have unlinked units.
            noLink = "XXX Unlinked Units:\n" + noLink;
        }
        if (planned) {
            rval = "Status for next turn networks:\n" + rval + noLink;
        } else {
            rval = "Status for current turn networks:\n" + rval + noLink;
        }
        return rval;
    }

    private String strListNetwork(int connID, int id, boolean planned) {
        String rval = "";
        Entity ent = server.getGame().getEntity(id);
        if (ent != null) {
            if (ent.getOwnerId() != connID) {
                return "This unit doesn't belong to you!\n";
            }
            for (Entity e : listNetwork(connID, ent, planned)) {
                rval += "+ " + e.getId() + " " + e.getDisplayName() + "\n";
            }
        }

        if (rval != "") {
            rval = "Unit " + id + " is in the Network consisting of:\n";
        } else {
            rval = "Error. No ID match.\n";
        }

        return rval;
    }

    /**
     * Returns a list with all members of e 's nova network, including e.
     *
     * @param e
     *            the entity.
     * @param planned
     *            set this to true if you want to calculate based on next turns
     *            net.
     * @return
     */
    private List<Entity> listNetwork(int connID, Entity e, boolean planned) {
        List<Entity> novaNetworkMembers = new LinkedList<Entity>();
        List<Entity> novaUnits = getMyNovaUnits(connID);

        for (Entity ent : novaUnits) {
            if (planned) {
                if (ent.getNewRoundNovaNetworkString() == e
                        .getNewRoundNovaNetworkString()) {
                    novaNetworkMembers.add(ent);
                }
            } else {
                if (ent.getC3NetId() == e.getC3NetId()) {
                    novaNetworkMembers.add(ent);
                }
            }
        }
        return novaNetworkMembers;
    }

    /**
     * Return a list of all nova CEWS units the clients player owns.
     *
     * @return
     */
    private List<Entity> getMyNovaUnits(int connID) {
        List<Entity> novaUnits = new LinkedList<Entity>();
        Enumeration<Entity> entities = server.getGame().getEntities();
        while (entities.hasMoreElements()) {
            Entity ent = entities.nextElement();
            if ((ent.getOwnerId() == connID) && ent.hasNovaCEWS()) {
                novaUnits.add(ent);
            }
        }
        return novaUnits;
    }

}
