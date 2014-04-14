import com.sun.nio.sctp.SctpChannel;


public class Stream {

	public Stream(SctpChannel channel2, int i) {
		this.channel = channel2;
		this.streamNumber = i;
	}
	@Override
	public String toString() {
		return "Stream [streamNumber=" + streamNumber + ", channel=" + channel
				+ "]";
	}
	public int streamNumber;
	public SctpChannel channel;
}
