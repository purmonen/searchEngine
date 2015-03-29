package ir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class DiskIndex extends BaseIndex {

	private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();
	private int wordCount = 0;
	private final int maxWordCount = 1_000_000;
	private int numBlocks = 0;
	private String directory = "index";
	private boolean rebuildIndex = true;

	String indexFileName = "index/index";
	String wordsToPostingsFileName = "index/wordsToPostings";
	String postingsFileName = "index/postings";

	public boolean deleteDir(File dir) { 
		if (dir.isDirectory()) { 
			String[] children = dir.list(); 
			for (int i=0; i<children.length; i++) { 
				boolean success = deleteDir(new File(dir, children[i])); 
				if (!success) {  
					return false; 
				} 
			} 
		}  
		return dir.delete(); 
	} 

	public DiskIndex() {
		rebuildIndex = !(new File(indexFileName).exists());
	}

	private void dumpToFile() {
		StopWatch dumpStopWatch = new StopWatch("Dumping block " + numBlocks);
		new File(directory).mkdir();
		try (
				IndexFile indexFile = new IndexFile(indexFileName + numBlocks);
				IntFile postingsFile = new IntFile(postingsFileName + numBlocks);
				IntFile wordsToPostingsFile = new IntFile(wordsToPostingsFileName + numBlocks)) {
			ArrayList<String> words = new ArrayList<String>(index.keySet());
			Collections.sort(words);
			int countPositions = 0;
			for (int i = 0; i < words.size(); i++) {
				for (PostingsEntry post: index.get(words.get(i))) {
					countPositions += post.positions.size();
				}
			}
			ByteBuffer postingsBuffer = postingsFile.getBuffer(2*countPositions);
			for (int i = 0; i < words.size(); i++) {
				String word = words.get(i);
				indexFile.add(word);
				wordsToPostingsFile.add(postingsBuffer.position() / 4);
				for (PostingsEntry post: index.get(word)) {
					for (int position: post.positions) {
						postingsBuffer.putInt(post.docID);
						postingsBuffer.putInt(position);
					}
				}
			}

			assert(wordsToPostingsFile.size() == words.size());
			internalCheck();
			index.clear();
		} catch (IOException e) {
			System.err.println("Failed to write to index file " + e.getMessage());
		}	
		numBlocks++;
		System.out.println(dumpStopWatch);
	}

	private void internalCheck() {
		try (IndexFile indexFile = new IndexFile(indexFileName)) {
			HashSet<String> words = new HashSet<String>();
			String prevWord = null;
			for (int i = 0; i < indexFile.size(); i++) {
				String word = indexFile.get(i);
				if (prevWord != null) {
					assert(word.compareTo(prevWord) > 0); 
				}
				prevWord = word; 
				assert(!words.contains(word));
				words.add(word);
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	private void mergeParts(String word, int position,
			IntFile tmpWordsToPostingsFile, IntFile tmpPostingsFile,
			IntFile wordsToPostingsFile, IntFile postingsFile) {
		int startPosition = wordsToPostingsFile.get(position);
		int endPosition = (int)postingsFile.size();
		if (position+1 < wordsToPostingsFile.size()) {
			endPosition = wordsToPostingsFile.get(position+1);
		}
		int chunkSize = 1_000_000 / 4;
		while (startPosition < endPosition) {
			tmpPostingsFile.add(postingsFile.getBytes(startPosition, Math.min(startPosition+chunkSize, endPosition)));
			startPosition += chunkSize;
		}
//		tmpPostingsFile.add(postingsFile.getBytes(startPosition, endPosition));
	}

	public void completeIndex() {
		if (!rebuildIndex) {
			return;
		}
		if (!index.isEmpty()) {
			dumpToFile();
		}
		System.out.println("Completing index");
		StopWatch completeIndexStopWatch = new StopWatch("Completing index");
		String tmpExtension = "tmp";
		for (int block = 0; block < numBlocks; block++) {
			try (
					IndexFile tmpIndexFile = new IndexFile(indexFileName+tmpExtension);
					IndexFile indexFile = new IndexFile(indexFileName);
					IndexFile blockIndexFile = new IndexFile(indexFileName+block);

					IntFile wordsToPostingsFile = new IntFile(wordsToPostingsFileName);
					IntFile tmpWordsToPostingsFile = new IntFile(wordsToPostingsFileName+tmpExtension);
					IntFile blockWordsToPostingsFile = new IntFile(wordsToPostingsFileName+block);

					IntFile postingsFile = new IntFile(postingsFileName); 
					IntFile tmpPostingsFile = new IntFile(postingsFileName+tmpExtension);
					IntFile blockPostingsFile = new IntFile(postingsFileName+block); 
					) {
				StopWatch blockStopWatch = new StopWatch("Merging block " + block);
				int i = 0;
				int j = 0;
				String lastWord = null;
				while (i < indexFile.size() && j < blockIndexFile.size()) {
					String indexWord = indexFile.get(i);
					String blockWord = blockIndexFile.get(j);
					String word = null;
					int wordComparison = indexWord.compareTo(blockWord);
					if (wordComparison == 0) {
						i++;
						j++;
						word = indexWord;
					} else if (wordComparison > 0) {
						j++;
						word = blockWord;
					} else {
						i++;
						word = indexWord;
					}
					if (word != lastWord) {
						lastWord = word;
						tmpIndexFile.add(word);
						tmpWordsToPostingsFile.add((int)tmpPostingsFile.size());
						if (indexWord.equals(word)) {
							mergeParts(word, i-1, tmpWordsToPostingsFile, tmpPostingsFile,
									wordsToPostingsFile, postingsFile);
						}
						if (blockWord.equals(word)) {
							mergeParts(word, j-1, tmpWordsToPostingsFile, tmpPostingsFile,
									blockWordsToPostingsFile, blockPostingsFile);
						}
					}
				}
				while (i < indexFile.size()) {
					String word = indexFile.get(i);
					i++;
					if (word != lastWord) {
						tmpIndexFile.add(word);
						tmpWordsToPostingsFile.add((int)tmpPostingsFile.size());
						mergeParts(word, i-1, tmpWordsToPostingsFile, tmpPostingsFile,
								wordsToPostingsFile, postingsFile);
					}
				}
				while (j < blockIndexFile.size()) {
					String word = blockIndexFile.get(j);
					j++;
					if (word != lastWord) {
						tmpIndexFile.add(word);
						tmpWordsToPostingsFile.add((int)tmpPostingsFile.size());
						mergeParts(word, j-1, tmpWordsToPostingsFile, tmpPostingsFile,
								blockWordsToPostingsFile, blockPostingsFile);
					}
				}
				System.out.println(blockStopWatch);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			for (String fileName: new String[]{indexFileName, postingsFileName, wordsToPostingsFileName}) {
				new File(fileName).delete();
				new File(fileName+block).delete();
				new File(fileName+tmpExtension).renameTo(new File(fileName));
			}
		}
		System.out.println(completeIndexStopWatch);
		internalCheck();
	}

	/**
	 *  Inserts this token in the index.
	 */
	public void insert( String token, int docID, int offset ) {
		if (!rebuildIndex) {
			return;
		}
		if (wordCount >= maxWordCount) {
			wordCount = 0;
			dumpToFile();
		}
		wordCount++;

		if (!index.containsKey(token)) {
			PostingsList postings = new PostingsList();
			index.put(token, postings);
		}
		PostingsList postings = index.get(token);	
		if (postings.size() == 0 || postings.getLast().docID != docID) {
			postings.add(new PostingsEntry(docID, 1337));
		}
		postings.getLast().positions.add(offset);
	}

	/**
	 *  Returns all the words in the index.
	 */
	public Iterator<String> getDictionary() {
		// 
		//  REPLACE THE STATEMENT BELOW WITH YOUR CODE
		//
		return null;
	}

	public PostingsList getPostings(String token) {
		PostingsList postingsList = new PostingsList();
		try (
				IndexFile indexFile = new IndexFile(indexFileName);
				IntFile wordsToPostingsFile = new IntFile(wordsToPostingsFileName);
				IntFile postingsFile = new IntFile(postingsFileName);) {
			int wordPosition = indexFile.indexOf(token);
			if (wordPosition == -1) {
				return postingsList;
			}
			int startPosition = wordsToPostingsFile.get(wordPosition);
			int endPosition = postingsFile.size();
			if (wordPosition + 1 < wordsToPostingsFile.size()) {
				endPosition = wordsToPostingsFile.get(wordPosition+1);
			}
			
			byte[] byteBuffer = postingsFile.getBytes(startPosition, endPosition);
			IntBuffer intBuffer = ByteBuffer.wrap(byteBuffer).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
			int[] array = new int[intBuffer.remaining()];
			intBuffer.get(array);
			for (int i = 0; i < array.length; i += 2) {
				int docId = array[i];
				int filePosition = array[i+1];
				if (postingsList.size() == 0 || postingsList.getLast().docID != docId) {
					postingsList.add(new PostingsEntry(docId, 0));
				}
				postingsList.getLast().positions.add(filePosition);
			}
			return postingsList;
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		return postingsList;
	}

	boolean indexingCompleted = false;

	public PostingsList search( Query query, int queryType, int rankingType, int structureType ) {
		if (!indexingCompleted) {
			indexingCompleted = true;
			completeIndex();
		}
		return super.search(query, queryType, rankingType, structureType);
	}

	/**
	 *  No need for cleanup in a HashedIndex.
	 */
	public void cleanup() {
	}
	
	public void destroy() {
		deleteDir(new File("index"));
	}
}