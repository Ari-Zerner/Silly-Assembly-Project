package sap.ide;

import java.io.*;
import java.util.*;
import sap.SymbolTable;
import static sap.Language.*;

/**
 * The VirtualMachine class can runBytecodeFile or debug a SAP binary file.
 * @author Ari Zerner
 */
public class VirtualMachine {

    private int pc, memory[], registers[], compare, stackc;
    private queuestack.Stack stack;
    private Console run, debug;
    private boolean halt;

    private class Halt extends RuntimeException {
    }
    private boolean debugging, watch;
    private SymbolTable symTable = new SymbolTable();
    private Set<Integer> breakpoints = new TreeSet<Integer>(),
            memWatch = new TreeSet<Integer>(),
            regWatch = new TreeSet<Integer>();

    /**
     * Creates a new virtual machine with the given memory size and Consoles,
     * 10 registers, and a stack with 400 slots.
     * @param memorySize the maximum memory capacity in number of integers
     * @param run the Console to use for program IO. cannot be null
     * @param debug the Console to use for debugging IO. if this is null,
     * the VirtualMachine will not be able to debug
     * @throws NullPointerException if run is null
     */
    public VirtualMachine(int memorySize, Console run, Console debug) {
        this(memorySize, 10, 400, run, debug);
    }

    /**
     * Creates a new virtual machine with the given parameters.
     * @param memorySize the maximum memory capacity in number of integers
     * @param numRegisters the number of registers
     * @param stackLength the length of the stack
     * @param run the Console to use for program IO. cannot be null
     * @param debug the Console to use for debugging IO. if this is null,
     * the VirtualMachine will not be able to debug
     * @throws NullPointerException if run is null
     */
    private VirtualMachine(int memorySize, int numRegisters, int stackLength,
            Console run, Console debug) {
        if (run == null) {
            throw new NullPointerException();
        }
        memory = new int[memorySize];
        registers = new int[numRegisters];
        stack = new queuestack.Stack(stackLength);
        this.run = run;
        this.debug = debug;
    }

    /**
     * Checks whether this VirtualMachine can debug.
     * @return true if and only if this VirtualMachine can debug
     */
    public boolean canDebug() {
        return debug != null;
    }

    /**
     * Runs or debugs a SAP binary file.
     * @param fileName the name of the file to run or debug. This
     * method automatically adds .bin to the end.
     * @param dbg whether to run in debug mode. does nothing if canDebug()
     * returns false
     * @see canDebug()
     */
    public void runBytecodeFile(String fileName, boolean dbg) {
        debugging = dbg && canDebug();
        reset();
        if (!loadProgram(fileName)) {
            return;
        }
        if (debugging && !loadSymbols(fileName)) {
            return;
        }
        try {
            if (debugging) {
                debugPrompt();
            }
            while (true) {
                doCommand();
            }
        } catch (Halt h) {
        }
    }

