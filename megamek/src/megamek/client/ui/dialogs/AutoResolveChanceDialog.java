/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.client.ui.dialogs;

import megamek.client.ui.baseComponents.AbstractDialog;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.autoresolve.Resolver;
import megamek.common.autoresolve.acar.SimulationOptions;
import megamek.common.autoresolve.converter.SetupForces;
import megamek.common.autoresolve.event.AutoResolveConcludedEvent;
import megamek.common.internationalization.Internationalization;
import megamek.logging.MMLogger;
import megamek.server.victory.VictoryResult;
import org.apache.commons.lang3.time.StopWatch;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoResolveChanceDialog extends AbstractDialog implements PropertyChangeListener {
    private static final MMLogger logger = MMLogger.create(AutoResolveChanceDialog.class);

    private JProgressBar progressBar;
    private final int numberOfSimulations;
    private int returnCode = JOptionPane.CLOSED_OPTION;
    private JLabel splash;
    private final Task task;

    private final List<String> progressText;

    private final SetupForces setupForces;
    private final int numberOfThreads;
    private final int currentTeam;

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

    public static int showSimulationProgressDialog(JFrame frame, int numberOfSimulations, int currentTeam, SetupForces setupForces) {
        var dialog = new AutoResolveChanceDialog(frame, numberOfSimulations, Runtime.getRuntime().availableProcessors(), currentTeam, setupForces);
        dialog.setModal(true);
        dialog.getTask().execute();
        dialog.setVisible(true);
        return dialog.getReturnCode();
    }

    public static int showSimulationProgressDialog(JFrame frame, int numberOfSimulations, int numberOfThreads, int currentTeam, SetupForces setupForces) {
        var dialog = new AutoResolveChanceDialog(frame, numberOfSimulations, numberOfThreads, currentTeam, setupForces);
        dialog.setModal(true);
        dialog.getTask().execute();
        dialog.setVisible(true);

        return dialog.getReturnCode();
    }

    private AutoResolveChanceDialog(JFrame frame, int numberOfSimulations, int numberOfThreads, int currentTeam, SetupForces setupForces) {
        super(frame, true, "AutoResolveMethod.dialog.name","AutoResolveMethod.dialog.title");
        this.numberOfSimulations = numberOfSimulations;
        this.setupForces = setupForces;
        this.numberOfThreads = numberOfThreads;
        this.currentTeam = currentTeam;
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

        getProgressBar().setString(Internationalization.getText("AutoResolveMethod.progress.0"));
        getProgressBar().setValue(0);
        getProgressBar().setStringPainted(true);
        getProgressBar().setVisible(true);
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

    public JLabel getSplash() {
        return splash;
    }

    public void setSplash(final JLabel splash) {
        this.splash = splash;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public void setProgressBar(final JProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    private int getReturnCode() {
        return returnCode;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        int progress = Math.min(progressBar.getMaximum(), task.getProgress());
        progressBar.setValue(progress);
        var factor = clamp(numberOfSimulations / 25, 3, 99);
        int i = (int) (factor * ((float) progress / progressBar.getMaximum()));
        getProgressBar().setString(Internationalization.getText(progressText.get(i)));
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
            String message;
            String title = Internationalization.getText("AutoResolveDialog.title");
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            var simulatedVictories = calculateNumberOfVictories();
            stopWatch.stop();

            if (simulatedVictories.getRuns() == 0 && simulatedVictories.getRuns() < numberOfSimulations) {
                message = Internationalization.getText("AutoResolveDialog.messageFailedCalc");
                logger.debug("No combat scenarios were simulated, possible error!");
            } else {
                var timePerRun = stopWatch.getDuration().getSeconds() / (numberOfSimulations / Runtime.getRuntime().availableProcessors());
                logger.debug("Simulated victories: {} runs, {} victories, {} losses, {} draws, {} failed - processed in {} seconds per CPU core - total of {}",
                    simulatedVictories.getRuns(),
                    simulatedVictories.getVictories(),
                    simulatedVictories.getLosses(),
                    simulatedVictories.getDraws(),
                    simulatedVictories.noResults(),
                    timePerRun,
                    stopWatch.toString());

                message = Internationalization.getFormattedText("AutoResolveDialog.messageSimulated",
                    simulatedVictories.getRuns(),
                    simulatedVictories.getVictories(),
                    simulatedVictories.getLosses(),
                    simulatedVictories.getDraws(),
                    simulatedVictories.getVictories() * 100 / simulatedVictories.getRuns());
            }

            returnCode = JOptionPane.showConfirmDialog(
                getFrame(),
                message, title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            return returnCode;
        }

        /**
         * Calculates the victory chance for a given scenario and list of units by running multiple auto resolve scenarios in parallel.
         *
         * @return the calculated victory chance score
         */
        private SimulationScore calculateNumberOfVictories() {
            var simulationScore = new SimulationScore(currentTeam);
            if (dialog.numberOfSimulations <= 0) {
                return simulationScore;
            }
            AtomicInteger runCounter = new AtomicInteger(0);

            try (var executor = Executors.newFixedThreadPool(numberOfThreads)) {
                List<Future<AutoResolveConcludedEvent>> futures = new ArrayList<>();
                for (int i = 0; i < numberOfSimulations; i++) {
                    futures.add(executor.submit(() -> {
                        var autoResolveConcludedEvent = new Resolver(
                            setupForces, SimulationOptions.empty())
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
