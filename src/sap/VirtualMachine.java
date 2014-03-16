package sap;
import java.io.*;
import java.util.*;
import static sap.Language.*;

/**
 * The VirtualMachine class can runBytecodeFile or debug a SAP binary file.
 * @author Ari Zerner
 */
public class VirtualMachine {
    private int pc, memory[], registers[], compare, stackc;
    private queuestack.Stack stack;
    private Scanner scan = new Scanner(System.in);
    private class Halt extends RuntimeException {}
    
    private boolean dbg, watch;
    private SymbolTable symTable = new SymbolTable();
    private Set<Integer> breakpoints = new TreeSet<Integer>(),
            memWatch = new TreeSet<Integer>(),
            regWatch = new TreeSet<Integer>();
    
    /**
     * Creates a new virtual machine with the given memory size, 10 registers,
     * and a stack with 400 slots.
     * @param memorySize the maximum memory capacity in number of integers
     */
    public VirtualMachine(int memorySize) {
        this(memorySize, 10, 400);
    }
    
    /**
     * Creates a new virtual machine with the given parameters.
     * @param memorySize the maximum memory capacity in number of integers
     * @param numRegisters the number of registers
     * @param stackLength the length of the stack
     */
    private VirtualMachine(int memorySize, int numRegisters, int stackLength) {
        memory = new int[memorySize];
        registers = new int[numRegisters];
        stack = new queuestack.Stack(stackLength);
    }
    
    /**
     * Runs or debugs a SAP binary file.
     * @param fileName the name of the file to runBytecodeFile or debug. This method
     * automatically adds .bin to the end.
     * @param debug whether to runBytecodeFile in debug mode.
     */
    public void runBytecodeFile(String fileName, boolean debug) {
        dbg = debug;
        reset();
        if (!loadProgram(fileName)) return;
        if (dbg && !loadSymbols(fileName)) return;
        try {
            if (dbg) debugPrompt();
            while (true) doCommand();
        } catch (Halt h) {}
    }
    
    /**
     * Clears memory, registers (including compare), and stack.
     * If debugging, also clears breakpoints and symbol table.
     */
    private void reset() {
        for (int i = 0; i < memory.length; i++)
            memory[i] = 0;
        for (int i = 0; i < registers.length; i++)
            registers[i] = 0;
        compare = 0;
        while (!stack.isEmpty())
            pop();
        if (dbg) {
            breakpoints.clear();
            symTable.clear();
            memWatch.clear();
            regWatch.clear();
        }
    }
    
    /**
     * Reads a SAP bin file into memory. Prints an error message if loading
     * fails. This method automatically adds .bin to the end.
     * @param fileName the name of the file to load
     * @return whether the program loaded successfully
     */
    private boolean loadProgram(String fileName) {
        fileName = fileName + ".bin";
        try {
            Scanner bin = new Scanner(new FileInputStream(fileName));
            int size = bin.nextInt();
            if (size >= memory.length) {
                System.out.println("Error: Program is too large");
                return false;
            }
            pc = bin.nextInt();
            for (int i = 0; i < size; i++)
                memory[i] = bin.nextInt();
            bin.close();
        } catch (FileNotFoundException exc) {
            System.out.println("Error: Unable to find " + fileName);
            return false;
        } catch (Exception exc) {
            System.out.println("Error: Unable to read " + fileName);
            return false;
        }
        return true;
    }
    
    /*
     * The following methods are for the debugger.
     */
    
    private boolean loadSymbols(String fileName) {
        fileName = fileName + ".sym";
        try {
            Scanner sym = new Scanner(new FileInputStream(fileName));
            while (sym.hasNext()) {
                String label = sym.next();
                int address = sym.nextInt();
                symTable.addSymbol(label, address);
            }
            sym.close();
        } catch (FileNotFoundException exc) {
            System.out.println("Error: Unable to find " + fileName);
            return false;
        } catch (Exception exc) {
            System.out.println("Error: Unable to read " + fileName);
            return false;
        }
        return true;
    }
    
