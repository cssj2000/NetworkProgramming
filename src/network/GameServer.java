package network;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class GameServer {

    public static final int PORT = 30000;

    private ServerSocket serverSocket;
    private Vector<ClientHandler> clients = new Vector<>();
    private Map<String, GameRoom> rooms = new HashMap<>(); // roomId -> GameRoom
    private Map<String, String> playerRooms = new HashMap<>(); // playerNickname -> roomId

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
        return playerRooms.containsKey(nickname);
    }

    // 방 생성
    public synchronized String createRoom(String roomName, String hostNickname, int maxPlayers) {
        GameRoom room = new GameRoom(roomName, maxPlayers);
        rooms.put(room.getRoomId(), room);
        System.out.println("Room created: " + room.getRoomId() + " - " + roomName);

        // 방 목록이 변경되었으므로 모든 클라이언트에게 알림
        broadcastRoomListToLobby();

        return room.getRoomId();
    }

    // 방 입장
    public synchronized boolean joinRoom(String roomId, Player player) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            System.out.println("Room not found: " + roomId);
            return false;
        }

        if (room.isFull()) {
            System.out.println("Room is full: " + roomId);
            return false;
        }

        if (room.isInGame()) {
            System.out.println("Game already in progress: " + roomId);
            return false;
        }

        room.addPlayer(player);
        playerRooms.put(player.getNickname(), roomId);
        System.out.println(player.getNickname() + " joined room: " + roomId);

        // 방의 모든 플레이어에게 플레이어 목록 브   로드캐스트
        broadcastToRoom(roomId, "SYS " + player.getNickname() + " 님이 입장했습니다.");
        broadcastPlayerListToRoom(roomId);

        // 방 목록 갱신
        broadcastRoomListToLobby();

        return true;
    }

    // 방 나가기
    public synchronized void leaveRoom(String nickname) {
        String roomId = playerRooms.get(nickname);
        if (roomId == null) return;

        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        Player player = null;
        for (Player p : room.getPlayers()) {
            if (p.getNickname().equals(nickname)) {
                player = p;
                break;
            }
        }

        if (player != null) {
            room.removePlayer(player);
            playerRooms.remove(nickname);
            System.out.println(nickname + " left room: " + roomId);

            // 방이 비었으면 삭제
            if (room.getPlayers().isEmpty()) {
                rooms.remove(roomId);
                System.out.println("Room deleted (empty): " + roomId);
            } else {
                // 남은 플레이어들에게 알림
                broadcastToRoom(roomId, "SYS " + nickname + " 님이 나갔습니다.");

                // 방장이 바뀌었으면 알림
                if (room.getHost() != null) {
                    broadcastToRoom(roomId, "SYS " + room.getHost().getNickname() + " 님이 방장이 되었습니다.");
                }

                broadcastPlayerListToRoom(roomId);
            }

            // 방 목록 갱신
            broadcastRoomListToLobby();
        }
    }

    // 방 목록 가져오기
    public synchronized String getRoomListString() {
        StringBuilder sb = new StringBuilder("ROOM_LIST");
        for (GameRoom room : rooms.values()) {
            sb.append(";").append(room.toProtocolString());
        }
        return sb.toString();
    }

    // 방 이름 가져오기
    public synchronized String getRoomName(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            return room.getRoomName();
        }
        return "알 수 없는 방";
    }

    // 특정 방의 플레이어에게만 브로드캐스트
    public synchronized void broadcastToRoom(String roomId, String msg) {
        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        for (Player p : room.getPlayers()) {
            try {
                p.getHandler().sendMessage(msg);
            } catch (IOException e) {
                System.err.println("Failed to send message to " + p.getNickname());
            }
        }
    }

    // 방의 플레이어 목록 브로드캐스트
    public synchronized void broadcastPlayerListToRoom(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        StringBuilder sb = new StringBuilder("PLAYER_LIST");
        for (Player p : room.getPlayers()) {
            sb.append(" ").append(p.toProtocolString());
        }

        broadcastToRoom(roomId, sb.toString());
    }

    // 방 목록을 방에 없는 모든 클라이언트에게 브로드캐스트
    public synchronized void broadcastRoomListToLobby() {
        String roomListMsg = getRoomListString();
        System.out.println("[DEBUG] Broadcasting room list: " + roomListMsg);
        System.out.println("[DEBUG] Total clients: " + clients.size());

        int sentCount = 0;
        for (ClientHandler client : clients) {
            // 방에 없는 클라이언트에게만 전송
            Player player = client.getPlayer();
            if (player != null) {
                String roomId = playerRooms.get(player.getNickname());
                if (roomId == null) {
                    // 방에 없는 클라이언트에게 방 목록 전송
                    try {
                        client.sendMessage(roomListMsg);
                        sentCount++;
                        System.out.println("[DEBUG] Sent room list to: " + player.getNickname());
                    } catch (IOException e) {
                        System.err.println("Failed to send room list to " + player.getNickname());
                    }
                } else {
                    System.out.println("[DEBUG] Skipping " + player.getNickname() + " (in room: " + roomId + ")");
                }
            } else {
                System.out.println("[DEBUG] Skipping client with null player");
            }
        }
        System.out.println("[DEBUG] Room list sent to " + sentCount + " clients");
    }

    // 플레이어의 준비 상태 변경
    public synchronized void setPlayerReady(String nickname, boolean ready) {
        String roomId = playerRooms.get(nickname);
        if (roomId == null) return;

        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        for (Player p : room.getPlayers()) {
            if (p.getNickname().equals(nickname)) {
                p.setReady(ready);
                broadcastPlayerListToRoom(roomId);
                break;
            }
        }
    }

    // 방장 위임
    public synchronized void transferHost(String currentHost, String newHostName) {
        String roomId = playerRooms.get(currentHost);
        if (roomId == null) return;

        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        Player currentHostPlayer = null;
        Player newHostPlayer = null;

        for (Player p : room.getPlayers()) {
            if (p.getNickname().equals(currentHost)) {
                currentHostPlayer = p;
            }
            if (p.getNickname().equals(newHostName)) {
                newHostPlayer = p;
            }
        }

        if (currentHostPlayer != null && newHostPlayer != null && currentHostPlayer.isHost()) {
            currentHostPlayer.setHost(false);
            newHostPlayer.setHost(true);
            System.out.println("Host transferred from " + currentHost + " to " + newHostName);
            broadcastToRoom(roomId, "SYS " + newHostName + " 님이 방장이 되었습니다.");
            broadcastPlayerListToRoom(roomId);
        }
    }

    // 게임 시작 요청
    public synchronized void requestStartGame(String hostNickname) {
        String roomId = playerRooms.get(hostNickname);
        if (roomId == null) return;

        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        if (room.isInGame()) {
            System.out.println("Game already in progress");
            return;
        }

        // 방장 확인
        Player host = room.getHost();
        if (host == null || !host.getNickname().equals(hostNickname)) {
            System.out.println("Not authorized to start game: " + hostNickname);
            return;
        }

        // 최소 1명 준비 확인
        boolean anyReady = false;
        for (Player p : room.getPlayers()) {
            if (p.isReady()) {
                anyReady = true;
                break;
            }
        }

        if (!anyReady) {
            System.out.println("No players ready");
            return;
        }

        // 게임 시작
        startGame(roomId);
    }

    // 게임 시작
    private synchronized void startGame(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        room.setInGame(true);
        room.setCurrentStage(1);

        // 모든 플레이어 점수 초기화
        for (Player p : room.getPlayers()) {
            p.setScore(0);
            p.setSuccessCount(0);
            p.setCombo(0);
            p.setCurrentStage(1);
            p.setFinished(false);
        }

        broadcastToRoom(roomId, "START_GAME");
        System.out.println("Game started in room: " + roomId);

        // 각 플레이어에게 첫 스테이지 시퀀스 전송
        for (Player p : room.getPlayers()) {
            sendSequenceToPlayer(p);
        }
    }

    // 플레이어에게 시퀀스 전송
    private synchronized void sendSequenceToPlayer(Player player) {
        int stage = player.getCurrentStage();
        // 스테이지 12부터는 난이도 고정 (화살표 14개)
        int length = stage >= 12 ? 14 : (3 + stage - 1);

        String[] directions = {"UP", "DOWN", "LEFT", "RIGHT"};
        Random rnd = new Random();

        StringBuilder sb = new StringBuilder("GAME_SEQUENCE ");
        sb.append(stage);
        for (int i = 0; i < length; i++) {
            sb.append(" ").append(directions[rnd.nextInt(directions.length)]);
        }

        System.out.println("[SEQUENCE] " + player.getNickname() + " - Stage " + stage + ", Length " + length);

        try {
            player.getHandler().sendMessage(sb.toString());
        } catch (IOException e) {
            System.err.println("Failed to send sequence to " + player.getNickname());
        }
    }

    // 플레이어 입력 처리
    public synchronized void handlePlayerInput(String nickname, String input) {
        String roomId = playerRooms.get(nickname);
        if (roomId == null) return;

        GameRoom room = rooms.get(roomId);
        if (room == null || !room.isInGame()) return;

        Player player = null;
        for (Player p : room.getPlayers()) {
            if (p.getNickname().equals(nickname)) {
                player = p;
                break;
            }
        }

        if (player == null) return;

        if (input.equals("SUCCESS")) {
            // 정답 개수 증가
            player.setSuccessCount(player.getSuccessCount() + 1);

            // 콤보 증가
            player.setCombo(player.getCombo() + 1);

            // 점수 계산: 기본 100점 + 콤보 보너스 (콤보 x 10점)
            int baseScore = 100;
            int comboBonus = player.getCombo() * 10;
            int earnedScore = baseScore + comboBonus;
            player.setScore(player.getScore() + earnedScore);

            System.out.println("[SCORE] " + player.getNickname() + " - 정답! 콤보: " + player.getCombo()
                    + ", 획득 점수: " + earnedScore + " (기본 " + baseScore + " + 콤보 보너스 " + comboBonus + ")");

            int nextStage = player.getCurrentStage() + 1;
            if (nextStage > 20) {
                System.out.println(player.getNickname() + " completed all stages (Stage 20)!");
                player.setFinished(true);
                checkGameEnd(roomId);
            } else {
                player.setCurrentStage(nextStage);
                sendSequenceToPlayer(player);
            }
        } else if (input.equals("FAIL")) {
            System.out.println("[SCORE] " + player.getNickname() + " - 실패! 콤보 초기화");
            player.setCombo(0);
        } else if (input.equals("TIME_UP")) {
            System.out.println("[TIME_UP] " + player.getNickname() + " - 타이머 종료");
            player.setFinished(true);
            checkGameEnd(roomId);
        }

        broadcastPlayerListToRoom(roomId);
    }

    // 플레이어의 게임 상태 업데이트 처리
    public synchronized void handleGameState(String nickname, String stateData) {
        String roomId = playerRooms.get(nickname);
        if (roomId == null) return;

        GameRoom room = rooms.get(roomId);
        if (room == null || !room.isInGame()) return;

        // GAME_STATE nickname stage currentIndex totalCount score combo sequence
        // 예: GAME_STATE player1 5 3 10 1500 5 UP DOWN LEFT RIGHT UP DOWN LEFT RIGHT UP DOWN
        // 다른 플레이어들에게 브로드캐스트
        String msg = "GAME_STATE " + nickname + " " + stateData;

        for (Player p : room.getPlayers()) {
            // 본인 제외
            if (!p.getNickname().equals(nickname)) {
                try {
                    p.getHandler().sendMessage(msg);
                } catch (IOException e) {
                    System.err.println("Failed to send game state to " + p.getNickname());
                }
            }
        }
    }

    // 게임 종료 확인
    private void checkGameEnd(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        for (Player p : room.getPlayers()) {
            if (!p.isFinished()) {
                System.out.println("[GAME_END_CHECK] " + p.getNickname() + " is not finished yet (stage " + p.getCurrentStage() + "), waiting...");
                return;
            }
        }

        // 모든 플레이어가 완료
        System.out.println("[GAME_END_CHECK] All players finished! Ending game...");
        endGame(roomId);
    }

    // 게임 종료
    private synchronized void endGame(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        room.setInGame(false);

        System.out.println("[DEBUG] ===== GAME END - RANKING INFO =====");
        System.out.println("[DEBUG] Room: " + roomId);
        System.out.println("[DEBUG] Players before sorting:");
        for (Player p : room.getPlayers()) {
            System.out.println("[DEBUG]   " + p.getNickname() + ": Score=" + p.getScore()
                    + ", Success=" + p.getSuccessCount() + ", MaxCombo=" + p.getMaxCombo());
        }

        // 랭킹 정렬: 점수 → 정답 개수 → 최고 콤보 순으로 비교
        java.util.List<Player> sortedPlayers = new java.util.ArrayList<>(room.getPlayers());
        sortedPlayers.sort((p1, p2) -> {
            // 1. 점수 비교 (내림차순)
            if (p2.getScore() != p1.getScore()) {
                return p2.getScore() - p1.getScore();
            }
            // 2. 정답 개수 비교 (내림차순)
            if (p2.getSuccessCount() != p1.getSuccessCount()) {
                return p2.getSuccessCount() - p1.getSuccessCount();
            }
            // 3. 최고 콤보 비교 (내림차순)
            return p2.getMaxCombo() - p1.getMaxCombo();
        });

        System.out.println("[DEBUG] Players after sorting:");
        for (int i = 0; i < sortedPlayers.size(); i++) {
            Player p = sortedPlayers.get(i);
            System.out.println("[DEBUG]   " + (i+1) + ". " + p.getNickname() + ": Score=" + p.getScore()
                    + ", Success=" + p.getSuccessCount() + ", MaxCombo=" + p.getMaxCombo());
        }

        // 랭킹 정보 생성: GAME_RANKING name1|score1|success1|combo1 name2|score2|success2|combo2 ...
        StringBuilder rankingMsg = new StringBuilder("GAME_RANKING");
        for (Player p : sortedPlayers) {
            rankingMsg.append(" ").append(p.getNickname())
                    .append("|").append(p.getScore())
                    .append("|").append(p.getSuccessCount())
                    .append("|").append(p.getMaxCombo());
        }

        System.out.println("[DEBUG] Ranking message: " + rankingMsg.toString());
        System.out.println("[DEBUG] ===================================");

        // 모든 플레이어에게 랭킹 정보 전송
        broadcastToRoom(roomId, rankingMsg.toString());

        // 모든 플레이어 준비 상태 해제
        for (Player p : room.getPlayers()) {
            p.setReady(false);
        }

        broadcastToRoom(roomId, "GAME_END");
        broadcastPlayerListToRoom(roomId);
        System.out.println("Game ended in room: " + roomId);
    }

    // 클라이언트 핸들러 제거
    public synchronized void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }
}
