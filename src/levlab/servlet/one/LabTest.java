package levlab.servlet.one;

//import java.io.IOException;
//import java.io.PrintWriter;
import java.io.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// TF TEST 2019 hijack this servlet for comms with PiOne pigpio socket
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Servlet implementation class LabTest
 */
public class LabTest extends HttpServlet {
	private static final long serialVersionUID = 1L;

	// Get (and keep) a reference to the one instance of
	// Shared object
	Shared shared = Shared.getInstance();	
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public LabTest() {
        super();
        // Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		// Try to get comms with the pigpiod running on PiOne
		String response = "<h1>LabTest Servlet</h1>";
		String arg;
		
		// array to hold the response
		int Y[] = {123,456,789,42};

		// These hold the input command default values
		int cmd = 17;
		int p1 = 0;
		int p2 = 0;
		int p3 = 0;
		
		// Parse the inputs which may overwrite the defaults
		arg=req.getParameter("cmd");
		if(arg!=null){
			cmd = Integer.parseInt(arg);
		}
		arg=req.getParameter("p1");
		if(arg!=null){
			p1 = Integer.parseInt(arg);
		}
		arg=req.getParameter("p2");
		if(arg!=null){
			p2 = Integer.parseInt(arg);
		}
		arg=req.getParameter("p3");
		if(arg!=null){
			p3 = Integer.parseInt(arg);
		}
		
		response += "<div> Command out = "+cmd+" "+p1+" "+p2+" "+p3+" </div>";
		try {
			// TF TEST 2019
			Socket pisock = new Socket("192.168.1.151",(int)8888);
			OutputStream out = pisock.getOutputStream();
			InputStream in = pisock.getInputStream();
			DataInputStream din=new DataInputStream(in);
			DataOutputStream dout=new DataOutputStream(out);
			
			// Write 4 ints (assumed 32 bit here)
			dout.writeInt(Integer.reverseBytes(cmd));
			dout.writeInt(Integer.reverseBytes(p1));
			dout.writeInt(Integer.reverseBytes(p2));
			dout.writeInt(Integer.reverseBytes(p3));

			// Try to read back 4 ints returned
			Y[0]=Integer.reverseBytes(din.readInt());
			Y[1]=Integer.reverseBytes(din.readInt());
			Y[2]=Integer.reverseBytes(din.readInt());
			Y[3]=Integer.reverseBytes(din.readInt());
			
			pisock.close();
			
			response += "<div> Response = "+Y[0]+ ", "+Y[1]+", "+Y[2]+", "+Y[3]+" </div>";
		}catch(UnknownHostException e) {
			response += "Unknown host ";
		}catch(IOException e) {
			response += "IO Exception";
		}

		
		// Generate a response to the XMLHttpRequest
		res.setContentType("text/html");
		PrintWriter out = res.getWriter();
		out.println(response);
		res.flushBuffer();		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Auto-generated method stub
	}

}
