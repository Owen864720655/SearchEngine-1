/**
Program: WebCrawl.java
Date: 01/20/2001
Desc: A webcrawler to craw the links on a page recursively
Model: Based on Webcrawler.java from Sun developer pages.
Written by: Ullas Nambiar(mallu@asu.edu)
 **/

package edu.asu.cse494;

import java.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.net.*;
import java.io.*;

/**
 * Crawls the web and stores the first 1000 URLs encountered. The files opened
 * are saved into the directory from where crawler has been invoked.
 * 
 * Usage: java Webcrawl (http://SiteURL).
 */

public class Webcrawl implements Runnable {
	public static final String SEARCH = "Search";
	public static final String STOP = "Stop";
	public static final String DISALLOW = "Disallow:";
	public static final int SEARCH_LIMIT = 5;
	/** Limit on Number of URLs Opened */
	public static final int URL_OPENED = 1000;

	// URLs to be searched
	Vector vectorToSearch;
	// URLs already searched
	Vector vectorSearched;
	// URLs which match
	Vector vectorMatches;

	Thread searchThread;

	String textURL;
	long urlsearched;

	private void init() {
		// initialize search data structures
		vectorToSearch = new Vector();
		vectorSearched = new Vector();
		vectorMatches = new Vector();
		// set default for URL access
		URLConnection.setDefaultAllowUserInteraction(false);
		return;
	}

	boolean robotSafe(URL url) {
		String strHost = url.getHost();

		// form URL of the robots.txt file
		String strRobot = "http://" + strHost + "/robots.txt";
		URL urlRobot;
		try {
			urlRobot = new URL(strRobot);
		} catch (MalformedURLException e) {
			// something weird is happening, so don't trust it
			return false;
		}

		String strCommands;
		try {
			InputStream urlRobotStream = urlRobot.openStream();

			// read in entire file
			byte b[] = new byte[1000];
			int numRead = urlRobotStream.read(b);
			strCommands = new String(b, 0, numRead);
			while (numRead != -1) {
				if (Thread.currentThread() != searchThread)
					break;
				numRead = urlRobotStream.read(b);
				if (numRead != -1) {
					String newCommands = new String(b, 0, numRead);
					strCommands += newCommands;
				}
			}
			urlRobotStream.close();
		} catch (IOException e) {
			// if there is no robots.txt file, it is OK to search
			return true;
		}

		// assume that this robots.txt refers to us and
		// search for "Disallow:" commands.
		String strURL = url.getFile();
		int index = 0;
		while ((index = strCommands.indexOf(DISALLOW, index)) != -1) {
			index += DISALLOW.length();
			String strPath = strCommands.substring(index);
			StringTokenizer st = new StringTokenizer(strPath);

			if (!st.hasMoreTokens())
				break;

			String strBadPath = st.nextToken();

			// if the URL starts with a disallowed path, it is not safe
			if (strURL.indexOf(strBadPath) == 0)
				return false;
		}

		return true;
	}

	public void run() {
		// the method to Begin the new crawl at depth 0
		int depth = 0;
		// Ullas - setting the type hardcoded now
		if (textURL.length() == 0) {
			setStatus("ERROR: must enter a starting URL");
			return;
		}

		// initialize search data structures
		vectorToSearch.removeAllElements();
		vectorSearched.removeAllElements();
		vectorMatches.removeAllElements();
		vectorToSearch.addElement(textURL);
		while ((vectorToSearch.size() > 0)
				&& (Thread.currentThread() == searchThread)
				&& depth <= SEARCH_LIMIT && urlsearched <= URL_OPENED) {
			// there is a problem here in removing all the previously searched
			// ones but to make things less memory hoggin
			vectorSearched.removeAllElements();
			vectorMatches.removeAllElements();
			setStatus("inside run , to call links :" + vectorToSearch.size());
			depth = crawler(++depth);
		}
	}

