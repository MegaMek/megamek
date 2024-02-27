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

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.swing.ButtonEsc;
import megamek.client.ui.swing.CloseAction;
import megamek.client.ui.swing.CommonSettingsDialog;
import megamek.client.ui.swing.dialog.DialogButton;
import megamek.client.ui.swing.util.ClickableLabel;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceManager;
import megamek.common.scenario.ScenarioFullInfo;
import megamek.common.scenario.ScenarioLoader;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This dialog lists all scenarios found in MM's scenario directory as well as the corresponding user directory.
 * The scenarions are grouped by subdirectory (only the one directly below scenarios, further subdirectories
 * are scanned for scenarios but not used for grouping). After closing, a chosen scenario can be retrieved
 * by calling {@link #getSelectedScenarioFilename()}. As a fallback, the dialog also allows choosing a
 * scenario by file.
 */
public class ScenarioChooser extends AbstractButtonDialog {

    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final List<ScenarioFullInfo> scenarioInfoList = getScenarioInfos();
    private final Map<String, List<ScenarioFullInfo>> sortedScenarios = sortScenarios(scenarioInfoList);
    private String scenarioFileName;

    public ScenarioChooser(final JFrame parentFrame) {
        super(parentFrame, "ScenarioChooser", "ScenarioChooser.title");
        initialize();
        setMinimumSize(UIUtil.scaleForGUI(ScenarioInfoPanel.BASE_MINIMUM_WIDTH, ScenarioInfoPanel.BASE_MINIMUM_HEIGHT * 3));
    }

    /**
     * @return the selected preset, or null if the dialog was cancelled or no preset was selected
     */
    public @Nullable String getSelectedScenarioFilename() {
        if (scenarioFileName != null) {
            return scenarioFileName;
        }
        Component selectedTab = tabbedPane.getSelectedComponent();
        if (!(selectedTab instanceof ScenarioInfoPane) || !getResult().isConfirmed()) {
            return null;
        } else {
            return ((ScenarioInfoPane) selectedTab).getSelectedPreset().getFileName();
        }
    }

    @Override
    protected Container createCenterPane() {
        ScenarioInfoPane basicPane = new ScenarioInfoPane(sortedScenarios.get(""));
        tabbedPane.addTab("Basic", basicPane);

        for (String directory : sortedScenarios.keySet()) {
            if (!sortedScenarios.get(directory).isEmpty()) {
                ScenarioInfoPane pane = new ScenarioInfoPane(sortedScenarios.get(directory));
                tabbedPane.addTab(directory, pane);
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
                new UIUtil.ScaledEmptyBorder(10, 0, 10, 0)));

        Box verticalBox = Box.createVerticalBox();
        verticalBox.add(Box.createVerticalGlue());
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
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

    private static List<ScenarioFullInfo> getScenarioInfos() {
        List<ScenarioFullInfo> scenarios = new ArrayList<>(parseScenariosInDirectory(Configuration.scenariosDir()));

        String userDir = PreferenceManager.getClientPreferences().getUserDir();
        if (!userDir.isBlank()) {
            File subDir = new File(userDir, Configuration.scenariosDir().toString());
            parseScenariosInDirectory(subDir);
        }
        return scenarios;
    }

    /**
     * Searches the provided directory and all subdirectories and registers any truetype
     * fonts from .ttf files it finds.
     *
     * @param directory the directory to parse
     */
    public static List<ScenarioFullInfo> parseScenariosInDirectory(final File directory) {
        LogManager.getLogger().info("Parsing scenarios from " + directory);
        List<ScenarioFullInfo> scenarios = new ArrayList<>();
        for (String scenarioFile : CommonSettingsDialog.filteredFilesWithSubDirs(directory, MMConstants.SCENARIO_EXT)) {
            try {
                ScenarioFullInfo scenario = new ScenarioLoader(new File(scenarioFile)).load();
                scenarios.add(scenario);
            } catch (Exception ex) {
                LogManager.getLogger().error("Failed to parse scenario " + scenarioFile, ex);
            }
        }
        return scenarios;
    }

    private Map<String, List<ScenarioFullInfo>> sortScenarios(List<ScenarioFullInfo> scenarioInfos) {
        return scenarioInfos.stream().collect(Collectors.groupingBy(ScenarioFullInfo::getSubDirectory, Collectors.toList()));
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            UIUtil.adjustDialog(this, UIUtil.FONT_SCALE1);
        }
        super.setVisible(b);
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
}