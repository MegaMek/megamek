package megamek.server.resolver;

import megamek.common.*;
import megamek.common.actions.BrushOffAttackAction;
import megamek.server.Server;

public class ResolveBrushOffAttack {
    /**
     * Handle a brush off attack
     */
    public static void resolveBrushOffAttack(Server server, Game game, PhysicalResult pr, int lastEntityId) {
        final BrushOffAttackAction baa = (BrushOffAttackAction) pr.aaa;
        final Entity ae = game.getEntity(baa.getEntityId());
        // PLEASE NOTE: buildings are *never* the target
        // of a "brush off", but iNarc pods **are**.
        Targetable target = game.getTarget(baa.getTargetType(), baa.getTargetId());
        Entity te = null;
        final String armName = baa.getArm() == BrushOffAttackAction.LEFT ? "Left Arm" : "Right Arm";
        Report r;

        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = game.getEntity(baa.getTargetId());
        }

        // get damage, ToHitData and roll from the PhysicalResult
        // ASSUMPTION: buildings can't absorb *this* damage.
        int damage = baa.getArm() == BrushOffAttackAction.LEFT ? pr.damage : pr.damageRight;
        final ToHitData toHit = baa.getArm() == BrushOffAttackAction.LEFT ? pr.toHit : pr.toHitRight;
        int roll = baa.getArm() == BrushOffAttackAction.LEFT ? pr.roll : pr.rollRight;

        if (lastEntityId != baa.getEntityId()) {
            // who is making the attacks
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            server.addReport(r);
        }

        r = new Report(4085);
        r.subject = ae.getId();
        r.indent();
        r.add(target.getDisplayName());
        r.add(armName);
        r.newlines = 0;
        server.addReport(r);

        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(4090);
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

            // Missed Brush Off attacks cause punch damage to the attacker.
            toHit.setHitTable(ToHitData.HIT_PUNCH);
            toHit.setSideTable(ToHitData.SIDE_FRONT);
            HitData hit = ae.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
            hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
            r = new Report(4095);
            r.subject = ae.getId();
            r.addDesc(ae);
            r.add(ae.getLocationAbbr(hit));
            r.newlines = 0;
            server.addReport(r);
            server.addReport(server.damageEntity(ae, hit, damage));
            server.addNewLines();
            // if this is an industrial mech, it needs to check for crits
            // at the end of turn
            if ((ae instanceof Mech) && ((Mech) ae).isIndustrial()) {
                ((Mech) ae).setCheckForCrit(true);
            }
            return;
        }

        // Different target types get different handling.
        switch (target.getTargetType()) {
            case Targetable.TYPE_ENTITY:
                // Handle Entity targets.
                HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
                hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
                r = new Report(4045);
                r.subject = ae.getId();
                r.add(toHit.getTableDesc());
                r.add(te.getLocationAbbr(hit));
                server.addReport(r);
                server.addReport(server.damageEntity(te, hit, damage));
                server.addNewLines();

                // Dislodge the swarming infantry.
                ae.setSwarmAttackerId(Entity.NONE);
                te.setSwarmTargetId(Entity.NONE);
                r = new Report(4100);
                r.subject = ae.getId();
                r.add(te.getDisplayName());
                server.addReport(r);
                break;
            case Targetable.TYPE_INARC_POD:
                // Handle iNarc pod targets.
                // TODO : check the return code and handle false appropriately.
                ae.removeINarcPod((INarcPod) target);
                // // TODO : confirm that we don't need to update the attacker.
                r = new Report(4105);
                r.subject = ae.getId();
                r.add(target.getDisplayName());
                server.addReport(r);
                break;
            // TODO : add a default: case and handle it appropriately.
        }
    }
}
