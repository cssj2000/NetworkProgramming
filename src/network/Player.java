package network;

public class Player {
    private String nickname;
    private boolean ready;
    private boolean isHost; // 방장 여부
    private int score;
    private int combo;
    private int maxCombo;
    private int currentStage;
    private ClientHandler handler;

    public Player(String nickname, ClientHandler handler) {
        this.nickname = nickname;
        this.handler = handler;
        this.ready = false;
        this.isHost = false;
        this.score = 0;
        this.combo = 0;
        this.maxCombo = 0;
        this.currentStage = 1;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean host) {
        isHost = host;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getCombo() {
        return combo;
    }

    public void setCombo(int combo) {
        this.combo = combo;
        if (combo > maxCombo) {
            maxCombo = combo;
        }
    }

    public int getMaxCombo() {
        return maxCombo;
    }

    public int getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(int currentStage) {
        this.currentStage = currentStage;
    }

    public ClientHandler getHandler() {
        return handler;
    }

    // 플레이어 정보를 문자열로 변환 (프로토콜용)
    public String toProtocolString() {
        return nickname + "|" + ready + "|" + isHost + "|" + score + "|" + combo + "|" + maxCombo;
    }
}
