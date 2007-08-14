/**
 * 
 */
package megamek.client.commands;

import java.util.Enumeration;

import megamek.client.Client;
import megamek.client.ui.IClientCommandHandler;

/**
 * @author dirk
 *
 */
public class HelpCommand extends ClientCommand {
    
    private IClientCommandHandler cmdHandler;
    
    /** Creates new HelpCommand */
    public HelpCommand(Client client, IClientCommandHandler cmdHandler) {
        super(client, "help", "Lists all of the commands available, or gives help on a specific command.  Usage: #help [command]");
        this.cmdHandler = cmdHandler;
    }
    
    public String run(String[] args) {
        if (args.length == 1) {
            // no args
            return "Type /help [command] for help on a specific command.  Commands available: " + commandList();
        } else {
            // argument
            ClientCommand command = cmdHandler.getCommand(args[1]);
            if (command == null) {
                return "Command \"" + args[1] + "\" not recognized.  Commands available: " + commandList();
            } else {
                return "/" + command.getName() + " : " + command.getHelp();
            }
        }
    }
    
    private String commandList() {
            StringBuffer commandList = new StringBuffer();

            for (Enumeration<String> i = cmdHandler.getAllCommandNames(); i.hasMoreElements();) {
                ClientCommand command = cmdHandler.getCommand(i.nextElement());
                if (commandList.length() > 0) {
                    commandList.append(", ");
                }
                commandList.append(command.getName());
            }

            return commandList.toString();
    }
}
