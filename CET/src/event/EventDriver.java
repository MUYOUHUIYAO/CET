package event;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class EventDriver implements Runnable {	
	
	String filename;
	int lastsec;
	final EventQueue eventqueue;			
	long startOfSimulation;
	AtomicInteger drProgress;
		
	public EventDriver (String f, int last, EventQueue eq, long start, AtomicInteger dp) {
		
		filename = f;
		lastsec = last;
		eventqueue = eq;			
		startOfSimulation = start;
		drProgress = dp;
	}

	/** 
	 * Read the input file, parse the events, 
	 * and put events into the event queue in timely manner.
	 */
	public void run() {	
		try {
			// Local variables
			double system_time = 0;
			double driver_wakeup_time = 0;
			// Input file
			Scanner scanner = new Scanner(new File(filename));
			// First event
			String line = scanner.nextLine();
	 		Event event = Event.parse(line);
	 		// Current Second
	 		int curr_sec = -1;		
			// First batch			
			Random random = new Random();
			int min = 6;
			int max = 14;			
			int end = random.nextInt(max - min + 1) + min;
			TimeInterval batch = new TimeInterval(0,end);
									
 			if (batch.end > lastsec) batch.end = lastsec;	
 			System.out.println("\n-------------------------\nBatch end: " + batch.end);
 			
 			/*** Put events within the current batch into the event queue ***/		
	 		while (true) { 
	 		
	 			while (event != null && event.sec <= batch.end) {	 			
	 				
	 				/*** Put the event into the event queue ***/						
	 				eventqueue.contents.add(event);	
	 					
	 				/*** Set distributer progress ***/	
	 				if (curr_sec < event.sec) {		
	 					
	 				// Avoid null run exception when the stream is read too fast
	 					if (curr_sec>300) { 
	 						eventqueue.setDriverProgress(curr_sec);
	 						//if (curr_sec % 10 == 0) System.out.println("Distribution time of second " + curr_sec + " is " + now);
	 					}
	 					curr_sec = event.sec;
	 				}
	 			
	 				/*** Reset event ***/
	 				if (scanner.hasNextLine()) {		 				
	 					line = scanner.nextLine();   
	 					event = Event.parse(line);		 				
	 				} else {
	 					event = null;		 				
	 				}
	 			}		 			
	 			/*** Set distributor progress ***/		 					
	 			eventqueue.setDriverProgress(batch.end);					
	 			curr_sec = batch.end;
	 			
				if (batch.end < lastsec) { 			
 				
					/*** Sleep if now is smaller than batch_limit ms ***/
					system_time = System.currentTimeMillis() - startOfSimulation;
					//System.out.println("Skipped time is " + skipped_time + " sec.\nSystem time is " + system_time/1000);
					
					if (system_time < batch.end*1000) { // !!!
	 			
						int sleep_time = new Double(batch.end*1000 - system_time).intValue(); // !!!	 			
						//System.out.println("Distributor sleeps " + sleep_time + " ms at " + curr_sec );		 			
						try { Thread.sleep(sleep_time); } catch (InterruptedException e) { e.printStackTrace(); }
						driver_wakeup_time = (System.currentTimeMillis() - startOfSimulation)/1000 - batch.end; // !!!
					} 
					
					/*** Rest batch_limit ***/
					int new_start = batch.end + 1;
					int new_end = batch.end + random.nextInt(max - min + 1) + min + new Double(driver_wakeup_time).intValue();
					batch = new TimeInterval(new_start, new_end);
					if (batch.end > lastsec) batch.end = lastsec;
					System.out.println("-------------------------\nBatch end: " + batch.end);
 				
					if (driver_wakeup_time > 1) {
						System.out.println(	"Distributor wakeup time is " + driver_wakeup_time + 
											". New batch is " + batch.toString() + ".");
					}	 				
				} else { /*** Terminate ***/	 				
					break;
				}						
	 		}
	 		
	 		/*** Clean-up ***/		
			scanner.close();				
			System.out.println("----------------------------------\nDriver is done.");	
 		
		} catch (FileNotFoundException e) { e.printStackTrace(); }
	}	
}
