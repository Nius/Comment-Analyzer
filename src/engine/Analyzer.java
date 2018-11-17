package engine;

import java.io.PrintWriter;
import java.util.ArrayList;

import managers.WordList;

/**
 * This class represents a type of comment, where it is assumed that the input data
 * file may contain several columns which represent multiple different classifications
 * of comment and their authors. For example, if a single data file contained in one
 * column a list of all Front Office comments while it held in another column a list
 * of all CRS comments, there would be two Analyzers: one for each of the two comment
 * types.
 * 
 * In reality the Analyzer performs very limited word comparison. Ignored words are the
 * purview of the Analyzer but more complex operations such as conjugation and
 * pluralization are delegated by the {@link WordList} itself.
 * 
 * The primary function of the Analyzer is to provide a programatical bridge between the
 * flat data file and the meaningful relationship between lists of comments and their
 * authors. The secondary function of this class is to continually calculate simple metrics,
 * such as the average word count per comment or the length of the longest comment.
 * 
 * @author Nius Atreides
 */
public class Analyzer
{
	/** The name of the comment type. */
	public final String TYPE_NAME;
	
	/** The column index of the literal text of comments of this type. */
	public final int TEXT_INDEX;
	
	/**
	 * The column index of the names of commenters for this type.
	 * Not used internally; unly used by classes which call this object.
	 * 
	 * A negative value indicates that there are no authors for this comment type.
	 */
	public final int AUTHOR_INDEX;
	
	// Lists
	
	/** The list of words occurring in comments. */
	protected final WordList WORDS = new WordList();
	
	/** The list of authors of comments. */
	protected final WordList AUTHORS = new WordList(false, false);
	
	/** The list of ignored words. */
	protected final ArrayList<String> IGNORED = new ArrayList<String>();
	
	// Analytics
	
	/** The total number of comments. */
	protected int commentQuantity = 0;
	
	/** The length of the longest comment. */
	protected int greatestLength = 0;
	
	/** The average length of a comment. */
	protected double averageLength = 0;
	
	/** The greatest number of words in a comment. */
	protected int greatestWordCount = 0;
	
	/** The average number of words in a comment. */
	protected double averageWordCount = 0;
	
	// Constructors
	
	/**
	 * Creates a new, empty analyzer for a single type of comment.
	 * 
	 * @param type				See {@link #TYPE_NAME}.
	 * @param text_index		See {@link #TEXT_INDEX}.
	 * @param commenter_index	See {@link #AUTHOR_INDEX}.
	 */
	public Analyzer(String type,int text_index, int commenter_index)
	{
		TYPE_NAME = type;
		TEXT_INDEX = text_index;
		AUTHOR_INDEX = commenter_index;
	}
	
	// Data
	
	/**
	 * Process a comment, updating the word list and other analytics.
	 * 
	 * @param comment	The literal text of the comment.
	 * @param author	The author of the comment. If {@link #AUTHOR_INDEX} is negative then this is ignored.
	 */
	public void processComment(String comment, String author)
	{
		//Increment the total number of comments processed by this Analyzer.
		commentQuantity ++;
		
		//Determine whether this is the new longest comment.
		if(comment.length() > greatestLength)
			greatestLength = comment.length();
		
		//Recalculate the average comment length.
		averageLength += ((comment.length() - averageLength) / commentQuantity);
		
		//Replace all punctuation with spaces, except for hyphens and apostrophies.
    	String regex = "[^a-zA-Z-']";
    	comment = comment.replaceAll(regex," ");
    	
    	//Split the string on spaces, and register each word.
    	int numberOfWords = 0;
    	String[] splat = comment.toUpperCase().split(" ");
    	for(String word : splat)
    	{    		
    		//Ignore blank words
    		if(word.length() == 0)
    			continue;
    		
    		numberOfWords ++;
    		
    		if(!IGNORED.contains(word))
    			WORDS.addWord(word);
    	}
    	
    	//Determine whether this is the new most-verbose comment.
    	if(numberOfWords > greatestWordCount)
    		greatestWordCount = numberOfWords;
    	
    	//Recalculate the average number of words per comment.
    	averageWordCount += ((numberOfWords - averageWordCount) / commentQuantity);
    	
    	//Register the author, if applicable.
    	if(AUTHOR_INDEX >= 0)
    		AUTHORS.addWord(author);
	}
	
