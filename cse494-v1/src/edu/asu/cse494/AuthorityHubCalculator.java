package edu.asu.cse494;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import com.lucene.index.IndexReader;

public class AuthorityHubCalculator {

	private LinkAnalysis linkAnalysis;
	private double[] authorityVector;
	private double[] hubVector;
	private int[][] adjacencyMatrix;
	private int[][] adjacencyMatrixTranspose;
	private int numDocs;
	private HashSet<Integer> baseSet;
	private ArrayList<DocScore> rankedNodes;
	private HashMap<Integer, Integer> docIdIndex;
	private final static double THRESHOLD = 0.0001;  

	public void calculateAuthorityHub(ArrayList<DocScore> topRankedDocs) throws IOException {
		rankedNodes = new ArrayList<DocScore>();
		docIdIndex = new HashMap<Integer, Integer>();
		baseSet = new HashSet<Integer>();
		linkAnalysis = new LinkAnalysis();
		boolean isStable = false;
		int[] docIds = new int[topRankedDocs.size()];
		for(int i=0; i<topRankedDocs.size(); i++){
			docIds[i] = topRankedDocs.get(i).getDocId();
		}
		createAdjcencyMatrix(docIds);
		initializeAuthorityVector();
		initializeHubVector();
		double[] temporaryVector = new double[numDocs];
		int numIterations = 0;
		long startTIme = System.nanoTime();
		while(true) {
			numIterations++;
			updateAuthVector();
			normalizeVector(true);
			updateHubVector(temporaryVector);
			normalizeVector(false);
			isStable = verifyStability(temporaryVector);
			if(isStable){
				System.out.println("Stability reached after Iteration : " + numIterations);
				break;
			}
		}
		System.out.println("Time taken to do Matrix Multiplication :  " + (System.nanoTime() - startTIme) + " nano seconds");
		System.out.println("Printing top 10 Authorities");
		 rankNodes(true);
		 writeResult(true);
		 System.out.println("Printing top 10 Hubs");
		 rankNodes(false);
		 writeResult(false);
	}

	private boolean verifyStability(double[] temporaryVector) {
		double diff = 0;
		for(int i=0; i<numDocs; i++){
			diff += ((hubVector[i] - temporaryVector[i])*(hubVector[i] - temporaryVector[i]));
		}
		if(diff <= THRESHOLD){
			return true;
		}
		return false;
	}

	/**
	 * @param topRankedDocs
	 */
	private void createAdjcencyMatrix(int[] topRankedDocs) {
		long startTIme = System.nanoTime();
		// Create Base Set
		for (int i = 0; i < topRankedDocs.length; i++) {
			baseSet.add(topRankedDocs[i]);
			int[] backLinks = linkAnalysis.getCitations(topRankedDocs[i]);
			int[] fwdLinks = linkAnalysis.getLinks(topRankedDocs[i]);
			for (int backLink : backLinks) {
				baseSet.add(backLink);
			}
			for (int fwdLink : fwdLinks) {
				baseSet.add(fwdLink);
			}
		}
		numDocs = baseSet.size();
		// Create Doc Id index
		int index = 0;
		for (int i : baseSet) {
			docIdIndex.put(i, index);
			index++;
		}
		// Create Adjacency Matrix
		adjacencyMatrix = new int[numDocs][numDocs];
		adjacencyMatrixTranspose = new int[numDocs][numDocs];
		for (int docId : baseSet) {
			int[] fwdLinks = linkAnalysis.getLinks(docId);
			int docIdPosition = docIdIndex.get(docId);
			for (int node : fwdLinks) {
				if (docIdIndex.containsKey(node)) {
					int nodePosition = docIdIndex.get(node);
					adjacencyMatrix[docIdPosition][nodePosition] = 1;
					adjacencyMatrixTranspose[nodePosition][docIdPosition] = 1;
				}
			}
		}
		System.out.println("Time taken to compute Adjacency Matrix :  " + (System.nanoTime() - startTIme) + " nano seconds");
	}

