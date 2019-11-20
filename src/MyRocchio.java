import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


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

    public MyRocchio(double a, double b, double g) {
        alpha = a;
        beta = b;
        gamma = g;
    }

    public static void main(String[] args) throws IOException{

    }

    public String getRocchioQuery(String queryString, HashMap<String, ArrayList<String>> query_feedback) throws ParseException {
        String rocchioQuery = "";
        int n_docs;
        HashMap<String, HashMap<String, Double>> rocchioQueryMap = new HashMap<>();
//         COMPUTE Q0
        rocchioQueryMap.put("Q0", generateQueryMap(queryString));
//         COMPUTE RELEVANT CENTROID
        rocchioQueryMap.put("REL", generateQueryMap(query_feedback.get("REL"), "REL"));
//         COMPUTE IRRELEVANT CENTROID
        rocchioQueryMap.put("IRR", generateQueryMap(query_feedback.get("IRR"), "IRR"));

        rocchioQuery = mapToQueryString(rocchioQueryMap);
        return(rocchioQuery);
    }

    private HashMap<String, Double> generateQueryMap(String queryString) {
        HashMap<String, Double> map = new HashMap<>();
        String[] terms;
        terms = queryString.trim().split("\\s");
        for (String term: terms) {
            if(map.containsKey("term"))
                map.replace(term, map.get(term) + 1.0*alpha);
            else
                map.put(term, 1.0*alpha);
        }
        return(map);
    }

    private HashMap<String, Double> generateQueryMap(ArrayList<String> queryStrings, String doc_type) {
        HashMap<String, Double> map = new HashMap<>();
        String[] terms;
        double boost_factor = (doc_type.equalsIgnoreCase("REL"))?beta:gamma;
        int n_docs = queryStrings.size();
        for(String queryString: queryStrings) {
            terms = queryString.trim().split("\\s");
            for (String term: terms) {
                if(map.containsKey("term"))
                    map.replace(term, map.get(term) + 1.0*boost_factor/n_docs);
                else
                    map.put(term, 1.0*boost_factor/n_docs);
            }
        }
        return(map);
    }

    private String mapToQueryString(HashMap<String, HashMap<String, Double>> mapHashMap) {
        String rocchioQuery = "";

        for(String key: mapHashMap.keySet()) {
            HashMap<String, Double> map = mapHashMap.get(key);
            for(String term: map.keySet()) {

            }
        }
        return (rocchioQuery);
    }



}
