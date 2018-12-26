package levlab.servlet.one;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
//import java.io.BufferedReader;
//import java.io.DataInputStream;
//import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
//import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
//import java.net.SocketTimeoutException;
import java.net.URL;
//import java.net.URLConnection;
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
 * Servlet implementation class AutoScan1
 */
public class AutoScan1 extends HttpServlet implements Runnable{
	private static final long serialVersionUID = 1L;
	// Get (and keep) a reference to the one instance of
	// Shared object
	Shared shared = Shared.getInstance();
	Bot bot = shared.bot5;
	
    // Setup variables here
    Thread status;		// this thread will read status continuously
    int scanState = 0;	// 
    
    int imageMax = 256;
    int imageTotal = 7;
    int imageIndex = 0;
    BufferedImage imageArray[] = new BufferedImage[imageMax];
    BufferedImage imageStitch = null;
    
    int positionArray[] = new int[imageMax];
    int commandArray[] = new int[imageMax];
    int commandZero = -3*105;								// position of first image
    int commandStep = 105;								// position increment per step    

    long timeArray[] = new long[imageMax];
	URL camURL;

	String debugText = "init";
	String debugText2 = "init2";
	String debugText3 = "init3";
	String botURL = "";
	String scanURL = "";
   
	String botPreURL = "http://localhost/TestOne/Bot5cmd?";
	String botPostURL = "&data2=0&data3=0&timestamp=";

	String scanPreURL = "http://localhost/TestOne/ScanCam1?";
	String scanPostURL = "&timestamp=";
	
	BufferedImage bin = null;
	Image logo32 = null;
	Image logo64 = null;

	// parameters that may be passed in on a GET or XHR request
	int type = 0;
	int index = 0;
	float dx = (float)1.9;
	float dy = (float)0.06;
	int offsetx, offsety, offseti, offsetj;
	
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AutoScan1() {
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
    	status = new Thread(this);
    	status.start();
    }
    
