package levlab.servlet.one;

public class Shared {

	// Protect constructor so no other class can call it
	private Shared() {
	}
	
	// Create only 1 instance, save to a private static var
	private static Shared instance = new Shared();
	
	// Make the static instance available publicly thru method
	public static Shared getInstance() { return instance; }
	    
    // Create instances of Bot object for all the rovers
    public Bot bot7 = new Bot("NXT5", "00:16:53:18:DF:2D");
    
    public Bot bot5 = new Bot("NXT3", "00:16:53:0F:5B:F2");

    public Bot bot9 = new Bot("NXT2", "00:16:53:07:6A:FE");

    public Bot bot8 = new Bot("NXT4", "00:16:53:18:9B:E1");
    
    

}
