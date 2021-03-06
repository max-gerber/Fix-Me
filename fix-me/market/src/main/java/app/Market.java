package app;

import app.fix.FixProtocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class Market {
    public static FixProtocol      fixProtocol;
    private static MarketHBSender marketHBSender;

    public static void main(String[] args) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", 5001)) {

            marketHBSender = new MarketHBSender(socket);
            marketHBSender.start();

            BufferedReader dIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter dOut = new PrintWriter(socket.getOutputStream(), true);
            
            String echoString;
            String msgType;
            String response;

            String savedServerResponse = dIn.readLine();
            MarketFunctions.assignRouteServiceID(savedServerResponse);
            System.out.println("--Market Connected--\n" + 
            "Market[" + MarketAccount.marketRouteID + "]" + " ServiceID => " + MarketAccount.marketServiceID);
            fixProtocol = new FixProtocol("" + MarketAccount.marketServiceID);
            do {
                echoString = dIn.readLine();
                
                // Thread.sleep(1000);
                
                if (echoString != null) {
                    System.out.println(echoString);
                    msgType = fixProtocol.getMsgType(echoString);
                    if (msgType.equals("1"))
                        response = MarketFunctions.brokerPurchaseCheck(echoString);
                    else if (msgType.equals("2"))
                        response = MarketFunctions.brokerSaleCheck(echoString);
                    else if (msgType.equals("6"))
                        response = MarketFunctions.marketQuery(echoString);
                    else {
                        response = "Market Cmd Error";
                    }
                    if (!"Market Cmd Error".equals(response))   
                        dOut.println(response);
                } else if (echoString == null) {
                    throw new NullPointerException("testing");
                }
            } while (!"exit".equals(echoString));

            marketHBSender.interrupt();
            System.out.println("Market [" + MarketAccount.marketRouteID + "] Closed");

        } catch(IOException | NullPointerException e) {
            System.out.println("Market Error: " + e.getMessage());
            marketHBSender.interrupt();
            System.out.println("Market [" + MarketAccount.marketRouteID + "] Closed");
        }
    }
}