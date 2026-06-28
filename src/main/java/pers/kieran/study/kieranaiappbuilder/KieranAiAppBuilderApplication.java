package pers.kieran.study.kieranaiappbuilder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("pers.kieran.study.kieranaiappbuilder.mapper")
public class KieranAiAppBuilderApplication {

    public static void main(String[] args) {
        SpringApplication.run(KieranAiAppBuilderApplication.class, args);
    }

}
