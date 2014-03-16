package sap;

import java.io.*;
import java.util.*;
import static sap.Language.*;

/**
 * Assembles a SAP source file. Assembler is not thread safe.
 * @author Ari Zerner
 */
public class Assembler {

    private int lineNumber, numErrors, prgmStart;
    private String errorMess = "No program assembled.";
    private SymbolTable symTable;
    private Vector<Integer> prgm, prgmBuffer;
    private boolean end, error = true;
    private Vector<String> duplicates;
    /**
     * Convenience field to eliminate the need to instantiate a new Assembler.
     * If this is used by more than one thread
     */
    public static final Assembler ASM = new Assembler();
    
    /**
     * Checks whether there was an error in the last assembly.
     * @return the same as the last call to assembleFile
     */
    public boolean wasError() {
        return error;
    }

    /**
     * Returns the number of errors from the last assembly.
     * @return the number of errors
     */
    public int getNumErrors() {
        return numErrors;
    }

    /**
     * Returns the error message from the last assembly.
     * @return the error message
     */
    public String getErrorMess() {
        return errorMess;
    }

    /**
     * Assembles a SAP source file. Generates three files: a .bin file with the
     * assembled code, a .lst file with a listing of the assembly, and a .sym
     * file with the symbol table. The source code should be in a .txt file.
     * Equivalent to calling assembleFile(fn, System.out).
     * @param fn the name of the .txt file
     * @return true if and only if there were no errors
     * @see assembleFile(String fn, PrintStream out)
     */
    public boolean assembleFile(String fn) {
        return assembleFile(fn, System.out);
    }

    /**
     * Assembles a SAP source file. Generates three files: a .bin file with the
     * assembled code, a .lst file with a listing of the assembly, and a .sym
     * file with the symbol table. The source code should be in a .txt file.
     * @param fn the name of the .txt file
     * @param out the PrintStream to which to display messages. Can be set to
     * null to signify that messages shouldn't be printed
     * @return true if and only if a bin file was created (there were no errors)
     */
    public boolean assembleFile(String fn, PrintStream out) {
        if (out == null) {
            out = new PrintStream(new OutputStream() {

                @Override
                public void write(int i) throws IOException {
                }
            });
        }
        Scanner txt = null, txt2 = null;
        errorMess = "No errors.";
        try {
            txt = new Scanner(new FileInputStream(fn + ".txt"));
            txt2 = new Scanner(new FileInputStream(fn + ".txt"));
        } catch (FileNotFoundException exc) {
            errorMess = "Error finding or reading " + fn + ".txt";
            out.println(errorMess);
            error = true;
            return false;
        }
        String listing = "";
        symTable = new SymbolTable();
        duplicates = new Vector<String>();
        lineNumber = 0;
        numErrors = 0;
        prgm = new Vector<Integer>();
        end = false;
        if (txt.hasNext()) { // account for empty file
            while (!end && txt.hasNextLine()) {
                assembleForLabel(txt.nextLine());
            }
            txt.close();
            if (!duplicates.isEmpty()) {
                listing += "Error! Duplicate label";
                if (duplicates.size() > 1) {
                    listing += "s";
                }
                listing += ": ";
                Iterator<String> dups = duplicates.iterator();
                while (dups.hasNext()) {
                    listing += dups.next();
                    if (dups.hasNext()) {
                        listing += ", ";
                    }
                }
                listing += "\n\n";
            }
            lineNumber = 0;
            prgm = new Vector<Integer>();
            end = false;
            try {
                while (!end) {
                    listing += assembleLine(txt2.nextLine());
                }
            } catch (NoSuchElementException exc) {
                listing += assembleLine(".end ;implicit");
            }
            txt2.close();
            listing += "\nSymbol Table:\n";
            listing += symTable.asAlphabeticalList();
            listing += "\nNumber Assembly Errors: " + numErrors;
        }
        out.println(listing);
        boolean isBadFile = false;
        PrintStream lst, sym, bin = null;
        try {
            lst = new PrintStream(new FileOutputStream(fn + ".lst"));
            lst.print(listing);
            lst.close();
        } catch (FileNotFoundException exc) {
            isBadFile = true;
        } finally {
            try {
                sym = new PrintStream(new FileOutputStream(fn + ".sym"));
                sym.print(symTable.asAlphabeticalList());
                sym.close();
            } catch (FileNotFoundException exc) {
                isBadFile = true;
            } finally {
                try {
                    if (numErrors == 0) {
                        bin = new PrintStream(
                                new FileOutputStream(fn + ".bin"));
                        bin.println(prgm.size());
                        bin.println(prgmStart);
                        Iterator code = prgm.iterator();
                        while (code.hasNext()) {
                            bin.println(code.next());
                        }
                        bin.close();
                    }
                } catch (FileNotFoundException exc) {
                    isBadFile = true;
                }
            }
        }
        if (isBadFile) {
            errorMess = "Error creating one or more assembled files.";
            out.println(errorMess);
            error = true;
            return false;
        }
        if (numErrors == 0) {
            error = false;
            return true;
        }
        errorMess = numErrors + " error" + (numErrors == 1 ? "" : "s")
                + " in program. See listing for details.";
        error = true;
        return false;
    }

    private void assembleForLabel(String line) {
        line = line.trim();
        if (line.isEmpty()) {
            return;
        }
        prgmBuffer = new Vector<Integer>(4);
        try {
            line = removeComment(line);
            line = extractLabel(line);
            parseCommand(line, true);
            prgm.addAll(prgmBuffer);
        } catch (Exception exc) {
        }
        lineNumber++;
    }

