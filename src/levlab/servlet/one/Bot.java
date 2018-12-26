package levlab.servlet.one;

/* This file is the Bot class.  It is how the servlets running on the Apache server
 * keep track of a bot, its bluetooth connection, and any data it may have.
 * 
 * Methods:
 * 
 * boolean connect();
 * void send( (int)command, (int)data1, (int)data2, (int)data3);
 * void setData( (String)label, value);
 * int getData( (String)label );
 * 
 * Dr Tom Flint, Leverett Laboratory, 1 Feb 2014.
 * 
 */
import java.io.DataInputStream;
import java.io.DataOutputStream;
//import java.io.FileInputStream;
import java.io.IOException;
//import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTConnector;

/**
 * @author tflint
 *
 */
public class Bot {

	/**  This class is how the java servlets on the Apache server host keep track of 
	 * the bots.
	 * @param args
	 */
	// Constants visible at the class level
	public static final int BT_NOT = 0;
	public static final int BT_OK = 1;
	public static final int BT_ERROR = 2;

	  int btState = BT_NOT;			// this is the bluetooth state
	  int speed = 0;
	  int turn = 0;
	
	  int recordNum = -1;
//	  int newData = 0;		// reset by requester, set on write by botXcmd
	
	  NXTConnector connector; // object for bluetooth comms
	  boolean connected = false; // bluetooth connection flag
	  DataInputStream dataIn;          // bluetooth data from the bot
	  DataOutputStream dataOut;        // bluetooth data to the bot
	  
	  String name;
	  String addr;
	  
	  // try to setup flexible data storage
	  Map<String, Object> m = new HashMap<String, Object>();
	  
	  // Here is the constructor
	  Bot(String aName, String aAddress){
		  name = aName;
		  addr = aAddress;
	  }
	  
	  // Store any kind of data from the bot using a label as the key
	  public void setData(String aLabel, Object aData){
		  m.put(aLabel, aData);		  
	  }
	
	  // Get any integer data from the bot using a label as the key
	  // note that if the key does not exist this still returns zero
	  // so that errors won't be thrown
	  public int getData(String aLabel){		  
		  Object value;
		  value = m.get(aLabel);
		  if(value==null){
			  return(0);
		  }else{
			  return ((Integer)(value));
		  }		  
	  }
	  // A variant to retrieve a float instead of an integer
	  // not tested yet
	  public float getDataF(String aLabel){		  
		  Object value;
		  value = m.get(aLabel);
		  if(value==null){
			  return(0);
		  }else{
			  return ((Float)(value));
		  }		  
	  }
	  
	  // Convert contents of the data hashMap to a JSON string 
	  public String jsonData(){
		  String out = "[ ";
	      // Get a set of the entries
	      Set set = m.entrySet();
	      // Get an iterator
	      Iterator i = set.iterator();
	      // Display elements
	      while(i.hasNext()) {
	         Map.Entry me = (Map.Entry)i.next();
	    	 out += "{ \"label\" : \""; 
	         out += me.getKey();
	         out += "\", \"value\" : \"";
	         out += me.getValue();
	         out += "\", },";
	      }
	      out += "]";
	      return(out);
	  }
	  
/*	  
		response = "[ ";
		response += "{ \"label\" : \"Battery\",";
		response += "  \"value\" : \"300\", }";
		response += ",";
		response += "{ \"label\" : \"Signal\",";
		response += "  \"value\" : \"500\", }";
		response += ",";
		response += "{ \"label\" : \"Pitch\",";
		response += "  \"value\" : \"-50\", }";
		response += "]";
*/												
	  
	  
	  
	  public boolean connect() {

			btState = Bot.BT_NOT;
			dataIn = null;
			dataOut = null;
			
			connector = new NXTConnector();
			boolean ok = connector.connectTo(name, addr, NXTCommFactory.BLUETOOTH);
			if (!ok) {
				btState = Bot.BT_ERROR;
				dataIn = null;
				dataOut = null;
				return (false);
			}
			dataIn = new DataInputStream(connector.getInputStream());
			if (dataIn == null) {
				btState = Bot.BT_ERROR;
				dataIn = null;
				dataOut = null;
				return (false);
			}
			dataOut = new DataOutputStream(connector.getOutputStream());
			if (dataOut == null) {
				btState = Bot.BT_ERROR;
				dataIn = null;
				dataOut = null;
				return (false);
			}
			// Success
			btState = Bot.BT_OK;
			return true;
		} // end of connect()
	
	  /** Send integer command and 3 int data to the Bot
	   * 
	   * @param command
	   * @param data1
	   * @param data2
	   * @param data3
	   * @return
	   */
	  public boolean send(int command, int data1, int data2, int data3) {

			if(dataOut != null){
				try {
					dataOut.writeInt(command);
					dataOut.writeInt(data1);
					dataOut.writeInt(data2);
					dataOut.writeInt(data3);
					dataOut.flush();
				} catch (IOException e) {
					btState=Bot.BT_ERROR;
					return (false);
				}
			}
			return (true);
		}
	  
	  // main does nothing in this class
	  public static void main(String[] args) {
	  }
		
	  

}

