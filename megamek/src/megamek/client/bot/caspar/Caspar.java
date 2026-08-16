/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.bot.caspar;

import java.util.List;

import megamek.client.bot.AIType;
import megamek.client.bot.princess.AerospaceFireControl;
import megamek.client.bot.princess.AerospacePathRanker;
import megamek.client.bot.princess.FireControl.FireControlType;
import megamek.client.bot.princess.FormationGeometry;
import megamek.client.bot.princess.MutualSupportDeployment;
import megamek.client.bot.princess.MutualSupportPathRanker;
import megamek.client.bot.princess.PathRanker.PathRankerType;
import megamek.client.bot.princess.Princess;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.force.Force;
import megamek.common.game.Game;
import megamek.client.bot.princess.AerospaceGroundOrder;
import megamek.common.moves.MovePath;
import megamek.common.units.Dropship;
import megamek.common.units.IAero;
import megamek.common.pathfinder.AeroGroundDoctrinePathFinder;
import megamek.common.pathfinder.AeroGroundPathFinder;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * The CASPAR bot: the experimental successor to {@link Princess}. It shares Princess's entire
 * network/phase/state plumbing; experimental behavior is added by overriding Princess's wiring seams (for
 * example {@code initializePathRankers()} and {@code initializeFireControls()}) so that each divergence can
 * be compared against Princess head-to-head.
 *
 * <p>Current divergences from Princess:</p>
 * <ul>
 *     <li><b>Mutual Support movement doctrine</b> ({@link MutualSupportPathRanker}): supporting-range
 *     cohesion instead of center-of-mass herding, a cover bonus for advancing inside a set friend's
 *     engagement envelope, and a uniform closing tempo so slow and fast elements commit together.</li>
 *     <li><b>Mutual Support deployment</b> ({@link MutualSupportDeployment}): a force comes onto the board
 *     as a formation instead of scattering across its whole deployment zone.</li>
 *     <li><b>Atmospheric aerospace doctrine</b> ({@link AerospacePathRanker},
 *     {@link AerospaceFireControl}, {@link AeroGroundDoctrinePathFinder}): altitude becomes something the
 *     bot chooses rather than a constant, air-to-air dead zones are respected in both movement and gunnery,
 *     and enemy fighters that have already moved are told apart from those that have not.</li>
 *     <li><b>Aerospace move order</b>: fighters whose engagement geometry is already settled move before
 *     those still waiting on an opponent, and an element's lead moves ahead of its wingmen.</li>
 * </ul>
 */
public class Caspar extends Princess {
    private static final MMLogger LOGGER = MMLogger.create(Caspar.class);

    /** Move-index multiplier for a fighter whose opposing air has all committed: go now, while it is true. */
    private static final double DECIDED_PRIORITY = 1.5;

    /** Move-index multiplier for a fighter whose opposing air has not moved: wait and learn something. */
    private static final double UNDECIDED_DEFERRAL = 0.6;

    /** Move-index multiplier for the aircraft an element forms up on, so wingmen stay reactive behind it. */
    private static final double LEAD_PRIORITY = 1.2;

    /**
     * Creates a new CASPAR bot with the given display name, configured for the given host and port.
     *
     * @param name The display name
     * @param host The host address to which to connect
     * @param port The port on the host where to connect
     */
    public Caspar(final String name, final String host, final int port) {
        super(name, host, port);
    }

    @Override
    public AIType getAIType() {
        return AIType.CASPAR;
    }

    @Override
    public void initializePathRankers() {
        super.initializePathRankers();
        // CASPAR divergence: the Mutual Support doctrine replaces the stock ground-unit path ranker.
        registerPathRanker(PathRankerType.Basic, new MutualSupportPathRanker(this));
        // CASPAR divergence: atmospheric aerospace gets a ranker that knows about altitude. Without this it
        // would inherit the Basic slot above, and be flown by a ground formation doctrine.
        registerPathRanker(PathRankerType.Aerospace, new AerospacePathRanker(this));
        // Info-level receipt so any headless run's logs prove which ranker this bot is actually using.
        LOGGER.info("[MutualSupport] {}: CASPAR registered MutualSupportPathRanker for ground units",
              getLocalPlayer() != null ? getLocalPlayer().getName() : getName());
        LOGGER.info("[Aerospace] {}: CASPAR registered AerospacePathRanker for atmospheric aerospace",
              getLocalPlayer() != null ? getLocalPlayer().getName() : getName());
    }

