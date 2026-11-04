import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

/** Small, responsive account interface; expensive password work runs off the UI thread. */
public class Login extends JFrame {
    private final Accounts accounts = new Accounts(JavaConnect.databasePath());
    private final JTextField user = new JTextField(24), name = new JTextField(24);
    private final JPasswordField password = new JPasswordField(24);
    private final JButton login = new JButton("Sign in"), signup = new JButton("Create account");
    private final JLabel status = new JLabel("Accounts stay on this computer.");
    private final JPanel fields = new JPanel(new GridLayout(0,1,0,8));
    private final JLabel heading = new JLabel("Smart Library");
    public Login() {
        super("Smart Library · Account prototype");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel page = new JPanel(new BorderLayout(0,24));
        page.setBorder(BorderFactory.createEmptyBorder(28,32,28,32));
        heading.setFont(heading.getFont().deriveFont(Font.BOLD,26f));
        page.add(heading,BorderLayout.NORTH);
        addField("Username",user); addField("Display name · for new accounts",name); addField("Password · 12–128 characters",password);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        actions.add(login); actions.add(Box.createHorizontalStrut(12)); actions.add(signup);
        fields.add(actions);
        JButton help=new JButton("Account help");
        help.addActionListener(event -> JOptionPane.showMessageDialog(this,"This local prototype has no password recovery service.\nPasswords are hashed and cannot be displayed.\nCreate another account if needed; existing accounts are preserved.\nBook catalog and lending features are not implemented.","Account help",JOptionPane.INFORMATION_MESSAGE));
        fields.add(help);
        page.add(fields,BorderLayout.CENTER); page.add(status,BorderLayout.SOUTH);
        login.addActionListener(event -> submit(false)); signup.addActionListener(event -> submit(true));
        getRootPane().setDefaultButton(login);
        setContentPane(page); pack(); setMinimumSize(getSize()); setLocationRelativeTo(null);
    }
    private void addField(String text,JTextField field) {
        JLabel label=new JLabel(text); label.setLabelFor(field); fields.add(label); fields.add(field);
    }
    private void submit(boolean create) {
        String username=user.getText(), displayName=name.getText(); char[] secret=password.getPassword();
        login.setEnabled(false); signup.setEnabled(false); password.setText(""); status.setText("Working…");
        new SwingWorker<String,Void>() {
            protected String doInBackground() throws Exception {
                try {
                    if (create) { accounts.register(username,displayName,secret); return null; }
                    return accounts.login(username,secret).orElseThrow(() -> new IllegalArgumentException("Username or password is incorrect."));
                } finally { Arrays.fill(secret,'\0'); }
            }
            protected void done() {
                login.setEnabled(true); signup.setEnabled(true);
                try {
                    String signedIn=get();
                    if (create) { status.setText("Account created. You can now sign in."); password.requestFocusInWindow(); }
                    else showSession(signedIn);
                } catch (Exception error) {
                    Throwable cause=error.getCause();
                    status.setText("Could not complete the request.");
                    JOptionPane.showMessageDialog(Login.this,cause instanceof IllegalArgumentException ? cause.getMessage() : "The local account store is unavailable. Check the database path and permissions.","Account",JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }
    private void showSession(String displayName) {
        fields.removeAll();
        // JTextArea displays names as text, never Swing HTML.
        JTextArea welcome=new JTextArea("Signed in as " + displayName + "\n\nAccount prototype\nBook catalog and lending features are not implemented.");
        welcome.setEditable(false); welcome.setOpaque(false); welcome.setLineWrap(true); welcome.setWrapStyleWord(true);
        fields.add(welcome);
        JButton logout=new JButton("Sign out");
        logout.addActionListener(event -> { dispose(); new Login().setVisible(true); });
        fields.add(logout); status.setText("Local session active.");
        fields.revalidate(); fields.repaint();
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> { try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {} new Login().setVisible(true); });
    }
}
