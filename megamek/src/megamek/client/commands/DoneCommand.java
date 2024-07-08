package megamek.client.commands;

import megamek.client.Client;
import megamek.client.ui.swing.ClientGUI;

/**
 * A command that exist purely for the accessibility UI so that commands can be made as the default rather than chat.
 */
public class DoneCommand extends ClientCommand {

    public DoneCommand(ClientGUI clientGUI) {
        super(clientGUI, "done", "equivalent of hitting the done button.");
    }

    @Override
    public String run(String[] args) {
        getClient().sendDone(true);
        return "done";
    }
}