    @Override
    public void initializeFireControls() {
        super.initializeFireControls();
        // CASPAR divergence: aerospace gunnery that will not plan a shot the dead zone forbids.
        registerFireControl(FireControlType.Aerospace, new AerospaceFireControl(this));
    }

    /**
     * CASPAR divergence: generate ground-mapsheet aerospace paths at more than one altitude, so the ranker
     * has an altitude to choose rather than inheriting a constant.
     */
    @Override
    protected AeroGroundPathFinder aeroGroundPathFinder(Game game) {
        return AeroGroundDoctrinePathFinder.getInstance(game);
    }

    /** How a grounded aerospace unit gets back in the air this turn, if at all. */
    enum TakeoffMode {
        /** Horizontal takeoff down a clear runway: no roll to fail. */
        RUNWAY,
        /** Vertical liftoff: works anywhere, costs a control roll. */
        VERTICAL,
        /** Neither is safe or legal: keep fighting as a ground turret. */
        STAY
    }

    /**
     * The vertical-liftoff roll must clear the same floor as a combat stunt: a healthy fighter
     * needs about a 7 (piloting +2 for a fighter's vertical liftoff), and below even odds the
     * failed-liftoff table is how the crashed-Hellcat game started.
     */
    static final double TAKEOFF_ROLL_FLOOR = 0.5;

    /**
     * CASPAR divergence: a grounded aerospace unit tries to get back in the air. The engine has
     * always supported both takeoff types; no bot has ever asked for one - a crashed fighter sat
     * as terrain for 33 rounds in the game that motivated this. Runway first when the strip is
     * clear (nothing to roll), vertical liftoff when boxed in and the roll clears the stunt floor,
     * ground turret otherwise. A crippled-but-flyable fighter taking off IS its forced withdrawal:
     * once airborne, the Winchester and fallback doctrines fly it off the board.
     */
    @Override
    protected MovePath continueMovementFor(final Entity entity) {
        MovePath takeoffPath = buildTakeoffPath(entity);
        if (takeoffPath != null) {
            return takeoffPath;
        }
        MovePath landingPath = buildLandingPath(entity);
        if (landingPath != null) {
            return landingPath;
        }
        return super.continueMovementFor(entity);
    }

    private @Nullable MovePath buildTakeoffPath(final Entity entity) {
        if (!(entity instanceof IAero aero) || !entity.isAero() || entity.isAirborne()
              || entity.isShutDown() || (entity.getPosition() == null)) {
            return null;
        }
        if (!groundOrderPermitsTakeoff(entity.isFighter(), getAerospaceGroundOrder())) {
            // A DropShip or small craft on the ground may be doing its job - cargo, fortress fire
            // support - so it moves between domains only on a player order. Fighters need no
            // orders; a grounded fighter's job is always to get back up. Debug so the decision
            // tree is never silent: holding is a choice, not an omission (live Union game).
            LOGGER.debug("TAKEOFF {}: holding domain per {} order", entity.getDisplayName(),
                  getAerospaceGroundOrder());
            return null;
        }
        if ((entity instanceof Dropship) && adjacentFriendlyGroundUnit(entity)) {
            // A DropShip liftoff blasts every adjacent unit (applyDropShipProximityDamage). Wait
            // for the friendlies to clear rather than fry the infantry screen.
            LOGGER.info("TAKEOFF {}: DEFERRED (friendly units adjacent to the liftoff blast)",
                  entity.getDisplayName());
            return null;
        }
        boolean runwayClear = aero.canTakeOffHorizontally()
              && (aero.hasRoomForHorizontalTakeOff() == null);
        boolean verticalLegal = aero.canTakeOffVertically();
        double verticalOdds = verticalLegal
              ? Compute.oddsAbove(aero.checkVerticalTakeOff().getValue()) / 100.0 : 0.0;
        TakeoffMode mode = chooseTakeoffMode(runwayClear, verticalLegal, verticalOdds);
        // The decision tree, visible per turn (house rule): what was chosen and what it was
        // chosen over.
        LOGGER.info("TAKEOFF {}: {} (runway clear={}, vertical odds={}%)",
              entity.getDisplayName(), mode, runwayClear, Math.round(verticalOdds * 100));
        if (mode == TakeoffMode.STAY) {
            return null;
        }
        MovePath takeoffPath = new MovePath(getGame(), entity);
        takeoffPath.addStep(mode == TakeoffMode.RUNWAY
              ? MoveStepType.TAKEOFF : MoveStepType.VERTICAL_TAKE_OFF);
        return takeoffPath;
    }