	/** Sort both the word list and author list by count. */
	public void sortByCount()
	{
		WORDS.sortByCount();
		AUTHORS.sortByCount();
	}
	
	/**
	 * Register a word that will be ignored by the analyzer.
	 * This word will be ignored verbatim; no pluralization or conjugation will be performed on it. Only words
	 * exactly matching the specified string will be ignored.
	 * 
	 * @param word	A word to be ignored by the analyzer, as if it wasn't even in the source data.
	 */
	public void ignore(String word)
	{
		IGNORED.add(word.trim().toUpperCase());
	}
	
	/**
	 * Add an alias merge for this comment type. All words in this list will be treated as if they
	 * were occurrences of the first word in this list.
	 * 
	 * Words already registered will not be affected.
	 * 
	 * @param words	A list of words to be treated as identical. All of these words will be treated as if
	 * 				they were the first word in this list.
	 */
	public void addAliasMerge(String[] words)
	{
		WORDS.addAliasMerge(words);
	}
	
	/**
	 * Add a wildcard merge for this comment type. All words which contain the specified term will
	 * be treated as if they literally were the specified term.
	 * 
	 * Words already registered will not be affected.
	 * 
	 * @param needle The term which will be matched inside any other word.
	 */
	public void addWildcardMerge(String needle)
	{
		WORDS.addWildcardMerge(needle);
	}
	
	/** Disable automatic merging of words with their conjugations. See {@link WordList#findConjugationMatch(String)}.*/
	public void disableConjugation()
	{
		WORDS.autoMergeConjugations = false;
	}
	
	/** Disable automatic merging of words with their plurals. See {@link WordList#findPluralMatch(String)}.*/
	public void disablePluralization()
	{
		WORDS.autoMergePlurals = false;
	}
	
	// Analytics
	
	/** @return The number of comments passed through {@link #processComment(String, String)}. */
	public int getNumberOfComments() { return commentQuantity; }
	
	/** @return The length of the longest comment passed through {@link #processComment(String, String)}. */
	public int getLongestCommentLength() { return greatestLength; }
	
	/** @return The average length of all comments passed through {@link #processComment(String, String)}. */
	public int getAverageCommentLength() { return (int)averageLength; }
	
	/** @return The largest quantity of words in any comment passed through {@link #processComment(String, String)}. */
	public int getHighestWordCount() { return greatestWordCount; }
	
	/** @return The average quantity of words in all comments passed through {@link #processComment(String, String)}. */
	public int getAverageWordCount() { return (int)averageWordCount; }

	/** @return The total quantity of words used in all comments passed through {@link #processComment(String, String)}. */
	public int getTotalWordCount() { return WORDS.sum(); }
	
	/** @return The total number of unique authors registered. */
	public int getNumberOfAuthors() { return AUTHORS.getQuantity(); }
	
	/** @return The total number of unique words registered. */
	public int getNumberOfWords() { return WORDS.getQuantity();	}
	
	// Printing
	
	/**
	 * Print the entire author list to the provided {@link PrintWriter}. The output stream is flushed after each line.
	 * 
	 * @param out The output stream to which to write the authors.
	 * @param includeCounts	Whether to print counts with the authors.
	 * @param includeRanks  Whether to print ranks with the words.
	 */
	public void printAuthors(PrintWriter out, boolean includeCounts, boolean includeRanks)
	{
		AUTHORS.print(out,includeCounts,includeRanks);
	}
	
	/**
	 * Print the entire word list to the provided {@link PrintWriter}. The output stream is flushed after each line.
	 * 
	 * @param out The output stream to which to write the words.
	 * @param includeCounts	Whether to print counts with the words.
	 * @param includeRanks  Whether to print ranks with the words.
	 */
	public void printWords(PrintWriter out, boolean includeCounts, boolean includeRanks)
	{
		WORDS.print(out,includeCounts,includeRanks);
	}
}