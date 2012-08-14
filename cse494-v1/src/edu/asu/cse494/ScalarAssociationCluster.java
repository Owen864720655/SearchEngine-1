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

public class ScalarAssociationCluster {

	private HashMap<Integer, Integer> docIdIndex;
	private ArrayList<String> terms;
	private ArrayList<Term> termsObj;
	private double[][] termTermCorrScore;
	private double[][] normalizedTermTermCorrScore;
	private int[][] termDocVector;
	private double[] euclDist;
	private ArrayList<ArrayList<TermScore>> scalarClusters;

	@SuppressWarnings("unchecked")
	public void createAssociationVectors(ArrayList<DocScore> topRankedDocs, String[] qTerms, final HashMap<String, Double> idfIndex) throws IOException{
		terms = new ArrayList<String>();
		termsObj = new ArrayList<Term>();
		docIdIndex = new HashMap<Integer, Integer>();
		int index = 0;
		for(DocScore docScore:topRankedDocs){
			docIdIndex.put(docScore.getDocId(), index);
			index++;
		}
		IndexReader reader = IndexReader.open("result3index");
		TermEnum termEnum = reader.terms();
		while (termEnum.next()) {
			Term termVal = termEnum.term();
			if(termVal.field().equals("contents")){
				TermDocs termDocs = reader.termDocs(termVal);
				boolean isAvailable = false;
				while (termDocs.next()) {
					if(docIdIndex.containsKey(termDocs.doc())){
						isAvailable = true;
						break;
					}
				}
				if(isAvailable){
					terms.add(termVal.text().toLowerCase());
					termsObj.add(termVal);
				}
			}	
		}
		System.out.println(" No of terms " + terms.size());
		int numTermsinDocs = terms.size();
		termTermCorrScore = new double[numTermsinDocs][numTermsinDocs];
		normalizedTermTermCorrScore = new double[numTermsinDocs][numTermsinDocs];
		euclDist = new double[numTermsinDocs];
//		System.out.println(" No of terms : " + terms.size());
		termDocVector = new int[numTermsinDocs][topRankedDocs.size()];
		int termIndex = 0;
		for(Term termVal : termsObj){
			TermDocs termDocs = reader.termDocs(termVal);
			while (termDocs.next()) {
				int docId = termDocs.doc();
				if(docIdIndex.containsKey(docId)){
					int termFreq = termDocs.freq();
					termDocVector[termIndex][docIdIndex.get(docId)] = termFreq;
				}
			}
			termIndex++;
		}
		calculateCorrelationMatrix();
		System.out.println(" Correlation Matrix generated");
		// Normalize Correlation Matrix
		normalizeCorrelationMatrix();
		calculateEuclDist();
		// Calculate Scalar Clusters
		calculateScalarCluster(qTerms);
		System.out.println("Scalar clusters calculated");
		// Rank correlated words for each query term
		for(int i=0; i<qTerms.length; i++){
			Collections.sort(scalarClusters.get(i), new Comparator() {
				@Override
				public int compare(Object o1, Object o2) {
					TermScore termA = (TermScore) o1;
					TermScore termB = (TermScore) o2;
					double scoreA = termA.getTermScore();
					double scoreB = termB.getTermScore();
					if (scoreB < scoreA) {
						return -1;
					} else if (scoreB == scoreA) {
						return 0;
					} else {
						return 1;
					}
				}
			});
		}
		System.out.println(" Printing the top 10 most correalted words to the query terms  ... ");
		for(int clusterNum = 0; clusterNum < scalarClusters.size(); clusterNum++){
			System.out.println(" Query Term : " + scalarClusters.get(clusterNum).get(0).getTermText());
			for(int i=1; i<11; i++){
				System.out.println(scalarClusters.get(clusterNum).get(i).getTermText() + "  " + scalarClusters.get(clusterNum).get(i).getTermScore());
			}	
			System.out.println(" ************ ");
		}
	}
	
	private void calculateScalarCluster(String[] qTerms) {
		scalarClusters = new ArrayList<ArrayList<TermScore>>();
		for(int k = 0; k < qTerms.length; k++){
			scalarClusters.add(new ArrayList<TermScore>());
		}
		int i = 0;
		for(String qTerm:qTerms){
			int qIndex = terms.indexOf(qTerm.toLowerCase());
			for(int index=0; index < terms.size(); index++){
				double score = 0.0;
				for(int j = 0; j < terms.size(); j++){
					score += normalizedTermTermCorrScore[qIndex][j] * normalizedTermTermCorrScore[index][j];
				}
				score /= (euclDist[qIndex] * euclDist[index]);
				TermScore termScore = new TermScore(terms.get(index), score);
				scalarClusters.get(i).add(termScore);
			}
			i++;
		}
	}

	private void calculateEuclDist() {
		for(int i=0; i<terms.size(); i++){
			double dist = 0.0;
			for(int j=0; j<terms.size(); j++){
				dist += normalizedTermTermCorrScore[i][j] * normalizedTermTermCorrScore[i][j];
			}
			euclDist[i] = Math.sqrt(dist);
		}
	}


	private void calculateCorrelationMatrix() {
		for(int i = 0; i < terms.size(); i++){
			for(int j = i; j<terms.size(); j++){
				double sum = 0.0;
				for(int k = 0; k < docIdIndex.size(); k++){
					sum += termDocVector[i][k] * termDocVector[j][k];	
				} 
				termTermCorrScore[i][j] = termTermCorrScore[j][i] = sum;
			}
		}
	}

	private void normalizeCorrelationMatrix() {
		for(int i = 0; i < terms.size(); i++){
			for(int j = i; j<terms.size(); j++){
				if(termTermCorrScore[i][j] > 0){
					double newVal = termTermCorrScore[i][j] / (termTermCorrScore[i][i] + termTermCorrScore[j][j] - termTermCorrScore[i][j]);
					normalizedTermTermCorrScore[i][j] = normalizedTermTermCorrScore[j][i] = newVal;
				}
			}
		}
	}

	// For Testing

	/*
	public static void main(String[] args) throws IOException {
		ScalarAssociationCluster scalarCluster = new ScalarAssociationCluster();
		ArrayList<DocScore> docScores = new ArrayList<DocScore>();
		// Test Data
		DocScore docScore = new DocScore(0, 0.5);
		docScores.add(docScore);
		docScore = new DocScore(1, 0.6);
		docScores.add(docScore);
		docScore = new DocScore(2, 0.7);
		docScores.add(docScore);
		docScore = new DocScore(3, 0.8);
		docScores.add(docScore);
		docScore = new DocScore(4, 0.22);
		docScores.add(docScore);
		docScore = new DocScore(5, 0.43);
		docScores.add(docScore);
		docScore = new DocScore(6, 0.93);
		docScores.add(docScore);
		docScore = new DocScore(7, 0.13);
		docScores.add(docScore);
		docScore = new DocScore(8, 0.33);
		docScores.add(docScore);
		docScore = new DocScore(9, 0.55);
		docScores.add(docScore);
		scalarCluster.createAssociationVectors();
		System.out.println("Hello");
	}
*/	
	
}
