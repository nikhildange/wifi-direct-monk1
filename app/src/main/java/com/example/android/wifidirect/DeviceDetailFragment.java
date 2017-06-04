/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wifidirect;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;

import static android.content.Context.WIFI_SERVICE;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

	public static final String IP_SERVER = "192.168.49.1";
	public static int PORT = 8988;
	private static boolean server_running = false;
    private static final int SOCKET_TIMEOUT = 5000;

	protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private boolean isServer = false;
	private View mContentView = null;
	private WifiP2pDevice device;
	private WifiP2pInfo info;
	ProgressDialog progressDialog = null;

    /*For server side*/
    ServerSocket serverSocket = null;
    /*For client side*/
    BufferedReader in;
    PrintWriter out;

    Set<DeviceProfile> profileSet = new HashSet<DeviceProfile>();
    private String mydeviceId = null;

    int inputMat[][];
    int numberOfCapablity;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        numberOfCapablity = 3;

        DeviceProfile deviceProfile = new ProfileReader().profileReader(getActivity());
        mydeviceId = deviceProfile.devId;
        profileSet.add(deviceProfile);
        deviceProfile.logDetails();

		mContentView = inflater.inflate(R.layout.device_detail, null);
		mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				WifiP2pConfig config = new WifiP2pConfig();
				config.deviceAddress = device.deviceAddress;
				config.wps.setup = WpsInfo.PBC;
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
						"Connecting to :" + device.deviceAddress, true, true
						//                        new DialogInterface.OnCancelListener() {
						//
						//                            @Override
						//                            public void onCancel(DialogInterface dialog) {
						//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
						//                            }
						//                        }
				);
				((DeviceActionListener) getActivity()).connect(config);

			}
		});

		mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
                        disconnectFromNetwork();
					}
				});

		mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
                        if (isServer) {
                            broadcastMessageToClients("Running Algorithm on server");
                            runAlgorithm();
                        }
                        else {
                            String messageReceived = "Ping from client";
                            sendMessageToServer(messageReceived);
                            Log.d("info","Message from client : "+messageReceived);
                        }
