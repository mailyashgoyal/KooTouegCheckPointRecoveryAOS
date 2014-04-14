import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

public class Peer
{
	public HashMap<Integer,Stream> channels;
	Message genericMessage;
	private ArrayList<Message> appMessagesReceived;
	boolean freezeAppMessages = false;
	
	
	//labels start from 1
	int label = 1;
	
	HashMap <Integer,Integer> FLS = new HashMap<Integer, Integer>();
	HashMap <Integer,Integer> LLS = new HashMap<Integer, Integer>();
	HashMap <Integer,Integer> LLR = new HashMap<Integer, Integer>();
	
	public boolean isFreezeAppMessages() {
		return freezeAppMessages;
	}
	public void setFreezeAppMessages(boolean freezeAppMessages) {
		System.out.println("INFO: Freezing/Unfreezing App Messeage sending : " + freezeAppMessages);
		this.freezeAppMessages = freezeAppMessages;
	}

	public int getNextLabel()
	{
		return label++;
	}
	public void updateLLR(int id, int label)
	{
		LLR.put(id, label);
	}
	public void updateLLS(int id, int label)
	{
		LLS.put(id, label);
	}
	public void updateFLS(int id, int label)
	{
		FLS.put(id, label);
	}
	public int getLLR(int id)
	{
		return LLR.get(id);
	}
	public int getLLS(int id)
	{
		return LLS.get(id);
	}
	public int getFLS(int id)
	{
		return FLS.get(id);
	}
	public HashMap<Integer, Message> timeStampToMessageBuffer;
	
	boolean talkerDoneEstablishingConnections;
	boolean listenerStartedListeningToChannels;
	ArrayList<RemotePeer> incomingList = new ArrayList<RemotePeer>();
	ArrayList<RemotePeer> outgoingList = new ArrayList<RemotePeer>(); 
	ArrayList<RemotePeer> allPeers  = new ArrayList<RemotePeer>();
	
	HashMap<Integer,Integer> outputChannelIdentifier = new HashMap<Integer, Integer>();
	
	PendingMessageQueue pendingMessageQueue;
	ArrayList<PendingMessage> deliveredMessageList;
	
	int MAX_MSG_COUNT = 5;
	
	
	Listener l;
	Talker t;
	CheckpointTaker checkPointTaker;
	RecoveryManager recoveryManager;
	CLIThread cli;
	private String domain;
	private int port;
	private int nodeId;
	
	public RecoveryManager getRecoveryManager() {
		return recoveryManager;
	}
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
		genMsg = new Message(MessageType.APPLICATION, this.nodeId, this.domain,getMaxLabel());
		return genMsg;
	} 
	
	
	private int getMaxLabel()
	{
		return label++;
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
	
	
	
	
	
	public CheckpointTaker getCheckPointTaker() {
		return checkPointTaker;
	}
	public void setCheckPointTaker(CheckpointTaker checkPointTaker) {
		this.checkPointTaker = checkPointTaker;
	}
	public static void main(String[] args) throws IOException 
	{
		System.out.println("AOS Project4");
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
	
	
	public ArrayList<Message> getAppMessagesReceived() {
		return appMessagesReceived;
	}
	public void PeerFunction() throws IOException
	{
		//1.read config and populate peer list : allPeers
	    this.allPeers =  readConfig();
	    System.out.println("INFO : Outgoing : " + this.outgoingList.toString());
		System.out.println("INFO : Incoming : " + this.incomingList.toString());	
		//end readConfig()
	   
		//2. Initialize data structures.
		Thread talkerThread, listenerThread;
		channels = new HashMap<Integer,Stream>();
		appMessagesReceived = new ArrayList<Message>();
		timeStampToMessageBuffer = new HashMap<Integer,Message>();
		pendingMessageQueue = new PendingMessageQueue("Output" + this.nodeId + ".txt");
		
		
		//create the Listener and Talker objects
		l = new Listener(this);
		t = new Talker(this); 
		checkPointTaker = new CheckpointTaker(this);
		recoveryManager = new RecoveryManager(this);
		cli = new CLIThread(this);
		
		System.out.println("-----------------STARTING PROTOCOL-----------------");
		
		//initialize the talker and listener threads.
		talkerDoneEstablishingConnections = false;
		listenerStartedListeningToChannels = false;
		talkerThread = new Thread(t);
		listenerThread = new Thread(l);
		
		//start listener thread first
		listenerThread.start();
    
		//add asynchronous user input wait, 
		//to ensure all listeners on all other Peers are ready
		System.out.println("INFO : Listener running. Press enter to continue.");
		new BufferedReader(new InputStreamReader(System.in)).readLine();
	
		
	/*//wait for 15 secods before starting sending messages
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
		}*/
		// then start talker thread.
		
		talkerThread.start();
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<RemotePeer> readConfig() throws IOException
	{		
		FileInputStream fstream =null;
		try {fstream = new FileInputStream("configfile.txt");}
		catch (FileNotFoundException e)
		{e.printStackTrace();}
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine ;

		strLine = br.readLine();
		int numLinks, numNodes;
		numNodes = Integer.parseInt(strLine);
		strLine = br.readLine();
		numLinks = Integer.parseInt(strLine);
		
		String localHostName = InetAddress.getLocalHost().getHostName();
		ArrayList<RemotePeer> allPeers = new ArrayList<RemotePeer>();
		for( int i =0; i < numNodes;i++)
		{
			strLine = br.readLine();
			StringTokenizer st = new StringTokenizer(strLine," ");
			int rId = Integer.parseInt(st.nextToken());
			String rHost = st.nextToken();
			int rPort = Integer.parseInt(st.nextToken());
			
			if(rHost.equalsIgnoreCase(localHostName))
			{
				nodeId = rId;
				domain = rHost;
				port = rPort;
			}
			else
			{
				RemotePeer r = new RemotePeer(rId, rHost, rPort);
				allPeers.add(r);
			}
		}
		for(RemotePeer rp : allPeers)
		{
			if(rp.id < nodeId)
			{
				incomingList.add(rp);
			}
			else if(rp.id > nodeId)
			{
				outgoingList.add(rp);
			}
			else 
			{
				continue;
			}
		}
		
		ArrayList<Integer> neighbours = new ArrayList<Integer>();
		for(int j =0; j < numLinks ;j++)
		{
			strLine = br.readLine();
			StringTokenizer st = new StringTokenizer(strLine,"(,)");
			int id1 = Integer.parseInt(st.nextToken());
			int id2 = Integer.parseInt(st.nextToken());
			if(id1 == nodeId || id2== nodeId )
			{
				neighbours.add(id1 == nodeId? id2:id1);
			}
		}
		ArrayList<RemotePeer> tempIncoming = (ArrayList<RemotePeer>) incomingList.clone();
		for(RemotePeer k : tempIncoming)
		{
			if(!(neighbours.contains(k.id)))
			{
				incomingList.remove(k);
			}
		}
		ArrayList<RemotePeer> tempOutgoing = (ArrayList<RemotePeer>) outgoingList.clone();
		for(RemotePeer k : tempOutgoing)
		{
			if(!(neighbours.contains(k.id)))
			{
				outgoingList.remove(k);
			}
		}
		return allPeers;
	}
	public void setAppMessagesReceived(ArrayList<Message> appMessageList) {
		// TODO Auto-generated method stub
		this.appMessagesReceived = appMessageList;
		
	}
}
