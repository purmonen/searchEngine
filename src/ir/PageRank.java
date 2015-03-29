/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2012
 */  
package ir;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.io.*;


public class PageRank{

	/**  
	 *   Maximal number of documents. We're assuming here that we
	 *   don't have more docs than we can keep in main memory.
	 */
	final static int MAX_NUMBER_OF_DOCS = 2_000_000;

	private int numberOfDocs;
	private List<Double> pageRank = new ArrayList<>();
	private List<Integer> pageRankOrder = new ArrayList<>();

	public PostingsList pageRankPostings(int size) {
		return new PostingsList(IntStream.range(0, size < pageRank.size() ? size : pageRank.size()).boxed().map(
				i -> new PostingsEntry(Integer.parseInt(docName[pageRankOrder.get(i)]), pageRank.get(pageRankOrder.get(i)))).sorted((e1, e2) -> -((Double)e1.score).compareTo(e2.score)).collect(Collectors.toList()));
	}

	public double getScore(int docID) {
		if (docNumber.containsKey("" + docID) && docNumber.get("" + docID) < pageRank.size()) {
			return pageRank.get(docNumber.get("" + docID));
		}
		return -1337;
	}
	
	/**
	 *   Mapping from document names to document numbers.
	 */
	private static Hashtable<String,Integer> docNumber = new Hashtable<String,Integer>();

	/**
	 *   Mapping from document numbers to document names
	 */
	private static String[] docName = new String[MAX_NUMBER_OF_DOCS];

	/**  
	 *   A memory-efficient representation of the transition matrix.
	 *   The outlinks are represented as a Hashtable, whose keys are 
	 *   the numbers of the documents linked from.<p>
	 *
	 *   The value corresponding to key i is a Hashtable whose keys are 
	 *   all the numbers of documents j that i links to.<p>
	 *
	 *   If there are no outlinks from i, then the value corresponding 
	 *   key i is null.
	 */
	private Hashtable<Integer,Hashtable<Integer,Boolean>> link = new Hashtable<Integer,Hashtable<Integer,Boolean>>();

	/**
	 *   The number of outlinks from each node.
	 */
	private int[] out = new int[MAX_NUMBER_OF_DOCS];

	/**
	 *   The number of documents with no outlinks.
	 */
	private int numberOfSinks = 0;

	/**
	 *   The probability that the surfer will be bored, stop
	 *   following links, and take a random jump somewhere.
	 */
	private final static double BORED = 0.15;

	/**
	 *   Convergence criterion: Transition probabilities do not 
	 *   change more that EPSILON from one iteration to another.
	 */
	private final static double EPSILON = 0.0001;

	/**
	 *   Never do more than this number of iterations regardless
	 *   of whether the transistion probabilities converge or not.
	 */
	private final static int MAX_NUMBER_OF_ITERATIONS = 1000;


	/* --------------------------------------------- */


	public PageRank( String filename ) {
		int noOfDocs = readDocs( filename );
		computePagerank( noOfDocs );
	}


	/* --------------------------------------------- */


