package sap.ide;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 * A text console that can be displayed in a window.
 * @author Ari Zerner
 */
public final class Console extends JComponent {

    private ConsoleTextArea text;
    public final PrintStream out;
    /**
     * @deprecated Should use getInputLine() and getInputChar() instead.
     */
    public final InputStream in;

    /**
     * Makes a new Console with default colors and font.
     */
    public Console() {
        text = new ConsoleTextArea();
        out = text.out;
        in = text.in;
        setLayout(new CardLayout() {

            private boolean canAdd = true;

            @Override
            public void addLayoutComponent(Component comp, Object constraints) {
                if (canAdd) {
                    super.addLayoutComponent(comp, constraints);
                }
                canAdd = false;
            }
        });
        add(new JScrollPane(text), "consoleText");
        text.setDragEnabled(false);
    }
    private boolean canSetLM = true;

    /**
     * Does nothing.
     * @param lm 
     */
    @Override
    public final void setLayout(LayoutManager lm) {
        if (canSetLM) {
            super.setLayout(lm);
        }
        canSetLM = false;
    }

    /**
     * Reads a line of input from the console. Calls String.trim() prior to
     * returning.
     * @return the input line
     */
    public String getInputLine() {
        return text.getInputLine().trim();
    }

    /**
     * Reads a character of input from the console. Returns the same as
     * getInputLine().charAt(0), but doesn't skip past the rest of the line.
     * @return the input character
     */
    public char getInputChar() {
        return text.getInputChar();
    }

    /**
     * Clears all text from this Console.
     */
    public void clear() {
        text.clear();
    }

    /**
     * Makes a new Console with the specified colors and the default font.
     * @param bg the background color
     * @param t the text color
     */
    public Console(Color bg, Color t) {
        this();
        setBackground(bg);
        setTextColor(t);
    }

    /**
     * Makes a new Console with the default colors and the specified font.
     * @param font the font
     */
    public Console(Font font) {
        this();
        setFont(font);
    }

    /**
     * Makes a new Console with the specified colors and font.
     * @param bg the background color
     * @param t the text color
     * @param font the font
     */
    public Console(Color bg, Color t, Font font) {
        this();
        setBackground(bg);
        setTextColor(t);
        setFont(font);
    }

    @Override
    public Color getBackground() {
        return text.getBackground();
    }

    @Override
    public void setBackground(Color c) {
        text.setBackground(c);
        this.getComponent(0).setBackground(c); // scroll pane
    }

    @Override
    public Color getForeground() {
        return getTextColor();
    }

    public Color getTextColor() {
        return text.getForeground();
    }

    @Override
    public void setForeground(Color c) {
        setTextColor(c);
    }

    public void setTextColor(Color c) {
        text.setForeground(c);
        text.setCaretColor(c);
    }

    @Override
    public Font getFont() {
        return text.getFont();
    }

    @Override
    public void setFont(Font f) {
        text.setFont(f);
    }

    /**
     * Sets whether the user can type in the console.
     * @param allow true if the user should be able to type, false if not
     */
    public void allowInput(boolean allow) {
        text.allowInput(allow);
    }

    /**
     * Checks whether the user can type in the console.
     * @return true if the user can type, false otherwise
     */
    public boolean isInputAllowed() {
        return text.isInputAllowed();
    }

    private class ConsoleTextArea extends JTextArea implements KeyListener {

        private int lastOutput;
        private String inputLine;
        private boolean canInput = true;
        public final InputStream in = new InputStream() {

            @Override
            public int read() throws IOException {
                return getInputChar();
            }
        };
        public final PrintStream out = new PrintStream(new OutputStream() {

            @Override
            public void write(int i) throws IOException {
                insert("" + (char) i, lastOutput++);
                setCaretPosition(Math.min(
                        getCaretPosition() + 1, getDocument().getLength()));
            }
        });

        public ConsoleTextArea() {
            clear();
            addKeyListener(this);
        }

        private void waitForInput() {
            while (inputLine == null || inputLine.isEmpty()) {
                Thread.yield();
            }
        }

        public void allowInput(boolean allow) {
            canInput = allow;
        }

        public boolean isInputAllowed() {
            return canInput;
        }

        public String getInputLine() {
            if (inputLine != null && inputLine.trim().isEmpty()) {
                inputLine = null;
            }
            waitForInput();
            String completeInput = inputLine;
            inputLine = null;
            return completeInput;
        }

        public char getInputChar() {
            waitForInput();
            char c = inputLine.charAt(0);
            inputLine = inputLine.substring(1);
            return c;
        }

        public void clear() {
            setText("");
            lastOutput = 0;
            inputLine = null;
        }
        
        @Override
        public void cut() {
            if (getSelectionStart() < lastOutput) copy();
            else super.cut();
        }
        
        @Override
        public void paste() {
            if (getSelectionStart() < lastOutput);
            else super.paste();
        }

        @Override
        public void keyTyped(KeyEvent ke) {
            ke.consume();
        }

        @Override
        public void keyPressed(KeyEvent ke) {
            if (ke.getKeyChar() == KeyEvent.CHAR_UNDEFINED
                    || ke.isMetaDown() || ke.isControlDown()) {
                return;
            }
            if (!canInput) {
                ke.consume();
                return;
            }
            if (getSelectionStart() == getSelectionEnd())
                doKeyPressedWithoutSelection(ke);
            else
                doKeyPressedWithSelection(ke);
            ke.consume();
        }
        
        private void doKeyPressedWithSelection(KeyEvent ke) {
            int start = getSelectionStart(), end = getSelectionEnd();
            if (start < lastOutput) return;
            Document doc = getDocument();
            if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                try {
                    inputLine = getText(lastOutput,
                            doc.getLength() - lastOutput);
                    lastOutput = doc.getLength();
                } catch (BadLocationException exc) {
                }
            } else if (ke.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                replaceSelection("");
            } else {
                replaceSelection("" + ke.getKeyChar());
            }
        }
        
        private void doKeyPressedWithoutSelection(KeyEvent ke) {
            Document doc = getDocument();
            if (getCaretPosition() < lastOutput) {
                setCaretPosition(doc.getLength());
            }
            int caretPos = getCaretPosition();
            if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                try {
                    append("\n");
                    inputLine = getText(lastOutput,
                            doc.getLength() - lastOutput);
                    lastOutput = doc.getLength();
                } catch (BadLocationException exc) {
                }
            } else if (ke.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                if (getCaretPosition() > lastOutput) {
                    replaceRange("", caretPos - 1, caretPos);
                }
            } else {
                insert("" + ke.getKeyChar(), caretPos);
                setCaretPosition(++caretPos);
            }
        }

        @Override
        public void keyReleased(KeyEvent ke) {
        }
    }
}
