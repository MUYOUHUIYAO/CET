package graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import event.*;
import optimizer.*;

public class Partition extends Graph {
	
	public String id;
	public int start;
	public int end;
	public int vertexNumber;	
		
	public Partition (int s, int e, int vn, int en, ArrayList<Node> fn, ArrayList<Node> ln, int n) {
		id = s + " " + e;
		start = s;
		end = e;
		
		vertexNumber = vn;
		edgeNumber = en;
		
		first_nodes = fn;
		last_nodes = ln;
		
		number_of_min_partitions = n;
	}
	
	public boolean equals (Object o) {
		Partition other = (Partition) o;
		return this.id.equals(other.id);
	}
	
	/*** Returns a minimal partition for events with the same time stamp ***/
	public static Partition getMinPartition (int sec, ArrayList<Event> batch) {
		ArrayList<Node> nodes = new ArrayList<Node>();
		for (Event e : batch) {
			Node n = new Node(e);
			nodes.add(n);
		}
		return new Partition (sec, sec, batch.size(), 0, nodes, nodes, 1);
	}
	
	/*** Get minimal number of required partitions ***/
	public int getMinNumberOfRequiredPartitions_walkDown(int memory_limit) {	
		
		// Find the minimal number of required partitions (T-CET, H-CET)
		for (int k=0; k<number_of_min_partitions; k++) {	
			double ideal_memory = getIdealMEMcost(k);
						
			System.out.println("k=" + k + " mem=" + ideal_memory);
			
			if (ideal_memory <= memory_limit) return k;
		}	
		// Each event is in a separate partition (M-CET)
		if (vertexNumber <= memory_limit) return vertexNumber;
		
		// Partitioning does not reduce the memory enough
		return -1;
	}
	
	public int getMinNumberOfRequiredPartitions_walkUp(int memory_limit) {	
		
		// Partitioning does not reduce the memory enough
		int result = -1;
		
		// Each event is in a separate partition (M-CET)
		if (vertexNumber <= memory_limit) result = vertexNumber;
		
		// Find the minimal number of required partitions (T-CET, H-CET)
		for (int k=number_of_min_partitions-1; k>=0; k--) {
			double ideal_memory = getIdealMEMcost(k);
			if (ideal_memory <= memory_limit) {
				result = k;
				
				System.out.println("k=" + k + " mem=" + ideal_memory);
			} else {
				break;
			}
		}				
		return result;
	}
	
	public double getIdealMEMcost (int k) {
		
		double exp;
		double ideal_memory;
		
		if (k == 0) {			
			exp = vertexNumber/new Double(3);
			ideal_memory = Math.pow(3, exp) * vertexNumber;			
		} else {			
			double vertex_number_per_partition = vertexNumber/new Double(k);
			exp = vertex_number_per_partition/new Double(3);			
			ideal_memory = k * Math.pow(3, exp) * vertex_number_per_partition;			
		}	
		return ideal_memory;
	}
	
	public int getSharingWindowNumber (ArrayDeque<Window> windows) {
		int count = 0;
		for (Window window : windows) {
			if (window.contains(this)) count++;  
		}
		return count;
	}
	
	public boolean is2write (ArrayDeque<Window> windows, Window window) {
		for (Window w : windows) {
			if (w.contains(this)) {
				return w.equals(window);  
			}
		}
		return false;
	}
	
	public boolean isShared (ArrayDeque<Window> windows) {
		return getSharingWindowNumber(windows)>1;
	}
	 
	/*** Get CPU cost of this partition ***/
	public double getCPUcost (ArrayDeque<Window> windows) {
		double cost = edgeNumber + Math.pow(3, Math.floor(vertexNumber/3));		
		int windowNumber = getSharingWindowNumber(windows);
		double final_cost = (windowNumber>1) ? cost/windowNumber : cost;
		return final_cost;
	}
	
	/*** Get memory cost of this partition ***/
	public double getMEMcost (ArrayDeque<Window> windows) {
		double cost = vertexNumber * Math.pow(3, Math.floor(vertexNumber/3)); 		
		int windowNumber = getSharingWindowNumber(windows);
		double final_cost = (windowNumber>1) ? cost/windowNumber : cost;
		return final_cost;
	}
	
