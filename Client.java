import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

/**
 * The client for the ticket system.
 * Usage: java Client [host] [port]
 * @author Evan Bechtol
 *
 */
public class Client
{
	static Scanner inputScanner;
	static Socket socket;
	static BufferedReader in;
	static PrintWriter out;	
	public static void main(String[] args)
	{
		// Get host and port arguments
		if(args.length < 2)
		{
			System.err.println("Not enough arguments. Usage: \"java Client [host] [port]\"");
			System.exit(-1);
		}
		String host = args[0];
		short port = -1;
		try
		{
			port = Short.parseShort(args[1]);
		} catch (NumberFormatException e) {
			System.err.println("Unable to parse port.");
			System.exit(-1);
		}

		// Try to open a connection to the host:port
		try
		{
			socket = new Socket(host, port);
			in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out    = new PrintWriter(socket.getOutputStream(), true);
			
		} catch (IOException e)
		{
			System.err.println("Unable to connect to host.");
			System.exit(-1);
		}
		
		try
		{
			inputScanner = new Scanner(System.in);
			boolean finished = false;
			while(!finished)
			{
				// Display menu
				System.out.println("A. Display the list of files\n"
						+ "B. Get a file\n"
						+ "C. Delete a file\n"
						+ "D. Add a new file\n"
						+ "E. Exit\n");
				
				// Get and do choice
				char choice = query("choice").toUpperCase().charAt(0);
				String line;
				
				switch(choice)
				{
					case 'A': // send LIST to server, 
						sendMessage("LIST");
						ArrayList<String> files = new ArrayList<>();
						line = "";
						
						while(!(line = in.readLine()).equals("END-FILES"))
						{
							String[] split = line.split(" ", 3);
							files.add(String.format("\t%s.\t%s", split[1], split[2]));
						}
						
						System.out.println("File list:");
						
						for(String file : files)
							System.out.println(file);
						
						System.out.println();
						break;
						
					case 'B': // Get a particular file
						String getFile = query("file number");
						sendMessage("GET " + getFile);
						line = in.readLine();
						String split [] = line.split(":");
						if(line.equals("IN-USE"))
						{
							System.out.println("\nSorry, the file is currently in-use.\n");
							break;
						}
						
						PrintWriter pw = new PrintWriter(split[0], "UTF-8");
						pw.format(Locale.US,  "%s\n", split[1]);
						pw.flush();
						pw.close();
						System.out.println("File saved as " + split[0] + "\n");
						break;
						
					case 'C': // Remove existing file
						String file = query("file number");
						sendMessage("DEL " + file);
						line = in.readLine();
						
						if(line.equals("CONFIRMED")) {
							System.out.println("File removed from the server.\n");
						}
						else {
							System.out.println("File could not be removed from the server.\n");
						}
						break;
						
					case 'D': // Add new file
						String addFile = query("file name");
						sendMessage("ADD " + addFile);
						
						line = in.readLine();

						if(line.equals("READY")) {
							Scanner input = new Scanner(new File(addFile));
							String contents = input.useDelimiter("\\Z").next();
							
							sendMessage(contents);
							
							line = in.readLine();
							if(line.equals("SAVED")) {
								System.out.println("File saved to server.\n");
							}
							else {
								System.out.println("File could not be saved to server.\n");
							}
							input.close();
						}
						else {
							System.out.println("Server not ready to save file, please try again.\n");
						}
						break;
						
					case 'E': // Exit the client
						finished = true;
						socket.close();
						break;
				}
			}
		} catch (IOException e)
		{
			System.err.println("Encountered IO problem.");
			System.exit(-1);
		}
		
		
	}
	
	/**
	 * Convenience method to send message to Server.
	 * @param message the message to send
	 */
	public static void sendMessage(String message)
	{
		out.println(message);
		out.flush();
	}
	
	/**
	 * Convenience method to query for a string
	 * @param name What to tell the user to input
	 * @return What the user input
	 */
	public static String query(String name)
	{
		System.out.print("Enter " + name + ": ");
		return inputScanner.nextLine();
	}
}