
package com.example.stockmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    excludeName = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"
    }
)
public class StockmarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockmarketApplication.class, args);
    }
}
