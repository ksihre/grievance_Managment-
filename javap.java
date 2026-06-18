import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

// 
//  javap.java  -  GEU Online Complaint Management System
//  Features:
//    - Portal Selection Screen (Admin / Student / Register)
//    - Role-Based Login with validation
//    - Student Dashboard: Submit and track own complaints live
//    - Admin Dashboard: Full moderation with stats strip and filter
//    - Complaint Lifecycle: Pending to Accept/Reject/Block to Progress to Resolved
//    - Blocked flow: Unblock resets to Pending
//    - Admin notes pushed to student view in real-time
//    - Modern Flat UI with animated slideshow background
// 
public class javap extends JFrame {

    // Shared In-Memory Store
    static final HashMap<String, String> USER_DB    = new HashMap<>();
    static final HashMap<String, String> ROLE_DB    = new HashMap<>();
    static final ArrayList<Complaint>    COMPLAINTS = new ArrayList<>();
    static final String USER_FILE = "users.txt";

    // Colors and dimensions
    static final Color C_BG_DARK    = new Color(8,   15,  35);
    static final Color C_BG_MID     = new Color(14,  26,  60);
    static final Color C_ACCENT     = new Color(99,  179, 237);
    static final Color C_ACCENT2    = new Color(154, 117, 255);
    static final Color C_TEXT       = new Color(230, 238, 255);
    static final Color C_TEXT_DIM   = new Color(160, 175, 210);

    static final Color C_PENDING  = new Color(255, 214, 90);
    static final Color C_ACCEPTED = new Color(72,  213, 151);
    static final Color C_INPROG   = new Color(99,  179, 237);
    static final Color C_RESOLVED = new Color(72,  213, 151);
    static final Color C_REJECTED = new Color(255, 100, 100);
    static final Color C_BLOCKED  = new Color(200,  80, 200);

    // Font styles
    static final Font F_DISPLAY = new Font("Georgia",    Font.BOLD,  50);
    static final Font F_TITLE   = new Font("Georgia",    Font.BOLD,  26);
    static final Font F_HEADING = new Font("SansSerif",  Font.BOLD,  19);
    static final Font F_BODY    = new Font("SansSerif",  Font.PLAIN, 15);
    static final Font F_SMALL   = new Font("SansSerif",  Font.PLAIN, 12);
    static final Font F_MONO    = new Font("Monospaced", Font.BOLD,  14);

    // Slideshow Background
    private final String[] IMAGE_PATHS = {
        "resources/image_8.png",  "resources/image_9.png",
        "resources/image_10.png", "resources/image_11.png",
        "resources/image_12.png", "resources/image_13.png"
    };
    
    final ArrayList<BufferedImage> bgImages = new ArrayList<>();
    int bgIndex = 0;

    // Session Management
    String sessionUser;
    String sessionRole;

    // Root pane & layout timer
    BackgroundPanel root;
    Timer           slideTimer;

    // Real-time sync callback
    Runnable onComplaintsChanged = () -> {};

    static {
        USER_DB.put("admin", "1234");
        ROLE_DB.put("admin", "admin");
    }

    // ════════════════════════════════════════════════════════════════
    public javap() {
        loadImages();
        loadUsers();
        setTitle("GEU Online Complaint Management System");
        USER_DB.put("admin", "1234");
        ROLE_DB.put("admin", "admin");
        COMPLAINTS.clear();
        COMPLAINTS.addAll(FileDatabase.loadAll());
        FileDatabase.saveAll(COMPLAINTS);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        root = new BackgroundPanel();
        root.setLayout(new BorderLayout());
        setContentPane(root);

        slideTimer = new Timer(4000, e -> {
            if (!bgImages.isEmpty()) { bgIndex = (bgIndex + 1) % bgImages.size(); root.repaint(); }
        });
        slideTimer.start();

        showPortalSelection();
        setVisible(true);
    }

