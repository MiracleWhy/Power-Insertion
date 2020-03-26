package com.process;

import com.process.entity.Ele;
import com.process.entity.Nod;
import com.process.utils.IntegerUtil;
import com.process.utils.JavaSerializable;
import com.process.utils.MapUtil;
import com.process.utils.StringUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.process.consts.Constants.*;

/**
 * @author wangheyu
 * @description: TODO
 * @date 2020/3/21
 */
public class PreProcessing {
    private double deltaLowerX = 181, deltaUpperX = 231, deltaLowerY = -20.0, deltaUpperY = 20.0;

    private String filePath = "";
    private String outputPath = "";

    private Integer part1 = 3;
    private Integer part2 = 4;

    private long startTime;
    private long processTime;

    //局部变量定义
    Map<Integer, Ele> eleDic = new HashMap<>();
    Map<Integer, Nod> nodDic = new HashMap<>();
    //开裂，非开裂单元处理
    Map<Integer, Ele> eleFractureDic = new HashMap<>();
    Map<Integer, Ele> eleUnFractureDic = new HashMap<>();
    //开裂，非开裂节点处理
    Map<Integer, Nod> nodFractureDic = new HashMap<>();
    Map<Integer, Nod> nodUnFractureDic = new HashMap<>();
    Map<Integer, Nod> nodEdgeDic = new HashMap<>();
    //新节点字典
    Map<Integer, Nod> newNodeDic = new LinkedHashMap<>();
    //Cohesive单元
    Map<Integer, Integer[]> cohesiveDic = new ConcurrentHashMap<>();
    List<Integer> cohesiveHorizontal = new Vector<>();

    //
    int nodPos;
    int nodPosY;

    PreProcessing(double x1, double x2, double y1, double y2, String outputPath, Integer part1, Integer part2) {
        this.deltaLowerX = x1;
        this.deltaUpperX = x2;
        this.deltaLowerY = y1;
        this.deltaUpperY = y2;
        this.outputPath = outputPath;
        this.part1 = part1;
        this.part2 = part2;
    }

    PreProcessing(String threshold, String inputPath, String outputPath, Integer part1, Integer part2) throws Exception {
        String[] thresholdArray = threshold.split(",");
        if (thresholdArray.length == 4) {
            this.deltaLowerX = Double.valueOf(thresholdArray[0]);
            this.deltaUpperX = Double.valueOf(thresholdArray[1]);
            this.deltaLowerY = Double.valueOf(thresholdArray[2]);
            this.deltaUpperY = Double.valueOf(thresholdArray[3]);
        } else {
            throw new Exception("阈值参数错误");
        }
        this.filePath = inputPath;
        this.outputPath = outputPath;
        this.part1 = part1;
        this.part2 = part2;
    }

    PreProcessing() {

    }

