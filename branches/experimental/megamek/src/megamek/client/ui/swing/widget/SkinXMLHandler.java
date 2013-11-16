package megamek.client.ui.swing.widget;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class reads in an XML file that specifies different aspects of the 
 * visual skin for Megamek.
 * 
 * @author walczak
 *
 */
public class SkinXMLHandler {
	
	public static String defaultUIElement = "defaultElement";
	public static String defaultButton = "defaultButton";
	public static String defaultSkinXML = "defaultSkin.xml";
	
	public static String UI_ELEMENT = "UI_Element";
	public static String NAME = "name";
	public static String BORDER = "border";
	public static String TR_CORNER = "corner_top_right";
	public static String TL_CORNER = "corner_top_left";
	public static String BR_CORNER = "corner_bottom_right";
	public static String BL_CORNER = "corner_bottom_left";
	public static String TOP_LINE = "line_top";
	public static String BOTTOM_LINE = "line_bottom";
	public static String RIGHT_LINE = "line_right";
	public static String LEFT_LINE = "line_left";
	public static String BACKGROUND_IMAGE = "background_image";
	
	private static Hashtable<String, SkinSpecification> skinSpecs;
	
	/**
	 * Initializes using the default skin file.
	 * @throws IOException
	 */
    public static void initSkinXMLHandler() throws IOException {
		initSkinXMLHandler(defaultSkinXML);
	}
	
    /**
	 * Initializes using the supplied skin file.
	 * @throws IOException
	 */
	public static void initSkinXMLHandler(String path) throws IOException {
			
        String filePath = System.getProperty("user.dir");
        if (!filePath.endsWith(File.separator)) {
            filePath += File.separator;
        }
        filePath += "mmconf" + File.separator + path;
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return;
        }

        // Build the XML document.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = dbf.newDocumentBuilder();
            System.out.println("Parsing " + filePath);
            Document doc = builder.parse(file);
            System.out.println("Parsing finished.");

            // Get the list of units.
            NodeList listOfComponents = doc.getElementsByTagName(UI_ELEMENT);
            int totalComponents = listOfComponents.getLength();
            skinSpecs = new Hashtable<String, SkinSpecification>(
            		(int)(totalComponents * 1.25));
            for (int comp = 0; comp < totalComponents; comp++) {
            	SkinSpecification skinSpec = new SkinSpecification();
                // Get the first element of this node.
                Element unitList = (Element) listOfComponents.item(comp);

                // Get the border specs
                Element border = (Element) 
                		unitList.getElementsByTagName(BORDER).item(0);
                if (border == null) {
                    System.err.println("Missing <" + BORDER +  
                    		"> tag in element #" + comp);
                    continue;
                }
                
                skinSpec.tr_corner = border.getElementsByTagName(TR_CORNER).
                		item(0).getTextContent();
                skinSpec.tl_corner = border.getElementsByTagName(TL_CORNER).
                		item(0).getTextContent();
                skinSpec.br_corner = border.getElementsByTagName(BR_CORNER).
                		item(0).getTextContent();
                skinSpec.bl_corner = border.getElementsByTagName(BL_CORNER).
                		item(0).getTextContent();
                
                skinSpec.top_line = border.getElementsByTagName(TOP_LINE).
                		item(0).getTextContent();
                skinSpec.bottom_line = border.getElementsByTagName(BOTTOM_LINE).
                		item(0).getTextContent();
                skinSpec.right_line = border.getElementsByTagName(RIGHT_LINE).
                		item(0).getTextContent();
                skinSpec.left_line = border.getElementsByTagName(LEFT_LINE).
                		item(0).getTextContent();

               // Get the border specs
                NodeList backgrounds = 
                		unitList.getElementsByTagName(BACKGROUND_IMAGE);
                if (backgrounds != null){
                	for (int bg = 0; bg < backgrounds.getLength(); bg++){
                		skinSpec.backgrounds.add(
                				backgrounds.item(bg).getTextContent());
                	}
                }
                
                String name = unitList.getElementsByTagName(NAME).
                		item(0).getTextContent();

                skinSpecs.put(name, skinSpec);
            }


        } catch (Exception e) {
            throw new IOException(e);
        }    
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
	public static SkinSpecification getSkin(String component, boolean isBtn){
		if (skinSpecs == null ){
			try {
				initSkinXMLHandler();
			} catch (IOException e){
				System.out.println("Error reading in default skin file!");
				System.out.println("Error: " + e.getMessage());
				return null;
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
