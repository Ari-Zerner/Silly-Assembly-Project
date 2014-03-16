package sap;
import java.util.Scanner;

public class Driver {    
    
    public static void main(String[] args) {
        VirtualMachine vm = new VirtualMachine(10000);
        Scanner scan = new Scanner(System.in);
        String command, parameter, line;
        do {
            System.out.print("sap>");
            line = scan.nextLine();
            int spaceIndex = line.indexOf(' ');
            if (spaceIndex > 0) {
                command = line.substring(0, spaceIndex);
                parameter = line.substring(spaceIndex).trim();
            } else {
                command = line;
                parameter = "";
            }
            if (command.equalsIgnoreCase("asm")) {
                System.out.println("Assembling " + parameter + ".txt");
                Assembler.ASM.assembleFile(parameter);
            } else if (command.equalsIgnoreCase("run")) {
                System.out.println("Running " + parameter + ".bin");
                vm.runBytecodeFile(parameter, false);
            } else if (command.equalsIgnoreCase("asmr")) {
                System.out.println("Assembling " + parameter + ".txt");
                if (Assembler.ASM.assembleFile(parameter)) {
                    System.out.println("Running " + parameter + ".bin");
                    vm.runBytecodeFile(parameter, false);
                }
            } else if (command.equalsIgnoreCase("debug")) {
                System.out.println("Debugging " + parameter + ".bin");
                vm.runBytecodeFile(parameter, true);
            } else if (command.equalsIgnoreCase("asmd")) {
                System.out.println("Assembling " + parameter + ".txt");
                if (Assembler.ASM.assembleFile(parameter)) {
                    System.out.println("Debugging " + parameter + ".bin");
                    vm.runBytecodeFile(parameter, true);
                }
            } else if (command.equalsIgnoreCase("help"))
                System.out.println("Commands:\n"
                        + "asm <filename> - assemble\n"
                        + "run <filename> - run\n"
                        + "asmr <filename> - assemble and run\n"
                        + "debug <filename> - debug\n"
                        + "asmd <filename> - assemble and debug\n"
                        + "exit");
            else if (!command.equalsIgnoreCase("exit"))
                System.out.println("Unrecognized command: " + command);
        } while (!command.equalsIgnoreCase("exit"));
    }    
}
