package megamek.server.resolver;

import megamek.common.*;
import megamek.common.actions.GrappleAttackAction;
import megamek.server.Server;

public class ResolveGrappleAttack {
    /**
     * Handle a grapple attack
     */
    public static void resolveGrappleAttack(Server server, PhysicalResult pr, int lastEntityId) {
        resolveGrappleAttack(server, pr, lastEntityId, Entity.GRAPPLE_BOTH, Entity.GRAPPLE_BOTH);
    }

    /**
     * Resolves a grapple attack.
     *
     * @param server
     * @param pr            the result of a physical attack - this one specifically being a grapple
     * @param lastEntityId  the entity making the attack
     * @param aeGrappleSide
     *            The side that the attacker is grappling with. For normal
     *            grapples this will be both, for chain whip grapples this will
     *            be the arm with the chain whip in it.
     * @param teGrappleSide
     *            The that the the target is grappling with. For normal grapples
     *            this will be both, for chain whip grapples this will be the
     *            arm that is being whipped.
     */
    public static void resolveGrappleAttack(Server server, PhysicalResult pr, int lastEntityId, int aeGrappleSide, int teGrappleSide) {
        final GrappleAttackAction paa = (GrappleAttackAction) pr.aaa;
        final Entity ae = server.getGame().getEntity(paa.getEntityId());
        // PLEASE NOTE: buildings are *never* the target of a "push".
        final Entity te = server.getGame().getEntity(paa.getTargetId());
        // get roll and ToHitData from the PhysicalResult
        int roll = pr.roll;
        final ToHitData toHit = pr.toHit;
        Report r;

        // same method as push, for counterattacks
        if (pr.pushBackResolved) {
            return;
        }

        if ((te.getGrappled() != Entity.NONE) || (ae.getGrappled() != Entity.NONE)) {
            toHit.addModifier(TargetRoll.IMPOSSIBLE, "Already Grappled");
        }

        if (lastEntityId != paa.getEntityId()) {
            // who is making the attack
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            server.addReport(r);
        }

        r = new Report(4295);
        r.subject = ae.getId();
        r.indent();
        r.addDesc(te);
        r.newlines = 0;
        server.addReport(r);

        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(4300);
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
        ae.setGrappled(te.getId(), true);
        te.setGrappled(ae.getId(), false);
        ae.setGrappledThisRound(true);
        te.setGrappledThisRound(true);
        // For normal grapples, AE moves into targets hex.
        if (aeGrappleSide == Entity.GRAPPLE_BOTH) {
            Coords pos = te.getPosition();
            ae.setPosition(pos);
            ae.setElevation(te.getElevation());
            te.setFacing((ae.getFacing() + 3) % 6);
            server.addReport(server.doSetLocationsExposure(ae, server.getGame().getBoard().getHex(pos), false, ae.getElevation()));
        }

        ae.setGrappleSide(aeGrappleSide);
        te.setGrappleSide(teGrappleSide);

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
