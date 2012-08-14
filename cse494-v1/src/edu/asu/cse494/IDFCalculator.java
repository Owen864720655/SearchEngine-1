package edu.asu.cse494;

import com.lucene.index.*;

import java.io.*;
import java.util.HashMap;

public class IDFCalculator {
	int count = 0;
	HashMap<String, Double> idfIndex = new HashMap<String, Double>();

	// create IDF index
	public void createIDFIndex() {
		try {
			IndexReader reader = IndexReader.open("result3index");
			int totalDocs =  reader.numDocs();
			TermEnum termEnum = reader.terms();
			while (termEnum.next()) {
				Term termVal = termEnum.term();
				if ((termVal.field()).equalsIgnoreCase("contents")) {
					double termIDF = Math
							.log((double) totalDocs / termEnum
									.docFreq());
					idfIndex.put(termVal.text().toLowerCase(), termIDF);
				}
			}
			reader.close();
		} catch (IOException e) {
			System.out.println("IO Error has occured: " + e);
			return;
		}
	}

	public void serializeIDFIndex() {
		try {
			FileOutputStream fout = new FileOutputStream(
					"result3index/idfIndex.ser");
			System.out.println("File created");
			ObjectOutputStream objOut = new ObjectOutputStream(fout);
			objOut.writeObject(idfIndex);
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
			fis = new FileInputStream("result3index/idfIndex.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			HashMap<String, Double> retrievedIndex = (HashMap<String, Double>) ois
					.readObject();
			System.out.println(" From retrieved Index : ");
			double termIDF = retrievedIndex.get("aa");
			System.out.println("IDF of aa : " + termIDF);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		IDFCalculator idfCalculator = new IDFCalculator();
		idfCalculator.createIDFIndex();
		idfCalculator.serializeIDFIndex();
		idfCalculator.testSerializedIndex();
	}
}
