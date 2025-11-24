package client;

import network.GameClient;
import network.GameServer;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ArrowGameClientApp extends JFrame {

    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    private LobbyPanel lobbyPanel;
    private GamePanel gamePanel;
    private ResultPanel resultPanel;
    private GameClient gameClient; // 실제 소켓 클라이언트
    private String myNickname; // 내 닉네임

    public ArrowGameClientApp() {
        setTitle("리듬 화살표 게임 (클라이언트)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 720);
        setLocationRelativeTo(null);

        lobbyPanel = new LobbyPanel();
        gamePanel = new GamePanel();
        resultPanel = new ResultPanel();

        mainPanel.add(lobbyPanel, "LOBBY");
        mainPanel.add(gamePanel, "GAME");
        mainPanel.add(resultPanel, "RESULT");
        add(mainPanel);

        // ---- 네트워크 연결 시도 ----
        initNetwork();

        // ---- 화면 전환 콜백 ----
        lobbyPanel.setOnStartGameListener(() -> {
            // 로비에서 게임 시작 버튼을 눌러도 서버가 시작을 관리하므로 아무것도 안 함
            // 서버에서 START_GAME 메시지가 오면 게임 시작
        });

        gamePanel.setOnGameEndListener((score, maxCombo) -> {
            resultPanel.setResult(score, maxCombo);
            cardLayout.show(mainPanel, "RESULT");
        });

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

        resultPanel.setOnRetryListener(() -> {
            cardLayout.show(mainPanel, "GAME");
            gamePanel.startGame();
        });

        resultPanel.setOnExitListener(() -> System.exit(0));

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
                        lobbyPanel.setPlayerName(0, finalName);
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
        String[] parts = msg.split(" ");
        if (parts.length == 0) return;

        String cmd = parts[0];

        switch (cmd) {
            case "SYS": {
                // SYS 시스템메시지
                String text = (parts.length >= 2) ? msg.substring(4) : "";
                lobbyPanel.addChatMessage("[시스템] " + text);
                break;
            }
            case "CHAT": {
                // CHAT 닉네임 내용
                if (parts.length >= 3) {
                    String nick = parts[1];
                    String text = msg.substring(6 + nick.length());
                    lobbyPanel.addChatMessage(nick + ": " + text);
                }
                break;
            }
            case "PLAYER_LIST": {
                // PLAYER_LIST player1|ready|score|combo|maxCombo player2|...
                lobbyPanel.clearPlayers();
                gamePanel.clearPlayers();

                // 서버에서 받은 플레이어 목록을 파싱
                java.util.List<PlayerInfo> playerList = new java.util.ArrayList<>();
                for (int i = 1; i < parts.length; i++) {
                    String[] playerData = parts[i].split("\\|");
                    if (playerData.length >= 5) {
                        String name = playerData[0];
                        boolean ready = Boolean.parseBoolean(playerData[1]);
                        int score = Integer.parseInt(playerData[2]);
                        int combo = Integer.parseInt(playerData[3]);

                        playerList.add(new PlayerInfo(name, ready, score, combo));
                    }
                }

                // 자기 자신을 0번에 배치
                int slot = 0;
                for (PlayerInfo info : playerList) {
                    if (info.name.equals(myNickname)) {
                        lobbyPanel.setPlayerInfo(0, info.name, info.ready);
                        gamePanel.setPlayerInfo(0, info.name, info.score, info.combo);
                        break;
                    }
                }

                // 나머지 플레이어들을 1, 2, 3번에 배치
                slot = 1;
                for (PlayerInfo info : playerList) {
                    if (!info.name.equals(myNickname) && slot < 4) {
                        lobbyPanel.setPlayerInfo(slot, info.name, info.ready);
                        gamePanel.setPlayerInfo(slot, info.name, info.score, info.combo);
                        slot++;
                    }
                }
                break;
            }
            case "START_GAME": {
                // 게임 시작 명령
                cardLayout.show(mainPanel, "GAME");
                gamePanel.prepareGame();
                lobbyPanel.addChatMessage("[시스템] 게임이 시작됩니다!");
                break;
            }
            case "GAME_SEQUENCE": {
                // GAME_SEQUENCE stage UP DOWN LEFT RIGHT ...
                if (parts.length >= 2) {
                    int stage = Integer.parseInt(parts[1]);
                    String[] directions = new String[parts.length - 2];
                    System.arraycopy(parts, 2, directions, 0, directions.length);
                    gamePanel.setSequenceFromServer(directions, stage);
                }
                break;
            }
            case "GAME_END": {
                // 게임 종료
                int score = gamePanel.getScore();
                int maxCombo = gamePanel.getMaxCombo();
                resultPanel.setResult(score, maxCombo);
                cardLayout.show(mainPanel, "RESULT");
                break;
            }
            default: {
                // 알 수 없는 메시지는 그냥 채팅창에 표시
                lobbyPanel.addChatMessage(msg);
                break;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ArrowGameClientApp::new);
    }

    // 플레이어 정보를 담는 간단한 클래스
    private static class PlayerInfo {
        String name;
        boolean ready;
        int score;
        int combo;

        PlayerInfo(String name, boolean ready, int score, int combo) {
            this.name = name;
            this.ready = ready;
            this.score = score;
            this.combo = combo;
        }
    }
}
