/**
 * 
 */
package megamek.server.commands;

import megamek.common.Entity;
import megamek.common.Player;
import megamek.common.Compute;
import megamek.server.Server;

import java.lang.StringBuffer;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Developer
 */

public class RCommand extends ServerCommand {

    public static final String[] rollCommands =
    {
        "init",
        "psr",
        "hit",
        "cluster",
        "location",
        "criticals",
        "critlocation",
        "physical",
        "punch",
        "kick",
        "consciousness",
        "wakeup",
        "heatshutdown",
        "heatrestart",
        "heatexplosion",
        "facing",
        "spotlight",
        "unjam"
    };
    
    public static final String[] rollGroupCommands =
    {
        "all",
        "agoac"
    };
    
    public static final String[] rollGroupDescriptions =
    {
        "All",
        "A Game of Armored Combat"
    };
    
    public static final int NO_GROUP = -1;
    public static final int ALL_ROLL_GROUPS = 0;
    public static final int AGOAC_ROLL_GROUP = 1;
    public static final int NUMBER_OF_ROLL_GROUPS = 2;
    
    public static final ArrayList<Integer>[] rollGroups = new ArrayList[]
    {
        new ArrayList<Integer>() {{             // ALL_ROLL_GROUPS
            add(Compute.INITIATIVE); 
            add(Compute.PILOTING);
            add(Compute.HIT);
            add(Compute.CLUSTER);
            add(Compute.HIT_LOCATION); 
            add(Compute.DETERMINE_CRITICALS);
            add(Compute.CRITICAL_LOCATION);
            add(Compute.PHYSICAL_ATTACK);
            add(Compute.PUNCH_LOCATION); 
            add(Compute.KICK_LOCATION);
            add(Compute.LOSE_CONSCIOUSNESS);
            add(Compute.REGAIN_CONSCIOUSNESS);
            add(Compute.HEAT_SHUTDOWN); 
            add(Compute.HEAT_RESTART);
            add(Compute.HEAT_AMMO_EXPLOSION);
            add(Compute.FACING_DIRECTION);
            add(Compute.SPOTLIGHT_HIT);
            add(Compute.UNJAM);
            }},
        
        new ArrayList<Integer>() {{             // AGOAC_ROLL_GROUP
            add(Compute.INITIATIVE); 
            add(Compute.PILOTING);
            add(Compute.HIT);
            add(Compute.CLUSTER);
            add(Compute.HIT_LOCATION); 
            add(Compute.DETERMINE_CRITICALS);
            add(Compute.CRITICAL_LOCATION);
            add(Compute.PHYSICAL_ATTACK);
            add(Compute.PUNCH_LOCATION); 
            add(Compute.KICK_LOCATION);
            add(Compute.LOSE_CONSCIOUSNESS);
            add(Compute.REGAIN_CONSCIOUSNESS);
            add(Compute.HEAT_SHUTDOWN); 
            add(Compute.HEAT_RESTART);
            add(Compute.HEAT_AMMO_EXPLOSION);
            add(Compute.FACING_DIRECTION);
            }}
    };
    
    public RCommand(Server server) {
        super(
                server,
                "r",
                "Roll input command. Input n amount of rolls to buffer of last requested manual roll. " +
                "Usage: /r <roll sum> <roll sum> ... <n times roll sum separated with space>");      
    }

    /**
     * Run this command with the arguments supplied
     * 
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
        Player p = server.getGame().getPlayer(connId);
        if(null == p) {
            return;
        }
        if (args.length == 1) {
            // no args
            server.sendServerChat(connId, "/r [args]: no args given");
        } else {
            try {   /* NumberFormatException */
                
                int type = Compute.NO_ROLL_TYPE;            
                for (int i = 0; i < Compute.NUMBER_OF_ROLL_TYPES; i++) {
                    if (args[1].equalsIgnoreCase(rollCommands[i])) {
                        type = i;
                    }
                }
                
                if (type != Compute.NO_ROLL_TYPE)
                {                
                    /* Roll toggle getter */
                    if (args.length == 3) {
                        if (Compute.GetRollToggle(type, Integer.parseInt(args[2]))) {
                            server.sendServerChat(connId, "/r id " + Integer.parseInt(args[2]) + ": " + Compute.rollDescriptions[type] + " roll: on");
                        }
                        else {
                            server.sendServerChat(connId, "/r id " + Integer.parseInt(args[2]) + ": " + Compute.rollDescriptions[type] + " roll: off");
                        }
                    }
    
                    /* Roll toggle setter */
                    if (args.length == 4) {
                        if (args[2].equalsIgnoreCase("on")) {
                            server.sendServerChat(connId, "/r id " + Integer.parseInt(args[3]) + ": " + Compute.rollDescriptions[type] + " roll set on");
                            Compute.SetRollToggle(type, Integer.parseInt(args[3]), true);
                        } else if (args[2].equalsIgnoreCase("off")) {
                            server.sendServerChat(connId, "/r id " + Integer.parseInt(args[3]) + ": " + Compute.rollDescriptions[type] + " roll set off");
                            Compute.SetRollToggle(type, Integer.parseInt(args[3]), false);
                        }
                    }
                    
                    return;
                }
                
