package app;

import app.fix.FixProtocol;

public class MarketFunctions {

    public static void assignRouteServiceID(String value) {
        String[] parts = value.split("-");
        MarketAccount.marketRouteID = Integer.parseInt(parts[0]);
        MarketAccount.marketServiceID = Integer.parseInt(parts[1]);
    }

    public static int getMarketItemAmount(int itemID) {
        int amount = 0;
        if (itemID == 1)
            amount = MarketAccount.silver;
        else if (itemID == 2)
            amount = MarketAccount.gold;
        else if (itemID == 3)
            amount = MarketAccount.platinum;
        else if (itemID == 4)
            amount = MarketAccount.fuel;
        else if (itemID == 5)
            amount = MarketAccount.bitcoin;
        return amount;
    }

    public static String brokerPurchaseCheck(String value) {
        Boolean checkPass = true;
        String ret;

        String[] message = value.split("\\|");
        int itemID = 0;
        int purchaseAmount = 0;
        int brokerRouteID = 0;

        try {
            for (int i=0; i < message.length; i++) {
                if (message[i].startsWith("100=")) {
                    itemID = Integer.parseInt(message[i].substring(4));
                }
                if (message[i].startsWith("101=")) {
                    purchaseAmount = Integer.parseInt(message[i].substring(4));
                }
                if (message[i].startsWith("554=")) {
                    brokerRouteID = Integer.parseInt(message[i].substring(4));
                }
            }
            if (itemID > 5 || itemID < 0)
                checkPass = false;
            else if (purchaseAmount > getMarketItemAmount(itemID) || purchaseAmount < 0)
                checkPass = false;
            if (checkPass)
                return ret = brokerPurchaseExecuted(value);
            else
                return ret = brokerPurchaseRejected(value);
        } catch (NumberFormatException e) {
            String rejectMessage = Market.fixProtocol.receiveMessage(value);
            rejectMessage = Market.fixProtocol.RejectMessageNumFormat(-1, 11, "Invalid Numeric Input Type", brokerRouteID);
            ret = rejectMessage;
        }
        return ret;
    }

    public static String brokerPurchaseExecuted(String value) {

        String[] message = value.split("\\|");
        int     itemID = 0;
        int     purchaseAmount = 0;
        int     purchasePrice = 0;
        String  brokerID = "";
        int     msgSeqNum = 0;

        for (int i=0; i < message.length; i++) {
            if (message[i].startsWith("100=")) {
                itemID = Integer.parseInt(message[i].substring(4));
            }
            if (message[i].startsWith("101=")) {
                purchaseAmount = Integer.parseInt(message[i].substring(4));
            }
            if (message[i].startsWith("102=")) {
                purchasePrice = Integer.parseInt(message[i].substring(4));
            }
            if (message[i].startsWith("554=")) {
                brokerID = message[i].substring(4);
            }
            if (message[i].startsWith("34=")) {
                msgSeqNum = Integer.parseInt(message[i].substring(3));
            }
        }

        if (itemID == 1)
            MarketAccount.silver -= purchaseAmount;
        else if (itemID == 2)
            MarketAccount.gold -= purchaseAmount;
        else if (itemID == 3)
            MarketAccount.platinum -= purchaseAmount;
        else if (itemID == 4)
            MarketAccount.fuel -= purchaseAmount;
        else if (itemID == 5)
            MarketAccount.bitcoin -= purchaseAmount;

        MarketAccount.capital += purchasePrice;

        String ret = Market.fixProtocol.PurchaseMessageSuccess(brokerID, msgSeqNum, "1");
        return ret;
    }


    public static String brokerPurchaseRejected(String value) {
        String[] message = value.split("\\|");
        String brokerID = "";
        int     msgSeqNum = 0;

        for (int i=0; i < message.length; i++) {
            if (message[i].startsWith("554=")) {
                brokerID = message[i].substring(4);
            }
            if (message[i].startsWith("34=")) {
                msgSeqNum = Integer.parseInt(message[i].substring(3));
            }
        }

        String ret = Market.fixProtocol.PurchaseMessageFail(brokerID, msgSeqNum);
        return ret;
    }

