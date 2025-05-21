package megamek.server.totalwarfare;

import megamek.common.*;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.Atmosphere;
import megamek.common.weapons.DamageType;
import megamek.logging.MMLogger;
import megamek.server.IDamageManager;
import megamek.server.ServerHelper;

import java.util.List;
import java.util.Vector;

public class TWDamageManagerNew extends TWDamageManager implements IDamageManager {
    private static final MMLogger logger = MMLogger.create(TWDamageManagerNew.class);

    public TWDamageManagerNew() {
        super();
    }

    public TWDamageManagerNew(TWGameManager manager) {
        super(manager);
    }

    public TWDamageManagerNew(TWGameManager manager, Game game) {
        super(manager, game);
    }

    /**
     * Top-level damage function; calls specialized functions to deal damage to specific unit types.
     *
     * @param te            Entity being damaged
     * @param hit           HitData recording aspects of the incoming damage
     * @param damage        Actual amount of incoming damage
     * @param ammoExplosion Whether damage was caused by an ammo explosion
     * @param damageType    Type of damage, mainly used for specialized armor
     * @param damageIS      Whether damage is going straight to internal structure
     * @param areaSatArty   Whether damage is caused by AE attack
     * @param throughFront  Through front arc or no, for some specialized armors
     * @param underWater    Whether damage is being dealt underwater, for breach check
     * @param nukeS2S       Whether damage is from a nuclear weapon
     * @param vDesc         Vector of Reports containing prior reports; usually modded and returned
     * @return
     */
    @Override
    public Vector<Report> damageEntity(Entity te, HitData hit, int damage, boolean ammoExplosion, DamageType damageType,
          boolean damageIS, boolean areaSatArty, boolean throughFront, boolean underWater, boolean nukeS2S,
        Vector<Report> vDesc) {

        Report r;
        int te_n = te.getId();

        // if this is a fighter squadron then pick an active fighter and pass on
        // the damage
        if (te instanceof FighterSquadron) {
            damageSquadronFighter(vDesc, te, hit, damage, ammoExplosion, damageType, damageIS,
                  areaSatArty, throughFront, underWater, nukeS2S);
            return vDesc;
        }

        // Battle Armor takes full damage to each trooper from area-effect.
        if (areaSatArty && (te instanceof BattleArmor)) {
            damageMultipleBAs(vDesc, te, hit, damage, ammoExplosion, damageType, damageIS,
                  areaSatArty, throughFront, underWater, nukeS2S);
            return vDesc;
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
        }

        // TACs from the hit location table
        int crits;
        if ((hit.getEffect() & HitData.EFFECT_CRITICAL) == HitData.EFFECT_CRITICAL) {
            crits = 1;
        } else {
            crits = 0;
        }

        // Store information to pass around
        ModsInfo mods = createDamageModifiers(te, hit, damageIS, damage_orig, crits);
        mods.critBonus = calcCritBonus(game.getEntity(hit.getAttackerId()), te, damage_orig, areaSatArty);

        // Some "hits" on a ProtoMek are actually misses.
        if ((te instanceof ProtoMek proto) && (hit.getLocation() == ProtoMek.LOC_NMISS)) {
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


        // Allocate the damage
        // Use different damageX methods to deal damage here
        // Don't pass vDesc back and forth, that's wasteful.
        if (te instanceof ProtoMek teCast) {
            damageProtoMek(vDesc, teCast, hit, damage, ammoExplosion, damageType,
                  areaSatArty, throughFront, underWater, nukeS2S, mods);
        } else if (te instanceof Mek teCast) {
            damageMek(vDesc, teCast, hit, damage, ammoExplosion, damageType,
                  areaSatArty, throughFront, underWater, nukeS2S, mods);
        } else if (te instanceof Aero teCast) {
            damageAeroSpace(vDesc, teCast, hit, damage, ammoExplosion, damageType,
                  areaSatArty, throughFront, underWater, nukeS2S, mods);
        } else if (te instanceof Tank teCast) {
            damageTank(vDesc, teCast, hit, damage, ammoExplosion, damageType,
                  areaSatArty, throughFront, underWater, nukeS2S, mods);
        } else if (te instanceof BattleArmor teCast) {
            damageBA(vDesc, teCast, hit, damage, ammoExplosion, damageType,
                  areaSatArty, throughFront, underWater, nukeS2S, mods);
        } else if (te instanceof Infantry teCast && teCast.isConventionalInfantry()) {
            damageInfantry(vDesc, teCast, hit, damage, ammoExplosion, damageType,
                  areaSatArty, throughFront, underWater, nukeS2S, mods);
        } else {
            logger.error(new UnknownEntityTypeException(te.toString()));
        }

        boolean tookInternalDamage = mods.tookInternalDamage;

        // Meks using EI implants take pilot damage each time a hit
        // inflicts IS damage
        if (tookInternalDamage && ((te instanceof Mek) || (te instanceof ProtoMek)) && te.hasActiveEiCockpit()) {
            Report.addNewline(vDesc);
            Roll diceRoll = Compute.rollD6(2);
            r = new Report(5075);
            r.subject = te.getId();
            r.addDesc(te);
            r.add(7);
            r.add(diceRoll);
            r.choose(diceRoll.getIntValue() >= 7);
            r.indent(2);
            vDesc.add(r);
            if (diceRoll.getIntValue() < 7) {
                vDesc.addAll(manager.damageCrew(te, 1));
            }
        }

        // if using VDNI (but not buffered), check for damage on an internal hit
        if (tookInternalDamage &&
                  te.hasAbility(OptionsConstants.MD_VDNI) &&
                  !te.hasAbility(OptionsConstants.MD_BVDNI) &&
                  !te.hasAbility(OptionsConstants.MD_PAIN_SHUNT)) {
            Report.addNewline(vDesc);
            Roll diceRoll = Compute.rollD6(2);
            r = new Report(3580);
            r.subject = te.getId();
            r.addDesc(te);
            r.add(7);
            r.add(diceRoll);
            r.choose(diceRoll.getIntValue() >= 8);
            r.indent(2);
            vDesc.add(r);

            if (diceRoll.getIntValue() >= 8) {
                vDesc.addAll(manager.damageCrew(te, 1));
            }
        }

        // TacOps p.78 Ammo booms can hurt other units in same and adjacent hexes
        // But, this does not apply to CASE'd units and it only applies if the
        // ammo explosion
        // destroyed the unit
        if (ammoExplosion &&
                  game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_AMMUNITION)
                  // For 'Meks we care whether there was CASE specifically in the
                  // location that went boom...
                  &&
                  !(te.locationHasCase(hit.getLocation()) || te.hasCASEII(hit.getLocation()))
                  // ...but vehicles and ASFs just have one CASE item for the
                  // whole unit, so we need to look whether there's CASE anywhere
                  // at all.
                  &&
                  !(((te instanceof Tank) || (te instanceof Aero)) && te.hasCase()) &&
                  (te.isDestroyed() || te.isDoomed()) &&
                  (damage_orig > 0) &&
                  ((damage_orig / 10) > 0)) {
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
            int[] damages = { (int) Math.floor(damage_orig / 10.0), (int) Math.floor(damage_orig / 20.0) };
            manager.doExplosion(damages, false, te.getPosition(), true, vDesc, null, 5, te.getId(), false, false);
            Report.addNewline(vDesc);
            r = new Report(5410, Report.PUBLIC);
            r.subject = te.getId();
            r.indent(2);
            vDesc.add(r);
        }

        // This flag indicates the hit was directly to IS
        if (mods.wasDamageIS) {
            Report.addNewline(vDesc);
        }
        return vDesc;
    }


