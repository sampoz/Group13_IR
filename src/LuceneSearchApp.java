import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.store.Directory;
import org.apache.lucene.analysis.Analyzer;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class LuceneSearchApp {

    private Directory directory;

    private static void addDoc(IndexWriter writer, String title, String description, Date pubdate) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("description", description, Field.Store.YES));
        doc.add(new IntField("pubdate", new Integer(DateTools.dateToString(pubdate, DateTools.Resolution.DAY)),
                Field.Store.YES));
        writer.addDocument(doc);
    }

    private static void parseTermQuery(String field, List<String> terms, BooleanQuery masterQuery,
                                       BooleanClause.Occur modifier) {
        if (terms != null && !terms.isEmpty()) {
            for (String term : terms) {
                masterQuery.add(new TermQuery(new Term(field, term)), modifier);
            }
        }
    }

	public LuceneSearchApp() {

	}

	public void index(List<RssFeedDocument> docs) {
        this.directory = new RAMDirectory();
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_42);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_42, analyzer);

        try {
            IndexWriter writer = new IndexWriter(directory, config);
            for (RssFeedDocument document : docs) {
                addDoc(writer, document.getTitle(), document.getDescription(), document.getPubDate());
            }
            writer.close();
        }
        catch (IOException e) {
            System.out.println("Caught IOException while creating the index : " + e.getCause());
        }
	}

	public List<String> search(List<String> inTitle, List<String> notInTitle, List<String> inDescription,
                               List<String> notInDescription, String startDate, String endDate) {
		
		printQuery(inTitle, notInTitle, inDescription, notInDescription, startDate, endDate);

		List<String> results = new LinkedList<String>();

        try {
            // Open the directory and create a searcher to search the index
            DirectoryReader reader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader);

            // Create the master query
            BooleanQuery masterQuery = new BooleanQuery();

            // Parse the term queries
            parseTermQuery("title", inTitle, masterQuery, BooleanClause.Occur.MUST);
            parseTermQuery("title", notInTitle, masterQuery, BooleanClause.Occur.MUST_NOT);
            parseTermQuery("description", inDescription, masterQuery, BooleanClause.Occur.MUST);
            parseTermQuery("description", notInDescription, masterQuery, BooleanClause.Occur.MUST_NOT);

            // Parse the date range query
            Integer start = (startDate != null ? new Integer(startDate.replace("-", "")) : null);
            Integer end = (endDate != null ? new Integer(endDate.replace("-", "")) : null);
            NumericRangeQuery nrq = NumericRangeQuery.newIntRange("pubdate", start, end, true, true);
            masterQuery.add(nrq, BooleanClause.Occur.MUST);

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
	
	public void printQuery(List<String> inTitle, List<String> notInTitle, List<String> inDescription,
                           List<String> notInDescription, String startDate, String endDate) {
		System.out.print("Search (");
		if (inTitle != null) {
			System.out.print("in title: "+inTitle);
			if (notInTitle != null || inDescription != null || notInDescription != null || startDate != null
                    || endDate != null)
				System.out.print("; ");
		}
		if (notInTitle != null) {
			System.out.print("not in title: "+notInTitle);
			if (inDescription != null || notInDescription != null || startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (inDescription != null) {
			System.out.print("in description: "+inDescription);
			if (notInDescription != null || startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (notInDescription != null) {
			System.out.print("not in description: "+notInDescription);
			if (startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (startDate != null) {
			System.out.print("startDate: "+startDate);
			if (endDate != null)
				System.out.print("; ");
		}
		if (endDate != null)
			System.out.print("endDate: "+endDate);
		System.out.println("):");
	}
	
	public void printResults(List<String> results) {
		if (results.size() > 0) {
			Collections.sort(results);
			for (int i=0; i<results.size(); i++)
				System.out.println(" " + (i+1) + ". " + results.get(i));
		}
		else
			System.out.println(" no results");
	}
	
	public static void main(String[] args) {
		if (args.length > 0) {
			LuceneSearchApp engine = new LuceneSearchApp();
			
			RssFeedParser parser = new RssFeedParser();
			parser.parse(args[0]);
			List<RssFeedDocument> docs = parser.getDocuments();
			
			engine.index(docs);

			List<String> inTitle;
			List<String> notInTitle;
			List<String> inDescription;
			List<String> notInDescription;
			List<String> results;
			
			// 1) search documents with words "kim" and "korea" in the title
			inTitle = new LinkedList<String>();
			inTitle.add("kim");
			inTitle.add("korea");
			results = engine.search(inTitle, null, null, null, null, null);
			engine.printResults(results);
			
			// 2) search documents with word "kim" in the title and no word "korea" in the description
			inTitle = new LinkedList<String>();
			notInDescription = new LinkedList<String>();
			inTitle.add("kim");
			notInDescription.add("korea");
			results = engine.search(inTitle, null, null, notInDescription, null, null);
			engine.printResults(results);

			// 3) search documents with word "us" in the title, no word "dawn" in the title and word "" and ""
			// in the description
			inTitle = new LinkedList<String>();
			inTitle.add("us");
			notInTitle = new LinkedList<String>();
			notInTitle.add("dawn");
			inDescription = new LinkedList<String>();
			inDescription.add("american");
			inDescription.add("confession");
			results = engine.search(inTitle, notInTitle, inDescription, null, null, null);
			engine.printResults(results);
			
			// 4) search documents whose publication date is 2011-12-18
			results = engine.search(null, null, null, null, "2011-12-18", "2011-12-18");
			engine.printResults(results);
			
			// 5) search documents with word "video" in the title whose publication date is 2000-01-01 or later
			inTitle = new LinkedList<String>();
			inTitle.add("video");
			results = engine.search(inTitle, null, null, null, "2000-01-01", null);
			engine.printResults(results);
			
			// 6) search documents with no word "canada" or "iraq" or "israel" in the description whose publication
			// date is 2011-12-18 or earlier
			notInDescription = new LinkedList<String>();
			notInDescription.add("canada");
			notInDescription.add("iraq");
			notInDescription.add("israel");
			results = engine.search(null, null, null, notInDescription, null, "2011-12-18");
			engine.printResults(results);
		}
		else
			System.out.println("ERROR: the path of a RSS Feed file has to be passed as a command line argument.");
	}
}
