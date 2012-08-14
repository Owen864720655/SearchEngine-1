package edu.asu.cse494;
import com.lucene.index.*;
import java.io.*;


public class VectorViewer {
	int count=0;
	//display the vector
	public void  showVector()
	{
		// lists the vector
		try{
			IndexReader reader = IndexReader.open("result3index");
			System.out.println(" Number of Docs in Index :" + reader.numDocs());
			
			// use the TermEnum object to iterate through all the terms in the index
			TermEnum termenum = reader.terms();
			System.out.println("Printing the Terms and the Frequency \n");
			while(termenum.next())
			{
				count++;
				Term termval = termenum.term();
				System.out.println("The Term :" + termval.text() + " Frequency :"+termenum.docFreq());
				
				
				   //Add following here to retrieve the <docNo,Freq> pair for each term
//				   TermDocs termDocs = reader.termDocs(termval);
//   			   System.out.println(" Doc No : " + termDocs.doc() + " Freq : " + termDocs.freq() );
				   
			
				   //to retrieve the <docNo,Freq,<pos1,......posn>> call
//				   TermPositions tpos = reader.termPositions(termval);
				   //TermPositions termpositions = termval.termPositions(termval)
				
			
			}
			System.out.println(" Total terms : " + count);
		
		}
		catch(IOException e){
		    System.out.println("IO Error has occured: "+ e);
		    return;
		}
	}


	public static void main(String[] args)
	{
		VectorViewer CSE494Viewer = new VectorViewer();
		CSE494Viewer.showVector();
		System.out.println(" Total terms : " + CSE494Viewer.count);
	}
}
