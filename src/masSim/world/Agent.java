package masSim.world;

import masSim.world.WorldEvent;
import masSim.world.WorldEventListener;
import masSim.world.WorldEvent.TaskType;
import masSim.schedule.IScheduleUpdateEventListener;
import masSim.schedule.ScheduleUpdateEvent;
import masSim.schedule.Scheduler;
import masSim.taems.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import raven.Main;
import raven.math.Vector2D;
import raven.game.RavenGame;

public class Agent extends BaseElement implements IAgent, IScheduleUpdateEventListener, Runnable{

	private boolean debugFlag = true;
	private boolean managing;
	private static int GloballyUniqueAgentId = 1;
	private int code;
	private Scheduler scheduler;
	private final AtomicReference<Schedule> schedule = new AtomicReference<Schedule>(); // the schedule content must be thread safe, not just a reference to it
	private int taskInd;
	private boolean resetScheduleExecutionFlag = false;
	private ArrayList<IAgent> agentsUnderManagement = null;
	private AgentMode mode;
	public List<WorldEventListener> listeners;
	public double x;
	public double y;
	public boolean flagScheduleRecalculateRequired;
	public Queue<Method> queue = new LinkedList<Method>();
	public volatile long eventTime;
	public volatile int dynamicEventX;
	public volatile int dynamicEventY;
	public volatile boolean dynamicEvent;
	private int masSimTaskCount = 1;
	
	
	private enum Status {
		IDLE, PROCESSNG, EMPTY
	}
	
	@Override
	public String getName()
	{
		return this.label;
	}
	/** alive, dead or spawning? */
	private Status status;
	
	public Agent(int newCode){
		this(newCode,"Agent"+newCode,false,0,0,null);
	}
	
	public Agent(String name, boolean isManagingAgent, int x, int y, List<WorldEventListener> listeners){
		this(GloballyUniqueAgentId++,name, isManagingAgent, x, y, listeners);
	}
	
	public Agent(int newCode, String label, boolean isManagingAgent, int x, int y, List<WorldEventListener> listeners){
		this.code = newCode;
		this.label = label;
		taskInd = 0;
		status = Status.EMPTY;
		flagScheduleRecalculateRequired = true;
		if (listeners==null)
			this.listeners = new ArrayList<WorldEventListener>();
		else
			this.listeners = listeners;
		scheduler = new Scheduler(this);
		this.scheduler.AddScheduleUpdateEventListener(this);
		this.x = x;
		this.y = y;
		managing = isManagingAgent;
		if (isManagingAgent) agentsUnderManagement = new ArrayList<IAgent>();
		fireWorldEvent(TaskType.AGENTCREATED, label, null, x, y, null);
	}
	public int schedSize() {return scheduler.schedSize();}
	public Schedule getSched() {
		return scheduler.getSched();
	}
	public boolean isMain() {return managing;}
	public synchronized boolean AreEnablersInPlace(Method m)
	{
		boolean methodEnablersCompleted = false;
		if (m.Interrelationships.size()>0)
		{
			for(Interrelationship ir: m.Interrelationships)
			{
				Method from = ir.from;
				for(Method mc : WorldState.CompletedMethods)
				{
					if (mc.label==from.label)
						methodEnablersCompleted = true;
				}
			}	
		}
		else
		{
			methodEnablersCompleted = true;
		}
		return methodEnablersCompleted;
	}
	
	public void Execute(Method m) throws InterruptedException
	{
		Main.Message(true, "[Agent " + this.label + " ] in Execute for method " + m.label + " with x = " + m.x + " ; y = " + m.y);
		while (!AreEnablersInPlace(m))
		{
			// why a polling loop instead of conditional synch ? it should wake up only when of the relevant enablers is set 
			Main.Message(true, "[Agent " + this.label + " ] " + m.label + " enabler not in place. Waiting...");
			Thread.sleep(1000);
		}
		if (m.x!=0 && m.y!=0)
		{
			Main.Message(true, "[Agent " + this.label + " ] about to fire EXECUTEMETHOD event for method " + m.label);
			fireAgentMovedEvent(TaskType.EXECUTEMETHOD, this.label, m.label, m.x, m.y, this, m);
			Main.Message(true, "[Agent " + this.label + " ] executing " + m.label);
			this.flagScheduleRecalculateRequired = false;
		}
		else {
			Main.Message(true, "[Agent " + this.label + " ] Execute method " + m.label + " with 0 x and y !");
		}
	}
	
