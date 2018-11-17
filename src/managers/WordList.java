package managers;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class handles word comparison and storage functionality.
 * 
 * @author Nius Atreides
 */
public class WordList
{
	/** The list of words. This is a linked hash map to allow this list to be sorted. **/
	protected LinkedHashMap<String,Integer> WORDS = new LinkedHashMap<String,Integer>();
	
	/**
	 * The list of alias mergers for this list.
	 * For each list, each word will be treated as if it were identical to the first word in the list.
	 */
	protected ArrayList<String[]> ALIAS_MERGES = new ArrayList<String[]>();
	
	/**
	 * The list of wildcard words for this list.
	 * For each wildcard word, any word which contains the wildcard will be treated as if it literally were
	 * the wildcard itself.
	 */
	protected ArrayList<String> WILDCARD_MERGES = new ArrayList<String>();
	
	/**
	 * Whether to automatically attempt to merge words with their plural or non-plural counterparts.
	 * See {@link #findPluralMatch(String)}.
	 */
	public boolean autoMergePlurals = true;
	
	/**
	 * Whether to automatically attempt to merge words with their conjugated or infinitive counterparts.
	 * See {@link #findConjugationMatch(String)}.
	 */
	public boolean autoMergeConjugations = true;
	
	/**
	 * Whether the list of words is currently sorted.
	 * Modifying the list will cause the WordList to assume that the list is no longer sorted.
	 * The list can be sorted with {@link #sortByCount()} or {@link #sortAlphabetically()}.
	 */
	protected boolean isSorted = false;
	
	//
	//	CONSTRUCTORS
	//
	
	/**
	 * Creates an empty WordList with the specified options for automatic pluralization and conjugation. 
	 * 
	 * @param autoMergePlurals			See {{@link #autoMergePlurals}.
	 * @param autoMergeConjugations		See {@link #autoMergeConjugations}.
	 */
	public WordList(boolean autoMergePlurals, boolean autoMergeConjugations)
	{
		this.autoMergePlurals = autoMergePlurals;
		this.autoMergeConjugations = autoMergeConjugations;
	}
	
	/** Creates an empty WordList with {@link #autoMergePlurals} and {@link #autoMergeConjugations} set to <code>true</code>. */
	public WordList(){}
	
	//
	//	MODIFICATION
	//
	
	/**
	 * Add a word to this list.
	 * If the word already exists, increment that word's count instead.
	 * 
	 * @param word	The string to register as a word.
	 */
	public synchronized void addWord(String word)
	{
		word = word.toUpperCase();
		boolean matched = false;
		
		String merged = findMergeMatch(word);
		if(merged != null)
		{
			word = merged;
			matched = true;
		}
		
		if(!matched && autoMergePlurals)
		{
			String plrl = findPluralMatch(word);
			if(plrl != null)
			{
				word = plrl;
				matched = true;
			}
		}
		
		if(!matched && autoMergeConjugations)
		{
			String conj = findConjugationMatch(word);
			if(conj != null)
			{
				word = conj;
				matched = true;
			}
		}
		
		if(WORDS.containsKey(word))
		{
			int count = WORDS.get(word);
			WORDS.put(word,count + 1);
		}
		else
			WORDS.put(word,1);
		
		isSorted = false;
	}
	
	/**
	 * Add an alias merge for this word list.
	 * See {@link #ALIAS_MERGES}.
	 * 
	 * Words already in the list will not be affected.
	 * 
	 * @param words	A list of words to be treated as identical. The word at index 0 will be the display name.
	 */
	public void addAliasMerge(String[] words)
	{
		for(int i = 0; i < words.length; i ++)
			words[i] = words[i].trim().toUpperCase();
		
		ALIAS_MERGES.add(words);
	}
	
	/**
	 * Add a wildcard merge for this word list.
	 * See {@link #WILDCARD_MERGES}.
	 * 
	 * Words already in the list will not be affected.
	 * 
	 * @param needle The term which will be matched inside any other word.
	 */
	public void addWildcardMerge(String needle)
	{
		WILDCARD_MERGES.add(needle.trim().toUpperCase());
	}
	
	//
	//	BASIC ANALYTICS
	//
	
	/**
	 * Total the occurrences of all registered words.
	 * 
	 * @return	The sum of all word counts in this list.
	 */
	public int sum()
	{
		int sum = 0;
		for(Map.Entry<String,Integer> word : WORDS.entrySet())
			sum += word.getValue();
		return sum;
	}
	
	//
	//	MATCHING
	//
	
	/**
	 * Search all merger lists for a match to this word.
	 * See {@link #WILDCARD_MERGES} and {@link #ALIAS_MERGES}.
	 * 
	 * @param word	The word to check against the merger lists.
	 * @return		If no match was found, returns <code>null</code>.
	 * 				If a match was found, returns that match instead.
	 */
	public String findMergeMatch(String word)
	{
		//Global matches first.
		for(String needle : WILDCARD_MERGES)
			if(word.contains(needle))
				return needle;
		
		//Alias matches
		for(String[] mergeList : ALIAS_MERGES)
			for(String suitor : mergeList)
				if(suitor.equals(word))
					return mergeList[0];
		
		return null;
	}
	
