package edu.asu.cse494;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.lucene.index.IndexReader;
import com.lucene.index.Term;
import com.lucene.index.TermDocs;
import com.lucene.index.TermEnum;

public class RelevanceFeedback {

	private double[][] termVectors;
	private ArrayList<String> terms;
	private HashMap<Integer, Integer> docIdIndex;
	private double[] newQueryVector;
	private double alpha = 1.0;
	private double beta = 0.75;
	private double gamma = 0.25;
	private HashMap<String, Double> idfIndex;

	@SuppressWarnings("unchecked")
	public void generateNewQuery(ArrayList<Integer> relevantDocs,
			ArrayList<Integer> irrelevantDocs,
			HashMap<Integer, Double> euclDistWithIDFIndex, String[] qTerms)
			throws IOException {
		docIdIndex = new HashMap<Integer, Integer>();
		terms = new ArrayList<String>();
		int index = 0;
		for (int docId : relevantDocs) {
			docIdIndex.put(docId, index);
//			System.out.println(docId + "  " + index);
			index++;
		}
		for (int docId : irrelevantDocs) {
			docIdIndex.put(docId, index);
//			System.out.println(docId + "  " + index);
			index++;
		}
		int totalDocs = relevantDocs.size() + irrelevantDocs.size();
		IndexReader reader = IndexReader.open("result3index");
		TermEnum termEnum = reader.terms();
		int numTerms = 0;
		idfIndex = new HashMap<String, Double>();
		double numDocsInCorpus = reader.numDocs();
		while (termEnum.next()) {
			Term termVal = termEnum.term();
			terms.add(termVal.text().toLowerCase());
			if (termVal.field().equalsIgnoreCase("contents")) {
				double termIDF = Math.log((double) numDocsInCorpus / termEnum.docFreq());
				idfIndex.put(termVal.text().toLowerCase(), termIDF);
				numTerms++;
			}
		}
		termVectors = new double[totalDocs][numTerms];
		
		termEnum = reader.terms();
		int termIndex = 0;
		while (termEnum.next()) {
			Term termVal = termEnum.term();
			if (termVal.field().equalsIgnoreCase("contents")) {
				double termIDF = idfIndex.get(termVal.text().toLowerCase());
				if(termIDF == 0){
					System.out.println("IDF is 0");
				}
				TermDocs termDocs = reader.termDocs(termVal);
				while (termDocs.next()) {
					int docId = termDocs.doc();
					if (docIdIndex.containsKey(docId)) {
//						 System.out.println("Index : " +
//						 docIdIndex.get(docId));
						termVectors[docIdIndex.get(docId)][termIndex] = termDocs
								.freq()
								* termIDF;
					}
				}
				termIndex++;
			}
		}
		// Create Original Query Vector
		double[] originalQueryVector = new double[numTerms];
//		int positives = 0;
		for (String qTerm : qTerms) {
			originalQueryVector[terms.indexOf(qTerm.toLowerCase())] = idfIndex.get(qTerm.toLowerCase());
//			if(originalQueryVector[terms.indexOf(qTerm.toLowerCase())] > 0){
//				positives++;
//			}
		}
		// Create Vector of Relevant Docs
//		positives = 0;
		double[] relevantQueryVector = new double[numTerms];
		for (int i = 0; i < numTerms; i++) {
			for (int j = 0; j < relevantDocs.size(); j++) {
				relevantQueryVector[i] += termVectors[docIdIndex.get(relevantDocs.get(j))][i];
			}
//			if(relevantQueryVector[i] > 0){
//				positives++;
//			}
		}
		// Create Vector of Irrelevant Docs
//		positives = 0;
		System.out.println("Size of irr Docs : " +  irrelevantDocs.size() + " NUm terms : " + numTerms);
		double[] irrelevantQueryVector = new double[numTerms];
		for (int i = 0; i < numTerms; i++) {
			for (int j = 0; j < irrelevantDocs.size(); j++) {
				irrelevantQueryVector[i] += termVectors[docIdIndex.get(irrelevantDocs.get(j))][i];
//				System.out.println(termVectors[docIdIndex.get(irrelevantDocs.get(j))][i]);
			}
//			if(irrelevantQueryVector[i] > 0){
//				positives++;
//			}
		}
		// Create New Query Vector
		newQueryVector = new double[numTerms];
		for (int i = 0; i < numTerms; i++) {
			newQueryVector[i] = alpha
					* originalQueryVector[i]
					+ ((beta / relevantDocs.size()) * relevantQueryVector[i])
					- ((gamma / irrelevantDocs.size()) * irrelevantQueryVector[i]);
		}

		double queryEuclLen = 0.0;
		for (int i = 0; i < numTerms; i++) {
			queryEuclLen += newQueryVector[i] * newQueryVector[i];
		}
		queryEuclLen = Math.sqrt(queryEuclLen);
		System.out.println("Query Eucl Len : " + queryEuclLen);

		// For TF-IDF
		HashMap<Integer, Double> rankedDocsWithIDF = new HashMap<Integer, Double>();
		long startTime = System.nanoTime();
		for (int i = 0; i < numTerms; i++) {
			String qTerm = terms.get(i);
			Term term = new Term("contents", qTerm);
			double qTermIDF = idfIndex.get(qTerm);
			TermDocs termDocs = reader.termDocs(term);
			while (termDocs.next()) {
				int docId = termDocs.doc();
				double prevVal = 0;
				if (rankedDocsWithIDF.containsKey(docId)) {
					prevVal = rankedDocsWithIDF.get(docId);
				}
				double newVal = prevVal + newQueryVector[i] * termDocs.freq()
						* qTermIDF;
				rankedDocsWithIDF.put(termDocs.doc(), newVal);
			}
		}
		// Sorting TF_IDF results
		ArrayList<DocScore> tfIdfResults = new ArrayList<DocScore>();
		for (Integer key : rankedDocsWithIDF.keySet()) {
			double euclLen = euclDistWithIDFIndex.get(key);
			euclLen = Math.sqrt(euclLen);
			DocScore docScore = new DocScore(key, rankedDocsWithIDF.get(key)
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
		/* Printing documents after considering feedback */
		System.out.println(" Printing documents after considering feedback ");
		for(int rank = 0; rank <10; rank++){
			System.out.println(tfIdfResults.get(rank).getDocId() + "  " + tfIdfResults.get(rank).getScore() + "  URL : " + reader.document(tfIdfResults.get(rank).getDocId()).getField("url"));
		}
		System.out.println("Time taken to compute TF*IDF based ranking "
				+ (System.nanoTime() - startTime) + " nano seconds");
	}
}