	/** The crawler which extracts the links recursively and goes through them */
	public int crawler(int newdepth) {
		int numberSearched = 0;
		Vector extractedlinks = new Vector();
		String content = "";

		while ((vectorToSearch.size() > 0)
				&& (Thread.currentThread() == searchThread)
				&& newdepth <= SEARCH_LIMIT && urlsearched <= URL_OPENED) {
			// get the first element from the to be searched list
			String strURL = (String) vectorToSearch.elementAt(0);
			vectorToSearch.removeElementAt(0);

			URL url;
			try {
				url = new URL(strURL);
			} catch (MalformedURLException e) {
				setStatus("ERROR: invalid URL " + strURL);
				return newdepth;
			}

			/**
			 * from here call the other function that takes the search URL and
			 * depth and recursively call itself to traverse the path to a
			 * certain depth and at each depth it will return more URLs test to
			 * make sure it is before searching
			 * 
			 * if (!robotSafe(url)) return newdepth;
			 **/

			try {
				try {
					// try opening the URL
					URLConnection urlConnection = url.openConnection();
					urlConnection.setAllowUserInteraction(false);
					InputStream urlStream = url.openStream();
					String type = urlConnection
							.guessContentTypeFromStream(urlStream);

					urlsearched = urlsearched + 1;
					vectorSearched.addElement(strURL);
					setStatus("Open: " + strURL + "  Count: " + urlsearched
							+ " To Check: " + vectorToSearch.size()
							+ " Depth: " + newdepth);
					if (type == null)
						return newdepth;
					if (type.compareTo("text/html") != 0)
						return newdepth;

					// search the input stream for links
					// first, read in the entire URL
					byte b[] = new byte[1000];

					// storing to a file
					String tempURL = strURL;
					tempURL = tempURL.replace('/', '1');
					tempURL = tempURL.substring(7);
					if (tempURL.length() > 40)
						tempURL = tempURL.substring(tempURL.length() - 40);
					if (tempURL.startsWith("."))
						tempURL = tempURL.substring(1);
					try {
						FileWriter fpw = new FileWriter(tempURL);

						int numRead = urlStream.read(b);
						content = new String(b, 0, numRead);
						while (numRead != -1) {
							if (Thread.currentThread() != searchThread)
								return newdepth;
							numRead = urlStream.read(b);
							if (numRead != -1) {
								String newContent = new String(b, 0, numRead);
								content += newContent;
							}
						}
						fpw.write(content);
						fpw.close();
					} catch (IOException e) {
					}
					urlStream.close();

					if (Thread.currentThread() != searchThread)
						return newdepth;

					String lowerCaseContent = content.toLowerCase();

					int index = 0;

					while ((index = lowerCaseContent.indexOf("<a", index)) != -1) {
						if ((index = lowerCaseContent.indexOf("href", index)) == -1)
							break;
						if ((index = lowerCaseContent.indexOf("=", index)) == -1)
							break;

						if (Thread.currentThread() != searchThread)
							break;
						index++;
						String remaining = content.substring(index);

						StringTokenizer st = new StringTokenizer(remaining,
								"\t\n\r\">#");
						String strLink = st.nextToken();

						URL urlLink;
						try {
							urlLink = new URL(url, strLink);
							strLink = urlLink.toString();
						} catch (MalformedURLException e) {
							setStatus("ERROR: bad URL " + strLink);
							continue;
						}

						// only look at http links
						if (urlLink.getProtocol().compareTo("http") != 0)
							break;
						if (Thread.currentThread() != searchThread)
							break;
						try {
							// try opening the URL
							URLConnection urlLinkConnection = urlLink
									.openConnection();
							urlLinkConnection.setAllowUserInteraction(false);
							InputStream linkStream = urlLink.openStream();
							String strType = urlLinkConnection
									.guessContentTypeFromStream(linkStream);
							linkStream.close();

							// if another page, add to the end of search list
							if (strType == null)
								break;
							if (strType.compareTo("text/html") == 0) {
								// check to see if this URL has already been
								// searched or is going to be searched
								if ((!vectorSearched.contains(strLink))
										&& (!extractedlinks.contains(strLink))
										&& (!vectorToSearch.contains(strLink))) {

									// test to make sure it is robot-safe!
									// if(robotSafe(urlLink)) //not checking for
									// the robotsafe feature at this moment
									vectorToSearch.addElement(strLink);
									// if(extractedlinks.size() < 1000)
									extractedlinks.addElement(strLink);
								}
							}

							// if the proper type, add it to the results list
							// unless we have already seen it
							if (strType.compareTo("html") == 0) {
								if (!vectorMatches.contains(strLink)) {
									vectorMatches.addElement(strLink);

									// if (numberFound >= SEARCH_LIMIT)
									// break;
								}
							}
						} catch (IOException e) {
							setStatus("ERROR: couldn't open URL " + strLink);
							break;
						}
					}
				} catch (IOException e) {
					setStatus("ERROR: couldn't open URL " + strURL);
					return newdepth;
				}
			} catch (OutOfMemoryError e) {
				setStatus("Out of Memory Reseting");
				int size = extractedlinks.size();
				if (size > 0 && vectorToSearch.size() == 0
						&& newdepth <= SEARCH_LIMIT
						&& urlsearched <= URL_OPENED) {
					for (int iterator = 0; iterator < size; iterator++)
						vectorToSearch.addElement(extractedlinks
								.elementAt(iterator));
					newdepth = newdepth++;

				}
				vectorSearched.removeAllElements();
				vectorMatches.removeAllElements();
				extractedlinks.removeAllElements();
				return newdepth;
			}

			// now calling the next level
			int size = extractedlinks.size();
			// save top 600 to see links in the memory
			// above that should be written to a file to be seen later : add
			// Ullas
			if (size > 0 && vectorToSearch.size() == 0
					&& newdepth <= SEARCH_LIMIT) {
				for (int iterator = 0; iterator < size; iterator++)
					vectorToSearch.addElement(extractedlinks
							.elementAt(iterator));
				newdepth = newdepth++;
				extractedlinks.removeAllElements();
			}

			/**
			 * if (numberSearched >= SEARCH_LIMIT || numberFound >=
			 * SEARCH_LIMIT) setStatus("reached search limit of " +
			 * SEARCH_LIMIT); else setStatus("done"); searchThread = null;
			 **/
		}
		return newdepth;
	}

