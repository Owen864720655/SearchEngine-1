package edu.asu.cse494;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import com.lucene.index.IndexReader;
import com.lucene.index.Term;
import com.lucene.index.TermDocs;
import com.lucene.index.TermEnum;

public class DocumentEuclideanDistanceWithIDF {
	int count = 0;
	HashMap<Integer, Double> docEuclDistIndexWithIDF = new HashMap<Integer, Double>();

	public void createDocEuclDistIndexWithIDF() {
		try {
			IndexReader reader = IndexReader.open("result3index");
			int numDocsInCorpus = reader.numDocs();
			// if(numDocs <= 0){
			// System.out.println("No Docs in corpus");
			// System.exit(0);
			// }
			System.out
					.println(" Number of Docs in corpus :" + reader.numDocs());
			TermEnum termEnum = reader.terms();
			while (termEnum.next()) {
				Term termVal = termEnum.term();
				TermDocs termDocs = reader.termDocs(termVal);
				// if (termVal.field().equalsIgnoreCase("contents")) {
				double termIDF = Math.log((double) numDocsInCorpus
						/ termEnum.docFreq());
				while (termDocs.next()) {
					int docId = termDocs.doc();
					int termFreq = termDocs.freq();
					double previousValue = 0;
					if (docEuclDistIndexWithIDF.containsKey(docId)) {
						previousValue = docEuclDistIndexWithIDF.get(docId);
					}
					double newValue = previousValue + (termFreq * termIDF)
							* (termFreq * termIDF);
					docEuclDistIndexWithIDF.put(docId, newValue);
				}
			}
			// }
			reader.close();
			System.out.println("No of K,V pairs : "
					+ docEuclDistIndexWithIDF.size());
			System.out.println("Index creation complete");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	public void serializeIndex() {
		try {
			FileOutputStream fout = new FileOutputStream(
					"result3index/docEuclDistIndexWithIDF.ser");
			System.out.println("File created");
			ObjectOutputStream objOut = new ObjectOutputStream(fout);
			objOut.writeObject(docEuclDistIndexWithIDF);
			System.out.println("Serialization complete");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void testSerializedIndex() {
		FileInputStream fis;
		try {
			fis = new FileInputStream(
					"result3index/docEuclDistIndexWithIDF.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			HashMap<Integer, Double> retrievedIndex = (HashMap<Integer, Double>) ois
					.readObject();
			System.out.println(" From retrieved Index : ");
			System.out.println(" Eucl dist for doc no 106 : "
					+ 1/ Math.sqrt(retrievedIndex.get(106)));
			System.out.println(" Eucl dist for doc no 5 : "
					+ 1/Math.sqrt(retrievedIndex.get(5)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		DocumentEuclideanDistanceWithIDF docEuclDistWithIDF = new DocumentEuclideanDistanceWithIDF();
		docEuclDistWithIDF.createDocEuclDistIndexWithIDF();
		docEuclDistWithIDF.serializeIndex();
		docEuclDistWithIDF.testSerializedIndex();
	}
}
