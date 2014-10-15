package masSim.taems;

import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import raven.Main;

public class Schedule {
	private Queue<ScheduleElement> items;
	private Map<String, ScheduleElement> itemsMap = new HashMap<String, ScheduleElement>();
	public int TotalQuality = 0;
	public Schedule() {
		items = new ConcurrentLinkedQueue<ScheduleElement>();
	}
	public void addItem(ScheduleElement item){
		//Main.Message("[Schedule] Added to schedule task " + item.getName());
		items.add(item);
		itemsMap.put(item.getName(), item);
	}
	public void RemoveElement(ScheduleElement item)
	{
		items.remove(item);
		itemsMap.remove(item.getName());
	}
	public boolean containsElement(String elName) {
		return itemsMap.containsKey(elName);
	}
	public ScheduleElement poll(){
		return items.poll();
	}
	public ScheduleElement peek(){
		return items.peek();
	}
	public boolean hasNext(int ind) {
		return ind < items.size();
	}
	public Iterator<ScheduleElement> getItems() {
		return items.iterator();
	}
	public void Merge(Schedule sch)
	{
		items.clear();
		items.addAll(sch.items);
	}
	@Override
	public String toString()
	{
		String val = "";
		Iterator<ScheduleElement> it = items.iterator();
		while(it.hasNext())
		{
			ScheduleElement el = (ScheduleElement)it.next();
			val += " > " + el.toString();
		}
		return val;
	}
}
