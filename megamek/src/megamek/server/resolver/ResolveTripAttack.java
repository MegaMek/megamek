package megamek.server.resolver;

import megamek.common.*;
import megamek.common.actions.TripAttackAction;
import megamek.server.Server;

public class ResolveTripAttack {
    /**
     * Handle a trip attack
     */
    public static void resolveTripAttack(Server server, PhysicalResult pr, int lastEntityId) {
        final TripAttackAction paa = (TripAttackAction) pr.aaa;
        final Entity ae = server.getGame().getEntity(paa.getEntityId());
        // PLEASE NOTE: buildings are *never* the target of a "trip".
        final Entity te = server.getGame().getEntity(paa.getTargetId());
        // get roll and ToHitData from the PhysicalResult
        int roll = pr.roll;
        final ToHitData toHit = pr.toHit;
        Report r;

        if (lastEntityId != paa.getEntityId()) {
            // who is making the attack
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            server.addReport(r);
        }

        r = new Report(4280);
        r.subject = ae.getId();
        r.indent();
        r.addDesc(te);
        r.newlines = 0;
        server.addReport(r);

        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(4285);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            server.addReport(r);
            return;
        }

        // report the roll
        r = new Report(4025);
        r.subject = ae.getId();
        r.add(toHit);
        r.add(roll);
        r.newlines = 0;
        server.addReport(r);

        // do we hit?
        if (roll < toHit.getValue()) {
            // miss
            r = new Report(4035);
            r.subject = ae.getId();
            server.addReport(r);
            return;
        }

        // we hit...
        if (te.canFall()) {
            PilotingRollData pushPRD = server.getKickPushPSR(te, te, "was tripped");
            server.getGame().addPSR(pushPRD);
        }

        r = new Report(4040);
        r.subject = ae.getId();
        server.addReport(r);
        server.addNewLines();
        // if the target is an industrial mech, it needs to check for crits at the end of turn
        if ((te instanceof Mech) && ((Mech) te).isIndustrial()) {
            ((Mech) te).setCheckForCrit(true);
        }
    }
}
