package com.qx.paymentdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.qx.paymentdemo.entity.OrderInfo;
import com.qx.paymentdemo.entity.RefundInfo;
import com.qx.paymentdemo.mapper.RefundInfoMapper;
import com.qx.paymentdemo.service.OrderInfoService;
import com.qx.paymentdemo.service.RefundInfoService;
import com.qx.paymentdemo.util.OrderNoUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class RefundInfoServiceImpl extends ServiceImpl<RefundInfoMapper, RefundInfo> implements RefundInfoService {

    @Resource
    OrderInfoService orderInfoService;

    @Override
    public RefundInfo createRefundByOrderNo(String orderNo, String reason) {

        //根据订单号获取订单信息
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setOrderNo(orderNo); //订单编号
        refundInfo.setRefundNo(OrderNoUtils.getRefundNo()); //退款单编号
        refundInfo.setTotalFee(orderInfo.getTotalFee()); //原订单定额
        refundInfo.setRefund(orderInfo.getTotalFee()); //退款金额
        refundInfo.setReason(reason); //退款原因

        baseMapper.insert(refundInfo);

        return refundInfo;
    }

    @Override
    public void updateRefund(String bodyAsString) {

        //将json字符串转换为map
        Gson gson = new Gson();
        Map<String,String> resultMap = gson.fromJson(bodyAsString, HashMap.class);

        //根据退款单编号修改退款单
        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no",resultMap.get("out_refund_no"));

        //设置要修改的字段
        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setRefundId(resultMap.get("refund_id")); //微信支付退款单号

        //查询退款和申请退款中的返回参数
        if(resultMap.get("status")!=null){
            refundInfo.setRefundStatus(resultMap.get("status"));
            refundInfo.setContentReturn(bodyAsString);
        }

        //退款回调中的回调函数
        if(resultMap.get("refund_status")!=null){
            refundInfo.setRefundStatus(resultMap.get("refund_status"));
            refundInfo.setContentNotify(bodyAsString);
        }
        baseMapper.update(refundInfo,queryWrapper);
    }
}
