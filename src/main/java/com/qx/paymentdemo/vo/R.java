package com.qx.paymentdemo.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Xuan
 * Date: 2022/2/21
 * Time: 19:16
 */
@Data
@Accessors(chain = true)
public class R {
    private Integer code;
    private String message;
    private Map<String, Object> data = new HashMap<>();

    public static R ok(){
        R r = new R();
        r.setCode(200);
        r.setMessage("成功");
        return r;
    }

    public static R error(){
        R r = new R();
        r.setCode(404);
        r.setMessage("失败");
        return r;
    }

    public R data(String key, Object value){
        this.data.put(key,value);
        return this;
    }
}
