package sap.ide;

import java.awt.Font;

/**
 * Displays help for the SAP language.
 *
 * @author Ari Zerner
 */
public class Help extends javax.swing.JFrame {

    /**
     * Creates new form Help
     */
    private Help() {
        initComponents();
    }

    /**
     * Accesses the singleton help frame.
     *
     * @return the help frame
     */
    public static Help getHelpFrame() {
        return helpFrame;
    }
    private final static Help helpFrame = new Help();
    private final static String basics, instructions, directives,
            source, listing, byteCodes;
    private final static Font lucida = new Font("Lucida Grande", Font.PLAIN, 13),
            mono = new Font("Monospaced", Font.PLAIN, 13);

    static {
        basics = "Welcome to SAP! SAP is an assembly language invented by "
                + "Jeffery Stulin. I have no idea what to write in here. "
                + "Please tell me (Ari) what you think would be helpful.";

        instructions = "Code\tInstruction\tParameters\tDescription\n"
                + "0\thalt\t<none>\tHalt the program.\n"
                + "1\tclrr\trX\tClear the contents of rX.\n"
                + "2\tclrx\trX\tClear the memory location specified by rX.\n"
                + "3\tclrm\tlabel\tClear the memory location specified by label.\n"
                + "4\tclrb\trX rY\tClear a block of memory. The starting location "
                + "is specified by rX, the count by rY.\n"
                + "5\tmovir\t#XXX rY\tMove XXX to rY.\n"
                + "6\tmovrr\trX rY\tMove the contents of rX to rY.\n"
                + "7\tmovrm\trX label\tMove the contents of rX to the memory "
                + "location specified by label.\n"
                + "8\tmovmr\tlabel rX\tMove the contents of the memory location "
                + "specified by label to rX.\n"
                + "9\tmovxr\trX rY\tMove the memory location specified by rX to rY.\n"
                + "10\tmovar\tlabel rX\tMove the address of label to rX.\n"
                + "11\tmovb\trX rY rZ\tMove a block of memory. The source is "
                + "specified by rX, the destination by rY, and the count by rZ.\n"
                + "12\taddir\t#XXX rY\tAdd XXX to rY.\n"
                + "13\taddrr\trX rY\tAdd the contents of rX to rY.\n"
                + "14\taddmr\tlabel rX\tAdd the contents of the memory location "
                + "specified by label to rX.\n"
                + "15\taddxr\trX rY\tAdd the contents of the memory location "
                + "specified by rY to rX.\n"
                + "16\tsubir\t#XXX rY\tSubtract XXX from rY.\n"
                + "17\tsubrr\trX rY\tSubtract the contents of rX from rY.\n"
                + "18\tsubmr\tlabel rX\tSubtract the contents of the memory location "
                + "specified by label from rX.\n"
                + "19\tsubxr\trX rY\tSubtract the contents of the memory location "
                + "specified by rY from rX.\n"
                + "20\tmulir\t#XXX rY\tMultiply rY by XXX.\n"
                + "21\tmulrr\trX rY\tMultiply rY by the contents of rX.\n"
                + "22\tmulmr\tlabel rX\tMultiply rX by the contents of the memory "
                + "location specified by label.\n"
                + "23\tmulxr\trX rY\tMultiply rX by the contents of the memory "
                + "location specified by rY.\n"
                + "24\tdivir\t#XXX rY\tDivide rY by XXX.\n"
                + "25\tdivrr\trX rY\tDivide rY by the contents of rX.\n"
                + "26\tdivmr\tlabel rX\tDivide rX by the contents of the memory "
                + "location specified by label.\n"
                + "27\tdivxr\trX rY\tDivide rX by the contents of the memory "
                + "location specified by rY.\n"
                + "28\tjmp\tlabel\tJump to memory location label.\n"
                + "29\tsojz\trX label\tSubtract one from rX. Jump to label if "
                + "result is zero.\n"
                + "30\tsojnz\trX label\tSubtract one from rX. Jump to label if "
                + "result is not zero.\n"
                + "31\taojz\trX label\tAdd one to rX. Jump to label if "
                + "result is zero.\n"
                + "32\taojnz\trX label\tAdd one to rX. Jump to label if "
                + "result is not zero.\n"
                + "33\tcmpir\t#XXX rY\tMove the contents of rY minus XXX to "
                + "the compare register.\n"
                + "34\tcmprr\t#rX rY\tMove the contents of rY minus the "
                + "contents of rX to the compare register.\n"
                + "35\tcmpmr\tlabel rY\tMove the contents of rY minus the "
                + "contents of the memory location specified by label to the "
                + "compare register.\n"
                + "36\tjmpn\tlabel\tJump to memory location label if the "
                + "contents of the compare register are negative.\n"
                + "37\tjmpz\tlabel\tJump to memory location label if the "
                + "contents of the compare register are zero.\n"
                + "38\tjmpp\tlabel\tJump to memory location label if the "
                + "contents of the compare register are positive.\n"
                + "39\tjsr\tlabel\tJump to subroutine label. "
                + "r5-r9 will be saved on the stack.\n"
                + "40\tret\t<none>\tReturn from subroutine.\n"
                + "41\tpush\trX\tPush the contents of rX onto the stack.\n"
                + "42\tpop\trX\tPop the top of the stack into rX.\n"
                + "43\tstackc\trX\tPuts the condition of the last push or pop "
                + "in rX. 0 - ok, 1 - full, 2 - empty.\n"
                + "44\toutci\t#XXX\tOutput the ASCII value of XXX to the console.\n"
                + "45\toutcr\trX\tOutput the ASCII value of the contents of rX "
                + "to the console.\n"
                + "46\toutcx\trX\tOutput the ASCII value of the contents of the "
                + "memory location specified by rX the to the console.\n"
                + "47\toutcb\trX rY\tOutput a block of characters. The starting "
                + "location is specified by rX, the count by rY.\n"
                + "48\treadi\trX rY\tRead an integer from the console into rX. "
                + "The condition is stored in rY. 0 - ok, 1 - error.\n"
                + "49\tprinti\trX\tPrint the integer in rX to the console.\n"
                + "50\treadc\trX\tRead a character from the console into rX.\n"
                + "51\treadln\tlabel rX\tRead a line of text from the console "
                + "starting at the memory location specified by label. Store the "
                + "length of the line in rX.\n"
                + "52\tbrk\t<none>\tIf debugging, break into debugger.\n"
                + "53\tmovrx\trX rY\tMove the contents of rX into the memory "
                + "location specified by rY.\n"
                + "54\tmovxx\trX rY\tMove the contents of the memory location "
                + "specified by rX into the memory location specified by rY.\n"
                + "55\touts\tlabel\tOutput the string stored in label.\n"
                + "56\tnop\t<none>\tNo operation.\n"
                + "57\tjmpne\tlabel\tJump to memory location label if the "
                + "contents of the compare register are not zero.";

        directives = "Directive\tParameters\tDescription\n"
                + ".start\tlabel\tThe program should start execution at memory "
                + "location label.\n"
                + ".end\t<none>\tAssembly ends.\n"
                + ".integer\t#XXX\tPlace the integer XXX into memory at the "
                + "specified location.\n"
                + ".allocate\t#XXX\tAllocate XXX memory locations.\n"
                + ".string\t\"text\"\tAllocate enough memory locations to hold "
                + "the string one character per location.\n"
                + "\t\tOne extra location is also allocated. The first "
                + "allocated location will contain the\n"
                + "\t\tlength of the string.";

        source = "; A program to print numbers and their doubles\n"
                + "\n"
                + "        .Start   Test\n"
                + "Begin:      .Integer #0     ;Begin Printing Doubles\n"
                + "End:        .Integer #20    ;End of Doubles to Print\n"
                + "NewLine:    .Integer #10    ;Ascii for newline\n"
                + "IntroMess:  .String \"A Program To Print Doubles\"\n"
                + "DoubleMess: .String \" Doubled is \"\n"
                + "\n"
                + ";r0 will contain the NewLine character\n"
                + ";r8 will contain the integer to double\n"
                + ";r1 will contain the doubled integer\n"
                + ";r9 will contain the last number to double\n"
                + "\n"
                + "Test:   movmr Begin r8\n"
                + "        movmr End r9\n"
                + "        movmr NewLine r0\n"
                + "\n"
                + "        outs IntroMess\n"
                + "        outcr r0\n"
                + "\n"
                + "Do01:   movrr r8 r1\n"
                + "        addrr r8 r1\n"
                + "        printi r8\n"
                + "        outs DoubleMess\n"
                + "        printi r1\n"
                + "        outcr r0\n"
                + "        cmprr r8 r9\n"
                + "        addir #1 r8\n"
                + "        jmpne do01\n"
                + "wh01:   halt\n"
                + "        .end";

        listing = "0 0:                                ; A program to print numbers and their doubles\n"
                + "1\n"
                + "2 0:                                .Start   Test\n"
                + "3 0:  0                             Begin:      .Integer #0     ;Begin Printing Doubles\n"
                + "4 1:  20                            End:        .Integer #20    ;End of Doubles to Print\n"
                + "5 2:  10                            NewLine:    .Integer #10    ;Ascii for newline\n"
                + "6 3:  26 65 32 80 114 111 103 114   IntroMess:  .String \"A Program To Print Doubles\"\n"
                + "7 30:  12 32 68 111 117 98 108 101  DoubleMess: .String \" Doubled is \"\n"
                + "8\n"
                + "9 43:                               ;r0 will contain the NewLine character\n"
                + "10 43:                              ;r8 will contain the integer to double\n"
                + "11 43:                              ;r1 will contain the doubled integer\n"
                + "12 43:                              ;r9 will contain the last number to double\n"
                + "13\n"
                + "14 43:  8 0 8                       Test:   movmr Begin r8\n"
                + "15 46:  8 1 9                       movmr End r9\n"
                + "16 49:  8 2 0                       movmr NewLine r0\n"
                + "17\n"
                + "18 52:  55 3                        outs IntroMess\n"
                + "19 54:  45 0                        outcr r0\n"
                + "20\n"
                + "21 56:  6 8 1                       Do01:   movrr r8 r1\n"
                + "22 59:  13 8 1                      addrr r8 r1\n"
                + "23 62:  49 8                        printi r8\n"
                + "24 64:  55 30                       outs DoubleMess\n"
                + "25 66:  49 1                        printi r1\n"
                + "26 68:  45 0                        outcr r0\n"
                + "27 70:  34 8 9                      cmprr r8 r9\n"
                + "28 73:  12 1 8                      addir #1 r8\n"
                + "29 76:  57 56                       jmpne do01\n"
                + "30 78:  0                           wh01:   halt\n"
                + "31 79:                              .end\n"
                + "\n"
                + "Symbol Table:\n"
                + "begin 0\n"
                + "do01 56\n"
                + "doublemess 30\n"
                + "end 1\n"
                + "intromess 3\n"
                + "newline 2\n"
                + "test 43\n"
                + "wh01 78\n"
                + "\n"
                + "Number Assembly Errors: 0";

        byteCodes = "79\n"
                + "43\n"
                + "0\n"
                + "20\n"
                + "10\n"
                + "26\n"
                + "65\n"
                + "32\n"
                + "80\n"
                + "114\n"
                + "111\n"
                + "103\n"
                + "114\n"
                + "97\n"
                + "109\n"
                + "32\n"
                + "84\n"
                + "111\n"
                + "32\n"
                + "80\n"
                + "114\n"
                + "105\n"
                + "110\n"
                + "116\n"
                + "32\n"
                + "68\n"
                + "111\n"
                + "117\n"
                + "98\n"
                + "108\n"
                + "101\n"
                + "115\n"
                + "12\n"
                + "32\n"
                + "68\n"
                + "111\n"
                + "117\n"
                + "98\n"
                + "108\n"
                + "101\n"
                + "100\n"
                + "32\n"
                + "105\n"
                + "115\n"
                + "32\n"
                + "8\n"
                + "0\n"
                + "8\n"
                + "8\n"
                + "1\n"
                + "9\n"
                + "8\n"
                + "2\n"
                + "0\n"
                + "55\n"
                + "3\n"
                + "45\n"
                + "0\n"
                + "6\n"
                + "8\n"
                + "1\n"
                + "13\n"
                + "8\n"
                + "1\n"
                + "49\n"
                + "8\n"
                + "55\n"
                + "30\n"
                + "49\n"
                + "1\n"
                + "45\n"
                + "0\n"
                + "34\n"
                + "8\n"
                + "9\n"
                + "12\n"
                + "1\n"
                + "8\n"
                + "57\n"
                + "56\n"
                + "0\n";
    }

