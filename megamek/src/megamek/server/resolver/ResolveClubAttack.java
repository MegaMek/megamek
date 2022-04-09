package megamek.server.resolver;

import megamek.common.*;
import megamek.common.actions.ClubAttackAction;
import megamek.common.actions.GrappleAttackAction;
import megamek.common.enums.DamageType;
import megamek.common.options.OptionsConstants;
import megamek.server.DamageEntityControl;
import megamek.server.Server;

import java.util.Vector;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class ResolveClubAttack {
    /**
     * Handle a club attack
     */
    public static void resolveClubAttack(Server server, PhysicalResult pr, int lastEntityId) {
        final ClubAttackAction caa = (ClubAttackAction) pr.aaa;
        final Entity ae = server.getGame().getEntity(caa.getEntityId());
        // get damage, ToHitData and roll from the PhysicalResult
        int damage = pr.damage;
        // LAMs in airmech mode do half damage if airborne.
        if (ae.isAirborneVTOLorWIGE()) {
            damage = (int) ceil(damage * 0.5);
        }
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        final Targetable target = server.getGame().getTarget(caa.getTargetType(), caa.getTargetId());
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }
        boolean throughFront = true;
        if (te != null) {
            throughFront = Compute
                    .isThroughFrontHex(server.getGame(), ae.getPosition(), te);
        }
        final boolean targetInBuilding = Compute.isInBuilding(server.getGame(), te);
        final boolean glancing = server.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS)
                && (roll == toHit.getValue());

        // Set Margin of Success/Failure.
        // Make sure the MoS is zero for *automatic* hits in case direct blows
        // are in force.
        toHit.setMoS((roll == Integer.MAX_VALUE) ? 0 : roll - max(2, toHit.getValue()));
        final boolean directBlow = server.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                && ((toHit.getMoS() / 3) >= 1);

        Report r;

        // Which building takes the damage?
        Building bldg = server.getGame().getBoard().getBuildingAt(target.getPosition());

        // restore club attack
        caa.getClub().restore();

        // Shield bash causes 1 point of damage to the shield
        if (((MiscType) caa.getClub().getType()).isShield()) {
            ((Mech) ae).shieldAbsorptionDamage(1, caa.getClub().getLocation(), false);
        }

        if (lastEntityId != caa.getEntityId()) {
            // who is making the attacks
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            server.addReport(r);
        }

        r = new Report(4145);
        r.subject = ae.getId();
        r.indent();
        r.add(caa.getClub().getName());
        r.add(target.getDisplayName());
        r.newlines = 0;
        server.addReport(r);

        // Flail/Wrecking Ball auto misses on a 2 and hits themself.
        if ((caa.getClub().getType().hasSubType(MiscType.S_FLAIL)
                || caa.getClub().getType().hasSubType(MiscType.S_WRECKING_BALL)) && (roll == 2)) {
            // miss
            r = new Report(4035);
            r.subject = ae.getId();
            server.addReport(r);
            ToHitData newToHit = new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, "hit with own flail/wrecking ball");
            pr.damage = ClubAttackAction.getDamageFor(ae, caa.getClub(), false, caa.isZweihandering());
            pr.damage = (pr.damage / 2) + (pr.damage % 2);
            newToHit.setHitTable(ToHitData.HIT_NORMAL);
            newToHit.setSideTable(ToHitData.SIDE_FRONT);
            pr.toHit = newToHit;
            pr.aaa.setTargetId(ae.getId());
            pr.aaa.setTargetType(Targetable.TYPE_ENTITY);
            pr.roll = Integer.MAX_VALUE;
            resolveClubAttack(server, pr, ae.getId());
            if (ae instanceof LandAirMech && ae.isAirborneVTOLorWIGE()) {
                server.getGame().addControlRoll(new PilotingRollData(ae.getId(), 0, "missed a flail/wrecking ball attack"));
            } else {
                server.getGame().addPSR(new PilotingRollData(ae.getId(), 0, "missed a flail/wrecking ball attack"));
            }

            if (caa.isZweihandering()) {
                server.applyZweihanderSelfDamage(ae, true, caa.getClub().getLocation());
            }
            return;
        }

        // Need to compute 2d6 damage. and add +3 heat build up.
        if (caa.getClub().getType().hasSubType(MiscType.S_BUZZSAW)) {
            damage = Compute.d6(2);
            ae.heatBuildup += 3;

            // Buzzsaw's blade will shatter on a roll of 2.
            if (roll == 2) {
                Mounted club = caa.getClub();

                for (Mounted eq : ae.getWeaponList()) {
                    if ((eq.getLocation() == club.getLocation())
                            && (eq.getType() instanceof MiscType)
                            && eq.getType().hasFlag(MiscType.F_CLUB)
                            && eq.getType().hasSubType(MiscType.S_BUZZSAW)) {
                        eq.setHit(true);
                        break;
                    }
                }
                r = new Report(4037);
                r.subject = ae.getId();
                server.addReport(r);
                if (caa.isZweihandering()) {
                    server.applyZweihanderSelfDamage(ae, true, caa.getClub().getLocation());
                }
                return;
            }
        }

        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(4075);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            server.addReport(r);
            if (caa.getClub().getType().hasSubType(MiscType.S_MACE)) {
                if (ae instanceof LandAirMech && ae.isAirborneVTOLorWIGE()) {
                    server.getGame().addControlRoll(new PilotingRollData(ae.getId(), 0, "missed a mace attack"));
                } else {
                    server.getGame().addPSR(new PilotingRollData(ae.getId(), 0, "missed a mace attack"));
                }
            }

            if (caa.isZweihandering()) {
                if (caa.getClub().getType().hasSubType(MiscType.S_CLUB)) {
                    server.applyZweihanderSelfDamage(ae, true, Mech.LOC_RARM, Mech.LOC_LARM);
                } else {
                    server.applyZweihanderSelfDamage(ae, true, caa.getClub().getLocation());
                }
            }
            return;
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            r = new Report(4080);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            r.newlines = 0;
            server.addReport(r);
            roll = Integer.MAX_VALUE;
        } else {
            // report the roll
            r = new Report(4025);
            r.subject = ae.getId();
            r.add(toHit);
            r.add(roll);
            r.newlines = 0;
            server.addReport(r);
            if (glancing) {
                r = new Report(3186);
                r.subject = ae.getId();
                r.newlines = 0;
                server.addReport(r);
            }

            if (directBlow) {
                r = new Report(3189);
                r.subject = ae.getId();
                r.newlines = 0;
                server.addReport(r);
            }
        }

        // do we hit?
        if (roll < toHit.getValue()) {
            // miss
            r = new Report(4035);
            r.subject = ae.getId();
            server.addReport(r);

            if (caa.getClub().getType().hasSubType(MiscType.S_MACE)) {
                if (ae instanceof LandAirMech && ae.isAirborneVTOLorWIGE()) {
                    server.getGame().addControlRoll(new PilotingRollData(ae.getId(), 2, "missed a mace attack"));
                } else {
                    server.getGame().addPSR(new PilotingRollData(ae.getId(), 2, "missed a mace attack"));
                }
            }

            // If the target is in a building, the building absorbs the damage.
            if (targetInBuilding && (bldg != null)) {
                // Only report if damage was done to the building.
                if (damage > 0) {
                    Vector<Report> buildingReport = server.damageBuilding(bldg, damage, target.getPosition());
                    for (Report report : buildingReport) {
                        report.subject = ae.getId();
                    }
                    server.addReport(buildingReport);
                }
            }

            if (caa.isZweihandering()) {
                if (caa.getClub().getType().hasSubType(MiscType.S_CLUB)) {
                    server.applyZweihanderSelfDamage(ae, true, Mech.LOC_RARM, Mech.LOC_LARM);
                } else {
                    server.applyZweihanderSelfDamage(ae, true, caa.getClub().getLocation());
                }
            }
            return;
        }

        // Targeting a building.
        if ((target.getTargetType() == Targetable.TYPE_BUILDING)
                || (target.getTargetType() == Targetable.TYPE_FUEL_TANK)) {
            // The building takes the full brunt of the attack.
            r = new Report(4040);
            r.subject = ae.getId();
            server.addReport(r);
            Vector<Report> buildingReport = server.damageBuilding(bldg, damage, target.getPosition());
            for (Report report : buildingReport) {
                report.subject = ae.getId();
            }
            server.addReport(buildingReport);

            // Damage any infantry in the hex.
            server.addReport(server.damageInfantryIn(bldg, damage, target.getPosition()));

            if (caa.isZweihandering()) {
                if (caa.getClub().getType().hasSubType(MiscType.S_CLUB)) {
                    server.applyZweihanderSelfDamage(ae, false, Mech.LOC_RARM, Mech.LOC_LARM);

                    // the club breaks
                    r = new Report(4150);
                    r.subject = ae.getId();
                    r.add(caa.getClub().getName());
                    server.addReport(r);
                    ae.removeMisc(caa.getClub().getName());
                } else {
                    server.applyZweihanderSelfDamage(ae, false, caa.getClub().getLocation());
                }
            }

            // And we're done!
            return;
        }

        HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
        hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
        r = new Report(4045);
        r.subject = ae.getId();
        r.add(toHit.getTableDesc());
        r.add(te.getLocationAbbr(hit));
        server.addReport(r);

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        if (targetInBuilding && (bldg != null)) {
            int bldgAbsorbs = bldg.getAbsorbtion(target.getPosition());
            int toBldg = min(bldgAbsorbs, damage);
            damage -= toBldg;
            server.addNewLines();
            Vector<Report> buildingReport = server.damageBuilding(bldg, damage, target.getPosition());
            for (Report report : buildingReport) {
                report.subject = ae.getId();
            }
            server.addReport(buildingReport);

            // some buildings scale remaining damage that is not absorbed
            // TODO : this isn't quite right for castles brian
            damage = (int) floor(bldg.getDamageToScale() * damage);
        }

        // A building may absorb the entire shot.
        if (damage == 0) {
            r = new Report(4050);
            r.subject = ae.getId();
            r.add(te.getShortName());
            r.add(te.getOwner().getName());
            r.newlines = 0;
            server.addReport(r);
        } else {
            if (glancing) {
                // Round up glancing blows against conventional infantry
                damage = (int) (te.isConventionalInfantry() ? ceil(damage / 2.0) : floor(damage / 2.0));
            }

            if (directBlow) {
                damage += toHit.getMoS() / 3;
                hit.makeDirectBlow(toHit.getMoS() / 3);
            }

            damage = server.checkForSpikes(te, hit.getLocation(), damage, ae, Entity.LOC_NONE);

            DamageType damageType = DamageType.NONE;
            server.addReport(DamageEntityControl.damageEntity(server, te, hit, damage, false, damageType, false,
                    false, throughFront));
            if (target instanceof VTOL) {
                // destroy rotor
                server.addReport(server.applyCriticalHit(te, VTOL.LOC_ROTOR,
                        new CriticalSlot(CriticalSlot.TYPE_SYSTEM, VTOL.CRIT_ROTOR_DESTROYED),
                        false, 0, false));
            }
        }

        // On a roll of 10+ a lance hitting a mech/Vehicle can cause 1 point of
        // internal damage
        if (caa.getClub().getType().hasSubType(MiscType.S_LANCE)
                && (te.getArmor(hit) > 0)
                && (te.getArmorType(hit.getLocation()) != EquipmentType.T_ARMOR_HARDENED)
                && (te.getArmorType(hit.getLocation()) != EquipmentType.T_ARMOR_FERRO_LAMELLOR)) {
            roll = Compute.d6(2);
            // Pierce checking report
            r = new Report(4021);
            r.indent(2);
            r.subject = ae.getId();
            r.add(te.getLocationAbbr(hit));
            r.add(roll);
            server.addReport(r);
            if (roll >= 10) {
                hit.makeGlancingBlow();
                server.addReport(DamageEntityControl.damageEntity(server, te, hit, 1, false, DamageType.NONE,
                        true, false, throughFront));
            }
        }

        // TODO : Verify this is correct according to latest rules
        if (caa.getClub().getType().hasSubType(MiscType.S_WRECKING_BALL)
                && (ae instanceof SupportTank) && (te instanceof Mech)) {
            // forces a PSR like a charge
            if (te instanceof LandAirMech && te.isAirborneVTOLorWIGE()) {
                server.getGame().addControlRoll(new PilotingRollData(te.getId(), 2, "was hit by wrecking ball"));
            } else {
                server.getGame().addPSR(new PilotingRollData(te.getId(), 2, "was hit by wrecking ball"));
            }
        }

        // Chain whips can entangle 'Mech and ProtoMech limbs. This
        // implementation assumes that in order to do so the limb must still
        // have some structure left, so if the whip hits and destroys a
        // location in the same attack no special effects take place.
        if (caa.getClub().getType().hasSubType(MiscType.S_CHAIN_WHIP)
                && ((te instanceof Mech) || (te instanceof Protomech))) {
            server.addNewLines();

            int loc = hit.getLocation();

            boolean mightTrip = (te instanceof Mech)
                    && te.locationIsLeg(loc)
                    && !te.isLocationBad(loc)
                    && !te.isLocationDoomed(loc)
                    && !te.hasActiveShield(loc)
                    && !te.hasPassiveShield(loc);

            boolean mightGrapple = ((te instanceof Mech)
                    && ((loc == Mech.LOC_LARM) || (loc == Mech.LOC_RARM))
                    && !te.isLocationBad(loc)
                    && !te.isLocationDoomed(loc)
                    && !te.hasActiveShield(loc)
                    && !te.hasPassiveShield(loc)
                    && !te.hasNoDefenseShield(loc))
                    || ((te instanceof Protomech)
                        && ((loc == Protomech.LOC_LARM) || (loc == Protomech.LOC_RARM)
                            || (loc == Protomech.LOC_LEG))
                        // Only check location status after confirming we did
                        // hit a limb -- Protos have no actual near-miss
                        // "location" and will throw an exception if it's
                        // referenced here.
                        && !te.isLocationBad(loc)
                        && !te.isLocationDoomed(loc));

            if (mightTrip) {
                roll = Compute.d6(2);
                int toHitValue = toHit.getValue();
                String toHitDesc = toHit.getDesc();
                if ((ae instanceof Mech) && ((Mech) ae).hasActiveTSM(false)) {
                    toHitValue -= 2;
                    toHitDesc += " -2 (TSM Active Bonus)";
                }

                r = new Report(4450);
                r.subject = ae.getId();
                r.add(ae.getShortName());
                r.add(te.getShortName());
                r.addDataWithTooltip(String.valueOf(toHitValue), toHitDesc);
                r.add(roll);
                r.indent(2);
                r.newlines = 0;
                server.addReport(r);

                if (roll >= toHitValue) {
                    r = new Report(2270);
                    r.subject = ae.getId();
                    r.newlines = 0;
                    server.addReport(r);

                    server.getGame().addPSR(new PilotingRollData(te.getId(), 3, "Snared by chain whip"));
                } else {
                    r = new Report(2357);
                    r.subject = ae.getId();
                    r.newlines = 0;
                    server.addReport(r);
                }
            } else if (mightGrapple) {
                GrappleAttackAction gaa = new GrappleAttackAction(ae.getId(), te.getId());
                int grappleSide;
                if (caa.getClub().getLocation() == Mech.LOC_RARM) {
                    grappleSide = Entity.GRAPPLE_RIGHT;
                } else {
                    grappleSide = Entity.GRAPPLE_LEFT;
                }
                ToHitData grappleHit = GrappleAttackAction.toHit(server.getGame(), ae.getId(), target,
                        grappleSide, true);
                PhysicalResult grappleResult = new PhysicalResult();
                grappleResult.aaa = gaa;
                grappleResult.toHit = grappleHit;
                grappleResult.roll = Compute.d6(2);
                ResolveGrappleAttack.resolveGrappleAttack(server, grappleResult, lastEntityId, grappleSide,
                        (hit.getLocation() == Mech.LOC_RARM) ? Entity.GRAPPLE_RIGHT : Entity.GRAPPLE_LEFT);
            }
        }

        server.addNewLines();

        if (caa.isZweihandering()) {
            if (caa.getClub().getType().hasSubType(MiscType.S_CLUB)) {
                server.applyZweihanderSelfDamage(ae, false, Mech.LOC_RARM, Mech.LOC_LARM);
            } else {
                server.applyZweihanderSelfDamage(ae, false, caa.getClub().getLocation());
            }
        }

        // If the attacker is Zweihandering with an improvised club, it will break on the attack.
        // Otherwise, only a tree club will break on the attack
        if ((caa.isZweihandering() && caa.getClub().getType().hasSubType(MiscType.S_CLUB))
                || caa.getClub().getType().hasSubType(MiscType.S_TREE_CLUB)) {
            // the club breaks
            r = new Report(4150);
            r.subject = ae.getId();
            r.add(caa.getClub().getName());
            server.addReport(r);
            ae.removeMisc(caa.getClub().getName());
        }

        server.addNewLines();

        // if the target is an industrial mech, it needs to check for crits at the end of turn
        if ((target instanceof Mech) && ((Mech) target).isIndustrial()) {
            ((Mech) target).setCheckForCrit(true);
        }
    }
}
