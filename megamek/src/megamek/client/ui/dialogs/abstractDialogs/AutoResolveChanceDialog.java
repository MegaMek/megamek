/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.abstractDialogs;

import java.awt.BorderLayout;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.RawImagePanel;
import megamek.common.Configuration;
import megamek.common.autoResolve.Resolver;
import megamek.common.autoResolve.acar.SimulationOptions;
import megamek.common.autoResolve.converter.SetupForces;
import megamek.common.autoResolve.event.AutoResolveConcludedEvent;
import megamek.common.board.Board;
import megamek.common.internationalization.I18n;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.logging.MMLogger;
import megamek.server.victory.VictoryResult;
import org.apache.commons.lang3.time.StopWatch;

public class AutoResolveChanceDialog extends AbstractDialog implements PropertyChangeListener {
    private static final MMLogger logger = MMLogger.create(AutoResolveChanceDialog.class);

    private JProgressBar progressBar;
    private final int numberOfSimulations;
    private RawImagePanel splash;
    private final Task task;

    private final List<String> progressText;
    private SimulationScore finalScore;
    private final SetupForces setupForces;
    private final int numberOfThreads;
    private final int currentTeam;
    private final Board board;
    private final PlanetaryConditions planetaryConditions;
    private int returnCode = JOptionPane.CLOSED_OPTION;
    private final TreeMap<Integer, String> splashImages = new TreeMap<>();

    {
        splashImages.put(0, Configuration.miscImagesDir() + "/acar_splash_hd.png");
    }

    private static class SimulationScore {
        private final AtomicInteger victories;
        private final AtomicInteger losses;
        private final AtomicInteger draws;
        private final AtomicInteger noResult;
        private final AtomicInteger gamesRun;
        private final int teamOfInterest;

        public SimulationScore(int teamOfInterest) {
            this.victories = new AtomicInteger(0);
            this.losses = new AtomicInteger(0);
            this.draws = new AtomicInteger(0);
            this.gamesRun = new AtomicInteger(0);
            this.noResult = new AtomicInteger(0);
            this.teamOfInterest = teamOfInterest;
        }

        public void addResult(AutoResolveConcludedEvent event) {
            this.addResult(event.getVictoryResult());
        }

        public void addResult(VictoryResult victoryResult) {
            if (victoryResult.getWinningTeam() == teamOfInterest) {
                victories.incrementAndGet();
            } else if (victoryResult.getWinningTeam() != teamOfInterest) {
                losses.incrementAndGet();
            } else {
                draws.incrementAndGet();
            }
            gamesRun.incrementAndGet();
        }

        public void addEmptyResult() {
            gamesRun.incrementAndGet();
            noResult.incrementAndGet();
        }

        public int getVictories() {
            return victories.get();
        }

        public int getRuns() {
            return gamesRun.get();
        }

        public int getLosses() {
            return losses.get();
        }

        public int getDraws() {
            return draws.get();
        }

        public int noResults() {
            return noResult.get();
        }
    }

    public static int showDialog(JFrame frame, int numberOfSimulations, int numberOfThreads, int currentTeam,
          SetupForces setupForces, Board board, PlanetaryConditions planetaryConditions) {
        var dialog = new AutoResolveChanceDialog(frame, numberOfSimulations, numberOfThreads, currentTeam,
              setupForces, board, planetaryConditions);
        dialog.setModal(true);
        dialog.getTask().execute();
        dialog.setVisible(true);
        final int returnCode = dialog.returnCode;
        dialog.dispose();
        return returnCode;
    }

    private AutoResolveChanceDialog(JFrame frame, int numberOfSimulations, int numberOfThreads, int currentTeam,
          SetupForces setupForces, Board board, PlanetaryConditions planetaryConditions) {
        super(frame, true, "AutoResolveMethod.dialog.name", "AutoResolveMethod.dialog.title");
        this.numberOfSimulations = numberOfSimulations;
        this.setupForces = setupForces;
        this.numberOfThreads = numberOfThreads;
        this.currentTeam = currentTeam;
        this.board = board;
        this.planetaryConditions = planetaryConditions;
        this.task = new Task(this);
        getTask().addPropertyChangeListener(this);
        progressText = new ArrayList<>();
        for (int i = 1; i < 100; i++) {
            progressText.add("AutoResolveMethod.progress." + i);
        }
        Collections.shuffle(progressText);
        initialize();
    }

    @Override
    protected void initialize() {
        setUndecorated(true);
        setLayout(new BorderLayout());
        add(createCenterPane(), BorderLayout.CENTER);
        add(createProgressBar(), BorderLayout.PAGE_END);
        finalizeInitialization();
    }

    @Override
    protected Container createCenterPane() {
        setSplash(UIUtil.createSplashComponent(splashImages, getFrame()));
        return getSplash();
    }