    private void debugPrompt() {
        System.out.print("dbg " + pc);
        String label = symTable.getLabel(pc);
        if (label != null)
            System.out.print('(' + label + ')');
        System.out.print('>');
        String line = scan.nextLine(), command, param;
        int spaceIndex = line.indexOf(' ');
        if (spaceIndex > 0) {
            command = line.substring(0, spaceIndex);
            param = line.substring(spaceIndex).trim();
        } else {
            command = line;
            param = "";
        }
        doDebug(command, param);
    }
    
    private void doDebug(String command, String param) {
        command = command.trim();
        param = param.trim();
        Scanner params = new Scanner(param);
        boolean go = false;
        try {
            if (command.equalsIgnoreCase("go")) {
                if (!param.isEmpty())
                    pc = parseLoc(params.next());
                else go = true;
            } else if (command.equalsIgnoreCase("dump")) {
                int start = parseLoc(params.next());
                int end = parseLoc(params.next());
                if (start < 0) start = 0;
                if (end >= memory.length) end = memory.length - 1;
                System.out.println("Memory:");
                for (int i = start; i <= end; i++)
                    System.out.println("@" + i + ": " + memory[i]);
            } else if (command.equalsIgnoreCase("dumpr")) {
                System.out.println("Registers:");
                for (int i = 0; i < registers.length; i++)
                    System.out.println("r" + i + ": " + registers[i]);
            } else if (command.equalsIgnoreCase("exit")) {
                throw new Halt();
            } else if (command.equalsIgnoreCase("deas")) {
                deassemble(parseLoc(params.next()), parseLoc(params.next()));
            } else if (command.equalsIgnoreCase("brkt")) {
                System.out.println("Breakpoints: " + breakpoints);
            } else if (command.equalsIgnoreCase("sbrk")) {
                breakpoints.add(parseLoc(params.next()));
            } else if (command.equalsIgnoreCase("cbrk")) {
                breakpoints.remove(parseLoc(params.next()));
            } else if (command.equalsIgnoreCase("cbrkt")) {
                breakpoints.clear();
            } else if (command.equalsIgnoreCase("help")) {
                printDebugHelp();
            } else if (command.equalsIgnoreCase("chngr")) {
                int reg = parseReg(params.next());
                setRegister(params.nextInt(), reg);
            } else if (command.equalsIgnoreCase("chngm")) {
                int mem = parseLoc(params.next());
                setMemory(params.nextInt(), mem);
            } else if (command.equalsIgnoreCase("wchon")) {
                watch = true;
            } else if (command.equalsIgnoreCase("wchoff")) {
                watch = false;
            } else if (command.equalsIgnoreCase("swchm")) {
                int loc = parseLoc(params.next());
                if (loc >= 0 && loc < memory.length)
                    memWatch.add(loc);
                watch = true;
            } else if (command.equalsIgnoreCase("swchr")) {
                int reg = parseReg(params.next());
                if (reg >= 0 && reg < registers.length)
                    regWatch.add(reg);
                watch = true;
            } else if (command.equalsIgnoreCase("cwchm")) {
                int loc = parseLoc(params.next());
                    memWatch.remove(loc);
            } else if (command.equalsIgnoreCase("cwchr")) {
                int reg = parseReg(params.next());
                    regWatch.remove(reg);
            } else if (command.equalsIgnoreCase("wcht")) {
                System.out.println("Watch variables:");
                Iterator<Integer> memPoints = memWatch.iterator(),
                        regPoints = regWatch.iterator();
                while (memPoints.hasNext()) {
                    int pt = memPoints.next();
                    System.out.println("@" + pt + ": " + memory[pt]);
                }
                while (regPoints.hasNext()) {
                    int pt = regPoints.next();
                    System.out.println("r" + pt + ": " + registers[pt]);
                }
            } else if (command.equalsIgnoreCase("cwcht")) {
                memWatch.clear();
                regWatch.clear();
            } else {
                System.out.println("Unrecognized command: " + command);
            }
        } catch (InputMismatchException exc) {
            System.out.println("Bad argument.");
        } catch (NoSuchElementException exc) {
            System.out.println("Not enough arguments.");
        }
        if (!go) debugPrompt();
    }
    
