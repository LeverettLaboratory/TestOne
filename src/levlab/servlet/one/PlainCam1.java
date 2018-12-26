package levlab.servlet.one;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
//import java.io.DataInputStream;
//import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
//import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import lejos.pc.comm.NXTConnector;

/**
 * Servlet implementation class PlainCam1
 * This class returns an image from one of the cameras.
 * It may create an overlay on the image using data from 
 * the Shared.Botx singleton.
 */
public class PlainCam1 extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// Get (and keep) a reference to the one instance of
	// Shared object
	Shared shared = Shared.getInstance();	
	
	// Frame counter
	int camFrame = 0;
	
    // Setup variables here?
//    NXTConnector connector;          // object for bluetooth comms
//    boolean connected = false;       // bluetooth connection flag
//    DataInputStream dataIn;          // bluetooth data from the bot
//    DataOutputStream dataOut;        // bluetooth data to the bot
    int dataVal;						// integer read from BT
    int nDataIn;						// number of bytes? avail on dataIn
    int dataSignal;						// bluetooth signal strength reported by Bot
    int dataBattery;					// battery millivolts reported by Bot

    BufferedImage bimg = null;
    BufferedImage camImg;
    BufferedImage camImg2;
    BufferedImage camImg3;
    BufferedImage backImg;
    BufferedImage outImg;

	String botPreURL = "http://localhost/TestOne/Bot6cmd?";
	String botPostURL = "&timestamp=";
    
    
    /**
     * @see HttpServlet#HttpServlet()
     */
    public PlainCam1() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		
		// Get any arguments that are passed in the url
		String arg;
		int imgType = 1;	// initialize to defaults
		int camNum = 1;
		int botNum = 3;
		String camAddr = "192.168.2.121";
		String titleString ="- no idea which bot -";
		Bot botx;
		
		arg=req.getParameter("type");
		if(arg!=null){
			imgType = Integer.parseInt(arg);
		}
		
		// choose the cam string to use
		arg=req.getParameter("cam");
		if(arg!=null){
			camNum = Integer.parseInt(arg);
		}
		if(camNum == 1){
			camAddr = "192.168.2.121";
		}
		if(camNum == 2){
			camAddr = "192.168.2.122";
		}
		if(camNum == 3){
			camAddr = "192.168.2.123";
		}
		if(camNum == 4){
			camAddr = "192.168.2.124";
		}
		
		// pick the bot for symbology
		arg=req.getParameter("bot");
		if(arg!=null){
			botNum = Integer.parseInt(arg);
		}
		botx = shared.bot5;
		titleString = "- no idea which bot -";
		if(botNum == 5){
			botx = shared.bot5;
			titleString = "- ScanBot 5 -";
		}
		if(botNum == 7){
			botx = shared.bot7;
			titleString = "- Speedy 7 -";
		}
		if(botNum == 9){
			botx = shared.bot9;
			titleString = "- Rover Nine -";
		}
		if(botNum == 8){
			botx = shared.bot8;
			titleString = "- Rover Eight -";
		}
		
		
		
		// common tasks for all image types		

		// get a time/date string 
		DateFormat plain = DateFormat.getTimeInstance(DateFormat.DEFAULT);
		String now = plain.format(new Date());
		
		// make a frame counter that rolls over after reaching 4 digits
		if(++camFrame>9999) camFrame=0;

		
		// text for the bluetooth connection
		String btText = "Error";
		int btState = botx.btState;
		if(btState==Bot.BT_OK){
			// bt is connected so show signal strength
			btText ="Sig "+botx.getData("Signal");
		}
		if(btState==Bot.BT_NOT){
			// not connected
			btText = "No BT";
		}
		if(btState==Bot.BT_ERROR){
			// error
			btText ="BT Error";
		}

		// text for the battery voltage
		String voltText = "Error";
		voltText = "Bat "+botx.getData("Battery"); 
			
		String statusText = now+", "+btText+", "+voltText+", #"+camFrame;
		
		if (imgType == 0){
			// Just a clean image from the cam with NO overlays
			// Open a stream and read a still JPG image from the camera
			URL camURL = new URL("http://"+camAddr+"/jpg/image.jpg?resolution=640x480&compression=0&clock=0&date=0&text=0");

			// TEST try a method that sets some timeouts on the connection so when battery runs down and camera stops
			// responding it doesn't lock up the image.
			try{
				URLConnection camConnect = camURL.openConnection();
				camConnect.setConnectTimeout(400);
				camConnect.setReadTimeout(400);
				InputStream camIn = camConnect.getInputStream();			
				BufferedInputStream camInBuf = new BufferedInputStream(camIn);
				outImg = ImageIO.read(camInBuf);
			}catch(Exception e){
				outImg = new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);
			}						
			
		}else if (imgType == 1){
			// Full size cam image with overlays	
			// Open a stream and read a still JPG image from the camera
			URL camURL = new URL("http://"+camAddr+"/jpg/image.jpg?resolution=640x480&compression=0&clock=0&date=0&text=0");

			try{
				URLConnection camConnect = camURL.openConnection();
				camConnect.setConnectTimeout(400);
				camConnect.setReadTimeout(400);
				InputStream camIn = camConnect.getInputStream();			
				BufferedInputStream camInBuf = new BufferedInputStream(camIn);
				outImg = ImageIO.read(camInBuf);
			}catch(Exception e){
				// if any of the image fetching code fails just make a plain black image of the right size
				outImg = new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);
			}						
			
			// obtain a graphics handle for the image
			Graphics2D g3 = outImg.createGraphics();
			
			// Title overlay
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,48));
			g3.drawString("Leverett Laboratory", 80, 50);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,24));
			g3.drawString(titleString, 240, 80);

			// Status overlay		
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,24));
			g3.drawString(statusText, 30, 460);
	
			g3.dispose();
			
		}else if(imgType == 2){
			
			// Smaller version of first image type
			
			// Open a stream and read a still JPG image from the camera 
			try{
				URL camURL = new URL("http://"+camAddr+"/jpg/image.jpg?resolution=480x360&compression=0&clock=0&date=0&text=0");
				URLConnection camConnect = camURL.openConnection();
				camConnect.setConnectTimeout(400);
				camConnect.setReadTimeout(400);
				InputStream camIn = camConnect.getInputStream();			
				BufferedInputStream camInBuf = new BufferedInputStream(camIn);
				outImg = ImageIO.read(camInBuf);
			}catch(Exception e){
				outImg = new BufferedImage(320,240,BufferedImage.TYPE_INT_RGB);
			}						

			// obtain a graphics handle for the image
			Graphics2D g3 = outImg.createGraphics();
			
			// Title overlay
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,32));
			g3.drawString("Leverett Laboratory", 65, 25);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,18));
			g3.drawString(titleString, 125, 40);
	
			// Status overlay		
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,18));
			g3.drawString(statusText, 15, 340);
	
			g3.dispose();

		}else if (imgType==3){

			// Half scale version of first image type
			
			// Open a stream and read a still JPG image from the camera 
			try{
				URL camURL = new URL("http://"+camAddr+"/jpg/image.jpg?resolution=320x240&compression=0&clock=0&date=0&text=0");
				URLConnection camConnect = camURL.openConnection();
				camConnect.setConnectTimeout(400);
				camConnect.setReadTimeout(400);
				InputStream camIn = camConnect.getInputStream();			
				BufferedInputStream camInBuf = new BufferedInputStream(camIn);
				outImg = ImageIO.read(camInBuf);
			}catch(Exception e){
				outImg = new BufferedImage(320,240,BufferedImage.TYPE_INT_RGB);
			}						

			// obtain a graphics handle for the image
			Graphics2D g3 = outImg.createGraphics();
			
			// Title overlay
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,24));
			g3.drawString("Leverett Laboratory", 45, 25);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,12));
			g3.drawString(titleString, 125, 40);
	
			// Status overlay		
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,12));
			g3.drawString(statusText, 15, 220);
	
			g3.dispose();
			
		}else if (imgType==4){
			// Image with overlays for the Nexus
			
			// Open a stream and read a still JPG image from the camera
			try{
				URL camURL = new URL("http://"+camAddr+"/jpg/image.jpg?resolution=320x240&compression=0&clock=0&date=0&text=0");
				URLConnection camConnect = camURL.openConnection();
				camConnect.setConnectTimeout(500);
				camConnect.setReadTimeout(1000);
				InputStream camIn = camConnect.getInputStream();			
				BufferedInputStream camInBuf = new BufferedInputStream(camIn);
				camImg = ImageIO.read(camInBuf);
			}catch(Exception e){
				camImg = new BufferedImage(320,240,BufferedImage.TYPE_INT_RGB);
			}						
			
			outImg = new BufferedImage(420,240,BufferedImage.TYPE_INT_RGB);
			Graphics2D g3 = outImg.createGraphics();
			
			// draw on the main image first
			g3.drawImage(camImg, 0, 0, 320, 240, null);

			int statX = 330;
			
			// Title overlay
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,18));
			g3.drawString("Leverett", statX,50);
			g3.drawString("Laboratory", statX-10,70);			
			g3.setFont(new Font("Times New Roman",Font.PLAIN,12));
			g3.drawString(titleString, statX, 85);
							
			// Status overlay		
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,12));
			// Can't use full status string, will need to break it into lines
			g3.drawString(now, statX, 130);
			
			// special colors for BT string
			if(btState<=0) g3.setPaint(Color.YELLOW);
			if(btState<-1) g3.setPaint(Color.RED);
			g3.drawString(btText, statX, 190);
			g3.setPaint(Color.GREEN);
			
			g3.drawString(voltText, statX, 210);
			g3.drawString("F "+camFrame, statX, 230);
			
			// Try to get the Lab Logo
			ServletContext context = getServletContext();
			InputStream in = context.getResourceAsStream("/images/L2-logo-32.png");
			BufferedInputStream inBuf = new BufferedInputStream(in);
			Image logo1 = ImageIO.read(inBuf);
			g3.drawImage(logo1, statX+20, 4, 32, 32, null);
									
			g3.dispose();

		}else{
			// Requested an imgType that is not defined

			// Try to insert the image onto a larger background canvas
			outImg = new BufferedImage(480,268,BufferedImage.TYPE_INT_RGB);
			Graphics2D g3 = outImg.createGraphics();

			// Title overlay
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,18));
			g3.drawString("Leverett Laboratory", 20, 20);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,12));
			g3.drawString(titleString, 35, 35);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,14));
			g3.drawString("Image Type undefined.", 30, 160);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,24));
			g3.drawString("Error: "+imgType, 50, 100);
	
			g3.dispose();

		}
			
		// set the response type
		res.setContentType("image/jpeg");

		// send the image straight to the client
		ServletOutputStream out = res.getOutputStream();

		// write the image as a jpg
		ImageIO.write(outImg, "JPEG", out);
	
		res.flushBuffer();
	
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}






