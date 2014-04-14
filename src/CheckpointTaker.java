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

public class CheckpointTaker implements Runnable {

	Peer parent;
	Checkpoint lastCheckpoint;
	ArrayList<Checkpoint> checkPoints;
	ArrayList<Integer> cohortIds;
	int waitingReplyCount;
	boolean takingCheckpoint;
	boolean checkpointFailed;
	int checkPointingParent = -1;
	private Checkpoint current;
	private boolean checkPointingdecision = false;
	private int checkPointingSource;
	

	public CheckpointTaker(Peer parent) {
		lastCheckpoint = null;
		checkPoints = new ArrayList<Checkpoint>();
		this.parent = parent;
		System.out
				.println("INFO : CheckpointTaker instance initialized for node "
						+ parent.getNodeId());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {
		System.out.println("INFO : CheckpointTaker instance running for node "
				+ parent.getNodeId());
		while (true) {
			System.out
					.println("INFO : CheckpointTaker.run() : press enter to take checkpoint ");
			try {
				new BufferedReader(new InputStreamReader(System.in)).readLine();
				takeCheckPoint(parent.getNodeId());

			} catch (IOException e) {
				System.out.println("ERROR: Checkpoint Taker crashed for node "
						+ parent.getNodeId());
				e.printStackTrace();
			}
		}

	}

	public boolean takeCheckPointIfNeeded(Message m) {

		checkPointingParent = m.getNodeId();
		if(takingCheckpoint && checkPointingSource == m.checkPointingSource){
			//send reply true
			System.out.println("INFO : Checkpointing tree has a cycle. resolved.");
			Message reply = new Message(MessageType.CHECKPOINT_REPLY,
					parent.getNodeId(), parent.getDomain(), 0);
			reply.setDecision(true);
			Stream checkPointingParentStream = parent.getChannels().get(
					checkPointingParent);
			sendMessage(checkPointingParentStream.channel, reply.toString(),
					checkPointingParentStream.streamNumber);
			System.out.println("INFO: checkpoint taker is already taking check point for same source");
			return true;
		}
		else if(takingCheckpoint == true && checkPointingSource != m.checkPointingSource){
			
			System.out.println("INFO : Checkpointing trees collide at this node. rejected, tree started by " + m.checkPointingSource);
			//send checkpoint reject
			Stream s = parent.getChannels().get(m.getNodeId());
			Message reject = new Message(MessageType.CHECKPOINT_REJECT, parent.getNodeId(), parent.getDomain(),0);
			sendMessage(s.channel, reject.toString(), s.streamNumber);

		}
		
		checkPointingSource = m.getCheckPointingSource();

		
		
		int id = m.getNodeId();
		int LLR = m.getLLR(id);
		if (LLR >= parent.getFLS(id) && parent.getFLS(id) > 0 && LLR > 0) {

			checkPointingdecision = true;
			// the checkpointing decision will be sent when all replies are
			// received
			takeCheckPoint(checkPointingSource);
		} else {
			// send check point reply with decision = false;
			checkPointingdecision = false;
			takingCheckpoint = false;
			Message reply = new Message(MessageType.CHECKPOINT_REPLY,
					parent.getNodeId(), parent.getDomain(), 0);
			reply.setDecision(false);
			Stream checkPointingParentStream = parent.getChannels().get(
					checkPointingParent);
			sendMessage(checkPointingParentStream.channel, reply.toString(),
					checkPointingParentStream.streamNumber);
		}
		return checkPointingdecision;

	}

	public void takeCheckPoint(int source) {
		System.out.println("INFO : CheckpointTaker : taking checkpoint initiated by " + source);
	    
		if (checkPointingParent == -1 ) {
			System.out
					.println("INFO : CheckPointTaker : no checkpointing parent. Self initiated. ");
			checkPointingSource = parent.getNodeId();
		} else {
			System.out
					.println("INFO : CheckPointTaker : checkpointing parent = "
							+ checkPointingParent);
		}
		takingCheckpoint = true;
		checkpointFailed = false;

		// freeze app message sending
		this.parent.setFreezeAppMessages(true);
		// take tentative check point
		ArrayList<Message> amCopy = new ArrayList<Message>();
		amCopy.addAll(parent.getAppMessagesReceived());
		
		HashMap<Integer, Integer> flsCopy = new HashMap<Integer,Integer>();
		flsCopy.putAll(parent.FLS);
		
		HashMap<Integer, Integer> llsCopy = new HashMap<Integer,Integer>();
		llsCopy.putAll(parent.LLS);
		
		HashMap<Integer, Integer> llrCopy = new HashMap<Integer,Integer>();
		llrCopy.putAll(parent.LLR);
		
		
		current = new Checkpoint(amCopy, flsCopy,
				llsCopy, llrCopy);
		System.out.println("TENTATIVE CHECK POINT : " + current);
		// notify the cohorts
		notifyCohorts(checkPointingParent,checkPointingSource);
		
		//reset vectors
		parent.LLR = new HashMap<Integer,Integer>();
		parent.FLS = new HashMap<Integer,Integer>();
		
		// while checkpointing is not complete do not return
		while (waitingReplyCount > 0 && !checkpointFailed) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (checkpointFailed) {
			
			if(checkPointingSource == parent.getNodeId()){
				//checkpoint failed try again.
				System.out.println("Checkpoint rejected. Try again later");
			}
			else{
			// send reject to parent
			Message m = new Message(MessageType.CHECKPOINT_REJECT,
					parent.getNodeId(), parent.getDomain(), 0);
			m.setDecision(false);
			Stream checkPointingParentStream = parent.getChannels().get(
					checkPointingParent);
			sendMessage(checkPointingParentStream.channel, m.toString(),
					checkPointingParentStream.streamNumber);
			}
			
		} else {

			// subtree is done checkpointing
			// check pointing in sub tree is over. send reply to checkpoinint
			// parent
			if (checkPointingParent == -1) {
				System.out
						.println("INFO : check pointing initiated at this node. Hence no further action");
				
				System.out.println("INFO : check pointing complete");
				
				//send check point confirmation to cohorts
				for (int cohortId : cohortIds) {
					Message m = new Message(MessageType.CHECKPOINT_CONFIRM, parent.getNodeId(),
							parent.getDomain(), 0);
					m.setCheckPointingSource(checkPointingSource);
					System.out.println("INFO : CheckPointTaker notifying cohort to confirm checkpoint "
							+ cohortId);
					System.out.println("INFO : CheckPointTaker notifying cohort to confirm checkpoint "
							+ cohortId + " message : " + m);
					Stream stream = parent.channels.get(cohortId);
					SctpChannel channel = stream.channel;
					int streamNum = stream.streamNumber;
					sendMessage(channel, m.getString(), streamNum);
				}
				checkPoints.add(current);
				System.out.println("CHECKPOINTS : \n" + checkPoints);
				checkpointFailed =  false;
				takingCheckpoint = false;
				checkPointingSource = -1;
				checkPointingParent = -1;
				waitingReplyCount = 0;
				cohortIds = new ArrayList<Integer>();
				parent.setFreezeAppMessages(false);
				
				
				
			} else {
				System.out
						.println("INFO : sending checkpoint reply to checkpointing parent");
				Message m = new Message(MessageType.CHECKPOINT_REPLY,
						parent.getNodeId(), parent.getDomain(), 0);
				m.setDecision(true);
				Stream checkPointingParentStream = parent.getChannels().get(
						checkPointingParent);
				sendMessage(checkPointingParentStream.channel, m.toString(),
						checkPointingParentStream.streamNumber);
			}
		}
		
		
		parent.setFreezeAppMessages(false);
		checkpointFailed = false;
		checkPointingParent = -1;
		checkPointingSource = -1;

	}

	public void notifyCohorts(int checkpointingParentId, int checkPointingSource) {
		System.out
				.println("INFO : CheckpointTaker : Checkpoint taken. Notifying cohorts");

		// calculate cohorts
		cohortIds = new ArrayList<Integer>();
		for (int cohortId : parent.LLR.keySet()) {
			if(cohortId == checkPointingSource || cohortId == checkpointingParentId){
				continue;
			}
			System.out.println("LLR from " + cohortId + " "+ parent.LLR.get(cohortId));
			if (parent.LLR.get(cohortId) > 0) {
				cohortIds.add(cohortId);
			}
		}
		System.out.println("Cohorts : " + cohortIds);
		// set waiting reply count
		waitingReplyCount = cohortIds.size();

		// send check point message to each cohort
		for (int cohortId : cohortIds) {
			Message m = new Message(MessageType.CHECKPOINT, parent.getNodeId(),
					parent.getDomain(), 0);
			m.setLLR(parent.LLR.get(cohortId));
			m.setCheckPointingSource(checkPointingSource);
			System.out.println("INFO : CheckPointTaker notifying cohort "
					+ cohortId);
			System.out.println("INFO : CheckPointTaker notifying cohort "
					+ cohortId + " message : " + m);
			Stream stream = parent.channels.get(cohortId);
			SctpChannel channel = stream.channel;
			int streamNum = stream.streamNumber;
			sendMessage(channel, m.getString(), streamNum);
		}
	}

	private void sendMessage(SctpChannel socket, String Message,
			int streamNumber) {
		ByteBuffer buffer = ByteBuffer.allocate(64000);

		BufferedReader is = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("INFO : Client : Inside : Thread send mesaage");
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

	public void recordCheckpointReply() {
		if (!takingCheckpoint) {
			System.out
					.println("ERROR:CheckpointTaker : not taking check point. Unexpected checkpoint reply");

		}
		if (cohortIds == null || cohortIds.size() == 0) {
			System.out
					.println("ERROR:CheckpointTaker : cohort list empty or null. Unexpected checkpoint reply");
		}

		waitingReplyCount--;
		System.out
				.println("INFO : Received check point reply. CheckPointTaker still waiting for "
						+ waitingReplyCount + " replies");

	}

	public void recordCheckpointReject() {
		// checkpoint failed
		checkpointFailed = true;
		takingCheckpoint = false;
		cohortIds = null;
		waitingReplyCount = 0;
		System.out.println("INFO: checkpoint rejected.");
	}

	public void confirmCheckPoint(Message confirmation) {
		System.out.println("INFO : checkpoint confirmed.");
		
		//relay confirmation to cohorts
		for(int id : cohortIds)
		{
			Stream s = parent.getChannels().get(id);
			sendMessage(s.channel, confirmation.toString(), s.streamNumber);
		}
		checkPoints.add(current);
		System.out.println("CHECKPOINTS : \n" + checkPoints);
		checkpointFailed =  false;
		takingCheckpoint = false;
		checkPointingSource = -1;
		checkPointingParent = -1;
		waitingReplyCount = 0;
		cohortIds = new ArrayList<Integer>();
		parent.setFreezeAppMessages(false);
		// TODO Auto-generated method stub
		
	}
}
