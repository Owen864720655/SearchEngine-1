package edu.asu.cse494;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import com.lucene.index.IndexReader;
import com.lucene.index.Term;
import com.lucene.index.TermDocs;
import com.lucene.index.TermEnum;

public class KMeansCluster {
	private int k;
	private ArrayList<ArrayList<Integer>> clusters;
	private ArrayList<ArrayList<Integer>> newClusters;
	private HashMap<Integer, Integer> docIdIndex;
	private ArrayList<String> terms;
	private boolean isStable = false;
	private double[][] termVectors;
	private ArrayList<BitSet> bitVectors;
	private double[][] centroids;
	private double[] euclDistOfCentroids;
	private double[] euclDistOfDocs;
	private HashMap<String,Double> idfIndex;
	private int numTerms;

	public void createTermVectors(final ArrayList<DocScore> docScores)
			throws IOException {
		int index = 0;
		for (DocScore docScore : docScores) {
			docIdIndex.put(docScore.getDocId(), index);
			index++;
		}
		IndexReader reader = IndexReader.open("result3index");
		int totalDocs =  reader.numDocs();
		TermEnum termEnum = reader.terms();
		numTerms = 0;
		while (termEnum.next()) {
			Term termVal = termEnum.term();
			if(termVal.field().equalsIgnoreCase("contents")){
				numTerms++;
				double termIDF = Math.log((double) totalDocs / termEnum.docFreq());
				idfIndex.put(termVal.text().toLowerCase(), termIDF);
			}	
		}
		for (int i = 0; i < docScores.size(); i++) {
			bitVectors.add(new BitSet(numTerms));
		}
		termVectors = new double[docScores.size()][numTerms];
		termEnum = reader.terms();
		int termIndex = 0;
		while (termEnum.next()) {
			Term termVal = termEnum.term();
			if(termVal.field().equalsIgnoreCase("contents")){
				terms.add(termVal.text().toLowerCase());
				double termIDF = idfIndex.get(termVal.text().toLowerCase());
				TermDocs termDocs = reader.termDocs(termVal);
				while (termDocs.next()) {
					int docId = termDocs.doc();
					if (docIdIndex.containsKey(docId)) {
						// System.out.println("Index : " + docIdIndex.get(docId));
						int termFreq = termDocs.freq();
						termVectors[docIdIndex.get(docId)][termIndex] = termFreq * termIDF;
						bitVectors.get(docIdIndex.get(docId)).set(termIndex, true);
					}
				}
				termIndex++;
			}	
		}
	}

	public void createKMeansClusters(ArrayList<DocScore> docScores,int numClusters) throws IOException {
		docIdIndex = new HashMap<Integer, Integer>();
		bitVectors = new ArrayList<BitSet>(docScores.size());
		terms = new ArrayList<String>();
		idfIndex = new HashMap<String, Double>();
		long startTime = System.nanoTime();
		//Create Term Vectors
		createTermVectors(docScores);
		k = numClusters;
		clusters = new ArrayList<ArrayList<Integer>>(k);
		centroids = new double[k][numTerms];
		euclDistOfCentroids = new double[k];
		euclDistOfDocs = new double[docScores.size()];
		// Generate Initial Centroids
		int count = 0;
		ArrayList<Integer> initialCentroids = new ArrayList<Integer>();
		Random random = new Random();

		while (count < k) {
			int randomNum = random.nextInt(docScores.size());
//			System.out.println("Random Number : " + randomNum);
			int docId = docScores.get(randomNum).getDocId();
			int docIndex = docIdIndex.get(docId);
			boolean hasDuplicateDocs = false;
			for(int docNum : initialCentroids){
				boolean isDuplicate = true;
				int docNumIndex = docIdIndex.get(docNum);
				BitSet bitVector = bitVectors.get(docNumIndex); 
				for (int i = bitVector.nextSetBit(0); i >= 0; i = bitVector.nextSetBit(i + 1)) {
					if(termVectors[docNumIndex][i] != termVectors[docIndex][i]){
						isDuplicate = false;
						break;
					}
				}
				if(isDuplicate){
					hasDuplicateDocs = true;
					break;
				}
			}
			if (!hasDuplicateDocs) {
				initialCentroids.add(docId);
				for (int i = 0; i < numTerms; i++) {
					centroids[count][i] = termVectors[docIndex][i];
				}
				clusters.add(new ArrayList<Integer>());
				// clusters.get(count).add(docId);
				count++;
			}
		}
		calcEuclDistOfCentroids();
		System.out.println("Initial Centroids generated");
		// Start Clustering ---- First Iteration
		for (DocScore docScore : docScores) {
			int docId = docScore.getDocId();
			if (initialCentroids.contains(docId)) {
				clusters.get(initialCentroids.indexOf(docId)).add(docId);
			} else {
				int clusterIndex = 0;
				double similarityScore = getVectorSimilarityScore(clusterIndex,
						docId);
				for (int i = 1; i < k; i++) {
					double candidateScore = getVectorSimilarityScore(i, docId);
					if (similarityScore < candidateScore) {
						clusterIndex = i;
						similarityScore = candidateScore;
					}
				}
				clusters.get(clusterIndex).add(docId);
			}
		}
		System.out.println("First Iteration Completed");
//		printClusters();
		// Further Iterations
		while (!isStable) {
			newClusters = new ArrayList<ArrayList<Integer>>(k);
			// Find new Centroids
			for (int i = 0; i < k; i++) {
				newClusters.add(new ArrayList<Integer>());
				for (int n = 0; n < numTerms; n++) {
					double newCentroid = 0.0;
					for (int docId : clusters.get(i)) {
						newCentroid += termVectors[docIdIndex.get(docId)][n];
					}
					centroids[i][n] = newCentroid
							/ (double) (clusters.get(i).size());
				}
			}
			calcEuclDistOfCentroids();
			// Put Docs in Clusters
			for (DocScore docScore : docScores) {
				int docId = docScore.getDocId();
				int clusterIndex = 0;
				double similarityScore = getVectorSimilarityScore(clusterIndex,
						docId);
				for (int i = 1; i < k; i++) {
					double candidateScore = getVectorSimilarityScore(i, docId);
					if (similarityScore < candidateScore) {
						clusterIndex = i;
						similarityScore = candidateScore;
					}
				}
				newClusters.get(clusterIndex).add(docId);
			}
			isStable = verifyStability();
			System.out.println(" isStable : " + isStable);
			updateCluster();
//			printClusters();
		}
		System.out.println("Time taken : " + (System.nanoTime() - startTime) + " nano seconds");
		printClusters();
		createClustersSummary();
	}
	
