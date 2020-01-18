package no.stelar7.logger;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import no.stelar7.api.r4j.basic.utils.*;
import no.stelar7.api.r4j.impl.lol.raw.*;
import no.stelar7.api.r4j.pojo.lol.staticdata.champion.*;
import no.stelar7.api.r4j.pojo.lol.staticdata.summonerspell.StaticSummonerSpell;

import javax.imageio.ImageIO;
import javax.net.ssl.SSLException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Client
{
    int        serverPort = 2997;
    JsonParser parser     = new JsonParser();
    
    Path inputFolder          = Paths.get("D:\\LCU\\INPUT");
    Path inputSpellFolder     = inputFolder.resolve("spells");
    Path inputSelectFolder    = inputFolder.resolve("select");
    Path inputBanHoverFolder  = inputFolder.resolve("banHover");
    Path inputBanLockedFolder = inputFolder.resolve("banLocked");
    
    Path outputFolder          = Paths.get("D:\\LCU\\OUTPUT");
    Path outputNameFolder      = outputFolder.resolve("names");
    Path outputSpellFolder     = outputFolder.resolve("spells");
    Path outputSelectFolder    = outputFolder.resolve("select");
    Path outputBanHoverFolder  = outputFolder.resolve("banHover");
    Path outputBanLockedFolder = outputFolder.resolve("banLocked");
    
    public static void main(String[] args)
    {
        new Client(2997);
    }
    
    public Client(int serverPort)
    {
        this.serverPort = serverPort;
        
        try
        {
            setupFolders();
            
            System.out.println("Checking image cache");
            Map<Integer, StaticChampion>      champions = DDragonAPI.getInstance().getChampions();
            Map<Integer, StaticSummonerSpell> spells    = DDragonAPI.getInstance().getSummonerSpells();
            downloadBaseImages(champions, spells);
            
            System.out.println("Cache updated!");
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        
        Thread inputThread = createInputThread();
        inputThread.start();
    }
    
    private void setupFolders() throws IOException
    {
        Files.createDirectories(inputSpellFolder);
        Files.createDirectories(inputSelectFolder);
        Files.createDirectories(inputBanHoverFolder);
        Files.createDirectories(inputBanLockedFolder);
        
        Files.createDirectories(outputNameFolder);
        Files.createDirectories(outputSpellFolder);
        Files.createDirectories(outputSelectFolder);
        Files.createDirectories(outputBanHoverFolder);
        Files.createDirectories(outputBanLockedFolder);
    }
    
    private void downloadBaseImages(Map<Integer, StaticChampion> champions, Map<Integer, StaticSummonerSpell> spells) throws IOException
    {
        System.out.println("Feching missing images...");
        List<String> selectFiles = Files.list(inputSelectFolder)
                                        .map(Path::toString)
                                        .map(s -> s.substring(s.lastIndexOf('\\') + 1))
                                        .collect(Collectors.toList());
        
        List<String> hoverBanFiles = Files.list(inputBanHoverFolder)
                                          .map(Path::toString)
                                          .map(s -> s.substring(s.lastIndexOf('\\') + 1))
                                          .collect(Collectors.toList());
        
        List<String> lockedBanFiles = Files.list(inputBanLockedFolder)
                                           .map(Path::toString)
                                           .map(s -> s.substring(s.lastIndexOf('\\') + 1))
                                           .collect(Collectors.toList());
        
        List<Integer> ids = new ArrayList<>(champions.keySet());
        ids.add(-1);
        
        ids.stream()
           .parallel()
           .forEach(i -> {
               String           filename = i + ".png";
               Optional<String> optUrl   = Optional.ofNullable(champions.get(i)).map(champ -> ImageAPI.getInstance().getSquare(champ, null));
            
               if (!selectFiles.contains(filename))
               {
                   System.out.println("Downloading missing select image for champion " + i);
                   optUrl.ifPresentOrElse(url -> downloadFile(inputSelectFolder, filename, url), () -> copyFileFromLocal("noChamp.png", inputSelectFolder, filename));
               }
            
               if (!hoverBanFiles.contains(filename))
               {
                   System.out.println("Downloading missing ban hover image for champion " + i);
                   copyFile(inputSelectFolder, inputBanHoverFolder, filename);
               }
            
               if (!lockedBanFiles.contains(filename))
               {
                   System.out.println("Downloading missing ban locked image for champion " + i);
                   copyFileAndOverlay(inputSelectFolder, inputBanLockedFolder, filename, "banOverlay.png");
               }
           });
        
        List<String> summonerSpellFiles = Files.list(inputSpellFolder)
                                               .map(Path::toString)
                                               .map(s -> s.substring(s.lastIndexOf('\\') + 1))
                                               .collect(Collectors.toList());
        
        ids = new ArrayList<>(spells.keySet());
        ids.stream()
           .parallel()
           .forEach(i -> {
               String           filename = i + ".png";
               Optional<String> optUrl   = Optional.ofNullable(spells.get(i)).map(spell -> ImageAPI.getInstance().getSummonerSpell(spell, null));
            
               if (!summonerSpellFiles.contains(filename))
               {
                   System.out.println("Downloading missing image for summoner spell " + i);
                   optUrl.ifPresentOrElse(url -> downloadFile(inputSpellFolder, filename, url), () -> System.out.println("No file found for spell id " + i));
               }
           });
    }
    
    private void copyFileFromLocal(String localName, Path outputFolder, String filename)
    {
        try
        {
            InputStream is = Client.class.getClassLoader().getResourceAsStream(localName);
            Files.copy(is, outputFolder.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void copyFile(Path inputBanHoverFolder, Path inputSelectFolder, String filename)
    {
        try
        {
            Files.copy(inputBanHoverFolder.resolve(filename), inputSelectFolder.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private synchronized void copyFileAndOverlay(Path inputFolder, Path outputFolder, String filename, String overlayFilename)
    {
        try
        {
            BufferedImage downloaded = ImageIO.read(inputFolder.resolve(filename).toFile());
            BufferedImage internal   = ImageIO.read(Client.class.getClassLoader().getResourceAsStream(overlayFilename));
            
            int           maxw     = Math.max(downloaded.getWidth(), internal.getWidth());
            int           maxh     = Math.max(downloaded.getHeight(), internal.getHeight());
            BufferedImage combined = new BufferedImage(maxw - 2, maxh, BufferedImage.TYPE_INT_ARGB);
            
            Graphics2D g = (Graphics2D) combined.getGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            g.drawImage(downloaded, 12, 15, maxw - 30, maxh - 30, null);
            g.drawImage(internal, -18, -15, maxw + 30, maxh + 30, null);
            
            ImageIO.write(combined, "PNG", outputFolder.resolve(filename).toFile());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private synchronized void downloadFileAndOverlay(Path outputFolder, String filename, String url, String overlayFilename)
    {
        try
        {
            downloadFile(outputFolder, filename, url);
            
            BufferedImage downloaded = ImageIO.read(outputFolder.resolve(filename).toFile());
            BufferedImage internal   = ImageIO.read(Client.class.getClassLoader().getResourceAsStream(overlayFilename));
            
            int           maxw     = Math.max(downloaded.getWidth(), internal.getWidth());
            int           maxh     = Math.max(downloaded.getHeight(), internal.getHeight());
            BufferedImage combined = new BufferedImage(maxw, maxh, BufferedImage.TYPE_INT_ARGB);
            
            Graphics2D g = (Graphics2D) combined.getGraphics();
            g.drawImage(downloaded, 0, 0, maxw, maxh, null);
            g.drawImage(internal, 0, 0, maxw, maxh, null);
            
            ImageIO.write(combined, "PNG", outputFolder.resolve(filename).toFile());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void downloadFile(Path outputFolder, String filename, String url)
    {
        try
        {
            Files.createDirectories(outputFolder);
            Path outputFile = outputFolder.resolve(filename);
            
            URL u = new URL(url);
            try (InputStream is = u.openStream();
                 ReadableByteChannel rbc = Channels.newChannel(is);
                 FileOutputStream fos = new FileOutputStream(outputFile.toFile()))
            {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
        } catch (SSLException e)
        {
            // try again
            downloadFile(outputFolder, filename, url);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private Thread createInputThread()
    {
        return new Thread(() -> {
            try
            {
                // closing the imput stream will close the socket!
                Socket         s  = new Socket("localhost", serverPort);
                BufferedReader is = new BufferedReader(new InputStreamReader(s.getInputStream()));
                System.out.println("Connected to server");
                
                while (true)
                {
                    Thread.sleep(500);
                    
                    while (is.ready())
                    {
                        String     line = is.readLine();
                        JsonObject obj  = parser.parse(line).getAsJsonObject();
                        for (String key : obj.keySet())
                        {
                            //System.out.println(key);
                            switch (key)
                            {
                                case "RESET":
                                {
                                    handleResetEvent(obj.get(key));
                                    break;
                                }
                                case "CHAMP_SELECT_START":
                                {
                                    handleChampSelectStartEvent(obj.get(key));
                                    break;
                                }
                                case "SELECT":
                                {
                                    handleChampionSelectEvent(obj.get(key));
                                    break;
                                }
                                case "BAN":
                                {
                                    handleChampionBanEvent(obj.get(key));
                                    break;
                                }
                                case "CHAMP_SELECT_STOP":
                                {
                                    handleChampSelectStopEvent(obj.get(key));
                                    break;
                                }
                                case "STATE":
                                {
                                    handleGameStateEvent(obj.get(key));
                                    break;
                                }
                                case "TIMER":
                                {
                                    handleTimerStartEvent(obj.get(key));
                                    break;
                                }
                                case "PLAYERS":
                                {
                                    handlePlayerStatusEvent(obj.get(key));
                                    break;
                                }
                                case "FINISH":
                                {
                                    handleGameEndEvent(obj.get(key));
                                    break;
                                }
                                default:
                                    throw new RuntimeException("Unhandled event! " + key);
                            }
                        }
                    }
                }
            } catch (IOException | InterruptedException e)
            {
                e.printStackTrace();
            }
        }, "Input thread");
    }
    
    private void handleGameEndEvent(JsonElement jsonElement)
    {
        
    }
    
    private void handlePlayerStatusEvent(JsonElement jsonElement)
    {
        
    }
    
    private void handleTimerStartEvent(JsonElement jsonElement)
    {
        
    }
    
    private void handleGameStateEvent(JsonElement jsonElement)
    {
        
    }
    
    private void handleChampSelectStopEvent(JsonElement jsonElement)
    {
        uuidToInfoAndIndex.clear();
        teamBanIndex.clear();
        fillEmptyChamps();
        writeSummonerNames(false);
    }
    
    
    Map<Integer, AtomicInteger> teamBanIndex = new HashMap<>();
    
    private void handleChampionBanEvent(JsonElement jsonElement) throws IOException
    {
        ChampionBanInfo info       = Utils.getGson().fromJson(jsonElement, ChampionBanInfo.class);
        int             championId = info.champion;
        if (championId < 1)
        {
            championId = -1;
        }
        
        String                            inputFilename = championId + ".png";
        Path                              inputFile     = inputBanLockedFolder.resolve(inputFilename);
        Pair<ChampionSelectInfo, Integer> teamIndex     = uuidToInfoAndIndex.get(info.uuid);
        
        String outputFilename = teamIndex.getKey() + "\\" + teamIndex.getValue() + ".png";
        Path   outputFile     = outputBanHoverFolder.resolve(outputFilename);
        
        if (info.lockedIn)
        {
            AtomicInteger index = teamBanIndex.computeIfAbsent(teamIndex.getKey().team, k -> new AtomicInteger(0));
            outputFilename = teamIndex.getKey() + "\\" + index.getAndIncrement() + ".png";
            outputFile = outputBanLockedFolder.resolve(outputFilename);
            
            String inputReplaceFilename = "-1.png";
            Path   inputReplaceFile     = inputBanLockedFolder.resolve(inputReplaceFilename);
            String replaceFileName      = teamIndex.getKey() + "\\" + teamIndex.getValue() + ".png";
            Path   replaceFile          = outputBanHoverFolder.resolve(replaceFileName);
            
            Files.createDirectories(replaceFile.getParent());
            Files.copy(inputReplaceFile, replaceFile, StandardCopyOption.REPLACE_EXISTING);
            
            Files.setLastModifiedTime(replaceFile, FileTime.from(Instant.now()));
        }
        
        Files.createDirectories(outputFile.getParent());
        Files.copy(inputFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
        
        Files.setLastModifiedTime(outputFile, FileTime.from(Instant.now()));
    }
    
    private void handleChampionSelectEvent(JsonElement jsonElement) throws IOException
    {
        ChampionSelectInfo info       = Utils.getGson().fromJson(jsonElement, ChampionSelectInfo.class);
        StringBuilder      idExtender = new StringBuilder(String.valueOf(info.skin));
        while (idExtender.length() < 6)
        {
            idExtender.insert(0, "0");
        }
        
        int championId = Integer.parseInt(idExtender.substring(0, 3));
        if (championId < 1)
        {
            championId = -1;
        }
        
        String inputFilename = championId + ".png";
        Path   inputFile     = inputSelectFolder.resolve(inputFilename);
        
        String outputFilename = info.team + "\\" + uuidToInfoAndIndex.get(info.uuid).getValue() + ".png";
        Path   outputFile     = outputSelectFolder.resolve(outputFilename);
        
        Files.createDirectories(outputFile.getParent());
        Files.copy(inputFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
        
        Files.setLastModifiedTime(outputFile, FileTime.from(Instant.now()));
    }
    
    Map<Integer, Pair<ChampionSelectInfo, Integer>> uuidToInfoAndIndex = new HashMap<>();
    
    private void handleChampSelectStartEvent(JsonElement jsonElement)
    {
        uuidToInfoAndIndex.clear();
        teamBanIndex.clear();
        
        List<ChampionSelectInfo>               info  = Utils.getGson().fromJson(jsonElement, new TypeToken<List<ChampionSelectInfo>>() {}.getType());
        Map<Integer, List<ChampionSelectInfo>> teams = info.stream().collect(Collectors.groupingBy(a -> a.team, Collectors.toList()));
        teams.forEach((teamId, v) -> {
            AtomicInteger counter = new AtomicInteger();
            
            v.sort(Comparator.comparingInt(a -> a.uuid));
            v.forEach(e -> uuidToInfoAndIndex.put(e.uuid, new Pair<>(e, counter.getAndIncrement())));
        });
        
        fillEmptyChamps();
        writeSummonerNames(true);
    }
    
    private void writeSummonerNames(boolean flag)
    {
        try
        {
            Files.createDirectories(outputNameFolder);
            
            if (flag)
            {
                uuidToInfoAndIndex.forEach((uuid, ti) -> {
                    try
                    {
                        ChampionSelectInfo inf = ti.getKey();
                        byte[]             data;
                        
                        // might break if someone is named bot, but doubt it....
                        if (inf.name.equalsIgnoreCase("BOT"))
                        {
                            int id = inf.skin / 1000;
                            
                            Map<Integer, StaticChampion> champions = DDragonAPI.getInstance().getChampions();
                            StaticChampion               champ     = champions.get(id);
                            data = ("[BOT] " + champ.getName()).getBytes(StandardCharsets.UTF_8);
                        } else
                        {
                            data = inf.name.getBytes(StandardCharsets.UTF_8);
                        }
                        
                        Files.write(outputNameFolder.resolve(inf.team + "\\" + ti.getValue() + ".txt"), data);
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                });
            } else
            {
                // for each team in a game
                for (int i = 1; i <= 2; i++)
                {
                    Path storeFolder = outputNameFolder.resolve(String.valueOf(i));
                    Files.createDirectories(storeFolder);
                    
                    // for each player in a game
                    for (int j = 0; j < 6; j++)
                    {
                        Files.write(storeFolder.resolve(j + ".txt"), new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    }
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void handleResetEvent(JsonElement jsonElement)
    {
        uuidToInfoAndIndex.clear();
        teamBanIndex.clear();
        
        fillEmptyChamps();
        writeSummonerNames(false);
    }
    
    private void fillEmptyChamps()
    {
        try
        {
            List<Path> outputFolders = Arrays.asList(outputSelectFolder, outputBanHoverFolder, outputBanLockedFolder);
            for (Path folder : outputFolders)
            {
                // for each team in a game
                for (int i = 1; i <= 2; i++)
                {
                    Files.createDirectories(folder.resolve(String.valueOf(i)));
                    
                    // for each player in a game
                    for (int j = 0; j < 6 * 2; j++)
                    {
                        String outputFilename = i + "\\" + j + ".png";
                        Files.copy(inputSelectFolder.resolve("-1.png"), folder.resolve(outputFilename), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private static class ChampionSelectInfo
    {
        int     uuid;
        int     team;
        String  name;
        String  spell1Id;
        String  spell2Id;
        int     skin;
        boolean lockedIn;
    }
    
    private static class ChampionBanInfo
    {
        int     uuid;
        int     champion;
        boolean lockedIn;
    }
}
