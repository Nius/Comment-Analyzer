package engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import utilities.CSVParser;

/**
 * This is the core of the Comment Analyzer program.
 * Instructions for file input and output are contained here,
 * as well as logic for interpreting the configuration file.
 * 
 * Word processing and mathematical analysis are performed by
 * {@link Analyzer} and its related classes.
 * 
 * @author Nius Atreides
 */
public class Launcher
{
	/** The current working directory of this program. */
	protected static String currentDir = System.getProperty("user.dir");
	
	/**
	 * This is the entry point for the Comment Analyzer program.
	 * 
	 * @param system_args	System arguments.
	 */
	public static void main(String[] system_args)
	{
		//This is the list of analyzers for the various comment types.
		HashMap<String,Analyzer> commentTypes = new HashMap<String,Analyzer>();
		
		//File I/O stuff.
		FileInputStream inputStream = null;
		Scanner sc = null;
		String dataPath = "";
		boolean didSpecifySource = false;
				
		//
		//	CONFIGURE
		//
		
		try
		{
			inputStream = new FileInputStream(Paths.get(currentDir + "\\config.cfg").toString());
			sc = new Scanner(inputStream, "UTF-8");
			
			boolean allTypesDone = false;
			int lineNumber = 0;
			while(sc.hasNextLine())
			{
				String line = sc.nextLine();
				lineNumber ++;
				
				//#DISABLE-CONJUGATION
				if(line.startsWith("#DISABLE-CONJUGATION"))
				{
					if(line.length() > 20)
					{
						String type = line.substring(20);
						Analyzer a = commentTypes.get(type.trim().toUpperCase());
						if(a == null)
							die("Error on line " + lineNumber + ": no comment type \"" + type.trim() + "\" has been registered.");
						a.disableConjugation();
					}
					else
						for(Map.Entry<String,Analyzer> type : commentTypes.entrySet())
							type.getValue().disableConjugation();
				}
				
				//#DISABLE-PLURALIZATION
				if(line.startsWith("#DISABLE-PLURALIZATION"))
				{
					if(line.length() > 22)
					{
						String type = line.substring(22);
						Analyzer a = commentTypes.get(type.trim().toUpperCase());
						if(a == null)
							die("Error on line " + lineNumber + ": no comment type \"" + type.trim() + "\" has been registered.");
						a.disablePluralization();
					}
					else
						for(Map.Entry<String,Analyzer> type : commentTypes.entrySet())
							type.getValue().disablePluralization();
				}
				
				//#END
				if(line.startsWith("#END"))
					break;
				
				//#IGNOREALL
				if(line.startsWith("#IGNOREALL "))
				{
					allTypesDone = true;
					
					line = line.substring(11);
					for(String word : line.split(","))
						for(Map.Entry<String,Analyzer> type : commentTypes.entrySet())
							type.getValue().ignore(word);
					
					continue;
				}
				
				//#IGNORETYPE
				if(line.startsWith("#IGNORETYPE"))
				{
					allTypesDone = true;
					
					line = line.substring(12);
					String[] args = line.split(",");
					
					Analyzer a = commentTypes.get(args[0].trim().toUpperCase());
					if(a == null)
						die("Error on line " + lineNumber + ": no comment type \"" + args[0].trim() + "\" has been registered.");
					
					for(int i = 1; i < args.length; i ++)
						a.ignore(args[i]);
					
					continue;
				}
				
				//#MERGEALL
				if(line.startsWith("#MERGEALL"))
				{
					if(allTypesDone)
						die("Error on line " + lineNumber + ": all #TYPEs must e defined before any other directive.");
					
					String[] args = line.substring(10).split(",");
					if(args.length < 2)
						die("Error on line " + lineNumber + ": expected at least 2 arguments for #MERGE but found " + args.length + ".");
					
					for(Map.Entry<String,Analyzer> type : commentTypes.entrySet())
						if(args.length == 2 && args[1].trim().equals("*"))
							type.getValue().addWildcardMerge(args[0]);
						else
							type.getValue().addAliasMerge(args);
				}
				
				//#MERGETYPE
				if(line.startsWith("#MERGETYPE"))
				{
					if(allTypesDone)
						die("Error on line " + lineNumber + ": all #TYPEs must e defined before any other directive.");
					
					String[] args = line.substring(11).split(",");
					if(args.length < 3)
						die("Error on line " + lineNumber + ": expected at least 3 arguments for #MERGE but found " + args.length + ".");
					
					Analyzer a = commentTypes.get(args[0].trim().toUpperCase());
					if(a == null)
						die("Error on line " + lineNumber + ": no comment type \"" + args[0] + "\" has been registered.");
					
					if(args.length == 3 && args[2].trim().equals("*"))
						a.addWildcardMerge(args[1]);
					else
					{
						String[] xargs = new String[args.length - 1];
						for(int i = 1; i < args.length; i ++)
							xargs[i - 1] = args[i];
						a.addAliasMerge(xargs);
					}
				}
				
				//#SOURCE
				if(line.startsWith("#SOURCE"))
				{
					if(didSpecifySource)
						die("Error on line " + lineNumber + ": only one #SOURCE may be specified.");
					
					if(!(line.length() > 8))
						die("Error on line " + lineNumber + ": the #SOURCE requires a filepath.");
					
					dataPath = line.substring(8).trim();
					
					File source = new File(dataPath);
					if(source.exists())
					{
						didSpecifySource = true;
						continue;
					}
					else
					{
						try
						{
							dataPath = Paths.get(currentDir + "/" + dataPath).toString();
						}
						catch(InvalidPathException e)
						{
							die("Error on line " + lineNumber + ": The specified data file path is invalid.");
						}
						
						source = new File(dataPath);
						if(source.exists())
						{
							didSpecifySource = true;
							continue;
						}
						else
							die("Error on line " + lineNumber + ": the specified data file does not exist.");
					}
				}
				
				//#TYPE
				if(line.startsWith("#TYPE "))
				{
					if(allTypesDone)
						die("Error on line " + lineNumber + ": all #TYPEs must e defined before any other directive (aside from #SOURCE).");
					
					String[] args = line.substring(6).split(",");
					if(args.length != 2 && args.length != 3)
						die("Error on line " + lineNumber + ": expected 2 or 3 arguments for #TYPE but found " + args.length + ".");
					try
					{
						int col = Integer.parseInt(args[1].trim());
						int usrcol = (args.length == 3 ? Integer.parseInt(args[2].trim()) : -1);
						commentTypes.put(args[0].toUpperCase(),new Analyzer(args[0].toUpperCase(),col,usrcol));
					}
					catch(NumberFormatException e)
					{
						die("Error on line " + lineNumber + ": expected an integer column number.");
					}
					
					continue;
				}
			}
			inputStream.close();
		}
		catch(FileNotFoundException e)
		{
			try
			{
				@SuppressWarnings("unused")
				long unused = Files.copy(Launcher.class.getResourceAsStream("/config.cfg"),Paths.get(currentDir + "/config.cfg"));
				System.out.println("No configuration file found.");
				System.out.println("A new configuraiton file has been generated.");
				System.out.println("Please see the new configuration file for details about how to set up the analyzer.");
				System.exit(0);
			}
			catch (IOException e1)
			{
				System.out.println("Failed to create configuration file:");
				e1.printStackTrace();
				System.exit(0);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		//
		//	READ
		//
			
		if(!didSpecifySource)
			die("Configuration error: no source file specified from which to read data.");
		
		try
		{
		    inputStream = new FileInputStream(dataPath);
		    sc = new Scanner(inputStream, "UTF-8");
		    int lineNumber = 0;
		    while (sc.hasNextLine())
		    {
		    	//Get the next line and parse it.
		    	String[] line = CSVParser.parseLine(sc.nextLine());
		    	lineNumber ++;
		    	
		    	//For each comment type...
		    	for(Map.Entry<String,Analyzer> type : commentTypes.entrySet())
		    	{
		    		//Verify that this line has the specified comment type.
		    		//If it does not, then skip this comment type.
		    		if(line.length < type.getValue().TEXT_INDEX + 1 ||
		    		   line[type.getValue().TEXT_INDEX].length() == 0)
		    			continue;
		    		
		    		//Verify that this line is not missing author information about this comment,
		    		//	if applicable.
		    		int usrcol = type.getValue().AUTHOR_INDEX;
		    		if(usrcol >= 0 &&
	    				(line.length < type.getValue().AUTHOR_INDEX + 1 ||
	    				line[type.getValue().AUTHOR_INDEX].length() == 0))
		    			die(
		    				"Error on line " + lineNumber + ": " +
		    				type.getValue().TYPE_NAME + " comments require an author but none was specified.");
		    		
		    		//Register the comment.
		    		type.getValue().processComment(
		    			line[type.getValue().TEXT_INDEX],
		    			(usrcol >= 0 ? line[type.getValue().AUTHOR_INDEX] : ""));
		    	}
		    }
		    inputStream.close();
		}
		catch(Exception e)
		{
			System.out.println("Error reading data file...");
			e.printStackTrace();
		}
		
		//
		// SORT
		//
		
		for(Map.Entry<String,Analyzer> type : commentTypes.entrySet())
			type.getValue().sortByCount();
		
		//
		//	WRITE
		//
		
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
		
		boolean isFirstType = true;
		try
		{
			PrintWriter out = new PrintWriter(new FileWriter(currentDir + "\\results.txt"));
			
			for(Map.Entry<String,Analyzer> type : commentTypes.entrySet())
			{
				if(isFirstType)
					isFirstType = false;
				else
					out.println();
				
				Analyzer a = type.getValue();
				out.println("Comment Type: " + type.getKey());
				out.println("Total number of comments: " + nf.format(a.getNumberOfComments()));
				out.println("Average length: " + nf.format(a.getAverageCommentLength()));
				out.println("Longest comment: " + nf.format(a.getLongestCommentLength()) + " characters.");
				out.println("Average word count: " + nf.format(a.getAverageWordCount()));
				out.println("Highest word count: " + nf.format(a.getHighestWordCount()));
				out.println();
				if(a.AUTHOR_INDEX >= 0)
				{
					out.println("-- AUTHORS -- ");
					out.println();
					out.println("Total unique authors: " + nf.format(a.getNumberOfAuthors()));
					out.println();
					a.printAuthors(out,true,true);
					out.println();
				}
				out.println("-- WORDS --");
				out.println();
				out.println("Total unique words: " + nf.format(a.getNumberOfWords()));
				out.println("Total words: " + nf.format(a.getTotalWordCount()));
				out.println();
				a.printWords(out,true,true);
				
				out.flush();
			}
			
			out.close();
		}
		catch (IOException e)
		{
			System.out.println("Error writing to results file...");
			e.printStackTrace();
		}
	}
	
	/**
	 * Terminate the program after writing an error message to file.
	 * The error message is also printed to the system console.
	 * 
	 * @param message The message to print to file before exiting.
	 */
	protected static void die(String message)
	{
		System.out.println(message);
		
		try
		{
			PrintWriter out = new PrintWriter(new FileWriter(currentDir + "\\results.txt"));
			out.print(message);
			out.flush();
			out.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		System.exit(0);
	}
}