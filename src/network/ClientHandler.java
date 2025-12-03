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
    private String currentRoomId;

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

    public Player getPlayer() {
        return player;
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
                    sendMessage("JOIN_OK");

                } else if (line.equals("REQUEST_ROOM_LIST")) {
                    // 방 목록 요청
                    String roomList = server.getRoomListString();
                    System.out.println("[DEBUG] Client " + (player != null ? player.getNickname() : "unknown") + " requested room list");
                    System.out.println("[DEBUG] Sending room list: " + roomList);
                    sendMessage(roomList);

                } else if (line.startsWith("CREATE_ROOM ")) {
                    // CREATE_ROOM 방이름|비밀번호 (비밀번호 없으면 공개방)
                    if (player != null) {
                        String data = line.substring(12).trim();
                        String roomName;
                        String password = null;

                        // 파이프로 구분 (방이름|비밀번호)
                        if (data.contains("|")) {
                            String[] parts = data.split("\\|", 2);
                            roomName = parts[0].trim();
                            password = parts.length > 1 ? parts[1].trim() : null;
                            if (password != null && password.isEmpty()) password = null;
                        } else {
                            roomName = data;
                        }

                        if (roomName.isEmpty()) roomName = player.getNickname() + "의 방";

                        String roomId;
                        if (password != null) {
                            roomId = server.createRoom(roomName, player.getNickname(), 4, password);
                        } else {
                            roomId = server.createRoom(roomName, player.getNickname(), 4);
                        }

                        if (server.joinRoom(roomId, player, password)) {
                            currentRoomId = roomId;
                            String actualRoomName = server.getRoomName(roomId);
                            sendMessage("ROOM_JOINED " + roomId + "|" + actualRoomName);
                        }
                    }

                } else if (line.startsWith("JOIN_ROOM ")) {
                    // JOIN_ROOM roomId|비밀번호 (비밀번호 없으면 공개방)
                    if (player != null) {
                        String data = line.substring(10).trim();
                        String roomId;
                        String password = null;

                        // 파이프로 구분 (roomId|비밀번호)
                        if (data.contains("|")) {
                            String[] parts = data.split("\\|", 2);
                            roomId = parts[0].trim();
                            password = parts.length > 1 ? parts[1].trim() : null;
                        } else {
                            roomId = data;
                        }

                        if (server.joinRoom(roomId, player, password)) {
                            currentRoomId = roomId;
                            String roomName = server.getRoomName(roomId);
                            sendMessage("ROOM_JOINED " + roomId + "|" + roomName);
                        }
                        // 실패 메시지는 joinRoom 메서드 내부에서 전송됨
                    }

                } else if (line.equals("LEAVE_ROOM")) {
                    // 방 나가기
                    if (player != null && currentRoomId != null) {
                        server.leaveRoom(player.getNickname());
                        currentRoomId = null;
                        sendMessage("LEFT_ROOM");
                    }

                } else if (line.startsWith("CHAT ")) {
                    // CHAT 내용
                    String text = line.substring(5);
                    if (player != null && currentRoomId != null) {
                        // 방장 여부를 포함하여 전송: CHAT 닉네임 방장여부 내용
                        server.broadcastToRoom(currentRoomId, "CHAT " + player.getNickname() + " " + player.isHost() + " " + text);
                    }

                } else if (line.equals("READY")) {
                    if (player != null && currentRoomId != null) {
                        server.setPlayerReady(player.getNickname(), true);
                        server.broadcastToRoom(currentRoomId, "SYS " + player.getNickname() + " 님이 준비했습니다.");
                    }

                } else if (line.equals("UNREADY")) {
                    if (player != null && currentRoomId != null) {
                        server.setPlayerReady(player.getNickname(), false);
                        server.broadcastToRoom(currentRoomId, "SYS " + player.getNickname() + " 님이 준비를 취소했습니다.");
                    }

                } else if (line.startsWith("PLAYER_INPUT ")) {
                    // PLAYER_INPUT SUCCESS|FAIL
                    if (player != null) {
                        String result = line.substring(13).trim();
                        server.handlePlayerInput(player.getNickname(), result);
                    }

                } else if (line.startsWith("GAME_STATE ")) {
                    // GAME_STATE stage currentIndex totalCount score combo sequence...
                    if (player != null) {
                        String stateData = line.substring(11).trim();
                        server.handleGameState(player.getNickname(), stateData);
                    }

                } else if (line.startsWith("TRANSFER_HOST ")) {
                    // TRANSFER_HOST 새방장닉네임
                    if (player != null) {
                        String newHostName = line.substring(14).trim();
                        server.transferHost(player.getNickname(), newHostName);
                    }

                } else if (line.startsWith("KICK ")) {
                    // KICK 대상닉네임
                    if (player != null && currentRoomId != null) {
                        String target = line.substring(5).trim();
                        server.kickPlayer(player.getNickname(), target);
                    }



                } else if (line.equals("START_GAME_REQUEST")) {
                    // 방장이 게임 시작 요청
                    if (player != null && player.isHost()) {
                        server.requestStartGame(player.getNickname());
                    }

                } else if (line.equals("QUIT")) {
                    if (player != null && currentRoomId != null) {
                        server.leaveRoom(player.getNickname());
                    }
                    break;

                } else {
                    // 그 외 문자열은 그냥 시스템 메시지로 브로드캐스트
                    if (player != null && currentRoomId != null) {
                        server.broadcastToRoom(currentRoomId, "SYS " + player.getNickname() + ": " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Connection lost: " + (player != null ? player.getNickname() : "Unknown") + " / " + socket);
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            if (player != null && currentRoomId != null) {
                server.leaveRoom(player.getNickname());
            }
            server.removeClient(this);
        }
    }
    public void closeSocket() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

}
