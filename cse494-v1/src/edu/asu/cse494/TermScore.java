package edu.asu.cse494;

public class TermScore {
	private String termText;
	private double termScore;
	
	public TermScore(){
		
	}
	
	public TermScore(String text, double score){
		termText = text;
		termScore = score;
	}
	
	public String getTermText() {
		return termText;
	}
	public void setTermText(String termText) {
		this.termText = termText;
	}
	public double getTermScore() {
		return termScore;
	}
	public void setTermScore(double score) {
		this.termScore = score;
	}
	
}
