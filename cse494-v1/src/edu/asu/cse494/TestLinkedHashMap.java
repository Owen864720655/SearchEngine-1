package edu.asu.cse494;

import java.util.Comparator;
import java.util.PriorityQueue;

public class TestLinkedHashMap {

	PriorityQueue<String> pQ;

	void init() {
		pQ = new PriorityQueue<String>(5, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				int val = o1.compareToIgnoreCase(o2);
				if (val < 1) {
					return -1;
				} else if (val > 1) {
					return 1;
				}
				return 0;
			}
		});
	}

	public static void main(String[] args) {
		TestLinkedHashMap testObj = new TestLinkedHashMap();
		testObj.init();
		testObj.pQ.add("Shreejay");
		testObj.pQ.add("Abhi");
		testObj.pQ.add("Hiten");
		testObj.pQ.add("Rajeev");

		System.out.println(testObj.pQ.peek());
	}

}
