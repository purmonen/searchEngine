/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.io.Serializable;



/**
 *   A list of postings for a given word.
 */
public class PostingsList implements Serializable, Iterable<PostingsEntry>, RandomAccess {
    
    /** The postings list as a linked list. */
    public ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();

    
    public PostingsList() {}
    
    public PostingsList(List<PostingsEntry> postings) {
    	list = new ArrayList<PostingsEntry>(postings);
    }

    /**  Number of postings in this list  */
    public int size() {
    	return list.size();
    }

    /**  Returns the ith posting */
    public PostingsEntry get( int i ) {
    	return list.get( i );
    }
    
    public PostingsEntry getLast() {
    	return list.get( list.size()-1 );
    }
    
    public void add(PostingsEntry entry) {
    	list.add(entry);
    }
    
	public PostingsList intersection(PostingsList postingsList) {
		return new PostingsList(intersection(this, postingsList, docIdComparator));
	}
	
	
	public <T> List<T> intersection(Iterable<T> it1, Iterable<T> it2, Comparator<T> comparator) {
		ArrayList<T> intersection = new ArrayList<T>();
		Iterator<T> a = it1.iterator();
		Iterator<T> b = it2.iterator();
		T ax = a.hasNext() ? a.next() : null;
		T bx = b.hasNext() ? b.next() : null;
		while (ax != null && bx != null) {
			int comparison = comparator.compare(ax, bx);
			if (comparison == 0) {
				intersection.add(ax);
				ax = a.hasNext() ? a.next() : null;
				bx = b.hasNext() ? b.next() : null;
			} else if (comparison < 0) {
				ax = a.hasNext() ? a.next() : null;
			} else {
				bx = b.hasNext() ? b.next() : null;
			}
		}
		return intersection;
	}
	
	@Override
	public Iterator<PostingsEntry> iterator() {
		// TODO Auto-generated method stub
		return list.iterator();
	}
	
	public boolean contains(PostingsEntry entry) {
		return indexOf(entry) >= 0;
	}
	
	public int indexOf(PostingsEntry entry) {
		return Collections.binarySearch(list, entry, docIdComparator);
	}
	
	
	private Comparator<PostingsEntry> docIdComparator = new Comparator<PostingsEntry>() {
		@Override
		public int compare(PostingsEntry o1, PostingsEntry o2) {
			return o1.docID - o2.docID;
		}
	};
	
	public boolean isEmpty() {
		return size() == 0;
	}
}