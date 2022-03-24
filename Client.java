import java.io.*;  
import java.net.*;  
public class MyClient {  
public static void main(String[] args) {  
try{      
Socket s=new Socket("localhost",50000);  
BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
DataOutputStream dout=new DataOutputStream(s.getOutputStream()); 

String str;
 
dout.write(("HELO\n").getBytes());  

str=(String)in.readLine();  
System.out.println("message= "+str); 

dout.write(("AUTH jacob\n").getBytes());

str=(String)in.readLine();  
System.out.println("message= "+str);

dout.write(("REDY\n").getBytes());

str=(String)in.readLine();  
System.out.println("message= "+str);

dout.write(("QUIT\n").getBytes());

dout.flush();  
dout.close();  
s.close();  
}catch(Exception e){System.out.println(e);}  
}  
}