	@Override
	public void MarkMethodCompleted(Method m)
	{
		//schedule.get().RemoveElement(e);Does this need to be done?
		m.MarkCompleted();
		WorldState.CompletedMethods.add(m);
		Main.Message(true, "[Agent " + this.label + " ] " + m.label + " added to completed queue");
		if (schedule.get()!=null)
		{
			Iterator<ScheduleElement> el = schedule.get().getItems();
			if(el.hasNext())
			{
				ScheduleElement e = el.next();
				if (e.getMethod().label.equals("Starting Point") && el.hasNext())
					e = el.next();
				if (m.equals(e.getMethod()))
				{
					schedule.get().RemoveElement(e);
					Main.Message(true, "[Agent " + this.label + " ] Removed " + e.getName() + " from schedule");
				}
			}
		}
		this.fireWorldEvent(TaskType.METHODCOMPLETED, null, m.label, m.x, m.y, m);
		flagScheduleRecalculateRequired = true;
		Main.Message(true, "[Agent " + this.label + " ] " + m.label + " completed and recalc flag set to " + flagScheduleRecalculateRequired);
		
		
	}
	
	public void fireAgentMovedEvent(TaskType type, final String agentId, final String methodId, final double x2, final double y2, final IAgent agent, final Method method) {
		final Agent ag = this;
		SimWorld3.EventProcPool.execute(new Runnable() {
			@Override
	 		public void run() {
				Main.Message(debugFlag, "[Agent " + ag.label + " ] Firing Execute Method for " + methodId);
				WorldEvent worldEvent = new WorldEvent(ag, TaskType.EXECUTEMETHOD, agentId, methodId, x2, y2, agent, method);
				for(WorldEventListener listener : listeners) {
					listener.HandleWorldEvent(worldEvent);
				}
			}
		});
    }
	
	// Returns identifying code, specific for this agent
	public int getCode(){
		return code;
	}
	
