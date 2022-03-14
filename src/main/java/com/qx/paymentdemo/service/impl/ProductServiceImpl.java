package com.qx.paymentdemo.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qx.paymentdemo.entity.Product;
import com.qx.paymentdemo.mapper.ProductMapper;
import com.qx.paymentdemo.service.ProductService;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
}