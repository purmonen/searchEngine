/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Hedvig Kjellström, 2012
 */  

package ir;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.HashMap;

public class Query {
    
    public List<String> terms = new LinkedList<String>();
    public List<Double> weights = new LinkedList<Double>();
    
    /**
     *  Creates a new empty Query 
     */
    public Query() {
    }
	
    /**
     *  Creates a new Query from a string of words
     */
    public Query( String queryString  ) {
	StringTokenizer tok = new StringTokenizer( queryString );
	while ( tok.hasMoreTokens() ) {
	    terms.add( tok.nextToken() );
	    weights.add( new Double(1) );
	}
    }
    
    /**
     *  Returns the number of terms
     */
    public int size() {
    	return terms.size();
    }
    
    /**
     *  Returns a shallow copy of the Query
     */
    public Query copy() {
	Query queryCopy = new Query();
	queryCopy.terms = terms.stream().map(x -> x).collect(Collectors.toList());
	queryCopy.weights = weights.stream().map(x -> x).collect(Collectors.toList());
	return queryCopy;
    }
    
    /**
     *  Expands the Query using Relevance Feedback
     */
    public void relevanceFeedback( PostingsList results, boolean[] docIsRelevant, Indexer indexer ) {
    	double a = 1;
    	double b = 0.8;
    	double c = 0.1;
    	
    	HashMap<String, Double> queryVector = new HashMap<String, Double>();
    	for (int i = 0; i < terms.size(); i++) {
    		queryVector.put(terms.get(i), weights.get(i));
    	}
    	
    	List<Integer> relevantDocuments = IntStream.range(0, 10)
    			.filter(i -> docIsRelevant[i])
    			.boxed()
    			.map(i -> results.get(i).docID)
    			.collect(Collectors.toList());
    	
    	List<Integer> nonRelevantDocuments = IntStream.range(0, 10)
    			.filter(i -> !docIsRelevant[i])
    			.boxed()
    			.map(i -> results.get(i).docID)
    			.collect(Collectors.toList());
    	
    	for (String term: queryVector.keySet()) {
    		queryVector.put(term, queryVector.get(term)*a);
    	}
    	
    	
    	for (int docID: relevantDocuments) {
    		HashMap<String, Integer> termFrequencies = indexer.index.getTermFrequencies(docID);
    		for (String term: termFrequencies.keySet()) {
    			queryVector.put(term, b * termFrequencies.get(term) / (double)relevantDocuments.size() + queryVector.getOrDefault(term, 0.0));
    		}
    	}
    	
    	for (int docID: nonRelevantDocuments) {
    		HashMap<String, Integer> termFrequencies = indexer.index.getTermFrequencies(docID);
    		for (String term: termFrequencies.keySet()) {
    			queryVector.put(term, -c * termFrequencies.get(term) / (double)nonRelevantDocuments.size() + queryVector.getOrDefault(term, 0.0));
    		}
    	}
 
    	terms = new LinkedList<String>(queryVector.keySet());
    	weights = new LinkedList<Double>(queryVector.values());
    }
    
    public String toString() {
    	// This is how a pro would do it: return terms.stream().reduce("", ((x,y) -> x + " " + y)).trim();
    	String out = "";
    	for (String term: terms) {
    		out += term + " ";
    	}
    	return out.trim();
    }
}

    
