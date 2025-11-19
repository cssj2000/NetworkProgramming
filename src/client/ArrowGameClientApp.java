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
            cardLayout.show(mainPanel, "GAME");
            gamePanel.startGame();
        });

        gamePanel.setOnGameEndListener((score, maxCombo) -> {
            resultPanel.setResult(score, maxCombo);
            cardLayout.show(mainPanel, "RESULT");
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

            // 서버로 JOIN 메시지 한 번 보내기 (플레이어1 이름 기준)
            String myName = lobbyPanel.getLocalPlayerName();
            if (myName.isEmpty()) myName = "플레이어1";
            gameClient.send("JOIN " + myName);

            // 서버에서 오는 메시지 처리
            gameClient.setListener(msg ->
                    SwingUtilities.invokeLater(() -> handleServerMessage(msg))
            );

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
        String[] parts = msg.split(" ", 3);
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
                    String text = parts[2];
                    lobbyPanel.addChatMessage(nick + ": " + text);
                }
                break;
            }
            case "READY": {
                // READY 닉네임
                if (parts.length >= 2) {
                    String nick = parts[1];
                    lobbyPanel.addChatMessage(nick + " 님이 준비했습니다.");
                }
                break;
            }
            case "UNREADY": {
                if (parts.length >= 2) {
                    String nick = parts[1];
                    lobbyPanel.addChatMessage(nick + " 님이 준비를 취소했습니다.");
                }
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
}
