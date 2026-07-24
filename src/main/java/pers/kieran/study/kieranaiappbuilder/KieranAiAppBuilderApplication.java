package pers.kieran.study.kieranaiappbuilder;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("pers.kieran.study.kieranaiappbuilder.mapper")
public class KieranAiAppBuilderApplication {
    public static void main(String[] args) {
        SpringApplication.run(KieranAiAppBuilderApplication.class, args);
    }
}