    /**
     * 从文件中区分出单元与节点
     *
     * @param fileList
     */
    private void buildDicFromFile(List<String> fileList) {
        System.out.println("开始建立字典");

        Iterator<String> iterator = fileList.iterator();
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
                eleDic.put(ele.getEid(), ele);
            }
            if (setToNod) {
                Nod nod = new Nod(currentLine);
                nodDic.put(nod.getNid(), nod);
            }
        }
        processTime = System.currentTimeMillis();
        System.out.println(String.format("com.process.entity.Ele,Nod字典建立完毕,耗时:%s,com.process.entity.Ele size:%s,com.process.entity.Nod size:%s", (processTime - startTime), eleDic.size(), nodDic.size()));
        startTime = processTime;
    }

    /**
     * 遍历单元，区分出开裂单元与非开裂单元
     */
    private void handleFracture() {
        //遍历eleDic，区分开裂与非开裂单元
        eleDic.keySet().stream().forEach(eid -> {
            Ele ele = eleDic.get(eid);
            boolean isFracture = true;
            for (Integer n : ele.getN()) {
                Nod nod = nodDic.get(n);
                Double x = nod.getX();
                Double y = nod.getY();
                if (x >= deltaLowerX && x <= deltaUpperX
                        && y >= deltaLowerY && y <= deltaUpperY) {
                    //do nothing
                } else {
                    isFracture = false;
                    break;
                }
            }
            if (isFracture) {
                //分配至开裂单元字典
                eleFractureDic.put(ele.getEid(), ele);
                //对应节点追加至开裂节点字典
                Arrays.stream(ele.getN()).forEach(nid -> {
                    nodFractureDic.putIfAbsent(nid, nodDic.get(nid));
                });
            } else {
                //分配至非开裂单元字典
                eleUnFractureDic.put(ele.getEid(), ele);
                //对应节点追加至非开裂节点字典
                Arrays.stream(ele.getN()).forEach(nid -> {
                    nodUnFractureDic.putIfAbsent(nid, nodDic.get(nid));
                });
            }
        });

    }

    /**
     * 处理节点分裂
     */
    private void handleNodFission() {

        Map<Integer, Integer> nodeAppearTimes = new LinkedHashMap<>();
        //遍历分界区节点+开裂区单元，出现次数+1
        nodEdgeDic.keySet().stream().forEach(item -> {
            nodeAppearTimes.put(item, 1);
        });
        //遍历开裂区单元,出现次数+1
        eleFractureDic.keySet().stream().forEach(item -> {
            Ele ele = eleFractureDic.get(item);
            Arrays.stream(ele.getN()).forEach(n -> {
                if (nodeAppearTimes.containsKey(n)) {
                    nodeAppearTimes.put(n, nodeAppearTimes.get(n) + 1);
                } else {
                    nodeAppearTimes.put(n, 1);
                }
            });
        });

        processTime = System.currentTimeMillis();
        System.out.println(String.format("节点出现次数统计完毕，耗时:%s", (processTime - startTime)));
        startTime = processTime;


        //建立新节点编号字典，实现节点分裂
        List<Integer> nodeUnFractureKeyList = nodUnFractureDic.keySet().stream().collect(Collectors.toList());
        List<Integer> nodeFractureKeyList = nodFractureDic.keySet().stream().collect(Collectors.toList());
        final Integer nodPosBase = Double.valueOf(Math.pow(10, nodPos)).intValue();
        AtomicInteger count = new AtomicInteger(0);
        nodDic.keySet().stream().forEach(nid -> {
            System.out.println(String.format("节点分裂处理中，当前进度%s/%s", count.incrementAndGet(), nodDic.size()));
            //若节点在非开裂区内，则编号不变
            if (nodeUnFractureKeyList.contains(nid)) {
                newNodeDic.put(nid, nodDic.get(nid));
            }
            //若节点在开裂区内，复制重复次数
            if (nodeFractureKeyList.contains(nid)) {
                for (int i = 0; i < nodeAppearTimes.get(nid); i++) {
                    Integer key = i * nodPosBase + nid;
                    newNodeDic.put(key, nodDic.get(nid));
                }
            }
        });
        processTime = System.currentTimeMillis();
        System.out.println(String.format("节点分裂完成，耗时:%s，新节点容量:%s", (processTime - startTime), newNodeDic.size()));
        startTime = processTime;
    }

    /**
     * 更新开裂单元
     *
     */
    private void updateEleFractureDic() {

        //开裂区单元节点号更新
        Map<Integer, Boolean> nodDispatchResult = new ConcurrentHashMap<>();
        newNodeDic.keySet().stream().sorted((e1, e2) -> {
            return e1.compareTo(e2);
        }).forEach(key -> {
            nodDispatchResult.put(key, false);
        });
        nodEdgeDic.keySet().stream().forEach(key -> {
            nodDispatchResult.put(key, true);
        });
        AtomicInteger count2 = new AtomicInteger(0);
        //这里需要使用单线程确保顺序
        eleFractureDic.keySet().parallelStream().sorted((e1, e2) -> {
            return Integer.valueOf(e1).compareTo(Integer.valueOf(e2));
        }).forEach(eid -> {
            System.out.println(String.format("开裂区单元节点号更新处理中，当前进度%s/%s", count2.incrementAndGet(), eleFractureDic.size()));
            Ele ele = eleFractureDic.get(eid);
            for (int i = 0; i < ele.getN().length; i++) {
                Iterator<Integer> iteratorNewNodeAssign = nodDispatchResult.keySet().iterator();
                while (iteratorNewNodeAssign.hasNext()) {
                    Integer k = iteratorNewNodeAssign.next();
                    synchronized (nodDispatchResult) {
                        if (nodDispatchResult.get(k) == false && mod(k).equals(ele.getN()[i])) {
                            ele.getN()[i] = k;
                            nodDispatchResult.put(k, true);
                        }
                    }
                }
            }
        });

        processTime = System.currentTimeMillis();
        System.out.println(String.format("开裂区单元节点号更新完成，耗时:%s", (processTime - startTime)));
        startTime = processTime;
    }

    /**
     * 生成Cohesive单元
     */
    private void buildCohesive() {

        Integer b = eleDic.size();
        AtomicInteger count1 = new AtomicInteger(0);
        AtomicInteger bIndex = new AtomicInteger(eleDic.size());
        eleFractureDic.keySet().parallelStream().forEach(ef -> {
            System.out.println(String.format("生成cohesive单元处理中，当前进度%s/%s", count1.incrementAndGet(), eleFractureDic.size()));
            eleUnFractureDic.keySet().stream().forEach(euf -> {
                Integer[] efnArray = eleFractureDic.get(ef).getN();
                List<Integer[]> New_ele_lis = new ArrayList<>();
                Arrays.stream(efnArray).forEach(n ->
                {
                    if (Arrays.asList(eleUnFractureDic.get(euf).getN()).contains(mod(n))) {
                        Integer[] item = {n, mod(n)};
                        New_ele_lis.add(item);
                    }
                });

                if (New_ele_lis.size() == 4) {
                    Integer[] array = {
                            New_ele_lis.get(0)[0],
                            New_ele_lis.get(1)[0],
                            New_ele_lis.get(3)[0],
                            New_ele_lis.get(2)[0],
                            New_ele_lis.get(0)[1],
                            New_ele_lis.get(1)[1],
                            New_ele_lis.get(3)[1],
                            New_ele_lis.get(2)[1]
                    };
                    cohesiveDic.put(bIndex.incrementAndGet(), array);
                }
            });
        });

        processTime = System.currentTimeMillis();
        System.out.println(String.format("cohesive单元节点更新完成，耗时:%s,容量:%s", (processTime - startTime), cohesiveDic.size()));
        startTime = processTime;

    }


    private void buildCohesiveHorizal(Map<Integer, Nod> newNodeDic) {
        System.out.println("水平单元计算开始");
        List<Integer> keyList = eleFractureDic.keySet().stream().collect(Collectors.toList());
        Map<Integer, Boolean> compareTable = new ConcurrentHashMap<>();
        AtomicInteger count3 = new AtomicInteger(0);
        AtomicInteger bbIndex = new AtomicInteger(eleDic.size() + cohesiveDic.size());
        keyList.parallelStream().forEach(iKey -> {
            System.out.println(String.format("水平单元计算中:当前进度:%s/%s", count3.incrementAndGet(), keyList.size()));
            keyList.stream().forEach(jKey -> {
                //已经比较过，不再比较
                if (compareTable.containsKey(iKey + jKey) || compareTable.containsKey(jKey + iKey)||iKey.equals(jKey)) {
                    return;
                } else {
                    compareTable.put(iKey + jKey, true);
                }
                List<Integer[]> New_ele_lis = new ArrayList<>();
                for (Integer m : eleFractureDic.get(iKey).getN()) {
                    for (Integer n : eleFractureDic.get(jKey).getN()) {
                        if (mod(m).equals(mod(n))) {
                            Integer[] item = {m, n};
                            New_ele_lis.add(item);
                        }
                    }
                }
                if (New_ele_lis.size() == 4) {
                    Double d = Double.valueOf(newNodeDic.get(New_ele_lis.get(0)[0]).getZ())
                            + Double.valueOf(newNodeDic.get(New_ele_lis.get(1)[0]).getZ())
                            - Double.valueOf(newNodeDic.get(New_ele_lis.get(2)[0]).getZ())
                            - Double.valueOf(newNodeDic.get(New_ele_lis.get(3)[0]).getZ());
                    int bb = bbIndex.incrementAndGet();

                    if (Math.abs(d) < Math.pow(10, -3)) {
                        cohesiveHorizontal.add(bb);
                        Integer[] array = {
                                New_ele_lis.get(0)[0],
                                New_ele_lis.get(1)[0],
                                New_ele_lis.get(2)[0],
                                New_ele_lis.get(3)[0],
                                New_ele_lis.get(0)[1],
                                New_ele_lis.get(1)[1],
                                New_ele_lis.get(2)[1],
                                New_ele_lis.get(3)[1]
                        };
                        cohesiveDic.put(bb, array);
                    } else {
                        Integer[] array = {
                                New_ele_lis.get(0)[0],
                                New_ele_lis.get(1)[0],
                                New_ele_lis.get(3)[0],
                                New_ele_lis.get(2)[0],
                                New_ele_lis.get(0)[1],
                                New_ele_lis.get(1)[1],
                                New_ele_lis.get(3)[1],
                                New_ele_lis.get(2)[1]
                        };
                        cohesiveDic.put(bb, array);
                    }
                }
            });
        });
        processTime = System.currentTimeMillis();
        System.out.println(String.format("水平单元节点处理完成，耗时:%s，Cohesive_dic总容量:%s", (processTime - startTime), cohesiveDic.size()));
        startTime = processTime;
    }


    private void writeToFile(List<String> fileList){

        System.out.println("处理完毕，开始写入新文件，路径为:" + outputPath);
        try (
                FileOutputStream fos = new FileOutputStream(outputPath);
                OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
        ) {
            boolean writeFromNod = false;
            boolean writeFromEle = false;
            for (String currentLine : fileList) {

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

                    eleUnFractureDic.keySet().stream().forEach(eid -> {
                        outputList.add(eleUnFractureDic.get(eid));
//                        this.write(osw, eleUnFractureDic.get(eid).toString());
                    });
                    eleFractureDic.keySet().stream().forEach(eid -> {
                        outputList.add(eleFractureDic.get(eid));
//                        this.write(osw, eleFractureDic.get(eid).toString());
                    });
                    cohesiveDic.keySet().stream().forEach(eid -> {
                        if (!cohesiveHorizontal.contains(eid)) {
                            Ele ele = new Ele(eid, part1, cohesiveDic.get(eid));
                            outputList.add(ele);
//                            this.write(osw, ele.toString());
                        }
                    });
                    cohesiveHorizontal.stream().forEach(eid -> {
                        Ele ele = new Ele(eid, part2, cohesiveDic.get(eid));
                        outputList.add(ele);
//                        this.write(osw, ele.toString());
                    });

                    outputList.stream().sorted((e1, e2) -> {
                        return Integer.valueOf(e1.getEid()).compareTo(Integer.valueOf(e2.getEid()));
                    }).forEach(ele -> {
                        this.write(osw, ele.toString());
                    });

                } else if (currentLine.contains(ELE_DIC_END)) {
                    writeFromEle = false;
                    writeFromNod = false;
                    osw.append(currentLine);
                    osw.append("\n");
                } else if (currentLine.contains(NOD_DIC_START)) {
                    writeFromNod = true;
                    writeFromEle = false;
                    newNodeDic.keySet().stream().forEach(nid -> {
                        this.write(osw, newNodeDic.get(nid).toString(nid));
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


    private void process(List<String> oriFileList) {

        startTime = System.currentTimeMillis();
        processTime = startTime;
        System.out.println("处理开始:文件行数:" + oriFileList.size());

        //提取FileName，标识当前唯一处理
        final String fileName = getFileNameByPath(filePath);
        //中间结果保存方法初始化
        JavaSerializable javaSerializable = new JavaSerializable();

        //文件处理，区分出Ele字典及Nod字典

        this.buildDicFromFile(oriFileList);
        final int nodSize = nodDic.size();
        nodPos = IntegerUtil.sizeOfInt(nodSize);
        nodPosY = Double.valueOf(Math.pow(10, nodPos)).intValue();

        //遍历单元，区分出开裂单元与非开裂单元，以及节点
        this.handleFracture();
        //节点集合做交集，计算出边界节点
        nodEdgeDic = MapUtil.getIntersectionSetByGuava(nodFractureDic, nodUnFractureDic);

        processTime = System.currentTimeMillis();
        System.out.println(String.format("开裂单元，非开裂单元 处理完毕,耗时:%s,开裂单元:%s,非开裂单元:%s,开裂节点:%s,非开裂节点:%s,边界节点:%s", (processTime - startTime), eleFractureDic.size(), eleUnFractureDic.size(), nodFractureDic.size(), nodUnFractureDic.size(), nodEdgeDic.size()));
        startTime = processTime;


        Object eleFractureDicTemp = javaSerializable.load(this.filePath + ELE_FRACTURE_MAP_SUFFIX);
        Object newNodeDicTemp = javaSerializable.load(this.filePath + NEW_NODE_DIC_SUFFIX);
        if (eleFractureDicTemp != null && newNodeDicTemp != null) {
            System.out.println("读取到中间计算结果");
            ((HashMap<Integer, Ele>) eleFractureDicTemp).keySet().stream().forEach(eid -> {
                eleFractureDic.put(eid, ((HashMap<String, Ele>) eleFractureDicTemp).get(eid));
            });
            ((HashMap<Integer, Nod>) newNodeDicTemp).keySet().stream().forEach(nid -> {
                newNodeDic.put(nid, ((HashMap<String, Nod>) newNodeDicTemp).get(nid));
            });
        } else {

            this.handleNodFission();

            this.updateEleFractureDic();
            javaSerializable.store(eleFractureDic, this.filePath + ELE_FRACTURE_MAP_SUFFIX);
            javaSerializable.store(newNodeDic, this.filePath + NEW_NODE_DIC_SUFFIX);
        }

        //开始生成cohesive单元
        this.buildCohesive();
        //水平单元计算开始
        this.buildCohesiveHorizal(newNodeDic);
        //开始写入新文件
        this.writeToFile(oriFileList);
    }

    private void write(OutputStreamWriter osw, String line) {
        try {
            osw.append(line);
            osw.write("\n");
        } catch (Exception e) {

        }
    }

    private Integer mod(Integer x) {
        return x % nodPosY;
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

    private static String getFileNameByPath(String path) {
        return path.substring(path.lastIndexOf("\\") + 1);
    }


    public static void main(String[] args) throws Exception {
        //参数准备
        String inputPath = System.getProperty("input");
        String outputPath = System.getProperty("output");
        String threshold = System.getProperty("threshold");
        String part1 = System.getProperty("part1");
        String part2 = System.getProperty("part2");


        //参数输入
        while (true) {
            Scanner scanner = new Scanner(System.in);
            if (StringUtils.isEmpty(inputPath)) {
                System.out.println("请输入文件路径:");
                inputPath = scanner.nextLine();
                System.out.println("尝试查找config文件....");
                String fileName = getFileNameByPath(inputPath);
                File file = new File(inputPath.replace(fileName, fileName+ "_config.dat"));
                if (file.exists()) {
                    List<String> configList = getFile(file);
                    for (String s : configList) {
                        if (s.contains("output")) {
                            outputPath = s.split("=")[1];
                        }
                        if (s.contains("threshold")) {
                            threshold = s.split("=")[1];
                        }
                        if (s.contains("part1")) {
                            part1 = s.split("=")[1];
                        }
                        if (s.contains("part2")) {
                            part2 = s.split("=")[1];
                        }
                    }
                }
                continue;
            }
            if (StringUtils.isEmpty(threshold)) {
                System.out.println("请输入阈值(逗号分隔):例如: 0,3,0,3");
                threshold = scanner.nextLine();
                continue;
            }
            if (StringUtils.isEmpty(part1)) {
                System.out.println("请输入Part1");
                part1 = scanner.nextLine();
                continue;
            }
            if (StringUtils.isEmpty(part2)) {
                System.out.println("请输入Part2");
                part2 = scanner.nextLine();
                continue;
            }
            if (StringUtils.isEmpty(outputPath)) {
                System.out.println("请输入输出路径（默认为输入路径）");
                outputPath = scanner.nextLine();
                if (StringUtils.isEmpty(outputPath)) {
                    outputPath = inputPath.replace(".", "-result.");
                }
                continue;
            }
            break;
        }

        //检查文件是否存在
        File file = new File(inputPath);
        List<String> oriFileList = getFile(file);

        //处理开始
        PreProcessing preProcessing = new PreProcessing(threshold, inputPath, outputPath, Integer.valueOf(part1), Integer.valueOf(part2));
        preProcessing.process(oriFileList);

        System.out.println("处理完成");
    }
}
