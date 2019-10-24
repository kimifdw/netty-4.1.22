package io.netty.example.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @author: he.tian
 * @time: 2019-07-29 14:06
 */
public class Client {

    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup workGroup  = new NioEventLoopGroup();
        Bootstrap bootstrap  = new Bootstrap();
        bootstrap.group(workGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY,true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new LoggingHandler());
                        pipeline.addLast(new ClientHandler());
                    }
                });
        ChannelFuture channelFuture = bootstrap.connect("localhost", 8080).sync();
        channelFuture.channel().closeFuture().sync();
        workGroup.shutdownGracefully();
    }
}
