package levlab.servlet.one;

import java.io.DataInputStream;
//import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
//import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


//import lejos.pc.comm.NXTCommFactory;
//import lejos.pc.comm.NXTConnector;

/**
 * Servlet implementation class Bot5cmd
 */
public class Bot5cmd extends HttpServlet implements Runnable{
	
	private static final long serialVersionUID = 1L;

	// Get (and keep) a reference to the one instance of
	// Shared object
	Shared shared = Shared.getInstance();	
	
    Thread status;		// this thread will read status continuously
    
    // Constructor
    public Bot5cmd() {
        super();
    }

    public void init() throws ServletException {
    	status = new Thread(this);
    	status.start();
    }
    
    // The run function should operate continuously reading status
    // whenever shared.state indicates the bot is connected.
    public void run(){
      	
		// Get (and keep) a reference to the one instance of
		// Shared object
		Shared shared = Shared.getInstance();	
		Bot bot = shared.bot5;
		
	    // Setup variables here
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
		int dataLastCommand = 0;
		int dataLastData = 0;
		int dataRange = -1;
		
	    double camTheta;			// camera angle calculated from dataPositionC
	    double camRotations;
	    int camDeg;
	    
//		int camState = 0;
//		int command = 0;
//		int motorState = 0;
	    		
		while (true) {
			
			// Only work with bot8 for now.  Does need other threads
			// so BT host doesn't go crazy when a bot is in BT_ERROR

			if (bot.btState == Bot.BT_OK) {
				dataIn = bot.dataIn;
				try {
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
					dataLastCommand = dataIn.readInt();
					dataLastData = dataIn.readInt();
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
					bot.setData("Command", dataLastCommand);
					bot.setData("Data", dataLastData);
					bot.setData("Range", dataRange);
					
					// Get the scancam total rotations, radian angle, then degree angle	
					camRotations = (double)bot.getData("PosC") / (double)2520.0;
					camTheta = 2.0*Math.PI*( camRotations-Math.floor(camRotations));
					camDeg = (int)(camTheta*180.0/Math.PI);
					if(camDeg>360) camDeg -=360;
					if(camDeg<0) camDeg +=360;
					
					bot.setData("CamDegree",camDeg);
					bot.setData("CamTurns", (int)camRotations);

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

		// Possible arguments and default values
		String arg;
		int command = 0;
		int data1 = 0;
		int data2 = 0;
		int data3 = 0;
		String response = "BotCmd.java response not set";

		Bot bot = null;
		bot = shared.bot5;

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
