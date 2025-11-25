package client;

import network.GameClient;
import network.GameServer;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ArrowGameClientApp extends JFrame {

    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    private RoomListPanel roomListPanel;
    private LobbyPanel lobbyPanel;
    private GamePanel gamePanel;
    private ResultPanel resultPanel;
    private GameClient gameClient; // 실제 소켓 클라이언트
    private String myNickname; // 내 닉네임
    private String currentRoomId; // 현재 방 ID

    public ArrowGameClientApp() {
        setTitle("리듬 화살표 게임 (클라이언트)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 720);
        setLocationRelativeTo(null);

        roomListPanel = new RoomListPanel();
        lobbyPanel = new LobbyPanel();
        gamePanel = new GamePanel();
        resultPanel = new ResultPanel();

        mainPanel.add(roomListPanel, "ROOM_LIST");
        mainPanel.add(lobbyPanel, "LOBBY");
        mainPanel.add(gamePanel, "GAME");
        mainPanel.add(resultPanel, "RESULT");
        add(mainPanel);

        // ---- 네트워크 연결 시도 ----
        initNetwork();

        // ---- 방 목록 콜백 ----
        roomListPanel.setOnRoomActionListener(new RoomListPanel.OnRoomActionListener() {
            @Override
            public void onCreateRoom(String roomName) {
                if (gameClient != null) {
                    try {
                        gameClient.send("CREATE_ROOM " + roomName);
                    } catch (IOException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(ArrowGameClientApp.this,
                                "방 생성 중 오류: " + e.getMessage(),
                                "네트워크 오류",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }

            @Override
            public void onJoinRoom(String roomId) {
                if (gameClient != null) {
                    try {
                        gameClient.send("JOIN_ROOM " + roomId);
                    } catch (IOException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(ArrowGameClientApp.this,
                                "방 입장 중 오류: " + e.getMessage(),
                                "네트워크 오류",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        roomListPanel.setNetworkSender(msg -> {
            if (gameClient != null) {
                try {
                    gameClient.send(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // ---- 화면 전환 콜백 ----
        lobbyPanel.setOnStartGameListener(() -> {
            // 방장이 게임 시작 버튼을 누르면 서버에 요청
            if (gameClient != null) {
                try {
                    gameClient.send("START_GAME_REQUEST");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // 더 이상 로컬에서 게임 종료를 처리하지 않음
        // 서버의 GAME_RANKING과 GAME_END 메시지를 기다림
        // gamePanel.setOnGameEndListener((score, maxCombo) -> {
        //     resultPanel.setResult(score, maxCombo);
        //     cardLayout.show(mainPanel, "RESULT");
        // });

        // GamePanel의 입력을 서버로 전송
        gamePanel.setInputSender(result -> {
            if (gameClient != null) {
                try {
                    gameClient.send("PLAYER_INPUT " + result);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        resultPanel.setOnExitListener(() -> {
            // 로비로 돌아가기 (방은 유지)
            cardLayout.show(mainPanel, "LOBBY");
            gamePanel.resetGame();
        });

        // 로비에서 방 나가기
        lobbyPanel.setOnLeaveRoomListener(() -> {
            if (gameClient != null && currentRoomId != null) {
                try {
                    gameClient.send("LEAVE_ROOM");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        setVisible(true);
    }

    private void initNetwork() {
        try {
            gameClient = new GameClient("127.0.0.1", GameServer.PORT);
            System.out.println("Connected to GameServer.");

            // LobbyPanel에서 문자열을 보내고 싶을 때 사용할 sender 지정
            lobbyPanel.setNetworkSender(msg -> {
                if (gameClient != null) {
                    try {
                        gameClient.send(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(this,
                                "서버 전송 중 오류: " + e.getMessage(),
                                "네트워크 오류",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            // 닉네임 입력 받기
            String myName = null;
            while (myName == null || myName.trim().isEmpty()) {
                myName = JOptionPane.showInputDialog(
                        this,
                        "닉네임을 입력하세요:",
                        "닉네임 설정",
                        JOptionPane.PLAIN_MESSAGE
                );

                if (myName == null) {
                    // 취소 버튼 누르면 종료
                    gameClient.close();
                    System.exit(0);
                    return;
                }

                myName = myName.trim();
                if (myName.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "닉네임을 입력해주세요!",
                            "오류",
                            JOptionPane.ERROR_MESSAGE);
                }
            }

            final String finalName = myName;
            myNickname = myName; // 내 닉네임 저장

            // 서버에서 오는 메시지 처리 (JOIN 응답 포함)
            final boolean[] joinAccepted = {false};
            gameClient.setListener(msg -> {
                SwingUtilities.invokeLater(() -> {
                    if (msg.startsWith("JOIN_FAILED ")) {
                        String reason = msg.substring(12);
                        JOptionPane.showMessageDialog(this,
                                reason,
                                "접속 실패",
                                JOptionPane.ERROR_MESSAGE);
                        gameClient.close();
                        System.exit(0);
                    } else if (msg.equals("JOIN_OK")) {
                        joinAccepted[0] = true;
                        // 닉네임 설정 성공, 방 목록 화면으로 이동
                        cardLayout.show(mainPanel, "ROOM_LIST");
                        // 방 목록 자동 새로고침 시작
                        roomListPanel.startAutoRefresh();
                    } else {
                        handleServerMessage(msg);
                    }
                });
            });

            // 서버로 JOIN 메시지 전송
            gameClient.send("JOIN " + myName);

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "서버에 연결할 수 없습니다.\n(로컬 싱글 플레이 모드로 진행합니다.)",
                    "서버 연결 실패",
                    JOptionPane.WARNING_MESSAGE);
            // gameClient == null 이면, 그냥 로컬 UI로만 동작 (채팅은 본인 화면에만 출력)
        }
    }

    // 서버에서 온 문자열을 해석해서 UI에 반영
    private void handleServerMessage(String msg) {
        System.out.println("From server: " + msg);

        if (msg.startsWith("ROOM_LIST")) {
                // ROOM_LIST;roomId|roomName|current|max|inGame;roomId2|...
                java.util.List<RoomListPanel.RoomInfo> rooms = new java.util.ArrayList<>();
                System.out.println("[DEBUG CLIENT] ===== ROOM_LIST PARSING START =====");
                System.out.println("[DEBUG CLIENT] Full message: " + msg);
                String data = msg.substring("ROOM_LIST".length());
                System.out.println("[DEBUG CLIENT] Data after substring: [" + data + "]");
                System.out.println("[DEBUG CLIENT] Data length: " + data.length());

                if (data.length() > 0) {
                    String[] roomEntries = data.split(";");
                    System.out.println("[DEBUG CLIENT] Split result - array length: " + roomEntries.length);
                    for (int i = 0; i < roomEntries.length; i++) {
                        System.out.println("[DEBUG CLIENT] Entry[" + i + "]: [" + roomEntries[i] + "]");
                    }

                    for (String roomEntry : roomEntries) {
                        if (roomEntry.trim().isEmpty()) {
                            System.out.println("[DEBUG CLIENT] Skipping empty entry");
                            continue;
                        }

                        String[] roomData = roomEntry.split("\\|");
                        System.out.println("[DEBUG CLIENT] Room data parts: " + roomData.length);

                        if (roomData.length >= 5) {
                            String roomId = roomData[0];
                            String roomName = roomData[1];
                            int currentPlayers = Integer.parseInt(roomData[2]);
                            int maxPlayers = Integer.parseInt(roomData[3]);
                            boolean inGame = Boolean.parseBoolean(roomData[4]);
                            System.out.println("[DEBUG CLIENT] Adding room - ID: " + roomId + ", Name: " + roomName + " (" + currentPlayers + "/" + maxPlayers + "), InGame: " + inGame);
                            rooms.add(new RoomListPanel.RoomInfo(roomId, roomName, currentPlayers, maxPlayers, inGame));
                        } else {
                            System.out.println("[DEBUG CLIENT] Invalid room data - expected 5 parts, got " + roomData.length);
                        }
                    }
                } else {
                    System.out.println("[DEBUG CLIENT] No room data (data length is 0)");
                }
                System.out.println("[DEBUG CLIENT] Total rooms to display: " + rooms.size());
                System.out.println("[DEBUG CLIENT] ===== ROOM_LIST PARSING END =====");
                roomListPanel.updateRoomList(rooms);

        } else if (msg.startsWith("ROOM_JOINED ")) {
                // ROOM_JOINED roomId|roomName
                String data = msg.substring("ROOM_JOINED ".length());
                String[] roomData = data.split("\\|", 2); // 2개로만 분리 (roomId와 나머지)
                if (roomData.length >= 2) {
                    currentRoomId = roomData[0];
                    String roomName = roomData[1];
                    lobbyPanel.setRoomTitle(roomName);
                } else {
                    currentRoomId = roomData[0];
                }
                // 방 목록 자동 새로고침 정지
                roomListPanel.stopAutoRefresh();
                cardLayout.show(mainPanel, "LOBBY");

        } else if (msg.equals("LEFT_ROOM")) {
                // 방 나가기 성공
                currentRoomId = null;
                cardLayout.show(mainPanel, "ROOM_LIST");
                // 방 목록 자동 새로고침 시작
                roomListPanel.startAutoRefresh();

        } else if (msg.startsWith("JOIN_ROOM_FAILED")) {
                // JOIN_ROOM_FAILED 메시지
                String reason = msg.length() > 17 ? msg.substring(17) : "방에 입장할 수 없습니다.";
                JOptionPane.showMessageDialog(this,
                        reason,
                        "입장 실패",
                        JOptionPane.WARNING_MESSAGE);

        } else if (msg.startsWith("SYS ")) {
                // SYS 시스템메시지
                String text = msg.substring(4);
                lobbyPanel.addChatMessage("[시스템] " + text);

        } else if (msg.startsWith("CHAT ")) {
                // CHAT 닉네임 내용
                String[] parts = msg.split(" ", 3);
                if (parts.length >= 3) {
                    String nick = parts[1];
                    String text = parts[2];
                    lobbyPanel.addChatMessage(nick + ": " + text);
                }

        } else if (msg.startsWith("PLAYER_LIST ")) {
                // PLAYER_LIST player1|ready|isHost|score|combo|maxCombo player2|...
                lobbyPanel.clearPlayers();
                gamePanel.clearPlayers();

                // 서버에서 받은 플레이어 목록을 파싱
                String[] parts = msg.split(" ");
                java.util.List<PlayerInfo> playerList = new java.util.ArrayList<>();
                for (int i = 1; i < parts.length; i++) {
                    String[] playerData = parts[i].split("\\|");
                    if (playerData.length >= 6) {
                        String name = playerData[0];
                        boolean ready = Boolean.parseBoolean(playerData[1]);
                        boolean isHost = Boolean.parseBoolean(playerData[2]);
                        int score = Integer.parseInt(playerData[3]);
                        int combo = Integer.parseInt(playerData[4]);

                        playerList.add(new PlayerInfo(name, ready, isHost, score, combo));
                    }
                }

                // 자기 자신을 0번에 배치
                boolean imHost = false;
                for (PlayerInfo info : playerList) {
                    if (info.name.equals(myNickname)) {
                        lobbyPanel.setPlayerInfo(0, info.name, info.ready, info.isHost);
                        gamePanel.setPlayerInfo(0, info.name, info.score, info.combo);
                        imHost = info.isHost;
                        break;
                    }
                }

                // 나머지 플레이어들을 1, 2, 3번에 배치
                int slot = 1;
                java.util.List<String> otherPlayers = new java.util.ArrayList<>();
                for (PlayerInfo info : playerList) {
                    if (!info.name.equals(myNickname) && slot < 4) {
                        lobbyPanel.setPlayerInfo(slot, info.name, info.ready, info.isHost);
                        gamePanel.setPlayerInfo(slot, info.name, info.score, info.combo);
                        otherPlayers.add(info.name);
                        slot++;
                    }
                }

                // 방장 여부와 다른 플레이어 목록 업데이트
                lobbyPanel.updateHostStatus(imHost, otherPlayers);

        } else if (msg.equals("START_GAME")) {
                // 게임 시작 명령
                cardLayout.show(mainPanel, "GAME");
                gamePanel.prepareGame();
                lobbyPanel.addChatMessage("[시스템] 게임이 시작됩니다!");

        } else if (msg.startsWith("GAME_SEQUENCE ")) {
                // GAME_SEQUENCE stage UP DOWN LEFT RIGHT ...
                String[] parts = msg.split(" ");
                if (parts.length >= 2) {
                    int stage = Integer.parseInt(parts[1]);
                    String[] directions = new String[parts.length - 2];
                    System.arraycopy(parts, 2, directions, 0, directions.length);
                    gamePanel.setSequenceFromServer(directions, stage);
                }

        } else if (msg.startsWith("GAME_RANKING ")) {
                // 게임 랭킹 정보
                // GAME_RANKING name1|score1|success1|combo1 name2|score2|success2|combo2 ...
                System.out.println("[DEBUG CLIENT] ===== GAME_RANKING RECEIVED =====");
                System.out.println("[DEBUG CLIENT] Full message: " + msg);

                String[] parts = msg.split(" ");
                System.out.println("[DEBUG CLIENT] Split parts: " + parts.length);

                java.util.List<ResultPanel.PlayerRankInfo> rankings = new java.util.ArrayList<>();

                for (int i = 1; i < parts.length; i++) {
                    System.out.println("[DEBUG CLIENT] Processing part[" + i + "]: " + parts[i]);
                    String[] playerData = parts[i].split("\\|");
                    System.out.println("[DEBUG CLIENT]   Split into " + playerData.length + " parts");

                    if (playerData.length >= 4) {
                        String name = playerData[0];
                        int score = Integer.parseInt(playerData[1]);
                        int successCount = Integer.parseInt(playerData[2]);
                        int maxCombo = Integer.parseInt(playerData[3]);
                        System.out.println("[DEBUG CLIENT]   Player: " + name + ", Score: " + score
                                + ", Success: " + successCount + ", Combo: " + maxCombo);
                        rankings.add(new ResultPanel.PlayerRankInfo(name, score, successCount, maxCombo));
                    }
                }

                System.out.println("[DEBUG CLIENT] Total rankings: " + rankings.size());
                System.out.println("[DEBUG CLIENT] ================================");

                resultPanel.setRankingResult(rankings);

        } else if (msg.equals("GAME_END")) {
                // 게임 종료 - 결과 화면으로 이동
                cardLayout.show(mainPanel, "RESULT");

        } else {
                // 알 수 없는 메시지는 그냥 채팅창에 표시
                lobbyPanel.addChatMessage(msg);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ArrowGameClientApp::new);
    }

    // 플레이어 정보를 담는 간단한 클래스
    private static class PlayerInfo {
        String name;
        boolean ready;
        boolean isHost;
        int score;
        int combo;

        PlayerInfo(String name, boolean ready, boolean isHost, int score, int combo) {
            this.name = name;
            this.ready = ready;
            this.isHost = isHost;
            this.score = score;
            this.combo = combo;
        }
    }
}
