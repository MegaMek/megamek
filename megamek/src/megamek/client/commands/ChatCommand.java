package megamek.client.commands;

import megamek.client.Client;

/**
 * A command that exist purly fot the accesibility UI so that commands can be made as the default rather than chat.
 */
public class ChatCommand extends ClientCommand {

    public ChatCommand(Client client) {
        super(client, "say","say <message>, sends message to chat.");
    }


    @Override
    public String run(String[] args) {
        //rejoin the strign but cut off the say at the begining.
        String str = String.join(" ", args).substring(4);
        client.sendChat(str);

        return str;
    }
}