    public static String brokerSaleCheck(String value) {
        Boolean checkPass = true;
        String ret;

        String[] message = value.split("\\|");
        int itemID = 0;
        int salePrice = 0;
        int brokerRouteID = 0;

        try {
            for (int i=0; i < message.length; i++) {
                if (message[i].startsWith("100=")) {
                    itemID = Integer.parseInt(message[i].substring(4));
                }
                if (message[i].startsWith("102=")) {
                    salePrice = Integer.parseInt(message[i].substring(4));
                }
                if (message[i].startsWith("554=")) {
                    brokerRouteID = Integer.parseInt(message[i].substring(4));
                }
            }

            int marketCapital = MarketAccount.capital;

            if (salePrice > marketCapital)
                checkPass = false;
            else if (itemID > 5 || itemID < 0)
                checkPass = false;
            if (checkPass)
                return ret = brokerSaleExecuted(value);
            else
                return ret = brokerSaleRejected(value);
        } catch (NumberFormatException e) {
            String rejectMessage = Market.fixProtocol.receiveMessage(value);
            rejectMessage = Market.fixProtocol.RejectMessageNumFormat(-1, 11, "Invalid Numeric Input Type", brokerRouteID);
            ret = rejectMessage;
        }
        return ret;
    }

    public static String  brokerSaleExecuted(String value) {
        String[] message = value.split("\\|");
        int itemID = 0;
        int saleAmount = 0;
        int salePrice = 0;
        String brokerID = "";
        int msgSeqNum = 0;

        for (int i=0; i < message.length; i++) {
            if (message[i].startsWith("100=")) {
                itemID = Integer.parseInt(message[i].substring(4));
            }
            if (message[i].startsWith("101=")) {
                saleAmount = Integer.parseInt(message[i].substring(4));
            }
            if (message[i].startsWith("102=")) {
                salePrice = Integer.parseInt(message[i].substring(4));
            }
            if (message[i].startsWith("554=")) {
                brokerID = message[i].substring(4);
            }
            if (message[i].startsWith("34=")) {
                msgSeqNum = Integer.parseInt(message[i].substring(3));
            }
        }

        if (itemID == 1)
            MarketAccount.silver += saleAmount;
        else if (itemID == 2)
            MarketAccount.gold += saleAmount;
        else if (itemID == 3)
            MarketAccount.platinum += saleAmount;
        else if (itemID == 4)
            MarketAccount.fuel += saleAmount;
        else if (itemID == 5)
            MarketAccount.bitcoin += saleAmount;

        MarketAccount.capital -= salePrice;
        
        String ret = Market.fixProtocol.SaleMessageSuccess(brokerID, msgSeqNum, "2");
        return ret;
    }

    public static String brokerSaleRejected(String value) {
        String[] message = value.split("\\|");
        String brokerID = "";
        int msgSeqNum = 0;

        for (int i=0; i < message.length; i++) {
            if (message[i].startsWith("554=")) {
                brokerID = message[i].substring(4);
            }
            if (message[i].startsWith("34=")) {
                msgSeqNum = Integer.parseInt(message[i].substring(3));
            }
        }

        String ret = Market.fixProtocol.SaleMessageFail(brokerID, msgSeqNum);
        return ret;
    }

    public static String marketQuery(String value) {
        String[] message = value.split("\\|");
        String brokerID = "";
        int msgSeqNum = 0;

        for (int i=0; i < message.length; i++) {
            if (message[i].startsWith("554=")) {
                brokerID = message[i].substring(4);
            }
            if (message[i].startsWith("34=")) {
                msgSeqNum = Integer.parseInt(message[i].substring(3));
            }
        }

        String ret = Market.fixProtocol.MarketQueryReturn(brokerID, msgSeqNum, MarketAccount.marketRouteID, 
                        MarketAccount.silver, MarketAccount.gold, MarketAccount.platinum, 
                        MarketAccount.fuel, MarketAccount.bitcoin, MarketAccount.capital);
        return ret;
    }
    
}