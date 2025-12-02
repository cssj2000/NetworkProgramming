package client;

import javax.swing.*;
import java.awt.*;
import javax.sound.sampled.*;

public class LobbyPanel extends JPanel {

    JTextField[] nameFields = new JTextField[4];
    JToggleButton[] readyButtons = new JToggleButton[4];
    JLabel[] hostLabels = new JLabel[4]; // 방장 표시 라벨
    JButton startButton = new JButton("게임 시작!");
    JButton transferHostButton = new JButton("방장 위임");
    JButton leaveRoomButton = new JButton("방 나가기");
    JTextArea chatArea = new JTextArea();
    JTextField chatInput = new JTextField();
    JButton sendButton = new JButton("전송");
    JLabel titleLabel; // 방 제목 라벨
    JButton kickButton = new JButton("강퇴"); // ⬅⬅ 새로 추가
    JList<String> playerListUI = new JList<>(); // ⬅⬅ 강퇴 대상 선택 리스트


    private boolean isHost = false;
    private java.util.List<String> otherPlayerNames = new java.util.ArrayList<>();
    private Clip chatClip;
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

    // ---- 방 나가기 콜백 ----
    public interface OnLeaveRoomListener {
        void onLeaveRoom();
    }

    private OnLeaveRoomListener onLeaveRoomListener;

