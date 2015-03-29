package ir;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;

public class IntFile extends ArrayFile {
	public IntFile(String fileName) throws FileNotFoundException {
		super(fileName, 4);
	}
	
	public int get(int index) {
		return ByteBuffer.wrap(getBytes(index)).getInt();
	}
	
	public void add(int value) {
		add(ByteBuffer.allocate(4).putInt(value).array());
	}
}