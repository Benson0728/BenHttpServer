package com.benson.server;



import com.benson.handler.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import java.util.concurrent.TimeUnit;


public class HTTPServer {

    public HTTPServer() {
    }

    public  void start(int port) throws InterruptedException {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        try {
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS));
                            pipeline.addLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS));
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new GetRouteHandler());
                            pipeline.addLast(new HttpObjectAggregator(1000000));
                            pipeline.addLast(new PostRouteHandler());
                            pipeline.addLast(new PutRouteHandler());
                            pipeline.addLast(new DeleteRouteHandler());
                            pipeline.addLast(new ExceptionHandler());
                        }
                    });
            ChannelFuture channelFuture = bootstrap.bind(port).sync();
            channelFuture.channel().closeFuture().sync();
        }finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

}
