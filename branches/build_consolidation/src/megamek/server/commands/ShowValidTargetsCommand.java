package megamek.server.commands;

import java.util.Vector;

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
            int id = Integer.parseInt(args[1]);
            Entity ent = server.getGame().getEntity(id);

            if (ent != null) {
                String str = "No valid targets.";
                boolean canHit = false;
                ToHitData thd;

                Vector<Entity> entList = server.getGame().getValidTargets(ent);
                Entity target;

                for (int i = 0; i < entList.size(); i++) {
                    target = entList.get(i);
                    thd = LosEffects.calculateLos(server.getGame(), id, target)
                            .losModifiers(server.getGame());
                    if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
                        thd.setSideTable(target.sideTable(ent.getPosition()));

                        if (!canHit) {
                            str = "This entity(" + id
                                    + ") can shoot the following entities: \n";
                            canHit = true;
                        }
                        str = str + entList.get(i).getId()
                                + " at a to hit penalty of ";
                        str = str
                                + thd.getValue()
                                + ", at range " + ent.getPosition().distance(entList.get(i).getPosition()) + thd.getTableDesc() + ";\n"; //$NON-NLS-1$
                    }

                }

                server.sendServerChat(connId, str);
            } else {
                server.sendServerChat(connId, "No such entity.");
            }
        } catch (NumberFormatException nfe) {
        } catch (NullPointerException npe) {
        } catch (IndexOutOfBoundsException ioobe) {
        }
    }
}
