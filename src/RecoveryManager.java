import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

public class RecoveryManager implements Runnable {

	Peer parent;
	boolean rollingBack;
	boolean rollBackFailed;
	boolean rollBackDecision;
	int rollBackSource;
	int rollBackParent;
	int rollBackRepliesExpected = 0;
	ArrayList<Integer> nodesThatWillRollBack;

	public RecoveryManager(Peer parent) {
		this.parent = parent;
		nodesThatWillRollBack = new ArrayList<Integer>();
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
		System.out.println("Press enter to rollback");
		try {
			new BufferedReader(new InputStreamReader(System.in)).readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		rollBack();
		
	}

	public void initiateRecoveryIfNeeded(int src, int rollBackParentId,
			Message m) {
		
		
		if(rollingBack && rollBackSource!=m.getRollBackSource() ){
			//cannot take rollback at all. already taking one
			System.out.println("INFO : ReoveyManager already rolling back, cannot roll back now. Sending reject to " + rollBackParentId);
			Stream s = parent.getChannels().get(rollBackParentId);
			Message reject = new Message(MessageType.ROLLBACK_REJECT, parent.getNodeId(), parent.getDomain(), 0);
			sendMessage(s.channel, reject.toString(), s.streamNumber);
			return;
		}
		if(rollingBack && rollBackSource==m.getRollBackSource() ){
			//already rolling back for the same source
			System.out.println("INFO : ReoveyManager already rolling back, cannot roll back now. Sending reject to " + rollBackParentId);
			Stream s = parent.getChannels().get(rollBackParentId);
			Message reply = new Message(MessageType.ROLLBACK_REPLY, parent.getNodeId(), parent.getDomain(), 0);
			reply.setData("true");
			sendMessage(s.channel, reply.toString(), s.streamNumber);
			return;
		}
		
		
		rollBackSource = src;
		rollBackParent = rollBackParentId;
		
		
		//not rolling back, available

		if (parent.getNodeId() == rollBackParent) {
			// must roll back. initiated by self
			System.out.println("INFO : roll back initiated by self");
			rollBackDecision = true;
			rollBack();
		} else {
			// decide based on rollback criterion
			System.out.println("INFO : rollback requested by src:"
					+ rollBackSource + " parent:" + rollBackParent);

			int llr =0;
			if(parent.LLR != null)
			{
				llr = parent.LLR.get(rollBackParentId);
			}
			int lls = m.getLLS();
			if (llr > lls) {
				rollBackDecision = true;
				rollBack();
			} else {
				rollBackDecision = false;
				Message rollbackReply = new Message(MessageType.ROLLBACK_REPLY,parent.getNodeId(),parent.getDomain(),0);
				rollbackReply.setWillRollBack(false);
				Stream s = parent.getChannels().get(rollBackParent);
				sendMessage(s.channel, rollbackReply.toString(), s.streamNumber);
			}
		}
	}
	
	public void rollBack(){
		
		rollingBack = true;
		rollBackFailed = false;
		//send rollback message to all neighbors
		
		int lastCheckPointIndex = parent.getCheckPointTaker().checkPoints.size()-1;
		Checkpoint lastCheckpoint = parent.getCheckPointTaker().checkPoints.get(lastCheckPointIndex);
		
		for(int id : parent.channels.keySet()){
			if(id == rollBackParent){
				continue;
			}
			
			int lls = 0;
			if(lastCheckpoint.LLS.get(id)!=null){
			lls =lastCheckpoint.LLS.get(id);
			}
			Message rollbackReq = new Message(MessageType.ROLLBACK, parent.getNodeId(), parent.getDomain(), 0);
			rollbackReq.setLLS(lls);
			Stream s = parent.getChannels().get(id);
			sendMessage(s.channel, rollbackReq.toString(), s.streamNumber);
			rollBackRepliesExpected++;
		}
		//end sent rollback reqs
		System.out.println("INFO : recovery manager expecting " + rollBackRepliesExpected + " replies");
		while(rollBackRepliesExpected > 0 && !rollBackFailed){
			 try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(rollBackFailed){
			System.out.println("INFO : recovery manager : roll back failed. sending reject to parent");
			Message rollbackReject = new Message(MessageType.ROLLBACK_REJECT,parent.getNodeId(),parent.getDomain(),0);
			Stream s = parent.getChannels().get(rollBackParent);
			sendMessage(s.channel, rollbackReject.toString(), s.streamNumber);
			parent.setFreezeAppMessages(false);
			rollBackDecision = false;
			rollBackFailed = false;
			rollBackParent = -1;
			rollBackSource = -1;
			rollingBack = false;
			nodesThatWillRollBack = new ArrayList<Integer>();
			
			
		}
		else{
			if(rollBackRepliesExpected == 0){
				if(rollBackParent != parent.getNodeId()){
				Message rollbackReply = new Message(MessageType.ROLLBACK_REPLY,parent.getNodeId(),parent.getDomain(),0);
				rollbackReply.setWillRollBack(true);
				Stream s = parent.getChannels().get(rollBackParent);
				sendMessage(s.channel, rollbackReply.toString(), s.streamNumber);
				}
				else{
					// roll back intiated at this source
					System.out.println("INFO : Recovery Manager : rollback complete. sending confirmation");
					for(int id :  nodesThatWillRollBack){
						Stream s = parent.getChannels().get(id);
						Message m = new Message(MessageType.ROLLBACK_CONFIRM,parent.getNodeId(),parent.getDomain(),0);
						sendMessage(s.channel, m.toString(), s.streamNumber);
						
					}
				}
				
				System.out.println("BEFORE ROLL BACK : now.label = " + parent.label + " APPMESSAGE COUNT " + parent.getAppMessagesReceived().size());
				confirmRollBack();
				System.out.println("ROLLED BACK : now.label = " + parent.label + " APPMESSAGE COUNT " + parent.getAppMessagesReceived().size());
				parent.setFreezeAppMessages(false);
				rollBackDecision = false;
				rollBackFailed = false;
				rollBackParent = -1;
				rollBackSource = -1;
				rollingBack = false;
				nodesThatWillRollBack = new ArrayList<Integer>();
			}
		}
	}
	
	private void sendMessage(SctpChannel socket, String Message,
			int streamNumber) {
		ByteBuffer buffer = ByteBuffer.allocate(64000);

		BufferedReader is = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("INFO : Client : sending message " + Message);
		while (Message != null) {
			// Reset a pointer to point to the start of buffer
			buffer.position(0);
			buffer.clear();
			buffer.put(Message.getBytes());
			buffer.flip();
			try {
				// System.out.println("Buffer is : "+ buffer.toString());
				// Send a message in the channel
				MessageInfo messageInfo = MessageInfo.createOutgoing(null,
						streamNumber);
				// System.out.println("Message from Server : "+ messageInfo);
				socket.send(buffer, messageInfo);
				System.out.println("message sent via channel to stream number"
						+ streamNumber);
				return;
			} catch (IOException ex) {
				Logger.getLogger(Listener.class.getName()).log(Level.SEVERE,
						null, ex);
			}
		}
	}

	public void recordRollBackReply(int id, Message m) {
		// TODO Auto-generated method stub
		if(!rollingBack){
			System.out.println("WARNING : Unexpected rollback reply from node "  + id );
			
		}
		else{
			rollBackRepliesExpected --;
			if(m.willRollBack()){
				nodesThatWillRollBack.add(id);
			}
			System.out.println("INFO : Recovery manager : rollback reply received. still awaiting " + rollBackRepliesExpected + " rollback replies" );
		}
	}
	
	public void recordRollBackReject(){
		rollBackFailed = true;
	}
	
	
	public void confirmRollBack(){
		if(!rollingBack ){
			System.out.println("INFO : Unexpected rollback confirm message/ roll back confirmation from non-parent node.");
			return;
		}
		
		else{
			int lastIndex = parent.getCheckPointTaker().checkPoints.size()-1;
			Checkpoint last = parent.getCheckPointTaker().checkPoints.get(lastIndex);
			System.out.println("LAST CHECKPOINT:\n" + last);
			int thenMaxLabel = -1;
			for(int label : last.LLS.values()){
				if(label>= thenMaxLabel){
					thenMaxLabel = label;
				}
			}
			//roll back to previous state
			System.out.println("MAX LABEL THEN : " + thenMaxLabel);
			parent.label = thenMaxLabel+1;
			parent.LLR = new HashMap<Integer,Integer>();
			parent.LLS = last.LLS;
			parent.FLS = last.FLS;
			parent.setAppMessagesReceived(last.appMessageList);
			
			rollingBack=false;
			rollBackFailed = false;
			rollBackDecision = false;
			rollBackSource = -1;
			rollBackParent = -1;
			rollBackRepliesExpected = 0;
			nodesThatWillRollBack = new ArrayList<Integer>();
			parent.setFreezeAppMessages(false);
			System.out.println("INFO : Recovery Manager: Rollback");
			
		}
	}
}
	
