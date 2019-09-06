package app;

public class BrokerFunctions {

    public static void assignRouteServiceID(String value) {
        String[] parts = value.split("-");
        BrokerAccount.brokerRouteID = Integer.parseInt(parts[0]);
        BrokerAccount.brokerServiceID = Integer.parseInt(parts[1]);
    }

    public static int getBrokerItemAmount(int itemID) {
        int amount = 0;
        if (itemID == 1)
            amount = BrokerAccount.accountSilver;
        else if (itemID == 2)
            amount = BrokerAccount.accountGold;
        else if (itemID == 3)
            amount = BrokerAccount.accountPlatinum;
        else if (itemID == 4)
            amount = BrokerAccount.accountFuel;
        else if (itemID == 5)
            amount = BrokerAccount.accountBitcoin;
        return amount;
    }

    public static Boolean brokerPurchaseValidate(String value) {
        Boolean ret = true;

        if (Integer.parseInt(value) > BrokerAccount.capital) {
            ret = false;
        }
        return ret;
    }

    public static Boolean brokerSaleValidate(String value, String itemID) {
        Boolean ret = true;

        if (Integer.parseInt(value) > getBrokerItemAmount(Integer.parseInt(itemID)))
            ret = false;

        return ret;
    }

    public static void brokerBuySuccess(String value) {
        String[] message = value.split("\\|");
        int itemID = 0;
        int purchaseAmount = 0;
        int purchasePrice = 0;
        
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
        }

        if (itemID == 1)
            BrokerAccount.accountSilver += purchaseAmount;
        else if (itemID == 2)
            BrokerAccount.accountGold += purchaseAmount;
        else if (itemID == 3)
            BrokerAccount.accountPlatinum += purchaseAmount;
        else if (itemID == 4)
            BrokerAccount.accountFuel += purchaseAmount;
        else if (itemID == 5)
            BrokerAccount.accountBitcoin += purchaseAmount;
            
        BrokerAccount.capital -= purchasePrice;
    }

    public static void brokerSellSuccess(String value) {
        String[] message = value.split("\\|");
        int itemID = 0;
        int purchaseAmount = 0;
        int purchasePrice = 0;
        
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
        }
        // Test
        String[] parts = value.split("-");
        int marketID = Integer.parseInt(parts[1]);
        int itemID = Integer.parseInt(parts[2]);
        int purchaseAmount = Integer.parseInt(parts[3]);
        int purchasePrice = Integer.parseInt(parts[4]);
        
        if (itemID == 1)
            BrokerAccount.accountSilver -= purchaseAmount;
        else if (itemID == 2)
            BrokerAccount.accountGold -= purchaseAmount;
        else if (itemID == 3)
            BrokerAccount.accountPlatinum -= purchaseAmount;
        else if (itemID == 4)
            BrokerAccount.accountFuel -= purchaseAmount;
        else if (itemID == 5)
            BrokerAccount.accountBitcoin -= purchaseAmount;

        BrokerAccount.capital += purchasePrice;
    }

    public static void brokerGetDataBroker () {
        System.out.println("__/Broker [" + BrokerAccount.brokerServiceID + "] Account/__" + 
            "\n[1]Silver   : " + BrokerAccount.accountSilver + "\n[2]Gold     : " + BrokerAccount.accountGold + 
            "\n[3]Platinum : " + BrokerAccount.accountPlatinum + "\n[4]Fuel     : " + BrokerAccount.accountFuel + 
            "\n[5]Bitcoin  : " + BrokerAccount.accountBitcoin + "\n[-]Capital  :" + BrokerAccount.capital);
    }
    public static void brokerReceiveDataMarket(String value) {
        String[] parts = value.split("-");
        int marketID = Integer.parseInt(parts[1]);
        int marketSilver = Integer.parseInt(parts[3]);
        int marketGold = Integer.parseInt(parts[4]);
        int marketPlatinum = Integer.parseInt(parts[5]);
        int marketFuel = Integer.parseInt(parts[6]);
        int marketBitCoin = Integer.parseInt(parts[7]);
        int marketCapital = Integer.parseInt(parts[8]);
        System.out.println("__/Market [" + marketID + "]/__" + 
            "\n[1]Silver   : " + marketSilver + "\n[2]Gold     : " + marketGold + 
            "\n[3]Platinum : " + marketPlatinum + "\n[4]Fuel     : " + marketFuel + 
            "\n[5]Bitcoin  : " + marketBitCoin + "\n[-]Capital  :" + marketCapital);
    }
}