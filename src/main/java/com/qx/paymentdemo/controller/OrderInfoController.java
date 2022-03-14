package com.qx.paymentdemo.controller;

import com.qx.paymentdemo.entity.OrderInfo;
import com.qx.paymentdemo.enums.OrderStatus;
import com.qx.paymentdemo.service.OrderInfoService;
import com.qx.paymentdemo.vo.R;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Xuan
 * Date: 2022/3/9
 * Time: 12:55
 */
@CrossOrigin
@Api(tags = "商品订单管理")
@RestController
@RequestMapping("/api/order-info")
public class OrderInfoController {

    @Resource
    OrderInfoService orderInfoService;

    @GetMapping("list")
    public R list(){
        List<OrderInfo> list = orderInfoService.listOrderByCreateTimeDesc();
        return R.ok().data("list",list);
    }

    /**
     * 查询本地订单状态
     * @param orderNo
     * @return
     */
    @GetMapping("query-order-status/{orderNo}")
    public R queryOrderStatus(@PathVariable String orderNo){
        String orderStatus = orderInfoService.getOrderStatus(orderNo);
        if(OrderStatus.SUCCESS.getType().equals(orderStatus)){
            return R.ok().setMessage("支付成功");
        }
        return R.ok().setCode(101).setMessage("支付中......");
    }
}
