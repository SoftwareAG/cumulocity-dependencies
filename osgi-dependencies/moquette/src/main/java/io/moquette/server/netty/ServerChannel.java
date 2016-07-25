package io.moquette.server.netty;

import io.moquette.server.netty.NettyUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.channel.socket.SocketChannel;

public class ServerChannel {
    
    public final static AttributeKey<String> TENANT_ATTRIBUTE = AttributeKey.valueOf("tenant");
    public final static AttributeKey<String> SERIAL_ATTRIBUTE = AttributeKey.valueOf("serial");
    public final static AttributeKey<String> TEMPLATE_ATTRIBUTE = AttributeKey.valueOf("template");
    public final static AttributeKey DEVICE_ATTRIBUTE = AttributeKey.valueOf("device");
    
    private ChannelHandlerContext ctx;
    
    private Channel channel;
    
    public ServerChannel(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        this.channel = ctx.channel();
    }
    
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        if (key == TENANT_ATTRIBUTE ||
            key == SERIAL_ATTRIBUTE ||
            key == TEMPLATE_ATTRIBUTE ||
            key == DEVICE_ATTRIBUTE) {
            return ctx.attr(key);
        }
        return channel.attr(key);
    }
    
    public void close() {
        channel.close();
    }
    
    public void flush() {
        channel.flush();
    }

    public void writeAndFlush(Object value) {
        channel.writeAndFlush(value);
    }
    
    public ChannelPipeline pipeline() {
        return channel.pipeline();
    }
    
    public Channel channel() {
        return channel;
    }
    
    @Override
    public String toString() {
        String clientID = (String) NettyUtils.clientID(channel);
        return "session [clientID: "+ clientID +"]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channel == null) ? 0 : channel.hashCode());
        result = prime * result + ((ctx == null) ? 0 : ctx.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ServerChannel other = (ServerChannel) obj;
        if (channel == null) {
            if (other.channel != null)
                return false;
        } else if (!channel.equals(other.channel))
            return false;
        if (ctx == null) {
            if (other.ctx != null)
                return false;
        } else if (!ctx.equals(other.ctx))
            return false;
        return true;
    }

}
