package gui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.InetSocketAddress;

public class StatusView{
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
        currentGameModel.addColumn("Host");
        currentGameModel.addColumn("Size");
        currentGameModel.addColumn("Food");
        currentGameScrollPane = new JScrollPane(currentGame);
        currentGameScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                "Current Game",
                TitledBorder.CENTER,
                TitledBorder.TOP));

        ratingModel = new DefaultTableModel();
        rating = new JTable(ratingModel);
        ratingModel.addColumn("#");
        ratingModel.addColumn("Name");
        ratingModel.addColumn("Score");
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

    public void setGameInformation(String hostName, int rows, int columns, int foodStatic, float foodPerPlayer){
        currentGameModel.getDataVector().removeAllElements();
        currentGameModel.addRow(new Object[] {hostName, rows + "x" + columns, foodStatic + "+" + foodPerPlayer + "x" });
    }

    public void addPlayerToRating(String name, int score){
        ratingModel.addRow(new Object[]{ratingModel.getRowCount() + 1, name, score});
    }

    public void clearRating(){
        ratingModel.getDataVector().removeAllElements();
    }

    public void replacePlayerScore(int number, int score){
        ratingModel.setValueAt(score, number - 1, 2);
    }
}
