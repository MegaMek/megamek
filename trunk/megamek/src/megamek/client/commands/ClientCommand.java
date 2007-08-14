/**
 * 
 */
package megamek.client.commands;

import megamek.client.Client;

/**
 * @author dirk
 *
 */
public abstract class ClientCommand {    
    protected Client client;
    
    private String name;
    private String helpText;

    /** Creates new ServerCommand */
    public ClientCommand(Client client, String name, String helpText) {
        this.client = client;
        this.name = name;
        this.helpText = helpText;
    }
    
    
    /**
     * Return the string trigger for this command
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns some help text for this command
     */
    public String getHelp() {
        return helpText;
    }

    /**
     * Run this command with the arguments supplied
     */
    public abstract String run(String[] args);
}
