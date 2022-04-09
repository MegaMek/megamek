package megamek.server.resolver;

import megamek.common.*;
import megamek.common.actions.PushAttackAction;
import megamek.server.Server;

public class ResolvePushAttack {
    /**
     * Handle a push attack
     */
    public static void resolvePushAttack(Server server, PhysicalResult pr, int lastEntityId) {
        final PushAttackAction paa = (PushAttackAction) pr.aaa;
        final Entity ae = server.getGame().getEntity(paa.getEntityId());
        // PLEASE NOTE: buildings are *never* the target of a "push".
        final Entity te = server.getGame().getEntity(paa.getTargetId());
        // get roll and ToHitData from the PhysicalResult
        int roll = pr.roll;
        final ToHitData toHit = pr.toHit;
        Report r;

        // was this push resolved earlier?
        if (pr.pushBackResolved) {
            return;
        }
        // don't try this one again
        pr.pushBackResolved = true;

        if (lastEntityId != paa.getEntityId()) {
            // who is making the attack
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            server.addReport(r);
        }

        r = new Report(4155);
        r.subject = ae.getId();
        r.indent();
        r.addDesc(te);
        r.newlines = 0;
        server.addReport(r);

        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(4160);
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

        // check if our target has a push against us, too, and get it
        PhysicalResult targetPushResult = null;
        for (PhysicalResult tpr : server.getPhysicalResults()) {
            if ((tpr.aaa.getEntityId() == te.getId()) && (tpr.aaa instanceof PushAttackAction)
                    && (tpr.aaa.getTargetId() == ae.getId())) {
                targetPushResult = tpr;
            }
        }

        // if our target has a push against us, and we are hitting, we need to resolve both now
        if ((targetPushResult != null) && !targetPushResult.pushBackResolved
            && (roll >= toHit.getValue())) {
            targetPushResult.pushBackResolved = true;
            // do they hit?
            if (targetPushResult.roll >= targetPushResult.toHit.getValue()) {
                r = new Report(4165);
                r.subject = ae.getId();
                r.addDesc(te);
                r.addDesc(te);
                r.addDesc(ae);
                r.add(targetPushResult.toHit);
                r.add(targetPushResult.roll);
                r.addDesc(ae);
                server.addReport(r);
                if (ae.canFall()) {
                    PilotingRollData pushPRD = server.getKickPushPSR(ae, te, "was pushed");
                    server.getGame().addPSR(pushPRD);
                } else if (ae instanceof LandAirMech && ae.isAirborneVTOLorWIGE()) {
                    server.getGame().addControlRoll(server.getKickPushPSR(ae, te, "was pushed"));
                }

                if (te.canFall()) {
                    PilotingRollData targetPushPRD = server.getKickPushPSR(te, te, "was pushed");
                    server.getGame().addPSR(targetPushPRD);
                } else if (ae instanceof LandAirMech && ae.isAirborneVTOLorWIGE()) {
                    server.getGame().addControlRoll(server.getKickPushPSR(te, te, "was pushed"));
                }
                return;
            }
            // report the miss
            r = new Report(4166);
            r.subject = ae.getId();
            r.addDesc(te);
            r.addDesc(ae);
            r.add(targetPushResult.toHit);
            r.add(targetPushResult.roll);
            server.addReport(r);
        }

        // do we hit?
        if (roll < toHit.getValue()) {
            // miss
            r = new Report(4035);
            r.subject = ae.getId();
            server.addReport(r);
            return;
        }

        // we hit...
        int direction = ae.getFacing();

        Coords src = te.getPosition();
        Coords dest = src.translated(direction);

        PilotingRollData pushPRD = server.getKickPushPSR(te, te, "was pushed");

        if (Compute.isValidDisplacement(server.getGame(), te.getId(), te.getPosition(), direction)) {
            r = new Report(4170);
            r.subject = ae.getId();
            r.newlines = 0;
            server.addReport(r);
            if (server.getGame().getBoard().contains(dest)) {
                r = new Report(4175);
                r.subject = ae.getId();
                r.add(dest.getBoardNum(), true);
            } else {
                // uh-oh, pushed off board
                r = new Report(4180);
                r.subject = ae.getId();
            }
            server.addReport(r);

            server.addReport(server.doEntityDisplacement(te, src, dest, pushPRD));

            // if push actually moved the target, attacker follows through
            if (!te.getPosition().equals(src)) {
                ae.setPosition(src);
            }
        } else {
            // target immovable
            r = new Report(4185);
            r.subject = ae.getId();
            server.addReport(r);
            if (te.canFall()) {
                server.getGame().addPSR(pushPRD);
            }
        }

        // if the target is an industrial mech, it needs to check for crits at the end of turn
        if ((te instanceof Mech) && ((Mech) te).isIndustrial()) {
            ((Mech) te).setCheckForCrit(true);
        }

        server.checkForSpikes(te, ae.rollHitLocation(ToHitData.HIT_PUNCH, Compute.targetSideTable(ae, te)).getLocation(),
                0, ae, Mech.LOC_LARM, Mech.LOC_RARM);

        server.addNewLines();
    }
}
