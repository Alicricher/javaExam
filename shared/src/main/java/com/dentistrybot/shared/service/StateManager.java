package com.dentistrybot.shared.service;

import com.dentistrybot.shared.repository.StateRepository;
import com.dentistrybot.shared.state.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class StateManager {

    private final StateRepository stateRepository;
    private final ObjectMapper objectMapper;
    private final String botType;

    public StateManager(StateRepository stateRepository, ObjectMapper objectMapper, String botType) {
        this.stateRepository = stateRepository;
        this.objectMapper = objectMapper;
        this.botType = botType;
    }

    public String getState(long telegramId) {
        UserState state = stateRepository.getUserState(telegramId, botType);
        return state != null ? state.getState() : StateConstants.IDLE;
    }

    public UserState getStateWithData(long telegramId) {
        return stateRepository.getUserState(telegramId, botType);
    }

    public void setState(long telegramId, String state) {
        stateRepository.setUserState(telegramId, botType, state, "{}");
    }

    public <T> void setStateWithData(long telegramId, String state, T data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            stateRepository.setUserState(telegramId, botType, state, json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize state data", e);
        }
    }

    public void clearState(long telegramId) {
        stateRepository.clearUserState(telegramId, botType);
    }

    public String getBotType() {
        return botType;
    }

    public <T> T getStateData(UserState userState, Class<T> clazz) {
        if (userState == null || userState.getStateData() == null) return null;
        try {
            return objectMapper.readValue(userState.getStateData(), clazz);
        } catch (Exception e) {
            return null;
        }
    }

    // Convenience typed helpers
    public TestStateData getTestStateData(UserState state) {
        return getStateData(state, TestStateData.class);
    }

    public SituationalStateData getSituationalStateData(UserState state) {
        return getStateData(state, SituationalStateData.class);
    }

    public LessonMenuStateData getLessonMenuStateData(UserState state) {
        return getStateData(state, LessonMenuStateData.class);
    }

    public AdminFilterData getAdminFilterData(UserState state) {
        return getStateData(state, AdminFilterData.class);
    }

    public EditingStateData getEditingStateData(UserState state) {
        return getStateData(state, EditingStateData.class);
    }

    public AdminGradingData getAdminGradingData(UserState state) {
        return getStateData(state, AdminGradingData.class);
    }
}
