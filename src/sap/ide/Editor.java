package sap.ide;

import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.undo.UndoManager;

/*
 * Things to add:
 * Printing
 * Syntax highlighting
 * Text searching
 * Numbered file text areas
 * User formatting
 *      Preferences saving
 */

/*
 * Problems:
 * Infinite print loop
 * All Editors closed
 */
/**
 * A frame used to manipulate one SAP program.
 *
 * @author Ari Zerner
 */
public class Editor extends javax.swing.JFrame {

    private Thread vmRunning;
    private VirtualMachine vm;
    private sap.Assembler asm;
    private File txtFile, lstFile, symFile, binFile;
    private UndoManager um = new UndoManager();
    private double runDebugSplitLast, outListSplitLast;
    private final Info info;
    private static Point nextPosition = new Point(0, 20);
    private static final int X_SHIFT = 20, Y_SHIFT = 20;
    private static Collection<Editor> allEditors =
            new LinkedList<Editor>(); // File is txt file
    private static final int SOURCE_INDEX = 0, LISTING_AND_OUTPUT_INDEX = 1;
    private static final String UNTITLED = "Untitled";
    private static final Color UNEDITABLE_BG = Color.WHITE /*new Color(230, 230, 230)*/;
    private static final JFileChooser FILE_CHOOSER = new JFileChooser(
            System.getProperty("user.dir"));

    static {
        FILE_CHOOSER.setAcceptAllFileFilterUsed(false);
        FILE_CHOOSER.setFileFilter(new FileNameExtensionFilter(
                "TXT files", "txt"));
        FILE_CHOOSER.setMultiSelectionEnabled(false);
    }

    /**
     * Creates an untitled Editor.
     */
    public Editor() {
        this(UNTITLED);
        txtFile.deleteOnExit();
        lstFile.deleteOnExit();
        binFile.deleteOnExit();
    }

    /**
     * Creates an Editor to manipulate the specified program.
     *
     * @param fn the name of the .txt file containing the program
     */
    public Editor(String fn) {
        initComponents();
        fn = removeExtension(fn);
        vm = new VirtualMachine(10000, run, debug);
        asm = new sap.Assembler();
        txtFile = new File(fn + ".txt");
        txt.load(txtFile);
        txt.getDocument().addUndoableEditListener(um);
        setTitle(removeExtension(txtFile.getName()));
        asm.assembleFile(fileNameNoExtension(), null);
        lstFile = new File(fn + ".lst");
        lst.load(lstFile);
        symFile = new File(fn + ".sym");
        binFile = new File(fn + ".bin");
        bin.load(binFile);
        setSplit(runDebugSplit, 0.5);
        outListSplit.setDividerLocation(0.5);
        setSplit(outListSplit, 1.0);
        run.allowInput(false);
        debug.allowInput(false);
        setLocation(nextPosition);
        nextPosition.translate(X_SHIFT, Y_SHIFT);
        allEditors.add(this);
        info = new Info(this);
    }

    static void quit() {
        Help.getHelpFrame().dispose();
        for (Editor e : allEditors) {
            e.formWindowClosing(null);
        }
    }

    /**
     * Removes the extension from a file name.
     *
     * @param fn the file name
     * @return the file name with the extension removed
     */
    private static String removeExtension(String fn) {
        if (fn == null) {
            return null;
        }
        int extStart = fn.lastIndexOf('.');
        return extStart > -1 && extStart > fn.lastIndexOf(File.pathSeparator)
                ? fn.substring(0, extStart) : fn;
    }

