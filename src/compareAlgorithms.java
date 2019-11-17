
import org.apache.lucene.search.similarities.ClassicSimilarity;

import java.io.IOException;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;

public class compareAlgorithms {

	public static void main(String[] args) throws IOException, ParseException {
		String indexPath = "F:\\Current Study\\Search\\Assignment 2\\retrieval-and-eval\\index";
		String outputPath = "F:\\Current Study\\Search\\Assignment 2\\retrieval-and-eval\\out\\";
		String topicsPath = "F:\\Current Study\\Search\\Assignment 2\\retrieval-and-eval\\topics.51-100";
		
		searchTRECtopics sObj = new searchTRECtopics(indexPath, outputPath, topicsPath);
		
		// EasySearch
//		sObj.generateTopicResult(null);
		System.out.println("Creating files...");
		// ClassicSimilarity
		sObj.generateTopicResult(new ClassicSimilarity());
		System.out.println("VectorSpace Done.");
		// BM25Similarity
		sObj.generateTopicResult(new BM25Similarity());
		System.out.println("BM25Similarity Done.");
		// LMDirichletSimilarity
		sObj.generateTopicResult(new LMDirichletSimilarity());
		System.out.println("LMDirichletSimilarity Done.");
		// LMJelinekMercerSimilarity
		sObj.generateTopicResult(new LMJelinekMercerSimilarity(0.7f));
		System.out.println("LMJelinekMercerSimilarity Done.");
		System.out.println("All files created.");
	}

}