    /**
     * The ground-or-sky order gate, pure: fighters always may (their takeoff doctrine self-gates
     * on odds), everything else only on a standing LIFT_OFF order.
     */
    static boolean groundOrderPermitsTakeoff(boolean isFighter, AerospaceGroundOrder order) {
        return isFighter || (order == AerospaceGroundOrder.LIFT_OFF);
    }

    /** The landing half of the order: never fighters, only on a standing LAND order. */
    static boolean groundOrderRequestsLanding(boolean isFighter, AerospaceGroundOrder order) {
        return !isFighter && (order == AerospaceGroundOrder.LAND);
    }

    private boolean adjacentFriendlyGroundUnit(final Entity entity) {
        for (Coords neighbor : entity.getPosition().allAdjacent()) {
            for (Entity unit : getGame().getEntitiesVector(neighbor, entity.getBoardId())) {
                if (!unit.getOwner().isEnemyOf(entity.getOwner()) && !unit.isAirborne()
                      && !unit.equals(entity)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Rough-in of the LAND order for the bot commands panel: an airborne DropShip or small craft
     * under a standing LAND order sets down vertically as soon as it is legal - altitude 1, the
     * capability, and clear ground below. No approach doctrine yet (the ship does not deliberately
     * descend to reach the window); that ships with the landing doctrine block.
     */
    private @Nullable MovePath buildLandingPath(final Entity entity) {
        if (!(entity instanceof IAero aero) || !entity.isAero() || !entity.isAirborne()
              || (entity.getPosition() == null)
              || !groundOrderRequestsLanding(entity.isFighter(), getAerospaceGroundOrder())) {
            return null;
        }
        if ((entity.getAltitude() != 1) || !aero.canLandVertically()
              || (aero.hasRoomForVerticalLanding() != null)) {
            return null;
        }
        LOGGER.info("LAND {}: vertical landing ordered and legal", entity.getDisplayName());
        MovePath landingPath = new MovePath(getGame(), entity);
        landingPath.addStep(MoveStepType.VERTICAL_LAND);
        return landingPath;
    }

    /**
     * The runway-versus-vertical decision, pure. A clear runway wins outright: it has no roll to
     * fail, and the exposure of ending twenty hexes downrange is survivable in a way the
     * failed-liftoff table is not. Vertical liftoff is the boxed-in answer - no strip needed - but
     * only above the stunt floor: below even odds the expected outcome is the crash that created
     * this situation. Neither means stay and fight as a turret.
     */
    static TakeoffMode chooseTakeoffMode(boolean runwayClear, boolean verticalLegal,
          double verticalOdds) {
        if (runwayClear) {
            return TakeoffMode.RUNWAY;
        }
        if (verticalLegal && (verticalOdds >= TAKEOFF_ROLL_FLOOR)) {
            return TakeoffMode.VERTICAL;
        }
        return TakeoffMode.STAY;
    }

    /**
     * Moves the fighters whose engagement is already decided before the ones still waiting to find out.
     *
     * <p>Aerospace units move last and in their own turn class, interleaved between players, so partway
     * through that block some enemy fighters have committed to an altitude this turn and some have not. A
     * fighter facing opponents who have committed can pick an altitude that guarantees an engagement; one
     * facing opponents still to move can only guess. Spending the earlier turn slots on the decided fighters
     * leaves the undecided ones later, by which time more of the enemy has committed.</p>
     *
     * <p>Within a force, the heaviest fighter goes first and its wingmen follow, so an element always keeps
     * something back that can react to whatever its lead flushes out (the move-order habit described in the
     * aerospace primer, chapter 10.4).</p>
     */
    @Override
    protected double calculateMoveIndex(final Entity entity, final StringBuilder msg) {
        double moveIndex = super.calculateMoveIndex(entity, msg);
        if (!isAtmosphericAerospace(entity)) {
            return moveIndex;
        }

        double reactionFactor = reactionFactor(entity);
        if (reactionFactor != 1.0) {
            msg.append("\n\t\tx")
                  .append(String.format("%.2f", reactionFactor))
                  .append(" (aerospace reaction: enemy fighters committed)");
        }

        double elementFactor = isForceLead(entity) ? LEAD_PRIORITY : 1.0;
        if (elementFactor != 1.0) {
            msg.append("\n\t\tx")
                  .append(String.format("%.2f", elementFactor))
                  .append(" (aerospace element lead moves first)");
        }

        return moveIndex * reactionFactor * elementFactor;
    }

    /**
     * How much sooner this fighter should move, based on how much of the opposing air picture has settled.
     *
     * @param entity the fighter being ordered
     *
     * @return a multiplier on the move index, 1.0 when there is no opposing air to wait for
     */
    private double reactionFactor(Entity entity) {
        int airborneEnemies = 0;
        int committedEnemies = 0;
        for (Entity enemy : getEnemyEntities()) {
            if (!enemy.isAero() || !enemy.isAirborne() || (enemy.getPosition() == null)) {
                continue;
            }
            // Board filter: an enemy over a different map tells this fighter nothing.
            if (enemy.getBoardId() != entity.getBoardId()) {
                continue;
            }
            airborneEnemies++;
            if (!enemy.isSelectableThisTurn() || enemy.isImmobile()) {
                committedEnemies++;
            }
        }

        if (airborneEnemies == 0) {
            // Nothing in the air to react to; ground-attack ordering is the stock ranking's business.
            return 1.0;
        }
        double committedShare = (double) committedEnemies / airborneEnemies;
        return UNDECIDED_DEFERRAL + (committedShare * (DECIDED_PRIORITY - UNDECIDED_DEFERRAL));
    }

    /**
     * Whether this fighter is the one its element forms up on.
     *
     * <p>The heaviest aircraft in a force is taken as its lead. Units with no force assigned are all leads,
     * which leaves their ordering exactly as the stock index had it.</p>
     *
     * @param entity the fighter being ordered
     *
     * @return {@code true} if no heavier friendly aircraft shares this unit's force
     */
    private boolean isForceLead(Entity entity) {
        if (entity.getForceId() == Force.NO_FORCE) {
            return true;
        }
        for (Entity friend : getFriendEntities()) {
            if ((friend.getId() == entity.getId()) || (friend.getForceId() != entity.getForceId())) {
                continue;
            }
            if (!friend.isAero() || !friend.isAirborne()) {
                continue;
            }
            if (friend.getWeight() > entity.getWeight()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected List<Coords> prioritizeDeploymentCoords(Entity deployedUnit, List<Coords> possibleDeployCoords) {
        // CASPAR divergence: deploy as a formation rather than scattering across the whole zone.
        int formationRadius = FormationGeometry.formationRadius(getEntitiesOwned(), mutualSupportMultiplier());
        LOGGER.info("[MutualSupport] {}: forming up {} within {} hexes of the force centre",
              getLocalPlayer() != null ? getLocalPlayer().getName() : getName(),
              deployedUnit.getChassis(),
              formationRadius);
        return MutualSupportDeployment.prioritize(deployedUnit,
              possibleDeployCoords,
              getFriendEntities(),
              getGame(),
              formationRadius);
    }

    /**
     * The player's mutual support setting: how tightly this bot keeps its force together, as a multiplier around 1.0.
     *
     * <p>The doctrine's single read of the underlying behavior setting, so no other Mutual Support code has to know
     * what it is currently called.</p>
     *
     * @return the multiplier, where higher means a tighter formation
     */
    private double mutualSupportMultiplier() {
        return getBehaviorSettings().getMutualSupportValue();
    }
}