	@SuppressWarnings("unchecked")
	public void createClustersSummary() throws IOException{
		ArrayList<ArrayList<TermScore>> clusterSummary = new ArrayList<ArrayList<TermScore>>();
		HashMap<String, Double> termICFIndex = new HashMap<String, Double>();
		for(int i = 0; i < numTerms; i++){
			int clustersWithTerm = 0;
			for(int j=0; j < clusters.size(); j++){
				if(centroids[j][i] > 0){
					clustersWithTerm++;
				}
			}
//			if(clustersWithTerm == 0){
//				termICFIndex.put(terms.get(i), 0.0);
//			}else{
//				double termICF = Math.log((double) clusters.size() / (double) clustersWithTerm);
//				termICFIndex.put(terms.get(i), termICF);
//			}
			if(clustersWithTerm > 0){
				double termICF = Math.log((double) clusters.size() / (double) clustersWithTerm);
				termICFIndex.put(terms.get(i), termICF);
			}
		}
		for(int i=0; i<clusters.size(); i++){
			clusterSummary.add(new ArrayList<TermScore>());
			for(int j=0; j<numTerms; j++){
				if(termICFIndex.containsKey(terms.get(j))){
					double score = centroids[i][j] * clusters.get(i).size() * termICFIndex.get(terms.get(j))/ idfIndex.get(terms.get(j));
					TermScore termScore = new TermScore(terms.get(j), score);
					clusterSummary.get(i).add(termScore);
				}
			}
//			for(int j=0; j<numTerms; j++){
//				double score = 0.0;
//				for(int docId: clusters.get(i)){
//					score += termVectors[docIdIndex.get(docId)][j];
//				}
//				TermScore termScore = new TermScore(terms.get(j), score);
//				clusterSummary.get(i).add(termScore);
//			}
			Collections.sort(clusterSummary.get(i), new Comparator() {
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
			System.out.println("Summary of Cluster " + i);
			for(int w=0; w<10; w++){
				System.out.println(" Text : " + clusterSummary.get(i).get(w).getTermText() + " Score : " + clusterSummary.get(i).get(w).getTermScore()); 
			}
			System.out.println(" **************");
		}
		calcEuclDistOfDocs();
		calcIntraClusterTightness();
		calcInterClusterTightness();
	}
	
	private void calcEuclDistOfDocs(){
		for(int i=0; i<termVectors.length; i++){
			double sum = 0.0;
			for (int j = 0; j < numTerms; j++) {
				sum += termVectors[i][j] *  termVectors[i][j];
			}
			euclDistOfDocs[i] = Math.sqrt(sum);
		}
	}
	
	private void calcIntraClusterTightness(){
		for(int i=0; i<clusters.size(); i++){
			double similarity = 0.0;
			for(int docId : clusters.get(i)){
				int docIndex = docIdIndex.get(docId);
				similarity += (getVectorSimilarityScore(i, docId)/euclDistOfDocs[docIndex]);
			}
			System.out.println(" Intra Cluster tightness for Cluster " + i + " : " + similarity / clusters.get(i).size());
		}
	}
	
	private void calcInterClusterTightness(){
		double avgSimilarity = 0.0;
		for(int i=0; i<clusters.size()-1; i++){
			for(int j=i+1; j<clusters.size(); j++){
				double similarity =  0.0;
				for(int k=0; k<numTerms; k++){
					similarity += centroids[i][k] * centroids[j][k];
				}
				avgSimilarity += similarity / (euclDistOfCentroids[i] * euclDistOfCentroids[j]);
			}
		}
		int numClusters = clusters.size();
		avgSimilarity = (avgSimilarity * 2) / (numClusters * (numClusters -1));
		System.out.println(" Inter Cluster tightness : " + avgSimilarity);
	}
	
	private void calcEuclDistOfCentroids() {
		for (int clusterIndex = 0; clusterIndex < k; clusterIndex++) {
			double sum = 0.0;
			for (int i = 0; i < numTerms; i++) {
				sum += centroids[clusterIndex][i] * centroids[clusterIndex][i];
			}
			euclDistOfCentroids[clusterIndex] = Math.sqrt(sum);
		}
	}

	private double getVectorSimilarityScore(int clusterIndex, int docId) {
		double score = 0;
		int docIndex = docIdIndex.get(docId);
		for (int i = bitVectors.get(docIndex).nextSetBit(0); i >= 0; i = bitVectors
				.get(docIndex).nextSetBit(i + 1)) {
			score += (termVectors[docIndex][i] * centroids[clusterIndex][i]);
		}
		return score / euclDistOfCentroids[clusterIndex];
	}

	private void updateCluster() {
		clusters = newClusters;
		newClusters = new ArrayList<ArrayList<Integer>>(k);
		for (int i = 0; i < k; i++) {
			newClusters.add(new ArrayList<Integer>());
		}
	}

	private boolean verifyStability() {
		for (int i = 0; i < k; i++) {
			if (!clusters.get(i).equals(newClusters.get(i))) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private void printClusters() throws IOException {
		IndexReader reader = IndexReader.open("result3index");
		for (int i = 0; i < k; i++) {
			System.out.println(" Cluster " + i + " ");
			ArrayList<DocScore> topDocs = new ArrayList<DocScore>();
			for(int docId : clusters.get(i)){
				DocScore docScore = new DocScore(docId, getVectorSimilarityScore(i, docId));
				topDocs.add(docScore);
			}
			Collections.sort(topDocs, new Comparator() {
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
			for (DocScore docScore : topDocs) {
				System.out.println("Doc Id: " + docScore.getDocId() + "  Url : "
						+ reader.document(docScore.getDocId()).getField("url")
						.stringValue() + " Score : " + docScore.getScore());
			}
			System.out.println("****************");
		}
		reader.close();
	}

	// For Testing 
	
//	public static void main(String[] args) throws IOException {
//		KMeansCluster kMeansCluster = new KMeansCluster();
//		ArrayList<DocScore> docScores = new ArrayList<DocScore>();
//		// Test Data
//		DocScore docScore = new DocScore(0, 0.5);
//		docScores.add(docScore);
//		docScore = new DocScore(1, 0.6);
//		docScores.add(docScore);
//		docScore = new DocScore(2, 0.7);
//		docScores.add(docScore);
//		docScore = new DocScore(3, 0.8);
//		docScores.add(docScore);
//		docScore = new DocScore(4, 0.22);
//		docScores.add(docScore);
//		docScore = new DocScore(5, 0.43);
//		docScores.add(docScore);
//		docScore = new DocScore(6, 0.93);
//		docScores.add(docScore);
//		docScore = new DocScore(7, 0.13);
//		docScores.add(docScore);
//		docScore = new DocScore(8, 0.33);
//		docScores.add(docScore);
//		docScore = new DocScore(9, 0.55);
//		docScores.add(docScore);
//
//		kMeansCluster.createKMeansClusters(docScores, 3);
//		// Start Clustering
//		// kMeansCluster.createClusters(docScores, 3);
//
//		/*
//		 * ArrayList<Integer> a = new ArrayList<Integer>(); a.add(3); a.add(4);
//		 * a.add(5); ArrayList<Integer> b = new ArrayList<Integer>(); b.add(3);
//		 * b.add(4); b.add(5); System.out.println("Equal: : " + a.equals(b));
//		 * ArrayList<Integer> c = a; a.clear();
//		 * System.out.println("Length of a : " + c.size());
//		 */
//
//	}
}
