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

public class DocumentEuclideanDistance {
	int count = 0;
	HashMap<Integer, Integer> docEuclDistIndex = new HashMap<Integer, Integer>();

	public void createDocEuclDistIndex() {
		try {
			IndexReader reader = IndexReader.open("result3index");
			System.out.println(" Number of Docs in Index :" + reader.numDocs());
			TermEnum termEnum = reader.terms();
			while (termEnum.next()) {
				Term termVal = termEnum.term();
				TermDocs termDocs = reader.termDocs(termVal);
				// if (termVal.field().equalsIgnoreCase("contents")) {
				while (termDocs.next()) {
					int docId = termDocs.doc();
					int termFreq = termDocs.freq();
					int previousValue = 0;
					if (docEuclDistIndex.containsKey(docId)) {
						previousValue = docEuclDistIndex.get(docId);
					}
					int newValue = previousValue + (termFreq * termFreq);
					docEuclDistIndex.put(docId, newValue);
				}
			}
			// }
			reader.close();
			System.out.println("No of K,V pairs : " + docEuclDistIndex.size());
			System.out.println("Index creation complete");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	public void serializeIndex() {
		try {
			FileOutputStream fout = new FileOutputStream(
					"result3index/docEuclDistIndex.ser");
			System.out.println("File created");
			ObjectOutputStream objOut = new ObjectOutputStream(fout);
			objOut.writeObject(docEuclDistIndex);
			System.out.println("Serialization complete");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException ioException) {
			// TODO Auto-generated catch block
			ioException.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void testSerializedIndex() {
		FileInputStream fis;
		try {
			fis = new FileInputStream("result3index/docEuclDistIndex.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			HashMap<Integer, Integer> retrievedIndex = (HashMap<Integer, Integer>) ois
					.readObject();
			System.out.println(" From retrieved Index : ");
			System.out.println(" Eucl dist for doc no 106 : "
					+ retrievedIndex.get(106));
			System.out.println(" Eucl dist for doc no 5 : "
					+ retrievedIndex.get(5));

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

	public static void main(String[] args) {
		DocumentEuclideanDistance docEuclDist = new DocumentEuclideanDistance();
		docEuclDist.createDocEuclDistIndex();
		docEuclDist.serializeIndex();
		docEuclDist.testSerializedIndex();
	}
}
