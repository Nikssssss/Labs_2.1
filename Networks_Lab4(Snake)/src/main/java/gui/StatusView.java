package gui;

import observers.Observable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class StatusView extends Observable {
    private JPanel statusPanel;
    private JTable rating;
    private JTable currentGame;
    private DefaultTableModel ratingModel;
    private DefaultTableModel currentGameModel;
    private JScrollPane ratingScrollPane;
    private JScrollPane currentGameScrollPane;

    public StatusView(){
        statusPanel = new JPanel();
        statusPanel.setBackground(Color.RED);
        statusPanel.setLayout(new GridBagLayout());
        createSubComponents();
        placeSubComponents();
    }

    public JPanel getStatusPanel(){
        return statusPanel;
    }

    private void createSubComponents(){
        currentGameModel = new DefaultTableModel();
        currentGame = new JTable(currentGameModel);
        currentGameModel.addColumn("#");
        currentGameModel.addColumn("Name");
        currentGameModel.addColumn("Score");
        currentGameScrollPane = new JScrollPane(currentGame);
        currentGameScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                "Current Game",
                TitledBorder.CENTER,
                TitledBorder.TOP));

        ratingModel = new DefaultTableModel();
        rating = new JTable(ratingModel);
        ratingModel.addColumn("Host");
        ratingModel.addColumn("Size");
        ratingModel.addColumn("Food");
        ratingScrollPane = new JScrollPane(rating);
        ratingScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                "Rating",
                TitledBorder.CENTER,
                TitledBorder.TOP));
    }

    private void placeSubComponents(){
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        statusPanel.add(currentGameScrollPane, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        statusPanel.add(ratingScrollPane, constraints);
    }
}