    public void damageProtoMek(Vector<Report> vDesc, ProtoMek te, HitData hit, int damage,
          boolean ammoExplosion, DamageType damageType,
          boolean areaSatArty, boolean throughFront, boolean underWater,
          boolean nukeS2S, ModsInfo mods)
    {
        int te_n = te.getId();
        Report r;
        boolean autoEject = false;
        HitData nextHit = null;
        boolean damageIS = mods.damageIS;

        // Accumulate crits here.
        int crits = mods.crits;

        while (damage > 0) {
            // Apply damage to armor
            damage = applyEntityArmorDamage(te, hit, damage, ammoExplosion, damageIS, areaSatArty, vDesc, mods);

            // is there damage remaining?
            if (damage > 0) {

                // is there internal structure in the location hit?
                if (te.getInternal(hit) > 0) {

                    // Now we need to consider alternate structure types!
                    int tmpDamageHold = -1;
                    if ((te.getInternal(hit) > damage) && (damage > 0)) {
                        // internal structure absorbs all damage
                        te.setInternal(te.getInternal(hit) - damage, hit);
                        // Triggers a critical hit on Vehicles and Meks.
                        crits++;
                        mods.tookInternalDamage = true;
                        // Alternate structures don't affect our damage total
                        // for later PSR purposes, so use the previously stored
                        // value here as necessary.
                        te.damageThisPhase += (tmpDamageHold > -1) ? tmpDamageHold : damage;
                        damage = 0;
                        r = new Report(6100);
                        r.subject = te_n;
                        r.indent(3);
                        r.add(te.getInternal(hit));
                        vDesc.addElement(r);
                    } else if (damage > 0) {
                        // Triggers a critical hit on Vehicles and Meks.
                        crits++;
                        // damage transfers, maybe
                        int absorbed = Math.max(te.getInternal(hit), 0);

                        // Handle ProtoMek pilot damage
                        // due to location destruction
                        int hits = ProtoMek.POSSIBLE_PILOT_DAMAGE[hit.getLocation()]
                                         - ((ProtoMek) te).getPilotDamageTaken(hit.getLocation());
                        if (hits > 0) {
                            vDesc.addAll(manager.damageCrew(te, hits));
                            ((ProtoMek) te).setPilotDamageTaken(hit.getLocation(),
                                  ProtoMek.POSSIBLE_PILOT_DAMAGE[hit.getLocation()]);
                        }

                        // Platoon, Trooper, or Section destroyed message
                        r = new Report(1210);
                        r.subject = te_n;
                        r.messageId = 6115;
                        r.indent(3);
                        vDesc.addElement(r);

                        // Mark off the internal structure here, but *don't*
                        // destroy the location just yet -- there are checks
                        // still to run!
                        te.setInternal(0, hit);
                        te.damageThisPhase += absorbed;
                        damage -= absorbed;
                    }
                }

                if (te.getInternal(hit) <= 0) {
                    // internal structure is gone, what are the transfer
                    // potentials?
                    nextHit = te.getTransferLocation(hit);
                    if (nextHit.getLocation() == Entity.LOC_DESTROYED) {

                        // No engine explosions for ProtoMeks
                        // Entity destroyed. Ammo explosions are
                        // neither survivable nor salvageable.
                        // Only ammo explosions in the CT are devastating.
                        vDesc.addAll(manager.destroyEntity(te, "damage", !ammoExplosion,
                              !(ammoExplosion || areaSatArty)));
                        // If the head is destroyed, kill the crew.

                        if ((hit.getLocation() == Mek.LOC_HEAD)
                                  || ((hit.getLocation() == Mek.LOC_CT)
                                            && ((ammoExplosion && !autoEject) || areaSatArty))) {
                            te.getCrew().setDoomed(true);
                        }
                        if (game.getOptions().booleanOption(
                              OptionsConstants.ADVGRNDMOV_AUTO_ABANDON_UNIT)) {
                            vDesc.addAll(manager.abandonEntity(te));
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
                        for (WeaponMounted m : te.getWeaponList()) {
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
                if (!(mods.hardenedArmor || mods.ferroLamellorArmor || mods.reactiveArmor)) {
                    mods.specCrits = mods.specCrits + 1;
                }
            }
            mods.crits = crits;

            // check for breaching
            vDesc.addAll(manager.breachCheck(te, hit.getLocation(), null, underWater));

            // Deal special effect damage and crits
            dealSpecialCritEffects(te, vDesc, hit, mods, underWater, damageType);

            // If the location has run out of internal structure, finally
            // actually
            // destroy it here. *EXCEPTION:* Aero units have 0 internal
            // structure
            // in every location by default and are handled elsewhere, so they
            // get a bye.
            if ((te.getInternal(hit) <= 0)) {
                te.destroyLocation(hit.getLocation());
            }

            // If damage remains, loop to next location; if not, be sure to stop
            // here because we may need to refer back to the last *damaged*
            // location again later. (This is safe because at damage <= 0 the
            // loop terminates anyway.)
            if (damage > 0) {
                hit = nextHit;
                updateArmorTypeMap(mods, te, hit);
            }
            if (damageIS) {
                mods.wasDamageIS = true;
                damageIS = false;
            }
        }

    }

    public void damageMek(Vector<Report> vDesc, Mek te, HitData hit, int damage,
          boolean ammoExplosion, DamageType damageType,
          boolean areaSatArty, boolean throughFront, boolean underWater,
          boolean nukeS2S, ModsInfo mods) {
        // This is good for shields if a shield absorbs the hit it shouldn't
        // effect the pilot.
        // TC SRM's that hit the head do external and internal damage but its
        // one hit and shouldn't cause 2 hits to the pilot.
        mods.isHeadHit = ((te.getCockpitType() != Mek.COCKPIT_TORSO_MOUNTED)
                                        && (hit.getLocation() == Mek.LOC_HEAD)
                                        && ((hit.getEffect() & HitData.EFFECT_NO_CRITICALS) !=
                                                  HitData.EFFECT_NO_CRITICALS));
        int te_n = te.getId();
        Entity ae = game.getEntity(hit.getAttackerId());
        Report r;
        boolean autoEject = false;
        boolean damageIS = mods.damageIS;

        if (ammoExplosion) {
            if (te.isAutoEject() && (!game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                           || (game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                                     && te.isCondEjectAmmo()))) {
                autoEject = true;
                vDesc.addAll(manager.ejectEntity(te, true));
            }
        }

        HitData nextHit = null;

        damage = manageDamageTypeReports(te, vDesc, damage, damageType, hit, false, mods);

        // Accumulate crits here.
        int crits = mods.crits;

        // Allocate the damage
        while (damage > 0) {
            // damage some cargo if we're taking damage
            // maybe move past "exterior passenger" check
            if (!ammoExplosion) {
                int damageLeftToCargo = damage;

                for (ICarryable cargo : te.getDistinctCarriedObjects()) {
                    if (cargo.isInvulnerable()) {
                        continue;
                    }

                    double tonnage = cargo.getTonnage();
                    boolean cargoDestroyed = cargo.damage(damageLeftToCargo);
                    damageLeftToCargo -= Math.ceil(tonnage);

                    // if we have destroyed the cargo, remove it, add a report
                    // and move on to the next piece of cargo
                    if (cargoDestroyed) {
                        te.dropGroundObject(cargo, false);

                        r = new Report(6721);
                        r.subject = te_n;
                        r.indent(2);
                        r.add(cargo.generalName());
                        vDesc.addElement(r);
                        // we have not destroyed the cargo means there is no damage left
                        // report and stop destroying cargo
                    } else {
                        r = new Report(6720);
                        r.subject = te_n;
                        r.indent(2);
                        r.add(cargo.generalName());
                        r.add(Double.toString(cargo.getTonnage()));
                        break;
                    }
                }
            }

            // Report this either way
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

            if (ammoExplosion) {
                if (te instanceof LandAirMek) {
                    // LAMs eject if the CT destroyed switch is on
                    LandAirMek lam = (LandAirMek) te;
                    if (lam.isAutoEject()
                              && (!game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                        || (game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                                  && lam.isCondEjectCTDest()))) {
                        vDesc.addAll(manager.ejectEntity(te, true, false));
                    }
                }
            }

            // was the section destroyed earlier this phase?
            if (te.getInternal(hit) == IArmorState.ARMOR_DOOMED) {
                // cannot transfer a through armor crit if so
                mods.crits = 0;
            }

            // here goes the fun :)
            // Shields take damage first then cowls then armor whee
            // Shield does not protect from ammo explosions or falls.
            if (!ammoExplosion && !hit.isFallDamage() && !damageIS && te.hasShield()
                      && ((hit.getEffect() & HitData.EFFECT_NO_CRITICALS) != HitData.EFFECT_NO_CRITICALS)) {
                int damageNew = te.shieldAbsorptionDamage(damage, hit.getLocation(), hit.isRear());
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
                        mods.crits = 0;
                        mods.specCrits = 0;
                        mods.isHeadHit = false;
                    }
                }
            }

            // Armored Cowl may absorb some damage from hit
            if (te.hasCowl() && (hit.getLocation() == Mek.LOC_HEAD)
                      && ((te.getPosition() == null) || (ae == null)
                                || !te.getPosition().isOnHexRow(te.getSecondaryFacing(), ae.getPosition()))) {
                int excessDamage = te.damageCowl(damage);
                int blockedByCowl = damage - excessDamage;
                r = new Report(3520).subject(te_n).indent(3).add(blockedByCowl);
                vDesc.addElement(r);
                te.damageThisPhase += blockedByCowl;
                damage = excessDamage;
            }

            damage = applyModularArmor(te, hit, damage, ammoExplosion, damageIS, vDesc);

            // Destroy searchlights on 7+ (torso hits on mechs)
            if (te.hasSearchlight()) {
                boolean spotlightHittable = true;
                int loc = hit.getLocation();
                if ((loc != Mek.LOC_CT) && (loc != Mek.LOC_LT) && (loc != Mek.LOC_RT)) {
                    spotlightHittable = false;
                }

                if (spotlightHittable) {
                    Roll diceRoll = Compute.rollD6(2);
                    r = new Report(6072);
                    r.indent(2);
                    r.subject = te_n;
                    r.add("7+");
                    r.add("Searchlight");
                    r.add(diceRoll);
                    vDesc.addElement(r);

                    if (diceRoll.getIntValue() >= 7) {
                        r = new Report(6071);
                        r.subject = te_n;
                        r.indent(2);
                        r.add("Searchlight");
                        vDesc.addElement(r);
                        te.destroyOneSearchlight();
                    }
                }
            }

            if (!damageIS) {
                // Does an exterior passenger absorb some of the damage?
                damage = handleExternalPassengerDamage(te, hit, damage, ammoExplosion, damageType, vDesc);
                if (damage == 0) {
                    // Return
                    return;
                }
                // is this a mech dumping ammo being hit in the rear torso?
                if (List.of(Mek.LOC_CT, Mek.LOC_RT, Mek.LOC_LT).contains(hit.getLocation())) {
                    for (Mounted<?> mAmmo : te.getAmmo()) {
                        if (mAmmo.isDumping() && !mAmmo.isDestroyed()
                                  && !mAmmo.isHit()) {
                            // doh. explode it
                            vDesc.addAll(manager.explodeEquipment(te,
                                  mAmmo.getLocation(), mAmmo));
                            mAmmo.setHit(true);
                        }
                    }
                }
            }

            // Apply damage to armor
            damage = applyEntityArmorDamage(te, hit, damage, ammoExplosion, damageIS, areaSatArty, vDesc, mods);

            // Apply CASE II first
            damage = applyCASEIIDamageReduction(te, hit, damage, ammoExplosion, vDesc);

            // if damage has not all been absorbed, continue dealing damage internally
            if (damage > 0) {
                // is there internal structure in the location hit?
                if (te.getInternal(hit) > 0) {

                    // Now we need to consider alternate structure types!
                    int tmpDamageHold = -1;
                    if (te.hasCompositeStructure()) {
                        tmpDamageHold = damage;
                        damage *= 2;
                        r = new Report(6091);
                        r.subject = te_n;
                        r.indent(3);
                        vDesc.add(r);
                    }
                    if (te.hasReinforcedStructure()) {
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
                        // Triggers a critical hit on Vehicles and Meks.
                        crits++;
                        mods.tookInternalDamage = true;
                        // Alternate structures don't affect our damage total
                        // for later PSR purposes, so use the previously stored
                        // value here as necessary.
                        te.damageThisPhase += (tmpDamageHold > -1) ? tmpDamageHold : damage;
                        damage = 0;
                        r = new Report(6100);
                        r.subject = te_n;
                        r.indent(3);
                        // Infantry platoons have men not "Internals".
                        r.add(te.getInternal(hit));
                        vDesc.addElement(r);
                    } else if (damage > 0) {
                        // Triggers a critical hit on Vehicles and Meks.
                        crits++;
                        // damage transfers, maybe
                        int absorbed = Math.max(te.getInternal(hit), 0);

                        // Platoon, Trooper, or Section destroyed message
                        r = new Report(1210);
                        r.subject = te_n;
                        r.messageId = 6115;
                        r.indent(3);
                        vDesc.addElement(r);

                        // If a sidetorso got destroyed, and the
                        // corresponding arm is not yet destroyed, add
                        // it as a club to that hex (p.35 BMRr)
                        if ((((hit.getLocation() == Mek.LOC_RT)
                                    && (te.getInternal(Mek.LOC_RARM) > 0))
                                   || ((hit.getLocation() == Mek.LOC_LT)
                                             && (te.getInternal(Mek.LOC_LARM) > 0)))) {
                            int blownOffLocation;
                            if (hit.getLocation() == Mek.LOC_RT) {
                                blownOffLocation = Mek.LOC_RARM;
                            } else {
                                blownOffLocation = Mek.LOC_LARM;
                            }
                            te.destroyLocation(blownOffLocation, true);
                            r = new Report(6120);
                            r.subject = te_n;
                            r.add(te.getLocationName(blownOffLocation));
                            vDesc.addElement(r);
                            Hex h = game.getBoard().getHex(te.getPosition());
                            if (null != h) {
                                if (te instanceof BipedMek) {
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
                                manager.sendChangedHex(te.getPosition());
                            }
                        }

                        // Troopers riding on a location
                        // all die when the location is destroyed.
                        Entity passenger = te.getExteriorUnitAt(
                              hit.getLocation(), hit.isRear());
                        if ((null != passenger) && !passenger.isDoomed()) {
                            HitData passHit = passenger
                                                    .getTrooperAtLocation(hit, te);
                            // ensures a kill
                            passHit.setEffect(HitData.EFFECT_CRITICAL);
                            if (passenger.getInternal(passHit) > 0) {
                                vDesc.addAll(damageEntity(new DamageInfo(passenger, passHit, damage)));
                            }
                            passHit = new HitData(hit.getLocation(),
                                  !hit.isRear());
                            passHit = passenger.getTrooperAtLocation(
                                  passHit, te);
                            // ensures a kill
                            passHit.setEffect(HitData.EFFECT_CRITICAL);
                            if (passenger.getInternal(passHit) > 0) {
                                vDesc.addAll(damageEntity(new DamageInfo(passenger, passHit, damage)));
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
                            if (((Mek) te).hasCompositeStructure()) {
                                // If there's a remainder, we can actually
                                // ignore it.
                                damage /= 2;
                            } else if (((Mek) te).hasReinforcedStructure()) {
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
                        if (te instanceof Mek) {
                            // Start with the number of engine crits in this
                            // location, if any...
                            te.engineHitsThisPhase += te.getNumberOfCriticals(
                                  CriticalSlot.TYPE_SYSTEM,
                                  Mek.SYSTEM_ENGINE, hit.getLocation());
                            // ...then deduct the ones destroyed previously or
                            // critically
                            // hit this round already. That leaves the ones
                            // actually
                            // destroyed with the location.
                            te.engineHitsThisPhase -= te.getHitCriticals(
                                  CriticalSlot.TYPE_SYSTEM,
                                  Mek.SYSTEM_ENGINE, hit.getLocation());
                        }

                        boolean engineExploded = manager.checkEngineExplosion(te,
                              vDesc, te.engineHitsThisPhase);

                        if (!engineExploded) {
                            // Entity destroyed. Ammo explosions are
                            // neither survivable nor salvageable.
                            // Only ammo explosions in the CT are devastating.
                            vDesc.addAll(manager.destroyEntity(te, "damage", !ammoExplosion,
                                  !((ammoExplosion || areaSatArty) && (hit.getLocation() == Mek.LOC_CT))));
                            // If the head is destroyed, kill the crew.
                            if ((hit.getLocation() == Mek.LOC_HEAD)
                                      && !te.getCrew().isDead() && !te.getCrew().isDoomed()
                                      && game.getOptions().booleanOption(
                                  OptionsConstants.ADVANCED_TACOPS_SKIN_OF_THE_TEETH_EJECTION)) {
                                Mek mek = (Mek) te;
                                if (mek.isAutoEject()
                                          && (!game.getOptions().booleanOption(
                                      OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                                    || (game.getOptions().booleanOption(
                                      OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                                              && mek.isCondEjectHeadshot()))) {
                                    autoEject = true;
                                    vDesc.addAll(manager.ejectEntity(te, true, true));
                                }
                            }

                            if ((hit.getLocation() == Mek.LOC_CT)
                                      && !te.getCrew().isDead() && !te.getCrew().isDoomed()) {
                                Mek mek = (Mek) te;
                                if (mek.isAutoEject()
                                          && game.getOptions().booleanOption(
                                      OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                          && mek.isCondEjectCTDest()) {
                                    if (mek.getCrew().getHits() < 5) {
                                        Report.addNewline(vDesc);
                                        mek.setDoomed(false);
                                        mek.setDoomed(true);
                                    }
                                    autoEject = true;
                                    vDesc.addAll(manager.ejectEntity(te, true));
                                }
                            }

                            if ((hit.getLocation() == Mek.LOC_HEAD)
                                      || ((hit.getLocation() == Mek.LOC_CT)
                                                && ((ammoExplosion && !autoEject) || areaSatArty))) {
                                te.getCrew().setDoomed(true);
                            }
                            if (game.getOptions().booleanOption(
                                  OptionsConstants.ADVGRNDMOV_AUTO_ABANDON_UNIT)) {
                                vDesc.addAll(manager.abandonEntity(te));
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
                        for (WeaponMounted m : te.getWeaponList()) {
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
                if (!(mods.hardenedArmor || mods.ferroLamellorArmor || mods.reactiveArmor)) {
                    mods.specCrits = mods.specCrits + 1;
                }
            }
            mods.crits = crits;

            // check for breaching
            vDesc.addAll(manager.breachCheck(te, hit.getLocation(), null, underWater));

            // Deal special effect damage and crits
            dealSpecialCritEffects(te, vDesc, hit, mods, underWater, damageType);

            if (mods.isHeadHit && !te.hasAbility(OptionsConstants.MD_DERMAL_ARMOR)) {
                Report.addNewline(vDesc);
                vDesc.addAll(manager.damageCrew(te, 1));
            }

            // If the location has run out of internal structure, finally
            // actually
            // destroy it here.
            if ((te.getInternal(hit) <= 0)) {
                te.destroyLocation(hit.getLocation());

                // Check for possible engine destruction here
                if (((hit.getLocation() == Mek.LOC_RT) || (hit.getLocation() == Mek.LOC_LT))) {

                    int numEngineHits = te.getEngineHits();
                    boolean engineExploded = manager.checkEngineExplosion(te, vDesc, numEngineHits);

                    int hitsToDestroy = 3;
                    if (te.isSuperHeavy() && te.hasEngine()
                              && (te.getEngine().getEngineType() == Engine.COMPACT_ENGINE)) {
                        hitsToDestroy = 2;
                    }

                    if (!engineExploded && (numEngineHits >= hitsToDestroy)) {
                        // third engine hit
                        vDesc.addAll(manager.destroyEntity(te, "engine destruction"));
                        if (game.getOptions()
                                  .booleanOption(OptionsConstants.ADVGRNDMOV_AUTO_ABANDON_UNIT)) {
                            vDesc.addAll(manager.abandonEntity(te));
                        }
                        te.setSelfDestructing(false);
                        te.setSelfDestructInitiated(false);
                    }

                    // Torso destruction in airborne LAM causes immediate crash.
                    if ((te instanceof LandAirMek) && !te.isDestroyed() && !te.isDoomed()) {
                        r = new Report(9710);
                        r.subject = te.getId();
                        r.addDesc(te);
                        if (te.isAirborneVTOLorWIGE()) {
                            vDesc.add(r);
                            manager.crashAirMek(te, new PilotingRollData(te.getId(), TargetRoll.AUTOMATIC_FAIL,
                                  "side torso destroyed"), vDesc);
                        } else if (te.isAirborne() && te.isAero()) {
                            vDesc.add(r);
                            vDesc.addAll(manager.processCrash(te, ((IAero) te).getCurrentVelocity(), te.getPosition()));
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
                updateArmorTypeMap(mods, te, hit);
            }
            if (damageIS) {
                mods.wasDamageIS = true;
                damageIS = false;
            }
        }
    }

    public void damageAeroSpace(Vector<Report> vDesc, Aero te, HitData hit, int damage,
          boolean ammoExplosion, DamageType damageType,
          boolean areaSatArty, boolean throughFront, boolean underWater,
          boolean nukeS2S, ModsInfo mods) {
        int te_n = te.getId();
        Report r;
        boolean damageIS = mods.damageIS;

        if (ammoExplosion) {
            if (te.isAutoEject() && (!game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                           || (game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                                     && te.isCondEjectAmmo()))) {
                vDesc.addAll(manager.ejectEntity(te, true));
            }
        }

        // booleans to indicate criticals for AT2
        boolean critSI = false;
        boolean critThresh = false;

        // save the relevant damage for damage thresholding
        int damageThisAttack = te.damageThisPhase;

        // weapon groups only get the damage of one weapon
        if ((hit.getSingleAV() > -1)
                  && !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            damage = hit.getSingleAV();
        }

        // is this capital-scale damage
        boolean isCapital = hit.isCapital();

        // check capital/standard damage
        if (isCapital
                  && (!te.isCapitalScale() || game.getOptions().booleanOption(
              OptionsConstants.ADVAERORULES_AERO_SANITY))) {
            damage = 10 * damage;
        }
        if (!isCapital && te.isCapitalScale()
                  && !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            damage = (int) Math.round(damage / 10.0);
        }
        int damage_orig = damage;

        damage = manageDamageTypeReports(te, vDesc, damage, damageType, hit, false, mods);

        if (ammoExplosion && te.hasCase()) {
            // damage should be reduced by a factor of 2 for ammo explosions
            // according to p. 161, TW
            damage /= 2;
            r = new Report(9010);
            r.subject = te_n;
            r.add(damage);
            r.indent(3);
            vDesc.addElement(r);
        }

        damage = applyModularArmor(te, hit, damage, ammoExplosion, damageIS, vDesc);

        // Allocate the damage
        while (damage > 0) {
            // first check for ammo explosions on aeros separately, because it
            // must be done before
            // standard to capital damage conversions
            if ((hit.getLocation() == Aero.LOC_AFT)
                      && !damageIS) {
                for (Mounted<?> mAmmo : te.getAmmo()) {
                    if (mAmmo.isDumping() && !mAmmo.isDestroyed() && !mAmmo.isHit()
                              && !(mAmmo.getType() instanceof BombType)) {
                        // doh. explode it
                        vDesc.addAll(manager.explodeEquipment(te, mAmmo.getLocation(), mAmmo));
                        mAmmo.setHit(true);
                    }
                }
            }

            // Report this either way
            if (!ammoExplosion) {
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

            // Capital fighters receive damage differently
            if (te.isCapitalFighter()) {
                te.setCurrentDamage(te.getCurrentDamage() + damage);
                te.setCapArmor(te.getCapArmor() - damage);
                r = new Report(9065);
                r.subject = te_n;
                r.indent(2);
                r.newlines = 0;
                r.addDesc(te);
                r.add(damage);
                vDesc.addElement(r);
                r = new Report(6085);
                r.subject = te_n;
                r.add(Math.max(te.getCapArmor(), 0));
                vDesc.addElement(r);
                // check to see if this destroyed the entity
                if (te.getCapArmor() <= 0) {
                    // Lets auto-eject if we can!
                    // Aeros eject if the SI Destroyed switch is on
                    if (te.isAutoEject()
                              && (!game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                        || (game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                                  && te.isCondEjectSIDest()))) {
                        vDesc.addAll(manager.ejectEntity(te, true, false));
                    }
                    vDesc.addAll(manager.destroyEntity(te, "Structural Integrity Collapse"));
                    te.doDisbandDamage();
                    te.setCapArmor(0);
                    if (hit.getAttackerId() != Entity.NONE) {
                        manager.creditKill(te, game.getEntity(hit.getAttackerId()));
                    }
                }
                // check for aero crits from natural 12 or threshold; LAMs take damage as mechs
                manager.checkAeroCrits(vDesc, te, hit, damage_orig, critThresh,
                      critSI, ammoExplosion, nukeS2S);
            }

            damage = applyEntityArmorDamage(te, hit, damage, ammoExplosion, damageIS, areaSatArty, vDesc, mods);

            if (damage > 0) {
                // if this is an Aero then I need to apply internal damage
                // to the SI after halving it. Return from here to prevent
                // further processing
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
                if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_OVER_PENETRATE)) {
                    int opRoll = Compute.d6(1);
                    if (((te instanceof Jumpship) && !(te instanceof Warship) && (opRoll > 3))
                              || ((te instanceof Dropship) && (opRoll > 4))
                              || ((te instanceof Warship) && (a.getOSI() <= 30) && (opRoll > 5))) {
                        // over-penetration happened
                        r = new Report(9090);
                        r.subject = te_n;
                        r.newlines = 0;
                        vDesc.addElement(r);
                        int new_loc = a.getOppositeLocation(hit.getLocation());
                        damage = Math.min(damage, te.getArmor(new_loc));
                        // We don't want to deal negative damage
                        damage = Math.max(damage, 0);
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
                // Minimum SI damage is now 1 (per errata: https://bg.battletech.com/forums/index.php?topic=81913.0 )
                if (!ammoExplosion && !nukeS2S
                          && !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
                    damage = (int) Math.round(damage / 2.0);
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
                r.add(Math.max(a.getSI(), 0));
                vDesc.addElement(r);
                // check to see if this would destroy the ASF
                if (a.getSI() <= 0) {
                    // Lets auto-eject if we can!
                    if (a.isAutoEject()
                              && (!game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                        || (game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                                  && a.isCondEjectSIDest()))) {
                        vDesc.addAll(manager.ejectEntity(te, true, false));
                    } else {
                        vDesc.addAll(manager.destroyEntity(te, "Structural Integrity Collapse"));
                    }
                    a.setSI(0);
                    if (hit.getAttackerId() != Entity.NONE) {
                        manager.creditKill(a, game.getEntity(hit.getAttackerId()));
                    }
                }
            }
        }

        // Damage _applied_ by this attack should be original damageThisPhase minus current value
        damageThisAttack = te.damageThisPhase - damageThisAttack;

        // chance of a critical if total suffered damage greater than threshold
        if ((damageThisAttack > te.getThresh(hit.getLocation()))) {
            critThresh = true;
            te.setCritThresh(true);
        }

        manager.checkAeroCrits(vDesc, te, hit, damageThisAttack, critThresh, critSI, ammoExplosion, nukeS2S);
    }

    public void damageTank(Vector<Report> vDesc, Tank te, HitData hit, int damage,
          boolean ammoExplosion, DamageType damageType,
          boolean areaSatArty, boolean throughFront, boolean underWater,
          boolean nukeS2S, ModsInfo mods) {
        int te_n = te.getId();
        boolean damageIS = mods.damageIS;
        Report r;

        HitData nextHit = null;

        damage = manageDamageTypeReports(te, vDesc, damage, damageType, hit, false, mods);

        // adjust VTOL rotor damage
        if ((te instanceof VTOL) && (hit.getLocation() == VTOL.LOC_ROTOR)
                  && (hit.getGeneralDamageType() != HitData.DAMAGE_PHYSICAL)
                  && !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_FULL_ROTOR_HITS)) {
            damage = (damage + 9) / 10;
        }

        // Accumulate crits here.
        int crits = mods.crits;

        // Allocate the damage
        while (damage > 0) {

            // Report this either way
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

            damage = applyModularArmor(te, hit, damage, ammoExplosion, damageIS, vDesc);

            // Destroy searchlights on 7+ (torso hits on mechs)
            if (te.hasSearchlight()) {
                boolean spotlightHittable = true;
                int loc = hit.getLocation();
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

                if (spotlightHittable) {
                    Roll diceRoll = Compute.rollD6(2);
                    r = new Report(6072);
                    r.indent(2);
                    r.subject = te_n;
                    r.add("7+");
                    r.add("Searchlight");
                    r.add(diceRoll);
                    vDesc.addElement(r);

                    if (diceRoll.getIntValue() >= 7) {
                        r = new Report(6071);
                        r.subject = te_n;
                        r.indent(2);
                        r.add("Searchlight");
                        vDesc.addElement(r);
                        te.destroyOneSearchlight();
                    }
                }
            }

            if (!damageIS) {
                // Does an exterior passenger absorb some of the damage?
                damage = handleExternalPassengerDamage(te, hit, damage, ammoExplosion, damageType, vDesc);
                if (damage == 0) {
                    // Return our description.
                    return;
                }
                // is this a mech/tank dumping ammo being hit in the rear torso?
                if ((te instanceof Tank) &&
                          (hit.getLocation() == (te instanceof SuperHeavyTank ? SuperHeavyTank.LOC_REAR : Tank.LOC_REAR))) {
                    for (Mounted<?> mAmmo : te.getAmmo()) {
                        if (mAmmo.isDumping() && !mAmmo.isDestroyed()
                                  && !mAmmo.isHit()) {
                            // doh. explode it
                            vDesc.addAll(manager.explodeEquipment(te,
                                  mAmmo.getLocation(), mAmmo));
                            mAmmo.setHit(true);
                        }
                    }
                }
            }

            damage = applyEntityArmorDamage(te, hit, damage, ammoExplosion, damageIS, areaSatArty, vDesc, mods);

            // For optional tank damage thresholds, the overthresh flag won't
            // be set if IS is damaged, so set it here.
            if (((te.getArmor(hit) < 1) || damageIS)
                      && game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)
                      && !((te instanceof VTOL)
                                 || (te instanceof GunEmplacement))) {
                te.setOverThresh(true);
            }
            // Apply CASE II first
            damage = applyCASEIIDamageReduction(te, hit, damage, ammoExplosion, vDesc);

            // is there damage remaining?
            if (damage > 0) {

                // is there internal structure in the location hit?
                if (te.getInternal(hit) > 0) {

                    // Now we need to consider alternate structure types!
                    int tmpDamageHold = -1;
                    if ((te.getInternal(hit) > damage) && (damage > 0)) {
                        // internal structure absorbs all damage
                        te.setInternal(te.getInternal(hit) - damage, hit);
                        // Triggers a critical hit on Vehicles and Meks.
                        crits++;
                        mods.tookInternalDamage = true;
                        // Alternate structures don't affect our damage total
                        // for later PSR purposes, so use the previously stored
                        // value here as necessary.
                        te.damageThisPhase += (tmpDamageHold > -1) ? tmpDamageHold : damage;
                        damage = 0;
                        r = new Report(6100);
                        r.subject = te_n;
                        r.indent(3);
                        r.add(te.getInternal(hit));
                        vDesc.addElement(r);
                    } else if (damage > 0) {
                        // Triggers a critical hit on Vehicles and Meks.
                        crits++;
                        // damage transfers, maybe
                        int absorbed = Math.max(te.getInternal(hit), 0);

                        // Platoon, Trooper, or Section destroyed message
                        r = new Report(1210);
                        r.subject = te_n;
                        r.messageId = 6115;
                        r.indent(3);
                        vDesc.addElement(r);

                        // Troopers riding on a location
                        // all die when the location is destroyed.
                        Entity passenger = te.getExteriorUnitAt(
                              hit.getLocation(), hit.isRear());
                        if ((null != passenger) && !passenger.isDoomed()) {
                            HitData passHit = passenger
                                                    .getTrooperAtLocation(hit, te);
                            // ensures a kill
                            passHit.setEffect(HitData.EFFECT_CRITICAL);
                            if (passenger.getInternal(passHit) > 0) {
                                vDesc.addAll(damageEntity(new DamageInfo(passenger, passHit, damage)));
                            }
                            passHit = new HitData(hit.getLocation(),
                                  !hit.isRear());
                            passHit = passenger.getTrooperAtLocation(
                                  passHit, te);
                            // ensures a kill
                            passHit.setEffect(HitData.EFFECT_CRITICAL);
                            if (passenger.getInternal(passHit) > 0) {
                                vDesc.addAll(damageEntity(new DamageInfo(passenger, passHit, damage)));
                            }
                        }

                        // Mark off the internal structure here, but *don't*
                        // destroy the location just yet -- there are checks
                        // still to run!
                        te.setInternal(0, hit);
                        te.damageThisPhase += absorbed;
                        damage -= absorbed;
                    }
                }

                if (te.getInternal(hit) <= 0) {
                    // internal structure is gone, what are the transfer
                    // potentials?
                    nextHit = te.getTransferLocation(hit);
                    if (nextHit.getLocation() == Entity.LOC_DESTROYED) {

                        boolean engineExploded = manager.checkEngineExplosion(te,
                              vDesc, te.engineHitsThisPhase);

                        if (!engineExploded) {
                            // Entity destroyed. Ammo explosions are
                            // neither survivable nor salvageable.
                            // Only ammo explosions in the CT are devastating.
                            vDesc.addAll(manager.destroyEntity(te, "damage", !ammoExplosion,
                                  !((ammoExplosion || areaSatArty))));

                            if ((hit.getLocation() == Mek.LOC_HEAD)
                                      || ((hit.getLocation() == Mek.LOC_CT)
                                                && (ammoExplosion || areaSatArty))) {
                                te.getCrew().setDoomed(true);
                            }
                            if (game.getOptions().booleanOption(
                                  OptionsConstants.ADVGRNDMOV_AUTO_ABANDON_UNIT)) {
                                vDesc.addAll(manager.abandonEntity(te));
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
                        for (WeaponMounted m : te.getWeaponList()) {
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
                if (!(mods.hardenedArmor || mods.ferroLamellorArmor || mods.reactiveArmor)) {
                    mods.specCrits = mods.specCrits + 1;
                }
            }
            mods.crits = crits;

            // check for breaching
            vDesc.addAll(manager.breachCheck(te, hit.getLocation(), null, underWater));

            // Deal special effect damage and crits
            dealSpecialCritEffects(te, vDesc, hit, mods, underWater, damageType);

            // If the location has run out of internal structure, finally
            // actually
            // destroy it here. *EXCEPTION:* Aero units have 0 internal
            // structure
            // in every location by default and are handled elsewhere, so they
            // get a bye.
            if ((te.getInternal(hit) <= 0)) {
                te.destroyLocation(hit.getLocation());
            }

            // If damage remains, loop to next location; if not, be sure to stop
            // here because we may need to refer back to the last *damaged*
            // location again later. (This is safe because at damage <= 0 the
            // loop terminates anyway.)
            if (damage > 0) {
                hit = nextHit;
                updateArmorTypeMap(mods, te, hit);
            }
            if (damageIS) {
                mods.wasDamageIS = true;
                damageIS = false;
            }
        }
    }

    public void damageSquadronFighter(Vector<Report> vDesc, Entity te, HitData hit, int damage,
          boolean ammoExplosion, DamageType damageType, boolean damageIS,
          boolean areaSatArty, boolean throughFront, boolean underWater,
          boolean nukeS2S) {
        List<Entity> fighters = te.getActiveSubEntities();

        if (fighters.isEmpty()) {
            return;
        }
        Entity fighter = fighters.get(hit.getLocation());
        HitData new_hit = fighter.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
        new_hit.setBoxCars(hit.rolledBoxCars());
        new_hit.setGeneralDamageType(hit.getGeneralDamageType());
        new_hit.setCapital(hit.isCapital());
        new_hit.setCapMisCritMod(hit.getCapMisCritMod());
        new_hit.setSingleAV(hit.getSingleAV());
        new_hit.setAttackerId(hit.getAttackerId());
        vDesc.addAll(damageEntity(fighter, new_hit, damage, ammoExplosion, damageType,
              damageIS, areaSatArty, throughFront, underWater, nukeS2S, vDesc));
    }

    public void damageMultipleBAs(Vector<Report> vDesc, Entity te, HitData hit, int damage,
          boolean ammoExplosion, DamageType damageType, boolean damageIS,
          boolean areaSatArty, boolean throughFront, boolean underWater,
          boolean nukeS2S) {
        Report r;
        r = new Report(6044);
        r.subject = te.getId();
        r.indent(2);
        vDesc.add(r);
        for (int i = 0; i < ((BattleArmor) te).getTroopers(); i++) {
            hit.setLocation(BattleArmor.LOC_TROOPER_1 + i);
            if (te.getInternal(hit) > 0) {
                vDesc.addAll(damageEntity(te, hit, damage, ammoExplosion, damageType,
                      damageIS, false, throughFront, underWater, nukeS2S, vDesc));
            }
        }
    }

    public void damageBA(Vector<Report> vDesc, BattleArmor te, HitData hit, int damage,
          boolean ammoExplosion, DamageType damageType,
          boolean areaSatArty, boolean throughFront, boolean underWater,
          boolean nukeS2S, ModsInfo mods) {
        final boolean eiStatus = te.hasActiveEiCockpit();
        final boolean vacuum = game.getPlanetaryConditions().getAtmosphere().isLighterThan(Atmosphere.THIN);
        int te_n = te.getId();
        boolean damageIS = mods.damageIS;
        HitData nextHit = null;
        Report r;

        // check for critical hit/miss vs. a BA
        // TODO: discover why BA reports are causing OOM errors.
        if (mods.crits > 0) {
            // possible critical miss if the rerolled location isn't alive
            if ((hit.getLocation() >= te.locations()) || (te.getInternal(hit.getLocation()) <= 0)) {
                r = new Report(6037);
                r.add(hit.getLocation());
                r.subject = te_n;
                r.indent(2);
                vDesc.addElement(r);
                return;
            }
            // otherwise critical hit
            r = new Report(6225);
            r.add(te.getLocationAbbr(hit));
            r.subject = te_n;
            r.indent(2);
            vDesc.addElement(r);

            mods.crits = 0;

            damage = Math.max(te.getInternal(hit.getLocation()) + te.getArmor(hit.getLocation()), damage);
        }

        // Is the squad in vacuum?
        if(!te.isDestroyed() && !te.isDoomed() && vacuum) {
            // PBI. Double damage.
            damage *= 2;
            r = new Report(6041);
            r.subject = te_n;
            r.indent(2);
            vDesc.addElement(r);
        }

        damage = manageDamageTypeReports(te, vDesc, damage, damageType, hit, false, mods);

        // BA using EI implants receive +1 damage from attacks
        damage += (eiStatus) ? 1 : 0;

        // Allocate the damage
        while (damage > 0) {

            // was the section destroyed earlier this phase?
            if (te.getInternal(hit) == IArmorState.ARMOR_DOOMED) {
                // cannot transfer a through armor crit if so
                mods.crits = 0;
            }
            if (!ammoExplosion && (te.getArmor(hit) > 0) && !damageIS) {
                int origDamage = damage;

                damage = applyEntityArmorDamage(te, hit, damage, ammoExplosion, damageIS, areaSatArty, vDesc, mods);
            }

            if (damage > 0) {

                // Report this either way
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

                // is there internal structure in the location hit?
                if (te.getInternal(hit) > 0) {
                    // Now we need to consider alternate structure types!
                    int tmpDamageHold = -1;
                    if ((te.getInternal(hit) > damage) && (damage > 0)) {
                        // internal structure absorbs all damage
                        te.setInternal(te.getInternal(hit) - damage, hit);
                        mods.tookInternalDamage = true;
                        // Alternate structures don't affect our damage total
                        // for later PSR purposes, so use the previously stored
                        // value here as necessary.
                        te.damageThisPhase += (tmpDamageHold > -1) ? tmpDamageHold : damage;
                        damage = 0;
                        r = new Report(6100);
                        r.subject = te_n;
                        r.indent(3);
                        r.add(te.getInternal(hit));
                        vDesc.addElement(r);
                    } else if (damage > 0) {
                        // damage transfers, maybe
                        int absorbed = Math.max(te.getInternal(hit), 0);

                        // Platoon, Trooper, or Section destroyed message
                        r = new Report(1210);
                        r.subject = te_n;
                        r.messageId = 6110;
                        r.indent(3);
                        vDesc.addElement(r);

                        // BA inferno explosions
                        int infernos = 0;
                        for (Mounted<?> m : te.getEquipment()) {
                            if (m.getType() instanceof AmmoType at) {
                                if (((at.getAmmoType() == AmmoType.T_SRM) ||
                                           (at.getAmmoType() == AmmoType.T_MML)) &&
                                          (at.getMunitionType().contains(AmmoType.Munitions.M_INFERNO))) {
                                    infernos += at.getRackSize() * m.getHittableShotsLeft();
                                }
                            } else if (m.getType().hasFlag(MiscType.F_FIRE_RESISTANT)) {
                                // immune to inferno explosion
                                infernos = 0;
                                break;
                            }
                        }
                        if (infernos > 0) {
                            Roll diceRoll = Compute.rollD6(2);
                            r = new Report(6680);
                            r.add(diceRoll);
                            vDesc.add(r);

                            if (diceRoll.getIntValue() >= 8) {
                                Coords c = te.getPosition();
                                if (c == null) {
                                    Entity transport = game.getEntity(te.getTransportId());
                                    if (transport != null) {
                                        c = transport.getPosition();
                                    }
                                    manager.getMainPhaseReport().addAll(manager.deliverInfernoMissiles(te, te,
                                          infernos));
                                }
                                if (c != null) {
                                    manager.getMainPhaseReport().addAll(manager.deliverInfernoMissiles(te,
                                          new HexTarget(c, Targetable.TYPE_HEX_ARTILLERY),
                                          infernos));
                                }
                            }
                        }

                        // Mark off the internal structure here, but *don't*
                        // destroy the location just yet -- there are checks
                        // still to run!
                        te.setInternal(0, hit);
                        te.damageThisPhase += absorbed;
                        damage -= absorbed;
                    }
                }
                if (te.getInternal(hit) <= 0) {
                    // internal structure is gone, what are the transfer
                    // potentials?
                    nextHit = te.getTransferLocation(hit);
                    if (nextHit.getLocation() == Entity.LOC_DESTROYED) {
                        // nowhere for further damage to go
                        damage = 0;
                    } else if (nextHit.getLocation() == Entity.LOC_NONE) {
                        // Rest of the damage is wasted.
                        damage = 0;
                    } else if (ammoExplosion && te.locationHasCase(hit.getLocation())) {
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
                        for (WeaponMounted m : te.getWeaponList()) {
                            if (m.isSplit()) {
                                if ((m.getLocation() == hit.getLocation()) ||
                                          (m.getLocation() == nextHit.getLocation())) {
                                    te.setWeaponHit(m);
                                }
                            }
                        }
                        // if this is damage from a nail/rivet gun, and we
                        // transfer
                        // to a location that has armor, and BAR >=5, no damage
                        if ((damageType == DamageType.NAIL_RIVET) &&
                                  (te.getArmor(nextHit.getLocation()) > 0) &&
                                  (te.getBARRating(nextHit.getLocation()) >= 5)) {
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
                if (!(mods.hardenedArmor || mods.ferroLamellorArmor || mods.reactiveArmor)) {
                    mods.specCrits = mods.specCrits + 1;
                }
            }
            // check for breaching
            vDesc.addAll(manager.breachCheck(te, hit.getLocation(), null, underWater));

            // Special crits
            dealSpecialCritEffects(te, vDesc, hit, mods, underWater, damageType);

            // If the location has run out of internal structure, finally
            // actually
            // destroy it here. *EXCEPTION:* Aero units have 0 internal
            // structure
            // in every location by default and are handled elsewhere, so they
            // get a bye.
            if ((te.getInternal(hit) <= 0)) {
                te.destroyLocation(hit.getLocation());
            }

            // If damage remains, loop to next location; if not, be sure to stop
            // here because we may need to refer back to the last *damaged*
            // location again later. (This is safe because at damage <= 0 the
            // loop terminates anyway.)
            if (damage > 0) {
                hit = nextHit;
                // Need to update armor status for the new location
                updateArmorTypeMap(mods, te, hit);
            }
            if (damageIS) {
                mods.wasDamageIS = true;
                damageIS = false;
            }
        }
    }

    public void damageInfantry(Vector<Report> vDesc, Infantry te, HitData hit, int damage,
          boolean ammoExplosion, DamageType damageType,
          boolean areaSatArty, boolean throughFront, boolean underWater,
          boolean nukeS2S, ModsInfo mods){
        boolean isPlatoon = true;
        int te_n = te.getId();
        Hex te_hex = game.getBoard().getHex(te.getPosition());
        boolean damageIS = mods.damageIS;
        Report r;

        // Infantry with TSM implants get 2d6 burst damage from ATSM munitions
        if (damageType.equals(DamageType.ANTI_TSM) && te.isConventionalInfantry() && te.antiTSMVulnerable()) {
            Roll diceRoll = Compute.rollD6(2);
            r = new Report(6434);
            r.subject = te_n;
            r.add(diceRoll);
            r.indent(2);
            vDesc.addElement(r);
            damage += diceRoll.getIntValue();
        }

        // area effect against infantry is double damage
        if (areaSatArty) {
            // PBI. Double damage.
            damage *= 2;
            r = new Report(6039);
            r.subject = te_n;
            r.indent(2);
            vDesc.addElement(r);
        }

        // Is the infantry in the open?
        if (ServerHelper.infantryInOpen(te, te_hex, game, isPlatoon, ammoExplosion,
              hit.isIgnoreInfantryDoubleDamage())) {
            // PBI. Damage is doubled.
            damage *= 2;
            r = new Report(6040);
            r.subject = te_n;
            r.indent(2);
            vDesc.addElement(r);
        }

        // Is the infantry in vacuum?
        if(!te.isDestroyed() && !te.isDoomed()
                 && game.getPlanetaryConditions().getAtmosphere().isLighterThan(Atmosphere.THIN)) {
            // PBI. Double damage.
            damage *= 2;
            r = new Report(6041);
            r.subject = te_n;
            r.indent(2);
            vDesc.addElement(r);
        }

        damage = manageDamageTypeReports(te, vDesc, damage, damageType, hit, true, mods);

        // infantry armor can reduce damage
        if (te.calcDamageDivisor() != 1.0) {
            r = new Report(6074);
            r.subject = te_n;
            r.indent(2);
            r.add(damage);
            damage = (int) Math.ceil((damage) / te.calcDamageDivisor());
            r.add(damage);
            vDesc.addElement(r);
        }

        HitData nextHit = null;

        // Allocate the damage
        while (damage > 0) {
            int tmpDamageHold = -1;

            // Report this either way
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

            if (!ammoExplosion && (te.getArmor(hit) > 0) && !damageIS) {

                int origDamage = damage;

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

                int armorThreshold = te.getArmor(hit);
                if (armorThreshold >= damage) {
                    te.setArmor(te.getArmor(hit) - damage, hit);

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
                }

                if ((tmpDamageHold > 0)) {
                    damage = tmpDamageHold;
                }
            }

            // is there damage remaining?
            if (damage > 0) {

                // is there internal structure in the location hit?
                if (te.getInternal(hit) > 0) {

                    // Now we need to consider alternate structure types!
                    if ((te.getInternal(hit) > damage) && (damage > 0)) {
                        // internal structure absorbs all damage
                        te.setInternal(te.getInternal(hit) - damage, hit);
                        mods.tookInternalDamage = true;
                        // Alternate structures don't affect our damage total
                        // for later PSR purposes, so use the previously stored
                        // value here as necessary.
                        te.damageThisPhase += (tmpDamageHold > -1) ? tmpDamageHold : damage;
                        damage = 0;
                        r = new Report(6095);
                        r.subject = te_n;
                        r.indent(3);
                        r.add(te.getInternal(hit));
                        vDesc.addElement(r);
                    } else if (damage > 0) {
                        // damage transfers, maybe
                        int absorbed = Math.max(te.getInternal(hit), 0);

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
                        }
                        r.indent(3);
                        vDesc.addElement(r);

                        // Mark off the internal structure here, but *don't*
                        // destroy the location just yet -- there are checks
                        // still to run!
                        te.setInternal(0, hit);
                        te.damageThisPhase += absorbed;
                        damage -= absorbed;
                    }
                }

                if (te.getInternal(hit) <= 0) {
                    // internal structure is gone, what are the transfer
                    // potentials?
                    nextHit = te.getTransferLocation(hit);
                    if (nextHit.getLocation() == Entity.LOC_DESTROYED) {
                        // No engine explosions for infantry
                        // Entity destroyed. Ammo explosions are
                        // neither survivable nor salvageable.
                        // Only ammo explosions in the CT are devastating.
                        vDesc.addAll(manager.destroyEntity(te, "damage", !ammoExplosion,
                              !((ammoExplosion || areaSatArty))));
                        // If the head is destroyed, kill the crew.

                        if ((hit.getLocation() == Mek.LOC_HEAD)
                                  || ((hit.getLocation() == Mek.LOC_CT)
                                            && (ammoExplosion || areaSatArty))) {
                            te.getCrew().setDoomed(true);
                        }
                        if (game.getOptions().booleanOption(
                              OptionsConstants.ADVGRNDMOV_AUTO_ABANDON_UNIT)) {
                            vDesc.addAll(manager.abandonEntity(te));
                        }

                        // nowhere for further damage to go
                        damage = 0;
                    } else if (nextHit.getLocation() == Entity.LOC_NONE) {
                        // Rest of the damage is wasted.
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
                        for (WeaponMounted m : te.getWeaponList()) {
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
                if (!(mods.hardenedArmor || mods.ferroLamellorArmor || mods.reactiveArmor)) {
                    mods.specCrits = mods.specCrits + 1;
                }
            }
        }
    }

    public int calcCritBonus(Entity ae, Entity te, int damageOriginal, boolean areaSatArty) {
        // the bonus to the crit roll if using the
        // "advanced determining critical hits rule"
        int critBonus = 0;
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CRIT_ROLL)
                  && (damageOriginal > 0)
                  && ((te instanceof Mek) || (te instanceof ProtoMek))) {
            critBonus = Math.min((damageOriginal - 1) / 5, 4);
        }

        // Find out if Human TRO plays a part it crit bonus
        if ((ae != null) && !areaSatArty) {
            if ((te instanceof Mek) && ae.hasAbility(OptionsConstants.MISC_HUMAN_TRO, Crew.HUMANTRO_MEK)) {
                critBonus += 1;
            } else if ((te instanceof Aero) && ae.hasAbility(OptionsConstants.MISC_HUMAN_TRO, Crew.HUMANTRO_AERO)) {
                critBonus += 1;
            } else if ((te instanceof Tank) && ae.hasAbility(OptionsConstants.MISC_HUMAN_TRO, Crew.HUMANTRO_VEE)) {
                critBonus += 1;
            } else if ((te instanceof BattleArmor) && ae.hasAbility(OptionsConstants.MISC_HUMAN_TRO, Crew.HUMANTRO_BA)) {
                critBonus += 1;
            }
        }
        return critBonus;
    }

    public boolean checkFerroFibrous(Entity te, HitData hit) {
        return ((te.getArmor(hit) > 0)
                      && ((te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_FERRO_FIBROUS)
                                || (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_LIGHT_FERRO)
                                || (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HEAVY_FERRO)));
    }

    public int applyModularArmor(Entity te, HitData hit, int damage, boolean ammoExplosion, boolean damageIS, Vector<Report> vDesc) {

        // modular armor might absorb some damage, if the location mounts any.
        if (!ammoExplosion && !damageIS
                  && ((hit.getEffect() & HitData.EFFECT_NO_CRITICALS) != HitData.EFFECT_NO_CRITICALS)) {
            int damageNew = te.getDamageReductionFromModularArmor(hit, damage, vDesc);
            int damageDiff = damage - damageNew;
            te.damageThisPhase += damageDiff;
            damage = damageNew;
        }
        return damage;
    }

    public int applyTankCASEDamageReduction(Entity te, HitData hit, int damage, boolean ammoExplosion, Vector<Report> vDesc) {
        // check for tank CASE here: damage to rear armor, excess
        // dissipating, and a crew stunned crit
        int te_n = te.getId();
        Report r;

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
            vDesc.addAll(manager.applyCriticalHit(te, Entity.NONE, new CriticalSlot(0, critIndex), true, 0, false));
        }
        return damage;
    }

    public int applyCASEIIDamageReduction(Entity te, HitData hit, int damage, boolean ammoExplosion, Vector<Report> vDesc) {
        // Check for CASE II right away. if so reduce damage to 1
        // and let it hit the IS.
        // Also remove as much of the rear armor as allowed by the
        // damage. If arm/leg/head
        // Then they lose all their armor if its less then the
        // explosion damage.
        int te_n = te.getId();
        Report r;

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
            if ((te instanceof Mek) && ((loc == Mek.LOC_HEAD) || ((Mek) te).isArm(loc)
                                              || te.locationIsLeg(loc))) {
                int half = (int) Math.ceil(te.getOArmor(loc, false) / 2.0);
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

            Roll diceRoll = Compute.rollD6(2);
            r = new Report(6127);
            r.subject = te.getId();
            r.add(diceRoll);
            vDesc.add(r);

            if (diceRoll.getIntValue() >= 8) {
                hit.setEffect(HitData.EFFECT_NO_CRITICALS);
            }
        }

        return damage;
    }

    public void dealSpecialCritEffects(Entity te, Vector<Report> vDesc, HitData hit, ModsInfo mods,
          boolean underWater, DamageType damageType
    ) {

        int crits = mods.crits;

        // resolve special results
        if ((hit.getEffect() & HitData.EFFECT_VEHICLE_MOVE_DAMAGED) == HitData.EFFECT_VEHICLE_MOVE_DAMAGED) {
            vDesc.addAll(manager.vehicleMotiveDamage((Tank) te, hit.getMotiveMod()));
        }
        // Damage from any source can break spikes
        if (te.hasWorkingMisc(MiscType.F_SPIKES, -1, hit.getLocation())) {
            vDesc.add(manager.checkBreakSpikes(te, hit.getLocation()));
        }

        // roll all critical hits against this location
        // unless the section destroyed in a previous phase?
        // Cause a crit.
        if ((te.getInternal(hit) != IArmorState.ARMOR_DESTROYED)
                  && ((hit.getEffect() & HitData.EFFECT_NO_CRITICALS) != HitData.EFFECT_NO_CRITICALS)) {
            for (int i = 0; i < crits; i++) {
                vDesc.addAll(manager.criticalEntity(te, hit.getLocation(), hit.isRear(),
                      hit.glancingMod() + mods.critBonus,
                      mods.damageOriginal, damageType));
            }
            crits = 0;

            for (int i = 0; i < mods.specCrits; i++) {
                // against BAR or reflective armor, we get a +2 mod
                int critMod = te.hasBARArmor(hit.getLocation()) ? 2 : 0;
                critMod += ((mods.reflectiveArmor) && !(mods.isBattleArmor)) ? 2 : 0; // BA
                // against impact armor, we get a +1 mod
                critMod += (mods.impactArmor) ? 1 : 0;
                // hardened armour has no crit penalty
                if (!mods.hardenedArmor) {
                    // non-hardened armor gets modifiers
                    // the -2 for hardened is handled in the critBonus
                    // variable
                    critMod += hit.getSpecCritMod();
                    critMod += hit.glancingMod();
                }
                vDesc.addAll(manager.criticalEntity(te, hit.getLocation(), hit.isRear(),
                      critMod + mods.critBonus, mods.damageOriginal));
            }
            mods.specCrits = 0;
        }
        mods.crits = crits;
    }

    protected ModsInfo createDamageModifiers(Entity te, HitData hit, boolean damageIS, int damageOriginal, int crits) {
        // Map that stores various values for passing and mutating.
        ModsInfo mods = new ModsInfo();

        mods.crits = crits;
        mods.damageOriginal = damageOriginal;
        mods.damageIS = damageIS;
        mods.tookInternalDamage = damageIS;
        updateArmorTypeMap(mods, te, hit);
        return mods;
    }

    protected void updateArmorTypeMap(ModsInfo mods, Entity te, HitData hit) {
        boolean isBattleArmor = (te instanceof BattleArmor);

        mods.isBattleArmor = isBattleArmor;

        mods.ferroFibrousArmor = (checkFerroFibrous(te, hit));
        mods.bar5 =(te.getBARRating(hit.getLocation()) <= 5);
        mods.ballisticArmor =((te instanceof Mek) || (te instanceof Tank) || (te instanceof Aero))
                                        && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_BALLISTIC_REINFORCED);
        mods.ferroLamellorArmor =((te instanceof Mek) || (te instanceof Tank) || (te instanceof Aero))
                                            && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_FERRO_LAMELLOR);
        mods.hardenedArmor =((te instanceof Mek) || (te instanceof Tank))
                                       && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HARDENED);
        mods.impactArmor =(te instanceof Mek)
                                     && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_IMPACT_RESISTANT);
        mods.reactiveArmor =(((te instanceof Mek) || (te instanceof Tank) || (te instanceof Aero))
                                        && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_REACTIVE))
                                       || (isBattleArmor && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_BA_REACTIVE));
        mods.reflectiveArmor =(((te instanceof Mek) || (te instanceof Tank) || (te instanceof Aero))
                                          && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_REFLECTIVE))
                                         || isBattleArmor && (te.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_BA_REFLECTIVE);
    }

    public int manageDamageTypeReports(Entity te, Vector<Report> vDesc, int damage, DamageType damageType,  HitData hit,
          boolean isPlatoon, ModsInfo mods) {
        Report r;
        int te_n = te.getId();

        switch (damageType) {
            case FRAGMENTATION:
                // Fragmentation missiles deal full damage to conventional
                // infantry
                // (only) and no damage to other target types.
                if (!isPlatoon) {
                    damage = 0;
                    r = new Report(6050); // For some reason this report never
                    // actually shows up...
                    r.subject = te_n;
                    r.indent(2);
                    vDesc.addElement(r);
                } else {
                    r = new Report(6045); // ...but this one displays just fine.
                    r.subject = te_n;
                    r.indent(2);
                    vDesc.addElement(r);
                }
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
                    r.subject = te_n;
                    r.indent(2);
                    vDesc.addElement(r);
                } else {
                    r = new Report(6055);
                    r.subject = te_n;
                    r.indent(2);
                    vDesc.addElement(r);
                }
                break;
            case ACID:
                if (
                      mods.ferroFibrousArmor
                            || mods.reactiveArmor
                            || mods.reflectiveArmor
                            || mods.ferroLamellorArmor
                            || mods.bar5
                ) {
                    if (te.getArmor(hit) <= 0) {
                        break; // hitting IS, not acid-affected armor
                    }
                    damage = Math.min(te.getArmor(hit), 3);
                    r = new Report(6061);
                    r.subject = te_n;
                    r.indent(2);
                    r.add(damage);
                    vDesc.addElement(r);
                } else if (isPlatoon) {
                    damage = (int) Math.ceil(damage * 1.5);
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
        return damage;
    }

    public int handleExternalPassengerDamage(Entity te, HitData hit, int damage, boolean ammoExplosion,
          DamageType damageType, Vector<Report> vDesc) {
        int te_n = te.getId();
        Report r;
        int extantDamage = damage;
        int nLoc = hit.getLocation();
        Entity passenger = te.getExteriorUnitAt(nLoc, hit.isRear());
        // Does an exterior passenger absorb some of the damage?
        if (!ammoExplosion && (null != passenger) && !passenger.isDoomed()
                  && (damageType != DamageType.IGNORE_PASSENGER)) {
            extantDamage = manager.damageExternalPassenger(te, hit, damage, vDesc, passenger);
        }

        boolean bTorso = (nLoc == Mek.LOC_CT) || (nLoc == Mek.LOC_RT) || (nLoc == Mek.LOC_LT);

        // Does a swarming unit absorb damage?
        int swarmer = te.getSwarmAttackerId();
        if ((!(te instanceof Mek) || bTorso) && (swarmer != Entity.NONE)
                  && ((hit.getEffect() & HitData.EFFECT_CRITICAL) == 0) && (Compute.d6() >= 5)
                  && (damageType != DamageType.IGNORE_PASSENGER) && !ammoExplosion) {
            Entity swarm = game.getEntity(swarmer);
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
            } while ((extantDamage > absorb)
                           && (nextPassHit.getLocation() >= 0));

            // Damage the swarm.
            int absorbedDamage = Math.min(extantDamage, absorb);
            Vector<Report> newReports = damageEntity(new DamageInfo(swarm, passHit, absorbedDamage));
            for (Report newReport : newReports) {
                newReport.indent(2);
            }
            vDesc.addAll(newReports);

            // Did some damage pass on?
            if (extantDamage > absorb) {
                // Yup. Remove the absorbed damage.
                extantDamage -= absorb;
                r = new Report(6080);
                r.subject = te_n;
                r.indent(2);
                r.add(extantDamage);
                r.addDesc(te);
                vDesc.addElement(r);
            }
        }
        return Math.max(extantDamage, 0);
    }

    /**
     * Apply damage to the armor in the hit location; remaining damage will be applied elsewhere.
     *
     * @param te            Entity being damaged
     * @param hit           HitData recording aspects of the incoming damage
     * @param damage        Actual amount of incoming damage
     * @param ammoExplosion Whether damage was caused by an ammo explosion
     * @param damageIS      Whether damage is going straight to internal structure
     * @param areaSatArty   Whether damage is caused by AE attack
     * @param vDesc         Vector of Reports containing prior reports; usually modded and returned
     * @return int          Remaining damage not absorbed by armor
     */
    public int applyEntityArmorDamage(Entity te, HitData hit, int damage, boolean ammoExplosion, boolean damageIS,
          boolean areaSatArty, Vector<Report> vDesc,
          ModsInfo mods) {
        boolean ferroLamellorArmor = mods.ferroLamellorArmor;
        boolean ballisticArmor = mods.ballisticArmor;
        boolean hardenedArmor = mods.hardenedArmor;
        boolean impactArmor = mods.impactArmor;
        boolean reflectiveArmor = mods.reflectiveArmor;
        boolean reactiveArmor = mods.reactiveArmor;
        boolean isBattleArmor = (te instanceof BattleArmor);
        int damageOriginal = mods.damageOriginal;
        int critBonus = mods.critBonus;

        int te_n = te.getId();
        Report r;

        // is there armor in the location hit?
        if (!ammoExplosion && (te.getArmor(hit) > 0) && !damageIS) {
            int tmpDamageHold = -1;
            int origDamage = damage;

            if (ferroLamellorArmor
                      && (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING)
                      && (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING_MISSILE)
                      && (hit.getGeneralDamageType() != HitData.DAMAGE_IGNORES_DMG_REDUCTION)
                      && (hit.getGeneralDamageType() != HitData.DAMAGE_AX)) {
                tmpDamageHold = damage;
                damage = (int) Math.floor((((double) damage) * 4) / 5);
                if (damage <= 0) {
                    mods.isHeadHit = false;
                    mods.crits = 0;
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
                                       || (hit.getGeneralDamageType() == HitData.DAMAGE_AX) //AX doesn't affect ballistic-reinforced armor, TO:AUE (6th), pg. 179
                                       || (hit.getGeneralDamageType() == HitData.DAMAGE_MISSILE))) {
                tmpDamageHold = damage;
                damage = Math.max(1, damage / 2);
                r = new Report(6088);
                r.subject = te_n;
                r.indent(3);
                r.add(damage);
                vDesc.addElement(r);
            } else if (impactArmor
                             && (hit.getGeneralDamageType() == HitData.DAMAGE_PHYSICAL)) {
                // As long as there is even 1 point of armor in this location, reduce _all_ damage
                // to 2 points for every whole 3 points applied (IntOps pg 88).
                damage = Math.max(1, (2 * (damage / 3)) + (damage % 3));
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
                int dmgToDouble = Math.min(damage, currArmor / 2);
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
                int dmgToDouble = Math.min(damage, currArmor / 2);
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
                damage = (int) Math.floor(((double) damage) / 2);
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
                damage = (int) Math.floor(((double) damage) / 2);
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
                      && game.getOptions()
                               .booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)
                      && !((te instanceof VTOL) || (te instanceof GunEmplacement))) {
                int thresh = (int) Math.ceil(
                      (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD_VARIABLE)
                             ? te.getArmor(hit)
                             : te.getOArmor(hit))
                            / (double) game.getOptions().intOption(
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
                    mods.crits = (((hit.getEffect() & HitData.EFFECT_CRITICAL)
                                 == HitData.EFFECT_CRITICAL) ? 1 : 0);
                } else {
                    ((Tank) te).setOverThresh(false);
                    mods.crits = 0;
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
                for (Mounted<?> mount : te.getMisc()) {
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
                    // Halve damage for hardened armor here so PSRs work correctly
                    damage = damage / 2;
                } else {
                    te.setArmor(te.getArmor(hit) - damage, hit);
                }

                // set "armor damage" flag for HarJel II/III
                // we only care about this if there is armor remaining,
                // so don't worry about the case where damage exceeds
                // armorThreshold
                if ((te instanceof Mek) && (damage > 0)) {
                    ((Mek) te).setArmorDamagedThisTurn(hit.getLocation(), true);
                }

                // if the armor is hardened, any penetrating crits are
                // rolled at -2
                if (hardenedArmor) {
                    mods.critBonus = critBonus - 2;
                }

                // We should only record the applied damage, although original and tmpDamageHold
                // damage may be needed for other calculations
                te.damageThisPhase += damage;

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
                    vDesc.addAll(manager.destroyEntity(te, "damage", false));
                }

            } else {

                // damage goes on to internal
                int absorbed = Math.max(te.getArmor(hit), 0);
                if (hardenedArmor
                          && (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING)
                          && (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING_MISSILE)) {
                    absorbed = (absorbed * 2)
                                     - ((te.isHardenedArmorDamaged(hit)) ? 1 : 0);
                }
                if (reflectiveArmor && (hit.getGeneralDamageType() == HitData.DAMAGE_PHYSICAL)
                          && !isBattleArmor) {
                    absorbed = (int) Math.ceil(absorbed / 2.0);
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
                        vDesc.addAll(manager.destroyEntity(te, "damage", false));
                    }
                }
            }

            // targets with BAR armor get crits, depending on damage and BAR
            // rating
            if (te.hasBARArmor(hit.getLocation())) {
                if (origDamage > te.getBARRating(hit.getLocation())) {
                    if (te.hasArmoredChassis()) {
                        // crit roll with -1 mod
                        vDesc.addAll(manager.criticalEntity(te, hit.getLocation(),
                              hit.isRear(), -1 + critBonus, damageOriginal));
                    } else {
                        vDesc.addAll(manager.criticalEntity(te, hit.getLocation(),
                              hit.isRear(), critBonus, damageOriginal));
                    }
                }
            }
        }
        return damage;
    }

    public class ModsInfo {
        public boolean ballisticArmor = false;
        public boolean ferroFibrousArmor = false;
        public boolean ferroLamellorArmor = false;
        public boolean hardenedArmor = false;
        public boolean impactArmor = false;
        public boolean reactiveArmor = false;
        public boolean reflectiveArmor = false;
        public boolean isBattleArmor = false;
        public boolean isHeadHit = false;
        public boolean damageIS = false;
        public boolean wasDamageIS = false;
        public boolean bar5 = false;
        public boolean tookInternalDamage = false;
        public int critBonus = 0;
        public int crits = 0;
        public int specCrits = 0;
        public int damageOriginal = 0;

    }
}
