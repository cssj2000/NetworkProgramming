package network;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Vector;

class ClientHandler extends Thread {

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Vector<ClientHandler> clients;
    private String nickname = "손님";

    public ClientHandler(Socket socket, Vector<ClientHandler> clients) {
        this.socket = socket;
        this.clients = clients;
        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendOne(String msg) throws IOException {
        dos.writeUTF(msg);
        dos.flush();
    }

    private void broadcast(String msg) {
        for (ClientHandler ch : clients) {
            try {
                ch.sendOne(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
                    nickname = line.substring(5).trim();
                    broadcast("SYS " + nickname + " 님이 입장했습니다.");
                } else if (line.startsWith("CHAT ")) {
                    // CHAT 내용
                    String text = line.substring(5);
                    broadcast("CHAT " + nickname + " " + text);
                } else if (line.equals("READY")) {
                    broadcast("READY " + nickname);
                } else if (line.equals("UNREADY")) {
                    broadcast("UNREADY " + nickname);
                } else if (line.equals("QUIT")) {
                    broadcast("SYS " + nickname + " 님이 나갔습니다.");
                    break;
                } else {
                    // 그 외 문자열은 그냥 시스템 메시지로 브로드캐스트
                    broadcast("SYS " + nickname + ": " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Connection lost: " + nickname + " / " + socket);
            broadcast("SYS " + nickname + " 님 연결이 끊어졌습니다.");
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            clients.remove(this);
        }
    }
}