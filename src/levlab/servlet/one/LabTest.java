package levlab.servlet.one;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
		// Respond to an updateRequest
		String arg;
		int command;
		String response = "LabTest Servlet: yes I am here.";
		
		// Get any arguments that were passed
		arg=req.getParameter("command");
		if(arg!=null){
			command = Integer.parseInt(arg);
		}
		
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