    public void run(){
    	
    	int poscount;
    	int poserr;
    	
    	while(true){
    		if(scanState==0){		// idle, do nothing
        		// Delay before checking scanState again
    	    	try{
    				Thread.sleep(100);
    			}catch(InterruptedException e){
    				// do nothing with interrupted sleep
    			}
    		}
    		if(scanState==1){		// setup variables prior to scan
    			imageIndex=0;
    			for(int i=0;i<imageTotal;i++){
    				commandArray[i]=commandZero+i*commandStep;
    				positionArray[i]=0;
    				timeArray[i]=0;
    			}
    			scanState=2;
    		}
    		if(scanState==2){		// scan is in progress
    			
    			// Command the mirror to the new position
    			try {
    				botURL = botPreURL+"command=210&data1="+commandArray[imageIndex]+botPostURL+new Date().getTime();
					debugText = debugText+" "+botURL;
					URL url = new URL(botURL);
					url.openStream();				
				} catch (MalformedURLException e) {
    				debugText2 = ""+e;
				} catch (IOException e) {
    				debugText3 = ""+e;
				}

				// Delay with a timeout
    			// 10 mSec delay * up to 100 tries = 1 second for command to work
				poserr = 100;
				poscount = 0;
				while( (poserr>5) && (poscount<100)){
					poscount++;
	    			poserr=Math.abs(bot.getData("PosC") - commandArray[imageIndex]);
					try{
	    				Thread.sleep(10);		
	    			}catch(InterruptedException e){	    			
	    			}
					
				}

				// If position error is still greater than 5 degrees, try repeating the procedure
				if(poserr>5){
	    			// Command the mirror to the new position
	    			try {
						URL url = new URL(botPreURL+"command=210&data1="+commandArray[imageIndex]+botPostURL+new Date().getTime());
						url.openStream();				
					} catch (MalformedURLException e) {
	    				debugText = "Mal2";
					} catch (IOException e) {
	    				debugText = "IO2";
					}

					// Delay with a timeout
	    			// 10 mSec delay * up to 100 tries = 1 second for command to work
					poserr = 100;
					poscount = 0;
					while( (poserr>5) && (poscount<100)){
						poscount++;
		    			poserr=Math.abs(bot.getData("PosC") - commandArray[imageIndex]);
						try{
		    				Thread.sleep(10);		
		    			}catch(InterruptedException e){	    			
		    			}						
					}
				}

				// read the camera position from the shared data
    			positionArray[imageIndex]=bot.getData("PosC");
    			timeArray[imageIndex]=new Date().getTime();
    			
    			// Get the next image using the ScanCam1 servlet
    			// image type 7 is 340x340 largest square that doesn't get clip by
    			// the rotation, no overlays.
    			try {
					URL scanURL = new URL(scanPreURL+"type=7"+scanPostURL+new Date().getTime());
					InputStream scanIn = scanURL.openStream();
					debugText2=scanURL.getPath()+" "+scanURL.getHost();
    				BufferedInputStream scanInBuf = new BufferedInputStream(scanIn);
    				bin = ImageIO.read(scanInBuf);
    				imageArray[imageIndex]=bin;
    			}catch(MalformedURLException e3){
    				//debugText2 = "Mal2: "+debugText2;
    			} catch (IOException e) {
    				//debugText2 = "IO2: "+debugText2;
				}
    			
    			// Increment the imageIndex and check for done
    			imageIndex++;
    			
    			if(imageIndex==imageTotal){
    				// done with scan, send mirror back to zero position
    				scanState = 3;
	    			try {
						URL url = new URL(botPreURL+"command=210&data1=0"+botPostURL+new Date().getTime());
						url.openStream();				
						//debugText3=url.getPath()+" "+url.getHost();
					} catch (MalformedURLException e) {
	    				//debugText3 = "Mal3: "+debugText3;
					} catch (IOException e) {
	    				//debugText3 = "IO3: "+debugText3;
					}
    			}    			
	    	}
    		if(scanState==3){
    			// assemble the panorama with default settings
//    			dx = (float)1.9;
//    			dy = (float)0.06;    			
    			makePano();    	
    			scanState = 0;
    		}
    	}
    }
    
	//-----------------------------------------------------------------
    void makePano(){
    	
    	// Figure out the background canvas size
    	int i = 0;
    	int titleHigh = 32;
		int imageHigh = imageArray[0].getHeight();
		int imageWide = imageArray[0].getWidth();
		int panoHigh = (int)( (positionArray[imageTotal-1]-positionArray[0]) * dy) + imageHigh + titleHigh;
		int panoWide = (int)( (positionArray[imageTotal-1]-positionArray[0]) * dx) + imageWide;
		if(panoWide > (int)(2520 * dx)) panoWide = (int)(2520 * dx); 
	
		// Make the canvas, add graphic handle
		BufferedImage backImg = new BufferedImage(panoWide,panoHigh,imageArray[0].getType());
		Graphics2D g2 = backImg.createGraphics();
					
		// calculate offsets and place images on the background canvas
		for(i=0;i<imageTotal;i++){
			offseti = positionArray[i]-positionArray[0]; 			// total position difference
			offsetj = offseti % 2520;								// position difference modulo one revolution
			offsetx = (int)(offsetj*dx);							// x offset depends on modulo index
			offsety = panoHigh - imageHigh - (int)( (float)offseti * dy);		// y offset depends of total index				
			g2.drawImage(imageArray[i], offsetx, offsety, imageWide, imageHigh, null);
			if( (offsetx+imageWide)>panoWide ){
				// This image has data that goes over the right edge so repeat it on the left edge at the height
				offsetx -= panoWide;
				g2.drawImage(imageArray[i], offsetx, offsety, imageWide, imageHigh, null);
			}
		}
		
		// Title overlay
		g2.setPaint(Color.BLACK);
		g2.fillRect(0, 0, panoWide, titleHigh);
		g2.setPaint(Color.GREEN);
		g2.setFont(new Font("Times New Roman",Font.PLAIN,18));
		g2.drawString("Leverett Laboratory - Scan-o-rama Camera - "+imageTotal+" images with dx = "
				       +dx+" and dy = "+dy+"   Images are "+imageWide+"x"+imageHigh
				       +", Panorama is "+panoWide+"x"+panoHigh, 40, 22);
				
		// Add the Lab Logo
		if(logo32!=null)g2.drawImage(logo32, 0, 0, 32, 32, null);
		g2.dispose();
		imageStitch = backImg;
    }
    
