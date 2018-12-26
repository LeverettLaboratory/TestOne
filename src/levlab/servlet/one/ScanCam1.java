package levlab.servlet.one;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
//import java.io.PrintWriter;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
//import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lejos.pc.comm.NXTConnector;
//import java.io.*;
//import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
//import java.net.URLConnection;
import java.text.DateFormat;
import java.util.Date;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

import javax.servlet.*;
//import javax.servlet.http.*;
//import javax.swing.ImageIcon;
//import javax.swing.ImageIcon;


/**
 * Servlet implementation class ScanCam1
 */
public class ScanCam1 extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// Get (and keep) a reference to the one instance of
	// Shared object
	Shared shared = Shared.getInstance();	
	
    // Frame counter
	int camFrame = 0;
	
    // Setup variables here?
    NXTConnector connector;          // object for bluetooth comms
    boolean connected = false;       // bluetooth connection flag
    DataInputStream dataIn;          // bluetooth data from the bot
    DataOutputStream dataOut;        // bluetooth data to the bot
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
    
	String botPreURL = "http://localhost/TestOne/Bot5cmd?";
	String botPostURL = "&timestamp=";
	
	Image logo32 = null;
	Image logo64 = null;
    
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ScanCam1() {
        super();
        // TODO Auto-generated constructor stub
    }

    public void init() throws ServletException {

    	// Try to get the Lab Logo 32
		try {
			ServletContext context = getServletContext();
			InputStream in = context.getResourceAsStream("/images/L2-logo-32.png");
			BufferedInputStream inBuf = new BufferedInputStream(in);
			logo32 = ImageIO.read(inBuf);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    	
    	// Try to get the Lab Logo 64
		try {
			ServletContext context = getServletContext();
			InputStream in = context.getResourceAsStream("/images/L2-logo-64.png");
			BufferedInputStream inBuf = new BufferedInputStream(in);
			logo64 = ImageIO.read(inBuf);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    	
    }
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		
		// Get any arguments that are passed in the url
		String arg;
		int imgType = 1;	// initialize to defaults
		int timeout = 300;
		
		arg=req.getParameter("type");
		if(arg!=null){
			imgType = Integer.parseInt(arg);
		}
		
		// only bot with a scancam so far.
		Bot bot = shared.bot5;
		
		// common tasks for all image types
		
		// get a time/date string 
		DateFormat plain = DateFormat.getTimeInstance(DateFormat.DEFAULT);
		String now = plain.format(new Date());
		
		// make a frame counter that rolls over after reaching 4 digits
		if(++camFrame>9999) camFrame=0;

		// Get the rotation angle in radians	
		double camRotations = (double)bot.getData("PosC") / (double)2520.0;
		double theta = 2.0*Math.PI*( camRotations-Math.floor(camRotations));

		// setup the text for angle overlay
		int camDeg = (int)(theta*180.0/Math.PI);
		if(camDeg>360) camDeg -=360;
		if(camDeg<0) camDeg +=360;

		String camDegText = "Error";
		if(camDeg==0){
			camDegText="Fwd "+camDeg+" deg";
		}else if(camDeg==180){
			camDegText="Back "+camDeg+" deg";
		}else if(camDeg<180){
			camDegText="Right "+camDeg+" deg";
		}else{
			camDegText="Left "+(360-camDeg)+" deg";
		}
		
		// calculate an elevation
		int camElev = (int)(100*camRotations / 11.0);	// elevation as percent of 4 rot
		
		// text for the bluetooth connection
		String btText = "unknown";
		switch(bot.btState){
		case Bot.BT_OK:
			// bt is connected so show signal strength
			btText ="Sig "+bot.getData("Signal");
			break;
		case Bot.BT_NOT:
			// bt is not connected 
			btText ="No BT";
			break;
		case Bot.BT_ERROR:
			// bt is not connected 
			btText ="BT Error";
			break;			
		}

		// text for the battery voltage
		String voltText = "Error";
		voltText = "Bat "+bot.getData("Battery"); 
			
		String statusText = now+", "+camDegText+" Elev "+camElev+", "+btText+", "+voltText+", #"+camFrame;

		if (imgType == 0){
			// This is the raw image from the camera with no overlays and no rotation
			
			// Open a stream and read a still JPG image from the camera
			URL camURL = new URL("http://192.168.2.122/jpg/image.jpg?resolution=640x480&compression=0&clock=0&date=0&text=0");

			// TEST try a method that sets some timeouts on the connection so when battery runs down and camera stops
			// responding it doesn't lock up the image.
			URLConnection camConnect = camURL.openConnection();
			camConnect.setConnectTimeout(timeout);
			camConnect.setReadTimeout(timeout);
			try{
				InputStream camIn = camConnect.getInputStream();			
				BufferedInputStream camInBuf = new BufferedInputStream(camIn);
				outImg = ImageIO.read(camInBuf);
			}catch(SocketTimeoutException e){
				outImg = new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);
			}						
			
		}else if (imgType == 1){
			
			// Open a stream and read a still JPG image from the camera
			URL camURL = new URL("http://192.168.2.122/jpg/image.jpg?resolution=640x480&compression=0&clock=0&date=0&text=0");

			// TEST try a method that sets some timeouts on the connection so when battery runs down and camera stops
			// responding it doesn't lock up the image.
			URLConnection camConnect = camURL.openConnection();
			camConnect.setConnectTimeout(timeout);
			camConnect.setReadTimeout(timeout);
			try{
				InputStream camIn = camConnect.getInputStream();			
				BufferedInputStream camInBuf = new BufferedInputStream(camIn);
				camImg = ImageIO.read(camInBuf);
			}catch(SocketTimeoutException e){
				camImg = new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);
			}						
			
			// Try to insert the image onto a larger background canvas
			backImg = new BufferedImage(800,800,camImg.getType());
			Graphics2D g2 = backImg.createGraphics();
			g2.drawImage(camImg,80,160,640,480,null);  
			g2.dispose();
			
			// Rotate the full 800x800, then trim back to 800x800 size
			BufferedImageOp op = new AffineTransformOp(AffineTransform.getRotateInstance(-theta, 400, 400),null);
			camImg2 = op.filter(backImg,null);
			outImg = camImg2.getSubimage(0, 0, 800, 800);
			
			// obtain a graphics handle for the image
			Graphics2D g3 = outImg.createGraphics();
			
			// Title overlay
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,48));
			g3.drawString("Leverett Laboratory", 130, 50);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,24));
			g3.drawString("- Scan-o-rama Camera - ", 250, 80);
	
			// Status overlay		
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,24));
			g3.drawString(statusText, 30, 780);
	
			g3.dispose();
			
		}else if(imgType == 2){
			
			// Half scale version of first image type
			
			// Open a stream and read a still JPG image from the camera 
			URL camURL = new URL("http://192.168.2.122/jpg/image.jpg?resolution=320x240&compression=0&clock=0&date=0&text=0");
			URLConnection camConnect = camURL.openConnection();
			camConnect.setConnectTimeout(timeout);
			camConnect.setReadTimeout(timeout);
			try{
				InputStream camIn = camConnect.getInputStream();			
				BufferedInputStream camInBuf = new BufferedInputStream(camIn);
				camImg = ImageIO.read(camInBuf);
			}catch(SocketTimeoutException e){
				camImg = new BufferedImage(320,240,BufferedImage.TYPE_INT_RGB);
			}						

			// Try to insert the image onto a larger background canvas
			backImg = new BufferedImage(400,400,camImg.getType());
			Graphics2D g2 = backImg.createGraphics();
			g2.drawImage(camImg,40,80,320,240,null);  
			g2.dispose();

			// Rotate the full image around center point then trim back to original size
			BufferedImageOp op = new AffineTransformOp(AffineTransform.getRotateInstance(-theta, 200, 200),null);
			camImg2 = op.filter(backImg,null);
			outImg = camImg2.getSubimage(0, 0, 400, 400);
			
			// obtain a graphics handle for the image
			Graphics2D g3 = outImg.createGraphics();
			
			// Title overlay
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,24));
			g3.drawString("Leverett Laboratory", 65, 25);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,12));
			g3.drawString("- Scan-o-rama Camera - ", 125, 40);
	
			// Status overlay		
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,12));
			g3.drawString(statusText, 15, 390);
	
			g3.dispose();

		}else if (imgType==3){
			
			// Open a stream and read a still JPG image from the camera
			URL camURL = new URL("http://192.168.2.122/jpg/image.jpg?resolution=640x480&compression=0&clock=0&date=0&text=0");

			// sets some timeouts on the connection so when battery runs down and camera stops
			// responding it doesn't lock up the image.
			URLConnection camConnect = camURL.openConnection();
			camConnect.setConnectTimeout(timeout);
			camConnect.setReadTimeout(timeout);
			try{
				InputStream camIn = camConnect.getInputStream();			
				BufferedInputStream camInBuf = new BufferedInputStream(camIn);
				camImg = ImageIO.read(camInBuf);
			}catch(SocketTimeoutException e){
				camImg = new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);
			}						
			
			// insert the image onto a larger background canvas
			backImg = new BufferedImage(800,800,camImg.getType());
			Graphics2D g2 = backImg.createGraphics();
			g2.drawImage(camImg,80,160,640,480,null);  
			g2.dispose();
			
			// Rotate the full 800x800, then trim back to 640x480 size
			BufferedImageOp op = new AffineTransformOp(AffineTransform.getRotateInstance(-theta, 400, 400),null);
			camImg2 = op.filter(backImg,null);
			outImg = camImg2.getSubimage(80, 160, 640, 480);
			
			// obtain a graphics handle for the image
			Graphics2D g3 = outImg.createGraphics();
			
			// Title overlay
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,48));
			g3.drawString("Leverett Laboratory", 80, 50);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,24));
			g3.drawString("- Scan-o-rama Camera - ", 190, 80);
	
			// Status overlay		
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,20));
			g3.drawString(statusText, 10, 460);
	
			g3.dispose();
			
		}else if (imgType==4){
			// Image with overlays for the iPod
			
			// Open a stream and read a still JPG image from the camera
			URL camURL = new URL("http://192.168.2.122/jpg/image.jpg?resolution=640x480&compression=0&clock=0&date=0&text=0");
			URLConnection camConnect = camURL.openConnection();
			camConnect.setConnectTimeout(500);
			camConnect.setReadTimeout(1000);
			try{
				InputStream camIn = camConnect.getInputStream();			
				BufferedInputStream camInBuf = new BufferedInputStream(camIn);
				camImg = ImageIO.read(camInBuf);
			}catch(SocketTimeoutException e){
				camImg = new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);
			}						

			// Try to insert the image onto a larger background canvas
			backImg = new BufferedImage(800,800,camImg.getType());
			Graphics2D g2 = backImg.createGraphics();
			g2.drawImage(camImg,80,160,640,480,null);  
			g2.dispose();
			
			// Rotate the full 800x800
			BufferedImageOp op = new AffineTransformOp(AffineTransform.getRotateInstance(-theta, 400, 400),null);
			camImg2 = op.filter(backImg,null);
			// iPod window is 480x268 wide x high, trim out center section of rotated image
			camImg3 = camImg2.getSubimage(160, 160, 480, 480);	// 480x480

			outImg = new BufferedImage(640, 480 ,camImg2.getType());
			Graphics2D g3 = outImg.createGraphics();
			
			// draw on the main image first
			g3.drawImage(camImg3, 0, 0, 480, 480, null);

			int statX = 520;
			int statY = 140;
			
			// Title overlay
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,20));
			g3.drawString("Leverett", statX,statY);
			g3.drawString("Laboratory", statX-10,statY+20);			
			g3.setFont(new Font("Times New Roman",Font.PLAIN,16));
			g3.drawString("Scan-o-rama", statX, statY+45);
			g3.drawString("Camera", statX+15, statY+65);
							
			// Status overlay		
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,16));
			// Can't use full status string, will need to break it into lines
			g3.drawString(now, statX, statY+120);
			g3.drawString(camDegText, statX, statY+140);
			g3.drawString("Elev "+camElev, statX, statY+160);
			
			// special colors for BT string
			if(bot.btState==Bot.BT_NOT) g3.setPaint(Color.YELLOW);
			if(bot.btState==Bot.BT_ERROR) g3.setPaint(Color.RED);
			g3.drawString(btText, statX, statY+180);
			g3.setPaint(Color.GREEN);
			
			g3.drawString(voltText, statX, statY+200);
			g3.drawString("F "+camFrame, statX, statY+220);
			
	        // Add the Lab Logo
			if(logo64!=null)g3.drawImage(logo64, 528, 20, 64, 64, null);
	        									
			g3.dispose();

		}else if (imgType==5){
			
			// Open a stream and read a still JPG image from the camera
			URL camURL = new URL("http://192.168.2.122/jpg/image.jpg?resolution=640x480&compression=0&clock=0&date=0&text=0");
			URLConnection camConnect = camURL.openConnection();
			camConnect.setConnectTimeout(timeout);
			camConnect.setReadTimeout(timeout);
			try{
				InputStream camIn = camConnect.getInputStream();			
				BufferedInputStream camInBuf = new BufferedInputStream(camIn);
				camImg = ImageIO.read(camInBuf);
			}catch(SocketTimeoutException e){
				camImg = new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);
			}						

			// Try to insert the image onto a larger background canvas
			backImg = new BufferedImage(800,800,camImg.getType());
			Graphics2D g2 = backImg.createGraphics();
			g2.drawImage(camImg,80,160,640,480,null);  
			g2.dispose();
			
			// Rotate the full 800x800, then trim to much smaller central region that always has image available
			BufferedImageOp op = new AffineTransformOp(AffineTransform.getRotateInstance(-theta, 400, 400),null);
			camImg2 = op.filter(backImg,null);
			camImg3 = camImg2.getSubimage(230, 230, 340, 340);		// Largest square?

			outImg = new BufferedImage(480, 360 ,camImg2.getType());

			// obtain a graphics handle for the image
			Graphics2D g3 = outImg.createGraphics();
			
			// draw on the main image first
			g3.drawImage(camImg3, 40, 10, 340, 340, null);
			
			// Title overlay
			g3.setPaint(Color.GREEN);
			g3.rotate(-Math.PI/2.0);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,14));
			g3.drawString("Leverett Laboratory", -250, 20);			
			g3.setFont(new Font("Times New Roman",Font.PLAIN,10));
			g3.drawString("- Scan-o-rama Camera - ", -240, 30);
			g3.rotate(+Math.PI/2.0);
				
			// Status overlay		
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,12));
			// Can't use full status string, will need to break it into lines
