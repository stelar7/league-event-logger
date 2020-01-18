package no.stelar7.logger;

import java.util.*;

public class LCUChampSelectCallback
{
    public ChampSelectData data;
    public String          eventType;
    public String          uri;
    
    @Override
    public String toString()
    {
        return "LCUChampSelectCallback{" +
               "data=" + data +
               ", eventType='" + eventType + '\'' +
               ", uri='" + uri + '\'' +
               '}';
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        LCUChampSelectCallback that = (LCUChampSelectCallback) o;
        return Objects.equals(data, that.data) &&
               Objects.equals(eventType, that.eventType) &&
               Objects.equals(uri, that.uri);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(data, eventType, uri);
    }
    
    static class ChampSelectData
    {
        public List<List<ChampSelectAction>> actions;
        public BanState                      bans;
        public List<PlayerState>             myTeam;
        public List<PlayerState>             theirTeam;
        public ChampSelectTimer              timer;
        
        @Override
        public String toString()
        {
            return "ChampSelectData{" +
                   "actions=" + actions +
                   ", bans=" + bans +
                   ", myTeam=" + myTeam +
                   ", theirTeam=" + theirTeam +
                   ", timer=" + timer +
                   '}';
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }
            ChampSelectData that = (ChampSelectData) o;
            return Objects.equals(actions, that.actions) &&
                   Objects.equals(bans, that.bans) &&
                   Objects.equals(myTeam, that.myTeam) &&
                   Objects.equals(theirTeam, that.theirTeam) &&
                   Objects.equals(timer, that.timer);
        }
        
        @Override
        public int hashCode()
        {
            return Objects.hash(actions, bans, myTeam, theirTeam, timer);
        }
        
        static class ChampSelectTimer
        {
            public int    adjustedTimeLeftInPhase;
            public long   internalNowInEpochMs;
            public int    timeLeftInPhase;
            public String phase;
            
            @Override
            public String toString()
            {
                return "ChampSelectTimer{" +
                       "adjustedTimeLeftInPhase=" + adjustedTimeLeftInPhase +
                       ", internalNowInEpochMs=" + internalNowInEpochMs +
                       ", timeLeftInPhase=" + timeLeftInPhase +
                       ", phase='" + phase + '\'' +
                       '}';
            }
            
            @Override
            public boolean equals(Object o)
            {
                if (this == o)
                {
                    return true;
                }
                if (o == null || getClass() != o.getClass())
                {
                    return false;
                }
                ChampSelectTimer that = (ChampSelectTimer) o;
                return adjustedTimeLeftInPhase == that.adjustedTimeLeftInPhase &&
                       internalNowInEpochMs == that.internalNowInEpochMs &&
                       timeLeftInPhase == that.timeLeftInPhase &&
                       Objects.equals(phase, that.phase);
            }
            
            @Override
            public int hashCode()
            {
                return Objects.hash(adjustedTimeLeftInPhase, internalNowInEpochMs, timeLeftInPhase, phase);
            }
            
        }
        
        static class AdjustedBanState
        {
            int     uuid;
            int     champion;
            boolean lockedIn;
            
            @Override
            public boolean equals(Object o)
            {
                if (this == o)
                {
                    return true;
                }
                if (o == null || getClass() != o.getClass())
                {
                    return false;
                }
                AdjustedBanState that = (AdjustedBanState) o;
                return uuid == that.uuid &&
                       champion == that.champion &&
                       lockedIn == that.lockedIn;
            }
            
            @Override
            public int hashCode()
            {
                return Objects.hash(uuid, champion, lockedIn);
            }
            
            @Override
            public String toString()
            {
                return "AdjustedBanState{" +
                       "uuid=" + uuid +
                       ", champion=" + champion +
                       ", lockedIn=" + lockedIn +
                       '}';
            }
        }
        
        static class AdjustedPlayerState
        {
            int     uuid;
            int     team;
            String  name;
            String  spell1Id;
            String  spell2Id;
            int     skin;
            boolean lockedIn;
            
            @Override
            public boolean equals(Object o)
            {
                if (this == o)
                {
                    return true;
                }
                if (o == null || getClass() != o.getClass())
                {
                    return false;
                }
                AdjustedPlayerState that = (AdjustedPlayerState) o;
                return uuid == that.uuid &&
                       team == that.team &&
                       skin == that.skin &&
                       lockedIn == that.lockedIn &&
                       Objects.equals(name, that.name) &&
                       Objects.equals(spell1Id, that.spell1Id) &&
                       Objects.equals(spell2Id, that.spell2Id);
            }
            
