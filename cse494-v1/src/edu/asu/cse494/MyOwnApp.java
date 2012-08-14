package edu.asu.cse494;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import com.lucene.analysis.Analyzer;
import com.lucene.analysis.StopAnalyzer;
import com.lucene.document.Document;
import com.lucene.index.IndexReader;
import com.lucene.search.Searcher;
import com.lucene.search.IndexSearcher;
import com.lucene.search.Query;
import com.lucene.search.Hits;
import com.lucene.queryParser.QueryParser;

/**
 * Gives a interface to ask Boolean queries on underlying index. Usage: java
 * SearchFiles
 */

public class MyOwnApp {

	HashMap<Integer, Integer> euclDistIndex;
	HashMap<String, TermInformation> invertedTermDocIndex;
	HashMap<String, Double> idfIndex;
	HashMap<Integer, Double> euclDistWithIDFIndex;

	@SuppressWarnings("unchecked")
	public void deSerializeIdfIndex() {
		FileInputStream fis;
		try {
			fis = new FileInputStream("result3index/idfIndex.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			idfIndex = (HashMap<String, Double>) ois.readObject();
			System.out.println(" From retrieved Index : ");
			double termIDF = idfIndex.get("aa");
			System.out.println("IDF of aa : " + termIDF);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException ioException) {
			// TODO Auto-generated catch block
			ioException.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void deSerializeInvertedTermDocIndex() {
		FileInputStream fis;
		try {
			fis = new FileInputStream("result3index/invertedIndex.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			invertedTermDocIndex = (HashMap<String, TermInformation>) ois
					.readObject();
			System.out.println(" From retrieved Index : ");
			TermInformation termInfo = invertedTermDocIndex.get("aa");
			System.out.println("No of docs containing aa : "
					+ termInfo.getNumDocs());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException ioException) {
			// TODO Auto-generated catch block
			ioException.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void deSerializeEuclDistIndex() {
		FileInputStream fis;
		try {
			fis = new FileInputStream("result3index/docEuclDistIndex.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			euclDistIndex = (HashMap<Integer, Integer>) ois.readObject();
			System.out.println(" From retrieved Index : ");
			System.out.println(" Eucl dist for doc no 106 : "
					+ euclDistIndex.get(106));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException ioException) {
			// TODO Auto-generated catch block
			ioException.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void deSerializeEuclDistWithIDFIndex() {
		FileInputStream fis;
		try {
			fis = new FileInputStream(
					"result3index/docEuclDistIndexWithIDF.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			euclDistWithIDFIndex = (HashMap<Integer, Double>) ois.readObject();
//			System.out.println(" From retrieved Index : ");
//			System.out.println(" Eucl dist for doc no 106 : "
//					+ euclDistWithIDFIndex.get(106));
			System.out.println(" Eucl dist for doc no 106 : "
					+ 1/ Math.sqrt(euclDistWithIDFIndex.get(106)));
			System.out.println(" Eucl dist for doc no 5 : "
					+ 1/Math.sqrt(euclDistWithIDFIndex.get(5)));
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void calculateRank(String queryString, boolean useIDF) {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					System.in));
			String[] queryTerms = queryString.split("\\s");
			double queryEuclLen = Math.sqrt(queryTerms.length);
			System.out.println("Length of query terms : " + queryTerms.length);
			HashMap<Integer, Double> rankedDocs = new HashMap<Integer, Double>();
			for (String qTerm : queryTerms) {
				System.out.println(qTerm + " IDF : " + idfIndex.get(qTerm));
				TermInformation termInfo = invertedTermDocIndex.get(qTerm);
				ArrayList<DocIdFreqStore> docFreqList = termInfo
						.getDocIdFreqStoreList();
				double qTermIDF = idfIndex.get(qTerm);
				for (DocIdFreqStore docIdFreqStore : docFreqList) {
					double prevVal = 0;
					int docId = docIdFreqStore.getDocId();
					if (rankedDocs.containsKey(docId)) {
						prevVal = rankedDocs.get(docId);
					}
					double newVal = 0; 
					if (useIDF) {
						newVal = prevVal + docIdFreqStore.getFreq() * qTermIDF;
					}else{
						newVal = prevVal + docIdFreqStore.getFreq();
					}
					rankedDocs.put(docId, newVal);
				}
			}

			ArrayList<DocScore> sortedList = new ArrayList<DocScore>();
			// Set<Integer> keySet = rankedDocs.keySet();
			for (Integer key : rankedDocs.keySet()) {
				double euclLen = 0.0;
				if (useIDF) {
					euclLen = euclDistWithIDFIndex.get(key);
				} else {
					euclLen = euclDistIndex.get(key);
				}
				euclLen = Math.sqrt(euclLen);
				DocScore docScore = new DocScore(key, rankedDocs.get(key)/ (euclLen * queryEuclLen));
				// System.out.println(rankedDocs.get(key));
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
			IndexReader reader = IndexReader.open("result3index");
			System.out.println("Total number of results  : " + sortedList.size());
			final int HITS_PER_PAGE = 10;
			for (int start = 0; start < sortedList.size(); start += HITS_PER_PAGE) {
				int end = Math.min(sortedList.size(), start + HITS_PER_PAGE);
				for (int i = start; i < end; i++) {
					int docId = sortedList.get(i).getDocId();
					double docScore = sortedList.get(i).getScore();
					System.out.println(i + ". " + docId + "  Url : "
							+ reader.document(docId).getField("url").stringValue()
							+ " Score : " + docScore);
				}
				if (sortedList.size() > end) {
					System.out.print("more (y/n) ? ");
					String line = in.readLine();
					if (line.length() == 0 || line.charAt(0) == 'n')
						break;
				}
			}
			reader.close();
			// int resCount = 0;
			// while (resCount < 10) {
			// System.out.println("Doc Id : "
			// + sortedList.get(resCount).getDocId() + " Score : "
			// + sortedList.get(resCount).getScore());
			// resCount++;
			// }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {

			Searcher searcher = new IndexSearcher("result3index");
			Analyzer analyzer = new StopAnalyzer();

			BufferedReader in = new BufferedReader(new InputStreamReader(
					System.in));

			MyOwnApp myOwnApp = new MyOwnApp();
			myOwnApp.deSerializeIdfIndex();
			myOwnApp.deSerializeEuclDistIndex();
			myOwnApp.deSerializeEuclDistWithIDFIndex();
			myOwnApp.deSerializeInvertedTermDocIndex();

			System.out.println("Deserialization Completed");

			while (true) {
				System.out.print("Query: ");
				String line = in.readLine();
				line.trim();
				if (line.length() == -1 || line.isEmpty())
					break;
				Query query = QueryParser.parse(line, "contents", analyzer);
				// System.out.println("Searching for: "
				// + query.toString("contents"));
				String queryString = query.toString("contents");

				myOwnApp.calculateRank(queryString,false);

				// Hits hits = searcher.search(query);
				// System.out.println(hits.length() +
				// " total matching documents");
				//
				// final int HITS_PER_PAGE = 10;
				// for (int start = 0; start < hits.length(); start +=
				// HITS_PER_PAGE) {
				// int end = Math.min(hits.length(), start + HITS_PER_PAGE);
				// for (int i = start; i < end; i++) {
				// System.out.println(i + ". " + hits.doc(i).get("url"));
				// // System.out.println(i + ". " +
				// // hits.doc(i).toString());
				// }
				// if (hits.length() > end) {
				// System.out.print("more (y/n) ? ");
				// line = in.readLine();
				// if (line.length() == 0 || line.charAt(0) == 'n')
				// break;
				// }
				// }
			}
			searcher.close();

		} catch (Exception e) {
			System.out.println(" caught a " + e.getClass()
					+ "\n with message: " + e.getMessage());
		}
	}
}