    /**
     * Clears memory, registers (including compare), and stack.
     * If debugging, also clears breakpoints and symbol table.
     */
    private void reset() {
        for (int i = 0; i < memory.length; i++) {
            memory[i] = 0;
        }
        for (int i = 0; i < registers.length; i++) {
            registers[i] = 0;
        }
        compare = 0;
        while (!stack.isEmpty()) {
            pop();
        }
        halt = false;
        if (debugging) {
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
        fileName += ".bin";
        try {
            Scanner bin = new Scanner(new FileInputStream(fileName));
            int size = bin.nextInt();
            if (size >= memory.length) {
                run.out.println("Error: Program is too large");
                return false;
            }
            pc = bin.nextInt();
            for (int i = 0; i < size; i++) {
                memory[i] = bin.nextInt();
            }
            bin.close();
        } catch (FileNotFoundException exc) {
            run.out.println("Error: Unable to find " + fileName);
            return false;
        } catch (Exception exc) {
            run.out.println("Error: Unable to read " + fileName);
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
            debug.out.println("Error: Unable to find " + fileName);
            return false;
        } catch (Exception exc) {
            debug.out.println("Error: Unable to read " + fileName);
            return false;
        }
        return true;
    }

    private void debugPrompt() {
        debug.out.print("dbg " + pc);
        String label = symTable.getLabel(pc);
        if (label != null) {
            debug.out.print('(' + label + ')');
        }
        debug.out.print('>');
        String line = debug.getInputLine(), command, param;
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
        if (command.isEmpty()) {
            debugPrompt();
            return;
        }
        command = command.trim();
        param = param.trim();
        Scanner params = new Scanner(param);
        boolean go = false;
        try {
            if (command.equalsIgnoreCase("go")) {
                if (!param.isEmpty()) {
                    pc = parseLoc(params.next());
                } else {
                    go = true;
                }
            } else if (command.equalsIgnoreCase("dump")) {
                int start = parseLoc(params.next());
                int end = parseLoc(params.next());
                if (start < 0) {
                    start = 0;
                }
                if (end >= memory.length) {
                    end = memory.length - 1;
                }
                debug.out.println("Memory:");
                for (int i = start; i <= end; i++) {
                    debug.out.println("@" + i + ": " + memory[i]);
                }
            } else if (command.equalsIgnoreCase("dumpr")) {
                debug.out.println("Registers:");
                for (int i = 0; i < registers.length; i++) {
                    debug.out.println("r" + i + ": " + registers[i]);
                }
            } else if (command.equalsIgnoreCase("exit")) {
                throw new Halt();
            } else if (command.equalsIgnoreCase("deas")) {
                deassemble(parseLoc(params.next()), parseLoc(params.next()));
            } else if (command.equalsIgnoreCase("brkt")) {
                debug.out.println("Breakpoints: " + breakpoints);
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
                if (loc >= 0 && loc < memory.length) {
                    memWatch.add(loc);
                }
                watch = true;
            } else if (command.equalsIgnoreCase("swchr")) {
                int reg = parseReg(params.next());
                if (reg >= 0 && reg < registers.length) {
                    regWatch.add(reg);
                }
                watch = true;
            } else if (command.equalsIgnoreCase("cwchm")) {
                int loc = parseLoc(params.next());
                memWatch.remove(loc);
            } else if (command.equalsIgnoreCase("cwchr")) {
                int reg = parseReg(params.next());
                regWatch.remove(reg);
            } else if (command.equalsIgnoreCase("wcht")) {
                debug.out.println("Watch variables:");
                Iterator<Integer> memPoints = memWatch.iterator(),
                        regPoints = regWatch.iterator();
                while (memPoints.hasNext()) {
                    int pt = memPoints.next();
                    debug.out.println("@" + pt + ": " + memory[pt]);
                }
                while (regPoints.hasNext()) {
                    int pt = regPoints.next();
                    debug.out.println("r" + pt + ": " + registers[pt]);
                }
            } else if (command.equalsIgnoreCase("cwcht")) {
                memWatch.clear();
                regWatch.clear();
            } else {
                debug.out.println("Unrecognized command: " + command);
            }
        } catch (InputMismatchException exc) {
            debug.out.println("Bad argument.");
        } catch (NoSuchElementException exc) {
            debug.out.println("Not enough arguments.");
        }
        if (!go) {
            debugPrompt();
        }
    }

    private int parseLoc(String loc) {
        try {
            if (loc.startsWith("#")) {
                return Integer.parseInt(loc.substring(1));
            }
            Integer address = symTable.getAddress(loc);
            if (address != null) {
                return address;
            }
            return Integer.parseInt(loc);
        } catch (NumberFormatException exc) {
            throw new InputMismatchException();
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
                debug.out.println("Error: Unrecognized command");
                break;
            }
            String Label = symTable.getLabel(i);
            if (Label != null) {
                debug.out.print(Label + ": ");
            }
            debug.out.print(sapCommand);
            for (int pt : getParamTypes(sapCommand)) {
                switch (pt) {
                    case REGISTER:
                        debug.out.print(" r" + memory[++i]);
                        break;
                    case IMMEDIATE:
                        debug.out.print(" #" + memory[++i]);
                        break;
                    case LABEL:
                        String label = symTable.getLabel(memory[++i]);
                        debug.out.print(" "
                                + (label != null ? label : "#" + memory[i]));
                        break;
                    case INDIRECT:
                        debug.out.print(" r" + memory[++i]);
                        break;
                }
            }
            debug.out.println();
        }
    }

