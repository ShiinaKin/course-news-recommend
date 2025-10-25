package io.sakurasou.newsrecommend.config

import org.mybatis.spring.annotation.MapperScan
import org.springframework.context.annotation.Configuration

@Configuration
@MapperScan("io.sakurasou.newsrecommend.dao")
class MyBatisConfig