            @Override
            public int hashCode()
            {
                return Objects.hash(uuid, team, name, spell1Id, spell2Id, skin, lockedIn);
            }
            
            @Override
            public String toString()
            {
                return "AdjustedPlayerState{" +
                       "uuid=" + uuid +
                       ", team=" + team +
                       ", name='" + name + '\'' +
                       ", spell1Id='" + spell1Id + '\'' +
                       ", spell2Id='" + spell2Id + '\'' +
                       ", skin=" + skin +
                       ", lockedIn=" + lockedIn +
                       '}';
            }
        }
        
        static class PlayerState
        {
            public String assignedPosition;
            public int    cellId;
            public int    championId;
            public int    championPickIntent;
            public String playerType;
            public int    selectedSkinId;
            public String spell1Id;
            public String spell2Id;
            public int    summonerId;
            public int    team;
            
            @Override
            public boolean equals(Object o)
            {
                if (this == o)
                {
                    return true;
                }
                if (o == null || getClass() != o.getClass())
                {
                    return false;
                }
                PlayerState that = (PlayerState) o;
                return cellId == that.cellId &&
                       championId == that.championId &&
                       championPickIntent == that.championPickIntent &&
                       selectedSkinId == that.selectedSkinId &&
                       summonerId == that.summonerId &&
                       team == that.team &&
                       Objects.equals(assignedPosition, that.assignedPosition) &&
                       Objects.equals(playerType, that.playerType) &&
                       Objects.equals(spell1Id, that.spell1Id) &&
                       Objects.equals(spell2Id, that.spell2Id);
            }
            
            @Override
            public int hashCode()
            {
                return Objects.hash(assignedPosition, cellId, championId, championPickIntent, playerType, selectedSkinId, spell1Id, spell2Id, summonerId, team);
            }
            
            @Override
            public String toString()
            {
                return "PlayerState{" +
                       "assignedPosition='" + assignedPosition + '\'' +
                       ", cellId=" + cellId +
                       ", championId=" + championId +
                       ", championPickIntent=" + championPickIntent +
                       ", playerType='" + playerType + '\'' +
                       ", selectedSkinId=" + selectedSkinId +
                       ", spell1Id='" + spell1Id + '\'' +
                       ", spell2Id='" + spell2Id + '\'' +
                       ", summonerId=" + summonerId +
                       ", team=" + team +
                       '}';
            }
        }
        
        static class BanState
        {
            public List<Integer> myTeamBans;
            public List<Integer> theirTeamBans;
            public int           numBans;
            
            @Override
            public String toString()
            {
                return "BanState{" +
                       "myTeamBans=" + myTeamBans +
                       ", theirTeamBans=" + theirTeamBans +
                       ", numBans=" + numBans +
                       '}';
            }
            
            @Override
            public boolean equals(Object o)
            {
                if (this == o)
                {
                    return true;
                }
                if (o == null || getClass() != o.getClass())
                {
                    return false;
                }
                BanState banState = (BanState) o;
                return numBans == banState.numBans &&
                       Objects.equals(myTeamBans, banState.myTeamBans) &&
                       Objects.equals(theirTeamBans, banState.theirTeamBans);
            }
            
            @Override
            public int hashCode()
            {
                return Objects.hash(myTeamBans, theirTeamBans, numBans);
            }
        }
        
        static class ChampSelectAction
        {
            public int     actorCellId;
            public int     championId;
            public boolean completed;
            public int     pickTurn;
            public String  type;
            public int     id;
            
            @Override
            public String toString()
            {
                return "ChampSelectAction{" +
                       "cellId=" + actorCellId +
                       ", championId=" + championId +
                       ", completed=" + completed +
                       ", pickTurn=" + pickTurn +
                       ", type='" + type + '\'' +
                       ", id=" + id +
                       '}';
            }
            
            @Override
            public boolean equals(Object o)
            {
                if (this == o)
                {
                    return true;
                }
                if (o == null || getClass() != o.getClass())
                {
                    return false;
                }
                ChampSelectAction that = (ChampSelectAction) o;
                return actorCellId == that.actorCellId &&
                       championId == that.championId &&
                       completed == that.completed &&
                       pickTurn == that.pickTurn &&
                       id == that.id &&
                       Objects.equals(type, that.type);
            }
            
            @Override
            public int hashCode()
            {
                return Objects.hash(actorCellId, championId, completed, pickTurn, type, id);
            }
        }
    }
}
