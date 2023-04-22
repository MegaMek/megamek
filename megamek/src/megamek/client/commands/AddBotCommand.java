/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.commands;

import megamek.client.TwClient;
import megamek.common.util.AddBotUtil;

/**
 * @author dirk
 */
public class AddBotCommand extends ClientCommand {
    /**
     * @param client the client this command will be registered to.
     */
    public AddBotCommand(TwClient client) {
        super(client, AddBotUtil.COMMAND, AddBotUtil.USAGE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public String run(String[] args) {
        return new AddBotUtil().addBot(args, getClient().getGame(), getClient().getHost(), getClient().getPort());
    }
}