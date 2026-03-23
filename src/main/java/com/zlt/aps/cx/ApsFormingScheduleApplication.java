package com.zlt.aps.cx;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 金宇轮胎APS成型排程系统启动类
 *
 * @author APS Team
 * @since 2.0.0
 */
@SpringBootApplication
@MapperScan("com.zlt.aps.cx.mapper")
public class ApsFormingScheduleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsFormingScheduleApplication.class, args);
    }
}
