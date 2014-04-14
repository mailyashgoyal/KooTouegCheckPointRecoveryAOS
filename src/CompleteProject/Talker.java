import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;

public class Talker implements Runnable{

	int port;
	int nodeId;
	Peer thisPeer;
	
	Talker(Peer peerObject){
		thisPeer = peerObject;
		this.port = peerObject.getPort();
		this.nodeId = peerObject.getNodeId();
	}

	public void run()
	{
		try
		{
			ByteBuffer byteBuffer;
			byteBuffer = ByteBuffer.allocate(64000);

			//Phase 1 : Establish Connections.
			for(int i = 0 ; i <thisPeer.outgoingList.size() ; i++)
			{
				SctpChannel ClientChannel;
				System.out.println("INFO : Sending connection request to "  + thisPeer.getOutgoingList().get(i));
				InetSocketAddress serverAddr = 
						new InetSocketAddress(thisPeer.getOutgoingList().get(i).domain,thisPeer.getOutgoingList().get(i).port);
				ClientChannel = SctpChannel.open();
				ClientChannel.connect(serverAddr, 0, 0);
				int id = thisPeer.getOutgoingList().get(i).id;
				thisPeer.getChannels().put(id,new Stream(ClientChannel, 1));
				System.out.println("INFO : Client : Create Connection Successfully......");
				
				//send a init message with self id on this port
				System.out.println("INFO : Talker : Sending my id information to listener on the other side");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				sendMessage(ClientChannel, ""+thisPeer.getNodeId(),1);
				//System.out.println("id message sent");
			}
			
			System.out.println("INFO : Talker is done establishing the connections in Phase 1. Moving to Phase 2");
			thisPeer.setTalkerDoneEstablishingConnections(true);
			
			// end of Phase 1.
			

					
			//start Phase 2 : send messages whenever user presses enter.
			
			//initislize random delay generator
			Random randomDelay = new Random();
			int MAX_WAIT = 5;
			int MIN_WAIT = 2;
			int MAX_MSG_COUNT = 25;
			int messageCounter = 0;
			
			while (true) {
	
				/*
				 * Automated Wait before Send algoorithm
				 */
				//wait for a random period of time before sending
				int  delaySeconds = randomDelay.nextInt(MAX_WAIT-MIN_WAIT) + MIN_WAIT+ 1;
				long delayMillis  = delaySeconds * 1000;
				try {
					Thread.sleep(delayMillis);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				//end automated wait before send algorithm
				
				
				//get generic message from Peer.
				Message m = thisPeer.getGenericMessage();

				//broadcast routine.
				
				//add message to the local queue
				int n = thisPeer.getChannels().size();
				thisPeer.pendingMessageQueue.addPendingMessage(m, thisPeer.getNodeId(), m.getLogicalTimeStamp(), n);
				
				//broadcast message
				//for each Channel try to send this message.
				for (int id : thisPeer.getChannels().keySet()) {
					SctpChannel sctpChannel = thisPeer.getChannels().get(id).channel;
					int streamNumber = thisPeer.getChannels().get(id).streamNumber;
					System.out.println("INFO : Sending broadcast message on sctp channel to node "	+ id + " : " + m);
					sendMessage(sctpChannel, m.toString(),streamNumber);
				}
				
				messageCounter++;
							
				//end broadcast routine
				
				//if MAX_MSG_COUNT messages have been sent then wait indefinitely
				if(messageCounter == MAX_MSG_COUNT){
					System.out.println("INFO : Talker has sent " + MAX_MSG_COUNT + " messages in total." +
							" Will not send any more messages.");
				
				//show message queue if enter is pressed	
				while(true){
					System.out.println("INFO : Talker done in Phase 2. Press enter to display message queue");
					 new BufferedReader(new InputStreamReader(System.in)).readLine();
					 
					    int msgQSize = thisPeer.pendingMessageQueue.deliveredMessages.size();
					    int expMsgQSize = (thisPeer.MAX_MSG_COUNT)*(1+thisPeer.getChannels().size());
					    System.out.println("MsgQueue size : " + msgQSize + " exp size = " + expMsgQSize);
					   	thisPeer.pendingMessageQueue.printAllDeliveredMessages();
					    System.out.println("Message Queue unordered : ");
					    thisPeer.pendingMessageQueue.printAllMessages();
				 }
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	private  void sendMessage(SctpChannel socket, String Message, int streamNumber) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(64000);

		BufferedReader is = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("INFO : Client : Inside : Thread send mesaage");
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
				MessageInfo messageInfo = MessageInfo.createOutgoing(null,streamNumber);
				//System.out.println("Message from Server : "+ messageInfo);
				socket.send(buffer, messageInfo);
				System.out.println("message sent via channel to stream number" + streamNumber);
				return;
			} catch (IOException ex) {
				Logger.getLogger(Listener.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	private  void recieveMessage(SctpChannel clientSock) {
		ByteBuffer recvBuffer = ByteBuffer.allocate(512);
		recvBuffer.clear();
		//Reset a pointer to point to the start of buffer 

		recvBuffer.flip();
		try {
			MessageInfo msgInfo =  clientSock.receive(recvBuffer, null, null);
			String message = byteToString(recvBuffer);
			//System.out.println("Receive Message from Server:");
			System.out.println("INFO : Client : Message from Server : " + message);
			//}
		} catch (IOException ex) {
			Logger.getLogger(Listener.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private  String byteToString(ByteBuffer byteBuffer)
	{
		byteBuffer.position(0);
		byteBuffer.limit(512);
		byte[] bufArr = new byte[byteBuffer.remaining()];
		byteBuffer.get(bufArr);
		return new String(bufArr).trim();
	}

}