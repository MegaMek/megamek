package megamek.client.commands;

import megamek.client.Client;

/**
 * A command that exist purly fot the accesibility UI so that commands can be made as the default rather than chat.
 */
public class DoneCommand extends ClientCommand {

    public DoneCommand(Client client) {
        super(client, "done","equivalent of hitting the done button.");
    }


    @Override
    public String run(String[] args) {
        client.sendDone(true);
        return "done";
    }
}
