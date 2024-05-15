package com.benson;

import com.benson.server.HTTPServer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NettyDemoApplication implements CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication.run(NettyDemoApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        HTTPServer httpServer = new HTTPServer();
        httpServer.start(8080);
    }
}
