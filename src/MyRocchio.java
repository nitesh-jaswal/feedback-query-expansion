
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import static java.util.stream.Collectors.*;
import static java.util.Map.Entry.*;

//Read more: https://javarevisited.blogspot.com/2017/09/java-8-sorting-hashmap-by-values-in.html#ixzz66AkSSP4j

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
    private float alpha = 1.0f, beta = 0.75f, gamma = 0f;
    private IndexReader reader;
    private String  relevanceIndexPath;
    HashMap<String, HashMap<String, Float>> rocchioQueryMap = new HashMap<>();

    public MyRocchio() {

    }

    public MyRocchio(float a, float b, float g) {
        alpha = a;
         beta = b;
        gamma = g;
    }

    public static void main(String[] args) throws IOException, ParseException {
        HashMap<String, Float> map = new HashMap<>();
        map.put("testing", 1.0f);
        map.put("123", 2.0f);
        map.put("again", 3.0f);
        map.put("a.gain", 4.0f);
//        for(int i = 0; i < 100; i++)
//            map.put("" + i, (float)Math.random());
//        System.out.println(map);
        MyRocchio.usingQueryBuilder(map);

//        HashMap<String, Float> sorted_map= sortByValue(map);
//        System.out.println(sorted_map);
    }

    public String getRocchioQueryString(String queryString, HashMap<String, ArrayList<String>> query_feedback) throws ParseException {
        String rocchioQuery = "";
        int n_docs;
//         COMPUTE Q0
        generateQueryMap(queryString);
//         COMPUTE RELEVANT CENTROID
        generateQueryMap(query_feedback.get("REL"), "REL");
//         COMPUTE IRRELEVANT CENTROID
        generateQueryMap(query_feedback.get("IRR"), "IRR");

        rocchioQuery = mapToQueryString(rocchioQueryMap).trim();
        System.out.println("ORIGINAL QUERY: " + queryString);
        System.out.println("BOOSTED QUERY: " + rocchioQuery);
        return(rocchioQuery);
    }

    public Query getRocchioQuery(String queryString, HashMap<String, ArrayList<String>> query_feedback) throws ParseException {
        Query rocchioQuery;

//         COMPUTE Q0
        generateQueryMap(queryString);
//         COMPUTE RELEVANT CENTROID
        generateQueryMap(query_feedback.get("REL"), "REL");
//         COMPUTE IRRELEVANT CENTROID
        generateQueryMap(query_feedback.get("IRR"), "IRR");

        rocchioQuery = mapToQueryString(rocchioQueryMap).trim();
        System.out.println("ORIGINAL QUERY: " + queryString);
        System.out.println("BOOSTED QUERY: " + rocchioQuery);
        return(rocchioQuery);
    }

    private void generateQueryMap(String queryString) {
        HashMap<String, Float> map = new HashMap<>();
        String[] terms;
        terms = queryString.trim().split("\\s");
        for (String term: terms) {
            term = term.toLowerCase().trim().replaceAll("[,;!:'`\"(){}\\[\\]\\-]", "");
            if(term.equals("") || term.equals(" ") || term.equals("\n"))
                continue;
            map.put(term, 1.0f*alpha);
        }
        rocchioQueryMap.put("Q0", map);
    }

    private void generateQueryMap(ArrayList<String> queryStrings, String doc_type) {
        HashMap<String, Float> map = new HashMap<>();

        // Select which hyper-parameter to multiply depending on relevancy of document
        int n_docs = queryStrings.size();
        float boost_factor = (doc_type.equalsIgnoreCase("REL"))? beta/n_docs: (-gamma/n_docs);

        for(String queryString: queryStrings) {
            HashMap<String, Float> doc_map = new HashMap<>();
            String[] terms = queryString.trim().replaceAll("\\s+", " ").split("\\s");
            // TODO: Make sure to not add when same terms occour in same document but add them when same terms occour in different documents
            for (String term: terms) {
                term = term.toLowerCase().trim().replaceAll("[,;!:'`\"(){}\\[\\]\\-]", "");
                if(term.equals("") || term.equals(" ") || term.equals("\n"))
                    continue;
                doc_map.put(term, 1.0f*boost_factor);
            }

            // Input the doc_map into you main HashMap
            for (String term: doc_map.keySet()) {
                float val = doc_map.get(term);
                if(map.containsKey(term))
                    map.put(term, map.get(term) + val);
                else
                    map.put(term, val);
            }
        }
        rocchioQueryMap.put(doc_type, map);
    }

    private String mapToQueryString(HashMap<String, HashMap<String, Float>> mapHashMap) throws NullPointerException{
        String rocchioQuery = "";
        HashMap<String, Float> final_map = new HashMap<>();
        HashMap<String, Float> sorted_final_map;

        for(String key: mapHashMap.keySet()) {
            HashMap<String, Float> map = mapHashMap.get(key);
            for(String term: map.keySet()) {
                if(final_map.containsKey(term))
                    final_map.put(term, final_map.get(term) + map.get(term));
                else
                    final_map.put(term, map.get(term));
            }
        }

        sorted_final_map = sortByValue(final_map, -1);

        for(String term: sorted_final_map.keySet()){
            if(!(term.equals(" ") || term.equals("") || term == null))
                rocchioQuery += term + "^" + roundAvoid(sorted_final_map.get(term), 2) + " ";
        }
        return (rocchioQuery);
    }
//    HashMap<String, Float> map
    private static void usingQueryBuilder(HashMap<String, Float> map) throws ParseException {
//        System.out.println(map);
        Analyzer analyzer = new StandardAnalyzer();
        QueryParser qparser = new QueryParser("TEXT", analyzer);
        BooleanQuery query_parsed = (BooleanQuery) qparser.parse("testing\\{ 123! a.gain");
//        System.out.println(query_parsed.toString("TEXT"));

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        Iterator<BooleanClause> iter = query_parsed.iterator();

        while(iter.hasNext()) {
            Query query = iter.next().getQuery();
            // Select only second part of the string which is the query text (<zone>:<query text> ==> TEXT:example)
            String query_string = query.toString().split(":")[1];

            BoostQuery boosted_query = new BoostQuery(query, map.get(query_string));
            builder.add(boosted_query, BooleanClause.Occur.SHOULD);
//            System.out.println(_query_[0] + "\t" + _query_[1]);
        }
        BooleanQuery t_boost = builder.build();
        System.out.println(t_boost.toString());
    }

    public static float roundAvoid(float value, int places) {
        float scale = (float) Math.pow(10, places);
        return(Math.round(value * scale) / scale);
    }

    public HashMap<String, Float> sortByValue(HashMap<String, Float> map, int limit)
    {
        HashMap<String, Float> sorted_map = new HashMap<>();
        if(limit <= 0)
            sorted_map = map
                    .entrySet()
                    .stream()
                    .sorted(Collections.reverseOrder(comparingByValue()))
                    .collect(
                            toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2,
                                    LinkedHashMap::new));

        else
            sorted_map = map
                    .entrySet()
                    .stream()
                    .sorted(Collections.reverseOrder(comparingByValue()))
                    .collect(
                            toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2,
                                    LinkedHashMap::new))
                    .entrySet()
                    .stream().limit(limit)
                    .collect(toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2,
                            LinkedHashMap::new));

        return(sorted_map);
    }

}
