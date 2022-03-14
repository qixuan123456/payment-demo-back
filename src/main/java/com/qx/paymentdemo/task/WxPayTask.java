package com.qx.paymentdemo.task;

import com.qx.paymentdemo.entity.OrderInfo;
import com.qx.paymentdemo.service.OrderInfoService;
import com.qx.paymentdemo.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Xuan
 * Date: 2022/3/12
 * Time: 16:36
 */
@Slf4j
@Component
public class WxPayTask {

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private WxPayService wxPayService;

    @Scheduled(cron = "030 * * * * ?")
    public void orderConfirm() throws Exception {

        log.info("orderConfirm 被执行......");

        List<OrderInfo> orderInfoList = orderInfoService.getNoPayOrderByDuration(5);

        for (OrderInfo orderInfo : orderInfoList) {
            String orderNo = orderInfo.getOrderNo();
            log.error("超时订单 ====> {}", orderNo);

            wxPayService.checkOrderStatus(orderNo);
        }
    }
}
