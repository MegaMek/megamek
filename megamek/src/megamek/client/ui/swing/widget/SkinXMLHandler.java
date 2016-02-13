package megamek.client.ui.swing.widget;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Configuration;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class reads in an XML file that specifies different aspects of the 
 * visual skin for Megamek.
 * 
 * @author arlith
 *
 */
public class SkinXMLHandler {
    
    /**
     * The XML name tag value for the default component border
     */
    public static String defaultUIElement = "defaultElement";
    
    /**
     * The XML name tag value for the default component border
     */
    public static String defaultButton = "defaultButton";
    
    /**
     * The XML name tag value for the BoardView border
     */
    public static String BOARDVIEW = "BoardViewBorder";
    
    /**
     * The XML name tag value for the PhaseDisplay border
     */
    public static String PHASEDISPLAY = "PhaseDisplayBorder";
    
    /**
     * The file name for the default Skin XML file, found in the config dir.
     */
    public static String defaultSkinXML = "defaultSkin.xml";
    
    public static String UI_ELEMENT = "UI_Element";
    public static String NAME = "name";
    public static String FONT_COLOR = "font_color";
    public static String BORDER = "border";
    public static String PLAIN = "plain";
    public static String NO_BORDER = "no_border";
    public static String TILE_BACKGROUND = "tile_background";
    public static String TR_CORNER = "corner_top_right";
    public static String TL_CORNER = "corner_top_left";
    public static String BR_CORNER = "corner_bottom_right";
    public static String BL_CORNER = "corner_bottom_left";
    public static String EDGE = "edge";
    public static String EDGE_NAME = "edgeName";
    public static String EDGE_ICON = "edgeIcon";
    public static String ICON = "icon";
    public static String TILED = "tiled";
    public static String TOP_LINE = "line_top";
    public static String BOTTOM_LINE = "line_bottom";
    public static String RIGHT_LINE = "line_right";
    public static String LEFT_LINE = "line_left";
    public static String BACKGROUND_IMAGE = "background_image";
    public static String SHOW_SCROLL_BARS = "show_scroll_bars";
    
    private static Hashtable<String, SkinSpecification> skinSpecs;
    
