package gui;

import models.GameInfo;
import observers.Observable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LobbyView extends Observable {
    private JPanel lobbyPanel;
    private DefaultTableModel availableGamesModel;
    private JTable availableGames;
    private JScrollPane availableGamesScrollPane;
    private JButton createGameButton;
    private JButton joinGameButton;

    public LobbyView(){
        lobbyPanel = new JPanel();
        lobbyPanel.setBackground(Color.PINK);
        lobbyPanel.setLayout(new GridBagLayout());
        createSubComponents();
        placeSubComponents();
    }

    private void createSubComponents(){
        createGameButton = new JButton("Create Game");
        createGameButton.setFont(new Font("Arial", Font.PLAIN, 18));
        createGameButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mouseClicked(e);
                notifyObservers("CreateGame");
            }
        });
        joinGameButton = new JButton("Join");

        availableGamesModel = new DefaultTableModel();
        availableGames = new JTable(availableGamesModel);
        availableGamesModel.addColumn("Host");
        availableGamesModel.addColumn("#");
        availableGamesModel.addColumn("Size");
        availableGamesModel.addColumn("Food");
        availableGamesModel.addColumn("Enter");
        availableGames.getColumn("Enter").setCellRenderer(new ButtonRenderer());
        availableGames.getColumn("Enter").setCellEditor(new ButtonEditor(new JCheckBox()));
        availableGames.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (availableGames.getSelectedColumn() == 4){
                    notifyObservers(availableGamesModel.getDataVector().elementAt(availableGames.getSelectedRow()).elementAt(1));
                }
            }
        });
        availableGamesScrollPane = new JScrollPane(availableGames);
        availableGamesScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                "Available Games",
                TitledBorder.CENTER,
                TitledBorder.TOP));
    }

    private void placeSubComponents(){
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.ipadx = 50;
        constraints.ipady = 30;
        constraints.insets.top = 10;
        lobbyPanel.add(createGameButton, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.ipadx = 100;
        constraints.insets.top = 10;
        lobbyPanel.add(availableGamesScrollPane, constraints);
    }

    public JPanel getLobbyPanel(){
        return lobbyPanel;
    }

    public void addAvailableGame(GameInfo gameInfo){
        for (int i = 0; i < availableGamesModel.getDataVector().size(); i++) {
            if (availableGamesModel.getDataVector().elementAt(i).elementAt(1).equals(gameInfo.getHostID())){
                availableGamesModel.setValueAt(new Object[]{
                        gameInfo.getHostAddress(), gameInfo.getHostID(), gameInfo.getBoardWidth() + "x" + gameInfo.getBoardHeight(),
                        gameInfo.getFoodStatic() + "+" + gameInfo.getFoodPerPerson() + "x", "Join"}, i, 1);
                return;
            }
        }
        availableGamesModel.addRow(new Object[]{
                gameInfo.getHostAddress(), gameInfo.getHostID(), gameInfo.getBoardWidth() + "x" + gameInfo.getBoardHeight(),
                gameInfo.getFoodStatic() + "+" + gameInfo.getFoodPerPerson() + "x", "Join"});
    }

    public void removeAvailableGame(GameInfo gameInfo){
        for (int i = 0; i < availableGamesModel.getDataVector().size(); i++) {
            if (availableGamesModel.getDataVector().elementAt(i).elementAt(1).equals(gameInfo.getHostID())){
                availableGamesModel.removeRow(i);
            }
        }
    }

    static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private String label;
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            joinGameButton.setText(label);
            return joinGameButton;
        }

        public Object getCellEditorValue() {
            return label;
        }
    }
}




