import java.io.IOException;
import java.util.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import static java.util.stream.Collectors.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;

public class MyRocchio {
    private float alpha = 1.0f, beta = 1.0f, gamma = 1.0f;
    private IndexReader reader;
    private String  relevancePath;
    HashMap<Query, Float> rocchioQueryMap;
    HashMap<Integer, HashMap<String, ArrayList<String>>> feedback = new HashMap<>();

    Analyzer analyzer = new StandardAnalyzer();
    QueryParser parser = new QueryParser("TEXT", analyzer);

    public MyRocchio(String r) throws IOException {
        relevancePath = r;
        parseRelevanceFile();
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
//        MyRocchio.usingQueryBuilder(map);

//        HashMap<String, Float> sorted_map= sortByValue(map);
//        System.out.println(sorted_map);
    }

    private void parseRelevanceFile() throws IOException {
        myRelevanceParser relevance_parser = new myRelevanceParser(relevancePath);
        feedback = relevance_parser.parseData();
    }

    public Query getRocchioQuery(String queryString, int qId, float a, float b, float g) throws ParseException {
        Query rocchioQuery;
        alpha = a; beta = b;  gamma = g;
        System.out.println("Alpha = " + a + ", Beta = " + b + ", Gamma = " + g);
        HashMap<String, ArrayList<String>> query_feedback = feedback.get(qId);
        rocchioQueryMap = new HashMap<>();

//        System.out.println("\nORIGINAL QUERY: " + queryString);
//         COMPUTE Q0 SCORES
        stringToQueryMap(queryString, alpha);
//         COMPUTE REL AND IRR SCORES
        for(String doc_type: query_feedback.keySet()) {
            ArrayList<String> queryStrings = query_feedback.get(doc_type);
            int n_docs = queryStrings.size();
            float boost = doc_type.equals("REL")? (beta/n_docs) : (-gamma/n_docs);
            for(String text: queryStrings)
                stringToQueryMap(text, boost);
        }

//      Compile map to return Query
        rocchioQuery = mapToQuery();

//      Read more: https://javarevisited.blogspot.com/2017/09/java-8-sorting-hashmap-by-values-in.html#ixzz66CKnor7a
//        System.out.println("Final Map = \n" + rocchioQueryMap.entrySet().stream()
//                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
//                .collect( toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new)));
//        System.out.println("BOOSTED QUERY: " + rocchioQuery);

        return(rocchioQuery);
    }

    private  void stringToQueryMap(String text, float boost) throws ParseException {
        Set<Query> query_term_set = new HashSet<>();
        BooleanQuery query_parsed = (BooleanQuery) parser.parse(text.replaceAll("[{}`\\[\\]:()]", "").concat(" eos"));
        Iterator<BooleanClause> iter = query_parsed.iterator();
        // Creates a set of unique query terms in the string
        while(iter.hasNext()) {
            Query query_term = iter.next().getQuery();
            query_term_set.add(query_term);
        }

        // Iterates over the query_term_set and adds it to the main map
        Iterator<Query> term_iter = query_term_set.iterator();
        while(term_iter.hasNext()) {
            Query query_term = term_iter.next();
            if(rocchioQueryMap.containsKey(query_term))
                rocchioQueryMap.put(query_term, rocchioQueryMap.get(query_term) + boost);
            else
                rocchioQueryMap.put(query_term, boost);
        }
    }

    private  Query mapToQuery() {
        Query rocchioQuery;
//      Choosing an arbitrary large number as maxClauseCount limit
        BooleanQuery.setMaxClauseCount(10000);
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for(Query query_term: rocchioQueryMap.keySet()) {
//          Round boost value to 2 decimal places
            float boost = roundAvoid(rocchioQueryMap.get(query_term), 2);
//          Do not include End of file term or terms with 0 or negative boosts
            if(query_term.toString().equalsIgnoreCase("eos") || boost <= 0.0f)
                continue;
            BoostQuery boosted_query_term = new BoostQuery(query_term, boost);
            builder.add(boosted_query_term, BooleanClause.Occur.SHOULD);
        }
        rocchioQuery = builder.build();
        return (rocchioQuery);
    }

    public static float roundAvoid(float value, int places) {
        float scale = (float) Math.pow(10, places);
        return(Math.round(value * scale) / scale);
    }

}