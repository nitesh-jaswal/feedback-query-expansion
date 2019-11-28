/*
    Class to retrieve the IDF scores from a given index for a term or set of terms.
*/

import org.apache.lucene.index.*;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Set;

public class IDFScorer {
        private IndexReader reader;
        private int docCount;
        private TFIDFSimilarity TFIDF = new ClassicSimilarity();
        private TermsEnum indexTermsEnum;
        private String field;

        public IDFScorer(String indexPath, String f) throws IOException {
            reader = DirectoryReader.open(FSDirectory.open(Paths
                    .get(indexPath)));
            docCount = reader.maxDoc();
            field = f;
            indexTermsEnum = MultiFields.getTerms(reader, field).iterator();
        }

        public float scoreCalculator (String term_text) throws IOException {
            int df = 0;
            float idf;
            BytesRef term_bytes = new BytesRef(term_text);
            if(indexTermsEnum.seekExact(term_bytes)) {
                if(indexTermsEnum.term().bytesEquals(term_bytes))
                    df = indexTermsEnum.docFreq();
            }
            idf = TFIDF.idf(df, docCount);
            return(idf);
        }

        public  HashMap<Query, Float> scoreCalculator(Set<Query> term_set) throws IOException {
            String term_text;
            HashMap<Query, Float> idf_map = new HashMap<>();

            for(Query term: term_set) {
                term_text = term.toString().split(":")[1];
                idf_map.put(term, scoreCalculator(term_text));
            }
            return(idf_map);
        }
}
