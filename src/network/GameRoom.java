package network;

import java.util.Random;
import java.util.Vector;

public class GameRoom {
    private String roomId;
    private String roomName;
    private Vector<Player> players;
    private int maxPlayers;
    private boolean inGame;
    private int currentStage;

    public GameRoom(String roomName, int maxPlayers) {
        this.roomId = generateRoomId();
        this.roomName = roomName;
        this.players = new Vector<>();
        this.maxPlayers = maxPlayers;
        this.inGame = false;
        this.currentStage = 1;
    }

    private String generateRoomId() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public Vector<Player> getPlayers() {
        return players;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean isInGame() {
        return inGame;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    public int getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(int currentStage) {
        this.currentStage = currentStage;
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public void addPlayer(Player player) {
        if (!isFull()) {
            // 첫 번째 플레이어는 방장
            if (players.isEmpty()) {
                player.setHost(true);
            } else {
                // 기존 플레이어와 중복되지 않도록 방장 권한 제거
                player.setHost(false);
            }
            players.add(player);
        }
    }

    public void removePlayer(Player player) {
        boolean wasHost = player.isHost();
        players.remove(player);

        // 방장이 나갔으면 다음 사람에게 위임
        if (wasHost && !players.isEmpty()) {
            players.get(0).setHost(true);
        }
    }

    public Player getHost() {
        for (Player p : players) {
            if (p.isHost()) {
                return p;
            }
        }
        return null;
    }

    // 방 정보를 프로토콜 문자열로 변환
    public String toProtocolString() {
        return roomId + "|" + roomName + "|" + players.size() + "|" + maxPlayers + "|" + inGame;
    }

    @Override
    public String toString() {
        return roomName + " (" + players.size() + "/" + maxPlayers + ")";
    }
}
