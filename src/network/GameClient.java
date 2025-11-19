package network;

import java.io.*;
import java.net.Socket;

public class GameClient {

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    public interface Listener {
        void onMessage(String msg);
    }

    private Listener listener;

    public GameClient(String host, int port) throws IOException {
        socket = new Socket(host, port);
        System.out.println("Connected to server: " + socket);

        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());

        // 서버에서 오는 메시지를 계속 읽는 스레드
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    String line = dis.readUTF();
                    System.out.println("From server: " + line);
                    if (listener != null) {
                        listener.onMessage(line);
                    }
                }
            } catch (IOException e) {
                System.out.println("Disconnected from server.");
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void send(String msg) throws IOException {
        dos.writeUTF(msg);
        dos.flush();
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}
