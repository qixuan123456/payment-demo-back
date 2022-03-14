package com.qx.paymentdemo.controller;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.qx.paymentdemo.service.WxPayService;
import com.qx.paymentdemo.util.HttpUtils;
import com.qx.paymentdemo.util.WechatPay2ValidatorForRequest;
import com.qx.paymentdemo.vo.R;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Xuan
 * Date: 2022/2/23
 * Time: 17:01
 */
@CrossOrigin
@RestController
@RequestMapping("/api/wx-pay")
@Api(tags = "网站微信支付API")
@Slf4j
public class WxPayController {

    @Resource
    private WxPayService wxPayService;

    @Resource
    private Verifier verifier;

    @ApiOperation("调用统一下单API,生成支付二维码")
    @PostMapping("/native/{productId}")
    public R nativePay(@PathVariable Long productId) throws IOException {

        log.info("发起支付请求");
        Map<String, Object> map = wxPayService.nativePay(productId);
        return R.ok().setData(map);
    }

    @PostMapping("/native/notify")
    public String nativeNotify(HttpServletRequest request, HttpServletResponse response) {

        Gson gson = new Gson();
        Map<String, String> map = new HashMap<>();
        System.out.println("-----------------------");

        try {
            String body = HttpUtils.readData(request);
            Map<String, Object> bodyMap = gson.fromJson(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            log.info("支付通知的id =====>{}", requestId);
            log.info("支付通知的完整数据 =====>{}", body);

            //验证签名
            WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest = new WechatPay2ValidatorForRequest(verifier, requestId, body);
            if (!wechatPay2ValidatorForRequest.validate(request)) {
                log.error("验证签名失败");
                response.setStatus(201);
                map.put("code", "ERROR");
                map.put("message", "验证签名失败");
                return gson.toJson(map);
            }
            log.info("验证签名成功");

            //处理订单
            wxPayService.processOrder(bodyMap);
            //模拟延时
//            try {
//                TimeUnit.SECONDS.sleep(5);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

            response.setStatus(200);
            map.put("code", "SUCCESS");
            map.put("message", "成功");
            return gson.toJson(map);
        } catch (JsonSyntaxException | IOException | GeneralSecurityException e) {
            e.printStackTrace();
            response.setStatus(201);
            map.put("code", "ERROR");
            map.put("message", "失败");
            return gson.toJson(map);
        }
    }

    @PostMapping("/cancel/{orderNo}")
    public R cancel(@PathVariable String orderNo) throws IOException {

        log.info("取消订单：" + orderNo);

        wxPayService.cancelOrder(orderNo);
        return R.ok().setMessage("订单已取消");
    }

    @GetMapping("/query/{orderNo}")
    public R queryOrder(@PathVariable String orderNo) throws Exception {

        log.info("查询订单");

        String result = wxPayService.queryOrder(orderNo);

        return R.ok().setMessage("查询成功").data("result", result);
    }

    @ApiOperation("申请退款")
    @PostMapping("/refunds/{orderNo}/{reason}")
    public R refunds(@PathVariable String orderNo, @PathVariable String reason) throws IOException {
        log.info("申请退款,订单号 ====> {},原因 ====>{}", orderNo, reason);

        wxPayService.refund(orderNo,reason);
        return R.ok();
    }

    @ApiOperation("查询退款，测试用")
    @GetMapping("/query-refund/{refundNo}")
    public R queryRefund(@PathVariable String refundNo) throws IOException {

        log.info("查询退款");

        String result = wxPayService.queryRefund(refundNo);

        return R.ok().setMessage("查询成功").data("result",result);
    }

    @PostMapping("/refunds/notify")
    public String refundsNotify(HttpServletRequest request,HttpServletResponse response) throws Exception{

        log.info("退款通知执行");

        Gson gson = new Gson();
        Map<String, String> map = new HashMap<>();

        try {
            //处理通知参数
            String body = HttpUtils.readData(request);
            Map<String, Object> bodyMap = gson.fromJson(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            log.info("支付通知的id =====>{}", requestId);
            log.info("支付通知的完整数据 =====>{}", body);

            //验证签名
            WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest = new WechatPay2ValidatorForRequest(verifier, requestId, body);
            if (!wechatPay2ValidatorForRequest.validate(request)) {
                log.error("验证签名失败");
                response.setStatus(201);
                map.put("code", "ERROR");
                map.put("message", "验证签名失败");
                return gson.toJson(map);
            }
            log.info("验证签名成功");

            //处理退款单
            wxPayService.processRefund(bodyMap);

            response.setStatus(200);
            map.put("code", "SUCCESS");
            map.put("message", "成功");
            return gson.toJson(map);
        } catch (JsonSyntaxException | IOException | GeneralSecurityException e) {
            e.printStackTrace();
            response.setStatus(201);
            map.put("code", "ERROR");
            map.put("message", "失败");
            return gson.toJson(map);
        }
    }

    @ApiOperation("获取下载账单url地址")
    @GetMapping("querybill/{billDate}/{type}")
    public R queryTradeBill(@PathVariable String billDate,@PathVariable String type) throws Exception {

        log.info("获取占单url");

        String downloadUrl = wxPayService.queryBill(billDate,type);

        return R.ok().setMessage("获取账单url成功").data("downloadUrl",downloadUrl);
    }

    @ApiOperation("下载账单")
    @GetMapping("downloadbill/{billDate}/{type}")
    public R downloadBill(@PathVariable String billDate,@PathVariable String type) throws Exception {
        log.info("下载账单");
        String result = wxPayService.downloadBill(billDate,type);
        return R.ok().data("result",result);
    }
}

