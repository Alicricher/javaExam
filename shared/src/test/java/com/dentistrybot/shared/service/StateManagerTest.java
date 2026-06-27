package com.dentistrybot.shared.service;

import com.dentistrybot.shared.repository.StateRepository;
import com.dentistrybot.shared.state.LessonMenuStateData;
import com.dentistrybot.shared.state.StateConstants;
import com.dentistrybot.shared.state.UserState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StateManager unit tests")
class StateManagerTest {

    private static final long TELEGRAM_ID = 12345L;
    private static final String BOT_TYPE = "student";

    @Mock
    private StateRepository stateRepository;

    private StateManager stateManager;

    @BeforeEach
    void setUp() {
        stateManager = new StateManager(stateRepository, new ObjectMapper(), BOT_TYPE);
    }

    @Test
    void shouldReturnStoredStateWhenUserStateExists() {
        UserState userState = new UserState();
        userState.setState(StateConstants.IN_TEST);
        when(stateRepository.getUserState(TELEGRAM_ID, BOT_TYPE)).thenReturn(userState);

        String state = stateManager.getState(TELEGRAM_ID);

        assertEquals(StateConstants.IN_TEST, state);
    }

    @Test
    void shouldReturnIdleWhenUserStateDoesNotExist() {
        when(stateRepository.getUserState(TELEGRAM_ID, BOT_TYPE)).thenReturn(null);

        String state = stateManager.getState(TELEGRAM_ID);

        assertEquals(StateConstants.IDLE, state);
    }

    @Test
    void shouldSetStateWithEmptyJsonData() {
        stateManager.setState(TELEGRAM_ID, StateConstants.LESSON_MENU);

        verify(stateRepository).setUserState(
            TELEGRAM_ID,
            BOT_TYPE,
            StateConstants.LESSON_MENU,
            "{}"
        );
    }

    @Test
    void shouldSerializeDataWhenSettingStateWithData() {
        LessonMenuStateData data = new LessonMenuStateData(7, 42);

        stateManager.setStateWithData(TELEGRAM_ID, StateConstants.LESSON_MENU, data);

        verify(stateRepository).setUserState(
            TELEGRAM_ID,
            BOT_TYPE,
            StateConstants.LESSON_MENU,
            "{\"unit_id\":7,\"lesson_id\":42}"
        );
    }

    @Test
    void shouldClearStateForConfiguredBotType() {
        stateManager.clearState(TELEGRAM_ID);

        verify(stateRepository).clearUserState(TELEGRAM_ID, BOT_TYPE);
    }

    @Test
    void shouldDeserializeTypedStateData() {
        UserState userState = new UserState();
        userState.setStateData("{\"unit_id\":7,\"lesson_id\":42}");

        LessonMenuStateData data = stateManager.getLessonMenuStateData(userState);

        assertEquals(7, data.getUnitId());
        assertEquals(42, data.getLessonId());
    }

    @Test
    void shouldReturnNullForInvalidStateData() {
        UserState userState = new UserState();
        userState.setStateData("not-json");

        LessonMenuStateData data = stateManager.getLessonMenuStateData(userState);

        assertNull(data);
    }
}
