package masSim.world;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import raven.Main;
import raven.game.RavenGame;
import raven.game.RoverBot;
import raven.game.Waypoints;
import raven.game.interfaces.IRavenBot;
import raven.goals.GoalComposite;
import raven.math.Vector2D;
import raven.ui.GoalCompletionWatcher;
import masSim.world.*;
import masSim.world.WorldEvent.TaskType;
import masSim.taems.*;

public class SimWorld3 implements WorldEventListener, Runnable {

	private boolean debugFlag = true;
	private List<IAgent> agents;
	private List<Task> tasks;
	private List<Task> tasksA;
	private List<Task> tasksB;
	
	private Method lastMethA;
	private Method lastMethB;
	
	private List<WorldEventListener> listeners;
	private IAgent mainAgent;
	
	public SimWorld3(WorldEventListener eventListener)
	{
		agents = new ArrayList<IAgent>();
		tasks = new ArrayList<Task>();
		tasksA = new ArrayList<Task>();
		tasksB = new ArrayList<Task>();
		listeners = new ArrayList<WorldEventListener>();
		listeners.add(eventListener);
		listeners.add(this);
		
		//Initialize two agents, and specify their initial positions
		Agent agentOne = new Agent("Helicopter0", true, 40, 100, listeners);
		Agent agentTwo = new Agent("Helicopter1", false, 40, 200, listeners);
		RegisterMainAgent(agentOne);
		mainAgent = agentOne;
		agentOne.AddChildAgent(agentTwo);
		agents.add(agentOne);
		agents.add(agentTwo);
				
		eventListener.RegisterMainAgent(agentOne);
	}
	private void initTasks() {
		Method m_from = new Method("Visit Station A1",10,100,110);
		tasksA.add(new Task("Station A1",new SumAllQAF(), m_from, mainAgent));
		tasksA.add(new Task("Station A2",new SumAllQAF(), new Method("Visit Station A2",10,200,90), mainAgent));
		tasksA.add(new Task("Station A3",new SumAllQAF(), new Method("Visit Station A3",10,500,90), mainAgent));
		tasksA.add(new Task("Station A4",new SumAllQAF(), new Method("Visit Station A4",10,500,400), mainAgent));
		
		Method lastMethA2 = new Method("Visit Station A5",10,400, 400);
		tasksA.add(new Task("Station A5",new SumAllQAF(), lastMethA2, mainAgent));
		
		lastMethA = new Method("Visit Station A6",10,100,150);
		tasksA.add(new Task("Station A6",new SumAllQAF(), lastMethA, mainAgent));
		
		
		Method m_to = new Method("Visit Station B1",1,100,210);
		//m_to.AddInterrelationship(new Interrelationship(m_from, m_to, new Outcome(100,1,1)));
		tasksB.add(new Task("Station B1",new SumAllQAF(), m_to, agents.get(1)));
		tasksB.add(new Task("Station B2",new SumAllQAF(), new Method("Visit Station B2",1,200,190), agents.get(1)));
		tasksB.add(new Task("Station B3",new SumAllQAF(), new Method("Visit Station B3",1,300,210), agents.get(1)));
		tasksB.add(new Task("Station B4",new SumAllQAF(), new Method("Visit Station B4",1,400,190), agents.get(1)));
		tasksB.add(new Task("Station B5",new SumAllQAF(), new Method("Visit Station B5",1,500,210), agents.get(1)));
		
		lastMethB = new Method("Visit Station B6",1,600,190);
		tasksB.add(new Task("Station B6",new SumAllQAF(), lastMethB, agents.get(1)));
		
	}
	
	private void assignTasksToMain() {
		for( Task task : tasksA) {
			mainAgent.assignTask(task);
		}
		
		for( Task task : tasksB) {
			mainAgent.assignTask(task);
		}
		
	}
	public List<IAgent> initAgents()
	{
		initTasks();
		assignTasksToMain();
				
		//Start Agents
				
		/*		
		Iterator<IAgent> it = agents.iterator();
		while(it.hasNext())
		{
			Agent agent = (Agent) it.next();
			Thread agentThread = new Thread(agent,agent.label);
			agentThread.start();
		}
		*/
		return agents;
		
		//agentOne.assignTask(new Task("Emergency Station",new SumAllQAF(), new Method("Emergency Method",1,300,90), null));
	}
	
	public void run() {
		while(true) {
			try {
				update();
				Thread.sleep(100);
			}
			catch(InterruptedException ex) {
				System.out.println("SimWorld update thread interrupted");
			}
		}
	}
	public void update() {
		for(IAgent agent : agents) {
			agent.executeSchedule();
		}
	}
	public synchronized void addListener(WorldEventListener sl) {
        listeners.add(sl);
    }
 
    public synchronized void removeListener(WorldEventListener sl) {
        listeners.remove(sl);
    }
    
    
    private boolean lastAcompleted = false;
    private boolean lastBcompleted = false;
    private void resetCompletedFlags() {
    	lastAcompleted = false;
    	lastBcompleted = false;
    }
    private int ticks = 0;
    public void HandleWorldEvent(WorldEvent event) {
		
		if (event.taskType == TaskType.METHODCOMPLETED)
		{
			Method m = event.method;
			Main.Message(debugFlag, "[SimWorld] method " + m.label + " completed");
			if(m.label.equals(lastMethA.label)) {
				lastAcompleted = true;
				Main.Message(debugFlag, "[SimWorld] last method A " + m.label + " completed");
			}
			
			else if(m.label.equals(lastMethB.label)) {
				lastBcompleted = true;
				Main.Message(debugFlag, "[SimWorld] last method B " + m.label + " completed");
			}
			
			if(lastAcompleted && lastBcompleted) {
				resetCompletedFlags();
				if(ticks < 4) {
					ticks++;
					reassignTasks();
				}
				Main.Message(debugFlag, "[SimWorld] both last methods A and B completed");
			}
		}
		//Main.Message(debugFlag, "[RavenUI 488] Executing Task at " + popupLoc.x + " " + popupLoc.y);
		
	}
    private void reassignTasks(){
    	WorldState.clearCompletedMethods();
    	tasksA.clear();
    	tasksB.clear();
    	initTasks();
    	assignTasksToMain();
    }
    public void RegisterMainAgent(IAgent agent) {
		this.mainAgent = agent;
	}
}
