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
	public enum ENTRIES {TOP5, TOP10, TOP20, TOP100, TOP1000, ALL};
	
	public searchTRECtopics(String i, String o, String t) throws IOException {
		indexPath = i;
		outputPath = o;
		topicsPath = t;
		reader = DirectoryReader.open(FSDirectory.open(Paths
				.get(indexPath)));
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		String indexPath = "F:\\Current Study\\Search\\Assignment 2\\retrieval-and-eval\\index";
		String outputPath = "F:\\Current Study\\Search\\Assignment 2\\retrieval-and-eval\\out\\";
		String topicsPath = "F:\\Current Study\\Search\\Assignment 2\\retrieval-and-eval\\topics.51-100";
		searchTRECtopics sObj = new searchTRECtopics(indexPath, outputPath, topicsPath);
		sObj.generateTopicResult(null);
	}
	
	public void generateTopicResult(Similarity sim) throws ParseException, IOException {
		
		LinkedHashMap<String, Double> shortQueryMap, longQueryMap;
		String fname = getFileName(sim);
		File file = new File(topicsPath);
		TrecTopicsReader topics = new TrecTopicsReader();
				
		QualityQuery myQueries[] = topics.readQueries(new BufferedReader(new FileReader(file)));
		
		String shortQueryTxt = "", longQueryTxt = "";
		
		for(QualityQuery query: myQueries) {
			// qId returned is of format "051" and we need "51"
			int qId = Integer.parseInt(query.getQueryID());
			/* 
			  Error was raised due to presence of "/" character
			  "Topic: " was removed from <title> query
			  "<smry> " was removed from <desc> query since parser was unable to parse it 
			 */
			String titleQuery = query.getValue("title").replaceFirst("[Tt][Oo][Pp][Ii][Cc]:", "").replace("/", " ");
			String descQuery = query.getValue("description").split("<smry>")[0].replace("/", " ");
			shortQueryMap = getSimilarityScores(titleQuery, sim);
			longQueryMap = getSimilarityScores(descQuery, sim);
			shortQueryTxt += toText(shortQueryMap, qId, fname);
			longQueryTxt += toText(longQueryMap, qId, fname);
		}
		writeToFile(outputPath + fname + "shortQuery.txt", shortQueryTxt);
		writeToFile(outputPath + fname + "longQuery.txt", longQueryTxt);
	}
	
	private LinkedHashMap<String, Double> getSimilarityScores(String queryString, Similarity sim) throws ParseException, IOException {
		LinkedHashMap<String, Double> docScore = new LinkedHashMap<String, Double>();
		String zone = "TEXT";
		
		if(sim == null) {
			easySearch obj = new easySearch(indexPath);
			docScore = getTopX(obj.calculateScores(zone, queryString), ENTRIES.TOP1000);
		}
		else {
			IndexSearcher searcher = new IndexSearcher(reader);
			Analyzer analyzer = new StandardAnalyzer();
			QueryParser parser = new QueryParser(zone, analyzer);
			Query query = parser.parse(queryString);
			
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
			if(cname.equals("org.apache.lucene.search.similarities.ClassicSimilarity"))
				f = "VectorSpace";
			else if(cname.equals("org.apache.lucene.search.similarities.BM25Similarity"))
				f = "BM25";
			else if(cname.equals("org.apache.lucene.search.similarities.LMDirichletSimilarity"))
				f = "LMDrichlet";
			else if(cname.equals("org.apache.lucene.search.similarities.LMJelinekMercerSimilarity"))
				f = "LMJelinkMercer";
		}
		else
			f = "EasySearch";
		return(f);
	}


}
