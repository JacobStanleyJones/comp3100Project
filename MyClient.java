import java.io.*;
import java.net.*;

public class MyClient {

	static Socket s;
	static BufferedReader in;
	static DataOutputStream dout;

	static Server[] servers;
	static int largestServerIdx;
	static int largestServerLimit = 0;

	public static void main(String[] args) {
		try {
			s = new Socket("localhost", 50000);
			in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			dout = new DataOutputStream(s.getOutputStream());

			String str = "";

			Handshake();
			
			int jobNum = 0;
			while(!str.equals("NONE")){
				dout.write(("REDY\n").getBytes());
				str = (String) in.readLine();
				System.out.println("message= " + str);
				
				if(servers == null)
					GetServerData();

				String[] jobData = str.split(" ");
				
				//If it's a job submission, schedule it to the largest server
				if(jobData[0].equals("JOBN")){
					String sch = "SCHD " + jobData[2] + " " + servers[largestServerIdx].sType +
						" " + jobNum % largestServerLimit + "\n";
					dout.write((sch).getBytes());

					str = (String) in.readLine();
					System.out.println("message= " + str);
					
					jobNum++;
				}
			}

			dout.write(("QUIT\n").getBytes());

			dout.flush();
			dout.close();
			s.close();
		}catch(Exception e){
			System.out.println(e);
		}
	}

	private static void Handshake(){
		try{
			dout.write(("HELO\n").getBytes());
			String str = (String) in.readLine();
			System.out.println("message= " + str);
	
			dout.write(("AUTH jacob\n").getBytes());
			str = (String) in.readLine();
			System.out.println("message= " + str);

		}catch(Exception e){
			System.out.println(e);
		}
	}

	private static void GetServerData(){
		try{
			dout.write(("GETS All\n").getBytes());
			String str = (String) in.readLine();
			System.out.println("message= " + str);
			String[] getsData = str.split(" ");
			int numServers = 0;
			if(getsData[1]!=".")
				numServers = Integer.parseInt(getsData[1]);

			servers = new Server[numServers];
			dout.write(("OK\n").getBytes());

			largestServerIdx = 0;

			for(int i = 0; i < numServers; i++){
				str = (String) in.readLine();
				System.out.println("message= " + str);
				String[] serverData = str.split(" ");

				//Add this server to the array
				servers[i] = new Server(serverData[0], Integer.parseInt(serverData[1]), Integer.parseInt(serverData[4]));
				
				//If this server has the most cores, save it as the largest server
				if(servers[i].sCores > servers[largestServerIdx].sCores){
					largestServerIdx = i;
					largestServerLimit = 1;
				}else if(servers[i].sType.equals(servers[largestServerIdx].sType)){
					largestServerLimit++;
				}
			}

			dout.write(("OK\n").getBytes());
			str = (String) in.readLine();
			System.out.println("message= " + str);
		}catch(Exception e){
			System.out.println(e);
		}
	}
}

class Server{
	String sType;
	int sId;
	int sCores;

	public Server(String type, int id, int cores){
		sType = type;
		sId = id;
		sCores = cores;
	}
}