import java.io.*;
import java.net.* ;
import java.util.* ;

public final class WebServer
{
	public static void main(String argv[]) throws Exception
	{
		int port = Integer.parseInt(argv[0]);
		System.out.println("Port: " + port);
		
		ServerSocket socket;
		try{
			socket = new ServerSocket(port);
			Socket conn;

			while(true){
				conn = socket.accept();
				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				//System.out.println(in.readLine());
				if(in.ready()){
					HttpRequest request = new HttpRequest(conn);
					Thread thread = new Thread(request);
					thread.start();
				}
			//	in.close();
			}
			
		}catch(UnknownHostException e){
			System.out.println(e);
		}	
	}
}

final class HttpRequest implements Runnable
{
	final static String CRLF = "\r\n";
	Socket socket;

	//http constructor
	public HttpRequest(Socket socket) throws Exception
	{
		this.socket = socket;
	}
	
	//run
	public void run()
	{
		try{
			processRequest();
		}catch(Exception e){
			System.out.println(e);
		}
	}

	private void processRequest() throws Exception
	{
		InputStream is = socket.getInputStream();
		DataOutputStream os =  new DataOutputStream(socket.getOutputStream());
		
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		
		//Client message
		String requestLine = br.readLine();
		System.out.println(requestLine);
		
		String headerLine = null;
		while((headerLine = br.readLine()).length() != 0){
			System.out.println(headerLine);
		}
		
		StringTokenizer tokens = new StringTokenizer(requestLine);
		tokens.nextToken();
		String fileName = tokens.nextToken();

		fileName = "." + fileName;

		FileInputStream fis = null;
		boolean fileExists = true;
		try{
			fis = new FileInputStream(fileName);
		}catch(FileNotFoundException e){
			fileExists = false;
		}
		
		//Construct response

		String statusLine = null;
		String contentTypeLine = null;
		String entityBody = null;

		if(fileExists){
			statusLine = "200 OK";
			contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
		}else{
			statusLine = "404 Not Found";
			contentTypeLine = "text/html" + CRLF;
			entityBody = "<html><head><title>404 File Not Found</title></head><body><h1>404!</h1><p>The file was not found on the server!</p></body></html>";
		}
		
		//Send the status line
		os.writeBytes("HTTP/1.0 " + statusLine);

		//Send the content type
		os.writeBytes(contentTypeLine);

		//Send a blank line to ind. the end of header lines
		os.writeBytes(CRLF);

		//Send file
		if(fileExists){
			sendBytes(fis, os);
			fis.close();
		}else{
			os.writeBytes(entityBody);
		}

		os.close();
		br.close();
//		socket.close();
	}

	private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception
	{
		//Construct 1K buffer to hold bytes on way out of socket
		byte[] buffer = new byte[1024];
		int bytes = 0;

		//Copy requested file into the socket's output stream
		while((bytes = fis.read(buffer)) != -1){
			os.write(buffer, 0, bytes);
		}
	}

	private static String contentType(String fileName)
	{
		if(fileName.matches(".html?$")){
			return "text/html";
		}else if(fileName.matches(".gif$")){
			return "image/gif";
		}else if(fileName.matches(".jpe?g$")){
			return "image/jpeg";
		}else if(fileName.matches(".png$")){
			return "image/png";
		}
		return "application/octet-stream";
	}
		
}