    public LobbyPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(224, 245, 255));

        // 상단 타이틀
        titleLabel = new JLabel("리듬 화살표 게임", SwingConstants.LEFT);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 40));
        titleLabel.setForeground(new Color(80, 190, 255));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        add(titleLabel, BorderLayout.NORTH);

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

            // 방장 표시 라벨
            hostLabels[i] = new JLabel("👑 방장");
            hostLabels[i].setFont(new Font("Dialog", Font.BOLD, 14));
            hostLabels[i].setForeground(new Color(255, 180, 0));
            hostLabels[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            hostLabels[i].setVisible(false); // 기본적으로 숨김

            nameFields[i] = new JTextField("플레이어" + (i + 1));
            nameFields[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            // 🔥 로비에서는 절대 닉네임 변경 불가 + 커서 깜빡임 제거
            nameFields[i].setEditable(false);            // 타이핑 불가
            nameFields[i].setFocusable(false);           // 클릭해도 커서 안 뜸
            nameFields[i].setCursor(Cursor.getDefaultCursor());  // I-bar 커서 제거

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
            p.add(Box.createVerticalStrut(4));
            p.add(hostLabels[i]);
            p.add(Box.createVerticalStrut(8));
            p.add(nameFields[i]);
            p.add(Box.createVerticalStrut(10));
            p.add(readyButtons[i]);
            playersPanel.add(p);
        }
        // ===== 방장용 플레이어 선택 리스트 UI =====
        playerListUI.setPreferredSize(new Dimension(200, 100));
        playerListUI.setForeground(Color.BLACK);
        playerListUI.setBackground(Color.WHITE);
        playerListUI.setBorder(BorderFactory.createTitledBorder("플레이어 목록 (강퇴 선택)"));
        add(playerListUI, BorderLayout.WEST);
        playerListUI.setVisible(false);


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

        // -------- 하단: 게임 시작 버튼 + 방장 위임 버튼 --------
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // 버튼 패널 (방 나가기 + 방장 위임 + 게임 시작)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);

        leaveRoomButton.setPreferredSize(new Dimension(120, 50));
        leaveRoomButton.setBackground(new Color(150, 150, 150));
        leaveRoomButton.setForeground(Color.WHITE);
        leaveRoomButton.setFont(new Font("Dialog", Font.BOLD, 16));
        leaveRoomButton.addActionListener(e -> {
            if (onLeaveRoomListener != null) onLeaveRoomListener.onLeaveRoom();
        });

        transferHostButton.setPreferredSize(new Dimension(120, 50));
        transferHostButton.setBackground(new Color(255, 180, 0));
        transferHostButton.setForeground(Color.WHITE);
        transferHostButton.setFont(new Font("Dialog", Font.BOLD, 16));
        transferHostButton.setVisible(false); // 기본적으로 숨김 (방장만 보임)
        transferHostButton.addActionListener(e -> showTransferHostDialog());
// ===== 강퇴 버튼 생성 =====
        kickButton.setPreferredSize(new Dimension(100, 50));
        kickButton.setBackground(new Color(255, 100, 100));
        kickButton.setForeground(Color.WHITE);
        kickButton.setFont(new Font("Dialog", Font.BOLD, 16));
        kickButton.setVisible(false); // 기본적으로 숨김 (방장만 보임)

        kickButton.addActionListener(e -> {
            String target = playerListUI.getSelectedValue();
            if (target != null && networkSender != null) {
                networkSender.send("KICK " + target);
            }
        });

        startButton.setEnabled(false);
        startButton.setPreferredSize(new Dimension(300, 60));
        startButton.setMaximumSize(new Dimension(300, 60));
        startButton.setBackground(new Color(120, 200, 255));
        startButton.setForeground(Color.WHITE);
        startButton.setFont(new Font("Dialog", Font.BOLD, 20));

        JLabel info = new JLabel("방장만 게임을 시작할 수 있습니다");
        info.setAlignmentX(Component.CENTER_ALIGNMENT);
        info.setForeground(new Color(170, 190, 210));

        buttonPanel.add(leaveRoomButton);
        buttonPanel.add(transferHostButton);
        buttonPanel.add(kickButton); // ⬅ 강퇴 버튼 추가됨
        bottom.add(buttonPanel);
        bottom.add(Box.createVerticalStrut(10));
        bottom.add(startButton);
        bottom.add(Box.createVerticalStrut(10));
        bottom.add(info);
        add(bottom, BorderLayout.SOUTH);

        startButton.addActionListener(e -> {
            if (onStartGameListener != null) onStartGameListener.onStartGame();
        });
    }

    // 방장 위임 다이얼로그
    private void showTransferHostDialog() {
        if (otherPlayerNames.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "방장을 위임할 다른 플레이어가 없습니다.",
                    "방장 위임",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] options = otherPlayerNames.toArray(new String[0]);
        String selected = (String) JOptionPane.showInputDialog(
                this,
                "방장을 위임할 플레이어를 선택하세요:",
                "방장 위임",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );

        if (selected != null && networkSender != null) {
            networkSender.send("TRANSFER_HOST " + selected);
        }
    }

    private void updateStartButton() {
        // 방장만 게임 시작 가능
        if (!isHost) {
            startButton.setEnabled(false);
            return;
        }

        // 모든 플레이어가 준비했는지 확인
        boolean allReady = false;
        for (JToggleButton b : readyButtons) {
            if (b.isSelected()) {
                allReady = true;
                break;
            }
        }
        startButton.setEnabled(allReady);
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
    // ✅ 채팅 알림음 재생

    // ---- 서버에서 온 내용을 채팅창에 추가할 때 사용 ----
    public void addChatMessage(String msg) {
        chatArea.append(msg + "\n");
        SoundPlayer.playForMillis("/sounds/chat.wav", this, 500);

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

    public void setOnLeaveRoomListener(OnLeaveRoomListener listener) {
        this.onLeaveRoomListener = listener;
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

    public void setPlayerInfo(int index, String name, boolean ready, boolean isHost) {
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

            // 방장 표시
            hostLabels[index].setVisible(isHost);

            // 첫 번째 플레이어(본인)만 버튼 활성화
            readyButtons[index].setEnabled(index == 0);

            updateStartButton();
        }
    }

    // 방장 상태 업데이트
    public void updateHostStatus(boolean imHost, java.util.List<String> otherPlayers) {
        this.isHost = imHost;
        this.otherPlayerNames = new java.util.ArrayList<>(otherPlayers);

        // 방장 위임 버튼 표시 여부
        transferHostButton.setVisible(imHost && !otherPlayers.isEmpty());

        kickButton.setVisible(imHost); // ⬅ 방장만 강퇴 버튼 보임
        playerListUI.setVisible(imHost);
// playerListUI에 플레이어 목록 갱신
        playerListUI.setListData(otherPlayers.toArray(new String[0]));

        // 게임 시작 버튼 활성화 여부 (방장만 + 모두 준비)
        updateStartButton();
    }

    // 방 제목 설정
    public void setRoomTitle(String roomName) {
        titleLabel.setText(roomName);
    }
}