    /**
     * Convenience method to show the help frame. Calls setVisible(true) and
     * toFront().
     */
    public static void showHelp() {
        helpFrame.setVisible(true);
        helpFrame.toFront();
    }

    /**
     * Displays the basics in the help frame.
     */
    public static void displayBasics() {
        helpFrame.text.setLineWrap(true);
        helpFrame.text.setFont(lucida);
        helpFrame.text.setText(basics);
    }

    /**
     * Displays the instructions in the help frame.
     */
    public static void displayInstructions() {
        helpFrame.text.setLineWrap(false);
        helpFrame.text.setFont(lucida);
        helpFrame.text.setText(instructions);
    }

    /**
     * Displays the directives in the help frame.
     */
    public static void displayDirectives() {
        helpFrame.text.setLineWrap(false);
        helpFrame.text.setFont(lucida);
        helpFrame.text.setText(directives);
    }

    /**
     * Displays the source in the help frame.
     */
    public static void displaySource() {
        helpFrame.text.setLineWrap(false);
        helpFrame.text.setFont(mono);
        helpFrame.text.setText(source);
    }

    /**
     * Displays the listing in the help frame.
     */
    public static void displayListing() {
        helpFrame.text.setLineWrap(false);
        helpFrame.text.setFont(mono);
        helpFrame.text.setText(listing);
    }

