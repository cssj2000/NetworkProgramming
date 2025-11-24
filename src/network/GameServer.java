package network;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class GameServer {

    public static final int PORT = 30000; // 클라이언트랑 맞출 포트 번호
    public static final int MAX_PLAYERS = 4;

    private ServerSocket serverSocket;
    private Vector<ClientHandler> clients = new Vector<>();
    private Vector<Player> players = new Vector<>();
    private boolean gameInProgress = false;

    public static void main(String[] args) {
        new GameServer().start();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("GameServer started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                if (players.size() >= MAX_PLAYERS) {
                    // 방이 가득 찼을 때
                    try {
                        DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                        dos.writeUTF("SYS 방이 가득 찼습니다.");
                        clientSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 닉네임 중복 체크
    public synchronized boolean isNicknameTaken(String nickname) {
        for (Player p : players) {
            if (p.getNickname().equals(nickname)) {
                return true;
            }
        }
        return false;
    }

    // 플레이어 추가
    public synchronized void addPlayer(Player player) {
        if (players.size() < MAX_PLAYERS) {
            players.add(player);
            System.out.println("Player added: " + player.getNickname() + " (Total: " + players.size() + ")");
            broadcastPlayerList();
        }
    }

    // 플레이어 제거
    public synchronized void removePlayer(Player player) {
        players.remove(player);
        System.out.println("Player removed: " + player.getNickname() + " (Total: " + players.size() + ")");
        broadcastPlayerList();
    }

    // 클라이언트 핸들러 제거
    public synchronized void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }

    // 모든 클라이언트에게 메시지 브로드캐스트
    public synchronized void broadcast(String msg) {
        for (ClientHandler ch : clients) {
            try {
                ch.sendMessage(msg);
            } catch (IOException e) {
                System.err.println("Failed to send message to client: " + e.getMessage());
            }
        }
    }

    // 플레이어 목록 브로드캐스트
    public synchronized void broadcastPlayerList() {
        StringBuilder sb = new StringBuilder("PLAYER_LIST");
        for (Player p : players) {
            sb.append(" ").append(p.toProtocolString());
        }
        broadcast(sb.toString());
    }

    // 플레이어의 준비 상태 변경
    public synchronized void setPlayerReady(String nickname, boolean ready) {
        for (Player p : players) {
            if (p.getNickname().equals(nickname)) {
                p.setReady(ready);
                broadcastPlayerList();
                checkAndStartGame();
                break;
            }
        }
    }

    // 모든 플레이어가 준비되었는지 확인하고 게임 시작
    private void checkAndStartGame() {
        if (players.size() < 1) return; // 최소 1명 필요

        for (Player p : players) {
            if (!p.isReady()) {
                return; // 한 명이라도 준비 안 됐으면 시작 안 함
            }
        }

        // 모두 준비됨 - 게임 시작
        startGame();
    }

    // 게임 시작
    private synchronized void startGame() {
        if (gameInProgress) return;

        gameInProgress = true;

        // 모든 플레이어 점수 및 스테이지 초기화
        for (Player p : players) {
            p.setScore(0);
            p.setCombo(0);
            p.setCurrentStage(1);
        }

        broadcast("START_GAME");
        System.out.println("Game started with " + players.size() + " players");

        // 각 플레이어에게 첫 스테이지 시퀀스 전송
        for (Player p : players) {
            sendSequenceToPlayer(p);
        }
    }

    // 특정 플레이어에게 시퀀스 전송
    private synchronized void sendSequenceToPlayer(Player player) {
        int stage = player.getCurrentStage();
        int length = 3 + stage - 1; // 1스테이지=3개, 이후 1씩 증가

        String[] directions = {"UP", "DOWN", "LEFT", "RIGHT"};
        Random rnd = new Random();

        StringBuilder sb = new StringBuilder("GAME_SEQUENCE ");
        sb.append(stage);
        for (int i = 0; i < length; i++) {
            sb.append(" ").append(directions[rnd.nextInt(directions.length)]);
        }

        try {
            player.getHandler().sendMessage(sb.toString());
            System.out.println("Sent sequence to " + player.getNickname() + " for stage " + stage);
        } catch (IOException e) {
            System.err.println("Failed to send sequence to " + player.getNickname());
        }
    }

    // 플레이어 입력 처리
    public synchronized void handlePlayerInput(String nickname, String input) {
        if (!gameInProgress) return;

        Player player = null;
        for (Player p : players) {
            if (p.getNickname().equals(nickname)) {
                player = p;
                break;
            }
        }

        if (player == null) return;

        // 입력 검증은 클라이언트에서 하고, 서버는 결과만 받음
        // PLAYER_INPUT SUCCESS|FAIL
        if (input.equals("SUCCESS")) {
            player.setScore(player.getScore() + 1);
            player.setCombo(player.getCombo() + 1);

            // 다음 스테이지로
            int nextStage = player.getCurrentStage() + 1;
            if (nextStage > 10) {
                // 이 플레이어는 게임 완료
                System.out.println(player.getNickname() + " completed all stages!");
                checkGameEnd();
            } else {
                player.setCurrentStage(nextStage);
                sendSequenceToPlayer(player);
            }
        } else if (input.equals("FAIL")) {
            player.setCombo(0);
            // 같은 스테이지 다시 시도 (시퀀스는 재전송하지 않음)
        }

        // 점수 업데이트 브로드캐스트
        broadcastPlayerList();
    }

    // 모든 플레이어가 게임을 완료했는지 확인
    private void checkGameEnd() {
        for (Player p : players) {
            if (p.getCurrentStage() <= 10) {
                return; // 아직 진행 중인 플레이어가 있음
            }
        }
        // 모든 플레이어가 완료
        endGame();
    }

    // 게임 종료
    private synchronized void endGame() {
        gameInProgress = false;

        // 모든 플레이어 준비 상태 해제
        for (Player p : players) {
            p.setReady(false);
        }

        broadcast("GAME_END");
        broadcastPlayerList();
        System.out.println("Game ended");
    }

    public synchronized boolean isGameInProgress() {
        return gameInProgress;
    }

    public synchronized Vector<Player> getPlayers() {
        return players;
    }
}


