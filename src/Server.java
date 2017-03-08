import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class Server {
    static int port;
    private static Socket client;
    private boolean running = true;

    Server(int port) {
        startServer(port);
    }

    static Socket getClient() {
        return client;
    }

    public void stopServer(){
        running = false;
    }

    private void startServer(int port) {
        Server.port = port;
        try (ServerSocket server = new ServerSocket(port)) {
            while (running) {
                /* Passes output for each method requiring output access, removed need for class variables */
                try {
                    client = server.accept();
                    Runnable clientHandler = new ClientHandler(client);
                    new Thread(clientHandler).start();
                } catch (IOException e) {
                    //Serves to break out of while, exception throw accomplishes the same task.
                    running = false;
                }
            }
        } catch (IOException e) {
            /* Gracefully avoid program shutdown by attempting new port. !~ Make sure you check the correct port is used. */
            startServer(Server.port + 1);
        }
    }
}