    private void printDebugHelp() {
        debug.out.print("go - Begin execution at current location\n"
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
            run.out.println("Error: Attmepted to access nonexistent "
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
            run.out.println("Error: Attmepted to access nonexistent "
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
            run.out.println("Error: Attmepted to access nonexistent "
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
            run.out.println("Error: Attmepted to access nonexistent "
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
        if (condition) {
            pc = nextMem();
        } else {
            pc++; // skip over jump address
        }
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
            run.out.println("Error: Attempted to divide by zero");
            throw new Halt();
        }
        setRegister(getRegister(index) / i, index);
        return registers[index];
    }

    private void push(int val) {
        boolean full = stack.isFull();
        stackc = full ? 1 : 0;
        if (!full) {
            stack.push(val);
        }
    }

    private int pop() {
        boolean empty = stack.isEmpty();
        stackc = empty ? 2 : 0;
        return empty ? 0 : stack.pop();
    }

    private void doCommand() {
        if (halt) {
            throw new Halt();
        }
        int start, dest, count; // for block operations
        if (debugging && breakpoints.contains(pc)) {
            debugPrompt();
        }
        Hashtable<Integer, Integer> memWatchVals = null, regWatchVals = null;
        boolean broken = false;
        if (debugging && watch) {
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
                for (int i = 0; i < count; i++) {
                    setMemory(0, count + i);
                }
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
                for (int i = 0; i < count; i++) {
                    setMemory(getMemory(start + i), dest + i);
                }
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
                compare = -(nextMem() - getRegister());
                break;
            case 34: // cmprr
                compare = -(getRegister() - getRegister());
                break;
            case 35: // cmpmr
                compare = -(getMemory() - getRegister());
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
                for (int i = 5; i <= 9; i++) {
                    push(registers[i]);
                }
                pc = nextMem();
                break;
            case 40: // ret
                for (int i = 9; i >= 5; i--) {
                    registers[i] = pop();
                }
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
                run.out.print((char) nextMem());
                break;
            case 45: // outcr
                run.out.print((char) getRegister());
                break;
            case 46: // outcx
                run.out.print((char) getIndirect());
                break;
            case 47: // outcb
                start = getRegister();
                count = getRegister();
                for (int i = 0; i < count; i++) {
                    run.out.print((char) getMemory(start + i));
                }
                break;
            case 48: // readi
                try {
                    setRegister(Integer.parseInt(run.getInputLine()));
                    setRegister(0);
                } catch (NumberFormatException exc) {
                    pc++;
                    setRegister(1);
                }
                break;
            case 49: // printi
                run.out.print(getRegister());
                break;
            case 50: // readc
                setRegister(run.getInputChar());
                break;
            case 51: // readln
                String line = run.getInputLine();
                start = nextMem();
                count = line.length();
                for (int i = 0; i < count; i++) {
                    setMemory(line.charAt(i), start + i);
                }
                setRegister(count);
                break;
            case 52: // brk
                if (debugging) {
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
                for (int i = 0; i < count; i++) {
                    run.out.print((char) getMemory(start + i));
                }
                break;
            case 56: // nop
                break;
            case 57: // jmpne
                jumpIf(compare != 0);
                break;
            default:
                run.out.println("Error: Invalid command " + command);
                throw new Halt();
        }
        if (debugging && watch && !broken) {
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
            if (changed) {
                debugPrompt();
            }
        }
    }
}