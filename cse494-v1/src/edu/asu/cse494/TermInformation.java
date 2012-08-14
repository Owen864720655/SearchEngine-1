package edu.asu.cse494;

import java.io.Serializable;
import java.util.ArrayList;

public class TermInformation implements Serializable{

	private int numDocs = 0;
	private ArrayList<DocIdFreqStore> docIdFreqStoreList;

	public TermInformation() {

	}

	public TermInformation(int numDocs, ArrayList<DocIdFreqStore> arrList) {
		this.numDocs = numDocs;
		this.docIdFreqStoreList = arrList;
	}

	public int getNumDocs() {
		return numDocs;
	}

	public void setNumDocs(int numDocs) {
		this.numDocs = numDocs;
	}

	public ArrayList<DocIdFreqStore> getDocIdFreqStoreList() {
		return docIdFreqStoreList;
	}

	public void setDocIdFreqStoreList(
			ArrayList<DocIdFreqStore> docIdFreqStoreList) {
		this.docIdFreqStoreList = docIdFreqStoreList;
	}

}
