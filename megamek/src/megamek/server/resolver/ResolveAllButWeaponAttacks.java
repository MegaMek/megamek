package megamek.server.resolver;

import megamek.common.*;
import megamek.common.MovePath.MoveStepType;
import megamek.common.actions.*;
import megamek.server.Server;
import org.apache.logging.log4j.LogManager;

import java.util.Enumeration;
import java.util.Vector;

public class ResolveAllButWeaponAttacks {
    /**
     * Called during the weapons fire phase. Resolves anything other than
     * weapons fire that happens. Torso twists, for example.
     * @param server
     * @param game
     */
    public static void resolveAllButWeaponAttacks(Server server, Game game) {
        Vector<EntityAction> triggerPodActions = new Vector<>();
        // loop through actions and handle everything we expect except attacks
        for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements(); ) {
            EntityAction ea = i.nextElement();
            Entity entity = game.getEntity(ea.getEntityId());
            if (ea instanceof TorsoTwistAction) {
                TorsoTwistAction tta = (TorsoTwistAction) ea;
                if (entity.canChangeSecondaryFacing()) {
                    entity.setSecondaryFacing(tta.getFacing());
                    entity.postProcessFacingChange();
                }
            } else if (ea instanceof FlipArmsAction) {
                FlipArmsAction faa = (FlipArmsAction) ea;
                entity.setArmsFlipped(faa.getIsFlipped());
            } else if (ea instanceof FindClubAction) {
                ResolveFindClub.resolveFindClub(server, entity);
            } else if (ea instanceof UnjamAction) {
                ResolveUnjam.resolveUnjam(server, entity);
            } else if (ea instanceof ClearMinefieldAction) {
                ResolveClearMinefield.resolveClearMinefield(server, entity, ((ClearMinefieldAction) ea).getMinefield());
            } else if (ea instanceof TriggerAPPodAction) {
                TriggerAPPodAction tapa = (TriggerAPPodAction) ea;

                // Don't trigger the same pod twice.
                if (!triggerPodActions.contains(tapa)) {
                    server.triggerAPPod(entity, tapa.getPodId());
                    triggerPodActions.addElement(tapa);
                } else {
                    LogManager.getLogger().error("AP Pod #" + tapa.getPodId() + " on "
                            + entity.getDisplayName() + " was already triggered this round!!");
                }
            } else if (ea instanceof TriggerBPodAction) {
                TriggerBPodAction tba = (TriggerBPodAction) ea;

                // Don't trigger the same pod twice.
                if (!triggerPodActions.contains(tba)) {
                    server.triggerBPod(entity, tba.getPodId(), game.getEntity(tba.getTargetId()));
                    triggerPodActions.addElement(tba);
                } else {
                    LogManager.getLogger().error("B Pod #" + tba.getPodId() + " on "
                            + entity.getDisplayName() + " was already triggered this round!!");
                }
            } else if (ea instanceof SearchlightAttackAction) {
                SearchlightAttackAction saa = (SearchlightAttackAction) ea;
                server.addReport(saa.resolveAction(game));
            } else if (ea instanceof UnjamTurretAction) {
                if (entity instanceof Tank) {
                    ((Tank) entity).unjamTurret(((Tank) entity).getLocTurret());
                    ((Tank) entity).unjamTurret(((Tank) entity).getLocTurret2());
                    Report r = new Report(3033);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    server.addReport(r);
                } else {
                    LogManager.getLogger().error("Non-Tank tried to unjam turret");
                }
            } else if (ea instanceof RepairWeaponMalfunctionAction) {
                if (entity instanceof Tank) {
                    Mounted m = entity.getEquipment(((RepairWeaponMalfunctionAction) ea).getWeaponId());
                    m.setJammed(false);
                    ((Tank) entity).getJammedWeapons().remove(m);
                    Report r = new Report(3034);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    r.add(m.getName());
                    server.addReport(r);
                } else {
                    LogManager.getLogger().error("Non-Tank tried to repair weapon malfunction");
                }
            } else if (ea instanceof DisengageAction) {
                MovePath path = new MovePath(game, entity);
                path.addStep(MoveStepType.FLEE);
                server.addReport(server.processLeaveMap(path, false, -1));
            } else if (ea instanceof ActivateBloodStalkerAction) {
                ActivateBloodStalkerAction bloodStalkerAction = (ActivateBloodStalkerAction) ea;
                Entity target = game.getEntity(bloodStalkerAction.getTargetID());

                if ((entity != null) && (target != null)) {
                    game.getEntity(bloodStalkerAction.getEntityId())
                            .setBloodStalkerTarget(bloodStalkerAction.getTargetID());
                    Report r = new Report(10000);
                    r.subject = entity.getId();
                    r.add(entity.getDisplayName());
                    r.add(target.getDisplayName());
                    server.addReport(r);
                }
            }
        }
    }
}
