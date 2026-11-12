package tsp;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Main GUI for the Smart Delivery Route Optimizer.
 * Layout matches the reference screenshot: purple gradient header,
 * distance-matrix grid, result panel, and history table.
 */
public class TSPMainGUI extends JFrame {

    // ── Colours (matching the screenshot palette) ────────────────────────────
    private static final Color BG_WHITE      = Color.WHITE;
    private static final Color HEADER_TOP    = new Color(90,  60, 170);
    private static final Color HEADER_BOT    = new Color(130, 90, 200);
    private static final Color ACCENT        = new Color(99,  91, 255);
    private static final Color BTN_PURPLE    = new Color(99,  91, 255);
    private static final Color BTN_HOVER     = new Color(79,  71, 235);
    private static final Color TABLE_HEADER  = new Color(248, 248, 252);
    private static final Color TABLE_DIAG    = new Color(240, 240, 248);
    private static final Color BORDER_LIGHT  = new Color(220, 220, 235);
    private static final Color TEXT_DARK     = new Color(30,  30,  50);
    private static final Color TEXT_SUB      = new Color(100,100,120);
    private static final Color SUCCESS_GREEN = new Color(34, 197, 94);
    private static final Color ERROR_RED     = new Color(239, 68, 68);

    // ── State ────────────────────────────────────────────────────────────────
    private int numNodes = 4;
    private JTextField[][] distFields;
    private JPanel matrixPanel;
    private JPanel resultPanel;
    private JLabel resultPathLabel;
    private JLabel resultCostLabel;
    private JLabel resultTimeLabel;
    private JLabel dbStatusLabel;
    private JTable historyTable;
    private DefaultTableModel historyModel;
    private DatabaseManager db;

    // ── Sample data (matches screenshot) ────────────────────────────────────
    private static final int[][] SAMPLE_4 = {
        {0, 5, 9, 10},
        {5, 0, 6,  7},
        {9, 6, 0,  8},
        {10,7, 8,  0}
    };

