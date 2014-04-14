
public enum MessageType {

	BROADCAST(0),
	TIMESTAMP_REPLY(1),
	GLOBAL_TIME_STAMP(2);
	
	int val;

	MessageType(int val){
		this.val = val;
	}
	
	public int getValue(){
		return val;
	}
	
}
