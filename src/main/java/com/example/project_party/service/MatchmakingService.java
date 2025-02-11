package com.example.project_party.service;

import com.example.project_party.model.Match;
import com.example.project_party.model.Player;
import com.example.project_party.repository.MatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.awt.print.Pageable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MatchmakingService {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private MatchRepository matchRepository;

    @Cacheable(value = "playersCache", key = "#players.hashCode()")
    public void matchPlayers(List<Player> players) {
        ObjectMapper objectMapper = new ObjectMapper();
        for (Player player : players) {
            int skillLevel = player.getSkillLevel();
            try {
                Map<String, Object> preferences = objectMapper.readValue(
                        player.getPreferences(),
                        Map.class
                );
                System.out.println("Player skill level: " + skillLevel);
                System.out.println("Player preferences: " + preferences);
            } catch (Exception e) {
                throw new RuntimeException("Error parsing preferences JSON", e);
            }
        }
    }

    @Cacheable(value = "matchesCache", key = "#matchRequest.id")
    public Match createMatch(Match matchRequest) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Player> players = new ArrayList<>();
        List<Player> availablePlayers = playerService.getAllAvailablePlayers();

        for (Player player : availablePlayers) {
            try {
                Map preferences = objectMapper.readValue(
                        player.getPreferences(),
                        Map.class
                );

                if (Math.abs(player.getSkillLevel() - matchRequest.getRequiredSkillLevel()) <= 1) {
                    if (preferences.get("maxDistance") != null &&
                            Integer.parseInt(preferences.get("maxDistance").toString()) <= matchRequest.getMaxDistance()) {
                        players.add(player);
                        if (players.size() == 2) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Error parsing preferences JSON", e);
            }
        }

        if (players.size() < 2) {
            throw new RuntimeException("Not enough players for a match");
        }

        matchRequest.setPlayers(players);
        return matchRepository.save(matchRequest);
    }

    public Match getMatch(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));
    }

    public Page<Match> getAllMatches(Pageable pageable) {
        return matchRepository.findAll((org.springframework.data.domain.Pageable) pageable);
    }
}