    private JProgressBar createProgressBar() {
        setProgressBar(new JProgressBar(0, 100));

        getProgressBar().setString(I18n.getText("AutoResolveMethod.progress.0"));
        getProgressBar().setValue(0);
        getProgressBar().setStringPainted(true);
        getProgressBar().setVisible(true);
        getProgressBar().setIndeterminate(true);
        return getProgressBar();
    }

    @Override
    protected void finalizeInitialization() {
        setPreferredSize(getSplash().getPreferredSize());
        setSize(getSplash().getPreferredSize());
        fitAndCenter();
    }

    private Task getTask() {
        return task;
    }

    public RawImagePanel getSplash() {
        return splash;
    }

    public void setSplash(final RawImagePanel splash) {
        this.splash = splash;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public void setProgressBar(final JProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        int progress = Math.min(getProgressBar().getMaximum(), task.getProgress());
        if (progress > 0) {
            getProgressBar().setIndeterminate(false);
            getProgressBar().setValue(progress);
        }

        var maxProgress = getProgressBar().getMaximum();
        var numberOfGags = 25;

        var factor = numberOfGags / (double) maxProgress;

        var index = clamp((long) (factor * progress) % 98, 3, 98);

        getProgressBar().setString(I18n.getText(progressText.get(index)));
    }

    public static int clamp(long value, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException(min + " > " + max);
        }
        return (int) Math.min(max, Math.max(value, min));
    }

    /**
     * Main task. This is executed in a background thread.
     */
    private class Task extends SwingWorker<Integer, Integer> {
        AutoResolveChanceDialog dialog;

        public Task(AutoResolveChanceDialog dialog) {
            this.dialog = dialog;
        }

        @Override
        public Integer doInBackground() {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            var simulatedVictories = calculateNumberOfVictories();
            stopWatch.stop();
            dialog.finalScore = simulatedVictories;


            var numberOfSimulations = dialog.numberOfSimulations;

            String messageKey = "AutoResolveDialog.messageSimulated";
            String title = I18n.getText("AutoResolveDialog.title");
            String message;

            if (simulatedVictories.getRuns() == 0 && simulatedVictories.getRuns() < numberOfSimulations) {
                messageKey = "AutoResolveDialog.messageFailedCalc";
                logger.warn("No combat scenarios were simulated, possible error!");
            } else {
                var timePerRun = (stopWatch.getTime(TimeUnit.MILLISECONDS) / (double) numberOfSimulations)
                      / (double) numberOfThreads;
                logger.debug(
                      "Simulated victories: {} runs, {} victories, {} losses, {} draws, {} failed - processed in {} seconds per CPU core - total of {}",
                      simulatedVictories.getRuns(),
                      simulatedVictories.getVictories(),
                      simulatedVictories.getLosses(),
                      simulatedVictories.getDraws(),
                      simulatedVictories.noResults(),
                      timePerRun,
                      stopWatch.toString());
            }

            message = I18n.getFormattedText(messageKey,
                  simulatedVictories.getRuns(),
                  simulatedVictories.getVictories(),
                  simulatedVictories.getLosses(),
                  simulatedVictories.getDraws(),
                  simulatedVictories.getRuns() != 0 ?
                        simulatedVictories.getVictories() * 100 / simulatedVictories.getRuns() :
                        0);

            var code = JOptionPane.showConfirmDialog(
                  getFrame(),
                  message, title,
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.INFORMATION_MESSAGE);

            dialog.returnCode = code;
            return code;
        }

        /**
         * Calculates the victory chance for a given scenario and list of units by running multiple auto resolve
         * scenarios in parallel.
         *
         * @return the calculated victory chance score
         */
        private SimulationScore calculateNumberOfVictories() {
            var simulationScore = new SimulationScore(currentTeam);
            if (dialog.numberOfSimulations <= 0) {
                return simulationScore;
            }
            AtomicInteger runCounter = new AtomicInteger(0);

            var executor = Executors.newFixedThreadPool(numberOfThreads);
            try {
                List<Future<AutoResolveConcludedEvent>> futures = new ArrayList<>();
                for (int i = 0; i < numberOfSimulations; i++) {
                    futures.add(executor.submit(() -> {
                        var autoResolveConcludedEvent = Resolver.simulationRunWithoutLog(
                                    setupForces, SimulationOptions.empty(), new Board(board.getWidth(), board.getHeight()),
                                    new PlanetaryConditions(planetaryConditions))
                              .resolveSimulation();
                        setProgress(Math.min(100 * runCounter.incrementAndGet() / numberOfSimulations, 100));
                        return autoResolveConcludedEvent;
                    }));
                }

                // Wait for all tasks to complete
                for (Future<AutoResolveConcludedEvent> future : futures) {
                    try {
                        var event = future.get();
                        simulationScore.addResult(event);
                    } catch (InterruptedException | ExecutionException e) {
                        simulationScore.addEmptyResult();
                        logger.error("While processing simulation", e);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                executor.shutdown();
            }


            return simulationScore;
        }

        /**
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            setVisible(false);
        }
    }
}
