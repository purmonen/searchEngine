/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

package ir;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

	public int docID;
	public double score;
	public ArrayList<Integer> positions = new ArrayList<>();

	public PostingsEntry(int docID, double score) {
		this.docID = docID;
		this.score = score; 
	}

	/**
	 *  PostingsEntries are compared by their score (only relevant 
	 *  in ranked retrieval).
	 *
	 *  The comparison is defined so that entries will be put in 
	 *  descending order.
	 */
	public int compareTo( PostingsEntry other ) {
		return Double.compare(score, other.score);
	}
	
	public boolean containsPosition(int position) {
		return (Collections.binarySearch(positions, position) >= 0);
	}

	//
	//  YOUR CODE HERE
	//

}


