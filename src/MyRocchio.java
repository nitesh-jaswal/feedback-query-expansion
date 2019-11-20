import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

public class MyRocchio {
    private double alpha = 1, beta = 0.75, gamma = 0.15;
    private IndexReader reader;
    private String  relevanceIndexPath;

//    public MyRocchio() {
//        reader = s;
//        relevanceIndexPath = "/home/nitesh/Study/Search/Assignment 3/Search_HW3/out/relevance_index/";
//    }

    public MyRocchio(double a, double b, double g, String r) {
        alpha = a;
        beta = b;
        gamma = g;
        relevanceIndexPath = r;
    }

    public static void main(String[] args) throws IOException{
        String relevancePath = "/home/nitesh/Study/Search/Assignment 3/Search_HW3/input/feedback.51-100.txt";
        String indexPath = "/home/nitesh/Study/Search/Assignment 3/Search_HW3/input/index/";
        String topicsPath = "/home/nitesh/Study/Search/Assignment 3/Search_HW3/input/topics.51-100";

        File file = new File(relevancePath);
        TrecTopicsReader topicReader = new TrecTopicsReader();
        QualityQuery[] myQueries = topicReader.readQueries(new BufferedReader(new FileReader(file)));

        String titleQuery = myQueries[0].getValue("title");
        String relevantQuery = myQueries[0].getNames()[2];
        String irrelevantQuery = myQueries[0].getValue("irrelevant");

        System.out.println(titleQuery + "\n" + relevantQuery + "\n" + irrelevantQuery);
        System.out.println("Hi from eclipse!");
    }

    public Query getRocchioQuery(String queryString) throws ParseException {

        Analyzer analyzer = new StandardAnalyzer();
        QueryParser parser = new QueryParser("TEXT", analyzer);
        Query query = parser.parse(queryString);
        return(query);
    }



}
