package raven.game;

//import java.util.ArrayList;
//import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.Set;
//import java.util.Map;
//import java.util.HashMap;
//import raven.math.Transformations;
import raven.math.Vector2D;
import raven.ui.GameCanvas;

public class Waypoints {
	private Queue<Wpt> wpts = new ConcurrentLinkedQueue<Wpt>();
	private ConcurrentMap<String, Wpt> wptsMap = new ConcurrentHashMap<String, Wpt>();
	
	public class Wpt {
		public Vector2D pos;
		public String name;
		public double x,y;
		public Wpt(Vector2D pos) {
			this(pos,"WP" + wpts.size());
		}
		public Wpt(Vector2D pos, String wayPointName) {
			this.pos = pos;
			x = pos.x;
			y = pos.y;
			name = wayPointName;
		}
	}
	/*
	public void addWpt(Vector2D pos) {
		wpts.add(new Wpt(pos));
	}
	*/
	// all updates involving both Queue wpts and HashMap wptsMap should be atomic
	public synchronized void  addWpt(Waypoints.Wpt wpt, String name) {
		wpts.add(wpt);
		wptsMap.put(name, wpt);
	}
	public synchronized void addWpt(Vector2D pos, String name) {
		Wpt wpt = new Wpt(pos, name);
		wpts.add(wpt);
		wptsMap.putIfAbsent(name, wpt);
	}
	public synchronized void removeWpt(String name) {
		Wpt wpt = wptsMap.get(name);
		if(wpt != null) {
			wptsMap.remove(name);
			wpts.remove(wpt);
		}
	}
	public synchronized void clearWpts(){
		wpts.clear();
		wptsMap.clear();
	}
	public int size(){return wpts.size();}
	public Iterator<Waypoints.Wpt> getIter() {return wpts.iterator();}
	//public Waypoints.Wpt get(int i) {return wpts.get(i);}
	public Waypoints.Wpt get(String name)
	{
		return wptsMap.get(name);
	}
	public void render() {		
		GameCanvas.bluePen();
		for (Wpt wpt : wpts) {
			GameCanvas.filledCircle(wpt.x, wpt.y, 3);
			GameCanvas.textAtPos(wpt.x - 10, wpt.y - 5, wpt.name);
		}
		//TODO ASIF CHANGE
		//GameCanvas.greenPen();
		//for (int i=0; i<wpts.size()-1; i++) {
		//	GameCanvas.lineWithArrow(wpts.get(i).pos, wpts.get(i+1).pos, 2.0);
		//}
		
	}	
}
