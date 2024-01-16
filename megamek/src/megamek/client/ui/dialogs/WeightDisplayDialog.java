package megamek.client.ui.dialogs;

import megamek.client.ui.baseComponents.AbstractDialog;
import megamek.client.ui.swing.calculationReport.FlexibleCalculationReport;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.verifier.TestEntity;
import megamek.common.verifier.TestInfantry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;

public class WeightDisplayDialog extends AbstractDialog {

    private final Entity entity;

    public WeightDisplayDialog(final JFrame frame, final Entity entity) {
        this(frame, false, entity);
    }

    public WeightDisplayDialog(final JFrame frame, final boolean modal, final Entity entity) {
        super(frame, modal, "BVDisplayDialog", "BVDisplayDialog.title");
        this.entity = Objects.requireNonNull(entity);
        initialize();
    }

    @Override
    protected void finalizeInitialization() throws Exception {
        super.finalizeInitialization();
        setTitle(getTitle() + " (" + entity.getShortName() + ")");
        adaptToGUIScale();
        pack();
        Dimension screenSize = UIUtil.getScaledScreenSize(this);
        setSize(new Dimension(getSize().width, Math.min(getHeight(), (int) (screenSize.getHeight() * 0.8))));
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    protected Container createCenterPane() {
        var scrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        String textReport;
        if (entity.isConventionalInfantry()) {
            FlexibleCalculationReport weightReport = new FlexibleCalculationReport();
            TestInfantry.getWeightExact((Infantry) entity, weightReport);
            scrollPane.setViewportView(weightReport.toJComponent());
            textReport = weightReport.getTextReport().toString();
        } else {
            TestEntity testEntity = TestEntity.getEntityVerifier(entity);
            textReport = testEntity.printEntity().toString();
            JTextPane textPane = new JTextPane();
            textPane.setText(textReport);
            textPane.setEditable(false);
            textPane.setCaret(new DefaultCaret());
            scrollPane.setViewportView(textPane);
        }

        JButton exportText = new JButton("Copy as Text");
        exportText.addActionListener(evt -> copyToClipboard(textReport));

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(new EmptyBorder(10, 0, 0, 0));

        Box centerPanel = Box.createVerticalBox();
        centerPanel.setBorder(new EmptyBorder(25, 15, 25, 15));
        JPanel buttonPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));

        buttonPanel.add(exportText);
        centerPanel.add(buttonPanel);
        centerPanel.add(scrollPane);
        return centerPanel;
    }

    private void copyToClipboard(String reportString) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(reportString), null);
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this, UIUtil.FONT_SCALE1);
    }
}