package client;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ResultPanel extends JPanel {

    JLabel titleLabel = new JLabel("게임 결과", SwingConstants.CENTER);
    JTable rankingTable;
    DefaultTableModel tableModel;
    JButton exitButton = new JButton("로비로 이동");

    public interface ExitListener { void onExit(); }

    private ExitListener exitListener;

    public ResultPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(224, 245, 255));

        // 상단 타이틀
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 40));
        titleLabel.setForeground(new Color(80, 190, 255));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        add(titleLabel, BorderLayout.NORTH);

        // 중앙 랭킹 테이블
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));

        String[] columnNames = {"순위", "플레이어", "점수", "정답", "최고 콤보"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        rankingTable = new JTable(tableModel);
        rankingTable.setFont(new Font("Dialog", Font.PLAIN, 16));
        rankingTable.setRowHeight(45);
        rankingTable.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 18));
        rankingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 모든 열 중앙 정렬
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < 5; i++) {
            rankingTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // 1등 행 강조
        rankingTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (row == 0 && table.getRowCount() > 0) {
                    c.setBackground(new Color(255, 215, 0, 100)); // 금색 배경
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    c.setBackground(Color.WHITE);
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                }
                ((JLabel) c).setHorizontalAlignment(JLabel.CENTER);
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(rankingTable);
        scrollPane.setBackground(Color.WHITE);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // 하단 버튼
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        exitButton.setPreferredSize(new Dimension(200, 50));
        exitButton.setBackground(new Color(120, 180, 255));
        exitButton.setForeground(Color.WHITE);
        exitButton.setFont(new Font("Dialog", Font.BOLD, 18));

        bottomPanel.add(exitButton);
        add(bottomPanel, BorderLayout.SOUTH);

        exitButton.addActionListener(e -> {
            if (exitListener != null) exitListener.onExit();
        });
    }

    // 랭킹 정보 설정
    public void setRankingResult(List<PlayerRankInfo> rankings) {
        rankings.sort((a, b) -> Integer.compare(b.score, a.score));
        tableModel.setRowCount(0);

        int currentRank = 0;
        int prevScore = Integer.MIN_VALUE;

        for (int i = 0; i < rankings.size(); i++) {
            PlayerRankInfo info = rankings.get(i);
            if (info.score != prevScore) {
                currentRank = i + 1;
                prevScore = info.score;
            }

            String rank = (i + 1) + "위";
            if (i == 0) rank = "🥇 1위";
            else if (i == 1) rank = "🥈 2위";
            else if (i == 2) rank = "🥉 3위";

            Object[] row = {
                    rank,
                    info.playerName,
                    String.format("%,d점", info.score),
                    info.successCount + "개",
                    info.maxCombo + " 콤보"
            };
            tableModel.addRow(row);
        }
    }

    // 이전 버전 호환성을 위한 메서드 (deprecated)
    @Deprecated
    public void setResult(int score, int maxCombo) {
        // 단일 플레이어 결과는 더 이상 사용하지 않음
        List<PlayerRankInfo> rankings = new ArrayList<>();
        rankings.add(new PlayerRankInfo("나", score, 0, maxCombo));
        setRankingResult(rankings);
    }

    public void setOnExitListener(ExitListener l) {
        this.exitListener = l;
    }

    // 플레이어 랭킹 정보 클래스
    public static class PlayerRankInfo {
        public String playerName;
        public int score;
        public int successCount;
        public int maxCombo;

        public PlayerRankInfo(String playerName, int score, int successCount, int maxCombo) {
            this.playerName = playerName;
            this.score = score;
            this.successCount = successCount;
            this.maxCombo = maxCombo;
        }

    }
}
