package com.metal_pony.tetrisai.drivers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;

import com.google.gson.Gson;
import com.metal_pony.bucket.tetris.TetrisState;
import com.metal_pony.bucket.tetris.gui.components.TetrisBoardPanel;
import com.metal_pony.bucket.tetris.util.structs.Coord;
import com.metal_pony.bucket.tetris.util.structs.Move;
import com.metal_pony.bucket.tetris.util.structs.Shape;

public class TestCaseManager extends JFrame {
    public static void main(String[] args) {
        new TestCaseManager().setVisible(true);
    }

    public static final FileNameExtensionFilter JSON_FILTER = new FileNameExtensionFilter("JSON Files", "json");

    class TestCaseProp {
        public static String[] columnNames = { "Test Name", "Args", "Expected Result", "Actual Result" };

        // Data includes (all strings): test name, args, expected result, actual result
        Object[] data;

        TestCaseProp() {
            this("", "", "", "");
        }

        TestCaseProp(String testName, String args, String expectedResult, String actualResult) {
            this.data = new Object[] { testName, args, expectedResult, actualResult };
        }
    }

    public class TetrisTestCase extends AbstractTableModel {
        static String defaultName(int n) {
            return String.format("New Test Case %d", n);
        }

        TetrisState state;
        List<TestCaseProp> testPropsList;
        String name;

        public TetrisTestCase() {
            this(new TetrisState(), defaultName(testCaseListModel.size() + 1), new ArrayList<>());
        }

        public TetrisTestCase(TetrisState state, String name, List<TestCaseProp> testProps) {
            this.state = state;
            this.name = name;
            this.testPropsList = testProps;

            // TODO this is necessary for the tetris panel to display the state, but it doesn't belong here
            this.state.isPaused = false;
            this.state.isGameOver = false;
            this.state.hasStarted = true;
            this.state.piece.disable();
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int getRowCount() {
            // Include extra rows for adding more test cases
            return testPropsList.size() + 4;
        }

        @Override
        public int getColumnCount() {
            // Test name + args(will be json string so just 1 column) + expected result + actual result
            return 4;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= testPropsList.size()) {
                return null;
            }

            TestCaseProp props = testPropsList.get(rowIndex);
            return props.data[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return TestCaseProp.columnNames[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            // TODO add validation
            if (rowIndex >= testPropsList.size()) {
                return;
            }

            TestCaseProp props = testPropsList.get(rowIndex);
            props.data[columnIndex] = value;
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        public void addRow() {
            testPropsList.add(new TestCaseProp());
            fireTableRowsInserted(testPropsList.size() - 1, testPropsList.size() - 1);
        }
    }

    public static final String TEST_CASE_FILENAME = "/test-cases.json";

    File testCasesFile;
    JFileChooser fileChooser;

    JMenuBar menuBar;
    JMenu fileMenu;
    // creates a new test case with a new TetrisState
    JMenuItem newTestCaseMenuItem;
    // shows the save file chooser then saves the test cases to the selected json file
    JMenuItem saveTestCasesMenuItem;
    // shows the open file chooser then loads the test cases from the selected json file
    JMenuItem openTestCasesMenuItem;
    // closes the current test case file and clears the test case list
    JMenuItem closeTestCasesMenuItem;
    // separator between these items
    // exits the program
    JMenuItem exitMenuItem;

    TetrisTestCase currentTestCase;
    DefaultListModel<TetrisTestCase> testCaseListModel;
    JScrollPane testCaseListScrollPane;
    JList<TetrisTestCase> testCaseList;

    // TODO extend this class to allow for editing test cases
    // TetrisState state;
    TetrisBoardPanel tetrisPanel;
    int blockSize = TetrisBoardPanel.DEFAULT_BLOCK_SIZE;

    // toolbar contains:
    //      label displaying current mouseover coord in tetrisPanel
    //      nav button to go to previous test case
    //      label displaying current test case index
    //      nav button to go to next test case
    JToolBar toolBar;

    // contains:
    //      button to run current test case
    //      button to run all test cases
    //      splitpane containing list of shapes, and panel displaying selected shape
    // TestCaseControlPanel controlPanel;

    // contains a table of test case properties
    // TestCasePropTable propTable;
    JScrollPane propTableScrollPane;
    JTable testPropsTable;

    public TestCaseManager() {
        super("Tetris Test Case Manager");

        fileChooser = new JFileChooser(System.getProperty("user.dir"));

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));
        setBackground(Color.LIGHT_GRAY);

        buildMenu();
        buildTestCaseList();
        buildTetrisPanel();
        buildTestPropsTable();

        setJMenuBar(menuBar);

        add(testCaseListScrollPane);
        add(tetrisPanel);
        add(propTableScrollPane);

        pack();
        // setResizable(false);

        newTestCaseMenuItem.doClick();
    }

