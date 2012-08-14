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
import com.lucene.index.Term;
import com.lucene.index.TermDocs;

/**
 * Gives a interface to ask Boolean queries on underlying index. Usage: java
 * SearchFiles
 */

public class MySearchEngine {

	HashMap<Integer, Integer> euclDistIndex;
	HashMap<String, Double> idfIndex;
	HashMap<Integer, Double> euclDistWithIDFIndex;
	HashMap<Integer, Double> pageRank;
	ArrayList<DocScore> tfResults;
	ArrayList<DocScore> tfIdfResults;

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
			IndexReader reader = IndexReader.open("result3index");
			BufferedReader in = new BufferedReader(new InputStreamReader(
					System.in));
			String[] queryTerms = queryString.split("\\s");
			double queryEuclLen = Math.sqrt(queryTerms.length);
			HashMap<Integer, Double> rankedDocs = new HashMap<Integer, Double>();
			System.out.println("Length of query terms : " + queryTerms.length);

			// For TF
			long startTime = System.nanoTime();
			for (String qTerm : queryTerms) {
				Term term = new Term("contents", qTerm);
				TermDocs termDocs = reader.termDocs(term);
				while (termDocs.next()) {
					int docId = termDocs.doc();
					double prevVal = 0;
					if (rankedDocs.containsKey(docId)) {
						prevVal = rankedDocs.get(docId);
					}
					double newVal = prevVal + termDocs.freq();
					rankedDocs.put(termDocs.doc(), newVal);
				}
			}
			// Sorting TF results
			tfResults = new ArrayList<DocScore>();
			for (Integer key : rankedDocs.keySet()) {
				double euclLen = 0.0;
				euclLen = euclDistIndex.get(key);
				euclLen = Math.sqrt(euclLen);
				DocScore docScore = new DocScore(key, rankedDocs.get(key)
						/ (euclLen * queryEuclLen));
				tfResults.add(docScore);
			}
			Collections.sort(tfResults, new Comparator() {
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
			System.out.println("Time taken to compute TF based ranking "
					+ (System.nanoTime() - startTime));

			// For TF-IDF
			HashMap<Integer, Double> rankedDocsWithIDF = new HashMap<Integer, Double>();
			startTime = System.nanoTime();
			for (String qTerm : queryTerms) {
				Term term = new Term("contents", qTerm);
				double qTermIDF = idfIndex.get(qTerm);
				TermDocs termDocs = reader.termDocs(term);
				while (termDocs.next()) {
					int docId = termDocs.doc();
					double prevVal = 0;
					if (rankedDocsWithIDF.containsKey(docId)) {
						prevVal = rankedDocsWithIDF.get(docId);
					}
					double newVal = prevVal + termDocs.freq() * qTermIDF;
					rankedDocsWithIDF.put(termDocs.doc(), newVal);
				}
			}
			// Sorting TF_IDF results
			tfIdfResults = new ArrayList<DocScore>();
			for (Integer key : rankedDocsWithIDF.keySet()) {
				double euclLen = euclDistWithIDFIndex.get(key);
				euclLen = Math.sqrt(euclLen);
				DocScore docScore = new DocScore(key, rankedDocsWithIDF
						.get(key)
						/ (euclLen * queryEuclLen));
				tfIdfResults.add(docScore);
			}
			Collections.sort(tfIdfResults, new Comparator() {
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

			System.out.println("Time taken to compute TF*IDF based ranking "
					+ (System.nanoTime() - startTime));
			// TF results
			System.out.println("  *******  TF Results  ******");
			System.out
					.println("Total number of results  : " + tfResults.size());
			final int HITS_PER_PAGE = 10;
			for (int start = 0; start < tfResults.size(); start += HITS_PER_PAGE) {
				int end = Math.min(tfResults.size(), start + HITS_PER_PAGE);
				for (int i = start; i < end; i++) {
					int docId = tfResults.get(i).getDocId();
					double docScore = tfResults.get(i).getScore();
					System.out.println((i + 1)
							+ ". Doc Id : "
							+ docId
							+ "      Score : "
							+ docScore
							+ "  Url : "
							+ reader.document(docId).getField("url")
									.stringValue());
				}
				if (tfResults.size() > end) {
					System.out.print("more (y/n) ? ");
					String line = in.readLine();
					if (line.length() == 0 || line.charAt(0) == 'n')
						break;
				}
			}
			// TF-IDF results
			System.out.println("  *******   TF_IDF  Results  ******");
			System.out.println("Total number of results  : "
					+ tfIdfResults.size());
			for (int start = 0; start < tfIdfResults.size(); start += HITS_PER_PAGE) {
				int end = Math.min(tfIdfResults.size(), start + HITS_PER_PAGE);
				for (int i = start; i < end; i++) {
					int docId = tfIdfResults.get(i).getDocId();
					double docScore = tfIdfResults.get(i).getScore();
					System.out.println((i + 1)
							+ ".  Doc Id : "
							+ docId
							+ "      Score : "
							+ docScore
							+ "  Url : "
							+ reader.document(docId).getField("url")
									.stringValue());
				}
				if (tfIdfResults.size() > end) {
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
			MySearchEngine mySearchEngine = new MySearchEngine();
			System.out.println("Deserializing Indexes .  Please Wait  .....");
			mySearchEngine.deSerializeIdfIndex();
			mySearchEngine.deSerializeEuclDistIndex();
			mySearchEngine.deSerializeEuclDistWithIDFIndex();
			System.out.println("Deserialization Completed");
			System.out.println("calculating Page rank .. ");
//			PageRankCalculator pageRankCalculator = new PageRankCalculator();
//			pageRankCalculator.calculatePageRank(0.85);
//			mySearchEngine.pageRank = pageRankCalculator.getPageRank();
			ModifiedPageRank modifiedPageRank = new ModifiedPageRank();
			modifiedPageRank.calculatePageRank(0.85);
			mySearchEngine.pageRank = modifiedPageRank.getPageRank();
			System.out.println("Page rank calculation Complete .. ");
//			mySearchEngine.pageRank.get(901);
			while (true) {
				System.out.print("Query: ");
				String queryString = in.readLine();
				queryString.trim();
				if (queryString.length() == -1 || queryString.isEmpty())
					break;
				mySearchEngine.calculateRank(queryString.toLowerCase());
			// AH Calculation
				AuthorityHubCalculator ahCalculator = new AuthorityHubCalculator();
				ArrayList<DocScore> seedPages = new ArrayList<DocScore>();
				System.out.println("Please Enter Base Set Size : ");
				int baseSetSize = Integer.parseInt(in.readLine());
				for (int i = 0; i < baseSetSize; i++) {
					seedPages.add(mySearchEngine.tfIdfResults.get(i));
				}
				ahCalculator.calculateAuthorityHub(seedPages);
				
			// Weighted Page Rank and TF-IDF weights
				System.out.println("Please enter value for weight assigned to Page rank (Between 0 and 1) : ");
				double w = Double.parseDouble(in.readLine());
				mySearchEngine.reRankResults(w);
			}
		} catch (Exception e) {
			System.out.println(" caught a " + e.getClass()
					+ "\n with message: " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private void reRankResults(double w) throws IOException {
		ArrayList<DocScore> reRankedDocs = new ArrayList<DocScore>();
		long startTime = System.nanoTime();
		for(DocScore d: tfIdfResults){
			double score = d.getScore();
			double modifiedScore = w * pageRank.get(d.getDocId())+ (1-w)*score;
			reRankedDocs.add(new DocScore(d.getDocId(),modifiedScore));
		}
		Collections.sort(reRankedDocs, new Comparator() {
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
		System.out.println("Time taken for re ranking "
				+ (System.nanoTime() - startTime));
		IndexReader reader = IndexReader.open("result3index");
		for(int i=0;i<10;i++){
			System.out.println(reRankedDocs.get(i).getDocId() + reRankedDocs.get(i).getScore() + "  URL : " + reader.document(reRankedDocs.get(i).getDocId()).getField("url")); 
		}
		reader.close();
	}
}
