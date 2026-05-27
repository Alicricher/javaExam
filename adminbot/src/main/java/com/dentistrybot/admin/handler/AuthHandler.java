package com.dentistrybot.admin.handler;

import com.dentistrybot.admin.keyboard.AdminKeyboards;
import com.dentistrybot.admin.localization.AdminMessages;
import com.dentistrybot.shared.model.Admin;
import com.dentistrybot.shared.repository.AdminRepository;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.state.StateConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;

public class AuthHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    private final TelegramClient bot;
    private final StateManager stateManager;
    private final AdminRepository adminRepository;
    private final String adminPassword;

    public AuthHandler(TelegramClient bot, StateManager stateManager,
                       AdminRepository adminRepository, String adminPassword) {
        this.bot = bot;
        this.stateManager = stateManager;
        this.adminRepository = adminRepository;
        this.adminPassword = adminPassword;
    }

    public void handleStart(long chatId, long telegramId) {
        try {
            if (!adminRepository.adminExists(telegramId)) {
                Admin admin = new Admin();
                admin.setTelegramId(telegramId);
                admin.setPasswordHash(encoder.encode(adminPassword));
                adminRepository.createAdmin(admin);
            }

            if (adminRepository.isAdminAuthenticated(telegramId)) {
                showMainMenu(chatId);
                return;
            }

            stateManager.setState(telegramId, StateConstants.ADMIN_LOGIN);
            bot.execute(SendMessage.builder().chatId(chatId).text(AdminMessages.MSG_ENTER_PASSWORD).build());
        } catch (Exception e) {
            log.error("handleStart error: {}", e.getMessage());
        }
    }

    public void handlePasswordInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        String password = message.getText();
        try {
            bot.execute(DeleteMessage.builder().chatId(chatId).messageId(message.getMessageId()).build());
            Admin admin = adminRepository.getAdminByTelegramId(telegramId);
            if (admin == null) return;

            if (!encoder.matches(password, admin.getPasswordHash())) {
                bot.execute(SendMessage.builder().chatId(chatId).text(AdminMessages.MSG_WRONG_PASSWORD).build());
                return;
            }

            LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
            adminRepository.updateAdminSession(telegramId, true, expiresAt);
            stateManager.clearState(telegramId);
            bot.execute(SendMessage.builder().chatId(chatId).text(AdminMessages.MSG_ADMIN_LOGIN_SUCCESS).build());
            showMainMenu(chatId);
        } catch (Exception e) {
            log.error("handlePasswordInput error: {}", e.getMessage());
        }
    }

    public void showMainMenu(long chatId) {
        try {
            bot.execute(SendMessage.builder()
                .chatId(chatId).text(AdminMessages.MSG_ADMIN_MENU)
                .replyMarkup(AdminKeyboards.adminMenu()).build());
        } catch (Exception e) {
            log.error("showMainMenu error: {}", e.getMessage());
        }
    }

    public void handleLogout(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            adminRepository.logoutAdmin(telegramId);
            stateManager.clearState(telegramId);
            bot.execute(AnswerCallbackQuery.builder().callbackQueryId(callback.getId()).build());
            bot.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
            bot.execute(SendMessage.builder().chatId(chatId).text(AdminMessages.MSG_LOGGED_OUT).build());
        } catch (Exception e) {
            log.error("handleLogout error: {}", e.getMessage());
        }
    }

    public boolean isAuthenticated(long telegramId) {
        try { return adminRepository.isAdminAuthenticated(telegramId); }
        catch (Exception e) { return false; }
    }

    public boolean isInLoginState(long telegramId) {
        return StateConstants.ADMIN_LOGIN.equals(stateManager.getState(telegramId));
    }
}
