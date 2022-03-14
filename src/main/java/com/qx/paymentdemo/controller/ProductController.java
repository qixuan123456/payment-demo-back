package com.qx.paymentdemo.controller;

import com.qx.paymentdemo.entity.Product;
import com.qx.paymentdemo.service.ProductService;
import com.qx.paymentdemo.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author Xuan
 * Date: 2022/2/21
 * Time: 18:54
 */
@CrossOrigin
@Api(tags = "商品管理")
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Resource
    private ProductService productService;

    @ApiOperation("测试接口")
    @GetMapping("/test")
    public R test(){
        return R.ok().data("message","hello").data("now",new Date());
    }

    @ApiOperation("商品列表")
    @GetMapping("/list")
    public R list(){
        List<Product> list = productService.list();
        return R.ok().data("productList",list);
    }
}
