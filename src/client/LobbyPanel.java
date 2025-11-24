package client;

import javax.swing.*;
import java.awt.*;

public class LobbyPanel extends JPanel {

    JTextField[] nameFields = new JTextField[4];
    JToggleButton[] readyButtons = new JToggleButton[4];
    JButton startButton = new JButton("게임 시작!");
    JTextArea chatArea = new JTextArea();
    JTextField chatInput = new JTextField();
    JButton sendButton = new JButton("전송");

    // ---- 네트워크 쪽으로 문자열을 보내기 위한 인터페이스 ----
    public interface NetworkSender {
        void send(String msg);
    }

    private NetworkSender networkSender;

    public void setNetworkSender(NetworkSender sender) {
        this.networkSender = sender;
    }

    // ---- ArrowGameClientApp 쪽으로 게임 시작 콜백 ----
    public interface OnStartGameListener {
        void onStartGame();
    }

    private OnStartGameListener onStartGameListener;

    public LobbyPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(224, 245, 255));

        // 상단 타이틀
        JLabel title = new JLabel("리듬 화살표 게임", SwingConstants.LEFT);
        title.setFont(new Font("Dialog", Font.BOLD, 40));
        title.setForeground(new Color(80, 190, 255));
        title.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        add(title, BorderLayout.NORTH);

        // -------- 왼쪽: 플레이어 카드 --------
        JPanel playersPanel = new JPanel(new GridLayout(2, 2, 16, 16));
        playersPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));
        playersPanel.setOpaque(false);

        for (int i = 0; i < 4; i++) {
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setBackground(Color.WHITE);
            p.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 230, 255), 2),
                    BorderFactory.createEmptyBorder(10, 12, 12, 12)));

            JLabel playerLabel = new JLabel("플레이어 " + (i + 1));
            playerLabel.setFont(new Font("Dialog", Font.BOLD, 18));
            playerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            nameFields[i] = new JTextField("플레이어" + (i + 1));
            nameFields[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            readyButtons[i] = new JToggleButton("준비 완료!");
            readyButtons[i].setBackground(new Color(120, 200, 255));
            readyButtons[i].setForeground(Color.WHITE);
            readyButtons[i].setFocusPainted(false);
            readyButtons[i].setAlignmentX(Component.LEFT_ALIGNMENT);

            int idx = i;
            readyButtons[i].addActionListener(e -> {
                boolean ready = readyButtons[idx].isSelected();
                if (ready) {
                    readyButtons[idx].setText("준비");
                    readyButtons[idx].setBackground(new Color(210, 210, 210));
                    // 네트워크로 READY 전송
                    if (networkSender != null && idx == 0) { // 일단 클라이언트 자신(0번)만 전송
                        networkSender.send("READY");
                    }
                } else {
                    readyButtons[idx].setText("준비 완료!");
                    readyButtons[idx].setBackground(new Color(120, 200, 255));
                    if (networkSender != null && idx == 0) {
                        networkSender.send("UNREADY");
                    }
                }
                updateStartButton();
            });

            p.add(playerLabel);
            p.add(Box.createVerticalStrut(8));
            p.add(nameFields[i]);
            p.add(Box.createVerticalStrut(10));
            p.add(readyButtons[i]);
            playersPanel.add(p);
        }

        // -------- 오른쪽: 채팅 --------
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(Color.WHITE);
        chatPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 20));
        chatPanel.setPreferredSize(new Dimension(330, 0));

        JLabel chatTitle = new JLabel("채팅");
        chatTitle.setFont(new Font("Dialog", Font.BOLD, 24));
        chatTitle.setForeground(new Color(80, 190, 255));
        chatPanel.add(chatTitle, BorderLayout.NORTH);

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(chatArea);
        chatPanel.add(sp, BorderLayout.CENTER);

        JPanel chatInputPanel = new JPanel(new BorderLayout(10, 0));
        chatInputPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        sendButton.setPreferredSize(new Dimension(80, 30));
        chatInputPanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendChatMessage());
        chatInput.addActionListener(e -> sendChatMessage());

        // 가운데 합치기
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(playersPanel, BorderLayout.CENTER);
        center.add(chatPanel, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);

        // -------- 하단: 게임 시작 버튼 --------
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        startButton.setEnabled(false);
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.setPreferredSize(new Dimension(300, 60));
        startButton.setMaximumSize(new Dimension(300, 60));
        startButton.setBackground(new Color(120, 200, 255));
        startButton.setForeground(Color.WHITE);
        startButton.setFont(new Font("Dialog", Font.BOLD, 20));

        JLabel info = new JLabel("모든 플레이어가 준비해야 합니다");
        info.setAlignmentX(Component.CENTER_ALIGNMENT);
        info.setForeground(new Color(170, 190, 210));

        bottom.add(startButton);
        bottom.add(Box.createVerticalStrut(10));
        bottom.add(info);
        add(bottom, BorderLayout.SOUTH);

        startButton.addActionListener(e -> {
            if (onStartGameListener != null) onStartGameListener.onStartGame();
        });
    }

    private void updateStartButton() {
        boolean anyReady = false;
        for (JToggleButton b : readyButtons) {
            if (b.isSelected()) {
                anyReady = true;
                break;
            }
        }
        startButton.setEnabled(anyReady);
    }

    private void sendChatMessage() {
        String text = chatInput.getText().trim();
        if (text.isEmpty()) return;

        // 내 화면에 먼저 표시
        chatArea.append("나: " + text + "\n");

        // 서버에도 전송
        if (networkSender != null) {
            networkSender.send("CHAT " + text);
        }

        chatInput.setText("");
    }

    // ---- 서버에서 온 내용을 채팅창에 추가할 때 사용 ----
    public void addChatMessage(String msg) {
        chatArea.append(msg + "\n");
    }

    // ---- ArrowGameClientApp에서 내 닉네임 전송할 때 사용 ----
    public String getLocalPlayerName() {
        return nameFields[0].getText().trim();
    }

    // 나중에 서버에서 닉네임 내려줄 때 사용 가능
    public void setPlayerName(int index, String name) {
        if (index >= 0 && index < nameFields.length) {
            nameFields[index].setText(name);
        }
    }

    public void setOnStartGameListener(OnStartGameListener listener) {
        this.onStartGameListener = listener;
    }

    // ---- 플레이어 정보 동기화 메서드 ----
    public void clearPlayers() {
        for (int i = 0; i < 4; i++) {
            nameFields[i].setText("플레이어" + (i + 1));
            readyButtons[i].setSelected(false);
            readyButtons[i].setText("준비 완료!");
            readyButtons[i].setBackground(new Color(120, 200, 255));
            readyButtons[i].setEnabled(i == 0); // 첫 번째만 활성화
        }
        updateStartButton();
    }

    public void setPlayerInfo(int index, String name, boolean ready) {
        if (index >= 0 && index < 4) {
            nameFields[index].setText(name);
            readyButtons[index].setSelected(ready);
            if (ready) {
                readyButtons[index].setText("준비");
                readyButtons[index].setBackground(new Color(210, 210, 210));
            } else {
                readyButtons[index].setText("준비 완료!");
                readyButtons[index].setBackground(new Color(120, 200, 255));
            }

            // 첫 번째 플레이어(본인)만 버튼 활성화
            readyButtons[index].setEnabled(index == 0);

            updateStartButton();
        }
    }
}
