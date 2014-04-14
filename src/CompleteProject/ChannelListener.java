import java.io.IOException;
import java.nio.ByteBuffer;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

public class ChannelListener implements Runnable {

	int remoteId;
	SctpChannel channel;
	int streamNumber;
	Listener parent;
	
	public ChannelListener(int remoteId, SctpChannel channel,int strNo, Listener parent) {
		super();
		this.remoteId = remoteId;
		this.channel = channel;
		this.parent = parent;
		this.streamNumber = strNo;
	}

	@Override
	public void run() {
		while(true){
			
			Message m = null;
			
			try {
				
				//wait till a message is received on this channel
				System.out.println("INFO: waiting to receive from "+remoteId);
				m = new Message(RecieveMessage(channel,streamNumber));
				//when a message does arrive send it to Listener.
				parent.handleNewMessage(remoteId, m);
				
			} catch (IOException e) {
				
				e.printStackTrace();
			}
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
	
	public String RecieveMessage(SctpChannel socket,int strNo) throws IOException
	{
		System.out.println("INFO : Peer specific Channel Listener.run : waiting to receive.");
		ByteBuffer buffer = ByteBuffer.allocate(64000);
		buffer.clear();
		@SuppressWarnings("unused")
		MessageInfo messageInfo = socket.receive(buffer , null, null); 
		System.out.println("INFO : ChannelListener : Message from Client : "+ byteToString(buffer));
		
		return byteToString(buffer);
	}
}