    /**
     * Generates information about the files maintained by this Editor.
     *
     * @return a String containing information about the files
     */
    String getInfo() {
        String info = "Source code:";
        info += "\nFile name: " + txtFile.getAbsolutePath();
        info += "\nFile length: " + txtFile.length();
        info += "\n\nAssembly listing:";
        info += "\nFile name: " + lstFile.getAbsolutePath();
        info += "\nFile length: " + lstFile.length();
        info += "\n\nSymbol table:";
        info += "\nFile name: " + symFile.getAbsolutePath();
        info += "\nFile length: " + symFile.length();
        if (binFile.exists()) {
            info += "\n\nByte codes:";
            info += "\nFile name: " + binFile.getAbsolutePath();
            info += "\nFile length: " + binFile.length();
        }
        return info;
    }

    @Override
    public void dispose() {
        info.dispose();
        super.dispose();
    }

    /**
     * Returns a String to be used as a parameter for the VirtualMachine and
     * Assembler.
     *
     * @return the absolute path name, minus the extension
     */
    private String fileNameNoExtension() {
        return removeExtension(txtFile.getAbsolutePath());
    }

    /**
     * Prepares to run a program.
     *
     * @return saveAndAssemble
     * @see saveAndAssemble(boolean showErrMess)
     */
    private boolean prepareToRun() {
        run.clear();
        debug.clear();
        tabPane.setSelectedIndex(LISTING_AND_OUTPUT_INDEX);
        return saveAndAssemble(true);
    }

    private boolean saveAndAssemble(boolean showErrMess) {
        txt.save();
        txt.isSaved();
        asm.assembleFile(fileNameNoExtension(), null);
        lst.load();
        bin.load();
        if (asm.wasError()) {
            bin.setText("");
            if (showErrMess) {
                JOptionPane.showMessageDialog(this, asm.getErrorMess(),
                        "Assembly Error!", JOptionPane.ERROR_MESSAGE);
            }
        }
        info.updateInfo();
        return !asm.wasError();
    }