	//-----------------------------------------------------------------

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		String msg = "AutoScan Init";
		String arg = null;
		//int i;
		
		// Parse all the possible arguments, leave defaults if no value is passed
		arg=req.getParameter("type");
		if(arg!=null){
			type = Integer.parseInt(arg);
		}
		arg=req.getParameter("index");
		if(arg!=null){
			index = Integer.parseInt(arg);
		}
		arg=req.getParameter("dx");
		if(arg!=null){
			dx = Float.parseFloat(arg);
		}
		arg=req.getParameter("dy");
		if(arg!=null){
			dy = Float.parseFloat(arg);
		}
		arg=req.getParameter("num");
		if(arg!=null){
			imageTotal = Integer.parseInt(arg);
		}
		arg=req.getParameter("zero");
		if(arg!=null){
			commandZero = Integer.parseInt(arg);
		}
		arg=req.getParameter("step");
		if(arg!=null){
			commandStep = Integer.parseInt(arg);
		}

			
    	if(type==0){
    		scanState=0;
	    	// Generate a text response
			res.setContentType("text/html");
			PrintWriter outWriter = res.getWriter();
			outWriter.println("AutoScan Halted and...");	
			if(imageArray==null) {
				outWriter.println("imageArray is null");
			}
			if(positionArray==null) {
				outWriter.println("positionArray is null");
			}
			if(commandArray==null) {
				outWriter.println("commandArray is null");
			}
			if(timeArray==null) {
				outWriter.println("timeArray is null");
			}
    	}else if (type==1){
    	    scanState=1;
	    	// Generate a text response
    		msg = "AutoScan type "+type+", num="+imageTotal+", zero="+commandZero+", step="+commandStep;
			res.setContentType("text/html");
			PrintWriter outWriter = res.getWriter();
			outWriter.println(msg);					
    	}else if (type==2){
			// send the image requested by index
    		if(index<0)index = 0;
    		if(index>(imageTotal-1)) index=(imageTotal-1);
			res.setContentType("image/jpeg");
			ServletOutputStream outStream = res.getOutputStream();
			ImageIO.write(imageArray[index], "JPEG", outStream);		
		}else if (type==3){
	    	// Generate an image response that tells progress

			// Make the canvas, add graphic handle
			BufferedImage statusImg = new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = statusImg.createGraphics();

			// Try to insert the image onto a larger background canvas
			int tempInt = imageIndex-1;
			if(tempInt<0)tempInt=0;
			g2.drawImage(imageArray[tempInt],70,70,340,340,null);  

			// get a time/date string 
			DateFormat plain = DateFormat.getTimeInstance(DateFormat.DEFAULT);
			String now = plain.format(new Date());
			
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

			int statX = 520;
			int statY = 140;
			
			// Title overlay
			g2.setPaint(Color.GREEN);
			g2.setFont(new Font("Times New Roman",Font.PLAIN,20));
			g2.drawString("Leverett", statX,statY);
			g2.drawString("Laboratory", statX-10,statY+20);			
			g2.setFont(new Font("Times New Roman",Font.PLAIN,16));
			g2.drawString("Scan-o-rama", statX, statY+45);
			g2.drawString("Camera", statX+15, statY+65);
							
			// Status overlay		
			g2.setPaint(Color.GREEN);
			g2.setFont(new Font("Times New Roman",Font.PLAIN,16));
			// Can't use full status string, will need to break it into lines
			g2.drawString(now, statX, statY+120);
			g2.drawString(camDegText, statX, statY+140);
			g2.drawString("Elev "+camElev, statX, statY+160);
			
			// special colors for BT string
			if(bot.btState==Bot.BT_NOT) g2.setPaint(Color.YELLOW);
			if(bot.btState==Bot.BT_ERROR) g2.setPaint(Color.RED);
			g2.drawString(btText, statX, statY+180);
			g2.setPaint(Color.GREEN);
			
			g2.drawString(voltText, statX, statY+200);

			g2.setPaint(Color.YELLOW);
			if(scanState==0){
				g2.drawString("Scan idle", statX, statY+220);
			}
			if(scanState==1){
				g2.drawString("Scan init", statX, statY+220);				
			}
			if(scanState==2){
				g2.drawString("Scan "+imageIndex+" of "+imageTotal, statX, statY+220);				
			}
			if(scanState==3){
				g2.drawString("Scan done", statX, statY+220);				
			}

			// Try to get the Lab Logo
			if(logo64!=null)g2.drawImage(logo64, 528, 20, 64, 64, null);
	        									
			g2.dispose();
						
			// output the image
			res.setContentType("image/jpeg");
			ServletOutputStream outStream = res.getOutputStream();
			ImageIO.write(statusImg, "JPEG", outStream);		
		}else if (type==4){
			// Tile the images into a single mosaic send out an html page with just the image set
			// to src=type5
			makePano();			
			
	    	// Generate an html response that includes references to this page for the images
			res.setContentType("text/html");
			PrintWriter outWriter = res.getWriter();
			outWriter.println("<html><head><title>Scan-o-rama Camera</title></head><body>" );
		
			// include the final image which is fetched using type=5
			outWriter.println("<img src=\"AutoScan1?type=5\" alt=\"Stitched Image\" id=\"imgStitch\"></img>");
			outWriter.println("</body></html>");
			
		}else if (type==5){
			// send the stitched image
			res.setContentType("image/jpeg");
			ServletOutputStream outStream = res.getOutputStream();
			if(imageStitch!=null){
				ImageIO.write(imageStitch, "JPEG", outStream);
			}else{
				// no pano available, send error frame instead
				BufferedImage backImg = new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);
				Graphics2D g3 = backImg.createGraphics();
				// Title overlay
				g3.setPaint(Color.GREEN);
				g3.setFont(new Font("Times New Roman",Font.PLAIN,18));
				g3.drawString("Leverett Laboratory", 20, 20);
				g3.setFont(new Font("Times New Roman",Font.PLAIN,12));
				g3.drawString("- AutoScan1 Servlet - ", 35, 35);
				g3.setFont(new Font("Times New Roman",Font.PLAIN,14));
				g3.drawString("No panorama available.", 30, 160);
				g3.setFont(new Font("Times New Roman",Font.PLAIN,24));
				g3.dispose();
				ImageIO.write(backImg, "JPEG", outStream);
			}
		}else{
			// unknown type so return an error image
			BufferedImage backImg = new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);
			Graphics2D g3 = backImg.createGraphics();

			// Title overlay
			g3.setPaint(Color.GREEN);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,18));
			g3.drawString("Leverett Laboratory", 20, 20);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,12));
			g3.drawString("- AutoScan1 Servlet - ", 35, 35);
			//g3.setFont(new Font("Times New Roman",Font.PLAIN,14));
			//g3.drawString("Image Type undefined.", 30, 160);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,10));
			g3.drawString(debugText, 30, 130);
			g3.drawString(debugText2, 30, 150);
			g3.drawString(debugText3, 30, 170);
			g3.setFont(new Font("Times New Roman",Font.PLAIN,24));
			g3.drawString("Error: "+type, 50, 100);
	
			g3.dispose();

			res.setContentType("image/jpeg");
			ServletOutputStream outStream = res.getOutputStream();
			ImageIO.write(backImg, "JPEG", outStream);		
			
		}
		res.flushBuffer();
	
	}	
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
