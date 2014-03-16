package sap;

import java.util.*;

/**
 * Has methods for parsing the SAP language.
 * @author Ari Zerner
 */
public class Language {
    private static final Hashtable<String, Integer> byteCodes;
    private static final Hashtable<Integer, String> commands;
    private static final Hashtable<String, int[]> paramTypes;
    
    /**
     * One of the integers representing the parameter types for use with the
     * getParamTypes method.
     * @see getParamTypes(String command)
     */
    public static final int REGISTER = 1, IMMEDIATE = 2, LABEL = 3,
            INDIRECT = 4;
    
    static {
        int size = 58;
        byteCodes = new Hashtable<String, Integer>(size, 1);
        byteCodes.put("halt", 0);
        byteCodes.put("clrr", 1);
        byteCodes.put("clrx", 2);
        byteCodes.put("clrm", 3);
        byteCodes.put("clrb", 4);
        byteCodes.put("movir", 5);
        byteCodes.put("movrr", 6);
        byteCodes.put("movrm", 7);
        byteCodes.put("movmr", 8);
        byteCodes.put("movxr", 9);
        byteCodes.put("movar", 10);
        byteCodes.put("movb", 11);
        byteCodes.put("addir", 12);
        byteCodes.put("addrr", 13);
        byteCodes.put("addmr", 14);
        byteCodes.put("addxr", 15);
        byteCodes.put("subir", 16);
        byteCodes.put("subrr", 17);
        byteCodes.put("submr", 18);
        byteCodes.put("subxr", 19);
        byteCodes.put("mulir", 20);
        byteCodes.put("mulrr", 21);
        byteCodes.put("mulmr", 22);
        byteCodes.put("mulxr", 23);
        byteCodes.put("divir", 24);
        byteCodes.put("divrr", 25);
        byteCodes.put("divmr", 26);
        byteCodes.put("divxr", 27);
        byteCodes.put("jmp", 28);
        byteCodes.put("sojz", 29);
        byteCodes.put("sojnz", 30);
        byteCodes.put("aojz", 31);
        byteCodes.put("aojnz", 32);
        byteCodes.put("cmpir", 33);
        byteCodes.put("cmprr", 34);
        byteCodes.put("cmpmr", 35);
        byteCodes.put("jmpn", 36);
        byteCodes.put("jmpz", 37);
        byteCodes.put("jmpp", 38);
        byteCodes.put("jsr", 39);
        byteCodes.put("ret", 40);
        byteCodes.put("push", 41);
        byteCodes.put("pop", 42);
        byteCodes.put("stackc", 43);
        byteCodes.put("outci", 44);
        byteCodes.put("outcr", 45);
        byteCodes.put("outcx", 46);
        byteCodes.put("outcb", 47);
        byteCodes.put("readi", 48);
        byteCodes.put("printi", 49);
        byteCodes.put("readc", 50);
        byteCodes.put("readln", 51);
        byteCodes.put("brk", 52);
        byteCodes.put("movrx", 53);
        byteCodes.put("movxx", 54);
        byteCodes.put("outs", 55);
        byteCodes.put("nop", 56);
        byteCodes.put("jmpne", 57);
        commands = new Hashtable<Integer, String>(size, 1);
        Iterator<Map.Entry<String, Integer>> iter =
                byteCodes.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Integer> entry = iter.next();
            commands.put(entry.getValue(), entry.getKey());
        }
        paramTypes = new Hashtable<String, int[]>(size,1);
        paramTypes.put("halt", new int[] {});
        paramTypes.put("clrr", new int[] {REGISTER});
        paramTypes.put("clrx", new int[] {INDIRECT});
        paramTypes.put("clrm", new int[] {LABEL});
        paramTypes.put("clrb", new int[] {REGISTER, REGISTER});
        paramTypes.put("movir", new int[] {IMMEDIATE, REGISTER});
        paramTypes.put("movrr", new int[] {REGISTER, REGISTER});
        paramTypes.put("movrm", new int[] {REGISTER, LABEL});
        paramTypes.put("movmr", new int[] {LABEL, REGISTER});
        paramTypes.put("movxr", new int[] {INDIRECT, REGISTER});
        paramTypes.put("movar", new int[] {LABEL, REGISTER});
        paramTypes.put("movb", new int[] {REGISTER, REGISTER, REGISTER});
        paramTypes.put("addir", new int[] {IMMEDIATE, REGISTER});
        paramTypes.put("addrr", new int[] {REGISTER, REGISTER});
        paramTypes.put("addmr", new int[] {LABEL, REGISTER});
        paramTypes.put("addxr", new int[] {INDIRECT, REGISTER});
        paramTypes.put("subir", new int[] {IMMEDIATE, REGISTER});
        paramTypes.put("subrr", new int[] {REGISTER, REGISTER});
        paramTypes.put("submr", new int[] {LABEL, REGISTER});
        paramTypes.put("subxr", new int[] {INDIRECT, REGISTER});
        paramTypes.put("mulir", new int[] {IMMEDIATE, REGISTER});
        paramTypes.put("mulrr", new int[] {REGISTER, REGISTER});
        paramTypes.put("mulmr", new int[] {LABEL, REGISTER});
        paramTypes.put("mulxr", new int[] {INDIRECT, REGISTER});
        paramTypes.put("divir", new int[] {IMMEDIATE, REGISTER});
        paramTypes.put("divrr", new int[] {REGISTER, REGISTER});
        paramTypes.put("divmr", new int[] {LABEL, REGISTER});
        paramTypes.put("divxr", new int[] {INDIRECT, REGISTER});
        paramTypes.put("jmp", new int[] {LABEL});
        paramTypes.put("sojz", new int[] {REGISTER, LABEL});
        paramTypes.put("sojnz", new int[] {REGISTER, LABEL});
        paramTypes.put("aojz", new int[] {REGISTER, LABEL});
        paramTypes.put("aojnz", new int[] {REGISTER, LABEL});
        paramTypes.put("cmpir", new int[] {IMMEDIATE, REGISTER});
        paramTypes.put("cmprr", new int[] {REGISTER, REGISTER});
        paramTypes.put("cmpmr", new int[] {LABEL, REGISTER});
        paramTypes.put("jmpn", new int[] {LABEL});
        paramTypes.put("jmpz", new int[] {LABEL});
        paramTypes.put("jmpp", new int[] {LABEL});
        paramTypes.put("jsr", new int[] {LABEL});
        paramTypes.put("ret", new int[] {});
        paramTypes.put("push", new int[] {REGISTER});
        paramTypes.put("pop", new int[] {REGISTER});
        paramTypes.put("stackc", new int[] {REGISTER});
        paramTypes.put("outci", new int[] {IMMEDIATE});
        paramTypes.put("outcr", new int[] {REGISTER});
        paramTypes.put("outcx", new int[] {INDIRECT});
        paramTypes.put("outcb", new int[] {REGISTER, REGISTER});
        paramTypes.put("readi", new int[] {REGISTER, REGISTER});
        paramTypes.put("printi", new int[] {REGISTER});
        paramTypes.put("readc", new int[] {REGISTER});
        paramTypes.put("readln", new int[] {LABEL, REGISTER});
        paramTypes.put("brk", new int[] {});
        paramTypes.put("movrx", new int[] {REGISTER, INDIRECT});
        paramTypes.put("movxx", new int[] {INDIRECT, INDIRECT});
        paramTypes.put("outs", new int[] {LABEL});
        paramTypes.put("nop", new int[] {});
        paramTypes.put("jmpne", new int[] {LABEL});
    }
    
    /**
     * Translates a command into a byte code.
     * @param command the command to translate
     * @return the byte code associated with the given command, or null if there
     * is no such command
     */
    public static Integer getByteCode(String command) {
        return byteCodes.get(command);
    }
    
    /**
     * Translates a command into its parameter types.
     * @param command
     * @return the parameter types associated with the given command, or null if
     * there is no such command
     */
    public static int[] getParamTypes(String command) {
        return paramTypes.get(command);
    }
    
    /**
     * Translates a byte code into a command.
     * @param byteCode the byte code to translate
     * @return the command associated with the given byte code, or null if there
     * is no such byte code
     */
    public static String getCommand(Integer byteCode) {
        return commands.get(byteCode);
    }
}
