/*
	Note: This class has been modified to only work on short queries. The original class was designed
	to work on both long and short queries.
	Author: Nitesh Singh Jaswal
	Date: 16/11/2019
 */

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

public class searchTRECtopics {
	private String indexPath, outputPath, topicsPath;
	private IndexReader reader;
	private IndexSearcher searcher;
	public enum ENTRIES {TOP5, TOP10, TOP20, TOP100, TOP1000, ALL};
	
	public searchTRECtopics(String i, String o, String t) throws IOException {
		indexPath = i;
		outputPath = o;
		topicsPath = t;
		reader = DirectoryReader.open(FSDirectory.open(Paths
				.get(indexPath)));
		searcher = new IndexSearcher(reader);
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		String indexPath = "/home/nitesh/Study/Search/Assignment 3/Search_HW3/input/index/";
		String topicsPath = "/home/nitesh/Study/Search/Assignment 3/Search_HW3/input/topics.51-100";
		String outputPath = "/home/nitesh/Study/Search/Assignment 3/Search_HW3/out/";

		searchTRECtopics sObj = new searchTRECtopics(indexPath, outputPath, topicsPath);
		sObj.generateTopicResult(null);
	}
	
	public void generateTopicResult(Similarity sim) throws ParseException, IOException {

//		MyRocchio rObj = new MyRocchio();

		LinkedHashMap<String, Double> shortQueryMap;
		String fname = getFileName(sim);
		File file = new File(topicsPath);
		TrecTopicsReader topics = new TrecTopicsReader();

		QualityQuery[] myQueries = topics.readQueries(new BufferedReader(new FileReader(file)));
		
		String shortQueryTxt = "";
		
		for(QualityQuery query: myQueries) {
			// qId returned is of format "051" and we need "51"
			int qId = Integer.parseInt(query.getQueryID());
			/* 
			  Error was raised due to presence of "/" character
			  "Topic: " was removed from <title> query
			 */
			String titleQuery = query.getValue("title").replaceFirst("[Tt][Oo][Pp][Ii][Cc]:", "").replace("/", " ");

//			query = .getRochhioQuery(titleQuery);
//			shortQueryMap = getSimilarityScores(titleQuery, sim);
//			shortQueryTxt += toText(shortQueryMap, qId, fname);
		}
		writeToFile(outputPath + fname + "shortQuery.txt", shortQueryTxt);
	}
	
	private LinkedHashMap<String, Double> getSimilarityScores(Query query, Similarity sim) throws ParseException, IOException {
		/*
		 TODO: Modify so that getSimilarityScores() calls a myRochhio query expander object that expands the query.
		*/

		LinkedHashMap<String, Double> docScore = new LinkedHashMap<String, Double>();
		String zone = "TEXT";
		
		if(sim == null) {
			System.out.println("Pass Similarity Function. Terminating...");
			System.exit(0);
		}
		else {
			searcher.setSimilarity(sim);
			ScoreDoc[] t = searcher.search(query, 1000).scoreDocs;
			
			for(ScoreDoc d: t) {
				String docKey = searcher.doc(d.doc).get("DOCNO");
				double score = (double) d.score;
				docScore.put(docKey, score);
			}
		}
		return(docScore);
	}
	
	
	public void writeToFile(String path , String text) throws IOException {
		File file = new File(path);
		if(!file.exists()) {
			file.createNewFile();
		}
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
        bw.write(text);
        bw.close();
	}
	
	public String toText(LinkedHashMap<String, Double> docScore, int qId, String algoName) {
		String txt = "";
		int i = 1;
		for(String docId: docScore.keySet()) {
			txt += qId + " 0 " + docId + " " + i + " " + docScore.get(docId) + " " + algoName + "\n";
			++i;
		}
		return(txt);
	}
	// Function not used but may come handy
	public LinkedHashMap<String, Double> getTopX(LinkedHashMap<String, Double> scoreDoc, ENTRIES e) {
		LinkedHashMap<String, Double> topXScoreDoc;
		switch(e) {
		case TOP5:		topXScoreDoc = scoreDoc.entrySet().stream().limit(5).
											collect(LinkedHashMap::new, (k, v) -> k.put(v.getKey(), v.getValue()), LinkedHashMap::putAll);
						break;
		case TOP10: 	topXScoreDoc = scoreDoc.entrySet().stream().limit(10).
											collect(LinkedHashMap::new, (k, v) -> k.put(v.getKey(), v.getValue()), LinkedHashMap::putAll);
						break;
		case TOP20: 	topXScoreDoc = scoreDoc.entrySet().stream().limit(20).
											collect(LinkedHashMap::new, (k, v) -> k.put(v.getKey(), v.getValue()), LinkedHashMap::putAll);
						break;

		case TOP100: 	topXScoreDoc = scoreDoc.entrySet().stream().limit(100).
											collect(LinkedHashMap::new, (k, v) -> k.put(v.getKey(), v.getValue()), LinkedHashMap::putAll);
						break;
		case TOP1000: 	topXScoreDoc = scoreDoc.entrySet().stream().limit(1000).
											collect(LinkedHashMap::new, (k, v) -> k.put(v.getKey(), v.getValue()), LinkedHashMap::putAll);
						break;
		default:		System.out.println("Limit not recognized. Returning all elements");
						topXScoreDoc = scoreDoc;
		}
		return(topXScoreDoc);
	}
	
	public void printScores(LinkedHashMap<String, Double> docScore) {
		for(String docKey: docScore.keySet())
			System.out.println("DocID: " + docKey + "\tScore: " + docScore.get(docKey));
	}
	
	public String getFileName(Similarity sim) {
		String f = "";
		if(sim != null) {
			String cname = sim.getClass().getName();
			switch (cname) {
				case "org.apache.lucene.search.similarities.ClassicSimilarity":
					f = "VectorSpace";
					break;
				case "org.apache.lucene.search.similarities.BM25Similarity":
					f = "BM25";
					break;
				case "org.apache.lucene.search.similarities.LMDirichletSimilarity":
					f = "LMDrichlet";
					break;
				case "org.apache.lucene.search.similarities.LMJelinekMercerSimilarity":
					f = "LMJelinkMercer";
					break;
			}
		}
		else
			f = "EasySearch";
		return(f);
	}


}
