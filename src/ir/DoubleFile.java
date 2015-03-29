package ir;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;


public class DoubleFile extends ArrayFile {
	public DoubleFile(String fileName) throws FileNotFoundException {
		super(fileName, Double.BYTES);
	}
	
	public double get(int index) {
		return ByteBuffer.wrap(getBytes(index)).getDouble();
	}
	
	public void add(double value) {
		add(ByteBuffer.allocate(Double.BYTES).putDouble(value).array());
	}
}