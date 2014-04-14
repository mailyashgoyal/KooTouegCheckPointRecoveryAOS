import java.util.ArrayList;
import java.util.HashMap;


public class Checkpoint {

	ArrayList<Message> appMessageList;
	HashMap <Integer,Integer> FLS = new HashMap<Integer, Integer>();
	HashMap <Integer,Integer> LLS = new HashMap<Integer, Integer>();
	HashMap <Integer,Integer> LLR = new HashMap<Integer, Integer>();
	
	
	
	public ArrayList<Message> getAppMessageList() {
		return appMessageList;
	}

	public void setAppMessageList(ArrayList<Message> appMessageList) {
		this.appMessageList = appMessageList;
	}

	public HashMap<Integer, Integer> getFLS() {
		return FLS;
	}

	public void setFLS(HashMap<Integer, Integer> fLS) {
		FLS = fLS;
	}

	public HashMap<Integer, Integer> getLLS() {
		return LLS;
	}

	public void setLLS(HashMap<Integer, Integer> lLS) {
		LLS = lLS;
	}

	public HashMap<Integer, Integer> getLLR() {
		return LLR;
	}

	public void setLLR(HashMap<Integer, Integer> lLR) {
		LLR = lLR;
	}


	public Checkpoint(ArrayList<Message> appMessageList,
			HashMap<Integer, Integer> fLS, HashMap<Integer, Integer> lLS,
			HashMap<Integer, Integer> lLR) {
		super();
		this.appMessageList = appMessageList;
		FLS = fLS;
		LLS = lLS;
		LLR = lLR;
	}

	
	
	
	@Override
	public String toString() {
		return "Checkpoint [appMessageList=" + appMessageList + ", \nFLS=" + FLS
				+ ", \nLLS=" + LLS + ", \nLLR=" + LLR + "]";
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		

	}

}
