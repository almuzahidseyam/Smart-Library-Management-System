import java.awt.*;
import java.nio.file.*;
import java.util.ArrayList;
import javax.swing.*;

public class LoginFlowTest {
    static Login frame;
    static java.util.List<Component> descendants(Container root) {
        var result=new ArrayList<Component>();
        for(Component item:root.getComponents()) { result.add(item); if(item instanceof Container child) result.addAll(descendants(child)); }
        return result;
    }
    static void click(String label) {
        descendants(frame.getContentPane()).stream().filter(c -> c instanceof JButton b && b.getText().equals(label)).map(c -> (JButton)c).findFirst().orElseThrow().doClick();
    }
    static void waitFor(String text) throws Exception {
        for(int i=0;i<100;i++) {
            boolean[] found={false};
            SwingUtilities.invokeAndWait(() -> found[0]=descendants(frame.getContentPane()).stream().anyMatch(c -> c instanceof JLabel l && l.getText().equals(text)));
            if(found[0]) return;
            Thread.sleep(100);
        }
        throw new AssertionError("UI did not reach: "+text);
    }
    public static void main(String[] args) throws Exception {
        Path home=Files.createTempDirectory("library-ui-");
        System.setProperty("user.home",home.toString());
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame=new Login();
                var fields=descendants(frame.getContentPane()).stream().filter(c -> c instanceof JTextField).map(c -> (JTextField)c).toList();
                fields.get(0).setText("ui_reader"); fields.get(1).setText("UI Reader"); fields.get(2).setText("testing password only");
                click("Create account");
            });
            waitFor("Account created. You can now sign in.");
            SwingUtilities.invokeAndWait(() -> {
                descendants(frame.getContentPane()).stream().filter(c -> c instanceof JPasswordField).map(c -> (JPasswordField)c).forEach(c -> c.setText("testing password only"));
                click("Sign in");
            });
            waitFor("Local session active.");
            SwingUtilities.invokeAndWait(() -> {
                boolean named=descendants(frame.getContentPane()).stream().anyMatch(c -> c instanceof JTextArea t && t.getText().contains("UI Reader"));
                if(!named) throw new AssertionError("No authenticated user shown");
                click("Sign out");
                if(frame.isDisplayable()) throw new AssertionError("Old session remains open");
                for(Window window:Window.getWindows()) window.dispose();
            });
            System.out.println("Swing registration, sign-in and sign-out passed.");
        } finally {
            SwingUtilities.invokeAndWait(() -> { for(Window window:Window.getWindows()) window.dispose(); });
            try(var paths=Files.walk(home)) { for(Path path:paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path); }
        }
    }
}
