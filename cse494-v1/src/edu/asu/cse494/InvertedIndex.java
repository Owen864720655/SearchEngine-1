package edu.asu.cse494;

import com.lucene.index.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class InvertedIndex {

	int count = 0;
	HashMap<String, TermInformation> invertedFile = new HashMap<String, TermInformation>();

	// create Inverted Index
	public void createInvertedIndex() {
		try {
			IndexReader reader = IndexReader.open("result3index");
			System.out.println(" Number of Docs in Index :" + reader.numDocs());

			TermEnum termEnum = reader.terms();
			int termCounter = 0;

			while (termEnum.next()) {
				Term termVal = termEnum.term();
				TermDocs termDocs = reader.termDocs(termVal);
				termCounter++;
				if (termVal.field().equalsIgnoreCase("contents")) {
					// System.out.println("Term text = " + termVal.text()
					// + " Term field = " + termVal.field());
					int numDocsWithTerm = 0;
					ArrayList<DocIdFreqStore> docIdFreStoreList = new ArrayList<DocIdFreqStore>();
					while (termDocs.next()) {
						numDocsWithTerm++;
						DocIdFreqStore docIdFreqStore = new DocIdFreqStore(
								termDocs.doc(), termDocs.freq());
						docIdFreStoreList.add(docIdFreqStore);
					}
					TermInformation termInfo = new TermInformation(
							numDocsWithTerm, docIdFreStoreList);
					invertedFile.put(termVal.text().toLowerCase(), termInfo);
				}
			}
			reader.close();
			System.out.println("Total Terms : " + termCounter);
			System.out.println("Inverted Index Created");

		} catch (IOException e) {
			System.out.println("IO Error has occured: " + e);
			return;
		}
	}

	public void serializeIndex() {
		try {
			FileOutputStream fout = new FileOutputStream(
					"result3index/invertedIndex.ser");
			System.out.println("File created");
			ObjectOutputStream objOut = new ObjectOutputStream(fout);
			objOut.writeObject(invertedFile);
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
			fis = new FileInputStream("result3index/invertedIndex.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			HashMap<String, TermInformation> retrievedIndex = (HashMap<String, TermInformation>) ois
					.readObject();
			System.out.println(" From retrieved Index : ");
			TermInformation termInfo = retrievedIndex.get("aa");
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

	public static void main(String[] args) {
		InvertedIndex invertedIndex = new InvertedIndex();
		invertedIndex.createInvertedIndex();
		invertedIndex.serializeIndex();
		System.out.println(" No of K,v pairs : "
				+ invertedIndex.invertedFile.size());
		// TermInformation termInfo = invertedIndex.invertedFile.get("aa");
		// if (termInfo == null) {
		// System.out.println("Error in Hash Map");
		// } else {
		// System.out.println("No of Docs containing aa : "
		// + termInfo.getNumDocs());
		// }
		invertedIndex.testSerializedIndex();
	}
}