	/**
	 * Search the existing list for a conjugation or infinitive of the specified word.
	 * The specified word will be stripped of any conjugation and then checked against the existing list.
	 * If no match is found, then each possible conjugation will be checked.
	 * Checked conjugations include:
	 * <ul>
	 *  <li>S
	 *  <li>IES (if ending in Y)
	 *  <li>IED (if ending in Y)
	 *  <li>ED
	 *  <li>ING
	 *  <li>ER
	 * </ul>
	 * This method also understands words which have a double-letter conjugation, such as "HUMMING" or "VETTED".
	 * 
	 * @param word	THe word to check against the already-existing list.
	 * @return		If no match was found, returns <code>null</code>.
	 * 				If a match was found, returns that match instead.
	 */
	public String findConjugationMatch(String word)
	{
		//Ignore empty words.
		if(word.length() == 0)
			return null;
		
		word = word.toUpperCase();
		
		//If this word already matches, just return it.
		//On recursive calls this also checks for the infinitive.
		if(WORDS.containsKey(word))
			return word;
		
		/* Check for merge matches.
		 * Even though this happens at the begining of addWord() it must happen
		 * here to allow recursive calls of this method to match mergers. This
		 * causes merged words to to match their conjugations in addition to their
		 * literals. 
		 */
		String merged = findMergeMatch(word);
		if(merged != null)
			return merged;
		
		//Remove any existing conjugation.
		
		//Words ending in a double-letter followed by ED or ER.
		if((word.endsWith("ED") || word.endsWith("ER")) &&
			word.length() >= 5 &&
			word.charAt(word.length() - 3) == word.charAt(word.length() - 4))
		{
			String conj = findConjugationMatch(word.substring(0,word.length() - 3));
			if(conj != null)
				return conj;
		}
		//Words ending in a double-letter followed by ING.
		if( word.endsWith("ING") &&
			word.length() >= 6 &&
			word.charAt(word.length() - 4) == word.charAt(word.length() - 5))
		{
			String conj = findConjugationMatch(word.substring(0,word.length() - 4));
			if(conj != null)
				return conj;
		}
		if(word.endsWith("IES") || word.endsWith("IED"))
		{
			String conj = findConjugationMatch(word.substring(0,word.length() - 3) + "Y");
			if(conj != null)
				return conj;
		}
		if(word.endsWith("ING"))
		{
			String conj =  findConjugationMatch(word.substring(0,word.length() - 3));
			if(conj != null)
				return conj;
		}
		if(word.endsWith("ED") || word.endsWith("ER"))
		{
			String conj = findConjugationMatch(word.substring(0,word.length() - 2));
			if(conj != null)
				return conj;
		}
		if(word.endsWith("S"))
		{
			String conj = findConjugationMatch(word.substring(0,word.length() - 1));
			if(conj != null)
				return conj;
		}
		
		
		//Add each conjugation and test it.
		
		if(word.endsWith("Y"))
		{
			String test = word.substring(0,word.length() - 1);
			
			if(WORDS.containsKey(test + "IED"))
				return test;
			if(WORDS.containsKey(test + "IES"))
				return test;
		}
		if(WORDS.containsKey(word + "S"))
			return word + "S";
		if(WORDS.containsKey(word + "ED"))
			return word + "ED";
		if(WORDS.containsKey(word + "ING"))
			return word + "ING";
		if(WORDS.containsKey("ER"))
			return word + "ER";
		
		String doubled = word + word.charAt(word.length() - 1);
		if(WORDS.containsKey(doubled + "ED"))
			return doubled + "ED";
		if(WORDS.containsKey(doubled + "ING"))
			return doubled + "ING";
		if(WORDS.containsKey(doubled + "ER"))
			return doubled + "ER";
		
		return null;
	}
	
