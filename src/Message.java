import java.util.StringTokenizer;

public class Message {
	
	int messageId;
	int nodeId;
	private int label;
	int LLRij;
	String nodeName;
	String string;
	String data;
	boolean decision;
	int checkPointingSource;
	int LLSij;
	boolean willRollBack;
	
	
	public boolean willRollBack() {
		return willRollBack;
	}
	public void setWillRollBack(boolean willRollBack) {
		this.willRollBack = willRollBack;
		update();
	}
	public int getLLS() {
		return LLSij;
	}
	public void setLLS(int lLSij) {
		LLSij = lLSij;
		update();
	}
	MessageType type;
	private int rollBackSource;

	public int getRollBackSource() {
		return rollBackSource;
	}
	public void setRollBackSource(int rollBackSource) {
		this.rollBackSource = rollBackSource;
		update();
	}
	public int getCheckPointingSource() {
		return checkPointingSource;
	}
	public void setCheckPointingSource(int checkPointingSource) {
		this.checkPointingSource = checkPointingSource;
	}
	public void update(){
		this.string = ""+this.messageId + "|"+ this.type + "|"+this.nodeId + "|" + this.nodeName + "|" + this.label + "|" + this.data + "|" + checkPointingSource + "|" + rollBackSource + "|" + LLSij + "|" + willRollBack ;
	}
	public int getNodeId() {
		return nodeId;
	}
	public int getLLR(int LLRij)
	{
		return this.LLRij;
	}
	public void setLLR(int LLRij) 
	{
		this.LLRij = LLRij;
		this.data = "" + LLRij;
		if(!this.type.equals(MessageType.CHECKPOINT)){
			System.err.println("WARNING : Setting LLR field for non-checkpoint message.");
		}
		update();
	}
	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
		update();
	}
	public int getLabel() {
		return label;
	}
	public void setLabel(int logicalTimeStamp) {
		this.label = logicalTimeStamp;
		update();
	}
	public String getNodeName() {
		return nodeName;
	}
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
		update();
	}
	public String getString() {
		return string;
	}
	public Message(MessageType type,int nodeId, String nodeName, int label) {
		super();
		this.type = type;
		this.nodeId = nodeId;
		this.label = label;
		this.nodeName = nodeName;
		this.messageId = (nodeId *1000) + label; 
		this.checkPointingSource = -1;
		update();
	}
	public Message(String string)
	{
		this.string = string;
		StringTokenizer st = new StringTokenizer(string,"|");
		try {
			String str = st.nextToken();
			//System.out.println("token 1 = " + str);
			this.messageId = Integer.parseInt(str);
			
			str = st.nextToken();
			//System.out.println("token 2 = " + str);
			this.type = getMessageType(str);
		}catch (Exception e) {
			System.out.println("Error : Error getting message type");
			e.printStackTrace();
		}
		String str = st.nextToken();
		//System.out.println("token 3 = " + str);
		this.nodeId = Integer.parseInt(str);
		
		str = st.nextToken();
		//System.out.println("token 4 = " + str);
		this.nodeName = str;
		
		str = st.nextToken();
		//System.out.println("token 5 = " + str);
		this.label = Integer.parseInt(str.trim());
		
		str = st.nextToken();
		//System.out.println("token 6 = " + str);
		this.data = str.trim();
		
				
		str = st.nextToken();
		//System.out.println("token 6 = " + str);
		this.checkPointingSource = Integer.parseInt(str.trim());
		
		
		str = st.nextToken();
		//System.out.println("token 6 = " + str);
		this.rollBackSource = Integer.parseInt(str.trim());
		
		str = st.nextToken();
		//System.out.println("token 6 = " + str);
		this.LLSij = Integer.parseInt(str.trim());
		
		str = st.nextToken();
		//System.out.println("token 6 = " + str);
		this.willRollBack = Boolean.parseBoolean(str.trim());
		
		
		
		//based on type, also parse data to set additional fields.
		parseData();
		
		
		
	}
	
	public void parseData(){
		switch(type){
		 
		case APPLICATION:
			// the data is not important
			break;
		case CHECKPOINT : 
			// the data is LLRij
			LLRij = Integer.parseInt(data.trim());
			break;
		}
	} 
	
	
	private static MessageType getMessageType(String s) throws Exception {
	
		MessageType resolvedType = null;
		for(MessageType type : MessageType.values()){
			if(type.name().equals(s)){
				return type;
			}
		}
		/*
		switch(s){
		case 0 : 
			resolvedType = MessageType.APPLICATION;
			break;
		case 1 :
			resolvedType = MessageType.CONTROL;
			break;
		case 2 :
			resolvedType = MessageType.CHECKPOINT;
			break;
		case 3 :
			resolvedType = MessageType.RECOVERY;
			break;
		case 4 :
			resolvedType = MessageType.CHECKPOINT_REPLY;
			break;
		case 5 :
			resolvedType = MessageType.CHECKPOINT_REJECT;
			break;
		case 6 :
			resolvedType = MessageType.CHECKPOINT_CONFIRM;
			break;
		case 7 :
			resolvedType = MessageType.ROLLBACK;
			break;
		case 8 :
			resolvedType = MessageType.ROLLBACK_REPLY;
			break;
		case 9 :
			resolvedType = MessageType.ROLLBACK_REJECT;
			break;
		case 10 :
			resolvedType = MessageType.ROLLBACK_CONFIRM;
			break;
		default : 
			throw new Exception("Error : Message type not recognized");
		}
		*/
		
		return resolvedType;
	}
	public String toString(){
		update();
		return this.string;
	}

	public String getData(){
		return data;
	}
	public void setData(String data){
		this.data = data;
		update();
	}
	public MessageType getType() {
		return type;
	}
	public void setType(MessageType type) {
		this.type = type;
	}
	public int getMessageId(){
		return messageId;
	}
	public void setMessageId(int messageId) {
		this.messageId = messageId;
		update();
	}
	public boolean getDecision() {
		if(!type.equals(MessageType.CHECKPOINT_REPLY)){
			System.out.println("WARNING : CheckpointTaker : querying decision field in non-checkpoint reply message");
		}
		return decision;
	}
	public void setDecision(boolean decision) {
		if(!type.equals(MessageType.CHECKPOINT_REPLY)){
			System.out.println("WARNING : CheckpointTaker : setting decision field in non-checkpoint reply message");
		}
		this.decision = decision;
		this.data = ""+decision;
		update();
	}

	public static void main(String args[]){
		String s = "APPLICATION";
		MessageType type = null;
		try {
			type = Message.getMessageType(s);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("type = " + type +" val = " + type.getValue());
	}
	
}
