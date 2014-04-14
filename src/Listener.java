import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yash Goyal
 */
public class Listener implements Runnable {

	Peer thisPeer;
	boolean keepAlive;

	Listener(Peer parent)
	{
		thisPeer = parent;
		keepAlive = true;
	}
	/**
	 * @param args the command line arguments
	 */
	public static int clientsSeen = 0;
	public void runListener() throws IOException
	{
		SctpServerChannel serverSock;
		// Binding Object into Server port

		serverSock = SctpServerChannel.open();
		InetSocketAddress serverAddr = new InetSocketAddress(thisPeer.getPort());
		serverSock.bind(serverAddr);

		System.out.println("INFO : Server : Bound port: "+ serverAddr.getPort());
		System.out.println("INFO : Server : Waiting for connection ...");

		//Waiting for Connection from client
		boolean Listening = true;

		// Phase 1: Accept channel requests
		int maxIncomingChannels = thisPeer.GetIncomingList().size();//3. will not be hard coded, this should be length of incoming set
		int numIncomingChannels = 0;
		System.out.println("INFO : Listener : waiting for " + maxIncomingChannels + " channels to connect ");
		while (Listening) {
			// Receive a connection from client and accept it
			if (numIncomingChannels < maxIncomingChannels) {
				SctpChannel channel = serverSock.accept();
				int id = -1;

				// resolve id from a message from the talker on the other side.
				String initMsg = RecieveMessage(channel);
				System.out.println("INFO : Client id =" + initMsg);
				id = Integer.parseInt(initMsg.split(" ")[0]);

				// add this channel to peers channel map.
				thisPeer.getChannels().put(id, new Stream(channel,0));
				numIncomingChannels++;

				// prompt
				System.out.println("INFO : New channel to peer with id: " + id);
				System.out.println();

			} else {
				System.out.println("INFO : Phase 1 : accept incoming hosts channels complete.");
				break;
			}
		}
		//End Phase 1.


		//wait for talker
		System.out.println("INFO : Listener waiting for talker to finish adding channels");
		while (!thisPeer.isTalkerDoneEstablishingConnections()) {

			try {
				Thread.sleep(1000);
				System.out.println("thisPeer.isTalkerDoneEstablishingConnections() = " + thisPeer.isTalkerDoneEstablishingConnections());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		System.out.println("INFO : Listener has detected Talker's phase 1 is complete. " +
				"\nINFO : Listener starting Phase 2.");
		System.out.println("-------------------------------ChannelMap------------------------");
		for(int id : thisPeer.channels.keySet())
		{
			System.out.println("Remote Node Id : "+ id + " " + thisPeer.channels.get(id));
		}
		System.out.println("-------------------------------ChannelMap------------------------");
		//Phase 2: Start channel specific lsitener
		// for each channel start channel listener.
		for (int remotePeerId : thisPeer.getChannels().keySet()) {
			SctpChannel channel = thisPeer.getChannels().get(remotePeerId).channel;
			int streamNumber = thisPeer.getChannels().get(remotePeerId).streamNumber;
			ChannelListener cl = new ChannelListener(remotePeerId, channel,streamNumber,this);
			new Thread(cl).start();
		}
		thisPeer.setListenerStartedListeningToChannels(true);

		//start cli thread
		new Thread(thisPeer.cli).start();

		//stay alive do nothing. This Listener thread is a master thread
		//for all channel listeners. It also handles the messages received
		//by any of the channel listeners.
		while(keepAlive){
			//do nothing
		}
	}


	private static String byteToString(ByteBuffer byteBuffer)
	{
		byteBuffer.position(0);
		byteBuffer.limit(1040);
		byte[] bufArr = new byte[byteBuffer.remaining()];
		byteBuffer.get(bufArr);
		return new String(bufArr);
	}

	public String RecieveMessage(SctpChannel socket) throws IOException
	{
		System.out.println("INFO : Peer specific listener.run : waiting to receive on stream number = thisPeer.nodeId.");
		ByteBuffer buffer = ByteBuffer.allocate(64000);
		buffer.clear();
		@SuppressWarnings("unused")
		MessageInfo messageInfo = socket.receive(buffer , null, null); 
		System.out.println("INFO : new message in channel. on stream number " + messageInfo.streamNumber());
		System.out.println("INFO : Listener : Message from Client : "+ byteToString(buffer));
		return byteToString(buffer).trim();
	}

	//TODO : 2. handles a new message sent to this peer from node with nodeId = id
	public void handleNewMessage(int id,Message m){	

		//Print message
		System.out.println("INFO : Message received from node id " +  id + " : " + m);

		// decipher message type and handle message accordingly.
		MessageType type = m.getType();
		switch(type){
		case APPLICATION:
			handleApplicationMessage(id, m);
			break;
		case CHECKPOINT : 
			handleCheckpoint(id, m);
			break;
		case CHECKPOINT_REPLY :
			handleCheckpointReply(id, m);
			break;
		case RECOVERY:
			handleRecoveryMessage(id, m);
			break;
		case CONTROL:
			handleControlMessage(id, m);
			break;
		case CHECKPOINT_CONFIRM : 
			handleCheckpointConfirmation(id, m);
			break;
		case CHECKPOINT_REJECT : 
			handleCheckpointReject(id, m);
			break;
		case ROLLBACK : 
			handleRollbackMessage(id, m);
			break;
		case ROLLBACK_REPLY : 
			handleRollbackReply(id, m);
			break;
		case ROLLBACK_REJECT : 
			handleRollbackReject(id, m);
			break;
		case ROLLBACK_CONFIRM : 
			handleRollbackConfirm(id, m);
			break;
		default:
			System.out.println("INFO : Unknown message type");
		}
	}


	private void handleRollbackConfirm(int id, Message m) {
		System.out.println("INFO : Rollback Confirm message handler for node "+ id + ": Rollback confirm Message" + m);
		System.out.println("BEFORE ROLL BACK : now.label = " + thisPeer.label + " APPMESSAGE COUNT " + thisPeer.getAppMessagesReceived().size());
		thisPeer.getRecoveryManager().confirmRollBack();
		System.out.println("ROLLED BACK : now.label = " + thisPeer.label + " APPMESSAGE COUNT " + thisPeer.getAppMessagesReceived().size());
	}


	private void handleRollbackReject(int id, Message m) {
		System.out.println("INFO : rollback reject received from node " + id + " MSG : " + m );
		thisPeer.getRecoveryManager().recordRollBackReject();
	}


	private void handleRollbackReply(int id, Message m) {
		System.out.println("INFO : rollback reply received from node " + id + " MSG : " + m );
		thisPeer.getRecoveryManager().recordRollBackReply(id,m);		
	}


	private void handleRollbackMessage(int id, Message m) {
		
		//Check if already checkpoint is not in progress
		if(thisPeer.getCheckPointTaker().takingCheckpoint == false)
		{
			thisPeer.getRecoveryManager().initiateRecoveryIfNeeded(m.getRollBackSource(), id, m);
		}
		else
		{
			// reject the rollback request 
			System.out.println("INFO : checkpoint rejected");
			Stream s = thisPeer.getChannels().get(id);
			Message reject = new Message(MessageType.ROLLBACK_REJECT, thisPeer.getNodeId(), thisPeer.getDomain(), 0);
			try {
				sendMessage(s.channel, reject.toString(), s.streamNumber);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	private void handleCheckpointReject(int id, Message m) {
		thisPeer.getCheckPointTaker().recordCheckpointReject();
	}


	private void handleApplicationMessage(int id, Message m) {
		System.out.println("INFO : Listener Phase 2 : app msg handler for node "+ id + ": App Message" + m);
		thisPeer.getAppMessagesReceived().add(m);
		thisPeer.LLR.put(m.nodeId, m.getLabel());
		System.out.println("LLR: " + thisPeer.LLR);
	}


	private void handleCheckpoint(int id, Message m) {

		//Check if there is no rollback message already in progress
		if(thisPeer.getRecoveryManager().rollingBack == false)
		{
			System.out.println("INFO : Listener Phase 2 : Listerner Acting as message handler for node "+ id + ": Checkpoint Message" + m);
			boolean decision = thisPeer.getCheckPointTaker().takeCheckPointIfNeeded(m);

			if(decision == false && thisPeer.getCheckPointTaker().checkpointFailed)
			{
				System.out.println("INFO : LISTENER :Checkpoint reject sent to "+ id);
			}
			else if(decision == false && !thisPeer.getCheckPointTaker().checkpointFailed)
			{
				System.out.println("INFO : LISTENER :Checkpoint not needed. Reply sent to "+ id);
			}
			else if(decision == true){
				System.out.println("INFO : LISTENER :Checkpoint taken. Reply sent to "+ id);
			}
		}
		else
		{
			// send reject 
			System.out.println("INFO : checkpoint rejected");
			Stream s = thisPeer.getChannels().get(id);
			Message reject = new Message(MessageType.CHECKPOINT_REJECT, thisPeer.getNodeId(), thisPeer.getDomain(), 0);
			try {
				sendMessage(s.channel, reject.toString(), s.streamNumber);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	private void handleCheckpointReply(int id, Message m) {
		System.out.println("INFO : Listener Phase 2 : Listerner Acting as message handler for node "+ id + ": CheckPoint Reply Message" + m);
		thisPeer.getCheckPointTaker().recordCheckpointReply();
	}

	private void handleCheckpointConfirmation(int id, Message m) {
		System.out.println("INFO : Listener Phase 2 : Listerner Acting as message handler for node "+ id + ": CheckPoint confirm Message" + m);
		thisPeer.getCheckPointTaker().confirmCheckPoint(m);
	}


	private void handleRecoveryMessage(int id, Message m) {
		System.out.println("INFO : Listener Phase 2 : Listerner Acting as message handler for node "+ id + ": Recovery Message" + m);
	}


	private void handleControlMessage(int id, Message m) {
		System.out.println("INFO : Listener Phase 2 : Listerner Acting as message handler for node "+ id + ": Control Message" + m);
	}


	private  void sendMessage(SctpChannel socket, String Message,int strNo) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(64000);

		BufferedReader is = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Client : Inside : Thread send mesaage");
		while(Message != null)
		{
			//Reset a pointer to point to the start of buffer 
			buffer.position(0);
			buffer.clear();
			buffer.put(Message.getBytes());
			buffer.flip();
			try {
				//System.out.println("Buffer is : "+ buffer.toString());
				//Send a message in the channel 
				MessageInfo messageInfo = MessageInfo.createOutgoing(null,strNo);
				//System.out.println("Message from Server : "+ messageInfo);
				socket.send(buffer, messageInfo);
				System.out.println("Client : Inside : Send finished");
				return;
			} catch (IOException ex) {
				Logger.getLogger(Listener.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}


	public void run()
	{
		try {
			new Listener(thisPeer).runListener();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}