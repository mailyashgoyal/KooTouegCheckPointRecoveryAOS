import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import com.sun.org.apache.xerces.internal.dom.DOMNormalizer;

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
		case BROADCAST:
			handleBroadcastMessage(id, m);
			break;
		case TIMESTAMP_REPLY : 
			handleTimeStampReply(id, m);
			break;
		case GLOBAL_TIME_STAMP :
			handleGlobalTimeStampReply(id, m);
			break;
		default:
			System.out.println("INFO : Unknown message type");
		}
	}
	
	public void handleBroadcastMessage(int id, Message m){
		
		// a broadcast message has arrived from remote node with nodeId = id
		
		//calculate new time stamp
		int replyTimeStamp = thisPeer.pendingMessageQueue.getMaxTimeStamp() + 1;
		//add the message to the peers pending message queue and send a reply
		thisPeer.pendingMessageQueue.addPendingMessage(m,id, replyTimeStamp, 1);
		//send best time stamp reply
	    Message reply = new Message(MessageType.TIMESTAMP_REPLY,thisPeer.getNodeId(), thisPeer.getDomain(), replyTimeStamp);
	    reply.setMessageId(m.getMessageId());
	    SctpChannel channel = thisPeer.getChannels().get(id).channel;
	    int streamNumber = thisPeer.getChannels().get(id).streamNumber;
	    try {
			sendMessage(channel,reply.toString(),streamNumber);
		} catch (IOException e) {
			System.out.println("Error : IOException while sending reply with timestamp");
			e.printStackTrace();
		}
	}
	
	public void handleTimeStampReply(int id, Message m){
		
		//read replyTimeStamp;
		int replyTimeStamp = m.getLogicalTimeStamp();
		
		//call add time stamp reply message of this peer's pending queue
		thisPeer.pendingMessageQueue.addTimeStampReply(m, id);
		
		
		
		//begin logic to handle messages that turned from pending to ready.
		//check if after this reply the message has become READY
		//if so send global time stamp reply
		for(PendingMessage pendingMessage : thisPeer.pendingMessageQueue.queue){
			if(pendingMessage.getStatus().equals(PendingMessageStatus.READY)){
				
				// send a global time stamp reply message
				int globalTimeStamp = pendingMessage.currentProposedTimeStamp;
				pendingMessage.getMessage().setLabel(globalTimeStamp);
				
				// construct global time stamp reply message
				Message globalTimeStampMessage =
						new Message(MessageType.GLOBAL_TIME_STAMP, 
								thisPeer.getNodeId(), 
								thisPeer.getDomain(), 
								globalTimeStamp);
				
				//broadcast it to all the other nodes.
				globalTimeStampMessage.setMessageId(pendingMessage.getMessageId());
				for(int k : thisPeer.channels.keySet()){
					Stream s = thisPeer.getChannels().get(k);
					SctpChannel channel = s.channel;
					int streamNumber = s.streamNumber;
					try {
						sendMessage(channel, globalTimeStampMessage.toString(),
								streamNumber);
						System.out
								.println("Sending GLOBAL_TIME_STAMP for message id : "
										+ globalTimeStampMessage.messageId
										+ " to Node : " + k);
					} catch (IOException e) {
						System.out
								.println("Error : IOException while sending global time stamp reply with timestamp");
						e.printStackTrace();
					}
				}
				
			    //mark this message for which you sent global time stamp reply
				//as DELIVERABLE
			    pendingMessage.setStatus(PendingMessageStatus.DELIVERABLE);
			    
			    //deliver this message.
			    thisPeer.pendingMessageQueue.deliverMessages();
			}
			//end logic for messages that turned from pending to ready
		}
	}
	
	public void handleGlobalTimeStampReply(int id, Message m){
		
		//read message id for which this reply was sent
		int msgId = m.getMessageId();
		System.out.println("INFO : Global time stamp for messageId " + msgId + " received.");
		
		// update this message
		thisPeer.pendingMessageQueue.addTimeStampReply(m, id);
		
		//message became deliverable.
		thisPeer.pendingMessageQueue.deliverMessages();
		
		
		
		
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