    /**
     * Assembles a line of code.
     * @param line the line of code
     * @return what to add to the listing file
     */
    private String assembleLine(String line) {
        line = line.trim();
        if (line.isEmpty()) {
            return lineNumber++ + "\n";
        }
        String lstBuffer = lineNumber++ + " ";
        String fullLine = line;
        prgmBuffer = new Vector<Integer>(4);
        try {
            line = removeComment(line);
            line = removeLabel(line);
            parseCommand(line, false);
            lstBuffer += prgm.size() + ": ";
            Iterator prgmAdd = prgmBuffer.iterator();
            while (prgmAdd.hasNext()) {
                lstBuffer += " " + prgmAdd.next();
            }
            prgm.addAll(prgmBuffer);
        } catch (Exception exc) {
            lstBuffer += "Error: " + exc.getMessage();
            numErrors++;
        }
        int lstLength = 36; // the amount of space before the line
        if (lstBuffer.length() > lstLength) {
            lstBuffer = lstBuffer.substring(0, lstLength);
            lstBuffer = lstBuffer.substring(0, lstBuffer.lastIndexOf(' '));
        }
        while (lstBuffer.length() < lstLength) {
            lstBuffer += " ";
        }
        lstBuffer += fullLine;
        return lstBuffer + '\n';
    }

    private String removeComment(String line) {
        int commentStart = line.indexOf(';');
        if (commentStart > -1) {
            return line.substring(0, commentStart);
        }
        return line;
    }

    private String removeLabel(String line) {
        int labelEnd = line.indexOf(':');
        if (labelEnd > -1) {
            try {
                return line.substring(labelEnd + 1);
            } catch (StringIndexOutOfBoundsException exc) {
                throw new RuntimeException("Blank label");
            }
        }
        return line;
    }

    private String extractLabel(String line) {
        int labelEnd = line.indexOf(':');
        if (labelEnd > -1) {
            String label = line.substring(0, labelEnd);
            if (symTable.addSymbol(label, prgm.size())) {
                duplicates.add(label.trim().toLowerCase()
                        + " in line " + lineNumber);
                numErrors++;
            }
            return line.substring(labelEnd + 1);
        }
        return line;
    }

    private void parseCommand(String line, boolean ignoreLabels) {
        line = line.trim();
        if (line.isEmpty()) {
            return;
        }
        Scanner scan = new Scanner(line);
        String command = scan.next().toLowerCase();
        try {
            Integer byteCode = getByteCode(command);
            if (byteCode != null) {
                prgmBuffer.add(byteCode);
                for (int pt : getParamTypes(command)) {
                    switch (pt) {
                        case REGISTER:
                            prgmBuffer.add(parseRegister(scan.next()));
                            break;
                        case IMMEDIATE:
                            prgmBuffer.add(parseImmediate(scan.next()));
                            break;
                        case LABEL:
                            if (ignoreLabels) {
                                prgmBuffer.add(0);
                                scan.next();
                            } else {
                                prgmBuffer.add(parseLabel(scan.next()));
                            }
                            break;
                        case INDIRECT:
                            prgmBuffer.add(parseIndirect(scan.next()));
                            break;
                    }
                }
            } else if (command.equals(".start")) {
                if (!ignoreLabels) {
                    prgmStart = parseLabel(scan.next());
                }
            } else if (command.equals(".end")) {
                end = true;
            } else if (command.equals(".integer")) {
                prgmBuffer.add(parseImmediate(scan.next()));
            } else if (command.equals(".allocate")) {
                int allocSize = parseImmediate(scan.next());
                prgmBuffer.ensureCapacity(allocSize);
                for (int i = 0; i < allocSize; i++) {
                    prgmBuffer.add(0);
                }
            } else if (command.equals(".string")) {
                String str = parseString(scan.nextLine());
                prgmBuffer.add(str.length());
                for (int i = 0; i < str.length(); i++) {
                    prgmBuffer.add((int) str.charAt(i));
                }
            } else {
                throw new RuntimeException("Unrecognized command.");
            }
        } catch (NoSuchElementException exc) {
            throw new RuntimeException("Not enough arguments.");
        }
        if (scan.hasNext()) {
            throw new RuntimeException("Too many arguments.");
        }
    }

    private int parseRegister(String param) {
        try {
            if (!param.startsWith("r")) {
                throw new Exception();
            }
            return Integer.parseInt(param.substring(1));
        } catch (Exception exc) {
            throw new RuntimeException("Bad argument (" + param + ").");
        }
    }

    private int parseImmediate(String param) {
        try {
            if (!param.startsWith("#")) {
                throw new Exception();
            }
            return Integer.parseInt(param.substring(1));
        } catch (Exception exc) {
            throw new RuntimeException("Bad argument (" + param + ").");
        }
    }

    private int parseLabel(String param) {
        try {
            return symTable.getAddress(param);
        } catch (Exception exc) {
            throw new RuntimeException("Unrecognized label.");
        }
    }

    private int parseIndirect(String param) {
        try {
            if (!param.startsWith("r")) {
                throw new Exception();
            }
            return Integer.parseInt(param.substring(1));
        } catch (Exception exc) {
            throw new RuntimeException("Bad argument (" + param + ").");
        }
    }

    private String parseString(String param) {
        param = param.trim();
        if (!(param.startsWith("\"") && param.endsWith("\""))) {
            throw new RuntimeException("String must be in quotes.");
        }
        return param.substring(1, param.length() - 1);
    }
}