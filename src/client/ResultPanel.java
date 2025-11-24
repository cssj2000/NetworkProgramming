package client;

import javax.swing.*;
import java.awt.*;

public class ResultPanel extends JPanel {

    JLabel titleLabel = new JLabel("게임 결과", SwingConstants.CENTER);
    JLabel scoreLabel = new JLabel("점수: 0", SwingConstants.CENTER);
    JLabel comboLabel = new JLabel("최고 콤보: 0", SwingConstants.CENTER);
    JButton retryButton = new JButton("다시 플레이");
    JButton exitButton = new JButton("로비로");

    public interface RetryListener { void onRetry(); }
    public interface ExitListener { void onExit(); }

    private RetryListener retryListener;
    private ExitListener exitListener;

    public ResultPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(224, 245, 255));

        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        titleLabel.setFont(new Font("Dialog", Font.BOLD, 40));
        titleLabel.setForeground(new Color(80, 190, 255));

        scoreLabel.setFont(new Font("Dialog", Font.BOLD, 24));
        comboLabel.setFont(new Font("Dialog", Font.PLAIN, 20));

        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        comboLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(20));
        card.add(scoreLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(comboLabel);
        card.add(Box.createVerticalStrut(30));

        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        retryButton.setPreferredSize(new Dimension(150, 40));
        exitButton.setPreferredSize(new Dimension(150, 40));

        btnPanel.add(retryButton);
        btnPanel.add(exitButton);
        card.add(btnPanel);

        add(card, BorderLayout.CENTER);

        retryButton.addActionListener(e -> {
            if (retryListener != null) retryListener.onRetry();
        });

        exitButton.addActionListener(e -> {
            if (exitListener != null) exitListener.onExit();
        });
    }

    public void setResult(int score, int maxCombo) {
        scoreLabel.setText("점수: " + score);
        comboLabel.setText("최고 콤보: " + maxCombo);
    }

    public void setOnRetryListener(RetryListener l) {
        this.retryListener = l;
    }

    public void setOnExitListener(ExitListener l) {
        this.exitListener = l;
    }
}
