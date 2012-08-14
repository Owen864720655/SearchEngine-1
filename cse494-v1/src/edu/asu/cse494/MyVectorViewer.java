package edu.asu.cse494;

import com.lucene.index.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class MyVectorViewer {
	int count = 0;
//	int stopWatch = 25000;
	HashMap<String, TermInformation> invertedFile = new HashMap<String, TermInformation>();

	// display the vector
	public void showVector() {
		// lists the vector
		try {
			IndexReader reader = IndexReader.open("result3index");
			System.out.println(" Number of Docs in Index :" + reader.numDocs());

			TermEnum termEnum = reader.terms();
			System.out.println("Printing the Terms and the Frequency \n");
			int termCounter = 0;

			while (termEnum.next()) {
				Term termVal = termEnum.term();
				TermDocs termDocs = reader.termDocs(termVal);
				termCounter++;
//				System.out.println("Term text = " + termVal.text()
//						+ " Term field = " + termVal.field());
				if (termVal.field().equalsIgnoreCase("contents")) {
					System.out.println("Term text = " + termVal.text()
							+ " Term field = " + termVal.field());
					int numDocsWithTerm = 0;
					ArrayList<DocIdFreqStore> docIdFreqStoreList = new ArrayList<DocIdFreqStore>();
					while (termDocs.next()) {
						numDocsWithTerm++;
						DocIdFreqStore docIdFreqStore = new DocIdFreqStore(
								termDocs.doc(), termDocs.freq());
						docIdFreqStoreList.add(docIdFreqStore);
						// docIdFreqStore.setDocId(termDocs.doc());
						// docIdFreqStore.setFreq(termDocs.freq());
						// System.out.println(" Doc No : " + termDocs.doc() +
						// " Freq : " + termDocs.freq() );
					}
					TermInformation termInfo = new TermInformation(
							numDocsWithTerm, docIdFreqStoreList);
					invertedFile.put(termVal.text(), termInfo);
				}
				// System.out.println("Total docs containing term : " +
				// // numDocsWithTerm);
//				if (termCounter == stopWatch) {
//					break;
//				}
			}

			/*
			 * while (termenum.next()) { Term termval = termenum.term();
			 * TermPositions termPositions = reader.termPositions(termval);
			 * termPositions.next(); termCounter++;
			 * System.out.println(" Term Freq : " + termPositions.freq() +
			 * "  Term Doc : " + termPositions.doc() + "  Term Text: " +
			 * termval.text() + "  Term Field: " + termval.field());
			 * System.out.print("Positions in the Doc : "); int posCount = 0;
			 * while (posCount < termPositions.freq()) {
			 * System.out.print(termPositions.nextPosition() + " "); posCount++;
			 * } System.out.println("\n"); if (termCounter == 2) { break; } }
			 */
		} catch (IOException e) {
			System.out.println("IO Error has occured: " + e);
			return;
		}
	}

	public static void main(String[] args) {
		MyVectorViewer CSE494Viewer = new MyVectorViewer();
		CSE494Viewer.showVector();
		TermInformation termInfo = CSE494Viewer.invertedFile.get("aa");
		if (termInfo == null) {
			System.out.println("Error in Hash Map");
		} else {
			System.out.println("No of Docs containing aa : "
					+ termInfo.getNumDocs());
		}
		// System.out.println(" Total terms : " + CSE494Viewer.count);
	}
}
