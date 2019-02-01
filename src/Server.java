import java.io.*;
import java.net.*;
import java.util.*;

public class Server implements Runnable{

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean isLoggedIn;
    private String username;
    private static List<Server> loggedInUsers;


    Server(Socket socket){

        this.socket = socket;
        isLoggedIn = false;

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(),true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String [] args){

        loggedInUsers = new ArrayList<>();

        try {
            ServerSocket ss = new ServerSocket(7777);
            while (true){
                Socket s = ss.accept();
                System.out.println("connected.");
                new Thread(new Server(s)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        while(true){

            try {
                String data = in.readLine();
                parseData(data);
            } catch (IOException e) {
                continue;
            }
        }
    }

    private void parseData(String data) {

        String [] message = data.split("#");

        switch (message[0]){

            case "L":
                if(isLoggedIn)
                    break;

                if(validateLogIn(message[1],message[2],message[3])){
                    isLoggedIn = true;
                    username = message[1];
                    loggedInUsers.add(this);

                    if(message[3].equals("admin")) {
                        out.println("accept#" + message[1] + "#admin");

                    }
                    else
                        out.println("accept#" + message[1]+ "#normal");
                }
                else out.println("decline");
                break;

            case "S":
                if(isLoggedIn){
                    if(message[1].equals("show")){
                        System.out.println(message[2]);

                        StringBuffer returnMessage = new StringBuffer("show#");

                        for(int i=0; i<loggedInUsers.size(); i++){
                            returnMessage.append(loggedInUsers.get(i).username+"#");
                        }

                        System.out.println(returnMessage.toString());
                        out.println(returnMessage.toString());
                    }

                    if(message[1].equals("logout")){
                        System.out.println(message[2]);

                        isLoggedIn = false;
                       for(int i=0; i<loggedInUsers.size();i++){
                            if(username.equals(loggedInUsers.get(i).username)){
                                loggedInUsers.remove(i);
                           }
                        }

                        out.println("disconnect");
                        try {
                            socket.close();
                        } catch (IOException e) {
                            System.err.println("error in closing socket");
                        }
                    }
                }
                break;

            case "B":

                for(Server receiver:loggedInUsers){
                    if(receiver!=this)
                        receiver.out.println("broad#"+ message[1]+ " (from: "+username+", admin)");
                }
                break;

            case "C":
                if(isLoggedIn){
                    for(Server receiver:loggedInUsers){
                        if(receiver.username.equals(message[1])) {
                            receiver.out.println("text#" + message[2] + " (from: " + username + ")");

                            if(message.length>3){
                                receiver.out.println("file#"+message[3]+"#"+message[4]);
                                receiver.receive_and_send(this,message[4]);
                            }
                        }
                    }
                }
        }
    }

    private void receive_and_send(Server sender,String size) {
        try
        {
            int filesize = Integer.parseInt(size);
            byte[] contents = new byte[10000];

            InputStream is = sender.socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            int bytesRead;
            int total = 0;

            while(total!=filesize)
            {
                bytesRead=is.read(contents);
                os.write(contents);
                total += bytesRead;

            }
            os.flush();
        }
        catch(Exception e)
        {
            System.err.println("Could not transfer file.");
        }
    }

    private boolean validateLogIn(String username, String password, String type) {

        FileReader fin = null;
        try {
            fin = new FileReader("data.txt");
        } catch (FileNotFoundException e) {
            System.err.println("couldn't read file");
        }

        Scanner in = new Scanner(fin);
        String [] entry;

        while(in.hasNextLine()){
            entry = in.nextLine().split("#");

            if(entry[0].equals(username) && entry[1].equals(password) && entry[2].equals(type)){
                in.close();
                return true;
            }
        }

        in.close();
        return false;
    }
}
