package megamek.server;

import megamek.common.*;
import megamek.common.enums.DamageType;
import megamek.common.options.OptionsConstants;

import java.util.List;
import java.util.Vector;

import static java.lang.Math.*;

public class DamageEntityControl {
    /**
     * Deals the listed damage to an entity. Returns a vector of Reports for the
     * phase report
     *
     * @param server
     * @param te            the target entity
     * @param hit           the hit data for the location hit
     * @param damage        the damage to apply
     * @param ammoExplosion ammo explosion type damage is applied directly to the IS,
     *                      hurts the pilot, causes auto-ejects, and can blow the unit to
     *                      smithereens
     * @param bFrag         The DamageType of the attack.
     * @param damageIS      Should the target location's internal structure be damaged
     *                      directly?
     * @param areaSatArty   Is the damage from an area saturating artillery attack?
     * @return a <code>Vector</code> of <code>Report</code>s
     */
    public static Vector<Report> damageEntity(Server server, Entity te, HitData hit, int damage, boolean ammoExplosion, DamageType bFrag, boolean damageIS, boolean areaSatArty) {
        return damageEntity(server, te, hit, damage, ammoExplosion, bFrag, damageIS,
                            areaSatArty, true);
    }

    /**
     * Deals the listed damage to an entity. Returns a vector of Reports for the
     * phase report
     *
     * @param server
     * @param te            the target entity
     * @param hit           the hit data for the location hit
     * @param damage        the damage to apply
     * @param ammoExplosion ammo explosion type damage is applied directly to the IS,
     *                      hurts the pilot, causes auto-ejects, and can blow the unit to
     *                      smithereens
     * @param bFrag         The DamageType of the attack.
     * @param damageIS      Should the target location's internal structure be damaged
     *                      directly?
     * @param areaSatArty   Is the damage from an area saturating artillery attack?
     * @param throughFront  Is the damage coming through the hex the unit is facing?
     * @return a <code>Vector</code> of <code>Report</code>s
     */
    public static Vector<Report> damageEntity(Server server, Entity te, HitData hit, int damage, boolean ammoExplosion, DamageType bFrag, boolean damageIS, boolean areaSatArty, boolean throughFront) {
        return damageEntity(server, te, hit, damage, ammoExplosion, bFrag, damageIS,
                            areaSatArty, throughFront, false, false);
    }

    /**
     * Deals the listed damage to an entity. Returns a vector of Reports for the
     * phase report
     *
     * @param server
     * @param te            the target entity
     * @param hit           the hit data for the location hit
     * @param damage        the damage to apply
     * @param ammoExplosion ammo explosion type damage is applied directly to the IS,
     *                      hurts the pilot, causes auto-ejects, and can blow the unit to
     *                      smithereens
     * @param bFrag         The DamageType of the attack.
     * @param damageIS      Should the target location's internal structure be damaged
     *                      directly?
     * @param areaSatArty   Is the damage from an area saturating artillery attack?
     * @param throughFront  Is the damage coming through the hex the unit is facing?
     * @param underWater    Is the damage coming from an underwater attack
     * @return a <code>Vector</code> of <code>Report</code>s
     */
    public static Vector<Report> damageEntity(Server server, Entity te, HitData hit, int damage, boolean ammoExplosion, DamageType bFrag, boolean damageIS, boolean areaSatArty, boolean throughFront, boolean underWater) {
        return damageEntity(server, te, hit, damage, ammoExplosion, bFrag, damageIS,
                            areaSatArty, throughFront, underWater, false);
    }

