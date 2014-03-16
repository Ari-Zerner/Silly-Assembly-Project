package sap.ide;

import java.io.*;
import javax.swing.JTextArea;

/**
 * A text area that allows text to be loaded from or saved to a file.
 * @author Ari Zerner
 */
public class FileTextArea extends JTextArea {
    
    private File lastFile;
    private String saved;

    /**
     * Returns the last file saved to or loaded from.
     * @return the last file used
     */
    public File getLastFile() {
        return lastFile;
    }
    
    /**
     * Checks whether changes to the text are saved.
     * @return true if the text is the same as when it was last saved
     */
    public boolean isSaved() {
        return getText().equals(saved);
    }
    
    /**
     * Saves the contents of this FileTextArea to the specified file.
     * @param file
     */
    public void save(File file) {
        lastFile = file;
        save();
    }
    
    /**
     * Saves the contents of this FileTextArea to the last file used.
     * @see getLastFile()
     */
    public void save() {
        if (lastFile == null) return;
        try {
            new FileOutputStream(lastFile).write(getText().getBytes());
        } catch (IOException exc) {}
        saved = getText();
    }
    
    /**
     * Loads the contents of the specified file into this FileTextArea.
     * @param file
     */
    public void load(File file) {
        lastFile = file;
        load();
    }
    
    /**
     * Loads the contents of this FileTextArea from the last file used.
     * @see getLastFile()
     */
    public void load() {
        if (lastFile == null || !lastFile.exists()) {
            setText("");
            saved = getText();
            return;
        }
        byte[] text = new byte[(int) lastFile.length()];
        try {
            new FileInputStream(lastFile).read(text);
        } catch (IOException exc) {}
        setText(new String(text));
        saved = getText();
    }
}