    private void buildMenu() {
        // TODO flesh out menu items
        menuBar = new JMenuBar();
        fileMenu = new JMenu("File");

        newTestCaseMenuItem = new JMenuItem("New Test Case");
        newTestCaseMenuItem.addActionListener(e -> {
            System.out.println("Creating new test case.");
            testCaseListModel.addElement(new TetrisTestCase());
            testCaseList.setSelectedIndex(testCaseListModel.size() - 1);
            currentTestCase = testCaseList.getSelectedValue();
        });
        fileMenu.add(newTestCaseMenuItem);

        saveTestCasesMenuItem = new JMenuItem("Save Test Cases");
        saveTestCasesMenuItem.addActionListener(e -> {
            System.out.print("Attempting to save test cases to file... ");
            try {
                saveTestCasesToFile("test-cases.json");
                System.out.println("Success!");
            } catch (IOException ex) {
                System.out.println("Failed!");
                ex.printStackTrace();
            }
        });
        fileMenu.add(saveTestCasesMenuItem);

        openTestCasesMenuItem = new JMenuItem("Open Test Cases");
        openTestCasesMenuItem.addActionListener(e -> {
            System.out.print("Attempting to load test cases from file... ");
            try {
                loadTestCasesFromFile();
                System.out.println("Success!");
            } catch (IOException ex) {
                System.out.println("Failed!");
                ex.printStackTrace();
            }
        });
        fileMenu.add(openTestCasesMenuItem);

        closeTestCasesMenuItem = new JMenuItem("Close Test Cases");
        closeTestCasesMenuItem.addActionListener(e -> {
            System.out.println("Closing test cases.");
            testCaseListModel.clear();
            testCaseList.setSelectedIndex(-1);
            currentTestCase = null;
        });
        fileMenu.add(closeTestCasesMenuItem);

        exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(e -> {
            System.out.println("Exiting.");
            dispose();
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);
    }

    private void buildTestCaseList() {
        testCaseListModel = new DefaultListModel<>();

        testCaseList = new JList<>();
        testCaseList.setModel(testCaseListModel);
        testCaseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        testCaseList.setLayoutOrientation(JList.VERTICAL);
        testCaseList.setVisibleRowCount(-1);
        testCaseList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }

            System.out.println("Selected test case index: " + testCaseList.getSelectedIndex());
            switchTestCase(testCaseList.getSelectedIndex());
        });

