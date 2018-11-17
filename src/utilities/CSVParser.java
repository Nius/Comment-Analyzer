package utilities;

import java.util.ArrayList;

/**
 * A utility for parsing CSV-formatted information.
 * 
 * @author Nius Atreides
 */
public abstract class CSVParser
{
	/**
	 * Read a string representing a single row, and divide it into cells according to CSV formatting.
	 * 
	 * @param input	A string representing a single CSV row.
	 * @return		An array of strings representing the contents of various cells.
	 */
	public static String[] parseLine(String input)
	{
		boolean quote = false;
		int cellStartIndex = 0;
		ArrayList<String> cells = new ArrayList<String>();
		
		//Iterate through the whole line...
		for(int i = 0; i < input.length(); i ++)
		{
			//Toggle whether iterating through a quoted block or not. Escaped quotes will be ignored this way.
			if(input.charAt(i) == '"')
				quote = !quote;
			
			//Ignore commas inside quote blocks. Any other commas denote cell delimitations.
			if(i == input.length() - 1 || (!quote && input.charAt(i) == ','))
			{
				String cell = input.substring(cellStartIndex,i);
				
				//If there are quotes at all, the first and last character will be control quotes and should be removed.
				if(cell.startsWith("\""))
					cell = cell.substring(1,cell.length());
				
				//De-escape all escaped (doubled) quotes in the cell string.
				cell = cell.replaceAll("\"\"","\"");
				
				cells.add(cell);
				
				cellStartIndex = i + 1;
			}
		}
		
		//Convert the ArrayList to a primitive Array.
		String[] output = new String[cells.size()];
		for(int i = 0; i < cells.size(); i ++)
			output[i] = cells.get(i);
		
		return output;
	}
}
