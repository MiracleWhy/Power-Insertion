import org.apache.commons.lang.time.DateUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author wangheyu
 * @description: TODO
 * @date 2020/3/21
 */
public class PreProcessing {
    private final static double DELTA_LOWER_X = 181, DELTA_UPPER_X = 231, DELTA_LOWER_Y = -20.0, DELTA_UPPER_Y = 20.0;

    private final static String NOD_DIC_START = "nid";
    private final static String NOD_DIC_END = "*NODE";
    private final static String ELE_DIC_START = "eid";
    private final static String ELE_DIC_END = "*END";

    private final static String FILE_PATH = "C:\\Users\\wangheyu\\Desktop\\jyy";
    private final static String FILE_NAME = "3";

    private final static String SPLIT_KEY = "      ";

    private final static String part1 = "3";
    private final static String part2 = "4";


    private void process(List<String> oriFileList) {

        long startTime = System.currentTimeMillis();
        long processTime = startTime;
        System.out.println("处理开始:文件行数:" + oriFileList.size());
        Map<String, Ele> eleMap = new LinkedHashMap<>();
        Map<String, Nod> nodMap = new LinkedHashMap<>();


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
        System.out.println(String.format("Ele,Nod字典建立完毕,耗时:%s,Ele size:%s,Nod size:%s", (processTime - startTime), eleMap.size(), nodMap.size()));
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
        nodeFractureMap = nodeFractureMapTemp; //MapUtil.getDifferenceSetByGuava(nodeFractureMapTemp,nodeEdgeMap);
        nodeUnFractureMap = nodeUnFractureMapTemp; //MapUtil.getDifferenceSetByGuava(nodeUnFractureMapTemp,nodeEdgeMap);

        processTime = System.currentTimeMillis();
        System.out.println(String.format("开裂单元，非开裂单元 处理完毕,耗时:%s,开裂单元:%s,非开裂单元:%s,开裂节点:%s,非开裂节点:%s,边界节点:%s", (processTime - startTime), eleFractureMap.size(), eleUnFractureMap.size(), nodeFractureMapTemp.size(), nodeUnFractureMapTemp.size(), nodeEdgeMap.size()));
        startTime = processTime;


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
        Map<String, Nod> New_node_dic = new LinkedHashMap<>();
        List<String> nodeUnFractureKeyList = nodeUnFractureMap.keySet().stream().collect(Collectors.toList());
        List<String> nodeFractureKeyList = nodeFractureMap.keySet().stream().collect(Collectors.toList());

        AtomicInteger count = new AtomicInteger(0);
        nodMap.keySet().parallelStream().forEach(nid -> {
            System.out.println(String.format("节点分裂处理中，当前进度%s/%s",count.incrementAndGet(),nodMap.size()));
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

        processTime = System.currentTimeMillis();
        System.out.println(String.format("节点分裂完成，耗时:%s，新节点容量:%s", (processTime - startTime), New_node_dic.size()));
        startTime = processTime;


        //开裂区单元节点号更新
        LinkedHashMap<String,Boolean> New_node_assign = new LinkedHashMap<>();
        New_node_dic.keySet().stream().sorted((e1,e2)->{return e1.compareTo(e2);}).forEach(key->{
            New_node_assign.put(key,false);
        });
        nodeEdgeMap.keySet().stream().forEach(key->{
            New_node_assign.put(key,true);
        });
        AtomicInteger count2 = new AtomicInteger(0);
        eleFractureMap.keySet().parallelStream().sorted((e1,e2)->{return Integer.valueOf(e1).compareTo(Integer.valueOf(e2));}).forEach(eid -> {
            System.out.println(String.format("开裂区单元节点号更新处理中，当前进度%s/%s",count2.incrementAndGet(),eleFractureMap.size()));
            Ele ele = eleFractureMap.get(eid);
            for (int i = 0; i < ele.getN().length; i++) {
                Iterator<Map.Entry<String, Boolean>> iteratorNewNodeAssign = New_node_assign.entrySet().iterator();
                while (iteratorNewNodeAssign.hasNext()) {
                    Map.Entry<String, Boolean> entry = iteratorNewNodeAssign.next();
                    String k = entry.getKey();
                    if (entry.getValue() == false && yu(k, max_node).equals(ele.getN()[i])) {
                        ele.getN()[i] = k;
                        New_node_assign.put(k,true);
                    }
                }
            }
        });

        processTime = System.currentTimeMillis();
        System.out.println(String.format("开裂区单元节点号更新完成，耗时:%s", (processTime - startTime)));
        startTime = processTime;

        //开始生成cohesive单元
        Integer b = eleMap.size();
        Map<String, String[]> Cohesive_dic = new LinkedHashMap<>();
        List<String> Cohesive_horizontal = new ArrayList<>();
        AtomicInteger count1 = new AtomicInteger(0);
        eleFractureMap.keySet().parallelStream().forEach(ef -> {
            System.out.println(String.format("生成cohesive单元处理中，当前进度%s/%s",count1.incrementAndGet(),eleFractureMap.size()));
            eleUnFractureMap.keySet().parallelStream().forEach(euf -> {
                String[] efnArray = eleFractureMap.get(ef).getN();
                List<String[]> New_ele_lis = new ArrayList<>();
                Arrays.stream(efnArray).forEach(n ->
                {
                    if(Arrays.asList(eleUnFractureMap.get(euf).getN()).contains(yu(n,max_node))){
                            String[] item = {n, yu(n, max_node)};
                            New_ele_lis.add(item);
                    }
//                    if (Arrays.stream(eleUnFractureMap.get(euf).getN()).anyMatch(nn -> nn == yu(n, max_node))) {
//                        String[] item = {n, yu(n, max_node)};
//                        New_ele_lis.add(item);
//                    }
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
                    Cohesive_dic.put(String.valueOf(eleMap.size() + Cohesive_dic.size()+1), array);
                }
            });
        });

        processTime = System.currentTimeMillis();
        System.out.println(String.format("cohesive单元节点更新完成，耗时:%s,容量:%s", (processTime - startTime),Cohesive_dic.size()));
        startTime = processTime;

        //
        List<String> keyList = eleFractureMap.keySet().stream().collect(Collectors.toList());
        for (int i = 0; i < keyList.size(); i++) {
            String iKey = keyList.get(i);
            for (int j = i+1; j < keyList.size(); j++) {
                String jKey = keyList.get(j);
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
                    int bb = eleMap.size()+Cohesive_dic.size()+1;

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
            }
        }
        //开始写入新文件
        try (
                FileOutputStream fos = new FileOutputStream(FILE_PATH + "\\output\\" + FILE_NAME + System.currentTimeMillis() + ".k");
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


    public static void main(String[] args) {

        String path = FILE_PATH + "\\input\\" + FILE_NAME + ".k";
        File file = new File(path);

        List<String> oriFileList = getFile(file);

        PreProcessing preProcessing = new PreProcessing();
        preProcessing.process(oriFileList);


    }
}
