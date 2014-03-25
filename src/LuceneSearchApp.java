import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.store.Directory;
import org.apache.lucene.analysis.Analyzer;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.IOException;
import java.util.*;


public class LuceneSearchApp {

    private Directory directory;

    private static void addDoc(IndexWriter writer, String title, String abstr) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("abstract", abstr, Field.Store.YES));
        writer.addDocument(doc);
    }

    private static void parseTermQuery(String field, List<String> terms, BooleanQuery masterQuery,
                                       BooleanClause.Occur modifier) {
        for (String term : terms) {
            masterQuery.add(new TermQuery(new Term(field, term)), modifier);
        }
    }

	public LuceneSearchApp() {

	}

	public void index(List<DocumentInCollection> docs) {
        this.directory = new RAMDirectory();
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_42);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_42, analyzer);

        try {
            IndexWriter writer = new IndexWriter(directory, config);
            for (DocumentInCollection document : docs) {
                // Add only the documents relevant to our subject to our index
                if (document.getSearchTaskNumber() == 18) {
                    addDoc(writer, document.getTitle(), document.getAbstractText());
                }
            }
            writer.close();
        }
        catch (IOException e) {
            System.out.println("Caught IOException while creating the index : " + e.getCause());
        }
	}
	
	public List<String> stemWords(List<String> words){
        PorterStemmer stemmer = new PorterStemmer();
        List<String> stemmed=new ArrayList<String>();
        for(String word : words){
            stemmer.setCurrent(word);
            stemmer.stem();
            stemmed.add(stemmer.getCurrent());
        }
        return stemmed;
    }

	public List<String> search(String query) {
		
		printQuery(query);

		List<String> results = new LinkedList<String>();
        List<String> queryTermList = Arrays.asList(query.split(" "));
        queryTermList=stemWords(queryTermList);

        try {
            // Open the directory and create a searcher to search the index
            DirectoryReader reader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader);

            // Set the searcher to use our VSMSimilarity
            VSMSimilarity vsmSimilarity = new VSMSimilarity();
            searcher.setSimilarity(vsmSimilarity);

            // Create the master query
            BooleanQuery masterQuery = new BooleanQuery();

            // Parse the term queries
            parseTermQuery("title", queryTermList, masterQuery, BooleanClause.Occur.MUST);
            parseTermQuery("abstract", queryTermList, masterQuery, BooleanClause.Occur.MUST);

            // Search the index
            TopDocs docs = searcher.search(masterQuery, Integer.MAX_VALUE);

            // Extract the results
            for (ScoreDoc document : docs.scoreDocs) {
                results.add(reader.document(document.doc).get("title"));
            }

            reader.close();
        }
        catch (IOException e) {
            System.out.println("Caught IOException while searching the index : " + e.getCause());
        }

		return results;
	}
	
	public void printQuery(String query) {
		System.out.println("Search (in title or abstract): " + query);
	}
	
	public void printResults(List<String> results) {
		if (results.size() > 0) {
			Collections.sort(results);
			for (int i=0; i<results.size(); i++)
				System.out.println(" " + (i+1) + ". " + results.get(i));
		}
		else {
            System.out.println(" no results");
        }
	}

    public List<DocumentInCollection> getRelevantDocumentsForQuery(
            List<DocumentInCollection> docs, String query) {
        List<DocumentInCollection> relevant = new ArrayList<DocumentInCollection>();
        for (DocumentInCollection doc : docs) {
            if (doc.getQuery() == query && doc.isRelevant()) {
                relevant.add(doc);
            }
        }
        return relevant;
    }

	public static void main(String[] args) {
		if (args.length > 0) {
			LuceneSearchApp engine = new LuceneSearchApp();
			
			DocumentCollectionParser parser = new DocumentCollectionParser();
			parser.parse(args[0]);
			List<DocumentInCollection> docs = parser.getDocuments();
			
			engine.index(docs);

		    List<String> results = engine.search("game");
			engine.printResults(results);
		}
		else {
            System.out.println("ERROR: the path of a XML document has to be passed as a command line argument.");
        }
	}
}
