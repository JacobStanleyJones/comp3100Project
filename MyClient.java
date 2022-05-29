import java.io.*;
import java.net.*;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

public class MyClient {

	static Socket s;
	static BufferedReader in;
	static DataOutputStream dout;

	static ArrayList<Job> jobs;
	static ArrayList<Server> servers;

	public static void main(String[] args) {
		try {
			s = new Socket("localhost", 50000);
			in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			dout = new DataOutputStream(s.getOutputStream());

			Handshake();

			GetJobData();
			GetSystemData();

			Boolean useDefaultAlgorithm = true;
			for (int i = 0; i < args.length; i++) {
				if (args[i].toLowerCase().equals("fc")) {
					useDefaultAlgorithm = false;
					ScheduleFC();
					break;
				} else if (args[i].toLowerCase().equals("lrr")) {
					useDefaultAlgorithm = false;
					ScheduleLRR();
					break;
				} else if (args[i].toLowerCase().equals("fa")) {
					useDefaultAlgorithm = false;
					ScheduleFA();
					break;
				}
			}
			if (useDefaultAlgorithm)
				ScheduleFA();

			dout.write(("QUIT\n").getBytes());

			dout.flush();
			dout.close();
			s.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	private static void Handshake() {
		try {
			dout.write(("HELO\n").getBytes());
			String str = (String) in.readLine();
			System.out.println("message= " + str);

			dout.write(("AUTH jacob\n").getBytes());
			str = (String) in.readLine();
			System.out.println("message= " + str);

		} catch (Exception e) {
			System.out.println(e);
		}
	}

	// Reads the ds-jobs.xml file and stores all the jobs in an ArrayList
	private static void GetJobData() {
		try {
			jobs = new ArrayList<Job>();
			File file = new File(System.getProperty("user.dir") + "/ds-jobs.xml");
			// an instance of factory that gives a document builder
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			// an instance of builder to parse the specified xml file
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
			NodeList nodeList = doc.getElementsByTagName("job");
			// nodeList is not iterable, so we are using for loop
			for (int itr = 0; itr < nodeList.getLength(); itr++) {
				Node node = nodeList.item(itr);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) node;
					Job job = new Job(eElement.getAttribute("id"), eElement.getAttribute("type"),
							eElement.getAttribute("submitTime"), eElement.getAttribute("estRunTime"),
							eElement.getAttribute("cores"), eElement.getAttribute("memory"),
							eElement.getAttribute("disk"));
					jobs.add(job);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Reads the ds-system.xml file and stores all the servers in an ArrayList
	private static void GetSystemData() {
		try {
			servers = new ArrayList<Server>();
			File file = new File(System.getProperty("user.dir") + "/ds-system.xml");
			// an instance of factory that gives a document builder
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			// an instance of builder to parse the specified xml file
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
			NodeList nodeList = doc.getElementsByTagName("server");
			// nodeList is not iterable, so we are using for loop
			for (int itr = 0; itr < nodeList.getLength(); itr++) {
				Node node = nodeList.item(itr);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) node;
					Server server = new Server(eElement.getAttribute("type"), eElement.getAttribute("limit"),
							eElement.getAttribute("bootupTime"), eElement.getAttribute("hourlyRate"),
							eElement.getAttribute("cores"), eElement.getAttribute("memory"),
							eElement.getAttribute("disk"));
					servers.add(server);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Largest Round Robin
	// Schedules all jobs to the largest (most cores) server in a round robin
	// fashion.
	private static void ScheduleLRR() {
		try {
			Server largest = null;
			for (int i = 0; i < servers.size(); i++) {
				Server s = servers.get(i);
				if (largest == null || largest.cores < s.cores)
					largest = s;
			}
			String str = "";
			int jobNum = 0;
			while (!str.equals("NONE")) {
				dout.write(("REDY\n").getBytes());
				str = (String) in.readLine();
				System.out.println("message= " + str);

				String[] jobData = str.split(" ");

				// If it's a job submission, schedule it to the largest server
				if (jobData[0].equals("JOBN")) {
					String sch = "SCHD " + jobData[2] + " " + largest.type +
							" " + jobNum % largest.limit + "\n";
					dout.write((sch).getBytes());

					str = (String) in.readLine();
					System.out.println("message= " + str);

					jobNum++;
				}
			}
		} catch (Exception e) {
			System.out.println(e);
		}

	}

	// First Capable.
	// Schedules a job to the first server in the response to GETS Capable
	// regardless of how many running and waiting jobs there are.
	private static void ScheduleFC() {
		try {
			String str = "";
			while (!str.equals("NONE")) {
				dout.write(("REDY\n").getBytes());
				str = (String) in.readLine();
				System.out.println("message= " + str);

				String[] jobData = str.split(" ");

				// If it's a job submission, schedule it to the largest server
				if (jobData[0].equals("JOBN")) {
					String jobStr = jobData[4] + " " + jobData[5] + " " + jobData[6];
					dout.write(("GETS Capable " + jobStr + "\n").getBytes());
					str = (String) in.readLine();
					System.out.println("Gets Response= " + str);

					String[] getsData = str.split(" ");
					int numServers = 0;
					if (getsData[1] != ".")
						numServers = Integer.parseInt(getsData[1]);

					dout.write(("OK\n").getBytes());

					str = (String) in.readLine();
					System.out.println("FirstServer= " + str);
					String[] serverData = str.split(" ");

					for (int i = 1; i < numServers; i++) {
						str = (String) in.readLine();
					}
					dout.write(("OK\n").getBytes());
					str = (String) in.readLine();
					System.out.println("message= " + str);

					String sch = "SCHD " + jobData[2] + " " + serverData[0] +
							" " + serverData[1] + "\n";
					dout.write((sch).getBytes());

					str = (String) in.readLine();
					System.out.println("message= " + str);
				}
			}
		} catch (Exception e) {
			System.out.println(e);
		}

	}

	// First Available.
	// Schedules a job to the first available server. If there aren't
	// any available servers, it schedules to the first capable server
	private static void ScheduleFA() {
		try {
			String str = "";
			while (!str.equals("NONE")) {
				dout.write(("REDY\n").getBytes());
				str = (String) in.readLine();
				System.out.println("message= " + str);

				String[] jobData = str.split(" ");

				// It's a job submission. Schedule it to the first available server
				if (jobData[0].equals("JOBN")) {
					String jobStr = jobData[4] + " " + jobData[5] + " " + jobData[6];
					dout.write(("GETS Avail " + jobStr + "\n").getBytes());
					str = (String) in.readLine();

					String[] getsData = str.split(" ");
					int numServers = 0;
					if (getsData[1] != ".")
						numServers = Integer.parseInt(getsData[1]);

					dout.write(("OK\n").getBytes());
					str = (String) in.readLine();

					String[] serverData;

					if (numServers == 0) { //No availabile servers
						System.out.println("No available servers. Finding the first capable server.");
						dout.write(("GETS Capable " + jobStr + "\n").getBytes());
						str = (String) in.readLine();
						getsData = str.split(" ");
						if (getsData[1] != ".")
							numServers = Integer.parseInt(getsData[1]);

						dout.write(("OK\n").getBytes());
						str = (String) in.readLine();
					}

					System.out.println("message= " + str);
					serverData = str.split(" ");

					for (int i = 1; i < numServers; i++) {
						str = (String) in.readLine();
					}
					dout.write(("OK\n").getBytes());
					str = (String) in.readLine();
					System.out.println("message= " + str);

					String sch = "SCHD " + jobData[2] + " " + serverData[0] +
							" " + serverData[1] + "\n";
					dout.write((sch).getBytes());

					str = (String) in.readLine();
					System.out.println("message= " + str);
				}
			}
		} catch (Exception e) {
			System.out.println(e);
		}

	}
}

class Server {
	String type;
	int limit;
	int bootupTime;
	String hourlyRate;
	int cores;
	int memory;
	int disk;

	int[] availableTime;

	public Server(String type, String limit, String bootupTime, String hourlyRate, String cores, String memory,
			String disk) {
		this.type = type;
		this.limit = Integer.parseInt(limit);
		this.bootupTime = Integer.parseInt(bootupTime);
		this.hourlyRate = hourlyRate;
		this.cores = Integer.parseInt(cores);
		this.memory = Integer.parseInt(memory);
		this.disk = Integer.parseInt(disk);

		availableTime = new int[this.limit];
		for (int i = 0; i < this.limit; i++) {
			availableTime[i] = this.bootupTime;
		}
	}

	public Boolean equals(String serverType) {
		return serverType.equals(type);
	}

	public void scheduleJob(int idx, Job job) {
		int avail = Math.max(job.submitTime, availableTime[idx]);

		availableTime[idx] = avail + job.estRunTime;
	}

	public int soonestAvailable() {
		int idx = 0;
		for (int i = 0; i < limit; i++) {
			if (availableTime[i] < availableTime[idx])
				idx = i;
		}
		return idx;
	}
}

class Job {
	int id;
	String type;
	int submitTime;
	int estRunTime;
	int cores;
	int memory;
	int disk;

	public Job(String id, String type, String submitTime, String estRunTime, String cores, String memory, String disk) {
		this.id = Integer.parseInt(id);
		this.type = type;
		this.submitTime = Integer.parseInt(submitTime);
		this.estRunTime = Integer.parseInt(estRunTime);
		this.cores = Integer.parseInt(cores);
		this.memory = Integer.parseInt(memory);
		this.disk = Integer.parseInt(disk);
	}
}