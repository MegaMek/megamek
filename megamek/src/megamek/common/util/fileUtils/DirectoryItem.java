package megamek.common.util.fileUtils;

/** 
 * A helper class to hold files in directories taken from 
 * a DirectoryItems object.
 * where both the filename and the directory are stored.
 * @author Juliez
 */
public class DirectoryItem {
    
    /** The category aka file directory of the item */
    private final String category;
    /** The filename of the item */
    private final String item;
    
    public DirectoryItem(String cat, String name) {
        category = cat;
        item = name;
    }
    
    /** Returns the filename of the item. */
    public String getItem() {
        return item;
    }
    
    /** Returns the category aka directory of the item. */
    public String getCategory() {
        return category;
    }

    @Override public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof DirectoryItem) {
            DirectoryItem dOther = (DirectoryItem)other;
            return dOther.getCategory().equals(category) && dOther.getItem().equals(item);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return (category + item).hashCode();
    }
}
