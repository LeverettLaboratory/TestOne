package levlab.servlet.one;

import java.io.DataInputStream;
//import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class Bot8cmd
 */
public class BotCmd extends HttpServlet implements Runnable{
	private static final long serialVersionUID = 1L;
	// Get (and keep) a reference to the one instance of
	// Shared object
	Shared shared = Shared.getInstance();

	Thread status; // this thread will read status continuously

	// Constructor
	public BotCmd() {
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
		shared.bot8.setData("Test", 1);

		Bot bot = null;
		boolean readOk = false;
		DataInputStream dataIn;
		int recordNum = 0;
		int dataSignal = 0;
		int dataBattery = 0;
		int dataPositionA = 0;
		int dataPositionB = 0;
		int dataPositionC = 0;
		int dataLastCommand = 0;
		int dataLastData = 0;
		int dataPitch = 0;
		int dataRoll = 0;
		int dataBearing = 0;
				
		
		while (true) {
			
			// Only work with bot8 for now.  Does need other threads
			// so BT host doesn't go crazy when a bot is in BT_ERROR

			bot = shared.bot8;
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
					dataLastCommand = dataIn.readInt();
					dataLastData = dataIn.readInt();
					dataPitch = dataIn.readInt();
					dataRoll = dataIn.readInt();
					dataBearing = dataIn.readInt();
					readOk = true;
				} catch (IOException e) {
					// An error reading data
					bot.btState = Bot.BT_ERROR;
					shared.bot8.setData("Test", 3);

				}
				if(readOk){					
					// Store the data into this bot's data map
					bot.setData("Record", recordNum);
					bot.setData("Signal", dataSignal);
					bot.setData("Battery", dataBattery);
					bot.setData("PosA", dataPositionA);
					bot.setData("PosB", dataPositionB);
					bot.setData("PosC", dataPositionC);
					bot.setData("Command", dataLastCommand);
					bot.setData("Data", dataLastData);
					bot.setData("Pitch", dataPitch);
					bot.setData("Roll", dataRoll);
					bot.setData("Bearing", dataBearing);
					shared.bot8.setData("Test", 4);

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

		// If connected send a command to NXT, 
		// then use Shared.data to formulate a response to the
		// web browser client.
		if (bot.btState == Bot.BT_OK) {

			
			// send a command, if its just a status request, nothing is sent
			// to the NXT, instead we rely on the data in shared.x to have
			// been updated recently.
			if(command != 1) bot.send(command, data1, data2, data3);

			// make a response string that reflects current state of the bot
			// this is returned to the web browser client and shown on the
			// status line
			/*
			  response =
			  "Sig "+bot.btSignal+", Bat "+bot.Battery+", Cmd "+bot.lastCommand+", "+bot.lastData
			  +"<br>"+					  
			  "Bear "+bot.Bearing+", Pitch "+bot.Pitch+", Roll "+bot.Roll;
			  */
			response = "Pitch "+bot.getData("Pitch")+", Roll "+bot.getData("Roll");

		}else{
			// Try to get a connection
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
