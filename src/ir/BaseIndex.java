package ir;

import java.awt.font.NumericShaper.Range;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
abstract public class BaseIndex implements Index {
	
	
	private final PageRank pageRank = new PageRank("/Users/Sami/Documents/workspace2/sokmotlab1/src/ir/linksDavis.txt");

	

	private PostingsList phraseSearch(Query query) {		
		PostingsList result = new PostingsList();
		for (String term: query.terms) {
			if (result.size() == 0) {
				if (!term.equals(query.terms.get(0))) break;
				result = getPostings(term);
			} else {
				PostingsList newResult = new PostingsList();
				PostingsList newPostings = getPostings(term);
				for (PostingsEntry previousEntry: result) {
					int index = newPostings.indexOf(previousEntry);
					if (index < 0) continue;
					PostingsEntry currentEntry = newPostings.get(index);
					for (int position: previousEntry.positions) {
						if (currentEntry.containsPosition(position+1)) {
							if (newResult.isEmpty() || newResult.getLast().docID != currentEntry.docID) {
								newResult.add(new PostingsEntry(previousEntry.docID, 1337));
							}
							newResult.getLast().positions.add(position+1);
						}
					}
				}
				result = newResult;
			}
		}
		return result;
	}

	public double getVectorLength(int docID) {
		return 0;
	}

	private PostingsList intersectionSearch(Query query) {
		PostingsList result = new PostingsList();
		for (String term: query.terms) {
			if (result.size() == 0) {
				if (!term.equals(query.terms.get(0))) break;
				result = getPostings(term);
			} else {
				result = result.intersection(getPostings(term));
			}
		}
		return result;
	}

	private PostingsList rankedSearch(Query query) {
		HashMap<Integer, Double> documentScores = new HashMap<>();
		for (int i = 0; i < query.size(); i++) {
			String term = query.terms.get(i);
			double idf = idf(term);
//			System.out.println("Idf for " + term + " is " + idf);
			for (PostingsEntry document: getPostings(term)) {
				if (!documentScores.containsKey(document.docID)) {
					documentScores.put(document.docID, 0.0);
				}
				double termFrequency = document.positions.size();
				double score = termFrequency / (double)getDocumentSize(document.docID) * idf * query.weights.get(i);
				documentScores.put(document.docID, documentScores.get(document.docID) + score);
//				System.out.println(docIDs.get("" + document.docID) + ", size: " + getDocumentSize(document.docID) + ", trm: " + termFrequency);
			}
		}
		PostingsList result = new PostingsList();
		
		Stream<Integer> sortedDocIDs = documentScores.keySet().stream().sorted((d1, d2) -> {
			return -1 * documentScores.get(d1).compareTo(documentScores.get(d2));
		});
		result = new PostingsList(sortedDocIDs.map(
				d -> new PostingsEntry(d, documentScores.get(d))).collect(Collectors.toList()));
		return result;
	}

	private double vectorLength(List<Double> vector) {
		return Math.sqrt(vector.stream().reduce(0.0, (x,y) -> x + y*y));
	}

	private double dotProduct(List<Double> x, List<Double> y) {
		return IntStream.range(0, x.size()).mapToDouble((i) -> x.get(i) * y.get(i)).sum();
	}

	public double idf(String term) {
		double df = getPostings(term).size();
		double n = getNumberOfDocuments();
		System.out.println(term + ", df: " + df + ", n: " + n + ", idf: " + Math.log10(n/df));
		assert(Math.log10(n/df) > 0.0);
		return Math.log10(n/df);
	}

	/**
	 *  Searches the index for postings matching the query.
	 */
	public PostingsList search( Query query, int queryType, int rankingType, int structureType ) {
		//internalCheck();
		StopWatch searchStopWatch = new StopWatch("Search for " + query);
		if (query.terms.size() == 0) return null;
		PostingsList result = null;
		switch (queryType) {
		case Index.PHRASE_QUERY: 
			result = phraseSearch(query);
			break;
		case Index.INTERSECTION_QUERY: 
			result = intersectionSearch(query);
			break;
		case Index.RANKED_QUERY:
			switch (rankingType) {
			case Index.TF_IDF:
				result = rankedSearch(query);
				break;
			case Index.PAGERANK:
				result = rankedSearch(query);
				for (PostingsEntry entry: result) {
					entry.score = pageRank.getScore(entry.docID);
				}
				Collections.sort(result.list);
				Collections.reverse(result.list);
				break;
			case Index.COMBINATION:
				result = rankedSearch(query);
//				double pageRankBias = .8;
				result.list.forEach(e -> {
					e.score = e.score + 10*pageRank.getScore(e.docID);
				});
				Collections.sort(result.list);
				Collections.reverse(result.list);
				break;	
			} 
			break;
		}
		System.out.println(searchStopWatch);
		return result;
	}

	public void cleanup() {
	}

	public void destroy() {

	}

	@Override
	public int getDocumentSize(int docID) {
		return 0;
	}

	@Override
	public int getNumberOfDocuments() {
		return 0;
	}
	
	@Override
	public HashMap<String, Integer> getTermFrequencies(int docID) {
		// TODO Auto-generated method stub
		return null;
	}
}
