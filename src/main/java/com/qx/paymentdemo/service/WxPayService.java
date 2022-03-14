package com.qx.paymentdemo.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * @author Xuan
 * Date: 2022/2/23
 * Time: 17:03
 */
public interface WxPayService {
    Map<String, Object> nativePay(Long productId) throws IOException;

    void processOrder(Map<String, Object> bodyMap) throws GeneralSecurityException;

    void cancelOrder(String orderNo) throws IOException;

    String queryOrder(String orderNo) throws Exception;

    void checkOrderStatus(String orderNo) throws Exception;

    void refund(String orderNo, String reason) throws IOException;

    String queryRefund(String refundNo) throws IOException;

    void processRefund(Map<String, Object> bodyMap) throws Exception;

    String queryBill(String billDate, String type) throws Exception;

    String downloadBill(String billDate, String type) throws Exception;
}

