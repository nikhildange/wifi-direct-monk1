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
    private Socket clientSocketArray[] = new Socket[10];
    private int clientSocketCount = 0;

	protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private boolean isServer = false;
	private View mContentView = null;
	private WifiP2pDevice device;
	private WifiP2pInfo info;
	ProgressDialog progressDialog = null;

    ServerSocket serverSocket = null;
    BufferedReader in;
    PrintWriter out;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

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
						((DeviceActionListener) getActivity()).disconnect();
					}
				});

		mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// Allow user to pick an image from Gallery or other
						// registered apps
//						Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//						intent.setType("image/*");
//						startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);

                        String messageReceived = "Tapp on button";

                        if (isServer)
                            broadcastMessage("Hi from server");
                        else {
                            out.println(messageReceived);
                            Log.d("info","Message from client : "+messageReceived);
                        }
//                        Toast t = Toast.makeText(getActivity().getApplicationContext(), messageReceived, Toast.LENGTH_SHORT);
//                        t.show();
					}
				});

		return mContentView;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		String localIP = Utils.getLocalIPAddress();
		// Trick to find the ip in the file /proc/net/arp
		String client_mac_fixed = new String(device.deviceAddress).replace("99", "19");
		String clientIP = Utils.getIPFromMac(client_mac_fixed);

		// User has picked an image. Transfer it to group owner i.e peer using
		// FileTransferService.
		Uri uri = data.getData();
		TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
		statusText.setText("Sending: " + uri);
		Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
		Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
		serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
		serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());

		if(localIP.equals(IP_SERVER)){
			serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS, clientIP);
		}else{
			serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS, IP_SERVER);
		}

		serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, PORT);
		getActivity().startService(serviceIntent);
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

	private void broadcastMessage (String message) {
        if (isServer) {
            for (int i = 0; i < clientSocketCount; i++) {
                String str = "Sending Message to #"+(i+1)+" client : "+message;
                Log.d("info",str);
                try {
                    Socket client = clientSocketArray[i];
                    out = new PrintWriter(client.getOutputStream(), true);
                    out.println(str);
                } catch (IOException e) {
                    Log.e("info", "Error handling client " + e);
                    e.printStackTrace();
                }
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
                                    clientSocketArray[clientSocketCount] = client;
                                    clientSocketCount++;
                                    Log.e("info","Socket Count : "+clientSocketCount);
                                    BufferedReader in = new BufferedReader(
                                            new InputStreamReader(client.getInputStream()));
                                    PrintWriter out = new PrintWriter(client.getOutputStream(), true);
									while(true) {
//										InputStream inputStream = client.getInputStream();
//                           	     		DataInputStream dataInputStream = new DataInputStream(inputStream);
//                                		String messageReceived = dataInputStream.readUTF();
                                        Log.d("info","waiting for client message");
                                        String messageReceived = in.readLine();
                                        Log.d("info","Message Received from Client : "+messageReceived);
//                                        Toast t = Toast.makeText(getActivity().getApplicationContext(), messageReceived, Toast.LENGTH_SHORT);
//                                        t.show();
									}
								} catch (Exception e){
                                    Log.e("info","Error handling client " + e);
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
                                    String sendMessage = "Namasker";
                                    Socket server = new Socket();
                                    InetAddress serverIPAddress = info.groupOwnerAddress;
                                    int portClient = 8986;
                                    server.bind(null);
                                    server.connect(new InetSocketAddress(serverIPAddress, portClient), SOCKET_TIMEOUT);
//                                    OutputStream outputStream = server.getOutputStream();
//                                    DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
//                                    dataOutputStream.writeUTF(sendMessage);
                                    in = new BufferedReader(
                                            new InputStreamReader(server.getInputStream()));
                                    out = new PrintWriter(server.getOutputStream(), true);
                                    while (true) {
                                        Log.d("info","waiting for server message");
                                        String messageReceived = in.readLine();
                                        Log.d("info","Message Received from Server : "+messageReceived);
//                                        Toast t = Toast.makeText(getActivity().getApplicationContext(), messageReceived, Toast.LENGTH_SHORT);
//                                        t.show();
                                    }
                                }
                                catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                );
                clientThread.start();
            }
	}

    public static Enumeration<InetAddress> getWifiInetAddresses(final Context context) {
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        final String macAddress = wifiInfo.getMacAddress();
        final String[] macParts = macAddress.split(":");
        final byte[] macBytes = new byte[macParts.length];
        for (int i = 0; i< macParts.length; i++) {
            macBytes[i] = (byte)Integer.parseInt(macParts[i], 16);
        }
        try {
            final Enumeration<NetworkInterface> e =  NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                final NetworkInterface networkInterface = e.nextElement();
                if (Arrays.equals(networkInterface.getHardwareAddress(), macBytes)) {
                    return networkInterface.getInetAddresses();
                }
            }
        } catch (SocketException e) {
            Log.wtf("WIFIIP", "Unable to NetworkInterface.getNetworkInterfaces()");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static<T extends InetAddress> T getWifiInetAddress(final Context context, final Class<T> inetClass) {
        final Enumeration<InetAddress> e = getWifiInetAddresses(context);
        while (e.hasMoreElements()) {
            final InetAddress inetAddress = e.nextElement();
            if (inetAddress.getClass() == inetClass) {
                return (T)inetAddress;
            }
        }
        return null;
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

}
