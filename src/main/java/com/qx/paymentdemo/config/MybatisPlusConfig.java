package com.qx.paymentdemo.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Xuan
 * Date: 2022/2/22
 * Time: 12:59
 */
@Configuration
@MapperScan("com.qx.paymentdemo.mapper")
@EnableTransactionManagement
public class MybatisPlusConfig {
}
