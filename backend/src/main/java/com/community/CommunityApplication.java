package com.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java &amp; AI 交流社区后端入口。
 * 模块化单体：业务按包划分（user/oauth/share/forum/notify），包结构即未来拆分边界。
 */
@SpringBootApplication
public class CommunityApplication {

    /** 应用入口。 */
    public static void main(String[] args) {
        SpringApplication.run(CommunityApplication.class, args);
    }
}
