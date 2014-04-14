import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class CLIThread implements Runnable{

	Peer thisPeer;
	public CLIThread(Peer thisPeer) {
		// TODO Auto-generated constructor stub
		this.thisPeer = thisPeer;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(true){
		System.out.println("Press c for checkpoint, r for rollback");
		int o;
		String l;
		try {
			l = new BufferedReader(new InputStreamReader(System.in)).readLine();
			if(l.equalsIgnoreCase("c")){
				thisPeer.getCheckPointTaker().takeCheckPoint(thisPeer.getNodeId());
			}
			else if(l.equalsIgnoreCase("r")){
				Message m = new Message(MessageType.ROLLBACK, thisPeer.getNodeId(), thisPeer.getDomain(), 0);
				thisPeer.getRecoveryManager().initiateRecoveryIfNeeded(thisPeer.getNodeId(), thisPeer.getNodeId(), m);
			}
			else if(l.startsWith("a")){
				int id = Integer.parseInt(l.split("|")[1]);
				Message m = new Message(MessageType.ROLLBACK, thisPeer.getNodeId(), thisPeer.getDomain(), 0);
				//send app message
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
	}

}
