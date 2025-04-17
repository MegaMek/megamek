/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MegaMek.
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
import megamek.common.*;
import megamek.common.autoresolve.Resolver;
import megamek.common.autoresolve.acar.SimulationOptions;
import megamek.common.autoresolve.converter.SetupForces;
import megamek.common.autoresolve.event.AutoResolveConcludedEvent;
import megamek.common.internationalization.I18n;
import megamek.logging.MMLogger;
import org.apache.commons.lang3.time.StopWatch;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.*;

public class AutoResolveProgressDialog extends AbstractDialog implements PropertyChangeListener {
    private static final MMLogger logger = MMLogger.create(AutoResolveProgressDialog.class);

    private JProgressBar progressBar;
    private JLabel splash;
    private final Task task;

    private final List<String> progressText;

    private final SetupForces setupForces;
    private AutoResolveConcludedEvent event;
    private final Board board;

    private static final TreeMap<Integer, String> splashImages = new TreeMap<>();
    static {
        splashImages.put(0, Configuration.miscImagesDir() + "/acar_splash_hd.png");
    }

    public static AutoResolveConcludedEvent showDialog(JFrame frame, SetupForces setupForces, Board board) {
        var dialog = new AutoResolveProgressDialog(frame, setupForces, board);
        dialog.setModal(true);
        dialog.getTask().execute();
        dialog.setVisible(true);
        var event = dialog.getEvent();
        dialog.dispose();
        return event;
    }

    private AutoResolveProgressDialog(JFrame frame, SetupForces setupForces, Board board) {
        super(frame, true, "AutoResolveMethod.dialog.name","AutoResolveMethod.dialog.title");
        this.setupForces = setupForces;
        this.board = board;
        this.task = new Task(this);
        getTask().addPropertyChangeListener(this);
        progressText = new ArrayList<>();
        for (int i = 1; i < 100; i++) {
            progressText.add("AutoResolveMethod.progress." + i);
        }
        Collections.shuffle(progressText);
        initialize();
    }

    private AutoResolveConcludedEvent getEvent() {
        return event;
    }

    private void setEvent(AutoResolveConcludedEvent event) {
        this.event = event;
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

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        int progress = task.getProgress();

        var index = clamp(progress % 98, 3, 98);

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
        AutoResolveProgressDialog dialog;

        public Task(AutoResolveProgressDialog dialog) {
            this.dialog = dialog;
        }

        @Override
        public Integer doInBackground() {

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            var result = simulateScenario();
            if (result == null) {
                JOptionPane.showMessageDialog(
                    getFrame(),
                    I18n.getText("AutoResolveDialog.messageScenarioError.text"),
                    I18n.getText("AutoResolveDialog.messageScenarioError.title"),
                    JOptionPane.INFORMATION_MESSAGE);
                return -1;
            }
            dialog.setEvent(result);
            stopWatch.stop();

            var messageKey = (result.getVictoryResult().getWinningTeam() != Entity.NONE) ?
                "AutoResolveDialog.messageScenarioTeam" :
                "AutoResolveDialog.messageScenarioPlayer";
            messageKey = ((result.getVictoryResult().getWinningTeam() == Player.TEAM_NONE)
                && (result.getVictoryResult().getWinningPlayer() == Player.PLAYER_NONE)) ?
                "AutoResolveDialog.messageScenarioDraw" :
                messageKey;
            var message = I18n.getFormattedText(messageKey,
                result.getVictoryResult().getWinningTeam(),
                result.getVictoryResult().getWinningPlayer());
            String title = I18n.getText("AutoResolveDialog.title");

            logger.info("AutoResolve simulation took: " + stopWatch.getTime(TimeUnit.MILLISECONDS) + "ms");

            JOptionPane.showMessageDialog(
                getFrame(),
                message, title,
                JOptionPane.INFORMATION_MESSAGE);

            return 0;
        }

        /**
         * Calculates the victory chance for a given scenario and list of units by running multiple auto resolve scenarios in parallel.
         *
         * @return the calculated victory chance score
         */
        private AutoResolveConcludedEvent simulateScenario() {

            AutoResolveConcludedEvent event = null;

            var executor = Executors.newFixedThreadPool(2);
            try {
                List<Future<AutoResolveConcludedEvent>> futures = new ArrayList<>();
                CountDownLatch countDownLatch = new CountDownLatch(1);
                futures.add(executor.submit(() -> {
                    int i = 0;
                    while (countDownLatch.getCount() > 0) {
                        try {
                            if (countDownLatch.await((long) (Compute.randomFloat() * 1500) + 750, TimeUnit.MILLISECONDS)) {
                                return null;
                            } else {
                                logger.info("Tick");
                                setProgress(i++ % 100);
                                if (i > 4800) {
                                    // its been at least an hour!
                                    throw new TimeoutException("Timeout");
                                }
                            }
                        } catch (InterruptedException ignored) {
                            logger.error("While waiting for countdown latch");
                        }
                    }
                    return null;
                }));
                futures.add(executor.submit(() -> {
                    try {
                        return Resolver.simulationRun(
                                setupForces, SimulationOptions.empty(), new Board(board.getWidth(), board.getHeight()))
                            .resolveSimulation();
                    } catch (Exception e) {
                        logger.error(e, e);
                    } finally {
                        countDownLatch.countDown();
                    }

                    return null;
                }));

                // Wait for all tasks to complete
                for (Future<AutoResolveConcludedEvent> future : futures) {
                    try {
                        var res = future.get();
                        if (res != null) {
                            event = res;
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        logger.error("While processing simulation", e);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                executor.shutdown();
            }

            return event;
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
