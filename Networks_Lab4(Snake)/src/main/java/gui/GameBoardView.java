package gui;

import models.BoardCell;
import observers.Observable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

public class GameBoardView extends Observable {
    private StatusView statusView;
    private GameBoardPanel gameBoardPanel;
    private JPanel commonPanel;
    private JButton exitGameButton;
    private int rows;
    private int columns;
    private int scalingFactor;
    private ArrayList<BoardCell> cells;

    private class GameBoardPanel extends JPanel {
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Graphics2D g2d = (Graphics2D) g;
            AffineTransform at = new AffineTransform();
            at.scale(scalingFactor, scalingFactor);
            g2d.setTransform(at);
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    switch(cells.get(i * rows + j).getBoardCellType()){
                        case EMPTY -> g2d.setColor(new Color(21, 0, 39));
                        case OWN_BODY -> g2d.setColor(new Color(35, 110, 39));
                        case OWN_HEAD -> g2d.setColor(new Color(25, 170, 39));
                        case FOOD -> g2d.setColor(new Color(215, 215, 78));
                        case ENEMY_BODY -> g2d.setColor(new Color(142, 39, 17));
                        case ENEMY_HEAD -> g2d.setColor(new Color(180, 39, 17));
                    }
                    g2d.fillRect(j, i, 1, 1);
                }
            }

        }
    }

    public GameBoardView(StatusView statusView){
        this.statusView = statusView;
        commonPanel = new JPanel();
        commonPanel.setLayout(new GridBagLayout());
        gameBoardPanel = new GameBoardPanel();
        exitGameButton = new JButton("Exit Game");
        exitGameButton.setPreferredSize(new Dimension(150, 50));
        exitGameButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                notifyObservers("ExitGame");
            }
        });
        addKeyBindings();
        placeSubPanels();
    }

    private void addKeyBindings(){
        commonPanel.getActionMap().put("W", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                notifyObservers("PressedW");
            }
        });
        commonPanel.getActionMap().put("A", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                notifyObservers("PressedA");
            }
        });
        commonPanel.getActionMap().put("S", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                notifyObservers("PressedS");
            }
        });
        commonPanel.getActionMap().put("D", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                notifyObservers("PressedD");
            }
        });
        commonPanel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("W"), "W");
        commonPanel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("A"), "A");
        commonPanel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("S"), "S");
        commonPanel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("D"), "D");
    }

    private void placeSubPanels(){
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        commonPanel.add(gameBoardPanel, constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridheight = 2;
        commonPanel.add(statusView.getStatusPanel(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridheight = 1;
        commonPanel.add(exitGameButton, constraints);
    }

    public JPanel getGameBoardPanel(){
        return commonPanel;
    }

    public void setGameBoardSize(int rows, int columns){
        this.rows = rows;
        this.columns = columns;
        int maxCellNumber = 100;
        int scalingConst = 16;
        scalingFactor = scalingConst * (maxCellNumber / rows);
        gameBoardPanel.setPreferredSize(new Dimension(rows * (scalingFactor / 2), columns * (scalingFactor / 2)));
        scalingFactor /= 2;
    }

    public void setCells(ArrayList<BoardCell> cells){
        this.cells = cells;
    }

    public void repaint(){
        gameBoardPanel.repaint();
    }
}
