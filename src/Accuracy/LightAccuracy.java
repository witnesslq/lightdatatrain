package Accuracy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.*;

import FileAnalysis.Util;
import StopJudge.Constant;

public class LightAccuracy {
    /**
     * key:lightid_特征日_时间段，value:(a,b)
     */
    public static HashMap<String, String> resultLeast = null;

    public static HashMap<String, String> secondStopMap = null;

    public static void main(String[] args) {
//		getresultOfLeast("/Users/yuxiao/项目/毕设/文件/2016/four_result/resultOfLeast/");
        getresultOfLeast(Constant.ResultOfLeast);
        getSecondDataMap(Constant.SecondStopDataPath);
        System.out.println("Catch A and B ready!");

        HashMap<String,String> acurracyImproMap = new HashMap<>();
        AccuracyOfAllDays(Constant.TestlightData_mid, Constant.AllDaysAccuracyCSV,acurracyImproMap);
        PTAccuracyOfAllDays(Constant.TestlightData_mid, Constant.TestAllDaysAccuracyCSV,acurracyImproMap);
        System.out.println("end");

    }


    public static void getSecondDataMap(String input) {
        secondStopMap = new HashMap<String, String>();
        try {
            File inputfile = new File(input);
            BufferedReader reader = new BufferedReader(new FileReader(inputfile));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] lineitems = line.split(",");
                if (lineitems.length != 3) continue;
                secondStopMap.put(lineitems[0], lineitems[1] + "_" + lineitems[2]);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void getresultOfLeast(String input) {
        resultLeast = new HashMap<String, String>();
        try {
            File file1 = new File(input);
            if (!file1.exists()) {
                System.out.println("File of FourTable not found! please check!");
                return;
            } else {
                File[] files = file1.listFiles();
                for (File file2 : files) {
                    String lightId = file2.getName().substring(0, file2.getName().indexOf('.'));
                    BufferedReader reader = new BufferedReader(new FileReader(file2));
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        String[] items = line.split(",");
                        String key = lightId + "_" + items[0] + "_" + items[1];
                        resultLeast.put(key, items[2] + "," + items[3]);
                    }
                    reader.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //增加数据天数

    /**
     * 原始算法
     * AccuracyOfAllDays(Constant.TrafficLightMidPath, Constant.AllDaysAccuracyCSV);
     *
     * @param input
     * @param output
     */
    public static void AccuracyOfAllDays(String input, String output,HashMap<String,String> accuracyImpMap) {
        try {
            File originTestCSV = new File("/Users/yuxiao/项目/毕设/文件/2016/测试/originTest.csv");
            FileWriter originTestWriter = new FileWriter(originTestCSV);
            //key为lightId,value 为这个路口的数据集合
            HashMap<String, ArrayList<File>> map = new HashMap<String, ArrayList<File>>();
            File file1 = new File(input);
            if (!file1.exists()) {
                System.out.println("Your Path is null!");
                return;
            } else {
                if (file1.isDirectory()) {

                    File[] files1 = file1.listFiles(); //读取每一天的文件夹
                    for (File file2 : files1) {
                        File[] files2 = file2.listFiles();//每一天的红绿灯路口数据集
                        for (File file3 : files2) //读取一个路口的数据
                        {
                            String lightId = file3.getName().substring(0, file3.getName().indexOf('.'));
                            if (map.containsKey(lightId)) {
                                map.get(lightId).add(file3);
                            } else {
                                ArrayList<File> fileMap = new ArrayList<File>();
                                fileMap.add(file3);
                                map.put(lightId, fileMap);
                                int a;
                            }
                        }
                    }
                }
            }
            //遍历map, <信号灯Id,所有文件>
            FileWriter fw = new FileWriter(new File(output));
            Iterator iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String keyID = entry.getKey().toString();
                ArrayList<File> fileMap = map.get(keyID);
                System.out.println(keyID + "   File counter:" + fileMap.size() + " 个");
                // 暂时规定误差的标准为30S， 误差在30S以内，则认为是准确的
                int standard = 20;
                int counter1 = 0, counter2 = 0;
                int count = 0; // 总数计数器，需要找出这天每个红绿灯的停车次数
                double totalRate = 0;
                for (File file4 : fileMap) //分析每个路口的数据集
                {
                    BufferedReader reader = new BufferedReader(new FileReader(file4));
                    String line = "";

                    while ((line = reader.readLine()) != null) {
                        String[] items = line.split(",");
                        if (items.length < 6) continue;
                        //过滤停车次数
                        if (!items[5].equals("2")) {
                            continue;
                        } else if (Double.parseDouble(items[2].trim()) < 40) //60真实停车时间50 40 70
                        {
                            continue;
                        }
                        /////TODO/////
//						else if(!inTime(items[1].trim(),7,10,17,20))
//						{
//							continue;
//						}
                        else {  //只对有停车的数据计算准确率
                            double forcastDelayTime =0;  // 预测公交车停车延误时间
                            String time = items[1].trim();
                            int day = Util.getDay(time);
                            int hour = Util.getHour(time);
                            String key = items[0] + "_" + day + "_" + hour;
                            if (!resultLeast.containsKey(key))
                                continue;
                            String result = resultLeast.get(key);
                            if (result.split(",")[0].equals("Error") || result.split(",")[1].equals("Error"))
                                continue;
                            double A = Double.parseDouble(result.split(",")[0]);
                            double B = Double.parseDouble(result.split(",")[1]);
                            //items[2] 真实停车时间
                            double error = Math.abs((Double.parseDouble(items[3]) * A + B) - Double.parseDouble(items[2]));
                            count++;
                            double tempRate = 0;//一个公交车数据文件计算出的准确度
                            if ((Math.abs((Double.parseDouble(items[3]) * A + B) - Double.parseDouble(items[2]))) <= 1) {
                                tempRate = 1;
                            } else {
                                //准确度计算公式  items[3] 到路口距离
                                forcastDelayTime = Double.parseDouble(items[3]) * A + B;
                                tempRate = 1 - (Math.abs((Double.parseDouble(items[3]) * A + B) - Double.parseDouble(items[2])) / Double.parseDouble(items[2]));
                                if(tempRate >0){
                                    // 原有系统的预测结果放入到对比性测试的map中,以待测试准确度的提高
                                    accuracyImpMap.put(key+","+items[2],tempRate+","+forcastDelayTime);
                                }
                                String outputEachLine = key+","+items[2]+","+items[3]+","+tempRate+","+forcastDelayTime;
                                originTestWriter.write(outputEachLine+"\n");
                            }
                            if (tempRate < 0) {
                                totalRate += 0;
                            } else totalRate += tempRate;
                            if (error > standard)
                                counter2++;
                            else counter1++;
                        }
                    }
                    reader.close();
                }

                double rate = counter1 * 1.00000 / count;
                String line2 = "";
//					line2 += lightId+","+counter1+","+count+","+rate+","+totalRate / count;
                if (count == 0 || totalRate == 0) continue;  //文件数为0，防止溢出错误
                double resRate = totalRate / count;
                if (resRate < Constant.originRange) continue;
                line2 += keyID + "," + count + "," + totalRate / count; //平均准确度
                fw.write(line2 + "\r\n");
                fw.flush();
            }
            originTestWriter.close();

            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 距离标志算法修正
     *
     * @param input
     * @param output
     */
    public static void PTAccuracyOfAllDays(String input, String output,HashMap<String,String> accuracyImpMap) {



        try {
            FileWriter compareTestCSV = new FileWriter("/Users/yuxiao/项目/毕设/文件/2016/测试/CompareTest.csv");
            File pTTestCSV = new File("/Users/yuxiao/项目/毕设/文件/2016/测试/PTtest.csv");
            FileWriter ptTestWriter = new FileWriter(pTTestCSV);
            HashMap<String,String> resultOfCompareCSV = new HashMap<>();

            //key为lightId,value 为这个路口的数据集合
            HashMap<String, ArrayList<File>> map = new HashMap<String, ArrayList<File>>();
            File file1 = new File(input);
            if (!file1.exists()) {
                System.out.println("Your Path is null!");
                return;
            } else {
                if (file1.isDirectory()) {
                    File[] files1 = file1.listFiles(); //读取每一天的文件夹
                    for (File file2 : files1) {
                        File[] files2 = file2.listFiles();//每一天的红绿灯路口数据集
                        for (File file3 : files2) //读取一个路口的数据
                        {
                            String lightId = file3.getName().substring(0, file3.getName().indexOf('.'));
                            if (map.containsKey(lightId)) {
                                map.get(lightId).add(file3);
                            } else {
                                ArrayList<File> fileMap = new ArrayList<File>();
                                fileMap.add(file3);
                                map.put(lightId, fileMap);
                            }
                        }
                    }
                }
            }
            //遍历map, <信号灯Id,所有文件>
            FileWriter fw = new FileWriter(new File(output));
            Iterator iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String keyID = entry.getKey().toString();
                ArrayList<File> fileMap = map.get(keyID);
                System.out.println(keyID + "   File counter:" + fileMap.size() + " 个");
                // 暂时规定误差的标准为30S， 误差在30S以内，则认为是准确的
                int standard = 20;
                int counter1 = 0, counter2 = 0;
                int count = 0; // 总数计数器，需要找出这天每个红绿灯的停车次数
                double totalRate = 0;
                for (File file4 : fileMap) //分析每个路口的数据集
                {
                    BufferedReader reader = new BufferedReader(new FileReader(file4));
                    String line = "";

                    while ((line = reader.readLine()) != null) {
                        String[] items = line.split(",");
                        if (items.length < 6) continue;
                        //过滤停车次数
                        if (!items[5].equals("2")) {
                            continue;
                        } else if (Double.parseDouble(items[2].trim()) < 40) //60真实停车时间 50 40
                        {
                            continue;
                        }
                        /////TODO/////
//						else if(!inTime(items[1].trim(),7,10,17,20))
//						{
//							continue;
//						}
                        else {  //只对有停车的数据计算准确率
                            String time = items[1].trim();
                            int day = Util.getDay(time);
                            int hour = Util.getHour(time);
                            String key = items[0] + "_" + day + "_" + hour;
                            if (!resultLeast.containsKey(key))
                                continue;
                            String result = resultLeast.get(key);
                            if (result.split(",")[0].equals("Error") || result.split(",")[1].equals("Error"))
                                continue;
                            double A = Double.parseDouble(result.split(",")[0]);
                            double B = Double.parseDouble(result.split(",")[1]);
                            //items[2] 真实停车时间
                            double error = Math.abs((Double.parseDouble(items[3]) * A + B) - Double.parseDouble(items[2]));
                            count++;
                            double tempRate = 0;//一个公交车数据文件计算出的准确度
                            if ((Math.abs((Double.parseDouble(items[3]) * A + B) - Double.parseDouble(items[2]))) <= 1) {
                                tempRate = 1;
                            } else {
                                double forcastDelayTime =0;  // 预测公交车停车延误时间

                                if (secondStopMap.containsKey(key)) {
                                    double toCrossingDis = Double.parseDouble(items[3]);
                                    String[] pTValues = secondStopMap.get(key).split("_");
                                    double pDis = Double.parseDouble(pTValues[0]);
                                    double tTime = Double.parseDouble(pTValues[1]);
                                    if (toCrossingDis >= pDis) {
                                        // 大于距离标志
                                        //准确度计算公式  items[3] 到路口距离
                                        forcastDelayTime=tTime + Double.parseDouble(items[3]) * A + B;
                                        tempRate = 1 - (Math.abs(tTime + (Double.parseDouble(items[3]) * A + B) - Double.parseDouble(items[2])) / Double.parseDouble(items[2]));

                                    } else {
                                        //准确度计算公式  items[3] 到路口距离
                                        forcastDelayTime=Double.parseDouble(items[3]) * A + B;
                                        tempRate = 1 - (Math.abs((Double.parseDouble(items[3]) * A + B) - Double.parseDouble(items[2])) / Double.parseDouble(items[2]));
                                    }
                                    String tmpKey = key+","+items[2];
                                    if(accuracyImpMap.containsKey(tmpKey)){
                                        // 拥有相同的红绿灯号和停车时间,准确度提高的提取到结果map中 红绿灯号时间段,实际停车延误,原有系统预测延误,改进后系统预测延误
                                        String originRateAndDelay = accuracyImpMap.get(tmpKey);
                                        String[] originRateAndDelayItem = originRateAndDelay.split(",");
                                        double originAccuracy =  Double.valueOf(originRateAndDelayItem[0]);
                                        if (tempRate > originAccuracy && originAccuracy > 0.5 && tempRate >0.5){
                                            // 目前筛选准确度大于0.5的
                                            resultOfCompareCSV.put(tmpKey,originRateAndDelayItem[1]+","+forcastDelayTime);
                                        }
                                    }
                                } else {
                                    //准确度计算公式  items[3] 到路口距离
                                    forcastDelayTime=Double.parseDouble(items[3]) * A + B;
                                    tempRate = 1 - (Math.abs((Double.parseDouble(items[3]) * A + B) - Double.parseDouble(items[2])) / Double.parseDouble(items[2]));
                                }

                                String outputEachLine = key+","+items[2]+","+items[3]+","+tempRate+","+forcastDelayTime;
                                ptTestWriter.write(outputEachLine+"\n");

                            }
                            if (tempRate < 0) {
                                totalRate += 0;
                            } else totalRate += tempRate;
                            if (error > standard) //standard 60秒
                                counter2++;
                            else counter1++;
                        }
                    }
                    reader.close();
                }

                double rate = counter1 * 1.00000 / count;
                String line2 = "";
//					line2 += lightId+","+counter1+","+count+","+rate+","+totalRate / count;
                if (count == 0 || totalRate == 0) continue;
                double resRate = totalRate / count;
                if (resRate < Constant.newRange) continue;

                line2 += keyID + "," + count + "," + totalRate / count; //平均准确度
                fw.write(line2 + "\r\n");
                fw.flush();
            }
            ptTestWriter.close();
            fw.close();

            for(Map.Entry<String,String> entry: resultOfCompareCSV.entrySet()){
                compareTestCSV.write(entry.getKey()+","+entry.getValue()+"\n");
            }
            resultOfCompareCSV.clear();
            accuracyImpMap.clear();
            compareTestCSV.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
