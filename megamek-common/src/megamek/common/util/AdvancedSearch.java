package megamek.common.util;

import megamek.common.util.MechSearchFilter;
import megamek.common.util.MechSearchFilter.BoolOp;





	/**
	 *  JDialog that allows the user to create a unit filter.
	 *
	 * @author Arlith
	 *
	 */
public class AdvancedSearch {
	
		public ParensFT getParensFT(String p){
			return new ParensFT(p);
		}
		
		public EquipmentFT getEquipmentFT(String in, String fn, int q){
			return new EquipmentFT(in,fn,q);
		}
		
		public OperationFT getOperationFT(MechSearchFilter.BoolOp o){
			return new OperationFT(o);
		}

	    /**
	     * Base class for different tokens that can be in a filter expression.
	     *
	     * @author Arlith
	     *
	     */
	    public class FilterTokens{
	    }

	    /**
	     * FilterTokens subclass that represents parenthesis.
	     * @author Arlith
	     *
	     */
	    public class ParensFT extends FilterTokens{
	        public String parens;

	        public ParensFT(String p){
	            parens = p;
	        }

	        @Override
	        public String toString(){
	            return parens;
	        }
	    }

	    /**
	     * FilterTokens subclass that represents equipment.
	     * @author Arlith
	     *
	     */
	    public class EquipmentFT extends FilterTokens{
	        public String internalName;
	        public String fullName;
	        public int qty;

	        public EquipmentFT(String in, String fn, int q){
	            internalName = in;
	            fullName = fn;
	            qty = q;
	        }

	        @Override
	        public String toString(){
	            if (qty == 1) {
	                return qty + " " + fullName;
	            } else {
	                return qty + " " + fullName + "s";
	            }
	        }
	    }

	    /**
	     * FilterTokens subclass that represents a boolean operation.
	     * @author Arlith
	     *
	     */
	    
	    public class OperationFT extends FilterTokens{
	        public BoolOp op;

	        public OperationFT(MechSearchFilter.BoolOp o){
	            op = o;
	        }

	        @Override
	        public String toString(){
	            if (op == MechSearchFilter.BoolOp.AND) {
	                return "And";
	            } else if (op == MechSearchFilter.BoolOp.OR) {
	                return "Or";
	            } else {
	                return "";
	            }
	        }
	    }
	}
