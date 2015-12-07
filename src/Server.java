import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * The server for the ticket system.
 * Usage: java Server [port]
 * @author Evan Bechtol
 *
 */
public class Server
{
	static ArrayList <String>files;
	static ArrayList <String>contents;
	
	static ServerSocket server;
	
	public static void main(String[] args)
	{
		// Get port from arguments
		String filename = "files.txt";
		short port = -1;
		try
		{
			if(args.length >= 1)
				port = Short.parseShort(args[0]);
			else
			{
				System.err.println("No port given. Usage \"java Server [port]\"");
				System.exit(-1);
			}
		} catch (NumberFormatException e) {
			System.err.println("Unable to parse port.");
			System.exit(-1);
		}
		
		// Read files... file.
		ArrayList<String> lines = new ArrayList<>();
		try(Scanner scan = new Scanner(new File(filename)))
		{
			while(scan.hasNextLine())
				lines.add(scan.nextLine());
		} catch (FileNotFoundException e)
		{
			System.err.println("ERROR: " + filename + " was not found.");
			System.exit(-1);
		}
		// Parse file contents into arrays
		files = new ArrayList <String> ();
		contents = new ArrayList <String> ();
		for(int k=0; k<lines.size(); k++)
		{
			String split[] = lines.get(k).split(":");
			files.add(split[0]);
			contents.add(split[1]);
		}
		
		// Start the server and begin accepting clients into threads
		try
		{
			server = new ServerSocket(port);
			System.out.println("Server is running on port "+port+"...");
			while(true)
			{
				try
				{
					Thread t = new Thread(new ServerThread(server.accept()));
					t.start();
				} catch (IOException e) {
					System.out.println("Client accept failed.");
					System.exit(-1);
				}
			}
		} catch (IOException e)
		{
			System.err.println("Unable to start server.");
			System.exit(-1);
		}
		
	}
	
	
	public static synchronized boolean removeFile(int index) {
		files.remove(index - 1);
		contents.remove(index - 1);
		return true;
	}
	
	/**
	 * The server thread that handles a single client.
	 * @author Rahat
	 *
	 */
	public static class ServerThread implements Runnable
	{
		BufferedReader in;
		PrintWriter out;
		Socket socket;
		int reserved;
		int reservedMovie;
		
		/*
		 * Constructor sets up input and output
		 * and misc variables
		 */
		public ServerThread(Socket s) throws IOException
		{
			socket = s;
			in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			out = new PrintWriter(s.getOutputStream(), true);
			reserved = 0;
			reservedMovie = -1;
		}

		
		@Override
		public void run()
		{
			System.out.println("Received client "+socket.getInetAddress().getHostAddress());
			String msg;
			// Keep reading the client's input until socket closed
			while(!socket.isClosed())
			{
				try
				{
					msg = in.readLine();
					if(msg == null)
						break;
					String[] split = msg.split(" ");
					String cmd = split[0];
					
					// Handle specific client commands
					switch(cmd)
					{
						case "LIST": // Requests a list of files
							for(int k = 0; k < files.size(); k++)
								sendMessage("FILES " + (k + 1) + " " + files.get(k));
							sendMessage("END-FILES");
							break;
							
						case "GET": // Attempts to get a file from the list
							int id = Integer.parseInt(split[1]);
							sendMessage(files.get(id - 1) + " : " + contents.get(id - 1));
							break;
							
						case "DEL": // Remove a file from the list
							int index = Integer.parseInt(split[1]);

							removeFile(index);
							sendMessage("CONFIRMED");
							break;
							
						case "ADD": // Add a file to the file list
							String fileName = split[1];
							files.add(fileName);
							
							// Get the contents of the file
							sendMessage("READY");
							
							msg = in.readLine();
							String content = msg;
							contents.add(content);
							
							// Append the new file to the list
							try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("movies.txt", true)))) {
							    out.println(files.get(files.size() - 1) + " : " + contents.get(contents.size() - 1));

							}catch (IOException e) {
							   System.out.println(e.getStackTrace());
							}
							
							sendMessage("SAVED");
							break;
					}
				} catch (IOException e)
				{
					break;
				}
			}
			System.out.println("Client " + socket.getInetAddress().getHostAddress() + " has left");
		}
		
		/**
		 * Convenience method to send a message to the client.
		 * @param message the message to send
		 */
		public void sendMessage(String message)
		{
			out.println(message);
			out.flush();
		}
	}
}