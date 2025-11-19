package network;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class GameServer {

    public static final int PORT = 30000; // 클라이언트랑 맞출 포트 번호

    private ServerSocket serverSocket;
    private Vector<ClientHandler> clients = new Vector<>();

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

                ClientHandler handler = new ClientHandler(clientSocket, clients);
                clients.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


