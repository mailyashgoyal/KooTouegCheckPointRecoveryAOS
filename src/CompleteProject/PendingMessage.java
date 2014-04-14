import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PendingMessage implements Comparable{
	
	private
	Message message;
	int messageId;
	int sourceId;
	int currentProposedTimeStamp;
	int numRepliesExpected;
	int numRepliesReceived;
	PendingMessageStatus status;
		
	public PendingMessage(int sourceId, Message m, int initTimeStamp, int numRepliesExp)
	{
		this.sourceId = sourceId;
		this.message = m;
		this.messageId = m.messageId;
		
		this.currentProposedTimeStamp = initTimeStamp;
		this.status = PendingMessageStatus.PENDING;
		this.numRepliesExpected = numRepliesExp;
		this.numRepliesReceived = 0;
	}

	public Message getMessage() {
		return message;
	}
	public int getMessageId() {
		return messageId;
	}
	public int getSourceId() {
		return sourceId;
	}
	public int getCurrentProposedTimeStamp() {
		return currentProposedTimeStamp;
	}
	public int getNumRepliesExpected() {
		return numRepliesExpected;
	}
	public int getNumRepliesReceived() {
		return numRepliesReceived;
	}
	public PendingMessageStatus getStatus() {
		return status;
	}
	public void setStatus(PendingMessageStatus status) {
		this.status = status;
		if(status.equals(PendingMessageStatus.DELIVERABLE)){
			System.out.println("INFO : Message has become Deliverable : " + this.messageId);
		}
	}

	public void addReplyCount() {
		
		this.numRepliesReceived++;
		if(numRepliesReceived == numRepliesExpected){
			this.setStatus(PendingMessageStatus.READY);
			System.out.println("INFO : Message status for message id "+ this.messageId + " changed to READY");
		}
	}

	public void updateProposedTimeStamp(int proposedTimeStamp) {
		int max = Math.max(proposedTimeStamp, this.currentProposedTimeStamp);
		if(max != this.currentProposedTimeStamp)
		{
			System.out.println("INFO :!!!!!!!!!!!!!!!!! Time stamp updated to new maximum");
		}
		this.currentProposedTimeStamp = max;
	}

	@Override
	public int compareTo(Object arg0) {
		PendingMessage o = (PendingMessage) arg0;
		
		if(o.currentProposedTimeStamp < this.currentProposedTimeStamp){
			return 1;
		}
		else if(o.currentProposedTimeStamp > this.currentProposedTimeStamp){
			return -1;
		}
		else if(o.currentProposedTimeStamp == this.currentProposedTimeStamp)
		{
			int oNodeId = o.messageId/1000;
			int thisNodeId = this.messageId/1000;
			if(oNodeId < thisNodeId){
				return 1;
			}
			else if (oNodeId > thisNodeId){
				return -1;
			}
			else if(oNodeId == thisNodeId)
			{
				System.out.println("WARN: Message queue has two identical messages.");
				return 0;
			}
		}
			return 0;
	}
  	
	@Override
	public String toString() {
		return "PendingMessage [message=" + message + ", messageId="
				+ messageId + ", sourceId=" + sourceId
				+ ", currentProposedTimeStamp=" + currentProposedTimeStamp
				+ ", numRepliesExpected=" + numRepliesExpected
				+ ", numRepliesReceived=" + numRepliesReceived + ", status="
				+ status + "]\n";
	}

	public static void main(String args[]){
		
		ArrayList<PendingMessage> list = new ArrayList<PendingMessage>();
		list.add(new PendingMessage(0,null,1,0));
		list.add(new PendingMessage(1,null,4,0));
		list.add(new PendingMessage(2,null,4,0));
		list.add(new PendingMessage(2,null,3,0));
		list.add(new PendingMessage(3,null,3,0));
		list.add(new PendingMessage(3,null,6,0));
		list.add(new PendingMessage(4,null,6,0));
		list.add(new PendingMessage(4,null,9,0));
		list.add(new PendingMessage(5,null,9,0));
		list.add(new PendingMessage(5,null,9,0));

		Collections.sort(list);
		System.out.println(list);
	}
}