    /**
     * Checks whether the given path points to a file that is a valid skin
     * specification.
     * 
     * @param path
     * @return
     */
    public static boolean validSkinSpecFile(String fileName) {
        File file = new File(Configuration.configDir(), fileName);
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(file);
            // TODO: Just validate against the XSD
            // Until that's done, just assume anything with UI_ELEMENT tags is
            //  valid
            NodeList listOfComponents = doc.getElementsByTagName(UI_ELEMENT);
            if (listOfComponents.getLength() > 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Initializes using the default skin file.
     * @throws IOException
     */
    public static boolean initSkinXMLHandler() {
        String skinFilePath = GUIPreferences.getInstance().getSkinFile();
        return initSkinXMLHandler(skinFilePath);
    }
    
    /**
     * Initializes using the supplied skin file.
     * @throws IOException
     */
    public synchronized static boolean initSkinXMLHandler(String fileName) {

        if (fileName == null) {
            System.out.println("ERROR: Bad skin specification file: " +
                    "null filename!");
            return false;
        }
        
        File file = new File(Configuration.configDir(), fileName);
        if (!file.exists() || !file.isFile()) {
            System.out.println("ERROR: Bad skin specification file: " +
                    "file doesn't exist!  File name: " + fileName);
            return false;
        }

        // Build the XML document.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = dbf.newDocumentBuilder();
            System.out.println("Parsing " + file.getName());
            Document doc = builder.parse(file);
            System.out.println("Parsing finished.");

            // Get the list of units.
            NodeList listOfComponents = doc.getElementsByTagName(UI_ELEMENT);
            int totalComponents = listOfComponents.getLength();
            skinSpecs = new Hashtable<String, SkinSpecification>(
                    (int)(totalComponents * 1.25));
            for (int comp = 0; comp < totalComponents; comp++) {
                SkinSpecification skinSpec;
                Element borderList = (Element) listOfComponents.item(comp);
                
                // Parse no border
                Element noBorderEle = (Element) borderList
                        .getElementsByTagName(NO_BORDER).item(0);
                boolean noBorder = false;
                if (noBorderEle != null) {
                    noBorder = Boolean
                            .parseBoolean(noBorderEle.getTextContent());
                }
                
                // Get the first element of this node.
                Element plainTag = (Element) 
                        borderList.getElementsByTagName(PLAIN).item(0);
                // If there is no plain tag, load the icons
                if (plainTag == null && !noBorder) {
                    // Get the border specs
                    Element border = (Element) 
                            borderList.getElementsByTagName(BORDER).item(0);
                    if (border == null) {
                        System.err.println("Missing <" + BORDER +  
                                "> tag in element #" + comp);
                        continue;
                    }
                    
                    skinSpec = parseBorderTag(border);
                } else { // Plain skin, no icons
                    skinSpec = new SkinSpecification();
                    skinSpec.noBorder = noBorder;
                }
                
                // Get the background specs
                if (plainTag == null) {
                    NodeList backgrounds = 
                            borderList.getElementsByTagName(BACKGROUND_IMAGE);
                    if (backgrounds != null){
                        for (int bg = 0; bg < backgrounds.getLength(); bg++){
                            skinSpec.backgrounds.add(
                                    backgrounds.item(bg).getTextContent());
                        }
                    }
                }
                
                // Pase show scroll bars
                Element showScrollEle = (Element) borderList
                        .getElementsByTagName(SHOW_SCROLL_BARS).item(0);
                if (showScrollEle != null) {
                    skinSpec.showScrollBars = Boolean
                            .parseBoolean(showScrollEle.getTextContent());
                }
                
                // Parse font color
                Element fontColorEle = (Element) 
                        borderList.getElementsByTagName(FONT_COLOR).item(0);
                if (fontColorEle != null) {
                    String fontColorContent = fontColorEle.getTextContent();
                    skinSpec.fontColor = Color.decode(fontColorContent);
                }                  
                
                // Parse tile background
                Element tileBGEle = (Element) borderList
                        .getElementsByTagName(TILE_BACKGROUND).item(0);
                if (tileBGEle != null) {
                    skinSpec.tileBackground = Boolean
                            .parseBoolean(tileBGEle.getTextContent());
                }
                
                String name = borderList.getElementsByTagName(NAME).
                        item(0).getTextContent();

                skinSpecs.put(name, skinSpec);
            }
            
            if (!skinSpecs.containsKey(defaultUIElement) 
                    || !skinSpecs.containsKey(defaultButton)) {
                System.out.println("ERROR: Bad skin specification file: " +
                        "file doesn't specify " + defaultUIElement + 
                        " or " + defaultButton + "!");
                return false;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    /**
     *  Create a new SkinSpecification and populate it from the supplied border
     *  tag
     *  
     * @param border
     * @return
     */
    private static SkinSpecification parseBorderTag(Element border){
        SkinSpecification skinSpec = new SkinSpecification();
        
        // Parse Corner Icons
        skinSpec.tr_corner = border.getElementsByTagName(TR_CORNER).
                item(0).getTextContent();
        skinSpec.tl_corner = border.getElementsByTagName(TL_CORNER).
                item(0).getTextContent();
        skinSpec.br_corner = border.getElementsByTagName(BR_CORNER).
                item(0).getTextContent();
        skinSpec.bl_corner = border.getElementsByTagName(BL_CORNER).
                item(0).getTextContent();
        
        // Parse Edge Icons from Edge tag
        NodeList edgeNodes = border.getElementsByTagName(EDGE);
        for (int i = 0; i < edgeNodes.getLength(); i++){
            // An edge tag will have some number of icons
            ArrayList<String> icons = new ArrayList<String>();
            // And icon can be tiled or static
            ArrayList<Boolean> shouldTile = new ArrayList<Boolean>(); 
            NodeList edgeIcons = ((Element) edgeNodes.item(i))
                    .getElementsByTagName(EDGE_ICON);
            // Iterate through each icon/tiled pair
            for (int j = 0; j < edgeIcons.getLength(); j++){
                String icon = ((Element) edgeIcons.item(j))
                        .getElementsByTagName(ICON).item(0).getTextContent();
                String tiled = ((Element) edgeIcons.item(j))
                        .getElementsByTagName(TILED).item(0).getTextContent();
                
                if (icon == null){
                    System.err.println("Missing <" + ICON + "> tag");
                    continue;
                }
                if (tiled == null){
                    System.err.println("Missing <" + TILED + "> tag");
                    continue;
                }
                icons.add(icon);
                shouldTile.add(tiled.equalsIgnoreCase("true"));
            }
            
            String edgeName = ((Element) edgeNodes.item(i))
                    .getElementsByTagName(EDGE_NAME).item(0).getTextContent();
            
            if (edgeName == null){
                System.err.println("Missing <" + EDGE_NAME + "> tag");
                continue;
            }
            
            if (edgeName.equals("top")){
                skinSpec.topEdge = icons;
                skinSpec.topShouldTile = shouldTile;
            } else if (edgeName.equals("bottom")){
                skinSpec.bottomEdge = icons;
                skinSpec.bottomShouldTile = shouldTile;
            } else if (edgeName.equals("left")){
                skinSpec.leftEdge = icons;
                skinSpec.leftShouldTile = shouldTile;
            } else if (edgeName.equals("right")){
                skinSpec.rightEdge = icons;
                skinSpec.rightShouldTile = shouldTile;
            }
        }        
        return skinSpec;        
    }
    
    public static SkinSpecification getSkin(String component){
        return getSkin(component,false);
    }
    
    /**
     * Get a <code>SkinSpecification</code> for a given component.
     * 
     * @param component  The name of the component to get skin info for.
     * @return           
     */
    public synchronized static SkinSpecification getSkin(String component, 
            boolean isBtn){
        if (skinSpecs == null ){
            boolean rv = initSkinXMLHandler();
            if (!rv) {
                // This will return a blank skin spec file, which will act like
                // a plain skin.
                return new SkinSpecification();
            }
        }
        
        SkinSpecification spec = skinSpecs.get(component);
        if (spec == null){
            if (isBtn){
                spec = skinSpecs.get(defaultButton);
            } else {
                spec = skinSpecs.get(defaultUIElement);
            }
        }
        return spec;
    }

}