//			g3.drawString(statusText, 30, 390);
			g3.drawString("Status:", 400, 50);
			g3.drawString(now, 400, 100);
			g3.drawString(camDegText, 400, 150);
			g3.drawString("Elev "+camElev, 400, 200);
			g3.drawString(btText, 400, 250);
			g3.drawString(voltText, 400, 300);
			g3.drawString("# "+camFrame, 400, 350);
			
			// Border boxes
			g3.drawRect(5, 5, 30, 350);
			g3.drawRect(35, 5, 350, 350);
			g3.drawRect(385, 5, 90, 350);
			
			g3.dispose();


		}else if (imgType==6){
			// Raw image
			
			// Open a stream and read a still JPG image from the camera
			URL camURL = new URL("http://192.168.2.122/jpg/image.jpg?resolution=640x480&compression=0&clock=0&date=0&text=0");
//			InputStream camIn = camURL.openStream();
//			BufferedInputStream camInBuf = new BufferedInputStream(camIn);
//			outImg = ImageIO.read(camInBuf);
			URLConnection camConnect = camURL.openConnection();
			camConnect.setConnectTimeout(timeout);
			camConnect.setReadTimeout(timeout);
			try{
				InputStream camIn = camConnect.getInputStream();			
				BufferedInputStream camInBuf = new BufferedInputStream(camIn);
				outImg = ImageIO.read(camInBuf);
			}catch(SocketTimeoutException e){
				outImg = new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);
			}						

			
		}else if (imgType==7){
			// For the panorama
			
			// Open a stream and read a still JPG image from the camera
			URL camURL = new URL("http://192.168.2.122/jpg/image.jpg?resolution=640x480&compression=0&clock=0&date=0&text=0");
			URLConnection camConnect = camURL.openConnection();
			camConnect.setConnectTimeout(timeout);
			camConnect.setReadTimeout(timeout);
			try{
				InputStream camIn = camConnect.getInputStream();			
				BufferedInputStream camInBuf = new BufferedInputStream(camIn);
				camImg = ImageIO.read(camInBuf);
			}catch(SocketTimeoutException e){
				camImg = new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);
			}						

			// Try to insert the image onto a larger background canvas
			backImg = new BufferedImage(800,800,camImg.getType());
			Graphics2D g2 = backImg.createGraphics();
			g2.drawImage(camImg,80,160,640,480,null);  
			g2.dispose();
			
			// Rotate the full 800x800, then trim to much smaller central region that always has image available
			BufferedImageOp op = new AffineTransformOp(AffineTransform.getRotateInstance(-theta, 400, 400),null);
			camImg2 = op.filter(backImg,null);
			outImg = camImg2.getSubimage(230, 230, 340, 340);		// Largest square?
//			outImg = camImg2.getSubimage(320, 230, 160, 340);		// just wide enough to butt up images at 1/24 rotation
						
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
			g3.drawString("- Scan-o-rama Camera - ", 35, 35);
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
