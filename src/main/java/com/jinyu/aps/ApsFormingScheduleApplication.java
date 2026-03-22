package com.jinyu.aps;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 金宇轮胎APS系统-成型排程模块启动类
 * 
 * @author APS Team
 * @version 1.0.0
 */
@SpringBootApplication
@MapperScan("com.jinyu.aps.mapper")
public class ApsFormingScheduleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsFormingScheduleApplication.class, args);
        System.out.println("========================================");
        System.out.println("  APS成型排程系统启动成功!");
        System.out.println("  Swagger文档地址: http://localhost:5000/api/doc.html");
        System.out.println("========================================");
    }
}
