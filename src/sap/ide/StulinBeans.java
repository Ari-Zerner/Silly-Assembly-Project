package sap.ide;

public class StulinBeans {
    
    public static void main(String[] args) {
        if (args.length == 0)
        new Editor().setVisible(true);
        else for (String fn : args) new Editor(fn).setVisible(true);
    }
}
