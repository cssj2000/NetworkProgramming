package client;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 상대방의 게임 화면을 작게 보여주는 미니뷰 컴포넌트
 */
public class MiniGameView extends JPanel {
    private String playerName;
    private int score;
    private int combo;
    private List<Direction> sequence = new ArrayList<>();
    private List<Color> arrowColors = new ArrayList<>();
    private int currentIndex = 0;

    private JLabel nameLabel;
    private JLabel scoreLabel;
    private MiniArrowPanel arrowPanel;

    public MiniGameView(String playerName) {
        this.playerName = playerName;
        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(245, 250, 255));
        setBorder(BorderFactory.createLineBorder(new Color(180, 210, 230), 2));
        setPreferredSize(new Dimension(250, 150));

        // 상단: 플레이어 정보
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        infoPanel.setBackground(new Color(245, 250, 255));

        nameLabel = new JLabel(playerName);
        nameLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        nameLabel.setForeground(new Color(80, 120, 180));
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        scoreLabel = new JLabel("점수: 0 | 콤보: 0");
        scoreLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
        scoreLabel.setForeground(new Color(60, 60, 60));
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);

        infoPanel.add(nameLabel);
        infoPanel.add(scoreLabel);

        // 중앙: 화살표 시퀀스
        arrowPanel = new MiniArrowPanel();
        arrowPanel.setBackground(new Color(235, 245, 255));

        add(infoPanel, BorderLayout.NORTH);
        add(arrowPanel, BorderLayout.CENTER);
    }

    /**
     * 게임 상태 업데이트
     */
    public void updateGameState(int score, int combo, List<Direction> sequence,
                                 List<Color> arrowColors, int currentIndex) {
        this.score = score;
        this.combo = combo;
        this.sequence = new ArrayList<>(sequence);
        this.arrowColors = new ArrayList<>(arrowColors);
        this.currentIndex = currentIndex;

        scoreLabel.setText("점수: " + score + " | 콤보: " + combo);
        arrowPanel.updateSequence(sequence, arrowColors, currentIndex);
        repaint();
    }

    /**
     * 화살표를 작게 그리는 패널
     */
    private class MiniArrowPanel extends JPanel {
        private List<Direction> seq = new ArrayList<>();
        private List<Color> colors = new ArrayList<>();
        private int curIndex = 0;

        public void updateSequence(List<Direction> sequence, List<Color> arrowColors, int currentIndex) {
            this.seq = new ArrayList<>(sequence);
            this.colors = new ArrayList<>(arrowColors);
            this.curIndex = currentIndex;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (seq.isEmpty()) {
                // 대기 상태 표시
                g2.setColor(Color.GRAY);
                g2.setFont(new Font("Dialog", Font.PLAIN, 10));
                String msg = "대기 중...";
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(msg)) / 2;
                int y = (getHeight() + fm.getAscent()) / 2;
                g2.drawString(msg, x, y);
                return;
            }

            int width = getWidth();
            int height = getHeight();
            int n = seq.size();

            // 화살표 크기 조정 (최대 5개까지만 보여주기)
            int displayCount = Math.min(n, 5);
            int startIdx = Math.max(0, curIndex - 2); // 현재 위치 기준 앞뒤로 보여주기
            int endIdx = Math.min(n, startIdx + displayCount);

            // startIdx 재조정 (끝에 도달했을 때)
            if (endIdx - startIdx < displayCount && startIdx > 0) {
                startIdx = Math.max(0, endIdx - displayCount);
            }

            int displayNum = endIdx - startIdx;
            int spacing = width / (displayNum + 1);
            int radius = Math.min(15, height / 3);
            int centerY = height / 2;

            for (int i = 0; i < displayNum; i++) {
                int idx = startIdx + i;
                int cx = spacing * (i + 1);
                int cy = centerY;

                // 배경 원 색상
                if (idx < curIndex) {
                    g2.setColor(new Color(210, 240, 210)); // 완료: 녹색
                } else if (idx == curIndex) {
                    g2.setColor(new Color(200, 230, 255)); // 현재: 파란색
                } else {
                    g2.setColor(new Color(235, 245, 255)); // 미완료: 밝은 파란색
                }
                g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

                // 원 테두리
                g2.setColor(new Color(180, 210, 230));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);

                // 화살표 심볼
                Direction d = seq.get(idx);
                Color arrowColor = colors.get(idx);
                g2.setColor(arrowColor);
                g2.setFont(new Font("Dialog", Font.BOLD, 16));
                String symbol = d.getSymbol();
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(symbol);
                int th = fm.getAscent();
                g2.drawString(symbol, cx - tw / 2, cy + th / 4);
            }
        }
    }

    public String getPlayerName() {
        return playerName;
    }
}