//                        Toast t = Toast.makeText(getActivity().getApplicationContext(), messageReceived, Toast.LENGTH_SHORT);
//                        t.show();
					}
				});

		return mContentView;
	}

	private void disconnectFromNetwork() {
            if (isServer){
                broadcastMessageToClients("disconnect");
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                serverSocket = null;
            }
            else {
                sendMessageToServer("disconnect");
            }
            ((DeviceActionListener) getActivity()).disconnect();
    }

	@Override
	public void onConnectionInfoAvailable(final WifiP2pInfo info) {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		this.info = info;
		this.getView().setVisibility(View.VISIBLE);

        isServer = ((info.isGroupOwner == true) ?  true : false);

		// The owner IP is now known.
		TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
		view.setText(getResources().getString(R.string.group_owner_text)
				+ ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
						: getResources().getString(R.string.no)));

		// InetAddress from WifiP2pInfo struct.
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

		mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);

		if (!server_running){
			new ServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text)).execute();
			server_running = true;
		}

		// hide the connect button
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
		establishConnectionForCommunication(info);
	}

	private void sendProfile(Socket socket) {
        try {
            OutputStream os = socket.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            DeviceProfile deviceProfile = profileSet.iterator().next();
            deviceProfile.result = "ClientSideMessage";
            System.out.println("Profile Send :" + deviceProfile.devId);
            oos.writeObject(deviceProfile);
//            oos.close();
//            os.close();
            Log.i("info","Profile Sent:");
            deviceProfile.logDetails();
        }
        catch (Exception e){
            Log.e("info", "Exception Error : " + e);
            e.printStackTrace();
        }
    }

    private DeviceProfile receiveProfile(Socket socket) {
        try {
            InputStream is = socket.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(is);
            DeviceProfile receivedProfile = (DeviceProfile)ois.readObject();
//            ois.close();
//            is.close();
            Log.i("info","Profile Received:");
            receivedProfile.socket = socket;
            receivedProfile.logDetails();
            profileSet.add(receivedProfile);
            broadcastMessageToClients("Welcome");
            return  receivedProfile;
        }
        catch (Exception e){
            Log.e("info", "Exception Error : " + e);
            e.printStackTrace();
        }
        return null;
    }

    private void removeProfile(String message) {
        if (isServer){
            Iterator<DeviceProfile> it = profileSet.iterator();
            String exitingDeviceId = message.replace("_disconnect","");
            while (it.hasNext()) {
                DeviceProfile devProfile = it.next();
                String nextDeviceId = devProfile.devId;
                if (nextDeviceId.equals(exitingDeviceId)) {
                    Log.i("info","Removing Profile :");
                    devProfile.logDetails();
                    try {
                        devProfile.socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    it.remove();
                }
            }
        }
    }

    private void sendMessageToServer (String message) {
        if (!isServer)
        out.println(mydeviceId+"_"+message);
    }

	private void broadcastMessageToClients (String msg) {
        if (isServer) {
            Iterator<DeviceProfile> it = profileSet.iterator();
            int i = 0;
            String message = "server"+"_"+msg;
            while (it.hasNext()) {
                try {
                    DeviceProfile dp = it.next();
                    if (!dp.devId.equals(mydeviceId)) {
                        Log.i("info","Sending Message to #"+(i+1)+" client : "+message);
                        Socket client = dp.socket;
                        out = new PrintWriter(client.getOutputStream(), true);
                        out.println(message);
                    }
                } catch (IOException e) {
                    Log.e("info", "Error broadcasting to client's " + e);
                    e.printStackTrace();
                }
                i++;
            }
        }
    }

	private void establishConnectionForCommunication(final WifiP2pInfo info) {
			if (info.groupFormed && info.isGroupOwner) {
				Thread serverThread = new Thread(
						new Runnable() {
							@Override
							public void run() {
								try {
									int port = 8986;
									Log.d("info","Started Server Thread");
                                    if (serverSocket == null)
                                            serverSocket = new ServerSocket(port);
//									ServerSocket serverSocket = new ServerSocket();
//									serverSocket.setReuseAddress(true);
//									serverSocket.bind(new InetSocketAddress(port));
                                    Socket client = serverSocket.accept();
                                    receiveProfile(client);
                                    BufferedReader in = new BufferedReader(
                                            new InputStreamReader(client.getInputStream()));
                                    PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                                    try {
                                        while(true ) {
                                            String messageReceived = in.readLine();
                                            Log.d("info","Message Received from Client : "+messageReceived);
                                            if (messageReceived.contains("_disconnect")) {
                                                removeProfile(messageReceived);
                                            }
//                                            if (messageReceived == null) {
//                                                Log.e("info","Message NULL on client #");
//                                                break;
//                                            }
                                        }
                                    }
                                    catch (Exception e) {
                                        Log.i("info","Exception in Server : "+e+" on client #");
                                    }
                                    finally {
//                                        in.close();
//                                        out.close();
//                                        client.close();
                                    }
								} catch (Exception e){
                                    Log.e("info","Error : " + e);
									e.printStackTrace();
								}
                            }
						}
				);
				serverThread.start();
			}
			else if (info.groupFormed) {
                Thread clientThread = new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Socket server = new Socket();
                                    InetAddress serverIPAddress = info.groupOwnerAddress;
                                    int portClient = 8986;
                                    server.bind(null);
                                    server.connect(new InetSocketAddress(serverIPAddress, portClient), SOCKET_TIMEOUT);
                                    sendProfile(server);
                                    in = new BufferedReader(
                                            new InputStreamReader(server.getInputStream()));
                                    out = new PrintWriter(server.getOutputStream(), true);
                                    try {
                                        while (true) {
                                            String messageReceived = in.readLine();
                                            Log.d("info","Message Received from Server : "+messageReceived);
                                            if (messageReceived.equals("server_disconnect")) {
                                                Log.e("info","Message NULL from server");
                                                break;
                                            }
                                            if (messageReceived == null) {
                                                Log.e("info","Message NULL from server");
                                                break;
                                            }
                                        }
                                    }
                                    catch (Exception e) {
                                        Log.i("info","Exception in Client : "+e);
                                    }
                                    finally {
//                                        in.close();
//                                        out.close();
//                                        server.close();
                                    }
                                }
                                catch (IOException e) {
                                    Log.e("info","Error : " + e);
                                    e.printStackTrace();
                                }
                            }
                        }
                );
                clientThread.start();
            }
	}

	/**
	 * Updates the UI with device data
	 * 
	 * @param device the device to be displayed
	 */
	public void showDetails(WifiP2pDevice device) {
		this.device = device;
		this.getView().setVisibility(View.VISIBLE);
		TextView view = (TextView) mContentView.findViewById(R.id.device_address);
		view.setText(device.deviceAddress);
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText(device.toString());

	}

	/**
	 * Clears the UI fields after a disconnect or direct mode disable operation.
	 */
	public void resetViews() {
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
		TextView view = (TextView) mContentView.findViewById(R.id.device_address);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.group_owner);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.status_text);
		view.setText(R.string.empty);
		mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
		this.getView().setVisibility(View.GONE);
	}

	/**
	 * A simple server socket that accepts connection and writes some data on
	 * the stream.
	 */
	public static class ServerAsyncTask extends AsyncTask<Void, Void, String> {

		private final Context context;
		private final TextView statusText;

		/**
		 * @param context
		 * @param statusText
		 */
		public ServerAsyncTask(Context context, View statusText) {
			this.context = context;
			this.statusText = (TextView) statusText;
		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				ServerSocket serverSocket = new ServerSocket(PORT);
				Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
				Socket client = serverSocket.accept();
				Log.d(WiFiDirectActivity.TAG, "Server: connection done");
				final File f = new File(Environment.getExternalStorageDirectory() + "/"
						+ context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
						+ ".jpg");

				File dirs = new File(f.getParent());
				if (!dirs.exists())
					dirs.mkdirs();
				f.createNewFile();

				Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
				InputStream inputstream = client.getInputStream();
				copyFile(inputstream, new FileOutputStream(f));
				serverSocket.close();
				server_running = false;
				return f.getAbsolutePath();
			} catch (IOException e) {
				Log.e(WiFiDirectActivity.TAG, e.getMessage());
				return null;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				statusText.setText("File copied - " + result);
				Intent intent = new Intent();
				intent.setAction(android.content.Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.parse("file://" + result), "image/*");
				context.startActivity(intent);
			}

		}

		/*
		 * (non-Javadoc)
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			statusText.setText("Opening a server socket");
		}

	}

	public static boolean copyFile(InputStream inputStream, OutputStream out) {
		byte buf[] = new byte[1024];
		int len;
		try {
			while ((len = inputStream.read(buf)) != -1) {
				out.write(buf, 0, len);

			}
			out.close();
			inputStream.close();
		} catch (IOException e) {
			Log.d(WiFiDirectActivity.TAG, e.toString());
			return false;
		}
		return true;
	}

    public void printMat(int[][] matrix,int row,int col){
        for(int r=0; r<row;r++){
            for(int c=0; c<col;c++){
                System.out.print(matrix[r][c]+"\t");
            }
            System.out.println();
        }
        System.out.println();
    }

    void generateInputMat(){
        int bigVal = numberOfCapablity > profileSet.size() ? numberOfCapablity : profileSet.size();
        inputMat = new int[bigVal][bigVal];
        Iterator<DeviceProfile> it = profileSet.iterator();
        DeviceProfile deviceProfile;
        int devNo = 0;

        while (it.hasNext()){
            deviceProfile = it.next();
            inputMat[devNo][0] = deviceProfile.getBatVal();
            inputMat[devNo][1] = deviceProfile.getCpuVal();
            inputMat[devNo][2] = deviceProfile.getMemVal();
            devNo++;
        }

        printMat(inputMat, profileSet.size(), numberOfCapablity);
    }

    void generateMatrix() {
        int col = numberOfCapablity;
        int row = profileSet.size();
        int bigVal = col > row ? col : row;

        for (int i = 0; i < bigVal; i++) {

            if (i < row) {
                for (int j = 0; j < bigVal; j++) {
                    if (j < col) {
                        inputMat[i][j] = inputMat[i][j];//Integer.parseInt(profileInfoMat[i][j+1]);
                    } else {
                        inputMat[i][j] = 0;
                    }
                }
            } else {
                for (int j = 0; j < bigVal; j++)
                    inputMat[i][j] = 0;
            }
        }
        printMat(inputMat, profileSet.size(), numberOfCapablity);
    }

    public void runRankerAlgoritm(){
        double per = 0;
        int width = numberOfCapablity > profileSet.size() ? numberOfCapablity : profileSet.size();

        int[] maxArray = new int[width];
        for(int r=0; r<width;r++){
            int max = 0;
            for(int c=0; c<width;c++){
                if (inputMat[c][r] >= max) {
                    max = inputMat[c][r];
                }
            }
            maxArray[r] = max;
        }

        //	for (int j =0; j< width;j++)
        //      	System.out.print(maxArray[j]+" ");

        for(int c=0; c<width;c++){
            for(int r=0; r<width;r++){
                if (inputMat[r][c]!=0) {
                    per = (((double)inputMat[r][c])/(double)maxArray[c]);
                    per = per*(width);
                    inputMat[r][c] = (int)(Math.round(per));//(int)(11-(Math.round(per)));
                }
            }
        }
        System.out.println("\nOutput for Comparator Algorithm : ");
        printMat(inputMat,profileSet.size(),numberOfCapablity);
    }

    private void runHungarianAlgorithm() {
        long time = System.currentTimeMillis();
        AlgoMethod algorithm = new AlgoMethod(inputMat,numberOfCapablity > profileSet.size() ? numberOfCapablity : profileSet.size());
        System.out.println(String.format("\nTotal time taken to execute : %dms\n", System.currentTimeMillis() - time));

        String capArr[] = {"MEM","BAT","CPU"};

        int[] r = algorithm.result();
        System.out.println("Output : ");
        for (int k =0 ; k < r.length;k++)
        {
            System.out.print("\n\n VALUE OF R : "+r[k]+" at "+k+"\n\n");
        }

        Iterator<DeviceProfile> it = profileSet.iterator();
        DeviceProfile d;
        int i = 0;
        String str = "";
        while (it.hasNext()) {
            d = it.next();
            str =  " "+d.getBatVal() + " ";
            str = str + d.getCpuVal() + " ";
            str = str + d.getMemVal();
            System.out.print("\n\n VALUE OF DEVICE "+d.devId+" : "+str+"\n\n");
//                            display = d.devId + "device_id" +i + 1 + " Device " + capArr[r[i]]/*(r[i] + 1)*/ + " Capability, Cost : " + profileInfoMat[i][r[i+1]];
            System.out.print("\n\n VALUE OF R : "+r[i]+"\n\n");
            System.out.println("device "+(i+1)+" with ID "+d.devId + " will work on " + capArr[r[i]]/*(r[i] + 1)*/ + " Capability");// Cost : " + inputMat[i][r[i]]);
//                            t = Toast.makeText(getActivity().getApplicationContext(), display, Toast.LENGTH_SHORT);
//                            t.show();
            i++;
        }

        System.out.println("\nTotal Cost : " + algorithm.total());
    }

    private void runAlgorithm() {
        generateInputMat();
        if (numberOfCapablity != profileSet.size()) {
            generateMatrix();
        }
        runRankerAlgoritm();
        runHungarianAlgorithm();
    }
}
