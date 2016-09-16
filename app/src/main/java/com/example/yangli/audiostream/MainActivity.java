package com.example.yangli.audiostream;

import android.app.Activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yangli.audiostream.media.AudioStreamer;
import com.example.yangli.audiostream.media.Demultiplex;
import com.example.yangli.audiostream.media.RtpFactory;

import com.example.yangli.audiostream.rtp.AudioCodec;
import com.example.yangli.audiostream.rtp.AudioStream;
import com.example.yangli.audiostream.rtp.RtpSocket;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class MainActivity extends Activity {


    private AudioStreamer mAudioStreamer;
    private String TAG = "PUB";
    private EditText mIpAddress;
    private EditText mPortNum;
    private CheckBox mMuteMic;
    private TextView mSystemMessage;
    private ListView mListView;
    private Button mStartTx;
    private Button mStartRx;
    private Button mStop;
    private ListAdapter mListAdapter;
    public Activity mActivity;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActivity = this;
        InitUI();
        mAudioStreamer = RtpFactory.createAudioStreamer(0, null, 8);
        /*DatagramSocket mSocket = null;


        try {
            mAudioStreamer = RtpFactory.createAudioSession(8, null, 8);
            Log.e(TAG, "mRtpSession = " + mAudioStreamer);
            mAudioStreamer.StartTx("239.192.2.2", 20000, 8000);
        } catch (Exception e) {
            Log.e(TAG, "" + e.getMessage());
        }
        */
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    void InitUI() {
        mIpAddress = (EditText) findViewById(R.id.IpAddress);
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start,
                                       int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) +
                            source.subSequence(start, end) +
                            destTxt.substring(dend);
                    if (!resultingTxt.matches("^\\d{1,3}(\\." +
                            "(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i = 0; i < splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        };
        mIpAddress.setFilters(filters);
        mPortNum = (EditText) findViewById(R.id.PortNum);
        mMuteMic = (CheckBox) findViewById(R.id.Mute);
        mSystemMessage = (TextView) findViewById(R.id.SystemMsg);
        mListView = (ListView) findViewById(R.id.ListView);
        mStartTx = (Button) findViewById(R.id.StartTx);
        mStartRx = (Button) findViewById(R.id.StartRx);
        mStop = (Button) findViewById(R.id.Stop);

        mStartTx.setOnClickListener(mClickListener);
        mStartRx.setOnClickListener(mClickListener);
        mStop.setOnClickListener(mClickListener);
        mMuteMic.setOnClickListener(mClickListener);
        mListAdapter = new ListAdapter(this);
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // selected item
                //String product = ((TextView) view).getText().toString();
                AlertDialog.Builder adb=new AlertDialog.Builder(MainActivity.this);
                adb.setTitle("Delete?");
                adb.setMessage("Are you sure you want to delete " + position);
                final int positionToRemove = position;
                adb.setNegativeButton("Cancel", null);
                adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        RemoteInfo vInfo = mListAdapter.getItem(positionToRemove);
                        mAudioStreamer.StopRx(vInfo.RemoteAddress,vInfo.RemotePort,vInfo.ListeningPort);
                        mListAdapter.remove(positionToRemove);
                        mListAdapter.notifyDataSetChanged();
                    }});
                adb.show();
            }
        });
    }

    View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String vIpAddress;
            int vPortNum;
            vIpAddress = mIpAddress.getText().toString();
            try {
                vPortNum = Integer.valueOf(mPortNum.getText().toString());
            } catch (Exception e) {
                vPortNum = 20000;
            }
            if (view == mStartTx) {


                if (vIpAddress.equalsIgnoreCase("")) {
                    new AlertDialog.Builder(mActivity)
                            .setTitle("Error")
                            .setMessage("Input the correct IpAddress and port!")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return;
                }
                mAudioStreamer.StartTx(vIpAddress, Integer.valueOf(vPortNum), 8000);
                mStartTx.setEnabled(false);
                mStartRx.setEnabled(false);
            } else if (view == mStartRx) {


                if (vIpAddress.equalsIgnoreCase("")) {
                    new AlertDialog.Builder(mActivity)
                            .setTitle("Error")
                            .setMessage("Input the correct IpAddress and port!")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return;
                }
                RemoteInfo vInfo = new RemoteInfo(vIpAddress, Integer.valueOf(vPortNum), Integer.valueOf(vPortNum));
                if (!mListAdapter.isExist(vInfo)) {
                    mAudioStreamer.StartRx(vIpAddress, Integer.valueOf(vPortNum), Integer.valueOf(vPortNum));
                    mListAdapter.add(vInfo);
                }
                //mListView.

            } else if (view == mStop) {
                mAudioStreamer.StopTx();
                mStartTx.setEnabled(true);
                mStartRx.setEnabled(true);

            } else if (view == mMuteMic) {
                mAudioStreamer.toggleMute();
            }
        }
    };

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
