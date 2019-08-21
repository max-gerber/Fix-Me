package app;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class Server {

    //-Broker Hash Map
    public static Map<String, Socket> mapBroker;
    private ServerSocket socketBroker;
    private Runnable bS;

    //-Market Hash Map
    public static Map<String, Socket> mapMarket;
    private ServerSocket socketMarket;
    private Runnable mS;

    public Server(int portA, int portB) throws IOException {

        mapBroker = new HashMap<String,Socket>();
        mapMarket = new HashMap<String,Socket>();

        socketBroker = new ServerSocket(portA);
        socketMarket = new ServerSocket(portB);

        bS = new BrokerSocket(socketBroker);
        mS = new MarketSocket(socketMarket);

        Thread tb = new Thread(bS);
        Thread tm = new Thread(mS);
        tb.start();
        tm.start();
    }

    // You should write a server that handles the requests made and 
    // is "smart" enough to call a time-out after X seconds without "news" from client Y.

    class HeartBeatScanner extends Thread {
        private Socket socket;

        public HeartBeatScanner(Socket socket) {
            this.socket = socket;
        }

        private Duration timeout = Duration.ofMillis(7000);
        
        // public void timeoutCheck() {
        //     int i = 0;
        //     while (i < mapBroker.size()) {
        //         // mapBroker
        //     }
        // }

		public void run() {
            try {
                BufferedReader dIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    
                while (true) {
                    String echoString = dIn.readLine();
                    if (echoString == null) {
                        break;
                    }
                    String[] echoStringParts = echoString.split("-");
                    if (echoStringParts[0].equals("HB")) {
                        System.out.println("-√v^√v^√❤ Received-" + echoStringParts[1]);
                    }
                }
            } catch(IOException e) {
                System.out.println("Oops: " + e.getMessage());
            } catch(Exception e) {
                System.out.println("HeartBeat Server exception " + e.getMessage());
            }
		}
    }

    class BrokerSocket implements Runnable {
        private ServerSocket socketB;

        BrokerSocket(ServerSocket sB) {
            socketB = sB;
        }

        public void run() {

            Socket hbSocket;
            try {
                hbSocket = new Socket("127.0.0.1", 5000);
                HeartBeatScanner heartBeatScanner = new HeartBeatScanner(hbSocket);
                heartBeatScanner.start();
            } catch (UnknownHostException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            
            System.out.println("--Broker Router Running--");
            while(true) {
                try {
                    Socket socket = socketB.accept();
                    Echoer echoer = new Echoer(socket);
                    echoer.start();

                    //-Broker Saved in Hash Map
                    int serviceID = LinkCounter.generateServiceID();
                    String routeID = LinkCounter.getBrokerRouteID(socket);
                    mapBroker.put(routeID, socket);
                    System.out.println("Broker[" + LinkCounter.brokerCount + "] connected");
                    System.out.println("Saved Brokers => " + mapBroker);

                    //-Send message to broker
                    Socket brokerPort = mapBroker.get(Integer.toString(LinkCounter.brokerCount));
                    PrintWriter output = new PrintWriter(brokerPort.getOutputStream(), true);
                    output.println(LinkCounter.brokerCount + "-" + serviceID);
                    
                    //-Count added Broker(Avoid nulls with brokerHB) 
                    LinkCounter.countBroker();
                } catch(Exception e) {
                    System.out.println("Broker Server exception " + e.getMessage());
                }
            }
        }
    }

    class MarketSocket implements Runnable {
        private ServerSocket socketM;

        MarketSocket(ServerSocket sM) {
            socketM = sM;
        }

        public void run() {
            System.out.println("--Market Router Running--");
            while(true) {
                try {
                    Socket socket = socketM.accept();
                    Echoer echoer = new Echoer(socket);
                    echoer.start();

                    //-Market Saved in Hash Map
                    LinkCounter.countMarket();
                    int serviceID = LinkCounter.generateServiceID();
                    String routeID = LinkCounter.getMarketRouteID(socket);
                    mapMarket.put(routeID, socket);
                    System.out.println("Market[" + LinkCounter.marketCount + "] connected");
                    System.out.println("Saved Markets => " + mapMarket);

                    //-Send message to market
                    Socket marketPort = mapMarket.get(Integer.toString(LinkCounter.marketCount));
                    PrintWriter output = new PrintWriter(marketPort.getOutputStream(), true);
                    output.println(LinkCounter.marketCount + "-" + serviceID);
                } catch(Exception e) {
                    System.out.println("Market Server exception " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args)
    {
        int portA = 5000;
        int portB = 5001;

        Server server;
        try {
            server = new Server(portA, portB);
        } catch(IOException ie) {
            System.err.println("Could not start server: " + ie);
        }
    }
} 