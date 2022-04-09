package megamek.server.resolver;

import megamek.common.Entity;
import megamek.common.PhysicalResult;
import megamek.common.Report;
import megamek.common.actions.*;
import megamek.server.Server;

import java.util.Enumeration;

public class ResolvePhysicalAttacks {
    /**
     * Handle all physical attacks for the round
     * @param server
     */
    public static void resolvePhysicalAttacks(Server server) {
        // Physical phase header
        server.addReport(new Report(4000, Report.PUBLIC));

        // add any pending charges
        for (Enumeration<AttackAction> i = server.getGame().getCharges(); i.hasMoreElements(); ) {
            server.getGame().addAction(i.nextElement());
        }
        server.getGame().resetCharges();

        // add any pending rams
        for (Enumeration<AttackAction> i = server.getGame().getRams(); i.hasMoreElements(); ) {
            server.getGame().addAction(i.nextElement());
        }
        server.getGame().resetRams();

        // add any pending Tele Missile Attacks
        for (Enumeration<AttackAction> i = server.getGame().getTeleMissileAttacks(); i.hasMoreElements(); ) {
            server.getGame().addAction(i.nextElement());
        }
        server.getGame().resetTeleMissileAttacks();

        // remove any duplicate attack declarations
        server.cleanupPhysicalAttacks();

        // loop thru received attack actions
        for (Enumeration<EntityAction> i = server.getGame().getActions(); i.hasMoreElements(); ) {
            Object o = i.nextElement();
            // verify that the attacker is still active
            AttackAction aa = (AttackAction) o;
            if (!server.getGame().getEntity(aa.getEntityId()).isActive()
                && !(o instanceof DfaAttackAction)) {
                continue;
            }
            AbstractAttackAction aaa = (AbstractAttackAction) o;
            // do searchlights immediately
            if (aaa instanceof SearchlightAttackAction) {
                SearchlightAttackAction saa = (SearchlightAttackAction) aaa;
                server.addReport(saa.resolveAction(server.getGame()));
            } else {
                server.getPhysicalResults().addElement(server.preTreatPhysicalAttack(aaa));
            }
        }
        int cen = Entity.NONE;
        for (PhysicalResult pr : server.getPhysicalResults()) {
            server.resolvePhysicalAttack(pr, cen);
            cen = pr.aaa.getEntityId();
        }
        server.getPhysicalResults().removeAllElements();
    }
}
