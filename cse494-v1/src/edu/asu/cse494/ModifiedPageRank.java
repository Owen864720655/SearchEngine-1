package edu.asu.cse494;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class ModifiedPageRank {

	HashMap<Integer, Double> pageRank = new HashMap<Integer, Double>();
	private LinkAnalysis linkAnalysis;
	public static final String citationsFile = "IntCitations.txt";
	private static double c = 0.85;
	private static final double threshold = 0.0001;
	private int numDocs;
	private double resetFactor;
	private double zFactor;
	double[] pageRankValues;
	private HashMap<Integer, Integer> docIdIndex = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> invertedDocIdIndex = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> nodeLinks = new HashMap<Integer, Integer>();

	public void calculatePageRank(double cVal) {
		try {
			c = cVal;
			linkAnalysis = new LinkAnalysis();
			numDocs = linkAnalysis.getNumDocs();
			resetFactor = (1 - c) * ((1.0) / numDocs);
			zFactor = c * ((1.0) / numDocs);
			int index = 0;
			BufferedReader br = new BufferedReader(
					new FileReader(citationsFile));
			String s = "";
			while ((s = br.readLine()) != null) {
				String[] words = s.split("->"); // split the
				// src->dest1,dest2,dest3 string
				int src = Integer.parseInt(words[0]);
				docIdIndex.put(src, index);
				// System.out.println(" Src : " + src + "  index :  " + index);
				invertedDocIdIndex.put(index, src);
				int numLinks = linkAnalysis.getLinks(src).length;
				nodeLinks.put(src, numLinks);
				index++;
			}
			br.close();

			long startTIme = System.nanoTime();
			initializePageRank();
			boolean isStable = false;
			double[] tempVal = new double[numDocs];
			int numIteration = 0;
			while (true) {
				numIteration++;
				for (int docId : docIdIndex.keySet()) {
					double sum = 0.0;
					int[] citations = linkAnalysis.getCitations(docId);
					if (citations.length == 0) {
						for (int i : docIdIndex.keySet()) {
							int numLinks = nodeLinks.get(i);
							// int numLinks = linkAnalysis.getLinks(i).length;
							if (numLinks == 0) {
								sum += ((zFactor + resetFactor) * pageRankValues[docIdIndex
										.get(i)]);
							} else {
								sum += (resetFactor * pageRankValues[docIdIndex
										.get(i)]);
							}
						}
					} else {
						HashSet<Integer> citationsList = new HashSet<Integer>();
						for (int citation : citations) {
							citationsList.add(citation);
						}

						for (int docNum : docIdIndex.keySet()) {
							int numLinks = nodeLinks.get(docNum);
							if (!citationsList.contains(docNum)) {
								if (numLinks == 0) {
									sum += ((zFactor + resetFactor) * pageRankValues[docIdIndex
											.get(docNum)]);
								} else {
									sum += (resetFactor * pageRankValues[docIdIndex
											.get(docNum)]);
								}
							} else {
								double val = c * ((1.0) / numLinks)
										+ resetFactor;
								sum += (val * pageRankValues[docIdIndex
										.get(docNum)]);
							}
						}
					}
					tempVal[docIdIndex.get(docId)] = pageRankValues[docIdIndex
							.get(docId)];
					pageRankValues[docIdIndex.get(docId)] = sum;
				}
				normalizePageRankValues();
				isStable = verifyStability(tempVal);
				System.out.println("Iteration No : " + numIteration);
				if (isStable) {
					System.out.println("Stability Reached ");
					break;
				}
			}
			divByMaxVal();
			mapValues();
			System.out.println("Time taken to compute Page Rank "
					+ (System.nanoTime() - startTIme));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public HashMap<Integer, Double> getPageRank() {
		return pageRank;
	}

	public void serializeIDFIndex() {
		try {
			FileOutputStream fout = new FileOutputStream(
					"result3index/PageRank_2.ser");
			System.out.println("File created");
			ObjectOutputStream objOut = new ObjectOutputStream(fout);
			objOut.writeObject(pageRank);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	private void mapValues() {
		for (int docId : docIdIndex.keySet()) {
			pageRank.put(docId, pageRankValues[docIdIndex.get(docId)]);
		}
	}

	private void divByMaxVal() {
		double maxVal = pageRankValues[0];
		for (int i = 1; i < pageRankValues.length; i++) {
			if (pageRankValues[i] > maxVal) {
				maxVal = pageRankValues[i];
			}
		}
		for (int i = 0; i < pageRankValues.length; i++) {
			pageRankValues[i] /= maxVal;
		}
	}

	private void printPageRanks(int[] pageIds) {
		for (int docNum : pageIds) {
			System.out.println("Doc id : " + docNum + "  PAge Rank : "
					+ pageRankValues[docIdIndex.get(docNum)] + " saved Val = "
					+ pageRank.get(docNum));
		}
	}

	private void normalizePageRankValues() {
		double sum = 0.0;
		for (int i = 0; i < numDocs; i++) {
			sum += pageRankValues[i];
		}
		for (int i = 0; i < numDocs; i++) {
			pageRankValues[i] /= sum;
		}
	}

	private boolean verifyStability(double[] tempVal) {
		double diff = 0.0;
		for (int docNum : docIdIndex.keySet()) {
			diff += ((tempVal[docIdIndex.get(docNum)] - pageRankValues[docIdIndex
					.get(docNum)]) * (tempVal[docIdIndex.get(docNum)] - pageRankValues[docIdIndex
					.get(docNum)]));
		}
		if (diff < threshold) {
			return true;
		} else {
			return false;
		}
	}

	private void initializePageRank() {
		pageRankValues = new double[numDocs];
		Arrays.fill(pageRankValues, (1.0) / numDocs);
	}

	public static void main(String[] args) {
		ModifiedPageRank modifiedPageRank = new ModifiedPageRank();
		modifiedPageRank.calculatePageRank(0.85);
		int[] pgIds = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
		modifiedPageRank.printPageRanks(pgIds);
	}
}