	/**
	 *   Reads the documents and creates the docs table. When this method 
	 *   finishes executing then the @code{out} vector of outlinks is 
	 *   initialised for each doc, and the @code{p} matrix is filled with
	 *   zeroes (that indicate direct links) and NO_LINK (if there is no
	 *   direct link. <p>
	 *
	 *   @return the number of documents read.
	 */
	private int readDocs( String filename ) {
		int fileIndex = 0;
		try {
			System.err.print( "Reading file... " );
			BufferedReader in = new BufferedReader( new FileReader( filename ));
			String line;
			while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
				int index = line.indexOf( ";" );
				String title = line.substring( 0, index );
				Integer fromdoc = docNumber.get( title );
				//  Have we seen this document before?
				if ( fromdoc == null ) {	
					// This is a previously unseen doc, so add it to the table.
					fromdoc = fileIndex++;
					docNumber.put( title, fromdoc );
					docName[fromdoc] = title;
				}
				// Check all outlinks.
				StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
				while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
					String otherTitle = tok.nextToken();
					Integer otherDoc = docNumber.get( otherTitle );
					if ( otherDoc == null ) {
						// This is a previousy unseen doc, so add it to the table.
						otherDoc = fileIndex++;
						docNumber.put( otherTitle, otherDoc );
						docName[otherDoc] = otherTitle;
					}
					// Set the probability to 0 for now, to indicate that there is
					// a link from fromdoc to otherDoc.
					if ( link.get(fromdoc) == null ) {
						link.put(fromdoc, new Hashtable<Integer,Boolean>());
					}
					if ( link.get(fromdoc).get(otherDoc) == null ) {
						link.get(fromdoc).put( otherDoc, true );
						out[fromdoc]++;
					}
				}
			}
			if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
				System.err.print( "stopped reading since documents table is full. " );
			}
			else {
				System.err.print( "done. " );
			}
			// Compute the number of sinks.
			for ( int i=0; i<fileIndex; i++ ) {
				if ( out[i] == 0 )
					numberOfSinks++;
			}
		}
		catch ( FileNotFoundException e ) {
			System.err.println( "File " + filename + " not found!" );
		}
		catch ( IOException e ) {
			System.err.println( "Error reading file " + filename );
		}
		System.err.println( "Read " + fileIndex + " number of documents" );
		numberOfDocs = fileIndex;
		return fileIndex;
	}

	private int randRange(int end) {
		return (int)Math.floor(Math.random() * end);
	}
	
	private List<Double> monteCarlo(int numRuns, boolean useRandomStart, boolean useCompletePath, boolean stopAtDanglingNodes) {
		double[] pageRank = new double[numberOfDocs];
		assert(Arrays.stream(pageRank).sum() == 0);
		for (int startPosition = 0; startPosition < numberOfDocs; startPosition++) {
			for (int iteration = 0; iteration < numRuns; iteration++) {
				int position = useRandomStart ? randRange(numberOfDocs) : startPosition;
				if (useCompletePath) {
					pageRank[position]++;
				}
				while (Math.random() > BORED) {
					Hashtable<Integer, Boolean> links = link.get(position);
					if (links != null) {
						List<Integer> keys = new ArrayList<Integer>(links.keySet());
						position = keys.get(randRange(keys.size()));
					} else {
						if (stopAtDanglingNodes) {
							break;
						}
						position = randRange(numberOfDocs);
					}
					if (useCompletePath) {
						pageRank[position]++;
					}
				}
				if (!useCompletePath) {
					pageRank[position]++;
				}
			}
		}
		double sum = Arrays.stream(pageRank).sum();
		List<Double> result = Arrays.stream(pageRank).map(x -> x / sum).boxed().collect(Collectors.toList());
		double sum2 = result.stream().mapToDouble(x->x).sum();
		assert(sum2 > 1-0.001 && sum2 < 1+0.001);
		return result;
	}

	boolean pageRankCachingAllowed = true;

	/*
	 *   Computes the pagerank of each document.
	 */
	private void computePagerank( int numberOfDocs ) {
		String pageRankFileName = "pageRank";
		String pageRankOrderFileName = "pageRankOrder";
		if (new File("pageRank").exists() && pageRankCachingAllowed) {
			System.out.println("Reading page rank from file");
			try (
					DoubleFile pageRankFile = new DoubleFile(pageRankFileName);
					IntFile pageRankOrderFile = new IntFile(pageRankOrderFileName);
					) {
				for (int i = 0; i < pageRankFile.size(); i++) {
					pageRank.add(pageRankFile.get(i));
				}
				for (int i = 0; i < pageRankOrderFile.size(); i++) {
					pageRankOrder.add(pageRankOrderFile.get(i));
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			double documentProportion = 1/(double)numberOfDocs;
			pageRank = new ArrayList<Double>(Collections.nCopies(numberOfDocs, 0.0));
			pageRank.set(0, 1.0);
			System.out.println("PGR sum: " + pageRank.stream().reduce(Double::sum));
			double c = 1-BORED;
			int iterations = 1;
			while (true) {
				StopWatch sw = new StopWatch("Page rank iteration");
				ArrayList<Double> newPageRank = new ArrayList<Double>(Collections.nCopies(numberOfDocs, 0.0));
				for (int i = 0; i < numberOfDocs; i++) {
					if (!link.containsKey(i)) {
						for (int j = 0; j < numberOfDocs; j++) {
							newPageRank.set(j, newPageRank.get(j) + documentProportion * pageRank.get(i));
						}
					} else {
						assert(link.get(i).size() > 0);
						for (int j = 0; j < numberOfDocs; j++) {
							newPageRank.set(j, newPageRank.get(j) + (documentProportion*(1-c)) * pageRank.get(i));
						}
						for (int j: link.get(i).keySet()) {
							assert(link.get(i).get(j));
							newPageRank.set(j, newPageRank.get(j) + (1/(double)out[i]*c) * pageRank.get(i));
						}
					}
				}
				double change = 0;
				for (int i = 0; i < numberOfDocs; i++) {
					change += Math.abs(pageRank.get(i) - newPageRank.get(i));
				}
				System.out.println("Iterations " + iterations + ", Error " + change);
				if (change < EPSILON) {
					break;
				}
				pageRank = newPageRank;
				System.out.println("PGR sum: " + pageRank.stream().reduce(Double::sum));
				System.out.println(sw);
				iterations++;
			}
			System.err.println("Took " + iterations + " iterations");
			List<Integer> docIDs =  IntStream.range(0, pageRank.size()).boxed().collect(Collectors.toList());
			final List<Double> pageRank2 = pageRank;
			
			docIDs.sort((o1, o2) -> pageRank2.get(o1).compareTo(pageRank2.get(o2)));
			Collections.reverse(docIDs);
			this.pageRankOrder = docIDs;
			try (
					DoubleFile pageRankFile = new DoubleFile(pageRankFileName);
					IntFile pageRankOrderFile = new IntFile(pageRankOrderFileName);
					) {
				pageRank.forEach(x->pageRankFile.add(x));
				pageRankOrder.forEach(x->pageRankOrderFile.add(x));

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		final List<Double> monteCarlo = monteCarlo(20, true, false, false);

		ArrayList<Integer> docIDs =  (ArrayList<Integer>) IntStream.range(0, monteCarlo.size()).boxed().collect(Collectors.toList());
		docIDs.sort((o1, o2) -> -monteCarlo.get(o1).compareTo(monteCarlo.get(o2)));
		for (int i = 0; i < 50; i++) {
			System.out.println(i+1 + ": " + docName[pageRankOrder.get(i)] + " " + docName[docIDs.get(i)]  + " " +pageRank.get(pageRankOrder.get(i)) + " " + monteCarlo.get(docIDs.get(i)));
		}
		double sum2 = pageRank.stream().mapToDouble(x->x).sum();
		assert(sum2 > 1-0.001 && sum2 < 1+0.001);
		System.out.println("Page rank done");
		//testMonteCarlos();
	}

	public void testMonteCarlos() {
		for (int numRuns = 1; numRuns <= 20; numRuns += 2) {
			List<List<Double>> monteCarlos = Arrays.asList(
					monteCarlo(numRuns, true, false, false),
					monteCarlo(numRuns, false, false, false),
					monteCarlo(numRuns, false, true, false),
					monteCarlo(numRuns, false, true, true),
					monteCarlo(numRuns, true, true, false)
					);

			//System.out.println("Monte carlos enters the world");
			
			for (List<Double> monteCarlo: monteCarlos) {
//				for (int i = 0; i < 10; i++) {
//					int docID = pageRankOrder.get(i);
//					System.out.println(i + ": " + docName[docID] + " " + pageRank.get(docID) + " " + monteCarlo.get(docID));
//				}
				
				double bestErr = pageRankOrder.subList(0, pageRankOrder.size()).stream().mapToDouble((x) -> Math.pow(pageRank.get(x) - monteCarlo.get(x), 2.0)).sum();
				double worstErr = pageRankOrder.subList(pageRankOrder.size()-50, pageRankOrder.size()).stream().mapToDouble(x -> Math.pow(pageRank.get(x) - monteCarlo.get(x), 2.0)).sum();
//				System.out.println("WORST " + worst);
//				System.out.println("WORSTERR " + worstErr);
				System.out.printf("%.20f,", bestErr);
//				System.out.print(Math.round(worstErr*1_000_000_000) + ",");
				

			}
			System.out.println();
		}
	}

	/* --------------------------------------------- */


	public static void main( String[] args ) {
		new PageRank("/Users/Sami/Documents/workspace2/sokmotlab1/src/ir/linksDavis.txt");
		/*
	if ( args.length != 1 ) {
	    System.err.println( "Please give the name of the link file" );
	}
	else {
	    new PageRank( args[0] );
	}
		 */
	}
}
