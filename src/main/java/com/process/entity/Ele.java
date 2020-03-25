package com.process.entity;


import com.process.utils.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wangheyu
 * @description: TODO
 * @date 2020/3/21
 */
@XmlAccessorType(value= XmlAccessType.PROPERTY)
public class Ele implements Serializable {

    private final static String SPLIT_KEY = " ";
    private final static String FORMAT="%8s%8s%8s%8s%8s%8s%8s%8s%8s%8s";

    public Ele(String line) {
        List<String> array = Arrays.stream(line.trim().split(SPLIT_KEY)).filter(s -> {
            return StringUtils.isNotBlank(s);}).collect(Collectors.toList());
        eid = array.get(0);
        pid = array.get(1);
        for (int i = 2; i < array.size(); i++) {
            n[i - 2] = array.get(i);
        }
    }

    public Ele(String eid,String pid,String[]n){
        this.eid=eid;
        this.pid=pid;
        this.n = n;
    }

    public Ele(){

    }

    private String eid;
    private String pid;
    private String[] n = new String[8];


    public String getEid() {
        return eid;
    }

    public String getPid() {
        return pid;
    }

    public String[] getN() {
        return n;
    }

    public void setEid(String eid) {
        this.eid = eid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public void setN(String[] n) {
        this.n = n;
    }

    @Override
    public String toString() {
        return String.format(FORMAT,eid,pid,n[0],n[1],n[2],n[3],n[4],n[5],n[6],n[7]);
    }
}
