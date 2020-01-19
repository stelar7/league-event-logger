package no.stelar7.logger;

import com.google.gson.*;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.basic.utils.Pair;
import no.stelar7.api.r4j.impl.lol.lcu.*;
import no.stelar7.api.r4j.impl.lol.liveclient.LiveClientDataAPI;
import no.stelar7.api.r4j.pojo.lol.liveclient.*;
import no.stelar7.api.r4j.pojo.lol.liveclient.events.*;
import no.stelar7.api.r4j.pojo.lol.replay.ReplayTeamType;
import no.stelar7.logger.LCUChampSelectCallback.ChampSelectData;
import no.stelar7.logger.LCUChampSelectCallback.ChampSelectData.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class Server
{
    public static void main(String[] args)
    {
        new Server(2997, true, true);
    }
    
    public Server(int serverPort, boolean ingameThread, boolean testThread)
    {
        Thread socket = createSocketThread(serverPort);
        socket.start();
        
        Thread th = createLCUThread();
        th.start();
        
        if (ingameThread)
        {
            Thread th2 = createIngameThread();
            th2.start();
        }
        
        if (testThread)
        {
            Thread connectionTest = createTestSocketThread(serverPort);
            connectionTest.start();
        }
    }
    
    private Thread createTestSocketThread(int serverPort)
    {
        return new Thread(() -> {
            try
            {
                Socket s = new Socket("localhost", serverPort);
                
                // closing the imput stream will close the socket!
                BufferedReader is = new BufferedReader(new InputStreamReader(s.getInputStream()));
                while (true)
                {
                    Thread.sleep(500);
                    
                    while (is.ready())
                    {
                        String line = is.readLine();
                        System.out.println("Got line: " + line);
                    }
                }
                
            } catch (IOException | InterruptedException e)
            {
                e.printStackTrace();
            }
        }, "Socket test thread");
    }
    
    List<Socket> connections = new ArrayList<>();
    
    private Thread createSocketThread(int serverPort)
    {
        return new Thread(() -> {
            try
            {
                ServerSocket server = new ServerSocket(serverPort);
                System.out.println("Server started on port: " + server.getLocalPort());
                while (true)
                {
                    Socket connection = server.accept();
                    System.out.println("Someone connected to the server!");
                    connections.add(connection);
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }, "Socket thread");
    }
    
    
    private void pushEvent(String name, String data)
    {
        String content = String.format("{\"%s\":%s}%n", name, data);
        
        Path file = Paths.get("D:\\LCU\\events.json");
        pushToFile(file, content);
        
        /*
        if (name.equalsIgnoreCase("FINISH"))
        {
            finishGame(file.resolveSibling("games"));
        }
        */
        
        pushToSockets(content);
    }
    
    private void pushToSockets(String content)
    {
        connections.removeIf(c -> c.isClosed() || !c.isConnected());
        for (Socket connection : new ArrayList<>(connections))
        {
            try
            {
                // closing the output stream will close the socket!
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
                bw.write(content);
                bw.flush();
                
                // System.out.println("Wrote line: \"" + content + "\"");
            } catch (IOException e)
            {
                connections.remove(connection);
            }
        }
    }
    
    private void pushToFile(Path file, String content)
    {
        try
        {
            if (!Files.exists(file) || Files.size(file) == 0)
            {
                Files.createDirectories(file.getParent());
                Files.write(file, "[\n".getBytes(StandardCharsets.UTF_8));
            }
            
            Files.write(file, (content + ",").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    
    private Thread createIngameThread()
    {
        return new Thread(() -> {
            boolean first = true;
            while (true)
            {
                try
                {
                    while (true)
                    {
                        long offsetMillis = (long) (1000f / speedOffset);
                        Thread.sleep(offsetMillis);
                        
                        ActiveGameState gameState = LiveClientDataAPI.getGameStats();
                        if (gameState == null)
                        {
                            break;
                        }
                        
                        if (gameState.getGameMode().equalsIgnoreCase("TFT"))
                        {
                            Thread.sleep(10000);
                            handleGameState(gameState);
                            break;
                        }
                        
                        float time = 0;
                        if (first)
                        {
                            time = gameState.getGameTime();
                            first = false;
                        }
                        
                        List<GameEvent> events = LiveClientDataAPI.getEventData();
                        if (events == null)
                        {
                            first = true;
                            continue;
                        }
                        
                        handleEvents(events, time);
                        
                        
                        if (skipper % gamedataOffset == 0)
                        {
                            handleGameState(gameState);
                        }
                        
                        
                        if (skipper % playerdataOffset == 0)
                        {
                            List<ActiveGamePlayer> players = LiveClientDataAPI.getPlayerList(ReplayTeamType.ALL);
                            handlePlayerList(players);
                        }
                        
                        
                        skipper++;
                    }
                } catch (APIResponseException | InterruptedException e)
                {
                    // ignore this?
                    e.printStackTrace();
                }
            }
        }, "INGAME thread");
    }
    
    private void handlePlayerList(List<ActiveGamePlayer> playerList)
    {
        if (playerList == null)
        {
            return;
        }
        
        if (!ended)
        {
            ended = true;
            pushEvent("CHAMP_SELECT_STOP", "{}");
        }
        
        pushEvent("PLAYERS", gson.toJson(playerList));
    }
    
    private void handleGameState(ActiveGameState gameState)
    {
        if (gameState == null)
        {
            return;
        }
        
        if (!ended)
        {
            ended = true;
            pushEvent("CHAMP_SELECT_STOP", "{}");
        }
        
        pushEvent("STATE", gson.toJson(gameState));
    }
    
    private void pushTimer(String name, float duration, float end)
    {
        JsonObject parent = new JsonObject();
        JsonObject time   = new JsonObject();
        time.addProperty("duration", String.format("%.0f", duration));
        time.addProperty("end", String.format("%.0f", end));
        parent.add(name, time);
        pushEvent("TIMER", parent.toString());
    }
    
    private void handleEvents(List<GameEvent> events, float fromTime)
    {
        if (events == null)
        {
            return;
        }
        
        if (!ended)
        {
            ended = true;
            pushEvent("CHAMP_SELECT_STOP", "{}");
        }
        
        for (GameEvent event : events)
        {
            if (event.getEventID() <= lastEventId)
            {
                continue;
            }
            
            lastEventId = Math.max(lastEventId, event.getEventID());
            
            if (fromTime > event.getEventTime())
            {
                continue;
            }
            
            if (event.getEventName().equalsIgnoreCase("GameStart"))
            {
                pushTimer("Dragon", dragonRespawnTime - event.getEventTime(), dragonRespawnTime);
                pushTimer("Herald", heraldRespawnTime - event.getEventTime(), heraldRespawnTime);
                pushTimer("Baron", baronRespawnTime - event.getEventTime(), baronRespawnTime);
            }
            
            if (event.getEventName().equalsIgnoreCase("DragonKill"))
            {
                DragonKillEvent realEvent = (DragonKillEvent) event;
                dragonKills++;
                dragonRespawnTime = event.getEventTime() + (dragonKills >= 4 ? elderDragonRespawnTimer : dragonRespawnTimer);
                dragonTypeList.add(realEvent.getDragonType());
                
                pushTimer("Dragon", dragonRespawnTime - event.getEventTime(), dragonRespawnTime);
            }
            
            if (event.getEventName().equalsIgnoreCase("HeraldKill"))
            {
                heraldKills++;
                heraldRespawnTime = event.getEventTime() + (heraldKills >= 2 ? 9999999 : heraldRespawnTimer);
                
                pushTimer("Herald", heraldRespawnTime - event.getEventTime(), heraldRespawnTime);
            }
            
            if (event.getEventName().equalsIgnoreCase("BaronKill"))
            {
                baronRespawnTime = event.getEventTime() + baronRespawnTimer;
                
                pushTimer("Baron", heraldRespawnTime - event.getEventTime(), heraldRespawnTime);
            }
            
            if (event.getEventName().equalsIgnoreCase("ChampionKill"))
            {
                ChampionKillEvent realEvent = (ChampionKillEvent) event;
                float             timer     = 0;
                String            victim    = realEvent.getVictimName();
                
                List<ActiveGamePlayer> list = LiveClientDataAPI.getPlayerList(ReplayTeamType.ALL);
                if (list != null)
                {
                    for (ActiveGamePlayer player : list)
                    {
                        if (player.getSummonerName().equalsIgnoreCase(victim))
                        {
                            timer = player.getRespawnTimer();
                            break;
                        }
                    }
                }
                
                pushTimer(victim, timer, timer + event.getEventTime());
            }
            
            if (event.getEventName().equalsIgnoreCase("InhibKilled"))
            {
                InhibKillEvent realEvent = (InhibKillEvent) event;
                
                String target = realEvent.getInhibKilled();
                inhibMap.put(target, event.getEventTime() + inhibRespawnTime);
                float respawnClock = inhibMap.get(target);
                
                pushTimer(target, respawnClock, respawnClock + event.getEventTime());
            }
            
            if (event.getEventName().equalsIgnoreCase("GameEnd"))
            {
                if (canSave)
                {
                    resetLog(true);
                    canSave = false;
                }
            }
            
            pushEvent("EVENT", gson.toJson(event));
        }
    }
    
    private void getCurrentState()
    {
        Object elem = LCUApi.customUrl("lol-champ-select/v1/session", null);
        if (elem instanceof Pair)
        {
            return;
        }
        
        String          real     = (String) elem;
        ChampSelectData callback = gson.fromJson(real, ChampSelectData.class);
        handleUpdateEvent(callback);
    }
    
    private void resetLog(boolean save)
    {
        if (save)
        {
            pushEvent("FINISH", "{}");
            pushEvent("RESET", "{}");
        } else
        {
            try
            {
                Path file = Paths.get("D:\\LCU\\events.json");
                Files.deleteIfExists(file);
                pushEvent("RESET", "{}");
                canSave = true;
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
        state.clear();
        lastBanId = -1;
        lastEventId = -1;
        skipper = 0;
        ended = false;
        dragonRespawnTime = dragonTimer;
        heraldRespawnTime = heraldTimer;
        baronRespawnTime = baronTimer;
        dragonKills = 0;
        heraldKills = 0;
        dragonTypeList.clear();
    }
    
    volatile boolean ended   = false;
    volatile boolean canSave = true;
    Map<Integer, AdjustedPlayerState> state       = new HashMap<>();
    int                               lastBanId   = -1;
    long                              skipper     = 0;
    int                               lastEventId = -1;
    
    // how many ticks in a second
    float speedOffset = 2f;
    
    // how many seconds between data pushes
    // push player stats once per 15 seconds
    int playerdataOffset = (int) (speedOffset * 20);
    // push map info once per 3 hours (large number so its only pushed once per game)
    int gamedataOffset   = (int) (speedOffset * 10800);
    
    // timers
    float              dragonRespawnTime;
    float              baronRespawnTime;
    float              heraldRespawnTime;
    Map<String, Float> inhibMap = new HashMap<>();
    
    // inhibitors respawn every 5min
    int inhibRespawnTime = 60 * 5;
    
    // dragon respawns every 5min
    List<String> dragonTypeList          = new ArrayList<>();
    int          dragonKills             = 0;
    int          dragonTimer             = 60 * 5;
    int          dragonRespawnTimer      = 60 * 5;
    int          elderDragonRespawnTimer = 60 * 6;
    
    // herald spawns at 8min, and respawns 6min after killed
    int heraldKills        = 0;
    int heraldTimer        = 60 * 8;
    int heraldRespawnTimer = 60 * 6;
    
    // herald is removed at 19:30 (19:50 if in combat)
    int heraldRemovalTimer         = (int) (60 * 19.5f);
    int heraldRemovalTimerInCombat = (int) (60 * 19.85f);
    
    // baron spawns at 20min, and respawns 6min after killed
    int baronTimer        = 60 * 20;
    int baronRespawnTimer = 60 * 6;
    
    public void handleChampSelectEvent(String eventData)
    {
        eventData = eventData.substring("{\"OnJsonApiEvent_lol-champ-select_v1_session\":".length(), eventData.length() - 1);
        
        pushToFile(Paths.get("D:\\LCU\\FULL.json"), eventData + "\n");
        
        LCUChampSelectCallback event = gson.fromJson(eventData, LCUChampSelectCallback.class);
        if (event.eventType.equalsIgnoreCase("Create"))
        {
            resetLog(false);
            
            Map<Integer, PlayerState>         newstate   = getTeamState(event.data);
            List<ChampSelectAction>           actionList = adjustActionList(event.data);
            Map<Integer, AdjustedPlayerState> otherState = getAdjustedStateMap(newstate, actionList);
            
            List<AdjustedPlayerState> differences = (List<AdjustedPlayerState>) mapDifferences(otherState, state);
            pushEvent("CHAMP_SELECT_START", gson.toJson(differences));
        }
        
        if (event.eventType.equalsIgnoreCase("Delete"))
        {
            if (!ended)
            {
                ended = true;
                pushEvent("CHAMP_SELECT_STOP", "{}");
            }
        }
        
        if (event.eventType.equalsIgnoreCase("Update"))
        {
            handleUpdateEvent(event.data);
        }
    }
    
    public Map<Integer, AdjustedPlayerState> getAdjustedStateMap(Map<Integer, PlayerState> newstate, List<ChampSelectAction> actionList)
    {
        Map<Integer, AdjustedPlayerState> otherState = new HashMap<>();
        newstate.forEach((k, v) -> {
            Optional<ChampSelectAction> relevant = actionList.stream()
                                                             .filter(a -> a.actorCellId == v.cellId)
                                                             .filter(a -> a.type.equalsIgnoreCase("pick"))
                                                             .filter(a -> a.completed)
                                                             .max((a, b) -> b.id - a.id);
            
            AdjustedPlayerState a = new AdjustedPlayerState();
            a.name = v.playerType.equalsIgnoreCase("bot") ? "BOT" : lookupName(v.summonerId);
            a.skin = v.selectedSkinId == 0 ? (v.championPickIntent * 1000) : v.selectedSkinId;
            a.uuid = v.cellId;
            a.lockedIn = relevant.isPresent();
            a.spell1Id = lookupSpell(v.spell1Id);
            a.spell2Id = lookupSpell(v.spell2Id);
            a.team = v.team;
            otherState.put(v.cellId, a);
        });
        return otherState;
    }
    
    Map<Integer, AdjustedBanState> storedBanData   = new HashMap<>();
    long                           lastInternalNow = 0;
    
    public void handleUpdateEvent(ChampSelectData data)
    {
        Map<Integer, PlayerState>         newstate   = getTeamState(data);
        List<ChampSelectAction>           actionList = adjustActionList(data);
        Map<Integer, AdjustedPlayerState> otherState = getAdjustedStateMap(newstate, actionList);
        
        // for some reason the internal timer doesnt update when we click things..?
        if (data.timer.internalNowInEpochMs != lastInternalNow)
        {
            pushTimer("PHASE_" + data.timer.phase, data.timer.adjustedTimeLeftInPhase, System.currentTimeMillis() + data.timer.adjustedTimeLeftInPhase);
            lastInternalNow = data.timer.internalNowInEpochMs;
        }
        
        List<ChampSelectAction> bans = actionList.stream()
                                                 .filter(a -> a.type.equalsIgnoreCase("ban"))
                                                 .collect(Collectors.toList());
        for (ChampSelectAction ban : bans)
        {
            AdjustedBanState b = new AdjustedBanState();
            b.champion = ban.championId;
            b.lockedIn = ban.completed;
            b.uuid = ban.actorCellId;
            
            AdjustedBanState old = storedBanData.getOrDefault(ban.id, new AdjustedBanState());
            if (!old.equals(b))
            {
                storedBanData.put(ban.id, b);
                pushEvent("BAN", gson.toJson(b));
            }
        }
        
        List<AdjustedPlayerState> differences = (List<AdjustedPlayerState>) mapDifferences(otherState, state);
        pushChange(differences);
        state = otherState;
    }
    
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    
    private void pushChange(List<AdjustedPlayerState> differences)
    {
        differences.forEach(d -> pushEvent("SELECT", gson.toJson(d)));
    }
    
    private List<?> mapDifferences(Map<?, ?> first, Map<?, ?> second)
    {
        return first.entrySet().stream()
                    .filter(e -> !e.getValue().equals(second.get(e.getKey())))
                    .map(Entry::getValue).collect(Collectors.toList());
    }
    
    private String lookupName(long summonerId)
    {
        if (summonerId == 0)
        {
            return "Unknown summoner";
        }
        
        return LCUApi.getSummoner(summonerId).get("displayName").getAsString();
    }
    
    private String lookupSpell(String id)
    {
        if (Objects.equals(id, "18446744073709551615") || Objects.equals(id, "0"))
        {
            return "-1";
        }
        
        /*
        Map<Integer, StaticSummonerSpell> spells = DDragonAPI.getInstance().getSummonerSpells();
        if (spells.containsKey(Integer.valueOf(id)))
        {
            return spells.get(Integer.valueOf(id)).getName();
        }
        */
        
        return id;
    }
    
    private List<ChampSelectAction> adjustActionList(ChampSelectData data)
    {
        List<ChampSelectAction> returnData = new ArrayList<>();
        data.actions.forEach(returnData::addAll);
        returnData.sort(Comparator.comparingInt(a -> a.id));
        return returnData;
    }
    
    public Map<Integer, PlayerState> getTeamState(ChampSelectData data)
    {
        Map<Integer, PlayerState> newstate = new HashMap<>();
        for (PlayerState playerState : data.myTeam)
        {
            newstate.put(playerState.cellId, playerState);
        }
        
        for (PlayerState playerState : data.theirTeam)
        {
            newstate.put(playerState.cellId, playerState);
        }
        
        return newstate;
    }
    
    public Thread createLCUThread()
    {
        return new Thread(() -> {
            while (true)
            {
                try
                {
                    LCUConnection.refetchConnection();
                    LCUSocketReader socket = LCUApi.createWebSocket();
                    socket.connect();
                    
                    // not sure if this is a feature i want?
                    socket.subscribe("OnJsonApiEvent_lol-matchmaking_v1_ready-check", (s) -> handleMatchmakingQueue(s, true));
                    socket.subscribe("OnJsonApiEvent_lol-matchmaking_v1_search", (s) -> handleMatchmakingQueue(s, false));
                    
                    // champion select events
                    socket.subscribe("OnJsonApiEvent_lol-champ-select_v1_session", this::handleChampSelectEvent);
                    
                    System.out.println("Connected to the LCU");
                    while (socket.isConnected())
                    {
                        Thread.sleep(1000);
                    }
                    
                    System.out.println("Socket disconnected, trying again..");
                    
                } catch (NullPointerException e)
                {
                    System.out.println("Unable to connect to the LCU, trying again in 5 seconds...");
                    try
                    {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex)
                    {
                        ex.printStackTrace();
                    }
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }, "LCU Socket thread");
    }
    
    private void handleMatchmakingQueue(String json, boolean isReadyCheck)
    {
        if (!isReadyCheck)
        {
            Map<String, Object> data      = gson.fromJson(json, Map.class);
            Map<String, Object> innerData = (Map<String, Object>) data.get("OnJsonApiEvent_lol-matchmaking_v1_search");
            Map<String, Object> realData  = (Map<String, Object>) innerData.get("data");
            System.out.println("In queue (" + realData.getOrDefault("queueId", "res") + ") for " + realData.getOrDefault("timeInQueue", "res2") + " seconds");
        } else
        {
            Map<String, Object> data      = gson.fromJson(json, Map.class);
            Map<String, Object> innerData = (Map<String, Object>) data.get("OnJsonApiEvent_lol-matchmaking_v1_ready-check");
            if (innerData.get("eventType").equals("Delete"))
            {
                return;
            }
            
            System.out.println("Waiting for ready accept");
        }
    }
}
