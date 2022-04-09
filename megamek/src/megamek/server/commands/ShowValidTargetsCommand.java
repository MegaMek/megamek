package megamek.server.commands;

import java.util.List;

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
    public void run(int connId, String[] args) throws NumberFormatException, NullPointerException, IndexOutOfBoundsException {
        int id = Integer.parseInt(args[1]);
        Entity ent = server.getGame().getEntity(id);

        if (ent != null) {
            StringBuilder str = new StringBuilder("No valid targets.");
            boolean canHit = false;
            ToHitData thd;

            List<Entity> entList = server.getGame().getValidTargets(ent);
            Entity target;

            for (Entity entity : entList) {
                target = entity;
                thd = LosEffects.calculateLOS(server.getGame(), ent, target).losModifiers(server.getGame());
                if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
                    thd.setSideTable(target.sideTable(ent.getPosition()));

                    if (!canHit) {
                        str = new StringBuilder("This entity(" + id + ") can shoot the following entities: \n");
                        canHit = true;
                    }
                    str.append(entity.getId()).append(" at a to hit penalty of ");
                    str.append(thd.getValue()).append(", at range ").append(ent.getPosition().distance(entity.getPosition())).append(thd.getTableDesc()).append(";\n");
                }

            }

            server.sendServerChat(connId, str.toString());
        } else {
            server.sendServerChat(connId, "No such entity.");
        }
    }
}
