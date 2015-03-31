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
import java.util.Collections

class BiwordIndex extends ScalaIndex {
  var lastToken: String = null
  var scalaIndex = new ScalaIndex()

  override def insert(token: String, docID: Int, offset: Int) {
    scalaIndex.insert(token, docID, offset)
    if (lastToken != null) {
      super.insert(lastToken + token, docID, offset)
    }
    lastToken = token
  }
  
  override def getTermFrequencies(docID: Int): java.util.HashMap[String, Integer] = {
    if (lastStructureType == Index.BIGRAM) {
      return super.getTermFrequencies(docID)
    } else {
      return scalaIndex.getTermFrequencies(docID)
    }
  }

  var lastStructureType = Index.UNIGRAM
  override def search(query: Query, queryType: Int, rankingType: Int, structureType: Int): PostingsList = {
    lastStructureType = structureType
    
    structureType match {
      case Index.UNIGRAM => {
        return scalaIndex.search(query, queryType, rankingType, structureType)
      }
      case Index.BIGRAM => {
        var bigramQuery = new Query()
        for (i <- 1 to query.terms.size-1) {
          bigramQuery.addTerm(query.terms(i-1) + query.terms(i))
        }
        return super.search(bigramQuery, queryType, rankingType, structureType)
      }
      case Index.SUBPHRASE => {
        val minNumberOfDocuments = 10
        val uniwordResults =  scalaIndex.search(query, queryType, rankingType, Index.UNIGRAM)
        val biwordResults = search(query, queryType, rankingType, Index.BIGRAM)
        if (biwordResults.size() < minNumberOfDocuments) {
          for (entry <- uniwordResults) {
            entry.score *= 0.001
          }
          val docIDs = biwordResults.map { _.docID }
          for (entry <- uniwordResults) {
            if (!docIDs.contains(entry.docID)) {
              biwordResults.add(entry)
            }
          }
          Collections.sort(biwordResults.list)
          Collections.reverse(biwordResults.list)
        }
        return biwordResults
      }
    }
  }
}

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
    val scores = new java.util.HashMap[Int, Double]()
    for ((term, index) <- query.terms.zipWithIndex) {
      val idf = getIdf(term)
      for (postings <- getPostings(term)) {
        if (!scores.containsKey(postings.docID)) {
          scores(postings.docID) = 0
        }
        scores(postings.docID) += postings.positions.size() / getDocumentSize(postings.docID).toDouble * idf * query.weights(index) * idf
      }
    }
    return new PostingsList(
            scores.keySet.toList.sortWith(scores(_) > scores(_)).map(x => new PostingsEntry(x, scores(x))))
  }
  


  def rankedSearchFast(query: Query): PostingsList = {
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
    val relevantDocuments = documentMatches.filter(_._2 >= minimumTermMatches).map(_._1)
    
    val scores = mutable.HashMap[Int, Double]()
    for ((term, index) <- query.terms.zipWithIndex) {
      val idf = getIdf(term)
      for (docID <- relevantDocuments) {
        val tf = termFrequencies(docID).getOrElse(term, 0)
        if (tf > 0) {
          if (!scores.containsKey(docID)) {
            scores(docID) = 0
          }
          scores(docID) += tf / getDocumentSize(docID).toDouble * idf * idf * query.weights(index)
        }
      }
    }
    return new PostingsList(
            scores.keySet.toList.sortWith(scores(_) > scores(_)).map(x => new PostingsEntry(x, scores(x))))
  }
  
  def getIdf(term: String): Double = Math.log10(getNumberOfDocuments() / getPostings(term).size().toDouble)

  def time[A](f: => A): A = {
    val s = System.nanoTime
    val ret = f
    println("Time: "+(System.nanoTime-s)/1e6+"ms")
    return ret
  }

  def search(query: Query, queryType: Int, rankingType: Int, structureType: Int): PostingsList = {
    return time { queryType match {
      case Index.INTERSECTION_QUERY => intersectionSearch(query)
      case Index.PHRASE_QUERY => phraseSearch(query)
      case Index.RANKED_QUERY => rankedSearch(query)
      case Index.RANKED_FAST_QUERY => rankedSearchFast(query)
    }
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
  }
}  