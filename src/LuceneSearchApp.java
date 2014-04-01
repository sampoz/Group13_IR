import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.store.Directory;
import org.apache.lucene.analysis.Analyzer;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.FileWriter;
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


    public enum SimilarityType {
        VSM_SIMILARITY,
        BM25_SIMILARITY
    }


    public LuceneSearchApp() {

    }

    public void index(List<DocumentInCollection> docs) {
        this.directory = new RAMDirectory();
        //Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_42); // uncomment this and comment porterstemmer, if you want stop stemming
        Analyzer analyzer = new PorterStemmerAnalyzer();
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

    public TopDocs search(String query, SimilarityType similarityType) {

        printQuery(query);

        TopDocs results = null;
        List<String> queryTermList = stemWords(Arrays.asList(query.split(" ")));

        try {
            // Open the directory and create a searcher to search the index
            DirectoryReader reader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader);

            // Set the searcher to use a specific similarity type
            switch (similarityType) {
                case VSM_SIMILARITY:
                    searcher.setSimilarity(new DefaultSimilarity());
                    break;
                case BM25_SIMILARITY:
                    searcher.setSimilarity(new BM25Similarity());
                    break;
                default:
                    break;
            }

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
        // Print the number of total hits and maximum score
        System.out.println("Total hits: " + retrieved.totalHits);
        System.out.println("Maximum score: " + retrieved.getMaxScore());

        if (retrieved.totalHits > 0) {
            try {
                IndexReader reader = DirectoryReader.open(directory);

                // Get the relevant documents for this query and print the search precision and recall
                List<DocumentInCollection> relevant = getRelevantDocumentsForQuery(docs, query, 18);
                int hits = getHits(relevant, retrieved.scoreDocs, reader);
                System.out.println("Relevant hits: " + hits);
                System.out.println("Precision: " + getPrecision(retrieved.scoreDocs, hits));
                System.out.println("Recall: " + getRecall(relevant, hits));
                System.out.println("F1 score: " + getF1score(relevant, retrieved.scoreDocs, reader));

                // Print the titles and individual scores of the retrieved documents
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

    public void getPRCurveData(List<DocumentInCollection> docs, TopDocs retrieved, String query, String filePath) {
        if (retrieved.totalHits > 0) {
            try {
                System.out.println("Opening path " + filePath + " for writing.."); // DEBUG
                FileWriter file = new FileWriter(filePath);

                IndexReader reader = DirectoryReader.open(directory);

                // Get the relevant documents for this query and print the search precision and recall
                List<DocumentInCollection> relevant = getRelevantDocumentsForQuery(docs, query, 18);

                System.out.println("Writing the precision-recall data.."); // DEBUG
                List<ScoreDoc> scoreDocs = new ArrayList<ScoreDoc>();
                int hits = 0;
                for (int i = 0; i < retrieved.scoreDocs.length; i++) {
                    scoreDocs.add(retrieved.scoreDocs[i]);
                    if (isHit(relevant, Integer.parseInt(reader.document(retrieved.scoreDocs[i].doc).get("id")))) {
                        hits++;
                    }
                    float precision = getPrecision(scoreDocs, hits);
                    float recall = getRecall(relevant, hits);
                    file.write(i + " " + precision + " " + recall + "\n");
                }

                System.out.println("Data writing finished successfully.."); // DEBUG
                file.close();
                reader.close();
            }
            catch (IOException e) {
                System.out.println("Caught IOException while reading the index in printResults : " + e.getCause());
            }
        }
    }

    public List<DocumentInCollection> getRelevantDocumentsForQuery(List<DocumentInCollection> docs, String query,
                                                                   int searchTaskNumber) {
        List<DocumentInCollection> relevant = new ArrayList<DocumentInCollection>();
        for (DocumentInCollection doc : docs) {
            if (doc.isRelevant() && (doc.getQuery().equals(query)) && doc.getSearchTaskNumber() == searchTaskNumber) {
                relevant.add(doc);
            }
        }
        return relevant;
    }

    public boolean isHit(List<DocumentInCollection> relevant, int id) {
        for (DocumentInCollection doc : relevant) {
            if (doc.getId() == id) {
                return true;
            }
        }
        return false;
    }

    public int getHits(List<DocumentInCollection> relevant, ScoreDoc[] retrieved, IndexReader reader) {
        int hits = 0;

        try {
            for (ScoreDoc sdoc : retrieved) {
                if (isHit(relevant, Integer.parseInt(reader.document(sdoc.doc).get("id")))) {
                    hits++;
                }
            }
        } catch (IOException e) {
            System.out.println("Caught IOException while reading the index in getPrecision : " + e.getCause());
        }

        return hits;
    }

    public int getHits(List<DocumentInCollection> relevant, List<ScoreDoc> retrieved, IndexReader reader) {
        int hits = 0;

        try {
            for (ScoreDoc sdoc : retrieved) {
                if (isHit(relevant, Integer.parseInt(reader.document(sdoc.doc).get("id")))) {
                    hits++;
                }
            }
        } catch (IOException e) {
            System.out.println("Caught IOException while reading the index in getPrecision : " + e.getCause());
        }

        return hits;
    }

    public float getPrecision(ScoreDoc[] retrieved, int hits) {
        return (((float)hits) / (retrieved.length));
    }

    public float getPrecision(List<ScoreDoc> retrieved, int hits) {
        return (((float)hits) / (retrieved.size()));
    }

    public float getRecall(List<DocumentInCollection> relevant, int hits) {
        return (relevant.size() == 0) ? 0 : (((float)hits) / (relevant.size()));
    }

    public float getF1score(List<DocumentInCollection> relevant, ScoreDoc[] retrieved, IndexReader reader) {
        int hits = getHits(relevant, retrieved, reader);
        float precision = getPrecision(retrieved, hits);
        float recall = getRecall(relevant, hits);
        return (precision + recall == 0) ? 0 : (2*precision*recall) / (precision + recall);
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            LuceneSearchApp engine = new LuceneSearchApp();

            // Parse the documents from the XML file
            DocumentCollectionParser parser = new DocumentCollectionParser();
            parser.parse(args[0]);
            List<DocumentInCollection> docs = parser.getDocuments();

            // 4 steps to victory
            // 1. Index the relevant documents
            engine.index(docs);

            // 2. Form the queries
            List<String> queries = new ArrayList<String>();
            queries.add("social multiplayer game");
            queries.add("online gaming group competition");
            queries.add("online gaming behaviour characteristics");

            // 3. Search the index for the documents
            List<TopDocs> vsm_retrieved = new ArrayList<TopDocs>();
            List<TopDocs> bm25_retrieved = new ArrayList<TopDocs>();

            for (String query : queries) {
                vsm_retrieved.add(engine.search(query, SimilarityType.VSM_SIMILARITY));
                bm25_retrieved.add(engine.search(query, SimilarityType.BM25_SIMILARITY));
            }

            // 4. Analyze the results
            for (int i = 0; i < queries.size(); i++) {
                //engine.analyzeResults(docs, vsm_retrieved.get(i), queries.get(i));
                engine.getPRCurveData(docs, vsm_retrieved.get(i), queries.get(i), "data/vsm_results" + i + ".txt");
                //engine.analyzeResults(docs, bm25_retrieved.get(i), queries.get(i));
                engine.getPRCurveData(docs, bm25_retrieved.get(i), queries.get(i), "data/bm25_results" + i + ".txt");
            }


        }
        else {
            System.out.println("ERROR: the path of a XML document has to be passed as a command line argument.");
        }
    }
}