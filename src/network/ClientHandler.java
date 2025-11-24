package network;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

class ClientHandler extends Thread {

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private GameServer server;
    private Player player;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String msg) throws IOException {
        dos.writeUTF(msg);
        dos.flush();
    }

    @Override
    public void run() {
        try {
            // 클라이언트가 보내는 문자열 계속 수신
            while (true) {
                String line = dis.readUTF();
                System.out.println("from client: " + line);

                if (line.startsWith("JOIN ")) {
                    // JOIN 닉네임
                    String nickname = line.substring(5).trim();
                    if (nickname.isEmpty()) nickname = "손님";

                    // 중복 닉네임 체크
                    if (server.isNicknameTaken(nickname)) {
                        sendMessage("JOIN_FAILED 이미 사용 중인 닉네임입니다.");
                        break;
                    }

                    player = new Player(nickname, this);
                    server.addPlayer(player);
                    sendMessage("JOIN_OK");
                    server.broadcast("SYS " + nickname + " 님이 입장했습니다.");

                } else if (line.startsWith("CHAT ")) {
                    // CHAT 내용
                    String text = line.substring(5);
                    if (player != null) {
                        server.broadcast("CHAT " + player.getNickname() + " " + text);
                    }

                } else if (line.equals("READY")) {
                    if (player != null) {
                        server.setPlayerReady(player.getNickname(), true);
                        server.broadcast("SYS " + player.getNickname() + " 님이 준비했습니다.");
                    }

                } else if (line.equals("UNREADY")) {
                    if (player != null) {
                        server.setPlayerReady(player.getNickname(), false);
                        server.broadcast("SYS " + player.getNickname() + " 님이 준비를 취소했습니다.");
                    }

                } else if (line.startsWith("PLAYER_INPUT ")) {
                    // PLAYER_INPUT SUCCESS|FAIL
                    if (player != null) {
                        String result = line.substring(13).trim();
                        server.handlePlayerInput(player.getNickname(), result);
                    }

                } else if (line.startsWith("TRANSFER_HOST ")) {
                    // TRANSFER_HOST 새방장닉네임
                    if (player != null) {
                        String newHostName = line.substring(14).trim();
                        server.transferHost(player.getNickname(), newHostName);
                    }

                } else if (line.equals("START_GAME_REQUEST")) {
                    // 방장이 게임 시작 요청
                    if (player != null && player.isHost()) {
                        server.requestStartGame(player.getNickname());
                    }

                } else if (line.equals("QUIT")) {
                    if (player != null) {
                        server.broadcast("SYS " + player.getNickname() + " 님이 나갔습니다.");
                    }
                    break;

                } else {
                    // 그 외 문자열은 그냥 시스템 메시지로 브로드캐스트
                    if (player != null) {
                        server.broadcast("SYS " + player.getNickname() + ": " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Connection lost: " + (player != null ? player.getNickname() : "Unknown") + " / " + socket);
            if (player != null) {
                server.broadcast("SYS " + player.getNickname() + " 님 연결이 끊어졌습니다.");
            }
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            if (player != null) {
                server.removePlayer(player);
            }
            server.removeClient(this);
        }
    }
}