    private void saveAndAssembleAsNew() {
        if (FILE_CHOOSER.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String fn = removeExtension(
                    FILE_CHOOSER.getSelectedFile().getAbsolutePath());
            File newTxtFile = new File(fn + ".txt");
            if (newTxtFile.exists()) {
                if (JOptionPane.showConfirmDialog(this,
                        newTxtFile + " already exists. Overwrite?", "Overwrite?",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
                        == JOptionPane.NO_OPTION) {
                    saveAndAssembleAsNew();
                    return;
                }
            }
            try {
                FileInputStream from = new FileInputStream(txtFile);
                FileOutputStream to = new FileOutputStream(newTxtFile);
                int i;
                do {
                    i = from.read();
                    if (i > -1) {
                        to.write(i);
                    }
                } while (i != -1);
            } catch (FileNotFoundException exc) {
            } catch (IOException exc) {
            }
            Editor e = new Editor(fn);
            e.setLocation(getLocation());
            e.setSize(getSize());
            e.setVisible(true);
            dispose();
        }
    }

    /*
     private double getSplit(JSplitPane jsp) {
     double d = ((double) jsp.getDividerLocation()) / (jsp.getOrientation() ==
     JSplitPane.HORIZONTAL_SPLIT ? jsp.getWidth() : jsp.getHeight());
     System.out.println(d);
     return d;
     }
     */
    private void setSplit(JSplitPane jsp, double d) {
        jsp.setDividerLocation(d);
        if (jsp == outListSplit) {
            outListSplitLast = d;
        }
        if (jsp == runDebugSplit) {
            runDebugSplitLast = d;
        }
    }

    private void reSplit(JSplitPane jsp) {
        if (jsp == outListSplit) {
            outListSplit.setDividerLocation(outListSplitLast);
        }
        if (jsp == runDebugSplit) {
            runDebugSplit.setDividerLocation(runDebugSplitLast);
        }
    }

    private void startRunning(boolean dbg) {
        menu_run_assemble.setEnabled(false);
        menu_run_run.setEnabled(false);
        menu_run_debug.setEnabled(false);
        menu_run_halt.setEnabled(true);
        run.allowInput(true);
        run.setFocusable(true);
        run.setBackground(Color.WHITE);
        if (dbg) {
            setSplit(runDebugSplit, 0.5);
            setSplit(outListSplit, 0.5);
            debug.allowInput(true);
            debug.setFocusable(true);
            debug.setBackground(Color.WHITE);
            debug.requestFocusInWindow();
        } else {
            setSplit(runDebugSplit, 1.0);
            setSplit(outListSplit, 0.0);
            run.requestFocusInWindow();
        }
    }

    private void stopRunning() {
        vmRunning = null;
        menu_run_assemble.setEnabled(true);
        menu_run_run.setEnabled(true);
        menu_run_debug.setEnabled(true);
        menu_run_halt.setEnabled(false);
        run.allowInput(false);
        run.setFocusable(false);
        run.setBackground(UNEDITABLE_BG);
        debug.allowInput(false);
        debug.setFocusable(false);
        debug.setBackground(UNEDITABLE_BG);
        outputPanel.requestFocusInWindow();
    }

    private void runVMOnNewThread(final boolean dbg) {
        if (vmRunning != null) {
            JOptionPane.showMessageDialog(this,
                    "Virtual Machine is already running!", null,
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        vmRunning = new Thread(new Runnable() {
            @Override
            public void run() {
                startRunning(dbg);
                vm.runBytecodeFile(fileNameNoExtension(), dbg);
                stopRunning();
            }
        });
        vmRunning.setDaemon(true);
        vmRunning.start();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tabPane = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        txt = new sap.ide.FileTextArea();
        outListSplit = new javax.swing.JSplitPane();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        lst = new sap.ide.FileTextArea();
        jScrollPane3 = new javax.swing.JScrollPane();
        bin = new sap.ide.FileTextArea();
        outputPanel = new javax.swing.JPanel();
        runDebugSplit = new javax.swing.JSplitPane();
        run = new sap.ide.Console();
        debug = new sap.ide.Console();
        jMenuBar1 = new javax.swing.JMenuBar();
        menu_stulinBeans = new javax.swing.JMenu();
        menu_stulinBeans_quit = new javax.swing.JMenuItem();
        menu_file = new javax.swing.JMenu();
        menu_file_new = new javax.swing.JMenuItem();
        menu_file_open = new javax.swing.JMenuItem();
        menu_file_save = new javax.swing.JMenuItem();
        menu_file_saveAs = new javax.swing.JMenuItem();
        menu_file_getInfo = new javax.swing.JMenuItem();
        menu_edit = new javax.swing.JMenu();
        menu_edit_cut = new javax.swing.JMenuItem();
        menu_edit_copy = new javax.swing.JMenuItem();
        menu_edit_paste = new javax.swing.JMenuItem();
        menu_edit_undo = new javax.swing.JMenuItem();
        menu_edit_redo = new javax.swing.JMenuItem();
        menu_edit_selectAll = new javax.swing.JMenuItem();
        menu_view = new javax.swing.JMenu();
        menu_view_source = new javax.swing.JMenuItem();
        menu_view_listing = new javax.swing.JMenuItem();
        menu_view_listingAndOutput = new javax.swing.JMenuItem();
        menu_view_output = new javax.swing.JMenuItem();
        menu_run = new javax.swing.JMenu();
        menu_run_assemble = new javax.swing.JMenuItem();
        menu_run_run = new javax.swing.JMenuItem();
        menu_run_debug = new javax.swing.JMenuItem();
        menu_run_halt = new javax.swing.JMenuItem();
        menu_help = new javax.swing.JMenu();
        menu_help_language = new javax.swing.JMenu();
        menu_help_language_instructions = new javax.swing.JMenuItem();
        menu_help_language_directives = new javax.swing.JMenuItem();
        help_language_basics = new javax.swing.JMenuItem();
        menu_help_example = new javax.swing.JMenu();
        menu_help_example_source = new javax.swing.JMenuItem();
        menu_help_example_listing = new javax.swing.JMenuItem();
        menu_help_example_byteCodes = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(new java.awt.Dimension(1200, 800));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        txt.setColumns(20);
        txt.setRows(5);
        txt.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jScrollPane1.setViewportView(txt);

        tabPane.addTab("Source", jScrollPane1);

        outListSplit.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        outListSplit.setOneTouchExpandable(true);

        lst.setBackground(UNEDITABLE_BG);
        lst.setColumns(20);
        lst.setEditable(false);
        lst.setRows(20);
        lst.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jScrollPane2.setViewportView(lst);

        jSplitPane1.setRightComponent(jScrollPane2);

        bin.setBackground(UNEDITABLE_BG);
        bin.setColumns(5);
        bin.setEditable(false);
        bin.setRows(5);
        bin.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jScrollPane3.setViewportView(bin);

        jSplitPane1.setLeftComponent(jScrollPane3);

        outListSplit.setLeftComponent(jSplitPane1);

        run.setBackground(UNEDITABLE_BG);
        run.setFocusable(false);
        run.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        runDebugSplit.setLeftComponent(run);

        debug.setBackground(UNEDITABLE_BG);
        debug.setFocusable(false);
        debug.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        runDebugSplit.setRightComponent(debug);

        org.jdesktop.layout.GroupLayout outputPanelLayout = new org.jdesktop.layout.GroupLayout(outputPanel);
        outputPanel.setLayout(outputPanelLayout);
        outputPanelLayout.setHorizontalGroup(
            outputPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(runDebugSplit, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1006, Short.MAX_VALUE)
        );
        outputPanelLayout.setVerticalGroup(
            outputPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(runDebugSplit, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 501, Short.MAX_VALUE)
        );

        setSplit(runDebugSplit, 0.5);

        outListSplit.setRightComponent(outputPanel);

        tabPane.addTab("Listing / Output", outListSplit);
        setSplit(outListSplit, 1.0);

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

        menu_file.setText("File");

        menu_file_new.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.META_MASK));
        menu_file_new.setText("New");
        menu_file_new.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_file_newActionPerformed(evt);
            }
        });
        menu_file.add(menu_file_new);

        menu_file_open.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.META_MASK));
        menu_file_open.setText("Open");
        menu_file_open.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_file_openActionPerformed(evt);
            }
        });
        menu_file.add(menu_file_open);

        menu_file_save.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.META_MASK));
        menu_file_save.setText("Save");
        menu_file_save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_file_saveActionPerformed(evt);
            }
        });
        menu_file.add(menu_file_save);

        menu_file_saveAs.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.META_MASK));
        menu_file_saveAs.setText("Save As");
        menu_file_saveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_file_saveAsActionPerformed(evt);
            }
        });
        menu_file.add(menu_file_saveAs);

        menu_file_getInfo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.META_MASK));
        menu_file_getInfo.setText("Get Info");
        menu_file_getInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_file_getInfoActionPerformed(evt);
            }
        });
        menu_file.add(menu_file_getInfo);

        jMenuBar1.add(menu_file);

        menu_edit.setText("Edit");

        menu_edit_cut.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.META_MASK));
        menu_edit_cut.setText("Cut");
        menu_edit_cut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_edit_cutActionPerformed(evt);
            }
        });
        menu_edit.add(menu_edit_cut);

        menu_edit_copy.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.META_MASK));
        menu_edit_copy.setText("Copy");
        menu_edit_copy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_edit_copyActionPerformed(evt);
            }
        });
        menu_edit.add(menu_edit_copy);

        menu_edit_paste.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.META_MASK));
        menu_edit_paste.setText("Paste");
        menu_edit_paste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_edit_pasteActionPerformed(evt);
            }
        });
        menu_edit.add(menu_edit_paste);

        menu_edit_undo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.META_MASK));
        menu_edit_undo.setText("Undo");
        menu_edit_undo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_edit_undoActionPerformed(evt);
            }
        });
        menu_edit.add(menu_edit_undo);

        menu_edit_redo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.META_MASK));
        menu_edit_redo.setText("Redo");
        menu_edit_redo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_edit_redoActionPerformed(evt);
            }
        });
        menu_edit.add(menu_edit_redo);

        menu_edit_selectAll.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.META_MASK));
        menu_edit_selectAll.setText("Select All");
        menu_edit_selectAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_edit_selectAllActionPerformed(evt);
            }
        });
        menu_edit.add(menu_edit_selectAll);

        jMenuBar1.add(menu_edit);

        menu_view.setText("View");

        menu_view_source.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        menu_view_source.setText("Source");
        menu_view_source.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_view_sourceActionPerformed(evt);
            }
        });
        menu_view.add(menu_view_source);

        menu_view_listing.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0));
        menu_view_listing.setText("Listing");
        menu_view_listing.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_view_listingActionPerformed(evt);
            }
        });
        menu_view.add(menu_view_listing);

        menu_view_listingAndOutput.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0));
        menu_view_listingAndOutput.setText("Listing and Output");
        menu_view_listingAndOutput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_view_listingAndOutputActionPerformed(evt);
            }
        });
        menu_view.add(menu_view_listingAndOutput);

        menu_view_output.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, java.awt.event.InputEvent.SHIFT_MASK));
        menu_view_output.setText("Output");
        menu_view_output.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_view_outputActionPerformed(evt);
            }
        });
        menu_view.add(menu_view_output);

        jMenuBar1.add(menu_view);

        menu_run.setText("Run");

        menu_run_assemble.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0));
        menu_run_assemble.setText("Assemble");
        menu_run_assemble.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_run_assembleActionPerformed(evt);
            }
        });
        menu_run.add(menu_run_assemble);

        menu_run_run.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        menu_run_run.setText("Run");
        menu_run_run.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_run_runActionPerformed(evt);
            }
        });
        menu_run.add(menu_run_run);

        menu_run_debug.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F6, 0));
        menu_run_debug.setText("Debug");
        menu_run_debug.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_run_debugActionPerformed(evt);
            }
        });
        menu_run.add(menu_run_debug);

        menu_run_halt.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F7, 0));
        menu_run_halt.setText("Halt");
        menu_run_halt.setEnabled(false);
        menu_run_halt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_run_haltActionPerformed(evt);
            }
        });
        menu_run.add(menu_run_halt);

        jMenuBar1.add(menu_run);

        menu_help.setText("Help");

        menu_help_language.setText("Language");

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

        help_language_basics.setText("Basics");
        help_language_basics.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                help_language_basicsActionPerformed(evt);
            }
        });
        menu_help_language.add(help_language_basics);

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
            .add(tabPane)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(tabPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 587, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void menu_run_runActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_run_runActionPerformed
        if (prepareToRun()) {
            runVMOnNewThread(false);
        }
    }//GEN-LAST:event_menu_run_runActionPerformed

    private void menu_run_assembleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_run_assembleActionPerformed
        saveAndAssemble(true);
        tabPane.setSelectedIndex(LISTING_AND_OUTPUT_INDEX);
        setSplit(outListSplit, 1.0);
    }//GEN-LAST:event_menu_run_assembleActionPerformed

    private void menu_run_debugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_run_debugActionPerformed
        if (prepareToRun()) {
            runVMOnNewThread(true);
        }
    }//GEN-LAST:event_menu_run_debugActionPerformed

    private void menu_file_saveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_file_saveActionPerformed
        if (getTitle().equals(UNTITLED)) {
            menu_file_saveAsActionPerformed(evt);
        } else {
            saveAndAssemble(false);
        }
    }//GEN-LAST:event_menu_file_saveActionPerformed

    private void menu_file_openActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_file_openActionPerformed
        if (FILE_CHOOSER.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String name = removeExtension(
                    FILE_CHOOSER.getSelectedFile().getAbsolutePath());
            Editor newEditor = new Editor(name);
            if (getTitle().equals(UNTITLED) && txt.isSaved()) {
                newEditor.setLocation(getLocation());
                nextPosition.translate(-X_SHIFT, -Y_SHIFT);
                newEditor.setSize(getSize());
                dispose();
            }
            newEditor.setVisible(true);
        }
    }//GEN-LAST:event_menu_file_openActionPerformed

    private void menu_file_newActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_file_newActionPerformed
        new Editor().setVisible(true);
    }//GEN-LAST:event_menu_file_newActionPerformed

    private void menu_file_saveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_file_saveAsActionPerformed
        saveAndAssembleAsNew();
    }//GEN-LAST:event_menu_file_saveAsActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (!txt.isSaved() && getTitle().equals(UNTITLED)) {
            toFront();
            switch (JOptionPane.showOptionDialog(this, "Close without saving?",
                    getTitle(), JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null,
                    new String[]{"Save", "Close", "Cancel"}, "Save")) {
                case 0: // save
                    if (getTitle().equals(UNTITLED)) {
                        saveAndAssembleAsNew();
                    } else {
                        saveAndAssemble(false);
                    }
                    break;
                case 2: // cancel
                case JOptionPane.CLOSED_OPTION:
                    return;
            }
        }
        dispose();
    }//GEN-LAST:event_formWindowClosing

    private void menu_edit_cutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_edit_cutActionPerformed
        txt.cut();
    }//GEN-LAST:event_menu_edit_cutActionPerformed

    private void menu_edit_copyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_edit_copyActionPerformed
        txt.copy();
    }//GEN-LAST:event_menu_edit_copyActionPerformed

    private void menu_edit_pasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_edit_pasteActionPerformed
        txt.paste();
    }//GEN-LAST:event_menu_edit_pasteActionPerformed

    private void menu_edit_undoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_edit_undoActionPerformed
        if (um.canUndo()) {
            um.undo();
        }
    }//GEN-LAST:event_menu_edit_undoActionPerformed

    private void menu_edit_redoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_edit_redoActionPerformed
        if (um.canRedo()) {
            um.redo();
        }
    }//GEN-LAST:event_menu_edit_redoActionPerformed

    private void menu_edit_selectAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_edit_selectAllActionPerformed
        txt.selectAll();
    }//GEN-LAST:event_menu_edit_selectAllActionPerformed

    private void menu_run_haltActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_run_haltActionPerformed
        if (vmRunning != null) {
            vmRunning.stop();
            stopRunning();
        }
    }//GEN-LAST:event_menu_run_haltActionPerformed

    private void menu_help_language_instructionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_help_language_instructionsActionPerformed
        Help.displayInstructions();
        Help.showHelp();
    }//GEN-LAST:event_menu_help_language_instructionsActionPerformed

    private void menu_help_language_directivesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_help_language_directivesActionPerformed
        Help.displayDirectives();
        Help.showHelp();
    }//GEN-LAST:event_menu_help_language_directivesActionPerformed

    private void menu_stulinBeans_quitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_stulinBeans_quitActionPerformed
        quit();
    }//GEN-LAST:event_menu_stulinBeans_quitActionPerformed

    private void menu_file_getInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_file_getInfoActionPerformed
        info.setVisible(true);
        info.toFront();
    }//GEN-LAST:event_menu_file_getInfoActionPerformed

    private void menu_view_sourceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_view_sourceActionPerformed
        tabPane.setSelectedIndex(SOURCE_INDEX);
    }//GEN-LAST:event_menu_view_sourceActionPerformed

    private void menu_view_listingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_view_listingActionPerformed
        tabPane.setSelectedIndex(LISTING_AND_OUTPUT_INDEX);
        setSplit(outListSplit, 1.0);
    }//GEN-LAST:event_menu_view_listingActionPerformed

    private void menu_view_outputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_view_outputActionPerformed
        tabPane.setSelectedIndex(LISTING_AND_OUTPUT_INDEX);
        setSplit(outListSplit, 0.0);
    }//GEN-LAST:event_menu_view_outputActionPerformed

    private void menu_view_listingAndOutputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_view_listingAndOutputActionPerformed
        tabPane.setSelectedIndex(LISTING_AND_OUTPUT_INDEX);
        setSplit(outListSplit, 0.5);
    }//GEN-LAST:event_menu_view_listingAndOutputActionPerformed

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
//        reSplit(outListSplit);
//        reSplit(runDebugSplit);
    }//GEN-LAST:event_formComponentResized

    private void help_language_basicsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_help_language_basicsActionPerformed
        Help.showHelp();
        Help.displayBasics();
    }//GEN-LAST:event_help_language_basicsActionPerformed

    private void menu_help_example_sourceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_help_example_sourceActionPerformed
        Help.showHelp();
        Help.displaySource();
    }//GEN-LAST:event_menu_help_example_sourceActionPerformed

    private void menu_help_example_listingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_help_example_listingActionPerformed
        Help.showHelp();
        Help.displayListing();
    }//GEN-LAST:event_menu_help_example_listingActionPerformed

    private void menu_help_example_byteCodesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_help_example_byteCodesActionPerformed
        Help.showHelp();
        Help.displayByteCodes();
    }//GEN-LAST:event_menu_help_example_byteCodesActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private sap.ide.FileTextArea bin;
    private sap.ide.Console debug;
    private javax.swing.JMenuItem help_language_basics;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private sap.ide.FileTextArea lst;
    private javax.swing.JMenu menu_edit;
    private javax.swing.JMenuItem menu_edit_copy;
    private javax.swing.JMenuItem menu_edit_cut;
    private javax.swing.JMenuItem menu_edit_paste;
    private javax.swing.JMenuItem menu_edit_redo;
    private javax.swing.JMenuItem menu_edit_selectAll;
    private javax.swing.JMenuItem menu_edit_undo;
    private javax.swing.JMenu menu_file;
    private javax.swing.JMenuItem menu_file_getInfo;
    private javax.swing.JMenuItem menu_file_new;
    private javax.swing.JMenuItem menu_file_open;
    private javax.swing.JMenuItem menu_file_save;
    private javax.swing.JMenuItem menu_file_saveAs;
    private javax.swing.JMenu menu_help;
    private javax.swing.JMenu menu_help_example;
    private javax.swing.JMenuItem menu_help_example_byteCodes;
    private javax.swing.JMenuItem menu_help_example_listing;
    private javax.swing.JMenuItem menu_help_example_source;
    private javax.swing.JMenu menu_help_language;
    private javax.swing.JMenuItem menu_help_language_directives;
    private javax.swing.JMenuItem menu_help_language_instructions;
    private javax.swing.JMenu menu_run;
    private javax.swing.JMenuItem menu_run_assemble;
    private javax.swing.JMenuItem menu_run_debug;
    private javax.swing.JMenuItem menu_run_halt;
    private javax.swing.JMenuItem menu_run_run;
    private javax.swing.JMenu menu_stulinBeans;
    private javax.swing.JMenuItem menu_stulinBeans_quit;
    private javax.swing.JMenu menu_view;
    private javax.swing.JMenuItem menu_view_listing;
    private javax.swing.JMenuItem menu_view_listingAndOutput;
    private javax.swing.JMenuItem menu_view_output;
    private javax.swing.JMenuItem menu_view_source;
    private javax.swing.JSplitPane outListSplit;
    private javax.swing.JPanel outputPanel;
    private sap.ide.Console run;
    private javax.swing.JSplitPane runDebugSplit;
    private javax.swing.JTabbedPane tabPane;
    private sap.ide.FileTextArea txt;
    // End of variables declaration//GEN-END:variables
}
