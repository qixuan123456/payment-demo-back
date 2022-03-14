package com.qx.paymentdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qx.paymentdemo.entity.OrderInfo;
import com.qx.paymentdemo.enums.OrderStatus;

import java.util.List;

public interface OrderInfoService extends IService<OrderInfo> {
    OrderInfo createOrderByProductId(Long ProductId);

    List<OrderInfo> listOrderByCreateTimeDesc();

    void updateStatusByOrderNo(String OrderNo, OrderStatus orderStatus);

    String getOrderStatus(String orderNo);

    List<OrderInfo> getNoPayOrderByDuration(int minutes);

    OrderInfo getOrderByOrderNo(String orderNo);
}
