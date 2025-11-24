package client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RoomListPanel extends JPanel {

    private JTable roomTable;
    private DefaultTableModel tableModel;
    private JButton createRoomButton = new JButton("방 만들기");
    private JButton joinRoomButton = new JButton("방 입장");
    private JButton refreshButton = new JButton("새로고침");

    private List<RoomInfo> rooms = new ArrayList<>();
    private Timer refreshTimer; // 자동 새로고침 타이머

    public interface NetworkSender {
        void send(String msg);
    }

    public interface OnRoomActionListener {
        void onCreateRoom(String roomName);
        void onJoinRoom(String roomId);
    }

    private NetworkSender networkSender;
    private OnRoomActionListener roomActionListener;

    public RoomListPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(224, 245, 255));

        // 상단 타이틀
        JLabel title = new JLabel("방 목록", SwingConstants.CENTER);
        title.setFont(new Font("Dialog", Font.BOLD, 40));
        title.setForeground(new Color(80, 190, 255));
        title.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        add(title, BorderLayout.NORTH);

        // 중앙 테이블
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        String[] columnNames = {"방 이름", "인원", "상태"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 셀 편집 불가
            }
        };
        roomTable = new JTable(tableModel);
        roomTable.setFont(new Font("Dialog", Font.PLAIN, 16));
        roomTable.setRowHeight(40);
        roomTable.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 18));
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(roomTable);
        scrollPane.setBackground(Color.WHITE);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // 하단 버튼들
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        createRoomButton.setPreferredSize(new Dimension(150, 50));
        createRoomButton.setBackground(new Color(100, 200, 100));
        createRoomButton.setForeground(Color.WHITE);
        createRoomButton.setFont(new Font("Dialog", Font.BOLD, 16));

        joinRoomButton.setPreferredSize(new Dimension(150, 50));
        joinRoomButton.setBackground(new Color(120, 180, 255));
        joinRoomButton.setForeground(Color.WHITE);
        joinRoomButton.setFont(new Font("Dialog", Font.BOLD, 16));

        refreshButton.setPreferredSize(new Dimension(120, 50));
        refreshButton.setBackground(new Color(150, 150, 150));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFont(new Font("Dialog", Font.BOLD, 16));

        bottomPanel.add(createRoomButton);
        bottomPanel.add(joinRoomButton);
        bottomPanel.add(refreshButton);

        add(bottomPanel, BorderLayout.SOUTH);

        // 버튼 리스너
        createRoomButton.addActionListener(e -> showCreateRoomDialog());
        joinRoomButton.addActionListener(e -> joinSelectedRoom());
        refreshButton.addActionListener(e -> requestRoomList());

        // 더블클릭으로 방 입장
        roomTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    joinSelectedRoom();
                }
            }
        });

        // 자동 새로고침 타이머 (2초마다)
        refreshTimer = new Timer(2000, e -> requestRoomList());
    }

    // 패널이 보일 때 타이머 시작
    public void startAutoRefresh() {
        if (refreshTimer != null && !refreshTimer.isRunning()) {
            refreshTimer.start();
            requestRoomList(); // 즉시 한 번 새로고침
        }
    }

    // 패널이 숨겨질 때 타이머 정지
    public void stopAutoRefresh() {
        if (refreshTimer != null && refreshTimer.isRunning()) {
            refreshTimer.stop();
        }
    }

    public void setNetworkSender(NetworkSender sender) {
        this.networkSender = sender;
    }

    public void setOnRoomActionListener(OnRoomActionListener listener) {
        this.roomActionListener = listener;
    }

    // 방 만들기 다이얼로그
    private void showCreateRoomDialog() {
        String roomName = JOptionPane.showInputDialog(
                this,
                "방 이름을 입력하세요:",
                "방 만들기",
                JOptionPane.PLAIN_MESSAGE
        );

        if (roomName != null && !roomName.trim().isEmpty()) {
            if (roomActionListener != null) {
                roomActionListener.onCreateRoom(roomName.trim());
            }
        }
    }

    // 선택된 방 입장
    private void joinSelectedRoom() {
        int selectedRow = roomTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "방을 선택해주세요!",
                    "알림",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (selectedRow < rooms.size()) {
            RoomInfo room = rooms.get(selectedRow);
            if (roomActionListener != null) {
                roomActionListener.onJoinRoom(room.roomId);
            }
        }
    }

    // 방 목록 요청
    public void requestRoomList() {
        if (networkSender != null) {
            networkSender.send("REQUEST_ROOM_LIST");
        }
    }

    // 방 목록 업데이트
    public void updateRoomList(List<RoomInfo> newRooms) {
        this.rooms = new ArrayList<>(newRooms);

        // 테이블 초기화
        tableModel.setRowCount(0);

        // 방 목록 추가
        for (RoomInfo room : rooms) {
            String status = room.inGame ? "게임 중" : "대기 중";
            Object[] row = {
                    room.roomName,
                    room.currentPlayers + "/" + room.maxPlayers,
                    status
            };
            tableModel.addRow(row);
        }

        // 방이 없으면 안내 메시지
        if (rooms.isEmpty()) {
            tableModel.addRow(new Object[]{"방이 없습니다", "-", "-"});
            joinRoomButton.setEnabled(false);
        } else {
            joinRoomButton.setEnabled(true);
        }
    }

    // 방 정보 클래스
    public static class RoomInfo {
        public String roomId;
        public String roomName;
        public int currentPlayers;
        public int maxPlayers;
        public boolean inGame;

        public RoomInfo(String roomId, String roomName, int currentPlayers, int maxPlayers, boolean inGame) {
            this.roomId = roomId;
            this.roomName = roomName;
            this.currentPlayers = currentPlayers;
            this.maxPlayers = maxPlayers;
            this.inGame = inGame;
        }
    }
}