    private int parseLoc(String loc) {
        if (loc.startsWith("#")) {
            return Integer.parseInt(loc.substring(1));
        } else {
            try {
                return symTable.getAddress(loc);
            } catch (NullPointerException npe) {
                throw new InputMismatchException();
            }
        }
    }
    
    private int parseReg(String reg) {
        if (!reg.startsWith("r")) {
            throw new InputMismatchException();
        }
        try {
            return Integer.parseInt(reg.substring(1));
        } catch (NumberFormatException exc) {
            throw new InputMismatchException();
        }
    }
    
    private void deassemble(int start, int end) {
        if (start < 0) {
            start = 0;
        }
        if (end >= memory.length) {
            end = memory.length - 1;
        }
        for (int i = start; i <= end; i++) {
            String sapCommand = getCommand(memory[i]);
            if (sapCommand == null) {
                System.out.println("Error: Unrecognized command");
                break;
            }
            String Label = symTable.getLabel(i);
            if (Label != null)
                System.out.print(Label + ": ");
            System.out.print(sapCommand);
            for (int pt : getParamTypes(sapCommand))
                switch (pt) {
                    case REGISTER:
                        System.out.print(" r" + memory[++i]);
                        break;
                    case IMMEDIATE:
                        System.out.print(" #" + memory[++i]);
                        break;
                    case LABEL:
                        String label = symTable.getLabel(memory[++i]);
                        System.out.print(" " +
                                (label != null ? label : "#" + memory[i]));
                        break;
                    case INDIRECT:
                        System.out.print(" r" + memory[++i]);
                        break;
                }
            System.out.println();
        }
    }
    
    private void printDebugHelp() {
        System.out.print("go - Begin execution at current location\n"
                + "go <loc1> - Begin execution at <loc1>\n"
                + "dump <loc1> <loc2> - Dump memory locations between\n\t"
                + "<loc1> and <loc2> inclusive\n"
                + "dumpr - Dump registers\n"
                + "exit - Exit debugger\n"
                + "deas <loc1> <loc2> - Deassemble memory locations between\n\t"
                + "<loc1> and <loc2> inclusive\n"
                + "brkt - List breakpoints\n"
                + "sbrk <loc1> - Set breakpoint at <loc1>\n"
                + "cbrk <loc1> - Clear breakpoint at <loc1>\n"
                + "cbrkt - Clear all breakpoints\n"
                + "chngr <r#> <value> - Change the value of register <r#> to "
                + "<value>\n"
                + "chngm <loc1> <value> - Change the value of memory address\n\t"
                + "<loc1> to <value>\n"
                + "wchon - turn watch variables on\n"
                + "wchoff - turn watch variables off\n"
                + "swchm <loc1> - Set watch variable at <loc1>\n"
                + "swchr <r#> - Set watch variable at <r#>\n"
                + "cwchm <loc1> - Clear watch variable at <loc1>\n"
                + "cwchr <r#> - Clear watch variable at <r#>\n"
                + "wcht - List watch variables and their values\n"
                + "cwcht - Clear all watch variables\n");
    }
    
    /* 
     * The following methods are for running the low-level program commands.
     */
    
    private int nextMem() {
        return getMemory(pc++);
    }
    
    private int getMemory() {
        return getMemory(nextMem());
    } 
    
    private int getMemory(int index) {
        try {
            return memory[index];
        } catch (ArrayIndexOutOfBoundsException exc) {
            System.out.println("Error: Attmepted to access nonexistent "
                    + "memory address " + index);
            throw new Halt();
        }
    }
    
    private void setMemory(int value) {
        setMemory(value, nextMem());
    }
    
    private void setMemory(int value, int index) {
        try {
            memory[index] = value;
        } catch (ArrayIndexOutOfBoundsException exc) {
            System.out.println("Error: Attmepted to access nonexistent "
                    + "memory address " + index);
            throw new Halt();
        }
    }
    
    private int getRegister() {
        return getRegister(nextMem());
    }
    
    private int getRegister(int index) {
        try {
            return registers[index];
        } catch (ArrayIndexOutOfBoundsException exc) {
            System.out.println("Error: Attmepted to access nonexistent "
                    + "register " + index);
            throw new Halt();
        }
    }
    
