package megamek.client.ui.swing.util;

import javax.imageio.ImageIO;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.ImageView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.Dictionary;
import java.util.Hashtable;

public class BASE64ImageView extends ImageView {

    private URL url;

    public BASE64ImageView(Element elmnt) {
        super(elmnt);
        populateImage();
    }

    @SuppressWarnings("unchecked")
    private void populateImage() {
        Dictionary<URL, Image> cache = (Dictionary<URL, Image>) getDocument()
                .getProperty("imageCache");
        if (cache == null) {
            cache = new Hashtable<>();
            getDocument().putProperty("imageCache", cache);
        }

        URL src = getImageURL();
        cache.put(src, loadImage());

    }

    private Image loadImage() {
        String b64 = getBASE64Image();
        BufferedImage newImage = null;
        ByteArrayInputStream bais = null;
        try {
            bais = new ByteArrayInputStream(
                    Base64.getDecoder().decode(b64.getBytes()));
            newImage = ImageIO.read(bais);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return newImage;
    }

    @Override
    public URL getImageURL() {
        String src = (String) getElement().getAttributes()
                .getAttribute(HTML.Attribute.SRC);
        if (isBase64Encoded(src)) {

            try {
                this.url = new URL(BASE64ImageView.class.getProtectionDomain()
                        .getCodeSource().getLocation().toString() + "/" + this.getElement().toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            return this.url;
        }
        return super.getImageURL();
    }

    private boolean isBase64Encoded(String src) {
        return src != null && src.contains("base64,");
    }

    private String getBASE64Image() {
        String src = (String) getElement().getAttributes()
                .getAttribute(HTML.Attribute.SRC);
        if (!isBase64Encoded(src)) {
            return null;
        }
        return src.substring(src.indexOf("base64,") + 7);
    }

}