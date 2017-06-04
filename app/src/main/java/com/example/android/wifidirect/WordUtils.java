package com.example.android.wifidirect;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by nikhildange on 03/03/16.
 */
public class WordUtils {
    String inputLine = null;
    int totalCount = 0;
    Context context;
    TreeMap<String,Integer> map = new TreeMap<String,Integer>();

    TreeMap runMem(String in,Context con,String fileName){
        inputLine = in;
        context = con;
        wordInit();
        readFile(fileName);
        return map;
    }

    public void readFile(String fileName){
        String fileText = "";
        AssetManager assetManager = context.getAssets();
        BufferedReader reader = null;
        try{
            reader = new BufferedReader( new InputStreamReader(context.getAssets().open(fileName),"UTF-8"));
            String textValue = "";
            while ((textValue = reader.readLine()) != null){
              //  System.out.println("TextValue : '"+textValue+" Length : "+textValue.length());
                fileWordCount(textValue);
              //  fileText = fileText+textValue;
            }
        }
        catch (Exception e){
            System.out.println("Exception Caught : "+e);
        }
        finally{
            if (reader != null){
                try{
                    reader.close();
                }
                catch (Exception e){
                    System.out.println("Exception Caught : "+e);
                }
            }
        }
       // return fileText;
    }

    void display(){
        Set<Map.Entry<String,Integer>> entrySet = map.entrySet();
        for(Map.Entry<String,Integer> entry : entrySet){
            System.out.println(entry.getValue() + "\t" + entry.getKey()+"\t");
        }
    }
    void wordInit(){
        String[] words = inputLine.split("[ \n\t\r.,;:!?(){}]");

        //Arrays.sort(words);
        //words = WordUtils.insertionSort(words);

        for(int wordCounter = 0; wordCounter < words.length ; wordCounter++){
            //System.out.println(words[wordCounter]);
            String key = words[wordCounter];//.toUpperCase();

            if (key.length()>0){
                if(map.get(key) == null){
                    map.put(key,0);
                }
                else{
                    // int curCount = map.get(key).intValue();
                    // curCount++;
                    // map.put(key,curCount);
                }
            }
        }
    }

    void fileWordCount(String text){
       // String word = "a";
        int wordCount = 0;
        Scanner s = new Scanner(text);
        String val;
        while (s.hasNext()){
            val = s.next();
            // System.out.println(" Text "+val);
            totalCount++;
            if(map.get(val) != null)
            {
                int curCount = map.get(val).intValue();
                curCount++;
                map.put(val,curCount);
            }
            //if (val.equals(word)) wordCount++;
        }
        //System.out.println("Total Text : "+totalCount);//+" word "+wordCount+"\n\n");

    }

    public static void main(String args[])
    {


    }
}
