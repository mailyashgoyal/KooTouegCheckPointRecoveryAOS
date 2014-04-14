import java.io.IOException;
import java.nio.ByteBuffer;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

public class ServerThread extends Thread{

	SctpChannel socket;
	ServerThread(SctpChannel socket)
	{
		this.socket = socket;
	}
		
	public String RecieveMessage(SctpChannel socket) throws IOException
	{
		System.out.println("INFO : Peer specific listener.run : waiting to receive.");
		ByteBuffer buffer = ByteBuffer.allocate(64000);
		buffer.clear();
		@SuppressWarnings("unused")
		MessageInfo messageInfo = socket.receive(buffer , null, null); 
		
		System.out.println("INFO : Server : Message from Client : "+ byteToString(buffer));
		
		return byteToString(buffer);
	}
	public void run()
	{		
		while(true)
		{
			try {
				System.out.println("INFO : Peer specific listener.run ");
				RecieveMessage(socket);
				//SendMessage(socket);
				System.out.println("");

			} catch (IOException e1) {
				e1.printStackTrace();
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
}