package com.example.yangli.audiostream.media;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by feell on 8/17/2016.
 */

public class Demultiplex {
    private Map<Integer,List<short[]>> mList;
    private int mBufferSize;
    private int mMaxBufferSize;
    private float[] mFloatBuffer;
    private short[] mMixedBuffer;
    public Demultiplex(int vBufferSize){
        mList = new ConcurrentHashMap<Integer,List<short[]>>();
        mBufferSize = vBufferSize;
        mMaxBufferSize =  0;

    }
    public void push(long vSSRC,short[] vData){
        List<short[]> vList = mList.get(Integer.valueOf((int)vSSRC));
        if(vList == null)
        {
            vList = new CopyOnWriteArrayList<short[]>();
            vList.add(vData);
            //return;
        }else
            vList.add(vData);
        mList.put(Integer.valueOf((int)vSSRC),vList);
    }
    public short[] getMixedData(){

        if(mList.size() == 0) return null;
        //Log.e("PUB","size = "+ mList.size());
        mFloatBuffer = new float[1];
        for(Map.Entry<Integer,List<short[]>> vMap:mList.entrySet()){
            //System.out.printf("%s -> %s%n", entry.getKey(), entry.getValue());
            //Log.e("PUB","key "+ vMap.getKey() + " value = "+vMap.getValue());
            short[] vBuffer = getArrayValue(vMap.getValue());
            mMaxBufferSize = (mMaxBufferSize > vBuffer.length )? mMaxBufferSize:vBuffer.length;
           // Log.e("PUB","mFloatBuffer len = "+mFloatBuffer.length+"mMaxBufferSize = "+ mMaxBufferSize);
            mFloatBuffer = (float[]) resizeArray(mFloatBuffer,mMaxBufferSize);
            //Log.e("PUB","mFloatBuffer len = "+mFloatBuffer.length);
            for(int vIndex = 0; vIndex < vBuffer.length ; vIndex++){
                mFloatBuffer[vIndex] = mFloatBuffer[vIndex] + vBuffer[vIndex] / 32768.0f;
            }
        }

        if(mMixedBuffer != null)
            mMixedBuffer = null;

        mMixedBuffer = new short[mMaxBufferSize];
        String str ="";
        for(int vIndex = 0; vIndex < mMaxBufferSize ;vIndex++){
            float mixed = mFloatBuffer[vIndex];
            mixed *= 0.8;
            // hard clipping
            if (mixed > 1.0f) mixed = 1.0f;
            if (mixed < -1.0f) mixed = -1.0f;
            short outputSample = (short)(mixed * 32768.0f);
            mMixedBuffer[vIndex] = outputSample;
        }
        //Log.e("PUB",str);
        return mMixedBuffer;
    }
    private short[] getArrayValue(List<short[]> vList){
        short[] vBuffer = new short[vList.size()*mBufferSize];
        int vIndex = 0;
        for(short[] buffer:vList){
            System.arraycopy(buffer,0,vBuffer,vIndex*mBufferSize,mBufferSize);
            vIndex ++;
        }
        return vBuffer;
    }

    private static Object resizeArray (Object oldArray, int newSize) {
        int oldSize = java.lang.reflect.Array.getLength(oldArray);
        Class elementType = oldArray.getClass().getComponentType();
        Object newArray = java.lang.reflect.Array.newInstance(
                elementType, newSize);

        int preserveLength = Math.min(oldSize, newSize);
        if (preserveLength > 0)
            System.arraycopy(oldArray, 0, newArray, 0, preserveLength);
        return newArray;
    }
}
