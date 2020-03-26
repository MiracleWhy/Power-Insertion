package com.process.entity;

import com.process.utils.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wangheyu
 * @description: TODO
 * @date 2020/3/21
 */
@XmlAccessorType(value= XmlAccessType.PUBLIC_MEMBER)
public class Nod implements Serializable {
    private final static String SPLIT_KEY = " ";
    private final static String FORMAT="%8s%16s%16s%16s%8s%8s";


    public Nod(String line) {
        List<String> array = Arrays.stream(line.trim().split(SPLIT_KEY)).filter(s -> {
            return StringUtils.isNotBlank(s);
        }).collect(Collectors.toList());
        try {
            nid = Integer.valueOf(array.get(0));
            x = Double.valueOf(array.get(1));
            y = Double.valueOf(array.get(2));
            z = Double.valueOf(array.get(3));
            tc = Integer.valueOf(array.get(4));
            rc = Integer.valueOf(array.get(5));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Nod(){

    }


    private Integer nid;
    private Double x;
    private Double y;
    private Double z;
    private Integer tc;
    private Integer rc;

    public Integer getNid() {
        return nid;
    }

    public void setNid(Integer nid) {
        this.nid = nid;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getZ() {
        return z;
    }

    public void setZ(Double z) {
        this.z = z;
    }

    public Integer getTc() {
        return tc;
    }

    public void setTc(Integer tc) {
        this.tc = tc;
    }

    public Integer getRc() {
        return rc;
    }

    public void setRc(Integer rc) {
        this.rc = rc;
    }

    @Override
    @XmlTransient
    public String toString() {
        return String.format(FORMAT,nid,x,y,z,tc,rc);
    }

    @XmlTransient
    public String toString(Integer nid){
        return String.format(FORMAT,nid,x,y,z,tc,rc);
    }
}
