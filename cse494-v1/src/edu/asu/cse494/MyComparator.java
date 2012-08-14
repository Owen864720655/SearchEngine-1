package edu.asu.cse494;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class MyComparator implements Comparator {

	Map rankedDocs;

	public MyComparator() {
		// TODO Auto-generated constructor stub
	}

	public MyComparator(Map rankMap) {
		this.rankedDocs = rankMap;
	}

	@Override
	public int compare(Object docA, Object docB) {
		double aVal = (Double) rankedDocs.get(docA);
		double bVal = (Double) rankedDocs.get(docB);
		// System.out.println(" aVal : "+ aVal + "  bVal : " + bVal);
		if (bVal < aVal) {
			return -1;
		} else if (bVal == aVal) {
			return 0;
		} else {
			return 1;
		}
	}
}