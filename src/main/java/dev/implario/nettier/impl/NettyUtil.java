package dev.implario.nettier.impl;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.experimental.UtilityClass;

import java.util.concurrent.TimeUnit;

@UtilityClass
public class NettyUtil {

    public static final Class<? extends SocketChannel> CLIENT_CHANNEL_CLASS;
    public static final Class<? extends ServerSocketChannel> SERVER_CHANNEL_CLASS;
    public static final EventLoopGroup CLIENT_GROUP;
    public static final EventLoopGroup SERVER_GROUP;

    static {
        boolean epoll;
        try {
            Class.forName("io.netty.channel.epoll.Epoll");
            epoll = !Boolean.getBoolean("nettier.disable-native-transport") && Epoll.isAvailable();
        } catch (ClassNotFoundException ignored) {
            epoll = false;
        }
        if (epoll) {
            CLIENT_CHANNEL_CLASS = EpollSocketChannel.class;
            SERVER_CHANNEL_CLASS = EpollServerSocketChannel.class;
            CLIENT_GROUP = new EpollEventLoopGroup(1);
            SERVER_GROUP = new EpollEventLoopGroup(1);
        } else {
            CLIENT_CHANNEL_CLASS = NioSocketChannel.class;
            SERVER_CHANNEL_CLASS = NioServerSocketChannel.class;
            CLIENT_GROUP = new NioEventLoopGroup(1);
            SERVER_GROUP = new NioEventLoopGroup(1);
        }
    }

    public static void schedule(Runnable command, long delay, TimeUnit unit) {
        CLIENT_GROUP.schedule(command, delay, unit);
    }



}
