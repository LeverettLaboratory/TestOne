package levlab.servlet.one;

import java.io.DataInputStream;
//import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/* This file is the servlet that answers XMLHttpRequests made by the javascript
 * on one of the web pages when it wants to send a command to the Rover.
 * It also makes a status thread that continually tries to read data from
 * the rover and place the results into the Shared class singleton so it
 * will be available to all the servlets.
 * 
 * Dr Tom Flint, Leverett Laboratory, 1 Feb 2014.
 * 
 */


/**
 * Servlet implementation class Bot8bcmd
 */
public class Bot8cmd extends HttpServlet implements Runnable{
	private static final long serialVersionUID = 1L;
	// Get (and keep) a reference to the one instance of
	// Shared object
	Shared shared = Shared.getInstance();

	Thread status; // this thread will read status continuously

	// Constructor
	public Bot8cmd() {
		super();
	}

	public void init() throws ServletException {
		status = new Thread(this);
		status.start();
	}

	// The run function should operate continuously reading status
	// whenever the bluetooth connection is up.
	
	public void run() {

		// Get (and keep) a reference to the one instance of
		// Shared object
		Shared shared = Shared.getInstance();
		Bot bot = shared.bot8;
		
		boolean readOk = false;
		DataInputStream dataIn;
		int recordNum = 0;
		int dataSignal = 0;
		int dataBattery = 0;
		int dataPositionA = 0;
		int dataPositionB = 0;
		int dataPositionC = 0;
		int dataPowerA = 0;
		int dataPowerB = 0;
		int dataPowerC = 0;
		int dataStateA = 0;
		int dataStateB = 0;
		int dataFwdSpeedIndex = 0;
		int dataTurnSpeedIndex = 0;
		int dataClaw = 0;
		int dataLastCommand = 0;
		int dataLastData = 0;
		int dataPitch = 0;
		int dataRoll = 0;
		int dataBearing = 0;
		int dataRange = -1;
				
		
		while (true) {
			
			// Only work with bot8 for now.  Does need other threads
			// so BT host doesn't go crazy when a bot is in BT_ERROR

			if (bot.btState == Bot.BT_OK) {
				dataIn = bot.dataIn;
				try {
					shared.bot8.setData("Test", 2);
					// Read integer data:
					readOk = false;
					recordNum = dataIn.readInt();
					dataSignal = dataIn.readInt();
					dataBattery = dataIn.readInt();
					dataPositionA = dataIn.readInt();
					dataPositionB = dataIn.readInt();
					dataPositionC = dataIn.readInt();
					dataPowerA = dataIn.readInt();
					dataPowerB = dataIn.readInt();
					dataPowerC = dataIn.readInt();
					dataStateA = dataIn.readInt();
					dataStateB = dataIn.readInt();
					dataFwdSpeedIndex = dataIn.readInt();
					dataTurnSpeedIndex = dataIn.readInt();
					dataClaw = dataIn.readInt();
					dataLastCommand = dataIn.readInt();
					dataLastData = dataIn.readInt();
					dataPitch = dataIn.readInt();
					dataRoll = dataIn.readInt();
					dataBearing = dataIn.readInt();
					dataRange = dataIn.readInt();
					readOk = true;
				} catch (IOException e) {
					// An error reading data
					bot.btState = Bot.BT_ERROR;

				}
				if(readOk){					
					// Store the data into this bot's data map
					bot.setData("Record", recordNum);
					bot.setData("Signal", dataSignal);
					bot.setData("Battery", dataBattery);
					bot.setData("PosA", dataPositionA);
					bot.setData("PosB", dataPositionB);
					bot.setData("PosC", dataPositionC);
					bot.setData("PowerA", dataPowerA);
					bot.setData("PowerB", dataPowerB);
					bot.setData("PowerC", dataPowerC);
					bot.setData("StateA", dataStateA);
					bot.setData("StateB", dataStateB);
					bot.setData("FwdSpeedIndex", dataFwdSpeedIndex);
					bot.setData("TurnSpeedIndex", dataTurnSpeedIndex);
					bot.setData("Claw", dataClaw);
					bot.setData("Command", dataLastCommand);
					bot.setData("Data", dataLastData);
					bot.setData("Pitch", dataPitch);
					bot.setData("Roll", dataRoll);
					bot.setData("Bearing", dataBearing);
					bot.setData("Range", dataRange);

				}
			}else{
				// BT_NOT or BT_ERROR
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
		}	// end of while(true)
	}  // end of run()

	// -----------------------------------------------------------------
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected synchronized void doGet(HttpServletRequest req,
			HttpServletResponse res) throws ServletException, IOException {

		// Possible arguements and default values
		String arg;
		int command = 0;
		int data1 = 0;
		int data2 = 0;
		int data3 = 0;
		String response = "BotCmd.java response not set";

		Bot bot = null;
		bot = shared.bot8;

		// get variable number of arguments from the http request
		// any non-existent args will have the default value set
		// up above when the variables are defined
		// TF TEST this doesn't work, any missing args throws error 500
		// back at the XHR
		arg=req.getParameter("command");
		if(arg!=null){
			command = Integer.parseInt(arg);
		}
		arg=req.getParameter("data1");
		if(arg!=null){
			data1 = Integer.parseInt(arg);
		}
		arg=req.getParameter("data2");
		if(arg!=null){
			data2 = Integer.parseInt(arg);
		}
		arg=req.getParameter("data3");
		if(arg!=null){
			data3 = Integer.parseInt(arg);
		}

		// Check if BT link is up
		if (bot.btState == Bot.BT_OK) {
			// Bluetooh link is up
			if(command == 1){
				// Status command, so return JSON encoded string
				// to represent the contents of the bot hashMap
				response = bot.jsonData();
			}else{
				// Other commands are passed on to rover, return
				// a string to acknowledge receipt
				bot.send(command, data1, data2, data3);
				response = "Command "+command+" ok";
			}
		}else{
			// Bluetooth link is not up.  Try to get a connection
			if (bot.connect()) {
				response = "Connection Good";
			} else {
				response = "Connection Failed";
			}
		}	// end of if BT_OK

		// Generate a response to the XMLHttpRequest
		res.setContentType("text/html");
		PrintWriter out = res.getWriter();
		out.println(response);
		res.flushBuffer();

	}

}