                int group = NO_GROUP;
                for (int i = 0; i < NUMBER_OF_ROLL_GROUPS; i++) {
                    if (args[1].equalsIgnoreCase(rollGroupCommands[i])) {
                        group = i;
                    }
                }
                
                if (group != NO_GROUP)
                {
                    /* Roll group setter */
                    if (args.length == 5) {
                        if (args[2].equalsIgnoreCase("on")) {
                            server.sendServerChat(connId, "/r player " + Integer.parseInt(args[3]) + ", unit " + Integer.parseInt(args[4]) + ": " + rollGroupDescriptions[group] + " rolls set on");

                            for (int i = 0; i < rollGroups[group].size(); i++) {
                                if (rollGroups[group].get(i) == Compute.INITIATIVE) {
                                    Compute.SetRollToggle(rollGroups[group].get(i), Integer.parseInt(args[3]), true);
                                }
                                else {
                                    Compute.SetRollToggle(rollGroups[group].get(i), Integer.parseInt(args[4]), true);
                                }
                            }
                            
                        } else if (args[2].equalsIgnoreCase("off")) {
                            server.sendServerChat(connId, "/r player " + Integer.parseInt(args[3]) + " unit " + Integer.parseInt(args[4]) + ": " + rollGroupDescriptions[group] + " rolls set off");

                            for (int i = 0; i < rollGroups[group].size(); i++) {
                                if (rollGroups[group].get(i) == Compute.INITIATIVE) {
                                    Compute.SetRollToggle(rollGroups[group].get(i), Integer.parseInt(args[3]), false);
                                }
                                else {
                                    Compute.SetRollToggle(rollGroups[group].get(i), Integer.parseInt(args[4]), false);
                                }
                            }
                        }
                    }
                    
                    return;
                }
                
                if (args[1].equalsIgnoreCase("buffer"))
                {
                    /* Roll buffer getter */
                    if (args.length == 4) {
                        CopyOnWriteArrayList<Integer> rollBuffer = server.GetRollBuffer(Integer.parseInt(args[2]), Integer.parseInt(args[3]));
                        if (rollBuffer != null) {
                            server.sendServerChat(connId, "/r player " + Integer.parseInt(args[2]) + " " + Compute.rollDescriptions[Integer.parseInt(args[3])] + " roll buffer: " + rollBuffer);
                        }
                        else {
                            server.sendServerChat(connId, "/r player " + Integer.parseInt(args[2]) + " " + Compute.rollDescriptions[Integer.parseInt(args[3])] + " roll buffer empty");
                        }
                    }
                }
                if (args[1].equalsIgnoreCase("verbose"))
                {
                    /* Roll verbose mode setter */
                    if (args.length == 3) {
                        if (args[2].equalsIgnoreCase("on")) {
                            Compute.SetVerboseMode(true);
                            server.sendServerChat(connId, "/r verbose rolls on");
                        }
                        else if (args[2].equalsIgnoreCase("off")) {
                            Compute.SetVerboseMode(false);
                            server.sendServerChat(connId, "/r verbose rolls off");
                        }
                    }
                }
                if (args[1].equalsIgnoreCase("input"))
                {
                    /* Roll input player id setter */
                    if (args.length == 3) {
                        Compute.SetRollInputPlayer(Integer.parseInt(args[2]));
                        server.sendServerChat(connId, "/r roll input player set to " + Integer.parseInt(args[2]));
                    }
                }
                else if (args[1].equalsIgnoreCase("help") || args[1].equalsIgnoreCase("rolls") || args[1].equalsIgnoreCase("groups"))
                {
                    /* Print all roll commands and roll group commands */
                    
                    StringBuffer rollsSB = new StringBuffer();
                    String delimiter = "";
                    for(int i = 0; i < rollCommands.length; i++) {
                        rollsSB.append(delimiter);
                        rollsSB.append(rollCommands[i]);
                        delimiter = ", ";
                    }
                    
                    StringBuffer rollGroupsSB = new StringBuffer();
                    delimiter = "";
                    for(int i = 0; i < rollGroupCommands.length; i++) {
                        rollGroupsSB.append(delimiter);
                        rollGroupsSB.append(rollGroupCommands[i]);
                        delimiter = ", ";
                    }
                    
                    server.sendServerChat(connId, "/r roll commands: " + rollsSB.toString() + ". Roll groups: " + rollGroupsSB.toString());
                }
                else
                {
                    /* Roll buffer input */
                    for (int i = 1; i < args.length; i++) {
                        server.SetRollBufferInt(Compute.GetActiveRollType(), Compute.GetActiveRollPlayer(), Integer.parseInt(args[i]));
                    }
                }
            } catch(NumberFormatException e) {
            }
        }
    }
}
