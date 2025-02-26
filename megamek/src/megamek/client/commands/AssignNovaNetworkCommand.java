/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.commands;

import megamek.client.ui.swing.ClientGUI;
import megamek.common.Entity;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * This command exists to print entity information to the chat window, it's primarily intended for
 * visually impaired users.
 * This command will change the nova net at end of turn.
 * /nova print
 * will print info about your current nova links
 * /nova print ID
 * will print the network for ID
 * /nova link ID ID
 * will link the two IDs into their own network. Unlinks from all other networks
 * /nova link ID ID ID
 * will link the 3 IDs in their own network. Unlinks from all other networks
 * /nova unlink
 * will unlink all
 * /nova unlink ID
 * will unlink ID
 * @author dirk
 */
public class AssignNovaNetworkCommand extends ClientCommand {

    public AssignNovaNetworkCommand(ClientGUI clientGUI) {
        super(
                clientGUI,
                "nova",
                "This command allows you to link NovaCEWS units." +
                "\nDo not use this command unless you can link something." +
                "\nCall #nova for detailed help.");
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public String run(String[] args) {
        if (args.length <= 1) {
            String help = "#nova print : will print all of your current nova networks and unlinked units.\n";
            help += "#nova print 5 : will print the network status for the Unit with ID 5.\n";
            help += "#nova link 5 6 : will link the units with ID 5 and 6.\n+++Will Disconnect them from all prior nets.\n";
            help += "#nova link 5 6 7 : will link the three units with ID 5 6 and 7.\n+++Will Disconnect them from all prior nets.\n";
            help += "#nova unlink : will unlink all your novaCEWS units.\n";
            help += "#nova unlink 5 : will unlink unit with ID 5 from all nova networks.\n";
            return help;
        }

        try {
            switch (args[1]) {
                case "print":
                    if (args.length > 2) {
                        // do /nova print ID
                        int id = Integer.parseInt(args[2]);
                        return strListNetwork(id, true);
                    } else {
                        // do /nova print
                        return strListNetworks(true);
                    }
                case "link":
                    if (args.length > 4) {
                        int id1 = Integer.parseInt(args[2]);
                        int id2 = Integer.parseInt(args[3]);
                        int id3 = Integer.parseInt(args[4]);
                        // do /nova link ID ID ID
                        return strLink3(id1, id2, id3);
                    } else if (args.length > 3) {
                        // do /nova link ID ID
                        int id1 = Integer.parseInt(args[2]);
                        int id2 = Integer.parseInt(args[3]);
                        return strLink2(id1, id2);
                    }
                    break;
                case "unlink":
                    if (args.length > 2) {
                        // do /nova unlink ID
                        int id = Integer.parseInt(args[2]);
                        return strUnlinkID(id);
                    } else {
                        // do /nova unlink
                        return strUnlinkAll();
                    }
                default:
                    return "Unknown command. #nova for help.\n";
            }
        } catch (NumberFormatException | IndexOutOfBoundsException | NullPointerException ignored) {

        }

        return "Error parsing the command.";
    }

    private void setNewNetworkID(Entity ent, String net) {
        ent.setNewRoundNovaNetworkString(net);
        getClient().sendNovaChange(ent.getId(), net);
    }

    private String strLink3(int id1, int id2, int id3) {
        String returnValue = "";

        Entity ent1 = getClient().getEntity(id1);
        Entity ent2 = getClient().getEntity(id2);
        Entity ent3 = getClient().getEntity(id3);

        if ((ent1 == null) || (ent2 == null) || (ent3 == null)) {
            return "ID Mismatch!\n";
        }

        returnValue += strUnlinkID(id1);
        returnValue += strUnlinkID(id2);
        returnValue += strUnlinkID(id3);

        setNewNetworkID(ent2, ent1.getNewRoundNovaNetworkString());
        setNewNetworkID(ent3, ent1.getNewRoundNovaNetworkString());

        return returnValue + "New Network! Linked Units: " + id1 + ", " + id2 + ", " + id3 + "\n";
    }

    private String strLink2(int id1, int id2) {
        String returnValue = "";

        Entity ent1 = getClient().getEntity(id1);
        Entity ent2 = getClient().getEntity(id2);
        if ((ent1 == null) || (ent2 == null)) {
            return "ID Mismatch!\n";
        }
        returnValue += strUnlinkID(id1);
        returnValue += strUnlinkID(id2);
        setNewNetworkID(ent2, ent1.getNewRoundNovaNetworkString());

        return returnValue + "New Network! Linked Units: " + id1 + ", " + id2 + "\n";
    }

    private String strUnlinkID(int id) {
        Entity ent = getClient().getEntity(id);

        if (ent == null) {
            return "ID Mismatch\n";
        }

        List<Entity> network = listNetwork(ent, true);
        if (network.size() < 2) {
            // no other member, we're done.
            return "Unit " + id + " was already unlinked\n";
        }

        // there are other members in that network. Need to find a different ID for them.
        // first remove the unit from the network list.
        network.removeIf(e -> e.getId() == id);

        // now set the network ID of the remaining units to something different.
        String newID = network.get(0).getOriginalNovaC3NetId(); // this resets the C3i network name to the default 'Nova.ID'
        for (Entity e : network) {
            setNewNetworkID(e, newID);
        }
        // finally set the unlinked units network ID to default value.
        setNewNetworkID(ent, ent.getOriginalNovaC3NetId());
        return "Unit "+id+" unlinked\n";
    }

    private String strUnlinkAll() {
        List<Entity> novaUnits = getMyNovaUnits();
        for (Entity e : novaUnits) {
            strUnlinkID(e.getId());
        }
        return "Everything unlinked";
    }


    private String strListNetworks(boolean planned) {
        StringBuilder returnValue = new StringBuilder();

        List<Integer> allReadyReported = new LinkedList<>();
        List<Entity> novaUnits = getAlliedNovaUnits();
        List<Entity> network;

        for (Entity ent : novaUnits) {
            if (!allReadyReported.contains(ent.getId())) {
                network = listNetwork(ent, planned);
                if (network.size() > 1) {// we actually have more than one member in this network
                    returnValue.append("Network ID '").append(ent.getC3NetId()).append("' contains:\n");
                    for (Entity re : network)
                    {
                        returnValue.append("+ ").append(re.getId()).append(" ").append(re.getDisplayName()).append("\n");
                        allReadyReported.add(re.getId());
                    }
                    returnValue.append("+-----\n");
                }
            }

        }

        if (returnValue.toString().isBlank()) {
            // no networks found
            returnValue = new StringBuilder("No Networks found. Create some with the #nova command\n");
        }

        if (planned) {
            returnValue.insert(0, "Status for next turn networks:\n");
        } else {
            returnValue.insert(0, "Status for current turn networks:\n");
        }

        return returnValue.toString();
    }

    private String strListNetwork(int id, boolean planned) {
        StringBuilder returnValue = new StringBuilder();
        Entity ent = getClient().getEntity(id);
        if (ent != null) {
            for (Entity e : listNetwork(ent, planned)) {
                returnValue.append("+ ").append(e.getId()).append(" ").append(e.getDisplayName()).append("\n");
            }
        }

        if (returnValue.toString().isBlank()) {
            returnValue = new StringBuilder("Unit " + id + " is in the Network consisting of:\n");
        } else {
            returnValue = new StringBuilder("Error. No ID match.\n");
        }

        return returnValue.toString();
    }

    /**
     * Returns a list with all members of e 's nova network, including e.
     * @param e the entity.
     * @param planned set this to true if you want to calculate based on next turns net.
     * @return
     */
    private List<Entity> listNetwork(Entity e, boolean planned) {
        List<Entity> novaNetworkMembers = new LinkedList<>();
        List<Entity> novaUnits = getAlliedNovaUnits();

        for (Entity ent : novaUnits) {
            if (planned) {
                if (Objects.equals(ent.getNewRoundNovaNetworkString(), e.getNewRoundNovaNetworkString())) {
                    novaNetworkMembers.add(ent);
                }
            } else {
                if (Objects.equals(ent.getC3NetId(), e.getC3NetId())) {
                    novaNetworkMembers.add(ent);
                }
            }
        }
        return novaNetworkMembers;
    }

    /**
     * @return a list of all nova CEWS units the client's player owns.
     */
    private List<Entity> getMyNovaUnits() {
        List<Entity> novaUnits = new LinkedList<>();
        for (Entity ent : getClient().getEntitiesVector()) {
            if ((ent.getOwnerId() == getClient().getLocalPlayer().getId()) && ent.hasNovaCEWS()) {
                novaUnits.add(ent);
            }
        }
        return novaUnits;
    }

    /**
     * @return a list of all nova CEWS units the clients could connect with.
     */
    private List<Entity> getAlliedNovaUnits() {
        List<Entity> novaUnits = new LinkedList<>();
        for (Entity ent : getClient().getEntitiesVector()) {
            if (ent.hasNovaCEWS()
                && ((ent.getOwnerId() == getClient().getLocalPlayer().getId())
                    || (getClient().getLocalPlayer() != null
                        && !getClient().getLocalPlayer().isEnemyOf(ent.getOwner())))){
                novaUnits.add(ent);
            }
        }
        return novaUnits;
    }
}