	private void updateAuthVector() {
		
		for (int i = 0; i < numDocs; i++) {
			double newVal = 0;
			for (int j = 0; j < numDocs; j++) {
				newVal += (adjacencyMatrixTranspose[i][j] * hubVector[j]);
			}
			authorityVector[i] = newVal;
		}
	}

	private void updateHubVector(double[] tempVector) {
		for (int i = 0; i < numDocs; i++) {
			double newVal = 0;
			tempVector[i] = hubVector[i];
			for (int j = 0; j < numDocs; j++) {
				newVal += (adjacencyMatrix[i][j] * authorityVector[j]);
			}
			hubVector[i] = newVal;
		}
	}

	@SuppressWarnings("unchecked")
	private void rankNodes(boolean rankAuthority) throws IOException {
		rankedNodes.clear();
		DocScore nodeScore;
		for (int docId : baseSet) {
			if (rankAuthority) {
				nodeScore = new DocScore(docId, authorityVector[docIdIndex.get(docId)]);
			} else {
				nodeScore = new DocScore(docId, hubVector[docIdIndex.get(docId)]);
			}
			rankedNodes.add(nodeScore);
		}
		Collections.sort(rankedNodes, new Comparator() {
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
		for(int i=0; i<10; i++){
			DocScore node = rankedNodes.get(i);
			System.out.println("Doc id : " + String.valueOf(node.getDocId()) + " Score:  "
					+ String.valueOf(node.getScore()) + "  Url : " + reader.document(rankedNodes.get(i).getDocId()).getField("url"));
		}
		reader.close();
	}

	private void writeResult(boolean writeAuthority) {
		try {
			FileOutputStream fout;
			IndexReader reader = IndexReader.open("result3index");
			if (writeAuthority) {
				fout = new FileOutputStream("result3index/AuthorityOutput.txt");
			} else {
				fout = new FileOutputStream("result3index/HubOutput.txt");
			}
			OutputStreamWriter out = new OutputStreamWriter(fout);
			System.out.println(" Authorities are stored in AuthorityOutput.txt file under result3index folder");
			System.out.println(" HUbs are stored in HubOutput.txt file under result3index folder");
			
			for (int i = 0; i < numDocs; i++) {
				DocScore node = rankedNodes.get(i);
				out.write(String.valueOf(node.getDocId()) + "    "
						+ String.valueOf(node.getScore()) + "  Url : " + reader.document(rankedNodes.get(i).getDocId()).getField("url").stringValue() + "\n");
			}
			out.close();
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	private void normalizeVector(boolean normalizeAuthority) {
		double normFactor = 0;
		if (normalizeAuthority) {
			for (int i = 0; i < numDocs; i++) {
				normFactor += (authorityVector[i] * authorityVector[i]);
			}
			normFactor = Math.sqrt(normFactor);
			for (int i = 0; i < numDocs; i++) {
				authorityVector[i] = authorityVector[i] / normFactor;
			}
		} else {
			for (int i = 0; i < numDocs; i++) {
				normFactor += (hubVector[i] * hubVector[i]);
			}
			normFactor = Math.sqrt(normFactor);
			for (int i = 0; i < numDocs; i++) {
				hubVector[i] = hubVector[i] / normFactor;
			}
		}
	}

	private void initializeHubVector() {
		hubVector = new double[numDocs];
		Arrays.fill(hubVector,1.0);
	}

	private void initializeAuthorityVector() {
		authorityVector = new double[numDocs];
		Arrays.fill(authorityVector,1.0);
	}

	public static void main(String[] args) throws IOException {
		AuthorityHubCalculator authHubCalculator = new AuthorityHubCalculator();
		authHubCalculator.linkAnalysis = new LinkAnalysis();
		DocScore d = new DocScore(1, 1.88);
		ArrayList<DocScore> docList = new ArrayList<DocScore>();
		docList.add(d);
		authHubCalculator.calculateAuthorityHub(docList);
	}

}
