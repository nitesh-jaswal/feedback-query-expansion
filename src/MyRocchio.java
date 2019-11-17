import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
public class MyRocchio {
    private double alpha, beta, gamma;

    public MyRocchio() {
        alpha = 1;
        beta = 0.75;
        gamma = 0.15;
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
}
