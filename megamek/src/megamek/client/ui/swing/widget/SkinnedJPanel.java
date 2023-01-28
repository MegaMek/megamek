package megamek.client.ui.swing.widget;

import megamek.common.Configuration;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SkinnedJPanel extends JPanel {

    private final Image backgroundIcon;

    public SkinnedJPanel(SkinSpecification.UIComponents skinComponent, int backgroundIndex) {
        this(skinComponent.getComp(), backgroundIndex);
    }

    public SkinnedJPanel(String skinComponent, int backgroundIndex) {
        SkinSpecification skinSpec = SkinXMLHandler.getSkin(skinComponent, true);

        if (skinSpec.hasBackgrounds() && (skinSpec.backgrounds.size() > backgroundIndex)) {
            File file = new MegaMekFile(Configuration.widgetsDir(), skinSpec.backgrounds.get(backgroundIndex)).getFile();
            if (file.exists()) {
                backgroundIcon = ImageUtil.loadImageFromFile(file.toString());
            } else {
                LogManager.getLogger().error("MainMenu Error: background icon doesn't exist: "
                        + file.getAbsolutePath());
                backgroundIcon = null;
            }
        } else {
            backgroundIcon = null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (backgroundIcon == null) {
            super.paintComponent(g);
            return;
        }
        int w = getWidth();
        int h = getHeight();
        int iW = backgroundIcon.getWidth(this);
        int iH = backgroundIcon.getHeight(this);
        // If the image isn't loaded, prevent an infinite loop
        if ((iW < 1) || (iH < 1)) {
            return;
        }
        for (int x = 0; x < w; x += iW) {
            for (int y = 0; y < h; y += iH) {
                g.drawImage(backgroundIcon, x, y, null);
            }
        }
    }
}