	/*** Get actual memory requirement of this partition ***/
	public int getCETlength () {
		int count = 0;
		for (Node first_node : first_nodes) {
			for (EventTrend result : first_node.results) {
				count += result.getEventNumber();
			}
		}
		return count;
	}
	
	/*** Split input partition and return the resulting partitions ***/
	public ArrayList<Partitioning> split () {	
		
		ArrayList<Partitioning> results = new ArrayList<Partitioning>();
		
		// Initial partitions
		Partition first = new Partition(0,0,0,0,new ArrayList<Node>(),new ArrayList<Node>(),0);
		Partition second = this;
		
		// Nodes
		ArrayList<Node> nodes2move = second.first_nodes;
		ArrayList<Node> followingOfNodes2move = new ArrayList<Node>();
		for (Node node2move : nodes2move) {
			for (Node following : node2move.following) {
				if (!followingOfNodes2move.contains(following)) followingOfNodes2move.add(following);			
		}}
		
		// Second
		int secOfNodes2move = nodes2move.get(0).event.sec;
		int secOfFollowingOfNodes2move = (followingOfNodes2move.isEmpty()) ? 0 : followingOfNodes2move.get(0).event.sec;
		
		while (!followingOfNodes2move.isEmpty() && secOfFollowingOfNodes2move <= end) {		
						
			// Vertexes
			int new_first_vn = first.vertexNumber + nodes2move.size();
			int new_second_vn = second.vertexNumber - nodes2move.size();
			
			// Edges
			int oldCutEdges = first.last_nodes.size() * nodes2move.size();
			int newCutEdges = nodes2move.size() * followingOfNodes2move.size();			
			int new_first_en = first.edgeNumber + oldCutEdges;
			int new_second_en = second.edgeNumber - newCutEdges;
			
			// Number of minimal partitions
			int new_first_nmp = first.number_of_min_partitions + 1;
			int new_second_nmp = second.number_of_min_partitions - 1;
			
			// New partitions
			first = new Partition(start,secOfNodes2move,new_first_vn,new_first_en,first_nodes,nodes2move,new_first_nmp);
			second = new Partition(secOfFollowingOfNodes2move,end,new_second_vn,new_second_en,followingOfNodes2move,last_nodes,new_second_nmp); 
			
			// New partitioning
			ArrayList<Partition> parts = new ArrayList<Partition>();
			parts.add(first);
			parts.add(second);
			Partitioning result = new Partitioning(parts);
			results.add(result);	
			
			// Reset nodes
			nodes2move = second.first_nodes;
			followingOfNodes2move = new ArrayList<Node>();
			for (Node node2move : nodes2move) {
				for (Node following : node2move.following) {
					if (!followingOfNodes2move.contains(following)) followingOfNodes2move.add(following);			
			}}
			
			// Reset second
			secOfNodes2move = nodes2move.get(0).event.sec;
			secOfFollowingOfNodes2move = (followingOfNodes2move.isEmpty()) ? 0 : followingOfNodes2move.get(0).event.sec;
		}		
		return results;
	}
	
	/*** Merge two input partitions and return the resulting partition ***/
	public Partition merge (Partition other) {		
		
		// Connect each vertex in this partition to each vertex in other partition
		for (Node node1 : this.nodes) {
			for (Node node2 : other.nodes) {
				node1.connect(node2);
			}
		}				
		// Create a merged partition
		int start = this.start;
		int end = other.end;
		int vertexes = this.vertexNumber + other.vertexNumber;
		int cut_edges = this.last_nodes.size() * other.first_nodes.size();
		int edges = this.edgeNumber + other.edgeNumber + cut_edges;
		ArrayList<Node> first = this.first_nodes;
		ArrayList<Node> last = other.last_nodes;
		int nmp = this.number_of_min_partitions + other.number_of_min_partitions;
		return new Partition(start,end,vertexes,edges,first,last,nmp);
	}
	
	public String toString() {
		return start + "-" + end + ": " + vertexNumber + "; " + edgeNumber;
	}
}
