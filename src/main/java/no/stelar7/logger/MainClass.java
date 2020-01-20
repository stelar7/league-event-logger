package no.stelar7.logger;

import java.nio.file.*;
import java.util.*;

public class MainClass
{
    public static void main(String[] args)
    {
        List<String> allowedArgs = Arrays.asList("client", "server");
        
        if (args.length < 2 || args.length > 8)
        {
            System.err.println("Invalid argument count, expected between 2 and 7");
            System.exit(0);
        }
        
        if (!allowedArgs.contains(args[0]))
        {
            System.err.println("Invalid argument, expected first argument to match 'server' or 'client'");
            System.exit(0);
        }
        
        int offset = 0;
        offset = parseArgs(args, offset);
        offset = parseArgs(args, offset);
    }
    
    private static int parseArgs(String[] args, int offset)
    {
        if (args.length <= offset)
        {
            return offset;
        }
        
        if (args[offset].equalsIgnoreCase("server"))
        {
            var temp = new Object()
            {
                int port = 2997;
                boolean test = false;
                boolean ingame = false;
            };
            try
            {
                temp.port = Integer.parseInt(args[offset + 1]);
            } catch (NumberFormatException e)
            {
                System.err.println("Invalid argument, expected a port number after 'server'");
                System.exit(0);
            }
            
            int total = args.length - offset;
            if (total > 2)
            {
                if (args[offset + 2].equalsIgnoreCase("client"))
                {
                    new Thread(() -> new Server(temp.port, temp.ingame, temp.test)).start();
                    return offset + 2;
                }
                
                List<String> valids = Arrays.asList("true", "false");
                if (valids.contains(args[offset + 2]))
                {
                    temp.ingame = Boolean.parseBoolean(args[offset + 2]);
                    if (total == 3)
                    {
                        new Thread(() -> new Server(temp.port, temp.ingame, temp.test)).start();
                        return offset + 3;
                    }
                }
                
                if (valids.contains(args[offset + 3]))
                {
                    temp.test = Boolean.parseBoolean(args[offset + 3]);
                    new Thread(() -> new Server(temp.port, temp.ingame, temp.test)).start();
                    return offset + 3;
                }
            } else
            {
                new Thread(() -> new Server(temp.port, temp.ingame, temp.test)).start();
                return offset + 2;
            }
        }
        
        if (args[offset].equalsIgnoreCase("client"))
        {
            if (args.length - offset < 3)
            {
                System.err.println("Expected 3 arguments for client (port, input, output)");
                System.exit(0);
            }
            
            int port = 2997;
            try
            {
                port = Integer.parseInt(args[offset + 1]);
            } catch (NumberFormatException e)
            {
                System.err.println("Invalid argument, expected a port number after 'client'");
                System.exit(0);
            }
            
            int  finalPort    = port;
            Path inputFolder  = Paths.get(args[offset + 2]);
            Path outputFolder = Paths.get(args[offset + 3]);
            
            new Thread(() -> {
                try
                {
                    // delay for a second so the server has time to boot
                    Thread.sleep(1000);
                    new Client(inputFolder, outputFolder, finalPort);
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }).start();
            return offset + 4;
        }
        
        return 0;
    }
}
