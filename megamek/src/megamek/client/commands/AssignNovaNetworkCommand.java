/**
 * 
 */
package megamek.client.commands;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import megamek.client.Client;
import megamek.common.Entity;

/**
 * @author dirk 
 * This command exists to print entity information to the chat
 * window, it's primarily intended for vissually impaired users.
 */

public class AssignNovaNetworkCommand extends ClientCommand {

    public AssignNovaNetworkCommand(Client client) {
        super(
                client,
                "nova",
                "This command allows you to link NovaCEWS units." +
                "\nDo not use this command unless you can link something." +
                "\nCall #nova for detailed help.");
        /*
         * This command will change the nova net at end of turn.
         *  /nova print
         *  will print info about your current nova linksp    
         *  /nova print ID
         *  will print the network for ID
         *  /nova link ID ID 
         *  will link the two IDs into their own network. Unlinks from all other networks
         *  /nova link ID ID ID
         *  will link the 3 IDs in their own network. Unlinks from all other networks
         *  /nova unlink
         *  will unlink all
         *  /nova unlink ID
         *  will unlink ID
         * 
         */
    }

    /**
     * Run this command with the arguments supplied
     * 
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public String run(String[] args) {
        
        if (args.length == 1) {
            String help = "#nova print : will print all of your current nova networks and unlinked units.\n";
            help += "#nova print 5 : will print the network status for the Unit with ID 5.\n";
            help += "#nova link 5 6 : will link the units with ID 5 and 6.\n+++Will Disconnect them from all prior nets.\n";
            help += "#nova link 5 6 7 : will link the three units with ID 5 6 and 7.\n+++Will Disconnect them from all prior nets.\n";
            help += "#nova unlink : will unlink all your novaCEWS units.\n";
            help += "#nova unlink 5 : will unlink unit with ID 5 from all nova networks.\n";
            return help;
        }
        try {
            if (args.length > 1)
            {
                // we have a command!
                String cmd = args[1];
                if(cmd == "print")
                {
                    if(args.length > 2)
                    {
                        // do /nova print ID
                        int id = Integer.parseInt(args[2]);
                        return strListNetwork(id, true);
                    } else {
                        // do /nova print
                        return strListNetworks(true);
                    }
                    
                } else if(cmd == "link")
                {
                    if(args.length > 4)
                    {
                        int id1 = Integer.parseInt(args[2]);
                        int id2 = Integer.parseInt(args[3]);
                        int id3 = Integer.parseInt(args[4]);
                        // do /nova link ID ID ID
                        return strLink3(id1, id2, id3);
                    } else if (args.length > 3){
                        // do /nova link ID ID
                        int id1 = Integer.parseInt(args[2]);
                        int id2 = Integer.parseInt(args[3]);
                        return strLink2(id1, id2);
                    }
                }
                else if(cmd == "unlink")
                {
                    if(args.length > 2)
                    {
                        // do /nova unlink ID
                        int id = Integer.parseInt(args[2]);
                        return strUnlinkID(id);
                    } else {
                        // do /nova unlink
                        return strUnlinkAll();
                    }
                }
                return "Unknown command. #nova for help.\n";
            }
            // delete this!
        } catch (NumberFormatException nfe) {
        } catch (NullPointerException npe) {
        } catch (IndexOutOfBoundsException ioobe) {
        }

        return "Error parsing the command.";
    }
    
    private void setNewNetworkID(Entity ent, String net)
    {
        ent.setNewRoundNovaNetworkString(net);       
        client.sendNovaChange(ent.getId(), net);
    }
    

    
    private String strLink3(int id1, int id2, int id3)
    {
        String rval = "";
        
        Entity ent1 = client.getEntity(id1);
        Entity ent2 = client.getEntity(id2);
        Entity ent3 = client.getEntity(id3);
        
        if(ent1 == null || ent2 == null || ent3 == null)
        {
            return "ID Mismatch!\n";
        }
        
        rval += strUnlinkID(id1);
        rval += strUnlinkID(id2);
        rval += strUnlinkID(id3);
        
        setNewNetworkID(ent2, ent1.getNewRoundNovaNetworkString());
        setNewNetworkID(ent3, ent1.getNewRoundNovaNetworkString());
        
        return rval+"New Network! Linked Units: "+id1+", "+id2+", "+id3+"\n";
    }
    
    private String strLink2(int id1, int id2)
    {
        String rval = "";
        
        Entity ent1 = client.getEntity(id1);
        Entity ent2 = client.getEntity(id2);
        if(ent1 == null || ent2 == null)
        {
            return "ID Mismatch!\n";
        }
        rval += strUnlinkID(id1);
        rval += strUnlinkID(id2);
        setNewNetworkID(ent2, ent1.getNewRoundNovaNetworkString());
        
        return rval+"New Network! Linked Units: "+id1+", "+id2+"\n";
    }
    
    private String strUnlinkID(int id)
    {
        Entity ent = client.getEntity(id);
        
        if(ent == null)
        {
            return "ID Mismatch\n";
        }
        
        List<Entity> network = listNetwork(ent, true);
        if(network.size()<2)
        {
            // no other member, we're done.
            return "Unit "+id+" was allready unlinked\n";
        }
        // there are other members in that network. Need to find a different ID for them.
        // first remove the unit from the network list.
        for(Iterator<Entity> i = network.iterator(); i.hasNext();)
        {
            Entity e = i.next();
            if(e.getId() == id)
            {
                i.remove();
            }
        }
        // now set the network ID of the remaining units to something different.
        String newID = network.get(0).getOriginalNovaC3NetId(); // this resets the C3i network name to the default 'Nova.ID'
        for(Entity e : network)
        {
            setNewNetworkID(e,newID);
        }
        // finally set the unlinked units network ID to default value.
        setNewNetworkID(ent, ent.getOriginalNovaC3NetId());
        return "Unit "+id+" unlinked\n";
    }
    
    private String strUnlinkAll()
    {
        List<Entity> novaUnits = getMyNovaUnits();
        for (Entity e : novaUnits)
        {
            setNewNetworkID(e, e.getOriginalNovaC3NetId());
        }
        return "Everything unlinked";
    }
    
    
    private String strListNetworks(boolean planned)
    {
        String rval = "";

        List<Integer> allreadyReported = new LinkedList<Integer>();
        List<Entity> novaUnits = getMyNovaUnits();
        List<Entity> network;
        
        for(Entity ent : novaUnits)
        {
            if(!allreadyReported.contains(ent.getId()))
            {
                network = listNetwork(ent, planned);
                if(network.size()>1) // we actually have more than one member in this network
                {
                    rval += "Network ID '"+ent.getC3NetId()+"' contains:\n";
                    for(Entity re : network)
                    {
                        rval += "+ "+re.getId()+" "+re.getDisplayName()+"\n";
                        allreadyReported.add(re.getId());
                    }
                    rval += "+-----\n";
                } else {
                    
                }
            }
            
        }
        if (rval == "")
        {
            // no networks found
            rval = "No Networks found. Create some with the #nova command\n";
        }
        
        if (planned)
        {
            rval = "Status for next turn networks:\n" + rval;
        } else {
            rval = "Status for current turn networks:\n" + rval;
        }
        return rval;
    }
        
    private String strListNetwork(int id, boolean planned)
    {
        String rval = "";
        Entity ent = client.getEntity(id);
        if(ent != null)
        {
            for(Entity e : listNetwork(ent,planned))
            {
                rval += "+ "+e.getId()+" "+e.getDisplayName()+"\n";
            }
        }
        
        if(rval != "")
        {
            rval = "Unit "+id+" is in the Network consisting of:\n";
        } else {
            rval = "Error. No ID match.\n";
        }
        
        return rval;
    }
    
    /**
     * Returns a list with all members of e 's nova network, including e.
     * @param e the entity.
     * @param planned set this to true if you want to calculate based on next turns net.
     * @return
     */
    private List<Entity> listNetwork(Entity e, boolean planned)
    {
        List<Entity> novaNetworkMembers = new LinkedList<Entity>();
        List<Entity> novaUnits = getMyNovaUnits();
        
        for (Entity ent : novaUnits)
        {    if(planned)
        {
            if(ent.getNewRoundNovaNetworkString() == e.getNewRoundNovaNetworkString())
            {
                novaNetworkMembers.add(ent);
            }
        }else {
            if(ent.getC3NetId() == e.getC3NetId())
            {
                novaNetworkMembers.add(ent);
            }
        }
        }
        return novaNetworkMembers;
    }
    
    /**
     * Return a list of all nova CEWS units the clients player owns.
     * @return
     */
    private List<Entity> getMyNovaUnits()
    {
        List<Entity> novaUnits = new LinkedList<Entity>();
        for (Entity ent : client.getEntitiesVector()) {
            if(ent.getOwnerId() == client.getLocalPlayer().getId() && ent.hasNovaCEWS())
            {
                novaUnits.add(ent);
            }
        }
        return novaUnits;
    }

}
