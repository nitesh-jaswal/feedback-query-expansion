/*
* Class to parse the relevance file and return the documents in the form of HashMaps
*/
import java.io.File;
import java.io.IOException;
import java.util.*;

public class myRelevanceParser {
    private String relevancePath;

    public myRelevanceParser(String r) {
        relevancePath = r;
    }

    public HashMap<Integer, HashMap<String, ArrayList<String>>> parseData() throws IOException{
        /*
            NOTE:
            Parsing discussed with: Ankit Mathur(anmath@iu.edu)
         */
        HashMap<Integer, HashMap<String, ArrayList<String>>> output = new HashMap<Integer, HashMap<String, ArrayList<String>>>();
        File file = new File(relevancePath);

        Scanner main_sc = new Scanner(file).useDelimiter("</top>\\n");
        while(main_sc.hasNext()) {
            int id;
            HashMap<String, ArrayList<String>> topic = new HashMap<String, ArrayList<String>>();
            ArrayList<String> rel_doc = new ArrayList<String>();
            ArrayList<String> irr_doc = new ArrayList<String>();

            String raw_text = main_sc.next();
            Queue<String> map_key = getMapKey(raw_text);
            Scanner sc = new Scanner(raw_text).useDelimiter("(<top>\\n<num>)|(<title>.*\n<relevant>)|(<title>.*\n<irrelevant>)|(<relevant>)" +
                    "|<irrelevant>");
            id = Integer.parseInt(sc.next().trim());
            map_key.remove();

            while(sc.hasNext()) {
                String element = sc.next();
                String key = map_key.remove();

                if(key.equals("REL"))
                    rel_doc.add(element);
                else if(key.equals("IRR"))
                    irr_doc.add(element);
            }
            topic.put("REL", rel_doc);
            topic.put("IRR", irr_doc);
            output.put(id, topic);

            sc.close();
        }
        main_sc.close();
        return (output);
    }


    public Queue<String> getMapKey(String text) {
        Queue<String> map_key = new LinkedList<>();
        map_key.add("ID");
        Scanner sc = new Scanner(text);
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if(line.startsWith("<relevant>"))
                map_key.add("REL");
            else if(line.startsWith("<irrelevant>"))
                map_key.add("IRR");
        }

        sc.close();
        return(map_key);
    }

}
