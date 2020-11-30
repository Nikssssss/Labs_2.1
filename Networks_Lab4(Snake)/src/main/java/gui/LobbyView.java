package gui;

import observers.Observable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LobbyView extends Observable {
    private JPanel lobbyPanel;
    private DefaultTableModel availableGamesModel;
    private JTable availableGames;
    private JScrollPane availableGamesScrollPane;
    private JButton createGameButton;

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

        availableGamesModel = new DefaultTableModel();
        availableGames = new JTable(availableGamesModel);
        availableGamesModel.addColumn("Host");
        availableGamesModel.addColumn("#");
        availableGamesModel.addColumn("Size");
        availableGamesModel.addColumn("Food");
        availableGamesModel.addColumn("Enter");
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
}