    private void setRegister(int value) {
        setRegister(value, nextMem());
    }
    
    private void setRegister(int value, int index) {
        try {
            registers[index] = value;
        } catch (ArrayIndexOutOfBoundsException exc) {
            System.out.println("Error: Attmepted to access nonexistent "
                    + "register " + index);
            throw new Halt();
        }
    }
    
    private int getIndirect() {
        return getIndirect(nextMem());
    }
    
    private int getIndirect(int index) {
        return getMemory(getRegister(index));
    }
    
    private void setIndirect(int value) {
        setIndirect(value, nextMem());
    }
    
    private void setIndirect(int value, int index) {
        setMemory(value, getRegister(index));
    }
    
    private void jumpIf(boolean condition) {
        if (condition) pc = nextMem();
        else pc++; // skip over jump address
    }
    
    private int add(int i) {
        return add(i, nextMem());
    }
    
    private int add(int i, int index) {
        setRegister(getRegister(index) + i, index);
        return registers[index];
    }
    
    private int subtract(int i) {
        return subtract(i, nextMem());
    }
    
    private int subtract(int i, int index) {
        setRegister(getRegister(index) - i, index);
        return registers[index];
    }
    
    private int multiply(int i) {
        return multiply(i, nextMem());
    }
    
    private int multiply(int i, int index) {
        setRegister(getRegister(index) * i, index);
        return registers[index];
    }
    
    private int divide(int i) {
        return divide(i, nextMem());
    }
    
    private int divide(int i, int index) {
        if (i == 0) {
            System.out.println("Error: Attempted to divide by zero");
            throw new Halt();
        }
        setRegister(getRegister(index) / i, index);
        return registers[index];
    }
    
    private void push (int val) {
        boolean full = stack.isFull();
        stackc = full ? 1 : 0;
        if (!full) stack.push(val);
    }
    
    private int pop () {
        boolean empty = stack.isEmpty();
        stackc = empty ? 2 : 0;
        return empty ? 0 : stack.pop();
    }

