package app.fix;

import app.Server;
import app.fix.exceptions.InvalidChecksumException;
import app.fix.exceptions.InvalidMsgLengthException;
import app.fix.exceptions.InvalidMsgTypeException;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class FixProtocol {
	// https://www.onixs.biz/fix-dictionary/4.4/msgs_by_category.html
	private String     userID;
	private int         msgSeqNum;

	public FixProtocol(String userID) {
		   this.userID = userID;
		   this.msgSeqNum = 0;
	   }
   
	//General checks
		public static boolean 	isNumeric(String str) {
			try {
				double d = Double.parseDouble(str);
			} catch(NumberFormatException nfe) {
				return false;
			}
			return true;
		}
	
		public static boolean   isInteger(String s) {
		   try {
			   Integer.parseInt(s);
		   } catch(NumberFormatException e) {
			   return false;
		   } catch(NullPointerException e) {
			   return false;
		   }
		   return true;
	   }

	//Receiving Messages
		public int				validateMessage(String fixMessage) {
			try {
				checksumValidator(fixMessage);
			} catch (InvalidChecksumException e) {
				return -1; 													//-1 means bad checksum
			}
			try {
				msgLengthValidator(fixMessage);

			}catch (InvalidMsgLengthException me) {
				return -2; 													// -2 means bad message length
			}
			return 1;
		}

		public String 		    receiveMessage(String messageInput) {
			int ret = validateMessage(messageInput);
			String rejectMessage = null;

			if (ret == -1) {
				int msgSqnNum = -1;																						//Reject through the checksum - first get the msgSequence number
				String[] message = messageInput.split("\\|");
				for (int i=0; i < message.length; i++) {
					if (message[i].startsWith("34=") && isNumeric(message[i].substring(3)) && isInteger(message[i].substring(3))) {
						msgSqnNum = Integer.parseInt(message[i].substring(3));
					}
				}
				if (msgSqnNum < 1) {
					msgSqnNum = 1;
				}
				rejectMessage = RejectMessage(msgSqnNum, 99, "InvalidCheckSum");
			} else if (ret == -2) {
				int msgSqnNum = -1;																						//Reject through the msgLength - first get the msgSequence number
				String[] message = messageInput.split("\\|");
				for (int i=0; i < message.length; i++) {
					if (message[i].startsWith("34=") && isNumeric(message[i].substring(3)) && isInteger(message[i].substring(3))) {
						msgSqnNum = Integer.parseInt(message[i].substring(3));
					}
				}
				if (msgSqnNum < 1) {
					msgSqnNum = 1;
				}
				rejectMessage = RejectMessage(msgSqnNum, 99, "InvalidMsgLength");
			} else if (ret == 1) {
				return null;																							//Message valid
			}
			return rejectMessage;
		}

	//Validation Checks
		public boolean          checksumValidator(String input) throws InvalidChecksumException {
			// Reference: https://gigi.nullneuron.net/gigilabs/calculating-the-checksum-of-a-fix-message/
			
			String[] values = input.split("\\|10=");                                                                //Separates the checksum from the message

			if (values.length != 2) {                                                                               //Make sure that there are only 2 strings: the input and the checksum
				throw new InvalidChecksumException("Invalid Checksum");
			}
			
			values[0] += '|';                                                                                       //Add the pipe back to the first string to end the input
			
			String checksumStr = checksumGenerator(values[0]);

			if (!checksumStr.equals(values[1].substring(0,3))) {
				throw new InvalidChecksumException("Invalid Checksum");
			}
			return true;
		}

		public boolean          msgLengthValidator(String messageInput) throws InvalidMsgLengthException {
			int msgLength = -1;

			if (!messageInput.contains("|9=")) {
				throw new InvalidMsgLengthException("Incorrect Message Length");
			}
			int msgIndexStart = messageInput.indexOf("|9=") + 3;
			while (messageInput.charAt(msgIndexStart) != '|') { msgIndexStart++;}                                   //Get the starting index of the message length flag to get the length
			msgIndexStart++;
			
			int msgIndexEnd = messageInput.indexOf("|10=");                                                         //Get the end index of the message to calculate length
			msgIndexEnd++;

			String innerMessage = messageInput.substring(msgIndexStart, msgIndexEnd);								//Get message length from indexes

			
			String[] message = messageInput.split("\\|");                                                           //Get the given message length
			for (int i=0; i < message.length; i++) {
				if (message[i].startsWith("9=") && 
					isNumeric(message[i].substring(2)) && isInteger(message[i].substring(2))) {
					msgLength = Integer.parseInt(message[i].substring(2));
				}
			}
			if (msgLength < 0 || msgLength != innerMessage.length()) {
				throw new InvalidMsgLengthException("Incorrect Message Length");
			}
			return true;
		}

	//Default Generators
		public String           checksumGenerator(String messageInput) {
			
			String message = messageInput.replace('|', '\u0001');                                                   //Replace | with the ascii value of 1 (a non-printable character) to ensure the correct byte size
			
			byte[] messageBytes = message.getBytes(StandardCharsets.US_ASCII);                                      //Put the message in ascii byte value into a byte array
			
			int total = 0;                                                                                          //Iterate through byte array and add each bytes value to get the total byte size of the message
			for (int i = 0; i < message.length(); i++) {
				total += messageBytes[i];
			}
			Integer checksumResult = total % 256;                                                                   //Modulus by 256 to get what the checksum value should be

			String checksumStr = "000".substring(checksumResult.toString().length()) + checksumResult.toString();   //Get the correct padding

			return checksumStr;
		}
	
		public String            constructHeader(String bodyMessage, String type) {
			//Protocol Version|length|Message Type|Message Sequence Number|Date|

			StringBuilder header = new StringBuilder();

			header.append("8=FIX4.4|");                                                                             //Protocol version. Always unencrypted, must be first field in message.

			StringBuilder message = new StringBuilder();

			message.append("35=" + type + "|");                                                                     //Message type. Always unencrypted, must be the third field in message.

			this.msgSeqNum++;                                                                                       //Message Sequence Number. Starts at 1
			message.append("34=" + this.msgSeqNum + "|");

			DateFormat df = new SimpleDateFormat("yyyyMMddHH:mm:ss");
			df.setTimeZone(TimeZone.getTimeZone("GMT"));
			Date date = new Date();
			message.append("52=" + df.format(date) + "|");                                                          //Time of message transmission (always expressed in UTC (Universal Time Coordinated), also known as 'GMT'))
			
			int length = message.length() + bodyMessage.length();
			header.append("9=" + length + "|");																		//Message body length. Always unencrypted, must be second field in message.
			header.append(message);

			return header.toString();
		}

	//Session Related Message Generation
		public String           logonMessage(int heartbeat) {
		   //Encryption|UserID|Heartbeat|resetSeqNum|

		   StringBuilder body = new StringBuilder();
   
		   body.append("98=0|");																					//Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)
   
		   body.append("553=" + this.userID + "|");																	//The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)
   
		   
		   if (heartbeat > 0) {																						//Heartbeat interval in seconds.
			   body.append("108=" + heartbeat + "|");
		   } else {
			   body.append("108=" + "120" + "|");
		   }

		   //TODO
		   //To be taken out
		   body.append("141=Y|");																					//Each FIX message has a unique sequence number (MsgSeqNum (34) tag) - https://kb.b2bits.com/display/B2BITS/Sequence+number+handling
		   this.msgSeqNum = 0;																						//Sequence numbers are initialized at the start of the FIX session starting at 1 (one) and increment through the session
		  
   
		   String header = constructHeader(body.toString(), "A"); 													//Logon = "A"
   
		   String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
   
		   return message;
	   }
   
		public String           logoutMessage() {
		//Encryption|UserID|

		StringBuilder body = new StringBuilder();
		
		body.append("98=0|");																								//Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)

		body.append("553=" + this.userID + "|");																			//The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)

		String header = constructHeader(body.toString(), "5"); 																//Logout = "5"

		String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

		return message;
	}

		public String           heartBeatMessage() {
			//Encryption|UserID|Heartbeat|resetSeqNum

			StringBuilder body = new StringBuilder();

			body.append("98=0|");																								//Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)
			
			body.append("553=" + this.userID + "|");																			//The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)

			String header = constructHeader(body.toString(), "0"); 																//Heartbeat = "0"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}
		   
	//Buy
		public String			buyOrderMessage(String marketID, String itemID, String purchaseAmount,
														String purchasePrice, String brokerRouteID) {
			StringBuilder body = new StringBuilder();

			body.append("98=0|");																								//Encryption

			body.append("553=" + this.userID + "|");																				//UserID

			body.append("554=" + brokerRouteID + "|"); 																			//Need to remove this one somehow, only one ID

			body.append("54=1|");																								//Side <54> = 1 to buy

			body.append("100=" + itemID + "|"); 																					//Instrument -> Product<460> -> Type of product

			body.append("101=" + purchaseAmount + "|"); 																			//Quantity<53>

			body.append("44=" + purchasePrice + "|"); 																			//Price<44>

			body.append("49=" + marketID + "|");																					//SenderCompID <49>

			String header = constructHeader(body.toString(), "D"); 																//New Order - Single = "D"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}

	//Sell
		public String           sellOrderMessage(String marketID, String itemID, String purchaseAmount,
													String purchasePrice, String brokerRouteID) {

		StringBuilder body = new StringBuilder();

		body.append("98=0|");																								//Encryption

		body.append("553=" + this.userID + "|");																			//UserID

		body.append("554=" + brokerRouteID + "|"); 																			//Need to remove this one somehow, only one ID

		body.append("54=2|");																								//Side <54> = 2 to sell

		body.append("100=" + itemID + "|"); 																				//Instrument -> Product<460> -> Type of product

		body.append("101=" + purchaseAmount + "|"); 																		//Quantity<53>

		body.append("44=" + purchasePrice + "|"); 																			//Price<44>

		body.append("49=" + marketID + "|");																				//SenderCompID <49>

		String header = constructHeader(body.toString(), "D"); 																//New Order - Single = "D"

		String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

		return message;
	}

	//List Markets
		public String           listMarketsRequest(String brokerRouteID) {
			//<Header MsgType=M>|Encryption|UserID|<TAIL>
			// itemID, Sell_Price, Sell_Amount, MarketID.
			StringBuilder body = new StringBuilder();
			
			body.append("98=0|");																									//Encryption
			
			body.append("553=" + this.userID + "|");																				//UserID

			body.append("554=" + brokerRouteID + "|"); 																				//Need to remove this one somehow, only one ID

			String header = constructHeader(body.toString(), "M"); 																	//List status request (list markets) - Single = "M"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}
	
		public String           listMarketsResponse(String marketsList) {
			//<Header MsgType=N>Encryption|Text<58>|Tail|
			// itemID, Sell_Price, Sell_Amount, MarketID.
			StringBuilder body = new StringBuilder();
			
			body.append("98=0|");																									//Encryption
			
			body.append("58=" + marketsList + "|");																					//Text

			String header = constructHeader(body.toString(), "N"); 																	//List status request (list markets) - Single = "M"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}
		
		public String           ListMarketReturn() {
			StringBuilder body = new StringBuilder();
	
			body.append("553=" + this.userID + "|");
	
			body.append("600=" + Server.mapHBMarket.keySet() + "|");
	
			String header = constructHeader(body.toString(), "60"); 																//ListMarkets = "60"
	
			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
	
			return message;
		}
	
	//List Market Goods
		public String           marketsDataRequest(String brokerRouteID, String marketDataReqID, String marketID) {
			// <Header MsgTyp=Y>|Encryption|UserID|MDReqID<262>|SenderCompId<49>|Tail   											Market Data Request (From broker) https://www.btobits.com/fixopaedia/fixdic44/message_Market_Data_Request_V_.html
			// itemID, Sell_Price, Sell_Amount, MarketID.
			StringBuilder body = new StringBuilder();
			
			body.append("98=0|");																									//Encryption
			
			body.append("553=" + this.userID + "|");																				//UserID
			
			body.append("554=" + brokerRouteID + "|");																				//Need to remove this one somehow, only one ID
			
			body.append("262=" + marketDataReqID + "|");																			//MarketDataReq ID
			
			body.append("49=" + marketID + "|");																					//MarketID - SenderCompID <49>

			String header = constructHeader(body.toString(), "V"); 																	//Market Data Request - Single = "V"
				
			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
	}
	
		public String           marketsDataResponse(String brokerID, String marketDataReqID, String marketData) {
			// <Header MsgTyp=W>|Encryption|UserID|MDReqID<262>|Text<58>|SenderCompID|Tail											Market Data Request Snapshot/Full Refresh (FromMarket)   https://www.btobits.com/fixopaedia/fixdic44/message_Market_Data_Snapshot_Full_Refresh_W_.html
			// itemID, Sell_Price, Sell_Amount, MarketID.
			StringBuilder body = new StringBuilder();

			
			body.append("98=0|");																									//Encryption

			
			body.append("553=" + brokerID + "|");																					//UserID (who the market is sending to)

			
			body.append("262=" + marketDataReqID + "|");																			//MarketDataReq ID

			
			body.append("58=" + marketData + "|");																					//Text

			body.append("49=" + this.userID + "|");																					//SenderCompID <49>

			String header = constructHeader(body.toString(), "W"); 																	//Market Data Request Snapshot/Full Refresh = "W"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}

		public String           marketsDataReject(String brokerID, String marketDataReqID, String marketData) {
		   // <Header MsgTyp=Y>|UserID|MDReqID<262>|SenderCompID|Tail																Market Data Request Reject (From Market) https://www.btobits.com/fixopaedia/fixdic44/message_Market_Data_Request_Reject_Y_.html
		// itemID, Sell_Price, Sell_Amount, MarketID.
		StringBuilder body = new StringBuilder();
		
		body.append("98=0|");																									//Encryption
		
		body.append("553=" + brokerID + "|");																					//UserID (who the market is sending to)
		
		body.append("262=" + marketDataReqID + "|");																			//MarketDataReq ID

		body.append("49=" + this.userID + "|");																					//SenderCompID <49>

		String header = constructHeader(body.toString(), "Y"); 																	//Market Data Request Snapshot/Full Refresh = "Y"

		String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

		return message;
	}


	//List Goods


	//Other Messages
		public String           RejectMessage(int refSeqNum, int sessionRejectReason, String text) {
			//Encryption|UserID|RefSeqNum|sessionRejectReason|Text
			
			StringBuilder body = new StringBuilder();

			body.append("98=0|");                                                                                                //Define a message encryption scheme. Valid value is "0" = NONE+OTHER (encryption is not used)

			body.append("553=" + this.userID + "|");                                                                             //The numeric User ID. - User is linked to SenderCompID (#49) value (the user's organisation)
			
			body.append("45=" + refSeqNum + "|");                                                                                //Reference to the Message Sequence Number that was rejected

			if ((sessionRejectReason >= 0 && sessionRejectReason <= 17)) {                                                       //Setting the sessionRejectionReason value, as well as adding text to explain further
				body.append("373=" + sessionRejectReason + "|");
			} else if (sessionRejectReason == 99) {
				body.append("373=" + sessionRejectReason + "|");
			} else {
				System.out.println("Invalid rejection value entered.");
				return null;
			}
			if (text != null && !text.isEmpty()) {
				body.append("58=" + text + "|");
			}

			String header = constructHeader(body.toString(), "3"); 																//Reject = "3"

			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";

			return message;
		}

		public String           NullMarket() {
			// Error Non-Existent Market
			StringBuilder body = new StringBuilder();
	
			body.append("553=" + this.userID + "|");
	
			String header = constructHeader(body.toString(), "91"); 																//Non-Existent Market = "91"
	
			String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
	
			return message;
		}
	
	//Getters
		public String		    getMsgType(String messageInput) throws InvalidMsgTypeException {
			String msgType = null;
			if (messageInput == null || !messageInput.contains("|35=")) {
				throw new InvalidMsgTypeException("Invalid Message Type");
			}
			String[] message = messageInput.split("\\|");
			for (int i=0; i < message.length; i++) {
				if (message[i].startsWith("35=")) {
					msgType =message[i].substring(3);
				}
			}
			if (msgType == null) {
				throw new InvalidMsgTypeException("Invalid Message Type");
			}
				return msgType;
		}

		public String		    getHBType(String messageInput) throws InvalidMsgTypeException {
			String msgType = null;
			if (!messageInput.contains("|560=")) {
				throw new InvalidMsgTypeException("Invalid  Heart Beat");
			}
			String[] message = messageInput.split("\\|");
			for (int i=0; i < message.length; i++) {
				if (message[i].startsWith("560=")) {
					msgType =message[i].substring(4);
				}
			}
			if (msgType == null) {
				throw new InvalidMsgTypeException("Invalid Heart Beat");
			}
			return msgType;
		}

		public String		    getMarketRouteID(String messageInput) throws InvalidMsgTypeException {
			String msgRouteID = null;
			if (!messageInput.contains("|103=")) {
				throw new InvalidMsgTypeException("Invalid RouteID Market");
			}
			String[] message = messageInput.split("\\|");
			for (int i=0; i < message.length; i++) {
				if (message[i].startsWith("103=")) {
					msgRouteID =message[i].substring(4);
				}
			}
			if (msgRouteID == null) {
				throw new InvalidMsgTypeException("Invalid Message Type");
			}
			return msgRouteID;
		}

		public String		    getRouteID(String messageInput) throws InvalidMsgTypeException {
			String msgType = null;
			if (!messageInput.contains("|554=")) {
				// TODO To be able to distinguish if message is required to check for broker or market routeID and give appropriate error return
				throw new InvalidMsgTypeException("Invalid RouteID (Broker/Market) Type");
			}
			String[] message = messageInput.split("\\|");
			for (int i=0; i < message.length; i++) {
			if (message[i].startsWith("554=")) {
				msgType =message[i].substring(4);
			}
		}
		if (msgType == null) {
			throw new InvalidMsgTypeException("Invalid Message Type");
		}
			return msgType;
		}





//To delete?
		   // Purchase Message Builder
		   public String           PurchaseMessage(String marketID, String itemID, String purchaseAmount,
												   String purchasePrice, String brokerRouteID) {
			   StringBuilder body = new StringBuilder();
   
			   body.append("553=" + this.userID + "|");
   
			   body.append("554=" + brokerRouteID + "|");
   
			   body.append("100=" + itemID + "|");
   
			   body.append("101=" + purchaseAmount + "|");
   
			   body.append("102=" + purchasePrice + "|");
   
			   body.append("103=" + marketID + "|");
   
			   String header = constructHeader(body.toString(), "1"); 													//Purchase = "1"
   
			   String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
   
			   return message;
		   }
	   
		   // Sale Message Builder
		   public String           SaleMessage(String marketID, String itemID, String purchaseAmount,
											   String purchasePrice, String brokerRouteID) {
			   StringBuilder body = new StringBuilder();
   
			   // Append to body
   
			   String header = constructHeader(body.toString(), "2"); //Sale = "2"
   
			   String message = header + body.toString() + "10=" + checksumGenerator(header + body.toString()) + "|";
   
			   return message;
		   }
   
}