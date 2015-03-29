package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex extends BaseIndex {

	/** The index as a hashtable. */
	private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();
	private HashMap<Integer, Integer> documentSize = new HashMap<>();
	private HashMap<Integer, ArrayList<PostingsEntry>> documentEntries = new HashMap<>();
	private HashMap<Integer, HashMap<String, Double>> termFrequency = new HashMap<>();
	
	/**
	 *  Inserts this token in the index.
	 */
	public void insert( String token, int docID, int offset ) {
		if (!index.containsKey(token)) {
			index.put(token, new PostingsList());
		}
		if (!documentSize.containsKey(docID)) {
			documentSize.put(docID, 0);
		}
		documentSize.put(docID, documentSize.get(docID)+1);
		PostingsList postings = index.get(token);
		if (postings.size() == 0 || postings.getLast().docID != docID) {
			PostingsEntry entry = new PostingsEntry(docID, 1337);
			postings.add(entry);
			if (!documentEntries.containsKey(docID)) {
				documentEntries.put(docID, new ArrayList<PostingsEntry>());
			}
			documentEntries.get(docID).add(entry);
		}
		postings.getLast().positions.add(offset);
//		if (!termFrequency.containsKey(docID)) {
//			termFrequency.put(docID, new HashMap<String, Double>());
//		}
//		if (!termFrequency.get(docID).containsKey(token)) {
//			termFrequency.get(docID).put(token, 0.0);
//		}
//		termFrequency.get(docID).put(token, termFrequency.get(docID).get(token)+1);
		
		// ArrayList<PostingsEntry> d = documentEntries.get(docID);
		// System.out.println(postings.getLast().positions.size() + " " + d.get(0).positions.size());
		//assert(postings.getLast().positions.size() == d.get(d.size()-1).positions.size());
	}
	
	@Override
	public double getVectorLength(int docID) {
		return Math.sqrt(termFrequency.get(docID).keySet().stream().mapToDouble(
				word -> Math.pow(termFrequency.get(docID).get(word) / (double)getDocumentSize(docID) * idf(word),2.0)).sum());
		//return Math.sqrt(documentEntries.get(docID).stream().mapToDouble(x -> x.positions.size() * x.positions.size()).sum());
	}

	/**
	 *  Returns all the words in the index.
	 */
	public Iterator<String> getDictionary() {
		return index.keySet().iterator();
	}

	/**
	 *  Returns the postings for a specific term, or null
	 *  if the term is not in the index.
	 */
	public PostingsList getPostings( String token ) {
		if (index.containsKey(token)) { 
			return index.get(token);
		}
		return new PostingsList();
	}

	/**
	 *  No need for cleanup in a HashedIndex.
	 */
	public void cleanup() {
	}

	@Override
	public int getDocumentSize(int docID) {
		// TODO Auto-generated method stub
		return documentSize.get(docID);
	}

	@Override
	public int getNumberOfDocuments() {
		// TODO Auto-generated method stub
		return documentSize.size();
	}
}
