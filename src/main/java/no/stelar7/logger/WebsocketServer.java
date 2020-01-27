package no.stelar7.logger;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class WebsocketServer extends WebSocketServer
{
    public WebsocketServer(int i)
    {
        super(new InetSocketAddress(i));
    }
    
    @Override
    public void onOpen(WebSocket con, ClientHandshake clientHandshake)
    {
        System.out.println("Someone connected to the websocket");
    }
    
    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b)
    {
    
    }
    
    @Override
    public void onMessage(WebSocket webSocket, String s)
    {
    
    }
    
    @Override
    public void onError(WebSocket webSocket, Exception e)
    {
    
    }
    
    @Override
    public void onStart()
    {
        System.out.println("Websocket started");
    }
}