        testCaseListScrollPane = new JScrollPane(testCaseList);
        testCaseListScrollPane.setPreferredSize(new Dimension(200, 400));
    }

    private void buildTetrisPanel() {
        MouseListener mouseListener = new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {}
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentTestCase == null) {
                    return;
                }

                if (!tetrisPanel.isFocusOwner()) {
                    // System.out.println("Requesting focus in window.");
                    tetrisPanel.requestFocusInWindow();
                    return;
                }

                int x = e.getX();
                int y = e.getY();

                if (
                    x < 4 ||
                    x > currentTestCase.state.cols * blockSize + 4 ||
                    y < 4 ||
                    y > currentTestCase.state.rows * blockSize + 4
                ) {
                    // System.out.println("Mouse clicked outside of tetrisPanel drawable area.");
                    return;
                }
                // Account for the border width + insets
                x -= 4;
                y -= 4;

                Coord mouseLocation = new Coord(y / blockSize, x / blockSize);
                // System.out.println("Mouse clicked at " + mouseLocation);

                if (
                    !currentTestCase.state.validateCoord(mouseLocation) ||
                    currentTestCase.state.pieceOverlapsBlocks()
                ) {
                    // TODO remove later
                    System.out.println("CANNOT PLACE BLOCK AT THIS POSITION " + mouseLocation);
                    return;
                }

                if (e.getButton() == MouseEvent.BUTTON1) {
                    currentTestCase.state.setCell(mouseLocation, Shape.O.value);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    currentTestCase.state.setCell(mouseLocation, 0);
                }

                updateStateAndRepaint();
            }
            @Override
            public void mouseReleased(MouseEvent e) {}
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
        };

        MouseMotionListener mouseMotionListener = new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentTestCase == null) {
                    return;
                }

                int x = e.getX();
                int y = e.getY();

                if (
                    x < 4 ||
                    x > currentTestCase.state.cols * blockSize + 4 ||
                    y < 4 ||
                    y > currentTestCase.state.rows * blockSize + 4
                ) {
                    // System.out.println("Mouse clicked outside of tetrisPanel drawable area.");
                    return;
                }
                // Account for the border width + insets
                x -= 4;
                y -= 4;

                Coord mouseLocation = new Coord(y / blockSize, x / blockSize);

                if (
                    !currentTestCase.state.validateCoord(mouseLocation) ||
                    currentTestCase.state.pieceOverlapsBlocks()
                ) {
                    return;
                }

                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (currentTestCase.state.getCell(mouseLocation) != Shape.O.value) {
                        currentTestCase.state.setCell(mouseLocation, Shape.O.value);
                        updateStateAndRepaint();
                    }
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    if (currentTestCase.state.getCell(mouseLocation) != 0) {
                        currentTestCase.state.setCell(mouseLocation, 0);
                        updateStateAndRepaint();
                    }
                }
            }
            @Override
            public void mouseMoved(MouseEvent e) {}
        };

        tetrisPanel = new TetrisBoardPanel();
        tetrisPanel.drawStats(false);
        tetrisPanel.addMouseListener(mouseListener);
        tetrisPanel.addMouseMotionListener(mouseMotionListener);
        tetrisPanel.setFocusable(true);
        tetrisPanel.setBorder(BorderFactory.createEmptyBorder());
        tetrisPanel.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // Add a dotted line border to the tetrisPanel when it has focus
                tetrisPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE, 1));
                tetrisPanel.setPreferredSize(new Dimension(
                    currentTestCase.state.cols * blockSize + 8,
                    currentTestCase.state.rows * blockSize + 8
                ));
                pack();
            }

            @Override
            public void focusLost(FocusEvent e) {
                tetrisPanel.setBorder(BorderFactory.createEmptyBorder());
            }
        });

        ActionMap actionMap = tetrisPanel.getActionMap();
        InputMap inputMap = tetrisPanel.getInputMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "PLACE_PIECE");
        actionMap.put("PLACE_PIECE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTestCase == null) {
                    return;
                }

                if (currentTestCase.state.piece.isActive()) {
                    currentTestCase.state.placePiece();
                    updateStateAndRepaint();
                } else {
                    currentTestCase.state.resetPiece();
                    // TODO search for position at top of board where piece is in bounds
                    // For now, just remove blocks at the entry position
                    currentTestCase.state.piece.forEachCell((coord) -> currentTestCase.state.setCell(coord, 0));
                    updateStateAndRepaint();
                }
            }
        });

        for (Shape shape : Shape.values()) {
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_0 + shape.value, 0), "CHANGE_SHAPE_" + shape.name());
            actionMap.put("CHANGE_SHAPE_" + shape.name(), new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (currentTestCase == null) {
                        return;
                    }

                    System.out.println("Changing shape to " + shape.name());

                    currentTestCase.state.piece.shapeShift(shape);
                    if (!currentTestCase.state.pieceInBounds()) {
                        System.out.println("Piece out of bounds, resetting to entry position.");
                        // TODO search for position at top of board where piece is in bounds
                        currentTestCase.state.piece.reset(currentTestCase.state.entryCoord, shape);
                        // For now, just remove blocks at the entry position
                        currentTestCase.state.piece.forEachCell((coord) -> currentTestCase.state.setCell(coord, 0));
                    }
                    currentTestCase.state.piece.enable();
                    updateStateAndRepaint();
                }
            });
        }

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "REMOVE_PIECE");
        actionMap.put("REMOVE_PIECE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTestCase == null) {
                    return;
                }

                currentTestCase.state.piece.disable();
                updateStateAndRepaint();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, 0), "ROTATE_CLOCKWISE");
        actionMap.put("ROTATE_CLOCKWISE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTestCase == null) {
                    return;
                }

                Move _move = currentTestCase.state.validateRotation(Move.CLOCKWISE);
                if (_move.equals(Move.STAND)) {
                    return;
                }
                currentTestCase.state.piece.move(_move);
                updateStateAndRepaint();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0), "ROTATE_COUNTERCLOCKWISE");
        actionMap.put("ROTATE_COUNTERCLOCKWISE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTestCase == null) {
                    return;
                }

                Move _move = currentTestCase.state.validateRotation(Move.COUNTERCLOCKWISE);
                if (_move.equals(Move.STAND)) {
                    return;
                }
                currentTestCase.state.piece.move(_move);
                updateStateAndRepaint();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "MOVE_LEFT");
        actionMap.put("MOVE_LEFT", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTestCase == null) {
                    return;
                }

                if (currentTestCase.state.canPieceMove(Move.LEFT)) {
                    currentTestCase.state.piece.move(Move.LEFT);
                    updateStateAndRepaint();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "MOVE_RIGHT");
        actionMap.put("MOVE_RIGHT", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTestCase == null) {
                    return;
                }

                if (currentTestCase.state.canPieceMove(Move.RIGHT)) {
                    currentTestCase.state.piece.move(Move.RIGHT);
                    updateStateAndRepaint();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "MOVE_DOWN");
        actionMap.put("MOVE_DOWN", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTestCase == null) {
                    return;
                }

                if (currentTestCase.state.canPieceMove(Move.DOWN)) {
                    currentTestCase.state.piece.move(Move.DOWN);
                    updateStateAndRepaint();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "MOVE_UP");
        actionMap.put("MOVE_UP", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTestCase == null) {
                    return;
                }

                if (currentTestCase.state.isPositionValid(currentTestCase.state.piece.position().add(Move.UP))) {
                    currentTestCase.state.piece.move(Move.UP);
                    updateStateAndRepaint();
                }
            }
        });
    }

    private void buildTestPropsTable() {
        testPropsTable = new JTable();
        testPropsTable.setFillsViewportHeight(true);
        testPropsTable.setRowSelectionAllowed(false);
        testPropsTable.setColumnSelectionAllowed(false);
        testPropsTable.setCellSelectionEnabled(true);
        testPropsTable.setShowGrid(true);
        testPropsTable.setGridColor(Color.BLACK);
        testPropsTable.setRowHeight(20);
        testPropsTable.setRowMargin(2);

        // Create the scroll pane for the table, and set orientation to vertical
        propTableScrollPane = new JScrollPane(testPropsTable);
    }

    private void updateStateAndRepaint() {
        tetrisPanel.mapStateToCells();
        tetrisPanel.mapPieceStateToCells();
        tetrisPanel.repaint();
    }

    private void switchTestCase(int index) {
        if (index < 0 || index >= testCaseListModel.size()) {
            System.out.println("Invalid test case index.");
            return;
        }

        testCaseList.setSelectedIndex(index);
        currentTestCase = testCaseList.getSelectedValue();
        tetrisPanel.setState(currentTestCase.state);
        // tetrisPanel.setPreferredSize(new Dimension(
        //     currentTestCase.state.cols * blockSize + 4,
        //     currentTestCase.state.rows * blockSize + 4
        // ));
        // pack();

        testPropsTable.setModel(currentTestCase);
    }

    // Load test cases from json file in resources using Gson
    public void loadTestCasesFromFile() throws IOException {
        fileChooser.setFileFilter(JSON_FILTER);
        int response = fileChooser.showOpenDialog(this);

        if (response != JFileChooser.APPROVE_OPTION) {
            System.out.println("No file selected.");
            return;
        }

        testCasesFile = fileChooser.getSelectedFile();

        try (FileReader reader = new FileReader(testCasesFile)) {
            Gson gson = new Gson();
            TetrisTestCase[] testCases = gson.fromJson(reader, TetrisTestCase[].class);

            if (testCases.length == 0) {
                System.out.println("No test cases found in file.");
                return;
            }

            testCaseListModel.clear();
            for (TetrisTestCase testCase : testCases) {
                testCaseListModel.addElement(testCase);
            }
            testCaseList.setSelectedIndex(0);
            currentTestCase = testCaseList.getSelectedValue();
            System.out.println("Successfully loaded " + testCases.length + " test cases from " + testCasesFile.getName());
        } catch (IOException e) {
            System.out.println("Error reading file.");
            e.printStackTrace();
        }
    }

    public void saveTestCasesToFile(String fileName) throws IOException {
        if (testCaseListModel.isEmpty()) {
            System.out.println("No test cases to save.");
            return;
        }

        fileChooser.setFileFilter(JSON_FILTER);
        int response = fileChooser.showSaveDialog(this);

        if (response != JFileChooser.APPROVE_OPTION) {
            System.out.println("No file selected.");
            return;
        }

        testCasesFile = fileChooser.getSelectedFile();

        // TODO If the file already exists, do we want to show an overwrite confirmation modal?

        try (PrintWriter pw = new PrintWriter(testCasesFile)) {
            System.out.println(Arrays.toString(testCaseList.getSelectedValue().state.board));
            Gson gson = new Gson();
            // gson.toJson(testCases.toArray(), pw);
            gson.toJson(testCaseListModel.toArray(), pw);
            System.out.println("Successfully saved " + testCaseListModel.size() + " test cases to " + testCasesFile.getName());
        } catch (IOException ex) {
            System.out.println("Error writing file.");
            ex.printStackTrace();
        }
    }
}