	/**
	 * Search the existing list for a plural or de-pluralized match for a given word.
	 * If the specified word ends with "S" or "ES" or "IES" but does not match an already-existing word
	 * then the WordList will attempt to find a word that matches without the tailing "S" or "ES" or "IES".
	 * If the specified word does not end with "S" or "ES" or "IES" and does not match an already-existing
	 * word then the WordList will attempt to find a word that matches with an appended "S" or "ES" or "IES".
	 * 
	 * The words "I", "IS", "ISS", "A", "AS", "ASS", "ASSES", and "ASSESS" will not be checked.
	 * 
	 * @param word	The word to check against the already-existing list.
	 * @return		If no match was found, returns <code>null</code>.
	 * 				If a match was found, returns that match instead.
	 */
	public String findPluralMatch(String word)
	{
		word = word.toUpperCase();
		
		//For matching against merged words.
		String merge;
		
		//If this word is "I", "IS", "A", or "AS" just return it.
		String[] ignore =
		{
			"A", "AS", "ASS", "ASSES", "ASSESS", "I", "IS", "ISS"
		};
		for(String ignored : ignore)
			if(word.equals(ignored))
				return null;
		
		//If this word already matches, just return it.
		if(WORDS.containsKey(word))
			return null;
		
		// DE-PLURALIZE
		
		//If this word ends with ES, replace the IES with a Y and try to match it.
		if(word.endsWith("IES"))
		{
			String test = word.substring(0,word.length() - 2) + "Y";
			if(WORDS.containsKey(test))
				return test;
			
			merge = findMergeMatch(test);
			if(merge != null)
				return merge;
		}
		
		//If this word ends with ES, strip the ES and try to match it.
		if(word.endsWith("ES"))
		{
			String test = word.substring(0,word.length() - 2);
			if(WORDS.containsKey(test))
				return test;
			
			merge = findMergeMatch(test);
			if(merge != null)
				return merge;
		}
		
		//If this word ends with S, strip the S and try to match it.
		if(word.endsWith("S"))
		{
			String test = word.substring(0,word.length() - 1);
			if(WORDS.containsKey(test))
				return test;
			
			merge = findMergeMatch(test);
			if(merge != null)
				return merge;
		}
		
		// PLURALIZE
		
		//If this word ends with Y, replace the Y with IES and try to match it.
		if(word.endsWith("Y"))
		{
			String test = word.substring(0,word.length() - 1) + "IES";
			if(WORDS.containsKey(test))
				return test;
			
			merge = findMergeMatch(test);
			if(merge != null)
				return merge;
		}
		
		//Append an S and try to match.
		if(WORDS.containsKey(word + "S"))
			return word + "S";
		merge = findMergeMatch(word + "S");
		if(merge != null)
			return merge;
		
		//Append an ES and try to match.
		if(WORDS.containsKey(word + "ES"))
			return word + "ES";
		merge = findMergeMatch(word + "ES");
		if(merge != null)
			return merge;
		
		return null;
	}
	
	//
	//	SORTING
	//
	
	/** Sort all words by count. */
	public synchronized void sortByCount()
	{
		LinkedHashMap<String,Integer> WORDS_SORTED = new LinkedHashMap<String,Integer>();
		
		while(WORDS.size() > 0)
		{
			String mostWord = "";
			int mostCount = 0;
			for(Map.Entry<String, Integer> word : WORDS.entrySet())
			{
				if(word.getValue() > mostCount)
				{
					mostCount = word.getValue();
					mostWord = word.getKey();
				}
			}
			WORDS_SORTED.put(mostWord,mostCount);
			WORDS.remove(mostWord);
		}
		
		WORDS = WORDS_SORTED;
		isSorted = true;
	}
	
	/** Sort all words alphabetically. */
	public synchronized void sortAlphabetically()
	{
		LinkedHashMap<String,Integer> WORDS_SORTED = new LinkedHashMap<String,Integer>();
		
		while(WORDS.size() > 0)
		{
			String firstWord = "";
			for(Map.Entry<String, Integer> word : WORDS.entrySet())
			{
				if(word.getKey().compareTo(firstWord) < 0)
					firstWord = word.getKey();
			}
			WORDS_SORTED.put(firstWord,WORDS.get(firstWord));
			WORDS.remove(firstWord);
		}
		
		WORDS = WORDS_SORTED;
		isSorted = true;
	}
	
	//
	//	MISCELLANEOUS
	//
	
	/**
	 * Print the entire word list to the sysem console.
	 * 
	 * @param includeCounts	Whether to print counts with the words.
	 * @param includeRanks  Whether to print ranks with the words.
	 */
	public void print(boolean includeCounts, boolean includeRanks)
	{
		int rank = 1;
		for(Map.Entry<String,Integer> word : WORDS.entrySet())
			System.out.println(rank++ + ": " + word.getKey() + (includeCounts ? ": " + word.getValue() : ""));
	}
	
	/**
	 * Print the entire word list to the provided {@link PrintWriter}. The output stream is flushed after each line.
	 * 
	 * @param out The output stream to which to write the words.
	 * @param includeCounts	Whether to print counts with the words.
	 * @param includeRanks  Whether to print ranks with the words.
	 */
	public void print(PrintWriter out, boolean includeCounts, boolean includeRanks)
	{
		int rank = 1;
		for(Map.Entry<String,Integer> word : WORDS.entrySet())
		{
			out.println(rank++ + ": " + word.getKey() + (includeCounts ? ": " + word.getValue() : ""));
			out.flush();
		}
	}
	
	/**
	 * The list is sorted if and only if there have been no modifications to the list since last time a sort method was called,
	 * even if the modification would not have been out-of-sort.
	 * 
	 * @return	Whether the list is currently in a sorted state.
	 */
	public boolean isSorted()
	{
		return isSorted;
	}
	
	/** @return The number of entries in the list. */
	public int getQuantity()
	{
		return WORDS.size();
	}
}