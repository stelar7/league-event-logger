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
    Path inputFolder;
    Path inputSpellFolder;
    Path inputBanHoverFolder;
    Path inputBanLockedFolder;
    Path inputSelectHoverFolder;
    Path inputSelectLockedFolder;
    
    Path outputFolder;
    Path outputPlayerNameFolder;
    Path outputChampionNameFolder;
    Path outputSpellFolder;
    Path outputBanHoverFolder;
    Path outputBanLockedFolder;
    Path outputSelectHoverFolder;
    Path outputSelectLockedFolder;
    
    public static void main(String[] args)
    {
        new Client(Paths.get("D:\\LCU\\INPUT"), Paths.get("D:\\LCU\\OUTPUT"), 2997);
    }
    
    public Client(Path inputFolder, Path outputFolder, int serverPort)
    {
        this.inputFolder = Paths.get("D:\\LCU\\INPUT");
        inputSpellFolder = inputFolder.resolve("spells");
        inputSelectHoverFolder = inputFolder.resolve("selectHover");
        inputSelectLockedFolder = inputFolder.resolve("selectLocked");
        inputBanHoverFolder = inputFolder.resolve("banHover");
        inputBanLockedFolder = inputFolder.resolve("banLocked");
        
        
        this.outputFolder = Paths.get("D:\\LCU\\OUTPUT");
        outputPlayerNameFolder = outputFolder.resolve("names").resolve("players");
        outputChampionNameFolder = outputFolder.resolve("names").resolve("champions");
        outputSpellFolder = outputFolder.resolve("spells");
        outputSelectHoverFolder = outputFolder.resolve("selectHover");
        outputSelectLockedFolder = outputFolder.resolve("selectLocked");
        outputBanHoverFolder = outputFolder.resolve("banHover");
        outputBanLockedFolder = outputFolder.resolve("banLocked");
        
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
        
        
        Thread inputThread = createInputThread(serverPort);
        inputThread.start();
    }
    
    private void setupFolders() throws IOException
    {
        Files.createDirectories(inputSpellFolder);
        Files.createDirectories(inputBanHoverFolder);
        Files.createDirectories(inputBanLockedFolder);
        Files.createDirectories(inputSelectHoverFolder);
        Files.createDirectories(inputSelectLockedFolder);
        
        Files.createDirectories(outputSpellFolder);
        Files.createDirectories(outputBanHoverFolder);
        Files.createDirectories(outputBanLockedFolder);
        Files.createDirectories(outputPlayerNameFolder);
        Files.createDirectories(outputSelectHoverFolder);
        Files.createDirectories(outputSelectLockedFolder);
        Files.createDirectories(outputChampionNameFolder);
    }
    
    private void downloadBaseImages(Map<Integer, StaticChampion> champions, Map<Integer, StaticSummonerSpell> spells) throws IOException
    {
        System.out.println("Feching missing images...");
        List<String> selectFilesLocked = Files.list(inputSelectLockedFolder)
                                              .map(Path::toString)
                                              .map(s -> s.substring(s.lastIndexOf('\\') + 1))
                                              // remove ext
                                              .map(s -> s.substring(0, s.lastIndexOf('.')))
                                              .collect(Collectors.toList());
        
        List<String> selectFilesHovered = Files.list(inputSelectHoverFolder)
                                               .map(Path::toString)
                                               .map(s -> s.substring(s.lastIndexOf('\\') + 1))
                                               // remove ext
                                               .map(s -> s.substring(0, s.lastIndexOf('.')))
                                               .collect(Collectors.toList());
        
        List<String> hoverBanFiles = Files.list(inputBanHoverFolder)
                                          .map(Path::toString)
                                          .map(s -> s.substring(s.lastIndexOf('\\') + 1))
                                          // remove ext
                                          .map(s -> s.substring(0, s.lastIndexOf('.')))
                                          .collect(Collectors.toList());
        
        List<String> lockedBanFiles = Files.list(inputBanLockedFolder)
                                           .map(Path::toString)
                                           .map(s -> s.substring(s.lastIndexOf('\\') + 1))
                                           // remove ext
                                           .map(s -> s.substring(0, s.lastIndexOf('.')))
                                           .collect(Collectors.toList());
        
        List<Integer> ids = new ArrayList<>(champions.keySet());
        ids.add(-1);
        ids.stream()
           .parallel()
           .forEach(i -> {
               String           filename = String.valueOf(i);
               Optional<String> optUrl   = Optional.ofNullable(champions.get(i)).map(champ -> ImageAPI.getInstance().getSquare(champ, null));
            
               if (!selectFilesHovered.contains(filename))
               {
                   System.out.println("Downloading missing select hover image for champion " + i);
                   optUrl.ifPresentOrElse(url -> downloadFile(inputSelectHoverFolder, filename + ".png", url), () -> copyFileFromLocal("noChamp.png", inputSelectHoverFolder, filename));
               }
            
               if (!selectFilesLocked.contains(filename))
               {
                   System.out.println("Downloading missing select locked image for champion " + i);
                   copyFile(findFile(inputSelectHoverFolder, filename), inputSelectLockedFolder);
               }
            
               if (!hoverBanFiles.contains(filename))
               {
                   System.out.println("Downloading missing ban hover image for champion " + i);
                   copyFile(findFile(inputSelectHoverFolder, filename), inputBanHoverFolder);
               }
            
               if (!lockedBanFiles.contains(filename))
               {
                   System.out.println("Downloading missing ban locked image for champion " + i);
                   copyFileAndOverlay(findFile(inputSelectHoverFolder, filename), inputBanLockedFolder, "banOverlay.png");
               }
           });
        
        List<String> summonerSpellFiles = Files.list(inputSpellFolder)
                                               .map(Path::toString)
                                               .map(s -> s.substring(s.lastIndexOf('\\') + 1))
                                               // remove ext
                                               .map(s -> s.substring(0, s.lastIndexOf('.')))
                                               .collect(Collectors.toList());
        
        ids = new ArrayList<>(spells.keySet());
        ids.add(-1);
        
        ids.stream()
           .parallel()
           .forEach(i -> {
               String           filename = String.valueOf(i);
               Optional<String> optUrl   = Optional.ofNullable(spells.get(i)).map(spell -> ImageAPI.getInstance().getSummonerSpell(spell, null));
            
               if (!summonerSpellFiles.contains(filename))
               {
                   System.out.println("Downloading missing image for summoner spell " + i);
                   optUrl.ifPresentOrElse(url -> downloadFile(inputSpellFolder, filename + ".png", url), () -> copyFileFromLocal("noSpell.png", inputSpellFolder, filename));
               }
           });
    }
    
    private void copyFileFromLocal(String localName, Path outputFolder, String filename)
    {
        try
        {
            String      ext = localName.substring(localName.lastIndexOf('.'));
            InputStream is  = Client.class.getClassLoader().getResourceAsStream(localName);
            Files.copy(is, outputFolder.resolve(filename + ext), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void copyFile(Path inputFile, Path outputfolder)
    {
        try
        {
            Files.copy(inputFile, outputfolder.resolve(inputFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void copyFile(Path inputfolder, Path outputfolder, String filename)
    {
        try
        {
            Files.copy(inputfolder.resolve(filename), outputfolder.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private synchronized void copyFileAndOverlay(Path inputFile, Path outputFolder, String overlayFilename)
    {
        try
        {
            BufferedImage downloaded = ImageIO.read(inputFile.toFile());
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
            
            ImageIO.write(combined, "PNG", outputFolder.resolve(inputFile.getFileName()).toFile());
        } catch (IOException e)
        {
            System.out.println();
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
    
    private Thread createInputThread(int serverPort)
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
                        JsonObject obj  = JsonParser.parseString(line).getAsJsonObject();
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
                System.err.println("Unable to connect to the server, make sure its started first!");
                System.exit(0);
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
        fillEmptySpells();
        writeSummonerNames(false);
        writeChampionNames(false);
    }
    
    
    Map<Integer, AtomicInteger> teamBanIndex = new HashMap<>();
    
    private void handleChampionBanEvent(JsonElement jsonElement)
    {
        ChampionBanInfo info       = Utils.getGson().fromJson(jsonElement, ChampionBanInfo.class);
        int             championId = info.champion;
        if (championId < 1)
        {
            championId = -1;
        }
        
        Path   inputFile = findFile(inputBanLockedFolder, String.valueOf(championId));
        String ext       = getExt(inputFile);
        
        Pair<ChampionSelectInfo, Integer> teamIndex = uuidToInfoAndIndex.get(info.uuid);
        int                               teamId    = teamIndex.getKey().team;
        int                               playerId  = teamIndex.getValue();
        
        String outputFilename = teamId + "\\" + playerId + ext;
        Path   outputFile     = outputBanHoverFolder.resolve(outputFilename);
        
        if (info.lockedIn)
        {
            AtomicInteger index = teamBanIndex.computeIfAbsent(teamId, k -> new AtomicInteger(0));
            outputFilename = teamId + "\\" + index.getAndIncrement() + ext;
            outputFile = outputBanLockedFolder.resolve(outputFilename);
            
            
            // replace the hover image with nothing?
            String inputReplaceFilename = "-1";
            Path   inputReplaceFile     = findFile(inputBanHoverFolder, inputReplaceFilename);
            String replaceExt           = getExt(inputReplaceFile);
            String replaceFileName      = teamId + "\\" + playerId + replaceExt;
            Path   replaceFile          = outputBanHoverFolder.resolve(replaceFileName);
            
            replaceFileAndUpdateTimestamp(inputReplaceFile, replaceFile);
        }
        
        replaceFileAndUpdateTimestamp(inputFile, outputFile);
    }
    
    private String getExt(Path path)
    {
        String filename = path.getFileName().toString();
        return filename.substring(filename.lastIndexOf('.'));
    }
    
    private void handleChampionSelectEvent(JsonElement jsonElement)
    {
        ChampionSelectInfo info          = Utils.getGson().fromJson(jsonElement, ChampionSelectInfo.class);
        int                playerId      = uuidToInfoAndIndex.get(info.uuid).getValue();
        String             teamAndPlayer = info.team + "\\" + playerId;
        
        StringBuilder idExtender = new StringBuilder(String.valueOf(info.skin));
        while (idExtender.length() < 6)
        {
            idExtender.insert(0, "0");
        }
        
        int championId = Integer.parseInt(idExtender.substring(0, 3));
        if (championId < 1)
        {
            championId = -1;
        }
        
        // champion
        Path inputPath  = info.lockedIn ? inputSelectLockedFolder : inputSelectHoverFolder;
        Path outputPath = info.lockedIn ? outputSelectLockedFolder : outputSelectHoverFolder;
        
        Path   inputFile      = findFile(inputPath, String.valueOf(championId));
        String inputExt       = getExt(inputFile);
        String outputFilename = teamAndPlayer + inputExt;
        Path   outputFile     = outputPath.resolve(outputFilename);
        replaceFileAndUpdateTimestamp(inputFile, outputFile);
        
        if (info.lockedIn)
        {
            inputFile = findFile(inputSelectHoverFolder, "-1");
            inputExt = getExt(inputFile);
            outputFilename = teamAndPlayer + inputExt;
            outputFile = outputSelectHoverFolder.resolve(outputFilename);
            replaceFileAndUpdateTimestamp(inputFile, outputFile);
        }
        
        
        // champion name
        outputFilename = teamAndPlayer + ".txt";
        outputFile = outputChampionNameFolder.resolve(outputFilename);
        writeStringToFile(outputFile, getChampionNameFromId(championId));
        
        
        // summmoner spells
        inputFile = findFile(inputSpellFolder, info.spell1Id);
        inputExt = getExt(inputFile);
        outputFilename = teamAndPlayer + "_1" + inputExt;
        outputFile = outputSpellFolder.resolve(outputFilename);
        replaceFileAndUpdateTimestamp(inputFile, outputFile);
        
        inputFile = findFile(inputSpellFolder, info.spell2Id);
        inputExt = getExt(inputFile);
        outputFilename = teamAndPlayer + "_2" + inputExt;
        outputFile = outputSpellFolder.resolve(outputFilename);
        replaceFileAndUpdateTimestamp(inputFile, outputFile);
    }
    
    private void replaceFileAndUpdateTimestamp(Path inputFile, Path outputFile)
    {
        try
        {
            Files.createDirectories(outputFile.getParent());
            Files.copy(inputFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
            Files.setLastModifiedTime(outputFile, FileTime.from(Instant.now()));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
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
        fillEmptySpells();
        writeSummonerNames(true);
        writeChampionNames(true);
    }
    
    private void writeStringToFile(Path file, String data)
    {
        try
        {
            Files.createDirectories(file.getParent());
            Files.write(file, data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.setLastModifiedTime(file, FileTime.from(Instant.now()));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private String getChampionNameFromId(int id)
    {
        Map<Integer, StaticChampion> champions = DDragonAPI.getInstance().getChampions();
        StaticChampion               champ     = champions.getOrDefault(id, null);
        return champ == null ? "" : champ.getName();
    }
    
    private void writeChampionNames(boolean flag)
    {
        if (flag)
        {
            uuidToInfoAndIndex.forEach((uuid, ti) -> {
                ChampionSelectInfo inf        = ti.getKey();
                Path               outputFile = outputChampionNameFolder.resolve(inf.team + "\\" + ti.getValue() + ".txt");
                
                int id = inf.skin / 1000;
                writeStringToFile(outputFile, getChampionNameFromId(id));
            });
        } else
        {
            // for each team in a game
            for (int i = 1; i <= 2; i++)
            {
                Path storeFolder = outputChampionNameFolder.resolve(String.valueOf(i));
                
                // for each player in a game
                for (int j = 0; j < 6; j++)
                {
                    writeStringToFile(storeFolder.resolve(j + ".txt"), "");
                }
            }
        }
    }
    
    private void writeSummonerNames(boolean flag)
    {
        if (flag)
        {
            uuidToInfoAndIndex.forEach((uuid, ti) -> {
                ChampionSelectInfo inf        = ti.getKey();
                Path               outputFile = outputPlayerNameFolder.resolve(inf.team + "\\" + ti.getValue() + ".txt");
                writeStringToFile(outputFile, inf.name);
            });
        } else
        {
            // for each team in a game
            for (int i = 1; i <= 2; i++)
            {
                Path storeFolder = outputPlayerNameFolder.resolve(String.valueOf(i));
                
                // for each player in a game
                for (int j = 0; j < 6; j++)
                {
                    writeStringToFile(storeFolder.resolve(j + ".txt"), "");
                }
            }
        }
    }
    
    private void handleResetEvent(JsonElement jsonElement)
    {
        uuidToInfoAndIndex.clear();
        teamBanIndex.clear();
        
        fillEmptyChamps();
        fillEmptySpells();
        writeSummonerNames(false);
        writeChampionNames(false);
    }
    
    private void fillEmptySpells()
    {
        // for each team in a game
        for (int i = 1; i <= 2; i++)
        {
            // for each player in a game
            for (int j = 0; j < 6 * 2; j++)
            {
                Path   inFile = findFile(inputSpellFolder, "-1");
                String ext    = getExt(inFile);
                
                String outputFilename = i + "\\" + j + "_1" + ext;
                replaceFileAndUpdateTimestamp(inFile, outputSpellFolder.resolve(outputFilename));
                
                outputFilename = i + "\\" + j + "_2" + ext;
                replaceFileAndUpdateTimestamp(inFile, outputSpellFolder.resolve(outputFilename));
            }
        }
    }
    
    private void fillEmptyChamps()
    {
        // for each team in a game
        for (int i = 1; i <= 2; i++)
        {
            // for each player in a game
            for (int j = 0; j < 6 * 2; j++)
            {
                String outputFilename = i + "\\" + j;
                
                Path   inFile = findFile(inputBanHoverFolder, "-1");
                String ext    = getExt(inFile);
                replaceFileAndUpdateTimestamp(inFile, outputBanHoverFolder.resolve(outputFilename + ext));
                
                inFile = findFile(inputBanLockedFolder, "-1");
                ext = getExt(inFile);
                replaceFileAndUpdateTimestamp(inFile, outputBanLockedFolder.resolve(outputFilename + ext));
                
                inFile = findFile(inputSelectHoverFolder, "-1");
                ext = getExt(inFile);
                replaceFileAndUpdateTimestamp(inFile, outputSelectHoverFolder.resolve(outputFilename + ext));
                
                inFile = findFile(inputSelectLockedFolder, "-1");
                ext = getExt(inFile);
                replaceFileAndUpdateTimestamp(inFile, outputSelectLockedFolder.resolve(outputFilename + ext));
            }
        }
    }
    
    private Path findFile(Path folder, String filename)
    {
        try
        {
            return Files.list(folder)
                        .filter(p -> p.getFileName().toString().startsWith(filename))
                        .findFirst()
                        .orElse(null);
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
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