    /**
     * Displays the byte codes in the help frame.
     */
    public static void displayByteCodes() {
        helpFrame.text.setLineWrap(false);
        helpFrame.text.setFont(mono);
        helpFrame.text.setText(byteCodes);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        text = new javax.swing.JTextArea();
        jMenuBar1 = new javax.swing.JMenuBar();
        menu_stulinBeans = new javax.swing.JMenu();
        menu_stulinBeans_quit = new javax.swing.JMenuItem();
        menu_help = new javax.swing.JMenu();
        menu_help_language = new javax.swing.JMenu();
        help_language_basics = new javax.swing.JMenuItem();
        menu_help_language_instructions = new javax.swing.JMenuItem();
        menu_help_language_directives = new javax.swing.JMenuItem();
        menu_help_example = new javax.swing.JMenu();
        menu_help_example_source = new javax.swing.JMenuItem();
        menu_help_example_listing = new javax.swing.JMenuItem();
        menu_help_example_byteCodes = new javax.swing.JMenuItem();

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        setTitle("Help");

        text.setEditable(false);
        text.setColumns(20);
        text.setRows(5);
        text.setWrapStyleWord(true);
        jScrollPane2.setViewportView(text);

        menu_stulinBeans.setText("StulinBeans");

        menu_stulinBeans_quit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        menu_stulinBeans_quit.setText("Quit StulinBeans");
        menu_stulinBeans_quit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_stulinBeans_quitActionPerformed(evt);
            }
        });
        menu_stulinBeans.add(menu_stulinBeans_quit);

        jMenuBar1.add(menu_stulinBeans);

        menu_help.setText("Help");

        menu_help_language.setText("Language");

        help_language_basics.setText("Basics");
        help_language_basics.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                help_language_basicsActionPerformed(evt);
            }
        });
        menu_help_language.add(help_language_basics);

        menu_help_language_instructions.setText("Instructions");
        menu_help_language_instructions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_help_language_instructionsActionPerformed(evt);
            }
        });
        menu_help_language.add(menu_help_language_instructions);

        menu_help_language_directives.setText("Directives");
        menu_help_language_directives.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_help_language_directivesActionPerformed(evt);
            }
        });
        menu_help_language.add(menu_help_language_directives);

        menu_help.add(menu_help_language);

        menu_help_example.setText("Example Program");

        menu_help_example_source.setText("Source");
        menu_help_example_source.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_help_example_sourceActionPerformed(evt);
            }
        });
        menu_help_example.add(menu_help_example_source);

        menu_help_example_listing.setText("Listing");
        menu_help_example_listing.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_help_example_listingActionPerformed(evt);
            }
        });
        menu_help_example.add(menu_help_example_listing);

        menu_help_example_byteCodes.setText("Byte Codes");
        menu_help_example_byteCodes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_help_example_byteCodesActionPerformed(evt);
            }
        });
        menu_help_example.add(menu_help_example_byteCodes);

        menu_help.add(menu_help_example);

        jMenuBar1.add(menu_help);

        setJMenuBar(jMenuBar1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1067, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 556, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void menu_stulinBeans_quitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_stulinBeans_quitActionPerformed
        Editor.quit();
    }//GEN-LAST:event_menu_stulinBeans_quitActionPerformed

    private void menu_help_language_instructionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_help_language_instructionsActionPerformed
        displayInstructions();
    }//GEN-LAST:event_menu_help_language_instructionsActionPerformed

    private void menu_help_language_directivesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_help_language_directivesActionPerformed
        displayDirectives();
    }//GEN-LAST:event_menu_help_language_directivesActionPerformed

    private void help_language_basicsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_help_language_basicsActionPerformed
        displayBasics();
    }//GEN-LAST:event_help_language_basicsActionPerformed

    private void menu_help_example_listingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_help_example_listingActionPerformed
        displayListing();
    }//GEN-LAST:event_menu_help_example_listingActionPerformed

    private void menu_help_example_sourceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_help_example_sourceActionPerformed
        displaySource();
    }//GEN-LAST:event_menu_help_example_sourceActionPerformed

    private void menu_help_example_byteCodesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_help_example_byteCodesActionPerformed
        displayByteCodes();
    }//GEN-LAST:event_menu_help_example_byteCodesActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem help_language_basics;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JMenu menu_help;
    private javax.swing.JMenu menu_help_example;
    private javax.swing.JMenuItem menu_help_example_byteCodes;
    private javax.swing.JMenuItem menu_help_example_listing;
    private javax.swing.JMenuItem menu_help_example_source;
    private javax.swing.JMenu menu_help_language;
    private javax.swing.JMenuItem menu_help_language_directives;
    private javax.swing.JMenuItem menu_help_language_instructions;
    private javax.swing.JMenu menu_stulinBeans;
    private javax.swing.JMenuItem menu_stulinBeans_quit;
    private javax.swing.JTextArea text;
    // End of variables declaration//GEN-END:variables
}
