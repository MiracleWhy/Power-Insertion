package com.process;

import com.process.entity.Ele;
import com.process.entity.Nod;
import com.process.utils.JavaSerializable;
import com.process.utils.MapUtil;
import com.process.utils.StringUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author wangheyu
 * @description: TODO
 * @date 2020/3/21
 */
public class PreProcessing {
    private double DELTA_LOWER_X = 181, DELTA_UPPER_X = 231, DELTA_LOWER_Y = -20.0, DELTA_UPPER_Y = 20.0;

    private final static String NOD_DIC_START = "nid";
    private final static String NOD_DIC_END = "*NODE";
    private final static String ELE_DIC_START = "eid";
    private final static String ELE_DIC_END = "*END";

    private final static String FRACTURE_MAP_SUFFIX="_eleFractureMap.dat";
    private final static String NEW_NODE_DIC_SUFFIX="_newNodeDic.dat";


    private String FILE_PATH = "";
    private String FILE_NAME = "";

    private String OUTPUT_PATH="";

    private String part1 = "3";
    private String part2 = "4";

    PreProcessing(double x1,double x2,double y1,double y2,String outputPath,String part1,String part2){
        this.DELTA_LOWER_X = x1;
        this.DELTA_UPPER_X = x2;
        this.DELTA_LOWER_Y = y1;
        this.DELTA_UPPER_Y = y2;
        this.OUTPUT_PATH = outputPath;
        this.part1=part1;
        this.part2=part2;
    }

    PreProcessing(String threshold,String inputPath,String outputPath,String part1,String part2) throws Exception{
        String[] thresholdArray = threshold.split(",");
        if(thresholdArray.length==4) {
            this.DELTA_LOWER_X = Double.valueOf(thresholdArray[0]);
            this.DELTA_UPPER_X = Double.valueOf(thresholdArray[1]);
            this.DELTA_LOWER_Y = Double.valueOf(thresholdArray[2]);
            this.DELTA_UPPER_Y = Double.valueOf(thresholdArray[3]);
        }else{
            throw new Exception("阈值参数错误");
        }
        this.FILE_PATH = inputPath;
        this.OUTPUT_PATH = outputPath;
        this.part1=part1;
        this.part2=part2;
    }

    PreProcessing(){

    }


