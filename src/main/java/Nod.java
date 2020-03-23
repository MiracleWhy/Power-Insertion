import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wangheyu
 * @description: TODO
 * @date 2020/3/21
 */
public class Nod {
    private final static String SPLIT_KEY = " ";
    private final static String FORMAT="%8s%16s%16s%16s%8s%8s";


    Nod(String line) {
        List<String> array = Arrays.stream(line.trim().split(SPLIT_KEY)).filter(s -> {
            return StringUtils.isNotBlank(s);}).collect(Collectors.toList());
        try{
        nid = array.get(0);
        x = array.get(1);
        y = array.get(2);
        z = array.get(3);
        tc = array.get(4);
        rc = array.get(5);}
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }


    private String nid;
    private String x;
    private String y;
    private String z;
    private String tc;
    private String rc;

    public String getNid() {
        return nid;
    }

    public String getX() {
        return x;
    }

    public String getY() {
        return y;
    }

    public String getZ() {
        return z;
    }

    public String getTc() {
        return tc;
    }

    public String getRc() {
        return rc;
    }

    @Override
    public String toString() {
        return String.format(FORMAT,nid,x,y,z,tc,rc);
    }

    public String toString(String nid){
        return String.format(FORMAT,nid,x,y,z,tc,rc);
    }
}
