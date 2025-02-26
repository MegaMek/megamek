/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.scenario;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import megamek.SuiteConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.swing.ButtonEsc;
import megamek.client.ui.swing.CloseAction;
import megamek.client.ui.swing.CommonSettingsDialog;
import megamek.client.ui.swing.dialog.DialogButton;
import megamek.client.ui.swing.util.ClickableLabel;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceManager;
import megamek.common.scenario.Scenario;
import megamek.common.scenario.ScenarioLoader;
import megamek.logging.MMLogger;

/**
 * This dialog lists all scenarios found in MM's scenario directory as well as
 * the corresponding user directory.
 * The scenarios are grouped by subdirectory (only the one directly below
 * scenarios, further subdirectories
 * are scanned for scenarios but not used for grouping). After closing, a chosen
 * scenario can be retrieved
 * by calling {@link #getSelectedScenarioFilename()}. As a fallback, the dialog
 * also allows choosing a
 * scenario by file.
 */
public class ScenarioChooser extends AbstractButtonDialog {
    private static final MMLogger logger = MMLogger.create(ScenarioChooser.class);

    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final Map<String, List<Scenario>> sortedScenarios = sortScenarios(getScenarioInfos());
    private String scenarioFileName;

    public ScenarioChooser(final JFrame parentFrame) {
        super(parentFrame, "ScenarioChooser", "ScenarioChooser.title");
        initialize();
        setMinimumSize(
                new Dimension(ScenarioInfoPanel.BASE_MINIMUM_WIDTH, ScenarioInfoPanel.BASE_MINIMUM_HEIGHT * 3));
    }

    /**
     * @return the selected preset, or null if the dialog was cancelled or no preset
     *         was selected
     */
    public @Nullable String getSelectedScenarioFilename() {
        if (scenarioFileName != null) {
            return scenarioFileName;
        }
        Component selectedTab = tabbedPane.getSelectedComponent();
        if (!(selectedTab instanceof ScenarioInfoPane selectedPane) || !getResult().isConfirmed()) {
            return null;
        } else if (selectedPane.getSelectedPreset() != null) {
            return selectedPane.getSelectedPreset().getFileName();
        } else {
            return null;
        }
    }

    @Override
    protected Container createCenterPane() {
        for (String directory : sortedScenarios.keySet()) {
            if (!sortedScenarios.get(directory).isEmpty()) {
                ScenarioInfoPane pane = new ScenarioInfoPane(sortedScenarios.get(directory));
                tabbedPane.addTab(directory.isBlank() ? "Basic" : directory, pane);
            }
        }
        return tabbedPane;
    }

    @Override
    protected JPanel createButtonPanel() {
        JButton cancelButton = new ButtonEsc(new CloseAction(this));
        JButton okButton = new DialogButton(Messages.getString("Ok.text"));
        okButton.addActionListener(this::okButtonActionPerformed);

        ClickableLabel chooseFileLabel = new ClickableLabel(this::selectFromFile);
        chooseFileLabel.setText("Select from File...");

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")),
                new EmptyBorder(10, 0, 10, 0)));

        Box verticalBox = Box.createVerticalBox();
        verticalBox.add(Box.createVerticalGlue());
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.add(chooseFileLabel);
        panel.add(Box.createHorizontalGlue());
        verticalBox.add(panel);
        verticalBox.add(Box.createVerticalGlue());

        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        rightButtonPanel.add(okButton);
        rightButtonPanel.add(cancelButton);
        getRootPane().setDefaultButton(okButton);

        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(verticalBox);
        buttonPanel.add(rightButtonPanel);
        return buttonPanel;
    }

    private static List<Scenario> getScenarioInfos() {
        List<Scenario> scenarios = new ArrayList<>(parseScenariosInDirectory(Configuration.scenariosDir()));

        String userDir = PreferenceManager.getClientPreferences().getUserDir();
        if (!userDir.isBlank()) {
            File subDir = new File(userDir, Configuration.scenariosDir().toString());
            scenarios.addAll(parseScenariosInDirectory(subDir));
        }

        // Add testdata scenarios - these should not be present in releases
        File testdataDir = new File("testresources/data/scenarios/test_setups");
        if (testdataDir.exists() && testdataDir.isDirectory()) {
            scenarios.addAll(parseScenariosInDirectory(testdataDir));
        }
        return scenarios;
    }

    /**
     * Searches the provided directory and all subdirectories for scenario files and
     * returns a list of
     * them.
     *
     * @param directory the directory to parse
     * @return a List of scenarios
     */
    private static List<Scenario> parseScenariosInDirectory(final File directory) {
        logger.info("Parsing scenarios from {}", directory);
        List<Scenario> scenarios = new ArrayList<>();
        for (String scenarioFile : CommonSettingsDialog.filteredFilesWithSubDirs(directory,
                SuiteConstants.SCENARIO_EXT)) {
            try {
                scenarios.add(new ScenarioLoader(scenarioFile).load());
            } catch (Exception ex) {
                logger.warn(ex.getMessage());
            }
        }
        return scenarios;
    }

    /**
     * Groups the given scenarios by the first subdirectory under scenarios they're
     * in (disregards any deeper dirs)
     */
    private Map<String, List<Scenario>> sortScenarios(List<Scenario> scenarioInfos) {
        return scenarioInfos.stream().collect(Collectors.groupingBy(this::getSubDirectory, Collectors.toList()));
    }

    private void selectFromFile(MouseEvent event) {
        JFileChooser fc = new JFileChooser(Configuration.scenariosDir());
        fc.setFileFilter(new FileNameExtensionFilter("Scenario files", "mms"));
        fc.setDialogTitle(Messages.getString("MegaMek.SelectScenarioDialog.title"));
        int returnVal = fc.showOpenDialog(this);
        if ((returnVal == JFileChooser.APPROVE_OPTION) && (fc.getSelectedFile() != null)) {
            scenarioFileName = fc.getSelectedFile().toString();
            okButtonActionPerformed(null);
        }
    }

    /**
     * @return The first subdirectory under the scenarios directory that the
     *         scenario is in; a scenario
     *         in scenarios/tukkayid/secondencounter/ would return "tukkayid". This
     *         is used for grouping
     */
    private String getSubDirectory(Scenario scenarioInfo) {
        String scenariosDir = Configuration.scenariosDir().toString();
        if (!scenarioInfo.getFileName().contains(scenariosDir)) {
            return "";
        } else {
            return subDirUnderScenarios(directoriesAsList(scenarioInfo.getFileName()));
        }
    }

    /**
     * @return The directories (and filename) present in the given full fileName as
     *         a list.
     */
    private static List<String> directoriesAsList(String fileName) {
        Path path = Paths.get(fileName);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < path.getNameCount(); i++) {
            result.add(path.getName(i).toString());
        }
        return result;
    }

    private String subDirUnderScenarios(List<String> directoryList) {
        if (!directoryList.contains("scenarios")) {
            return "";
        } else {
            int index = directoryList.indexOf("scenarios");
            // The next entry must not be the last, as the last entry is the scenario
            // filename
            return (index + 2 < directoryList.size()) ? directoryList.get(index + 1) : "";
        }
    }
}
