package raven;

import raven.game.RavenGame;
import raven.ui.GameCanvas;
import raven.ui.RavenUI;
import raven.utils.Log;
import raven.utils.Log.Level;

import javax.swing.SwingUtilities;

import masSim.world.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
	
	public final static RavenGame game = new RavenGame();
	private final static RavenUI ui = new RavenUI(game);
	private static boolean debug = true;
	private final static SimWorld3 world = new SimWorld3((WorldEventListener) ui);
	private final static ExecutorService LogPool = Executors.newSingleThreadExecutor();
	
	public static long scenStartTime; // nanoseconds
	public static long dynamicEventDelay = 2500; // milliseconds, 2.5 secs
	public static long eventTime; // nanoseconds
	public static volatile boolean dynamicEvent = true;
	public static void Message(final boolean flag, final String message)
	{
		
		LogPool.execute(new Runnable() {
			public void run() {
				if (debug && flag) System.out.println(message);
			}
		}
		);
		
		//if (debug && flag) System.out.println(message);
	}
	
    public static void main(String args[]) {
    	
    	Log.setLevel(Level.DEBUG);
    	SwingUtilities.invokeLater(new Runnable() {
  	      public void run() {
  	    	GameCanvas.getInstance().setNewSize(game.getMap().getSizeX(), game.getMap().getSizeY());
  	      }
  	    });
    	//ui = new RavenUI(game);
    	//GameCanvas.getInstance().setNewSize(game.getMap().getSizeX(), game.getMap().getSizeY());
    	game.togglePause();
		game.setAgents(world.initAgents());
		
		world.startAgentThreads();
		
    	gameLoop();
	}
    
    public static RavenUI getUI(){return ui;};
	//////////////////////////////////////////////////////////////////////////
	// Game simulation

	private static void gameLoop() {
    	
    	Log.info("raven", "Starting game...");
    	
    	long lastTime = System.nanoTime();
    	scenStartTime = lastTime;
    	eventTime = scenStartTime + (long)(dynamicEventDelay*1.0e6);
    	while (true) {
    		// TODO Resize UI if the map changes!
    		
    		long currentTime = System.nanoTime();
    	
    		game.update((currentTime - lastTime) * 1.0e-9); // converts nano to seconds
    		lastTime = currentTime;
    		// Always dispose the canvas
    		//if(game.getMap() != null){
    		//if(!game.isPaused()) {
    			try {
    				//GameCanvas.startDrawing(game.getMap().getSizeX(), game.getMap().getSizeY());
    				
    				SwingUtilities.invokeLater(new Runnable() {
    			  	      public void run() {
    			  	    	GameCanvas.startDrawing();
    			  	    	game.render();
    			  	      }
    			  	    });

    			} finally {
    				SwingUtilities.invokeLater(new Runnable() {
  			  	      public void run() {
  			  	    	GameCanvas.stopDrawing();
  			  	      }
  			  	    });
    			}
    		//}
    		//}
    		//TestTaemsScheduler();

    		long millisToNextUpdate = (long) Math.max(0, 16.66667 - (System.nanoTime() - currentTime)*1.0e-6);
    		
			try {
				Thread.sleep(millisToNextUpdate); // sleeps in case there is remaining time in an iteration to maintain a certain update frequency
			} catch (InterruptedException e) {
				break;
			}
    	}
    }

	/*
	public static void assignDynamicTask(final long sleep, final double x, final double y) {
		SimWorld3.TimerPool.execute(new Runnable() {
			@Override
	 		public void run() {
				//try {
					//Thread.sleep(sleep);
					world.assignDynamicTask(x, y);
				//}
				//catch(InterruptedException ex) {
					//System.out.println("Thread for Main.assignDynamicTask x " + x + " y " + y + " interrupted");
				//}
			}
		});
	}
	*/
}
