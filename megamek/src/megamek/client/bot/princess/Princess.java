/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import megamek.client.bot.BotClient;
import megamek.client.bot.ChatProcessor;
import megamek.client.bot.PhysicalOption;
import megamek.client.bot.princess.FireControl.PhysicalAttackType;
import megamek.client.bot.princess.PathRanker.RankedPath;
import megamek.common.Building;
import megamek.common.BuildingTarget;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Mech;
import megamek.common.MechWarrior;
import megamek.common.Minefield;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.PilotingRollData;
import megamek.common.Targetable;
import megamek.common.containers.PlayerIDandList;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.logging.LogLevel;
import megamek.common.logging.Logger;
import megamek.common.util.StringUtil;

public class Princess extends BotClient {

    private static final Logger logger = new Logger();

    private boolean initialized = false;

    private PathSearcher pathSearcher;
    private BasicPathRanker pathRanker;
    private FireControl fireControl;
    private BehaviorSettings behaviorSettings;
    private double moveEvaluationTimeEstimate = 0;
    private Precognition precognition;
    private final Set<Coords> strategicTargets = new HashSet<Coords>();
    private boolean flee = false;
    protected ChatProcessor chatProcessor = new ChatProcessor();
    private boolean mustFlee = false;

    public Princess(String name, String host, int port, LogLevel verbosity) {
        super(name, host, port);
        logger.setVerbosity(verbosity);
        setBehaviorSettings(BehaviorSettingsFactory.getInstance(logger).DEFAULT_BEHAVIOR);
    }

    public LogLevel getVerbosity() {
        return logger.getVerbosity();
    }

    public BasicPathRanker getPathRanker() {
        return pathRanker;
    }

    public boolean shouldFlee() {
        return flee;
    }

    public boolean isMustFlee() {
        return mustFlee;
    }

    public void setMustFlee(boolean mustFlee) {
        this.mustFlee = mustFlee;
    }

    protected Precognition getPrecognition() {
        return precognition;
    }

    public void setShouldFlee(boolean shouldFlee, String reason) {
        log(getClass(), "setShouldFlee(boolean, String)", LogLevel.INFO, "Setting Should Flee " + shouldFlee +
                                                                         " because: " + reason);
        flee = shouldFlee;
    }

    public void setBehaviorSettings(BehaviorSettings behaviorSettings) {
        log(getClass(), "setBehaviorSettings(BehaviorSettings)", LogLevel.INFO, "New behavior settings for " +
                                                                                getName() + "\n" +
                                                                                behaviorSettings.toLog());
        this.behaviorSettings = behaviorSettings;
        getStrategicTargets().clear();
        setShouldFlee(behaviorSettings.shouldAutoFlee(), "Configured to auto flee.");
        if (shouldFlee()) {
            return;
        }

        for (String targetCoords : behaviorSettings.getStrategicTargets()) {
            if (!StringUtil.isPositiveInteger(targetCoords) || (targetCoords.length() != 4)) {
                continue;
            }
            String x = targetCoords.substring(0, 2);
            String y = targetCoords.replaceFirst(x, "");
            Coords coords = new Coords(Integer.parseInt(x), Integer.parseInt(y));
            getStrategicTargets().add(coords);
        }
    }

    public FireControl getFireControl() {
        return fireControl;
    }

    public BehaviorSettings getBehaviorSettings() {
        return behaviorSettings;
    }

    public Set<Coords> getStrategicTargets() {
        return strategicTargets;
    }

