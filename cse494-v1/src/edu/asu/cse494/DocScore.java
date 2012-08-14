package edu.asu.cse494;

public class DocScore {
	private int docId;
	private double score;
	
	public DocScore(){
		
	}
	
	public DocScore(int docId, double score){
		this.docId = docId;
		this.score = score;
	}
	
	public int getDocId() {
		return docId;
	}
	public void setDocId(int docId) {
		this.docId = docId;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
}