    /**
     * Deals the listed damage to an entity. Returns a vector of Reports for the
     * phase report
     *
     * @param server
     * @param te            the target entity
     * @param hit           the hit data for the location hit
     * @param damage        the damage to apply
     * @param ammoExplosion ammo explosion type damage is applied directly to the IS,
     *                      hurts the pilot, causes auto-ejects, and can blow the unit to
     *                      smithereens
     * @param damageType         The DamageType of the attack.
     * @param damageIS      Should the target location's internal structure be damaged
     *                      directly?
     * @param areaSatArty   Is the damage from an area saturating artillery attack?
     * @param throughFront  Is the damage coming through the hex the unit is facing?
     * @param underWater    Is the damage coming from an underwater attack?
     * @param nukeS2S       is this a ship-to-ship nuke?
     * @return a <code>Vector</code> of <code>Report</code>s
     */
    public static Vector<Report> damageEntity(Server server, Entity te, HitData hit, int damage, boolean ammoExplosion, DamageType damageType, boolean damageIS, boolean areaSatArty, boolean throughFront, boolean underWater, boolean nukeS2S) {

        Vector<Report> vDesc = new Vector<>();
        Report r;
        int te_n = te.getId();

        // if this is a fighter squadron then pick an active fighter and pass on
        // the damage
        if (te instanceof FighterSquadron) {
            List<Entity> fighters = te.getActiveSubEntities();

            if (fighters.isEmpty()) {
                return vDesc;
            }
            Entity fighter = fighters.get(hit.getLocation());
            HitData new_hit = fighter.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
            new_hit.setBoxCars(hit.rolledBoxCars());
            new_hit.setGeneralDamageType(hit.getGeneralDamageType());
            new_hit.setCapital(hit.isCapital());
            new_hit.setCapMisCritMod(hit.getCapMisCritMod());
            new_hit.setSingleAV(hit.getSingleAV());
            new_hit.setAttackerId(hit.getAttackerId());
            return damageEntity(server, fighter, new_hit, damage, ammoExplosion, damageType,
                                damageIS, areaSatArty, throughFront, underWater, nukeS2S);
        }

        // Battle Armor takes full damage to each trooper from area-effect.
        if (areaSatArty && (te instanceof BattleArmor)) {
            r = new Report(6044);
            r.subject = te.getId();
            r.indent(2);
            vDesc.add(r);
            for (int i = 0; i < ((BattleArmor) te).getTroopers(); i++) {
                hit.setLocation(BattleArmor.LOC_TROOPER_1 + i);
                if (te.getInternal(hit) > 0) {
                    vDesc.addAll(damageEntity(server, te, hit, damage, ammoExplosion, damageType,
                            damageIS, false, throughFront, underWater, nukeS2S));
                }
            }
            return vDesc;
        }

        // This is good for shields if a shield absorps the hit it shouldn't
        // effect the pilot.
        // TC SRM's that hit the head do external and internal damage but its
        // one hit and shouldn't cause
        // 2 hits to the pilot.
        boolean isHeadHit = (te instanceof Mech)
                            && (((Mech) te).getCockpitType() != Mech.COCKPIT_TORSO_MOUNTED)
                            && (hit.getLocation() == Mech.LOC_HEAD)
                            && ((hit.getEffect() & HitData.EFFECT_NO_CRITICALS) != HitData.EFFECT_NO_CRITICALS);

        // booleans to indicate criticals for AT2
        boolean critSI = false;
        boolean critThresh = false;

        // get the relevant damage for damage thresholding
        int threshDamage = damage;
        // weapon groups only get the damage of one weapon
        if ((hit.getSingleAV() > -1)
            && !server.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            threshDamage = hit.getSingleAV();
        }

        // is this capital-scale damage
        boolean isCapital = hit.isCapital();

        // check capital/standard damage
        if (isCapital
            && (!te.isCapitalScale() || server.getGame().getOptions().booleanOption(
                OptionsConstants.ADVAERORULES_AERO_SANITY))) {
            damage = 10 * damage;
            threshDamage = 10 * threshDamage;
        }
        if (!isCapital && te.isCapitalScale()
            && !server.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            damage = (int) round(damage / 10.0);
            threshDamage = (int) round(threshDamage / 10.0);
        }

        int damage_orig = damage;

        // show Locations which have rerolled with Edge
        HitData undoneLocation = hit.getUndoneLocation();
        while (undoneLocation != null) {
            r = new Report(6500);
            r.subject = te_n;
            r.indent(2);
            r.addDesc(te);
            r.add(te.getLocationAbbr(undoneLocation));
            vDesc.addElement(r);
            undoneLocation = undoneLocation.getUndoneLocation();
        } // while
        // if edge was uses, give at end overview of remaining
        if (hit.getUndoneLocation() != null) {
            r = new Report(6510);
            r.subject = te_n;
            r.indent(2);
            r.addDesc(te);
            r.add(te.getCrew().getOptions().intOption(OptionsConstants.EDGE));
            vDesc.addElement(r);
        } // if

        boolean autoEject = false;
        if (ammoExplosion) {
            if (te instanceof Mech) {
                Mech mech = (Mech) te;
                if (mech.isAutoEject() && (!server.getGame().getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                        || (server.getGame().getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                && mech.isCondEjectAmmo()))) {
                    autoEject = true;
                    vDesc.addAll(server.ejectEntity(te, true));
                }
            } else if (te instanceof Aero) {
                Aero aero = (Aero) te;
                if (aero.isAutoEject() && (!server.getGame().getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                        || (server.getGame().getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                && aero.isCondEjectAmmo()))) {
                    autoEject = true;
                    vDesc.addAll(server.ejectEntity(te, true));
                }
            }
        }
        boolean isBattleArmor = te instanceof BattleArmor;
        boolean isPlatoon = !isBattleArmor && (te instanceof Infantry);
        boolean isFerroFibrousTarget = false;
        boolean wasDamageIS = false;
        boolean tookInternalDamage = damageIS;
        Hex te_hex = null;

        boolean hardenedArmor = ((te instanceof Mech) || (te instanceof Tank))
                && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HARDENED);
        boolean ferroLamellorArmor = ((te instanceof Mech) || (te instanceof Tank) || (te instanceof Aero))
                && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_FERRO_LAMELLOR);
        boolean reflectiveArmor = (((te instanceof Mech) || (te instanceof Tank) || (te instanceof Aero))
                && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_REFLECTIVE))
                || (isBattleArmor && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_BA_REFLECTIVE));
        boolean reactiveArmor = (((te instanceof Mech) || (te instanceof Tank) || (te instanceof Aero))
                && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_REACTIVE))
                || (isBattleArmor && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_BA_REACTIVE));
        boolean ballisticArmor = ((te instanceof Mech) || (te instanceof Tank) || (te instanceof Aero))
                && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_BALLISTIC_REINFORCED);
        boolean impactArmor = (te instanceof Mech)
                && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_IMPACT_RESISTANT);
        boolean bar5 = te.getBARRating(hit.getLocation()) <= 5;

        // TACs from the hit location table
        int crits;
        if ((hit.getEffect() & HitData.EFFECT_CRITICAL) == HitData.EFFECT_CRITICAL) {
            crits = 1;
        } else {
            crits = 0;
        }

        // this is for special crits, like AP and tandem-charge
        int specCrits = 0;

        // the bonus to the crit roll if using the
        // "advanced determining critical hits rule"
        int critBonus = 0;
        if (server.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CRIT_ROLL)
            && (damage_orig > 0)
            && ((te instanceof Mech) || (te instanceof Protomech))) {
            critBonus = min((damage_orig - 1) / 5, 4);
        }

        // Find out if Human TRO plays a part it crit bonus
        Entity ae = server.getGame().getEntity(hit.getAttackerId());
        if ((ae != null) && !areaSatArty) {
            if ((te instanceof Mech) && ae.hasAbility(OptionsConstants.MISC_HUMAN_TRO, Crew.HUMANTRO_MECH)) {
                critBonus += 1;
            } else if ((te instanceof Aero) && ae.hasAbility(OptionsConstants.MISC_HUMAN_TRO, Crew.HUMANTRO_AERO)) {
                critBonus += 1;
            } else if ((te instanceof Tank) && ae.hasAbility(OptionsConstants.MISC_HUMAN_TRO, Crew.HUMANTRO_VEE)) {
                critBonus += 1;
            } else if ((te instanceof BattleArmor) && ae.hasAbility(OptionsConstants.MISC_HUMAN_TRO, Crew.HUMANTRO_BA)) {
                critBonus += 1;
            }
        }

        HitData nextHit = null;

        // Some "hits" on a ProtoMech are actually misses.
        if ((te instanceof Protomech) && (hit.getLocation() == Protomech.LOC_NMISS)) {
            Protomech proto = (Protomech) te;
            r = new Report(6035);
            r.subject = te.getId();
            r.indent(2);
            if (proto.isGlider()) {
                r.messageId = 6036;
                proto.setWingHits(proto.getWingHits() + 1);
            }
            vDesc.add(r);
            return vDesc;
        }

        // check for critical hit/miss vs. a BA
        if ((crits > 0) && (te instanceof BattleArmor)) {
            // possible critical miss if the rerolled location isn't alive
            if ((hit.getLocation() >= te.locations()) || (te.getInternal(hit.getLocation()) <= 0)) {
                r = new Report(6037);
                r.add(hit.getLocation());
                r.subject = te_n;
                r.indent(2);
                vDesc.addElement(r);
                return vDesc;
            }
            // otherwise critical hit
            r = new Report(6225);
            r.add(te.getLocationAbbr(hit));
            r.subject = te_n;
            r.indent(2);
            vDesc.addElement(r);

            crits = 0;
            damage = max(te.getInternal(hit.getLocation()) + te.getArmor(hit.getLocation()), damage);
        }

        if ((te.getArmor(hit) > 0) && ((te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_FERRO_FIBROUS)
                || (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_LIGHT_FERRO)
                || (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HEAVY_FERRO))) {
            isFerroFibrousTarget = true;
        }

        // Infantry with TSM implants get 2d6 burst damage from ATSM munitions
        if (damageType.equals(DamageType.ANTI_TSM) && te.isConventionalInfantry() && te.antiTSMVulnerable()) {
            int burst = Compute.d6(2);
            r = new Report(6434);
            r.subject = te_n;
            r.add(burst);
            r.indent(2);
            vDesc.addElement(r);
            damage += burst;
        }

        // area effect against infantry is double damage
        if (isPlatoon && areaSatArty) {
            // PBI. Double damage.
            damage *= 2;
            r = new Report(6039);
            r.subject = te_n;
            r.indent(2);
            vDesc.addElement(r);
        }

        // Is the infantry in the open?
        if (ServerHelper.infantryInOpen(te, te_hex, server.getGame(), isPlatoon, ammoExplosion, hit.isIgnoreInfantryDoubleDamage())) {
            // PBI. Damage is doubled.
            damage *= 2;
            r = new Report(6040);
            r.subject = te_n;
            r.indent(2);
            vDesc.addElement(r);
        }

        // Is the infantry in vacuum?
        if ((isPlatoon || isBattleArmor) && !te.isDestroyed() && !te.isDoomed()
            && server.getGame().getPlanetaryConditions().isVacuum()) {
            // PBI. Double damage.
            damage *= 2;
            r = new Report(6041);
            r.subject = te_n;
            r.indent(2);
            vDesc.addElement(r);
        }

        switch (damageType) {
            case FRAGMENTATION:
                // Fragmentation missiles deal full damage to conventional
                // infantry
                // (only) and no damage to other target types.
                if (!isPlatoon) {
                    damage = 0;
                    r = new Report(6050); // For some reason this report never
                    // actually shows up...
                } else {
                    r = new Report(6045); // ...but this one displays just fine.
                }
                r.subject = te_n;
                r.indent(2);
                vDesc.addElement(r);
                break;
            case NONPENETRATING:
                if (!isPlatoon) {
                    damage = 0;
                    r = new Report(6051);
                    r.subject = te_n;
                    r.indent(2);
                    vDesc.addElement(r);
                }
                break;
            case FLECHETTE:
                // Flechette ammo deals full damage to conventional infantry and
                // half damage to other targets (including battle armor).
                if (!isPlatoon) {
                    damage /= 2;
                    r = new Report(6060);
                } else {
                    r = new Report(6055);
                }
                r.subject = te_n;
                r.indent(2);
                vDesc.addElement(r);
                break;
            case ACID:
                if (isFerroFibrousTarget || reactiveArmor || reflectiveArmor
                    || ferroLamellorArmor || bar5) {
                    if (te.getArmor(hit) <= 0) {
                        break; // hitting IS, not acid-affected armor
                    }
                    damage = min(te.getArmor(hit), 3);
                    r = new Report(6061);
                    r.subject = te_n;
                    r.indent(2);
                    r.add(damage);
                    vDesc.addElement(r);
                } else if (isPlatoon) {
                    damage = (int) ceil(damage * 1.5);
                    r = new Report(6062);
                    r.subject = te_n;
                    r.indent(2);
                    vDesc.addElement(r);
                }
                break;
            case INCENDIARY:
                // Incendiary AC ammo does +2 damage to unarmoured infantry
                if (isPlatoon) {
                    damage += 2;
                    r = new Report(6064);
                    r.subject = te_n;
                    r.indent(2);
                    vDesc.addElement(r);
                }
                break;
            case NAIL_RIVET:
                // no damage against armor of BAR rating >=5
                if ((te.getBARRating(hit.getLocation()) >= 5)
                    && (te.getArmor(hit.getLocation()) > 0)) {
                    damage = 0;
                    r = new Report(6063);
                    r.subject = te_n;
                    r.indent(2);
                    vDesc.add(r);
                }
                break;
            default:
                // We can ignore this.
                break;
        }

        // adjust VTOL rotor damage
        if ((te instanceof VTOL) && (hit.getLocation() == VTOL.LOC_ROTOR)
            && (hit.getGeneralDamageType() != HitData.DAMAGE_PHYSICAL)
            && !server.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_FULL_ROTOR_HITS)) {
            damage = (damage + 9) / 10;
        }

        // save EI status, in case sensors crit destroys it
        final boolean eiStatus = te.hasActiveEiCockpit();
        // BA using EI implants receive +1 damage from attacks
        if (!(te instanceof Mech) && !(te instanceof Protomech) && eiStatus) {
            damage += 1;
        }

        // check for case on Aeros
        if (te instanceof Aero) {
            Aero a = (Aero) te;
            if (ammoExplosion && a.hasCase()) {
                // damage should be reduced by a factor of 2 for ammo explosions
                // according to p. 161, TW
                damage /= 2;
                r = new Report(9010);
                r.subject = te_n;
                r.add(damage);
                r.indent(3);
                vDesc.addElement(r);
            }
        }

        // infantry armor can reduce damage
        if (isPlatoon && (((Infantry) te).calcDamageDivisor() != 1.0)) {
            r = new Report(6074);
            r.subject = te_n;
            r.indent(2);
            r.add(damage);
            damage = (int) ceil((damage) / ((Infantry) te).calcDamageDivisor());
            r.add(damage);
            vDesc.addElement(r);
        }

        // Allocate the damage
        while (damage > 0) {

            // first check for ammo explosions on aeros separately, because it
            // must be done before
            // standard to capital damage conversions
            if ((te instanceof Aero) && (hit.getLocation() == Aero.LOC_AFT)
                && !damageIS) {
                for (Mounted mAmmo : te.getAmmo()) {
                    if (mAmmo.isDumping() && !mAmmo.isDestroyed() && !mAmmo.isHit()
                            && !(mAmmo.getType() instanceof BombType)) {
                        // doh. explode it
                        vDesc.addAll(server.explodeEquipment(te, mAmmo.getLocation(), mAmmo));
                        mAmmo.setHit(true);
                    }
                }
            }

            if (te.isAero()) {
                // chance of a critical if damage greater than threshold
                IAero a = (IAero) te;
                if ((threshDamage > a.getThresh(hit.getLocation()))) {
                    critThresh = true;
                    a.setCritThresh(true);
                }
            }

            // Capital fighters receive damage differently
            if (te.isCapitalFighter()) {
                IAero a = (IAero) te;
                a.setCurrentDamage(a.getCurrentDamage() + damage);
                a.setCapArmor(a.getCapArmor() - damage);
                r = new Report(9065);
                r.subject = te_n;
                r.indent(2);
                r.newlines = 0;
                r.addDesc(te);
                r.add(damage);
                vDesc.addElement(r);
                r = new Report(6085);
                r.subject = te_n;
                r.add(max(a.getCapArmor(), 0));
                vDesc.addElement(r);
                // check to see if this destroyed the entity
                if (a.getCapArmor() <= 0) {
                    // Lets auto-eject if we can!
                    if (a instanceof LandAirMech) {
                        // LAMs eject if the CT destroyed switch is on
                        LandAirMech lam = (LandAirMech) a;
                        if (lam.isAutoEject()
                            && (!server.getGame().getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                    || (server.getGame().getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                            && lam.isCondEjectCTDest()))) {
                            server.addReport(server.ejectEntity(te, true, false));
                        }
                    } else {
                        // Aeros eject if the SI Destroyed switch is on
                        Aero aero = (Aero) a;
                        if (aero.isAutoEject()
                                && (!server.getGame().getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                    || (server.getGame().getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                            && aero.isCondEjectSIDest()))) {
                            server.addReport(server.ejectEntity(te, true, false));
                        }
                    }
                    vDesc.addAll(server.destroyEntity(te, "Structural Integrity Collapse"));
                    a.doDisbandDamage();
                    a.setCapArmor(0);
                    if (hit.getAttackerId() != Entity.NONE) {
                        server.creditKill(te, server.getGame().getEntity(hit.getAttackerId()));
                    }
                }
                // check for aero crits from natural 12 or threshold; LAMs take damage as mechs
                if (te instanceof Aero) {
                    server.checkAeroCrits(vDesc, (Aero) te, hit, damage_orig, critThresh,
                                   critSI, ammoExplosion, nukeS2S);
                }
                return vDesc;
            }

            if (!((te instanceof Aero) && ammoExplosion)) {
                // report something different for Aero ammo explosions
                r = new Report(6065);
                r.subject = te_n;
                r.indent(2);
                r.addDesc(te);
                r.add(damage);
                if (damageIS) {
                    r.messageId = 6070;
                }
                r.add(te.getLocationAbbr(hit));
                vDesc.addElement(r);
            }

            // was the section destroyed earlier this phase?
            if (te.getInternal(hit) == IArmorState.ARMOR_DOOMED) {
                // cannot transfer a through armor crit if so
                crits = 0;
            }

            // here goes the fun :)
            // Shields take damage first then cowls then armor whee
            // Shield does not protect from ammo explosions or falls.
            if (!ammoExplosion && !hit.isFallDamage() && !damageIS && te.hasShield()
                    && ((hit.getEffect() & HitData.EFFECT_NO_CRITICALS) != HitData.EFFECT_NO_CRITICALS)) {
                Mech me = (Mech) te;
                int damageNew = me.shieldAbsorptionDamage(damage, hit.getLocation(), hit.isRear());
                // if a shield absorbed the damage then lets tell the world
                // about it.
                if (damageNew != damage) {
                    int absorb = damage - damageNew;
                    te.damageThisPhase += absorb;
                    damage = damageNew;

                    r = new Report(3530);
                    r.subject = te_n;
                    r.indent(3);
                    r.add(absorb);
                    vDesc.addElement(r);

                    if (damage <= 0) {
                        crits = 0;
                        specCrits = 0;
                        isHeadHit = false;
                    }
                }
            }

            // Armored Cowl may absorb some damage from hit
            if (te instanceof Mech) {
                Mech me = (Mech) te;
                if (me.hasCowl() && (hit.getLocation() == Mech.LOC_HEAD)
                    && !throughFront) {
                    int damageNew = me.damageCowl(damage);
                    int damageDiff = damage - damageNew;
                    me.damageThisPhase += damageDiff;
                    damage = damageNew;

                    r = new Report(3520);
                    r.subject = te_n;
                    r.indent(3);
                    r.add(damageDiff);
                    vDesc.addElement(r);
                }
            }

            // So might modular armor, if the location mounts any.
            if (!ammoExplosion && !damageIS
                    && ((hit.getEffect() & HitData.EFFECT_NO_CRITICALS) != HitData.EFFECT_NO_CRITICALS)) {
                int damageNew = te.getDamageReductionFromModularArmor(hit, damage, vDesc);
                int damageDiff = damage - damageNew;
                te.damageThisPhase += damageDiff;
                damage = damageNew;
            }

            // Destroy searchlights on 7+ (torso hits on mechs)
            if (te.hasSearchlight()) {
                boolean spotlightHittable = true;
                int loc = hit.getLocation();
                if (te instanceof Mech) {
                    if ((loc != Mech.LOC_CT) && (loc != Mech.LOC_LT) && (loc != Mech.LOC_RT)) {
                        spotlightHittable = false;
                    }
                } else if (te instanceof Tank) {
                    if (te instanceof SuperHeavyTank) {
                        if ((loc != Tank.LOC_FRONT)
                                && (loc != SuperHeavyTank.LOC_FRONTRIGHT)
                                && (loc != SuperHeavyTank.LOC_FRONTLEFT)
                                && (loc != SuperHeavyTank.LOC_REARRIGHT)
                                && (loc != SuperHeavyTank.LOC_REARLEFT)) {
                            spotlightHittable = false;
                        }
                    } else if (te instanceof LargeSupportTank) {
                        if ((loc != Tank.LOC_FRONT)
                                && (loc != LargeSupportTank.LOC_FRONTRIGHT)
                                && (loc != LargeSupportTank.LOC_FRONTLEFT)
                                && (loc != LargeSupportTank.LOC_REARRIGHT)
                                && (loc != LargeSupportTank.LOC_REARLEFT)) {
                            spotlightHittable = false;
                        }
                    } else {
                        if ((loc != Tank.LOC_FRONT) && (loc != Tank.LOC_RIGHT)
                                && (loc != Tank.LOC_LEFT)) {
                            spotlightHittable = false;
                        }
                    }

                }
                if (spotlightHittable) {
                    int spotroll = Compute.d6(2);
                    r = new Report(6072);
                    r.indent(2);
                    r.subject = te_n;
                    r.add("7+");
                    r.add("Searchlight");
                    r.add(spotroll);
                    vDesc.addElement(r);
                    if (spotroll >= 7) {
                        r = new Report(6071);
                        r.subject = te_n;
                        r.indent(2);
                        r.add("Searchlight");
                        vDesc.addElement(r);
                        te.destroyOneSearchlight();
                    }
                }
            }

            // Does an exterior passenger absorb some of the damage?
            if (!damageIS) {
                int nLoc = hit.getLocation();
                Entity passenger = te.getExteriorUnitAt(nLoc, hit.isRear());
                // Does an exterior passenger absorb some of the damage?
                if (!ammoExplosion && (null != passenger) && !passenger.isDoomed()
                        && (damageType != DamageType.IGNORE_PASSENGER)) {
                    damage = server.damageExternalPassenger(te, hit, damage, vDesc, passenger);
                }

                boolean bTorso = (nLoc == Mech.LOC_CT) || (nLoc == Mech.LOC_RT)
                        || (nLoc == Mech.LOC_LT);

                // Does a swarming unit absorb damage?
                int swarmer = te.getSwarmAttackerId();
                if ((!(te instanceof Mech) || bTorso) && (swarmer != Entity.NONE)
                        && ((hit.getEffect() & HitData.EFFECT_CRITICAL) == 0) && (Compute.d6() >= 5)
                        && (damageType != DamageType.IGNORE_PASSENGER) && !ammoExplosion) {
                    Entity swarm = server.getGame().getEntity(swarmer);
                    // Yup. Roll up some hit data for that passenger.
                    r = new Report(6076);
                    r.subject = swarmer;
                    r.indent(3);
                    r.addDesc(swarm);
                    vDesc.addElement(r);

                    HitData passHit = swarm.rollHitLocation(
                            ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);

                    // How much damage will the swarm absorb?
                    int absorb = 0;
                    HitData nextPassHit = passHit;
                    do {
                        if (0 < swarm.getArmor(nextPassHit)) {
                            absorb += swarm.getArmor(nextPassHit);
                        }
                        if (0 < swarm.getInternal(nextPassHit)) {
                            absorb += swarm.getInternal(nextPassHit);
                        }
                        nextPassHit = swarm.getTransferLocation(nextPassHit);
                    } while ((damage > absorb)
                             && (nextPassHit.getLocation() >= 0));

                    // Damage the swarm.
                    int absorbedDamage = min(damage, absorb);
                    Vector<Report> newReports = server.damageEntity(swarm, passHit,
                                                             absorbedDamage);
                    for (Report newReport : newReports) {
                        newReport.indent(2);
                    }
                    vDesc.addAll(newReports);

                    // Did some damage pass on?
                    if (damage > absorb) {
                        // Yup. Remove the absorbed damage.
                        damage -= absorb;
                        r = new Report(6080);
                        r.subject = te_n;
                        r.indent(2);
                        r.add(damage);
                        r.addDesc(te);
                        vDesc.addElement(r);
                    } else {
                        // Nope. Return our description.
                        return vDesc;
                    }
                }

                // is this a mech/tank dumping ammo being hit in the rear torso?
                if (((te instanceof Mech) && hit.isRear() && bTorso)
                        || ((te instanceof Tank) && (hit.getLocation() == (te instanceof SuperHeavyTank ? SuperHeavyTank.LOC_REAR
                                : Tank.LOC_REAR)))) {
                    for (Mounted mAmmo : te.getAmmo()) {
                        if (mAmmo.isDumping() && !mAmmo.isDestroyed()
                            && !mAmmo.isHit()) {
                            // doh. explode it
                            vDesc.addAll(server.explodeEquipment(te,
                                                          mAmmo.getLocation(), mAmmo));
                            mAmmo.setHit(true);
                        }
                    }
                }
            }
            // is there armor in the location hit?
            if (!ammoExplosion && (te.getArmor(hit) > 0) && !damageIS) {
                int tmpDamageHold = -1;
                int origDamage = damage;

                if (isPlatoon) {
                    // infantry armour works differently
                    int armor = te.getArmor(hit);
                    int men = te.getInternal(hit);
                    tmpDamageHold = damage % 2;
                    damage /= 2;
                    if ((tmpDamageHold == 1) && (armor >= men)) {
                        // extra 1 point of damage to armor
                        tmpDamageHold = damage;
                        damage++;
                    } else {
                        // extra 0 or 1 point of damage to men
                        tmpDamageHold += damage;
                    }
                    // If the target has Ferro-Lamellor armor, we need to adjust
                    // damage. (4/5ths rounded down),
                    // Also check to eliminate crit chances for damage reduced
                    // to 0
                } else if (ferroLamellorArmor
                           && (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING)
                           && (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING_MISSILE)
                           && (hit.getGeneralDamageType() != HitData.DAMAGE_IGNORES_DMG_REDUCTION)) {
                    tmpDamageHold = damage;
                    damage = (int) floor((((double) damage) * 4) / 5);
                    if (damage <= 0) {
                        isHeadHit = false;
                        crits = 0;
                    }
                    r = new Report(6073);
                    r.subject = te_n;
                    r.indent(3);
                    r.add(damage);
                    vDesc.addElement(r);
                } else if (ballisticArmor
                           && ((hit.getGeneralDamageType() == HitData.DAMAGE_ARMOR_PIERCING_MISSILE)
                               || (hit.getGeneralDamageType() == HitData.DAMAGE_ARMOR_PIERCING)
                               || (hit.getGeneralDamageType() == HitData.DAMAGE_BALLISTIC)
                               || (hit.getGeneralDamageType() == HitData.DAMAGE_MISSILE))) {
                    tmpDamageHold = damage;
                    damage = max(1, damage / 2);
                    r = new Report(6088);
                    r.subject = te_n;
                    r.indent(3);
                    r.add(damage);
                    vDesc.addElement(r);
                } else if (impactArmor
                           && (hit.getGeneralDamageType() == HitData.DAMAGE_PHYSICAL)) {
                    tmpDamageHold = damage;
                    damage -= (int) ceil((double) damage / 3);
                    damage = max(1, damage);
                    r = new Report(6089);
                    r.subject = te_n;
                    r.indent(3);
                    r.add(damage);
                    vDesc.addElement(r);
                } else if (reflectiveArmor
                           && (hit.getGeneralDamageType() == HitData.DAMAGE_PHYSICAL)
                           && !isBattleArmor) { // BA reflec does not receive extra physical damage
                    tmpDamageHold = damage;
                    int currArmor = te.getArmor(hit);
                    int dmgToDouble = min(damage, currArmor / 2);
                    damage += dmgToDouble;
                    r = new Report(6066);
                    r.subject = te_n;
                    r.indent(3);
                    r.add(currArmor);
                    r.add(tmpDamageHold);
                    r.add(dmgToDouble);
                    r.add(damage);
                    vDesc.addElement(r);
                } else if (reflectiveArmor && areaSatArty && !isBattleArmor) {
                    tmpDamageHold = damage; // BA reflec does not receive extra AE damage
                    int currArmor = te.getArmor(hit);
                    int dmgToDouble = min(damage, currArmor / 2);
                    damage += dmgToDouble;
                    r = new Report(6087);
                    r.subject = te_n;
                    r.indent(3);
                    r.add(currArmor);
                    r.add(tmpDamageHold);
                    r.add(dmgToDouble);
                    r.add(damage);
                    vDesc.addElement(r);
                } else if (reflectiveArmor
                           && (hit.getGeneralDamageType() == HitData.DAMAGE_ENERGY)) {
                    tmpDamageHold = damage;
                    damage = (int) floor(((double) damage) / 2);
                    if (tmpDamageHold == 1) {
                        damage = 1;
                    }
                    r = new Report(6067);
                    r.subject = te_n;
                    r.indent(3);
                    r.add(damage);
                    vDesc.addElement(r);
                } else if (reactiveArmor
                           && ((hit.getGeneralDamageType() == HitData.DAMAGE_MISSILE)
                               || (hit.getGeneralDamageType() == HitData.DAMAGE_ARMOR_PIERCING_MISSILE) ||
                               areaSatArty)) {
                    tmpDamageHold = damage;
                    damage = (int) floor(((double) damage) / 2);
                    if (tmpDamageHold == 1) {
                        damage = 1;
                    }
                    r = new Report(6068);
                    r.subject = te_n;
                    r.indent(3);
                    r.add(damage);
                    vDesc.addElement(r);
                }

                // If we're using optional tank damage thresholds, setup our hit
                // effects now...
                if ((te instanceof Tank)
                        && server.getGame().getOptions()
                                .booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)
                        && !((te instanceof VTOL) || (te instanceof GunEmplacement))) {
                    int thresh = (int) ceil(
                            (server.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD_VARIABLE)
                                    ? te.getArmor(hit)
                                    : te.getOArmor(hit)) / (double) server.getGame().getOptions().intOption(
                                            OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD_DIVISOR));

                    // adjust for hardened armor
                    if (hardenedArmor
                            && (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING)
                            && (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING_MISSILE)
                            && (hit.getGeneralDamageType() != HitData.DAMAGE_IGNORES_DMG_REDUCTION)) {
                        thresh *= 2;
                    }

                    if ((damage > thresh) || (te.getArmor(hit) < damage)) {
                        hit.setEffect(((Tank) te).getPotCrit());
                        ((Tank) te).setOverThresh(true);
                        // TACs from the hit location table
                        crits = ((hit.getEffect() & HitData.EFFECT_CRITICAL)
                                == HitData.EFFECT_CRITICAL) ? 1 : 0;
                    } else {
                        ((Tank) te).setOverThresh(false);
                        crits = 0;
                    }
                }

                // if there's a mast mount in the rotor, it and all other
                // equipment
                // on it get destroyed
                if ((te instanceof VTOL)
                    && (hit.getLocation() == VTOL.LOC_ROTOR)
                    && te.hasWorkingMisc(MiscType.F_MAST_MOUNT, -1,
                                         VTOL.LOC_ROTOR)) {
                    r = new Report(6081);
                    r.subject = te_n;
                    r.indent(2);
                    vDesc.addElement(r);
                    for (Mounted mount : te.getMisc()) {
                        if (mount.getLocation() == VTOL.LOC_ROTOR) {
                            mount.setHit(true);
                        }
                    }
                }
                // Need to account for the possibility of hardened armor here
                int armorThreshold = te.getArmor(hit);
                if (hardenedArmor
                    && (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING)
                    && (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING_MISSILE)
                    && (hit.getGeneralDamageType() != HitData.DAMAGE_IGNORES_DMG_REDUCTION)) {
                    armorThreshold *= 2;
                    armorThreshold -= (te.isHardenedArmorDamaged(hit)) ? 1 : 0;
                    vDesc.lastElement().newlines = 0;
                    r = new Report(6069);
                    r.subject = te_n;
                    r.indent(3);
                    int reportedDamage = damage / 2;
                    if ((damage % 2) > 0) {
                        r.add(reportedDamage + ".5");
                    } else {
                        r.add(reportedDamage);
                    }

                    vDesc.addElement(r);
                }
                if (armorThreshold >= damage) {

                    // armor absorbs all damage
                    // Hardened armor deals with damage in its own fashion...
                    if (hardenedArmor
                        && (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING)
                        && (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING_MISSILE)
                        && (hit.getGeneralDamageType() != HitData.DAMAGE_IGNORES_DMG_REDUCTION)) {
                        armorThreshold -= damage;
                        te.setHardenedArmorDamaged(hit, (armorThreshold % 2) > 0);
                        te.setArmor((armorThreshold / 2) + (armorThreshold % 2), hit);
                    } else {
                        te.setArmor(te.getArmor(hit) - damage, hit);
                    }

                    // set "armor damage" flag for HarJel II/III
                    // we only care about this if there is armor remaining,
                    // so don't worry about the case where damage exceeds
                    // armorThreshold
                    if ((te instanceof Mech) && (damage > 0)) {
                        ((Mech) te).setArmorDamagedThisTurn(hit.getLocation(), true);
                    }

                    // if the armor is hardened, any penetrating crits are
                    // rolled at -2
                    if (hardenedArmor) {
                        critBonus -= 2;
                    }

                    if (tmpDamageHold >= 0) {
                        te.damageThisPhase += tmpDamageHold;
                    } else {
                        te.damageThisPhase += damage;
                    }
                    damage = 0;
                    if (!te.isHardenedArmorDamaged(hit)) {
                        r = new Report(6085);
                    } else {
                        r = new Report(6086);
                    }

                    r.subject = te_n;
                    r.indent(3);
                    r.add(te.getArmor(hit));
                    vDesc.addElement(r);

                    // telemissiles are destroyed if they lose all armor
                    if ((te instanceof TeleMissile)
                        && (te.getArmor(hit) == damage)) {
                        vDesc.addAll(server.destroyEntity(te, "damage", false));
                    }

                } else {
                    // damage goes on to internal
                    int absorbed = max(te.getArmor(hit), 0);
                    if (hardenedArmor
                        && (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING)
                        && (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING_MISSILE)) {
                        absorbed = (absorbed * 2)
                                   - ((te.isHardenedArmorDamaged(hit)) ? 1 : 0);
                    }
                    if (reflectiveArmor && (hit.getGeneralDamageType() == HitData.DAMAGE_PHYSICAL)
                            && !isBattleArmor) {
                        absorbed = (int) ceil(absorbed / 2.0);
                        damage = tmpDamageHold;
                        tmpDamageHold = 0;
                    }
                    te.setArmor(IArmorState.ARMOR_DESTROYED, hit);
                    if (tmpDamageHold >= 0) {
                        te.damageThisPhase += 2 * absorbed;
                    } else {
                        te.damageThisPhase += absorbed;
                    }
                    damage -= absorbed;
                    r = new Report(6090);
                    r.subject = te_n;
                    r.indent(3);
                    vDesc.addElement(r);
                    if (te instanceof GunEmplacement) {
                        // gun emplacements have no internal,
                        // destroy the section
                        te.destroyLocation(hit.getLocation());
                        r = new Report(6115);
                        r.subject = te_n;
                        vDesc.addElement(r);

                        if (te.getTransferLocation(hit).getLocation() == Entity.LOC_DESTROYED) {
                            vDesc.addAll(server.destroyEntity(te, "damage", false));
                        }
                    }
                }

                // targets with BAR armor get crits, depending on damage and BAR
                // rating
                if (te.hasBARArmor(hit.getLocation())) {
                    if (origDamage > te.getBARRating(hit.getLocation())) {
                        if (te.hasArmoredChassis()) {
                            // crit roll with -1 mod
                            vDesc.addAll(server.criticalEntity(te, hit.getLocation(),
                                                        hit.isRear(), -1 + critBonus, damage_orig));
                        } else {
                            vDesc.addAll(server.criticalEntity(te, hit.getLocation(),
                                                        hit.isRear(), critBonus, damage_orig));
                        }
                    }
                }

                if ((tmpDamageHold > 0) && isPlatoon) {
                    damage = tmpDamageHold;
                }
            }

            // For optional tank damage thresholds, the overthresh flag won't
            // be set if IS is damaged, so set it here.
            if ((te instanceof Tank)
                    && ((te.getArmor(hit) < 1) || damageIS)
                    && server.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)
                    && !((te instanceof VTOL)
                            || (te instanceof GunEmplacement))) {
                ((Tank) te).setOverThresh(true);
            }

            // is there damage remaining?
            if (damage > 0) {

                // if this is an Aero then I need to apply internal damage
                // to the SI after halving it. Return from here to prevent
                // further processing
                if (te instanceof Aero) {
                    Aero a = (Aero) te;

                    // check for large craft ammo explosions here: damage vented through armor, excess
                    // dissipating, much like Tank CASE.
                    if (ammoExplosion && te.isLargeCraft()) {
                        te.damageThisPhase += damage;
                        r = new Report(6128);
                        r.subject = te_n;
                        r.indent(2);
                        r.add(damage);
                        int loc = hit.getLocation();
                        //Roll for broadside weapons so fore/aft side armor facing takes the damage
                        if (loc == Warship.LOC_LBS) {
                            int locRoll = Compute.d6();
                            if (locRoll < 4) {
                                loc = Jumpship.LOC_FLS;
                            } else {
                                loc = Jumpship.LOC_ALS;
                            }
                        }
                        if (loc == Warship.LOC_RBS) {
                            int locRoll = Compute.d6();
                            if (locRoll < 4) {
                                loc = Jumpship.LOC_FRS;
                            } else {
                                loc = Jumpship.LOC_ARS;
                            }
                        }
                        r.add(te.getLocationAbbr(loc));
                        vDesc.add(r);
                        if (damage > te.getArmor(loc)) {
                            te.setArmor(IArmorState.ARMOR_DESTROYED, loc);
                            r = new Report(6090);
                        } else {
                            te.setArmor(te.getArmor(loc) - damage, loc);
                            r = new Report(6085);
                            r.add(te.getArmor(loc));
                        }
                        r.subject = te_n;
                        r.indent(3);
                        vDesc.add(r);
                        damage = 0;
                    }

                    // check for overpenetration
                    if (server.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_OVER_PENETRATE)) {
                        int opRoll = Compute.d6(1);
                        if (((te instanceof Jumpship) && !(te instanceof Warship) && (opRoll > 3))
                                || ((te instanceof Dropship) && (opRoll > 4))
                                || ((te instanceof Warship) && (a.get0SI() <= 30) && (opRoll > 5))) {
                            // over-penetration happened
                            r = new Report(9090);
                            r.subject = te_n;
                            r.newlines = 0;
                            vDesc.addElement(r);
                            int new_loc = a.getOppositeLocation(hit.getLocation());
                            damage = min(damage, te.getArmor(new_loc));
                            // We don't want to deal negative damage
                            damage = max(damage, 0);
                            r = new Report(6065);
                            r.subject = te_n;
                            r.indent(2);
                            r.newlines = 0;
                            r.addDesc(te);
                            r.add(damage);
                            r.add(te.getLocationAbbr(new_loc));
                            vDesc.addElement(r);
                            te.setArmor(te.getArmor(new_loc) - damage, new_loc);
                            if ((te instanceof Warship) || (te instanceof Dropship)) {
                                damage = 2;
                            } else {
                                damage = 0;
                            }
                        }
                    }

                    // divide damage in half
                    // do not divide by half if it is an ammo explosion
                    if (!ammoExplosion && !nukeS2S
                        && !server.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
                        damage /= 2;
                    }

                    // this should result in a crit
                    // but only if it really did damage after rounding down
                    if (damage > 0) {
                        critSI = true;
                    }

                    // Now apply damage to the structural integrity
                    a.setSI(a.getSI() - damage);
                    te.damageThisPhase += damage;
                    // send the report
                    r = new Report(1210);
                    r.subject = te_n;
                    r.newlines = 1;
                    if (!ammoExplosion) {
                        r.messageId = 9005;
                    }
                    //Only for fighters
                    if (ammoExplosion && !a.isLargeCraft()) {
                        r.messageId = 9006;
                    }
                    r.add(damage);
                    r.add(max(a.getSI(), 0));
                    vDesc.addElement(r);
                    // check to see if this would destroy the ASF
                    if (a.getSI() <= 0) {
                        // Lets auto-eject if we can!
                        if (a.isAutoEject()
                            && (!server.getGame().getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                    || (server.getGame().getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                            && a.isCondEjectSIDest()))) {
                            vDesc.addAll(server.ejectEntity(te, true, false));
                        } else {
                            vDesc.addAll(server.destroyEntity(te,"Structural Integrity Collapse"));
                        }
                        a.setSI(0);
                        if (hit.getAttackerId() != Entity.NONE) {
                            server.creditKill(a, server.getGame().getEntity(hit.getAttackerId()));
                        }
                    }
                    server.checkAeroCrits(vDesc, a, hit, damage_orig, critThresh, critSI, ammoExplosion, nukeS2S);
                    return vDesc;
                }

                // Check for CASE II right away. if so reduce damage to 1
                // and let it hit the IS.
                // Also remove as much of the rear armor as allowed by the
                // damage. If arm/leg/head
                // Then they lose all their armor if its less then the
                // explosion damage.
                if (ammoExplosion && te.hasCASEII(hit.getLocation())) {
                    // 1 point of damage goes to IS
                    damage--;
                    // Remaining damage prevented by CASE II
                    r = new Report(6126);
                    r.subject = te_n;
                    r.add(damage);
                    r.indent(3);
                    vDesc.addElement(r);
                    int loc = hit.getLocation();
                    if ((te instanceof Mech) && ((loc == Mech.LOC_HEAD) || ((Mech) te).isArm(loc)
                            || te.locationIsLeg(loc))) {
                        int half = (int) ceil(te.getOArmor(loc, false) / 2.0);
                        if (damage > half) {
                            damage = half;
                        }
                        if (damage >= te.getArmor(loc, false)) {
                            te.setArmor(IArmorState.ARMOR_DESTROYED, loc, false);
                        } else {
                            te.setArmor(te.getArmor(loc, false) - damage, loc, false);
                        }
                    } else {
                        if (damage >= te.getArmor(loc, true)) {
                            te.setArmor(IArmorState.ARMOR_DESTROYED, loc, true);
                        } else {
                            te.setArmor(te.getArmor(loc, true) - damage, loc, true);
                        }
                    }

                    if (te.getInternal(hit) > 0) {
                        // Mek takes 1 point of IS damage
                        damage = 1;
                    } else {
                        damage = 0;
                    }

                    te.damageThisPhase += damage;

                    int roll = Compute.d6(2);
                    r = new Report(6127);
                    r.subject = te.getId();
                    r.add(roll);
                    vDesc.add(r);
                    if (roll >= 8) {
                        hit.setEffect(HitData.EFFECT_NO_CRITICALS);
                    }
                }
                // check for tank CASE here: damage to rear armor, excess
                // dissipating, and a crew stunned crit
                if (ammoExplosion && (te instanceof Tank)
                    && te.locationHasCase(Tank.LOC_BODY)) {
                    te.damageThisPhase += damage;
                    r = new Report(6124);
                    r.subject = te_n;
                    r.indent(2);
                    r.add(damage);
                    vDesc.add(r);
                    int loc = (te instanceof SuperHeavyTank) ? SuperHeavyTank.LOC_REAR
                            : (te instanceof LargeSupportTank) ? LargeSupportTank.LOC_REAR : Tank.LOC_REAR;
                    if (damage > te.getArmor(loc)) {
                        te.setArmor(IArmorState.ARMOR_DESTROYED, loc);
                        r = new Report(6090);
                    } else {
                        te.setArmor(te.getArmor(loc) - damage, loc);
                        r = new Report(6085);
                        r.add(te.getArmor(loc));
                    }
                    r.subject = te_n;
                    r.indent(3);
                    vDesc.add(r);
                    damage = 0;
                    int critIndex;
                    if (((Tank) te).isCommanderHit()
                        && ((Tank) te).isDriverHit()) {
                        critIndex = Tank.CRIT_CREW_KILLED;
                    } else {
                        critIndex = Tank.CRIT_CREW_STUNNED;
                    }
                    vDesc.addAll(server.applyCriticalHit(te, Entity.NONE, new CriticalSlot(0, critIndex), true, 0, false));
                }

                // is there internal structure in the location hit?
                if (te.getInternal(hit) > 0) {

                    // Now we need to consider alternate structure types!
                    int tmpDamageHold = -1;
                    if ((te instanceof Mech)
                        && ((Mech) te).hasCompositeStructure()) {
                        tmpDamageHold = damage;
                        damage *= 2;
                        r = new Report(6091);
                        r.subject = te_n;
                        r.indent(3);
                        vDesc.add(r);
                    }
                    if ((te instanceof Mech)
                        && ((Mech) te).hasReinforcedStructure()) {
                        tmpDamageHold = damage;
                        damage /= 2;
                        damage += tmpDamageHold % 2;
                        r = new Report(6092);
                        r.subject = te_n;
                        r.indent(3);
                        vDesc.add(r);
                    }
                    if ((te.getInternal(hit) > damage) && (damage > 0)) {
                        // internal structure absorbs all damage
                        te.setInternal(te.getInternal(hit) - damage, hit);
                        // Triggers a critical hit on Vehicles and Mechs.
                        if (!isPlatoon && !isBattleArmor) {
                            crits++;
                        }
                        tookInternalDamage = true;
                        // Alternate structures don't affect our damage total
                        // for later PSR purposes, so use the previously stored
                        // value here as necessary.
                        te.damageThisPhase += (tmpDamageHold > -1) ?
                                tmpDamageHold : damage;
                        damage = 0;
                        r = new Report(6100);
                        r.subject = te_n;
                        r.indent(3);
                        // Infantry platoons have men not "Internals".
                        if (isPlatoon) {
                            r.messageId = 6095;
                        }
                        r.add(te.getInternal(hit));
                        vDesc.addElement(r);
                    } else if (damage > 0) {
                        // Triggers a critical hit on Vehicles and Mechs.
                        if (!isPlatoon && !isBattleArmor) {
                            crits++;
                        }
                        // damage transfers, maybe
                        int absorbed = max(te.getInternal(hit), 0);

                        // Handle ProtoMech pilot damage
                        // due to location destruction
                        if (te instanceof Protomech) {
                            int hits = Protomech.POSSIBLE_PILOT_DAMAGE[hit.getLocation()]
                                    - ((Protomech) te).getPilotDamageTaken(hit.getLocation());
                            if (hits > 0) {
                                vDesc.addAll(server.damageCrew(te, hits));
                                ((Protomech) te).setPilotDamageTaken(hit.getLocation(),
                                        Protomech.POSSIBLE_PILOT_DAMAGE[hit.getLocation()]);
                            }
                        }

                        // Platoon, Trooper, or Section destroyed message
                        r = new Report(1210);
                        r.subject = te_n;
                        if (isPlatoon) {
                            // Infantry have only one section, and
                            // are therefore destroyed.
                            if (((Infantry) te).isSquad()) {
                                r.messageId = 6106; // Squad Killed
                            } else {
                                r.messageId = 6105; // Platoon Killed
                            }
                        } else if (isBattleArmor) {
                            r.messageId = 6110;
                        } else {
                            r.messageId = 6115;
                        }
                        r.indent(3);
                        vDesc.addElement(r);

                        // If a sidetorso got destroyed, and the
                        // corresponding arm is not yet destroyed, add
                        // it as a club to that hex (p.35 BMRr)
                        if ((te instanceof Mech)
                                && (((hit.getLocation() == Mech.LOC_RT)
                                        && (te.getInternal(Mech.LOC_RARM) > 0))
                                    || ((hit.getLocation() == Mech.LOC_LT)
                                        && (te.getInternal(Mech.LOC_LARM) > 0)))) {
                            int blownOffLocation;
                            if (hit.getLocation() == Mech.LOC_RT) {
                                blownOffLocation = Mech.LOC_RARM;
                            } else {
                                blownOffLocation = Mech.LOC_LARM;
                            }
                            te.destroyLocation(blownOffLocation, true);
                            r = new Report(6120);
                            r.subject = te_n;
                            r.add(te.getLocationName(blownOffLocation));
                            vDesc.addElement(r);
                            Hex h = server.getGame().getBoard().getHex(te.getPosition());
                            if (null != h) {
                                if (te instanceof BipedMech) {
                                    if (!h.containsTerrain(Terrains.ARMS)) {
                                        h.addTerrain(new Terrain(Terrains.ARMS, 1));
                                    } else {
                                        h.addTerrain(new Terrain(Terrains.ARMS, h.terrainLevel(Terrains.ARMS) + 1));
                                    }
                                } else if (!h.containsTerrain(Terrains.LEGS)) {
                                    h.addTerrain(new Terrain(Terrains.LEGS, 1));
                                } else {
                                    h.addTerrain(new Terrain(Terrains.LEGS, h.terrainLevel(Terrains.LEGS) + 1));
                                }
                                server.sendChangedHex(te.getPosition());
                            }
                        }

                        // Troopers riding on a location
                        // all die when the location is destroyed.
                        if ((te instanceof Mech) || (te instanceof Tank)) {
                            Entity passenger = te.getExteriorUnitAt(
                                    hit.getLocation(), hit.isRear());
                            if ((null != passenger) && !passenger.isDoomed()) {
                                HitData passHit = passenger
                                        .getTrooperAtLocation(hit, te);
                                // ensures a kill
                                passHit.setEffect(HitData.EFFECT_CRITICAL);
                                if (passenger.getInternal(passHit) > 0) {
                                    vDesc.addAll(server.damageEntity(passenger,
                                                              passHit, damage));
                                }
                                passHit = new HitData(hit.getLocation(),
                                                      !hit.isRear());
                                passHit = passenger.getTrooperAtLocation(
                                        passHit, te);
                                // ensures a kill
                                passHit.setEffect(HitData.EFFECT_CRITICAL);
                                if (passenger.getInternal(passHit) > 0) {
                                    vDesc.addAll(server.damageEntity(passenger,
                                                              passHit, damage));
                                }
                            }
                        }

                        // BA inferno explosions
                        if (te instanceof BattleArmor) {
                            int infernos = 0;
                            for (Mounted m : te.getEquipment()) {
                                if (m.getType() instanceof AmmoType) {
                                    AmmoType at = (AmmoType) m.getType();
                                    if (((at.getAmmoType() == AmmoType.T_SRM) || (at.getAmmoType() == AmmoType.T_MML))
                                            && (at.getMunitionType() == AmmoType.M_INFERNO)) {
                                        infernos += at.getRackSize() * m.getHittableShotsLeft();
                                    }
                                } else if (m.getType().hasFlag(MiscType.F_FIRE_RESISTANT)) {
                                    // immune to inferno explosion
                                    infernos = 0;
                                    break;
                                }
                            }
                            if (infernos > 0) {
                                int roll = Compute.d6(2);
                                r = new Report(6680);
                                r.add(roll);
                                vDesc.add(r);
                                if (roll >= 8) {
                                    Coords c = te.getPosition();
                                    if (c == null) {
                                        Entity transport = server.getGame().getEntity(te.getTransportId());
                                        if (transport != null) {
                                            c = transport.getPosition();
                                        }
                                        server.getvPhaseReport().addAll(server.deliverInfernoMissiles(te, te, infernos));
                                    }
                                    if (c != null) {
                                        server.getvPhaseReport().addAll(server.deliverInfernoMissiles(te,
                                                new HexTarget(c, Targetable.TYPE_HEX_ARTILLERY),
                                                infernos));
                                    }
                                }
                            }
                        }

                        // Mark off the internal structure here, but *don't*
                        // destroy the location just yet -- there are checks
                        // still to run!
                        te.setInternal(0, hit);
                        te.damageThisPhase += absorbed;
                        damage -= absorbed;

                        // Now we need to consider alternate structure types!
                        if (tmpDamageHold > 0) {
                            if (((Mech) te).hasCompositeStructure()) {
                                // If there's a remainder, we can actually
                                // ignore it.
                                damage /= 2;
                            } else if (((Mech) te).hasReinforcedStructure()) {
                                damage *= 2;
                                damage -= tmpDamageHold % 2;
                            }
                        }
                    }
                }
                if (te.getInternal(hit) <= 0) {
                    // internal structure is gone, what are the transfer
                    // potentials?
                    nextHit = te.getTransferLocation(hit);
                    if (nextHit.getLocation() == Entity.LOC_DESTROYED) {
                        if (te instanceof Mech) {
                            // Start with the number of engine crits in this
                            // location, if any...
                            te.engineHitsThisPhase += te.getNumberOfCriticals(
                                    CriticalSlot.TYPE_SYSTEM,
                                    Mech.SYSTEM_ENGINE, hit.getLocation());
                            // ...then deduct the ones destroyed previously or
                            // critically
                            // hit this round already. That leaves the ones
                            // actually
                            // destroyed with the location.
                            te.engineHitsThisPhase -= te.getHitCriticals(
                                    CriticalSlot.TYPE_SYSTEM,
                                    Mech.SYSTEM_ENGINE, hit.getLocation());
                        }

                        boolean engineExploded = server.checkEngineExplosion(te,
                                vDesc, te.engineHitsThisPhase);

                        if (!engineExploded) {
                            // Entity destroyed. Ammo explosions are
                            // neither survivable nor salvageable.
                            // Only ammo explosions in the CT are devastating.
                            vDesc.addAll(server.destroyEntity(te, "damage", !ammoExplosion,
                                    !((ammoExplosion || areaSatArty) && ((te instanceof Tank)
                                            || ((te instanceof Mech) && (hit.getLocation() == Mech.LOC_CT))))));
                            // If the head is destroyed, kill the crew.

                            if ((te instanceof Mech) && (hit.getLocation() == Mech.LOC_HEAD)
                                    && !te.getCrew().isDead() && !te.getCrew().isDoomed()
                                    && server.getGame().getOptions().booleanOption(
                                            OptionsConstants.ADVANCED_TACOPS_SKIN_OF_THE_TEETH_EJECTION)) {
                                Mech mech = (Mech) te;
                                if (mech.isAutoEject()
                                        && (!server.getGame().getOptions().booleanOption(
                                                OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                        || (server.getGame().getOptions().booleanOption(
                                                OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                                && mech.isCondEjectHeadshot()))) {
                                    autoEject = true;
                                    vDesc.addAll(server.ejectEntity(te, true, true));
                                }
                            }

                            if ((te instanceof Mech) && (hit.getLocation() == Mech.LOC_CT)
                                    && !te.getCrew().isDead() && !te.getCrew().isDoomed()) {
                                Mech mech = (Mech) te;
                                if (mech.isAutoEject()
                                        && server.getGame().getOptions().booleanOption(
                                                OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                        && mech.isCondEjectCTDest()) {
                                    if (mech.getCrew().getHits() < 5) {
                                        Report.addNewline(vDesc);
                                        mech.setDoomed(false);
                                        mech.setDoomed(true);
                                    }
                                    autoEject = true;
                                    vDesc.addAll(server.ejectEntity(te, true));
                                }
                            }

                            if ((hit.getLocation() == Mech.LOC_HEAD)
                                    || ((hit.getLocation() == Mech.LOC_CT)
                                    && ((ammoExplosion && !autoEject) || areaSatArty))) {
                                te.getCrew().setDoomed(true);
                            }
                            if (server.getGame().getOptions().booleanOption(
                                    OptionsConstants.ADVGRNDMOV_AUTO_ABANDON_UNIT)) {
                                vDesc.addAll(server.abandonEntity(te));
                            }
                        }

                        // nowhere for further damage to go
                        damage = 0;
                    } else if (nextHit.getLocation() == Entity.LOC_NONE) {
                        // Rest of the damage is wasted.
                        damage = 0;
                    } else if (ammoExplosion
                               && te.locationHasCase(hit.getLocation())) {
                        // Remaining damage prevented by CASE
                        r = new Report(6125);
                        r.subject = te_n;
                        r.add(damage);
                        r.indent(3);
                        vDesc.addElement(r);

                        // The target takes no more damage from the explosion.
                        damage = 0;
                    } else if (damage > 0) {
                        // remaining damage transfers
                        r = new Report(6130);
                        r.subject = te_n;
                        r.indent(2);
                        r.add(damage);
                        r.add(te.getLocationAbbr(nextHit));
                        vDesc.addElement(r);

                        // If there are split weapons in this location, mark it
                        // as hit, even if it took no criticals.
                        for (Mounted m : te.getWeaponList()) {
                            if (m.isSplit()) {
                                if ((m.getLocation() == hit.getLocation())
                                    || (m.getLocation() == nextHit
                                        .getLocation())) {
                                    te.setWeaponHit(m);
                                }
                            }
                        }
                        // if this is damage from a nail/rivet gun, and we
                        // transfer
                        // to a location that has armor, and BAR >=5, no damage
                        if ((damageType == DamageType.NAIL_RIVET)
                            && (te.getArmor(nextHit.getLocation()) > 0)
                            && (te.getBARRating(nextHit.getLocation()) >= 5)) {
                            damage = 0;
                            r = new Report(6065);
                            r.subject = te_n;
                            r.indent(2);
                            vDesc.add(r);
                        }
                    }
                }
            } else if (hit.getSpecCrit()) {
                // ok, we dealt damage but didn't go on to internal
                // we get a chance of a crit, using Armor Piercing.
                // but only if we don't have hardened, Ferro-Lamellor, or reactive armor
                if (!hardenedArmor && !ferroLamellorArmor && !reactiveArmor) {
                    specCrits++;
                }
            }
            // check for breaching
            vDesc.addAll(server.breachCheck(te, hit.getLocation(), null, underWater));

            // resolve special results
            if ((hit.getEffect() & HitData.EFFECT_VEHICLE_MOVE_DAMAGED) == HitData.EFFECT_VEHICLE_MOVE_DAMAGED) {
                vDesc.addAll(server.vehicleMotiveDamage((Tank) te, hit.getMotiveMod()));
            }
            // Damage from any source can break spikes
            if (te.hasWorkingMisc(MiscType.F_SPIKES, -1, hit.getLocation())) {
                vDesc.add(server.checkBreakSpikes(te, hit.getLocation()));
            }

            // roll all critical hits against this location
            // unless the section destroyed in a previous phase?
            // Cause a crit.
            if ((te.getInternal(hit) != IArmorState.ARMOR_DESTROYED)
                    && ((hit.getEffect() & HitData.EFFECT_NO_CRITICALS) != HitData.EFFECT_NO_CRITICALS)) {
                for (int i = 0; i < crits; i++) {
                    vDesc.addAll(server.criticalEntity(te, hit.getLocation(), hit.isRear(),
                            hit.glancingMod() + critBonus, damage_orig, damageType));
                }
                crits = 0;

                for (int i = 0; i < specCrits; i++) {
                    // against BAR or reflective armor, we get a +2 mod
                    int critMod = te.hasBARArmor(hit.getLocation()) ? 2 : 0;
                    critMod += (reflectiveArmor && !isBattleArmor) ? 2 : 0; // BA
                    // against impact armor, we get a +1 mod
                    critMod += impactArmor ? 1 : 0;
                    // hardened armour has no crit penalty
                    if (!hardenedArmor) {
                        // non-hardened armor gets modifiers
                        // the -2 for hardened is handled in the critBonus
                        // variable
                        critMod += hit.getSpecCritMod();
                        critMod += hit.glancingMod();
                    }
                    vDesc.addAll(server.criticalEntity(te, hit.getLocation(), hit.isRear(),
                            critMod + critBonus, damage_orig));
                }
                specCrits = 0;
            }

            // resolve Aero crits
            if (te instanceof Aero) {
                server.checkAeroCrits(vDesc, (Aero) te, hit, damage_orig, critThresh, critSI,
                        ammoExplosion, nukeS2S);
            }

            if (isHeadHit
                && !te.hasAbility(OptionsConstants.MD_DERMAL_ARMOR)) {
                Report.addNewline(vDesc);
                vDesc.addAll(server.damageCrew(te, 1));
            }

            // If the location has run out of internal structure, finally
            // actually
            // destroy it here. *EXCEPTION:* Aero units have 0 internal
            // structure
            // in every location by default and are handled elsewhere, so they
            // get a bye.
            if (!(te instanceof Aero) && (te.getInternal(hit) <= 0)) {
                te.destroyLocation(hit.getLocation());

                // Check for possible engine destruction here
                if ((te instanceof Mech)
                        && ((hit.getLocation() == Mech.LOC_RT) || (hit.getLocation() == Mech.LOC_LT))) {

                    int numEngineHits = te.getEngineHits();
                    boolean engineExploded = server.checkEngineExplosion(te, vDesc, numEngineHits);

                    int hitsToDestroy = 3;
                    if ((te instanceof Mech) && te.isSuperHeavy() && te.hasEngine()
                            && (te.getEngine().getEngineType() == Engine.COMPACT_ENGINE)) {
                        hitsToDestroy = 2;
                    }

                    if (!engineExploded && (numEngineHits >= hitsToDestroy)) {
                        // third engine hit
                        vDesc.addAll(server.destroyEntity(te, "engine destruction"));
                        if (server.getGame().getOptions()
                                .booleanOption(OptionsConstants.ADVGRNDMOV_AUTO_ABANDON_UNIT)) {
                            vDesc.addAll(server.abandonEntity(te));
                        }
                        te.setSelfDestructing(false);
                        te.setSelfDestructInitiated(false);
                    }

                    // Torso destruction in airborne LAM causes immediate crash.
                    if ((te instanceof LandAirMech) && !te.isDestroyed() && !te.isDoomed()) {
                        r = new Report(9710);
                        r.subject = te.getId();
                        r.addDesc(te);
                        if (te.isAirborneVTOLorWIGE()) {
                            vDesc.add(r);
                            server.crashAirMech(te, new PilotingRollData(te.getId(), TargetRoll.AUTOMATIC_FAIL,
                                    "side torso destroyed"), vDesc);
                        } else if (te.isAirborne() && te.isAero()) {
                            vDesc.add(r);
                            vDesc.addAll(server.processCrash(te, ((IAero) te).getCurrentVelocity(), te.getPosition()));
                        }
                    }
                }

            }

            // If damage remains, loop to next location; if not, be sure to stop
            // here because we may need to refer back to the last *damaged*
            // location again later. (This is safe because at damage <= 0 the
            // loop terminates anyway.)
            if (damage > 0) {
                hit = nextHit;
                // Need to update armor status for the new location
                hardenedArmor = ((te instanceof Mech) || (te instanceof Tank))
                        && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HARDENED);
                ferroLamellorArmor = ((te instanceof Mech) || (te instanceof Tank) || (te instanceof Aero))
                        && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_FERRO_LAMELLOR);
                reflectiveArmor = (((te instanceof Mech) || (te instanceof Tank) || (te instanceof Aero))
                        && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_REFLECTIVE))
                        || (isBattleArmor
                                && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_BA_REFLECTIVE));
                reactiveArmor = (((te instanceof Mech) || (te instanceof Tank) || (te instanceof Aero))
                        && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_REACTIVE))
                        || (isBattleArmor && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_BA_REACTIVE));
                ballisticArmor = ((te instanceof Mech) || (te instanceof Tank) || (te instanceof Aero))
                        && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_BALLISTIC_REINFORCED);
                impactArmor = (te instanceof Mech)
                        && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_IMPACT_RESISTANT);
            }
            if (damageIS) {
                wasDamageIS = true;
                damageIS = false;
            }
        }
        // Mechs using EI implants take pilot damage each time a hit
        // inflicts IS damage
        if (tookInternalDamage
            && ((te instanceof Mech) || (te instanceof Protomech))
            && te.hasActiveEiCockpit()) {
            Report.addNewline(vDesc);
            int roll = Compute.d6(2);
            r = new Report(5075);
            r.subject = te.getId();
            r.addDesc(te);
            r.add(7);
            r.add(roll);
            r.choose(roll >= 7);
            r.indent(2);
            vDesc.add(r);
            if (roll < 7) {
                vDesc.addAll(server.damageCrew(te, 1));
            }
        }

        // if using VDNI (but not buffered), check for damage on an internal hit
        if (tookInternalDamage
            && te.hasAbility(OptionsConstants.MD_VDNI)
            && !te.hasAbility(OptionsConstants.MD_BVDNI)
            && !te.hasAbility(OptionsConstants.MD_PAIN_SHUNT)) {
            Report.addNewline(vDesc);
            int roll = Compute.d6(2);
            r = new Report(3580);
            r.subject = te.getId();
            r.addDesc(te);
            r.add(7);
            r.add(roll);
            r.choose(roll >= 8);
            r.indent(2);
            vDesc.add(r);
            if (roll >= 8) {
                vDesc.addAll(server.damageCrew(te, 1));
            }
        }

        // TacOps p.78 Ammo booms can hurt other units in same and adjacent hexes
        // But, this does not apply to CASE'd units and it only applies if the
        // ammo explosion
        // destroyed the unit
        if (ammoExplosion && server.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_AMMUNITION)
            // For 'Mechs we care whether there was CASE specifically in the
            // location that went boom...
            && !(te.locationHasCase(hit.getLocation()) || te.hasCASEII(hit.getLocation()))
            // ...but vehicles and ASFs just have one CASE item for the
            // whole unit, so we need to look whether there's CASE anywhere
            // at all.
            && !(((te instanceof Tank) || (te instanceof Aero)) && te
                .hasCase()) && (te.isDestroyed() || te.isDoomed())
            && (damage_orig > 0) && ((damage_orig / 10) > 0)) {
            Report.addNewline(vDesc);
            r = new Report(5068, Report.PUBLIC);
            r.subject = te.getId();
            r.addDesc(te);
            r.indent(2);
            vDesc.add(r);
            Report.addNewline(vDesc);
            r = new Report(5400, Report.PUBLIC);
            r.subject = te.getId();
            r.indent(2);
            vDesc.add(r);
            int[] damages = {(int) floor(damage_orig / 10.0),
                             (int) floor(damage_orig / 20.0)};
            server.doExplosion(damages, false, te.getPosition(), true, vDesc, null, 5,
                        te.getId(), false);
            Report.addNewline(vDesc);
            r = new Report(5410, Report.PUBLIC);
            r.subject = te.getId();
            r.indent(2);
            vDesc.add(r);
        }

        // This flag indicates the hit was directly to IS
        if (wasDamageIS) {
            Report.addNewline(vDesc);
        }
        return vDesc;
    }
}