    // ── Constructor ──────────────────────────────────────────────────────────
    public TSPMainGUI() {
        setTitle("Smart Delivery Route Optimizer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1050, 820);
        setMinimumSize(new Dimension(900, 700));
        setLocationRelativeTo(null);
        setBackground(new Color(245, 245, 250));

        // Try DB connection (non-fatal if unavailable)
        tryConnectDB();

        buildUI();
        buildMatrix(numNodes);
        loadSampleData();
        setVisible(true);
    }

    // ── Database ─────────────────────────────────────────────────────────────
    private void tryConnectDB() {
        try {
            db = new DatabaseManager();
        } catch (TSPException e) {
            db = null; // GUI still works without DB
        }
    }

    // ── Top-level layout ─────────────────────────────────────────────────────
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(245, 245, 250));

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);

        setContentPane(root);
    }

    // ── Header panel (purple gradient) ───────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, HEADER_TOP, getWidth(), getHeight(), HEADER_BOT);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setPreferredSize(new Dimension(0, 110));
        header.setBorder(new EmptyBorder(18, 30, 18, 30));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("🚚  Smart Delivery Route Optimizer");
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Solve the Traveling Salesman Problem with the Held-Karp Algorithm");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(new Color(220, 210, 255));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(6));
        titleBox.add(sub);
        header.add(titleBox, BorderLayout.CENTER);

        // DB status chip (top-right)
        dbStatusLabel = new JLabel();
        dbStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        dbStatusLabel.setBorder(new EmptyBorder(4, 10, 4, 10));
        updateDbStatus();
        header.add(dbStatusLabel, BorderLayout.EAST);

        return header;
    }

    private void updateDbStatus() {
        if (db != null && db.isConnected()) {
            dbStatusLabel.setText("● DB Connected");
            dbStatusLabel.setForeground(SUCCESS_GREEN);
        } else {
            dbStatusLabel.setText("● DB Offline");
            dbStatusLabel.setForeground(new Color(255, 180, 100));
        }
    }

    // ── Scrollable centre area ────────────────────────────────────────────────
    private JScrollPane buildCenter() {
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(new Color(245, 245, 250));
        center.setBorder(new EmptyBorder(20, 30, 20, 30));

        center.add(buildInputCard());
        center.add(Box.createVerticalStrut(16));
        center.add(buildResultCard());
        center.add(Box.createVerticalStrut(16));
        center.add(buildHistoryCard());
        center.add(Box.createVerticalStrut(20));

        JScrollPane sp = new JScrollPane(center);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    // ── Input card ────────────────────────────────────────────────────────────
    private JPanel buildInputCard() {
        JPanel card = card();
        card.setLayout(new BorderLayout(0, 14));

        // Title row
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setOpaque(false);
        JLabel lbl = new JLabel("📍  Enter Distance Matrix");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lbl.setForeground(TEXT_DARK);
        titleRow.add(lbl);
        card.add(titleRow, BorderLayout.NORTH);

        // Controls row
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controls.setOpaque(false);

        controls.add(label("Number of locations:"));
        JTextField nodeField = new JTextField(String.valueOf(numNodes), 5);
        styleTextField(nodeField);
        controls.add(nodeField);

        controls.add(Box.createHorizontalStrut(6));
        JButton applyBtn = outlineButton("Apply");
        controls.add(applyBtn);

        // Spacer pushes Load/Clear to the right
        controls.add(Box.createHorizontalStrut(800));

        JButton loadBtn = outlineButton("Load Sample");
        JButton clearBtn = outlineButton("Clear");
        controls.add(loadBtn);
        controls.add(clearBtn);

        card.add(controls, BorderLayout.CENTER);

        // Matrix container
        matrixPanel = new JPanel(new BorderLayout());
        matrixPanel.setOpaque(false);
        card.add(matrixPanel, BorderLayout.SOUTH);

        // Solve button row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnRow.setOpaque(false);
        JButton solveBtn = primaryButton("🔍  Find Optimal Route");
        btnRow.add(solveBtn);
        // Attach below card via outer wrapper
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);
        wrapper.add(card, BorderLayout.CENTER);
        wrapper.add(btnRow, BorderLayout.SOUTH);

        // ── Listeners ────────────────────────────────────────────────────────
        applyBtn.addActionListener(e -> {
            try {
                int n = Integer.parseInt(nodeField.getText().trim());
                if (n < 2 || n > 15)
                    throw new TSPException(TSPException.ErrorType.INVALID_NODE_COUNT,
                        "Number of locations must be between 2 and 15.");
                numNodes = n;
                buildMatrix(numNodes);
            } catch (NumberFormatException ex) {
                showError("Invalid Input", "Please enter a whole number for the number of locations.");
            } catch (TSPException ex) {
                handleTSPException(ex);
            }
        });

        loadBtn.addActionListener(e -> loadSampleData());
        clearBtn.addActionListener(e -> clearMatrix());
        solveBtn.addActionListener(e -> solveTSP());

        return wrapper;
    }

    // ── Result card ───────────────────────────────────────────────────────────
    private JPanel buildResultCard() {
        resultPanel = card();
        resultPanel.setLayout(new GridLayout(1, 3, 20, 0));
        resultPanel.setVisible(false);

        resultPathLabel = resultItem("🗺  Optimal Route", "—");
        resultCostLabel = resultItem("💰  Total Distance", "—");
        resultTimeLabel = resultItem("⚡  Solve Time", "—");

        resultPanel.add(resultPathLabel.getParent());
        resultPanel.add(resultCostLabel.getParent());
        resultPanel.add(resultTimeLabel.getParent());

        return resultPanel;
    }

    private JLabel resultItem(String title, String value) {
        JPanel pane = new JPanel();
        pane.setOpaque(false);
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.setForeground(TEXT_SUB);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.BOLD, 18));
        v.setForeground(ACCENT);
        v.setAlignmentX(Component.CENTER_ALIGNMENT);

        pane.add(t);
        pane.add(Box.createVerticalStrut(6));
        pane.add(v);

        return v; // return the value label for later update
    }

    // ── History card ──────────────────────────────────────────────────────────
    private JPanel buildHistoryCard() {
        JPanel card = card();
        card.setLayout(new BorderLayout(0, 10));

        JLabel lbl = new JLabel("📋  Session History");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lbl.setForeground(TEXT_DARK);
        card.add(lbl, BorderLayout.NORTH);

        String[] cols = {"#", "Date / Time", "Nodes", "Cost", "Path", "Time"};
        historyModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        historyTable.setRowHeight(28);
        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        historyTable.getTableHeader().setBackground(TABLE_HEADER);
        historyTable.setShowVerticalLines(false);
        historyTable.setGridColor(BORDER_LIGHT);
        historyTable.setIntercellSpacing(new Dimension(0, 1));

        // Set column widths
        int[] widths = {40, 140, 60, 70, 350, 80};
        for (int i = 0; i < widths.length; i++)
            historyTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane sp = new JScrollPane(historyTable);
        sp.setPreferredSize(new Dimension(0, 160));
        sp.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT));
        card.add(sp, BorderLayout.CENTER);

        JButton refreshBtn = outlineButton("↻  Refresh History");
        refreshBtn.addActionListener(e -> loadHistory());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(refreshBtn);
        card.add(btnRow, BorderLayout.SOUTH);

        loadHistory();
        return card;
    }

    // ── Build distance matrix grid ────────────────────────────────────────────
    private void buildMatrix(int n) {
        matrixPanel.removeAll();

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(10, 0, 10, 0));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1; c.weighty = 1;
        c.insets = new Insets(2, 2, 2, 2);

        // Top-left empty corner
        c.gridx = 0; c.gridy = 0;
        grid.add(headerCell(""), c);

        // Column headers
        for (int j = 0; j < n; j++) {
            c.gridx = j + 1; c.gridy = 0;
            grid.add(headerCell("City " + j), c);
        }

        // Rows
        distFields = new JTextField[n][n];
        for (int i = 0; i < n; i++) {
            c.gridx = 0; c.gridy = i + 1;
            grid.add(headerCell("City " + i), c);

            for (int j = 0; j < n; j++) {
                c.gridx = j + 1; c.gridy = i + 1;
                JTextField tf;
                if (i == j) {
                    tf = new JTextField("0");
                    tf.setEditable(false);
                    tf.setBackground(TABLE_DIAG);
                    tf.setForeground(TEXT_SUB);
                } else {
                    tf = new JTextField();
                    tf.setBackground(Color.WHITE);
                    tf.setForeground(TEXT_DARK);
                }
                tf.setHorizontalAlignment(JTextField.CENTER);
                tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_LIGHT),
                    new EmptyBorder(4, 6, 4, 6)));
                tf.setPreferredSize(new Dimension(80, 36));
                distFields[i][j] = tf;
                grid.add(tf, c);
            }
        }

        matrixPanel.add(grid, BorderLayout.CENTER);
        matrixPanel.revalidate();
        matrixPanel.repaint();
    }

    // ── Solve logic ───────────────────────────────────────────────────────────
    private void solveTSP() {
        try {
            int[][] matrix = readMatrix();
            GraphValidator.validate(matrix, numNodes);

            long start = System.currentTimeMillis();
            HeldKarpSolver solver = new HeldKarpSolver(matrix);
            solver.solve();
            long elapsed = System.currentTimeMillis() - start;

            String path = solver.getPathString();
            int cost     = solver.getBestCost();

            showResult(path, cost, elapsed + " ms");

            // Save to DB
            if (db != null && db.isConnected()) {
                db.saveSession(numNodes, matrix, path, cost, elapsed);
                loadHistory();
            }

        } catch (TSPException ex) {
            if (db != null) db.logError(ex);
            handleTSPException(ex);
        } catch (Exception ex) {
            showError("Unexpected Error", ex.getMessage());
        }
    }

    private int[][] readMatrix() throws TSPException {
        int[][] m = new int[numNodes][numNodes];
        for (int i = 0; i < numNodes; i++) {
            for (int j = 0; j < numNodes; j++) {
                String txt = distFields[i][j].getText().trim();
                if (txt.isEmpty()) {
                    throw new TSPException(TSPException.ErrorType.MISSING_VALUE,
                        "Distance from City " + i + " to City " + j + " is empty.");
                }
                try {
                    m[i][j] = Integer.parseInt(txt);
                } catch (NumberFormatException e) {
                    throw new TSPException(TSPException.ErrorType.INVALID_DISTANCE,
                        "Invalid value at City " + i + " → City " + j + ": \"" + txt + "\" is not a number.");
                }
            }
        }
        return m;
    }

    private void showResult(String path, int cost, String time) {
        resultPathLabel.setText(path);
        resultCostLabel.setText(String.valueOf(cost));
        resultTimeLabel.setText(time);
        resultPanel.setVisible(true);
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    // ── History ───────────────────────────────────────────────────────────────
    private void loadHistory() {
        historyModel.setRowCount(0);
        if (db == null || !db.isConnected()) {
            historyModel.addRow(new Object[]{"—", "DB not connected", "—", "—", "—", "—"});
            return;
        }
        try {
            List<String[]> rows = db.getRecentSessions(50);
            if (rows.isEmpty()) {
                historyModel.addRow(new Object[]{"—", "No sessions yet", "—", "—", "—", "—"});
            } else {
                for (String[] r : rows) historyModel.addRow(r);
            }
        } catch (TSPException ex) {
            handleTSPException(ex);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void loadSampleData() {
        if (numNodes != 4) {
            numNodes = 4;
            buildMatrix(4);
        }
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                distFields[i][j].setText(String.valueOf(SAMPLE_4[i][j]));
    }

    private void clearMatrix() {
        for (int i = 0; i < numNodes; i++)
            for (int j = 0; j < numNodes; j++)
                if (i != j) distFields[i][j].setText("");
        resultPanel.setVisible(false);
    }

    private void handleTSPException(TSPException ex) {
        showError(ex.getErrorType().getLabel(), ex.getMessage());
    }

    private void showError(String title, String msg) {
        JOptionPane.showMessageDialog(this,
            "<html><body style='width:360px;font-family:Segoe UI;'>"
            + "<b style='color:#ef4444;'>⚠  " + title + "</b><br><br>"
            + msg + "</body></html>",
            "Error — TSP Optimizer",
            JOptionPane.ERROR_MESSAGE);
    }

    // ── Style helpers ─────────────────────────────────────────────────────────
    private JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(BG_WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LIGHT),
            new EmptyBorder(18, 20, 18, 20)));
        return p;
    }

    private JLabel headerCell(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(TEXT_SUB);
        l.setBackground(TABLE_HEADER);
        l.setOpaque(true);
        l.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LIGHT),
            new EmptyBorder(6, 8, 6, 8)));
        l.setPreferredSize(new Dimension(80, 36));
        return l;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(TEXT_DARK);
        return l;
    }

    private void styleTextField(JTextField tf) {
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LIGHT),
            new EmptyBorder(4, 8, 4, 8)));
    }

    private JButton primaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? BTN_HOVER : BTN_PURPLE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(220, 42));
        return btn;
    }

    private JButton outlineButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(TEXT_DARK);
        btn.setBackground(BG_WHITE);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LIGHT),
            new EmptyBorder(5, 14, 5, 14)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Entry point ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(TSPMainGUI::new);
    }
}
