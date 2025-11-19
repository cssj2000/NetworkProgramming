package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GamePanel extends JPanel implements KeyListener {

    // 게임 진행 상태
    private List<Direction> sequence = new ArrayList<>();
    private List<Color> arrowColors = new ArrayList<>(); // 각 화살표 색
    private int currentIndex = 0;
    private int stage = 1;
    private int score = 0;
    private int maxCombo = 0;
    private int combo = 0;

    // 타이머
    private int remainingSeconds = 60;
    private Timer gameTimer;

    // 상단 플레이어 정보
    private JLabel[] playerNameLabels = new JLabel[4];
    private JLabel[] playerScoreLabels = new JLabel[4];
    private JLabel[] playerComboLabels = new JLabel[4];
    private JLabel timeValueLabel;

    // 중앙/하단 텍스트
    private JLabel statusLabel = new JLabel("화살표 키를 순서대로 눌러주세요!", SwingConstants.CENTER);
    private JLabel difficultyLabel = new JLabel("난이도: 3개 화살표", SwingConstants.CENTER);
    private JLabel bigMessageLabel = new JLabel("", SwingConstants.CENTER);

    private ArrowPanel arrowPanel = new ArrowPanel();

    public interface GameEndListener {
        void onGameEnd(int score, int maxCombo);
    }

    private GameEndListener onGameEndListener;

    public GamePanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(224, 245, 255));

        // -------- 상단: 플레이어 정보 + 남은 시간 --------
        JPanel top = new JPanel(new GridLayout(1, 5, 10, 0));
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        for (int i = 0; i < 4; i++) {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(Color.WHITE);
            p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            playerNameLabels[i] = new JLabel("플레이어" + (i + 1));
            playerScoreLabels[i] = new JLabel("성공: 0");
            playerComboLabels[i] = new JLabel("콤보: 0");

            playerNameLabels[i].setForeground(new Color(120, 180, 255));
            playerNameLabels[i].setFont(new Font("Dialog", Font.BOLD, 14));

            JPanel inner = new JPanel();
            inner.setOpaque(false);
            inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
            inner.add(playerNameLabels[i]);
            inner.add(playerScoreLabels[i]);
            inner.add(playerComboLabels[i]);

            p.add(inner, BorderLayout.CENTER);
            top.add(p);
        }

        JPanel timePanel = new JPanel(new BorderLayout());
        timePanel.setBackground(Color.WHITE);
        timePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel timeTitle = new JLabel("남은 시간", SwingConstants.CENTER);
        timeValueLabel = new JLabel(remainingSeconds + "초", SwingConstants.CENTER);
        timeTitle.setForeground(new Color(120, 180, 255));
        timeValueLabel.setFont(new Font("Dialog", Font.BOLD, 18));
        timePanel.add(timeTitle, BorderLayout.NORTH);
        timePanel.add(timeValueLabel, BorderLayout.CENTER);
        top.add(timePanel);

        add(top, BorderLayout.NORTH);

        // -------- 중앙: 화살표 카드 --------
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(30, 80, 30, 80)); // 좌우 여백 조금 줄여 넓게

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 210, 230), 2),
                BorderFactory.createEmptyBorder(30, 30, 30, 30)));

        bigMessageLabel.setFont(new Font("Dialog", Font.BOLD, 60));
        bigMessageLabel.setForeground(new Color(0, 200, 120));

        statusLabel.setFont(new Font("Dialog", Font.PLAIN, 18));
        difficultyLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        statusLabel.setForeground(new Color(100, 120, 140));
        difficultyLabel.setForeground(new Color(150, 160, 170));

        card.add(bigMessageLabel, BorderLayout.NORTH);
        card.add(arrowPanel, BorderLayout.CENTER);

        JPanel bottomText = new JPanel();
        bottomText.setOpaque(false);
        bottomText.setLayout(new BoxLayout(bottomText, BoxLayout.Y_AXIS));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        difficultyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        bottomText.add(Box.createVerticalStrut(10));
        bottomText.add(statusLabel);
        bottomText.add(Box.createVerticalStrut(5));
        bottomText.add(difficultyLabel);

        card.add(bottomText, BorderLayout.SOUTH);
        center.add(card, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        setFocusable(true);
        addKeyListener(this);

        updatePlayerStats();    // 처음 0으로 세팅
        updateTimeLabel();
    }

    // ====== 외부에서 호출하는 메서드들 ======

    /** 게임 전체를 새로 시작 (타이머 리셋 + 1스테이지부터) */
    public void startGame() {
        resetGame();
        startTimer();
        startNewStageInternal();
    }

    /** 결과 화면에서 돌아올 때 등 필요하면 사용 가능 */
    public void resetGame() {
        stage = 1;
        score = 0;
        combo = 0;
        maxCombo = 0;
        remainingSeconds = 60;

        if (gameTimer != null) {
            gameTimer.stop();
            gameTimer = null;
        }
        updatePlayerStats();
        updateTimeLabel();
    }

    public void setOnGameEndListener(GameEndListener listener) {
        this.onGameEndListener = listener;
    }

    /** 나중에 서버에서 닉네임 내려줄 때 사용 가능 */
    public void setPlayerName(int index, String name) {
        if (index >= 0 && index < playerNameLabels.length) {
            playerNameLabels[index].setText(name);
        }
    }

    // ====== 내부 게임 진행 로직 ======

    private void startNewStageInternal() {
        requestFocusInWindow();
        generateSequence();
        currentIndex = 0;
        bigMessageLabel.setText("");
        statusLabel.setText("화살표 키를 순서대로 눌러주세요!");
        arrowPanel.repaint();
    }

    private void startTimer() {
        gameTimer = new Timer(1000, e -> {
            remainingSeconds--;
            updateTimeLabel();
            if (remainingSeconds <= 0) {
                gameTimer.stop();
                if (onGameEndListener != null) {
                    onGameEndListener.onGameEnd(score, maxCombo);
                }
            }
        });
        gameTimer.start();
    }

    private void updatePlayerStats() {
        // 지금은 플레이어1(인덱스 0)만 사용
        playerScoreLabels[0].setText("성공: " + score);
        playerComboLabels[0].setText("콤보: " + combo);
    }

    private void updateTimeLabel() {
        if (timeValueLabel != null) {
            timeValueLabel.setText(remainingSeconds + "초");
        }
    }

    private void generateSequence() {
        int length = 3 + stage - 1; // 1스테이지=3개, 이후 1씩 증가
        sequence.clear();
        arrowColors.clear();

        Random rnd = new Random();
        Direction[] values = Direction.values();

        for (int i = 0; i < length; i++) {
            Direction d = values[rnd.nextInt(values.length)];
            sequence.add(d);

            // 5스테이지 이상이면 방향과 상관없이 랜덤 색
            if (stage >= 5) {
                arrowColors.add(randomColor(rnd));
            } else {
                arrowColors.add(defaultColor(d));
            }
        }

        difficultyLabel.setText("난이도: " + length + "개 화살표");
        arrowPanel.setSequence(sequence);
    }

    // 서버에서 시퀀스를 내려줄 때 사용하도록 준비
    public void setSequenceFromServer(List<Direction> seq, int stageNumber) {
        this.stage = stageNumber;
        sequence.clear();
        sequence.addAll(seq);
        arrowColors.clear();

        Random rnd = new Random();
        for (Direction d : sequence) {
            if (stage >= 5) {
                arrowColors.add(randomColor(rnd));
            } else {
                arrowColors.add(defaultColor(d));
            }
        }

        currentIndex = 0;
        arrowPanel.setSequence(sequence);
        arrowPanel.setCurrentIndex(0);
        arrowPanel.repaint();
    }

    private Color defaultColor(Direction d) {
        switch (d) {
            case UP:    return new Color(255, 120, 120);
            case DOWN:  return new Color(120, 180, 255);
            case LEFT:  return new Color(120, 180, 255);
            case RIGHT: return new Color(120, 200, 120);
            default:    return Color.BLACK;
        }
    }

    private Color randomColor(Random rnd) {
        Color[] palette = {
                new Color(255, 120, 120),
                new Color(120, 200, 120),
                new Color(120, 180, 255),
                new Color(255, 190, 120),
                new Color(200, 120, 255)
        };
        return palette[rnd.nextInt(palette.length)];
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (sequence.isEmpty()) return;

        Direction inputDir = null;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:    inputDir = Direction.UP;    break;
            case KeyEvent.VK_DOWN:  inputDir = Direction.DOWN;  break;
            case KeyEvent.VK_LEFT:  inputDir = Direction.LEFT;  break;
            case KeyEvent.VK_RIGHT: inputDir = Direction.RIGHT; break;
        }
        if (inputDir == null) return;

        Direction correct = sequence.get(currentIndex);
        if (inputDir == correct) {
            currentIndex++;
            arrowPanel.setCurrentIndex(currentIndex);
            arrowPanel.repaint();

            if (currentIndex == sequence.size()) {
                // 스테이지 클리어
                bigMessageLabel.setForeground(new Color(0, 200, 120));
                bigMessageLabel.setText("PERFECT!");
                score++;
                combo++;
                maxCombo = Math.max(maxCombo, combo);
                updatePlayerStats();

                // 잠깐 기다렸다가 다음 스테이지
                Timer t = new Timer(200, evt -> {
                    stage++;
                    startNewStageInternal();
                });
                t.setRepeats(false);
                t.start();
            }
        } else {
            // 실패
            bigMessageLabel.setForeground(new Color(230, 80, 80));
            bigMessageLabel.setText("다시!");
            combo = 0;
            currentIndex = 0;
            arrowPanel.setCurrentIndex(0);
            arrowPanel.repaint();
            updatePlayerStats();
        }

        // 예시: stage가 10 넘어가면 게임 종료 (원하면 조절)
        if (stage > 10 && onGameEndListener != null) {
            if (gameTimer != null) gameTimer.stop();
            onGameEndListener.onGameEnd(score, maxCombo);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    // ====== 화살표들을 그리는 내부 패널 ======
    class ArrowPanel extends JPanel {
        private List<Direction> seq = new ArrayList<>();
        private int curIndex = 0;

        ArrowPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(820, 180)); // 가로 폭 넓게
        }

        void setSequence(List<Direction> s) {
            seq = new ArrayList<>(s);
            curIndex = 0;
        }

        void setCurrentIndex(int idx) {
            curIndex = idx;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (seq == null || seq.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int n = seq.size();
            int width = getWidth();
            int height = getHeight();

            int maxRadius = Math.min(50, height / 3);  // 기본 최대 반지름
            int margin = 40;                           // 좌우 여백
            int maxWidthPerCircle = (width - margin) / n;
            int radius = Math.min(maxRadius, maxWidthPerCircle / 2);

            int spacing = (width - margin) / n;       // 원 중심 간 간격
            int startX = margin / 2 + spacing / 2;
            int centerY = height / 2;

            for (int i = 0; i < n; i++) {
                int cx = startX + i * spacing;
                int cy = centerY;

                // 배경 원 (진행 상황에 따라 색 구분)
                if (i < curIndex) {
                    g2.setColor(new Color(210, 240, 210));
                } else if (i == curIndex) {
                    g2.setColor(new Color(200, 230, 255));
                } else {
                    g2.setColor(new Color(235, 245, 255));
                }
                g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

                g2.setColor(new Color(180, 210, 230));
                g2.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);

                // 화살표 심볼
                Direction d = seq.get(i);
                Color arrowColor;
                if (i < arrowColors.size()) {
                    arrowColor = arrowColors.get(i);
                } else {
                    arrowColor = defaultColor(d);
                }
                g2.setColor(arrowColor);
                g2.setFont(new Font("Dialog", Font.BOLD, 28));
                FontMetrics fm = g2.getFontMetrics();
                String symbol = d.getSymbol();
                int tw = fm.stringWidth(symbol);
                int th = fm.getAscent();
                g2.drawString(symbol, cx - tw / 2, cy + th / 4);
            }
        }
    }
}
