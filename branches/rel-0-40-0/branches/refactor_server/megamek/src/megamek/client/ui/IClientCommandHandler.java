/**
 * 
 */
package megamek.client.ui;

import java.util.Enumeration;

import megamek.client.commands.ClientCommand;

/**
 * @author dirk
 *
 */
public interface IClientCommandHandler {
    public ClientCommand getCommand(String name);
    public Enumeration<String> getAllCommandNames();
    public void registerCommand(ClientCommand command);

}
