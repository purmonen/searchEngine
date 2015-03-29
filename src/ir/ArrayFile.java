package ir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class ArrayFile implements AutoCloseable {
	private RandomAccessFile file;
	private int elementSize;
	private byte[] buffer;

	public ArrayFile(String fileName, int elementSize) throws FileNotFoundException {
		this.elementSize = elementSize;
		buffer = new byte[elementSize];
		file = new RandomAccessFile(fileName, "rw");
	}

	public int size() {
		try {
			return (int)file.length() / elementSize;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	public byte[] getBytes(int index) {
		try {
			file.seek(index*elementSize);
			byte[] bytes = new byte[elementSize];
			file.read(bytes);
			return bytes;
		} catch (IOException e1) {
			throw new IndexOutOfBoundsException();
		}
	}
	
	public byte[] getBytes(int start, int end) {
		try {
			file.seek(start*elementSize);
			// System.out.println(new String(stringBuffer));
			// System.out.println(new String(stringBuffer).trim().length());
			byte[] bytes = new byte[elementSize*(end-start)];
			file.read(bytes);
			return bytes;
		} catch (IOException e1) {
			throw new IndexOutOfBoundsException();
		}
	}

	public void add(byte[] value) {
		try {
			file.write(value);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void close() throws IOException {
		file.close();
	}
	
	public ByteBuffer getBuffer(int size) {
		try {
			return file.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, elementSize*size);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ByteBuffer.allocate(0);
	}
}