    private void doCommand() {
        int start, dest, count; // for block operations
        if (dbg && breakpoints.contains(pc)) debugPrompt();
        Hashtable<Integer, Integer> memWatchVals = null, regWatchVals = null;
        boolean broken = false;
        if (dbg && watch) {
            memWatchVals = new Hashtable<Integer, Integer>(memWatch.size(), 1);
            regWatchVals = new Hashtable<Integer, Integer>(regWatch.size(), 1);
            Iterator<Integer> memPoints = memWatch.iterator(),
                    regPoints = regWatch.iterator();
            while (memPoints.hasNext()) {
                int pt = memPoints.next();
                memWatchVals.put(pt, memory[pt]);
            }
            while (regPoints.hasNext()) {
                int pt = regPoints.next();
                regWatchVals.put(pt, registers[pt]);
            }
        }
        int command = nextMem();
        switch (command) {
            case 0: // halt
                throw new Halt();
            case 1: // clrr
                setRegister(0);
                break;
            case 2: // clrx
                setIndirect(0);
                break;
            case 3: // clrm
                setMemory(0);
                break;
            case 4: // clrb
                start = getRegister();
                count = getRegister();
                for (int i = 0; i < count; i++)
                    setMemory(0, count + i);
                break;
            case 5: // movir
                setRegister(nextMem());
                break;
            case 6: // movrr
                setRegister(getRegister());
                break;
            case 7: // movrm
                setMemory(getRegister());
                break;
            case 8: // movmr
                setRegister(getMemory());
                break;
            case 9: // movxr
                setRegister(getIndirect());
                break;
            case 10: // movar
                setRegister(nextMem());
                break;
            case 11: // movb
                start = getRegister();
                dest = getRegister();
                count = getRegister();
                for (int i = 0; i < count; i++)
                    setMemory(getMemory(start + i), dest + i);
                break;
            case 12: // addir
                add(nextMem());
                break;
            case 13: // addrr
                add(getRegister());
                break;
            case 14: // addmr
                add(getMemory());
                break;
            case 15: // addxr
                add(getIndirect());
                break;
            case 16: // subir
                subtract(nextMem());
                break;
            case 17: // subrr
                subtract(getRegister());
                break;
            case 18: // submr
                subtract(getMemory());
                break;
            case 19: // subxr
                subtract(getIndirect());
                break;
            case 20: // mulir
                multiply(nextMem());
                break;
            case 21: // mulrr
                multiply(getRegister());
                break;
            case 22: // mulmr
                multiply(getMemory());
                break;
            case 23: // mulxr
                multiply(getIndirect());
                break;
            case 24: // divir
                divide(nextMem());
                break;
            case 25: // divrr
                divide(getRegister());
                break;
            case 26: // divmr
                divide(getMemory());
                break;
            case 27: // divxr
                divide(getIndirect());
                break;
            case 28: // jmp
                pc = nextMem();
                break;
            case 29: // sojz
                jumpIf(subtract(1) == 0);
                break;
            case 30: // sojnz
                jumpIf(subtract(1) != 0);
                break;
            case 31: // aojz
                jumpIf(add(1) == 0);
                break;
            case 32: // aojnz
                jumpIf(add(1) != 0);
                break;
            case 33: // cmpir
                compare = - (nextMem() - getRegister());
                break;
            case 34: // cmprr
                compare = - (getRegister() - getRegister());
                break;
            case 35: // cmpmr
                compare = - (getMemory() - getRegister());
                break;
            case 36: // jmpn
                jumpIf(compare < 0);
                break;
            case 37: // jmpz
                jumpIf(compare == 0);
                break;
            case 38: // jmpp
                jumpIf(compare > 0);
                break;
            case 39: // jsr
                push(pc + 1);
                for (int i = 5; i <= 9; i++)
                    push(registers[i]);
                pc = nextMem();
                break;
            case 40: // ret
                for (int i = 9; i >= 5; i--)
                    registers[i] = pop();
                pc = pop();
                break;
            case 41: // push
                push(getRegister());
                break;
            case 42: // pop
                setRegister(pop());
                break;
            case 43: // stackc
                setRegister(stackc);
                break;
            case 44: // outci
                System.out.print((char) nextMem());
                break;
            case 45: // outcr
                System.out.print((char) getRegister());
                break;
            case 46: // outcx
                System.out.print((char) getIndirect());
                break;
            case 47: // outcb
                start = getRegister();
                count = getRegister();
                for (int i = 0; i < count; i++)
                    System.out.print((char) getMemory(start + i));
                break;
            case 48: // readi
                try {
                    setRegister(scan.nextInt());
                    setRegister(0);
                } catch (InputMismatchException exc) {
                    pc++;
                    setRegister(1);
                }
                break;
            case 49: // printi
                System.out.print(getRegister());
                break;
            case 50: // readc
                try {
                    setRegister(System.in.read());
                } catch (IOException exc) {
                    pc++;
                }
                break;
            case 51: // readln
                String line = scan.nextLine();
                start = nextMem();
                count = line.length();
                for (int i = 0; i < count; i++)
                    setMemory(line.charAt(i), start + i);
                setRegister(count);
                break;
            case 52: // brk
                if (dbg) {
                    broken = true;
                    debugPrompt();
                }
                    break;
            case 53: // movrx
                setIndirect(getRegister());
                break;
            case 54: // movxx
                setIndirect(getIndirect());
                break;
            case 55: // outs
                start = nextMem();
                count = getMemory(start++);
                for (int i = 0; i < count; i++)
                    System.out.print((char) getMemory(start + i));
                break;
            case 56: // nop
                break;
            case 57: // jmpne
                jumpIf(compare != 0);
                break;
            default:
                System.out.println("Error: Invalid command " + command);
                throw new Halt();
        }
        if (dbg && watch && !broken) {
            boolean changed = false;
            Iterator<Integer> memPoints = memWatch.iterator(),
                    regPoints = regWatch.iterator();
            while (!changed && memPoints.hasNext()) {
                int pt = memPoints.next();
                changed = memWatchVals.get(pt) != memory[pt];
            }
            while (!changed && regPoints.hasNext()) {
                int pt = regPoints.next();
                changed = regWatchVals.get(pt) != registers[pt];
            }
            if (changed) debugPrompt();
        }
    }
}