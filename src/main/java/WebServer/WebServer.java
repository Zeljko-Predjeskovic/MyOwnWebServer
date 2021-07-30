package WebServer;

import java.net.Socket;
import java.net.ServerSocket;

public class WebServer {
    public static void main(String[] args) throws Exception {
        try(ServerSocket serverSocket = new ServerSocket(8080)){
            try(Socket client = serverSocket.accept()) {
                System.out.println("there is a client");

            }

        }
        catch (Exception e){
            throw new RuntimeException("", e);
        }
    }
}