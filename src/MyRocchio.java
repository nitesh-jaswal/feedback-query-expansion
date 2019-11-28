import java.io.IOException;
import java.util.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import static java.util.stream.Collectors.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;

public class MyRocchio {
    private float alpha = 1.0f, beta = 1.0f, gamma = 1.0f;
    private IndexReader reader;
    private String  relevancePath;
    public enum FILTER {NONE, IDF_QUANTILE, IDF_MEAN_THRESHOLD}
    private FILTER type;
    HashMap<Query, Float> rocchioQueryMap;
    HashMap<Integer, HashMap<String, ArrayList<String>>> feedback = new HashMap<>();

    Analyzer analyzer = new StandardAnalyzer();
    QueryParser parser = new QueryParser("TEXT", analyzer);
    IDFScorer idfScorer;
    public MyRocchio(String r) throws IOException {
        relevancePath = r;
        parseRelevanceFile();
        type = FILTER.NONE;
    }

    public MyRocchio(String r, String indexPath, FILTER type_flag) throws IOException {
        relevancePath = r;
        idfScorer = new IDFScorer(indexPath, "TEXT");
        type = type_flag;
        parseRelevanceFile();
    }

    public MyRocchio(){}

    private void parseRelevanceFile() throws IOException {
        myRelevanceParser relevance_parser = new myRelevanceParser(relevancePath);
        feedback = relevance_parser.parseData();
    }

    public Query getRocchioQuery(String queryString, int qId, float a, float b, float g) throws ParseException, IOException {
        Query rocchioQuery;
        alpha = a; beta = b;  gamma = g;
//        System.out.println("Alpha = " + a + ", Beta = " + b + ", Gamma = " + g);
        HashMap<String, ArrayList<String>> query_feedback = feedback.get(qId);
//      Instantiate rocchioQueryMap every function call
        rocchioQueryMap = new HashMap<>();

        System.out.println("\nORIGINAL QUERY: " + queryString);
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

//      Calculate idf for the terms in your document if a filter is to be applied
        if(type != FILTER.NONE) {
            System.out.println("Smart Filtering...");
            Set<Query> query_term_set = rocchioQueryMap.keySet();
            Set<Query> filtered_query_term_set;
            HashMap<Query, Float> term_score_map = idfScorer.scoreCalculator(query_term_set);
            float param = (type == FILTER.IDF_QUANTILE) ? 0.8f : 5.0f;
//          Filter query term set by term_score_map
//            System.out.println("IDF MAP: " + term_score_map);

            filtered_query_term_set = filterQueryTermSet(term_score_map, param);
            for(Query query_term: query_term_set) {
                if(! filtered_query_term_set.contains(query_term)) {
                    // Quick Fix: Setting value for terms to be removed to 0 so that it can be handled by mapToQueryBoost and
                    // not included in the final boosted query. Also, using map.remove() raised errors because I could not
                    // iterate over the keyset for map and remove elements from it at the same time.
                    rocchioQueryMap.put(query_term, 0.0f);
                }
            }
        }

//      Compile map to return Query
        rocchioQuery = mapToBoostQuery();

//      Read more: https://javarevisited.blogspot.com/2017/09/java-8-sorting-hashmap-by-values-in.html#ixzz66CKnor7a
////      Print Sorted queries
//        System.out.println("Final Map = \n" + rocchioQueryMap.entrySet().stream()
//                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
//                .collect( toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new)));
//        System.out.println("BOOSTED QUERY: " + rocchioQuery);
        return(rocchioQuery);
    }


    private  void stringToQueryMap(String text, float boost) throws ParseException, IOException {
        HashMap<Query, Float> query_term_map = new HashMap<>();
        Set<Query> query_term_set;
//
        BooleanQuery query_parsed = (BooleanQuery) parser.parse(text.replaceAll("[{}`\\[\\]:()]", "").concat(" eos"));
        Iterator<BooleanClause> iter = query_parsed.iterator();
//      Creates a HashMap of unique query terms in the string. The flag decides if we want the term freq for the given document.
        while(iter.hasNext()) {
            Query query_term = iter.next().getQuery();
            query_term_map.put(query_term,1f);
        }
        query_term_set = query_term_map.keySet();

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

    private  Query mapToBoostQuery() {
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

    private Set<Query> filterQueryTermSet(HashMap<Query, Float> map, float param) {
        Set<Query> query_term_set;
        if(type == FILTER.IDF_QUANTILE) {
            assert param < 1.0f && param > 0.0f;
            LinkedHashMap<Query, Float> sorted_map = new LinkedHashMap<>();
            int limit = Math.round(map.size() * param);
            // Sort HashMap of idf descending order
            map.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .forEachOrdered(x -> sorted_map.put(x.getKey(), x.getValue()));

            // Clear original map so that it can be reused
            map.clear();
            // Limit map based on the integer limit calculated before
            map = sorted_map.entrySet().stream().limit(limit)
                    .collect( toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
            query_term_set = map.keySet();
        }
        else {
            LinkedHashMap<Query, Float> filtered_map = new LinkedHashMap<>();
            float mean = 0;
            for(Map.Entry<Query, Float> val: map.entrySet())
                mean = mean + val.getValue()/map.size();
            param = Math.round(mean);

            for(Query query_term: map.keySet()) {
                if(map.get(query_term) > param)
                    filtered_map.put(query_term, map.get(query_term));
            }
            query_term_set = filtered_map.keySet();
            System.out.println("Rounded Mean IDF (Threshold) = " + param);
        }

        return(query_term_set);
    }

    public static float roundAvoid(float value, int places) {
        float scale = (float) Math.pow(10, places);
        return(Math.round(value * scale) / scale);
    }

}