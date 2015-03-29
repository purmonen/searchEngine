package ir

import scala.collection.mutable
import scala.collection.immutable
import scala.collection.JavaConversions._
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.PrintWriter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.charset.Charset
import java.util.HashMap


class ScalaIndex extends Index {  
	val index = mutable.HashMap[String, PostingsList]()
			val termFrequencies = mutable.HashMap[Int, mutable.HashMap[String, Int]]()

			def intersectionSearch(query: Query): PostingsList = {
		println("Scala intersection search")
		var result = new PostingsList()
		for (term <- query.terms) {
			if (result.isEmpty()) {
				if (term != query.terms(0)) {
					return result
				}
				result = getPostings(term)
			} else {
				var newResult = new PostingsList()
				var newPostings = getPostings(term)
				var i = 0
				var j = 0
				while (i < result.size() && j < newPostings.size()) {
					if (result.get(i).docID < newPostings.get(j).docID) {
						i += 1
					} else if (result.get(i).docID > newPostings.get(j).docID) {
						j += 1
					} else {
						newResult.add(result.get(i))
						i += 1
						j += 1
					}
				}
				result = newResult
			}
		}
		return result
	}

	def phraseSearch(query: Query): PostingsList = {
		var result = new PostingsList()
		for (term <- query.terms) {
			if (result.isEmpty()) {
				if (term != query.terms(0)) {
					return result
				}
				result = getPostings(term)
			} else {
				val newResult = new PostingsList()
				val newPostings = getPostings(term)
				for (previousEntry <- result) {
					val index = newPostings.indexOf(previousEntry)
							if (index >= 0) {
								val currentEntry = newPostings.get(index)
										for (position <- previousEntry.positions) {
											if (currentEntry.positions.contains(position+1)) {
												if (newResult.isEmpty() || newResult.getLast().docID != currentEntry.docID) {
													newResult.add(new PostingsEntry(previousEntry.docID, 1337))
												}
												newResult.getLast().positions.add(position+1)
											}
										}
							}
				}
				result = newResult
			}
		}
		return result
	}


	def rankedSearch(query: Query): PostingsList = {
		val scores = mutable.HashMap[Int, Double]()
				var index = 0
				for (term <- query.terms) {
					val idf = Math.log10(getNumberOfDocuments() / getPostings(term).size().toDouble)
							for (postings <- getPostings(term)) {
								if (!scores.containsKey(postings.docID)) {
									scores(postings.docID) = 0
								}
								termFrequencies(postings.docID).size
								scores(postings.docID) += postings.positions.size() / getDocumentSize(postings.docID).toDouble * idf * query.weights(index)
							}
					index += 1
				}

		return new PostingsList(
				scores.keySet.toList.sortWith(scores(_) > scores(_)).map(x => new PostingsEntry(x, scores(x))))
	}

	def rankedSearch2(query: Query): PostingsList = {
		val minimumTermMatches = 2
				val documentMatches = new mutable.HashMap[Int, Int]()
				for (term <- query.terms) {
					for (docID <- getPostings(term).map { _.docID }) {
						if (!documentMatches.containsKey(docID)) {
							documentMatches(docID) = 0
						}
						documentMatches(docID) += 1
					}
				}
    
    for ((k,v) <- documentMatches) {
      println(k + " " + v)
    }
    
		val relevantDocuments = documentMatches.filter(_._2 >= minimumTermMatches).map(_._1)
				val scores = mutable.HashMap[Int, Double]()
				var index = 0
				for (term <- query.terms) {
					val idf = Math.log10(getNumberOfDocuments() / getPostings(term).size().toDouble)
							for (docID <- relevantDocuments) {
								val tf = termFrequencies(docID)(term)
										if (tf > 0) {
											if (!scores.containsKey(docID)) {
												scores(docID) = 0
											}
											termFrequencies(docID).size
											scores(postings.docID) += tf / getDocumentSize(docID).toDouble * idf * query.weights(index)
										}
							}
					index += 1
				}

		return new PostingsList(
				scores.keySet.toList.sortWith(scores(_) > scores(_)).map(x => new PostingsEntry(x, scores(x))))
	}

