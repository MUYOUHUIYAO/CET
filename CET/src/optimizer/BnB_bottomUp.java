package optimizer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import event.*;

public class BnB_bottomUp extends Partitioner {	
	
	ArrayList<Event> events;
	
	public BnB_bottomUp (ArrayDeque<Window> w, ArrayList<Event> e) {
		super(w);
		events = e;
	}
	
	public Partitioning getPartitioning (Partitioning root, double memory_limit, int bin_number, int bin_size) {
		
		// M-CET in the non-partitioned graph root
		Partitioning solution = root;		
		double minCPU = root.getCPUcost(windows,1);
		int maxHeapSize = 0;
		
		// H-CET in the partitioned graph starting from min_partitions
		Partitioning min_partitions = Partitioning.getPartitioningWithMinPartitions(events);						
		LinkedList<Partitioning> heap = new LinkedList<Partitioning>();
		heap.add(min_partitions);
		
		int pruning_1_count = 0;
		int pruning_2_count = 0;
		int considered_count = 0;
		int improved_count = 0;
		
		while (!heap.isEmpty()) {
			
			// Get current node and compute its costs 
			Partitioning temp = heap.poll();			
			double temp_cpu = temp.getCPUcost(windows,3);
			double temp_mem = temp.getMEMcost(windows,3);
			considered_count++;
						
			//System.out.println("Considered: " + temp.toString());
			
			// Update the best solution seen so far
			if (minCPU > temp_cpu && temp_mem <= memory_limit) {
				minCPU = temp_cpu;
				solution = temp;
				improved_count++;
				
				//System.out.println("Best so far: " + solution.toString());
			}
			// Add children to the heap and store their memory cost
			ArrayList<Partitioning> children = temp.getChildrenByMerging();
			for (Partitioning child : children) {
				double child_mem = child.getMEMcost(windows,3);
				double child_cpu = child.getCPUcost(windows,3);
				
				if  (child_mem > memory_limit) pruning_1_count++;
				if  (child_cpu > minCPU) pruning_2_count++;
				
				if (child_mem <= memory_limit && child_cpu <= minCPU && !heap.contains(child)) {
					heap.add(child);					
				}
			}
			// Update max heap size
			if (maxHeapSize < heap.size()) maxHeapSize = heap.size();			
		}
		System.out.println("Max heap size: " + maxHeapSize + 
				"\nPruning 1: " + pruning_1_count + 
				"\nPruning 2: " + pruning_2_count + 
				"\nConsidered: " + considered_count +
				"\nImproved: " + improved_count);				
		
		return solution;		
	}
}