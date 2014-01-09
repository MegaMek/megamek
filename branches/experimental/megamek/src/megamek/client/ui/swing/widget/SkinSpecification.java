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
	
	public ArrayList<String> topEdge;
	public ArrayList<Boolean> topShouldTile;
	public ArrayList<String> rightEdge;
	public ArrayList<Boolean> rightShouldTile;
	public ArrayList<String> bottomEdge;
	public ArrayList<Boolean> bottomShouldTile;
	public ArrayList<String> leftEdge;
	public ArrayList<Boolean> leftShouldTile;
	
	public ArrayList<String> backgrounds;
	
	public SkinSpecification(){
		tl_corner = tr_corner = bl_corner = br_corner = "";
		topEdge = new ArrayList<String>();
		rightEdge = new ArrayList<String>();
		bottomEdge = new ArrayList<String>();
		leftEdge = new ArrayList<String>();
		backgrounds = new ArrayList<String>();
		topShouldTile = new ArrayList<Boolean>();
		rightShouldTile = new ArrayList<Boolean>();
		bottomShouldTile = new ArrayList<Boolean>();
		leftShouldTile = new ArrayList<Boolean>();
	}


}
