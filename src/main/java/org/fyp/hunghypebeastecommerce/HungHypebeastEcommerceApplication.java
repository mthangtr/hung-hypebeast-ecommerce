package org.fyp.hunghypebeastecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HungHypebeastEcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HungHypebeastEcommerceApplication.class, args);
    }

}
