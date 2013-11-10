package megamek.client.ui.swing.widget;

import java.util.ArrayList;

/**
 * A class that contains state information that specifies a skin.
 * 
 * @author walczak
 *
 */
public class SkinSpecification {
	
	public String tl_corner;
	public String tr_corner;
	public String bl_corner;
	public String br_corner;
	
	public String top_line;
	public String right_line;
	public String bottom_line;
	public String left_line;
	
	public ArrayList<String> backgrounds;
	
	public SkinSpecification(){
		tl_corner = tr_corner = bl_corner = br_corner = "";
		top_line = right_line = bottom_line = left_line = "";
		backgrounds = new ArrayList<String>();
	}
	
	public SkinSpecification(String tl, String tr, String bl, String br, 
			String top, String right, String bottom, String left){
		tl_corner = tl;
		tr_corner = tr;
		bl_corner = bl;
		br_corner = br;
		
		top_line = top;
		right_line = right;
		bottom_line = bottom;
		left_line = left;
		backgrounds = new ArrayList<String>();
	}

}
