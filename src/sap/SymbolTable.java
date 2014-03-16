package sap;

import java.util.Arrays;
import java.util.Hashtable;

/**
 * Stores a table of symbols with constant-time lookup in either direction.
 * @author Ari Zerner
 */
public class SymbolTable {
    private Hashtable<String, Integer> addresses =
            new Hashtable<String, Integer>();
    private Hashtable<Integer, String> labels =
            new Hashtable<Integer, String>();
    
    /**
     * Removes all symbols from this SymbolTable.
     */
    public void clear() {
        addresses.clear();
        labels.clear();
    }
    
    /**
     * Adds a symbol with the given label and address. Converts the label to
     * lowercase.
     * @param label
     * @param address
     * @return true if the symbol replaced a previous symbol with the same label
     */
    public boolean addSymbol(String label, Integer address) {
        label = label.trim().toLowerCase();
        labels.put(address, label);
        return addresses.put(label, address) != null;
    }
    
    /**
     * Gets the address of a symbol.
     * @param label
     * @return the address of the symbol with the given label, or null if there
     * is no such symbol.
     */
    public Integer getAddress(String label) {
        label = label.trim().toLowerCase();
        return addresses.get(label);
    }
    
    /**
     * Gets the label of a symbol.
     * @param address
     * @return the label of the symbol with the given label, or null if there
     * is no such symbol.
     */
    public String getLabel(Integer address) {
        return labels.get(address);
    }
    
    /**
     * Returns an alphabetically sorted list of the symbols in this table.
     * Each symbol is on its own line in the format "symbol address".
     * @return an alphabetically sorted list of the symbols in this table
     */
    public String asAlphabeticalList() {
        String[] labelArray =
                labels.values().toArray(new String[labels.size()]);
        Arrays.sort(labelArray);
        String list = "";
        for (String label: labelArray)
            list += label + " " + getAddress(label) + "\n";
        return list;
    }
    
    /**
     * Returns a numerically sorted list of the symbols in this table.
     * Each symbol is on its own line in the format "symbol address".
     * @return a numerically sorted list of the symbols in this table
     */
    public String asNumericalList() {
        Integer[] addressArray =
                addresses.values().toArray(new Integer[labels.size()]);
        Arrays.sort(addressArray);
        String list = "";
        for (Integer address: addressArray)
            list += getLabel(address) + " " + address + "\n";
        return list;
    }
}
