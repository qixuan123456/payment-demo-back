package com.qx.paymentdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qx.paymentdemo.entity.RefundInfo;

public interface RefundInfoService extends IService<RefundInfo> {

    RefundInfo createRefundByOrderNo(String orderNo, String reason);

    void updateRefund(String bodyAsString);
}
