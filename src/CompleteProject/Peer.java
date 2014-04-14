import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.HashMap;
import com.sun.nio.sctp.SctpChannel;

public class Peer
{
	public HashMap<Integer,Stream> channels;
	Message genericMessage;
	
	public HashMap<Integer, Message> timeStampToMessageBuffer;
	
	boolean talkerDoneEstablishingConnections;
	boolean listenerStartedListeningToChannels;
	ArrayList<RemotePeer> incomingList = new ArrayList<RemotePeer>();
	ArrayList<RemotePeer> outgoingList = new ArrayList<RemotePeer>(); 
	ArrayList<RemotePeer> allPeers  = new ArrayList<RemotePeer>();
	
	PendingMessageQueue pendingMessageQueue;
	ArrayList<PendingMessage> deliveredMessageList;
	
	int MAX_MSG_COUNT = 25;
	
	
	Listener l;
	Talker t;
	
	private String domain;
	private int port;
	private int nodeId;
	
	public boolean isListenerStartedListeningToChannels() {
		return listenerStartedListeningToChannels;
	}
	public void setListenerStartedListeningToChannels(
			boolean listenerDoneEstablishingConnections) {
		this.listenerStartedListeningToChannels = listenerDoneEstablishingConnections;
	}
	//1. TODO: modify this function to correctly update timestamp.  
	public Message getGenericMessage(){
	  	Message genMsg = null;
		// create broadcast message of current time stamp.
		int broadcastTimeStamp = pendingMessageQueue.getMaxTimeStamp()+1;
		genMsg = new Message(MessageType.BROADCAST, this.nodeId, this.domain,broadcastTimeStamp);
		return genMsg;
	} 
	public boolean isTalkerDoneEstablishingConnections() {
		return talkerDoneEstablishingConnections;
	}
	public void setTalkerDoneEstablishingConnections(
			boolean talkerDoneEstablishingConnections) {
		this.talkerDoneEstablishingConnections = talkerDoneEstablishingConnections;
		System.out.println("talkerDoneEstablishingConnection = " 
		+ talkerDoneEstablishingConnections);
	}
	public ArrayList<RemotePeer> getOutgoingList() {
		return outgoingList;
	}
	public void setOutgoingList(ArrayList<RemotePeer> outgoingList) {
		this.outgoingList = outgoingList;
	}
	public ArrayList<RemotePeer> GetIncomingList() {
		return incomingList;
	}
	public void SetIncomingList(ArrayList<RemotePeer> incomingList) {
		this.incomingList = incomingList;
	}
	public ArrayList<RemotePeer> getAllPeers() {
		return allPeers;
	}
	public void setAllPeers(ArrayList<RemotePeer> allPeers) {
		this.allPeers = allPeers;
	}
	public HashMap<Integer,Stream> getChannels() {
		return channels;
	}
	public void setChannels(HashMap<Integer,Stream> channels) {
		this.channels = channels;
	}
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public int getNodeId() {
		return nodeId;
	}
	public void setnodeId(int nodeId) {
		this.nodeId = nodeId;
	}
	
	
	
	
	
	public static void main(String[] args) throws IOException 
	{
		Peer peer = new Peer();
		peer.PeerFunction();
	}
	
	public void registerMessage(int timeStamp, Message m){
		// add message to buffer
		timeStampToMessageBuffer.put(timeStamp, m);
		
		// check for all messages in the buffer with time stamp < the time stamp of newly arrived message
		// and deliver them.
		for(int time : timeStampToMessageBuffer.keySet()){
			if(time < timeStamp){
				deliverMessage(timeStampToMessageBuffer.get(time));
			}
		}		
	}
	
	public void deliverMessage(Message m){
		//deliver this message to Peer. i.e Print it.
		System.out.println("INFO : Delivered new message :" + m);
	}
	
	
	public void PeerFunction() throws IOException
	{
		//1.read config and populate peer list : allPeers
	    this.allPeers =  readConfig();
		//end readConfig()
	    
	    
	    //2.identify self from the config file.
		this.domain = Inet4Address.getLocalHost().getHostName();
		for(RemotePeer p : this.allPeers){
			if(p.domain.equalsIgnoreCase(this.domain)){
				this.nodeId = p.id;
				this.port = p.port;
			}
		}
		
		//3. Initialize data structures.
		Thread talkerThread, listenerThread;
		channels = new HashMap<Integer,Stream>();
		timeStampToMessageBuffer = new HashMap<Integer,Message>();
		pendingMessageQueue = new PendingMessageQueue("Output" + this.nodeId + ".txt");
		
		
		
		//4.Partition ids from All Peers into incoming and outgoing.
				this.incomingList = new ArrayList<RemotePeer>();
				this.outgoingList = new ArrayList<RemotePeer>();
		for(RemotePeer c : this.allPeers)
		{
			if(c.id == this.nodeId)
			{
				// the node is same as the this
			}
			else if(c.id < this.nodeId)
			{
				this.incomingList.add(c);
			}
			else
			{
				this.outgoingList.add(c);
			}
		}
		
		System.out.println("INFO : Outgoing : " + this.outgoingList.toString());
		System.out.println("INFO : Incoming : " + this.incomingList.toString());	
		//end of Partition logic
		
		//create the Listener and Talker objects
		l = new Listener(this);
		t = new Talker(this); 
		
		System.out.println("-----------------STARTING ABCAST-----------------");
		
		//initialize the talker and listener threads.
		talkerDoneEstablishingConnections = false;
		listenerStartedListeningToChannels = false;
		talkerThread = new Thread(t);
		listenerThread = new Thread(l);
		
		//start listener thread first
		listenerThread.start();
    
	/*removing async wait	
	 
		//add asynchronous user input wait, 
		//to ensure all listeners on all other Peers are ready
		System.out.println("INFO : Listener running. Press enter to continue.");
		new BufferedReader(new InputStreamReader(System.in)).readLine();
	*/
		
	//wait for 15 secods before starting sending messages
	//this will give enough time to other peers to start their listeners
		System.out.println("INFO : Peer : waiting 15 seconds before starting listener");
		int wait = 0;
		while(wait<15){
		try {
			Thread.sleep(1000);
			System.out.println(++wait);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		// then start talker thread.
		
		talkerThread.start();
	}
	
	public ArrayList<RemotePeer> readConfig() throws IOException
	{		
		ArrayList<RemotePeer> array = new ArrayList<RemotePeer>();
		FileInputStream fstream =null;
		try {fstream = new FileInputStream("configfile.txt");}
		catch (FileNotFoundException e)
		{e.printStackTrace();}
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine ;

		strLine = br.readLine();
		while (strLine != null)   
		{
			String ss = null;
			ArrayList<String> string = new ArrayList<String>();
			// get the substring and put in another array
			for (int i =0 ; i <3 ;i ++)
			{
				int spc = 0;
				spc = strLine.indexOf(" ");
				if(spc == -1)
				{
					ss = strLine.substring(0);
					string.add( ss);
				}
				else
				{
					ss = strLine.substring(0,spc);
					string.add( ss);
					strLine = strLine.substring((ss.length()+1));
				}
			}
			RemotePeer rp = 
					new RemotePeer(Integer.parseInt(string.get(0)),string.get(1),Integer.parseInt(string.get(2)));
			array.add(rp);
			strLine = br.readLine();
		}
		br.close();
		fstream.close();
		return array;
	}
}