    private void process(List<String> oriFileList) {

        long startTime = System.currentTimeMillis();
        long processTime = startTime;
        System.out.println("处理开始:文件行数:" + oriFileList.size());

        String fileName = getFileNameByPath(FILE_PATH);
        String filePath = FILE_PATH.replace(fileName,"");

        Map<String, Ele> eleMap = new LinkedHashMap<>();
        Map<String, Nod> nodMap = new LinkedHashMap<>();

        JavaSerializable javaSerializable = new JavaSerializable();



        //文件处理
        Iterator<String> iterator = oriFileList.iterator();
        boolean setToNod = false;
        boolean setToEle = false;
        while (iterator.hasNext()) {
            String currentLine = iterator.next();

            if (currentLine.contains(ELE_DIC_START)) {
                setToEle = true;
                setToNod = false;
                continue;
            }
            if (currentLine.contains(ELE_DIC_END)) {
                setToEle = false;
                setToNod = false;
                continue;
            }
            if (currentLine.contains(NOD_DIC_START)) {
                setToNod = true;
                setToEle = false;
                continue;
            }
            if (currentLine.contains(NOD_DIC_END)) {
                setToNod = false;
                setToEle = false;
                continue;
            }
            if (setToEle) {
                Ele ele = new Ele(currentLine);
                eleMap.put(ele.getEid(), ele);
            }
            if (setToNod) {
                Nod nod = new Nod(currentLine);
                nodMap.put(nod.getNid(), nod);
            }
        }
        processTime = System.currentTimeMillis();
        System.out.println(String.format("com.process.entity.Ele,Nod字典建立完毕,耗时:%s,com.process.entity.Ele size:%s,com.process.entity.Nod size:%s", (processTime - startTime), eleMap.size(), nodMap.size()));
        startTime = processTime;


        int max_node = nodMap.size();

        //开裂，非开裂单元处理
        Map<String, Ele> eleFractureMap = new LinkedHashMap<>();
        Map<String, Ele> eleUnFractureMap = new LinkedHashMap<>();
        //开裂，非开裂节点处理
        Map<String, Nod> nodeFractureMapTemp = new LinkedHashMap<>();
        Map<String, Nod> nodeUnFractureMapTemp = new LinkedHashMap<>();
        Map<String, Nod> nodeEdgeMap = new LinkedHashMap<>();
        Map<String, Nod> nodeFractureMap = new LinkedHashMap<>();
        Map<String, Nod> nodeUnFractureMap = new LinkedHashMap<>();


        //遍历eleDic，区分开裂与非开裂单元
        eleMap.keySet().stream().forEach(eid -> {
            Ele ele = eleMap.get(eid);
            String[] nArray = ele.getN();

            boolean isFracture = true;
            for (String n : nArray) {
                Nod nod = nodMap.get(n);
                double x = Double.valueOf(nod.getX());
                double y = Double.valueOf(nod.getY());
                if (x >= DELTA_LOWER_X && x <= DELTA_UPPER_X
                        && y >= DELTA_LOWER_Y && y <= DELTA_UPPER_Y) {
                    //do nothing
                } else {
                    isFracture = false;
                    break;
                }
            }
            if (isFracture) {
                eleFractureMap.put(ele.getEid(), ele);
                Arrays.stream(ele.getN()).forEach(nid -> {
                    if (!nodeFractureMapTemp.keySet().contains(nid)) {
                        nodeFractureMapTemp.put(nid, nodMap.get(nid));
                    }
                });
            } else {
                eleUnFractureMap.put(ele.getEid(), ele);
                Arrays.stream(ele.getN()).forEach(nid -> {
                    if (!nodeUnFractureMapTemp.keySet().contains(nid)) {
                        nodeUnFractureMapTemp.put(nid, nodMap.get(nid));
                    }
                });
            }
        });

        //计算开裂与非开裂节点
        nodeEdgeMap = MapUtil.getIntersectionSetByGuava(nodeFractureMapTemp, nodeUnFractureMapTemp);
        nodeFractureMap = nodeFractureMapTemp; //com.process.utils.MapUtil.getDifferenceSetByGuava(nodeFractureMapTemp,nodeEdgeMap);
        nodeUnFractureMap = nodeUnFractureMapTemp; //com.process.utils.MapUtil.getDifferenceSetByGuava(nodeUnFractureMapTemp,nodeEdgeMap);

        processTime = System.currentTimeMillis();
        System.out.println(String.format("开裂单元，非开裂单元 处理完毕,耗时:%s,开裂单元:%s,非开裂单元:%s,开裂节点:%s,非开裂节点:%s,边界节点:%s", (processTime - startTime), eleFractureMap.size(), eleUnFractureMap.size(), nodeFractureMapTemp.size(), nodeUnFractureMapTemp.size(), nodeEdgeMap.size()));
        startTime = processTime;

        Map<String, Nod> New_node_dic = new LinkedHashMap<>(); ;
        Object fractureMapTemp = javaSerializable.load(FILE_PATH + FRACTURE_MAP_SUFFIX);
        Object newNodeDicTemp = javaSerializable.load(FILE_PATH + NEW_NODE_DIC_SUFFIX);
        if (fractureMapTemp != null && newNodeDicTemp != null) {
            System.out.println("读取到中间计算结果");
            ((LinkedHashMap<String, Ele>) fractureMapTemp).keySet().stream().forEach(eid->{
                eleFractureMap.put(eid,((LinkedHashMap<String, Ele>) fractureMapTemp).get(eid));
            });
            ((LinkedHashMap<String, Nod>) newNodeDicTemp).keySet().stream().forEach(nid->{
                New_node_dic.put(nid,((LinkedHashMap<String, Nod>) newNodeDicTemp).get(nid));
            });
        } else {
            Map<String, Integer> Node_appeartimes = new LinkedHashMap<>();
            //遍历分界区节点+开裂区单元，出现次数+1
            nodeEdgeMap.keySet().stream().forEach(item -> {
                Node_appeartimes.put(item, 1);
            });
            //遍历开裂区单元,出现次数+1
            eleFractureMap.keySet().stream().forEach(item -> {
                Ele ele = eleFractureMap.get(item);
                Arrays.stream(ele.getN()).forEach(n -> {
                    if (Node_appeartimes.containsKey(n)) {
                        Node_appeartimes.put(n, Node_appeartimes.get(n) + 1);
                    } else {
                        Node_appeartimes.put(n, 1);
                    }
                });
            });

            processTime = System.currentTimeMillis();
            System.out.println(String.format("节点出现次数统计完毕，耗时:%s", (processTime - startTime)));
            startTime = processTime;


            //建立新节点编号字典，实现节点分裂
            List<String> nodeUnFractureKeyList = nodeUnFractureMap.keySet().stream().collect(Collectors.toList());
            List<String> nodeFractureKeyList = nodeFractureMap.keySet().stream().collect(Collectors.toList());

            AtomicInteger count = new AtomicInteger(0);
            nodMap.keySet().stream().forEach(nid -> {
                System.out.println(String.format("节点分裂处理中，当前进度%s/%s", count.incrementAndGet(), nodMap.size()));
                //若节点在非开裂区内，则编号不变
                if (nodeUnFractureKeyList.contains(nid)) {
                    synchronized (this) {
                        New_node_dic.put(nid, nodMap.get(nid));
                    }
                }
                //若节点在开裂区内，复制重复次数
                if (nodeFractureKeyList.contains(nid)) {
                    for (int i = 0; i < Node_appeartimes.get(nid); i++) {
                        String key = String.valueOf((int) (i * Math.pow(10, String.valueOf(max_node).length())) + Integer.valueOf(nid));
                        synchronized (this) {
                            New_node_dic.put(key, nodMap.get(nid));
                        }
                    }
                }
            });
            //TODO:计算中间结果保存机制
//        try {
//            File file = new File(FILE_PATH + "_new_node_dic.dat");
//            com.process.utils.JavaSerializable javaSerializable = new com.process.utils.JavaSerializable();
//            javaSerializable.store(New_node_dic, new FileOutputStream(file));
//            Map<String, com.process.entity.Nod> New_node_dic_backup = new LinkedHashMap<>();
//            New_node_dic_backup = (LinkedHashMap<String, com.process.entity.Nod>) javaSerializable.load(new FileInputStream(file));
//        }catch (Exception e){
//            e.printStackTrace();
//        }
            processTime = System.currentTimeMillis();
            System.out.println(String.format("节点分裂完成，耗时:%s，新节点容量:%s", (processTime - startTime), New_node_dic.size()));
            startTime = processTime;


            //开裂区单元节点号更新
            Map<String, Boolean> New_node_assign = new ConcurrentHashMap<>();
            New_node_dic.keySet().stream().sorted((e1, e2) -> {
                return e1.compareTo(e2);
            }).forEach(key -> {
                New_node_assign.put(key, false);
            });
            nodeEdgeMap.keySet().stream().forEach(key -> {
                New_node_assign.put(key, true);
            });
            AtomicInteger count2 = new AtomicInteger(0);
            //这里需要使用单线程确保顺序
            eleFractureMap.keySet().parallelStream().sorted((e1, e2) -> {
                return Integer.valueOf(e1).compareTo(Integer.valueOf(e2));
            }).forEach(eid -> {
                System.out.println(String.format("开裂区单元节点号更新处理中，当前进度%s/%s", count2.incrementAndGet(), eleFractureMap.size()));
                Ele ele = eleFractureMap.get(eid);
                for (int i = 0; i < ele.getN().length; i++) {
                    Iterator<String> iteratorNewNodeAssign = New_node_assign.keySet().iterator();
                    while (iteratorNewNodeAssign.hasNext()) {
                        String k = iteratorNewNodeAssign.next();
                        synchronized (New_node_assign) {
                            if (New_node_assign.get(k) == false && yu(k, max_node).equals(ele.getN()[i])) {
                                ele.getN()[i] = k;
                                New_node_assign.put(k, true);
                            }
                        }
                    }
                }
            });

            processTime = System.currentTimeMillis();
            System.out.println(String.format("开裂区单元节点号更新完成，耗时:%s", (processTime - startTime)));
            startTime = processTime;

            //TODO:保存更新后的eleFractureMap
            javaSerializable.store(eleFractureMap, FILE_PATH + "_eleFractureMap.dat");
            javaSerializable.store(New_node_dic, FILE_PATH + "_newNodeDic.dat");
        }

        //开始生成cohesive单元
        Integer b = eleMap.size();
        Map<String, String[]> Cohesive_dic = new ConcurrentHashMap<>();
        List<String> Cohesive_horizontal = new Vector<>();
        AtomicInteger count1 = new AtomicInteger(0);
        AtomicInteger bIndex = new AtomicInteger(eleMap.size());
        eleFractureMap.keySet().parallelStream().forEach(ef -> {
            System.out.println(String.format("生成cohesive单元处理中，当前进度%s/%s",count1.incrementAndGet(),eleFractureMap.size()));
            eleUnFractureMap.keySet().stream().forEach(euf -> {
                String[] efnArray = eleFractureMap.get(ef).getN();
                List<String[]> New_ele_lis = new ArrayList<>();
                Arrays.stream(efnArray).forEach(n ->
                {
                    if(Arrays.asList(eleUnFractureMap.get(euf).getN()).contains(yu(n,max_node))){
                            String[] item = {n, yu(n, max_node)};
                            New_ele_lis.add(item);
                    }
                });

                if (New_ele_lis.size() == 4) {
                    String[] array = {
                            New_ele_lis.get(0)[0],
                            New_ele_lis.get(1)[0],
                            New_ele_lis.get(3)[0],
                            New_ele_lis.get(2)[0],
                            New_ele_lis.get(0)[1],
                            New_ele_lis.get(1)[1],
                            New_ele_lis.get(3)[1],
                            New_ele_lis.get(2)[1]
                    };
                    Cohesive_dic.put(String.valueOf(bIndex.incrementAndGet()), array);
                }
            });
        });

        processTime = System.currentTimeMillis();
        System.out.println(String.format("cohesive单元节点更新完成，耗时:%s,容量:%s", (processTime - startTime),Cohesive_dic.size()));
        startTime = processTime;

        //水平单元计算开始
        System.out.println("水平单元计算开始");
        List<String> keyList = eleFractureMap.keySet().stream().collect(Collectors.toList());
        Map<String,Boolean> compareTable = new ConcurrentHashMap<>();
        AtomicInteger count3 = new AtomicInteger(0);
        AtomicInteger bbIndex = new AtomicInteger(eleMap.size()+Cohesive_dic.size());
        keyList.parallelStream().forEach(iKey->{
            System.out.println(String.format("水平单元计算中:当前进度:%s/%s",count3.incrementAndGet(),keyList.size()));
            keyList.stream().forEach(jKey->{
                //已经比较过，不再比较
                if (compareTable.containsKey(iKey + jKey) || compareTable.containsKey(jKey + iKey)) {
                    return;
                } else {
                    compareTable.put(iKey + jKey, true);
                }
                //List<String> iList = Arrays.asList(eleFractureMap.get(iKey).getN());
                //List<String> jList = Arrays.asList(eleFractureMap.get(jKey).getN());
                List<String[]> New_ele_lis = new ArrayList<>();
                for (String m : eleFractureMap.get(iKey).getN()) {
                    for (String n : eleFractureMap.get(jKey).getN()) {
                        if (yu(m, max_node).equals(yu(n, max_node))) {
                            String[] item = {m, n};
                            New_ele_lis.add(item);
                        }
                    }
                }
                if (New_ele_lis.size() == 4) {
                    Double d = Double.valueOf(New_node_dic.get(New_ele_lis.get(0)[0]).getZ())
                            + Double.valueOf(New_node_dic.get(New_ele_lis.get(1)[0]).getZ())
                            - Double.valueOf(New_node_dic.get(New_ele_lis.get(2)[0]).getZ())
                            - Double.valueOf(New_node_dic.get(New_ele_lis.get(3)[0]).getZ());
                    int bb = bbIndex.incrementAndGet();

                    if (Math.abs(d) < Math.pow(10, -3)) {
                        Cohesive_horizontal.add(String.valueOf(bb));
                        String[] array = {
                                New_ele_lis.get(0)[0],
                                New_ele_lis.get(1)[0],
                                New_ele_lis.get(2)[0],
                                New_ele_lis.get(3)[0],
                                New_ele_lis.get(0)[1],
                                New_ele_lis.get(1)[1],
                                New_ele_lis.get(2)[1],
                                New_ele_lis.get(3)[1]
                        };
                        Cohesive_dic.put(String.valueOf(bb), array);
                    } else {
                        String[] array = {
                                New_ele_lis.get(0)[0],
                                New_ele_lis.get(1)[0],
                                New_ele_lis.get(3)[0],
                                New_ele_lis.get(2)[0],
                                New_ele_lis.get(0)[1],
                                New_ele_lis.get(1)[1],
                                New_ele_lis.get(3)[1],
                                New_ele_lis.get(2)[1]
                        };
                        Cohesive_dic.put(String.valueOf(bb), array);
                    }
                }
            });
        });
        processTime = System.currentTimeMillis();
        System.out.println(String.format("水平单元节点处理完成，耗时:%s，Cohesive_dic总容量:%s", (processTime - startTime),Cohesive_dic.size()));
        startTime = processTime;

//        for (int i = 0; i < keyList.size(); i++) {
//            String iKey = keyList.get(i);
//            for (int j = i+1; j < keyList.size(); j++) {
//                String jKey = keyList.get(j);
//                List<String[]> New_ele_lis = new ArrayList<>();
//                for (String m : eleFractureMap.get(iKey).getN()) {
//                    for (String n : eleFractureMap.get(jKey).getN()) {
//                        if (yu(m, max_node).equals(yu(n, max_node))) {
//                            String[] item = {m, n};
//                            New_ele_lis.add(item);
//                        }
//                    }
//                }
//
//                if (New_ele_lis.size() == 4) {
//                    Double d = Double.valueOf(New_node_dic.get(New_ele_lis.get(0)[0]).getZ())
//                            + Double.valueOf(New_node_dic.get(New_ele_lis.get(1)[0]).getZ())
//                            - Double.valueOf(New_node_dic.get(New_ele_lis.get(2)[0]).getZ())
//                            - Double.valueOf(New_node_dic.get(New_ele_lis.get(3)[0]).getZ());
//                    int bb = eleMap.size()+Cohesive_dic.size()+1;
//
//                    if (Math.abs(d) < Math.pow(10, -3)) {
//                        Cohesive_horizontal.add(String.valueOf(bb));
//                        String[] array = {
//                                New_ele_lis.get(0)[0],
//                                New_ele_lis.get(1)[0],
//                                New_ele_lis.get(2)[0],
//                                New_ele_lis.get(3)[0],
//                                New_ele_lis.get(0)[1],
//                                New_ele_lis.get(1)[1],
//                                New_ele_lis.get(2)[1],
//                                New_ele_lis.get(3)[1]
//                        };
//                        Cohesive_dic.put(String.valueOf(bb), array);
//                    } else {
//                        String[] array = {
//                                New_ele_lis.get(0)[0],
//                                New_ele_lis.get(1)[0],
//                                New_ele_lis.get(3)[0],
//                                New_ele_lis.get(2)[0],
//                                New_ele_lis.get(0)[1],
//                                New_ele_lis.get(1)[1],
//                                New_ele_lis.get(3)[1],
//                                New_ele_lis.get(2)[1]
//                        };
//                        Cohesive_dic.put(String.valueOf(bb), array);
//                    }
//                }
//            }
//        }
        //开始写入新文件
        System.out.println("处理完毕，开始写入新文件，路径为:" + OUTPUT_PATH);
        try (
                FileOutputStream fos = new FileOutputStream(OUTPUT_PATH);
                OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
        ) {
            boolean writeFromNod = false;
            boolean writeFromEle = false;
            for (String currentLine : oriFileList) {

                if (writeFromEle) {

                } else if (writeFromNod) {
                    //osw.append(currentLine);
                    //osw.append("\n");
                } else {
                    osw.append(currentLine);
                    osw.append("\n");
                }

                if (currentLine.contains(ELE_DIC_START)) {
                    writeFromEle = true;
                    writeFromNod = false;

                    List<Ele> outputList = new ArrayList<>();

                    eleUnFractureMap.keySet().stream().forEach(eid -> {
                        outputList.add(eleUnFractureMap.get(eid));
//                        this.write(osw, eleUnFractureMap.get(eid).toString());
                    });
                    eleFractureMap.keySet().stream().forEach(eid -> {
                        outputList.add(eleFractureMap.get(eid));
//                        this.write(osw, eleFractureMap.get(eid).toString());
                    });
                    Cohesive_dic.keySet().stream().forEach(eid -> {
                        if (!Cohesive_horizontal.contains(eid)) {
                            Ele ele = new Ele(eid, part1, Cohesive_dic.get(eid));
                            outputList.add(ele);
//                            this.write(osw, ele.toString());
                        }
                    });
                    Cohesive_horizontal.stream().forEach(eid -> {
                        Ele ele = new Ele(eid, part2, Cohesive_dic.get(eid));
                        outputList.add(ele);
//                        this.write(osw, ele.toString());
                    });

                    outputList.stream().sorted((e1,e2)->{
                        return Integer.valueOf(e1.getEid()).compareTo(Integer.valueOf(e2.getEid()));
                    }).forEach(ele->{
                        this.write(osw,ele.toString());
                    });

                } else if (currentLine.contains(ELE_DIC_END)) {
                    writeFromEle = false;
                    writeFromNod = false;
                    osw.append(currentLine);
                    osw.append("\n");
                } else if (currentLine.contains(NOD_DIC_START)) {
                    writeFromNod = true;
                    writeFromEle = false;
                    New_node_dic.keySet().stream().forEach(nid -> {
                        this.write(osw, New_node_dic.get(nid).toString(nid));
                    });
                } else if (currentLine.contains(NOD_DIC_END)) {
                    writeFromNod = false;
                    writeFromEle = false;
                    osw.append(currentLine);
                    osw.append("\n");
                }
            }

            osw.flush();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    private void write(OutputStreamWriter osw, String line) {
        try {
            osw.append(line);
            osw.write("\n");
        } catch (Exception e) {

        }
    }

    private String yu(String x, int max_node) {
        if (x.length() <= String.valueOf(max_node).length()) {
            return x;
        } else {
            return Integer.valueOf(x.substring(x.length()-String.valueOf(max_node).length())).toString();
        }
    }

    /**
     * 逐行读取文件，并保存为list
     *
     * @param file
     * @return
     */
    private static List<String> getFile(File file) {
        System.out.println("开始读取文件:" + file.getName());
        long startTime = System.currentTimeMillis();
        List<String> result = new ArrayList<>();
        try (InputStream is = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8")); // 实例化输入流，并获取网页代码
        ) {
            String s;
            while ((s = reader.readLine()) != null) {
                result.add(s);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        long endTime = System.currentTimeMillis();
        System.out.println("文件处理完成，耗时：" + (endTime - startTime));
        return result;
    }

    private static String getFileNameByPath(String path){
        return path.substring(path.lastIndexOf("\\")+1);
    }


    public static void main(String[] args) throws Exception{
        //参数准备
        String inputPath = System.getProperty("input");
        String outputPath = System.getProperty("output");
        String threshold = System.getProperty("threshold");
        String part1 = System.getProperty("part1");
        String part2 = System.getProperty("part2");


        //参数输入
        while(true){
            Scanner scanner = new Scanner(System.in);
            if(StringUtils.isEmpty(inputPath)){
                System.out.println("请输入文件路径:");
                inputPath = scanner.nextLine();
                System.out.println("尝试查找config文件....");
                String fileName = getFileNameByPath(inputPath);
                File file = new File(inputPath.replace(fileName,"config.dat"));
                if(file.exists()){
                    List<String> configList = getFile(file);
                   for(String s:configList){
                        if(s.contains("output")){
                            outputPath = s.split("=")[1];
                        }
                        if(s.contains("threshold")){
                            threshold = s.split("=")[1];
                        }
                        if(s.contains("part1")){
                            part1= s.split("=")[1];
                        }
                        if(s.contains("part2")){
                            part2= s.split("=")[1];
                        }
                    }
                }
                continue;
            }
            if(StringUtils.isEmpty(threshold)){
                System.out.println("请输入阈值(逗号分隔):例如: 0,3,0,3");
                threshold = scanner.nextLine();
                continue;
            }
            if(StringUtils.isEmpty(part1)){
                System.out.println("请输入Part1");
                part1 = scanner.nextLine();
                continue;
            }
            if(StringUtils.isEmpty(part2)){
                System.out.println("请输入Part2");
                part2 = scanner.nextLine();
                continue;
            }
            if(StringUtils.isEmpty(outputPath)){
                System.out.println("请输入输出路径（默认为输入路径）");
                outputPath = scanner.nextLine();
                if(StringUtils.isEmpty(outputPath)){
                    outputPath = inputPath.replace(".","-result.");
                }
                continue;
            }
            break;
        }

        //检查文件是否存在
        File file = new File(inputPath);
        List<String> oriFileList = getFile(file);

        //处理开始
        PreProcessing preProcessing = new PreProcessing(threshold,inputPath,outputPath,part1,part2);
        preProcessing.process(oriFileList);

        System.out.println("处理完成");
    }
}