    void saveUsers() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USER_FILE))) {
            for (String user : USER_DB.keySet()) {
                bw.write(user + "|" + USER_DB.get(user));
                bw.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void loadUsers() {
        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    USER_DB.put(parts[0], parts[1]);
                    ROLE_DB.put(parts[0], "student");   
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    void loadImages() {
        for (String p : IMAGE_PATHS) {
            try { File f = new File(p); if (f.exists()) bgImages.add(ImageIO.read(f)); }
            catch (IOException ignored) {}
        }
    }
    
    // ════════════════════════════════════════════════════════════════
    //  SCREEN 1 — PORTAL SELECTION
    // ════════════════════════════════════════════════════════════════
    void showPortalSelection() {
        root.removeAll();
        root.setLayout(new GridBagLayout());

        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setOpaque(false);

        JLabel title = lbl("GEU Complaint Portal", F_DISPLAY, C_TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = lbl("Select your dashboard to continue", F_BODY, C_TEXT_DIM);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        wrap.add(Box.createVerticalStrut(10));
        wrap.add(title);
        wrap.add(Box.createVerticalStrut(8));
        wrap.add(sub);
        wrap.add(Box.createVerticalStrut(48));

        JPanel cards = new JPanel(new FlowLayout(FlowLayout.CENTER, 36, 0));
        cards.setOpaque(false);

        cards.add(portalCard("⚙",  "Admin Portal",
            "Manage all complaints,\nmoderate users,\nview analytics.",
            C_ACCENT2, () -> showLoginScreen("admin")));

        cards.add(portalCard("🎓", "Student Portal",
            "Submit complaints\nand track their\nlive status.",
            C_ACCENT, () -> showLoginScreen("student")));

        cards.add(portalCard("✎",  "Register",
            "New student?\nCreate your\naccount here.",
            C_ACCEPTED, this::showRegisterScreen));

        wrap.add(cards);
        root.add(wrap);
        root.revalidate();
        root.repaint();
    }

    JPanel portalCard(String icon, String title, String desc, Color accent, Runnable action) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(C_BG_MID);
        card.setOpaque(true);
        card.setPreferredSize(new Dimension(250, 270));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent, 1),
            new EmptyBorder(32, 28, 28, 28)
        ));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { action.run(); }
        });

        JLabel ico = lbl(icon, new Font("SansSerif", Font.PLAIN, 46), Color.WHITE);
        ico.setHorizontalAlignment(JLabel.CENTER);
        ico.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel ttl = lbl(title, F_TITLE, C_TEXT);
        ttl.setHorizontalAlignment(JLabel.CENTER);

        JLabel dsc = new JLabel("<html><center>" + desc.replace("\n","<br>") + "</center></html>");
        dsc.setFont(F_BODY); dsc.setForeground(C_TEXT_DIM);
        dsc.setHorizontalAlignment(JLabel.CENTER);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);
        inner.add(ico);
        inner.add(Box.createVerticalStrut(12));
        inner.add(ttl);
        inner.add(Box.createVerticalStrut(8));
        inner.add(dsc);
        card.add(inner, BorderLayout.CENTER);

        JButton btn = accentBtn("Enter →", accent);
        btn.addActionListener(e -> action.run());
        card.add(btn, BorderLayout.SOUTH);
        return card;
    }

    //  SCREEN 2 - LOGIN
    void showLoginScreen(String expectedRole) {
        root.removeAll();
        root.setLayout(new GridBagLayout());

        JPanel box = new JPanel(new GridBagLayout());
        box.setBackground(C_BG_MID);
        box.setOpaque(true);
        box.setPreferredSize(new Dimension(500, 420));
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_ACCENT, 1),
            new EmptyBorder(44, 50, 44, 50)
        ));

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.weightx = 1;

        String rl = "admin".equals(expectedRole) ? "Admin" : "Student";
        JLabel hdr = lbl(rl + " Login", F_TITLE, C_TEXT);
        hdr.setHorizontalAlignment(JLabel.CENTER);
        gc.gridy = 0; gc.insets = new Insets(0,0,22,0); box.add(hdr, gc);

        JTextField     userF = styledField();
        JPasswordField passF = styledPass();

        gc.insets = new Insets(5,0,5,0);
        gc.gridy = 1; box.add(fieldLbl("Username"), gc);
        gc.gridy = 2; box.add(userF, gc);
        gc.gridy = 3; box.add(fieldLbl("Password"), gc);
        gc.gridy = 4; box.add(passF, gc);

        gc.gridy = 5; gc.insets = new Insets(20,0,8,0);
        JButton loginBtn = accentBtn("LOGIN", C_ACCENT);
        loginBtn.setPreferredSize(new Dimension(380, 46));
        box.add(loginBtn, gc);

        gc.gridy = 6; gc.insets = new Insets(2,0,0,0);
        JButton backBtn = ghostBtn("← Back to Portal");
        box.add(backBtn, gc);
        
        loginBtn.addActionListener(e -> {
            String u = userF.getText().trim();
            String p = new String(passF.getPassword());

            if (!USER_DB.containsKey(u)) {
                JOptionPane.showMessageDialog(this, "User not registered. Please register first.");
                return;
            }

            if (!USER_DB.get(u).equals(p)) {
                shake(box);
                JOptionPane.showMessageDialog(this, "Invalid credentials.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String role = ROLE_DB.getOrDefault(u, "student");
            sessionUser = u;
            sessionRole = role;

            if ("admin".equals(role)) showAdminDashboard();
            else showStudentDashboard();
        });
        backBtn.addActionListener(e -> showPortalSelection());

        root.add(box);
        root.revalidate(); root.repaint();
    }

    //  SCREEN 3 - REGISTER
    void showRegisterScreen() {
        root.removeAll();
        root.setLayout(new GridBagLayout());

        JPanel box = new JPanel(new GridBagLayout());
        box.setBackground(C_BG_MID);
        box.setOpaque(true);
        box.setPreferredSize(new Dimension(500, 460));
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_ACCEPTED, 1),
            new EmptyBorder(44, 50, 44, 50)
        ));

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.weightx = 1;

        JLabel hdr = lbl("Create Account", F_TITLE, C_TEXT);
        hdr.setHorizontalAlignment(JLabel.CENTER);
        gc.gridy = 0; gc.insets = new Insets(0,0,22,0); box.add(hdr, gc);

        JTextField     userF  = styledField();
        JPasswordField passF  = styledPass();
        JPasswordField pass2F = styledPass();

        gc.insets = new Insets(5,0,5,0);
        gc.gridy = 1; box.add(fieldLbl("Username"), gc);
        gc.gridy = 2; box.add(userF, gc);
        gc.gridy = 3; box.add(fieldLbl("Password"), gc);
        gc.gridy = 4; box.add(passF, gc);
        gc.gridy = 5; box.add(fieldLbl("Confirm Password"), gc);
        gc.gridy = 6; box.add(pass2F, gc);

        gc.gridy = 7; gc.insets = new Insets(20,0,8,0);
        JButton regBtn = accentBtn("REGISTER", C_ACCEPTED);
        regBtn.setPreferredSize(new Dimension(380, 46));
        box.add(regBtn, gc);

        gc.gridy = 8; gc.insets = new Insets(2,0,0,0);
        JButton backBtn = ghostBtn("← Back to Portal");
        box.add(backBtn, gc);

        regBtn.addActionListener(e -> {
            String u  = userF.getText().trim();
            String p  = new String(passF.getPassword());
            String p2 = new String(pass2F.getPassword());
            if (u.isEmpty() || p.isEmpty()) { JOptionPane.showMessageDialog(this, "All fields are required."); return; }
            if (!p.equals(p2))             { JOptionPane.showMessageDialog(this, "Passwords do not match.");   return; }
            if (USER_DB.containsKey(u))    { JOptionPane.showMessageDialog(this, "Username already exists!"); return; }
            USER_DB.put(u, p); ROLE_DB.put(u, "student"); saveUsers();
            JOptionPane.showMessageDialog(this, "✔ Registration successful! Please login.");
            showLoginScreen("student");
        });
        backBtn.addActionListener(e -> showPortalSelection());

        root.add(box);
        root.revalidate(); root.repaint();
    }

    //  SCREEN 4 - STUDENT DASHBOARD
    void showStudentDashboard() {
        root.removeAll();
        root.setLayout(new BorderLayout());
        root.add(topBar("Student Dashboard  —  " + sessionUser.toUpperCase(),
                        C_ACCENT, () -> { sessionUser=null; showPortalSelection(); }), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setOpaque(false); split.setBorder(null);
        split.setDividerLocation(0.46); split.setResizeWeight(0.46);

        // Left Panel: Form Submission
        JPanel leftPanel = new JPanel(new BorderLayout(0, 14));
        leftPanel.setBackground(C_BG_MID);
        leftPanel.setOpaque(true);
        leftPanel.setBorder(new EmptyBorder(28, 36, 28, 18));
        JLabel subTitle = lbl("Submit a Complaint  |  Your Total: " + countUserComplaints(sessionUser), F_HEADING, C_TEXT);
        
        JTextArea area = new JTextArea(6, 20);
        styleTA(area);
        JScrollPane areaScroll = glassScroll(area);

        String[] cats = {"Academic","Hostel","Library","Canteen","Infrastructure","Other"};
        JComboBox<String> catBox = styledCombo(cats);
        JButton submitBtn = accentBtn("SUBMIT COMPLAINT", C_ACCENT);
        submitBtn.setPreferredSize(new Dimension(280, 44));
        submitBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.add(fieldLbl("Description"));  form.add(Box.createVerticalStrut(6));
        form.add(areaScroll);               form.add(Box.createVerticalStrut(12));
        form.add(fieldLbl("Category"));     form.add(Box.createVerticalStrut(5));
        form.add(catBox);                   form.add(Box.createVerticalStrut(12));
        form.add(submitBtn);

        leftPanel.add(subTitle, BorderLayout.NORTH);
        leftPanel.add(form,     BorderLayout.CENTER);

        // Right Panel: Tracking Board
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(new EmptyBorder(28, 18, 28, 36));

        JLabel boardTitle = lbl("My Complaints", F_HEADING, C_TEXT);

        JPanel listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);

        JScrollPane listScroll = glassScroll(listContainer);

        rightPanel.add(boardTitle, BorderLayout.NORTH);
        rightPanel.add(listScroll, BorderLayout.CENTER);

        split.setLeftComponent(leftPanel);
        split.setRightComponent(rightPanel);
        root.add(split, BorderLayout.CENTER);

        Runnable refreshStudent = () -> {
            listContainer.removeAll();
            List<Complaint> mine = COMPLAINTS.stream()
                .filter(c -> c.getUser().equals(sessionUser))
                .collect(Collectors.toList());

            if (mine.isEmpty()) {
                JLabel e = lbl("No complaints yet. Submit one!", F_BODY, C_TEXT_DIM);
                e.setAlignmentX(Component.CENTER_ALIGNMENT);
                listContainer.add(Box.createVerticalStrut(40));
                listContainer.add(e);
            }
            for (int i = mine.size()-1; i >= 0; i--) {
                listContainer.add(studentCard(mine.get(i)));
                listContainer.add(Box.createRigidArea(new Dimension(0, 10)));
            }
            listContainer.revalidate(); listContainer.repaint();
        };

        onComplaintsChanged = refreshStudent;
        refreshStudent.run();

        submitBtn.addActionListener(e -> {
            String txt = area.getText().trim();
            if (txt.isEmpty()) { JOptionPane.showMessageDialog(this,"Please describe your complaint."); return; }
            String category = (String) catBox.getSelectedItem();
            Complaint c = new Complaint(sessionUser, txt, category);

            COMPLAINTS.add(c);
            FileDatabase.saveAll(COMPLAINTS);  
            area.setText("");
            JOptionPane.showMessageDialog(this, "✔ Complaint #" + c.getId() + " submitted!");
            refreshStudent.run();
        });

        root.revalidate(); root.repaint();
    }

    JPanel studentCard(Complaint c) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(C_BG_MID);
        card.setOpaque(true);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel topRow = new JPanel(new BorderLayout()); topRow.setOpaque(false);
        topRow.add(lbl("#" + c.getId() + "  [" + c.getCategory() + "]", F_SMALL, C_TEXT_DIM), BorderLayout.WEST);
        topRow.add(statusBadge(c.getStatus()), BorderLayout.EAST);

        String prev = c.getText().length()>85 ? c.getText().substring(0,82)+"..." : c.getText();
        JLabel body = lbl(prev, F_BODY, C_TEXT);

        JPanel bottom = new JPanel(new BorderLayout()); bottom.setOpaque(false);
        if (c.getAdminNote()!=null && !c.getAdminNote().isEmpty())
            bottom.add(lbl("Admin note: "+c.getAdminNote(), F_SMALL, new Color(160,210,255)), BorderLayout.WEST);

        card.add(topRow, BorderLayout.NORTH);
        card.add(body,   BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    int countUserComplaints(String username) {
        int count = 0;
        for(Complaint c : COMPLAINTS) {
            if(c.getUser().equals(username)) {
                count++;
            }
        }
        return count;
    }

    //  SCREEN 5 - ADMIN DASHBOARD
    void showAdminDashboard() {
        root.removeAll();
        root.setLayout(new BorderLayout());

        JPanel northWrapper = new JPanel(new BorderLayout());
        northWrapper.setOpaque(false);

        JPanel top = topBar(
            "Admin Dashboard — " + sessionUser.toUpperCase(),
            C_ACCENT2,
            () -> { sessionUser = null; showPortalSelection(); }
        );

        JPanel northArea = new JPanel(new BorderLayout());
        northArea.setOpaque(false);

        northWrapper.add(top, BorderLayout.NORTH);
        northWrapper.add(northArea, BorderLayout.CENTER);

        root.add(northWrapper, BorderLayout.NORTH);

        JPanel tableContainer = new JPanel();
        tableContainer.setLayout(new BoxLayout(tableContainer, BoxLayout.Y_AXIS));
        tableContainer.setOpaque(false);

        JScrollPane tableScroll = glassScroll(tableContainer);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(0, 22, 22, 22));
        center.add(tableScroll, BorderLayout.CENTER);

        root.add(center, BorderLayout.CENTER);

        String[] filterState = {"ALL"};
        final Runnable[] refreshAdmin = new Runnable[1];

        refreshAdmin[0] = () -> {
            northArea.removeAll();
            northArea.add(buildStatsStrip(), BorderLayout.NORTH);

            JPanel fb = buildFilterBar(filterState, () -> refreshAdmin[0].run());
            northArea.add(fb, BorderLayout.SOUTH);

            tableContainer.removeAll();
            tableContainer.add(adminTableHeader());
            tableContainer.add(Box.createRigidArea(new Dimension(0, 5)));

            List<Complaint> shown = COMPLAINTS.stream()
                .filter(c -> "ALL".equals(filterState[0]) || filterState[0].equals(c.getStatus()))
                .sorted((a, b) -> b.getId() - a.getId())
                .collect(Collectors.toList());

            if (shown.isEmpty()) {
                JLabel none = lbl("No complaints match this filter.", F_BODY, C_TEXT_DIM);
                none.setAlignmentX(Component.CENTER_ALIGNMENT);
                tableContainer.add(Box.createVerticalStrut(28));
                tableContainer.add(none);
            }

            for (Complaint c : shown) {
                tableContainer.add(adminRow(c, () -> {
                    refreshAdmin[0].run();
                    onComplaintsChanged.run();
                }));
                tableContainer.add(Box.createRigidArea(new Dimension(0, 7)));
            }

            tableContainer.revalidate();
            tableContainer.repaint();
            northArea.revalidate();
            northArea.repaint();
        };

        refreshAdmin[0].run();
        root.revalidate();
        root.repaint();
    }

    JPanel buildFilterBar(String[] filterState, Runnable onFilter) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 22, 4, 22));
        bar.add(lbl("Filter: ", F_SMALL, C_TEXT_DIM));

        String[] filters = {"ALL","Pending","Accepted","In Progress","Resolved","Rejected","Blocked"};
        Color[]  fClrs   = {C_ACCENT, C_PENDING, C_ACCEPTED, C_INPROG, C_RESOLVED, C_REJECTED, C_BLOCKED};

        for (int i = 0; i < filters.length; i++) {
            final String f = filters[i];
            final Color  c = fClrs[i];
            JButton btn = filterChip(f, c);
            btn.addActionListener(e -> { filterState[0] = f; onFilter.run(); });
            bar.add(btn);
        }
        return bar;
    }

    JPanel buildStatsStrip() {
        long total    = COMPLAINTS.size();
        long pending  = count("Pending");
        long accepted = count("Accepted");
        long inProg   = count("In Progress");
        long resolved = count("Resolved");
        long rejected = count("Rejected");
        long blocked  = count("Blocked");

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        bar.setOpaque(false);

        bar.add(statTile("Total",       total,    new Color(120,140,200)));
        bar.add(statTile("Pending",     pending,  C_PENDING));
        bar.add(statTile("Accepted",    accepted, C_ACCEPTED));
        bar.add(statTile("In Progress", inProg,   C_INPROG));
        bar.add(statTile("Resolved",    resolved, C_RESOLVED));
        bar.add(statTile("Rejected",    rejected, C_REJECTED));
        bar.add(statTile("Blocked",     blocked,  C_BLOCKED));
        return bar;
    }

    long count(String status) {
        return COMPLAINTS.stream().filter(c -> status.equals(c.getStatus())).count();
    }

    JPanel statTile(String name, long cnt, Color bg) {
        JPanel tile = new JPanel(new GridLayout(2,1));
        tile.setBackground(bg);
        tile.setOpaque(true);
        tile.setPreferredSize(new Dimension(124, 58));
        tile.setBorder(new EmptyBorder(5, 10, 5, 10));
        JLabel num = lbl(String.valueOf(cnt), new Font("Georgia", Font.BOLD, 24), Color.WHITE);
        num.setHorizontalAlignment(JLabel.CENTER);
        JLabel nm  = lbl(name, F_SMALL, new Color(210,220,240));
        nm.setHorizontalAlignment(JLabel.CENTER);
        tile.add(num); tile.add(nm);
        return tile;
    }

    JPanel adminTableHeader() {
        JPanel row = new JPanel(new GridLayout(1, 6));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        row.setOpaque(false);

        String[] cols = {"ID","User","Category","Status","Complaint","Actions"};

        for (String col : cols) {
            JLabel h = new JLabel(col, JLabel.CENTER);
            h.setFont(F_SMALL);
            h.setForeground(new Color(140,170,230));

            JPanel p = new JPanel(new BorderLayout());
            p.setOpaque(false);
            p.add(h, BorderLayout.CENTER);
            row.add(p);
        }
        return row;
    }

    JPanel adminRow(Complaint c, Runnable onAction) {
        JPanel row = new JPanel(new GridLayout(1, 6));
        row.setBackground(C_BG_MID);
        row.setOpaque(true);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        row.setBorder(new EmptyBorder(4, 4, 4, 4));

        // ID
        JLabel id = new JLabel("#" + c.getId(), JLabel.CENTER);
        id.setFont(F_MONO);
        id.setForeground(new Color(230, 240, 255));
        row.add(id);

        // USER
        JLabel user = new JLabel(c.getUser(), JLabel.CENTER);
        user.setFont(F_BODY);
        user.setForeground(new Color(230, 240, 255));
        row.add(user);

        // CATEGORY
        JLabel cat = new JLabel(c.getCategory(), JLabel.CENTER);
        cat.setFont(F_SMALL);
        cat.setForeground(new Color(200, 215, 240));
        row.add(cat);

        // STATUS
        JPanel statusPanel = new JPanel(new GridBagLayout());
        statusPanel.setOpaque(false);
        statusPanel.add(statusBadge(c.getStatus()));
        row.add(statusPanel);

        // COMPLAINT PREVIEW
        String prev = c.getText().length() > 65
            ? c.getText().substring(0, 62) + "..."
            : c.getText();

        JLabel txt = new JLabel(prev, JLabel.LEFT);
        txt.setFont(F_BODY);
        txt.setForeground(new Color(230, 240, 255));
        row.add(txt);

        // ACTIONS
        row.add(adminActions(c, onAction));

        return row;
    }

    JPanel adminActions(Complaint c, Runnable refresh) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        p.setOpaque(false);

        switch (c.getStatus()) {
            case "Pending":
                p.add(aBtn("✔ Accept",   new Color(72,213,151), e -> { c.setStatus("Accepted");   notePrompt(c); FileDatabase.saveAll(COMPLAINTS); refresh.run(); }));
                p.add(aBtn("✘ Reject",   new Color(220,70,70),  e -> { if(confirm("Reject",c))  { c.setStatus("Rejected");   notePrompt(c); FileDatabase.saveAll(COMPLAINTS); refresh.run(); }}));
                p.add(aBtn("⊘ Block",    new Color(180,60,180), e -> { if(confirm("Block",c))   { c.setStatus("Blocked");    notePrompt(c); FileDatabase.saveAll(COMPLAINTS); refresh.run(); }}));
                break;
            case "Accepted":
                p.add(aBtn("▶ Progress", new Color(99,179,237), e -> { c.setStatus("In Progress"); FileDatabase.saveAll(COMPLAINTS); refresh.run(); }));
                p.add(aBtn("⊘ Block",    new Color(180,60,180), e -> { if(confirm("Block",c))   { c.setStatus("Blocked");    notePrompt(c); FileDatabase.saveAll(COMPLAINTS); refresh.run(); }}));
                break;
            case "In Progress":
                p.add(aBtn("✔ Resolve",  new Color(72,213,151), e -> { c.setStatus("Resolved");   notePrompt(c); FileDatabase.saveAll(COMPLAINTS); refresh.run(); }));
                p.add(aBtn("⊘ Block",    new Color(180,60,180), e -> { if(confirm("Block",c))   { c.setStatus("Blocked");    notePrompt(c); FileDatabase.saveAll(COMPLAINTS); refresh.run(); }}));
                break;
            case "Blocked":
                p.add(aBtn("↺ Unblock",  new Color(200,175,50), e -> { c.setStatus("Pending"); c.setAdminNote(""); FileDatabase.saveAll(COMPLAINTS); refresh.run(); }));
                break;
            case "Resolved":
                p.add(lbl("✔ Closed", F_SMALL, new Color(72,213,151))); break;
            case "Rejected":
                p.add(lbl("✘ Closed", F_SMALL, new Color(220,70,70)));  break;
        }
        return p;
    }

    boolean confirm(String action, Complaint c) {
        return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
            action + " complaint #" + c.getId() + "?",
            "Confirm " + action, JOptionPane.YES_NO_OPTION);
    }

    void notePrompt(Complaint c) {
        String n = JOptionPane.showInputDialog(this,
            "Leave a note for the student (optional):", "Admin Note", JOptionPane.PLAIN_MESSAGE);
        if (n != null && !n.trim().isEmpty()) c.setAdminNote(n.trim());
    }

    JPanel topBar(String title, Color accent, Runnable logout) {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0,0,0,140));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(accent);
                g2.fillRect(0, getHeight()-2, getWidth(), 2);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(13, 26, 13, 26));
        bar.add(lbl(title, F_HEADING, C_TEXT), BorderLayout.WEST);

        JButton logoutBtn = accentBtn("LOGOUT", new Color(255, 70, 70));
        logoutBtn.setPreferredSize(new Dimension(140, 40));
        logoutBtn.setFont(new Font("SansSerif", Font.BOLD, 14));

        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?",
                "Logout",
                JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                logout.run();
            }
        });

        bar.add(logoutBtn, BorderLayout.EAST);
        return bar;
    }

    // Handles painting the slideshow background
    class BackgroundPanel extends JPanel {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            if (!bgImages.isEmpty()) {
                g2.drawImage(bgImages.get(bgIndex), 0, 0, getWidth(), getHeight(), this);
                g2.setColor(new Color(5, 12, 35, 175));
                g2.fillRect(0, 0, getWidth(), getHeight());
            } else {
                g2.setPaint(new GradientPaint(0, 0, C_BG_DARK, getWidth(), getHeight(), C_BG_MID));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();
        }
    }

    // Component Factories
    static JLabel lbl(String t, Font f, Color c)  { JLabel l=new JLabel(t); l.setFont(f); l.setForeground(c); return l; }
    static JLabel fieldLbl(String t) { return lbl(t, F_SMALL, C_TEXT_DIM); }

    static JTextField styledField() {
        JTextField f = new JTextField();
        f.setFont(F_BODY); f.setForeground(Color.WHITE); f.setCaretColor(Color.WHITE);
        f.setBackground(new Color(18, 26, 58));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        f.setPreferredSize(new Dimension(380, 40));
        return f;
    }

    static JPasswordField styledPass() {
        JPasswordField f = new JPasswordField();
        f.setFont(F_BODY); f.setForeground(Color.WHITE); f.setCaretColor(Color.WHITE);
        f.setBackground(new Color(18, 26, 58));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        f.setPreferredSize(new Dimension(380, 40));
        return f;
    }

    static void styleTA(JTextArea a) {
        a.setFont(F_BODY); a.setForeground(Color.WHITE); a.setCaretColor(Color.WHITE);
        a.setBackground(new Color(18, 26, 58));
        a.setBorder(new EmptyBorder(10, 12, 10, 12));
        a.setLineWrap(true); a.setWrapStyleWord(true);
    }

    static JScrollPane glassScroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setOpaque(false); sp.getViewport().setOpaque(false);
        sp.setBorder(null); sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    static JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(F_BODY); cb.setForeground(C_TEXT);
        cb.setBackground(new Color(28, 38, 78));
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return cb;
    }

    static JButton accentBtn(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setForeground(Color.WHITE); b.setFont(F_HEADING);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JButton ghostBtn(String text) {
        JButton b = new JButton(text);
        b.setForeground(C_TEXT_DIM); b.setFont(F_BODY);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JButton aBtn(String text, Color bg, ActionListener al) {
        JButton b = accentBtn(text, bg);
        b.setFont(F_SMALL); b.setPreferredSize(new Dimension(106, 30));
        b.addActionListener(al); return b;
    }

    static JButton filterChip(String text, Color accent) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setForeground(Color.WHITE); b.setFont(F_SMALL);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(102, 26));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JLabel statusBadge(String status) {
        JLabel l = new JLabel(status, JLabel.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Color sc = statusColor(getText());
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(sc);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose(); super.paintComponent(g);
            }
        };
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(Color.BLACK); l.setOpaque(false);
        l.setBorder(new EmptyBorder(3, 8, 3, 8));
        l.setPreferredSize(new Dimension(96, 22));
        return l;
    }

    static Color statusColor(String s) {
        switch (s) {
            case "Pending":     return C_PENDING;
            case "Accepted":    return C_ACCEPTED;
            case "In Progress": return C_INPROG;
            case "Resolved":    return C_RESOLVED;
            case "Rejected":    return C_REJECTED;
            case "Blocked":     return C_BLOCKED;
            default:            return Color.WHITE;
        }
    }

    void shake(Component c) {
        Point orig = c.getLocation();
        int[] offs = {10,-10,8,-8,5,-5,2,-2,0};
        int[] i = {0};
        Timer t = new Timer(28, null);
        t.addActionListener(e -> {
            if (i[0] >= offs.length) { c.setLocation(orig); t.stop(); return; }
            c.setLocation(orig.x + offs[i[0]++], orig.y);
        });
        t.start();
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(javap::new);
    }
}