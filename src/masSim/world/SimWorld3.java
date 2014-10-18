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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class SimWorld3 implements WorldEventListener, Runnable {

	private boolean debugFlag = true;
	private List<IAgent> agents;
	private List<Task> tasks;
	private List<Task> tasksA;
	private List<Task> tasksB;
	private List<Task> allTasks;
	
	private static final ExecutorService agentPool = Executors.newFixedThreadPool(2);
	public static final ExecutorService EventProcPool = Executors.newSingleThreadExecutor();
	
	private List<WorldEventListener> listeners;
	private IAgent mainAgent;
	
	public SimWorld3(WorldEventListener eventListener)
	{
		agents = new ArrayList<IAgent>();
		tasks = new ArrayList<Task>();
		tasksA = new ArrayList<Task>();
		tasksB = new ArrayList<Task>();
		allTasks = new ArrayList<Task>();
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
		//allMethods.add(m_from);
		Task tA1 = new Task("Station A1",new SumAllQAF(), m_from, mainAgent);
		tasksA.add(tA1);
		
		
		Method mA2 = new Method("Visit Station A2",10,200,90);
		//allMethods.add(mA2);
		Task tA2 = new Task("Station A2",new SumAllQAF(), mA2, mainAgent);
		tasksA.add(tA2);
		
		
		Method mA3 = new Method("Visit Station A3",10,500,90);
		//allMethods.add(mA3);
		tasksA.add(new Task("Station A3",new SumAllQAF(), mA3, mainAgent));
		
		Method mA4 = new Method("Visit Station A4",10,500,400);
		//allMethods.add(mA4);
		tasksA.add(new Task("Station A4",new SumAllQAF(), mA4, mainAgent));
		
		Method mA5 = new Method("Visit Station A5",10,400, 400);
		//allMethods.add(mA5);
		tasksA.add(new Task("Station A5",new SumAllQAF(), mA5, mainAgent));
		
		Method mA6 = new Method("Visit Station A6",10,100,150);
		//allMethods.add(mA6);
		tasksA.add(new Task("Station A6",new SumAllQAF(), mA6, mainAgent));
		
		
		Method m_to = new Method("Visit Station B1",1,100,210);
		//allMethods.add(m_to);
		//m_to.AddInterrelationship(new Interrelationship(m_from, m_to, new Outcome(100,1,1)));
		tasksB.add(new Task("Station B1",new SumAllQAF(), m_to, agents.get(1)));
		
		Method mB2 = new Method("Visit Station B2",1,200,190);
		//allMethods.add(mB2);
		tasksB.add(new Task("Station B2",new SumAllQAF(), mB2, agents.get(1)));
		
		Method mB3 = new Method("Visit Station B3",1,300,210);
		//allMethods.add(mB3);
		tasksB.add(new Task("Station B3",new SumAllQAF(), mB3, agents.get(1)));
		
		Method mB4 = new Method("Visit Station B4",1,400,190);
		//allMethods.add(mB4);
		tasksB.add(new Task("Station B4",new SumAllQAF(), mB4, agents.get(1)));
		
		Method mB5 = new Method("Visit Station B5",1,500,210);
		//allMethods.add(mB5);
		tasksB.add(new Task("Station B5",new SumAllQAF(), mB5, agents.get(1)));
		
		
		Method mB6 = new Method("Visit Station B6",1,600,190);
		//allMethods.add(mB6);
		tasksB.add(new Task("Station B6",new SumAllQAF(), mB6, agents.get(1)));
		//allTasks.addAll(tasksA);
		//allTasks.addAll(tasksB);
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
		return agents;
		//agentOne.assignTask(new Task("Emergency Station",new SumAllQAF(), new Method("Emergency Method",1,300,90), null));
	}
	public void startAgentThreads() {
		for(IAgent agent : agents) {
			agentPool.execute((Agent)agent);
		}
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
    
    private int ticks = 0;
    public synchronized void HandleWorldEvent(WorldEvent event) {
		
		if (event.taskType == TaskType.METHODCOMPLETED)
		{
			Method m = event.method;
			Main.Message(debugFlag, "[SimWorld] method " + m.label + " completed");
			
			int schedsSize = 0;
			boolean schedsOneEl = true;
			for(IAgent agent : agents) {
				int schedSize = ((Agent)agent).schedSize();
				if(schedSize != 1) {
					schedsOneEl = false;
				}
				Main.Message(debugFlag, "[SimWorld] agent " + agent.getName() + " has sched size " + schedSize);
				schedsSize += schedSize;
			}
			
			Main.Message(debugFlag, "[SimWorld] size of agents schedules " + schedsSize);
			if(schedsSize < 3) {
				for(IAgent agent : agents) {
					Schedule sched = ((Agent)agent).getSched();
					if(sched == null) {
						Main.Message(debugFlag, "[SimWorld] agent " + agent.getName() + " has NULL schedule !");
					}
					else {
						String schedS = sched.toString();
						Main.Message(debugFlag, "[SimWorld] agent " + agent.getName() + " schedule : " + schedS);
					}
				}
			}
			//Main.Message(debugFlag, "[SimWorld] CompletedMethods size " + WorldState.CompletedMethods.size() + " tasksA size " + tasksA.size() + " tasksB size " + tasksB.size());
			// if(WorldState.CompletedMethods.size() == (tasksA.size() + tasksB.size())) {
			if(schedsOneEl) {
			//if(schedsSize == 1) {
				if(ticks < 4) {
					ticks++;
					reassignTasks();
					Main.Message(debugFlag, "[SimWorld] both paths A and B completed, reassigning tasks");
				}
				else {
					Main.Message(debugFlag, "[SimWorld] both paths A and B completed, end of scenario");
				}
				
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
