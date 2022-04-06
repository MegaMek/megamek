package megamek.server.commands;

import megamek.common.Coords;
import megamek.common.LosEffects;
import megamek.common.LosEffects.AttackInfo;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.server.Server;

/**
 * This is the ruler for LOS stuff implemented in command line.
 * @author dirk
 */
public class RulerCommand extends ServerCommand {
    public RulerCommand(Server server) {
        super(server, "ruler",
                "Show Line of Sight (LOS) information between two points of the map. Usage: /ruler x1 y1 x2 y2 [elev1 [elev2]]. Where x1, y1 and x2, y2 are the coordinates of the tiles, and the optional elev numbers are the elevations of the targets over the terrain. If elev is not given 1 is assumed which is for standing mechs. Prone mechs and most other units are at elevation 0.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String... args) {
        try {
            int elev1 = 1, elev2 = 1;
            String toHit1 = "", toHit2 = "";

            Coords start = new Coords(Integer.parseInt(args[1]) - 1, Integer.parseInt(args[2]) - 1);
            Coords end = new Coords(Integer.parseInt(args[3]) - 1, Integer.parseInt(args[4]) - 1);
            if (args.length > 5) {
                try {
                    elev1 = Integer.parseInt(args[5]);
                } catch (NumberFormatException ignored) {
                    // leave at default value
                }

                if (args.length > 6) {
                    try {
                        elev1 = Integer.parseInt(args[6]);
                    } catch (NumberFormatException ignored) {
                        // leave at default value
                    }
                }
            }

            ToHitData thd = LosEffects.calculateLos(server.getGame(), buildAttackInfo(start, end, elev1, elev2))
                    .losModifiers(server.getGame());
            if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
                toHit1 = thd.getValue() + " because ";
            }
            toHit1 += thd.getDesc();

            thd = LosEffects.calculateLos(server.getGame(), buildAttackInfo(end, start, elev2, elev1))
                    .losModifiers(server.getGame());
            if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
                toHit2 = thd.getValue() + " because ";
            }
            toHit2 += thd.getDesc();

            server.sendServerChat(connId, String.format(
                    "The ToHit from hex (%d, %d) at elevation %d to (%d, %d) at elevation %d has a range of %d, a modifier of %s, and a return fire modifier of %s.",
                    (start.getX() + 1), (start.getY() + 1), elev1, (end.getX() + 1),
                    (end.getY() + 1), elev2, start.distance(end), toHit1, toHit2));
        } catch (Exception ignored) {

        }
    }

    /**
     * Build line of sight effects between coordinates c1 and c2 at height h1
     * and h2 respectively.
     *
     * @param c1 the source coordinates.
     * @param c2 the target coordinates.
     * @param h1 the height in the source tile that is being shot from.
     * @param h2 the height of the target tile to shoot for.
     * @return an attackInfo object that describes the applicable modifiers.
     */
    private AttackInfo buildAttackInfo(Coords c1, Coords c2, int h1, int h2) {
        AttackInfo ai = new AttackInfo();
        ai.attackPos = c1;
        ai.targetPos = c2;
        ai.attackHeight = h1;
        ai.targetHeight = h2;
        ai.attackAbsHeight = server.getGame().getBoard().getHex(c1).floor() + h1;
        ai.targetAbsHeight = server.getGame().getBoard().getHex(c2).floor() + h2;
        return ai;
    }
}
