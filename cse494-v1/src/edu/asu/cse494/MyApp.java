/* SearchTest.java
 *
 * Copyright (c) 1997, 2000 Douglass R. Cutting.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package edu.asu.cse494;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.lucene.index.IndexReader;

/**
 * Gives a interface to ask Boolean queries on underlying index. Usage: java
 * SearchFiles
 */

public class MyApp {

	HashMap<Integer, Integer> euclDistIndex;
	HashMap<String, TermInformation> invertedTermDocIndex;
	HashMap<String, Double> idfIndex;
	HashMap<Integer, Double> euclDistWithIDFIndex;

	@SuppressWarnings("unchecked")
	public void deSerializeIdfIndex() {
		FileInputStream fis;
		try {
			System.out.println("Deserializing IDF index");
			fis = new FileInputStream("result3index/idfIndex.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			idfIndex = (HashMap<String, Double>) ois.readObject();
			System.out.println("Deserialization of IDF index completed");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void deSerializeInvertedTermDocIndex() {
		FileInputStream fis;
		try {
			System.out.println("Deserializing Inverted Term-Doc index");
			fis = new FileInputStream("result3index/invertedIndex.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			invertedTermDocIndex = (HashMap<String, TermInformation>) ois
					.readObject();
			System.out.println("Deserialization of Term-Doc index completed");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void deSerializeEuclDistIndex() {
		FileInputStream fis;
		try {
			System.out.println("Deserializing Doc Euclidean Length index");
			fis = new FileInputStream("result3index/docEuclDistIndex.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			euclDistIndex = (HashMap<Integer, Integer>) ois.readObject();
			System.out
					.println("Deserialization of Doc Euclidean Length index completed");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void deSerializeEuclDistWithIDFIndex() {
		FileInputStream fis;
		try {
			System.out
					.println("Deserializing Doc Euclidean Length {With IDF} index");
			fis = new FileInputStream(
					"result3index/docEuclDistIndexWithIDF.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			euclDistWithIDFIndex = (HashMap<Integer, Double>) ois.readObject();
			System.out
					.println("Deserialization of Doc Euclidean Length {With IDF} index completed");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void calculateRank(String queryString) {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					System.in));
			String[] queryTerms = queryString.split("\\s");
			double queryEuclLen = Math.sqrt(queryTerms.length);
			HashMap<Integer, Double> rankedDocs = new HashMap<Integer, Double>();
			System.out.println("Length of query terms : " + queryTerms.length);

			// For TF
			long startTIme = System.nanoTime();
			for (String qTerm : queryTerms) {
				TermInformation termInfo = invertedTermDocIndex.get(qTerm);
				ArrayList<DocIdFreqStore> docFreqList = termInfo
						.getDocIdFreqStoreList();
				for (DocIdFreqStore docIdFreqStore : docFreqList) {
					double prevVal = 0;
					int docId = docIdFreqStore.getDocId();
					if (rankedDocs.containsKey(docId)) {
						prevVal = rankedDocs.get(docId);
					}
					double newVal = 0;
					newVal = prevVal + docIdFreqStore.getFreq();
					rankedDocs.put(docId, newVal);
				}
			}

			ArrayList<DocScore> sortedList = new ArrayList<DocScore>();
			for (Integer key : rankedDocs.keySet()) {
				double euclLen = 0.0;
				euclLen = euclDistIndex.get(key);
				euclLen = Math.sqrt(euclLen);
				DocScore docScore = new DocScore(key, rankedDocs.get(key)
						/ (euclLen * queryEuclLen));
				sortedList.add(docScore);
			}
			Collections.sort(sortedList, new Comparator() {
				@Override
				public int compare(Object o1, Object o2) {
					DocScore docA = (DocScore) o1;
					DocScore docB = (DocScore) o2;
					double scoreDocA = docA.getScore();
					double scoreDocB = docB.getScore();
					if (scoreDocB < scoreDocA) {
						return -1;
					} else if (scoreDocB == scoreDocA) {
						return 0;
					} else {
						return 1;
					}
				}
			});
			System.out.println("Time taken to compute TF based ranking " + (System.nanoTime() - startTIme));

			// For TF-IDF
			HashMap<Integer, Double> rankedDocsWithIDF = new HashMap<Integer, Double>();
			startTIme = System.nanoTime();
			for (String qTerm : queryTerms) {
				TermInformation termInfo = invertedTermDocIndex.get(qTerm);
				ArrayList<DocIdFreqStore> docFreqList = termInfo
						.getDocIdFreqStoreList();
				double qTermIDF = idfIndex.get(qTerm);
				for (DocIdFreqStore docIdFreqStore : docFreqList) {
					double prevVal = 0;
					int docId = docIdFreqStore.getDocId();
					if (rankedDocsWithIDF.containsKey(docId)) {
						prevVal = rankedDocsWithIDF.get(docId);
					}
					double newVal = 0;
					newVal = prevVal + docIdFreqStore.getFreq() * qTermIDF;
					rankedDocsWithIDF.put(docId, newVal);
				}
			}
			ArrayList<DocScore> sortedListWithIDF = new ArrayList<DocScore>();
			for (Integer key : rankedDocsWithIDF.keySet()) {
				double euclLen = 0.0;
				euclLen = euclDistWithIDFIndex.get(key);
				euclLen = Math.sqrt(euclLen);
				DocScore docScore = new DocScore(key, rankedDocsWithIDF.get(key)
						/ (euclLen * queryEuclLen));
				sortedListWithIDF.add(docScore);
			}
			Collections.sort(sortedListWithIDF, new Comparator() {
				@Override
				public int compare(Object o1, Object o2) {
					DocScore docA = (DocScore) o1;
					DocScore docB = (DocScore) o2;
					double scoreDocA = docA.getScore();
					double scoreDocB = docB.getScore();
					if (scoreDocB < scoreDocA) {
						return -1;
					} else if (scoreDocB == scoreDocA) {
						return 0;
					} else {
						return 1;
					}
				}
			});
			
			System.out.println("Time taken to compute TF*IDF based ranking " + (System.nanoTime() - startTIme));
			IndexReader reader = IndexReader.open("result3index");
			// TF results
			System.out.println("  *******  TF Results  ******");
			System.out.println("Total number of results  : "
					+ sortedList.size());
			final int HITS_PER_PAGE = 10;
			for (int start = 0; start < sortedList.size(); start += HITS_PER_PAGE) {
				int end = Math.min(sortedList.size(), start + HITS_PER_PAGE);
				for (int i = start; i < end; i++) {
					int docId = sortedList.get(i).getDocId();
					double docScore = sortedList.get(i).getScore();
					System.out.println((i + 1)
							+ ". Doc Id : "
							+ docId
							+ "      Score : " + docScore + "  Url : "
							+ reader.document(docId).getField("url")
							.stringValue());
				}
				if (sortedList.size() > end) {
					System.out.print("more (y/n) ? ");
					String line = in.readLine();
					if (line.length() == 0 || line.charAt(0) == 'n')
						break;
				}
			}
			// TF-IDF results
			System.out.println("  *******   TF_IDF  Results  ******");
			System.out.println("Total number of results  : "
					+ sortedListWithIDF.size());
			for (int start = 0; start < sortedListWithIDF.size(); start += HITS_PER_PAGE) {
				int end = Math.min(sortedListWithIDF.size(), start
						+ HITS_PER_PAGE);
				for (int i = start; i < end; i++) {
					int docId = sortedListWithIDF.get(i).getDocId();
					double docScore = sortedListWithIDF.get(i).getScore();
					System.out.println((i + 1)
							+ ".  Doc Id : "
							+ docId
							+ "      Score : " + docScore + "  Url : "
							+ reader.document(docId).getField("url")
							.stringValue());
				}
				if (sortedListWithIDF.size() > end) {
					System.out.print("more (y/n) ? ");
					String line = in.readLine();
					if (line.length() == 0 || line.charAt(0) == 'n')
						break;
				}
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					System.in));
			MyApp myApp = new MyApp();
			System.out.println("Deserializing Indexes .  Please Wait  .....");
			myApp.deSerializeIdfIndex();
			myApp.deSerializeEuclDistIndex();
			myApp.deSerializeEuclDistWithIDFIndex();
			myApp.deSerializeInvertedTermDocIndex();
			System.out.println("Deserialization Completed");
			while (true) {
				System.out.print("Query: ");
				String queryString = in.readLine();
				queryString.trim();
				if (queryString.length() == -1 || queryString.isEmpty())
					break;
				myApp.calculateRank(queryString.toLowerCase());
			}
		} catch (Exception e) {
			System.out.println(" caught a " + e.getClass()
					+ "\n with message: " + e.getMessage());
		}
	}
}
