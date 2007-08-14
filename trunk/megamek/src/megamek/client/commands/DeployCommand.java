/**
 * 
 */
package megamek.client.commands;

import megamek.client.Client;

/**
 * @author dirk
 *
 */
public class DeployCommand extends ClientCommand {

    /**
     * @param client
     * @param name
     * @param helpText
     */
    public DeployCommand(Client client) {
        super(client, "deploy", "This command deploys a given unit to the specified hex. Usage: '#deploy unit x y' where unit is the unit id number and x and y are the coordinates of the hex.");
    }

    /* (non-Javadoc)
     * @see megamek.client.commands.ClientCommand#run(java.lang.String[])
     */
    @Override
    public String run(String[] args) {
        // TODO Auto-generated method stub
        return "Sorry this command is not yet implemented.";
    }

}
