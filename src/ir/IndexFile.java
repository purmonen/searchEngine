package ir;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class IndexFile extends ArrayFile {
	private static final int wordSize = 80;
	private byte[] buffer = new byte[wordSize];

	public IndexFile(String fileName) throws FileNotFoundException {
		super(fileName, wordSize);
	}

	public String get(int index) {
		return new String(getBytes(index)).trim();
	}

	public void add(String value) {
			try {
				Arrays.fill(buffer, (byte)0);
				byte[] bytes = value.getBytes("utf-8");
				System.arraycopy(bytes, 0, buffer, 0, Math.min(buffer.length, bytes.length));
				add(buffer);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	public String get2(int index) {
		return get(index);
	}
	
	public int size2() {
		return size();
	}

	public int indexOf(String value) {
		return (int) binarySearch(value, new BinarySearchable<String>() {
			public int size() {
				return size2();
			}
			public String get(int index) {
				return get2(index);
			}
		});
	}
	
	private interface BinarySearchable<T extends Comparable<T>> {
		int size();
		T get(int index);
	}
	
	private <T extends Comparable<T>> long binarySearch(T word, BinarySearchable<T> list) {
		long low = 0;
		long high = list.size()-1;		
		while (low <= high) {
			long middle = (low + high) / 2;
			T middleWord = list.get((int) middle);
			int wordComparison = word.compareTo(middleWord);
			if (wordComparison == 0) {
				return middle;
			} else if (wordComparison > 0) {
				low = middle + 1;
			} else {
				high = middle - 1;
			}
		}
		return -1;
	}
}