	/** Method to print. Mimics to System.out.println */
	public void setStatus(String status) {
		// labelStatus.setText(status);
		System.out.println(status);
	}

	/**
	 * Invoke the crawler from commandline. To invoke type: Webcrawl
	 * http://<SITEURL> The HTML files are stored in the directory from where it
	 * is invoked
	 */
	public static void main(String[] args) {
		// Frame f = new Frame("WebFrame");
		Webcrawl wbcrawler = new Webcrawl();
		// f.add("Center", applet);

		/*if (args.length > 0) */
			wbcrawler.textURL = "http://www.yahoo.com";
		/*else {
			System.out
					.println("\n The execution format is : java Webcraw <URL> \n");
			return;
		}*/

		/*
		 * Behind a firewall set your proxy and port here!
		 */
		Properties props = new Properties(System.getProperties());
		/*
		 * props.put("http.proxySet", "true");props.put("http.proxyHost",
		 * "webcache-cup");props.put("http.proxyPort", "8080");
		 */
		// Properties newprops = new Properties(props);
		System.setProperties(props);
		// launch a thread to do the search
		if (wbcrawler.searchThread == null) {
			wbcrawler.searchThread = new Thread(wbcrawler);
		}
		wbcrawler.init();
		wbcrawler.searchThread.start();
		// wbcrawler.myinit();
		// wbcrawler.myrun();
		/**/
		// applet.init();
		// applet.start();
		// f.pack();
		// f.show();
	}

}
