package megamek.server.resolver;

import megamek.common.*;
import megamek.common.actions.BreakGrappleAttackAction;
import megamek.common.actions.GrappleAttackAction;
import megamek.server.Server;

public class ResolverBreakGrappleAttack {
    /**
     * Handle a break grapple attack
     */
    public static void resolveBreakGrappleAttack(Server server, PhysicalResult pr, int lastEntityId) {
        final BreakGrappleAttackAction paa = (BreakGrappleAttackAction) pr.aaa;
        final Entity ae = server.getGame().getEntity(paa.getEntityId());
        // PLEASE NOTE: buildings are *never* the target of a "push".
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

        r = new Report(4305);
        r.subject = ae.getId();
        r.indent();
        r.addDesc(te);
        r.newlines = 0;
        server.addReport(r);

        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(4310);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            server.addReport(r);
            if (ae instanceof LandAirMech && ae.isAirborneVTOLorWIGE()) {
                server.getGame().addControlRoll(new PilotingRollData(ae.getId(), 0, "missed a physical attack"));
            }
            return;
        }

        if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            r = new Report(4320);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
        } else {
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
                if (ae instanceof LandAirMech && ae.isAirborneVTOLorWIGE()) {
                    server.getGame().addControlRoll(new PilotingRollData(ae.getId(), 0, "missed a physical attack"));
                }
                return;
            }

            // hit
            r = new Report(4040);
            r.subject = ae.getId();
        }
        server.addReport(r);

        // is there a counterattack?
        PhysicalResult targetGrappleResult = server.getPhysicalResults().stream().filter(tpr -> (tpr.aaa.getEntityId() == te.getId()) && (tpr.aaa instanceof GrappleAttackAction) && (tpr.aaa.getTargetId() == ae.getId())).findFirst().orElse(null);

        if (targetGrappleResult != null) {
            targetGrappleResult.pushBackResolved = true;
            // counterattack
            r = new Report(4315);
            r.subject = te.getId();
            r.newlines = 0;
            r.addDesc(te);
            server.addReport(r);

            // report the roll
            r = new Report(4025);
            r.subject = te.getId();
            r.add(targetGrappleResult.toHit);
            r.add(targetGrappleResult.roll);
            r.newlines = 0;
            server.addReport(r);

            // do we hit?
            if (roll < toHit.getValue()) {
                // miss
                r = new Report(4035);
                r.subject = ae.getId();
                server.addReport(r);
            } else {
                // hit
                r = new Report(4040);
                r.subject = ae.getId();
                server.addReport(r);

                // exchange attacker and defender
                ae.setGrappled(te.getId(), false);
                te.setGrappled(ae.getId(), true);

                return;
            }
        }

        // score the adjacent hexes
        Coords[] hexes = new Coords[6];
        int[] scores = new int[6];

        Hex curHex = server.getGame().getBoard().getHex(ae.getPosition());
        for (int i = 0; i < 6; i++) {
            hexes[i] = ae.getPosition().translated(i);
            scores[i] = 0;
            Hex hex = server.getGame().getBoard().getHex(hexes[i]);
            if (hex.containsTerrain(Terrains.MAGMA)) {
                scores[i] += 10;
            }

            if (hex.containsTerrain(Terrains.WATER)) {
                scores[i] += hex.terrainLevel(Terrains.WATER);
            }

            if ((curHex.getLevel() - hex.getLevel()) >= 2) {
                scores[i] += 2 * (curHex.getLevel() - hex.getLevel());
            }
        }

        int bestScore = 99999;
        int best = 0;
        int worstScore = -99999;
        int worst = 0;

        for (int i = 0; i < 6; i++) {
            if (bestScore > scores[i]) {
                best = i;
                bestScore = scores[i];
            }
            if (worstScore < scores[i]) {
                worst = i;
                worstScore = scores[i];
            }
        }

        // attacker doesn't fall, unless off a cliff
        if (ae.isGrappleAttacker()) {
            // move self to least dangerous hex
            PilotingRollData psr = ae.getBasePilotingRoll();
            psr.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "break grapple");
            server.addReport(server.doEntityDisplacement(ae, ae.getPosition(), hexes[best], psr));
            ae.setFacing(hexes[best].direction(te.getPosition()));
        } else {
            // move enemy to most dangerous hex
            PilotingRollData psr = te.getBasePilotingRoll();
            psr.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "break grapple");
            server.addReport(server.doEntityDisplacement(te, te.getPosition(), hexes[worst], psr));
            te.setFacing(hexes[worst].direction(ae.getPosition()));
        }

        // grapple is broken
        ae.setGrappled(Entity.NONE, false);
        te.setGrappled(Entity.NONE, false);

        server.addNewLines();
    }
}
