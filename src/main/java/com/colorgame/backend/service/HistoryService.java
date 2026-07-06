package com.colorgame.backend.service;

import com.colorgame.backend.dto.HistoryDTO;
import com.colorgame.backend.dto.HistoryRequest;
import com.colorgame.backend.model.GridHistory;
import com.colorgame.backend.model.User;
import com.colorgame.backend.repository.HistoryRepository;
import com.colorgame.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final UserRepository userRepository;

    public HistoryService(HistoryRepository historyRepository, UserRepository userRepository) {
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    public HistoryDTO recordChange(String username, HistoryRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé."));

        GridHistory history = new GridHistory();
        history.setUser(user);
        history.setRowIndex(request.getRowIndex());
        history.setColIndex(request.getColIndex());
        history.setColor(request.getColor());

        historyRepository.save(history);

        return toDTO(history);
    }

    public List<HistoryDTO> getMyHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé."));

        return historyRepository.findByUserOrderByChangedAtDesc(user)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<HistoryDTO> getAllHistory() {
        return historyRepository.findAllByOrderByChangedAtDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private HistoryDTO toDTO(GridHistory h) {
        return new HistoryDTO(
                h.getId(),
                h.getUser().getUsername(),
                h.getRowIndex(),
                h.getColIndex(),
                h.getColor(),
                h.getChangedAt()
        );
    }
}