import java.util.StringTokenizer;

public class Message {
	
	int messageId;
	int nodeId;
	private int logicalTimeStamp;
	String nodeName;
	String string;
	String data;
	
	MessageType type;
		
	public void update(){
		this.string = ""+this.messageId + "|"+ this.type.getValue() + "|"+this.nodeId + "|" + this.nodeName + "|" + this.logicalTimeStamp + "|" + this.data;
	}
	public int getNodeId() {
		return nodeId;
	}
	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
		update();
	}
	public int getLogicalTimeStamp() {
		return logicalTimeStamp;
	}
	public void setLogicalTimeStamp(int logicalTimeStamp) {
		this.logicalTimeStamp = logicalTimeStamp;
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
	public Message(MessageType type,int nodeId, String nodeName, int logicalTimeStamp) {
		super();
		this.type = type;
		this.nodeId = nodeId;
		this.logicalTimeStamp = logicalTimeStamp;
		this.nodeName = nodeName;
		this.messageId = (nodeId *1000) + logicalTimeStamp;  
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
			this.type = getMessageType(Integer.parseInt(str));
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
		this.logicalTimeStamp = Integer.parseInt(str.trim());
		
		str = st.nextToken();
		//System.out.println("token 6 = " + str);
		this.data = str.trim();
	}
	private MessageType getMessageType(int parseInt) throws Exception {
	
		MessageType resolvedType = null;
		switch(parseInt){
		case 0 : 
			resolvedType = MessageType.BROADCAST;
			break;
		
		case 1 :
			resolvedType = MessageType.TIMESTAMP_REPLY;
			break;
		case 2 :
			resolvedType = MessageType.GLOBAL_TIME_STAMP;
			break;
		default : 
			throw new Exception("Error : Message type not recognized");
		}
		
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
	
	
}
