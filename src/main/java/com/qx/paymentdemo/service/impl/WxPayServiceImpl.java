package com.qx.paymentdemo.service.impl;

import com.google.gson.Gson;
import com.qx.paymentdemo.config.WxPayConfig;
import com.qx.paymentdemo.entity.OrderInfo;
import com.qx.paymentdemo.entity.RefundInfo;
import com.qx.paymentdemo.enums.OrderStatus;
import com.qx.paymentdemo.enums.wxpay.WxApiType;
import com.qx.paymentdemo.enums.wxpay.WxNotifyType;
import com.qx.paymentdemo.enums.wxpay.WxTradeState;
import com.qx.paymentdemo.service.OrderInfoService;
import com.qx.paymentdemo.service.PaymentInfoService;
import com.qx.paymentdemo.service.RefundInfoService;
import com.qx.paymentdemo.service.WxPayService;

import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Xuan
 * Date: 2022/2/23
 * Time: 17:03
 */
@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Resource
    private WxPayConfig wxPayConfig;

    @Resource
    private CloseableHttpClient wxPayClient;

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private PaymentInfoService paymentInfoService;

    @Resource
    private RefundInfoService refundInfoService;

    @Resource
    private CloseableHttpClient wxPayNoSignClient;

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 创建订单，调用Native支付接口
     *
     * @param productId 产品号
     * @return code_url 和订单号
     * @throws IOException
     */
    @Override
    public Map<String, Object> nativePay(Long productId) throws IOException {

        log.info("生成订单");


        //生成订单
        OrderInfo orderInfo = orderInfoService.createOrderByProductId(productId);

        //调用统一下单API
        HttpPost httpPost = new HttpPost(wxPayConfig.getDomain().concat(WxApiType.NATIVE_PAY.getType()));
        Gson gson = new Gson();
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("appid", wxPayConfig.getAppid());
        paramsMap.put("mchid", wxPayConfig.getMchId());
        paramsMap.put("description", orderInfo.getTitle());
        paramsMap.put("out_trade_no", orderInfo.getOrderNo());
        paramsMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyType.NATIVE_NOTIFY.getType()));

        Map<String, Object> amountMap = new HashMap<>();
        amountMap.put("total", orderInfo.getTotalFee());
        amountMap.put("currency", "CNY");

        paramsMap.put("amount", amountMap);
        String jsonParams = gson.toJson(paramsMap);
        log.info("请求参数," + jsonParams);

        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        //完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpPost);

        try {
            String bodyAsString = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) { //处理成功
                log.info("成功,返回结果 = " + bodyAsString);
            } else if (statusCode == 204) { //处理成功，无返回Body
                log.info("成功");
            } else {
                log.info("Native下单失败, 响应吗 = " + statusCode + ",返回结果 = " + bodyAsString);
                throw new IOException("request failed");
            }
            //响应结果
            Map<String, String> resultMap = gson.fromJson(bodyAsString, HashMap.class);
            //二维码
            String codeUrl = resultMap.get("code_url");

            Map<String, Object> map = new HashMap<>();
            map.put("codeUrl", codeUrl);
            map.put("orderNo", orderInfo.getOrderNo());
            return map;
        } finally {
            response.close();
        }
    }

    @Override
    public void processOrder(Map<String, Object> bodyMap) throws GeneralSecurityException {
        log.info("处理订单");
        String plainText = decryptFromResource(bodyMap);

        Gson gson = new Gson();
        HashMap plainTextMap = gson.fromJson(plainText, HashMap.class);
        String orderNo = (String) plainTextMap.get("out_trade_no");

        if (lock.tryLock()) {
            try {
                String orderStatus = orderInfoService.getOrderStatus(orderNo);
                if (!OrderStatus.NOTPAY.getType().equals(orderStatus)) {
                    return;
                }

                //修改订单状态
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);

                //记录支付记录
                paymentInfoService.createPaymentInfo(plainText);
            } finally {
                //释放锁
                lock.unlock();
            }
        }

    }

    @Override
    public void cancelOrder(String orderNo) throws IOException {

        //调用微信支付的关单接口
        this.closeOrder(orderNo);

        //更新客户端的订单状态
        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CANCEL);
    }

    @Override
    public String queryOrder(String orderNo) throws Exception {

        log.info("查单接口调用 ===> {}", orderNo);

        String url = String.format(WxApiType.ORDER_QUERY_BY_NO.getType(), orderNo);
        url = wxPayConfig.getDomain().concat(url).concat("?mchid=").concat(wxPayConfig.getMchId());

        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");

        CloseableHttpResponse response = wxPayClient.execute(httpGet);

        try {
            String bodyAsString = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) { //处理成功
                log.info("成功,返回结果 = " + bodyAsString);
            } else if (statusCode == 204) { //处理成功，无返回Body
                log.info("成功");
            } else {
                log.info("失败, 响应吗 = " + statusCode + ",返回结果 = " + bodyAsString);
                throw new IOException("request failed");
            }
            return bodyAsString;
        } finally {
            response.close();
        }
    }

    /**
     * 跟据订单号查询，查询订单状态
     * 如果已支付 更新订单状态
     * 如果未支付 关闭支付 并更新订单状态
     *
     * @param orderNo 订单号
     */
    @Override
    public void checkOrderStatus(String orderNo) throws Exception {
        log.error("根据订单号核实订单状态 =====> {}", orderNo);

        String result = this.queryOrder(orderNo);

        Gson gson = new Gson();
        Map resultMap = gson.fromJson(result, HashMap.class);
        Object trade_state = resultMap.get("trade_state");

        if (WxTradeState.SUCCESS.getType().equals(trade_state)) {
            log.error("核实订单已支付，订单号为====>{}", orderNo);
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);
            paymentInfoService.createPaymentInfo(result);
        }

        if (WxTradeState.NOTPAY.getType().equals(trade_state)) {
            log.error("核实订单未支付，订单号====>{}", orderNo);

            this.closeOrder(orderNo);

            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
        }
    }


    @Override
    public void refund(String orderNo, String reason) throws IOException {
        log.info("创建退款单记录");

        RefundInfo refundInfo = refundInfoService.createRefundByOrderNo(orderNo, reason);

        log.info("调用退款API");

        //调用统一退款API
        String url = wxPayConfig.getDomain().concat(WxApiType.DOMESTIC_REFUNDS.getType());

        HttpPost httpPost = new HttpPost(url);

        Gson gson = new Gson();
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("out_trade_no", orderNo);
        paramsMap.put("out_refund_no", refundInfo.getRefundNo());
        paramsMap.put("reason", reason);
        paramsMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyType.REFUND_NOTIFY.getType()));

        Map<String, Object> amountMap = new HashMap<>();
        amountMap.put("refund", refundInfo.getRefund());
        amountMap.put("total", refundInfo.getTotalFee());
        amountMap.put("currency", "CNY");
        paramsMap.put("amount", amountMap);

        String jsonParams = gson.toJson(paramsMap);
        log.info("请求参数," + jsonParams);

        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        //完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpPost);

        try {
            String bodyAsString = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) { //处理成功
                log.info("成功,返回结果 = " + bodyAsString);
            } else if (statusCode == 204) { //处理成功，无返回Body
                log.info("成功");
            } else {
                log.info("失败, 响应吗 = " + statusCode + ",返回结果 = " + bodyAsString);
                throw new IOException("request failed");
            }

            //更新订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_PROCESSING);

            //更新退款单
            refundInfoService.updateRefund(bodyAsString);
        } finally {
            response.close();
        }

        //
    }

    @Override
    public String queryRefund(String refundNo) throws IOException {

        log.info("查询退款接口调用 ====> {}", refundNo);

        String url = String.format(WxApiType.DOMESTIC_REFUNDS_QUERY.getType(), refundNo);
        url = wxPayConfig.getDomain().concat(url);

        //创建远程Get 请求对象
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");

        //完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpGet);

        try {
            String bodyAsString = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) { //处理成功
                log.info("成功,返回结果 = " + bodyAsString);
            } else if (statusCode == 204) { //处理成功，无返回Body
                log.info("成功");
            } else {
                log.info("失败, 响应吗 = " + statusCode + ",返回结果 = " + bodyAsString);
                throw new IOException("request failed");
            }

            return bodyAsString;
        } finally {
            response.close();
        }
    }

    @Override
    public void processRefund(Map<String, Object> bodyMap) throws Exception {

        log.info("退款单");

        String plainText = decryptFromResource(bodyMap);
        Gson gson = new Gson();
        HashMap plainTextMap = gson.fromJson(plainText, HashMap.class);
        String orderNo = (String) plainTextMap.get("out_trade_no");

        if (lock.tryLock()) {
            try {
                String orderStatus = orderInfoService.getOrderStatus(orderNo);
                if (!OrderStatus.REFUND_PROCESSING.getType().equals(orderStatus)) {
                    return;
                }
                //修改订单状态
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_SUCCESS);
                //记录退款记录
                refundInfoService.updateRefund(plainText);
            } finally {
                //释放锁
                lock.unlock();
            }
        }
    }

    @Override
    public String queryBill(String billDate, String type) throws Exception {

        log.info("申请账单接口调用 {}", billDate);

        String url = "";
        if ("tradebill".equals(type)) {
            url = WxApiType.TRADE_BILLS.getType();
        } else if ("fundflowbill".equals(type)) {
            url = WxApiType.FUND_FLOW_BILLS.getType();
        } else {
            throw new RuntimeException("不支持的账单类型");
        }

        url = wxPayConfig.getDomain().concat(url).concat("?bill_date=").concat(billDate);

        HttpGet httpGet = new HttpGet(url);

        httpGet.addHeader("Accept", "application/json");

        CloseableHttpResponse response = wxPayClient.execute(httpGet);

        try {
            String bodyAsString = EntityUtils.toString(response.getEntity());

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                log.info("成功,申请账单返回结果 = " + bodyAsString);
            } else if (statusCode == 204) {
                log.info("成功");
            } else {
                throw new RuntimeException("申请账单异常，响应吗 = " + statusCode + "申请账单返回结果" + bodyAsString);
            }
            Gson gson = new Gson();

            Map<String,String > resultMap = gson.fromJson(bodyAsString,HashMap.class);
            return resultMap.get("download_url");
        }finally {
            response.close();
        }



    }

    @Override
    public String downloadBill(String billDate, String type) throws Exception{

        log.info("下载账单接口调用 {},{}",billDate,type);

        String downloadUrl = this.queryBill(billDate,type);

        HttpGet httpGet = new HttpGet(downloadUrl);
        httpGet.addHeader("Accept","application/json");

        CloseableHttpResponse response = wxPayNoSignClient.execute(httpGet);
        try {
            String bodyAsString = EntityUtils.toString(response.getEntity());

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                log.info("成功,申请账单返回结果 = " + bodyAsString);
            } else if (statusCode == 204) {
                log.info("成功");
            } else {
                throw new RuntimeException("下载账单异常，响应吗 = " + statusCode + "下载账单返回结果" + bodyAsString);
            }
            return bodyAsString;
        }finally {
            response.close();
        }
    }

    /**
     * 关单接口的调用
     *
     * @param orderNo 订单号
     */
    private void closeOrder(String orderNo) throws IOException {
        log.info("关单接口的调用，订单号 ===> {}", orderNo);

        String url = String.format(WxApiType.CLOSE_ORDER_BY_NO.getType(), orderNo);
        url = wxPayConfig.getDomain().concat(url);
        HttpPost httpPost = new HttpPost(url);

        Gson gson = new Gson();
        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("mchid", wxPayConfig.getMchId());
        String jsonParams = gson.toJson(paramsMap);
        log.info("请求参数 ===> {}", jsonParams);

        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        try (CloseableHttpResponse response = wxPayClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) { //处理成功
                log.info("成功,200 ");
            } else if (statusCode == 204) { //处理成功，无返回Body
                log.info("成功,204");
            } else {
                log.info("失败, 响应吗 = " + statusCode);
                throw new IOException("request failed");
            }
        }
    }

    /**
     * 对称解密
     *
     * @param bodyMap
     * @return
     */
    private String decryptFromResource(Map<String, Object> bodyMap) throws GeneralSecurityException {

        log.info("密文解密");

        //通知数据
        Map<String, String> resourceMap = (Map) bodyMap.get("resource");

        //密文数据
        String ciphertext = resourceMap.get("ciphertext");

        //随机数
        String nonce = resourceMap.get("nonce");

        log.info("密文数据=========》{}", ciphertext);
        String associatedData = resourceMap.get("associated_data");
        AesUtil aesUtil = new AesUtil(wxPayConfig.getApiV3Key().getBytes(StandardCharsets.UTF_8));
        String plainText = aesUtil.decryptToString(associatedData.getBytes(StandardCharsets.UTF_8),
                nonce.getBytes(StandardCharsets.UTF_8),
                ciphertext);
        log.info("明文数据=========》{}", plainText);

        return plainText;
    }
}