	// why executeSchedule invokes the scheduler in a loop ? shouldn't it just execute a given schedule ?
	public void executeSchedule() {
		while(flagScheduleRecalculateRequired)
		{
			Main.Message(debugFlag, "[Agent " + this.label + " ] Executing Schedule");
			if(managing) {
				if(dynamicEvent) {
					if(System.currentTimeMillis() >=  eventTime ) {
						Main.Message(debugFlag, "[Agent " + this.label + " ] currrent time = " + System.currentTimeMillis() + " event time = " + eventTime + " currentTime >= eventTime , adding dynamic task");
						SimWorld3.TimerPool.execute(new Runnable() {
							public void run() {
								assignTask(Task.CreateDefaultTask(masSimTaskCount++, dynamicEventX, dynamicEventY));
								dynamicEvent = false;
							}
						}
						);
					}
					else {
						Main.Message(debugFlag, "[Agent " + this.label + " ] currrent time = " + System.currentTimeMillis() + " event time = " + eventTime + " currentTime < eventTime , no dynamic task added");
					}
				}
			}
			flagScheduleRecalculateRequired = false;
			//Main.Message(debugFlag, "[Agent " + this.label +  " ] Running again");
			Schedule newSchedule = this.scheduler.RunStatic();
			if (newSchedule!=null) {
				schedule.set(newSchedule);
				Main.Message(debugFlag, "[Agent " + this.label + " ] Schedule Updated. New first method " + schedule.get().peek().getMethod().label);
			}
			if (schedule.get()!=null)
			{
				Iterator<ScheduleElement> el = schedule.get().getItems();
				if(el.hasNext())
				{
					try {
						ScheduleElement e = el.next();
						if (e.getMethod().label.equals("Starting Point") && el.hasNext())
							e = el.next();
						else
							continue;
						Method m = e.getMethod();
						Main.Message(debugFlag, "[Agent " + this.label + " ] Next method to be executed from schedule " + m.label);
						Execute(m);
						// this should be replaced with conditional synchronization if this thread indeed needs to sleep
						while(!flagScheduleRecalculateRequired)
						{	
							//Main.Message(debugFlag, "[Agent 126] Waiting completion of " + m.label + " with flag " + flagScheduleRecalculateRequired);
							Thread.sleep(1000);
						}
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
	 * this method handles the assignment goals
	 */
	public synchronized void assignTask(Task task){
		if (task.agent != null)
		{
			if (this.equals(task.agent))
			{
				Main.Message(debugFlag, "[Agent " + this.label + " ] assigned " + task.label);
				Iterator<Node> it = task.getSubtasks();
				while(it.hasNext())
				{
					Node node = it.next();
					if (!node.IsTask())
					{
						Method method = (Method)node;
						this.fireWorldEvent(TaskType.METHODCREATED, null, method.label, method.x, method.y, method);
					}
				}
				this.scheduler.AddTask(task);
				flagScheduleRecalculateRequired = true;
			}
			else if (this.agentsUnderManagement.contains(task.agent)) 
			{
				Main.Message(debugFlag, "[Agent " + this.label + " ] task "+ task.label + " is assigned to an agent other than mainAgent");
				task.agent.assignTask(task);
			}
			else
			{
				Main.Message(debugFlag, task.agent.getCode() + " is not a child of " + this.label);
			}
		}
		else
		{
			//Italian guy practical applications to quadrovers. Look at that.dellefave-IAAI-12.pdf
			//Calculate which agent is best to assign
			int baseQuality = this.getExpectedScheduleQuality(null, this);
			int qualityWithThisAgent = this.getExpectedScheduleQuality(task, this);
			int addedQuality = qualityWithThisAgent - baseQuality;
			Main.Message(true, "[Agent " + this.label + " ] Quality with agent " + this.getName() + " " + qualityWithThisAgent + " + " + baseQuality + " = " + addedQuality);
			IAgent selectedAgent = this;
			for(IAgent ag : this.agentsUnderManagement)
			{
				baseQuality = this.getExpectedScheduleQuality(null, ag);
				qualityWithThisAgent = ag.getExpectedScheduleQuality(task, ag);
				int newAddedQuality = qualityWithThisAgent-baseQuality;
				Main.Message(true, "[Agent " + this.label + " ] Quality with agent " + this.getName() + " " + qualityWithThisAgent + " + " + baseQuality + " = " + newAddedQuality);
				if (newAddedQuality>addedQuality)
				{
					addedQuality = newAddedQuality;
					selectedAgent = ag;
				}
			}
			task.agent = selectedAgent;
			Main.Message(true, "[Agent " + this.label + " ] Assigning " + task.label + " to " + task.agent.getName());
			flagScheduleRecalculateRequired = true;
			assignTask(task);
		}
	}
	/*
	public void update(int tick) {
	
	}
	
	public void update(int tick) {
		
		if(schedule.get().hasNext(taskInd)) {
			ScheduleElement el = schedule.get().peek();
			ScheduleElement.Status status = el.update(tick);
			if(status == ScheduleElement.Status.COMPLETED) {
				System.out.println("Agent " + label + " completed item " + el.getName());
				schedule.get().poll();
			}
		}
		else {
			System.out.println("Agent " + label + " idle");
		}
	}
*/
	@Override
	public void run() {
		//Running the agent means that the agent starts doing two things, and does them indefinitely unless it is killed or suspended.
		//First, it creates a background thread to keep checking for new tasks, and to calculate an optimum schedule for those.
		//Second, it executes those tasks whose schedule had already been created.
		//Thread agentScheduler = new Thread(this.scheduler,"Scheduler " + this.label);
		//agentScheduler.start();
		try {
			while(true) {
				// bad pausing; should use proper thread pausing
				//if(!RavenGame.paused) {executeSchedule();} 
				executeSchedule();
				Thread.sleep(100); // should use frequency regulator instead of constant sleep duration
				//System.out.println("[Agent " + this.label + " ] currrent time = " + System.currentTimeMillis() + " event time = " + eventTime);
				/*
				if(managing) {
					System.out.println("[Agent " + this.label + " ] currrent time = " + System.currentTimeMillis() + " event time = " + eventTime);
					if(System.currentTimeMillis() >  eventTime ) {
						assignTask(Task.CreateDefaultTask(masSimTaskCount++, dynamicEventX, dynamicEventY));
					}
				}
				*/
			}
		} catch (InterruptedException e) {
			//e.printStackTrace();
			System.out.println("Thread of agent " + this.label + " interrupted");
		}
		
	}

	@Override
	public void HandleScheduleEvent(ScheduleUpdateEvent scheduleUpdateEvent) {
		Schedule currentSchedule = schedule.get();
		if (currentSchedule!=null)
			schedule.get().Merge(scheduleUpdateEvent.Schedule);
		else
			schedule.set(scheduleUpdateEvent.Schedule);
	}

	@Override
	public void AddChildAgent(IAgent agent){
		if (agentsUnderManagement==null)
			System.out.println("Child Agent being added to non-managing agent");
		this.agentsUnderManagement.add(agent);
	}
	
	public void fireWorldEvent(final TaskType type, final String agentId, final String methodId, final double x2, final double y2, final Method method) {
		final Agent ag = this;
		SimWorld3.EventProcPool.execute(new Runnable() {
			@Override
	 		public void run() {
				Main.Message(debugFlag, "[Agent " + ag.label + " ] Firing Execute Method for " + methodId);
				WorldEvent worldEvent = new WorldEvent(ag, type, agentId, methodId, x2, y2, ag, method);
				for(WorldEventListener listener : listeners) {
					listener.HandleWorldEvent(worldEvent);
				}
			}
		});
    }

	@Override
	public int getExpectedScheduleQuality(Task task, IAgent agent) {
		int cost = 0;
		if (task!=null)
		{
			IAgent previousAgent = task.agent;
			task.agent = agent;
			cost = this.scheduler.GetScheduleCostSync(task, agent);
			task.agent = previousAgent;
		}
		else
			cost = this.scheduler.GetScheduleCostSync(null, agent); 
		return cost;
	}

	@Override
	public void setPosition(Vector2D pos) {
		this.x = pos.x;
		this.y = pos.y;
	}
	
	@Override
	public Vector2D getPosition() {
		return new Vector2D(x,y);
	}

	@Override
	public AgentMode getMode() {
		return mode;
	}

	@Override
	public void setMode(AgentMode mode) {
		this.mode = mode;
	}
}
