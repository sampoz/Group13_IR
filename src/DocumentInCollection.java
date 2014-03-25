/*
 * Class for representing a document in a document collection
 * Created on 2012-01-04
 * Jouni Tuominen <jouni.tuominen@aalto.fi>
 */

public class DocumentInCollection {

	private static int documentIndex = 0;

    private String title;
	private String abstractText;
	private int searchTaskNumber;
	private String query;
	private boolean relevant;
    private int id;
	
	public DocumentInCollection() {
		this(null, null, 0, null, false);
	}
	
	public DocumentInCollection(String title, String abstractText, int searchTaskNumber, String query, boolean relevant) {
        this.id = documentIndex++;
		this.title = title;
		this.abstractText = abstractText;
		this.searchTaskNumber = searchTaskNumber;
		this.query = query;
		this.relevant = relevant;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAbstractText() {
		return abstractText;
	}

	public void setAbstractText(String abstractText) {
		this.abstractText = abstractText;
	}

	public int getSearchTaskNumber() {
		return searchTaskNumber;
	}

	public void setSearchTaskNumber(int searchTaskNumber) {
		this.searchTaskNumber = searchTaskNumber;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public boolean isRelevant() {
		return relevant;
	}

	public void setRelevant(boolean relevant) {
		this.relevant = relevant;
	}

	public String toString() {
		return "Title: "+title+"\n abstract: "+abstractText+"\n search task number: "+searchTaskNumber+"\n query: "+query+"\n relevant: "+relevant;
	}

    public int getId() { return id; }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        DocumentInCollection other = (DocumentInCollection) obj;
        return this.id == other.id;
    }
}