package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    //Setting up the remote ports as an Array
    static final String[] REMOTE_PORTS=new String[]{"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;
    //Declaring the iterator variable for the key column
    private int key=0;
    //Declaring the column names of the Content Provider
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    //Uri Builder module to build the uri in the required format for Content Provider
    //The module is taken from the OnPTestClickListener Code
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        // The below snippets are taken from PA -1

        //To get the information about the current AVD port from the telephony manager
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        //Create a server task which listens on port 10000
        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        //Registering the send button with onclicklistener and sending the message(i.e creating a clientTask)
        final EditText editText = (EditText) findViewById(R.id.editText1);
        final View sendbtn=(View)  findViewById(R.id.button4);
        sendbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                Log.i("message",msg);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg,myPort);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    // The below code snippet is also taken from PA-1

    /************************************************************
     Person No:50290572
     ---------------
     References Used
     ---------------

     1) https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
     The above reference was used to understand the concept of sockets,client,server sockets and how to implement
     input and output streams along with BufferedReader and PrintWriter respectively

     2) https://docs.oracle.com/javase/7/docs/api/java/net/ServerSocket.html
     The above reference was used to understand the methods available for ServerSocket and their functionality

     3) https://docs.oracle.com/javase/7/docs/api/java/net/Socket.html
     The above reference was used to understand the methods available for Socket and their functionality

     4) https://stackoverflow.com/questions/10240694/java-socket-api-how-to-tell-if-a-connection-has-been-closed/10241044#10241044
     The above reference was to understand the concept of socket closing and how to identify that with readLine

     5)https://docs.oracle.com/javase/7/docs/api/java/io/BufferedReader.html
     The above docs was used to understand the BufferedReader Class and their functionality

     6)https://docs.oracle.com/javase/7/docs/api/java/io/PrintWriter.html
     The above docs was used to understand the PrintWriter Class and their functionality

     7)https://docs.oracle.com/javase/tutorial/essential/concurrency/sleep.html
     The above docs was used to understand sleep in a thread
     ************************************************************************/

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            try
            {
                //Server keeps listening for connections and accepting them and passing the message to Progress update
                while(true) {

                    //Accepts an incoming client connection
                    Socket client = serverSocket.accept();
                    //Reads the message from the input stream and sends to Progress Update through publish progress
                    BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String message = input.readLine();
                    if (message != null) {
                        publishProgress(message);
                        //Sending back acknowledgement to client
                        String result = "Received Message";
                        PrintWriter outserver = new PrintWriter(client.getOutputStream(),true);
                        outserver.println(result);
                    }

                    client.close();
                }
            }
            catch(Exception e)
            {
                Log.e(TAG,"Server couldn't accept a Client");
            }


            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            //Receiving the message
            String strReceived = strings[0].trim();
            //Displaying the message in the textView
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            //Appending the key and message(value) to the content values
            final ContentValues mContentValues=new ContentValues();
            //Getting the contentResolver for this context
            ContentResolver mContentResolver= getContentResolver();
            mContentValues.put(KEY_FIELD,Integer.toString(key));
            mContentValues.put(VALUE_FIELD,strReceived);
            //Creating the provider URI
            final Uri mUri= buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");
            //Increasing the key value
            key=key+1;
            try {
                //Inserting the value to content provider and in turn to the file
                mContentResolver.insert(mUri, mContentValues);
            }catch(Exception e)
            {
                Log.e(TAG,e.toString());
            }
            return;
        }
    }

    // The below code snippet is also taken from PA-1

    /************************************************************
     Person No:50290572
     ---------------
     References Used
     ---------------
     1) https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
     The above reference was used to understand the concept of sockets,client,server sockets and how to implement
     input and output streams along with BufferedReader and PrintWriter respectively

     2) https://docs.oracle.com/javase/7/docs/api/java/net/ServerSocket.html
     The above reference was used to understand the methods available for ServerSocket and their functionality

     3) https://docs.oracle.com/javase/7/docs/api/java/net/Socket.html
     The above reference was used to understand the methods available for Socket and their functionality

     4) https://stackoverflow.com/questions/10240694/java-socket-api-how-to-tell-if-a-connection-has-been-closed/10241044#10241044
     The above reference was to understand the concept of socket closing and how to identify that with readLine

     5)https://docs.oracle.com/javase/7/docs/api/java/io/BufferedReader.html
     The above docs was used to understand the BufferedReader Class and their functionality

     6)https://docs.oracle.com/javase/7/docs/api/java/io/PrintWriter.html
     The above docs was used to understand the PrintWriter Class and their functionality

     7)https://docs.oracle.com/javase/tutorial/essential/concurrency/sleep.html
     The above docs was used to understand sleep in a thread
     ************************************************************************/

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {

                int i=0;
                //Implementing the concept of reliable unicast to send message to all avds.
                //The loop is used to send msg to each avd one by one,that is the loop runs five times
                while(i<5) {
                    String remotePort = msgs[1];

                    //Retrieving the respective remote port from the array and creating a socket
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORTS[i]));

                    String msgToSend = msgs[0];
                    /*
                     * TODO: Fill in your client code that sends out a message.
                     */
                    //Put the message in the outputstream for server to read
                    PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                    output.println(msgToSend);

                    //wait to receive an acknowledgement from server & pausing the thread for half a second
                    Thread.sleep(500);

                    //Close socket if ack received from server
                    BufferedReader inserver = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String msgRcvd = inserver.readLine();
                    if (msgRcvd.contentEquals("Received Message")) {
                        Log.i(TAG, "Closing Client Socket"+Integer.toString(i));
                        socket.close();
                    }
                    i++;
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }catch(InterruptedException e){
                Log.e(TAG,"ClientTask InterruptedException");
            }

            return null;
        }
    }

}
