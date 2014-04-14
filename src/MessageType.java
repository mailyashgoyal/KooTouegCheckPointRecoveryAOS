
public enum MessageType {

	/*
	 * Here declare all the new message types
	 * and handle them using switchcase in 
	 */

	APPLICATION(0),
	CONTROL(1),
	CHECKPOINT(2),
	RECOVERY(3),
	CHECKPOINT_REPLY(4),
	CHECKPOINT_REJECT(5), 
	CHECKPOINT_CONFIRM(6),
	ROLLBACK(7),
	ROLLBACK_REPLY(8),
	ROLLBACK_REJECT(9),
	ROLLBACK_CONFIRM(10);
	
	int val;

	MessageType(int val){
		this.val = val;
	}

	public int getValue(){
		return val;
	}

}
