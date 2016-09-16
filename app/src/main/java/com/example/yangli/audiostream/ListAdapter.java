package com.example.yangli.audiostream;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by feell on 8/17/2016.
 */

public class ListAdapter extends BaseAdapter {
    ArrayList<RemoteInfo> mList = null;
    Context mContext;
    ListAdapter(Context context){
        mContext = context;
        mList = new ArrayList<RemoteInfo>();
    }
    public void add(RemoteInfo data){
        mList.add(data);
    }

    public boolean isExist(RemoteInfo findItem){
        for(RemoteInfo value:mList)
        {
            if(value.RemoteAddress.equalsIgnoreCase(findItem.RemoteAddress) && value.RemotePort == findItem.RemotePort)
                return true;
        }
        return false;
    }
    public  void remove(int position){
        mList.remove(position);
    }
    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public RemoteInfo getItem(int position) {

        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (null == convertView) {
            convertView = ((Activity) mContext).getLayoutInflater().inflate(
                    R.layout.list_element, null);


            }
        TextView txt = (TextView) convertView.findViewById(R.id.textView_IP);
        RemoteInfo vInfo = mList.get(position);
        String listItem = "RemoteIP:" + vInfo.RemoteAddress + " ,RemotePort:"+ vInfo.RemotePort + ",LocalPort:"+vInfo.ListeningPort;
        txt.setText(listItem);

        return convertView;

    }
}

class RemoteInfo{
    RemoteInfo(String address,int port, int listeningport){
        RemoteAddress = address;
        RemotePort = port;
        ListeningPort = listeningport;
    }
    public String RemoteAddress;
    public int   RemotePort;
    public int   ListeningPort;
}