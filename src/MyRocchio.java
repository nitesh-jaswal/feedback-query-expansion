import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
import org.apache.lucene.search.*;

public class MyRocchio {
    private float alpha = 1.0f, beta = 1.0f, gamma = 0.0f;
    private IndexReader reader;
    private String  relevanceIndexPath;

    public MyRocchio() {

    }

    public MyRocchio(float a, float b, float g) {
        alpha = a;
        beta = b;
        gamma = g;
    }

    public static void main(String[] args) throws IOException{

    }

    public String getRocchioQuery(String queryString, HashMap<String, ArrayList<String>> query_feedback) throws ParseException {
        String rocchioQuery = "";
        int n_docs;
        HashMap<String, HashMap<String, Float>> rocchioQueryMap = new HashMap<>();
//         COMPUTE Q0
        rocchioQueryMap.put("Q0", generateQueryMap(queryString));
//         COMPUTE RELEVANT CENTROID
        rocchioQueryMap.put("REL", generateQueryMap(query_feedback.get("REL"), "REL"));
//         COMPUTE IRRELEVANT CENTROID
        rocchioQueryMap.put("IRR", generateQueryMap(query_feedback.get("IRR"), "IRR"));

        rocchioQuery = mapToQueryString(rocchioQueryMap).trim();
        System.out.println("ORIGINAL QUERY: " + queryString);
        System.out.println("BOOSTED QUERY: " + rocchioQuery);
        return(rocchioQuery);
    }

    private HashMap<String, Float> generateQueryMap(String queryString) {
        HashMap<String, Float> map = new HashMap<>();
        String[] terms;
        terms = queryString.trim().split("\\s");
        for (String term: terms) {
            term = term.toLowerCase().trim().replaceAll("[,;:'\"(){}]", "");
            if(term.equals("") || term.equals(" ") || term.equals("\n"))
                continue;
            map.put(term, 1.0f*alpha);
        }
        return(map);
    }

    private HashMap<String, Float> generateQueryMap(ArrayList<String> queryStrings, String doc_type) {
        HashMap<String, Float> map = new HashMap<>();
        String[] terms;
        // Select which hyper-parameter to multiply depending on relevancy of document
        Float boost_factor = (doc_type.equalsIgnoreCase("REL"))?beta:gamma;
        int n_docs = queryStrings.size();
        for(String queryString: queryStrings) {
            HashMap<String, Float> temp_map = new HashMap<>();
            terms = queryString.trim().split("\\s");
            // TODO: Make sure to not add when same terms occour in same document but add them when same terms occour in different documents
            for (String term: terms) {
                term = term.toLowerCase().trim().replaceAll("[,;:'\"(){}]", "");
                if(term.equals("") || term.equals(" ") || term.equals("\n"))
                    continue;
                temp_map.put(term, 1.0f*boost_factor/n_docs);
            }
            // Input the temp_map into you main HashMap
            for (String term: temp_map.keySet()) {
                if(map.containsKey(term))
                    map.replace(term, map.get(term) + temp_map.get(term));
                else
                    map.replace(term, temp_map.get(term));
            }
        }
        return(map);
    }

    private String mapToQueryString(HashMap<String, HashMap<String, Float>> mapHashMap) throws NullPointerException{
        String rocchioQuery = "";
        LinkedHashMap<String, Float> final_map = new LinkedHashMap<>();
        LinkedHashMap<String, Float> sorted_final_map = new LinkedHashMap<>();

        for(String key: mapHashMap.keySet()) {
            HashMap<String, Float> map = mapHashMap.get(key);
            for(String term: map.keySet()) {
                if(final_map.containsKey(term))
                    final_map.put(term, final_map.get(term) + map.get(term));
                else
                    final_map.put(term, map.get(term));
            }
        }
        sorted_final_map = final_map.entrySet().stream().limit(10).
                collect(LinkedHashMap::new, (k, v) -> k.put(v.getKey(), v.getValue()), LinkedHashMap::putAll);

        for(String term: sorted_final_map.keySet()){
            if(!(term.equals(" ") || term.equals("") || term == null))
                rocchioQuery += term + "^" + roundAvoid(sorted_final_map.get(term), 1) + " ";
        }
        return (rocchioQuery);
    }

    private void usingQueryBuilder(HashMap<String, Float> map) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        for(String term: map.keySet()) {
            BoostQuery boosted_query = new BoostQuery(new TermQuery(new Term("TEXT", term)), map.get(term));
        }

    }

    public static float roundAvoid(float value, int places) {
        float scale = (float) Math.pow(10, places);
        return(Math.round(value * scale) / scale);
    }

}
