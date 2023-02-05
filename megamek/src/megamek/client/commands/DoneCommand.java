package megamek.client.commands;

import megamek.client.Client;
import megamek.client.TwGameClient;

/**
 * A command that exist purely for the accessibility UI so that commands can be made as the default rather than chat.
 */
public class DoneCommand extends ClientCommand {

    public DoneCommand(TwGameClient client) {
        super(client, "done", "equivalent of hitting the done button.");
    }

    @Override
    public String run(String[] args) {
        client.sendDone(true);
        return "done";
    }
}
