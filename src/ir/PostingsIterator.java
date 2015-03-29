package ir;

import java.io.FileNotFoundException;
import java.util.Iterator;

public class PostingsIterator implements Iterator {
	private int start;
	private int end;
	IntFile postingsFile;

	public PostingsIterator(IntFile postingsFile, int start, int end) throws FileNotFoundException  {
		this.start = start;
		this.end = end;
		this.postingsFile = postingsFile;
		//		indexFile = new IndexFile(indexFileName);
		//		wordsToPostingsFile = new IntFile(wordsToPostingsFileName);
		//		postingsFile = new IntFile(postingsFileName);
		//
		//		int wordPosition = indexFile.indexOf(token);
		//		if (wordPosition != -1) {
		//			startPosition = wordsToPostingsFile.get(wordPosition);
		//			endPosition = postingsFile.size();
		//			if (wordPosition + 1 < wordsToPostingsFile.size()) {
		//				endPosition = wordsToPostingsFile.get(wordPosition+1);
		//			}
		//		}
	}

	@Override
	public boolean hasNext() {
		return start < end;
	}

	@Override
	public PostingsEntry next() {
		// TODO Auto-generated method stub
		int docID = postingsFile.get(start);
		int position = postingsFile.get(start+1);
		start += 2;
		return new PostingsEntry(docID, position);
	}
}