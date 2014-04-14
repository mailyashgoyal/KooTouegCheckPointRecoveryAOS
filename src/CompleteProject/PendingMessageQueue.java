import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

public class PendingMessageQueue {

	CopyOnWriteArrayList<PendingMessage> queue;
	CopyOnWriteArrayList<PendingMessage> deliveredMessages;
	String outputFileName;
	int maxTimeStamp;
	
	public PendingMessageQueue(String outputFileName){
		this.outputFileName = outputFileName;
		queue = new CopyOnWriteArrayList<PendingMessage>();
		deliveredMessages = new CopyOnWriteArrayList<>();
		maxTimeStamp = 0;
	}
	
	public int getMaxTimeStamp(){
		
		updateMaxTimeStamp();
		return maxTimeStamp;
	}
		
	private synchronized void updateMaxTimeStamp()
	{
		int max = maxTimeStamp;
		for(PendingMessage pm : queue){
			max = Math.max(max, pm.currentProposedTimeStamp);
		}
		this.maxTimeStamp = max;
	}
	
	public void addPendingMessage(Message m, int sourceId, int initTimeStamp, int numRepliesExp)
	{
		PendingMessage p = new PendingMessage(sourceId, m, initTimeStamp, numRepliesExp);
		queue.add(p);
		updateMaxTimeStamp();
		System.out.println("INFO : Message added to queue from node " + sourceId);
		//System.out.println(queue);
	}
		
	public void addTimeStampReply(Message m, int remoteId){
		
		// when a node sends a reply with time stamp proposal it will
		//String[] data = m.getData().split(" ");
		// check the message id;
		int msgId = m.getMessageId();
		int proposedTimeStamp = m.getLogicalTimeStamp();

		System.out.println("INFO : Node " + remoteId 
				+ " has suggested time stamp " + proposedTimeStamp 
				+ " for message id " + msgId);
		
		for (PendingMessage pm : queue) {

			if (pm.messageId == msgId) {
				if (m.getType().equals(MessageType.TIMESTAMP_REPLY)) {
					pm.updateProposedTimeStamp(proposedTimeStamp);
					pm.addReplyCount();
				}

				if (m.getType().equals(MessageType.GLOBAL_TIME_STAMP)) {
					if (pm.currentProposedTimeStamp > proposedTimeStamp) {
						System.out
								.println("WARNING: Global time stamp < proposal "
										+ "sent from this node");
					}
					pm.currentProposedTimeStamp = proposedTimeStamp;
					pm.setStatus(PendingMessageStatus.DELIVERABLE);
				}

				// System.out.println(queue);
			}

			updateMaxTimeStamp();

		}
	}

	public void printAllMessages(){
		for(PendingMessage p : this.queue){
			
			System.out.println("ID : " + p.getMessage().messageId + " TS: " + p.currentProposedTimeStamp + " STATUS : " + p.status);
			
			}
	}
	
	public void printAllDeliveredMessages(){
		for(PendingMessage p : this.deliveredMessages){
			
			System.out.println("ID : " + p.getMessage().messageId + " TS: " + p.currentProposedTimeStamp + " STATUS : " + p.status);
		}
	}
	
	public void deliverMessages(){
		ArrayList<PendingMessage> deliveryMessages  =  new ArrayList<PendingMessage>();
		
		//extract deliverable messages
		for(PendingMessage p : queue){
			if(p.getStatus().equals(PendingMessageStatus.DELIVERABLE)){
				p.setStatus(PendingMessageStatus.DELIVERED);
				deliveryMessages.add(p);
			}
		}
		
		//sort the deliverable messages
		Collections.sort(deliveryMessages);
		
		//open conn to outputFile
		PrintWriter pw = null;
		try {
			 pw = new PrintWriter(new FileWriter(outputFileName, true));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//deliver the messages. Send it to the deliveredMessages or file
		for(PendingMessage p : deliveryMessages){
			System.out.println("Message delivered " + p.getMessage());
			deliveredMessages.add(p);
			pw.println("ID : " + p.getMessage().messageId 
					+ " TS: " + p.currentProposedTimeStamp 
					+ " STATUS : " + p.status);
			
		}
		
		pw.close();
		
		
	}
}
