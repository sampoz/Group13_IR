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

    private static void addDoc(IndexWriter writer, DocumentInCollection document) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("title", document.getTitle(), Field.Store.YES));
        doc.add(new TextField("abstract", document.getAbstractText(), Field.Store.YES));
        doc.add(new IntField("searchTaskNumber", document.getSearchTaskNumber(), Field.Store.YES));
        doc.add(new TextField("query", document.getQuery(), Field.Store.YES));
        doc.add(new IntField("relevant", document.isRelevant() ? 1 : 0, Field.Store.YES));
        doc.add(new IntField("id", document.getId(), Field.Store.YES));

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
                addDoc(writer, document);
            }
            writer.close();
        }
        catch (IOException e) {
            System.out.println("Caught IOException while creating the index : " + e.getCause());
        }
    }

    public List<String> stemWords(List<String> words) {
        PorterStemmer stemmer = new PorterStemmer();
        List<String> stemmed=new ArrayList<String>();
        for(String word : words){
            stemmer.setCurrent(word);
            stemmer.stem();
            stemmed.add(stemmer.getCurrent());
        }
        return stemmed;
    }

    public TopDocs search(String query) {

        printQuery(query);

        TopDocs results = null;
        List<String> queryTermList = stemWords(Arrays.asList(query.split(" ")));

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
            results = searcher.search(masterQuery, Integer.MAX_VALUE);

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

    public void analyzeResults(List<DocumentInCollection> docs, TopDocs retrieved, String query) {
        System.out.println("Total hits: " + retrieved.totalHits);
        System.out.println("Maximum score: " + retrieved.getMaxScore());

        if (retrieved.totalHits > 0) {

            List<DocumentInCollection> relevant = getRelevantDocumentsForQuery(docs, query);

            try {
                IndexReader reader = DirectoryReader.open(directory);

                System.out.println("Precision: " + getPrecision(relevant, retrieved.scoreDocs, reader));

                List<Document> retrievedDocuments = new ArrayList<Document>();
                System.out.println("Scores and titles of the retrieved documents:");
                for (ScoreDoc sdoc : retrieved.scoreDocs) {
                    String title = reader.document(sdoc.doc).get("title");
                    System.out.println(sdoc.score + " : " + title);
                }

                reader.close();
            }
            catch (IOException e) {
                System.out.println("Caught IOException while reading the index in printResults : " + e.getCause());
            }
        }
    }

    public List<DocumentInCollection> getRelevantDocumentsForQuery(List<DocumentInCollection> docs, String query) {
        List<DocumentInCollection> relevant = new ArrayList<DocumentInCollection>();
        for (DocumentInCollection doc : docs) {
            if (doc.isRelevant() && (doc.getQuery().equals(query))) {
                relevant.add(doc);
            }
        }
        return relevant;
    }

    public float getPrecision(List<DocumentInCollection> relevant, ScoreDoc[] retrieved, IndexReader reader) {

        int hits = 0;

        try {
            for (ScoreDoc sdoc : retrieved) {
                for (DocumentInCollection doc : relevant) {
                    if (doc.getId() == Integer.parseInt(reader.document(sdoc.doc).get("id"))) {
                        hits++;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Caught IOException while reading the index in getPrecision : " + e.getCause());
        }

        return (((float)hits) / (retrieved.length));
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

            // Form the query
            String query = "social multiplayer game";

            // Search the index for the documents
            TopDocs retrieved = engine.search(query);

            // Analyze the results
            engine.analyzeResults(docs, retrieved, query);
        }
        else {
            System.out.println("ERROR: the path of a XML document has to be passed as a command line argument.");
        }
    }
}
