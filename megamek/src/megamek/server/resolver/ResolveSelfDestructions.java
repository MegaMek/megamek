package megamek.server.resolver;

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Mech;
import megamek.common.Report;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

import java.util.Vector;

public class ResolveSelfDestructions {
    /*
     * Called during the weapons firing phase to initiate self destruction.
     */
    public static void resolveSelfDestructions(Server server, Game game) {
        Vector<Report> vDesc = new Vector<>();
        Report r;
        for (Entity e : game.getEntitiesVector()) {
            if (e.getSelfDestructInitiated() && e.hasEngine()) {
                r = new Report(6166, Report.PUBLIC);
                int target = e.getCrew().getPiloting();
                int roll = e.getCrew().rollPilotingSkill();
                r.subject = e.getId();
                r.addDesc(e);
                r.indent();
                r.add(target);
                r.add(roll);
                r.choose(roll >= target);
                vDesc.add(r);

                // Blow it up...
                if (roll >= target) {
                    int engineRating = e.getEngine().getRating();
                    r = new Report(5400, Report.PUBLIC);
                    r.subject = e.getId();
                    r.indent(2);
                    vDesc.add(r);

                    if (e instanceof Mech) {
                        Mech mech = (Mech) e;
                        if (mech.isAutoEject()
                                && (!game.getOptions().booleanOption(
                                        OptionsConstants.RPG_CONDITIONAL_EJECTION) || (game
                                        .getOptions().booleanOption(
                                                OptionsConstants.RPG_CONDITIONAL_EJECTION) && mech
                                        .isCondEjectEngine()))) {
                            vDesc.addAll(server.ejectEntity(e, true));
                        }
                    }
                    e.setSelfDestructedThisTurn(true);
                    server.doFusionEngineExplosion(engineRating, e.getPosition(),
                            vDesc, null);
                    Report.addNewline(vDesc);
                    r = new Report(5410, Report.PUBLIC);
                    r.subject = e.getId();
                    r.indent(2);
                    Report.addNewline(vDesc);
                    vDesc.add(r);
                }
                e.setSelfDestructInitiated(false);
            }
        }
        server.addReport(vDesc);
    }
}
