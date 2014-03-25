import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.store.Directory;
import org.apache.lucene.analysis.Analyzer;

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
                addDoc(writer, document.getTitle(), document.getAbstractText());
            }
            writer.close();
        }
        catch (IOException e) {
            System.out.println("Caught IOException while creating the index : " + e.getCause());
        }
	}

	public List<String> search(String query) {
		
		printQuery(query);

		List<String> results = new LinkedList<String>();
        List<String> queryTermList = Arrays.asList(query.split(" "));

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
            parseTermQuery("title", queryTermList, masterQuery, BooleanClause.Occur.SHOULD);
            parseTermQuery("abstract", queryTermList, masterQuery, BooleanClause.Occur.SHOULD);

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
			//Collections.sort(results);
			for (int i=0; i<results.size(); i++)
				System.out.println(" " + (i+1) + ". " + results.get(i));
		}
		else {
            System.out.println(" no results");
        }
	}

    public static List<DocumentInCollection> getRelevantDocumentsForQuery(
            List<DocumentInCollection> docs, String query) {
        List<DocumentInCollection> relevant = new ArrayList<DocumentInCollection>();
        for (DocumentInCollection doc : docs) {
            if (doc.isRelevant() && (doc.getQuery().equals(query))) {
                relevant.add(doc);
            }
        }
        return relevant;
    }

    public static float getPrecision(
            List<DocumentInCollection> docs, List<String> retrieved, String query) {

        List<DocumentInCollection> result = new ArrayList<DocumentInCollection>();
        List<DocumentInCollection> relevant = getRelevantDocumentsForQuery(docs, query);

        for (String title : retrieved) {
            for (DocumentInCollection doc : relevant) {
                if (doc.getTitle().equals(title)) {
                    result.add(doc);
                }
            }
        }

        return ((float)result.size()) / ((float)retrieved.size());
    }

	public static void main(String[] args) {
		if (args.length > 0) {
			LuceneSearchApp engine = new LuceneSearchApp();

            // Parse the documents from the XML file
			DocumentCollectionParser parser = new DocumentCollectionParser();
			parser.parse(args[0]);
			List<DocumentInCollection> docs = parser.getDocuments();

            // Select only the documents relevant to our subject
            for (Iterator<DocumentInCollection> i = docs.listIterator(); i.hasNext(); ) {
                DocumentInCollection doc = (DocumentInCollection) i.next();
                if (doc.getSearchTaskNumber() != 18) {
                    i.remove();
                }
            }

            // Index the relevant documents
			engine.index(docs);

            String query = "social multiplayer game";

		    List<String> retrieved = engine.search(query);
            System.out.println("Precision: " + getPrecision(docs, retrieved, query));
            engine.printResults(retrieved);
		}
		else {
            System.out.println("ERROR: the path of a XML document has to be passed as a command line argument.");
        }
	}
}