    @Override
    protected Vector<Coords> calculateArtyAutoHitHexes() {
        final String METHOD_NAME = "calculateArtyAutoHitHexes()";
        methodBegin(getClass(), METHOD_NAME);

        try {
            // currently returns no artillery hit spots
            // make an empty list
            PlayerIDandList<Coords> artyAutoHitHexes = new PlayerIDandList<Coords>();
            // attach my player id to it
            artyAutoHitHexes.setPlayerID(getLocalPlayer().getId());
            return artyAutoHitHexes;
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    @Override
    protected void calculateDeployment() {
        final String METHOD_NAME = "calculateDeployment()";
        methodBegin(getClass(), METHOD_NAME);

        try {

            // get the first unit
            int entityNum = game.getFirstDeployableEntityNum();
            if (logger.getVerbosity().getLevel() > LogLevel.WARNING.getLevel()) {
                sendChat("deploying unit " + getEntity(entityNum).getChassis());
            }

            // on the list to be deployed get a set of all the
            Coords[] startingCoords = getStartingCoordsArray();
            if (startingCoords.length == 0) {
                log(getClass(), METHOD_NAME, LogLevel.ERROR,
                        "No valid locations to deploy "
                                + getEntity(entityNum).getDisplayName());
            }
            // get the coordinates I can deploy on
            Coords deployCoords = getCoordsAround(getEntity(entityNum), startingCoords);
            if (deployCoords == null) {
                log(getClass(),
                        METHOD_NAME,
                        LogLevel.ERROR,
                        "getCoordsAround gave no location for "
                                + getEntity(entityNum).getChassis());
            }
            // first coordinate that is legal to put this unit on now find some sort of reasonable facing. If there
            // are deployed enemies, face them
            int decentFacing = -1;
            for (Entity e : getEnemyEntities()) {
                if (e.isDeployed() && (!e.isOffBoard())) {
                    decentFacing = deployCoords.direction(e.getPosition());
                    break;
                }
            }
            // if I haven't found a decent facing, then at least face towards the center of the board
            if (decentFacing == -1) {
                Coords center = new Coords(game.getBoard().getWidth() / 2, game
                        .getBoard().getHeight() / 2);
                decentFacing = deployCoords.direction(center);
            }
            deploy(entityNum, deployCoords, decentFacing, 0);
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    @Override
    protected void calculateFiringTurn() {
        final String METHOD_NAME = "calculateFiringTurn()";
        methodBegin(getClass(), METHOD_NAME);

        try {
            Entity shooter = game.getFirstEntity(getMyTurn()); // get the first
            // entity that can
            // act this turn
            // make sure weapons are loaded
            fireControl.loadAmmo(shooter, game);
            FireControl.FiringPlan plan = fireControl.getBestFiringPlan(
                    shooter, game);
            if (plan != null) {
                log(getClass(), METHOD_NAME, plan.getDebugDescription(false));
                // tell the game I want to fire
                sendAttackData(shooter.getId(), plan.getEntityActionVector(game));

            } else {
                sendAttackData(shooter.getId(), null);
            }
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    @Override
    protected Vector<Minefield> calculateMinefieldDeployment() {
        final String METHOD_NAME = "calculateMinefieldDeployment()";
        methodBegin(getClass(), METHOD_NAME);

        try {
            // currently returns no minefields
            // make an empty vector
            return new Vector<Minefield>();
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    @Override
    protected MovePath calculateMoveTurn() {
        final String METHOD_NAME = "calculateMoveTurn()";
        methodBegin(getClass(), METHOD_NAME);

        try {
            // first move useless units: immobile units, ejected mechwarrior,
            // etc
            Entity moving_entity = null;
            Entity e = game.getFirstEntity();
            do {
                // ignore loaded and off-board units
                if ((e.getPosition() == null) || e.isOffBoard()) {
                    continue;
                }
                if (e.isImmobile()) {
                    moving_entity = e;
                    break;
                }
                if (e instanceof MechWarrior) {
                    moving_entity = e;
                    break;
                }
                e = game.getEntity(game.getNextEntityNum(e.getId()));
            } while (!e.equals(game.getFirstEntity()));
            // after that, moving farthest units first
            if (moving_entity == null) {
                double furthest_dist = 0;
                e = game.getFirstEntity();
                do {
                    // ignore loaded and off-board units
                    if ((e.getPosition() == null) || e.isOffBoard()) {
                        continue;
                    }
                    double dist = pathRanker.distanceToClosestEnemy(e, e.getPosition(), game);
                    if ((moving_entity == null) || (dist > furthest_dist)) {
                        moving_entity = e;
                        furthest_dist = dist;
                    }
                    e = game.getEntity(game.getNextEntityNum(e.getId()));
                } while (!e.equals(game.getFirstEntity()));
            }

            return continueMovementFor(moving_entity);
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    @Override
    protected PhysicalOption calculatePhysicalTurn() {
        final String METHOD_NAME = "calculatePhysicalTurn()";
        methodBegin(getClass(), METHOD_NAME);

        try {
            // get the first entity that can act this turn
            Entity first_entity = game.getFirstEntity(getMyTurn());
            Entity hitter = first_entity;
            FireControl.PhysicalInfo best_attack = null;
            do {
                log(getClass(),
                        METHOD_NAME,
                        "Calculating physical attacks for "
                                + hitter.getDisplayName());
                // this is an array of all my enemies
                ArrayList<Entity> enemies = getEnemyEntities();
                // cycle through potential enemies
                for (Entity e : enemies) {
                    if (e.getPosition() == null) {
                        continue; // Skip enemies not on the board.
                    }
                    FireControl.PhysicalInfo right_punch = new FireControl.PhysicalInfo(
                            hitter, e, PhysicalAttackType.RIGHT_PUNCH, game);
                    fireControl.calculateUtility(right_punch);
                    if (right_punch.utility > 0) {
                        if ((best_attack == null)
                                || (right_punch.utility > best_attack.utility)) {
                            best_attack = right_punch;
                        }
                    }
                    FireControl.PhysicalInfo left_punch = new FireControl.PhysicalInfo(
                            hitter, e, PhysicalAttackType.LEFT_PUNCH, game);
                    fireControl.calculateUtility(left_punch);
                    if (left_punch.utility > 0) {
                        if ((best_attack == null)
                                || (left_punch.utility > best_attack.utility)) {
                            best_attack = left_punch;
                        }
                    }
                    FireControl.PhysicalInfo right_kick = new FireControl.PhysicalInfo(
                            hitter, e, PhysicalAttackType.RIGHT_KICK, game);
                    if (right_kick.utility > 0) {
                        if ((best_attack == null)
                                || (right_kick.utility > best_attack.utility)) {
                            best_attack = right_kick;
                        }
                    }
                    FireControl.PhysicalInfo left_kick = new FireControl.PhysicalInfo(
                            hitter, e, PhysicalAttackType.LEFT_KICK, game);
                    if (left_kick.getExpectedDamage() > 0) {
                        if ((best_attack == null)
                                || (left_kick.utility > best_attack.utility)) {
                            best_attack = left_kick;
                        }
                    }

                }
                if (best_attack != null) {
                    log(getClass(), METHOD_NAME, "Attack is a "
                            + best_attack.attack_type.name());
                } else {
                    log(getClass(), METHOD_NAME, "No useful attack to be made");
                }
                if (best_attack != null) {
                    return best_attack.getAsPhysicalOption();
                }
                hitter = game.getEntity(game.getNextEntityNum(hitter.getId()));
                // otherwise, check if the next entity can hit something
                if (hitter.equals(first_entity)) {
                    hitter = null; // getNextEntity is incorrect, it does not
                                   // return
                    // null at the end, it returns the first entity
                }
            } while (hitter != null);
            // no one can hit anything anymore, so give up
            return null;
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    private boolean wantsToFlee(Entity entity) {
        if (shouldFlee() || isMustFlee()) {
            return true;
        }
        return entity.isCrippled() && behaviorSettings.isForcedWithdrawal();
    }

    private boolean isFleeing(Entity entity) {
        if (entity.isImmobile()) {
            return false;
        }

        return wantsToFlee(entity);
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean mustFleeBoard(Entity entity) {
        if (!isFleeing(entity)) {
            return false;
        }
        if (!entity.canFlee()) {
            return false;
        }
        if (pathRanker.distanceToHomeEdge(entity.getPosition(), getHomeEdge(), game) > 0) {
            return false;
        }
        return true;
    }

    private boolean isImmobilized(Entity mover) {
        final String METHOD_NAME = "isImmobilized(Entity, MovePath)";
        if (mover.isImmobile() && !mover.isShutDown()) {
            log(getClass(), METHOD_NAME, LogLevel.INFO, "Is truly immobile.");
            return true;
        }
        if (!(mover instanceof Mech)) {
            return false;
        }

        Mech mech = (Mech)mover;
        if (!mech.isProne() && !mech.isStuck() && !mech.isStalled()) {
            return false;
        }

        MovePath movePath = new MovePath(game, mover);

        // For a normal fall-shame setting (index 5), our threshold should be a 10+ piloting roll.
        int threshold;
        switch (behaviorSettings.getFallShameIndex()) {
            case 10:
                threshold = 7;
                break;
            case 9:
                threshold = 8;
                break;
            case 8:
            case 7:
                threshold = 9;
                break;
            case 6:
                threshold = 10;
                break;
            case 5:
                threshold = 11;
                break;
            case 4:
            case 3:
                threshold = 12;
                break;
            default:
                threshold = 13; // Actually impossible.
        }

        // If we're prone, see if we have a chance of getting up.
        if (mech.isProne()) {
            if (mech.cannotStandUpFromHullDown()) {
                log(getClass(), METHOD_NAME, LogLevel.INFO, "Cannot stand up.");
                return true;
            }

            MovePath.MoveStepType type = (game.getOptions().booleanOption("tacops_careful_stand") ?
                    MovePath.MoveStepType.CAREFUL_STAND :
                    MovePath.MoveStepType.GET_UP);
            MoveStep getUp = new MoveStep(movePath, type);

            // If our odds to get up are equal to or worse than the threshold, consider ourselves immobile.
            PilotingRollData target = mech.checkGetUp(getUp);
            log(getClass(), METHOD_NAME, LogLevel.INFO,
                    "Need to roll " + target.getValue() + " to stand and our tolerance is " + threshold);
            return (target.getValue() >= threshold);
        }

        // How likely are we to get unstuck.
        MovePath.MoveStepType type = MovePath.MoveStepType.FORWARDS;
        MoveStep walk = new MoveStep(movePath, type);
        IHex hex = getBoard().getHex(mech.getPosition());
        PilotingRollData target = mech.checkBogDown(walk, hex, mech.getPriorPosition(), mech.getPosition(),
                hex.getElevation(), false);
        log(getClass(), METHOD_NAME, LogLevel.INFO,
                "Need to roll " + target.getValue() + " to get unstuck and our tolerance is " + threshold);
        return (target.getValue() >= threshold);
    }

    protected ArrayList<MovePath> getMovePaths(Entity entity) {
        return getPrecognition().getPathEnumerator().unit_paths.get(entity.getId());
    }

    protected ArrayList<RankedPath> rankPaths(ArrayList<MovePath> paths){
        return getPathRanker().rankPaths(paths, getGame());
    }

    @Override
    protected MovePath continueMovementFor(Entity entity) {
        final String METHOD_NAME = "continueMovementFor(Entity)";
        methodBegin(getClass(), METHOD_NAME);

        try {
            // figure out who moved last, and who's move lists need to be updated

            // moves this entity during movement phase
            log(getClass(), METHOD_NAME, "Moving " + entity.getDisplayName() + " (ID " + entity.getId() + ")");
            getPrecognition().insureUpToDate();

            if (wantsToFlee(entity)) {
                String msg = entity.getDisplayName();
                if (entity.isCrippled()) {
                    msg += " is crippled and withdrawing.";
                } else if (shouldFlee()) {
                    msg += " is retreating.";
                } else if (mustFlee) {
                    msg += " is forced to withdraw.";
                }
                log(getClass(), METHOD_NAME, msg);
                sendChat(msg);

                // If this entity must withdraw, is on its home edge and is able to flee the board, do so.
                if (mustFleeBoard(entity)) {
                    MovePath mp = new MovePath(game, entity);
                    mp.addStep(MovePath.MoveStepType.FLEE);
                    return mp;
                }

                // If we want to flee, but cannot, eject the crew.
                if (isImmobilized(entity) && entity.isEjectionPossible()) {
                    msg = entity.getDisplayName() + " is immobile.  Abandoning unit.";
                    log(getClass(), METHOD_NAME, LogLevel.INFO, msg);
                    sendChat(msg);
                    MovePath mp = new MovePath(game, entity);
                    mp.addStep(MovePath.MoveStepType.EJECT);
                    return mp;
                }
            }

            ArrayList<MovePath> paths = getPrecognition().getPathEnumerator().unit_paths.get(entity.getId());

            if (paths == null) {
                log(getClass(), METHOD_NAME, LogLevel.WARNING,
                        "No valid paths found.");
                return new MovePath(game, entity);
            }
            double this_time_estimate = (paths.size() * moveEvaluationTimeEstimate) / 1e3;
            if (logger.getVerbosity().getLevel() > LogLevel.WARNING.getLevel()) {
                String timeestimate = "unknown.";
                if (this_time_estimate != 0) {
                    timeestimate = Integer.toString((int) this_time_estimate)
                            + " seconds";
                }
                String message = "Moving " + entity.getChassis() + ". "
                        + Long.toString(paths.size())
                        + " paths to consider.  Estimated time to completion: "
                        + timeestimate;
                sendChat(message);
            }
            long start_time = System.currentTimeMillis();
            getPathRanker().initUnitTurn(entity, getGame());
            ArrayList<RankedPath> rankedpaths = rankPaths(paths);
            long stop_time = System.currentTimeMillis();
            // update path evaluation time estimate
            double updated_estimate = ((double) (stop_time - start_time)) / ((double) paths.size());
            if (moveEvaluationTimeEstimate == 0) {
                moveEvaluationTimeEstimate = updated_estimate;
            }
            moveEvaluationTimeEstimate = 0.5 * (updated_estimate + moveEvaluationTimeEstimate);
            if (rankedpaths.size() == 0) {
                return new MovePath(game, entity);
            }
            log(getClass(), METHOD_NAME, "Path ranking took " + Long.toString(stop_time - start_time) + " millis");
            precognition.unpause();
            RankedPath bestpath = PathRanker.getBestPath(rankedpaths);
            log(getClass(), METHOD_NAME,
                    "Best Path: " + bestpath.path.toString() + "  Rank: "
                            + bestpath.rank);
            bestpath.path.printAllSteps();
            return bestpath.path;
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    @Override
    protected void initFiring() {
        final String METHOD_NAME = "initFiring()";
        methodBegin(getClass(), METHOD_NAME);

        try {

            // ----Debugging: print out any errors made in guessing to hit
            // values-----
            Vector<Entity> ents = game.getEntitiesVector();
            for (Entity ent : ents) {
                String errors = fireControl.checkAllGuesses(ent, game);
                if (errors != null) {
                    log(getClass(), METHOD_NAME, LogLevel.ERROR, errors);
                }
            }
            // -----------------------------------------------------------------------
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    @Override
    protected void initMovement() {
        final String METHOD_NAME = "initMovement()";
        methodBegin(getClass(), METHOD_NAME);

        try {
            // reset strategic targets
            fireControl.additional_targets = new ArrayList<Targetable>();
            for (Coords strategic_target : getStrategicTargets()) {
                if (game.getBoard().getBuildingAt(strategic_target) == null) {
                    sendChat("No building to target in Hex "
                            + strategic_target.toFriendlyString()
                            + ", ignoring.");
                } else {
                    fireControl.additional_targets.add(new BuildingTarget(
                            strategic_target, game.getBoard(), false));
                    sendChat("Building in Hex "
                            + strategic_target.toFriendlyString()
                            + " designated strategic target.");
                }
            }

            // Pick up on any turrets and shoot their buildings as well.
            Enumeration<Building> buildings = game.getBoard().getBuildings();
            while (buildings.hasMoreElements()) {
                Building bldg = buildings.nextElement();
                Enumeration<Coords> bldgCoords = bldg.getCoords();
                while (bldgCoords.hasMoreElements()) {
                    Coords coords = bldgCoords.nextElement();
                    for (Enumeration<Entity> i = game.getEntities(coords, true); i
                            .hasMoreElements();) {
                        Entity entity = i.nextElement();
                        BuildingTarget bt = new BuildingTarget(coords,
                                game.getBoard(), false);
                        if ((entity instanceof GunEmplacement)
                                && entity.getOwner()
                                        .isEnemyOf(getLocalPlayer())
                                && (fireControl.additional_targets.indexOf(bt) == -1)) {
                            fireControl.additional_targets.add(bt);
                            sendChat("Building in Hex "
                                    + coords.toFriendlyString()
                                    + " designated target due to Gun Emplacement.");
                        }
                    }
                }
            }
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    public IGame getGame() {
        return game;
    }

    @Override
    public void initialize() {
        final String METHOD_NAME = "initialize()";
        methodBegin(getClass(), METHOD_NAME);

        try {
            if (initialized) {
                return; // no need to initialize twice
            }
            PathSearcher pathSearcher = new PathSearcher(this);
            pathRanker = new BasicPathRanker(this);
            pathSearcher.ranker = pathRanker;
            fireControl = new FireControl(this);
            pathRanker.setFireControl(fireControl);
            precognition = new Precognition(this);
            precognition.setGame(getGame());
            pathRanker.setPathEnumerator(precognition.getPathEnumerator());

            Thread precognitionThread = new Thread(precognition, "Princess-precognition");
            precognitionThread.start();

            // Pick up any turrets and add their buildings to the strategic targets list.
            Enumeration<Building> buildings = getGame().getBoard().getBuildings();
            while (buildings.hasMoreElements()) {
                Building bldg = buildings.nextElement();
                Enumeration<Coords> bldgCoords = bldg.getCoords();
                while (bldgCoords.hasMoreElements()) {
                    Coords coords = bldgCoords.nextElement();
                    for (Enumeration<Entity> i = getGame().getEntities(coords, true); i.hasMoreElements();) {
                        Entity entity = i.nextElement();
                        if (entity instanceof GunEmplacement && entity.getOwner().isEnemyOf(getLocalPlayer())
                                && !getStrategicTargets().contains(coords)) {
                            getStrategicTargets().add(coords);
                            sendChat("Building in Hex " + coords.toFriendlyString() +
                                    " designated target due to Gun Emplacement.");
                        }
                    }
                }
            }

            initialized = true;
            BotGeometry.debugSelfTest(this);
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    @Override
    protected void processChat(GamePlayerChatEvent ge) {
        final String METHOD_NAME = "processChat(GamePlayerChatEvent)";
        methodBegin(getClass(), METHOD_NAME);

        try {
            String msg = "Received message: \"" + ge.getMessage()
                    + "\".\tMessage type: " + ge.getEventName();
            log(getClass(), METHOD_NAME, msg);

            //            StringTokenizer st = new StringTokenizer(ge.getMessage(), ":"); //$NON-NLS-1$
            // String message = st.nextToken();
            // if (message == null) {
            // return;
            // }
            // if (message.contains("flee")) {
            // log(getClass(), METHOD_NAME, "Received flee order!");
            // sendChat("Run Away!");
            // should_flee = true;
            // }

            StringTokenizer st = new StringTokenizer(ge.getMessage(), ":"); //$NON-NLS-1$
            String nameFrom = st.nextToken();
            String secondToken = null;
            String nameTo = null;
            String message = null;

            if (st.hasMoreTokens()) {
                secondToken = st.nextToken().trim();

                if ("help".equalsIgnoreCase(secondToken)) {

                    sendChat("Available commands :");
                    String[] commands = { "[help]", "[botname]:flee",
                            "[botname]:reset (Resets bot parameters from file)" };
                    for (String command : commands) {
                        sendChat(command);
                    }

                    chatProcessor.processChat(ge, this);
                    return;
                } else {
                    nameTo = secondToken;
                }
            }

            if (st.hasMoreTokens()) {
                message = st.nextToken().trim();
            }

            if ((nameTo == null) || (message == null) || (getLocalPlayer() == null)) {
                chatProcessor.processChat(ge, this);
                return;
            }

            if (nameTo.equalsIgnoreCase(getLocalPlayer().getName())) {
                if (message.equalsIgnoreCase("flee")) {
                    log(getClass(), METHOD_NAME,
                            " received flee order. Running away to " + getHomeEdge().toString() + " edge !");
                    sendChat(getLocalPlayer().getName()
                            + " received flee order. Running away to "
                            + getHomeEdge().toString() + " edge !");
                    setShouldFlee(true, "Received flee order.");

                } else if (message.equalsIgnoreCase("reset")) {
                    log(getClass(), METHOD_NAME, " reseting parameters from properties file");
                    sendChat(getLocalPlayer().getName() + " reseting parameters from properties file");
                    BehaviorSettingsFactory.getInstance().init(true);
                    setBehaviorSettings(BehaviorSettingsFactory.getInstance()
                                                               .getBehavior(behaviorSettings.getDescription()));
                }
            }

            chatProcessor.processChat(ge, this);
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    public void log(Class<?> callingClass, String methodName, LogLevel level,
            String msg) {
        logger.log(callingClass, methodName, level, msg);
    }

    public void log(Class<?> callingClass, String methodName, String msg) {
        log(callingClass, methodName, LogLevel.DEBUG, msg);
    }

    public void log(Class<?> callingClass, String methodName, LogLevel level,
            Throwable t) {
        logger.log(callingClass, methodName, level, t);
    }

    public void log(Class<?> callingClass, String methodName, Throwable t) {
        log(callingClass, methodName, LogLevel.ERROR, t);
    }

    public void methodBegin(Class<?> callingClass, String methodName) {
        log(callingClass, methodName, LogLevel.DEBUG, "method begin");
    }

    public void methodEnd(Class<?> callingClass, String methodName) {
        log(callingClass, methodName, LogLevel.DEBUG, "method end");
    }

    public HomeEdge getHomeEdge() {
        return getBehaviorSettings().getHomeEdge();
    }

    public void setHomeEdge(HomeEdge homeEdge) {
        if (homeEdge == null) {
            log(getClass(), "setHomeEdge(BasicPathRanker.HomeEdge)",
                new IllegalArgumentException("Home Edge is required!"));
            return;
        }
        getBehaviorSettings().setHomeEdge(homeEdge);
    }
}
