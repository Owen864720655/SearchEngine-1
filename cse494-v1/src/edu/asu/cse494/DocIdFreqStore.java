package edu.asu.cse494;

import java.io.Serializable;

public class DocIdFreqStore implements Serializable{

	int docId;
	int freq;

	public DocIdFreqStore() {

	}

	public DocIdFreqStore(int docId, int freq) {
		this.docId = docId;
		this.freq = freq;
	}

	public int getDocId() {
		return docId;
	}

	public void setDocId(int docId) {
		this.docId = docId;
	}

	public int getFreq() {
		return freq;
	}

	public void setFreq(int freq) {
		this.freq = freq;
	}

}