	def search(query: Query, queryType: Int, rankingType: Int, structureType: Int): PostingsList = {
		return queryType match {
		case Index.INTERSECTION_QUERY => intersectionSearch(query)
		case Index.PHRASE_QUERY => phraseSearch(query)
		case Index.RANKED_QUERY => rankedSearch2(query)
		}
	}

	def insert(token: String, docID: Int, offset: Int) {
		if (!index.containsKey(token)) {
			index(token) = new PostingsList()
		}
		var postingsList = index(token)
				if (postingsList.isEmpty() || postingsList.getLast().docID != docID) {
					postingsList.add(new PostingsEntry(docID, 1337))
				}
		postingsList.getLast().positions.add(offset)

		if (!termFrequencies.containsKey(docID)) {
			termFrequencies(docID) = new mutable.HashMap[String, Int]()
		}
		if (!termFrequencies(docID).containsKey(token)) {
			termFrequencies(docID)(token) = 0
		}
		termFrequencies(docID)(token) += 1
	}

	def getTermFrequencies(docID: Int): java.util.HashMap[String, Integer] = {
			var hashMap = new java.util.HashMap[String, Integer]
					for ((k, v) <- termFrequencies(docID)) {
						hashMap.put(k, v)
					}
			return hashMap
	}




	def getDictionary() = index.keysIterator
			def getPostings(term: String) = if (index.containsKey(term)) index(term) else new PostingsList()

	def getDocumentSize(docID: Int): Int = {
					return termFrequencies(docID).values.sum 
			}
	def getNumberOfDocuments(): Int = termFrequencies.size
			def cleanup() = {}
	def destroy() = {
		//    var parseWatch = new StopWatch("Parsing millions of integers")
		//    var sum = 0
		//    for (i <- 1 to 8804438) {
		//      sum += Integer.parseInt("11821")
		//      if (i % 100000 == 0) {
		//        println(parseWatch)
		//      }
		//    }
		//    println("Sum " + sum)
		//    println(parseWatch)
		//    
		//    var stopWatch = new StopWatch("Writing index to file")
		//    var numWords = documentSizes.values.foldLeft(0) {(x,y) => x+y}
		//    println("Num words " + numWords)
		//    var stringBuilder = new StringBuilder()
		//    for ((word, postingsList) <- index) {
		//      stringBuilder.append(" " + word + "\n")
		//      for (entry <- postingsList) {
		//        stringBuilder.append(entry.docID + "\n")
		//        for (position <- entry.positions) {
		//          stringBuilder.append(position + " ")
		//        }
		//        stringBuilder.append("\n")
		//      }
		//    }
		//    println(stopWatch)
		//    new PrintWriter(new FileOutputStream("index")).write(stringBuilder.toString())
		//    println(stopWatch)
		//    var lines = Files.readAllLines(Paths.get(".", "index"), Charset.forName("utf-8"))
		//    
		//    stopWatch = new StopWatch("Reading index from file")
		//
		//    var isDocIdLine = true
		//    var newIndex = mutable.HashMap[String, PostingsList]()
		//    var lastWord = ""
		//    for (line <- lines) {
		//      if (line.charAt(0) == ' ') {
		//         lastWord = line.trim()
		//         newIndex(lastWord) = new PostingsList()
		//      } else {
		//        if (isDocIdLine) {
		//          var docId = Integer.parseInt(line)
		//          newIndex(lastWord).add(new PostingsEntry(docId, 1337))
		//        } else {
		//          var positions = newIndex(lastWord).last.positions
		//          line.trim().split(" ").map(Integer.parseInt).foreach { position => 
		//            positions.add(position)
		//          }
		//        }
		//        isDocIdLine = !isDocIdLine
		//      }
		//    }
		//    


	}
}  