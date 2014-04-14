public class RemotePeer {
	
	int id;
	int port;
	String domain;

	public RemotePeer(int id,String domain, int port) {
		this.id = id;
		this.domain = domain;
		this.port = port;
	}
	
	public String toString(){
		return ""+this.id + " " + this.domain + " : " + this.port;
	}
}
