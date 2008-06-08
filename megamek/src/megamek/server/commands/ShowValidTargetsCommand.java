package megamek.server.commands;

import megamek.common.Entity;
import megamek.common.LosEffects;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.server.Server;

public class ShowValidTargetsCommand extends ServerCommand {

    public ShowValidTargetsCommand(Server server) {
        super(
                server,
                "validTargets",
                "Shows a list of entity id's that are valid targets for the current entity. Usage: /validTargets # where # is the id number of the entity you are shooting from.");
    }

    @Override
    public void run(int connId, String[] args) {
        try {
            final int id = Integer.parseInt(args[1]);
            final Entity ent = server.getGame().getEntity(id);

            if (ent != null) {
                String str = "No valid targets.";
                boolean canHit = false;
                ToHitData thd;

                for (final Entity target : server.getGame().getValidTargets(ent)) {
                    thd = LosEffects.calculateLos(server.getGame(), id, target)
                            .losModifiers(server.getGame());
                    if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
                        thd.setSideTable(target.sideTable(ent.getPosition()));

                        if (!canHit) {
                            str = "This entity(" + id
                                    + ") can shoot the following entities: \n";
                            canHit = true;
                        }
                        str = str + target.getId()
                                + " at a to hit penalty of ";
                        str = str
                                + thd.getValue()
                                + ", at range " + ent.getPosition().distance(target.getPosition()) + thd.getTableDesc() + ";\n"; //$NON-NLS-1$
                    }

                }

                server.sendServerChat(connId, str);
            } else {
                server.sendServerChat(connId, "No such entity.");
            }
        } catch (final NumberFormatException nfe) {
        } catch (final NullPointerException npe) {
        } catch (final IndexOutOfBoundsException ioobe) {
        }
    }
}
