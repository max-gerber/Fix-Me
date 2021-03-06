package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class BrokerHBSender extends Thread {
    private Socket socket;

    public BrokerHBSender(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            PrintWriter dOut = new PrintWriter(socket.getOutputStream(), true);

            while (true) {
                Thread.sleep(2000);
                dOut.println(Broker.fixProtocol.heartBeatMessage(BrokerAccount.brokerRouteID));
                }
        } catch (IOException e) {
            System.out.println("Oops, BrokerHeartBeat Send Error: " + e.getMessage());
        } catch (InterruptedException te) {
            System.out.println("Broker HeartBeat Stopped");
        } finally {
            try {
                socket.close();
            } catch(IOException e) {}
        }
    }
}